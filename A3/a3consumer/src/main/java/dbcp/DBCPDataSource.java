package dbcp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DBCPDataSource {
  private static HikariDataSource ds;

  static {
    Properties prop = null;
    try {
      try (InputStream input = DBCPDataSource.class.getClassLoader().getResourceAsStream("config.properties")) {
        if (input == null) {
          System.out.println("Sorry, unable to find config.properties");
          System.exit(1);
        }
        //load a properties file from class path, inside static method
        prop = new Properties();
        prop.load(input);
      }
    } catch (IOException e) {
      System.exit(1);
    }

    // https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-jdbc-url-format.html
    HikariConfig config = new HikariConfig();
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      System.exit(1);
    }

    String HOST_NAME = prop.getProperty("hostName");
    String PORT = prop.getProperty("port");
    String DATABASE = prop.getProperty("database");
    String USERNAME = prop.getProperty("userName");
    String PASSWORD = prop.getProperty("password");

    String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
    config.setJdbcUrl(url);
    config.setUsername(USERNAME);
    config.setPassword(PASSWORD);
    config.setMinimumIdle(10);
    config.setMaximumPoolSize(55);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("maintainTimeStats", "false");

    ds = new HikariDataSource(config);
  }

  public static HikariDataSource getDataSource() {
    return ds;
  }
}
