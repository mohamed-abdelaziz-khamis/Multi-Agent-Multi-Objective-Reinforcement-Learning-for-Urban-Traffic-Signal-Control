
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

import gld.GLDSim;
import gld.algo.tlc.TLController;
import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Node;
import gld.infra.Road;
import gld.infra.Roaduser;
import gld.sim.SimModel;
import gld.xml.TwoStageLoader;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Random;

/**
 *
 * This is the abstract class for each driving policy.
 *
 * @author Group Algorithms
 * @version 1.0
 *
 */
public abstract class DrivingPolicy implements XMLSerializable,TwoStageLoader
{
	protected SimModel model;
	protected TLController tlc;
    protected Random random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);    
    //random number generator must be independent on other generators (to avoid magic influence) (DOAS 06)

	DrivingPolicy(SimModel m, TLController _tlc) 
	{
		model = m;
		tlc = _tlc;
	}
        
    /** Reset necessary stuff (DOAS 06)
    */
    public void reset()
    {
        random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
    }

	/**
	 * The lane to which a car continues its trip.
	 * @param r The road user being asked.
	 * @param lane_now The current lane
	 * @param node_now The current node
	 * @return The chosen lane.
	 */
	public Drivelane getDirection(Roaduser r, Drivelane lane_now, Node node_now) throws InfraException 
	{                
		/* Returns an array of all available outbound lanes supporting roadusers of given type 
		 * that can be reached from the given inbound lane.*/
		Drivelane[] lanesleadingfrom = node_now.getAvailableLanesLeadingFrom(lane_now, r.getType());
         /*EJUST: check getLanesLeadingFrom --> getAvailableLanesLeadingFrom*/
                
		//All the lanes which are in a shortest path to the car's destination
        Drivelane[] shortestpaths = node_now.getShortestPaths(r.getDestNode().getId(), r.getType());
                
        //The lane to which a car continues its trip.
        Drivelane result = getDirectionLane(r, lane_now, lanesleadingfrom, shortestpaths);

        //Because of the accidents the shortest path might not be among the possible directions (DOAS 06)
        //But if re-routing is off, we like the "null way"
        
        //if(lanesleadingfrom.length == 0) 
        	//System.out.println("Uh: " + lane_now.getName() + " " + node_now.getName());

        if(result == null && this.model.getSimController().getRerouting())
        {        
        	if(lanesleadingfrom.length == 0)            
        		return null;    
        		//i.e., no line leads from the EdgeNode            
        	 
        	//else {            
        	
        	//random lane; 
        	//TODO: find smarter solution                
        	//look for the line which does not lead to the EndgeNode (not even two steps ahead)
            //return lanesleadingfrom[0];
                       
        	Drivelane[] possible = new Drivelane[lanesleadingfrom.length];                
        	int cnt = 0;                
        	
        	for(int i = 0; i < lanesleadingfrom.length; i++)
        	{                        		
        		if(lanesleadingfrom[i].getNodeLeadsTo().getType() != Node.EDGE)
        		{                            			
        			Drivelane[] lanesTwoStepsAhead = lanesleadingfrom[i].getNodeLeadsTo().getAvailableLanesLeadingFrom(lanesleadingfrom[i], r.getType());                        
        			/*EJUST: check getLanesLeadingFrom --> getAvailableLanesLeadingFrom*/                        
        			
        			for(int j = 0; j < lanesTwoStepsAhead.length; j++)
        			{                        
        				if(lanesTwoStepsAhead[j].getNodeLeadsTo().getType() != Node.EDGE)
        				{                            
        					possible[cnt] = lanesleadingfrom[i];                                
        					cnt++;                                
        					break;  
        					//we do not need to look for other excuse for lanesleadingfrom[i]                             
        				}                          
        			}                       
        		}                   
        	}                
        	
        	if(cnt > 0)
        	{                
        		Drivelane selectedLane = possible[random.nextInt(cnt)];                    
        		//System.out.println("Forced way out [" + node_now.getId() + ":" + selectedLane.getId() + "]" + i);                    
        		return selectedLane;                    
        	}
        	
        	else 
        	{                
        		/*
        		 if(lanesleadingfrom[0].getNodeLeadsTo().getId() != r.getDestNode().getId()){                    
                    
                    //now it is critical: 
                    //TODO: this car must leave the city in the wrong EdgeNode
                                                                        
                    System.out.println("Found no way out to " + r.getDestNode().getId() + "[" + node_now.getId() + ":"
 									+ lane_now.getId()                                                    
                                    + "]: possibilities " + lanesleadingfrom.length                                                    
                                    + " - first one (" + lanesleadingfrom[0].getName()                                                    
                                    + ") leads to " + lanesleadingfrom[0].getNodeLeadsTo().getName());
                                                                
                 }*/                                    		
        		return lanesleadingfrom[0];                    
        	}                
         //}            
        } 
        else 
        {        
        	return result;            
        }
	}

	/**
	 * The lane to which a car continues its trip.
	 * @param r The road user being asked.
	 * @param lane_now The current lane
	 * @param allOutgoing All the possible outgoing lanes
	 * @param shortest All the lanes which are in a shortest path to the car's destination
	 * @return The chosen lane.
	 */
	public abstract Drivelane getDirectionLane(Roaduser r, Drivelane lane_now, Drivelane[] allOutgoing, Drivelane[] shortest);

	/*EJUST: moved from AggressiveDP to DrivingPolicy*/
	public int determineDirection(Drivelane fromLane, Drivelane toLane) throws InfraException  
	// 0=left, 1=straight ahead, 2=right
	{
		Node n = fromLane.getSign().getNode();
		Road [] rds = n.getAllRoads();
		int fromIndex = -1;
		int toIndex   = -1;
		
		for (int k = 0; k < rds.length; k++)
		{
			Road r = rds[k];
			if (r!=null)
			{
				Drivelane [] dls = r.getInboundLanes(n);
				
				for (int l=0; l < dls.length; l++)
				{
					if (dls[l]==fromLane) 
						fromIndex = k;
				}
				
				dls = r.getOutboundLanes(n);
				
				for (int l=0; l < dls.length; l++)
				{
					if (dls[l]==toLane) 
						toIndex = k;
				}				
			}
		}
		
		//if (fromIndex == -1) System.out.println("No fromLane"); //EJUST commented
		//if (toIndex == -1) System.out.println("No toLane"); //EJUST commented
		
		int dir = toIndex - fromIndex -1;
		
		while (dir < 0) 
			dir += 4;
		
		return dir;
	}

	
	// Generic XMLSerializable implementation
	/**Empty for Drivingpolicy*/
	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{ 	
		// Empty
	}
	
	/**Empty for Drivingpolicy*/
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ 	
		// Empty
	}

	public void setParentName (String parentName) throws XMLTreeException
	{	
		throw new XMLTreeException("Attempt to change fixed parentName of a DP class.");
	}

	// Empty TwoStageLoader (standard)
	public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
	{
		
	}
}
