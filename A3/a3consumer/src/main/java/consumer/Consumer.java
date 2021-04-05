package consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import dao.PurchaseDao;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import model.Purchase;
import model.PurchaseItems;

public class Consumer implements Runnable{
  private static final String SPLITER = "/";
  private static final String TASK_QUEUE_NAME = "purchase_queue";
  private Connection connection;

  public Consumer(Connection connection) {
    this.connection = connection;
  }

  private void save2DB(String msg) {
    PurchaseDao dao = new PurchaseDao();
    String[] args = msg.split(SPLITER);
    // args[0]: purchaseInfo, args[1]: storeId, args[2]: custId, args[3]: date
    dao.createPurchase(args[0], args[1], args[2], args[3]);
  }

  private void save2Map(String msg) {
    // args[0]: purchaseInfo, args[1]: storeId, args[2]: custId, args[3]: date
    String[] args = msg.split(SPLITER);

    Gson gson = new GsonBuilder().create();
    Purchase purchase = gson.fromJson(args[0], Purchase.class);
    assert purchase != null;

    ConcurrentHashMap<String, Integer> itemSaleInStoreN =
        Store.purchasesInStores.getOrDefault(args[1], new ConcurrentHashMap<String, Integer>());

    for (PurchaseItems item: purchase.getItems()) {
      // update map1
      itemSaleInStoreN.put(item.getItemID(), itemSaleInStoreN.getOrDefault(item.getItemID(),0) + 1);

      // update map2
      ConcurrentHashMap<String, Integer> salesInStoresForItemN =
          Store.itemsSale.getOrDefault(item.getItemID(), new ConcurrentHashMap<String, Integer>());
      salesInStoresForItemN.put(args[1], salesInStoresForItemN.getOrDefault(args[1],0) + 1);
      Store.itemsSale.put(item.getItemID(), salesInStoresForItemN);
    }

    // update map1
    Store.purchasesInStores.put(args[1], itemSaleInStoreN);
  }

  @Override
  public void run() {

    try {
      final Channel channel = this.connection.createChannel();
      channel.queueDeclare(TASK_QUEUE_NAME, false, false, false, null);
      // max one message per receiver
      channel.basicQos(1);

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//        System.out.println(
//            "Callback thread ID = " + Thread.currentThread().getId() + " Received '" + message
//                + "'");
        save2DB(message);
        save2Map(message);
      };
      // process messages
      channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> { });

      channel.addShutdownListener(new ShutdownListener() {
        @Override
        public void shutdownCompleted(ShutdownSignalException cause) {
          cause.printStackTrace();
        }
      });

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
