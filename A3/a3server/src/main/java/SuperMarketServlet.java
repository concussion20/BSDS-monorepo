import channelpool.ChannelFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Purchase;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@WebServlet(name = "SuperMarketServlet")
public class SuperMarketServlet extends HttpServlet {
  private Connection connection;
  private ObjectPool<Channel> pool;
  private static final String TASK_QUEUE_NAME = "purchase_queue";

  public void init() throws ServletException {
    // Initialization code...
    Properties prop = null;
    try {
      try (InputStream input = SuperMarketServlet.class.getClassLoader().getResourceAsStream("config.properties")) {
        if (input == null) {
          System.out.println("Sorry, unable to find config.properties");
        }
        //load a properties file from class path, inside static method
        prop = new Properties();
        prop.load(input);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(prop.getProperty("rabbitmqHost"));
    factory.setUsername(prop.getProperty("rabbitmqUser"));
    factory.setPassword(prop.getProperty("rabbitmqPassword"));
    try {
      this.connection = factory.newConnection();
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }

    GenericObjectPoolConfig<Channel> config = new GenericObjectPoolConfig<>();
    config.setMaxIdle(10);
    config.setMaxTotal(200);
    config.setTestOnBorrow(true);
    config.setTestOnReturn(true);
    this.pool = new GenericObjectPool<>(new ChannelFactory(this.connection, TASK_QUEUE_NAME), config);
  }

  private boolean isPostUrlValid(String[] urlPath) {
    // TODO: validate the request url path according to the API spec
    // urlPath  = "/1/customer/2/date/20200101"
    // urlParts = [, 1, customer, 2, date, 20200101]
    if (urlPath.length != 6
        || !urlPath[1].matches("\\d+")
        || !urlPath[3].matches("\\d+")
        || !urlPath[5].matches("\\d+")
        || urlPath[5].length() != 8) {
      return false;
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    dateFormat.setLenient(false);
    try {
      dateFormat.parse(urlPath[5]);
    } catch (ParseException pe) {
      return false;
    }
    return true;
  }

  private void producePurchaseMsg(String msg) throws Exception {
    Channel channel = this.pool.borrowObject();
    channel.basicPublish("", TASK_QUEUE_NAME,
        null,
        msg.getBytes("UTF-8"));
    this.pool.returnObject(channel);
  }

  protected void doPost(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    String urlPath = request.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"missing paramterers.\"");
      return;
    }

    String[] urlParts = urlPath.split("/");
    // and now validate url path and return the response status code
    // (and maybe also some value if input is valid)

    if (!isPostUrlValid(urlParts)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"paramterers have wrong formats.\"");
    } else {
      // Read from request
      StringBuilder buffer = new StringBuilder();
      BufferedReader reader = request.getReader();
      String line;
      while ((line = reader.readLine()) != null) {
        buffer.append(line);
        buffer.append(System.lineSeparator());
      }
      String data = buffer.toString();
      Gson gson = new GsonBuilder().create();
      Purchase purchase = gson.fromJson(data, Purchase.class);
      assert purchase != null;

      try {
        producePurchaseMsg(data + "/" + urlParts[1] + "/" + urlParts[3] + "/" + urlParts[5]);
      } catch (Exception e) {
        e.printStackTrace();
      }

      response.setStatus(HttpServletResponse.SC_CREATED);
      response.getWriter().write("{\"message\": \"It works!\"}");
    }
  }

  private boolean isGetUrlValid(String[] urlPath) {
    return urlPath.length == 3
        && (urlPath[1].equals("store") || urlPath[1].equals("top10"))
        && urlPath[2].matches("\\d+");
  }

  private String RPCClientCall(String storeApiType, String idStr)
      throws IOException, InterruptedException {
    final String corrId = UUID.randomUUID().toString();

    Channel channel = this.connection.createChannel();
    String replyQueueName = channel.queueDeclare().getQueue();
    AMQP.BasicProperties props = new AMQP.BasicProperties
        .Builder()
        .correlationId(corrId)
        .replyTo(replyQueueName)
        .build();

    channel.basicPublish("",
        storeApiType.equals("store")?"rpc_queue_top_10_items":"rpc_queue_top_10_stores",
        props, idStr.getBytes("UTF-8"));

    final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

    String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
      if (delivery.getProperties().getCorrelationId().equals(corrId)) {
        response.offer(new String(delivery.getBody(), "UTF-8"));
      }
    }, consumerTag -> {
    });

    String result = response.take();
    channel.basicCancel(ctag);
    return result;
  }

  protected void doGet(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    String urlPath = request.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"missing paramterers.\"");
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isGetUrlValid(urlParts)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"paramterers have wrong formats.\"");
    } else {
      String jsonString = "";
      try {
        jsonString = RPCClientCall(urlParts[1], urlParts[2]);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(jsonString);
    }
  }

  public void destroy() {
    // Finalization code...
    try {
      this.connection.close();
      this.pool.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
