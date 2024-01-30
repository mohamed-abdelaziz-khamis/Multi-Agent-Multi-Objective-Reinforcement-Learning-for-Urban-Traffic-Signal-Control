package gld.sim.sync;

import java.net.*;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.io.*;

public class SynchronizationProtocol implements SynchronizationProtocolKeys{
   	private String sessionId = null;
	private boolean generateSessionId = false;
   
    /**
	 * @param sessionId
	 */
	public SynchronizationProtocol(String _sessionId) {
		if (_sessionId != null)	sessionId = _sessionId;
		else generateSessionId = true;
		syncInfo = new HashMap();
	}

	private static final int WAITING = 0;
    private static final int SENTSYNC= 1;
    private static final int SENTACK = 2;
    

    

    private int state = WAITING;
    
    private HashMap syncInfo = null;

   
	public SynchronizationProtocol(){
		syncInfo = new HashMap();
	}

    public String processInput(String theInput) {
        
        //we should still add the sessionId usage
        String theOutput = null;
		String inputKey = null;
        if (theInput == null) theInput="";
		StringTokenizer st = new StringTokenizer(theInput," ");
		if (st.hasMoreTokens()) {
			inputKey = st.nextToken();
		}; 

		
        if (state == WAITING) {
            theOutput = "SYNC";
            state = SENTSYNC;
        } else if (state == SENTSYNC) {
            if (inputKey.equalsIgnoreCase(INTERVAL)) {
				String value = null;
				if (st.hasMoreTokens()) {
					value = st.nextToken().trim();
				}
				if (value != null  && !value.equals("") && isNumber(value) ){
            		syncInfo.put(INTERVAL,value);
            		String sessionId = getSessionId();
	                theOutput = "SESSION_ID " + sessionId ;
	                syncInfo.put(SESSIONID,sessionId);
	                state = SENTACK;
				}
	            else{
					theOutput = "INCORRECT INTERVAL. TRY AGAIN" ;
					state = SENTSYNC;
	            }
            } else {
                theOutput = "You're supposed to say \"INTERVAL <interval>\"! " +
			    "Try again.";
            }
        } else if (state == SENTACK) {
			if (inputKey.equalsIgnoreCase("ACK")) {
				theOutput = "BYE";
				state = WAITING;
			} 
			else{
				theOutput = "You're supposed to say \"ACK\"! " +
			    "Try again." ;
			}
		}
        
        return theOutput;
    }
    
    
    
    private String getSessionId(){
    	String sessionId = null;
    	Random rand = new Random();
    	sessionId = Integer.toString(rand.nextInt());
    	
    	return sessionId;
    	
    }
    
    
	/**
	 * Check if number is valid
	 */
	public static boolean isNumber(String n) {
	  try {
		int d = Integer.valueOf(n).intValue();
		return true;
		}
	  catch (NumberFormatException e) {
		//e.printStackTrace();
		return false;
		}
	}

    
    
    
    
    
    
	/**
	 * @return
	 */
	public HashMap getSyncInfo() {
		return syncInfo;
	}

	/**
	 * @param map
	 */
	public void setSyncInfo(HashMap map) {
		syncInfo = map;
	}

}
