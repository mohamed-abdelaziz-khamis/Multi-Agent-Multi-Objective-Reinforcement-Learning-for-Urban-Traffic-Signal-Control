
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

package gld.algo.dp;

import gld.algo.tlc.TLController;
import gld.idm.Constants;
import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Road;
import gld.infra.Roaduser;
import gld.sim.SimModel;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLTreeException;

import java.io.IOException;

/**
 *
 * This extension of {@see gld.DrivingPolicy} selects the next lane 
 * by finding one which is on the shortest path to road user's destination.
 *
 * @author Chaim Z
 * @version 1.0
 */
public class AggressiveDP extends DrivingPolicy implements Constants /*EJUST*/
{
	public static final String shortXMLName="dp-agr";
	
	/**
	 * The constructor for a shortest driving policy. 
	 * @param m The model which is used
	 */
    
	public AggressiveDP(SimModel sim, TLController _tlc) {
		super(sim,_tlc);
	}
    

	/**
	 * The lane to which a car continues his trip.
	 * @param r The road user being asked.
	 * @param allOutgoing All the possible outgoing lanes
	 * @param shortest All the lanes which are in a shortest path to the car's destination
	 * @return The chosen lane.
	 */
	public Drivelane getDirectionLane(Roaduser r, Drivelane lane_now, Drivelane[] allOutgoing, Drivelane[] shortest) {
		//Create a subset from the 2 sets allOutgoing and shortest
		Drivelane current;
		Drivelane best_lane = null;
		int best_waiting = Integer.MAX_VALUE;
		int num_outgoing = allOutgoing.length;
		int num_shortest = shortest.length;

		for(int i=0; i<allOutgoing.length; i++) {
			current = allOutgoing[i];
			for(int j=0; j<shortest.length; j++) {
				if(current.getId() == shortest[j].getId())
				    if(current.getNumRoadusersWaiting()<best_waiting) {
					    best_lane = shortest[j];
					    best_waiting = current.getNumRoadusersWaiting();
				    }
		    }
	    }
	    return best_lane;
	}
		
	private boolean checkDrivelane(Drivelane testLane, Drivelane origLane, Roaduser ru, 
								   Drivelane[] shortest) throws InfraException
	{
		if (testLane==null) 
			return false;

		/* Test if the drivelane has the same or more targets as the original dl.
		 * EJUST: As the original lane may be directed right or straight
		 * And the left lane may be directed left
		 * So, if the roaduser change lane it will not reach to its destination*/
		boolean found = true;
		
		Drivelane nextDL = getDirectionLane(ru, origLane, origLane.getSign().getNode().getOutboundLanes(), shortest);
		
		int direction = determineDirection(testLane, nextDL);
		
		/* The directions Roadusers switch lanes to: left:0, straight ahead:1 and right:2.*/
		if (origLane.getTarget(direction) && !testLane.getTarget(direction)) 
			found = false;
		
		for (int i=0; i<shortest.length; i++) 
			if (shortest[i]==testLane) 
				found = true;
		
		if (!found) 
			return false;
				
		// Check whether this roaduser may enter that road, i.e. the types should be correct.
		if ((testLane.getType() & ru.getType())==0) 
			return false;

		// Is the position free?
		if (!testLane.isPosFree(ru.getPosition(),ru.getLength())) 
			return false;
		
		// Is the next position free?, otherwise it is useless...
		if (ru.getPosition()<=STOP_SPEED_MS*TIMESTEP_S /*EJUST replaced: ru.getPosition()==0*/) 
			return false; 
		
		if (testLane.getNumRoadusersWaiting()>0)
		  if (!testLane.isPosFree(ru.getPosition()-1,1)) 
			  return false;
		
		return true;		
	}

	/**
	 * Checks the drivelanes left and right of this lane, to see if an aggressive roaduser can switch drivelane.
	 * @param lane The lane, the roaduser is now on.
	 * @param ru The roaduser
	 * @param speed_left The left speed of this roaduser
	 * @param shortest All the lanes which are in a shortest path to the car's destination
	 * @return True when the roaduser has switched, false otherwise.
	 */
	public boolean checkNeighbourLanes(Drivelane lane, Roaduser ru, double speed_left /*EJUST: int --> double*/,
						Drivelane[] shortest) throws InfraException
	{
		Road r = lane.getRoad();
		Drivelane [] lanes = r.getAlphaLanes();
		
		// search for this lane
		int index = -1;
		for (int i=0; i<lanes.length; i++) 
			if (lanes[i]==lane) 
				index = i;
		
		if (index==-1) 
		{
			lanes = r.getBetaLanes();
			for (int i=0; i<lanes.length; i++) if (lanes[i]==lane) index=i;
		}
		
		Drivelane leftDL = null, rightDL = null;
		
		if (index>0) 
			leftDL = lanes[index-1];
		
		if (index<lanes.length-1) 
			rightDL= lanes[index+1];
		
					
		if (checkDrivelane(leftDL, lane, ru, shortest))
		{
			try
			{
				double ru_pos=ru.getPosition()-1; /*EJUST: int --> double*/
				leftDL.addRoaduser(ru,ru_pos);
				ru.setPosition(ru_pos);
			}
			catch (InfraException e)
			{
				System.out.println(e+"");
				e.printStackTrace();
			}
			//System.out.println("I am aggressive, I am going left...");
			return true;
		}
		
		if (checkDrivelane(rightDL, lane, ru, shortest))
		{
			try
			{
				double ru_pos=ru.getPosition()-1; /*EJUST: int --> double*/
				rightDL.addRoaduser(ru,ru_pos);
				ru.setPosition(ru_pos);
			}
			catch (InfraException e)
			{
				System.out.println(e+"");
				e.printStackTrace();
			}
			//System.out.println("I am aggressive, I am going right...");
			return true;
		}
		return false;
	}
	
	// Trivial XMLSerializable implementation
	
	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{ 	
		System.out.println("DP AGR loaded");
	}
	
	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	
		return new XMLElement(shortXMLName);
	}
  
	public String getXMLName ()
	{ 	
		return "model."+shortXMLName;
	}
}