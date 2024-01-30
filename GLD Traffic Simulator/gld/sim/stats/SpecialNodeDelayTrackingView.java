
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
 * TrackingView that tracks the average trip delay of one Special node
 *
 * @author  Group GUI
 * @version 1.0
 */

public class SpecialNodeDelayTrackingView extends ExtendedTrackingView
{
	NodeStatistics[] stats;
	int id;
	
  public SpecialNodeDelayTrackingView(int startTimeStep,SpecialNode node)
  {	
	  super(startTimeStep);
	  stats = node.getStatistics();
	  id = node.getId();
  }

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int src) 
	{ 	
		return stats[src].getAvgDelay(allTime);
	}
	
	/** Returns the description for this tracking window. */
	public String getDescription() { return "Special node " + id + " - average trip delay"; }
	
	protected String getYLabel() { return "delay (time steps)"; }
}
