import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dynamo.Purchaser;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Purchase;

@WebServlet(name = "SuperMarketServlet")
public class SuperMarketServlet extends HttpServlet {
  private AmazonDynamoDB[] clients = new AmazonDynamoDB[25];

  public void init() throws ServletException {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setMaxConnections(400);

    AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard()
        .withCredentials(new InstanceProfileCredentialsProvider(false))
        .withClientConfiguration(clientConfiguration);
//        .withExecutorFactory(() -> Executors.newFixedThreadPool(200));

    for (int i = 0; i < this.clients.length; i++) {
      this.clients[i] = builder.build();
    }
  }

  protected void doPost(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    String urlPath = request.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("missing paramterers");
      return;
    }

    String[] urlParts = urlPath.split("/");
    // and now validate url path and return the response status code
    // (and maybe also some value if input is valid)

    if (!isUrlValid(urlParts)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("paramterers have wrong formats");
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

      // insert into database
      UUID uuid = UUID.randomUUID();
      HashMap<String, String> valuesMap = new HashMap<>();
      valuesMap.put("Items", data);
      valuesMap.put("StoreID", urlParts[1]);
      valuesMap.put("CustomerID", urlParts[3]);
      valuesMap.put("Date", urlParts[5]);

      AmazonDynamoDB client = this.clients[ThreadLocalRandom.current().nextInt(this.clients.length)];
      boolean flag = Purchaser.addPurchase(client,"Purchases", uuid.toString(), valuesMap);

      if (flag) {
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.getWriter().write("{\"message\": \"It works!\"}");
      } else {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("{\"message\": \"Error: Can't save to databse!\"}");
      }
    }
  }

  protected void doGet(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {

  }

  private boolean isUrlValid(String[] urlPath) {
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

  public void destroy() {
    // Finalization code...
    for (int i = 0; i < this.clients.length; i++) {
      this.clients[i].shutdown();
      this.clients[i] = null;
    }
  }
}
