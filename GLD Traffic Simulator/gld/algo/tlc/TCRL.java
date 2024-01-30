/*
 * TCRL.java
 *
 * Created on January 26, 2006, 1:37 PM
 *
 */

package gld.algo.tlc;

import gld.infra.Drivelane;
import gld.infra.Infrastructure;
import gld.infra.Roaduser;
import gld.infra.Sign;

/**
 *  This is a base class for reinforcement learners.
 * @author DOAS 06
 */
public abstract class TCRL extends TLController{
    
	protected boolean learnValues = true; /*POMDPGLD*/
	
    /** Creates a new instance of TCRL */
    public TCRL(Infrastructure infra) {
        super(infra); 	
    }
    
    public void updateRoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, double prevpos /*EJUST: int --> double*/, 
    		Drivelane dlanenow, Sign signnow, double posnow /*EJUST: int --> double*/, 
    		PosMov[] posMovs, Drivelane desired)
    {
        updateRoaduserMove(ru, prevlane, prevsign, prevpos, dlanenow, signnow, posnow, posMovs, desired, 0);
    }
    
    public abstract void updateRoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, double prevpos /*EJUST: int --> double*/, 
    		Drivelane dlanenow, Sign signnow, double posnow /*EJUST: int --> double*/, 
    		PosMov[] posMovs, Drivelane desired, int penalty);
}
