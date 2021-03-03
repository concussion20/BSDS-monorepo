package util;

import constants.Constants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {
  public static List<Integer> readLatenciesFromCSV(int bucketNum) throws IOException {
    String line = "";
    String splitBy = ",";
    List<Integer> latencies = new ArrayList<>();

    BufferedReader br = new BufferedReader(new FileReader(Constants.OUTPUT_PATH
        + "POSTs_" + bucketNum + ".csv"));
    while ((line = br.readLine()) != null) {
      String[] record = line.split(splitBy);    // use comma as separator
      latencies.add(Integer.parseInt(record[2]));
    }
    br.close();
    return latencies;
  }
}
