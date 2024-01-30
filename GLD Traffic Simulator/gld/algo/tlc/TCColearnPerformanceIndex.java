/*
 * TCColearnPerformanceIndex.java
 *
 * Created on March 11, 2012, 10:00 PM
 *
 */

package gld.algo.tlc;

import gld.infra.Infrastructure;

/**
 *  This is a base class for co-learn performance indices RL controllers.
 *  TC1TLCOpt, TC1TLCOptBayesian, TC1TLCOptMultiObjective, TC1TLCOptMultiObjectiveBayesian, TC1TLCOptEnhancedMultiObjectiveBayesian, TC1TLCOptHybridExplorationEnhancedMultiObjectiveBayesian 
 *  @author EJUST
 */
public abstract class TCColearnPerformanceIndex extends TCRL{
    
	protected boolean learnValues = true; /*POMDPGLD*/
	
    /** Creates a new instance of TCColearnPerformanceIndex */
    public TCColearnPerformanceIndex(Infrastructure infra) {
        super(infra); 	
    }
    
    public abstract float getExpectedTripWaitingTime(int tlNewId, int posNew, int desId);
    public abstract float getExpectedTripTime(int tlNewId, int posNew, int desId);
    public abstract float getExpectedDistance(int tlNewId, int posNew, int desId);
    public abstract float getExpectedJunctionWaitingTime(int tlNewId, int posNew);
}
