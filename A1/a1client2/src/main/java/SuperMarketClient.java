import config.SuperMarketConfig;
import constants.Constants;
import execption.ArgsException;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import multithread.CSVWriter;
import multithread.PurchaseTask;
import multithread.ResponseRecordsConsumer;
import org.apache.commons.io.FileUtils;
import util.CSVReader;
import util.LineCharter;

public class SuperMarketClient {

  private static void cleanOutputDir() throws IOException {
    FileUtils.cleanDirectory(new File(Constants.OUTPUT_PATH));
  }

  public static void main(String[] args) throws InterruptedException, IOException, ArgsException {
    Properties prop = SuperMarketConfig.loadConfig();
    assert prop != null;
    int[] numThreadsArray = {32, 64, 128, 256};
    double[] throughputs = new double[numThreadsArray.length];
    double[] means = new double[numThreadsArray.length];

    int loop = 0;
    for (int numThreads : numThreadsArray) {
      cleanOutputDir();

      BlockingQueue<String> responses = new ArrayBlockingQueue<>(100000);
      ThreadPoolExecutor producers = (ThreadPoolExecutor) Executors.newFixedThreadPool(1000);
      ThreadPoolExecutor consumers = (ThreadPoolExecutor) Executors.newFixedThreadPool(60);

      int latencyLowerBarrier = Integer.parseInt(prop.getProperty(Constants.LATENCY_LOWER_BARRIER));
      int latencyUpperBarrier = Integer.parseInt(prop.getProperty(Constants.LATENCY_UPPER_BARRIER));
      int gap = Integer.parseInt(prop.getProperty(Constants.LATENCY_BUCKET_GAP));
      int numBuckets = (latencyUpperBarrier - latencyLowerBarrier) / gap + 2;
      StringBuffer[] buckets = new StringBuffer[numBuckets];
      for (int i = 0; i < numBuckets; i++) {
        buckets[i] = new StringBuffer();
      }
      Object[] bucketLocks = new Object[numBuckets];
      for (int i = 0; i < numBuckets; i++) {
        bucketLocks[i] = new Object();
      }
      AtomicInteger[] numResponsesInBuckets = new AtomicInteger[numBuckets];
      for (int i = 0; i < numBuckets; i++) {
        numResponsesInBuckets[i] = new AtomicInteger();
      }

      AtomicInteger numReqs = new AtomicInteger();
      AtomicInteger numFailed = new AtomicInteger();
      AtomicLong meanLatency = new AtomicLong();
      AtomicInteger maxLatency = new AtomicInteger();

      long start = System.currentTimeMillis();
      AtomicBoolean isPhase2Begin = new AtomicBoolean(false);
      AtomicBoolean isPhase3Begin = new AtomicBoolean(false);

      // producers sending POST requests
      // phase 1
      int curStoreID = 0;
      for (int i = 1; i <= numThreads / 4; i++) {
        PurchaseTask task = new PurchaseTask(prop, curStoreID + i, numReqs, numFailed
            , responses, isPhase2Begin, isPhase3Begin);
        producers.execute(task);
      }
      curStoreID += numThreads / 4;

      // consumers retrieving response strings
      for (int i = 0; i < 60; i++) {
        ResponseRecordsConsumer task = new ResponseRecordsConsumer(responses, buckets, bucketLocks
            , numResponsesInBuckets, latencyLowerBarrier, latencyUpperBarrier, gap
            , meanLatency, maxLatency);
        consumers.execute(task);
      }

      // for every 10 secs, output results to CSV files
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      scheduler.scheduleAtFixedRate(new CSVWriter(buckets, bucketLocks), 10, 15, TimeUnit.SECONDS);

      // phase 2
      while (true) {
        if (isPhase2Begin.get()) {
          for (int i = 1; i <= numThreads / 4; i++) {
            PurchaseTask task = new PurchaseTask(prop, curStoreID + i, numReqs, numFailed
               , responses, isPhase2Begin, isPhase3Begin);
            producers.execute(task);
          }
          curStoreID += numThreads / 4;
          break;
        }
      }

      // phase 3
      while (true) {
        if (isPhase3Begin.get()) {
          for (int i = 1; i <= numThreads / 2; i++) {
            PurchaseTask task = new PurchaseTask(prop, curStoreID + i, numReqs, numFailed
                , responses, isPhase2Begin, isPhase3Begin);
            producers.execute(task);
          }
          curStoreID += numThreads / 2;
          break;
        }
      }

      producers.shutdown();
      producers.awaitTermination(10, TimeUnit.MINUTES);

      long end = System.currentTimeMillis();
      long wallTime = end - start;
      System.out.println("Job finished after " + wallTime + " ms");

      System.out.println("Number of sent POST requests:" + numReqs.get());
      System.out.println("Number of failed POST requests: " + numFailed.get());
      double throughput = numReqs.get() * 1000.0 / wallTime;
      throughputs[loop] = throughput;
      System.out.println("Throughput is: " + throughput);

      // mark ending for consumers to know
//      responses.put(Constants.END_OF_POSTS);
      consumers.shutdown();
      consumers.awaitTermination(20, TimeUnit.SECONDS);
      // wait for all results saved to files
      Thread.sleep(15000);
      scheduler.shutdown();
      scheduler.awaitTermination(15, TimeUnit.SECONDS);

      System.out.println("The max latency value is: " + maxLatency.get());
      double dmeanLatency = meanLatency.get() * 1.0 / numReqs.get();
      means[loop] = dmeanLatency;
      System.out.println("The mean latency value is: " + dmeanLatency);

      System.out.println("Number of buckets:" + numBuckets);
      System.out.println("Number of requests in each bucket:");
      for (int i = 0; i < numBuckets; i++) {
        System.out.print((i + 1) + ":" + numResponsesInBuckets[i].get() + " ");
      }
      System.out.println("\n");

      int p99 = (int)(0.99 * numReqs.get());
      int median = (int)(0.5 * numReqs.get());
      int curNum = 0;
      boolean isMedianFound = false;
      for (int i = 0; i < numBuckets; i++) {
        if ( (!isMedianFound && curNum + numResponsesInBuckets[i].get() > median)
            || (curNum + numResponsesInBuckets[i].get() > p99) ) {
          List<Integer> latencies = CSVReader.readLatenciesFromCSV(i + 1);
          Collections.sort(latencies);
          if (!isMedianFound) {
            System.out.println("Median latency is: " + latencies.get(median - curNum));
            isMedianFound = true;
          } else {
            System.out.println("P99 latency is: " + latencies.get(p99 - curNum));
            break;
          }
        }
        curNum += numResponsesInBuckets[i].get();
      }
      System.out.println();

      loop++;
    }

    EventQueue.invokeLater(() -> {
      try {
        LineCharter ex = new LineCharter(numThreadsArray, throughputs
            , "Throughput per numThreads", "NumThreads"
            , "Throughput (numRequest/s)", "Throughput per numThreads.png");
        ex.setVisible(true);
      } catch (IOException ignored) {
      }
      try {
        LineCharter ex2 = new LineCharter(numThreadsArray, means
            , "Mean latency per numThreads", "NumThreads", "Mean (ms)"
            , "Mean latency per numThreads.png");
        ex2.setVisible(true);
      } catch (IOException ignored) {
      }
    });
  }

}
