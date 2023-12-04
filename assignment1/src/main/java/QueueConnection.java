import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
public class QueueConnection {
  private ConnectionFactory factory;
  private Connection connection;

  public QueueConnection() throws ServletException {
    // RabbitMQ server connection details
    String rabbitmqHost = "localhost";
    int rabbitmqPort = 5672;
    String rabbitmqUsername = "guest";
    String rabbitmqPassword = "guest";

    try {
      // Create connection factory
      factory = new ConnectionFactory();
      factory.setHost(rabbitmqHost);
      factory.setPort(rabbitmqPort);
      factory.setUsername(rabbitmqUsername);
      factory.setPassword(rabbitmqPassword);

      // Create connection and channel
      connection = factory.newConnection();

    } catch (Exception e) {
      throw new ServletException("Error initializing RabbitMQ connection", e);
    }
  }

  public Connection getConnection() {
    return connection;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }


}
