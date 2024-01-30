
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
import gld.infra.Drivelane.DrivelaneColearnStatistics;
import gld.infra.Drivelane.DrivelaneSpeedStatistics;
import gld.infra.Node.NodeStatistics;
import gld.sim.SimModel;

/**
 *
 * TrackingView that tracks the colearn average trip time.
 *
 * @author  EJUST
 * @version 1.0
 */

public class TotalColearnTripTimeTrackingView extends ExtendedTrackingView
{
	NodeStatistics[][] stats;
	DrivelaneSpeedStatistics[][] drivelaneSpeedStats;
	DrivelaneColearnStatistics[][] drivelaneColearnStats;
	
	public TotalColearnTripTimeTrackingView(int startTimeStep, SimModel model)
	{
		super(startTimeStep);
		stats = model.getInfrastructure().getEdgeNodeStatistics();
		drivelaneSpeedStats = model.getInfrastructure().getDrivelaneSpeedStatistics();
		drivelaneColearnStats = model.getInfrastructure().getDrivelaneColearnStatistics();
	}

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int index) 
	{ 
		float tripTimeSample = 0;
		int roadusersArrivedCount = 0;
		int ru;

		for(int i=0; i<stats.length; i++) {
			if(allTime)
				ru = stats[i][index].getTotalRoadusers();
			else
				ru = Math.min(Node.STAT_NUM_DATA, stats[i][index].getTotalRoadusers());
			
			tripTimeSample += stats[i][index].getAvgTripTime(allTime) * ru;
			roadusersArrivedCount += ru;
		}

		float colearnTripTimeSample = 0;
		int roadusersNotArrivedYetCount = 0;

		for(int i=0; i<drivelaneColearnStats.length; i++) {
			if(allTime)
				ru = drivelaneColearnStats[i][index].getTotalRoadusersNotArrivedYet();
			else
				ru = Math.min(Drivelane.STAT_NUM_DATA, drivelaneColearnStats[i][index].getTotalRoadusersNotArrivedYet());
			
			colearnTripTimeSample += (drivelaneSpeedStats[i][index].getAvgTime(allTime)+
									  drivelaneColearnStats[i][index].getAvgExpectedTripTime(allTime)) * ru;
			roadusersNotArrivedYetCount += ru;
		}

		float totalTripTimeSample = tripTimeSample + colearnTripTimeSample;
		float roadusersCount = roadusersArrivedCount + roadusersNotArrivedYetCount;

		return roadusersCount == 0 ? 0 : totalTripTimeSample / roadusersCount;
	}

	/** Returns the description for this tracking window. */
	public String getDescription() { return "colearn average trip time"; }
	
	protected String getYLabel() { return "colearn trip time (time steps)"; }
}