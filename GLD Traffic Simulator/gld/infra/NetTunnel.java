
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University 
 *
 * This program (Green Light District) is free software.
 * You may redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation (version 2 or later).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * See the documentation of Green Light District for further information.
 *------------------------------------------------------------------------*/

package gld.infra;

import gld.GLDException;
import gld.sim.SimModel;
import gld.sim.SimulationRunningException;
import gld.idm.Constants;
import gld.idm.IDMBicycleDry;
import gld.idm.IDMBusDry;
import gld.idm.IDMCarDry;
import gld.idm.LaneChangeGLD;
import gld.idm.MicroModelGLD;

import gld.utils.ListEnumeration;
import gld.xml.XMLArray;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLTreeException;
import gld.xml.XMLUtils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedList;


/**
 *
 * NetTunnel, our K-r4D 31337 eAsTr egq0r! ph34R uS !!!!!
 * 
 * @author Group Datastructures
 * @version 666 
 */

public class NetTunnel extends SpecialNode implements Constants
{
	/** The type of this node */
	protected static final int type = Node.NET_TUNNEL;
	/** The port on which this this Tunnel should be listening */
	protected int localPort;
	/** The hostname and port to which this Tunnel should send its Roadusers
	 */ 
	protected String remoteHostname;
	protected int remotePort;
	/** Indicates if our network gear is ready for rock 'n roll */
	protected boolean netInitialized=false;
	/** A LinkedList with Roadusers which are waiting to be send to the 
	  * remote machine 
	 */
	protected LinkedList sendQueue; 
	/** These variables indicate the state of the infra */
	protected boolean paused=false,stopped=false,sigStop=false;
	/** Network stuff */
	XMLSaver sender;
	SocketServer server;
	
	/**EJUST*/
    protected MicroModelGLD idmCar =new IDMCarDry();
    protected MicroModelGLD idmBus=new IDMBusDry();
    protected MicroModelGLD idmBicycle=new IDMBicycleDry();
    
    //!!! bicycle (=> vw for better impl!)
    protected LaneChangeGLD cute=new LaneChangeGLD(P_FACTOR_BICYCLE, DB_BICYCLE, MAIN_SMIN,MAIN_BSAVE, BIAS_RIGHT_BICYCLE);

	//!!! bus (=> vw for better impl!)
    protected LaneChangeGLD polite=new LaneChangeGLD(P_FACTOR_TRUCK, DB_TRUCK, MAIN_SMIN,MAIN_BSAVE, BIAS_RIGHT_TRUCK);

    //!!! car (=> vw for better impl!)
    protected LaneChangeGLD inconsiderate=new LaneChangeGLD(P_FACTOR_CAR, DB_CAR, MAIN_SMIN,MAIN_BSAVE, BIAS_RIGHT_CAR);
    /*EJUST*/


	public NetTunnel() 
	{	super();
		sendQueue=new LinkedList();
	}
	
	public NetTunnel(Point _coord) 
	{	super(_coord); 
		sendQueue=new LinkedList();
	}
	
	public void start()
	{ 	try
	   {	sendQueue=new LinkedList();
			server=new SocketServer();
			server.start();
			netInitialized=true;	}
		catch (Exception e)
		{ 	System.out.println("Cannot start NetTunnel "+nodeId+" : "+e);
			e.printStackTrace(System.out);
		}
	}
	
	public void stop()
	{	sigStop=true;
		sender.close();
		stopped=true;
		notifyAll();
		reset();
	}
	
	public void enter(Roaduser ru)
	{
		sendQueue.add(ru);
		super.enter(ru);
	}
	
	/*============================================*/
	/* LOAD and SAVE                              */
	/*============================================*/

	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{	
		super.load(myElement,loader);
		remoteHostname=myElement.getAttribute("remote-host").getValue();		
		remotePort=myElement.getAttribute("remote-port").getIntValue();		
		localPort=myElement.getAttribute("local-port").getIntValue();
		sendQueue=(LinkedList)(XMLArray.loadArray(this,loader));
	}

	public XMLElement saveSelf () throws XMLCannotSaveException {
		XMLElement result=super.saveSelf();
		result.setName("node-tunnel");
		result.addAttribute(new XMLAttribute("remote-host",remoteHostname));
		result.addAttribute(new XMLAttribute("remote-port",remotePort));
		result.addAttribute(new XMLAttribute("local-port",localPort));
		return result;
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	super.saveChilds(saver);
		XMLUtils.setParentName(new ListEnumeration(sendQueue),getXMLName());
		XMLArray.saveArray(sendQueue,this,saver,"send-queue");
	}
	
 
 	public String getXMLName ()
 	{ 	return parentName+".node-tunnel";
 	}
	
 	public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
 	{ 	super.loadSecondStage(dictionaries);
		XMLUtils.loadSecondStage (new ListEnumeration(sendQueue),dictionaries);
 	}
	
	class TwoStageLoaderData {	
		int roadId;
	}
 
   /*============================================*/
	/* Basic GET and SET methods                  */
	/*============================================*/

	/** Returns the type of this node */
	public int getType() { return type; }
	
	/** Returns the name of this nettunnel. */
	public String getName() { return "NetTunnel " + nodeId; }
	
	public String getRemoteHostname () { return remoteHostname; }
	public int getLocalPort () { return localPort; }
	public int getRemotePort () { return remotePort; }
	
	public void setRemoteHostname (String remoteHostname) throws SimulationRunningException
	{	if (netInitialized)
			throw new SimulationRunningException
			("Cannot change network settings of NetTunnel when sim is running.");
		else
			this.remoteHostname=remoteHostname;
	}
		
	public void setLocalPort (int localPort) throws SimulationRunningException
	{	if (netInitialized)
			throw new SimulationRunningException
			("Cannot change network settings of NetTunnel when sim is running.");
		else
			this.localPort=localPort;
	}

	public void setRemotePort (int remotePort) throws SimulationRunningException
	{	if (netInitialized)
			throw new SimulationRunningException
			("Cannot change network settings of NetTunnel when sim is running.");
		else
			this.remotePort=remotePort;
	}
	
	public void reset ()
	{ 	super.reset();
		sendQueue=new LinkedList();
		waitingQueue=new LinkedList();
	}
	
	public int getSendQueueLength() { return sendQueue.size(); }

	//SBC
	/** not needed for this class */
	public int getPhaseDiff() { return -1; }
	/** not needed for this class */
	public void setPhaseDiff(int newPhase) { }
	/** not needed for this class */
	public boolean isGreenWaveNode() { return false; }
	/** not needed for this class */
	public void setGreenWaveNode(boolean gw) {}
	/** not needed for this class */
	public boolean isGreenWaveStart() {return false; }
	/** not needed for this class */
	public void setGreenWaveStart(boolean s) {}
	/** not needed for this class */
	public boolean isGreenWaveFinish() {return false;}
	/** not needed for this class */
	public void setGreenWaveFinish(boolean f) {}	
	//SBC

	/*============================================*/
	/* Graphics stuff                             */
	/*============================================*/


	public void paint(Graphics g) throws GLDException
	{
		paint(g, 0, 0, 1.0f, 0.0);
	}
	
	public void paint(Graphics g, int x, int y, float zf) throws GLDException
	{
		paint(g,x,y,zf,0.0);
	}
	
	public void paint(Graphics g, int x, int y, float zf, double bogus) throws GLDException
	{
		int width = getWidth();
		g.setColor(Color.green);
		g.drawRect((int)((coord.x + x - 5 * width) * zf), (int)((coord.y + y - 5 * width) * zf), (int)(10 * width * zf), (int)(10 * width * zf));
		if(nodeId != -1)
			g.drawString("" + nodeId,(int)((coord.x + x - 5 * width) * zf) - 10,(int)((coord.y + y - 5 * width) * zf) - 3);
	}
	
   /*============================================*/
	/* Server thread                              */
	/*============================================*/
	
 class SocketServer extends Thread
 {	private ServerSocket socket;
	
  	public SocketServer () 
   { try
     { socket=new ServerSocket(localPort);
     }
     catch (Exception e)
     {  System.out.println ("Unable to make socket server for NetTunnel."+
	          "Cannot receive roadusers :"+e);
     }
   }
 
   public void run ()
   { Socket client;
     while (! sigStop)
     { System.out.println("Server socket of tunnel "+nodeId+" runs on  "+localPort);
       try
       { client=socket.accept();
         (new ReceivingSocket(client)).start();
			System.out.println("NetTunnel "+nodeId+" received new connection.");
       }
       catch (Exception e)
       { System.out.println ("NetTunnel couldn't accept connection :"+e);
       }
     }
   }
 }
 
   /*============================================*/
	/* Receive stuff                             	*/
	/*============================================*/
	
 public void receive (XMLElement element) throws XMLInvalidInputException,InfraException
 { 		
	 	/*EJUST*/
		MicroModelGLD idm; 
		LaneChangeGLD laneChange;
		int ruType = element.getAttribute("type").getIntValue();		
		if (ruType == RoaduserFactory.CAR){
			idm = idmCar;
			laneChange = inconsiderate;
		}
		else if (ruType == RoaduserFactory.BUS){
			idm = idmBus;
			laneChange = polite;
		}
		else {
			idm = idmBicycle;
			laneChange = cute;
		}
		/*EJUST*/	
	 
		waitingQueue.add(RoaduserFactory.genRoaduser(ruType, this, this, 0, idm /*EJUST*/, laneChange/*EJUST*/));
 }
 
 public void doStep (SimModel model)
 { super.doStep(model);
 	try
   {	processSend();
 		processReceive(model);
	}
	catch (Exception e)
	{ e.printStackTrace(System.out);
	}
 }
 
 public void processReceive (SimModel model) throws InfraException
 {	Iterator i=waitingQueue.iterator();
 	Roaduser ru;
	while (i.hasNext())
	{	ru=(Roaduser)(i.next());
		ru.setDestNode(model.getRandomDestination(this));
		try
		{	placeRoaduser(ru);
			i.remove();
		}
		catch (InfraException e)
		{	// Lane was full. Wait till next turn
		}
	}
 }
 
 public void processSend () throws IOException,XMLCannotSaveException,XMLTreeException
 { if ( sender==null || ! sender.hasStream())
	{ try
	  {	sender=new XMLSaver(remoteHostname,remotePort);
	  }
	  catch (Exception e)
	  { System.out.println ("NetTunnel "+nodeId+" cannot connect to its peer on "+
	  		remoteHostname+":"+remotePort+". Postponing send. I will retry later.");
	  }
	}
	else
	{ 	Iterator i=sendQueue.iterator();
 		Roaduser ru;
		while (i.hasNext())
		{	sender.saveAtomaryElement(null,((Roaduser)(i.next())).saveSelf());
			i.remove();
		}
 	}
 }
 
 class ReceivingSocket extends Thread
 { 
	 private XMLLoader loader;
   
   public ReceivingSocket (Socket socket)
   { try
     { loader=new XMLLoader(socket);
     }
     catch (Exception e)
     { System.out.println("Problem with initializing listening socket for NetTunnel");
     }
   }
  
   public void run()
   { 	while ( !sigStop )
		{	try
		   { 	receive(loader.getNextElement(null));
			}
			catch (InfraException e)
			{ 	System.out.println("Cannot generate received RU in NetTunnel "+nodeId);
			}
			catch (Exception e)
			{	System.out.println("A receiver in NetTunnel "+nodeId+
						" broke of its connection.");	
				//e.printStackTrace(System.out);
				return;
			}
      }
		if (sigStop)
			loader.close();
   }
  }
}