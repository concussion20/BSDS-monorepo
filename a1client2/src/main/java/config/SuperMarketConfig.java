package config;

import constants.Constants;
import execption.ArgsException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;

public class SuperMarketConfig {

  private static boolean checkArgsValid(Properties prop) throws ArgsException {
    String maxStores = prop.getProperty(Constants.MAX_STORES);
    if (!maxStores.matches("\\d+")) {
      throw new ArgsException("Wrong format of property \"maxStores\"");
    }
    String numCusts = prop.getProperty(Constants.NUM_CUSTS);
    if (!numCusts.matches("\\d+")) {
      throw new ArgsException("Wrong format of property \"numCusts\"");
    }
    String maxItemID = prop.getProperty(Constants.MAX_ITEM_ID);
    if (!maxItemID.matches("\\d+")) {
      throw new ArgsException("Wrong format of property \"maxItemID\"");
    }
    String numPurchases = prop.getProperty(Constants.NUM_PURCHASES);
    if (!numPurchases.matches("\\d+")) {
      throw new ArgsException("Wrong format of property \"numPurchases\"");
    }
    String numItems = prop.getProperty(Constants.NUM_ITEMS);
    if (!numItems.matches("\\d+")
        || Integer.parseInt(numItems) < Constants.NUM_ITEMS_LOWER_BOUND
        || Integer.parseInt(numItems) > Constants.NUM_ITEMS_UPPER_BOUND) {
      throw new ArgsException("Wrong format of property \"numItems\"");
    }
    String date = prop.getProperty(Constants.DATE);
    if (date.length() != 8 || !date.matches("\\d+")) {
      throw new ArgsException("Wrong format of property \"date\"");
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    dateFormat.setLenient(false);
    try {
      dateFormat.parse(date);
    } catch (ParseException pe) {
      throw new ArgsException("Wrong format of property \"date\"");
    }
    String ip = prop.getProperty(Constants.SERVER_IP);
    String port = prop.getProperty(Constants.SERVER_PORT);
    if (!ip.matches("\\d+[.]\\d+[.]\\d+[.]\\d+") || !port.matches("\\d+")) {
      throw new ArgsException("Wrong format of property \"serverIP\" or \"serverPort\"");
    }
    return true;
  }

  public static Properties loadConfig() throws IOException, ArgsException {
    try (InputStream input = SuperMarketConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        System.out.println("Sorry, unable to find config.properties");
        return null;
      }
      //load a properties file from class path, inside static method
      Properties prop = new Properties();
      prop.load(input);
      assert checkArgsValid(prop);
      return prop;
    } catch (Exception ex) {
      throw ex;
    }
  }

}
