/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University
 * Copyright of the TC1 algorithm (C) Marco Wiering, Utrecht University
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

package gld.algo.tlc;

import gld.Controller;
import gld.GLDSim;
import gld.algo.dp.DrivingPolicy;
import gld.algo.heuristic.HEC;
import gld.idm.Constants;
import gld.infra.Beliefstate;
import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Node;
import gld.infra.ObservedRoaduser;
import gld.infra.PODrivelanes;
import gld.infra.Roaduser;
import gld.infra.Sign;
import gld.infra.TrafficLight;
import gld.sim.SimModel;
import gld.xml.InstantiationAssistant;
import gld.xml.XMLArray;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;

import java.awt.Color;
import java.io.IOException;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

/**
 *
 * An extra state has been added to the implementation of TC1TCLOPT. In this state we
 * will be able to express a boolean for congestion of the next lane current Roaduser is heading to.
 * To get this boolean to select the state congestion of the next lane (num cars waiting on dl / numblocks of road
 * is tested against a threshold.
 * If true -> state will be [][][][1][] else [][][][0][];
 * We set the threshold to 0.8, but this can probable be an more educated number, or even an dynamic number??
 * Colearning hasn't been implementend correctly as well, in fact it hasn't changed since the TC1TLCOpt version.
 * @author Project group DOAS uva 2005
 * @version 1.0
 */
public class TCCBG extends TCColearnPerformanceIndex implements Colearning, InstantiationAssistant, HECinterface, Constants /*EJUST*/
{
    // TLC vars
    protected Infrastructure infrastructure;
    protected TrafficLight[][] tls;
    protected Node[] allnodes;
    protected Vector allsigns;
    protected int num_nodes;
    protected float threshold = 0.8f;
    protected boolean hecAddon = false;

    // TC1 vars
    protected Vector count[][][][], pTable[][][][];
    protected float[][][][][] qTable /*sign, pos, des, isCongested(no=0, yes=1), color (red=0, green=1)*/,
	 						qTableAverageTripTime /*EJUST*/;
    
    protected float[][][][] vTable,
    						vTableAverageTripTime /*EJUST*/;
    
    protected static float gamma = 0.90f; //Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
    protected final static boolean red = false, green = true;
    protected final static int green_index = 0, red_index = 1;
    protected final static String shortXMLName = "tlc-cbg";
    protected static float random_chance = 0.01f; //A random gain setting is chosen instead of the on the TLC dictates with this chance
    private Random random_number;

    // to make this one destless
    protected boolean useDestination = true; /*POMDPGLD*/
    /*EJUST: false --> true*/
    
    /**
     * The constructor for TL controllers
     * @param The model being used.
     */

    public TCCBG(Infrastructure infra) throws InfraException
    {
        super(infra);
    }

    public void setInfrastructure(Infrastructure infra)
    {
        super.setInfrastructure(infra);
        try
        {
            Node[] nodes = infra.getAllNodes();
            allnodes = nodes;
            num_nodes = nodes.length;

            int numSigns = infra.getAllInboundLanes().size();
            allsigns = new Vector(numSigns);

            for (int i = 0; i < nodes.length; i++)
            {
                Drivelane[] allInboundLanes = nodes[i].getInboundLanes();
                for (int j = 0; j < allInboundLanes.length; j++)
                {
                    allsigns.add(allInboundLanes[j].getSign().getId(),(Object) allInboundLanes[j].getSign());
                }
            }

            qTable = new float[numSigns][][][][];
            qTableAverageTripTime = new float [numSigns][][][][]; /*EJUST*/
            
            vTable = new float[numSigns][][][];
            vTableAverageTripTime = new float [numSigns][][][]; /*EJUST*/
            
            count = new Vector[numSigns][][][];
            pTable = new Vector[numSigns][][][];

            int num_specialnodes = infra.getNumSpecialNodes();

            for (int i = 0; i < num_nodes; i++)
            {
                Node n = nodes[i];
                Drivelane[] dls = n.getInboundLanes();
                for (int j = 0; j < dls.length; j++)
                {
                    Drivelane d = dls[j];
                    Sign s = d.getSign();
                    int id = s.getId();
                    int num_pos_on_dl = d.getCompleteLength();

                    qTable[id] = new float[num_pos_on_dl][][][];
                    qTableAverageTripTime[id] = new float [num_pos_on_dl][][][]; /*EJUST*/
                    
                    vTable[id] = new float[num_pos_on_dl][][];
                    vTableAverageTripTime[id] = new float [num_pos_on_dl][][]; /*EJUST*/
                    
                    count[id] = new Vector[num_pos_on_dl][][];
                    pTable[id] = new Vector[num_pos_on_dl][][];

                    for (int k = 0; k < num_pos_on_dl; k++)
                    {
                        qTable[id][k] = new float[num_specialnodes][][];
                        qTableAverageTripTime[id][k]=new float[num_specialnodes][][]; /*EJUST*/
                        
                        vTable[id][k] = new float[num_specialnodes][];
                        vTableAverageTripTime[id][k]=new float[num_specialnodes][]; /*EJUST*/
                        
                        count[id][k] = new Vector[num_specialnodes][];
                        pTable[id][k] = new Vector[num_specialnodes][];

                        for (int m = 0; m < num_specialnodes; m++)
                        {

                            int isCongested = 2;
                            qTable[id][k][m] = new float[isCongested][];
                            qTableAverageTripTime[id][k][m] = new float[isCongested][]; /*EJUST*/                         
                            
                            vTable[id][k][m] = new float[isCongested];
                            vTableAverageTripTime[id][k][m] = new float[isCongested]; /*EJUST*/
                            
                            count[id][k][m] = new Vector[isCongested];
                            pTable[id][k][m] = new Vector[isCongested];

                            for (int congest = 0; congest < isCongested;
                                               congest++)
                            {
                                qTable[id][k][m][congest] = new float[2];
                                qTable[id][k][m][congest][0] = 0.0f;
                                qTable[id][k][m][congest][1] = 0.0f;
                                
                                /*EJUST*/
                                qTableAverageTripTime[id][k][m][congest] = new float[2];
                                qTableAverageTripTime[id][k][m][congest][0] = 0.0f;
                                qTableAverageTripTime[id][k][m][congest][1] = 0.0f;
                                
                                vTable[id][k][m][congest] = 0.0f;
                                vTableAverageTripTime[id][k][m][congest] = 0.0f; /*EJUST*/
                                
                                count[id][k][m][congest] = new Vector();
                                pTable[id][k][m][congest] = new Vector();
                            }

                        }
                    }
                }
            }
        }
        catch (Exception e)
        {}
        random_number = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
    }

    /**
     * Calculates how every traffic light should be switched
     * Per node, per sign the waiting roadusers are passed and per each roaduser the gain is calculated.
     * @param The TLDecision is a tuple consisting of a traffic light and a reward (Q) value, for it to be green
     * @see gld.algo.tlc.TLDecision
     */
    public TLDecision[][] decideTLs()
    {
        /* gain = 0
         * For each TL
         *  For each Roaduser waiting
         *   gain = gain + pf*(Q([tl,pos,des,isCongested],red) - Q([tl,pos,des,isCongested],green))
         */
        int num_dec, waitingsize, pos = 0 /* = 0 POMDPGLD */, tlId, desId = 0 /* = 0 POMDPGLD */;
        float gain, passenger_factor;
        Sign tl;
        Drivelane lane;
        Roaduser ru;
        ListIterator queue;
        Node destination;
        ListIterator mlQueue = null;

        //Determine whether it should be random or not
        boolean randomrun = false;
        if (random_number.nextFloat() < random_chance)
        {
            randomrun = true;
        }

        // For all Nodes
        for (int i = 0; i < num_nodes; i++)
        {
            num_dec = tld[i].length;
            // For all Trafficlights
            for (int j = 0; j < num_dec; j++) // for all inbound lanes in node
            {
                tl = tld[i][j].getTL();
                tlId = tl.getId();
                lane = tld[i][j].getTL().getLane();
               
                if( SimModel.useAllRoadusers )  /*POMDPGLD*/
                {    
                	waitingsize = lane.getQueue().size(); /*POMDPGLD*/
                }
                else 
                {
                    waitingsize = lane.getNumRoadusersWaiting();
                }
                
                queue = lane.getQueue().listIterator();
                gain = 0;

                /*POMDPGLD*/
                if(lane instanceof PODrivelanes)
                {
                    try
                    {
                        ((PODrivelanes)lane).updateBeliefstate(SimModel.beliefstate_method);
                    }
                    catch(InfraException e)
                    {
                        e.printStackTrace();
                    }

                    if( !SimModel.useAllRoadusers ) {
                        int POwaitingsize = ((PODrivelanes)lane).getNumVRoadusersWaiting();
//                      DEBUG for the sanity check (NOCHANGE/ NOCHANGE)
//                      if(waitingsize != POwaitingsize) {
//                            SimModel.waitingsizeFailures++;
//                            System.out.println((waitingsize - POwaitingsize) + " Time Step: " + infra.getCurTimeStep() + " 1: " + waitingsize + " 2: " + POwaitingsize + " Idea: " + lane.getId());
//                            gld.infra.Test.Printqueue(lane.getQueue(), lane.getCompleteLength());
//                            gld.infra.Test.Printqueue(((PODrivelanes)lane).getBeliefstate());
//                      }
                        waitingsize = POwaitingsize;
                    }

                    if(waitingsize > 0)
                    {
                        try
                        {
                            mlQueue = ((PODrivelanes)lane).getPOQueue().listIterator();
                        }
                        catch(NullPointerException e)
                        {
                        }

                    }
                }
                /* QMDP: cashing for previously found beliefstates*/
                int[] prevQMDPpos = null;
                /* end QMDP*/
                /*POMDPGLD*/
                
                // For each waiting Roaduser
                // POMDPGLD: For each waiting Roaduser or all roadusers, if the option useAllRoadusers is set
                for (int k = 0; k < waitingsize; k++)
                {
                    /*POMDPGLD*/
                    float congestion = (float)1.0;
                    int isCongestedInt = 0;
                    passenger_factor = 1;

                    // Partially observable model
                    if(lane instanceof PODrivelanes)
                    {
                        try {
                            ObservedRoaduser vru = (ObservedRoaduser)mlQueue.next();
                            ru = vru.getRoaduser();
                            
                            /*EJUST: cast to int*/
                            pos = (int)Math.ceil(vru.getPos());

                            //HEC Addon: Congestion weight factor used in calculation of the gain.
                            if(hecAddon == true)
                            {
                                congestion = HEC.getCongestion(ru, lane, tl.getNode());
                            }

                            // CBG
                            isCongestedInt = isCongested(ru, lane);
                        }
                        catch(NullPointerException e){
                            ru = null;
                        }

                        desId = 0;

                    }
                    // Fully observable model
                    else
                    {
                        ru = (Roaduser)queue.next();
                        if(ru.getVisibility() != ru.NOT_DETECTED)
                        {                            
        					/*EJUST: Position pos will be considered by the controller while vehicle is on its way from position pos to position pos-1*/
        					pos = (int)Math.ceil(ru.getPosition());
                        	
                        	if(useDestination)
                            {
                                desId = ru.getDestNode().getId();
                            }
                            else
                            {
                                desId = 0;
                            }
                            passenger_factor = ru.getNumPassengers();

                            //HEC Addon: Congestion weight factor used in calculation of the gain.
                            if(hecAddon == true)
                            {
                                congestion = HEC.getCongestion(ru, lane, tl.getNode());
                            }

                            // CBG
                            isCongestedInt = isCongested(ru, lane);

                        }

                    }

                    if(lane instanceof PODrivelanes && SimModel.beliefstate_method == Beliefstate.QMDP) {

                        Vector bsProbs = ((PODrivelanes)lane).getBeliefstate().getBeliefstateProbabilityVector();

                        if(k == 0) {
                            prevQMDPpos = new int[bsProbs.size()];
                        }

                        for(int l = 0; l < bsProbs.size(); l++)
                        {
                            /*EJUST: cast to int*/
                            pos = (int)Math.ceil(((PODrivelanes)lane).getBeliefstate().getRoaduserPosition(prevQMDPpos[l], l, k));                            
                            
                            prevQMDPpos[l] = pos;

                            gain += passenger_factor * congestion * ((Double)bsProbs.get(l)).doubleValue() *
                                    (qTable[tlId][pos][desId][isCongestedInt][red_index] -
                                     qTable[tlId][pos][desId][isCongestedInt][green_index]); //red - green
                        }
                    }/*POMDPGLD*/
                    else {
	
	                    // Add the pf*(Q([tl,pos,des,isCongested],red)-Q([tl,pos,des,isCongested],green))
	                    gain += passenger_factor * congestion *
	                            (qTable[tlId][pos][desId][isCongestedInt][red_index] -
	                             qTable[tlId][pos][desId][isCongestedInt][green_index]); //red - green
	                    
                        if(ru.getVisibility() == ru.DOUBLE_DETECTED)
                        {
                            gain += passenger_factor * congestion *
                                (qTable[tlId][pos][desId][isCongestedInt][red_index] -
                                 qTable[tlId][pos][desId][isCongestedInt][green_index]); //red - green
                        }

                    }
                }

                // Debug info generator
                if (trackNode != -1 && i == trackNode)
                {
                    Drivelane currentlane2 = tld[i][j].getTL().getLane();
                    boolean[] targets = currentlane2.getTargets();
                    System.out.println("node: " + i + " light: " + j + " gain: " + gain +
                                       " " + targets[0] + " " + targets[1] + " " + targets[2] + " " +
                                       currentlane2.getNumRoadusersWaiting());
                }

                // If this is a random run, set all gains randomly
                if (randomrun)
                {
                    gain = random_number.nextFloat();
                }

                if (gain > 1000.0 || gain < -1000.0f)
                {
                    System.out.println("Gain might be too high? : " + gain);
                }
                
                tld[i][j].setGain(gain);
            }
        }
        return tld;
    }

    public void updateRoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, double prevpos /*EJUST: int --> double*/, 
    								Drivelane dlanenow, Sign signnow, double posnow /*EJUST: int --> double*/, 
    								PosMov[] posMovs, Drivelane desired, int penalty)
    {
        // Roaduser has just left the building!
        if (dlanenow == null || signnow == null)
        {
            return;
        }

        //Is next sign congested?
        int isCongestedInt = isCongested(ru, prevlane); // total screw
		
        if (isCongestedInt == 1)
        {
            ru.setHeadColor(new Color(255, 0, 0));
        }
        else
        {
            ru.setHeadColor(new Color(0, 255, 0));
        }

        //This ordering is important for the execution of the algorithm!

        if (prevsign.getType() == Sign.TRAFFICLIGHT &&
            (signnow.getType() == Sign.TRAFFICLIGHT ||
             signnow.getType() == Sign.NO_SIGN))
        {
            int tlId = prevsign.getId();
            
            int desId = 0 /* = 0 POMDPGLD*/;
            
            if(useDestination) /* POMDPGLD*/;
            {
                desId = ru.getDestNode().getId();
            }
            
			int prevpos_int = (int)Math.ceil(prevpos); /*EJUST: cast to int*/
			int posnow_int =  (int)Math.ceil(posnow); /*EJUST: cast to int*/
			
            recalcP(prevsign /*EJUST commented: tlId*/, prevpos, desId, prevsign.mayDrive(), signnow /*EJUST replaced: signnow.getId()*/, posnow, isCongestedInt);
            recalcQ(tlId, prevpos_int, desId, prevsign.mayDrive(), signnow.getId(), posnow_int, posMovs, isCongestedInt, prevlane, ru, penalty);
            recalcV(tlId, prevpos_int, desId, isCongestedInt);
        }
    }

    protected void recalcP(Sign prevsign /*EJUST commented: int tlId*/, 
    						double pos /*EJUST: int --> double*/, 
    						int desId, boolean light, 
    						Sign signNew /*EJUST replaced: int tlNewId*/, 
    						double posNew /*EJUST: int --> double*/, 
    						int isCongested)
    {
		/*EJUST*/
		int tlId = prevsign.getId();
		int tlNewId = signNew.getId(); 		
		int pos_int = (int)Math.ceil(pos);
		int posNew_int = (int)Math.ceil(posNew);
    	
    	// - First create a CountEntry, find if it exists, and if not add it.
        CountEntry thisSituation = new CountEntry(tlId, pos_int, desId, light, tlNewId, posNew_int, isCongested);
        
        int c_index;
        try { /*POMDPGLD*/
        	c_index = count[tlId][pos_int][desId][isCongested].indexOf(thisSituation);
        } /*POMDPGLD*/
		catch(ArrayIndexOutOfBoundsException ex) { /*POMDPGLD*/
			System.err.print("ArrayIndexOutOfBoundsException in: TC1TLCOpt, r: 504, values: tlId: "+tlId+" pos: "+pos+" desId: "+desId);
			c_index = 0;
		}/*POMDPGLD*/
		
        if (c_index >= 0){
            // Entry found
            thisSituation = (CountEntry) count[tlId][pos_int][desId][isCongested].elementAt(c_index);
            thisSituation.incrementValue();
        }
        else
        {
            // Entry not found
            count[tlId][pos_int][desId][isCongested].addElement(thisSituation);
        }

        // We now know how often this exact situation has occurred
        // - Calculate the chance
        long sameSituation = thisSituation.getValue();
        long sameStartSituation = 0;

        CountEntry curC;
        int num_c = count[tlId][pos_int][desId][isCongested].size();
		PEntry curP;
		int p_index;
		
        for (int i = 0; i < num_c; i++)
        {
            curC = (CountEntry) count[tlId][pos_int][desId][isCongested].elementAt(i);
            sameStartSituation += curC.sameStartSituation(thisSituation);
        }

        // - Update this chance
        // Calculate the new P(L|(tl,pos,des,isCongested))
        // P(L|(tl,pos,des,isCongested))	= P([tl,pos,des,isCongested],L)/P([tl,pos,des,isCongested])
        //									= #([tl,pos,des,isCongested],L)/#([tl,pos,des,isCongested])
        // Niet duidelijk of dit P([tl,p,d,isCongested],L,[*,*]) of P([tl,p,d,isCongested],L,[tl,d]) moet zijn
        // Oftewel, kans op deze transitie of kans om te wachten!

        PEntry thisChance = new PEntry(tlId, pos_int, desId, light, tlNewId, posNew_int, isCongested);
        p_index = pTable[tlId][pos_int][desId][isCongested].indexOf(thisChance);

        if (p_index >= 0)
        {
            thisChance = (PEntry) pTable[tlId][pos_int][desId][isCongested].elementAt(p_index);
        }
        else
        {
            pTable[tlId][pos_int][desId][isCongested].addElement(thisChance);
            p_index = pTable[tlId][pos_int][desId][isCongested].indexOf(thisChance);
        }

        thisChance.setSameSituation(sameSituation);
        thisChance.setSameStartSituation(sameStartSituation);

		/*EJUST*/
		double r = rewardFunction(tlId, pos, tlNewId, posNew, isCongested);
		
		/* EJUST: Add the immediate reward value to the total reward value collected so far due to the state transition from state s to state s'*/
		thisChance.addReward(r);
		
        // - Update rest of the Chance Table
        int num_p = pTable[tlId][pos_int][desId][isCongested].size();
        for (int i = 0; i < num_p; i++)
        {
            curP = (PEntry) pTable[tlId][pos_int][desId][isCongested].elementAt(i);
            if (curP.sameStartSituation(thisSituation) && i != p_index)
            {
                curP.addSameStartSituation();
            }
        }
    }

    protected void recalcQ(int tlId, int pos, 
    					   int desId, boolean light, 
    					   int tlNewId, int posNew, 
    					   PosMov[] posMovs, int isCongested, 
    					   Drivelane dlnow, Roaduser ru, int penalty)
    {
        // Q([tl,p,d,isCongested],L)	= Sum(tl', p') [P([tl,p,d,isCongested],L,[tl',p'])(R([tl,p,isCongested],[tl',p'])+ yV([tl',p',d,isCongested']))


        // First gather All tl' and p' in one array
        
    	int num_posmovs = posMovs.length;

        PosMov curPosMov;
        int curPMTlId /*Current PositionMove instance .Traffic Light Id*/, curPMPos /*Current PositionMove instance .Position*/, curNextIsCongested = isCongested;
        Sign curNextSign = dlnow.getSign();
        
        float R = 0, V = 0, Q = penalty;

		/*EJUST*/
		float V_AverageTripTime=0, Q_AverageTripTime=penalty;
		
		/*EJUST*/
		double chance = 0; 
        
        for (int t = 0; t < num_posmovs; t++)		// For All tl', pos'
        { 
            curPosMov = posMovs[t];
            curPMTlId = curPosMov.sign.getId(); /*EJUST: curPosMov.tlId --> curPosMov.sign.getId() */
                        
            curPMPos = (int) Math.ceil(curPosMov.pos); /*EJUST: cast to int*/

            if (curPMTlId != tlId)
            { 
            	// In case RU just crossed a junction, a next sign has te be determened.
                Drivelane dlnext = getNextDrivelaneByRu(ru, dlnow);
                if (dlnext != null)
                {
                    curNextSign = (Sign) dlnext.getSign();
                    curNextIsCongested = isCongested(ru, curNextSign.getLane());
                }
                else
                {
                    curNextIsCongested = 0;
                }
            }
            else
            {
                curNextSign = dlnow.getSign();
                curNextIsCongested = isCongested;
            }

            PEntry P = new PEntry(tlId, pos, desId, light, curPMTlId, curPMPos, isCongested);
            int p_index = pTable[tlId][pos][desId][isCongested].indexOf(P);

            if (p_index >= 0)
            {
                P = (PEntry) pTable[tlId][pos][desId][isCongested].elementAt(p_index);
                
                //R = rewardFunction(tlId, pos, curPMTlId, curPosMov.pos, isCongested);
                
                V = vTable[curPMTlId][curPMPos][desId][curNextIsCongested];
                
				/*EJUST*/
				V_AverageTripTime = vTableAverageTripTime[curPMTlId][curPMPos][desId][curNextIsCongested]; 

				/*EJUST*/
				chance = P.getChance();
				
				Q += chance * (P.getReward() /*EJUST replaced: R*/ + (gamma * V));
				
				/*EJUST: Reward = 1 as we always count 1 time step either when the vehicle is stopping or moving.*/
				Q_AverageTripTime += chance*(1 + (gamma * V_AverageTripTime));	
            }
            // Else P(..)=0, thus will not add anything in the summation
        }
        qTable[tlId][pos][desId][isCongested][light ? green_index : red_index] = Q;
        
		/*EJUST*/
		qTableAverageTripTime[tlId][pos][desId][isCongested][light?green_index:red_index]=Q_AverageTripTime;
    }

    protected void recalcV(int tlId, int pos, int desId, int isCongested)
    { 
    	//  V([tl,p,d,isCongested]) = Sum (L) [P(L|(tl,p,d,isCongested))Q([tl,p,d,isCongested],L)] ??
        float qRed = qTable[tlId][pos][desId][isCongested][red_index];
        
		/*EJUST*/
		float qRedAverageTripTime = qTableAverageTripTime[tlId][pos][desId][isCongested][red_index]; 
		
        float qGreen = qTable[tlId][pos][desId][isCongested][green_index];
        
		/*EJUST*/
		float qGreenAverageTripTime	= qTableAverageTripTime[tlId][pos][desId][isCongested][green_index]; 
		
        float[] pGR = calcPGR(tlId, pos, desId, isCongested);
        float pGreen = pGR[green_index];
        float pRed = pGR[red_index];

        vTable[tlId][pos][desId][isCongested] = (pGreen * qGreen) + (pRed * qRed);
        
		/*EJUST*/
		vTableAverageTripTime[tlId][pos][desId][isCongested] = (pGreen*qGreenAverageTripTime) + (pRed*qRedAverageTripTime); 
    }

    /*
     ==========================================================================
     Additional methods, used by the recalc methods
     ==========================================================================
     */


    protected float[] calcPGR(int tlId, int pos, int desId, int isCongested)
    {
        float[] counters = new float[2];
        double countR = 0, countG = 0;

        int psize = pTable[tlId][pos][desId][isCongested].size() - 1;
        
        for (; psize >= 0; psize--)
        {
            PEntry cur = (PEntry) pTable[tlId][pos][desId][isCongested].elementAt(psize);
            if (cur.light == green)
            {
                countG += cur.getSameSituation();
            }
            else
            {
                countR += cur.getSameSituation();
            }
        }
        
        counters[green_index] = (float) (countG / (countG + countR));
        counters[red_index] = (float) (countR / (countG + countR));
        
        return counters;
    }

	/**EJUST comment: 
	 * ATWT objective reward function
	 * Co-learning ATWT performance index reward function
	 * If the vehicle waits at its position (leads to high ATWT), then it is penalized by the reward value*/
    protected int rewardFunction(int tlId, double pos /*EJUST: int --> double*/, 
    							 int tlNewId, double posNew /*EJUST: int --> double*/, int isCongested)
    {
        if (isCongested == 1)
        {
            return 1;
        }

        if (tlId != tlNewId || pos-posNew > STOP_SPEED_MS*TIMESTEP_S /*EJUST replaced: pos != posNew*/)
        {
            return 0;
        }
        
        return 1; //tlId==tlNewId and pos-posNew <= STOP_SPEED_MS*TIMESTEP_S /* EJUST replaced: pos == posNew*/	
		/*EJUST: 
		 * The reward on this form represents: Minimize the Average Trip Waiting Time 
		 * */
		
		 /* In case of co-learning AJWT 
		 * No need to change the structure of the states table but sum on all possible destinations
		 * */        
    }

    public float getVValue(Sign sign, Node des, int pos)
    {
        int isCongested = 0; //hier moet nog iets zinnigs voor isCongested worden bedacht
        return vTable[sign.getId()][pos][des.getId()][isCongested];
    }

    public float getColearnValue(Sign now, Sign sign, Node des, int pos)
    {
        return getVValue(sign, des, pos);
    }

    public void setHecAddon(boolean b, Controller c)
    {
        if (b) c.setStatus("Using HEC on CBG");
        else c.setStatus("Using CBG without HEC");
        hecAddon = b;
    }

	/*EJUST: return the expected trip waiting time*/
	public float getExpectedTripWaitingTime(int tlNewId, int posNew, int desId){
		int isCongested = 0; //hier moet nog iets zinnigs voor isCongested worden bedacht
		return vTable[tlNewId][posNew][desId][isCongested];
	}

	/*EJUST: return the expected trip time*/
	public float getExpectedTripTime(int tlNewId, int posNew, int desId){
		int isCongested = 0; //hier moet nog iets zinnigs voor isCongested worden bedacht
		return vTableAverageTripTime[tlNewId][posNew][desId][isCongested];
	}

	@Override
	public float getExpectedDistance(int tlNewId, int posNew, int desId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getExpectedJunctionWaitingTime(int tlNewId, int posNew) {
		// TODO Auto-generated method stub
		return 0;
	}
	
    /*
     ==========================================================================
     Internal Classes to provide a way to put entries into the tables
     ==========================================================================
     */

    public int isCongested(Roaduser ru, Drivelane currentLane)
    {

        Drivelane destLane = getNextDrivelaneByRu(ru, currentLane);

        if (destLane == null)
        { //Edgenode, returns 0 because an edgenode is never congested;
            return 0;
        }

        float percWaiting = (float) destLane.getNumBlocksWaiting() / (float) destLane.getLength();

        return (percWaiting > this.threshold) ? 1 : 0;

    }

    public Drivelane getNextDrivelaneByRu(Roaduser ru, Drivelane currentLane)
    {
        DrivingPolicy dp = SimModel.getDrivingPolicy();
        Drivelane destLane;
        try
        {
            destLane = dp.getDirection(ru, currentLane, currentLane.getSign().getNode());
            return destLane;
        }
        catch (InfraException e)
        {
            System.out.println(e.getMessage());
        }
        return null;

    }

    public class CountEntry implements XMLSerializable
    {
        // CountEntry vars
        int tlId, pos, desId, tlNewId, posNew, isCongested;
        long value;
        
        boolean light;

        // XML vars
        String parentName = "model.tlc";

        CountEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew, int _isCongested)
        {
            tlId = _tlId; // The Sign the RU was at
            pos = _pos; // The position the RU was at
            desId = _desId; // The SpecialNode the RU is travelling to
            light = _light; // The colour of the Sign the RU is at now
            tlNewId = _tlNewId; // The Sign the RU is at now
            posNew = _posNew; // The position the RU is on now
            isCongested = _isCongested; // ID of the next sign(congestion there?)
            value = 1; // How often this situation has occurred
        }

        public CountEntry()
        { 
        	// Empty constructor for loading
        }

        public void incrementValue()
        {
            value++;
        }

        // Returns how often this situation has occurred
        public long getValue()
        {
            return value;
        }

        public boolean equals(Object other)
        {
            if (other != null && other instanceof CountEntry)
            {
                CountEntry countnew = (CountEntry) other;
                if (countnew.tlId != tlId)
                {
                    return false;
                }
                if (countnew.pos != pos)
                {
                    return false;
                }
                if (countnew.desId != desId)
                {
                    return false;
                }
                if (countnew.light != light)
                {
                    return false;
                }
                if (countnew.tlNewId != tlNewId)
                {
                    return false;
                }
                if (countnew.posNew != posNew)
                {
                    return false;
                }
                if (countnew.isCongested != isCongested)
                {
                    return false;
                }
                return true;
            }
            return false;
        }

        // Retuns the count-value if the situations match
        public long sameSituation(CountEntry other)
        {
            if (equals(other))
            {
                return value;
            }
            else
            {
                return 0;
            }
        }

        // Retuns the count-value if the startingsituations match
        public long sameStartSituation(CountEntry other)
        {
            if (other.tlId == tlId && other.pos == pos && other.desId == desId && other.light == light && other.isCongested == isCongested)
            {
                return value;
            }
            else
            {
                return 0;
            }
        }

        // XMLSerializable implementation of CountEntry
        public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
        {
            pos = myElement.getAttribute("pos").getIntValue();
            tlId = myElement.getAttribute("tl-id").getIntValue();
            desId = myElement.getAttribute("des-id").getIntValue();
            light = myElement.getAttribute("light").getBoolValue();
            tlNewId = myElement.getAttribute("new-tl-id").getIntValue();
            posNew = myElement.getAttribute("new-pos").getIntValue();
            isCongested = myElement.getAttribute("next-sign-id").getIntValue();
            value = myElement.getAttribute("value").getLongValue();
        }

        public XMLElement saveSelf() throws XMLCannotSaveException
        {
            XMLElement result = new XMLElement("count");
            result.addAttribute(new XMLAttribute("pos", pos));
            result.addAttribute(new XMLAttribute("tl-id", tlId));
            result.addAttribute(new XMLAttribute("des-id", desId));
            result.addAttribute(new XMLAttribute("light", light));
            result.addAttribute(new XMLAttribute("new-tl-id", tlNewId));
            result.addAttribute(new XMLAttribute("new-pos", posNew));
            result.addAttribute(new XMLAttribute("next-sign-id", isCongested));
            result.addAttribute(new XMLAttribute("value", value));
            return result;
        }

        public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException
        { 
        	// A count entry has no child objects
        }

        public String getXMLName()
        {
            return parentName + ".count";
        }

        public void setParentName(String parentName)
        {
            this.parentName = parentName;
        }
    }


    public class PEntry implements XMLSerializable
    {
        // PEntry vars
        int pos, posNew, tlId, tlNewId, desId, isCongested;
        double sameStartSituation, sameSituation;
        
        boolean light;

		/*EJUST*/
		double  R;
		
        // XML vars
        String parentName = "model.tlc";

        PEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew, int _isCongested)
        {
            tlId = _tlId; // The Sign the RU was at
            pos = _pos; // The position the RU was at
            desId = _desId; // The SpecialNode the RU is travelling to
            light = _light; // The colour of the Sign the RU is at now
            tlNewId = _tlNewId; // The Sign the RU is at now
            posNew = _posNew; // The position the RU is on now
            isCongested = _isCongested;
            
            sameStartSituation = 0; // How often this situation has occurred
            sameSituation = 0;
        }

        public PEntry()
        { 
        	// Empty constructor for loading
        }

        public void addSameStartSituation()
        {
            sameStartSituation++;
        }

        public void setSameStartSituation(long s)
        {
            sameStartSituation = s;
        }

        public void setSameSituation(long s)
        {
            sameSituation = s;
        }

        public double getSameStartSituation()
        {
            return sameStartSituation;
        }

        public double getSameSituation()
        {
            return sameSituation;
        }

        public double getChance()
        {
            return getSameSituation() / getSameStartSituation();
        }

		/*EJUST*/
		public void addReward(double r){
			R += r;
		}
		
		/*EJUST*/
		public double getReward(){
			return R/getSameSituation();
		}
		
        public boolean equals(Object other)
        {
            if (other != null && other instanceof PEntry)
            {
                PEntry pnew = (PEntry) other;
                if (pnew.tlId != tlId)
                {
                    return false;
                }
                if (pnew.pos != pos)
                {
                    return false;
                }
                if (pnew.desId != desId)
                {
                    return false;
                }
                if (pnew.light != light)
                {
                    return false;
                }
                if (pnew.tlNewId != tlNewId)
                {
                    return false;
                }
                if (pnew.posNew != posNew)
                {
                    return false;
                }
                if (pnew.isCongested != isCongested)
                {
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean sameSituation(CountEntry other)
        {
            return equals(other);
        }

        public boolean sameStartSituation(CountEntry other)
        {
            if (other.tlId == tlId && other.pos == pos && other.desId == desId && other.light == light && other.isCongested == isCongested)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        // XMLSerializable implementation of PEntry
        public void load(XMLElement myElement, XMLLoader loader) throws  XMLTreeException, IOException, XMLInvalidInputException
        {
            pos = myElement.getAttribute("pos").getIntValue();
            tlId = myElement.getAttribute("tl-id").getIntValue();
            desId = myElement.getAttribute("des-id").getIntValue();
            light = myElement.getAttribute("light").getBoolValue();
            tlNewId = myElement.getAttribute("new-tl-id").getIntValue();
            posNew = myElement.getAttribute("new-pos").getIntValue();
            isCongested = myElement.getAttribute("next-sign-id").getIntValue();
            sameStartSituation = myElement.getAttribute("same-startsituation").getLongValue();
            sameSituation = myElement.getAttribute("same-situation").getLongValue();
        }

        public XMLElement saveSelf() throws XMLCannotSaveException
        {
            XMLElement result = new XMLElement("pval");
            result.addAttribute(new XMLAttribute("pos", pos));
            result.addAttribute(new XMLAttribute("tl-id", tlId));
            result.addAttribute(new XMLAttribute("des-id", desId));
            result.addAttribute(new XMLAttribute("light", light));
            result.addAttribute(new XMLAttribute("new-tl-id", tlNewId));
            result.addAttribute(new XMLAttribute("new-pos", posNew));
            result.addAttribute(new XMLAttribute("next-sign-id", isCongested));
            result.addAttribute(new XMLAttribute("same-startsituation", sameStartSituation));
            result.addAttribute(new XMLAttribute("same-situation", sameSituation));
            return result;
        }

        public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException
        { 
        	// A PEntry has no child objects
        }

        public void setParentName(String parentName)
        {
            this.parentName = parentName;
        }

        public String getXMLName()
        {
            return parentName + ".pval";
        }
    }


    public void showSettings(Controller c)
    {
        String[] descs = {"Gamma (discount factor)", "Random decision chance", "Congestion Threshold"};
        float[] floats = { gamma, random_chance, threshold};
        TLCSettings settings = new TLCSettings(descs, null, floats);

        settings = doSettingsDialog(c, settings);
        gamma = settings.floats[0];
        random_chance = settings.floats[1];
        threshold = settings.floats[2];
    }

    // XMLSerializable, SecondStageLoader and InstantiationAssistant implementation

    public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
    {
        super.load(myElement, loader);
        gamma = myElement.getAttribute("gamma").getFloatValue();
        random_chance = myElement.getAttribute("random-chance").getFloatValue();
        
        qTable = (float[][][][][]) XMLArray.loadArray(this, loader);
        qTableAverageTripTime=(float[][][][][])XMLArray.loadArray(this,loader); /*EJUST*/
        
        vTable = (float[][][][]) XMLArray.loadArray(this, loader);
        vTableAverageTripTime=(float[][][][])XMLArray.loadArray(this,loader); /*EJUST*/
        
        count = (Vector[][][][]) XMLArray.loadArray(this, loader, this);
        pTable = (Vector[][][][]) XMLArray.loadArray(this, loader, this);
    }

    public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException
    {
        super.saveChilds(saver);
        XMLArray.saveArray(qTable, this, saver, "q-table");
        XMLArray.saveArray(qTableAverageTripTime,this,saver,"q-table-average-trip-time"); /*EJUST*/
        
        XMLArray.saveArray(vTable, this, saver, "v-table");
        XMLArray.saveArray(vTableAverageTripTime,this,saver,"v-table-average-trip-time"); /*EJUST*/
        
        XMLArray.saveArray(count, this, saver, "counts");
        XMLArray.saveArray(pTable, this, saver, "p-table");
    }

    public XMLElement saveSelf() throws XMLCannotSaveException
    {
        XMLElement result = super.saveSelf();
        result.setName(shortXMLName);
        result.addAttribute(new XMLAttribute("random-chance", random_chance));
        result.addAttribute(new XMLAttribute("gamma", gamma));
        return result;
    }

    public String getXMLName()
    {
        return "model." + shortXMLName;
    }

    public boolean canCreateInstance(Class request)
    {
        System.out.println("Called TCCBG instantiation assistant ??");
        return CountEntry.class.equals(request) || PEntry.class.equals(request);
    }

    public Object createInstance(Class request) throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        System.out.println("Called TCCBG instantiation assistant");
        if (CountEntry.class.equals(request))
        {
            return new CountEntry();
        }
        else if (PEntry.class.equals(request))
        {
            return new PEntry();
        }
        else
        {
            throw new ClassNotFoundException ("TCCBG IntstantiationAssistant cannot make instances of " + request);
        }
    }
}