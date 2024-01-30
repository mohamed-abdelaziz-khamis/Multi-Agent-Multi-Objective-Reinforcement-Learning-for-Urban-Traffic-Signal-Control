
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

import gld.sim.SimModel;

/**
 *
 * TrackingView that tracks the average number of roadusers waiting for the Sign of all Drivelanes.
 *
 * @author  EJUST
 * @version 1.0
 */

public class AverageLanesNumRoadusersWaitingTrackingView extends TrackingView
{

	private SimModel model;
	
	public AverageLanesNumRoadusersWaitingTrackingView(int startTimeStep, SimModel model)
	{
		super(startTimeStep);
		this.model = model;
	}

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int index)
	{
		return(float)model.averageLaneNumRoadusersWaiting;
	}

	protected String getYLabel() { return "average lane num roadusers waiting (roadusers)"; }

	protected String getSourceDesc(int src) { return "All"; }
	public String getDescription() { return "average num of roadusers waiting of all lanes"; }
}
