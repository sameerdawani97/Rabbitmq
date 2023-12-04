import javax.swing.plaf.metal.MetalBorders.PaletteBorder;

class RequestInfo {
  String requestType;
  int statusCode;
  long startTime;
  long latency;
  String albumId;

  /**
   * This class is to store the properties of response information which is used to create csv file.
   * @param startTime startTime
   * @param requestType requestType
   * @param latency latency
   * @param statusCode statusCode
   */
  public RequestInfo(long startTime, String requestType, long latency, int statusCode, String albumId ){
    this.startTime = startTime;
    this.requestType = requestType;
    this.latency = latency;
    this.statusCode = statusCode;
    this.albumId = albumId;
  }

  /**
   * Get request type
   * @return requestType
   */
  public String getRequestType() {
    return requestType;
  }

  /**
   * Set request type
   * @param requestType requestType
   */
  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  /**
   * Get status code
   * @return statusCode
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * set status code
   * @param statusCode statusCode
   */
  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * get start time
   * @return startTime
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * set start time
   * @param startTime startTime
   */
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  /**
   * get latency
   * @return latency
   */
  public long getLatency() {
    return latency;
  }

  /**
   * set latency
   * @param latency latency
   */
  public void setLatency(long latency) {
    this.latency = latency;
  }

}