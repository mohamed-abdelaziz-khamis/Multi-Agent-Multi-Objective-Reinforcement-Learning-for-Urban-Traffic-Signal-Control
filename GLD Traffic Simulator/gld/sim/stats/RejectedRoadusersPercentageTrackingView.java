
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
 * TrackingView that tracks the percentage of road users rejected as opposed to the total number of road users generated.
 *
 * @author  EJUST
 * @version 1.0
 */

public class RejectedRoadusersPercentageTrackingView extends TrackingView
{
	private SimModel model;

	public RejectedRoadusersPercentageTrackingView(int startTimeStep, SimModel model)
	{
		super(startTimeStep);
		this.model = model;
	}

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int index)
	{
       float totalNumberOfGeneratedRoadusers = model.getInfrastructure().getGeneratedRoadusersCount();
       float totalNumberOfEnteredRoadusers = model.getInfrastructure().getEnteredCarsCount();
       return totalNumberOfGeneratedRoadusers==0?0:(totalNumberOfGeneratedRoadusers-totalNumberOfEnteredRoadusers)*100/totalNumberOfGeneratedRoadusers;
	}

	protected String getYLabel() { return "percentage of roadusers rejected/generated (%)"; }

	protected String getSourceDesc(int src) { return "All"  /*EJUST: percentage-->All*/; }
	public String getDescription() { return "percentage of roadusers rejected as opposed to total roadusers generated"; }
}
