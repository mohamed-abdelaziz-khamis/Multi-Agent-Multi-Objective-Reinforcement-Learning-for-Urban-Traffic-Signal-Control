
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

package gld.sim.stats;

import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Node;
import gld.infra.RoaduserFactory;
import gld.infra.Drivelane.DrivelaneColearnStatistics;
import gld.infra.Drivelane.DrivelaneSpeedStatistics;
import gld.infra.Node.NodeStatistics;
import gld.sim.SimModel;
import gld.sim.stats.sender.AverageSender;
import gld.sim.stats.sender.AverageSenderFactory;
import gld.sim.stats.tracks.JAXBStatisticsUtils;
import gld.sim.stats.tracks.bind.Track;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Observable;
import java.util.ResourceBundle;

/**
*
* Class to maintain statistics as shown in extensions of StatisticsView.
*
* @author Group GUI
* @version 1.0
*/

public class StatisticsModel extends Observable
{
	/** Separator used when saving data. */
	public static String SEP = "\t";
	/** The length of the delay tables maintained in each Node. */
	protected final static int statNumData = Node.STAT_NUM_DATA;
	/** Determines whether to use all time averages or 'last <statNumData> averages' */
	protected boolean allTimeAvg;
	/** The SimModel to get the statistical data from. */
	protected SimModel model;

	// All variables used to maintain the statistics.
	protected String infraName, infraAuthor, simName;
	protected int numNodes, numSpecial, numJunctions, numInboundLanes /*EJUST*/, timeStep;
	
	protected NodeStatistics[][] nodeStats;	
	protected DrivelaneColearnStatistics[][] drivelaneColearnStats; //EJUST
	protected DrivelaneSpeedStatistics[][] drivelaneSpeedStats; //EJUST
	
	protected float allTimeTripDelay, lastXTripDelay, allTimeJunctionDelay, lastXJunctionDelay,
	/*EJUST*/ allTimeTripWT, lastXTripWT, allTimeJunctionWT, lastXJunctionWT, allTimeTripT, lastXTripT,
	/*EJUST*/ allTimeColearnTripWT, lastXColearnTripWT, allTimeColearnJunctionWT, lastXColearnJunctionWT, allTimeColearnTripT, lastXColearnTripT,
	/*EJUST*/ allTimeTotalTripWT, lastXTotalTripWT, allTimeTotalJunctionWT, lastXTotalJunctionWT, allTimeTotalTripT, lastXTotalTripT;
	
	protected int roadusersArrived, junctionCrossings, lastXTripCount, lastXJunctionCount,
	/*EJUST*/ roadusersNotArrivedYet, roadusersNotCrossedYet, lastXTripNotArrivedYetCount, lastXJunctionNotCrossedYetCount,
	/*EJUST*/ roadusersTotalCount, junctionTotalCount, lastXTripTotalCount, lastXJunctionTotalCount;

	/**
	* Creates a <code>StatisticsModel</code>.
	*
	* @param _model The <code>SimModel</code> statistics should be read from.
	*/
	public StatisticsModel(SimModel _model)
	{
		model = _model;
		refresh();
	}


	/*============================================*/
	/* GET AND SET                                */
	/*============================================*/

	/** Sets the SimModel to be used. */
	public void setSimModel(SimModel _model) { model = _model; refresh(); }
	
	/** Returns the SimModel to be shown. */
	public SimModel getSimModel() { return model; }
	
	/** Sets whether all time averages should be used or not. */
	public void setAllTimeAvg(boolean b) 
	{ 
		allTimeAvg = b; 
		setChanged(); 
		notifyObservers(); 
	}
	
	/** Returns whether all time averages should be used or not. */
	public boolean getAllTimeAvg() { return allTimeAvg; }

	/// GET ///
	public String getInfraName() { return infraName; }
	public String getInfraAuthor() { return infraAuthor; }
	public String getSimName() { return simName; }
	
	public int getNumNodes() { return numNodes; }
	public int getNumSpecialNodes() { return numSpecial; }
	public int getNumJunctions() { return numJunctions; }
	public int getNumInboundLanes() { return numInboundLanes; } /*EJUST*/
	
	public int getTimeStep() { return timeStep; }
	
	public NodeStatistics[][] getNodeStatistics() { return nodeStats; }
	
	public float getAllTimeTripDelay() { return allTimeTripDelay; }
	public float getLastXTripDelay() { return lastXTripDelay; }	
	public float getAllTimeJunctionDelay() { return allTimeJunctionDelay; }
	public float getLastXJunctionDelay() { return lastXJunctionDelay; }
	
	/*EJUST*/
	public float getAllTimeTripWT() { return allTimeTripWT; }
	public float getLastXTripWT() { return lastXTripWT; }	
	public float getAllTimeJunctionWT() { return allTimeJunctionWT; }
	public float getLastXJunctionWT() { return lastXJunctionWT; }
	public float getAllTimeTripT() { return allTimeTripT; }
	public float getLastXTripT() { return lastXTripT; }
	
	/*EJUST*/
	public float getAllTimeTotalTripWT() { return allTimeTotalTripWT; }
	public float getLastXTotalTripWT() { return lastXTotalTripWT; }	
	public float getAllTimeTotalJunctionWT() { return allTimeTotalJunctionWT; }
	public float getLastXTotalJunctionWT() { return lastXTotalJunctionWT; }
	public float getAllTimeTotalTripT() { return allTimeTotalTripT; }
	public float getLastXTotalTripT() { return lastXTotalTripT; }
		
	/*EJUST*/
	public float getAllTimeColearnTripWT() { return allTimeColearnTripWT; }
	public float getLastXColearnTripWT() { return lastXColearnTripWT; }	
	public float getAllTimeColearnJunctionWT() { return allTimeColearnJunctionWT; }
	public float getLastXColearnJunctionWT() { return lastXColearnJunctionWT; }
	public float getAllTimeColearnTripT() { return allTimeColearnTripT; }
	public float getLastXColearnTripT() { return lastXColearnTripT; }
	
	public int getRoadusersArrived() { return roadusersArrived; }
	public int getJunctionCrossings() { return junctionCrossings; }
	public int getLastXTripCount() { return lastXTripCount; }
	public int getLastXJunctionCount() { return lastXJunctionCount; }

	/*EJUST*/
	public int getRoadusersNotArrivedYet() { return roadusersNotArrivedYet; }
	public int getRoadusersNotCrossedYet() { return roadusersNotCrossedYet; }
	public int getLastXTripNotArrivedYetCount() { return lastXTripNotArrivedYetCount; }
	public int getLastXJunctionNotCrossedYetCount() { return lastXJunctionNotCrossedYetCount; }
	
	/*EJUST*/
	public int getRoadusersTotalCount() { return roadusersTotalCount; }
	public int getJunctionTotalCount() { return junctionTotalCount; }
	public int getLastXTripTotalCount() { return lastXTripTotalCount; }
	public int getLastXJunctionTotalCount() { return lastXJunctionTotalCount; }

	/*============================================*/
	/* REFRESHING                                 */
	/*============================================*/

	/** Refresh the statistical data from the model and repaint. */
	public void refresh()
	{
		Infrastructure infra = model.getInfrastructure();
		
		infraName 		= infra.getTitle();
		infraAuthor 	= infra.getAuthor();
		simName 		= model.getSimName();
		timeStep 		= model.getCurTimeStep();
		
		numNodes 		= infra.getNumNodes();
		numSpecial 		= infra.getNumSpecialNodes();
		numJunctions 	= infra.getNumJunctions();
		numInboundLanes	= infra.getNumInboundLanes(); /*EJUST*/
		
		NodeStatistics[][] ns = infra.getNodeStatistics();
		DrivelaneColearnStatistics[][] dls = infra.getDrivelaneColearnStatistics(); //EJUST
		DrivelaneSpeedStatistics[][] dlss = infra.getDrivelaneSpeedStatistics(); //EJUST
		
		nodeStats = new NodeStatistics[ns.length][];
		drivelaneColearnStats = new DrivelaneColearnStatistics[dls.length][]; //EJUST
		drivelaneSpeedStats = new DrivelaneSpeedStatistics[dlss.length][]; //EJUST
		
		for(int i=0; i<ns.length; i++) {
			nodeStats[i] = new NodeStatistics[ns[i].length];
			
			for(int j=0; j<ns[i].length; j++)
				nodeStats[i][j] = (NodeStatistics)ns[i][j].getClone();
		}

		/*EJUST*/
		for(int i=0; i<dls.length; i++) {
			drivelaneColearnStats[i] = new DrivelaneColearnStatistics[dls[i].length];
			
			for(int j=0; j<dls[i].length; j++)
				drivelaneColearnStats[i][j] = (DrivelaneColearnStatistics)dls[i][j].getClone();
		}
		
		/*EJUST*/
		for(int i=0; i<dlss.length; i++) {
			drivelaneSpeedStats[i] = new DrivelaneSpeedStatistics[dlss[i].length];
			
			for(int j=0; j<dlss[i].length; j++)
				drivelaneSpeedStats[i][j] = (DrivelaneSpeedStatistics)dlss[i][j].getClone();
		}
		
		roadusersArrived = junctionCrossings = lastXTripCount = lastXJunctionCount = 0;
		
		allTimeTripDelay = lastXTripDelay = allTimeJunctionDelay = lastXJunctionDelay = 0;
		allTimeTripWT = lastXTripWT = allTimeJunctionWT = lastXJunctionWT = allTimeTripT = lastXTripT = 0; /*EJUST*/

		for(int i=0; i<numSpecial; i++) {
			
			//EJUST comment: Total number of roadusers that have arrived at their destination
			int ru = nodeStats[i][0].getTotalRoadusers();
			roadusersArrived += ru; 
			
			//EJUST comment: Average trip delay (based on all roadusers arrived)
			allTimeTripDelay += nodeStats[i][0].getAvgDelay(true) * ru;  
			
			//EJUST: Average trip waiting time (based on all roadusers arrived)
			allTimeTripWT += nodeStats[i][0].getAvgWaitingTime(true) * ru;
			
			//EJUST: Average trip time (based on all roadusers arrived)
			allTimeTripT += nodeStats[i][0].getAvgTripTime(true) * ru;
			
			//EJUST comment: Average trip delay (based on last STAT_NUM_DATA roadusers arrived)
			int tmp = Math.min(ru, statNumData);
			lastXTripDelay += nodeStats[i][0].getAvgDelay(false) * tmp; 
			
			//EJUST: Average trip waiting time (based on last STAT_NUM_DATA roadusers arrived)
			lastXTripWT += nodeStats[i][0].getAvgWaitingTime(false) * tmp;
			
			//EJUST: Average trip time (based on last STAT_NUM_DATA roadusers arrived)
			lastXTripT += nodeStats[i][0].getAvgTripTime(false) * tmp;
						
			lastXTripCount += tmp;
		}
		allTimeTripDelay = roadusersArrived > 0 ? allTimeTripDelay / roadusersArrived : 0;
		lastXTripDelay = lastXTripCount > 0 ? lastXTripDelay / lastXTripCount : 0;

		for(int i=numSpecial; i<numNodes; i++) {
			
			//EJUST comment: Total number of junction crossings
			int ru = nodeStats[i][0].getTotalRoadusers();
			junctionCrossings += ru; 
			
			//EJUST comment: Average junction delay (based on all junction crossings)
			//Junction crossings: total number of roadusers that crossed this junction.
			allTimeJunctionDelay += nodeStats[i][0].getAvgDelay(true) * ru; 
			
			//EJUST: Average junction waiting time (based on all junction crossings)
			allTimeJunctionWT += nodeStats[i][0].getAvgWaitingTime(true) * ru;
			
			//EJUST comment: Average junction delay (based on last STAT_NUM_DATA junction crossings)
			int tmp = Math.min(ru, statNumData);
			lastXJunctionDelay += nodeStats[i][0].getAvgDelay(false) * tmp; 
			
			//EJUST: Average junction waiting time (based on last STAT_NUM_DATA junction crossings)
			lastXJunctionWT += nodeStats[i][0].getAvgWaitingTime(false) * tmp; 
			
			lastXJunctionCount += tmp;			
		}
		allTimeJunctionDelay = junctionCrossings > 0 ? allTimeJunctionDelay / junctionCrossings : 0;		
		lastXJunctionDelay = lastXJunctionCount > 0 ? lastXJunctionDelay / lastXJunctionCount : 0;
		
		/*EJUST*/
		roadusersNotArrivedYet = roadusersNotCrossedYet = lastXTripNotArrivedYetCount = lastXJunctionNotCrossedYetCount = 0;
		allTimeColearnTripWT = lastXColearnTripWT = allTimeColearnJunctionWT = lastXColearnJunctionWT = allTimeColearnTripT = lastXColearnTripT = 0;
		
		/*EJUST*/
		for(int i=0; i<numInboundLanes; i++) {
			
			//Total number of roadusers that move on this drivelane and not arrived at their destination yet
			int ru = drivelaneColearnStats[i][0].getTotalRoadusersNotArrivedYet();
			roadusersNotArrivedYet += ru; 
			
			//Average colearn trip waiting time (based on all roadusers not arrived at their destination yet)
			allTimeColearnTripWT += drivelaneColearnStats[i][0].getAvgColearnTripWaitingTime(true) * ru;
			
			//Average colearn trip time (based on all roadusers not arrived at their destination yet)
			allTimeColearnTripT += (drivelaneSpeedStats[i][0].getAvgTime(true)+
									drivelaneColearnStats[i][0].getAvgExpectedTripTime(true))*ru;
			
			//Average colearn trip waiting time (based on last STAT_NUM_DATA roadusers not arrived at their destination yet)
			int tmp = Math.min(ru, statNumData);
			lastXColearnTripWT += drivelaneColearnStats[i][0].getAvgColearnTripWaitingTime(false) * tmp;
			
			//Average colearn trip time (based on last STAT_NUM_DATA roadusers not arrived at their destination yet)
			lastXColearnTripT += (drivelaneSpeedStats[i][0].getAvgTime(false)+
								  drivelaneColearnStats[i][0].getAvgExpectedTripTime(false))*tmp;
						
			lastXTripNotArrivedYetCount += tmp;
			
			//Total number of roadusers that move on this drivelane and not crossed their current junction yet
			ru = drivelaneColearnStats[i][0].getTotalRoadusersNotCrossedYet();
			roadusersNotCrossedYet += ru; 
			
			//Average colearn junction waiting time (based on all roadusers not crossed their current junction yet)
			allTimeColearnJunctionWT += drivelaneColearnStats[i][0].getAvgColearnJunctionWaitingTime(true) * ru;
			
			//Average colearn junction waiting time (based on last STAT_NUM_DATA roadusers not crossed their current junction yet)
			tmp = Math.min(ru, statNumData);
			lastXColearnJunctionWT += drivelaneColearnStats[i][0].getAvgColearnJunctionWaitingTime(false) * tmp; 
						
			lastXJunctionNotCrossedYetCount += tmp;	
		}
		
		/*EJUST*/
		allTimeTotalTripWT = allTimeTripWT + allTimeColearnTripWT;
		allTimeTotalJunctionWT = allTimeJunctionWT + allTimeColearnJunctionWT;
		allTimeTotalTripT = allTimeTripT + allTimeColearnTripT;
		
		/*EJUST*/
		roadusersTotalCount = roadusersArrived + roadusersNotArrivedYet;
		junctionTotalCount = junctionCrossings + roadusersNotCrossedYet;
				
		/*EJUST*/
		allTimeTotalTripWT = roadusersTotalCount > 0 ? allTimeTotalTripWT / roadusersTotalCount : 0; 
		allTimeTotalJunctionWT = junctionTotalCount > 0 ? allTimeTotalJunctionWT / junctionTotalCount : 0; 
		allTimeTotalTripT = roadusersTotalCount > 0 ? allTimeTotalTripT / roadusersTotalCount : 0; 
		
		/*EJUST*/
		allTimeTripWT = roadusersArrived > 0 ? allTimeTripWT / roadusersArrived : 0;
		allTimeJunctionWT = junctionCrossings > 0 ? allTimeJunctionWT / junctionCrossings : 0;
		allTimeTripT = roadusersArrived > 0 ? allTimeTripT / roadusersArrived : 0;		
		
		/*EJUST*/
		allTimeColearnTripWT = roadusersNotArrivedYet > 0 ? allTimeColearnTripWT / roadusersNotArrivedYet : 0; 
		allTimeColearnJunctionWT = roadusersNotCrossedYet > 0 ? allTimeColearnJunctionWT / roadusersNotCrossedYet : 0; 
		allTimeColearnTripT = roadusersNotArrivedYet > 0 ? allTimeColearnTripT / roadusersNotArrivedYet : 0; 
		
		/*EJUST*/
		lastXTotalTripWT = lastXTripWT + lastXColearnTripWT;
		lastXTotalJunctionWT = lastXJunctionWT + lastXColearnJunctionWT;
		lastXTotalTripT = lastXTripT + lastXColearnTripT;
		
		/*EJUST*/
		lastXTripTotalCount = lastXTripCount + lastXTripNotArrivedYetCount;
		lastXJunctionTotalCount = lastXJunctionCount + lastXJunctionNotCrossedYetCount;
		
		/*EJUST*/
		lastXTotalTripWT = lastXTripTotalCount > 0 ? lastXTotalTripWT / lastXTripTotalCount : 0;
		lastXTotalJunctionWT = lastXJunctionTotalCount > 0 ? lastXTotalJunctionWT / lastXJunctionTotalCount : 0;
		lastXTotalTripT = lastXTripTotalCount > 0 ? lastXTotalTripT / lastXTripTotalCount : 0;
		
		/*EJUST*/
		lastXTripWT = lastXTripCount > 0 ? lastXTripWT / lastXTripCount : 0;
		lastXJunctionWT = lastXJunctionCount > 0 ? lastXJunctionWT / lastXJunctionCount : 0;
		lastXTripT = lastXTripCount > 0 ? lastXTripT / lastXTripCount : 0;
		
		/*EJUST*/
		lastXColearnTripWT = lastXTripNotArrivedYetCount > 0 ? lastXColearnTripWT / lastXTripNotArrivedYetCount : 0;
		lastXColearnJunctionWT = lastXJunctionNotCrossedYetCount > 0 ? lastXColearnJunctionWT / lastXJunctionNotCrossedYetCount : 0;
		lastXColearnTripT = lastXTripNotArrivedYetCount > 0 ? lastXColearnTripT / lastXTripNotArrivedYetCount : 0;
		
		setChanged();
		notifyObservers();
	}
		
	/** GASTON: sends runtime statistics. */
	public void sendStatistics(ResourceBundle rb)
	{
		//refresh(); //ESTO NO ES NECESARIO EN UN PRINCIPIO, PERO SE PODRIA EVALUAR ENVIARLO
		
		Infrastructure infra = model.getInfrastructure();
		Node[] nodes = infra.getAllNodes();
		JAXBStatisticsUtils su = new JAXBStatisticsUtils();
		//Tracks tracks = su.makeNewTracks("");
		for (int i = 0; i < nodes.length; i++) {
			Node node = nodes[i];
			try {				
				Drivelane[] lanes = node.getInboundLanes();
				for (int j = 0; j < lanes.length; j++) {
					
					Drivelane drivelane = lanes[j];
					Drivelane.DrivelaneStatistics dls = drivelane.getStatistics();

					/* EJUST comment:
					 * =============
					 * Every track 'tag' represents a specific lane, e.g., the lane of id = 0
					 *     <track id="0"> 
					 *     		<roadUsersWaiting>0.009708738</roadUsersWaiting> 
					 *     </track> 
					 * Means in lane_id = 0, the average number of roadusers waiting per time step = 0.009708738    
					 */
					
					Track track = su.makeNewTrack(Integer.toString(drivelane.getId()),dls.getAvgNumberRUWaiting());
					su.addItem(track);
					drivelane.initStats();					
				}
			} catch (InfraException e) {
				e.printStackTrace();
			}			
		}
		try{
			AverageSender sender = AverageSenderFactory.getAverageSender(rb);
			PrintWriter out = sender.getSender();
			su.persistTracks(out);
			sender.closeSender(out);
		}
		catch(Exception e){
			System.out.println("Simulator runtime data could not be sent. Please check SenderFactory being used");
			System.out.println("Causes could be (if using socket factory), incorrect port number or host");
		}		
	}
	
	/*============================================*/
	/* SAVING                                     */
	/*============================================*/
	
	/**
	 * Save data to a CSV file. 
	 */
	protected void saveData(String filename) throws IOException
	{
		int[] ruTypes = new int[nodeStats[0].length];
		for(int i=0; i<ruTypes.length; i++)
			ruTypes[i] = RoaduserFactory.statIndexToRuType(i);
		
		PrintWriter out=new PrintWriter(new FileWriter(new File(filename)));
		out.println("# Data exported by Green Light District"); out.println("#");
		
		Infrastructure infra = model.getInfrastructure();
		out.println("# Infrastructure: \"" + infra.getTitle() + "\" by " + infra.getAuthor());
		out.println("# Simulation: \"" + model.getSimName() + "\""); out.println("#");
		out.println("# Data at timeStep: " + timeStep);
		out.println("# #nodes = " + numNodes + ", #specialnodes = " + numSpecial + ", #junctions = " + numJunctions + /*EJUST*/", #inboundlanes = " + numInboundLanes );
		out.println("#"); 
		
		out.println("# Total number of roadusers that have arrived at their destinations: " + roadusersArrived); 
		out.println("# Total number of roadusers that have not arrived at their destinations yet: " + roadusersNotArrivedYet); /*EJUST*/
		out.println("# Total number of roadusers that have arrived at their destinations or not arrived yet: " + roadusersTotalCount); /*EJUST*/
		
		out.println("# Average trip delay (based on all roadusers arrived): " + allTimeTripDelay);
		
		out.println("# Average trip waiting time (based on all roadusers arrived): " + allTimeTripWT); /*EJUST*/
		out.println("# Average trip waiting time (based on all roadusers not arrived yet): " + allTimeColearnTripWT); /*EJUST*/
		out.println("# Average trip waiting time (based on all roadusers arrived or not arrived yet): " + allTimeTotalTripWT); /*EJUST*/
		
		out.println("# Average trip time (based on all roadusers arrived): " + allTimeTripT); /*EJUST*/
		out.println("# Average trip time (based on all roadusers not arrived yet): " + allTimeColearnTripT); /*EJUST*/
		out.println("# Average trip time (based on all roadusers arrived or not arrived yet): " + allTimeTotalTripT); /*EJUST*/
		
		if(roadusersArrived != lastXTripCount) {
			out.println("# Average trip delay (based on last " + lastXTripCount + " roadusers arrived): " + lastXTripDelay);
			
			out.println("# Average trip waiting time (based on last " + lastXTripCount + " roadusers arrived): " + lastXTripWT); /*EJUST*/
			out.println("# Average trip time (based on last " + lastXTripCount + " roadusers arrived): " + lastXTripT); /*EJUST*/
		}
		
		/*EJUST*/
		if(roadusersNotArrivedYet != lastXTripNotArrivedYetCount) {			
			out.println("# Average trip waiting time (based on last " + lastXTripNotArrivedYetCount + " roadusers not arrived yet): " + lastXColearnTripWT);
			out.println("# Average trip time (based on last " + lastXTripNotArrivedYetCount + " roadusers not arrived yet): " + lastXColearnTripT);
		}
		
		/*EJUST*/
		if(roadusersTotalCount != lastXTripTotalCount) {			
			out.println("# Average trip waiting time (based on last " + lastXTripTotalCount + " roadusers arrived or not arrived yet): " + lastXTotalTripWT);
			out.println("# Average trip time (based on last " + lastXTripTotalCount + " roadusers arrived or not arrived yet): " + lastXTotalTripT);
		}
		
		out.println("# Total number of junction crossings: " + junctionCrossings); 
		out.println("# Total number of roadusers that have not crossed their current junctions yet: " + roadusersNotCrossedYet); /*EJUST*/
		out.println("# Total number of junction crossings or roadusers not crossed yet: " + junctionTotalCount); /*EJUST*/
		
		out.println("# Average junction delay (based on all junction crossings): " + allTimeJunctionDelay); 
		
		/*EJUST*/
		out.println("# Average junction waiting time (based on all junction crossings): " + allTimeJunctionWT); 
		out.println("# Average junction waiting time (based on all roadusers not crossed yet): " + allTimeColearnJunctionWT);
		out.println("# Average junction waiting time (based on all junction crossings or roadusers not crossed yet): " + allTimeTotalJunctionWT);
		
		if(junctionCrossings != lastXJunctionCount){
			out.println("# Average junction delay (based on last " + lastXJunctionCount + " junction crossings): " + lastXJunctionDelay);			
			out.println("# Average junction waiting time (based on last " + lastXJunctionCount + " junction crossings): " + lastXJunctionWT); /*EJUST*/
		}
		
		/*EJUST*/
		if(roadusersNotCrossedYet != lastXJunctionNotCrossedYetCount){			
			out.println("# Average junction waiting time (based on last " + lastXJunctionNotCrossedYetCount + " roadusers not crossed yet): " + lastXColearnJunctionWT);
		}
		
		/*EJUST*/
		if(junctionTotalCount != lastXJunctionTotalCount){			
			out.println("# Average junction waiting time (based on last " + lastXJunctionTotalCount + " junction crossings or roadusers not crossed yet): " + lastXTotalJunctionWT);
		}
		
		out.println("#"); 
		out.println("#"); 
		
		out.println("# EdgeNodes"); 
		
		out.println("# Data format: " + "<id"+SEP+"ruType"+
										SEP+"roadusersArrived"+
										SEP+"avgTripDelayAllTime"+SEP+"avgTripDelayLast" + Node.STAT_NUM_DATA + 
							/*EJUST*/	SEP+"avgTripWaitingTimeAllTime"+SEP+"avgTripWaitingTimeLast"+ Node.STAT_NUM_DATA + 
							/*EJUST*/ 	SEP+"avgTripTimeAllTime"+SEP+"avgTripTimeLast"+Node.STAT_NUM_DATA + ">"); 
		
		out.println("#");
		
		for(int id=0; id<numSpecial; id++) {
			
			for(int statIndex=0; statIndex < nodeStats[id].length; statIndex++) {
				NodeStatistics ns = nodeStats[id][statIndex];
				out.println(id + SEP + ruTypes[statIndex] + 
										SEP + ns.getTotalRoadusers() +
										SEP + ns.getAvgDelay(true) + SEP + ns.getAvgDelay(false)+
							/*EJUST*/	SEP + ns.getAvgWaitingTime(true) + SEP + ns.getAvgWaitingTime(false)+
							/*EJUST*/	SEP + ns.getAvgTripTime(true) + SEP + ns.getAvgTripTime(false));
			}
		}
		
		out.println("#"); 
		out.println("#"); 
		
		out.println("# Junctions"); 
		
		out.println("# Data format: " + "<id"+SEP+"ruType"+
										SEP+"roadusersCrossed"+
										SEP+"avgJunctionDelayAllTime"+SEP+"avgJunctionDelayLast" + Node.STAT_NUM_DATA + 
							/*EJUST*/	SEP+"avgJunctionWaitingTimeAllTime"+SEP+"avgJunctionWaitingTimeLast"+Node.STAT_NUM_DATA + ">"); 
		
		out.println("#");
		
		for(int id=numSpecial; id<numNodes; id++) {
			
			for(int statIndex=0; statIndex < nodeStats[id].length; statIndex++) {
				NodeStatistics ns = nodeStats[id][statIndex];
				out.println(id + SEP + ruTypes[statIndex] + 
										SEP + ns.getTotalRoadusers() +
										SEP + ns.getAvgDelay(true) + SEP + ns.getAvgDelay(false)+
							/*EJUST*/	SEP + ns.getAvgWaitingTime(true) + SEP + ns.getAvgWaitingTime(false));
			}
		}
		
		/*EJUST*/
		out.println("#"); 
		out.println("#"); 
		
		out.println("# Inbound Lanes"); 
		
		out.println("# Data format: " + "<id"+SEP+"ruType"+
										SEP+"roadusersNotArrivedYet"+
										SEP+"avgColearnTripWaitingTimeAllTime"+SEP+"avgColearnTripWaitingTimeLast"+ Drivelane.STAT_NUM_DATA +										
										SEP+"avgColearnTripTimeAllTime"+SEP+"avgColearnTripTimeLast"+ Drivelane.STAT_NUM_DATA + 
										SEP+"roadusersNotCrossedYet"+
										SEP+"avgColearnJunctionWaitingTimeAllTime"+SEP+"avgColearnJunctionWaitingTimeLast"+ Drivelane.STAT_NUM_DATA + ">"); 
		
		out.println("#");
		
		/*EJUST*/
		for(int id=0; id<numInboundLanes; id++) {
			
			for(int statIndex=0; statIndex < drivelaneColearnStats[id].length; statIndex++) {
				DrivelaneColearnStatistics dls = drivelaneColearnStats[id][statIndex];
				DrivelaneSpeedStatistics dlss = drivelaneSpeedStats[id][statIndex];
				out.println(id + SEP + ruTypes[statIndex] + 
										SEP + dls.getTotalRoadusersNotArrivedYet() +
										SEP + dls.getAvgColearnTripWaitingTime(true) + 
										SEP + dls.getAvgColearnTripWaitingTime(false)+
										SEP + String.valueOf(dlss.getAvgTime(true)+dls.getAvgExpectedTripTime(true)) + 
										SEP + String.valueOf(dlss.getAvgTime(false)+dls.getAvgExpectedTripTime(false)) +
										SEP + dls.getTotalRoadusersNotCrossedYet() +
										SEP + dls.getAvgColearnJunctionWaitingTime(true) + 
										SEP + dls.getAvgColearnJunctionWaitingTime(false));
			}
		}
		
		out.close();
	}	
}