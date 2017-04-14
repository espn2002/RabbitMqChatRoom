package chatroom;

import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
 
 
public class AlertWarningConsumer {
    private  final static  String EMAIL_RECIPIENTS="********@163.com";//接收者邮箱
    private  final static  String EMAIL_SENDER="********@163.com";//发送者邮箱
    private  final static  String EXCHANGE="alerts";//topic类型交换器
    private  final static  String TYPE="topic";
    private  final static  String QUEUE1="critical";
    private  final static  String QUEUE2="rate_limt";
    private  final static  String ROUTING_KEY1="critical.*";
    private  final static  String ROUTING_KEY2="*.rate_limt";
    /**
     *
     * @param recipients    接收人
     * @param subject       发送主题
     * @param msg       发送消息内容
     * @throws IOException
     * @throws MessagingException
     */
    public static void sendEmail(String recipients, String subject, Object msg) throws IOException, MessagingException {
        final Properties props = new Properties();
        /*
         * 可用的属性： mail.store.protocol / mail.transport.protocol / mail.host /
         * mail.user / mail.from
         */
        // 表示SMTP发送邮件，需要进行身份验证
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.163.com");
        // 发件人的账号
        props.put("mail.user", EMAIL_SENDER);
        // 访问SMTP服务时需要提供的密码
        props.put("mail.password", "*******");
 
        // 构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 用户名、密码
                String userName = props.getProperty("mail.user");
                String password = props.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };
        // 使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(props, authenticator);
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);
        // 设置发件人
        InternetAddress form = new InternetAddress(
                props.getProperty("mail.user"));
        message.setFrom(form);
 
        // 设置收件人
        InternetAddress to = new InternetAddress(recipients);
        message.setRecipient(RecipientType.TO, to);
 
        // 设置邮件标题
        message.setSubject(subject);
        // 设置邮件的内容体{"message":"告警消息邮件发送"}
        message.setContent(msg, "application/json;charset=UTF-8");
        // 发送邮件
        Transport.send(message);
    }
    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = null;
 
        try {
 
            factory.setPort(5672);
            //factory.setHost("rabbitmq128");
           // factory.setUsername("admin");
           // factory.setPassword("admin");
            connection = factory.newConnection();
            //创建连接
            final Channel channel = connection.createChannel();
            //声明交换器队列绑定等信息
            channel.exchangeDeclare(EXCHANGE, TYPE, true);
            channel.queueDeclare(QUEUE1, false, false, false, null);
            channel.queueBind(QUEUE1,EXCHANGE,ROUTING_KEY1);
 
            channel.queueDeclare(QUEUE2, false, false, false, null);
            channel.queueBind(QUEUE2,EXCHANGE,ROUTING_KEY2);
 
            Consumer rate_limit_notify = new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String subject = "rate_limit Alert";
                    String msg = new String(body,"UTF-8");
                    try {
                        sendEmail(EMAIL_RECIPIENTS, subject, msg);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                    System.out.println("send alert E-mail!Alert text:Recipients: " + EMAIL_RECIPIENTS+" subject: "+subject);
                    channel.basicAck(envelope.getDeliveryTag(),false);
                }
            };
 
            Consumer critical_notify = new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String subject = "Critical Alert";
                    String msg = new String(body,"UTF-8");
                    try {
                        sendEmail(EMAIL_RECIPIENTS, subject, msg);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                    System.out.println("send alert E-mail!Alert text:Recipients: " + EMAIL_RECIPIENTS+" subject: "+subject);
                    channel.basicAck(envelope.getDeliveryTag(),false);
                }
            };
            //消息消费
            channel.basicConsume(QUEUE1,false,"critical",critical_notify);
            channel.basicConsume(QUEUE2,false,"rate_limit",rate_limit_notify);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
