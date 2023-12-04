import com.rabbitmq.client.Connection;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import com.rabbitmq.client.Channel;

@WebServlet("/review/")
public class AlbumReview extends HttpServlet {
  private QueueConnection queueConnection;
  private Connection connection;
  private String QueueName = "RabbitMqReview";

  private Channel channel;
  private static int NUM_CONSUMERS = 20;

  public void init() throws ServletException {
    super.init();
    queueConnection = new QueueConnection();
    connection = queueConnection.getConnection();
    try {
      channel = connection.createChannel();
      channel.queueDeclare(QueueName, false, false, false, null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    initializeConsumers();
  }

  private void initializeConsumers(){
    for (int i = 0; i < NUM_CONSUMERS; i++){
      Consumer consumer = new Consumer(connection, QueueName);
      consumer.start();
    }
  }


  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {

      String id = request.getParameter("albumId");
      String status = request.getParameter("status");
      String message = id + ":" + status;

      System.out.println("Sent: '" + message + "'");

//      channel.queueDeclare(QueueName, false, false, false, null);
      channel.basicPublish("", QueueName, null, message.getBytes());

      //channel.close();

      response.setStatus(HttpServletResponse.SC_OK);
      //System.out.println("Sent '" + message + "'");
    } catch (Exception e) {
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error publishing message to RabbitMQ");
    }


    response.getWriter().write("Message published to RabbitMQ queue!");

  }
}