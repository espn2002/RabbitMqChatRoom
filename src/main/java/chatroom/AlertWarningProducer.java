package chatroom;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
 
 
public class AlertWarningProducer {
    private static final String EXCHANGE_NAME = "alerts";
    private static final String ROUTING_KEY = "critical.alert";
    public static void main(String[] argv) {
        Connection connection = null;
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
          //  factory.setUsername("admin");
          //  factory.setPassword("admin");
          //  factory.setPort(5672);
 
            connection = factory.newConnection();
            channel = connection.createChannel();
 
            channel.exchangeDeclare(EXCHANGE_NAME, "topic",true);
 
            String message = "critical content！！！";
 
            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.getBytes());
            System.out.println(" [x] Sent '" + ROUTING_KEY + "':'" + message + "'");
 
        }
        catch  (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
                try {
                    connection.close();
                }
                catch (Exception ignore) {}
            }
        }
    }
}