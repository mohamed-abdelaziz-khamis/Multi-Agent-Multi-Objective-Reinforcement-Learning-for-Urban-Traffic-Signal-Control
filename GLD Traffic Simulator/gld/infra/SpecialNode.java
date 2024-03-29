
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

import gld.idm.Constants;
import gld.sim.SimModel;
import gld.utils.Arrayutils;
import gld.utils.ListEnumeration;
import gld.xml.TwoStageLoader;
import gld.xml.XMLArray;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;
import gld.xml.XMLUtils;

import java.awt.Point;
import java.io.IOException;
import java.util.Dictionary;
import java.util.LinkedList;

/** Class with common code for NetTunnels and EdgeNodes */

public abstract class SpecialNode extends Node implements XMLSerializable,TwoStageLoader, Constants /*EJUST*/ 
{	
	/** The road this SpecialNode is connected to */
	protected Road road;

	/** True if the connected road is an alpha road */
	protected boolean isAlpha;

	/** The connection-position of the connected road */
	protected int roadPos;

	/** The queue with all road users which have not entered the road.
	  * For example because it's already full*/
	protected LinkedList waitingQueue = new LinkedList();
	
	/** Temporary data structure to transfer info from the first stage loader
     * to the second stage loader
	 */
 	protected TwoStageLoaderData loadData = new TwoStageLoaderData();
 	
    /** EJUST: tracking the number of cars entered */
    protected int enteredCars = 0;
    
    /** EJUST: tracking the number of roadusers generated */
    protected int generatedRoadusers = 0;
    
	/** EJUST: Position update time step*/
	protected static final double dt = TIMESTEP_S;
	
 	public SpecialNode() {}
	
	public SpecialNode(Point _coord) 
	{ 	
		super(_coord); 
	}
	
	/*============================================*/
	/* LOAD and SAVE                              */
	/*============================================*/

	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{	
		super.load(myElement,loader);
		isAlpha = myElement.getAttribute("road-is-alpha").getBoolValue();
		roadPos = myElement.getAttribute("position").getIntValue();
		if (isAlpha) {
			road = new Road();
			loader.load(this,road);
		}
		loadData.roadId=myElement.getAttribute("road-id").getIntValue();
		waitingQueue=(LinkedList)(XMLArray.loadArray(this,loader));
	}
	
	public XMLElement saveSelf () throws XMLCannotSaveException {
		
		XMLElement result=super.saveSelf();
		result.setName("node-special");
		result.addAttribute(new XMLAttribute("road-is-alpha",isAlpha));
		result.addAttribute(new XMLAttribute("road-id",road.getId()));
		result.addAttribute(new XMLAttribute("position",roadPos));
		return result;
	}
	
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	
		super.saveChilds(saver);
		if (isAlpha)
			saver.saveObject(road);
		XMLUtils.setParentName(new ListEnumeration(waitingQueue), getXMLName());
		XMLArray.saveArray(waitingQueue,this,saver,"queue");
	}
	
 	public String getXMLName ()
 	{ 	
 		return parentName+".node-special";
 	}
	
 	public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
 	{ 	
 		super.loadSecondStage(dictionaries);
		
 		if (!isAlpha) {
			Dictionary roadDictionary=(Dictionary)(dictionaries.get("road"));
     		road=(Road)(roadDictionary.get(new Integer(loadData.roadId)));
   		}
		
		road.loadSecondStage(dictionaries);
		
		try	{	
			updateLanes();		
		}
		catch (InfraException e) {
			throw new XMLInvalidInputException ("Cannot initialize lanes of node "+nodeId);	
		}
		
		XMLUtils.loadSecondStage(new ListEnumeration(waitingQueue), dictionaries);		
 	}
	
	class TwoStageLoaderData {	
		int roadId;
	}
	
 
	/*============================================*/
	/* Basic GET and SET methods                  */
	/*============================================*/

	/** Returns the road this SpecialNode is connected to */
	public Road getRoad() { 
		return road; 
	}
	
	/** Sets the road this SpecialNode is connected to */
	public void setRoad(Road r)  throws InfraException
	{ 	
		road = r;
		updateLanes();
	}
	
	/** Returns the position of the road */
	public int getRoadPos() { 
		return roadPos; 
	}

	/** Sets the position of the road */
	public void setRoadPos(int pos)  throws InfraException
	{	
		roadPos = pos; 
		updateLanes();
	}
	
	/** Returns true if the road is an alpha road */
	public boolean getAlpha() { return isAlpha; }
	
	/** Sets the isAlpha flag */
	public void setAlpha(boolean f) { isAlpha = f; }
	
	/** Returns all roads connected to this node */
	public Road[] getAllRoads() { Road[] r = { road }; return r; }
  
	/** Returns the alpha roads connected to this node */
	public Road[] getAlphaRoads()
	{
		if (isAlpha) return getAllRoads();
		return new Road[0];
	}

	public int getWidth() {
		if (road != null) {
			int w = road.getWidth();
			if (w < 4) return 4;
			return w;
		}
		return 4;
	}
	
 
	/*============================================*/
	/* RU over node methods	                      */
	/*============================================*/
	
	/** Place a roaduser in one of the outbound queues */
	public void placeRoaduser (Roaduser ru) throws InfraException
	{		
		Drivelane[] lanes = (Drivelane[]) getShortestPaths(ru.getDestNode().getId(), ru.getType()).clone();	
		Arrayutils.randomizeArray(lanes);
			// The next person who outcomments this code will
			// be chopped into little pieces, burned, hanged, chainsawed, 
			// shredded, killed, /toaded and then ported to Microsoft Visual Lisp.
			// You were warned.
			//							Was signed,
			//												Siets El Snel
		if (lanes.length==0) throw new InfraException ("Cannot find shortest path for new Roaduser in EdgeNode");
		
		lanes[0].addRoaduserAtEnd(ru);
	}	

	/** Returns the queue with waiting road users for this node */
	public LinkedList getWaitingQueue()	{ return waitingQueue;	}	
	/** Sets a new queue with waiting road users*/
	public void setWaitingQueue( LinkedList l )	{ waitingQueue = l ; }
	/** Get the number of waiting road users, i.e. the length of the waitingQueue */
	public int getWaitingQueueLength()	{ return waitingQueue.size(); }

	/**Place a roaduser in the waitingQueue*/
	public void enqueueRoaduser(Roaduser ru) {
		waitingQueue.addLast(ru);
	}
	/**Remove a roaduser from the waitingQueue*/
	public Roaduser dequeueRoaduser()	
	{	
		return (Roaduser)waitingQueue.removeFirst();
	}

    /**
     *  EJUST: Returns the number of cars entered from this edge node
     * (used for statistics purposes)
     */

    public int getEnteredCarsCount()
    {
        return enteredCars;

    }
    
    /**
     *  EJUST: Returns the number of road users generated from this edge node
     * (used for statistics purposes)
     */

    public int getGeneratedRoadusersCount()
    {
        return generatedRoadusers;

    }
    
    /**
     *  EJUST: increment number of cars entered from this edge node
     * (used for statistics purposes)
     */

    public void enteredCarsIncrement()
      {
          enteredCars++;
      }

    /**
     *  EJUST: increment number of road users generated from this edge node
     * (used for statistics purposes)
     */

    public void generatedRoadusersIncrement()
      {
    	generatedRoadusers++;
      }

	/*============================================*/
	/* STATISTICS                                 */
	/*============================================*/

	protected double calcDelay(Roaduser ru, int stop, Drivelane lane /*SBC: int distance --> Drivelane lane*/){		
		
		// first, add the delay for the drivelane leading to this EdgeNode
		int start = ru.getDrivelaneStartTime();	
				
		double speed = lane.getSpeedMaxAllowed(); 
		/*SBC: speed = ru.getSpeed()--> lane.getSpeedMaxAllowed()*/
		/*EJUST: speed = lane.getSpeedMaxAllowed() --> ru.get_v0()*/
		
		int distance = lane.getCompleteLength(); 
		/*SBC: lane.getCompleteLength()-->lane.getLength()*/
		
		ru.addDelay((stop - start) - (distance / (speed*dt))); 
		
		// then, return the total delay of the full trip
		return ru.getDelay();
	}
	
	/*EJUST: Return the experienced trip waiting time for the drivelane leading to this EdgeNode*/
	protected int calcWaitingTime(Roaduser ru, int stop){		
				
		/* Returns the engage queue time of this road user in the current drivelane*/
		int engageQueueTime = ru.getDrivelaneEngageQueueTime();

		/* Means that we called node.processStats() from model.moveLane() due to roaduser 
		 * arrived this edge node with NO waiting time in the queue towards this edge node*/
		if (engageQueueTime == -1) 
			return ru.getWaitingTime(); //return the total waiting time of the full trip 
		
		/* Time this road user enters this special node - time road user engaged the queue towards this special node*/
		ru.addWaitingTime(stop - engageQueueTime);
		/*e.g., someone road user reaches his destination, Cairo entrance gate, 
		 * where there is no traffic light but queue to exit Alexandria city.*/
		
		// then, return the total waiting time of the full trip
		return ru.getWaitingTime();
	}
	
	/*============================================*/
	/* MODIFYING DATA                             */
	/*============================================*/
	
	public void reset() {
		super.reset();
		if (isAlpha) road.reset();
		waitingQueue = new LinkedList();
        
		enteredCars = 0; //EJUST (reset the number of cars entered from this edge node)
        generatedRoadusers = 0; //EJUST (reset the number of road users generated from this edge node)
	}

	public void addRoad(Road r, int pos) throws InfraException
	{
		if (r == null) throw new InfraException("Parameter r is null");
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range: " + pos);
		if (road != null) throw new InfraException("Road already exists");
		Node other = r.getOtherNode(this);
		if (other == null || !other.isAlphaRoad(r))
			isAlpha = true;
		else
			isAlpha = false;

		roadPos = pos;
		road = r;
		updateLanes();
	}
	
	public void setAlphaRoad(int pos) throws InfraException
	{
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range");
		if (road == null || pos != roadPos) throw new InfraException("Road at position " + pos + " does not exist");
		isAlpha = true;
		updateLanes();
	}
	
	public void remRoad(int pos) throws InfraException
	{
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range");
		if (road == null || pos != roadPos) throw new InfraException("Road at position " + pos + " does not exist");
		road = null;
		isAlpha = false;
		updateLanes();
	}
	
	public void remRoad(Road r) throws InfraException
	{
		if (r == null) throw new InfraException("Parameter r is null");
		if (road == null) throw new InfraException("No road is connected to this node");
		if (r != road) throw new InfraException("Road not found on this node");
		road = null;
		isAlpha = false;
		updateLanes();
	}
	
	public void remAllRoads() throws InfraException
	{
		road = null;
		isAlpha = false;
		updateLanes();
	}
	
	
	public void setSigns(Sign[] s) throws InfraException { }
	public int getDesiredSignType() throws InfraException { return Sign.NO_SIGN; }
	
	/*============================================*/
	/* COMPLEX GET                                */
	/*============================================*/

	
	public boolean isAlphaRoad(Road r) throws InfraException {
		if (r == null) throw new InfraException("Parameter r is null");
		return r == road && isAlpha;
	}

	public boolean isConnected(Road r) throws InfraException {
		if (r == null) throw new InfraException("Parameter r is null");
		return r == road;
	}
	public int isConnectedAt(Road r) throws InfraException {
		if (r == null) throw new InfraException("Parameter r is null");
		if (r != road) throw new InfraException("Road is not connected to this node");
		return roadPos;
	}
	public boolean isConnectionPosFree(int pos) throws InfraException {
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range");
		return (road == null);
	}
	
	public int getNumRoads() {
		return road != null ? 1 : 0;
	}
	
	public int getNumAlphaRoads() {
		return road != null && isAlpha ? 1 : 0;
	}

	public int getNumInboundLanes() throws InfraException {
		return road.getNumInboundLanes(this);
	}
	public int getNumOutboundLanes() throws InfraException {
		return road.getNumOutboundLanes(this);
	}
	public int getNumAllLanes() {
		return road.getNumAllLanes();
	}
	public int getNumSigns() { return 0; }
	public int getNumRealSigns() { return 0; }
  
	public Drivelane[] getLanesLeadingTo(Drivelane lane, int ruType) throws InfraException {
		return new Drivelane[0];
	}
	
	public Drivelane[] getLanesLeadingFrom(Drivelane lane, int ruType) throws InfraException {
		return new Drivelane[0];
	}

	/** GASTON: PARA DAR COMPATIBILIDAD CON LOS OTROS TIPO DE NODOS
	 * TO GIVE SUPPORT TO OTHER TYPES OF NODES
	 * */
	public Drivelane[] getAvailableLanesLeadingFrom(Drivelane lane, int ruType) throws InfraException {
		return getLanesLeadingFrom(lane,ruType);
	}
	
	/** GASTON: PARA DAR COMPATIBILIDAD CON LOS OTROS TIPO DE NODOS
	 * TO GIVE SUPPORT TO OTHER TYPES OF NODES
	 * */
	public Drivelane[] getAvailableInboundLanes() throws InfraException {
		return getInboundLanes();
	}

	/** GASTON: PARA DAR COMPATIBILIDAD CON LOS OTROS TIPO DE NODOS
	 * TO GIVE SUPPORT TO OTHER TYPES OF NODES*/
	public Drivelane[] getAvailableLanesLeadingTo(Drivelane lane, int ruType) throws InfraException {
		return getLanesLeadingTo(lane,ruType);
	}
	
	public Drivelane[] getOutboundLanes() throws InfraException {
		return road != null ? road.getOutboundLanes(this) : new Drivelane[0];
	}

	public Drivelane[] getInboundLanes() throws InfraException {
		return road != null ? road.getInboundLanes(this) : new Drivelane[0];
	}

	public Drivelane[] getAllLanes() throws InfraException {
		return (Drivelane[])Arrayutils.addArray(getInboundLanes(), getOutboundLanes());
	}

	/*============================================*/
	/* Hook methods                               */
	/*============================================*/
	
	/* Hook method for stuff that has to be done every step in the sim */
	public void doStep (SimModel model)
	{ 

	}

   /** Hook method for stuff that has to be done when the sim is started */
	public void start ()
	{ 
	}
	
   /** Hook method for stuff that has to be done when the sim is stopped */
	public void stop ()
	{ 
	}
	
	/** Hook method that is called by the infra when a roaduser reaches this
	  * node
	 */
	public void enter (Roaduser ru)
	{
		if (ru instanceof CustomRoaduser) CustomFactory.removeCustom((CustomRoaduser)ru);
	}

}
	