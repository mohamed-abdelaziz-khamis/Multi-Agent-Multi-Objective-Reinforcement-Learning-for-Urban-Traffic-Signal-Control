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
import gld.Model;
import gld.Selectable;
import gld.utils.ArrayEnumeration;
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

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;

/**
 *
 * Basic node
 *
 * @author Group Datastructures
 * @version 1.0
 */

public abstract class Node implements Selectable, XMLSerializable, TwoStageLoader, InstantiationAssistant
{
	/** The number of roadusers delay tables are maintained of. */
	public static int STAT_NUM_DATA = 1000;

	/** The Id of this node */
	protected int nodeId;
	/** Shortest path data */
	protected SPData spdata;
	/** The coordinates of this node in pixels */
	protected Point coord;
	/** All statistics of this Node. */
	protected NodeStatistics[] statistics;

	/** Caches for inbound and outbound lanes */
	protected Drivelane[] inboundLanes, outboundLanes;

	protected String parentName = "model.infrastructure";

	/** Constant for Node type EdgeNode. */
	public static final int EDGE = 1;
	/** Constant for Node type Junction (normal Node with traffic lights). */
	public static final int JUNCTION = 2;
	/** Constant for Node type NoTLJunction (Node without traffic lights). */
	public static final int NON_TL = 3;
	/** Constant for Node type NetTunnel. */
	public static final int NET_TUNNEL = 4;

	// Some constants for directions and fixed connections
	public static final int
	C_UP = 0,
	C_RIGHT = 1,
	C_DOWN = 2,
	C_LEFT = 3,
	D_BACK = 0,
	D_RIGHT = 1,
	D_AHEAD = 2,
	D_LEFT = 3;


	/** SBC: Boolean representing the clearance period */
	public boolean allRed = false;
	protected boolean keepTLDFlag = false;
	private int phaseMinimal = 0;
	/** used in MorevtsSotlPlatoon. 
	 * if true, there is a platoon crossing 
	 */
	protected boolean platoonCrossing = false;

	protected Node(Point c)
	{
		this();
		coord = c;
	}

	protected Node()
	{
		nodeId = -1;
		spdata = new SPData();
		inboundLanes = new Drivelane[0];
		outboundLanes = new Drivelane[0];
		initStats();
	}


	/*============================================*/
	/* Basic GET and SET methods                  */
	/*============================================*/

	/** Returns the Id of this node */
	public int getId()
	{
		return nodeId;
	}

	/** Sets the Id of this node */
	public void setId(int id)
	{
		nodeId = id;
	}

	/** Returns the view coordinates of this node */
	public Point getCoord()
	{
		return coord;
	}

	/** Sets the view coordinates of this node */
	public void setCoord(Point p)
	{
		coord = p;
	}

	/** Returns the statistics for all types of roadusers. */
	public NodeStatistics[] getStatistics()
	{
		return statistics;
	}

	/**
	 * Returns the statistics for the given roaduser type.
	 * @param ruType The roaduser type to return statistics of. (0 if all roadusers)
	 */
	public NodeStatistics getStatistics(int ruType)
	{
		return statistics[RoaduserFactory.ruTypeToStatIndex(ruType)];
	}

	/** Calculates the direction from one road to another.
	 * @param src The position of the source road (expressed in a C_ constant, see above)
	 * @param dest The position of the destination road (expressed in a C_ constant, see above)
	 * @returns The Direction from src to dest (expressed in D_ constants,see above in Object variables of Node)
	 */
	public static int getDirection(int src, int dest)
	{
		int diff = dest - src;
		return diff < 0 ? diff + 4 : diff;
	}

	/** Calculates the number of positions a roaduser can skip in the tail of its destination lane
	 * @param direction The direction in which the roaduser is going
	 * @param laneNumber The position in the road of the lane from which the roaduser is coming. (0=rightmost lane)
	 */
	public int getNumSkip(int direction, int laneNumber) throws InfraException
	{
		switch (direction)
		{
		case D_BACK:
			return getWidth();
		case D_RIGHT:
			return getWidth() - laneNumber;
		case D_AHEAD:
			return 0;
		case D_LEFT:
			return laneNumber;
		}
		throw new InfraException("Illegal direction in Node.getNumSkip");
	}

	//SBC
	/** Returns the phasediff of this node */
	abstract public int getPhaseDiff();
	/** sets the phasediff of this node */
	abstract public void setPhaseDiff(int newPhase);
	/** Returns if this node is on a green wave */
	abstract public boolean isGreenWaveNode();
	/** Sets if this node is on a green wave */
	abstract public void setGreenWaveNode(boolean gw);
	/** Returns if this node is the start of an green wave */
	abstract public boolean isGreenWaveStart();
	/** sets greenWaveStart of this node */
	abstract public void setGreenWaveStart(boolean s);
	/** returns if this node is the finish of an green wave */
	abstract public boolean isGreenWaveFinish();
	/** sets greenWaveFinish of this node */
	abstract public void setGreenWaveFinish(boolean f);
	/** returns if this node is in a clearance period */
	public boolean isAllRed() { return allRed; }
	/** sets if this node is in a clearance period */
	public void setAllRed(boolean r) { allRed = r; } 

	public void setKeepTLDFlag(boolean b) { keepTLDFlag = b; } 
	public boolean getKeepTLDFlag() { return keepTLDFlag; }
	public int getPhaseMinimal() {
		return phaseMinimal;
	}
	/** sets the minimal phase that a decision has to hold */
	public void setPhaseMinimal(int p) {
		phaseMinimal = p;
	}
	public void decrPhaseMinimal() {
		phaseMinimal--;
	}
	public void setPlatoonCrossing(boolean b) { platoonCrossing = b; }
	public boolean isPlatoonCrossing() { return platoonCrossing; }
	//SBC	

	/*============================================*/
	/* Statistics                                 */
	/*============================================*/

	/** Initializes the statistics for this node. */
	public void initStats()
	{
		int[] ruTypes = RoaduserFactory.getConcreteTypes();
		statistics = new NodeStatistics[ruTypes.length + 1];

		resetStats();
		/*
		//GASTON: intent� reiniciar las estadisticas de los lanes aca pero falla cuando quiere cargar la red, ademas 
		//no seria muy correcto ya que no deberia ser responsabilidad de este metodo reiniciar las estadisticas de los drivelanes
		 * 
		 * I tried to reset the statistics of the lanes here but fails when you load the network, plus
		 * not be very correct and it should not be the responsibility of this method resets the statistics of drivelanes
		 * 
		 try {
			Drivelane[] lanes = getAllLanes();
			for (int i = 0; i < lanes.length; i++) {
				Drivelane drivelane = lanes[i];
				drivelane.initStats();
			}
		} catch (InfraException e) {
			e.printStackTrace();
		}*/
	}

	/** Resets the statistics for this node. */
	public void resetStats()
	{
		for (int i = 0; i < statistics.length; i++)
		{
			statistics[i] = new NodeStatistics();
		}
	}

	/**
	 * Processes the statistics of a Roaduser crossing/arriving at this node.
	 * @param ru The Roaduser to process.
	 * @param timeStep The current timeStep.
	 * @param sign The Sign the Roaduser is currently passing by.
	 */
	public void processStats(Roaduser ru, int timeStep, Sign sign)
	{
		//int distance = sign.getLane().getCompleteLength();  /*SBC: commented*/
		double delay = calcDelay(ru, timeStep,  sign.getLane()); /*SBC: distance --> sign.getLane()*/
		/*EJUST: int --> double*/
		
		/*EJUST: calculates the waiting time experienced by the road user due to engaging the queue*/
		int waitingTime = calcWaitingTime(ru, timeStep);
		
		/*EJUST: calculates the absolute stops count experienced by the road user due to engaging the queue*/
		int absoluteStopsCount = ru.getAbsoluteStopsCount();
		
		/*EJUST: calculates the stops count experienced by the road user due to engaging the queue*/
		int stopsCount = ru.getStopsCount();
		
		/*EJUST: calculates the trip time experienced by the road user that just arrives the current special node*/
		int tripTime = timeStep - ru.getTripStartTime();
		
		/*EJUST: calculates the distance experienced by the road user that just arrives the current special node*/
		double distance = ru.getDistance();
		
		/*EJUST comment: add the delay, waiting time, absoluteStopsCount, stopsCount, trip time and distance 
		 * 				for index zero: All roadusers types*/
		statistics[0].addRoaduser(delay, waitingTime /*EJUST*/, 
								  absoluteStopsCount /*EJUST*/, stopsCount /*EJUST*/, 
								  tripTime /*EJUST*/, distance /*EJUST*/); 
		
		/*EJUST comment: add the delay, waiting time, absoluteStopsCount, stopsCount, trip time and distance 
		 * 				for the index concerning the specific roaduser type*/
		statistics[RoaduserFactory.ruTypeToStatIndex(ru.getType())].addRoaduser(delay, waitingTime /*EJUST*/, 
									absoluteStopsCount /*EJUST*/, stopsCount /*EJUST*/,
									tripTime /*EJUST*/, distance /*EJUST*/);
		
		//GASTON: processing statistics for all drivelanes
		//we need to process the drivelane statistics in every timeStep, thus we do not do it here. 
		/*try {
			Drivelane[] lanes = getAllLanes();
			for (int i = 0; i < lanes.length; i++) {
				lanes[i].processStats();
			}
		} catch (InfraException e) {
			e.printStackTrace();
		}*/
	}

	/** Calculates the delay that has to be logged for this Node. */
	protected abstract double calcDelay(Roaduser ru, int timeStep, Drivelane lane); 
	/*SBC: int distance --> Drivelane lane*/
	/*EJUST: int --> double*/

	/** EJUST: Calculates the waiting time that has to be logged for this Node. */
	protected abstract int calcWaitingTime(Roaduser ru, int timeStep); 

	/**
	 * Basic node statistics.
	 * Class to maintain statistics for one roaduser type.
	 * {ALL or CAR or BUS, or BICYCLE}
	 * @author Group Statistics
	 * @version 1.0
	 */
	public class NodeStatistics implements Cloneable, XMLSerializable
	{
		protected int roadusers, tableIndex;
		protected float avgDelay, avgWaitingTime /*EJUST*/, 
						avgAbsoluteStopsCount /*EJUST*/, avgStopsCount /*EJUST*/, 
						avgTripTime /*EJUST*/, avgDistance /*EJUST*/;
		protected double[] delayTable;	  	 /*EJUST: int --> double*/
		protected double[] distanceTable; 	 /*EJUST*/
		protected int[]    waitingTimeTable  /*EJUST*/, absoluteStopsCountTable /*EJUST*/, stopsCountTable /*EJUST*/, tripTimeTable /*EJUST*/;
		protected boolean  tableFilled;

		protected String parentName = "model.infrastructure.node";

		/** Create an (initially empty) statistics data structure. */
		public NodeStatistics()
		{
			avgDelay = avgWaitingTime /*EJUST*/ = 
				avgAbsoluteStopsCount /*EJUST*/ = avgStopsCount /*EJUST*/ = 
					avgTripTime /*EJUST*/ = avgDistance /*EJUST*/ = 0;
			
			tableIndex = roadusers = 0;
			tableFilled = false;
			
			delayTable = new double[STAT_NUM_DATA]; 			/*EJUST: int --> double*/
			waitingTimeTable = new int[STAT_NUM_DATA];  		/*EJUST*/
			
			absoluteStopsCountTable = new int[STAT_NUM_DATA];  	/*EJUST*/
			stopsCountTable = new int[STAT_NUM_DATA];  			/*EJUST*/
			
			tripTimeTable = new int[STAT_NUM_DATA]; 			/*EJUST*/
			distanceTable = new double[STAT_NUM_DATA]; 			/*EJUST*/
		}

		/** Returns a clone of this NodeStatistics. */
		public NodeStatistics getClone()
		{
			NodeStatistics ns = null;
			try
			{
				ns = (NodeStatistics) clone();
				
				double[] odt = ns.getDelayTable(); 				/*EJUST: int --> double*/				
				int[] owt = ns.getWaitingTimeTable(); 			/*EJUST*/
				
				int[] asct = ns.getAbsoluteStopsCountTable(); 	/*EJUST*/
				int[] sct = ns.getStopsCountTable(); 			/*EJUST*/
				
				int[] ott = ns.getTripTimeTable(); 				/*EJUST*/
				double[] odist = ns.getDistanceTable(); 		/*EJUST*/
				
				double[] ndt = new double[odt.length]; 			/*EJUST: int --> double*/
				int[] nwt = new int[owt.length]; 				/*EJUST*/
				
				int[] nasct = new int[asct.length]; 			/*EJUST*/
				int[] nsct = new int[sct.length]; 				/*EJUST*/
				
				int[] ntt = new int[ott.length]; 				/*EJUST*/
				double[] ndist = new double[odist.length];  	/*EJUST*/
				
				for (int i = 0; i < odt.length; i++)
				{
					ndt[i] = odt[i];
					nwt[i] = owt[i];   	  	/*EJUST*/
					
					nasct[i] = asct[i];   	/*EJUST*/
					nsct[i] = sct[i];   	/*EJUST*/
					
					ntt[i] = ott[i];  	  	/*EJUST*/
					ndist[i] = odist[i];  	/*EJUST*/
				}
				
				ns.setDelayTable(ndt);
				ns.setWaitingTimeTable(nwt); 			/*EJUST*/
				
				ns.setAbsoluteStopsCountTable(nasct); 	/*EJUST*/
				ns.setStopsCountTable(nsct); 			/*EJUST*/
				
				ns.setTripTimeTable(ntt); 	 			/*EJUST*/
				ns.setDistanceTable(ndist);  			/*EJUST*/
			}
			catch (CloneNotSupportedException c)
			{}
			return ns;
		}

		/** Returns the delay table. */
		private double[] getDelayTable() /*EJUST: int --> double*/
		{
			return delayTable;
		}

		/** Sets the delay table. */
		private void setDelayTable(double[] dt) /*EJUST: int --> double*/
		{
			delayTable = dt;
		}

		/** EJUST: Returns the waiting time table. */
		private int[] getWaitingTimeTable()
		{
			return waitingTimeTable;
		}

		/** EJUST: Sets the waiting time table. */
		private void setWaitingTimeTable(int[] wt)
		{
			waitingTimeTable = wt;
		}
		
		/** EJUST: Returns the absolute stops count table. */
		private int[] getAbsoluteStopsCountTable()
		{
			return absoluteStopsCountTable;
		}

		/** EJUST: Sets the absolute stops count table. */
		private void setAbsoluteStopsCountTable(int[] asct)
		{
			absoluteStopsCountTable = asct;
		}

		/** EJUST: Returns the stops count table. */
		private int[] getStopsCountTable()
		{
			return stopsCountTable;
		}

		/** EJUST: Sets the stops count table. */
		private void setStopsCountTable(int[] sct)
		{
			stopsCountTable = sct;
		}
		
		/** EJUST: Returns the trip time table. */
		private int[] getTripTimeTable()
		{
			return tripTimeTable;
		}

		/** EJUST: Sets the trip time table. */
		private void setTripTimeTable(int[] tt)
		{
			tripTimeTable = tt;
		}
		
		/** EJUST: Returns the distance table. */
		private double[] getDistanceTable()
		{
			return distanceTable;
		}

		/** EJUST: Sets the distance table. */
		private void setDistanceTable(double[] dt)
		{
			distanceTable = dt;
		}
		
		/** Returns the total number of roadusers that crossed/arrived at this node. */
		public int getTotalRoadusers()
		{
			return roadusers;
		}

		/**
		 * Returns the average delay the roadusers experienced.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 */
		public float getAvgDelay(boolean allTime)
		{
			if (allTime)
			{
				return avgDelay;
			}

			int stopIndex, totalDelay = 0;
			if (tableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalDelay += delayTable[i];
			}
			return stopIndex == 0 ? 0 : (float) totalDelay / stopIndex;
		}

		/**
		 * Returns the average waiting time the roadusers experienced.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 * @author EJUST
		 */
		public float getAvgWaitingTime(boolean allTime)
		{
			if (allTime)
			{
				return avgWaitingTime;
			}

			int stopIndex, totalWaitingTime = 0;
			if (tableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalWaitingTime += waitingTimeTable[i];
			}
			return stopIndex == 0 ? 0 : (float) totalWaitingTime / stopIndex;
		}
		
		/**
		 * Returns the average absolute stops count the roadusers experienced.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 * @author EJUST
		 */
		public float getAvgAbsoluteStopsCount(boolean allTime)
		{
			if (allTime)
			{
				return avgAbsoluteStopsCount;
			}

			int stopIndex, totalAbsoluteStopsCount = 0;
			if (tableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalAbsoluteStopsCount += absoluteStopsCountTable[i];
			}
			return stopIndex == 0 ? 0 : (float) totalAbsoluteStopsCount / stopIndex;
		}
		
		/**
		 * Returns the average stops count the roadusers experienced.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 * @author EJUST
		 */
		public float getAvgStopsCount(boolean allTime)
		{
			if (allTime)
			{
				return avgStopsCount;
			}

			int stopIndex, totalStopsCount = 0;
			if (tableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalStopsCount += stopsCountTable[i];
			}
			return stopIndex == 0 ? 0 : (float) totalStopsCount / stopIndex;
		}
		
		/**
		 * Returns the average trip time the roadusers experienced.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 * @author EJUST
		 */
		public float getAvgTripTime(boolean allTime)
		{
			if (allTime)
			{
				return avgTripTime;
			}

			int stopIndex, totalTripTime = 0;
			if (tableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalTripTime += tripTimeTable[i];
			}
			return stopIndex == 0 ? 0 : (float) totalTripTime / stopIndex;
		}
		
		/**
		 * Returns the average distance the roadusers experienced.
		 * @param allTime Returns an all-time average if true, 
		 * the average of the last STAT_NUM_DATA roadusers otherwise.
		 * @author EJUST
		 */
		public float getAvgDistance(boolean allTime)
		{
			if (allTime)
			{
				return avgDistance;
			}

			int stopIndex;
			double totalDistance = 0;
			if (tableFilled)
			{
				stopIndex = STAT_NUM_DATA;
			}
			else
			{
				stopIndex = tableIndex;
			}
			for (int i = 0; i < stopIndex; i++)
			{
				totalDistance += distanceTable[i];
			}
			return stopIndex == 0 ? 0 :  (float) totalDistance / stopIndex;
		}
		
		/**
		 * Add statistics for one roaduser.
		 * @param delay The delay of this roaduser to be logged.
		 * @param waitingTime The waiting time of this roaduser to be logged
		 * 
		 * @param absoluteStopsCount The absolute stops count of this roaduser to be logged
		 * @param stopsCount The stops count of this roaduser to be logged
		 * 
		 * @param tripTime The trip time of this roaduser to be logged
		 * @param distance The distance of this roaduser to be logged
		 */
		public void addRoaduser(double delay /*EJUST: int --> double*/, int waitingTime /*EJUST*/, 
								int absoluteStopsCount /*EJUST*/, int stopsCount /*EJUST*/,
								int tripTime /*EJUST*/, double distance /*EJUST*/) 		
		{
			roadusers++;
						
			delay = delay > 0 ? delay : 0;
			delayTable[tableIndex] = delay;
			
			/*EJUST*/
			waitingTime = waitingTime > 0 ? waitingTime : 0;
			waitingTimeTable[tableIndex] = waitingTime; 
			
			/*EJUST*/
			absoluteStopsCount = absoluteStopsCount > 0 ? absoluteStopsCount : 0;
			absoluteStopsCountTable[tableIndex] = absoluteStopsCount; 
			
			/*EJUST*/
			stopsCount = stopsCount > 0 ? stopsCount : 0;
			stopsCountTable[tableIndex] = stopsCount; 
			
			/*EJUST*/
			tripTime = tripTime > 0 ? tripTime : 0;
			tripTimeTable[tableIndex] = tripTime; 
			
			/*EJUST*/ 
			distance = distance > 0 ? distance : 0;
			distanceTable[tableIndex] = distance; 
			
			tableIndex++;			
			if (tableIndex == STAT_NUM_DATA)
			{
				tableIndex = 0;
				tableFilled = true;
			}
			
			avgDelay = addToAverage(avgDelay, roadusers, delay);
			avgWaitingTime = addToAverage(avgWaitingTime, roadusers, waitingTime);  					/*EJUST*/
			
			avgAbsoluteStopsCount = addToAverage(avgAbsoluteStopsCount, roadusers, absoluteStopsCount); /*EJUST*/
			avgStopsCount = addToAverage(avgStopsCount, roadusers, stopsCount); 						/*EJUST*/
			
			avgTripTime = addToAverage(avgTripTime, roadusers, tripTime); 								/*EJUST*/
			avgDistance = addToAverage(avgDistance, roadusers, distance); 								/*EJUST*/
		}

		/**
		 * Adds a certain value to an existing average.
		 * @param oldAvg The previous average.
		 * @param newNum The number of samples the new average is based on.
		 * @param value  The new sample to add to this average.
		 */
		private float addToAverage(float oldAvg, float newNum, double value) /*EJUST: int --> double*/
		{
			float tmp = oldAvg * (newNum - 1);
			tmp += value;
			return tmp / newNum;
		}


		//// XMLSerializable implementation ///

		public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
		{
			roadusers = myElement.getAttribute("roadusers").getIntValue();
			
			avgDelay = myElement.getAttribute("avg-waiting-time").getFloatValue();						
			avgWaitingTime = myElement.getAttribute("actual-avg-waiting-time").getFloatValue(); 		/*EJUST*/
			
			avgAbsoluteStopsCount = myElement.getAttribute("avg-absolute-stops-count").getFloatValue();	/*EJUST*/
			avgStopsCount = myElement.getAttribute("avg-stops-count").getFloatValue();					/*EJUST*/
			
			avgTripTime = myElement.getAttribute("avg-trip-time").getFloatValue();						/*EJUST*/
			avgDistance = myElement.getAttribute("avg-distance").getFloatValue();						/*EJUST*/
			
			tableIndex = myElement.getAttribute("table-index").getIntValue();			
			tableFilled = myElement.getAttribute("table-filled").getBoolValue();
						
			delayTable = (double[]) XMLArray.loadArray(this, loader); 							/*EJUST: int --> double*/						
			waitingTimeTable = (int[]) XMLArray.loadArray(this, loader); 						/*EJUST*/
			
			absoluteStopsCountTable = (int[]) XMLArray.loadArray(this, loader); 				/*EJUST*/
			stopsCountTable = (int[]) XMLArray.loadArray(this, loader); 						/*EJUST*/
			
			tripTimeTable = (int[]) XMLArray.loadArray(this, loader);							/*EJUST*/
			distanceTable = (double[]) XMLArray.loadArray(this, loader);						/*EJUST*/
		}

		public XMLElement saveSelf() throws XMLCannotSaveException
		{
			XMLElement result = new XMLElement("statistics");
			
			result.addAttribute(new XMLAttribute("roadusers", roadusers));
			
			result.addAttribute(new XMLAttribute("avg-waiting-time", avgDelay));						
			result.addAttribute(new XMLAttribute("actual-avg-waiting-time", avgWaitingTime)); 			/*EJUST*/
			
			result.addAttribute(new XMLAttribute("avg-absolute-stops-count", avgAbsoluteStopsCount)); 	/*EJUST*/
			result.addAttribute(new XMLAttribute("avg-stops-count", avgStopsCount)); 					/*EJUST*/
			
			result.addAttribute(new XMLAttribute("avg-trip-time", avgTripTime));						/*EJUST*/
			result.addAttribute(new XMLAttribute("avg-distance", avgDistance));							/*EJUST*/
			
			result.addAttribute(new XMLAttribute("table-index", tableIndex));			
			result.addAttribute(new XMLAttribute("table-filled", tableFilled));
			
			return result;
		}

		public void saveChilds(XMLSaver saver) throws XMLTreeException,	IOException, XMLCannotSaveException
		{
			XMLArray.saveArray(delayTable, this, saver, "delay-table");						
			XMLArray.saveArray(waitingTimeTable, this, saver, "waiting-time-table"); 				/*EJUST*/
			
			XMLArray.saveArray(absoluteStopsCountTable, this, saver, "absolute-stops-count-table"); /*EJUST*/
			XMLArray.saveArray(stopsCountTable, this, saver, "stops-count-table"); 					/*EJUST*/
			
			XMLArray.saveArray(tripTimeTable, this, saver, "trip-time-table");						/*EJUST*/
			XMLArray.saveArray(distanceTable, this, saver, "distance-table");						/*EJUST*/
		}

		public String getXMLName()
		{
			return parentName + ".statistics";
		}

		public void setParentName(String parentName) throws XMLTreeException
		{
			this.parentName = parentName;
		}
	}


	/*============================================*/
	/* LOAD and SAVE                              */
	/*============================================*/

	// Generic XMLSerializable implementation for subclasses

	public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
	{
		coord = new Point(myElement.getAttribute("x-pos").getIntValue(), myElement.getAttribute("y-pos").getIntValue());
		nodeId = myElement.getAttribute("node-id").getIntValue();
		if (Model.SAVE_STATS)
		{
			statistics = (NodeStatistics[]) XMLArray.loadArray(this, loader, this);
		}
		else
		{
			initStats();
		}
		spdata = new SPData();
		//GASTON: ac� es donde va a levantar del archivo .infra la informaci�n de todos los shortestPaths para este nodo.        
		// here is where you will pick up the file. below the information of all shortestPaths for this node.
		loader.load(this, spdata);
	}

	public XMLElement saveSelf() throws XMLCannotSaveException
	{
		XMLElement result = new XMLElement("node");
		result.addAttribute(new XMLAttribute("node-id", nodeId));
		result.addAttribute(new XMLAttribute("x-pos", (int) (coord.getX())));
		result.addAttribute(new XMLAttribute("y-pos", (int) (coord.getY())));
		result.addAttribute(new XMLAttribute("type", getType()));
		return result;
	}

	public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException
	{
		if (Model.SAVE_STATS)
		{
			XMLArray.saveArray(statistics, this, saver, "statistics");
		}
		saver.saveObject(spdata);
	}

	public void loadSecondStage(Dictionary dictionaries) throws	XMLInvalidInputException, XMLTreeException
	{
		spdata.loadSecondStage(dictionaries);
	}

	public void setParentName(String parentName)
	{
		this.parentName = parentName;
	}


	public boolean canCreateInstance(Class request)
	{
		return NodeStatistics.class.equals(request);
	}

	public Object createInstance(Class request) throws	ClassNotFoundException, InstantiationException,	IllegalAccessException
	{
		if (NodeStatistics.class.equals(request))
		{
			return new NodeStatistics();
		}
		else
		{
			throw new ClassNotFoundException ("Node IntstantiationAssistant cannot make instances of " + request);
		}
	}


	/*============================================*/
	/* Selectable                                 */
	/*============================================*/


	public Rectangle getBounds()
	{
		int w = getWidth();
		return new Rectangle((int) (coord.x - w * 5), (int) (coord.y - w * 5), w * 10, w * 10);
	}

	public Shape getComplexBounds()
	{
		return getBounds();
	}

	public Point getSelectionPoint()
	{
		return new Point(coord.x + 5, coord.y + 5);
	}

	public Point[] getSelectionPoints()
	{
		return null;
	}

	public Point getCenterPoint()
	{
		return new Point(coord.x + 5, coord.y + 5);
	}

	public int getDistance(Point p)
	{
		return (int) p.distance(getSelectionPoint());
	}

	public boolean hasChildren()
	{
		return getNumAlphaRoads() > 0;
	}

	public boolean isSelectable()
	{
		return true;
	}

	public Enumeration getChildren()
	{
		return new ArrayEnumeration(getAlphaRoads());
	}

	public abstract void paint(Graphics g) throws GLDException;

	public abstract void paint(Graphics g, int offx, int offy, float zf) throws	GLDException;


	/*============================================*/
	/* ABSTRACT GET/SET METHODS                   */
	/*============================================*/


	/** Returns the type of this node */
	public abstract int getType();

	/** Returns the name of this node. The name of a node is unique in an infrastructure. */
	public abstract String getName();

	/** Returns all roads connected to this node. This may contain null pointers */
	public abstract Road[] getAllRoads();

	/** Returns the alpha roads connected to this node */
	public abstract Road[] getAlphaRoads();

	/** Returns the width of this node in number of lanes */
	public abstract int getWidth();


	/** Resets the data of this node. This also resets all AlphaRoads. @see Road#reset() */
	public void reset()
	{
		resetStats();
		
		//SBC
		keepTLDFlag = false;
		phaseMinimal = 0;
		platoonCrossing = false;
	}

	/**
	 * Adds a road at a connection-position.
	 *
	 * @param r The road to add
	 * @param pos The connection-position to connect this road at
	 * @throw InfraException if a road is already connected at the given position
	 * @throw InfraException if the connection-position is out of range
	 * @throw InfraException if r is null
	 */
	public abstract void addRoad(Road r, int pos) throws InfraException;

	/**
	 * Sets a road to be an alpha road
	 *
	 * @param pos The connection-position the road is connected at
	 * @throw InfraException if no road is connected at the given position
	 * @throw InfraException if the connection-position is out of range
	 */
	public abstract void setAlphaRoad(int pos) throws InfraException;

	/**
	 * Removes the road at the given position from this node.
	 *
	 * @param pos The connection-position of the road to remove
	 * @throw InfraException if no road is connected at the given position
	 * @throw InfraException if the connection-position is out of range
	 */
	public abstract void remRoad(int pos) throws InfraException;

	/**
	 * Removes a road from this node.
	 *
	 * @param road The road to remove
	 * @throw InfraException if r is null
	 * @throw InfraException if the given road is not connected to this node
	 */
	public abstract void remRoad(Road r) throws InfraException;

	/**
	 * Removes all roads from this node.
	 */
	public abstract void remAllRoads() throws InfraException;

	/**
	 * Sets the list of signs on this node
	 *
	 * @param s The new signs
	 */
	public abstract void setSigns(Sign[] s) throws InfraException;

	/**
	 * Returns the desired type of signs to add to inbound lanes on this node.
	 */
	public abstract int getDesiredSignType() throws InfraException;

	/**
	 *  Checks if the given road is an alpha road of this node.
	 *
	 * @param r The road to check
	 * @return true if the given road is an alpha road
	 * @throw InfraException if r is null
	 */
	public abstract boolean isAlphaRoad(Road r) throws InfraException;

	/**
	 * Checks if the given road is connected to this node.
	 *
	 * @param r The road to check
	 * @return true if the given road is conencted to this node
	 * @throw InfraException if r is null
	 */
	public abstract boolean isConnected(Road r) throws InfraException;

	/**
	 * Finds the connection-position of a given road.
	 *
	 * @param r The road to find the connection-position of
	 * @return the connection-position of the given road
	 * @throw InfraException if r is null
	 * @throw InfraException if the given road is not connected to this node
	 */
	public abstract int isConnectedAt(Road r) throws InfraException;

	/**
	 * Checks if a road is connected at the given connection-position
	 *
	 * @param pos The position to check
	 * @return true if no road is connected at the given position
	 * @throw InfraException if the given position is not a valid connection-position
	 */
	public abstract boolean isConnectionPosFree(int pos) throws InfraException;

	/** Returns the number of roads connected to this node */
	public abstract int getNumRoads();

	/** Returns the number of alpha roads of this node */
	public abstract int getNumAlphaRoads();

	/** Returns the number of inbound lanes on this node */
	public abstract int getNumInboundLanes() throws InfraException;

	/** Returns the number of outbound lanes on this node */
	public abstract int getNumOutboundLanes() throws InfraException;

	/** Returns the total number of lanes on this node */
	public abstract int getNumAllLanes();

	/** Returns the total number of signs on this node */
	public abstract int getNumSigns(); // should equal getNumInboundLanes()

	/** Returns the number of signs with a type != Sign.NO_SIGN */
	public abstract int getNumRealSigns();

	/**
	 * Returns an array of all inbound lanes supporting roadusers of given type
	 * that lead to the given outbound lane.
	 *
	 * @param lane Outbound lane the lanes lead to
	 * @param ruType The type of roaduser the inbound lanes should support
	 * @return An array of inbound lanes
	 * @throw InfraException if the given lane is not an outbound lane on this node
	 */
	public abstract Drivelane[] getLanesLeadingTo(Drivelane lane, int ruType) throws InfraException;

	/**
	 * GASTON: Returns an array of all available inbound lanes supporting roadusers of given type
	 * that lead to the given outbound lane.
	 *
	 * @param lane Outbound lane the lanes lead to
	 * @param ruType The type of roaduser the inbound lanes should support
	 * @return An array of inbound lanes
	 * @throw InfraException if the given lane is not an outbound lane on this node
	 */
	public abstract Drivelane[] getAvailableLanesLeadingTo(Drivelane lane, int ruType) throws InfraException;

	/**
	 * Returns an array of all outbound lanes supporting roadusers of given type
	 * that can be reached from the given inbound lane.
	 *
	 * @param lane Inbound lane
	 * @param ruType The type of roaduser the outbound lanes should support
	 * @return An array of outbound lanes
	 * @throw InfraException if the given lane is not an inbound lane on this node
	 */
	public abstract Drivelane[] getLanesLeadingFrom(Drivelane lane, int ruType) throws	InfraException;

	/**
	 * GASTON: Returns an array of all available outbound lanes supporting roadusers of given type
	 * that can be reached from the given inbound lane.
	 *
	 * @param lane Inbound lane
	 * @param ruType The type of roaduser the outbound lanes should support
	 * @return An array of outbound lanes
	 * @throw InfraException if the given lane is not an inbound lane on this node
	 */
	public abstract Drivelane[] getAvailableLanesLeadingFrom(Drivelane lane, int ruType) throws InfraException;

	/** Returns an array of all outbound lanes on this node */
	public abstract Drivelane[] getOutboundLanes() throws InfraException;

	/** Returns an array of all inbound lanes on this node */
	public abstract Drivelane[] getInboundLanes() throws InfraException;

	/** GASTON: Returns an array of all available inbound lanes on this node */
	public abstract Drivelane[] getAvailableInboundLanes() throws InfraException;

	/** Returns an array of all lanes on this node */
	public abstract Drivelane[] getAllLanes() throws InfraException;

	// *** DEZE METHODEN NIET WEGHALEN AJB ***
	/** Update inboundLanes,outboundLanes and outboundQueues */
	protected void updateLanes() throws InfraException
	{
		inboundLanes = getInboundLanes();
		outboundLanes = getOutboundLanes();
	}


	/** Returns whether or not all the Tails of all the outbound lanes of this Node are free or not */
	public boolean areAllTailsFree()
	{
		int num_inb = outboundLanes.length;
		for (int i = 0; i < num_inb; i++)
		{
			if (!outboundLanes[i].isTailFree())
			{
				return false;
			}
		}
		return true;
	}


	/*============================================*/
	/* SHORTEST PATH                              */
	/*============================================*/

	/** Sets the shortest path for an exitnode */
	public void setShortestPath(Drivelane lane, int nodeId, int ruType, int length)
	{
		//Sets the shortest path to given exit node using roaduser type to given Drivelane
		spdata.setShortestPath(lane, nodeId, ruType, length);
	}

	/** Adds shortest path for an exitnode */
	public void addShortestPath(Drivelane lane, int nodeId, int ruType,	int length)
	{
		//Adds a Drivelane to the lanes already found for exitNodeId and ruType.
		spdata.addShortestPath(lane, nodeId, ruType, length);
	}

	/** Returns shortest paths for exitnode and Roaduser type */
	public Drivelane[] getShortestPaths(int nodeID, int ruType)
	{	
		//GASTON: se usa durante la simulaci�n
		//Used during simulation
		//System.out.println("Node.Getting the shortestpaths for nodeID: " + nodeID + " and ruType: " + ruType);
		
		// Returns an array of Drivelanes that are on one of the shortest paths 
		// from the node this SPData belongs to, 
		// to the Node with exitNodeId, 
		// for Roadusers with type ruType.
		return spdata.getShortestPaths(nodeID, ruType);
	}

	/** Returns all the exitnode ids this Node has shortestpath data to */
	public int[] getShortestPathDestinations(int ruType)
	{	
		//GASTON: se usa durante la simulaci�n
		//Used during simulation
		
		return spdata.getShortestPathDestinations(ruType);
	}

	/** Removes all lanes on the path towards nodeId with length larger than length */
	public void remPaths(int nodeId, int ruType, int length)
	{
		//Removes all the Drivelanes found for exitNodeId and ruType and length > length
		spdata.remPaths(nodeId, ruType, length);
	}

	/** Removes all lanes on the path towards nodeId  
	 * 
	 * GASTON: AGREGU� ESTE METHOD PARA PODER BORRAR TODOS LOS PATHS DE UN NODO DETERMINADO SIN IMPORTAR EL LENGTH
	 *
	 * Add this METHOD IN ORDER TO DELETE ALL THE PATHS OF A PARTICULAR NODE REGARDLESS OF THE LENGTH
	 * */
	public void remAllPaths(int nodeId, int ruType)
	{
		//Removes all Drivelanes found for exitNodeId and ruType.
		spdata.remAllPaths(nodeId, ruType);
	}

	/** Removes all lanes on the path towards nodeId  
	 * 
	 * GASTON: AGREGU� ESTE METHOD PARA PODER BORRAR TODOS LOS PATHS DE UN NODO DETERMINADO SIN IMPORTAR NADA
	 * 
	 * Add this METHOD IN ORDER TO DELETE ALL THE PATHS OF A PARTICULAR NODE no matter
	 * */
	public void remAllPaths() 
	{
		//ATTEMPTING TO DELETE ALL PATHS THAT HAS A NODE, SO THAT THE CALCULATED SHORTESTPATHCALCULATOR ALL NEW
		spdata.remAllPaths();
	}

	/** Removes all SPData, creating a new instance */
	public void zapShortestPaths()
	{
		spdata = new SPData();
	}
}
