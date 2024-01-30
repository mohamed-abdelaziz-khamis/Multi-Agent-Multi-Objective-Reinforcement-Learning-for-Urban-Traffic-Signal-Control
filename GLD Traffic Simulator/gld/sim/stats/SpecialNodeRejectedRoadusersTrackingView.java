
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

import gld.infra.SpecialNode;
import gld.infra.Node.NodeStatistics;

/**
 *
 * TrackingView that tracks the rejected roadusers as opposed to the total roadusers generated of one Special node
 *
 * @author  EJUST
 * @version 1.0
 */

public class SpecialNodeRejectedRoadusersTrackingView extends TrackingView
{
	SpecialNode node;
	
	  public SpecialNodeRejectedRoadusersTrackingView(int startTimeStep,SpecialNode node)
	  {	
		  super(startTimeStep);
		  this.node=node;
	  }

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int src) 
	{ 	
	  float nodeGeneratedRoadusers = node.getGeneratedRoadusersCount();
	  float nodeEnteredRoadusers = node.getEnteredCarsCount();
	  return nodeGeneratedRoadusers==0?0:(nodeGeneratedRoadusers-nodeEnteredRoadusers)*100/nodeGeneratedRoadusers;
	}
	
	protected String getYLabel() { return "percentage of roadusers rejected/generated (%)"; }

	protected String getSourceDesc(int src) { return "All" /*EJUST: percentage --> All*/;}
	
	/** Returns the description for this tracking window. */
	public String getDescription() { return "Special node " + node.getId() + " - percentage of roadusers rejected/generated (%)"; }
	}
