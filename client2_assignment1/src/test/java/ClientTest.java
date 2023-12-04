import java.io.IOException;
import javax.imageio.IIOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientTest {
  @Test
  public void testWithJava() throws IOException {
    // Simulate command-line arguments by creating an args array
//    String[] args = {"arg1", "arg2", "arg3"};
    String[] args = {"10","10","2","http://ec2-user@ec2-35-92-120-143.us-west-2.compute.amazonaws.com:8080/assignment1_war/albums/", "http://ec2-user@ec2-35-92-120-143.us-west-2.compute.amazonaws.com:8080/assignment1_war/review/"};

    // Call the main method with the simulated arguments
    ClientConcurrent.main(args);

    // Add assertions to verify the expected behavior
    // For example, check the output or state of your application
  }

  @Test
  public void testWithGo() throws IOException {
    // Simulate command-line arguments by creating an args array
//    String[] args = {"arg1", "arg2", "arg3"};
    String[] args = {"10","10","2","http://localhost:8080/assignment1_war_exploded/albums/", "http://localhost:8080/assignment1_war_exploded/review/"};

    // Call the main method with the simulated arguments
    ClientConcurrent.main(args);

    // Add assertions to verify the expected behavior
    // For example, check the output or state of your application
  }


}
