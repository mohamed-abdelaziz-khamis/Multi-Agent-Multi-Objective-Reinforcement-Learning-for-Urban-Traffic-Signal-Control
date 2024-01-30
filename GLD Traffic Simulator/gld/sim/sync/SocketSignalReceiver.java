/*
 * Created on Sep 5, 2004
 * Universidad Tecnol�gica Nacional - Facultad Regional Buenos Aires
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
package gld.sim.sync;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
* CVS Log
* $Id: SocketSignalReceiver.java,v 1.1 2006/08/04 21:18:51 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class SocketSignalReceiver extends BaseSignalReceiver {

	private static String syncPortKey = "gld.synchronization.listener.port";

	public HashMap getSignalInfo(String sessionId, ResourceBundle rb) throws IOException {
		//We open and close the socket in every synchronization because we considered it was
		//not necessary to leave the port opened during the whole simulation
		ServerSocket serverSocket = null;
		if (serverSocket == null) {
			try {
				serverSocket = new ServerSocket(Integer.parseInt(rb.getString(syncPortKey)));
			} catch (IOException e1) {
				System.out.println("Could not create the socket"); //no pudo crear el socket
			}
		}
		Socket clientSocket = null;

		clientSocket = serverSocket.accept();
		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
		BufferedReader in =
			new BufferedReader(
				new InputStreamReader(clientSocket.getInputStream()));
		String inputLine, outputLine;

		//initiate conversation with client
		SynchronizationProtocol syncProtocol = new SynchronizationProtocol(sessionId);
		outputLine = syncProtocol.processInput(null);
		out.println(outputLine);

		while ((inputLine = in.readLine()) != null) {
			outputLine = syncProtocol.processInput(inputLine);
			out.println(outputLine);
			if (outputLine.equals("BYE"))
				break;
		}
		
		out.close();
		in.close();
		clientSocket.close();
		serverSocket.close();


		return syncProtocol.getSyncInfo();
	}
}
