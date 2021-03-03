package multithread;

import constants.Constants;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.PurchaseApi;
import io.swagger.client.model.Purchase;
import io.swagger.client.model.PurchaseItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PurchaseTask implements Runnable {
  private Integer storeID;
  private AtomicInteger numReqs;
  private AtomicInteger numFailed;
  private Integer numCusts;
  private Integer maxItemID;
  private Integer numPurchases;
  private Integer numItems;
  private String date;
  private String baseURL;
  private BlockingQueue<String> blockingQueue;
  private String savedWhenTimeout = "";
  private AtomicBoolean isPhase2Begin;
  private AtomicBoolean isPhase3Begin;

  public PurchaseTask(Properties prop, Integer storeID, AtomicInteger numReqs,
      AtomicInteger numFailed, BlockingQueue<String> blockingQueue,
      AtomicBoolean isPhase2Begin, AtomicBoolean isPhase3Begin) {
    this.storeID = storeID;
    this.numReqs = numReqs;
    this.numFailed = numFailed;
    this.blockingQueue = blockingQueue;
    this.isPhase2Begin = isPhase2Begin;
    this.isPhase3Begin = isPhase3Begin;
    this.numCusts = Integer.parseInt(prop.getProperty(Constants.NUM_CUSTS));
    this.maxItemID = Integer.parseInt(prop.getProperty(Constants.MAX_ITEM_ID));
    this.numPurchases = Integer.parseInt(prop.getProperty(Constants.NUM_PURCHASES));
    this.numItems = Integer.parseInt(prop.getProperty(Constants.NUM_ITEMS));
    this.date = prop.getProperty(Constants.DATE);
    this.baseURL = "http://" + prop.getProperty(Constants.SERVER_IP) + ":"
        + prop.getProperty(Constants.SERVER_PORT) + "/" + Constants.WAR_NAME;
  }

  private void sendPurchase(Purchase body, Integer storeID, Integer custID, String date)
      throws InterruptedException {
    Long start = null, end = null;
    int responseCode = 0;

    try {
      this.numReqs.getAndIncrement();

      PurchaseApi apiInstance = new PurchaseApi();
      ApiClient apiClient = apiInstance.getApiClient();
      apiClient.setBasePath(this.baseURL);
      start = System.currentTimeMillis();
      ApiResponse response = apiInstance.newPurchaseWithHttpInfo(body, storeID, custID, date);
      responseCode = response.getStatusCode();
//      System.out.println("Response code is: " + responseCode);
    } catch (ApiException | IllegalArgumentException e) {
      System.err.println("Exception when calling PurchaseApi#newPurchase");
      System.err.println(e.getMessage());
      if (e instanceof ApiException) {
        responseCode = ((ApiException)e).getCode();
        System.err.println(responseCode);
        System.err.println(((ApiException)e).getResponseBody());
      }
      this.numFailed.getAndIncrement();
    }
    end = System.currentTimeMillis();
    assert start != null;
//    System.out.println("This POST request has latency value: " + (end - start));

    String record = start + "," + "POST," + (end - start) + "," + responseCode + "\n";
    boolean flag = this.blockingQueue.offer(record, 2, TimeUnit.SECONDS);
    if (!flag) {
      this.savedWhenTimeout += record;
    } else if (!this.savedWhenTimeout.isEmpty()) {
      flag = this.blockingQueue.offer(this.savedWhenTimeout, 2, TimeUnit.SECONDS);
      if (flag) {
        this.savedWhenTimeout = "";
      }
    }
  }

  @Override
  public void run() {
    for (int i = 0; i < Constants.OPEN_HOURS * this.numPurchases; i++) {
      if (!this.isPhase3Begin.get() && i >= Constants.PHASE3 * this.numPurchases) {
        this.isPhase3Begin.set(true);
      } else if (!this.isPhase2Begin.get() && i >= Constants.PHASE2 * this.numPurchases) {
        this.isPhase2Begin.set(true);
      }
      final ThreadLocalRandom random = ThreadLocalRandom.current();
      Integer custID = random.nextInt(this.storeID * Constants.MAX_NUM_CUSTS,
          this.storeID * Constants.MAX_NUM_CUSTS + this.numCusts);
      List<PurchaseItems> itemsList = new ArrayList<>();
      for (int j = 0; j < this.numItems; j++) {
        Integer itemID = random.nextInt(1, this.maxItemID + 1);
        PurchaseItems purchaseItems = new PurchaseItems();
        purchaseItems.setItemID(String.valueOf(itemID));
        purchaseItems.setNumberOfItems(1);
        itemsList.add(purchaseItems);
      }
      Purchase body = new Purchase();
      body.setItems(itemsList);
      try {
        sendPurchase(body, this.storeID, custID, this.date);
      } catch (InterruptedException ignored) {
      }
    }
    if (!this.savedWhenTimeout.isEmpty()) {
      try {
        this.blockingQueue.put(this.savedWhenTimeout);
      } catch (InterruptedException ignored) {
      }
    }
  }
}
