
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

public class DynamoDBAccess {

  private static final String tableName = "Album";
  private static final ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
  private static final Region region = Region.US_WEST_2;
  private DynamoDbClient ddb;
//  private static final ThreadLocal<DynamoDBAccess> threadLocalInstance = ThreadLocal.withInitial(() -> {
//    DynamoDBAccess instance = new DynamoDBAccess();
//    instance.initializeDynamoDBClient();
//    return instance;
//  });


  public DynamoDBAccess() {
    ddb = DynamoDbClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(region)
        .build();
    //ddb = null; // Initialized in the thread-local initializer
  }
  private void initializeDynamoDBClient() {
    // Use the thread-specific configuration to initialize the DynamoDB client
    ddb = DynamoDbClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(region)
        .build();
  }

//  public static DynamoDBAccess getInstance() {
//    return threadLocalInstance.get();
//  }

  public DynamoDbClient getDynamoDbClient() {
    return ddb;
  }


//ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();

//  private static final DynamoDbClient ddb = DynamoDbClient.builder()
//      //.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
//      .credentialsProvider(credentialsProvider)
//      .region(region)
//      .build();


//  public static DynamoDBAccess getDynamoDbClient() {
//    return threadLocalDdb.get();
//  }
//  public DynamoDBAccess() {
//
//  }
  public void putItemWithImageToDynamoDB(String primaryKeyValue, byte[] imageData, String artist, String title, String year) {

    long expirationTimestamp = System.currentTimeMillis() / 1000 + 3600; // 1 hour from now
    HashMap<String, AttributeValue> itemValues = new HashMap<>();
    itemValues.put("albumId", AttributeValue.builder().s(primaryKeyValue).build());
    itemValues.put("artist", AttributeValue.builder().s(artist).build());
    itemValues.put("title", AttributeValue.builder().s(title).build());
    itemValues.put("year", AttributeValue.builder().s(year).build());
    itemValues.put("imageData", AttributeValue.builder().b(SdkBytes.fromByteArray(imageData)).build());
    itemValues.put("ttl", AttributeValue.builder().n(String.valueOf(expirationTimestamp)).build());

    PutItemRequest request = PutItemRequest.builder()
        .tableName(tableName)
        .item(itemValues)
        .build();

      try {
        PutItemResponse response = ddb.putItem(request);

      } catch (ResourceNotFoundException e) {
        System.err.format("Error: The Amazon DynamoDB table \"%s\" can't be found.\n", tableName);
        System.err.println("Be sure that it exists and that you've typed its name correctly!");
        System.exit(1);
      } catch (DynamoDbException e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }
  }

  public Map<String,AttributeValue> getItemFromDynamoDB(String key) {
    HashMap<String,AttributeValue> keyToGet = new HashMap<>();
    keyToGet.put("albumId", AttributeValue.builder()
        .s(key)
        .build());

    GetItemRequest request = GetItemRequest.builder()
        .key(keyToGet)
        .tableName(tableName)
        .build();

    try {

      Map<String,AttributeValue> returnedItem = ddb.getItem(request).item();
      if (!returnedItem.isEmpty()){
        return returnedItem;
      } else {
        return Collections.emptyMap();
      }

    } catch (DynamoDbException e) {
      //System.err.println(e.getMessage());
      return Collections.emptyMap();
    }
  }

  public void updateLikesAndDislikes(String albumId, String likeStatus) {
    String tableNameLikes = "likeStatus";

    boolean isLike;
    if (likeStatus.equals("like")){
      isLike = true;
    }
    else{
      isLike = false;
    }

    // Define the primary key and attribute values for the update
    HashMap<String, AttributeValue> keyValues = new HashMap<>();
    keyValues.put("albumId", AttributeValue.builder().s(albumId).build());

//     Create an UpdateItemRequest
    UpdateItemRequest updateRequest = UpdateItemRequest.builder()
        .tableName(tableNameLikes)
        .key(keyValues)
        .updateExpression(isLike ? "ADD #like :incr" : "ADD #dislike :incr")
        .expressionAttributeNames(isLike ?
            Collections.singletonMap("#like", "like") :
            Collections.singletonMap("#dislike", "dislike"))
        .expressionAttributeValues(Collections.singletonMap(":incr", AttributeValue.builder().n("1").build())
        )
        .build();

    try {
      UpdateItemResponse updateResponse = ddb.updateItem(updateRequest);
      System.out.println("Update successful: " + updateResponse);
    } catch (DynamoDbException e) {
      System.err.println(e.getMessage());
    }
  }


}