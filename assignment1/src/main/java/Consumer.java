import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

public class Consumer extends Thread {

  private Connection connection;
  private String queueName;
  private DynamoDBAccess dynamoDBAccess;
  public static final int MAX_RETRY_ATTEMPTS = 5;

  public Consumer(Connection connection, String queueName){
    this.connection = connection;
    this.queueName = queueName;
//    this.dynamoDBAccess = DynamoDBAccess.getInstance();
    this.dynamoDBAccess = new DynamoDBAccess();

  }

  @Override
  public void run() {
    try {
      Channel channel = connection.createChannel();
      System.out.println("welcome to consumer");
      // Declare the queue
      channel.queueDeclare(queueName, false, false, false, null);

      // Set up the consumer
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println("Received message: " + message);
        String[] parts = message.split(":");
        String albumId = parts[0];
        String likeStatus = parts[1];
        //DynamoDbClient dynamoDbClient = DynamoDBAccess.getDynamoDbClient();
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++){
          try{
            dynamoDBAccess.updateLikesAndDislikes(albumId, likeStatus);
            System.out.println("likeStatus inserted successful: " + albumId);
            break;
          }
          catch (DynamoDbException e) {
            if (isTransactionDeadlockException(e) && attempt < MAX_RETRY_ATTEMPTS) {
              System.out.println("Transaction deadlock detected. Retrying...");
            } else {
              System.err.println("Error during DynamoDB insert: " + e.getMessage());
              e.printStackTrace();
              break; // Break out of the retry loop if the exception is not a deadlock or max attempts reached
            }
          }
        }

      };

      // Consume messages from the queue
      //channel.basicQos(1000);
      channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});

      // This will keep the thread alive to continue consuming messages
      while (true){

      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static boolean isTransactionDeadlockException(DynamoDbException e) {
    // Check if the exception is a transaction deadlock
    return e instanceof TransactionCanceledException;
  }

}
