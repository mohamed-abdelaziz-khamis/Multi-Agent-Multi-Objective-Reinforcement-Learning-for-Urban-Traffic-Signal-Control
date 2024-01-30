package gld.utils;

public class DoubleUtils {
	/** EJUST: truncate a double to an arbitrary number of decimal places
	 * @author Syed Mustaffa*/
	public static double truncateDouble(double number, int numDigits) {
	    double result = number;
	    String arg = "" + number;
	    
	    int idx = arg.indexOf('.');
	    if (idx!=-1) {
	        if (arg.length() > idx + numDigits) {
	            arg = arg.substring(0, idx + numDigits + 1);
	            result  = Double.parseDouble(arg);
	        }
	    }
	    return result ;
	}
}
