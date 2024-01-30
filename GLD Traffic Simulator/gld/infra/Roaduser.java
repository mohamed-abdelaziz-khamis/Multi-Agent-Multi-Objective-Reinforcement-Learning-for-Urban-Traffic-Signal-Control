
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
import gld.GLDSim;
import gld.Selectable;
import gld.idm.MicroModelGLD;
import gld.xml.TwoStageLoader;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;
import gld.xml.XMLUtils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Random;

/**
 *
 * Basic Roaduser
 *
 * @author Group Datastructures
 * @version 1.0
 *
 * Todo:
 * Fix Save/Load
 */

abstract public class Roaduser implements Selectable, XMLSerializable, TwoStageLoader, Cloneable 
{
	/*POMDPGLD*/
    public static int NOT_DETECTED = 0;
    public static int DETECTED = 1;
    public static int DOUBLE_DETECTED = 2;

    public static int NOCHANGE = 0;
    public static int GAUSSIAN = 1;
    public static int UNIFORM = 2;
    public static int SPETHIAL = 3;

    protected static Random RND = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
	/*POMDPGLD*/
	
	/** The node this Roaduser spawned at */
	protected Node startNode;
	
	/** The node that is the destination of this Roaduser */
	protected Node destNode;
	
	/** The last timeStep this Roaduser moved */
	protected int timeStepMoved;
	
	/** The last timeStep this Roaduser was asked when it had last moved */
	protected int timeStepAsked;
	
	/** The position of this Roaduser on the drivelane. Zero based. */
	protected double position; /*EJUST: int --> double*/
	
	/** The SignID of the lane this Roaduser came from */
	protected int prevSign = -1;
	
	/** EJUST: The last position on the previous lane this Roaduser came from */
	protected double prevSignLastPosition = -1;
	
	/** The starttime on this lane */
	protected int drivelaneStartTime;
	
	/**EJUST: The engage queue time on this lane */
	protected int drivelaneEngageQueueTime;
	
	/**EJUST: The trip start time*/
	protected int tripStartTime;
	
	/** The delay experienced so far */
	protected double delay; /*EJUST: int --> double*/
	
	/**EJUST: The waiting time experienced so far */
	protected int waitingTime;
	
	/**EJUST: The absolute stops count experienced so far */
	protected int absoluteStopsCount;
	
	/**EJUST: The stops count experienced so far */
	protected int stopsCount;
	
	/**EJUST: The distance experienced so far */
	protected double distance;
	
	/** Stuff to transfer between the first and second stage loader */
	protected TwoStageLoaderData loadData=new TwoStageLoaderData();
	
	/** The color of this Roaduser */
	protected Color color = new Color(0, 0, 0);
    
	/** If it has a head, the headercolor will be: **/
    protected Color headcolor = new Color(0,0,0);
	
    /** The name of the parent of this Roaduser */
	protected String parentName="model.infrastructure.lane";

    /** The id of the sign this roaduser last passed */
	protected int waitTl=-1;
	
	/** The position in the sign this roaduser last passed */
	protected double waitPos=-1; /*EJUST: int --> double*/
	
	/** The color of the sign this roaduser last passed */
	protected boolean waitTlColor=false;
    
	/** The prev of the sign this roaduser last passed */
	protected int prevWaitTl=-1;
	
	/** The prev position of the sign this roaduser last passed */
	protected double prevWaitPos=-1; /*EJUST: int --> double*/
	
	/** The id of the sign this roaduser last passed */
	protected boolean prevWaitTlColor=false;
	
	protected boolean voted=false;
	
	protected boolean inQueueForSign=false;

	//POMDPGLD
    protected int visibility = 1;
	
	public void setInQueueForSign(boolean b) { inQueueForSign=b; }
	public boolean getInQueueForSign() { return inQueueForSign; }

	public Roaduser(Node _startNode, Node _destNode, double pos /*EJUST: int --> double*/) 
	{	
		this();
		startNode = _startNode;
		destNode = _destNode;
		position = pos;
	}

	public Roaduser()
	{	
		resetStats();
	}

	public void resetStats ()
	{	
		timeStepMoved = -1;
		timeStepAsked = -1;
		
		drivelaneStartTime = -1;
		drivelaneEngageQueueTime = -1; /*EJUST*/
		
		delay=0;
		waitingTime=0; /*EJUST*/
		absoluteStopsCount=0; /*EJUST*/
		stopsCount=0; /*EJUST*/
		distance=0; /*EJUST*/
	}

	public Object clone ()
	{ 	
		try {return super.clone();} 
		catch(Exception e) {System.out.println(e);}
		return null;
	}

	/*POMDPGLD*/
    public void setVisibility( int visibility ) {
        this.visibility = visibility;
        /*if(visibility == Roaduser.NOT_DETECTED) {
            color = new Color(color.getRed(),color.getGreen(),color.getBlue(),25);
        }
        if(visibility == Roaduser.DETECTED) {
            color = new Color(color.getRed(),color.getGreen(),color.getBlue(),60);
        }
        if(visibility == Roaduser.DETECTED) {
            color = new Color(color.getRed(),color.getGreen(),color.getBlue(),100);
        }*/
    }
    
    /*POMDPGLD*/
	 public int getVisibility( ) {
	        return this.visibility;
	 }
	

	/*============================================*/
	/* Basic GET and SET methods                  */
	/*============================================*/

	/** Returns the start Node of this Roaduser */
	public Node getStartNode() { return startNode; }
	/** Sets the start Node of this Roaduser */
	public void setStartNode(Node n) { startNode = n; }

	/** Returns the destination Node of this Roaduser */
	public Node getDestNode() { return destNode; }
	/** Sets the destination Node of this Roaduser */
	public void setDestNode(Node n) { destNode = n; }

	/** Returns the position of this Roaduser on the current Drivelane */
	public double getPosition() { return position; } /*EJUST: int --> double*/
	
	/** Sets the position of this Roaduser on the current Drivelane */
	public void setPosition(double pos) /*EJUST: int --> double*/ 
	{ /*System.out.println("newPos:"+pos);*/ position = pos; }

	/** Returns the last timeStep this Roaduser moved */
	public int getTimeStepMoved() { return timeStepMoved; }
	/** Sets the last timeStep this Roaduser moved */
	public void setTimeStepMoved(int timeStep) { timeStepMoved = timeStep; }

	/** Returns the last timeStep this Roaduser was asked about its movements */
	public int getTimeStepAsked() { return timeStepAsked; }
	/** Sets the last timeStep this Roaduser was asked its movements */
	public void setTimeStepAsked(int timeStep) { timeStepAsked = timeStep; }

	/** Returns the start time of this Roaduser in the current drivelane */
	public int getDrivelaneStartTime() { return drivelaneStartTime; }
	/** Sets the start time of this Roaduser on the current Drivelane */
	public void setDrivelaneStartTime(int time) { drivelaneStartTime = time; }

	/** Returns the engage queue time of this Roaduser in the current drivelane 
	 * @author EJUST*/
	public int getDrivelaneEngageQueueTime() { return drivelaneEngageQueueTime; }
	/** Sets the engage queue time of this Roaduser on the current Drivelane 
	 * @author EJUST*/
	public void setDrivelaneEngageQueueTime(int time) { drivelaneEngageQueueTime = time; }
	
	/** Returns the trip start time of this Roaduser 
	 * @author EJUST*/
	public int getTripStartTime() { return tripStartTime; }
	/** Sets the trip start time of this Roaduser 
	 * @author EJUST*/
	public void setTripStartTime(int time) { tripStartTime = time; }
	
	/** Returns the delay experienced so far */
	public double getDelay() { return delay; } /*EJUST: int --> double*/
	/** Add a given delay to the total delay already experienced */
	public void addDelay(double d) { delay += d; } /*EJUST: int --> double*/
	/** Sets a new delay */
	public void setDelay(double delay) { this.delay=delay;} /*EJUST: int --> double*/
	
	/** Returns the waiting time experienced so far 
	 * @author EJUST*/
	public int getWaitingTime() { return waitingTime; }
	/** Add a given waiting time to the total waiting time already experienced 
	 * @author EJUST*/
	public void addWaitingTime(int w) { waitingTime += w; }
	
	/** Returns the absolute stops count experienced so far 
	 * @author EJUST*/
	public int getAbsoluteStopsCount() { return absoluteStopsCount; }
	/** increment the absolute stops count experienced so far 
	 * @author EJUST*/
	public void  incrementAbsoluteStopsCount() { absoluteStopsCount++; }
	
	/** Returns the stops count experienced so far 
	 * @author EJUST*/
	public int getStopsCount() { return stopsCount; }
	/** increment the stops count experienced so far 
	 * @author EJUST*/
	public void  incrementStopsCount() { stopsCount++; }
	
	/** Returns the distance experienced so far 
	 * @author EJUST*/
	public double getDistance() { return distance; }
	/** Add a given distance to the total distance already experienced 
	 * @author EJUST*/
	public void addDistance(double d) { distance += d; }
	
	/** Returns the Id of the previous lane this Roaduser hit */
	public int getPrevSign() { return prevSign; }
	/** Sets the Id of the previous lane this Roaduser hit */
	public void setPrevSign(int _prevSign) { prevSign = _prevSign; }
	
	/** Returns the last position on the previous lane this Roaduser hit 
	 * @author EJUST*/
	public double getPrevSignLastPosition() { return prevSignLastPosition; }
	/** Sets the last position on the previous lane this Roaduser hit 
	 * @author EJUST*/
	public void setPrevSignLastPosition(double _prevSignLastPosition) { prevSignLastPosition = _prevSignLastPosition; }
	
	/** Returns the color of this Roaduser */
	public Color getColor(){ return color; }
	/** Sets the color of this Roaduser */
	public void setColor(Color c) { color = c; }
    /** Returns the color of this Roaduser */
    public Color getHeadColor(){ return headcolor; }
    /** Sets the color of this Roaduser */
    public void setHeadColor(Color c) { headcolor = c; }


	/** Sets the last waiting point's position relative to the sign*/
	public void setWaitPos(int tlId, boolean b, double pos /*EJUST: int --> double*/) { 
	    prevWaitTl = waitTl;
	    prevWaitTlColor = waitTlColor;
	    prevWaitPos = waitPos;
	    waitTl=tlId;
	    waitTlColor=b;
	    waitPos=pos;
	}
	/** Gets the last waiting point's position relative to the sign*/
	public double getPrevWaitPos() { return prevWaitPos; } /*EJUST: int --> double*/
	/** Gets the last waiting point's sign*/
	public int getPrevWaitTl() { return prevWaitTl; }
	/** Gets the last waiting point's sign*/
	public boolean getPrevWaitTlColor() { return prevWaitTlColor; }
	/** Gets the current waiting point's sign*/
	public int getCurrentWaitTl() { return waitTl; }
	/** Gets the current waiting point's position relative to the sign*/
	public double getCurrentWaitPos() { return waitPos; } /*EJUST: int --> double*/
	/** Gets the current waiting point's sign*/
	public boolean getCurrentWaitTlColor() { return waitTlColor; }

	public boolean didVote() { return voted; }
	public void setVoted(boolean v) { voted = v; }

	public boolean didMove(int timeStepNow) {
		timeStepAsked = timeStepNow;
		return timeStepMoved == timeStepNow;
	}

	public abstract String getName();
	public abstract int getNumPassengers();
	public abstract int getLength();
	public abstract double getSpeed(); /*EJUST: int --> double*/
	
	/**EJUST: Get the previous speed for counting the roaduser stops*/
	public abstract double getPrevSpeed();
	
	/** POMDPGLD*/
	public abstract void updateSpeed();  
	
	//SBC
	public abstract void setSpeed(double newspeed); /*EJUST: int --> double*/
	
	public abstract double accelerate(double goalSpeed, double dt /*EJUST*/); /*EJUST: int --> double*/
	public abstract double deaccelerate(double goalSpeed, double dt /*EJUST*/); /*EJUST: int --> double*/
	public abstract double adjustSpeedTo(double goalSpeed, double dt /*EJUST*/); /*EJUST: int --> double*/
	public abstract int getPlatoonValue();
	
	public abstract int getType();
	
	//SBC
	public abstract double getAcceleration(); /*EJUST: int --> double*/
	public abstract double getStopDistance(); /*EJUST: int --> double*/
	
	/*EJUST*/
	public abstract void accelerate(Roaduser previousRoaduser, double dt);
	public abstract boolean change(boolean leftLane, Roaduser frontRoaduserOld, Roaduser frontRoaduserNew, Roaduser backRoaduserNew);
	public abstract MicroModelGLD model();
	public abstract void setModel(MicroModelGLD model);
	public abstract boolean timeToChange(double dt);
	
		
	public String getVehicleName() { return "unknown"; }
	public String getDriverName() { return "unknown"; }
	public String getDescription() { return "no description available."; }
	public String getPicture() { return null; }
	public String getSound() { return null; }

	/*============================================*/
	/* Selectable                                 */
	/*============================================*/

	public Rectangle getBounds() { return new Rectangle(0, 0, 0, 0); }
	public Shape getComplexBounds() { return getBounds(); }
	public int getDistance(Point p) { return 0; }
	public Point getSelectionPoint() { return new Point(0, 0); }
	public Point[] getSelectionPoints() { return new Point[0]; }
	public Point getCenterPoint() { return new Point(0, 0); }
	public boolean isSelectable() { return false; }
	public boolean hasChildren() { return false; }
	public Enumeration<?> getChildren() { return null; }

	public void paint(Graphics g) throws GLDException
	{
		paint(g, 0, 0, 1.0f);
	}

	public void paint(Graphics g, int dx, int dy, double angle) throws GLDException {
		paint(g, dx, dy, 1.0f, angle);
	}
	
	public abstract void paint(Graphics g, int dx, int dy, float zf) throws GLDException;
	public abstract void paint(Graphics g, int dx, int dy, float zf, double angle) throws GLDException;

	/*============================================*/
	/* Load/save                                  */
	/*============================================*/

 	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
 	{ 	
 		loadData.startNodeId=myElement.getAttribute("start-node-id").getIntValue();
   		loadData.destNodeId=myElement.getAttribute("dest-node-id").getIntValue();
   		timeStepMoved=myElement.getAttribute("timeStep-moved").getIntValue();
   		timeStepAsked=myElement.getAttribute("timeStep-asked").getIntValue();
   		position=myElement.getAttribute("position").getDoubleValue(); /*EJUST: int --> double*/
   		drivelaneStartTime=myElement.getAttribute("lane-start-time").getIntValue();
   		drivelaneEngageQueueTime=myElement.getAttribute("lane-engage-queue-time").getIntValue(); /*EJUST*/
   		tripStartTime=myElement.getAttribute("trip-start-time").getIntValue(); /*EJUST*/
   		delay=myElement.getAttribute("delay").getIntValue();
   		waitingTime=myElement.getAttribute("waiting-time").getIntValue(); /*EJUST*/
   		absoluteStopsCount=myElement.getAttribute("absolute-stops-count").getIntValue(); /*EJUST*/
   		stopsCount=myElement.getAttribute("stops-count").getIntValue(); /*EJUST*/
   		distance=myElement.getAttribute("distance").getDoubleValue(); /*EJUST*/
   		prevSign=myElement.getAttribute("prevSign").getIntValue();
   		prevSignLastPosition=myElement.getAttribute("prevSign-last-position").getDoubleValue(); /*EJUST*/		
		color=new Color (myElement.getAttribute("color-red").getIntValue(),
		myElement.getAttribute("color-green").getIntValue(),
		myElement.getAttribute("color-blue").getIntValue());
	}

 	public XMLElement saveSelf () throws XMLCannotSaveException
 	{ 	
 		XMLElement result=new XMLElement(XMLUtils.getLastName(getXMLName()));
   		result.addAttribute(new XMLAttribute("start-node-id",startNode.getId()));
   		result.addAttribute(new XMLAttribute("dest-node-id",destNode.getId()));
   		result.addAttribute(new XMLAttribute("timeStep-moved",timeStepMoved));
   		result.addAttribute(new XMLAttribute("timeStep-asked",timeStepAsked));
   		result.addAttribute(new XMLAttribute("position",position));
   		result.addAttribute(new XMLAttribute("type",getType())); // For tunnels
   		result.addAttribute(new XMLAttribute("lane-start-time",drivelaneStartTime));
   		result.addAttribute(new XMLAttribute("lane-engage-queue-time",drivelaneEngageQueueTime)); /*EJUST*/
   		result.addAttribute(new XMLAttribute("trip-start-time",tripStartTime)); /*EJUST*/
   		result.addAttribute(new XMLAttribute("delay",delay));
   		result.addAttribute(new XMLAttribute("waiting-time",waitingTime)); /*EJUST*/
   		result.addAttribute(new XMLAttribute("absolute-stops-count",absoluteStopsCount)); /*EJUST*/
   		result.addAttribute(new XMLAttribute("stops-count",stopsCount)); /*EJUST*/
   		result.addAttribute(new XMLAttribute("distance",distance)); /*EJUST*/
   		result.addAttribute(new XMLAttribute("prevSign",prevSign));
   		result.addAttribute(new XMLAttribute("prevSign-last-position",prevSignLastPosition)); /*EJUST*/		
		result.addAttribute(new XMLAttribute("color-green",color.getGreen()));
		result.addAttribute(new XMLAttribute("color-blue",color.getBlue()));
		result.addAttribute(new XMLAttribute("color-red",color.getRed()));
   		return result;
 	}

 	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
 	{ 	
 		// Roadusers don't have child objects
 	}

 	public void setParentName (String parentName)
 	{ 	
 		this.parentName=parentName;
 	}

 	class TwoStageLoaderData
 	{ 	
 		int startNodeId, destNodeId;
 	}

 	public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
 	{	
 		Dictionary nodeDictionary=(Dictionary)(dictionaries.get("node"));
   		startNode=(Node)(nodeDictionary.get(new Integer(loadData.startNodeId)));
   		destNode=(Node)(nodeDictionary.get(new Integer(loadData.destNodeId)));
 	}
}