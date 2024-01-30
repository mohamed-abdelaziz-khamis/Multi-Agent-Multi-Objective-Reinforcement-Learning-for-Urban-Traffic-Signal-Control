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
public class StateZeroTrackingView extends ExtendedTrackingView
{
    private SimModel model;

    /** Creates a new instance of AccidentsCountTrackingView */
    public StateZeroTrackingView(int startTimeStep, SimModel model) {
        super(startTimeStep);
        this.model = model;
    }

        /** Returns the next sample to be 'tracked'. */
        protected float nextSample(int index)
        {
            return(float)((float)model.state_zeros/ (float)model.Max_state_zeros) *100;
        }

        protected String getYLabel() { return "stateZeros"; }

        public String getDescription() { return "zero state probability"; }

        public boolean useModes() { return false; }

        /** Determines the number of tracking graphs. */
        protected int getMaxTrack() { return 2; }
        /** Determines the colors of the tracking graphs. */
        protected Color[] getColors() { Color[] c = {Color.black, Color.red}; return c; }


}
