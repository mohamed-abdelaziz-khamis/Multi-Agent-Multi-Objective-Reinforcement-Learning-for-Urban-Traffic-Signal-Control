
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
 * TrackingView that tracks the percentage of road users arrived as opposed to the total number of road users entered.
 *
 * @author  EJUST
 * @version 1.0
 */

public class ArrivedRoadusersPercentageTrackingView extends TrackingView
{
	private SimModel model;
	private TotalRoadusersTrackingView totalRoadusersTrackingView;

	public ArrivedRoadusersPercentageTrackingView(int startTimeStep, SimModel model)
	{
		super(startTimeStep);
		this.model = model;
		totalRoadusersTrackingView = new TotalRoadusersTrackingView(startTimeStep, model);	
	}

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int index)
	{
	    float totalNumberOfArrivedRoadusers = totalRoadusersTrackingView.nextSample(0); //index = 0 means all roadusers types
	    float totalNumberOfEnteredRoadusers = model.getInfrastructure().getEnteredCarsCount();
        return  totalNumberOfEnteredRoadusers==0?0:totalNumberOfArrivedRoadusers*100/totalNumberOfEnteredRoadusers;
	}

	protected String getYLabel() { return "percentage of roadusers arrived/entered (%)"; }

	protected String getSourceDesc(int src) { return "All"  /*EJUST: percentage-->All*/; }
	public String getDescription() { return "percentage of roadusers arrived as opposed to total roadusers entered"; }
}
