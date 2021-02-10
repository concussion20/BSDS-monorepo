package multithread;

import constants.Constants;
import java.io.FileWriter;
import java.io.IOException;

public class CSVWriter implements Runnable {
  private StringBuffer[] buckets;
  private final Object[] bucketLocks;

  public CSVWriter(StringBuffer[] buckets, Object[] bucketLocks) {
    this.buckets = buckets;
    this.bucketLocks = bucketLocks;
  }

  private void writeCSV() throws IOException {
    for (int i = 0; i < this.buckets.length; i++) {
      String lines = "";
      synchronized (this.bucketLocks[i]) {
        lines = this.buckets[i].toString();
        this.buckets[i].setLength(0);
      }
      if (lines.isEmpty()) continue;
      FileWriter csvWriter = new FileWriter( Constants.OUTPUT_PATH + "POSTs_" + (i + 1) + ".csv"
          , true);
      csvWriter.append(lines);
      csvWriter.flush();
      csvWriter.close();
    }
  }

  @Override
  public void run() {
    try {
      writeCSV();
    } catch (IOException ignored) {
    }
  }
}
