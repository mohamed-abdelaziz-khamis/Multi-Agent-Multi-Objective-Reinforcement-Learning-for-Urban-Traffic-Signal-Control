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
package gld.algo.tlc.iatracos.listener;

import gld.algo.tlc.iatracos.SignConfigInterval;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ResourceBundle;

/**
* CVS Log
* $Id: SocketSignConfigListener.java,v 1.1 2006/08/04 21:18:46 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class SocketSignConfigListener extends BaseSignConfigListener {
	private ServerSocket serverSocket;
	private Socket socket;
	private static String portKey = "gld.algo.tlc.iatracos.listener.socket.port";
	private boolean alive = true;
 
	public void stopListener() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
				this.serverSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.alive = false;
	}

	public void readData() {
		if (socket == null) {
			this.createListener();
		}
		try {
			populateSignConfigInterval(socket.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				this.socket.close();
				this.socket = null;
				this.stopListener();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void createListener() {
		if (serverSocket == null) {
			try {
				serverSocket = new ServerSocket(Integer.parseInt(rb.getString(portKey)));
			} catch (IOException e1) {
				System.out.println("Could not create socket."); //no pudo crear el socket
			}
		}
		try {
			socket = serverSocket.accept();
		} catch (IOException e) {
			System.out.println("Could not accept the connection."); //no pudo aceptar la conexión
		}
		this.alive = true;
	}


	public void run() {
		while (true) {
			this.readData();
		}
	}

}
