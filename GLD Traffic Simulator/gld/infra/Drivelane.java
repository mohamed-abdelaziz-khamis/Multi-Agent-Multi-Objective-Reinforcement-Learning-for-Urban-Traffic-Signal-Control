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

import gld.Model;
import gld.Selectable;
import gld.algo.dp.DrivingPolicy;
import gld.idm.Constants;
import gld.sim.SimModel;
import gld.utils.Arrayutils;
import gld.utils.ListEnumeration;
import gld.utils.Typeutils;
import gld.xml.InstantiationAssistant;
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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 *
 * The basic lane.
 *
 * @author Group Datastructures
 * @version 1.0
 */

/* Todo:
 * Possible speedup: cache 'numRoadUsersWaiting'
 * 
 * Dr. Walid:
 * I should not empty the number of waiting roadusers in every queue at every time step
 * then recalculate again the number of waiting roadusers in every queue
 */

public class Drivelane implements XMLSerializable, TwoStageLoader, Selectable, Constants /*EJUST*/ {

	/** EJUST: The number of roadusers waiting time tables are maintained of. */
	public static int STAT_NUM_DATA = 125;
	/* If on average every junction has 8 ingoing lanes
	 * And the last statistics number of data for the whole junction is 1000
	 * Then it is reasonable to handle the data of the last 125 roadusers inside every lane*/
	
	/*POMDPGLD*/
	public static final String shortXMLName="dl-fo";

	/** The Id of this drivelane. */
	protected int Id;
	/** The type of drivelane. This is a combination of Roaduser types that may use this drivelane */
	protected int type;
	/** The Road this Drivelane is part of */
	protected Road road;

	/*POMDPGLD*/
	/** Type of lane **/
	protected int laneType;

	/** The roadusers currently on this Drivelane */
	protected LinkedList queue;
	/** The Sign at the end of this Drivelane */
	protected Sign sign;
	/** The directions Roadusers switch lanes to: left, straight ahead and right. */
	protected boolean[] targets = {false, false, false};
	/** The directions that were before lane was disabled (DOAS 05)*/
	protected int[] targetsOriginal = { -1 , -1 , -1};

	/** The last timeStep this Drivelane was asked if it had moved its Roadusers yet */
	protected int timeStepAsked;
	/** The last timeStep this Drivelane moved its Roadusers */
	protected int timeStepMoved;
	/** Data for loading the second stage */
	protected TwoStageLoaderData loadData = new TwoStageLoaderData();
	protected String parentName = "model.infrastructure";
	/** A Shape array holding this drivelane's boundaries */
	protected Shape[] bounds = null;


	/**GASTON: All statistics of this Drivelane. */
	protected DrivelaneStatistics statistics;
	
	
	/**GASTON: we also need to know the timeStep we are in in order to calculate the statistics */
	/*EJUST comment: 
	 * This represents the denominator for calculating the lane statistics
	 * (similar to Marco Wiering uses the number of arrived/crossed roadusers for calculating the node statistics) 
	 * For example: if the total waiting roadusers in the queue of this lane (so far) = 100 roadusers
	 * And the curTimeStep = 50, then the average waiting roadusers = 2 waiting roadusers per timeStep
	 * If the total waiting roadusers in the queue of this lane in this time step = 10 roadusers
	 * And the curTimeStep = 51, then the new average waiting roadusers = [(2*50) + 10]/51=2.156 roadusers per timeStep */
	protected int curTimeStep;

	/** EJUST: Position update time step*/
	protected static final double dt = TIMESTEP_S;
	
	/** EJUST: All colearning statistics of this Drivelane:
	 * Average Colearn Trip Waiting Time 
	 * Average Colearn Trip Time
	 * Average Colearn Average Speed
	 * Average Colearn Junction Waiting Time
	 * */
	protected DrivelaneColearnStatistics[] colearnStatistics;
	
	/** EJUST: All speed statistics of this Drivelane:
	 * Total Distance 
	 * Total Time
	 * */
	protected DrivelaneSpeedStatistics[] speedStatistics;
	
	public Drivelane(Drivelane lane)
	{
		road = lane.getRoad();
		sign = lane.getSign();
		
		timeStepAsked = lane.getTimeStepAsked();
		timeStepMoved = lane.getTimeStepMoved();
		
		Id = lane.getId();
		type = lane.getType();
		
		targets = lane.getTargets();
		
		laneType = DrivelaneFactory.DRIVELANE;
		queue = new LinkedList();
		
		//GASTON
		initStats(); /*Added by EJUST*/
		
		//EJUST
		initColearnStats();
		initSpeedStats();
	}

	public Drivelane(Road _road) {
		road = _road;
		sign = null;
		timeStepAsked = -1;
		timeStepMoved = -1;
		Id = -1;
		type = RoaduserFactory.getTypeByDesc("Automobiles");
		queue = new LinkedList();

		//GASTON
		initStats();
		
		//EJUST
		initColearnStats();
		initSpeedStats();
	}

	/** Empty constructor for loading */
	public Drivelane() {

		//GASTON
		initStats();
		
		//EJUST
		initColearnStats();
		initSpeedStats();
	}

	/*============================================*/
	/* Basic GET and SET methods                  */
	/*============================================*/


	/** Returns the Id of this drivelane. */
	public int getId() {
		return Id;
	}

	/** Sets the Id of this drivelane. */
	public void setId(int newid) {
		Id = newid;
	}

	/** Returns the queue of this Drivelane */
	public LinkedList getQueue() {
		return queue;
	}

	/** Sets the queue of this Drivelane */
	public void setQueue(LinkedList q) {
		queue = q;
	}

	/** Returns the type of this Drivelane */
	public int getType() {
		return type;
	}

	/** Sets the type of this Drivelane */
	public void setType(int t) {
		type = t;
	}

	/** Returns the Road that this Drivelane belongs to */
	public Road getRoad() {
		return road;
	}

	/** Sets the Road that this Drivelane belongs to */
	public void setRoad(Road r) {
		road = r;
	}

	/** Returns the Sign that regulates the traffic on this Drivelane */
	public Sign getSign() {
		return sign;
	}

	/** Sets the Sign that regulates the traffic on this Drivelane */
	public void setSign(Sign s) {
		sign = s;
	}

	/** Returns the roads users can move to when crossing the Node this lane leads to */
	public boolean[] getTargets() {
		return targets;
	}

	/** Sets the roads users can move to when crossing the Node this lane leads to */
	public void setTargets(boolean[] r) {
		targets = r;
	}

	/** Return the last timeStep this Drivelane has moved its Roadusers */
	public int getTimeStepMoved() {
		return timeStepMoved;
	}

	/** Sets the last timeStep this Drivelane has moved its Roadusers */
	public void setTimeStepMoved(int timeStep) {
		timeStepMoved = timeStep;
	}

	/** Return the last timeStep this Drivelane was last asked about the movements of its Roadusers */
	public int getTimeStepAsked() {
		return timeStepAsked;
	}

	/** Sets the last timeStep this Drivelane was last asked about the movements of its Roadusers */
	public void setTimeStepAsked(int timeStep) {
		timeStepAsked = timeStep;
	}

	/** Returns the length of the tail of this Drivelane */
	public int getTailLength() {
		if (getNodeComesFrom() instanceof Junction) {
			return getNodeComesFrom().getWidth()*10; // Nice and easy
			/*EJUST: According to SBC update to the block/road lengths, we multiply by 10*/
		}
		else {
			return 0;
		}
	}

	/** Returns the name of this drivelane. It is unique, and only used by the GUI for decoration. */
	public String getName() {
		return "Drivelane " + Id;
	}

	/** Returns the length of this Drivelane including tail*/
	public int getCompleteLength() {
		return getLength() + getTailLength();
	}

	//SBC
	/** Returns the maximum allowed speed on this lane, which is specified in road. */
	public double getSpeedMaxAllowed() { return road.getSpeedMaxAllowed(); } /*EJUST: int --> double*/

	//SBC
	/** Returns if the lane is on an primary road */
	public boolean isPrimary() { return road.isPrimary(); }

	public double getFreeUnitsInFront(Roaduser ru) { /*EJUST: int --> double*/
		double posroom = ru.getPosition(); /*EJUST: int --> double*/ 
		ListIterator li = queue.listIterator();
		Roaduser r = null;
		while (li.hasNext()) {
			r = (Roaduser) li.next();
			if (r == ru) {
				break;
			}
			posroom -= r.getLength();
		}
		return posroom;
	}

	/**Returns the road user that is on front of the first road user
	 * @param ru The road user at back
	 * @return frontRoadUser
	 * @author EJUST*/
	public Roaduser getFrontRoaduserOfFirstRoaduser(Roaduser ru) 
	{		
		
		//System.out.println("Roaduser position: " + ru.getPosition());
		
		Roaduser frontRoaduser = new CustomRoaduser();

		try 
		{
			int front_length, des_lane_complete_length;
			double front_pos;
			
			DrivingPolicy dp = SimModel.getDrivingPolicy();
			/* If the driving policy is changed from SimController, 
			 * it will be reflected in SimModel and consequently in Drivelane.getFrontRoaduserOfFirstRoaduser()*/
			
			Node node = getSign().getNode();
			
			Drivelane destLane = dp.getDirection(ru, this, node);
			/* The shortest path driving policy gets the subset between all available outbound lanes supporting roadusers 
			 * of the given type that can be reached from the given inbound lane and 
			 * all the lanes which are in one of the shortest paths to the car's destination, 
			 * then choose a random next lane from the result subset.
			 * 
			 * The colearning driving policy gets the shortest path that has the minimum V(s): 
			 * Expected waiting time before all traffic lights met by the vehicle before exit the city.
			 * 
			 * Note that both of the two driving policies give an "estimate" shortest path in the current time step, 
			 * while when the vehicle is in the situation of taking the decision, 
			 * it may take another path due to a possible change in the randomly chosen shortest path
			 * or due to an updated minimum value for V(s).
			 * */

			// destLane==null  means no available lanes lead from this lane, then this lane leads to an edge node
			if (destLane==null /*lanesleadingfrom.length = 0*/){ 
				//System.out.println("No Next Lane.");
				return null;
			}
			
			if (getSign().getType()==Sign.TRAFFICLIGHT && !getSign().getState()) 
			{				
				//System.out.println("Light is red.");
				frontRoaduser.setSpeed(0);  //The red light has no speed
				frontRoaduser.setPosition(0);
				((CustomRoaduser)frontRoaduser).setLength(0);
			}
			else
			{ 
				// light is green
				// check if it can cross junction and if there is place left. 
				// if the road user cannot cross the junction because of no place on destination lane, it has to stop

				LinkedList destqueue = destLane.getQueue();
				des_lane_complete_length = destLane.getCompleteLength();

				//System.out.println("Next lane complete length: " + des_lane_complete_length);
				
				if (destqueue.isEmpty()) 
				{		
					//System.out.println("Light is Green and Next Lane is Empty");													
					frontRoaduser = destLane.getFrontRoaduserOfFirstRoaduser(ru);
					if (frontRoaduser != null)
						frontRoaduser.setPosition(frontRoaduser.getPosition()-des_lane_complete_length);
				}
				else
				{
					//System.out.println("Light is Green and Next Lane is Not Empty");
					Roaduser front_ru = (Roaduser) destqueue.getLast();					
					front_length = front_ru.getLength();
					front_pos = front_ru.getPosition();

				//	if ( ru.getPosition() < ru.get_v0()*dt &&
				//			front_pos + front_length > des_lane_complete_length + ru.getPosition() - ru.getSpeed()*dt){  	
						/*
						 * e.g. If the current road user position = 0 in the back lane
						 * And the last road user position in the destination lane = 125 (+length 125, 126, 127)
						 * And the destination lane length = 100 
						 * Destination lane complete length (plus tail) = 100 + 4*10 = 140
						 * Then the last road user relative position in the destination lane after 1 time step 
						 * (140 + 0 - 14) = 126
						 * Then fron_pos + fron_length = 127 > 126
						 * */
				//		frontRoaduser.setSpeed(0); 
				//		frontRoaduser.setPosition(0);
				//		((CustomRoaduser)frontRoaduser).setLength(0);
				//	}
				//	else {
						frontRoaduser = (Roaduser) front_ru.clone();	
						frontRoaduser.setPosition(front_pos-des_lane_complete_length);
						
						/* Get the last road user position in the destination lane relative to the current road user position
						 * If the last road user position in the destination lane = 125 (+length 125, 126, 127)
						 * And the destination lane length = 100
						 * Destination lane complete length (plus tail) = 100 + 4*10 = 140 
						 * Then the last road user relative position in the destination lane = 125-140 = -15
						 */
				//	}
				}
			}	
			
			//if (frontRoaduser != null)
			//	System.out.println("Front roaduser relative position: " + frontRoaduser.getPosition());
			//else
			//	System.out.println("Front roaduser is null");
			
			//System.out.println("##################################");
		}
		catch (InfraException e) 
		{
			System.out.println(e.getMessage());
		}
        	
		return frontRoaduser;
	}

	/** Returns the road user that is on front of the given road user
	 * @param ru The road user at back
	 * @return frontRoadUser
	 * @author EJUST*/
	public Roaduser getFrontRoaduserOnNewLane(Roaduser ru) {		
		
		double ruPosition = ru.getPosition(); 
		int ruLength = ru.getLength();
		ListIterator li = queue.listIterator();
		Roaduser frontRoaduser = null;
		boolean flag = false;
		
		while (li.hasPrevious()) {
			frontRoaduser = (Roaduser) li.previous();
			if (frontRoaduser.getPosition() < ruPosition + ruLength){
				flag = true;
				
				/* No need to check overlap using the following condition:
				 * 		if (frontRoaduser.getPosition() + frontRoaduser.getLength() > ruPosition) frontRoaduser = null; 
				 * 
				 * That is because we already checked overlap in SimModel.checkDrivelane() using the following condition:
				 * 		//Is the position free?
				 * 		if (!testLane.isPosFree(ru.getPosition(),ru.getLength())) return false;
				 */
				
				break;
			}
		}
		if (flag == true) 
			return frontRoaduser;

		return getFrontRoaduserOfFirstRoaduser(ru);
	}

	/**Returns the road user that is at back of the last road user
	 * @param ru The road user at back
	 * @return backRoadUser
	 * @author EJUST*/
	public Roaduser getBackRoaduserOfLastRoaduser(Roaduser ru) {
		
		Roaduser backRoaduser = new CustomRoaduser(null, null, -1, ru.model(), null);		

		try {
			int ru_type, ru_des;
			DrivingPolicy dp = SimModel.getDrivingPolicy();
			Node node = getNodeComesFrom(); //Returns the Node that this Drivelane comes from 
			ru_type = ru.getType();
			ru_des = ru.getDestNode().getId();

			//Returns an array of all available inbound lanes supporting roadusers of given type that lead to the given outbound lane.
			Drivelane[] lanesleadingTo = node.getAvailableLanesLeadingTo(this, ru_type);
						
			//lanesleadingTo.length = 0 mean no available lanes lead to this lane, then this lane come from an edge node
			if (lanesleadingTo.length ==0) return null;					 
							
			Drivelane[] shortestpaths = new Drivelane[lanesleadingTo.length];
			ArrayList<Node> nodesComeFrom = new ArrayList<Node>();
			
			for (int i=0;i<lanesleadingTo.length;i++){
				if (nodesComeFrom.contains(lanesleadingTo[i].getNodeComesFrom())) break;
				nodesComeFrom.add(lanesleadingTo[i].getNodeComesFrom());
			}
			
			int cnt = 0;
			for (int i=0; i<nodesComeFrom.size(); i++){
				Drivelane[] nodeComesFromShortestpaths=nodesComeFrom.get(i).getShortestPaths(ru_des, ru_type);
			    for(int j = 0; j < nodeComesFromShortestpaths.length; j++){
			    		shortestpaths[cnt] = nodeComesFromShortestpaths[j];
                        cnt++;
                }
            }
			
			Drivelane sourceLane = dp.getDirectionLane(ru, this, lanesleadingTo, shortestpaths);
				
			if (sourceLane.sign.getType()==Sign.TRAFFICLIGHT && !sourceLane.sign.getState()) {
				backRoaduser.setSpeed(0); //The red light has no speed
				backRoaduser.setPosition(getLength()-1); //The last position in the new lane
				((CustomRoaduser)backRoaduser).setLength(0);
			}
			else{
				// Previous sign light is green
				// check if back road user can cross junction and if there is place left in this new lane. 
				// if the back road user cannot cross the junction because of no place on the new lane, it has to stop
	
				int source_lane_complete_length, current_lane_complete_length;
				double back_pos;
				LinkedList sourceQueue = sourceLane.getQueue();
				source_lane_complete_length = sourceLane.getCompleteLength();
				current_lane_complete_length = getCompleteLength();
				if (sourceQueue.isEmpty()) {
					backRoaduser.setSpeed(0); 
					backRoaduser.setPosition(source_lane_complete_length+current_lane_complete_length-ru.getPosition());
					((CustomRoaduser)backRoaduser).setLength(0);
				}
				else{
					Roaduser back_ru = (Roaduser)sourceQueue.getFirst();
					back_pos = back_ru.getPosition();
		
					//	if ( back_pos < back_ru.get_v0()*dt && 
					//			ru.getPosition() + ru.getLength() > current_lane_complete_length+back_pos-back_ru.getSpeed()*dt){  	
							/*
							 * e.g. If the road user position = 136 (+length 136, 137, 138) in the current lane
							 * And the first road user position in the source lane = 12
							 * And the current lane length = 100 
							 * Current lane complete length (plus tail) = 100 + 4*10 = 140
							 * Then the first road user relative position in the current lane after 1 time step 
							 * (140 + 12 - 14) = 138
							 * Then ru.getPosition() + ru.getLength() = 136 + 3 > 138 
							 * */
					//		backRoaduser.setSpeed(0); 
					//		backRoaduser.setPosition(current_lane_complete_length);//The last position in the current lane
					//		((CustomRoaduser)backRoaduser).setLength(0);
					//	}
					//	else {
							backRoaduser = (Roaduser) back_ru.clone();	
							backRoaduser.setPosition(back_pos+current_lane_complete_length);
							/* Get the first road user in the source lane position relative to the road user position in the current lane 
							 * e.g. If the first road user position in the source lane = 12
							 * And the current lane length = 100 
							 * Current lane complete length (plus tail) = 100 + 4*10 = 140
							 * Then the first road user relative position in the current lane = (12 + 140) = 152
							 */
					//	}
					}
				}						
				//System.out.println("backRoaduser position: " + backRoaduser.getPosition());
			}
		catch (Exception e) {						
				System.out.println(e.getMessage());
		}							
		return backRoaduser;
	}

	/**EJUST
	 * Returns the road user that is at back of the given road user
	 * @param ru The road user on front
	 * @return backRoadUser*/
	public Roaduser getBackRoaduserOnNewLane(Roaduser ru) {
		double ruPosition = ru.getPosition();
		int ruLength = ru.getLength();
		ListIterator li = queue.listIterator();
		Roaduser backRoaduser = null;
		boolean flag = false;
		while (li.hasNext()) {
			backRoaduser = (Roaduser) li.next();
			if (backRoaduser.getPosition() + backRoaduser.getLength() > ruPosition){
				flag = true;
				
				/* No need to check overlap using the following condition:
				 * 		if (backRoaduser.getPosition() < ruPosition + ruLength) backRoaduser = null; 
				 * 
				 * That is because we already checked overlap in SimModel.checkDrivelane() using the following condition:
				 * 		//Is the position free?
				 * 		if (!testLane.isPosFree(ru.getPosition(),ru.getLength())) return false;
				 */
				
				break;
			}
		}

		if (flag == true) 
			return backRoaduser;

		return getBackRoaduserOfLastRoaduser(ru);
	}


	
	/*============================================*/
	/* Selectable                                 */
	/*============================================*/

	public Rectangle getBounds() {
		return getComplexBounds().getBounds();
	}

	public Shape getComplexBounds() {
		Area a = new Area();
		if (bounds != null) {
			for (int i = 0; i < bounds.length; i++) {
				a.add(new Area(bounds[i]));
			}
		}
		return a;
	}

	public int getDistance(Point p) {
		return (int) getCenterPoint().distance(p);
	}

	public Point getSelectionPoint() {
		return getCenterPoint();
	}

	public Point[] getSelectionPoints() {
		return null;
	}

	public Point getCenterPoint() {
		Rectangle r = getBounds();
		return new Point(r.x + r.width / 2, r.y + r.height / 2);
	}

	public boolean isSelectable() {
		return true;
	}

	public boolean hasChildren() {
		return false;
	}

	public Enumeration getChildren() {
		return new ListEnumeration(queue);
	}

	/*============================================*/
	/* LOAD and SAVE                              */
	/*============================================*/

	public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException {
		
		Id = myElement.getAttribute("id").getIntValue();
		timeStepMoved = myElement.getAttribute("timeStep-moved").getIntValue();
		timeStepAsked = myElement.getAttribute("timeStep-asked").getIntValue();
		type = myElement.getAttribute("type").getIntValue();
		loadData.roadId = myElement.getAttribute("road-id").getIntValue();
		
		/*EJUST*/
		if (Model.SAVE_STATS)
		{
			colearnStatistics = (DrivelaneColearnStatistics[]) XMLArray.loadArray(this, loader, (InstantiationAssistant)this);
			speedStatistics = (DrivelaneSpeedStatistics[]) XMLArray.loadArray(this, loader, (InstantiationAssistant)this);
			statistics = (DrivelaneStatistics) XMLArray.loadArray(this, loader, (InstantiationAssistant)this);
		}
		else
		{
			initColearnStats();
			initSpeedStats();
			initStats();
		}
		
		targets = (boolean[]) XMLArray.loadArray(this, loader);

		if (loader.getNextElementName().equals("sign-tl")) {
			sign = new TrafficLight();
		}
		else if (loader.getNextElementName().equals("sign-no")) {
			sign = new NoSign();
		}
		else {
			throw new XMLInvalidInputException ("A drivelane in road " + loadData.roadId + " couldn't load its sign. No sign element found.");
		}
		loader.load(this, sign);
		sign.setLane(this);
		queue = (LinkedList) XMLArray.loadArray(this, loader);
	}

	/* POMDPGLD: XML saver, doesnt save any extra data than original, in PO, there will be an extra element,
    old files will thus be loaded as Fully observable, the way they were intended to load*/
	public XMLElement saveSelf() throws XMLCannotSaveException {
		XMLElement result = new XMLElement("lane");
		result.addAttribute(new XMLAttribute("id", Id));
		result.addAttribute(new XMLAttribute("timeStep-moved", timeStepMoved));
		result.addAttribute(new XMLAttribute("timeStep-asked", timeStepAsked));
		result.addAttribute(new XMLAttribute("road-id", road.getId()));
		result.addAttribute(new XMLAttribute("type", type));
		return result;
	}

	public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException {
		
		/*EJUST*/
		if (Model.SAVE_STATS)
		{
			XMLArray.saveArray(colearnStatistics, this, saver, "colearn-statistics");
			XMLArray.saveArray(speedStatistics, this, saver, "speed-statistics");
			XMLArray.saveArray(statistics, this, saver, "statistics");
		}
		
		XMLArray.saveArray(targets, this, saver, "targets");
		
		saver.saveObject(sign);
		
		XMLUtils.setParentName(new ListEnumeration(queue), getXMLName());
		
		XMLArray.saveArray(queue, this, saver, "queue");
	}

	public String getXMLName() {
		return parentName + ".lane";
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	
	class TwoStageLoaderData {
		int roadId;
	}

	public void loadSecondStage(Dictionary dictionaries) throws XMLInvalidInputException, XMLTreeException {
		road = (Road)(((Dictionary) (dictionaries.get("road"))).get(new Integer(loadData.roadId)));
		sign.loadSecondStage(dictionaries);
		XMLUtils.loadSecondStage(new ListEnumeration(queue), dictionaries);
	}
	
	/*============================================*/
	/* MODIFYING DATA                             */
	/*============================================*/


	/**
	 * Resets this Drivelane.
	 * This will remove all Roadusers on this lane,
	 * reset the timeStepMoved and timeStepAsked counters,
	 * and reset the sign.
	 * @see Sign#reset()
	 */
	public void reset() {
		//System.out.println("Resetting lane " + Id);
		resetTargets();
		
		//EJUST:
		resetStats();
		
		//EJUST:
		resetDrivelaneColearnStats();
		resetDrivelaneSpeedStats();
		
		queue = new LinkedList();
		timeStepMoved = -1;
		timeStepAsked = -1;
		sign.reset();
	}

	/*POMDPGLD: private --> public*/
	public void resetTargets() {
		for (int i = 0; i < targetsOriginal.length; i++)
		{
			if (targetsOriginal[i] > -1) {
				targets[i] = (targetsOriginal[i] == 0)? false: true ;
				targetsOriginal[i] = -1;
			}
		}
	}

	/**
	 * Adds a Roaduser at the end of this lane
	 *
	 * @param ru The roaduser to add
	 * @throws InfraException if the roaduser could not be added
	 */
	public void addRoaduserAtEnd(Roaduser ru) throws InfraException {
		double pos = getCompleteLength() - ru.getLength(); /*EJUST: int --> double*/
		addRoaduserAtEnd(ru, pos);
	}

	/**
	 * Adds a Roaduser at the end of this lane
	 *
	 * @param ru The roaduser to add
	 * @param pos The position where the roadusers should be added
	 * @throws InfraException if the roaduser could not be added
	 */
	public void addRoaduserAtEnd(Roaduser ru, double pos /*EJUST: int --> double*/) throws InfraException {
		if (!queue.isEmpty()) {
			Roaduser last = (Roaduser) queue.getLast();
			if (last.getPosition() + last.getLength() <= pos) {
				ru.setPosition(pos);
				queue.addLast(ru);
				return;
			}
			else {
				throw new InfraException("Position taken.");
			}
		}
		ru.setPosition(pos);
		queue.addLast(ru);
	}

	/**
	 * Adds a Roaduser at a given position to the lane
	 *
	 * @param ru The roaduser to add
	 * @param pos The position at which to add the roaduser
	 * @throws InfraException if the position is taken by another roaduser
	 */
	public void addRoaduser(Roaduser ru, double pos /*EJUST: int --> double*/) throws InfraException { 
		if (!queue.isEmpty()) {
			ListIterator li = queue.listIterator();
			Roaduser r = null;
			while (li.hasNext()) {
				
				r = (Roaduser) li.next();
				
				if (r.getPosition() <= pos && r.getLength() + r.getPosition() > pos) {
					throw new InfraException("Position taken");
				}
				
				if (r.getPosition() > pos) {
					if (ru.getLength() > r.getPosition() - pos) {
						throw new InfraException("Position taken");
					}
					li.add(ru);
					break;
				}
			}
			if (pos >= r.getPosition() + r.getLength()) {
				queue.addLast(ru);
			}
		}
		else {
			queue.addLast(ru);
		}
		ru.setPosition(pos);
	}

	/**
	 * Removes Roaduser at start of this lane
	 *
	 * @return The roaduser removed from the queue
	 * @throw InfraException if there are no roadusers on this lane
	 */
	public Roaduser remRoaduserAtStart() throws InfraException {
		if (queue.isEmpty()) {
			throw new InfraException("No roaduser to remove");
		}
		Roaduser ru = (Roaduser) queue.removeFirst();
		//if (ru.getPosition() != 0) throw new InfraException("First Roaduser not at start of lane");
		ru.setPosition( -1);
		return ru;
	}

	/*============================================*/
	/* COMPLEX GET                                */
	/*============================================*/


	/** Returns an array of primitive Roaduser types that may roam this Drivelane */
	public int[] getTypes() {
		return Typeutils.getTypes(type);
	}

	/** Returns if a Roaduser of type ruType may use this Drivelane */
	public boolean mayUse(int ruType) {
		return (type & ruType) == ruType;
	}

	/** Returns the length of the Road of this Drivelane */
	public int getLength() {
		return road.getLength();
	}

	/** Returns the first Roaduser on this Drivelane */
	public Roaduser getFirstRoaduser() {
		return (Roaduser) queue.getFirst();
	}

	/** Returns the number of Roadusers that are waiting for the Sign of this Drivelane */
	public int getNumRoadusersWaiting() {
		/* old
	       ListIterator li = queue.listIterator();
	       Roaduser ru = null;
	       int pos = 0;
	       int ru_pos;
	       int count = 0;
	       while (li.hasNext()) {
		     ru = (Roaduser) li.next();
		     ru_pos = ru.getPosition();
		     if (ru_pos > pos) return count;
		     else if (ru_pos == pos) {
		      pos += ru.getLength();
		      count++;
	     	}
       }*/

		ListIterator li = queue.listIterator();
		Roaduser ru = null;
		double pos = 0; /*EJUST: int --> double*/
		double ru_pos; /*EJUST: int --> double*/
		int count = 0;
		double cnt_step = 0; /*EJUST: int --> double*/		
		
		while (li.hasNext()) 
		{
			ru = (Roaduser) li.next();
			
			/* EJUST commented
			 * ru_pos = ru.getPosition();

			// was:
			// if(ru_pos > pos) return count;
			// nu: waar ru terecht kan komen, moet nog rekening worden gehouden met inloop vakjes
			// EJUST translation: where you can come, should still be considered walking vehicles 
			if (ru_pos - ru.getSpeed()*dt > pos - cnt_step) {
				return count; // Wont be able to wait.
			}
			
			else if (ru_pos - ru.getSpeed()*dt <= pos - cnt_step) {
				cnt_step += ru_pos - pos; // The free blocks ahead of ru, if everyone moves on.
				pos = ru_pos + ru.getLength();
				count++;
			}
			*/
			
			/*EJUST*/
			if(ru.getSpeed()>STOP_SPEED_MS /*EJUST replaced: ru.getSpeed>0*/) 
				return count;
			else count++;
		}
		return count;
	}

	/* !! Klopt geen fuck van -alert !! */

	/** Returns if this ru is waiting*/
	public boolean updateWaitingPosition(Roaduser current_ru) {		
		ListIterator li = queue.listIterator();
		Roaduser ru = null;
		double pos = 0; /*EJUST: int --> double*/
		double ru_pos; /*EJUST: int --> double*/
		int count = 0;
		double cnt_step = 0; /*EJUST: int --> double*/
		
		while (li.hasNext()) {
			ru = (Roaduser) li.next();
			
			/* EJUST commented
			if (ru.equals(current_ru)) {
				ru_pos = ru.getPosition();

				if (ru_pos - ru.getSpeed()*dt > pos - cnt_step) {
					return false; // Wont be able to wait.
				}
				else if (ru_pos - ru.getSpeed()*dt <= pos - cnt_step) {
					cnt_step += ru_pos - pos; // The free blocks ahead of ru, if everyone moves on.
					pos = ru_pos + ru.getLength();
					
					//Deze wacht dus dus geef zijn huidige wacht positie aan
					 if the ru is waiting, update the waiting position of this RoadUser  
					 * ru.setLastWaitPointPos(ru_pos);
					 * ru.setLastWaitPointTl(sign);
					
					return true;
				}
			}*/
			
			if(ru.getSpeed()<=STOP_SPEED_MS /*EJUST replaced: ru.getSpeed==0*/){
				if (ru.equals(current_ru)) 
					return true;
			}
			else return false;
						
		}
		return false;
	}

	/** Returns the number of Passengers in the Roadusers that are waiting for the Sign of this Drivelane */
	public int getNumPassengersWaiting() {
		ListIterator li = queue.listIterator();
		Roaduser ru = null;
		double pos = 0; /*EJUST: int --> double*/
		double ru_pos;	/*EJUST: int --> double*/
		int count = 0;
		double cnt_step = 0; /*EJUST: int --> double*/
		
		while (li.hasNext()) {
			ru = (Roaduser) li.next();
			
			/* EJUST commented
			ru_pos = ru.getPosition();
			
			if (ru_pos - ru.getSpeed()*dt > pos - cnt_step) {
				return count; // Wont be able to wait.
			}
			else if (ru_pos - ru.getSpeed()*dt <= pos - cnt_step) {
				cnt_step += ru_pos - pos; // The free blocks ahead of ru, if everyone moves on.
				pos = ru_pos + ru.getLength();
				count += ru.getNumPassengers();
			}*/
			
			if(ru.getSpeed()>STOP_SPEED_MS /*EJUST replaced: ru.getSpeed>0*/) 
				return count;
			else count+= ru.getNumPassengers();
		}
		return count;
	}

	/** Returns the number of blocks taken by Roadusers that are waiting for the Sign of this Drivelane */
	public int getNumBlocksWaiting() {
		ListIterator li = queue.listIterator();
		Roaduser ru = null;
		double pos = 0; /*EJUST: int --> double*/
		double ru_pos;	/*EJUST: int --> double*/
		int count = 0;
		double cnt_step = 0; /*EJUST: int --> double*/
		
		while (li.hasNext()) {
			ru = (Roaduser) li.next();
			
			/* EJUST commented
			ru_pos = ru.getPosition();
			
			if (ru_pos - ru.getSpeed()*dt > pos - cnt_step) 
			{
				return count; // Wont be able to wait.
			}
			else if (ru_pos - ru.getSpeed()*dt <= pos - cnt_step) 
			{
				cnt_step += ru_pos - pos; // The free blocks ahead of ru, if everyone moves on.
				pos = ru_pos + ru.getLength();
				count += ru.getLength();
			}*/
			
			if(ru.getSpeed()>STOP_SPEED_MS /*EJUST replaced: ru.getSpeed>0*/) 
				return count;
			else count+= ru.getLength();
		}
		return count;
	}

	/** Returns the number of blocks taken by Roadusers on this Drivelane */
	public int getNumBlocksTaken() {
		ListIterator li = queue.listIterator();
		Roaduser ru = null;
		int pos = 0;
		int ru_pos;
		int count = 0;
		while (li.hasNext()) {
			ru = (Roaduser) li.next();
			count += ru.getLength();
		}
		return count;
	}

	/** Returns the Node that this Drivelane comes from */
	public Node getNodeComesFrom() {
		if (road.getAlphaNode() == getNodeLeadsTo()) {
			return road.getBetaNode();
		}
		else {
			return road.getAlphaNode();
		}
	}

	/** Returns Node that this Drivelane leads to */
	public Node getNodeLeadsTo() {
		return sign.getNode();
	}

	/** Returns the state of given target */
	public boolean getTarget(int target) throws InfraException {
		if (target < 0 || target > 2) {
			throw new InfraException("Target out of range");
		}
		return targets[target];
	}

	/** Sets the state of given target */
	public void setTarget(int target, boolean state) throws InfraException {
		if (target < 0 || target > 2) {
			throw new InfraException("Target out of range");
		}
		targets[target] = state;
	}

	/** Converts the lane to another type if nessecary **/

	/**
	 * Sets the states of a given target, recover us used to set an alternate config with false,
	 * or by restoring the default value (DOAS 05)
	 *
	 */
	public void setTarget(int target, boolean state, boolean recover) throws InfraException {
		int oState = (getTarget(target))? 1: 0;
		if (recover == false) {
			if(targetsOriginal[target] == -1)
				targetsOriginal[target] = oState;
			setTarget(target, state);
		}
		else if (targetsOriginal[target] > -1){
			boolean bState = (targetsOriginal[target] == 0)? false : true;
			setTarget(target, bState);
			targetsOriginal[target] = -1;
		}
	}

	/**
	 * Checks whether length blocks from the given position are free.
	 *
	 * @param position The position in the Queue of this Drivelane.
	 * @param length The amount of blocks that need to be free.
	 */
	public boolean isPosFree(double position, int length) { /*EJUST: int --> double*/
		ListIterator li = queue.listIterator();
		Roaduser ru = null;
		double rupos; /*EJUST: int --> double*/
		int rulen;

		while (li.hasNext()) {
			ru = (Roaduser) li.next();
			rupos = ru.getPosition();
			rulen = ru.getLength();

			if (rupos > position + length) {
				return true;
			}
			else if (rupos + rulen >= position) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks whether length blocks from the given position, before the given roaduser, are free.
	 *
	 * @param position The position in the Queue of this Drivelane
	 * @param length The amount of blocks that need to be free.
	 * @param me The roaduser
	 * @return whether or not the requested position and <code>length</code> blocks are free for the supplied RU
	 */
	public boolean isPosFree(int position, int length, Roaduser me) {
		ListIterator li = queue.listIterator();
		Roaduser ru = null;
		double rupos; /*EJUST: int --> double*/
		int rulen;

		while (li.hasNext()) {
			ru = (Roaduser) li.next();
			if (ru != me) {
				rupos = ru.getPosition();
				rulen = ru.getLength();

				if (rupos + rulen > position) {
					return false;
				}
				else if (rupos > position + length) {
					return true;
				}
			}
			else {
				return true;
			}
		}
		return true;
	}

	/** Returns whether or not the 'tail' of this Drivelane has any Roadusers on it. */
	public boolean isTailFree() {
		if (!queue.isEmpty()) {
			Roaduser ru = (Roaduser) queue.getLast();
			if (ru.getPosition() < getLength()) {
				return true;
			}
			else {
				return false;
			}
		}
		return true;
	}

	public boolean isLastPosFree(int length) {
		int qSize = queue.size();
		int dLength = getLength();

		if (qSize > 0) {
			ListIterator li = queue.listIterator();
			Roaduser ru = null;
			int desired_position = getLength() - length;
			int real_pos = getCompleteLength() - length;
			int dlSize = 0;

			while (li.hasNext()) {
				ru = (Roaduser) li.next();
				dlSize += ru.getLength();
				if (ru.getPosition() + ru.getLength() > real_pos) {
					return false;
				}
			}
			if (dlSize + length > getLength()) {
				// See if the current inhabitants already fill up the Drivelane
				return false;
			}
			else {
				// The current inhabitants and length fit on the drivelane
				// Furthermore, there is space on the tail (see return false in while{})
				return true;
			}
		}
		return true;
	}

	/**
	 * Returns true when this drivelane is completely full
	 * @ author Chaim Z
	 */
	public boolean isFull() {
		ListIterator li = queue.listIterator();
		Roaduser ru = null;
		int position = 0; 
		double rupos; /*EJUST: int --> double*/
		int rulen;

		// if empty return false
		if (!li.hasNext()) {
			return false;
		}

		while (li.hasNext()) {
			ru = (Roaduser) li.next();
			rupos = ru.getPosition();

			if (rupos > position) {
				return false;
			}

			rulen = ru.getLength();
			position += rulen;
		}
		return true;
	}

	/**
	 * Returns the best reacheable free position for the supplied Roaduser on the Queue given in the ListIterator
	 *
	 * @param li The Queue of this Drivelane represented in a ListIterator. li.previous() is the current RoadUser
	 * @param position The position on the Drivelane of the Roaduser
	 * @param length The amount of blocks that have to be free
	 * @param speed_left the number of 'moves' this Roaduser has left in this turn
	 * @param ru The Roaduser to be checked
	 * @return the number of blocks the Roaduser can move ahead
	 */
	public double getPosFree(ListIterator li, double position /*EJUST: int --> double*/, 
							int length, double speed_left /*EJUST: int --> double*/, Roaduser ru) 
	{
		
		//SBC, EJUST: int --> double
		double ru_stopdistance = 0; /*EJUST commented: ru.getStopDistance()*/
		
		/*EJUST: int --> double*/
		double best_pos = position;
		
		/*EJUST: int --> double*/
		double max_pos = position;
		
		/*EJUST: int --> double*/
		double target_pos =  (position - speed_left*dt > 0)	? position - speed_left*dt : 0; 

		// Previous should be 'ru'
		Roaduser prv = (Roaduser) li.previous();

		if (prv == ru && li.hasPrevious()) { // roaduser not first
			
			/* has car in front */
			prv = (Roaduser) li.previous();
			
			max_pos = prv.getPosition() + prv.getLength() + ru_stopdistance /*SBC: stopdistance support*/;
			
			if (max_pos < target_pos) 
			{
				best_pos = target_pos;
			}
			else 
			{
				best_pos = max_pos;
			}
			li.next();				
		}
		else {
			best_pos = target_pos;
		}

		li.next(); // Setting the ListIterator back in the position we got it like.

		if (best_pos != position){
			// The Roaduser can advance some positions
			return best_pos; /*EJUST: int --> double*/
		}
		else 
		{
			return 0;
		}
	}

	public LinkedList getCompleteQueue() {
		return queue;
	}

	/** Clears the bounds of this drivelane */
	public void clearCurveBounds() {
		bounds = null;
	}

	/** Adds new bounds to the bounds of this drivelane */
	public void addCurveBounds(Shape s) {
		if (bounds == null) {
			bounds = new Shape[1];
			bounds[0] = s;
		}
		else {
			bounds = (Shape[]) Arrayutils.addElement(bounds, s);
		}
	}

	/* Returns true if the given point is contained in this drivelane */
	public boolean contains(Point p) {
		if (bounds != null) {
			for (int i = 0; i < bounds.length; i++) {
				if (bounds[i].contains(p)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/** Initializes the colearn statistics for this drivelane. 
	 * @author EJUST*/
	public void initColearnStats()
	{
		int[] ruTypes = RoaduserFactory.getConcreteTypes();
		colearnStatistics = new DrivelaneColearnStatistics[ruTypes.length + 1];
		resetDrivelaneColearnStats();
	}

	/** Resets the colearn statistics for this drivelane. 
	 * @author EJUST*/
	public void resetDrivelaneColearnStats()
	{
		for (int i = 0; i < colearnStatistics.length; i++)
		{
			colearnStatistics[i] = new DrivelaneColearnStatistics();
		}
	}
	
	/** Processes the colearn trip statistics of a Roaduser moving on this drivelane: colearnTripWaitingTime, expectedTripTime, expectedDistance
	 * @param ru The Roaduser to process.
	 * @param timeStep The current timeStep.
	 * @param expectedTripWaitingTime The expected trip waiting time
	 * @param expectedTripTime The expected trip time
	 * @param expectedDistance The expected distance
	 * @author EJUST*/
	public void processColearnTripStatistics(Roaduser ru, int timeStep, double expectedTripWaitingTime, double expectedTripTime, double expectedDistance)
	{ 
		/*Calculates the expected trip waiting time of the road user that does not arrive to its destination edge node yet*/
		double colearnTripWaitingTime = ru.getWaitingTime()+expectedTripWaitingTime;
		
		/*Adds the engage queue time experienced by the road user in the current lane*/
		if (ru.getDrivelaneEngageQueueTime()!= -1)
			colearnTripWaitingTime += (timeStep-ru.getDrivelaneEngageQueueTime());
		
		/*Adds the colearn trip statistics for index zero: All roadusers types*/
		colearnStatistics[0].addRoaduserNotArrivedYet(colearnTripWaitingTime, expectedTripTime, expectedDistance); 
		
		/*Adds the colearn trip statistics for the index concerning the specific roaduser type*/
		colearnStatistics[RoaduserFactory.ruTypeToStatIndex(ru.getType())].addRoaduserNotArrivedYet(colearnTripWaitingTime, expectedTripTime, expectedDistance);
	}

	/** Processes the colearn junction waiting time statistics of a Roaduser moving on this drivelane.
	 * @param ru The Roaduser to process.
	 * @param timeStep The current timeStep.
	 * @param expectedJunctionWaitingTime The expected junction waiting time
	 * @author EJUST*/
	public void processColearnJunctionWaitingTime(Roaduser ru, int timeStep, double expectedJunctionWaitingTime)
	{ 		
		/*Calculates the expected junction waiting time of the road user that does not cross the current junction yet*/
		double colearnJunctionWaitingTime = expectedJunctionWaitingTime;
		
		/*Adds the engage queue time experienced by the road user in the current lane*/
		if (ru.getDrivelaneEngageQueueTime()!= -1)
			colearnJunctionWaitingTime += (timeStep-ru.getDrivelaneEngageQueueTime());
		
		/*Adds the colearn junction waiting time for index zero: All roadusers types*/
		colearnStatistics[0].addRoaduserNotCrossedYet(colearnJunctionWaitingTime); 
		
		/*Adds the colearn junction waiting time for the index concerning the specific roaduser type*/
		colearnStatistics[RoaduserFactory.ruTypeToStatIndex(ru.getType())].addRoaduserNotCrossedYet(colearnJunctionWaitingTime);
	}
	
	/** Basic Lane Colearn Statistics 
	 * @author EJUST*/
	public class DrivelaneColearnStatistics implements Cloneable, XMLSerializable
	{
		protected int roadusersNotArrivedYet, roadusersNotCrossedYet, tripTableIndex, junctionTableIndex;
		protected double avgColearnTripWaitingTime, avgExpectedTripTime, avgExpectedDistance, avgColearnJunctionWaitingTime;
		protected double[] colearnTripWaitingTimeTable, expectedTripTimeTable, expectedDistanceTable, colearnJunctionWaitingTimeTable;
		protected boolean tripTableFilled, junctionTableFilled;

		protected String parentName = "model.infrastructure.drivelane";

		/** Create an (initially empty) statistics data structure. */
		public DrivelaneColearnStatistics()
		{
			avgColearnTripWaitingTime = avgExpectedTripTime = avgExpectedDistance = avgColearnJunctionWaitingTime = 0;
			tripTableIndex = junctionTableIndex = roadusersNotArrivedYet = roadusersNotCrossedYet = 0;
			tripTableFilled = junctionTableFilled = false;
			
			colearnTripWaitingTimeTable = new double[STAT_NUM_DATA]; 
			expectedTripTimeTable = new double[STAT_NUM_DATA];
			expectedDistanceTable = new double[STAT_NUM_DATA];
			colearnJunctionWaitingTimeTable = new double[STAT_NUM_DATA];
		}

		/** Returns a clone of this DrivelaneColearnStatistics. */
		public DrivelaneColearnStatistics getClone()
		{
			DrivelaneColearnStatistics dls = null;
			try
			{
				dls = (DrivelaneColearnStatistics) clone();
				
				double[] owt = dls.getColearnTripWaitingTimeTable();
				double[] ott = dls.getExpectedTripTimeTable();
				double[] odt = dls.getExpectedDistanceTable();
				
				double[] dlwt = new double[owt.length]; 
				double[] dltt = new double[ott.length];
				double[] dldt = new double[odt.length];
				
				for (int i = 0; i < ott.length; i++)
				{
					dlwt[i] = owt[i];
					dltt[i] = ott[i];
					dldt[i] = odt[i];
				}
				
				dls.setColearnTripWaitingTimeTable(dlwt);
				dls.setExpectedTripTimeTable(dltt);
				dls.setExpectedDistanceTable(dldt);
				
				owt = dls.getColearnJunctionWaitingTimeTable();
				
				dlwt = new double[owt.length];
				
				for (int i = 0; i < owt.length; i++)
				{
					dlwt[i] = owt[i];
				}
				
				dls.setColearnJunctionWaitingTimeTable(dlwt);
			}
			catch (CloneNotSupportedException c)
			{}
			return dls;
		}

		/** Returns the colearn trip waiting time table. */
		private double[] getColearnTripWaitingTimeTable()
		{
			return colearnTripWaitingTimeTable;
		}

		/** Sets the colearn trip waiting time table. */
		private void setColearnTripWaitingTimeTable(double[] wt)
		{
			colearnTripWaitingTimeTable = wt;
		}

		/** Returns the expected trip time table. */
		private double[] getExpectedTripTimeTable()
		{
			return expectedTripTimeTable;
		}

		/** Sets the expected trip time table. */
		private void setExpectedTripTimeTable(double[] tt)
		{
			expectedTripTimeTable = tt;
		}
		
		/** Returns the expected distance table. */
		private double[] getExpectedDistanceTable()
		{
			return expectedDistanceTable;
		}

		/** Sets the expected distance table. */
		private void setExpectedDistanceTable(double[] dt)
		{
			expectedDistanceTable = dt;
		}
		
		/** Returns the colearn junction waiting time table. */
		private double[] getColearnJunctionWaitingTimeTable()
		{
			return colearnJunctionWaitingTimeTable;
		}

		/** Sets the colearn junction waiting time table. */
		private void setColearnJunctionWaitingTimeTable(double[] wt)
		{
			colearnJunctionWaitingTimeTable = wt;
		}
		
		/** Returns the total number of roadusers that move on this drivelane and not arrived to its destination edge node yet. */
		public int getTotalRoadusersNotArrivedYet()
		{
			return roadusersNotArrivedYet;
		}

		/** Returns the total number of roadusers that move on this drivelane and not crossed their current junction yet. */
		public int getTotalRoadusersNotCrossedYet()
		{
			return roadusersNotCrossedYet;
		}
		
		/**
		 * Returns the average colearn trip waiting time by roadusers.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 */
		public double getAvgColearnTripWaitingTime(boolean allTime)
		{
			if (allTime)
			{
				return avgColearnTripWaitingTime;
			}

			int stopIndex;
			double totalColearnTripWaitingTime = 0;
			if (tripTableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tripTableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalColearnTripWaitingTime += colearnTripWaitingTimeTable[i];
			}
			return stopIndex == 0 ? 0 : totalColearnTripWaitingTime / stopIndex;
		}
		
		/**
		 * Returns the average expected trip time by roadusers.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 */
		public double getAvgExpectedTripTime(boolean allTime)
		{
			if (allTime)
			{
				return avgExpectedTripTime;
			}

			int stopIndex;
			double totalExpectedTripTime = 0;
			if (tripTableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tripTableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalExpectedTripTime += expectedTripTimeTable[i];
			}
			return stopIndex == 0 ? 0 : totalExpectedTripTime / stopIndex;
		}
		
		/**
		 * Returns the average expected distance by roadusers.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 */
		public double getAvgExpectedDistance(boolean allTime)
		{
			if (allTime)
			{
				return avgExpectedDistance;
			}

			int stopIndex;
			double totalExpectedDistance = 0;
			if (tripTableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tripTableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalExpectedDistance += expectedDistanceTable[i];
			}
			return stopIndex == 0 ? 0 : totalExpectedDistance / stopIndex;
		}
		
		/**
		 * Returns the average colearn junction waiting time by roadusers.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 */
		public double getAvgColearnJunctionWaitingTime(boolean allTime)
		{
			if (allTime)
			{
				return avgColearnJunctionWaitingTime;
			}

			int stopIndex;
			double totalColearnJunctionWaitingTime = 0;
			if (junctionTableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = junctionTableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalColearnJunctionWaitingTime += colearnJunctionWaitingTimeTable[i];
			}
			return stopIndex == 0 ? 0 : totalColearnJunctionWaitingTime / stopIndex;
		}
		
		/**
		 * Add statistics for one roaduser.
		 * @param colearnTripWaitingTime The colearn trip waiting time of this roaduser to be logged
		 * @param expectedTripTime The expected trip time of this roaduser to be logged
		 * @param expectedDistance The expected distance of this roaduser to be logged
		 */
		public void addRoaduserNotArrivedYet(double colearnTripWaitingTime, double expectedTripTime, double expectedDistance)
		{
			roadusersNotArrivedYet++;
						
			colearnTripWaitingTime = colearnTripWaitingTime > 0 ? colearnTripWaitingTime : 0;
			colearnTripWaitingTimeTable[tripTableIndex] = colearnTripWaitingTime;
			
			expectedTripTime = expectedTripTime > 0 ? expectedTripTime : 0;
			expectedTripTimeTable[tripTableIndex] = expectedTripTime;
			
			expectedDistance = expectedDistance > 0 ? expectedDistance : 0;
			expectedDistanceTable[tripTableIndex] = expectedDistance;
			
			tripTableIndex++;
			if (tripTableIndex == STAT_NUM_DATA)
			{
				tripTableIndex = 0;
				tripTableFilled = true;
			}

			avgColearnTripWaitingTime = addToAverage(avgColearnTripWaitingTime, roadusersNotArrivedYet, colearnTripWaitingTime);
			avgExpectedTripTime = addToAverage(avgExpectedTripTime, roadusersNotArrivedYet, expectedTripTime);
			avgExpectedDistance = addToAverage(avgExpectedDistance, roadusersNotArrivedYet, expectedDistance);
		}

		/**
		 * Add statistics for one roaduser.
		 * @param colearnJunctionWaitingTime The colearn junction waiting time of this roaduser to be logged
		 */
		public void addRoaduserNotCrossedYet(double colearnJunctionWaitingTime)
		{
			roadusersNotCrossedYet++;

			colearnJunctionWaitingTime = colearnJunctionWaitingTime > 0 ? colearnJunctionWaitingTime : 0;
			colearnJunctionWaitingTimeTable[junctionTableIndex++] = colearnJunctionWaitingTime;
			
			if (junctionTableIndex == STAT_NUM_DATA)
			{
				junctionTableIndex = 0;
				junctionTableFilled = true;
			}

			avgColearnJunctionWaitingTime = addToAverage(avgColearnJunctionWaitingTime, roadusersNotCrossedYet, colearnJunctionWaitingTime);
		}
		
		/**
		 * Adds a certain value to an existing average.
		 * @param oldAvg The previous average.
		 * @param newNum The number of samples the new average is based on.
		 * @param value  The new sample to add to this average.
		 */
		private double addToAverage(double oldAvg, double newNum, double value)
		{
			double tmp = oldAvg * (newNum - 1);
			tmp += value;
			return tmp / newNum;
		}


		//// XMLSerializable implementation ///

		public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
		{
			roadusersNotArrivedYet = myElement.getAttribute("roadusers-not-arrived-yet").getIntValue();
			roadusersNotCrossedYet = myElement.getAttribute("roadusers-not-crossed-yet").getIntValue();
			
			avgColearnTripWaitingTime = myElement.getAttribute("avg-colearn-trip-waiting-time").getDoubleValue();
			avgExpectedTripTime = myElement.getAttribute("avg-expected-trip-time").getDoubleValue();
			avgExpectedDistance = myElement.getAttribute("avg-expected-distance").getDoubleValue();
			avgColearnJunctionWaitingTime = myElement.getAttribute("avg-colearn-junction-waiting-time").getDoubleValue();
			
			tripTableIndex = myElement.getAttribute("trip-table-index").getIntValue();
			junctionTableIndex = myElement.getAttribute("junction-table-index").getIntValue();
			
			tripTableFilled = myElement.getAttribute("trip-table-filled").getBoolValue();
			junctionTableFilled = myElement.getAttribute("junction-table-filled").getBoolValue();
			
			colearnTripWaitingTimeTable = (double[]) XMLArray.loadArray(this, loader);
			expectedTripTimeTable = (double[]) XMLArray.loadArray(this, loader);
			expectedDistanceTable = (double[]) XMLArray.loadArray(this, loader);
			colearnJunctionWaitingTimeTable = (double[]) XMLArray.loadArray(this, loader);
		}

		public XMLElement saveSelf() throws XMLCannotSaveException
		{
			XMLElement result = new XMLElement("drivelane-colearn-statistics");
			
			result.addAttribute(new XMLAttribute("roadusers-not-arrived-yet", roadusersNotArrivedYet));
			result.addAttribute(new XMLAttribute("roadusers-not-crossed-yet", roadusersNotCrossedYet));
			
			result.addAttribute(new XMLAttribute("avg-colearn-trip-waiting-time", avgColearnTripWaitingTime));			
			result.addAttribute(new XMLAttribute("avg-expected-trip-time", avgExpectedTripTime));
			result.addAttribute(new XMLAttribute("avg-expected-distance", avgExpectedDistance));
			result.addAttribute(new XMLAttribute("avg-colearn-junction-waiting-time", avgColearnJunctionWaitingTime));
			
			result.addAttribute(new XMLAttribute("trip-table-index", tripTableIndex));
			result.addAttribute(new XMLAttribute("junction-table-index", junctionTableIndex));
			
			result.addAttribute(new XMLAttribute("trip-table-filled", tripTableFilled));
			result.addAttribute(new XMLAttribute("junction-table-filled", junctionTableFilled));
			
			return result;
		}

		public void saveChilds(XMLSaver saver) throws XMLTreeException,	IOException, XMLCannotSaveException
		{
			XMLArray.saveArray(colearnTripWaitingTimeTable, this, saver, "colearn-trip-waiting-time-table");
			XMLArray.saveArray(expectedTripTimeTable, this, saver, "expected-trip-time-table");
			XMLArray.saveArray(expectedDistanceTable, this, saver, "expected-distance-table");
			XMLArray.saveArray(colearnJunctionWaitingTimeTable, this, saver, "colearn-junction-waiting-time-table");
		}

		public String getXMLName()
		{
			return parentName + ".drivelane-colearn-statistics";
		}

		public void setParentName(String parentName) throws XMLTreeException
		{
			this.parentName = parentName;
		}
	}
	
	/** Returns the colearn statistics for all types of roadusers. 
	 * @author EJUST*/
	public DrivelaneColearnStatistics[] getColearnStatistics()
	{
		return colearnStatistics;
	}

	/**
	 * Returns the colearn statistics for the given roaduser type.
	 * @param ruType The roaduser type to return statistics of. (0 if all roadusers)
	 * @author EJUST
	 */
	public DrivelaneColearnStatistics getDrivelaneColearnStatistics(int ruType)
	{
		return colearnStatistics[RoaduserFactory.ruTypeToStatIndex(ruType)];
	}
	
	/** Initializes the speed statistics for this drivelane. 
	 * @author EJUST*/
	public void initSpeedStats()
	{
		int[] ruTypes = RoaduserFactory.getConcreteTypes();
		speedStatistics = new DrivelaneSpeedStatistics[ruTypes.length + 1];
		resetDrivelaneSpeedStats();
	}

	/** Resets the speed statistics for this drivelane. 
	 * @author EJUST*/
	public void resetDrivelaneSpeedStats()
	{
		for (int i = 0; i < speedStatistics.length; i++)
		{
			speedStatistics[i] = new DrivelaneSpeedStatistics();
		}
	}
	
	/** Processes the speed statistics of a Roaduser moving on this drivelane
	 * @param ru The Roaduser to process.
	 * @param timeStep The current timeStep.
	 * @author EJUST*/
	public void processSpeedStatistics(Roaduser ru, int timeStep)
	{ 					
		/*Calculates the distance experienced by the roaduser that does not arrive to an edge node yet*/
		double distance = ru.getDistance();
		
		/*Calculates the time experienced by the roaduser that does not arrive to an edge node yet*/
		int time = (timeStep-ru.getTripStartTime());
		
		/*Adds the speed statistics for index zero: All roadusers types*/
		speedStatistics[0].addRoaduserNotArrivedYet(distance, time); 
		
		/*Adds the speed statistics for the index concerning the specific roaduser type*/
		speedStatistics[RoaduserFactory.ruTypeToStatIndex(ru.getType())].addRoaduserNotArrivedYet(distance, time);
	}
	
	/** Basic Lane Speed Statistics 
	 * @author EJUST*/
	public class DrivelaneSpeedStatistics implements Cloneable, XMLSerializable
	{
		protected int roadusersNotArrivedYet, speedTableIndex;
		protected float avgDistance, avgTime;
		protected double[] distanceTable;
		protected int[] timeTable;
		protected boolean speedTableFilled;

		protected String parentName = "model.infrastructure.drivelane";

		/** Create an (initially empty) statistics data structure. */
		public DrivelaneSpeedStatistics()
		{
			avgDistance = avgTime = 0;
			speedTableIndex = roadusersNotArrivedYet = 0;
			speedTableFilled = false;
			
			distanceTable = new double[STAT_NUM_DATA];
			timeTable = new int[STAT_NUM_DATA];
		}

		/** Returns a clone of this DrivelaneSpeedStatistics. */
		public DrivelaneSpeedStatistics getClone()
		{
			DrivelaneSpeedStatistics dls = null;
			try
			{
				dls = (DrivelaneSpeedStatistics) clone();
				
				int[] ott = dls.getTimeTable();
				double[] odist = dls.getDistanceTable();
				 
				int[] dltt = new int[ott.length];
				double[] dldist = new double[odist.length];
				
				for (int i = 0; i < ott.length; i++)
				{
					dltt[i] = ott[i];
					dldist[i] = odist[i];
				}
				
				dls.setTimeTable(dltt);
				dls.setDistanceTable(dldist);				
			}
			catch (CloneNotSupportedException c)
			{
				
			}
			return dls;
		}

		/** Returns the time table. */
		private int[] getTimeTable()
		{
			return timeTable;
		}

		/** Sets the time table. */
		private void setTimeTable(int[] tt)
		{
			timeTable = tt;
		}

		/** Returns the distance table. */
		private double[] getDistanceTable()
		{
			return distanceTable;
		}

		/** Sets the distance table. */
		private void setDistanceTable(double[] dt)
		{
			distanceTable = dt;
		}
		
		/** Returns the total number of roadusers that move on this drivelane and not arrived to its destination yet. */
		public int getTotalRoadusersNotArrivedYet()
		{
			return roadusersNotArrivedYet;
		}
		
		/**
		 * Returns the average time the roadusers experienced.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 */
		public float getAvgTime(boolean allTime)
		{
			if (allTime)
			{
				return avgTime;
			}

			int stopIndex, totalTime = 0;
			if (speedTableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = speedTableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalTime += timeTable[i];
			}
			return stopIndex == 0 ? 0 : (float) totalTime / stopIndex;
		}
		
		/**
		 * Returns the average distance the roadusers experienced.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 */
		public float getAvgDistance(boolean allTime)
		{
			if (allTime)
			{
				return avgDistance;
			}

			int stopIndex;
			double totalDistance = 0;
			if (speedTableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = speedTableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalDistance += distanceTable[i];
			}
			return stopIndex == 0 ? 0 : (float) totalDistance / stopIndex;
		}
		
		/**
		 * Add statistics for one roaduser.
		 * @param distance The distance of this roaduser to be logged
		 * @param time The time of this roaduser to be logged
		 */
		public void addRoaduserNotArrivedYet(double distance, int time)
		{
			roadusersNotArrivedYet++;			
			
			distance = distance > 0 ? distance : 0;
			distanceTable[speedTableIndex] = distance;
			
			time = time > 0 ? time : 0;
			timeTable[speedTableIndex] = time;
			
			speedTableIndex++;
			if (speedTableIndex == STAT_NUM_DATA)
			{
				speedTableIndex = 0;
				speedTableFilled = true;
			}

			avgDistance = addToAverage(avgDistance, roadusersNotArrivedYet, distance);
			avgTime = addToAverage(avgTime, roadusersNotArrivedYet, time);
		}
		
		/**
		 * Adds a certain value to an existing average.
		 * @param oldAvg The previous average.
		 * @param newNum The number of samples the new average is based on.
		 * @param value  The new sample to add to this average.
		 */
		private float addToAverage(float oldAvg, float newNum, double value)
		{
			float tmp = oldAvg * (newNum - 1);
			tmp += value;
			return tmp / newNum;
		}


		//// XMLSerializable implementation ///

		public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
		{
			roadusersNotArrivedYet = myElement.getAttribute("roadusers-not-arrived-yet").getIntValue();
			
			avgTime = myElement.getAttribute("avg-time").getFloatValue();
			avgDistance = myElement.getAttribute("avg-distance").getFloatValue();
			
			speedTableIndex = myElement.getAttribute("speed-table-index").getIntValue();
			
			speedTableFilled = myElement.getAttribute("speed-table-filled").getBoolValue();
			
			distanceTable = (double[]) XMLArray.loadArray(this, loader);
			timeTable = (int[]) XMLArray.loadArray(this, loader);
		}

		public XMLElement saveSelf() throws XMLCannotSaveException
		{
			XMLElement result = new XMLElement("drivelane-speed-statistics");
			
			result.addAttribute(new XMLAttribute("roadusers-not-arrived-yet", roadusersNotArrivedYet));
			
			result.addAttribute(new XMLAttribute("avg-time", avgTime));			
			result.addAttribute(new XMLAttribute("avg-distance", avgDistance));
			
			result.addAttribute(new XMLAttribute("speed-table-index", speedTableIndex));
			
			result.addAttribute(new XMLAttribute("speed-table-filled", speedTableFilled));
			
			return result;
		}

		public void saveChilds(XMLSaver saver) throws XMLTreeException,	IOException, XMLCannotSaveException
		{
			XMLArray.saveArray(distanceTable, this, saver, "distance-table");
			XMLArray.saveArray(timeTable, this, saver, "time-table");
		}

		public String getXMLName()
		{
			return parentName + ".drivelane-speed-statistics";
		}

		public void setParentName(String parentName) throws XMLTreeException
		{
			this.parentName = parentName;
		}
	}
	
	/** Returns the speed statistics for all types of roadusers. 
	 * @author EJUST*/
	public DrivelaneSpeedStatistics[] getSpeedStatistics()
	{
		return speedStatistics;
	}

	/**
	 * Returns the speed statistics for the given roaduser type.
	 * @param ruType The roaduser type to return statistics of. (0 if all roadusers)
	 * @author EJUST
	 */
	public DrivelaneSpeedStatistics getDrivelaneSpeedStatistics(int ruType)
	{
		return speedStatistics[RoaduserFactory.ruTypeToStatIndex(ruType)];
	}
	
	/** GASTON: Initializes the statistics for this Drivelane. */
	public void initStats()
	{
		curTimeStep = 0;
		statistics = new DrivelaneStatistics();
	}
	
	/** GASTON: Resets the statistics for this Drivelane. */
	public void resetStats()
	{
		curTimeStep = 0;
		statistics = new DrivelaneStatistics();
	}
	
	/** GASTON: Processes the statistics of all Roadusers moving on this drivelane.*/
	public void processStats()
	{ 
		/*EJUST comment:
		 * ============
		 * 1) Gaston uses the same method used by the TC-1 RL controller to include ONLY the actually waiting roadusers
		 * (means the roadusers that vote in the gain according to which the TL decision is taken)
		 * 
		 * 2) Gaston adds statitics only to "all" roadusers types (not to "every" roaduser type as well), 
		 * and that is the reason why he used: 
		 * DrivelaneStatistics gld.infra.Drivelane.statistics
		 * and did not use an array with the index of the roaduser type where 0 is ALL roaduser types, as an example:
		 * NodeStatistics[] gld.infra.Node.statistics
		 * --> This seems logical as what concerns us is the number of roadusers waiting per time step.
		 * 
		 * 3) Gaston calls his method every time step (in the SimModel.movelane() method) in order to 
		 * check the current number of roadusers waiting per lane as there is no other way to know 
		 * wether a vehicle actually "engaged" the waiting queue of the lane or not @curTimeStep*/
		
		statistics.addStep(getNumRoadusersWaiting()); 
		//Means add a new timeStep with its number of roadusers waiting in this lane
	}

	/**GASTON: Basic lane Statistics */
	public class DrivelaneStatistics implements Cloneable
	{
		protected float avgNumberRUWaiting; 

		protected String parentName="model.infrastructure.node";

		/** Create an (initially empty) statistics datastructure. */
		public DrivelaneStatistics()
		{	
			avgNumberRUWaiting = 0;
		}

		/** Returns a clone of this NodeStatistics. */
		public DrivelaneStatistics getClone() {
			DrivelaneStatistics ls = null;
			try { 
				ls = (DrivelaneStatistics)clone(); 
			}
			catch(CloneNotSupportedException c) {}
			return ls;
		}

		/**
		 * Returns the average number of roadusers waiting.
		 */
		public float getAvgNumberRUWaiting()
		{
			return avgNumberRUWaiting;
		}

		/**
		 * GASTON: Add statistics for each step the simulator does.
		 * @param numberRUWaiting The number of roadusers waiting to be logged.
		 */
		public void addStep(int numberRUWaiting)
		{
			//GASTON: the following is the real average. However if we want to take an average with a faster reaction,
			//we would use the piece of code that follow this one.
			float tmp = avgNumberRUWaiting * (curTimeStep);
			tmp += numberRUWaiting;
			curTimeStep++;
			avgNumberRUWaiting = tmp / curTimeStep;
			//if (getId() == 9)
			//	System.out.println("LaneId " + getId() + ": "+avgNumberRUWaiting);//puse este codigo para ver c�mo se manejaban las estadisticas agregadas de un lane al azar

			//GASTON: we could decide to take this approach to generate a faster reaction when there is an event 
			/*numberRUWaiting = numberRUWaiting > 0 ? numberRUWaiting : 0;
			//avgWaitingTime = addToAverage(avgWaitingTime, roadusers, delay);
			avgNumberRUWaiting = (avgNumberRUWaiting + numberRUWaiting)/2;*/
		}

		/**
		 * Adds a certain value to an existing average.
		 * @param oldAvg The previous average.
		 * @param oldNum The number of samples the new average is based on.
		 * @param value  The new sample to add to this average.
		 */
		/*private float addToAverage(float oldAvg, float newNum, int value)
		{	
			float tmp = oldAvg * (newNum-1);
			tmp += value;
			return tmp / newNum;*
		}*/

		//// XMLSerializable implementation ///

		/*	
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{
			roadusers = myElement.getAttribute("roadusers").getIntValue();
			avgWaitingTime = myElement.getAttribute("avg-waiting-time").getFloatValue();
			tableIndex = myElement.getAttribute("table-index").getIntValue();
			tableFilled = myElement.getAttribute("table-filled").getBoolValue();
			delayTable = (int[])XMLArray.loadArray(this,loader);
		}*/

		/*
		public XMLElement saveSelf () throws XMLCannotSaveException
		{	
			XMLElement result = new XMLElement("statistics");
			result.addAttribute(new XMLAttribute("roadusers",roadusers));
			result.addAttribute(new XMLAttribute("avg-waiting-time",avgWaitingTime));
			result.addAttribute(new XMLAttribute("table-index",tableIndex));
			result.addAttribute(new XMLAttribute("table-filled",tableFilled));
			return result;
		}*/

		/*
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 
			//XMLArray.saveArray(delayTable,this,saver,"delay-table");
		}*/
		 	
		public String getXMLName ()
		{ 
			return parentName + ".statistics";
		}

		public void setParentName (String parentName) throws XMLTreeException
		{ 
			this.parentName = parentName; 
		}
	}
	/**
	 * @return statistics
	 */
	public DrivelaneStatistics getStatistics() {
		return statistics;
	}

	/**
	 * @param statistics
	 */
	public void setStatistics(DrivelaneStatistics statistics) {
		this.statistics = statistics;
	}
}
