public class Album {
  private int ID;
  private String artist;
  private String Title;
  private String year;

  // Default constructor (no-args constructor)
  public Album() {
  }

  // Constructor with parameters
  public Album(int ID, String artist, String Title, String year) {

    this.artist = artist;
    this.Title = Title;
    this.year = year;
    this.ID = ID;
  }

  // Constructor with parameters
  public Album(String artist, String Title, String year) {

    this.artist = artist;
    this.Title = Title;
    this.year = year;
  }

  // Getter and setter methods for each field
  public int getId() {
    return ID;
  }

  public void setID(int ID) {
    this.ID = ID;
  }
  public String getArtist() {
    return artist;
  }

  public void setArtist(String artist) {
    this.artist = artist;
  }

  public String getTitle() {
    return Title;
  }

  public void setTitle(String Title) {
    this.Title = Title;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }
}
