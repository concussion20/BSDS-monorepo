package multithread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ResponseRecordsConsumer implements Runnable {
  private BlockingQueue<String> blockingQueue;
  private StringBuffer[] buckets;
  private final Object[] bucketLocks;
  private AtomicInteger[] numResponsesInBuckets;
  private int latencyLowerBarrier;
  private int latencyUpperBarrier;
  private int gap;
  private AtomicLong meanLatency;
  private AtomicInteger maxLatency;

  public ResponseRecordsConsumer(BlockingQueue<String> blockingQueue, StringBuffer[] buckets
      , Object[] bucketLocks, AtomicInteger[] numResponsesInBuckets, int latencyLowerBarrier
      , int latencyUpperBarrier, int gap, AtomicLong meanLatency, AtomicInteger maxLatency) {
    this.blockingQueue = blockingQueue;
    this.buckets = buckets;
    this.bucketLocks = bucketLocks;
    this.numResponsesInBuckets = numResponsesInBuckets;
    this.latencyLowerBarrier = latencyLowerBarrier;
    this.latencyUpperBarrier = latencyUpperBarrier;
    this.gap = gap;
    this.meanLatency = meanLatency;
    this.maxLatency = maxLatency;
  }

  @Override
  public void run() {
    while (true) {
      try {
        String response = blockingQueue.poll(10, TimeUnit.SECONDS);
        if (response == null) {
          break;
        }
//        if (response.equals(Constants.END_OF_POSTS)) {
//          blockingQueue.put(response);
//          break;
//        }
        String[] lines = response.split("\n");
        for (String line: lines) {
          String[] parts = line.split(",");
          assert parts.length == 4;
          int latency = Integer.parseInt(parts[2]);
          this.meanLatency.getAndAdd(latency);
          this.maxLatency.updateAndGet(x -> Math.max(x, latency));
          int bucketNum;
          if (latency < this.latencyLowerBarrier) {
            bucketNum = 0;
          } else if (latency >= this.latencyUpperBarrier) {
            bucketNum = this.buckets.length - 1;
          } else {
            bucketNum = (latency - this.latencyLowerBarrier) / this.gap + 1;
          }
          synchronized (this.bucketLocks[bucketNum]) {
            this.buckets[bucketNum].append(line).append("\n");
          }
          this.numResponsesInBuckets[bucketNum].getAndIncrement();
//        System.out.println("bucketNum: " + bucketNum + ", numResponsesInBuckets: " + numResponsesInBuckets[bucketNum].get());
        }
      } catch (InterruptedException ignored) {
      }
    }
  }
}
