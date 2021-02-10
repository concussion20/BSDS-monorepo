import config.SuperMarketConfig;
import exception.ArgsException;
import java.awt.EventQueue;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import multithread.PurchaseTask;
import util.LineCharter;

public class SuperMarketClient {

  public static void main(String[] args) throws InterruptedException, IOException, ArgsException {
    Properties prop = SuperMarketConfig.loadConfig();
    assert prop != null;
    int[] numThreadsArray = {32, 64, 128, 256};
    double[] walltimes = new double[numThreadsArray.length];

    int loop = 0;
    for (int numThreads : numThreadsArray) {
      ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1000);
      AtomicInteger numReqs = new AtomicInteger();
      AtomicInteger numFailed = new AtomicInteger();

      long start = System.currentTimeMillis();

      // phase 1
      int curStoreID = 0;
      for (int i = 1; i <= numThreads / 4; i++) {
        PurchaseTask task = new PurchaseTask(prop, curStoreID + i, numReqs, numFailed);
        executor.execute(task);
      }
      curStoreID += numThreads / 4;

      // phase 2
      Thread.sleep(3 * 60 * 60 * 1000);
      for (int i = 1; i <= numThreads / 4; i++) {
        PurchaseTask task = new PurchaseTask(prop, curStoreID + i, numReqs, numFailed);
        executor.execute(task);
      }
      curStoreID += numThreads / 4;

      // phase 3
      Thread.sleep(2 * 60 * 60 * 1000);
      for (int i = 1; i <= numThreads / 2; i++) {
        PurchaseTask task = new PurchaseTask(prop, curStoreID + i, numReqs, numFailed);
        executor.execute(task);
      }
      curStoreID += numThreads / 2;

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.MINUTES);

      long end = System.currentTimeMillis();
      long wallTime = end - start;
      walltimes[loop] = wallTime * 1.0 / 1000;
      System.out.println("Job finished after " + wallTime + " ms");

      System.out.println("Number of sent POST requests:" + numReqs.get());
      System.out.println("Number of failed POST requests: " + numFailed.get());
      System.out.println("Throughput is: " + (numReqs.get() * 1000.0 / wallTime ));

      loop++;
    }

    EventQueue.invokeLater(() -> {
      try {
        LineCharter ex = new LineCharter(numThreadsArray, walltimes
            , "Walltime per numThreads", "NumThreads"
            , "Walltime (s)", "Walltime per numThreads.png");
        ex.setVisible(true);
      } catch (IOException ignored) {
      }
    });
  }

}
