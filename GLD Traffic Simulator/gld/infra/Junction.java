
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
import gld.idm.Constants;
import gld.utils.ArrayEnumeration;
import gld.utils.Arrayutils;
import gld.xml.XMLArray;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLTreeException;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 *
 * Basic junction. A Node either is a Junction or an EdgeNode.
 *
 * @author Group Datastructures
 * @version 1.0
 */


public class Junction extends Node implements Constants /*EJUST*/
{
	/** The type of this node */
	protected static final int type = Node.JUNCTION;
	/** The width/height of this node in drivelanes. Needed to draw the node */
	protected int width = 4;
	/** A vector containing all roads connected to this node */
	protected Road[] allRoads = { null, null, null, null };


	/** GASTON: A vector containing all available roads connected to this node */
	protected Road[] allAvailableRoads = { null, null, null, null };

	/** A vector containing all roads that have this node as their Alpha node */
	protected Road[] alphaRoads = { };
	/** A Vector containing all Signs on this node */
	protected Sign[] signs = { };
	/** Contains all possible combinations of signs which may be turned green at the same time */
	protected Sign[][] signconfigs = { { } };
	/** Temporary data structure to transfer info from the first stage loader
	 * to the second stage loader
	 */
	protected TwoStageLoaderData loadData = new TwoStageLoaderData();

	/** (SBC)
	 * phaseDiff is used by trafficlight controllers of Carlos Gershenson 
	 */
	protected int phaseDiff = 0;
	protected boolean greenWave = false;
	protected boolean greenWaveStart = false;
	protected boolean greenWaveFinish = false;

	/** Number of roads, that lead from this junction and are disabled by an accident  (DOAS 06) */
	protected int accidentsCount = 0;
	
	/** EJUST: Position update time step*/
	protected static final double dt = TIMESTEP_S;
	
	/** EJUST: The current gain of this junction*/
	private float currentGain = 0;   
	
	/** EJUST: The transient period of this junction*/
	private int transientPeriod = 0;   
	
	/** Creates an empty junction (for loading) */
	public Junction () { }

	/**
	 * Creates a new standard Junction
	 *
	 * @param _coord The coordinates of this node on the map in pixels.
	 */
	public Junction(Point _coord) {
		super(_coord);
	}

	/*============================================*/
	/* Basic GET and SET methods                  */
	/*============================================*/

	/** Returns the type of this junction */
	public int getType() { return type; }

	/** Returns the name of this junction. */
	public String getName() { return "Junction " + nodeId; }

	/** Returns all roads connected to this node */
	public Road[] getAllRoads() { return allRoads; }
	/** Sets the array that contains all roads connected to this node */

	public void setAllRoads(Road[] r)  throws InfraException {
		allRoads = r;
		updateLanes();
	}

	/** Returns the alpha roads connected to this node */
	public Road[] getAlphaRoads() { return alphaRoads; }
	/** Sets the alpha roads connected to this node */
	public void setAlphaRoads(Road[] r) throws InfraException {
		alphaRoads = r;
		updateLanes();
	}

	/** Returns all possible sign configurations */
	public Sign[][] getSignConfigs() { 		
		if (allRed) //SBC
			return new Sign[0][0];
		else
			return signconfigs;  }
	
	/** Sets all possible sign configurations */
	public void setSignConfigs(Sign[][] s) { signconfigs = s; }

	/** EJUST: Returns the index of the sign configuration that is currently green*/
	public float getCurrentGreenConfigurationIndex(){
		int i, j, num_thissc, num_sc = signconfigs.length;		
		Sign[] thisSC;				
		for (i=0; i<num_sc /*Typically there are 8 possible sign configurations*/; i++) {
			thisSC = signconfigs[i]; //number of signs in this sign configuration
			num_thissc = thisSC.length; 
			for (j=0; j<num_thissc /*Typically there are two signs in this sign configuration*/; j++) {
			  if (!thisSC[j].state)
				  break;
			}
			if (j==num_thissc){
				//System.out.println(" junction: " + this.nodeId + " configuration index: " + i);
				return i;
			}
		}
		return -1;
	}
	
	/** Returns the signs on this Node */
	public Sign[] getSigns() { return signs; }
	/** Sets the signs on this Node */
	public void setSigns(Sign[] s) throws InfraException { signs = s; }

	/** Returns the width of this Node in number of lanes */
	public int getWidth() { return width; }
	/** Sets the width of this Node in number of lanes */
	public void setWidth(int max) { width = max; }


	//SBC
	/** Returns the phase of this node */
	public int getPhaseDiff() { return phaseDiff; }
	/** sets the phase of this node */
	public void setPhaseDiff(int newPhase) { phaseDiff = newPhase; }
	/** Returns if this node is on a green wave */
	public boolean isGreenWaveNode() { return greenWave; }
	/** Sets if this node is on a green wave */
	public void setGreenWaveNode(boolean gw) { greenWave = gw; }
	/** Returns if this node is the start of an green wave */
	public boolean isGreenWaveStart() { return greenWaveStart; }
	/** sets greenWaveStart of this node */
	public void setGreenWaveStart(boolean s) { greenWaveStart = s; }
	/** returns if this node is the finish of an green wave */
	public boolean isGreenWaveFinish() { return greenWaveFinish; }
	/** sets greenWaveFinish of this node */
	public void setGreenWaveFinish(boolean f) { greenWaveFinish = f; }
	//SBC

	/** EJUST: Returns the current gain of this junction. */	
	public float getCurrentGain() {return currentGain;}
	/** EJUST: Sets the current gain of this junction. */
	public void setCurrentGain(float g){currentGain = g;}
	
	
	/** EJUST: Increment the transient period of this junction. */	
	public void incrementTransientPeriod() {transientPeriod++;}
	/** EJUST: Returns the transient period of this junction. */
	public int getTransientPeriod(){return transientPeriod;}
	/** EJUST: Sets the transient period of this junction. */
	public void setTransientPeriod(int _transientPeriod){transientPeriod = _transientPeriod;}
	
	/*============================================*/
	/* STATISTICS                                 */
	/*============================================*/

	protected double calcDelay(Roaduser ru, int stop, Drivelane lane /*SBC: int distance --> Drivelane lane*/) {
		// calculate the delay for the drivelane leading to this Junction
		
		//Returns the start time of this road user in the current drivelane 
		int start = ru.getDrivelaneStartTime();
		
		double speed = lane.getSpeedMaxAllowed(); 
		/*SBC: speed = ru.getSpeed()-->lane.getSpeedMaxAllowed()*/
		/*EJUST: speed = lane.getSpeedMaxAllowed() --> ru.get_v0()*/

		int distance = lane.getCompleteLength(); 
		/*SBC: int distance = lane.getCompleteLength()--> lane.getLength()*/

		double min_steps = distance / (speed*dt); /*EJUST: int --> double*/ 

		int num_steps = stop - start;

		double delay = num_steps - min_steps; /*EJUST: int --> double*/

		ru.addDelay(delay);

		//System.out.println("Just crossed, and delayed: "+delay+"	start:	"+start+"	stop:	"+stop+"	dist:	"+distance+"	speed:	"+speed);

		ru.setDrivelaneStartTime(stop);

		// then return the delay
		return delay;
	}

	/*EJUST: Calculate the experienced waiting time due to engaging the queue*/
	protected int calcWaitingTime(Roaduser ru, int stop) {
		// calculate the waiting time for the drivelane leading to this Junction
		
		/* Returns the engage queue time of this road user in the current drivelane*/
		int engageQueueTime = ru.getDrivelaneEngageQueueTime();
		
		/*Means that we called node.processStats() from model.moveLane() due to roaduser 
		 * crossed this junction with NO waiting time in queue, e.g., the sign was GREEN*/
		if (engageQueueTime == -1) 
			return 0;
		
		int waitingTime = stop - engageQueueTime;
		
		/* Time this road user left the junction - time road user engaged the queue*/
		ru.addWaitingTime(waitingTime);

		//System.out.println("Just crossed, and waited:	"+waitingTime+"	engageQueueTime:	"+engageQueueTime+"	stop:	"+stop);

		/*To recalculate drivelaneEngageQueueTime in moveLane() when exactly ru will engage the NEXT queue,
		 * (after crossing the current junction) */
		ru.setDrivelaneEngageQueueTime(-1);
		
		// then return the waiting time
		return waitingTime;
	}
	/*============================================*/
	/* MODIFYING DATA                             */
	/*============================================*/


	public void reset() {
		super.reset();
		for (int i=0; i < alphaRoads.length; i++){
			alphaRoads[i].reset();
		}
		for (int i=0; i < signs.length; i++){
			signs[i].reset();
		}
		accidentsCount = 0;   //(DOAS 06)
	}

	public void addRoad(Road r, int pos) throws InfraException
	{
		if (r == null) throw new InfraException("Parameter r is null");
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range");
		if (allRoads[pos] != null) throw new InfraException("Road already connected to position "+pos);
		allRoads[pos] = r;

		//GASTON: SE AGREGA EL ROAD AL ARRAY DE AVAILABLEROADS TAMBIEN (SA HACE ACA PARA QUE NO FALLE EL EDITOR CUANDO TIRA LA VALIDACION) 
		/*
		 * THE ROAD IS ADDED TO THE ROADS ALSO AVAILABLE ARRAY (IS HERE TO NOT FAIL THE EDITOR STRIP WHEN THE VALIDITY)
		 * */		
		allAvailableRoads[pos] = r;

		Node other = r.getOtherNode(this);
		if (other == null || !other.isAlphaRoad(r))
			alphaRoads = (Road[])Arrayutils.addElement(alphaRoads, r);   
		updateLanes();
		calculateWidth();
	}

	public void setAlphaRoad(int pos) throws InfraException
	{
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range");
		if (allRoads[pos] == null) throw new InfraException("No road is conencted at position "+pos);
		alphaRoads = (Road[])Arrayutils.addElementUnique(alphaRoads, allRoads[pos]);
		updateLanes();
	}

	public void remRoad(int pos) throws InfraException
	{
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range");
		Road road = allRoads[pos];
		if (road == null) throw new InfraException("Road at position " + pos + " does not exist");
		allRoads[pos] = null;
		alphaRoads = (Road[])Arrayutils.remElement(alphaRoads, road);
		updateLanes();
		calculateWidth();
	}

	//GASTON: EN ESTE METHOD VOY A INTENTAR ELIMINAR EL NODO Y CALCULAR LOS SHORTETSPATH PARA ESTE NODO, DE MODO DE ESQUIVAR LA ROAD QUE AHORA NO ESTA MAS
	/*
	 * THIS METHOD I WILL ATTEMPT TO DELETE THE NODE AND CALCULATING SHORTETSPATH for this node, 
	 * SO THE ROAD OF OVERCOMING THIS NOW NO MORE
	 * */
	public void remRoadSim(int pos) throws InfraException
	{
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range");
		Road road = allRoads[pos];
		if (road == null) throw new InfraException("Road at position " + pos + " does not exist");
		allRoads[pos] = null;
		alphaRoads = (Road[])Arrayutils.remElement(alphaRoads, road);
		updateLanes();
		calculateWidth();

		spdata.remAllPaths();
		//ShortestPathCalculator spcalc = new ShortestPathCalculator();
		//spcalc.calcAllShortestPaths(this);
	}
	//FIN DEL NUEVO METHOD DE PRUEBA
	//END OF THE NEW METHOD OF TEST

	public void remRoad(Road r) throws InfraException
	{
		if (r == null) throw new InfraException("Parameter r is null");
		alphaRoads = (Road[])Arrayutils.remElement(alphaRoads, r);
		for (int i=0; i < 4; i++) {
			if (allRoads[i] == r) {
				allRoads[i] = null;
				updateLanes();
				calculateWidth();
				return;
			}
		}
		throw new InfraException("Road not found in this node");
	}

	public void remAllRoads() throws InfraException
	{
		for (int i=0; i < allRoads.length; i++) allRoads[i] = null;
		alphaRoads = new Road[0];
		updateLanes();
		calculateWidth();
	}


	/** Adds a sign configuration */
	public void addSignconfig(Sign[] conf) throws InfraException {
		if (conf == null) throw new InfraException("Parameter conf is null");
		signconfigs = (Sign[][])Arrayutils.addElement(signconfigs, conf);
	}

	/** Removes a sign configuration */
	public void remSignconfig(Sign[] conf) throws InfraException {
		if (conf == null) throw new InfraException("Parameter conf is null");
		int i = Arrayutils.findElement(signconfigs, conf);
		if (i == -1) throw new InfraException("Sign configuration is not in the list");
		signconfigs = (Sign[][])Arrayutils.remElement(signconfigs, i);
	}

	/*============================================*/
	/* SMALL GET                                  */
	/*============================================*/


	public boolean isAlphaRoad(Road r) throws InfraException {
		if (r == null) throw new InfraException("Parameter r == null");
		for (int i=0; i < alphaRoads.length; i++) {
			if (alphaRoads[i] == r) return true;
		}
		return false;
	}

	public boolean isConnected(Road r) throws InfraException {
		return isConnectedAt(r) != -1;
	}

	public int isConnectedAt(Road r) throws InfraException {
		if (r == null) throw new InfraException("Parameter r == null");
		if (allRoads[0] == r) return 0;
		if (allRoads[1] == r) return 1;
		if (allRoads[2] == r) return 2;
		if (allRoads[3] == r) return 3;
		throw new InfraException("Road is not connected to this node");
	}

	public boolean isConnectionPosFree(int pos) throws InfraException {
		if (pos > 3 || pos < 0) throw new InfraException("Position out of range");
		return (allRoads[pos] == null);
	}

	public int getNumRoads() {
		int i = 0;
		if (allRoads[0] != null) i++;
		if (allRoads[1] != null) i++;
		if (allRoads[2] != null) i++;
		if (allRoads[3] != null) i++;
		return i;
	}

	public int getNumAlphaRoads() {
		return alphaRoads.length;
	}

	public int getNumInboundLanes() throws InfraException {
		int num = 0;
		for (int i=0; i < allRoads.length; i++) {
			if (allRoads[i] != null)
				num += allRoads[i].getNumInboundLanes(this);
		}
		return num;
	}

	public int getNumOutboundLanes() throws InfraException {
		int num = 0;
		for (int i=0; i < allRoads.length; i++) {
			if (allRoads[i] != null)
				num += allRoads[i].getNumOutboundLanes(this);
		}
		return num;
	}

	public int getNumAllLanes() {
		int num = 0;
		for (int i=0; i < allRoads.length; i++) {
			if (allRoads[i] != null)
				num += allRoads[i].getNumAllLanes();
		}
		return num;
	}

	public int getNumSigns() {
		return signs.length;
	}
	public int getNumRealSigns() {
		int c = 0;
		for (int i=0; i < signs.length; i++)
			if (signs[i].getType() != Sign.NO_SIGN) c++;
		return c;
	}

	public int getDesiredSignType() throws InfraException {
		return getNumRoads() > 2 ? Sign.TRAFFICLIGHT : Sign.NO_SIGN;
	}




	/** Returns the number of roads, that lead from this junction and are disabled by an accident (DOAS 06) */
	public int getAccidentsCount(){ return accidentsCount; }
	/** Sets the number of roads, that lead from this junction and are disabled by an accident (DOAS 06)
	 *  @throw If there was no way through the junction (dead end, or leading only to the EdgeNode), the exception is thrown
	 */
	public void setAccidentsCount(int count) throws InfraException{
		int usefullRoadsCount = 0;  //usefull road leads somewhere else, than to the EdgeNode
		for(int i = 0; i < allRoads.length; i++){
			if((allRoads[i] != null)
					&& (allRoads[i].getAlphaNode().getType() != Node.EDGE)
					&& (allRoads[i].getBetaNode().getType() != Node.EDGE)){

				usefullRoadsCount++;
			}
		}
		if(count > usefullRoadsCount - 2){
			throw new InfraException("Dead end created by an accident");
		}
		accidentsCount = count;
		if(accidentsCount < 0){
			accidentsCount = 0;
		}
	}
	/** Increase the number of known accidents on the roads, that lead from this junction, by one. (DOAS 06)
	 *  @throw If one more accident would create an dead-end, the exception is thrown
	 */
	public void increaseAccidentsCount() throws InfraException{
		setAccidentsCount(accidentsCount + 1);
	}
	/** Decrease the number of known accidents on the roads, that lead from this junction, by one. (DOAS 06)
	 */
	public void decreaseAccidentsCount(){
		try{
			setAccidentsCount(getAccidentsCount() - 1);
		}catch(InfraException e){
			//the exception cannot be thrown during the decrease
			e.printStackTrace();
		}
	}

	/*============================================*/
	/* LARGE GET                                  */
	/*============================================*/

	public Drivelane[] getLanesLeadingTo(Drivelane lane, int ruType) throws InfraException
	{
		Road road = lane.getRoad();
		// Road[] which will contain the Roads of this Node in a sorted fashion:
		// [0] == the drivelanes on this Road will have to turn left to get to 'road', ..
		Road[] srt_rarr = new Road[3];

		if(allRoads[0] == road) {
			srt_rarr[0] = allRoads[3];	// Must turn left
			srt_rarr[1] = allRoads[2];	// Must go straight on
			srt_rarr[2] = allRoads[1];	// Must turn right
		}
		else if(allRoads[1] == road) {
			srt_rarr[0] = allRoads[0];
			srt_rarr[1] = allRoads[3];
			srt_rarr[2] = allRoads[2];	
		}
		else if(allRoads[2] == road) {
			srt_rarr[0] = allRoads[1];
			srt_rarr[1] = allRoads[0];
			srt_rarr[2] = allRoads[3];
		}
		else {
			srt_rarr[0] = allRoads[2];
			srt_rarr[1] = allRoads[1];
			srt_rarr[2] = allRoads[0];
		}

		Vector v = new Vector();
		Drivelane[] lanes;
		int num_lanes;
		int cnt_lanes = 0;
		boolean[] targets;

		for(int i=0;i<3;i++) {
			if(srt_rarr[i]!=null) {
				lanes = srt_rarr[i].getInboundLanes(this);
				num_lanes = lanes.length;
				for(int j=0;j<num_lanes;j++) {
					Drivelane l = lanes[j];
					targets = l.getTargets();
					if(targets[i]==true && l.mayUse(ruType)) {
						v.addElement(l);
						cnt_lanes++;
					}
				}
			}
		}

		return (Drivelane[]) v.toArray(new Drivelane[cnt_lanes]);
	}


	/* Needs Testing! */
	public Drivelane[] getLanesLeadingFrom(Drivelane lane, int ruType) throws InfraException
	{
		Road road = lane.getRoad();
		// Road[] which will contain the Roads of this Node in a sorted fashion:
		// [0] == the drivelanes on this Road will have to turn left to get to 'road', ..
		Road[] srt_rarr = new Road[3];

		if(allRoads[0] == road) {
			srt_rarr[0] = allRoads[1];	// Must turn left
			srt_rarr[1] = allRoads[2];	// Must go straight on
			srt_rarr[2] = allRoads[3];	// Must turn right
		}
		else if(allRoads[1] == road) {
			srt_rarr[0] = allRoads[2];
			srt_rarr[1] = allRoads[3];
			srt_rarr[2] = allRoads[0];	
		}
		else if(allRoads[2] == road) {
			srt_rarr[0] = allRoads[3];
			srt_rarr[1] = allRoads[0];
			srt_rarr[2] = allRoads[1];
		}
		else {
			srt_rarr[0] = allRoads[0];
			srt_rarr[1] = allRoads[1];
			srt_rarr[2] = allRoads[2];
		}

		//System.out.println("Junction getLanesLeadingFrom "+nodeId);		
		Vector v = new Vector();
		Drivelane[] lanes;
		int num_lanes;
		int cnt_lanes = 0;
		boolean[] targets = lane.getTargets();

		for(int i=0;i<3;i++) {
			if(srt_rarr[i]!=null && targets[i]==true) {
				//System.out.println("Road at target:"+i+" isnt null, getting Outboundlanes");
				lanes = srt_rarr[i].getOutboundLanes(this);
				num_lanes = lanes.length;
				//System.out.println("Num lanes :"+num_lanes);
				for(int j=0;j<num_lanes;j++) {
					Drivelane l = lanes[j];
					//System.out.println("Lane"+j+" being checked now. Has type:"+l.getType());
					if(l.mayUse(ruType)) {
						v.addElement(l);
						cnt_lanes++;
					}
				}
			}
		}
		return (Drivelane[]) v.toArray(new Drivelane[cnt_lanes]);
	}

	public Drivelane[] getLanesLeadingFrom(Drivelane lane) throws InfraException
	{
		Road road = lane.getRoad();
		// Road[] which will contain the Roads of this Node in a sorted fashion:
		// [0] == the drivelanes on this Road will have to turn left to get to 'road', ..
		Road[] srt_rarr = new Road[3];

		if(allRoads[0] == road) {
			srt_rarr[0] = allRoads[1];	// Must turn left
			srt_rarr[1] = allRoads[2];	// Must go straight on
			srt_rarr[2] = allRoads[3];	// Must turn right
		}
		else if(allRoads[1] == road) {
			srt_rarr[0] = allRoads[2];
			srt_rarr[1] = allRoads[3];
			srt_rarr[2] = allRoads[0];	
		}
		else if(allRoads[2] == road) {
			srt_rarr[0] = allRoads[3];
			srt_rarr[1] = allRoads[0];
			srt_rarr[2] = allRoads[1];
		}
		else {
			srt_rarr[0] = allRoads[0];
			srt_rarr[1] = allRoads[1];
			srt_rarr[2] = allRoads[2];
		}

		//System.out.println("Junction getLanesLeadingFrom "+nodeId);		
		Vector v = new Vector();
		Drivelane[] lanes;
		int num_lanes;
		int cnt_lanes = 0;
		boolean[] targets = lane.getTargets();

		for(int i=0;i<3;i++) {
			if(srt_rarr[i]!=null && targets[i]==true) {
				//System.out.println("Road at target:"+i+" isnt null, getting Outboundlanes");
				lanes = srt_rarr[i].getOutboundLanes(this);
				num_lanes = lanes.length;
				//System.out.println("Num lanes :"+num_lanes);
				for(int j=0;j<num_lanes;j++) {
					v.addElement(lanes[j]);
					cnt_lanes++;
				}
			}
		}
		return (Drivelane[]) v.toArray(new Drivelane[cnt_lanes]);
	}

	//GASTON:
	public Drivelane[] getAvailableLanesLeadingFrom(Drivelane lane) throws InfraException
	{
		Road road = lane.getRoad();
		// Road[] which will contain the Roads of this Node in a sorted fashion:
		// [0] == the drivelanes on this Road will have to turn left to get to 'road', ..
		Road[] srt_rarr = new Road[3];

		if(allAvailableRoads[0] == road) {
			srt_rarr[0] = allAvailableRoads[1];	// Must turn left
			srt_rarr[1] = allAvailableRoads[2];	// Must go straight on
			srt_rarr[2] = allAvailableRoads[3];	// Must turn right
		}
		else if(allAvailableRoads[1] == road) {
			srt_rarr[0] = allAvailableRoads[2];
			srt_rarr[1] = allAvailableRoads[3];
			srt_rarr[2] = allAvailableRoads[0];	
		}
		else if(allAvailableRoads[2] == road) {
			srt_rarr[0] = allAvailableRoads[3];
			srt_rarr[1] = allAvailableRoads[0];
			srt_rarr[2] = allAvailableRoads[1];
		}
		else {
			srt_rarr[0] = allAvailableRoads[0];
			srt_rarr[1] = allAvailableRoads[1];
			srt_rarr[2] = allAvailableRoads[2];
		}

		//System.out.println("Junction getLanesLeadingFrom "+nodeId);		
		Vector v = new Vector();
		Drivelane[] lanes;
		int num_lanes;
		int cnt_lanes = 0;
		boolean[] targets = lane.getTargets();

		for(int i=0;i<3;i++) {
			if(srt_rarr[i]!=null && targets[i]==true) {
				//System.out.println("Road at target:"+i+" isnt null, getting Outboundlanes");
				lanes = srt_rarr[i].getOutboundLanes(this);
				num_lanes = lanes.length;
				//System.out.println("Num lanes :"+num_lanes);
				for(int j=0;j<num_lanes;j++) {
					v.addElement(lanes[j]);
					cnt_lanes++;
				}
			}
		}
		return (Drivelane[]) v.toArray(new Drivelane[cnt_lanes]);
	}

	/** Returns an array of all outbound lanes on this junction */
	public Drivelane[] getOutboundLanes() throws InfraException {
		int pointer = 0;
		//System.out.println("NewNumOutboundLanes: "+getNumOutboundLanes());
		Drivelane[] lanes = new Drivelane[getNumOutboundLanes()];
		Drivelane[] temp;
		for (int i=0; i < allRoads.length; i++) {
			if (allRoads[i] != null) {
				temp = allRoads[i].getOutboundLanes(this);
				System.arraycopy(temp, 0, lanes, pointer, temp.length);
				pointer += temp.length;
			}
		}
		return lanes;
	}

	/** Returns an array of all inbound lanes on this junction */
	public Drivelane[] getInboundLanes() throws InfraException {
		//System.out.println("Junction.getInboundLanes()");
		int pointer = 0;
		Drivelane[] lanes = new Drivelane[getNumInboundLanes()];
		Drivelane[] temp;
		for (int i=0; i < allRoads.length; i++) {
			if (allRoads[i] != null) {
				temp = allRoads[i].getInboundLanes(this);
				System.arraycopy(temp, 0, lanes, pointer, temp.length);
				pointer += temp.length;
			}
		}
		return lanes;
	}


	//GASTON: AGREGO ESTE METHOD PARA CALCULAR LOS SHORTESTPATH
	// Add this method for calculating shortest path
	public Drivelane[] getAvailableInboundLanes() throws InfraException {
		//System.out.println("Junction.getInboundLanes()");
		int pointer = 0;
		Drivelane[] lanes = new Drivelane[getNumInboundLanes()];
		Drivelane[] temp;
		//GASTON: CAMBIO allRoads por allAvailableRoads
		//Change allRoads by allAvailableRoads
		for (int i=0; i < allAvailableRoads.length; i++) {
			if (allAvailableRoads[i] != null) {
				temp = allAvailableRoads[i].getInboundLanes(this);
				System.arraycopy(temp, 0, lanes, pointer, temp.length);
				pointer += temp.length;
			}
		}
		return lanes;
	}


	/* clockwise order guaranteed */	
	public Drivelane[] getAllLanes() throws InfraException {
		int pointer = 0;
		Drivelane[] lanes = new Drivelane[getNumAllLanes()];
		Drivelane[] temp;
		Road road;
		for (int i=0; i < allRoads.length; i++) {
			road = allRoads[i];
			if (road != null) {
				temp = road.getInboundLanes(this);
				System.arraycopy(temp, 0, lanes, pointer, temp.length);
				pointer += temp.length;
				temp = road.getOutboundLanes(this);
				System.arraycopy(temp, 0, lanes, pointer, temp.length);
				pointer += temp.length;
			}
		}
		return lanes;
	}

	/** Returns an array of all lanes connected to this node, in clock-wise order, starting at the given lane */
	public Drivelane[] getAllLanesCW(Drivelane lane) throws InfraException {
		Drivelane[] lanes = getAllLanes(); // in clockwise order starting at road 0, lane 0

		// find the starting-lane
		int i = Arrayutils.findElement(lanes, lane);
		if (i == -1) throw new InfraException("Lane is not on this node");

		// shift all the lanes i places and remove the i-th element
		Drivelane[] result = new Drivelane[lanes.length-1];
		System.arraycopy(lanes, i+1, result, 0, lanes.length -  i - 1);
		System.arraycopy(lanes, 0, result, lanes.length - i - 1, i);

		return result;
	}

	/** Returns whether or not the tails of the lanes not being accessible by the given array of Signs are free */
	public boolean areOtherTailsFree(Sign[] mayUse) {
		boolean[] pos = new boolean[4];
		Road[] roads = getAllRoads();
		Drivelane lane;
		int thisRoad;
		boolean[] targets;

		for(int i=0;i<4;i++)
			pos[i] = true;

		int num_mayuse = mayUse.length;

		for(int i=0;i<num_mayuse;i++) {
			lane = mayUse[i].getLane();
			try{ 
				thisRoad = isConnectedAt(lane.getRoad()); 
			}
			catch(InfraException e) { 
				thisRoad = 0; 
				System.out.println("Something went wrong in areOtherTailsFree()"); 
			}
			targets = lane.getTargets();

			if(targets[0]) {
				pos[(thisRoad+1)%4] = false;
			}
			else if(targets[1]) {
				pos[(thisRoad+2)%4] = false;
			}
			else if(targets[2]) {
				pos[(thisRoad+3)%4] = false;
			}
		}

		int num_check = 0;
		for(int i=0;i<4;i++) {
			if(pos[i] && roads[i]!=null) {
				Drivelane[] check = new Drivelane[0];
				try { 
					check = roads[i].getOutboundLanes(this); 
				}
				catch(Exception e) {
					check = new Drivelane[0]; 
					System.out.println("Something went wrong in areOtherTailsFree() 2"); 
				}

				num_check = check.length;
				for(int j=0;j<num_check;j++) {
					if(!check[j].isTailFree()) {
						return false;
					}
				}
			}
		}
		return true;

	}



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
		// TODO: * tekenen status stoplichten
		int width = getWidth();
		g.setColor(Color.black);
		g.drawRect((int)((coord.x + x - 5 * width) * zf), (int)((coord.y + y - 5 * width) * zf), (int)(10 * width * zf), (int)(10 * width * zf));
		if (nodeId != -1)
			g.drawString("" + nodeId,(int)((coord.x + x - 5 * width) * zf) - 10,(int)((coord.y + y - 5 * width) * zf) - 3);
	}



	/** (Re)Calculates the width of this junction */
	public void calculateWidth() {
		Road road;
		width = 4;
		for (int i=0; i < 4; i++) {
			road = allRoads[i];
			if (road != null && road.getWidth() > width) width = road.getWidth();
		}
	}

	/*============================================*/
	/* LOAD and SAVE                              */
	/*============================================*/

	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{ 	
		super.load(myElement,loader);
		width=myElement.getAttribute("width").getIntValue();
		alphaRoads=(Road[])XMLArray.loadArray(this,loader);
		loadData.roads=(int[])XMLArray.loadArray(this,loader);
		loadData.signconfigs=(int[][])XMLArray.loadArray(this,loader);
		loadData.signs=(int[])XMLArray.loadArray(this,loader);
		//GDE: these attributes are not required. This is to achieve compatibility with older versions of the product
	try {
		//SBC
		greenWave = myElement.getAttribute("greenWave").getBoolValue();
		greenWaveStart = myElement.getAttribute("greenWaveStart").getBoolValue();
		greenWaveFinish = myElement.getAttribute("greenWaveFinish").getBoolValue();
		//SBC
	} catch (NoSuchElementException e) {
		//We don't really want to do anything here... just compatibility
	}
	}

	//GASTON: Used in EditController.java
	public void updateAllAvailableRoads() {
		for (int i=0;i<allRoads.length;i++){
			if (allRoads[i]!=null && allRoads[i].isEnabled()){
				allAvailableRoads[i] = allRoads[i];
			}else{
				if (allRoads[i]!=null && !allRoads[i].isEnabled())
					allAvailableRoads[i] = null;
			}
		}
	}

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	
		XMLElement result=super.saveSelf();
		result.setName("node-junction");
		result.addAttribute(new XMLAttribute("width",width));
	
		//SBC
		result.addAttribute(new XMLAttribute("greenWave", isGreenWaveNode()));
		result.addAttribute(new XMLAttribute("greenWaveStart", isGreenWaveStart()));
		result.addAttribute(new XMLAttribute("greenWaveFinish", isGreenWaveFinish()));
		//SBC
	
		return result;
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ 	
		super.saveChilds(saver);
		XMLArray.saveArray(alphaRoads,this,saver,"alpha-roads");
		XMLArray.saveArray(getRoadIdArray(),this,saver,"roads");		
		XMLArray.saveArray(getSignConfigIdArray(),this,saver,"sign-configs");		
		XMLArray.saveArray(getSignIdArray(),this,saver,"signs");
	}		

	protected int[] getSignIdArray ()
	{ 	
		int[] result=new int[signs.length];
		for (int t=0;t<signs.length;t++)
		{	if (signs[t]==null)
			result[t]=-1;
		else
			result[t]=signs[t].getId();
		}
		return result;
	}

	protected int[] getRoadIdArray ()
	{ 	
		int[] result=new int[allRoads.length];
		for (int t=0;t<allRoads.length;t++)
		{ 	if (allRoads[t]==null)
			result[t]=-1;
		else
			result[t]=allRoads[t].getId();
		}
		return result;
	}

	protected int[][] getSignConfigIdArray ()
	{ 	
		int [][] result;
		if (signconfigs.length==0)
			result=new int[0][0];
		else
			result=new int[signconfigs.length][signconfigs[0].length];
		for (int t=0;t<signconfigs.length;t++)
		{	
			result[t]=new int[signconfigs[t].length]; 
			for (int u=0;u<signconfigs[t].length;u++)
			{ 	if (signconfigs[t][u]==null)
				result[t][u]=-1;
			else
				result[t][u]=signconfigs[t][u].getId();
			}
		}
		return result;
	}

	public String getXMLName ()
	{ 	
		return parentName+".node-junction";
	}

	private class TwoStageLoaderData
	{ 	
		int[] roads; // Storage for road-id's
		int[] signs; // For sign ids
		int[][] signconfigs; // For the sign configs
	}

	public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
	{ 	
		super.loadSecondStage(dictionaries);
		// Load roads
		Dictionary roadDictionary=(Dictionary)(dictionaries.get("road"));
		allRoads=new Road[loadData.roads.length];
		for (int t=0;t<loadData.roads.length;t++)
		{	
			allRoads[t]=(Road)(roadDictionary.get(new Integer(loadData.roads[t])));
			if (allRoads[t]==null && loadData.roads[t]!=-1 )
				System.out.println("Warning : "+getName()+" could not find road "+ loadData.roads[t]);
	}
		
	// Load normal signs		       
	Dictionary laneDictionary=(Dictionary)(dictionaries.get("lane")); 		       
	signs=new Sign[loadData.signs.length];
	for (int t=0;t<loadData.signs.length;t++)
		signs[t]=getSign(laneDictionary,loadData.signs[t]);		       
		// Load Signconfigurations
		signconfigs=new Sign[loadData.signconfigs.length][2];
		for (int t=0;t<signconfigs.length;t++)
		{ 
			signconfigs[t]=new Sign[loadData.signconfigs[t].length];
		for (int u=0;u<signconfigs[t].length;u++)
		{ 	
			signconfigs[t][u]=getSign(laneDictionary,loadData.signconfigs[t][u]);
		}
	}
	// Tell *all* roads to load themselves
	// It's possible that this Node has a BetaLane that has not been SecondStageLoaded
	// And so we cant do an UpdateLanes() as that one needs secondStageData to proceed.
	// Hence, we need to 2ndStage all Roads.
	Enumeration e= new ArrayEnumeration(allRoads);
	Road tmpRoad;
	while (e.hasMoreElements()) {
		tmpRoad=(Road) e.nextElement();
		if (tmpRoad!=null) tmpRoad.loadSecondStage(dictionaries);
	}
	try
	{	//System.out.println("Trying to updateLanes()");
		updateLanes();
	}
	catch (InfraException x)
	{	
		throw new XMLInvalidInputException ("Cannot initialize lanes of node "+nodeId);
	}
	}	

	protected Sign getSign (Dictionary laneDictionary,int id)
	{
		Drivelane tmp=(Drivelane)(laneDictionary.get(new Integer(id)));
		if (tmp==null) {
			return null;
		}
		else {
			return tmp.getSign();
		}
	}

	//GASTON: /* Needs Testing! */
	public Drivelane[] getAvailableLanesLeadingFrom(Drivelane lane, int ruType) throws InfraException {
		Road road = lane.getRoad();
		// Road[] which will contain the Roads of this Node in a sorted fashion:
		// [0] == the drivelanes on this Road will have to turn left to get to 'road', ..
		Road[] srt_rarr = new Road[3];

		if(allAvailableRoads[0] == road) {
			srt_rarr[0] = allAvailableRoads[1];	// Must turn left
			srt_rarr[1] = allAvailableRoads[2];	// Must go straight on
			srt_rarr[2] = allAvailableRoads[3];	// Must turn right
		}
		else if(allAvailableRoads[1] == road) {
			srt_rarr[0] = allAvailableRoads[2];
			srt_rarr[1] = allAvailableRoads[3];
			srt_rarr[2] = allAvailableRoads[0];	
		}
		else if(allAvailableRoads[2] == road) {
			srt_rarr[0] = allAvailableRoads[3];
			srt_rarr[1] = allAvailableRoads[0];
			srt_rarr[2] = allAvailableRoads[1];
		}
		else {
			srt_rarr[0] = allAvailableRoads[0];
			srt_rarr[1] = allAvailableRoads[1];
			srt_rarr[2] = allAvailableRoads[2];
		}

		//System.out.println("Junction getLanesLeadingFrom "+nodeId);		
		Vector v = new Vector();
		Drivelane[] lanes;
		int num_lanes;
		int cnt_lanes = 0;
		boolean[] targets = lane.getTargets();

		for(int i=0;i<3;i++) {
			if(srt_rarr[i]!=null && targets[i]==true) {
				//System.out.println("Road at target:"+i+" isnt null, getting Outboundlanes");
				lanes = srt_rarr[i].getOutboundLanes(this);
				num_lanes = lanes.length;
				//System.out.println("Num lanes :"+num_lanes);
				for(int j=0;j<num_lanes;j++) {
					Drivelane l = lanes[j];
					//System.out.println("Lane"+j+" being checked now. Has type:"+l.getType());
					if(l.mayUse(ruType)) {
						v.addElement(l);
						cnt_lanes++;
					}
				}
			}
		}
		return (Drivelane[]) v.toArray(new Drivelane[cnt_lanes]);
	}

	//GASTON:
	public Drivelane[] getAvailableLanesLeadingTo(Drivelane lane, int ruType) throws InfraException {
		Road road = lane.getRoad();
		// Road[] which will contain the Roads of this Node in a sorted fashion:
		// [0] == the drivelanes on this Road will have to turn left to get to 'road', ..
		Road[] srt_rarr = new Road[3];

		if(allAvailableRoads[0] == road) {
			srt_rarr[0] = allAvailableRoads[3];	// Must turn left
			srt_rarr[1] = allAvailableRoads[2];	// Must go straight on
			srt_rarr[2] = allAvailableRoads[1];	// Must turn right
		}
		else if(allAvailableRoads[1] == road) {
			srt_rarr[0] = allAvailableRoads[0];
			srt_rarr[1] = allAvailableRoads[3];
			srt_rarr[2] = allAvailableRoads[2];	
		}
		else if(allAvailableRoads[2] == road) {
			srt_rarr[0] = allAvailableRoads[1];
			srt_rarr[1] = allAvailableRoads[0];
			srt_rarr[2] = allAvailableRoads[3];
		}
		else {
			srt_rarr[0] = allAvailableRoads[2];
			srt_rarr[1] = allAvailableRoads[1];
			srt_rarr[2] = allAvailableRoads[0];
		}

		Vector v = new Vector();
		Drivelane[] lanes;
		int num_lanes;
		int cnt_lanes = 0;
		boolean[] targets;

		for(int i=0;i<3;i++) {
			if(srt_rarr[i]!=null) {
				lanes = srt_rarr[i].getInboundLanes(this);
				num_lanes = lanes.length;
				for(int j=0;j<num_lanes;j++) {
					Drivelane l = lanes[j];
					targets = l.getTargets();
					if(targets[i]==true && l.mayUse(ruType)) {
						v.addElement(l);
						cnt_lanes++;
					}
				}
			}
		}

		return (Drivelane[]) v.toArray(new Drivelane[cnt_lanes]);
	}

	/*GASTON*/
	/**
	 * @return
	 */
	public Road[] getAllAvailableRoads() {
		return allAvailableRoads;
	}

	/**
	 * @param roads
	 */
	public void setAllAvailableRoads(Road[] roads) {
		allAvailableRoads = roads;
	}
	/*GASTON*/
}