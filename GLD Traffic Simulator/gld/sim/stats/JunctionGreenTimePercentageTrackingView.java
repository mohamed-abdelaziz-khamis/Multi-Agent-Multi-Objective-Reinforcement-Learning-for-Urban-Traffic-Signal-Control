
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

import gld.infra.Junction;

/**
 *
 * TrackingView that tracks the green time percentage of one Junction.
 *
 * @author  EJUST
 * @version 1.0
 */

public class JunctionGreenTimePercentageTrackingView extends ExtendedTrackingView
{
	private Junction junction;
	int id;
	
  public JunctionGreenTimePercentageTrackingView(int startTimeStep, Junction junction)
  {
		super(startTimeStep);
		this.junction = junction;
		id = junction.getId();
  }

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int src) 
	{ 
		return this.junction.getCurrentGreenConfigurationIndex();
	}
	
	/** Returns the description for this tracking window. */
	public String getDescription() { return "junction " + id + " - junction green time percentage"; }
	
	protected String getYLabel() { return "green time percentage (%)"; }
}