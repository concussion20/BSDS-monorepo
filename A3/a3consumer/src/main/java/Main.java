import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import consumer.RPCServer;
import dbcp.DBCPDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import consumer.Consumer;

public class Main {

  public static void main(String[] argv) throws Exception {
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

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(prop.getProperty("rabbitmqHost"));
    factory.setUsername(prop.getProperty("rabbitmqUser"));
    factory.setPassword(prop.getProperty("rabbitmqPassword"));
    final Connection connection = factory.newConnection();

    for (int i = 0; i < 100; i++) {
      Thread t = new Thread(new Consumer(connection));
      t.start();
    }

    new Thread(new RPCServer(connection)).start();

  }

}
