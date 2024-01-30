
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

import gld.infra.*;
import gld.infra.Node.NodeStatistics;
import gld.sim.SimModel;

/**
 *
 * TrackingView that tracks the fraction of cars removed as oposed to the number
 * of cars entered.
 *
 * @author  DOAS 06
 * @version 1.0
 */

public class RemovedCarsTrackingView extends ExtendedTrackingView
{
	private SimModel model;

	public RemovedCarsTrackingView(int startTimeStep, SimModel model)
	{
		super(startTimeStep);
		this.model = model;
        this.maximum = 1.0f;
	}

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int index)
	{
            return (float) model.getInfrastructure().getRemovedCarsCount();
	}

	protected String getYLabel() { return "removed cars"; }

	public String getDescription() { return "Total number of removed cars"; }
}
