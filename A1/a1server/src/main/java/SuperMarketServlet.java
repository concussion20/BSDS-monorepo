import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Purchase;

@WebServlet(name = "SuperMarketServlet")
public class SuperMarketServlet extends HttpServlet {

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
      response.setStatus(HttpServletResponse.SC_CREATED);
      // do any sophisticated processing with urlParts which contains all the url params
      // TODO: process url params in `urlParts`
      response.getWriter().write("It works!\n" + data);
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
}
