package gld.sim.stats;

import gld.sim.SimModel;

import java.awt.Color;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
/*POMDPGLD*/
public class MaxLanesLoadTrackingView extends ExtendedTrackingView
{
	private SimModel model;

	/** Creates a new instance of AccidentsCountTrackingView */
	public MaxLanesLoadTrackingView(int startTimeStep, SimModel model) {
		super(startTimeStep);
		this.model = model;
	}

	/** Returns the next sample to be 'tracked'. */
	protected float nextSample(int index)
	{

		return(float)model.maxLaneLoad * 100;

	}

	protected String getYLabel() { return "maxLaneLoad"; }

	public String getDescription() { return "the max load of all lanes in %"; }

	public boolean useModes() { return false; }

	/** Determines the number of tracking graphs. */
	protected int getMaxTrack() { return 1; }
	/** Determines the colors of the tracking graphs. */
	protected Color[] getColors() { Color[] c = {Color.black}; return c; }

	protected String getSourceDesc(int i) {
		return "the max load of all lanes in %";

	}
}
