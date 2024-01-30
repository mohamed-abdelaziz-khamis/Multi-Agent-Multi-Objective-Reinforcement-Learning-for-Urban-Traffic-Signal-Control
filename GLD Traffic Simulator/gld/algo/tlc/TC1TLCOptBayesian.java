
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
import gld.algo.heuristic.HEC;
import gld.idm.Constants;
import gld.infra.Beliefstate;
import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Junction;
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

/**
 *
 * This controller will decide it's Q values for the traffic lights according to the traffic situation on
 * the lane connected to the TrafficLight. It will learn how to alter it's outcome by reinforcement learning.
 * Now Optimized 2.0
 * Now Long-Fixed, so run run run, Forrest!
 *
 * @author Arne K, Jilles V
 * @version 2.0
 */
public class TC1TLCOptBayesian extends TCColearnPerformanceIndex implements Colearning, InstantiationAssistant, HECinterface, Constants /*EJUST*/
{
	// TLC vars
	protected Infrastructure infrastructure;
	protected TrafficLight[][] tls;
	protected Node[] allnodes;
	protected int num_nodes;
	protected boolean hecAddon;

	// TC1 vars
	protected Vector count[][][], pTable[][][];
	protected float [][][][] qTable /*sign, pos, des, color (red=0, green=1)*/,
							 qTableAverageTripTime /*EJUST*/,
	 						 qTableAverageSpeed /*EJUST*/;
	
	protected float [][][]	 qTableAverageJunctionWaitingTime /*EJUST*/;
	
	protected float [][][]   vTable,
	   						 vTableAverageTripTime /*EJUST*/,
		   					 vTableAverageSpeed /*EJUST*/;
	
	protected float [][]	 vTableAverageJunctionWaitingTime /*EJUST*/;

	/*POMDPGLD*/
	protected Vector Tcount[][][], TpTable[][][];
	protected float[][][][] TqTable; //sign, pos, des, color (red=0, green=1)
	protected float[][][] TvTable;

	protected static float gamma=0.90f;				//Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
	protected final static boolean red=false, green=true;
	protected final static int green_index=0, red_index=1;
	protected final static String shortXMLName="tlc-tc1o1-bayesian";
	protected static float random_chance=0.01f;				//A random gain setting is chosen instead of the on the TLC dictates with this chance
	private Random random_number;

	// to make this one destless
	protected boolean useDestination = true; /*POMDPGLD*/
	/*EJUST: false --> true*/

	/** EJUST: used in comparing the previous gain of each node with the newly calculated one
	 * If the difference exceeds some threshold (which means a transient period), 
	 * then use the hybrid exploration method (for better learning)
	 * 
	 * In order to detect the transient state, let THRESHOLD=0.1 (instead of 0.2), i.e., 10% from the "current" gain value, 
	 * In this case, we check if in each of the 10 consecutive time steps an increase of 10% from the "current" gain value has happened 
	 * (i.e., about 100% increase from the original gain value that was before the 10 consecutive time steps) 
	 */
	protected static double THRESHOLD = 0.1;
	
	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */

	public TC1TLCOptBayesian ( Infrastructure infra ) throws InfraException
	{	
		super(infra);
	}

	/*POMDPGLD*/
	public Vector[][][] getCount()
	{
		return count;
	}

	public Vector[][][] getpTable()
	{
		return pTable;
	}

	public float[][][][] getqTable()
	{
		return qTable;
	}

	public float[][][] getvTable()
	{
		return vTable;
	}

	public void setCount(Vector[][][] e)
	{
		learnValues = false;
		Tcount = e;
	}

	public void setpTable(Vector[][][] e)
	{
		learnValues = false;
		TpTable = e;
	}

	public void setqTable(float[][][][] e)
	{
		learnValues = false;
		TqTable = e;
	}

	public void setvTable(float[][][] e)
	{
		learnValues = false;
		TvTable = e;
	}
	/*POMDPGLD*/

	public void setInfrastructure(Infrastructure infra) {

		super.setInfrastructure(infra);

		SimModel.state_zeros = 0; /*POMDPGLD*/
		if (learnValues == true || !SimModel.usePO /*EJUST*/) {/*POMDPGLD*/
			try{
				Node[] nodes = infra.getAllNodes();
				num_nodes = nodes.length;

				int numSigns = infra.getAllInboundLanes().size();			

				qTable = new float [numSigns][][][];
				qTableAverageTripTime = new float [numSigns][][][]; /*EJUST*/
				qTableAverageSpeed = new float [numSigns][][][]; /*EJUST*/
				qTableAverageJunctionWaitingTime = new float [numSigns][][]; /*EJUST*/
				
				vTable = new float [numSigns][][];
				vTableAverageTripTime = new float [numSigns][][]; /*EJUST*/
				vTableAverageSpeed = new float [numSigns][][]; /*EJUST*/
				vTableAverageJunctionWaitingTime = new float [numSigns][]; /*EJUST*/
				
				count	= new Vector[numSigns][][];
				pTable = new Vector[numSigns][][];

				int num_specialnodes = infra.getNumSpecialNodes();
				for (int i=0; i<num_nodes; i++)	
				{
					Node n = nodes[i];
					Drivelane [] dls = n.getInboundLanes();
					for (int j=0; j<dls.length; j++) 
					{
						Drivelane d = dls[j];
						Sign s = d.getSign();
						int id = s.getId();
						int num_pos_on_dl = d.getCompleteLength();

						qTable[id] = new float [num_pos_on_dl][][];
						qTableAverageTripTime[id] = new float [num_pos_on_dl][][]; /*EJUST*/
						qTableAverageSpeed[id] = new float [num_pos_on_dl][][]; /*EJUST*/
						qTableAverageJunctionWaitingTime[id] = new float [num_pos_on_dl][]; /*EJUST: descretize the number of positions on drivelane by ceiling*/

						vTable[id] = new float [num_pos_on_dl][];
						vTableAverageTripTime[id] = new float [num_pos_on_dl][]; /*EJUST*/
						vTableAverageSpeed[id] = new float [num_pos_on_dl][]; /*EJUST*/
						vTableAverageJunctionWaitingTime[id] = new float [num_pos_on_dl]; /*EJUST*/
						
						count[id] = new Vector[num_pos_on_dl][];
						pTable[id] = new Vector[num_pos_on_dl][];

						for (int k=0; k<num_pos_on_dl; k++)	
						{
							qTable[id][k]=new float[num_specialnodes][];
							qTableAverageTripTime[id][k]=new float[num_specialnodes][]; /*EJUST*/
							qTableAverageSpeed[id][k]=new float[num_specialnodes][]; /*EJUST*/
							
							qTableAverageJunctionWaitingTime[id][k]	= new float [2]; /*EJUST*/
							qTableAverageJunctionWaitingTime[id][k][0]= 0.0f; /*EJUST*/
							qTableAverageJunctionWaitingTime[id][k][1]= 0.0f; /*EJUST*/
							
							vTable[id][k]=new float[num_specialnodes];
							vTableAverageTripTime[id][k]=new float[num_specialnodes]; /*EJUST*/
							vTableAverageSpeed[id][k]=new float[num_specialnodes]; /*EJUST*/
							
							vTableAverageJunctionWaitingTime[id][k]	= 0.0f; /*EJUST*/
							
							count[id][k] = new Vector[num_specialnodes];
							pTable[id][k] = new Vector[num_specialnodes];

							/*POMDPGLD*/
							// Don't need the extra num_specialnodes, cause no destination is used.
							SimModel.state_zeros += 2;

							for (int l=0; l<num_specialnodes;l++)	
							{
								qTable[id][k][l]	= new float [2];
								qTable[id][k][l][0]= 0.0f;
								qTable[id][k][l][1]= 0.0f;
								
								/*EJUST*/
								qTableAverageTripTime[id][k][l]	= new float [2];
								qTableAverageTripTime[id][k][l][0]= 0.0f;
								qTableAverageTripTime[id][k][l][1]= 0.0f;
								
								/*EJUST*/
								qTableAverageSpeed[id][k][l]	= new float [2];
								qTableAverageSpeed[id][k][l][0]= 0.0f;
								qTableAverageSpeed[id][k][l][1]= 0.0f;
								
								vTable[id][k][l]	= 0.0f;
								
								vTableAverageTripTime[id][k][l]	= 0.0f; /*EJUST*/
								vTableAverageSpeed[id][k][l]	= 0.0f; /*EJUST*/
								
								count[id][k][l] 	= new Vector();
								pTable[id][k][l]	= new Vector();
							}
						}
					}
				}
			}
			catch(Exception e) {}
		}
		else {  /*POMDPGLD*/
			count = Tcount;
			qTable = TqTable;
			vTable = TvTable;
			pTable = TpTable;
		}
		random_number = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
		SimModel.Max_state_zeros = SimModel.state_zeros;
	}

	/**
	 * Calculates how every traffic light should be switched
	 * Per node, per sign the waiting roadusers are passed and per each roaduser the gain is calculated.
	 * @param The TLDecision is a tuple consisting of a traffic light and a reward (Q) value, for it to be green
	 * @see gld.algo.tlc.TLDecision
	 */
	public TLDecision[][] decideTLs() {
		/* gain = 0
		 * For each TL
		 *  For each Roaduser waiting
		 *   gain = gain + pf*(Q([tl,pos,des],red) - Q([tl,pos,des],green))
		 */
		int num_dec, waitingsize, pos, tlId, desId;
		float gain, passenger_factor;
		
		Sign tl;
		Drivelane lane;
		Roaduser ru;
		ListIterator queue;
		ListIterator mlQueue = null; /*POMDPGLD*/ 

		//Determine whether it should be random or not
		boolean randomrun = false;
		//EJUST
		setRandomRun(false);
		
		/* EJUST:
		 * Dr. Walid proposed new decision making strategy
		 * The proposed method is made for better adaptation in (exploration and decision making).
		 * Probabilistic decision making according to gains
		 * */
		
		if (random_number.nextFloat() < random_chance) 
			setRandomRun(true);	/* EJUST commented: randomrun = true;*/

		// For all Nodes
		for (int i=0;i<num_nodes;i++) {
			
			num_dec = tld[i].length;
			
			/*EJUST: borrowed from SBC; SOTL Phase Traffic Controller*/
			Junction currentNode = null;			
			for (int j = 0; j < num_dec; j++)
				if (currentNode == null) 
					currentNode= (Junction) tld[i][j].getTL().getNode();			
			
			if (currentNode != null && currentNode.getKeepTLDFlag()) {
				currentNode.decrPhaseMinimal();
				//System.out.println("Decr PHASEMIN for node "+ tld[i][0].getTL().getNode().getId());		
			}
			
		  /*EJUST: borrowed from SBC; SOTL Phase Traffic Controller*/			
		  if (currentNode != null && currentNode.getPhaseMinimal() <= 0) {
			 
			/*EJUST: used for checking whether the currentNode reaches its steady state or not*/
			float gainTotal = 0;  
			// For all Trafficlights
			for(int j=0; j < num_dec; j++) { // for all inbound lanes in node
				
				tl = tld[i][j].getTL();
				tlId = tl.getId();
				lane = tld[i][j].getTL().getLane();

				if(SimModel.useAllRoadusers) /*POMDPGLD*/
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

					if(!SimModel.useAllRoadusers)
					{
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
					else {
						int POwaitingsize = ((PODrivelanes)lane).getNumVRoadusers();
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
				for(int k=0; k < waitingsize; k++) {

					/*POMDPGLD*/
					passenger_factor = 1;
					
					// placeholder... will be initialized properly further down
					pos = 0;
					
					//HEC Addon: Congestion weight factor used in calculation of the gain.
					float congestion = (float)1.0;

					// Partially observable model
					if(lane instanceof PODrivelanes)
					{
						try
						{
							ObservedRoaduser vru = (ObservedRoaduser)mlQueue.next();
							ru = vru.getRoaduser();
							
							/*EJUST: cast to int*/
							pos = (int)Math.ceil(vru.getPos());

							//HEC Addon: Congestion weight factor used in calculation of the gain.
							if(hecAddon == true)
							{
								congestion = HEC.getCongestion(ru, lane, tl.getNode());
							}
						}
						catch(NullPointerException e)
						{
							ru = null;
						}
						desId = 0;
					}

					// Fully observable model
					else
					{
						ru = (Roaduser)queue.next();
						
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
					}

					if(lane instanceof PODrivelanes && SimModel.beliefstate_method == Beliefstate.QMDP)
					{

						Vector bsProbs = ((PODrivelanes)lane).getBeliefstate().getBeliefstateProbabilityVector();

						if(k == 0)
						{
							prevQMDPpos = new int[bsProbs.size()];
						}

						for(int l = 0; l < bsProbs.size(); l++)
						{
							int ruID = ((Integer)((PODrivelanes)lane).getBeliefstate().getVisibleRoaduserIDs().get(k)).intValue();
							
							/*EJUST: cast to int*/
							pos = (int) Math.ceil(((PODrivelanes)lane).getBeliefstate().getRoaduserPosition(prevQMDPpos[l], l, ruID));
							
							prevQMDPpos[l] = pos;
							
							gain += passenger_factor * congestion * ((Double)bsProbs.get(l)).doubleValue() *
											(qTable[tlId][pos][desId][red_index] -	
											 qTable[tlId][pos][desId][green_index]); //red - green
						}
					}

					else if (SimModel.use_SCOMDP) {
						double frontweight = 0, centerweight = 0, backweight = 0;

						centerweight = count[tlId][pos][desId].size() * 0.8;
						double support = centerweight;
						boolean atFront = true, atBack = true;
						
						if(pos > 0) {
							frontweight = count[tlId][pos-1][desId].size() * 0.2;
							support += frontweight;
							atFront = false;
						}

						if(pos < lane.getCompleteLength() -1) {
							backweight  = count[tlId][pos+1][desId].size() * 0.2;
							support += backweight;
							atBack = false;
						}
						
						if(support > 0) {
							if(atFront)
							{
								backweight = backweight / support;
							}
							else if(atBack)
							{
								frontweight = frontweight / support;
							}
							else
							{
								backweight = backweight / support;
								frontweight = frontweight / support;
							}
							centerweight = centerweight / support;

							if(SimModel.SCOMDP_version == 1) {
								// this version uses the weighted support for the three points forward backward and the center
								// of the roaduser position.
								
								// DEBUG:
								/*if(tlId == 135)
	                                {
	                                    System.out.println("Front, Center, Back: " + frontweight + ", " +  centerweight + ", " + backweight);
	                                }*/

								if(frontweight > 0)
								{
									gain += passenger_factor * congestion *	frontweight *
												(qTable[tlId][pos - 1][desId][red_index] -	
												 qTable[tlId][pos - 1][desId][green_index]); //red - green
								}
								
								if(backweight > 0)
								{
									gain += passenger_factor * congestion *	backweight *
												(qTable[tlId][pos + 1][desId][red_index] -	
												 qTable[tlId][pos + 1][desId][green_index]); //red - green
								}

								gain += passenger_factor * congestion *  centerweight *
												(qTable[tlId][pos][desId][red_index] -	
												 qTable[tlId][pos][desId][green_index]); //red - green
							}
							else {
								// only if the support for the back and forward spots are higher take them instead of the center
								if((frontweight + backweight) > centerweight) {
									double fbsupport = frontweight + backweight;
									frontweight /= fbsupport;
									backweight /= fbsupport;
									/*// DEBUG:
	                                    if(tlId == 135)
	                                    {
	                                       System.out.println("Front, Center, Back: " + frontweight + ", " +  centerweight + ", " + backweight);
	                                    }*/

									if(frontweight > 0)
									{
										gain += passenger_factor * congestion * frontweight *
														(qTable[tlId][pos - 1][desId][red_index] - 
														 qTable[tlId][pos - 1][desId][green_index]); //red - green
									}
									
									if(backweight > 0)
									{
										gain += passenger_factor * congestion * backweight *
														(qTable[tlId][pos + 1][desId][red_index] - 
														 qTable[tlId][pos + 1][desId][green_index]); //red - green
									}
								}
								else {
									/*// DEBUG:
	                                    if(tlId == 135)
	                                    {
	                                        System.out.println("Front, Center, Back: 0, 1, 0");
	                                    }*/

									gain += passenger_factor * congestion *
												(qTable[tlId][pos][desId][red_index] - 
												 qTable[tlId][pos][desId][green_index]); //red - green
								}

							}//end version 1 or 2
						}//end SCOMDPs...
					}/*POMDPGLD*/
					else
					{
						// the normal TC1 gain computation
						// Add the pf*(Q([tl,pos,des],red)-Q([tl,pos,des],green))
						gain += passenger_factor * congestion *	
										(qTable[tlId][pos][desId][red_index]-
										 qTable[tlId][pos][desId][green_index]); //red - green
						
						//System.out.print("[" + pos + "] "); //EJUST
					}

				}

				// Debug info generator
				if(trackNode!=-1 && i==trackNode) {
					Drivelane currentlane2 = tld[i][j].getTL().getLane();
					boolean[] targets = currentlane2.getTargets();
					System.out.println("node: "+i+" light: "+j+" gain: "+gain+" "+
							" lane: "+ tld[i][j].getTL().getId() + " targets: "+ /*EJUST*/
							targets[0]+" "+targets[1]+" "+targets[2]+" "+
							" Num Roadusers Waiting: "+ /*EJUST*/
							currentlane2.getNumRoadusersWaiting());
				}

				/* EJUST comment: The check on random run is placed improperly here:
				 * 1- Every iteration, we might set the gain "randomly" after the whole ordinary calculation for each gain (uselessly), 
				 * 2- Every iteration, we make a check on a variable that was set by true before the whole big loop (in case of random runs)
				 * 3- Better to move this check in the SignController class, such that in case of using the epsilon exploration 
				 * 	  and the run is scheduled to be random, it will not differ to choose the max gain from a set of random gains 
				 * 	  or choose one random gain from the set of random gains
				 * 4- I do not prefer at all to change in the values of calculated gains, the simple inconsistency that might happen, 
				 * in the next decision point we might compare an old "random" gain with a currently calculated gain.
				 
				// If this is a random run, set all gains randomly
				 if(randomrun){
					gain = random_number.nextFloat();
					//System.out.println("This is a random run, set all gains randomly:	" + gain); //EJUST
				}*/

				/*EJUST commented:
				if(gain > 1000.0 || gain < -1000.0f)
					System.out.println("Gain might be too high? : "+gain);
				*/
				
				gainTotal += gain;
				
				tld[i][j].setGain(gain);
			}
			
			/* EJUST: borrowed from SBC SOTL Phase Traffic Controller
			 * Keep Traffic Light Decision Flag = False
			 * In order to change/fix the current traffic light configuration according to the newly calculated gains*/
			currentNode.setKeepTLDFlag(false);
			
			/*EJUST: This part used to check whether the currentNode reaches its steady state,
		 	(in steady state the gain value almost becomes fixed)
			*/
			float absoluteDiff = Math.abs(currentNode.getCurrentGain() - gainTotal); 
			
			if (absoluteDiff >= THRESHOLD*Math.abs(currentNode.getCurrentGain()))
				currentNode.incrementTransientPeriod();
			else
				currentNode.setTransientPeriod(0);
			
			currentNode.setCurrentGain(gainTotal);
		  }	
		}
		return tld;
	}

	public void updateRoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, double prevpos /*EJUST: int --> double*/, 
									Drivelane dlanenow, Sign signnow, double posnow /*EJUST: int --> double*/, 
									PosMov[] posMovs, Drivelane desired, int penalty)
	{
		// Roaduser has just left the building!
		if(dlanenow == null || signnow == null) {
			return;
		}

		//This ordering is important for the execution of the algorithm!
		if(prevsign.getType()==Sign.TRAFFICLIGHT && (signnow.getType()==Sign.TRAFFICLIGHT || signnow.getType()==Sign.NO_SIGN)) {
			
			int tlId = prevsign.getId();

			int desId = 0;
			
			if(useDestination) /*POMDPGLD*/
			{
				desId = ru.getDestNode().getId();
			}

			int prevpos_int = (int)Math.ceil(prevpos); /*EJUST: cast to int*/
			int posnow_int =  (int)Math.ceil(posnow); /*EJUST: cast to int*/

			recalcP(prevsign /*EJUST commented: tlId*/, prevpos, desId, prevsign.mayDrive(), signnow /*EJUST replaced: signnow.getId()*/, posnow);			
			recalcQ(tlId, prevpos_int, desId, prevsign.mayDrive(), signnow.getId(), posnow_int, posMovs, penalty);			
			recalcV(tlId, prevpos_int, desId);
			
            /*if(tlId == 15) {
            	printValues(tlId);
            	printPValues(tlId);
        	}*/
		}
	}

	protected void recalcP(Sign prevsign /*EJUST commented: int tlId*/, 
							double pos /*EJUST: int --> double*/, 
							int desId, boolean light, 
							Sign signNew /*EJUST replaced: int tlNewId*/, 
							double posNew /*EJUST: int --> double*/)
	{
		/*EJUST*/
		int tlId = prevsign.getId();
		int tlNewId = signNew.getId(); 		
		int pos_int = (int)Math.ceil(pos);
		int posNew_int = (int)Math.ceil(posNew);
		
		// - First create a CountEntry, find if it exists, and if not add it.
		CountEntry thisSituation = new CountEntry(tlId, pos_int, desId, light, tlNewId, posNew_int);

		int	c_index;
		try { /*POMDPGLD*/			
			c_index = count[tlId][pos_int][desId].indexOf(thisSituation);
		} /*POMDPGLD*/
		catch(ArrayIndexOutOfBoundsException ex) { /*POMDPGLD*/
			System.err.print("ArrayIndexOutOfBoundsException in: TC1TLCOptBayesian, r: 504, values: tlId: "+tlId+" pos: "+pos+" desId: "+desId);
			c_index = 0;
		}/*POMDPGLD*/

		if(c_index >= 0) {
			// Entry found
			thisSituation = (CountEntry) count[tlId][pos_int][desId].elementAt(c_index);
			thisSituation.incrementValue();
			
			/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
			thisSituation.incrementWeight(); 
			
			/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
			thisSituation.incrementNewStateWeight();
		}
		else {
			// Entry not found
			count[tlId][pos_int][desId].addElement(thisSituation);
		}

		// We now know how often this exact situation has occurred
		// - Calculate the chance
		long sameSituation = thisSituation.getValue();
		long sameStartSituation = 0;
		
		long sameStateWeight = thisSituation.getWeight(); /*EJUST*/
		long newStateWeight = thisSituation.getNewStateWeight(); /*EJUST*/

		CountEntry curC;
		int num_c = count[tlId][pos_int][desId].size();
		PEntry curP;
		int p_index;
		
		for(int i=0;i<num_c;i++) {
			
			curC = (CountEntry) count[tlId][pos_int][desId].elementAt(i);
			sameStartSituation	+= curC.sameStartSituation(thisSituation);
			
			/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
			if (curC.light==light && (curC.tlNewId!=tlNewId || curC.posNew !=posNew_int)){
				
				curC.incrementNewStateWeight(); 
				
				curP = new PEntry(tlId, pos_int, desId, curC.light, curC.tlNewId, curC.posNew);
				
				p_index = pTable[tlId][pos_int][desId].indexOf(curP);
				if(p_index >= 0)
					curP = (PEntry) pTable[tlId][pos_int][desId].elementAt(p_index);
				else 
					pTable[tlId][pos_int][desId].addElement(curP);
				
				curP.setNewStateWeight(curC.getNewStateWeight()); 	
			}

			/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
			if (curC.light!=light || curC.tlNewId!=tlNewId || curC.posNew !=posNew_int){
				
				curC.incrementWeight(); 
				
				curP = new PEntry(tlId, pos_int, desId, curC.light, curC.tlNewId, curC.posNew);
				
				p_index = pTable[tlId][pos_int][desId].indexOf(curP);
				if(p_index >= 0)
					curP = (PEntry) pTable[tlId][pos_int][desId].elementAt(p_index);
				else 
					pTable[tlId][pos_int][desId].addElement(curP);
				
				curP.setSameStateWeight(curC.getWeight()); 	
			}
		}

		// - Update this chance
		// Calculate the new P(L|(tl,pos,des))
		// P(L|(tl,pos,des))	= P([tl,pos,des],L)/P([tl,pos,des])
		//						= #([tl,pos,des],L)/#([tl,pos,des])

		// Niet duidelijk of dit P([tl,p,d],L,[*,*]) of P([tl,p,d],L,[tl',p']) moet zijn
		// Oftewel, kans op deze transitie of kans om te wachten!

		// Not clear if this P ([tl, p, d], L ,[*,*]) or P ([tl, p, d], L, [tl', p']) must be
		// In other words, this transition probability or chance to wait!

		PEntry thisChance = new PEntry(tlId, pos_int, desId, light, tlNewId, posNew_int);
		p_index = pTable[tlId][pos_int][desId].indexOf(thisChance);

		if(p_index >= 0)
			thisChance = (PEntry) pTable[tlId][pos_int][desId].elementAt(p_index);
		else {
			pTable[tlId][pos_int][desId].addElement(thisChance);
			p_index = pTable[tlId][pos_int][desId].indexOf(thisChance);
		}

		thisChance.setSameSituation(sameSituation);
		thisChance.setSameStartSituation(sameStartSituation);
		
		/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
		thisChance.setSameStateWeight(sameStateWeight); 	
		
		/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
		thisChance.setNewStateWeight(newStateWeight); 				

		/*EJUST*/
		double r, r_AverageSpeed;
		r = rewardFunction(tlId, pos, tlNewId, posNew);

		if (tlNewId!=tlId)
		{			
			/*EJUST: Speed reward: positive distance travelled (delta p) per time step.*/				
			r_AverageSpeed = signNew.getLane().getCompleteLength() - posNew + pos;			
		}
		else{ 		
			r_AverageSpeed = pos - posNew;					
		}
		
		/* EJUST: Add the immediate reward value to the total reward value collected so far due to the state transition from state s to state s'*/
		thisChance.addReward(r);
		thisChance.addAverageSpeedReward(r_AverageSpeed);
		
		// - Update rest of the Chance Table
		int num_p = pTable[tlId][pos_int][desId].size();
		for(int i=0;i<num_p;i++) {
			curP = (PEntry) pTable[tlId][pos_int][desId].elementAt(i);
			if(curP.sameStartSituation(thisSituation) && i!=p_index)
				curP.addSameStartSituation();			
		}
	}

	protected void recalcQ(int tlId, int pos, 
						   int desId, boolean light, 
						   int tlNewId, int posNew, 
						   PosMov[] posMovs, int penalty)
	{	
		// Meneer Kaktus zegt: OK!
		// Q([tl,p,d],L)	= Sum(tl', p') [P([tl,p,d],L,[tl',p'])(R([tl,p],[tl',p'])+ yV([tl',p',d]))

		/* EJUST: P([tl,p,d],L,[tl',p']) = P([tl',p']|[tl,p,d],L)
		 * P([tl',p']|[tl,p,d],L) = #([tl,p,d],L,[tl',p'])/#([tl,p,d],L)
		 * */ 

		// First gather All tl' and p' in one array

		/*EJUST commented*/
		//int num_posmovs	= posMovs.length;

		//PosMov curPosMov;
		//int curPMTlId /*Current PositionMove instance .Traffic Light Id*/, curPMPos /*Current PositionMove instance .Position*/;
		
		float R=0, V=0, Q=penalty;

		/*EJUST*/
		float V_AverageTripTime=0, V_AverageSpeed=0, V_AverageJunctionWaitingTime=0,
			  Q_AverageTripTime=penalty, Q_AverageSpeed=penalty, Q_AverageJunctionWaitingTime=penalty;
		
		/*EJUST*/
		double chance = 0, chance_AJWT = 0, reward_AJWT = 0;
		int num_p = pTable[tlId][pos][desId].size();
		
		for(int t=0; t< num_p /*EJUST replaced: num_posmovs*/; t++) {		// For All tl', pos'
			
			/*EJUST commented:*/
			//curPosMov = posMovs[t];
			//curPMTlId = curPosMov.sign.getId() /*EJUST: curPosMov.tlId --> curPosMov.sign.getId() */;
			
			//curPMPos  = (int) Math.ceil (curPosMov.pos);	/*EJUST: cast to int*/

			PEntry P; /*EJUST commented: new PEntry(tlId, pos, desId, light, curPMTlId, curPMPos);*/
			//int p_index = pTable[tlId][pos][desId].indexOf(P); /*EJUST commented:*/

			//if(p_index>=0) { /*EJUST commented*/				
			P = (PEntry) pTable[tlId][pos][desId].elementAt(t /*EJUST replaced: p_index*/);
						
			if (P.tlId==tlId && P.pos==pos && P.desId==desId && P.light==light){ /*EJUST*/
				
				//R = rewardFunction(tlId, pos, P.tlNewId /*EJUST replaced: curPMTlId*/, P.posNew /*EJUST replaced: curPosMov.pos*/);
				
				V = vTable[P.tlNewId][P.posNew][desId];
				
				/*EJUST*/
				V_AverageTripTime = vTableAverageTripTime[P.tlNewId][P.posNew][desId];
				V_AverageSpeed = vTableAverageSpeed[P.tlNewId][P.posNew][desId];
				V_AverageJunctionWaitingTime = vTableAverageJunctionWaitingTime[P.tlNewId][P.posNew];
				
				/*EJUST*/
				chance = P.getChance();
				
				Q += chance *(P.getReward() /*EJUST replaced: R*/ + (gamma * V));
				
				/*EJUST: Reward = 1 as we always count 1 time step either when the vehicle is stopping or moving.*/
				Q_AverageTripTime += chance * V_AverageTripTime; //chance*(1 + (gamma * V_AverageTripTime));				
				Q_AverageSpeed += chance * (P.getAverageSpeedReward() + (gamma * V_AverageSpeed));
				
				/*EJUST*/
				chance_AJWT = reward_AJWT = 0;
				for (int i = 0; i < pTable[tlId][pos].length; i++){
					PEntry P_AJWT = new PEntry(tlId, pos, i, light, P.tlNewId, P.posNew);
					int p_index_AJWT = pTable[tlId][pos][i].indexOf(P_AJWT);			
					if(p_index_AJWT >= 0) {							
						P_AJWT = (PEntry) pTable[tlId][pos][i].elementAt(p_index_AJWT);
						chance_AJWT+=P_AJWT.getChance();
						reward_AJWT+=P_AJWT.getReward();
					}
				}				
				Q_AverageJunctionWaitingTime += chance_AJWT * (reward_AJWT + (gamma * V_AverageJunctionWaitingTime));
			}
			// Else P(..)=0, thus will not add anything in the summation
		}
		
		/*EJUST*/
		Q_AverageTripTime *= gamma;
		Q_AverageTripTime ++;
		
		/*POMDPGLD*/
		if( qTable[tlId][pos][desId][light ? green_index : red_index] != 0 && Q == 0 ) { SimModel.state_zeros++ ;  }
		if( qTable[tlId][pos][desId][light ? green_index : red_index] == 0 && Q != 0 ) { SimModel.state_zeros-- ;  }
		
		qTable[tlId][pos][desId][light?green_index:red_index]=Q;
		
		/*EJUST*/
		qTableAverageTripTime[tlId][pos][desId][light?green_index:red_index]=Q_AverageTripTime;
		qTableAverageSpeed[tlId][pos][desId][light?green_index:red_index]=Q_AverageSpeed;
		qTableAverageJunctionWaitingTime[tlId][pos][light?green_index:red_index]=Q_AverageJunctionWaitingTime; 
	}


	protected void recalcV(int tlId, int pos, int desId)
	{	
		//  V([tl,p,d]) = Sum (L) [P(L|(tl,p,d))Q([tl,p,d],L)]

		float qRed		= qTable[tlId][pos][desId][red_index];
		
		/*EJUST*/
		float qRedAverageTripTime = qTableAverageTripTime[tlId][pos][desId][red_index];
		float qRedAverageSpeed = qTableAverageSpeed[tlId][pos][desId][red_index];
		float qRedAverageJunctionWaitingTime = qTableAverageJunctionWaitingTime[tlId][pos][red_index]; 
		
		float qGreen	= qTable[tlId][pos][desId][green_index];
		
		/*EJUST*/
		float qGreenAverageTripTime	= qTableAverageTripTime[tlId][pos][desId][green_index];
		float qGreenAverageSpeed	= qTableAverageSpeed[tlId][pos][desId][green_index];
		float qGreenAverageJunctionWaitingTime	= qTableAverageJunctionWaitingTime[tlId][pos][green_index]; 
		
		float[] pGR 	= calcPGR(tlId,pos,desId);
		float pGreen	= pGR[green_index];
		float pRed		= pGR[red_index];

		vTable[tlId][pos][desId] = (pGreen*qGreen) + (pRed*qRed);
		
		/*EJUST*/
		vTableAverageTripTime[tlId][pos][desId] = (pGreen*qGreenAverageTripTime) + (pRed*qRedAverageTripTime);		
		vTableAverageSpeed[tlId][pos][desId] = (pGreen*qGreenAverageSpeed) + (pRed*qRedAverageSpeed);
		
		/*EJUST*/
		float[] pGR_AJWT;
		float pGreen_AJWT=0;
		float pRed_AJWT=0;
		for (int i=0; i<pTable[tlId][pos].length; i++){
			pGR_AJWT 	= calcPGR(tlId,pos,i);
			pGreen_AJWT	+= pGR_AJWT[green_index];
			pRed_AJWT	+= pGR_AJWT[red_index];
		}
		vTableAverageJunctionWaitingTime[tlId][pos] = (pGreen_AJWT*qGreenAverageJunctionWaitingTime) + (pRed_AJWT*qRedAverageJunctionWaitingTime); 

	}

	/*
				==========================================================================
							Additional methods, used by the recalc methods
				==========================================================================
	 */


	protected float[] calcPGR(int tlId, int pos, int desId) {
		float[] counters = new float[2];
		double countR=0, countG=0;
		double weightR=0, weightG=0; /*EJUST*/

		int psize = pTable[tlId][pos][desId].size()-1;

		for(; psize>=0; psize--) {
			PEntry cur = (PEntry) pTable[tlId][pos][desId].elementAt(psize);
			if(cur.light==green){
				countG += cur.getSameSituation();
				weightG += cur.getSameStateWeight(); /*EJUST*/				
			}
			else{
				countR += cur.getSameSituation();
				weightR += cur.getSameStateWeight(); /*EJUST*/
			}
		}

		//counters[green_index] = (float) (countG/(countG+countR)); /*EJUST commented*/
		//counters[red_index] = (float)(countR/(countG+countR)); /*EJUST commented*/
		
		/*EJUST: Calculating the posterior in case of fixing the state with different actions*/
		double t = countG+countR; 
		counters[green_index] = (float)((2*weightG)/(t*(t+1)));
		counters[red_index] = (float)((2*weightR)/(t*(t+1))); 
		
		return counters;
	}

	/**EJUST comment: 
	 * ATWT objective reward function
	 * Co-learning ATWT/AJWT performance index reward
	 * If the vehicle waits at its position (leads to high ATWT/AJWT), then it is penalized by the reward value*/
	protected int rewardFunction(int tlId, double pos, int tlNewId, double posNew) 
	{		
		if (tlId!=tlNewId || pos-posNew > STOP_SPEED_MS*TIMESTEP_S /*EJUST replaced: pos != posNew*/)
			return 0;		
		
		return 1; //tlId==tlNewId and pos-posNew <= STOP_SPEED_MS*TIMESTEP_S /* EJUST replaced: pos == posNew*/ 
		
		/*EJUST: 
		 * The reward on this form represents: Minimize the Average Trip Waiting Time 
		 * */
	}
	
	public float getVValue(Sign sign, Node des, int pos) {
		return vTable[sign.getId()][pos][des.getId()];
	}

	/*POMDPGLD*/
	public void printValues(int tlID) {
		System.out.print("\n**********************");
		System.out.print("\nV\t");
		for(int i = 0; i < vTable[tlID].length; i++)
		{
			System.out.print(vTable[tlID][i][0] + " ");
		}

		System.out.print("\nQ:green\t");
		for(int i = 0; i < qTable[tlID].length; i++)
		{
			System.out.print(qTable[tlID][i][0][green_index] + " ");
		}

		System.out.print("\nQ:red\t");
		for(int i = 0; i < qTable[tlID].length; i++)
		{
			System.out.print(qTable[tlID][i][0][red_index] + " ");
		}

	}

	public void printPValues(int tlID) {
		System.out.print("\n**********************");
		System.out.print("\nP\t");
		for(int i = 0; i < pTable[tlID].length; i++)
		{
			System.out.print("\n\t["+i+"]:\t");
			Vector pVector = pTable[tlID][i][0];

			for(int j = 0; j < pVector.size(); j++)
			{
				PEntry pe = (PEntry)pVector.get(j);
				System.out.print("\t\t"+ ((pe.light == green)? "G: ":"R: ") + pe.pos +" => "+pe.tlNewId+":"+pe.posNew+" : "+pe.getChance());
			}
		}
	}
	/*POMDPGLD*/

	public float getColearnValue(Sign now, Sign sign, Node des, int pos) {
		return getVValue(sign,des,pos);
	}

	public void setHecAddon(boolean b, Controller c)
	{
		if(b) c.setStatus("Using HEC on TC1TLCOptBayesian");
		else c.setStatus("Using TC1TLCOptBayesian without HEC");
		hecAddon = b;
	}
	
	/*EJUST: return the expected trip waiting time*/
	public float getExpectedTripWaitingTime(int tlNewId, int posNew, int desId){
		return vTable[tlNewId][posNew][desId];
	}

	/*EJUST: return the expected trip time*/
	public float getExpectedTripTime(int tlNewId, int posNew, int desId){
		return vTableAverageTripTime[tlNewId][posNew][desId];
	}
	
	/*EJUST: return the expected distance*/
	public float getExpectedDistance(int tlNewId, int posNew, int desId){
		return vTableAverageSpeed[tlNewId][posNew][desId];
	}
	
	/*EJUST: return the expected junction waiting time*/
	public float getExpectedJunctionWaitingTime(int tlNewId, int posNew){
		return vTableAverageJunctionWaitingTime[tlNewId][posNew];
	}

	/*
				==========================================================================
					Internal Classes to provide a way to put entries into the tables
				==========================================================================
	 */

	public class CountEntry implements XMLSerializable, Serializable
	{
		// CountEntry vars
		int tlId, pos, desId, tlNewId, posNew;
		long value;
		
		/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
		long weight;
		
		/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
		long newStateWeight;
		
		boolean light;
		
		// XML vars
		String parentName="model.tlc";

		CountEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew) 
		{
			tlId = _tlId;					// The Sign the RU was at
			pos = _pos;						// The position the RU was at
			desId = _desId;					// The SpecialNode the RU is traveling to
			light = _light;					// The colour of the Sign the RU is at now
			tlNewId = _tlNewId;				// The Sign the RU is at now
			posNew = _posNew;				// The position the RU is on now
			value = 1;						// How often this situation has occurred
			
			weight = 1;						// EJUST: The weight of this situation occurenace
			newStateWeight = 1;			// EJUST: The weight of the new situation occurenace
		}

		public CountEntry ()
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

		/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
		public void incrementWeight() 
		{
			weight+=value;			
		}

		/*EJUST*/
		public long getWeight() 
		{
			return weight;
		}

		/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
		public void incrementNewStateWeight() 
		{
			newStateWeight+=value;			
		}

		/*EJUST*/
		public long getNewStateWeight() 
		{
			return newStateWeight;
		}
		
		public boolean equals(Object other) {
			if(other != null && other instanceof CountEntry)
			{	
				CountEntry countnew = (CountEntry) other;
				if(countnew.tlId!=tlId) return false;
				if(countnew.pos!=pos) return false;
				if(countnew.desId!=desId) return false;
				if(countnew.light!=light) return false;
				if(countnew.tlNewId!=tlNewId) return false;
				if(countnew.posNew!=posNew) return false;
				return true;
			}
			return false;
		}

		// Returns the count-value if the situations match
		public long sameSituation(CountEntry other) {
			if(equals(other))
				return value;
			else
				return 0;
		}

		// Retuns the count-value if the startingsituations match
		public long sameStartSituation(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light)
				return value;
			else
				return 0;
		}

		// XMLSerializable implementation of CountEntry
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	
			pos=myElement.getAttribute("pos").getIntValue();
			tlId=myElement.getAttribute("tl-id").getIntValue();
			desId=myElement.getAttribute("des-id").getIntValue();
			light=myElement.getAttribute("light").getBoolValue();
			tlNewId=myElement.getAttribute("new-tl-id").getIntValue();
			posNew=myElement.getAttribute("new-pos").getIntValue();
			value=myElement.getAttribute("value").getLongValue();
			
			/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
			weight=myElement.getAttribute("weight").getLongValue();
			
			/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
			newStateWeight=myElement.getAttribute("new-state-weight").getLongValue();
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	
			XMLElement result=new XMLElement("count");
			result.addAttribute(new XMLAttribute("pos",pos));
			result.addAttribute(new XMLAttribute("tl-id",tlId));
			result.addAttribute(new	XMLAttribute("des-id",desId));
			result.addAttribute(new XMLAttribute("light",light));
			result.addAttribute(new XMLAttribute("new-tl-id",tlNewId));
			result.addAttribute(new XMLAttribute("new-pos",posNew));
			result.addAttribute(new XMLAttribute("value",value));
			
			/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
			result.addAttribute(new XMLAttribute("weight",weight));
			
			/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
			result.addAttribute(new XMLAttribute("new-state-weight",newStateWeight));
			
			return result;
		}

		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	
			// A count entry has no child objects
		}

		public String getXMLName ()
		{ 	
			return parentName+".count";
		}

		public void setParentName (String parentName)
		{	
			this.parentName=parentName;
		}
	}

	public class PEntry implements XMLSerializable, Serializable
	{
		// PEntry vars
		int pos, posNew, tlId, tlNewId, desId;
		double sameStartSituation, sameSituation;
		
		/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
		double sameStateWeight;
		
		/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
		double newStateWeight;
		
		boolean light;

		/*EJUST*/
		double  R, R_AverageSpeed;
		
		// XML vars
		String parentName="model.tlc";

		PEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew) 
		{
			tlId = _tlId;					// The Sign the RU was at
			pos = _pos;						// The position the RU was at
			desId = _desId;					// The SpecialNode the RU is travelling to
			light = _light;					// The colour of the Sign the RU is at now
			tlNewId= _tlNewId;				// The Sign the RU is at now
			posNew = _posNew;				// The position the RU is on now
			
			sameStartSituation=0;			// How often this situation has occurred
			sameSituation=0;
			
			sameStateWeight=0;				// EJUST: The weight of this situation occurenace
			newStateWeight=0;				// EJUST: The weight of the new situation occurenace
		}

		public PEntry ()
		{	
			// Empty constructor for loading
		}

		public void addSameStartSituation() {	sameStartSituation++;	}
		public void setSameStartSituation(long s) {	sameStartSituation = s;	}

		public void setSameSituation(long s) {	sameSituation = s;	}

		public double getSameStartSituation() {	return sameStartSituation;	}
		public double getSameSituation() {	return sameSituation;	}


		/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
		public void setSameStateWeight(long weight) {	sameStateWeight = weight;	} 			
		public double getSameStateWeight() {return sameStateWeight;	}

		/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
		public void setNewStateWeight(long _newStateWeight) {	newStateWeight = _newStateWeight;	}				
		public double getNewStateWeight() {return newStateWeight;	} /*EJUST*/
		
		public double getChance() {									
			double t = getSameStartSituation(); /*EJUST*/			
			 //return getSameSituation()/getSameStartSituation();	/*EJUST commented*/
			return (2*getNewStateWeight())/(t*(t+1));/*EJUST*/						
		}

		/*EJUST*/
		public void addReward(double r){
			R += r;
		}
		
		/*EJUST*/
		public void addAverageSpeedReward(double r_averageSpeed){
			R_AverageSpeed += r_averageSpeed;
		}
		
		/*EJUST*/
		public double getReward(){
			return R/getSameSituation();
		}
		
		/*EJUST*/
		public double getAverageSpeedReward(){
			return R_AverageSpeed/getSameSituation();
		}
		
		public boolean equals(Object other) {
			if(other != null && other instanceof PEntry) {
				PEntry pnew = (PEntry) other;
				if(pnew.tlId!=tlId) return false;
				if(pnew.pos!=pos) return false;
				if(pnew.desId!=desId) return false;
				if(pnew.light!=light) return false;
				if(pnew.tlNewId!=tlNewId) return false;
				if(pnew.posNew!=posNew) return false;
				return true;
			}
			return false;
		}

		public boolean sameSituation(CountEntry other) {
			return equals(other);
		}

		public boolean sameStartSituation(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light)
				return true;
			else
				return false;
		}

		// XMLSerializable implementation of PEntry
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	
			pos=myElement.getAttribute("pos").getIntValue();
			tlId=myElement.getAttribute("tl-id").getIntValue();
			desId=myElement.getAttribute("des-id").getIntValue();
			light=myElement.getAttribute("light").getBoolValue();
			tlNewId=myElement.getAttribute("new-tl-id").getIntValue();
			posNew=myElement.getAttribute("new-pos").getIntValue();
			sameStartSituation=myElement.getAttribute("same-startsituation").getLongValue();
			sameSituation=myElement.getAttribute("same-situation").getLongValue();
			
			/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
			sameStateWeight=myElement.getAttribute("same-state-weight").getLongValue();
			
			/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
			newStateWeight=myElement.getAttribute("new-state-weight").getLongValue();			
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	
			XMLElement result=new XMLElement("pval");
			result.addAttribute(new XMLAttribute("pos",pos));
			result.addAttribute(new XMLAttribute("tl-id",tlId));
			result.addAttribute(new	XMLAttribute("des-id",desId));
			result.addAttribute(new XMLAttribute("light",light));
			result.addAttribute(new XMLAttribute("new-tl-id",tlNewId));
			result.addAttribute(new XMLAttribute("new-pos",posNew));
			result.addAttribute(new XMLAttribute("same-startsituation",sameStartSituation));
			result.addAttribute(new XMLAttribute("same-situation",sameSituation));
			
			/*EJUST: Used in calculating the posterior in case of fixing the state with different actions*/
			result.addAttribute(new XMLAttribute("same-state-weight", sameStateWeight));
			
			/*EJUST: Used in calculating the posterior in case of fixing the state-action with different new states*/
			result.addAttribute(new XMLAttribute("new-state-weight", newStateWeight));	
			
			return result;
		}

		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	
			// A PEntry has no child objects
		}

		public void setParentName (String parentName)
		{	
			this.parentName=parentName;
		}

		public String getXMLName ()
		{ 	
			return parentName+".pval";
		}
	}

	public void showSettings(Controller c)
	{
		String[] descs = {"Gamma (discount factor)", "Random decision chance"};
		float[] floats = {gamma, random_chance};
		TLCSettings settings = new TLCSettings(descs, null, floats);

		settings = doSettingsDialog(c, settings);
		gamma = settings.floats[0];
		random_chance = settings.floats[1];
	}

	// XMLSerializable, SecondStageLoader and InstantiationAssistant implementation

	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{	
		super.load(myElement,loader);
		gamma=myElement.getAttribute("gamma").getFloatValue();
		random_chance=myElement.getAttribute("random-chance").getFloatValue();
		
		qTable=(float[][][][])XMLArray.loadArray(this,loader);
		
		qTableAverageTripTime=(float[][][][])XMLArray.loadArray(this,loader); /*EJUST*/
		qTableAverageSpeed=(float[][][][])XMLArray.loadArray(this,loader); /*EJUST*/
		qTableAverageJunctionWaitingTime=(float[][][])XMLArray.loadArray(this,loader); /*EJUST*/
		
		vTable=(float[][][])XMLArray.loadArray(this,loader);
		
		vTableAverageTripTime=(float[][][])XMLArray.loadArray(this,loader); /*EJUST*/
		vTableAverageSpeed=(float[][][])XMLArray.loadArray(this,loader); /*EJUST*/
		vTableAverageJunctionWaitingTime=(float[][])XMLArray.loadArray(this,loader); /*EJUST*/
		
		count=(Vector[][][])XMLArray.loadArray(this,loader,this);
		pTable=(Vector[][][])XMLArray.loadArray(this,loader,this);
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	
		super.saveChilds(saver);
		XMLArray.saveArray(qTable,this,saver,"q-table");
		
		XMLArray.saveArray(qTableAverageTripTime,this,saver,"q-table-average-trip-time"); /*EJUST*/
		XMLArray.saveArray(qTableAverageSpeed,this,saver,"q-table-average-speed"); /*EJUST*/
		XMLArray.saveArray(qTableAverageJunctionWaitingTime,this,saver,"q-table-average-junction-waiting-time"); /*EJUST*/
		
		XMLArray.saveArray(vTable,this,saver,"v-table");
		
		XMLArray.saveArray(vTableAverageTripTime,this,saver,"v-table-average-trip-time"); /*EJUST*/
		XMLArray.saveArray(vTableAverageSpeed,this,saver,"v-table-average-speed"); /*EJUST*/
		XMLArray.saveArray(vTableAverageJunctionWaitingTime,this,saver,"v-table-average-junction-waiting-time"); /*EJUST*/
		
		XMLArray.saveArray(count,this,saver,"counts");
		XMLArray.saveArray(pTable,this,saver,"p-table");
	}

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	
		XMLElement result=super.saveSelf();
		result.setName(shortXMLName);
		result.addAttribute(new XMLAttribute ("random-chance",random_chance));
		result.addAttribute(new XMLAttribute ("gamma",gamma));
		return result;
	}

	public String getXMLName ()
	{ 	
		return "model."+shortXMLName;
	}

	public boolean canCreateInstance (Class request)
	{ 	
		System.out.println("Called TC1TLC-opt Bayesian instantiation assistant ??");
		return CountEntry.class.equals(request) ||	PEntry.class.equals(request);
	}

	public Object createInstance (Class request) throws ClassNotFoundException,InstantiationException,IllegalAccessException
	{ 	
		System.out.println("Called TC1TLC-opt Bayesian instantiation assistant");
		if (CountEntry.class.equals(request))
		{ 
			return new CountEntry();
		}
		else if ( PEntry.class.equals(request))
		{ 
			return new PEntry();
		}
		else
		{ 
			throw new ClassNotFoundException("TC1 Bayesian IntstantiationAssistant cannot make instances of "+ request);
		}
	}
}