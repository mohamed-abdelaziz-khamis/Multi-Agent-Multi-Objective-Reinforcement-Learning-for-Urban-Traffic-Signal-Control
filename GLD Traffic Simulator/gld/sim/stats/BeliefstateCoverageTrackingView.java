package gld.sim.stats;

import gld.sim.SimModel;
import gld.infra.Node;
import gld.infra.Node.NodeStatistics;
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
public class BeliefstateCoverageTrackingView extends ExtendedTrackingView
{
    NodeStatistics[][] stats;
    private SimModel model;

    /** Creates a new instance of AccidentsCountTrackingView */
    public BeliefstateCoverageTrackingView(int startTimeStep, SimModel model) {
        super(startTimeStep);
        this.model = model;
        stats = model.getInfrastructure().getEdgeNodeStatistics();
    }

        /** Returns the next sample to be 'tracked'. */
        protected float nextSample(int index)
        {

             return(float)model.currentBeliefStateProb* 100;

        }

        protected String getYLabel() { return "beliefStateCoverage"; }

        public String getDescription() { return "the average amount of probability density mass coverd by the Beliefstates"; }

        public boolean useModes() { return false; }

        /** Determines the number of tracking graphs. */
        protected int getMaxTrack() { return 1; }
        /** Determines the colors of the tracking graphs. */
        protected Color[] getColors() { Color[] c = {Color.black}; return c; }

        protected String getSourceDesc(int i) {
            return "average traffic load per lane in %";
        }
}