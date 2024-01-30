
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

import gld.infra.*;
import gld.sim.SimModel;
import gld.sim.SimController;
import gld.GLDException;

import java.util.Vector;
import java.util.Enumeration;

/**
 *
 * TrackerFactory shows a TrackingController with a TrackingView of a given type.
 *
 * @author Group GUI
 * @version 1.0
 */

public class TrackerFactory
{
	/** Show the average total trip delay tracking window. */
	public final static int TOTAL_DELAY = 0;
	/** Show the average junction delay tracking window. */
	public final static int TOTAL_JUNCTION_DELAY = 1;
	/** Show the total waiting queue length tracking window. */
	public final static int TOTAL_QUEUE = 2;
	/** Show the total roadusers arrived tracking window. */
	public final static int TOTAL_ROADUSERS = 3;
	
	
	/** Show the single SpecialNode average trip delay tracking window. */
	public final static int SPECIAL_DELAY = 4;
	/** Show the single EdgeNode waiting queue length tracking window. */
	public final static int SPECIAL_QUEUE = 5;
	/** Show the single SpecialNode roadusers arrived tracking window. */
	public final static int SPECIAL_ROADUSERS = 6;
	
	
	/** Show the single Junction average junction delay tracking window. */
	public final static int JUNCTION_DELAY = 7;
	/** Show the single Junction roadusers tracking window. */
	public final static int JUNCTION_ROADUSERS = 8;
	/** Show the single NetTunnel send queue tracking window */
	public final static int NETTUNNEL_SEND = 9;
	/** Show the single NetTunnel receive queue tracking window */
	public final static int NETTUNNEL_RECEIVE = 10;

	/** Show the number of accidents (DOAS 06) */
	public final static int ACCIDENTS_COUNT = 11;
	/** Show the number of removed cars (DOAS 06) */
	public final static int REMOVEDCARS_COUNT = 12;

	/*POMDPGLD*/
    /** Show the size all Beliefstate vectors summed together */
    public final static int BELIEFSTATE_SIZE = 13;
    /** Show the percentage of probability density covered by the Beliefstate */
    public final static int BELIEFSTATE_COVERAGE = 14;

    public final static int AVERAGE_LANE_LOAD = 15;
    public final static int MAX_LANE_LOAD = 16;

    public final static int STATE_ZEROS = 17;
	/*POMDPGLD*/
	
	/**EJUST: Show the percentage of road users arrived as opposed to the total road users entered tracking window.*/
	public final static int ARRIVED_ROADUSERS_PERCENTAGE = 18;
    
	/**EJUST: Show the percentage of road users rejected as opposed to the total road users generated tracking window.*/
	public final static int REJECTED_ROADUSERS_PERCENTAGE = 19;
	
	/**EJUST: Show the single SpecialNode roadusers rejected/generated tracking window.*/
	public final static int SPECIAL_REJECTED_ROADUSERS = 20;
	
	/**EJUST: Show the average total trip waiting time tracking window. */
	public final static int TOTAL_WAITING_TIME = 21;
	
	/**EJUST: Show the average junction waiting time tracking window. */
	public final static int TOTAL_JUNCTION_WAITING_TIME = 22;

	/**EJUST: Show the single SpecialNode average trip waiting time tracking window. */
	public final static int SPECIAL_WAITING_TIME = 23;

	/**EJUST: Show the single Junction average junction waiting time tracking window. */
	public final static int JUNCTION_WAITING_TIME = 24;
	
	/**EJUST: Show the average total trip time tracking window. */
	public final static int TOTAL_TRIP_TIME = 25;
	
	/**EJUST: Show the single SpecialNode average trip time tracking window. */
	public final static int SPECIAL_TRIP_TIME = 26;
	
	/**EJUST: Show the total colearn average trip waiting time tracking window. */
	public final static int TOTAL_COLEARN_WAITING_TIME = 27;
	
	/**EJUST: Show the total colearn average junction waiting time tracking window. */
	public final static int TOTAL_COLEARN_JUNCTION_WAITING_TIME = 28;
	
	/**EJUST: Show the total colearn average trip time tracking window. */
	public final static int TOTAL_COLEARN_TRIP_TIME = 29;
	
	/**EJUST: Show the total roadusers not arrived yet tracking window. */
	public final static int TOTAL_ROADUSERS_NOT_ARRIVED_YET = 30;
	
	/**EJUST: Show the total average speed of all roadusers that either have/haven't arrived yet tracking window. */
	public final static int TOTAL_AVERAGE_SPEED = 31;
	
	/**EJUST: Show the average number of roadusers waiting for the Sign of all Drivelanes tracking window. */
	public final static int AVERAGE_LANE_NUM_ROADUSERS_WAITING = 32;
	
	/**EJUST: Show the maximum number of roadusers waiting for the Sign of all Drivelanes tracking window. */
	public final static int MAX_LANE_NUM_ROADUSERS_WAITING = 33;
	
	/**EJUST: Show the average total trip absolute stops count tracking window. */
	public final static int TOTAL_ABSOLUTE_STOPS_COUNT = 34;
	
	/**EJUST: Show the single SpecialNode average trip absolute stops count tracking window. */
	public final static int SPECIAL_ABSOLUTE_STOPS_COUNT = 35;
	
	/**EJUST: Show the average total trip stops count tracking window. */
	public final static int TOTAL_STOPS_COUNT = 36;
	
	/**EJUST: Show the single SpecialNode average trip stops count tracking window. */
	public final static int SPECIAL_STOPS_COUNT = 37;
	
	/**EJUST: Show the total colearn average speed of all roadusers that either have/haven't arrived yet tracking window. */
	public final static int TOTAL_COLEARN_AVERAGE_SPEED = 38;
	
	/**EJUST: Show the single Junction green time percentage tracking window. */
	public final static int JUNCTION_GREEN_TIME_PERCENTAGE = 39;
	
	protected static Vector trackingControllers = new Vector();

	/**
	* Shows one of the 'global' tracking windows.
	* @param type One of the 'TOTAL_' constants.
	*/
 	public static TrackingController showTracker(SimModel model, SimController controller, int type) throws GLDException
	{
		
 		
 		if(type == TOTAL_QUEUE) {
			TrackingView view = new AllQueuesTrackingView(model.getCurTimeStep(), model);
			return genTracker(model, controller, view);
		}
 		
        //EJUST
        else if(type == ARRIVED_ROADUSERS_PERCENTAGE){
        	TrackingView view = new ArrivedRoadusersPercentageTrackingView(model.getCurTimeStep(), model);
        	return genTracker(model, controller, view);
        }
		
        //EJUST
        else if(type == REJECTED_ROADUSERS_PERCENTAGE){
        	TrackingView view = new RejectedRoadusersPercentageTrackingView(model.getCurTimeStep(), model);
        	return genTracker(model, controller, view);
        }

        //EJUST
        else if(type == AVERAGE_LANE_NUM_ROADUSERS_WAITING){
        	TrackingView view = new AverageLanesNumRoadusersWaitingTrackingView(model.getCurTimeStep(), model);
        	return genTracker(model, controller, view);
        }
 		
        //EJUST
        else if(type == MAX_LANE_NUM_ROADUSERS_WAITING){
        	TrackingView view = new MaxLanesNumRoadusersWaitingTrackingView(model.getCurTimeStep(), model);
        	return genTracker(model, controller, view);
        }
 		
		ExtendedTrackingView view = null;
		
		if(type == TOTAL_JUNCTION_DELAY)
			view = new AllJunctionsDelayTrackingView(model.getCurTimeStep(), model);
		else if(type == TOTAL_DELAY)
			view = new TotalDelayTrackingView(model.getCurTimeStep(), model);
		else if(type == TOTAL_ROADUSERS)
			view = new TotalRoadusersTrackingView(model.getCurTimeStep(), model);
		else if(type == ACCIDENTS_COUNT)
			view = new AccidentsCountTrackingView(model.getCurTimeStep(), model);
		else if(type == REMOVEDCARS_COUNT)
			view = new RemovedCarsTrackingView(model.getCurTimeStep(), model);
		
		/*POMDPGLD*/
        else if(type == BELIEFSTATE_SIZE)
			view = new BeliefStateSizeTrackingView(model.getCurTimeStep(), model);
        else if(type == BELIEFSTATE_COVERAGE)
			view = new BeliefstateCoverageTrackingView(model.getCurTimeStep(), model);
        else if(type == AVERAGE_LANE_LOAD)
			view = new AverageLanesLoadTrackingView(model.getCurTimeStep(), model);
        else if(type == MAX_LANE_LOAD)
			view = new MaxLanesLoadTrackingView(model.getCurTimeStep(), model);
        else if(type == STATE_ZEROS)
			view = new StateZeroTrackingView(model.getCurTimeStep(), model);
		/*POMDPGLD*/
        
		//EJUST
		if(type == TOTAL_JUNCTION_WAITING_TIME)
			view = new AllJunctionsWaitingTimeTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_WAITING_TIME)
			view = new TotalWaitingTimeTrackingView(model.getCurTimeStep(), model);

		//EJUST
		else if(type == TOTAL_TRIP_TIME)
			view = new TotalTripTimeTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_COLEARN_WAITING_TIME)
			view = new TotalColearnWaitingTimeTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_COLEARN_TRIP_TIME)
			view = new TotalColearnTripTimeTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_COLEARN_JUNCTION_WAITING_TIME)
			view = new AllJunctionsColearnWaitingTimeTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_ROADUSERS_NOT_ARRIVED_YET)
			view = new TotalRoadusersNotArrivedYetTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_AVERAGE_SPEED)
			view = new TotalAverageSpeedTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_ABSOLUTE_STOPS_COUNT)
			view = new TotalAbsoluteStopsCountTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_STOPS_COUNT)
			view = new TotalStopsCountTrackingView(model.getCurTimeStep(), model);
		
		//EJUST
		else if(type == TOTAL_COLEARN_AVERAGE_SPEED)
			view = new TotalColearnAverageSpeedTrackingView(model.getCurTimeStep(), model);
		
		if(view == null) throw new GLDException("Invalid tracker type!");
		return genExtTracker(model, controller, view);
	}

	/**
	* Shows one of the EdgeNode tracking windows.
	* @param type One of the 'EDGE_' constants.
	*/
 	public static TrackingController showTracker(SimModel model, SimController	controller, SpecialNode node, int type) throws GLDException
	{	
 		if(type == SPECIAL_QUEUE && node instanceof SpecialNode) 
 		{
			TrackingView view = new	SpecialNodeQueueTrackingView(model.getCurTimeStep(),(SpecialNode)node);
			return genTracker(model, controller, view);
		}
		else if (type==NETTUNNEL_SEND && node instanceof NetTunnel)
		{	
			TrackingView view = new	NetTunnelSendQueueTrackingView(model.getCurTimeStep(),(NetTunnel)node);
			return genTracker(model, controller, view);
		}
		
 		/*EJUST*/
		else if(type == SPECIAL_REJECTED_ROADUSERS)
		{
			TrackingView view = new SpecialNodeRejectedRoadusersTrackingView(model.getCurTimeStep(), node);
			return genTracker(model, controller, view);
		}
 		
 		ExtendedTrackingView view = null;
 		
		if(type == SPECIAL_DELAY)
			view = new SpecialNodeDelayTrackingView(model.getCurTimeStep(), node);
		
		else if(type == SPECIAL_ROADUSERS)
			view = new NodeRoadusersTrackingView(model.getCurTimeStep(), node);
		
		/*EJUST*/
		else if(type == SPECIAL_WAITING_TIME)
			view = new SpecialNodeWaitingTimeTrackingView(model.getCurTimeStep(), node);
		
		/*EJUST*/
		else if(type == SPECIAL_ABSOLUTE_STOPS_COUNT)
			view = new SpecialNodeAbsoluteStopsCountTrackingView(model.getCurTimeStep(), node);
		
		/*EJUST*/
		else if(type == SPECIAL_STOPS_COUNT)
			view = new SpecialNodeStopsCountTrackingView(model.getCurTimeStep(), node);
		
		/*EJUST*/
		else if(type == SPECIAL_TRIP_TIME)
			view = new SpecialNodeTripTimeTrackingView(model.getCurTimeStep(), node);
		
		if(view == null) throw new GLDException("Invalid tracker type!");
		return genExtTracker(model, controller, view);
	}

	/**
	* Shows the specified Junction tracking window.
	*/
 	public static TrackingController showTracker(SimModel model, SimController controller, Junction junction, int type) throws GLDException
	{
		ExtendedTrackingView view = null;
		
		if(type == JUNCTION_DELAY)
			view = new JunctionDelayTrackingView(model.getCurTimeStep(), junction);
		
		else if(type == JUNCTION_ROADUSERS)
			view = new NodeRoadusersTrackingView(model.getCurTimeStep(), junction);

		/*EJUST*/
		else if(type == JUNCTION_WAITING_TIME)
			view = new JunctionWaitingTimeTrackingView(model.getCurTimeStep(), junction);
		
		/*EJUST*/
		else if(type == JUNCTION_GREEN_TIME_PERCENTAGE)
			view = new JunctionGreenTimePercentageTrackingView(model.getCurTimeStep(), junction);
		
		if(view == null) throw new GLDException("Invalid tracker type!");
		
		return genExtTracker(model, controller, view);
	}


	protected static TrackingController genTracker(SimModel model, SimController controller, TrackingView view)
	{
		TrackingController tc = new TrackingController(model, controller, view);
		trackingControllers.add(tc);
		return tc;
	}

	public static TrackingController genExtTracker(SimModel model, SimController controller, ExtendedTrackingView view)
	{
		TrackingController tc = new ExtendedTrackingController(model, controller, view);
		trackingControllers.add(tc);
		return tc;
	}

	public static void purgeTrackers()
	{
		Enumeration enumr = trackingControllers.elements();
		while(enumr.hasMoreElements())
			((TrackingController)enumr.nextElement()).closeWindow();
	}

	public static void disableTrackerViews()
	{
		Enumeration enumr = trackingControllers.elements();
		while(enumr.hasMoreElements())
			((TrackingController)enumr.nextElement()).setViewEnabled(false);
	}

	/*POMDPGLD*/
    public static void enableTrackerViews()
    {
        Enumeration enumr = trackingControllers.elements();
        while(enumr.hasMoreElements())
            ((TrackingController)enumr.nextElement()).setViewEnabled(true);
    }
	
	public static void resetTrackers()
	{
		Enumeration enumr = trackingControllers.elements();
		while(enumr.hasMoreElements())
			((TrackingController)enumr.nextElement()).reset();
	}

	public static TrackingController[] getTrackingControllers()
	{
		TrackingController[] tca = new TrackingController[trackingControllers.size()];
		Enumeration enumr = trackingControllers.elements();
		int i=0;
		while(enumr.hasMoreElements()) {
			tca[i] = ((TrackingController)enumr.nextElement());
			i++;
		}
		return tca;
	}
}