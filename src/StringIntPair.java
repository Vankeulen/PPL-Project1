/*  pair up a string and
    an int,
    used for holes and info
*/

public class StringIntPair {

  public String s;
  public int x;

  public StringIntPair( String sIn, int xIn ) {
     s = sIn;
     x = xIn;
  }

  public String toString() {
     return "[" + s + "," + x + "]";
  }

}
