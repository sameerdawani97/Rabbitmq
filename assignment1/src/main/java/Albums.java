//import com.amazonaws.AmazonClientException;
//import com.amazonaws.AmazonServiceException;
//import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
//import com.amazonaws.services.dynamodbv2.model.AttributeValue;
//import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
//import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@WebServlet("/albums/")
public class Albums extends javax.servlet.http.HttpServlet {
  // Assuming 'albums' is a list of albums you want to access
  private List<Album> albums;
  Album album = new Album("Sex Pistols","Never Mind The Bollocks!","1977");

  private DynamoDBAccess dynamoDBAccess;

  @Override
  public void init() throws ServletException {
    super.init();
    // Initialize the DynamoDB client
    //dynamoDBAccess = DynamoDBAccess.getInstance();
    dynamoDBAccess = new DynamoDBAccess();

  }
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String urlPath = request.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("missing parameters");
      return;
    }

    String[] urlParts = urlPath.split("/");

    String albumId = urlParts[1]; // Assuming you want to check the fourth part
    try {
      Map<String, AttributeValue> result = dynamoDBAccess.getItemFromDynamoDB(albumId);
      //System.out.println(result);
      if (!result.isEmpty()) {
        String artist = result.get("artist").s();
        String title = result.get("title").s();
        String year = result.get("year").s();
        System.out.println("got the result");
        //String employeeJsonString = new Gson().toJson(album);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print("{\"artist\": \"" + artist + "\", \"Title\": \"" + title + "\", \"year\": \"" + year + "\"}");
        out.flush();
      } else {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (NumberFormatException e) {
      // Handle invalid request
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().print("{\"message\": \"Invalid Request\"}");
    } catch (Exception e) {
      // Handle other unexpected exceptions
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().print("{\"message\": \"An unexpected error occurred\"}");
      response.getWriter().print(e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    System.out.println("sameer");
    if (ServletFileUpload.isMultipartContent(request)) {
      try{
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = upload.parseRequest(request);

        String albumId = null;
        String artist = null;
        String title = null;
        String year = null;
        byte[] imageData = null;
        long imageSize = 0;

        for (FileItem item : items) {
          if (item.isFormField()) {
            JSONObject profileJsonObject = new JSONObject(item.getString());

            // Now you can access values in the JSON object
            artist = profileJsonObject.getString("artist");
            title = profileJsonObject.getString("title");
            year = profileJsonObject.getString("year");

          } else {
            // Handle uploaded files (e.g., image)
            if ("Image".equals(item.getFieldName())) {
              // Process or save the uploaded image file as needed
              imageSize = item.getSize();
              imageData = item.get();
              // For simplicity, let's calculate and return the size of the image.
            }
          }
        }

        if (imageSize!=0){

          albumId = UUID.randomUUID().toString();
          //albumID = "101-w3z-101123";
          //dynamoDBAccess.putItemWithImageToDynamoDB(albumId, imageData, artist, title, year);
          dynamoDBAccess.putItemWithImageToDynamoDB(albumId, imageData, "sam", "daw", "1997");
          response.setStatus(HttpServletResponse.SC_OK);
          response.setContentType("application/json");
          response.getWriter().write(
              "{\"albumId\": \"" + albumId + "\", \"ImageSize\": \""
                  + String.valueOf(imageSize) + "\"}");
        }
        else {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          response.getWriter().write("{\"error\": \"Invalid request\"}");
        }

      } catch (Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("{\"error\": \"Internal Server Error\"}");
        // Handle DynamoDB-specific exceptions
        e.printStackTrace();

      }
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"error\": \"Invalid request\"}");
    }
  }
}

