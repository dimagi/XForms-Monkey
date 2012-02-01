package org.dimagi.hammer.util;
import java.util.Random;

public class RandomString
{

  private static final char[] symbols = new char[56];
  public static final int RANDOM_STRING_MAX_LEN=80;
  static {
    for (int idx = 0; idx < 10; ++idx)
      symbols[idx] = (char) ('0' + idx);
    for (int idx = 10; idx < 36; ++idx)
      symbols[idx] = (char) ('a' + idx - 10);
    for (int idx = 36; idx < 56; ++idx)
    	symbols[idx] = (char) ('A' + idx - 36);
  }

  private static Random random = new Random();

  private static char[] buf = null;

//  public RandomString(int length)
//  {
//    
//  }

  /**
   * Generates a random alphanumeric string
   * @param ramdomLength - returns a string of random length (between 1 and 80 chars long inclusive)
   * 						if randomLength = true, length can = anything.
   * @param length - used only if randomLength = false. Specifies the length of the random string
   */
  private static String nextString(boolean randomLength, int length)
  {
	  int len = 1;
	  if(randomLength){
		  len = (int)(random.nextInt(79)+1);
	  }else{
		  if (length < 1)
		      throw new IllegalArgumentException("length < 1: " + length);
		  len = length;
	  }
	  buf = new char[len];
    for (int idx = 0; idx < buf.length; ++idx) 
      buf[idx] = symbols[random.nextInt(symbols.length)];
    return new String(buf);
  }
  
  /**
   * 
   * @return a random length random alpha numeric string
   */
  public static String nextRandomString(){
	  return nextString(true,0);
  }
  
  /**
   * 
   * @param length the length of the random string
   * @return a specified length random alpha numeric string
   */
  public static String nextRandomString(int length){
	  return nextString(false,length);
  }
  

}