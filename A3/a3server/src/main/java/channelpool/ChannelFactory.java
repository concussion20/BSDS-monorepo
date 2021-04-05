package channelpool;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class ChannelFactory extends BasePooledObjectFactory<Channel> {
  private Connection connection;
  private String queueName;

  public ChannelFactory(Connection connection, String queueName) {
    this.connection = connection;
    this.queueName = queueName;
  }

  @Override
  public Channel create() throws Exception {
    Channel channel = this.connection.createChannel();
    channel.queueDeclare(this.queueName, false, false, false, null);
    return channel;
  }

  @Override
  public PooledObject<Channel> wrap(Channel channel) {
    return new DefaultPooledObject<>(channel);
  }

  @Override
  public void destroyObject(PooledObject<Channel> p) throws Exception {
    p.getObject().close();
  }
}
