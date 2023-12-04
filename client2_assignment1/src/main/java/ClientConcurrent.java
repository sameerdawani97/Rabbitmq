import static java.util.Collections.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Main client class to run client and calculate statistics and through put of program.
 */
public class ClientConcurrent {
  private static final int INITIAL_THREADS_SIZE = 10;
  private static final int NUM_REQUESTS_PER_THREAD_FOR_INITIAL = 10;
  private static final int NUM_REQUESTS_PER_THREAD_FOR_GROUP = 100;
  public static HttpClient httpClient = HttpClient.newHttpClient();

  private static double getMean = 0.0;
  private static double postMean = 0.0;
  private static double postMedian = 0.0;
  private static double getMedian = 0.0;
  private static AtomicInteger successfulRequestsGet = new AtomicInteger(0);
  private static AtomicInteger failedRequestsGet = new AtomicInteger(0);
  private static AtomicInteger successfulRequestsPost = new AtomicInteger(0);
  private static AtomicInteger failedRequestsPost = new AtomicInteger(0);


  /**
   *
   * @param args arguments for thread groups and their size, delays in seconds and ip address.
   * @throws IOException IOException
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 5) {
      System.err.println("Four arguments should be passed: ");
      System.err.println("1) Size of threads in group 2) number of thread groups 3) delay time in second 4) Server address 5) server address for review");
      System.exit(1);
    }


    int threadGroupSize = Integer.parseInt(args[0]);
    int numOfGroups = Integer.parseInt(args[1]);
    int delayInSeconds = Integer.parseInt(args[2]);
    String ipAddress = args[3];
    String ipAddressReview = args[4];

    //final int serverOption = Integer.parseInt(args[4]);
    final int albumId = 1;

    HttpRequest requestGet;

    File imageFile = new File("src/main/resources/testImage.png");
    byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
    String boundary = "Boundary-" + System.currentTimeMillis();

    String CRLF = "\r\n";
    String requestBody = "--" + boundary + CRLF +
        "Content-Disposition: form-data; name=\"profile\"" + CRLF +
        "Content-Type: application/json" + CRLF +
        CRLF +
        "{\"artist\":\"Sex Pistols\",\"title\":\"Never Mind The Bollocks!\",\"year\":\"1977\"}" + CRLF +
        "--" + boundary + CRLF +
        "Content-Disposition: form-data; name=\"Image\"; filename=\"testImage.png\"" + CRLF +
        "Content-Type: application/octet-stream" + CRLF +
        CRLF;

    // Add the image bytes directly to the request body without converting to a string
    byte[] boundaryBytes = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);
    byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

    // Construct the full request body as a byte array
    byte[] fullRequestBody = new byte[requestBodyBytes.length + imageBytes.length + boundaryBytes.length];
    System.arraycopy(requestBodyBytes, 0, fullRequestBody, 0, requestBodyBytes.length);
    System.arraycopy(imageBytes, 0, fullRequestBody, requestBodyBytes.length, imageBytes.length);
    System.arraycopy(boundaryBytes, 0, fullRequestBody, requestBodyBytes.length + imageBytes.length, boundaryBytes.length);

    HttpRequest requestPost = HttpRequest.newBuilder()
        .uri(URI.create(ipAddress))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofByteArray(fullRequestBody))
        .build();

//    requestBody += new String(imageBytes, StandardCharsets.UTF_8) + CRLF;
//    requestBody += "--" + boundary + "--" + CRLF;
//
//    HttpRequest requestPost = HttpRequest.newBuilder()
//        .uri(URI.create(ipAddress))
//        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
//        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
//        .build();


    URI baseUri = URI.create(ipAddress);
    requestGet = HttpRequest.newBuilder()
        .uri(baseUri)
        .GET()
        .build();

    URI baseUriReview = URI.create(ipAddressReview);
    HttpRequest.Builder requestReview = HttpRequest.newBuilder()
        .uri(baseUriReview)
        .header("Content-Type", "application/x-www-form-urlencoded");


    // Create an HttpClient
    long startTime, endTime;
    System.out.println("Threads are running...");
    Thread[] initialThreads = new Thread[INITIAL_THREADS_SIZE];
    List<Thread> arrayList = new ArrayList<>();
    List<Thread> groupThreads = Collections.synchronizedList(arrayList);
    //List<RequestInfo> csvRecord = new ArrayList<>();
    ConcurrentLinkedQueue<RequestInfo> csvRecord = new ConcurrentLinkedQueue<>();
    //List<RequestInfo> csvRecord = Collections.synchronizedList(arrayList1 );

    /**
     * Initial threads.
     */
    for (int i = 0; i < INITIAL_THREADS_SIZE; i++) {
      initialThreads[i] = new Thread(() -> callRequests(baseUri, NUM_REQUESTS_PER_THREAD_FOR_INITIAL, requestGet, requestPost, csvRecord, ipAddressReview, requestReview));
      initialThreads[i].start();
    }

    /**
     * All initial threads to be joined together before proceeding main threads requests.
     */
    for (Thread thread : initialThreads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // getting the start time of our main threads.
    startTime = System.currentTimeMillis();

    for (int i = 0; i < numOfGroups; i++) {

      for (int j = 0; j < threadGroupSize; j++) {
        Thread thread = new Thread(() -> callRequests(baseUri, NUM_REQUESTS_PER_THREAD_FOR_GROUP, requestGet, requestPost, csvRecord, ipAddressReview, requestReview));
        thread.start();
        groupThreads.add(thread);
      }

      // delay function for each thread group
      try {
        Thread.sleep(delayInSeconds * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    for (Thread thread : groupThreads){
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // end time taken for calculating wall time and through put
    endTime = System.currentTimeMillis();
    long wallTime = (endTime - startTime)/2000;
    long throughput = 4*numOfGroups*threadGroupSize*NUM_REQUESTS_PER_THREAD_FOR_GROUP / wallTime;
    System.out.println("Wall Time: " + wallTime + " seconds");
    System.out.println("Throughput: " + throughput + "/sec");

    List<Long> valuesGet = new ArrayList<>();
    List<Long> valuesPost = new ArrayList<>();
    calculateMean(csvRecord, valuesGet, valuesPost);
    //System.out.println("Mean Latency Get: " + getMean + " milliseconds");
    System.out.println("Mean Latency Post: " + postMean + " milliseconds");
    // Sort the list in ascending order
    Collections.sort(valuesGet);
    Collections.sort(valuesPost);
    //System.out.println("Median Latency Get: " + calculateMedian(valuesGet) + " milliseconds");
    System.out.println("Median Latency Post: " + calculateMedian(valuesPost) + " milliseconds");
    //System.out.println("99th percentile Latency Get: " + calculate99thPercentile(valuesGet) + " milliseconds");
    System.out.println("99th percentile Latency Post: " + calculate99thPercentile(valuesPost) + " milliseconds");
    //System.out.println("Maximum Latency Get: " + max(valuesGet) + " milliseconds");
    System.out.println("Maximum Latency Post: " + max(valuesPost) + " milliseconds");
    //System.out.println("Minimum Latency Get: " + min(valuesGet) + " milliseconds");
    System.out.println("Minimum Latency Post: " + min(valuesPost) + " milliseconds");
    //System.out.println("Successful Get requests: " + successfulRequestsGet.get());
    //System.out.println("Failed Get requests: " + failedRequestsGet.get());
    System.out.println("Successful Post requests: " + successfulRequestsPost.get());
    System.out.println("Failed Post requests: " + failedRequestsPost.get());

    writeCsv(csvRecord);

  }


  /**
   * This method is to calculate median value
   * @param values values
   * @return median value as double.
   */
  private static double calculateMedian(List<Long> values){
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("The list of values is empty or null.");
    }

    int size = values.size();
    int middle = size / 2;

    if (size % 2 == 1) {
      // Odd number of values, return the middle value
      return values.get(middle);
    } else {
      // Even number of values, return the average of the two middle values
      double middleValue1 = values.get(middle - 1);
      double middleValue2 = values.get(middle);
      return (middleValue1 + middleValue2) / 2.0;
    }
  }

  /**
   * This method is used to calculate mean value.
   * @param csvRecord csvRecord
   * @param valuesGet values
   * @param valuesPost values
   * @return mean value
   */
  private static void calculateMean(ConcurrentLinkedQueue<RequestInfo> csvRecord, List<Long> valuesGet, List<Long> valuesPost){
    double meanGet = 0.0;
    double meanPost = 0.0;
    int sizeGet = 0;
    int sizePost = 0;
    for (RequestInfo requestInfo : csvRecord){
      if (requestInfo.requestType == "POST"){
        meanPost+=requestInfo.latency;
        sizePost+=1;
        valuesPost.add(requestInfo.latency);
      }
      else{
        meanGet+=requestInfo.latency;
        sizeGet+=1;
        valuesGet.add(requestInfo.latency);
      }
    }
    getMean = meanGet / sizeGet;
    postMean = meanPost / sizePost;
  }

  /**
   * This method is to calculate the 99th percentile.
   * @param values values
   * @return 99th percentile
   */
  private static double calculate99thPercentile(List<Long> values){
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("The list of values is empty or null.");
    }

    int size = values.size();
    double index = 0.99 * (size - 1);

    if (index == Math.floor(index)) {
      // If the index is an integer, return the value at that index
      return values.get((int) index);
    } else {
      // Interpolate between the two nearest values
      int lowerIndex = (int) Math.floor(index);
      int upperIndex = (int) Math.ceil(index);
      double lowerValue = values.get(lowerIndex);
      double upperValue = values.get(upperIndex);
      double fraction = index - lowerIndex;
      return lowerValue + fraction * (upperValue - lowerValue);
    }
  }

  /**
   * Method to write to csv file
   * @param csvRecord csvRecord
   */
  private static void writeCsv(ConcurrentLinkedQueue<RequestInfo> csvRecord){
    String filePath = "responseRecord.csv";

    try (FileWriter writer = new FileWriter(filePath)) {
      // Write header row
      writer.append("StartTime, RequestType, Latency, StatusCode, albumId");
      writer.append("\n");

      for (RequestInfo requestInfo : csvRecord){
        writer.append(requestInfo.startTime + ", " + requestInfo.requestType + ", "
            + requestInfo.latency + ", " + requestInfo.statusCode + ", " + requestInfo.albumId);
        writer.append("\n");
      }

      System.out.println("CSV file written successfully.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * This method is used to call 1000 request of each type.
   * @param baseUri baseUri
   * @param numRequests numRequests
   * @param requestGet requestGet
   * @param requestPost requestPost
   * @param csvRecord csvRecord
   */
  private static void callRequests(URI baseUri, int numRequests, HttpRequest requestGet, HttpRequest requestPost, ConcurrentLinkedQueue<RequestInfo> csvRecord, String reviewURL, HttpRequest.Builder requestReview) {
    //HttpClient httpClient = HttpClient.newHttpClient();
    for (int i = 0; i < numRequests; i++) {
      try {
        // Send a POST request
        String albumId = sendPostRequest(requestPost, csvRecord);
        // Send a GET request
        sendPostRequestReview(csvRecord, albumId, "like", reviewURL, requestReview);
        sendPostRequestReview(csvRecord, albumId, "like", reviewURL, requestReview);
        sendPostRequestReview(csvRecord, albumId, "dislike", reviewURL, requestReview);
        //sendPostRequestReview(csvRecord, albumId, "like", reviewURL);
        //sendGetRequest(baseUri, requestGet, csvRecord, albumId);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  /**
   * This method is used to send post request to api
   * @param csvRecord csvRecord
   * @param albumId albumId
   * @param likeStatus likeStatus
   * @param reviewURL reviewURL
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private static void sendPostRequestReview(ConcurrentLinkedQueue<RequestInfo> csvRecord, String albumId, String likeStatus, String reviewURL, HttpRequest.Builder requestReview)
      throws Exception {
    System.out.println(albumId);
    long startRequestTime, endRequestTime;
    int retryCount = 0;
    int statusCode = 0;
    boolean failed = true;
    HttpResponse<String> responseReview = null;
    startRequestTime = System.currentTimeMillis();
    while (retryCount < 5 && failed){

      HttpRequest request1 = createPostRequest(requestReview, Map.of("albumId", albumId, "status", likeStatus));
      responseReview = httpClient.send(request1, HttpResponse.BodyHandlers.ofString());
      statusCode = responseReview.statusCode();

      if (statusCode == 200 || statusCode ==201){
        failed = false;
      }
      else {
        retryCount+=1;
      }
    }

//    HttpRequest request1 = createPostRequest(requestReview, Map.of("albumId", albumId, "status", likeStatus));
//    HttpResponse<String> responseReview = httpClient.send(request1, HttpResponse.BodyHandlers.ofString());


//    int statusCode = responseReview.statusCode();

    String responseBody = responseReview.body();

    //System.out.println("Response Status: " + statusCode);
    //System.out.println("Response body: " + responseBody);

    endRequestTime = System.currentTimeMillis();
    long requestLatency = endRequestTime - startRequestTime;
    RequestInfo requestInfo = new RequestInfo(startRequestTime, "POST", requestLatency, statusCode, albumId);
    if (statusCode == 200 || statusCode == 201){
      successfulRequestsPost.incrementAndGet();
    }
    else{
      failedRequestsPost.incrementAndGet();
    }
    csvRecord.add(requestInfo);
  }

  /**
   * This method is used to send post request to api
   * @param requestPost requestPost
   * @param csvRecord csvRecord
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private static String sendPostRequest(HttpRequest requestPost, ConcurrentLinkedQueue<RequestInfo> csvRecord)
      throws IOException, InterruptedException {

    long startRequestTime, endRequestTime;
    int retryCount = 0;
    int statusCode = 0;
    HttpResponse<String> response = null;
    boolean failed = true;
    startRequestTime = System.currentTimeMillis();
    while (retryCount < 5 && failed){

      response = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

      statusCode = response.statusCode();

      if (statusCode == 200 || statusCode ==201){
        failed = false;
      }
      else {
        retryCount+=1;
      }
    }
    //System.out.println("Status Code: " + statusCode);

    // Get and print the response body
    String responseBody = response.body();
    String albumId = "";
    JSONObject jsonObject;
    if (statusCode == 200 || statusCode ==201){
      jsonObject = new JSONObject(responseBody);
      albumId = jsonObject.getString("albumId");
      //System.out.println(jsonObject.getString("albumId"));
    }

    //System.out.println("Response Body:\n" + responseBody);

    endRequestTime = System.currentTimeMillis();
    long requestLatency = endRequestTime - startRequestTime;
    RequestInfo requestInfo = new RequestInfo(startRequestTime, "POST", requestLatency, statusCode, albumId);
    if (statusCode == 200 || statusCode == 201){
      successfulRequestsPost.incrementAndGet();
    }
    else{
      failedRequestsPost.incrementAndGet();
    }
    csvRecord.add(requestInfo);
    return albumId;
  }

  /**
   * This method is used to send get request to api.
   * @param baseUri baseUri
   * @param request request
   * @param csvRecord csvRecord
   * @throws IOException IOException
   */
  private static void sendGetRequest(URI baseUri, HttpRequest request, ConcurrentLinkedQueue<RequestInfo> csvRecord, String albumId) throws IOException {

    try {

      long startRequestTime, endRequestTime;
      int statusCode = 0;
      HttpResponse<String> response = null;
      boolean failed = true;
      int retryCount = 0;

      startRequestTime = System.currentTimeMillis();

      HttpRequest requestGet = request.newBuilder()
          .uri(baseUri.resolve(albumId))
          .build();
      while (retryCount < 5 && failed){

        response = httpClient.send(requestGet, HttpResponse.BodyHandlers.ofString());

        statusCode = response.statusCode();

        if (statusCode == 200 || statusCode ==201){
          failed = false;
        }
        else {
          retryCount+=1;
        }
      }

      //System.out.println("Status Code: " + statusCode);

      // Get and print the response body
      String responseBody = response.body();
      //System.out.println("Response Body:\n" + responseBody);

      endRequestTime = System.currentTimeMillis();
      long requestLatency = endRequestTime - startRequestTime;
      RequestInfo requestInfo = new RequestInfo(startRequestTime, "GET", requestLatency, statusCode, albumId);
      if (statusCode == 200 || statusCode == 201){
        successfulRequestsGet.incrementAndGet();
      }
      else{
        failedRequestsGet.incrementAndGet();
      }
      csvRecord.add(requestInfo);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static HttpRequest createPostRequest(HttpRequest.Builder builder, Map<Object, Object> data) throws Exception {
    // Build the request body
    String requestBody = data.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

    // Build the HttpRequest using the provided builder
    return builder
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();
  }
}
