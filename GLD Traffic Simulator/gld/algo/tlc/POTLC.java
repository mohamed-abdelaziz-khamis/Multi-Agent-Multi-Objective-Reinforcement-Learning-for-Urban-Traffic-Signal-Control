package gld.algo.tlc;

import gld.infra.Drivelane;
import gld.infra.Sign;
import gld.infra.Roaduser;
import gld.infra.ObservedQueue;

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
public interface POTLC
{
    void updatePORoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, double [] prevPosProbs
                                    , Drivelane dlanenow, Sign signnow, ObservedQueue vcq
                                    , Drivelane desired);
}
