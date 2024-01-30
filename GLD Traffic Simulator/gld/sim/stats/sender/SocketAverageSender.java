/*
 * Created on Sep 5, 2004
 * Universidad Tecnológica Nacional - Facultad Regional Buenos Aires
 * Proyecto Final 2004
 * I-ATraCoS - Grupo :
 * - Brey, Gustavo
 * - Escobar, Gaston
 * - Espinosa, Marisa
 * - Pastorino, Marcelo
 * All right reserved
 * Class Description
 * 
 */
package gld.sim.stats.sender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
* CVS Log
* $Id: SocketAverageSender.java,v 1.1 2006/08/04 21:18:53 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class SocketAverageSender extends BaseAverageSender {

	private static String hostKey = "gld.sim.stats.sender.socket.host";
	private static String portKey = "gld.sim.stats.sender.socket.port";
	
	public PrintWriter getSender() throws UnknownHostException , IOException {
		Socket socket = null;
		PrintWriter out = null;
		try {
			//Passing null to getByName() produces the
			//special "Local Loopback" IP address, for
			//testing on one machine w/o a network:
			//InetAddress addr = InetAddress.getByName(null);
			//Alternatively, you can use
			//the address or name:
			//InetAddress addr = InetAddress.getByName("ibmprofe");
			InetAddress addr = InetAddress.getByName(rb.getString(hostKey));
			//InetAddress addr =
			//InetAddress.getByName("localhost");
			System.out.println("addr = " + addr);
			socket = new Socket(addr, Integer.parseInt(rb.getString(portKey)));
		} catch (UnknownHostException e1) {
			//e1.printStackTrace();
			throw e1;
		} catch (IOException e1) {
			//e1.printStackTrace();
			throw e1;
		}
		//Guard everything in a try-finally to make
		//sure that the socket is closed:
		try {
			//va a intentar crear el PrintWriter
			System.out.println("will try to create the PrintWriter");
			
			out =
			new PrintWriter(
			new BufferedWriter(
			new OutputStreamWriter(
			socket.getOutputStream())),true);
			

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			return out;
		}
		
	}
	
	
	
	public void closeSender(PrintWriter out) {
		out.close();

	}	

}
