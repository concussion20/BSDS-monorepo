package consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

public class RPCServer implements Runnable {
  private static final String TOP10_ITEMS = "rpc_queue_top_10_items";
  private static final String TOP10_STORES = "rpc_queue_top_10_stores";
  private Connection connection;

  public RPCServer(Connection connection) {
    this.connection = connection;
  }

  private String convertEntryList2JsonString(List<Entry<String, Integer>> list, String idType) {
    if (list == null) {
      return "{\"stores:\": []}";
    }

    StringBuilder res = new StringBuilder("{\n");
    res.append("\"stores\":[\n");
    for (Entry<String, Integer> entry: list) {
      res.append("{\n");
      // idType is "storeID" or "itemID"
      res.append("\"").append(idType).append("\": ").append(entry.getKey()).append(",\n");
      res.append("\"numOfItems\": ").append(entry.getValue()).append("\n");
      res.append("}\n");
    }
    res.append("]\n}");
    return res.toString();
  }

  @Override
  public void run() {

    try {
      // get the top 10 most purchased items at Store N
      final Channel channel = this.connection.createChannel();
      channel.queueDeclare(TOP10_ITEMS, false, false, false, null);

      channel.basicQos(1);

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
            .Builder()
            .correlationId(delivery.getProperties().getCorrelationId())
            .build();

        String storeId = new String(delivery.getBody(), "UTF-8");
        List<Entry<String, Integer>> top10List = Store.getTop10ItemsInStore(storeId);
        String top10Json = convertEntryList2JsonString(top10List, "itemID");
        channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, top10Json.getBytes("UTF-8"));
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      };

      channel.basicConsume(TOP10_ITEMS, false, deliverCallback, (consumerTag -> { }));

      // get the top 10 stores for sales for item N
      final Channel channel2 = this.connection.createChannel();
      channel2.queueDeclare(TOP10_STORES, false, false, false, null);

      channel2.basicQos(1);

      DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
            .Builder()
            .correlationId(delivery.getProperties().getCorrelationId())
            .build();

        String itemId = new String(delivery.getBody(), "UTF-8");
        List<Entry<String, Integer>> top5List = Store.getTop5StoresForItem(itemId);
        String top5Json = convertEntryList2JsonString(top5List, "storeID");
        channel2.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, top5Json.getBytes("UTF-8"));
        channel2.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      };

      channel2.basicConsume(TOP10_STORES, false, deliverCallback2, (consumerTag -> { }));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
