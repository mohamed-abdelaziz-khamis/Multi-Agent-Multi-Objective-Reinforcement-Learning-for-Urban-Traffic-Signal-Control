
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

import gld.infra.Drivelane.DrivelaneSpeedStatistics;
import gld.sim.SimModel;

/**
 *
 * TrackingView that tracks the total number of Roadusers that has not arrived yet.
 *
 * @author  EJUST
 * @version 1.0
 */

public class TotalRoadusersNotArrivedYetTrackingView extends ExtendedTrackingView
{
	DrivelaneSpeedStatistics[][] drivelaneSpeedStats;
	
	public TotalRoadusersNotArrivedYetTrackingView(int startTimeStep, SimModel model)
	{
		super(startTimeStep);
		drivelaneSpeedStats = model.getInfrastructure().getDrivelaneSpeedStatistics();
	}

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int index) 
	{ 
		int sample = 0;
		for(int i=0; i<drivelaneSpeedStats.length; i++)
			sample += drivelaneSpeedStats[i][index].getTotalRoadusersNotArrivedYet();
		return (float)sample;
	}

	protected String getYLabel() { return "roadusers not arrived yet"; }

	public String getDescription() { return "total roadusers not arrived yet"; }
	
	public boolean useModes() { return false; }
}