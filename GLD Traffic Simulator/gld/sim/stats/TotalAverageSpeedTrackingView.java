
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
import gld.infra.Node;
import gld.infra.Drivelane.DrivelaneSpeedStatistics;
import gld.infra.Node.NodeStatistics;
import gld.sim.SimModel;

/**
 *
 * TrackingView that tracks the total average speed of all roadusers that either have/haven't arrived yet.
 *
 * @author  EJUST
 * @version 1.0
 */

public class TotalAverageSpeedTrackingView extends ExtendedTrackingView
{
	NodeStatistics[][] stats;
	DrivelaneSpeedStatistics[][] drivelaneSpeedStats;	
	
	public TotalAverageSpeedTrackingView(int startTimeStep, SimModel model)
	{
		super(startTimeStep);
		stats = model.getInfrastructure().getEdgeNodeStatistics();
		drivelaneSpeedStats = model.getInfrastructure().getDrivelaneSpeedStatistics();
	}

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int index) 
	{ 
		float tripTimeSample = 0;
		float tripDistanceSample = 0;
		int ru;
		
		for(int i = 0; i < stats.length; i++) {
			if(allTime)
				ru = stats[i][index].getTotalRoadusers();
			else
				ru = Math.min(Node.STAT_NUM_DATA, stats[i][index].getTotalRoadusers());
			
			tripTimeSample += stats[i][index].getAvgTripTime(allTime) * ru;
			tripDistanceSample += stats[i][index].getAvgDistance(allTime) * ru;
		}
		
		float timeSample = 0;
		float distanceSample = 0;
		
		for(int i = 0; i < drivelaneSpeedStats.length; i++) {
			if(allTime)
				ru = drivelaneSpeedStats[i][index].getTotalRoadusersNotArrivedYet();
			else
				ru = Math.min(Drivelane.STAT_NUM_DATA, drivelaneSpeedStats[i][index].getTotalRoadusersNotArrivedYet());
			
			timeSample += drivelaneSpeedStats[i][index].getAvgTime(allTime) * ru;
			distanceSample += drivelaneSpeedStats[i][index].getAvgDistance(allTime) * ru;
		}
			
		float totalTimeSample = tripTimeSample + timeSample;
		float totalDistanceSample = tripDistanceSample + distanceSample;
		
		return totalTimeSample == 0 ? 0 : totalDistanceSample / totalTimeSample;
	}

	/** Returns the description for this tracking window. */
	public String getDescription() { return "average speed"; }
	
	protected String getYLabel() { return "average speed (meter/timestep)"; }
}