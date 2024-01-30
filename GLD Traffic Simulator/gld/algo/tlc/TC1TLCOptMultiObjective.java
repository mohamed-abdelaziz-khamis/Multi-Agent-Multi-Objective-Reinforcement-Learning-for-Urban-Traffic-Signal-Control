
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
public class TC1TLCOptMultiObjective extends TCColearnPerformanceIndex implements Colearning, InstantiationAssistant, HECinterface, Constants /*EJUST*/
{
	// TLC vars
	protected Infrastructure infrastructure;
	protected TrafficLight[][] tls;
	protected Node[] allnodes;
	protected int num_nodes;
	protected float threshold = 0.8f; /*EJUST*/
	protected boolean hecAddon;

	// TC1 vars
	protected Vector count[][][], pTable[][][];
	
	protected float [][][][] qTable /*sign, pos, des, color (red=0, green=1)*/,
							 qTableMultiObjective /*EJUST*/,
							 qTableAverageTripTime /*EJUST*/,
							 qTableAverageSpeed /*EJUST*/;
	
	protected float [][][]	 qTableAverageJunctionWaitingTime /*EJUST*/;
	
	protected float [][][] vTable,
						   vTableMultiObjective /*EJUST*/,
						   vTableAverageTripTime /*EJUST*/,
						   vTableAverageSpeed /*EJUST*/;
	
	protected float [][]   vTableAverageJunctionWaitingTime /*EJUST*/;

	/*POMDPGLD*/
	protected Vector Tcount[][][], TpTable[][][];
	protected float[][][][] TqTable; //sign, pos, des, color (red=0, green=1)
	protected float[][][] TvTable;
	/*POMDPGLD*/

	protected static float gamma=0.90f;				//Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
	protected final static boolean red=false, green=true;
	protected final static int green_index=0, red_index=1;
	protected final static String shortXMLName="tlc-tc1o1-multiobjective";
	protected static float random_chance=0.01f;				//A random gain setting is chosen instead of the on the TLC dictates with this chance
	private Random random_number;

	// to make this one destless
	protected boolean useDestination = true; /*POMDPGLD*/
	/*EJUST: false --> true*/

	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */

	public TC1TLCOptMultiObjective ( Infrastructure infra ) throws InfraException
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
				qTableMultiObjective = new float [numSigns][][][]; /*EJUST*/				
				qTableAverageTripTime = new float [numSigns][][][]; /*EJUST*/
				qTableAverageSpeed = new float [numSigns][][][]; /*EJUST*/
				qTableAverageJunctionWaitingTime = new float [numSigns][][]; /*EJUST*/
				
				vTable = new float [numSigns][][];
				vTableMultiObjective = new float [numSigns][][]; /*EJUST*/
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
						int num_pos_on_dl = d.getCompleteLength(); /*EJUST comment: descretize the number of positions on drivelane by ceiling*/

						qTable[id] = new float [num_pos_on_dl][][];
						qTableMultiObjective[id] = new float [num_pos_on_dl][][]; /*EJUST*/
						qTableAverageTripTime[id] = new float [num_pos_on_dl][][]; /*EJUST*/
						qTableAverageSpeed[id] = new float [num_pos_on_dl][][]; /*EJUST*/
						qTableAverageJunctionWaitingTime[id] = new float [num_pos_on_dl][]; /*EJUST*/
						
						vTable[id] = new float [num_pos_on_dl][];
						vTableMultiObjective[id] = new float [num_pos_on_dl][]; /*EJUST*/
						vTableAverageTripTime[id] = new float [num_pos_on_dl][]; /*EJUST*/
						vTableAverageSpeed[id] = new float [num_pos_on_dl][]; /*EJUST*/
						vTableAverageJunctionWaitingTime[id] = new float [num_pos_on_dl]; /*EJUST*/
						
						count[id] = new Vector[num_pos_on_dl][];
						pTable[id] = new Vector[num_pos_on_dl][];

						for (int k=0; k<num_pos_on_dl; k++)	
						{
							qTable[id][k]=new float[num_specialnodes][];
							qTableMultiObjective[id][k]=new float[num_specialnodes][]; /*EJUST*/
							qTableAverageTripTime[id][k]=new float[num_specialnodes][]; /*EJUST*/
							qTableAverageSpeed[id][k]=new float[num_specialnodes][]; /*EJUST*/
							
							qTableAverageJunctionWaitingTime[id][k]	= new float [2]; /*EJUST*/
							qTableAverageJunctionWaitingTime[id][k][0]= 0.0f; /*EJUST*/
							qTableAverageJunctionWaitingTime[id][k][1]= 0.0f; /*EJUST*/
							
							vTable[id][k]=new float[num_specialnodes];
							vTableMultiObjective[id][k]=new float[num_specialnodes]; /*EJUST*/
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
								qTableMultiObjective[id][k][l]	= new float [2];
								qTableMultiObjective[id][k][l][0]= 0.0f;
								qTableMultiObjective[id][k][l][1]= 0.0f;
								
								/*EJUST*/
								qTableAverageTripTime[id][k][l]	= new float [2];
								qTableAverageTripTime[id][k][l][0]= 0.0f;
								qTableAverageTripTime[id][k][l][1]= 0.0f;
															
								/*EJUST*/
								qTableAverageSpeed[id][k][l]	= new float [2];
								qTableAverageSpeed[id][k][l][0]= 0.0f;
								qTableAverageSpeed[id][k][l][1]= 0.0f;
								
								vTable[id][k][l]	= 0.0f;
								
								vTableMultiObjective[id][k][l]	= 0.0f; /*EJUST*/								
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
		 *  For each Roaduser (EJUST: waiting -->in the lane)
		 *   gain = gain + pf*(Q([tl,pos,des],red) - Q([tl,pos,des],green))
		 */
		int num_dec, waitingsize, pos, tlId, desId;
		float gain, passenger_factor;
				
		Sign tl;
		Drivelane lane;
		Roaduser ru;
		ListIterator queue;
		Node destination;
		ListIterator mlQueue = null; /*POMDPGLD*/ 

		//Determine whether it should be random or not
		boolean randomrun = false;
		if (random_number.nextFloat() < random_chance) 
			randomrun = true;

		// For all Nodes
		for (int i=0;i<num_nodes;i++) {
			num_dec = tld[i].length;

			// For all Trafficlights
			for(int j=0;j<num_dec;j++) {
				
				tl = tld[i][j].getTL();
				tlId = tl.getId();
				lane = tld[i][j].getTL().getLane();

				//Comments done by EJUST as in the multi-objective case it is not optional
				//if(SimModel.useAllRoadusers) /*POMDPGLD*/
				//{
					waitingsize = lane.getQueue().size(); /*POMDPGLD*/
				//}
				//else
				//{
				//	waitingsize = lane.getNumRoadusersWaiting();
				//}

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
						{}
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
					
					// place holder... will be initialized properly further down
					pos = 0;

					//HEC Add on: Congestion weight factor used in calculation of the gain.
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

							//HEC Add on: Congestion weight factor used in calculation of the gain.
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

						//HEC Add-on: Congestion weight factor used in calculation of the gain.
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

						if(pos < lane.getCompleteLength()-1) {
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
										(qTableMultiObjective[tlId][pos][desId][red_index]-
										 qTableMultiObjective[tlId][pos][desId][green_index]); //red - green
					}
				}

				// Debug info generator
				if(waitingsize>0 /*EJUST*/ && trackNode!=-1 && i==trackNode) {
					boolean[] targets = lane.getTargets();
					System.out.println("\n node: "+i+" light: "+j+" gain: "+gain+" "+ 
							" lane: "+ tlId + " targets: "+ /*EJUST*/
							targets[0]+" "+targets[1]+" "+targets[2]+" "+
							" Num Roadusers Waiting in the lane: "+ /*EJUST*/
							waitingsize);
				}

				// If this is a random run, set all gains randomly
				if(randomrun)
					gain = random_number.nextFloat();

				if(gain > 1000.0 || gain < -1000.0f)
					System.out.println("Gain might be too high? : "+gain);
				
				/*EJUST comment: For node i, for traffic light j*/
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
			
			recalcP(tlId, prevpos_int, desId, prevsign.mayDrive(), signnow.getId(), posnow_int);
			recalcQ(tlId, prevpos, desId, prevsign.mayDrive(), signnow.getId(), posnow, posMovs, penalty);
			recalcV(tlId, prevpos_int, desId);
			
            /*
			if(tlId == 15) {
            	printValues(tlId);
            	printPValues(tlId);
        	}*/
		}
	}

	protected void recalcP(int tlId, int pos, int desId, boolean light, int tlNewId, int posNew)
	{
		// - First create a CountEntry, find if it exists, and if not add it.
		CountEntry thisSituation = new CountEntry(tlId, pos, desId, light, tlNewId, posNew);

		int	c_index;
		try { /*POMDPGLD*/			
			c_index = count[tlId][pos][desId].indexOf(thisSituation);
		} /*POMDPGLD*/
		catch(ArrayIndexOutOfBoundsException ex) { /*POMDPGLD*/
			System.err.print("ArrayIndexOutOfBoundsException in: TC1TLCOptMultiObjective, r: 504, values: tlId: "+tlId+" pos: "+pos+" desId: "+desId);
			c_index = 0;
		}/*POMDPGLD*/

		if(c_index >= 0) {
			// Entry found
			thisSituation = (CountEntry) count[tlId][pos][desId].elementAt(c_index);
			thisSituation.incrementValue();			
		}
		else {
			// Entry not found
			count[tlId][pos][desId].addElement(thisSituation);
		}

		// We now know how often this exact situation has occurred
		// - Calculate the chance
		long sameSituation = thisSituation.getValue();
		long sameStartSituation = 0;
		
		CountEntry curC;
		int num_c = count[tlId][pos][desId].size();
		PEntry curP;
		int p_index;
		for(int i=0;i<num_c;i++) {
			curC = (CountEntry) count[tlId][pos][desId].elementAt(i);
			sameStartSituation	+= curC.sameStartSituation(thisSituation);			
		}

		// - Update this chance
		// Calculate the new P(L|(tl,pos,des))
		// P(L|(tl,pos,des))	= P([tl,pos,des],L)/P([tl,pos,des])
		//						= #([tl,pos,des],L)/#([tl,pos,des])

		// Niet duidelijk of dit P([tl,p,d],L,[*,*]) of P([tl,p,d],L,[tl',p']) moet zijn
		// Oftewel, kans op deze transitie of kans om te wachten!

		// Not clear if this P ([tl, p, d], L ,[*,*]) or P ([tl, p, d], L, [tl', p']) must be
		// In other words, this transition probability or chance to wait!

		PEntry thisChance = new PEntry(tlId, pos, desId, light, tlNewId, posNew);
		p_index = pTable[tlId][pos][desId].indexOf(thisChance);

		if(p_index >= 0)
			thisChance = (PEntry) pTable[tlId][pos][desId].elementAt(p_index);
		else {
			pTable[tlId][pos][desId].addElement(thisChance);
			p_index = pTable[tlId][pos][desId].indexOf(thisChance);
		}

		thisChance.setSameSituation(sameSituation);
		thisChance.setSameStartSituation(sameStartSituation);					


		// - Update rest of the Chance Table
		int num_p = pTable[tlId][pos][desId].size();
		for(int i=0;i<num_p;i++) {
			curP = (PEntry) pTable[tlId][pos][desId].elementAt(i);
			if(curP.sameStartSituation(thisSituation) && i!=p_index)
				curP.addSameStartSituation();			
		}
	}

	protected void recalcQ(int tlId, double pos /*EJUST: int --> double*/, int desId, boolean light, 
						   int tlNewId, double posNew /*EJUST: int --> double*/, PosMov[] posMovs, int penalty)
	{	
		// Meneer Kaktus zegt: OK!
		// Q([tl,p,d],L)	= Sum(tl', p') [P([tl,p,d],L,[tl',p'])(R([tl,p],[tl',p'])+ yV([tl',p',d]))

		/* EJUST: 
		 * =====
		 * P([tl,p,d],L,[tl',p']) = P([tl',p']|[tl,p,d],L)
		 * P([tl',p']|[tl,p,d],L) = #([tl,p,d],L,[tl',p'])/#([tl,p,d],L)
		 * */ 

		// First gather All tl' and p' in one array

		int num_posmovs	= posMovs.length;

		PosMov curPosMov;
		int curPMTlId /*Current PositionMove instance .Traffic Light Id*/, curPMPos /*Current PositionMove instance .Position*/;

		float R = 0, V=0, Q=penalty;
		
		/*EJUST*/
		float V_MultiObjective=0, V_AverageTripTime=0,  V_AverageSpeed=0, V_AverageJunctionWaitingTime=0,
			  Q_MultiObjective=penalty, Q_AverageTripTime=penalty, Q_AverageSpeed=penalty, Q_AverageJunctionWaitingTime=penalty;
		
		/*EJUST*/
		double R_AverageTripTime = 0, R_AverageSpeed = 0, R_AverageJunctionWaitingTime = 0, R_FlowRate = 0, R_Safety = 0,
				weightFlowRate = 1, chance = 0, chance_AJWT = 0;
		
		int pos_int = (int)Math.ceil(pos); /*EJUST: cast to int*/
		
		/*if (tlId!=tlNewId){ //EJUST: In this case: s=(tlNewId,posNew) absorbing state
			vTable[tlNewId][posNew][desId] = -100; 
			qTable[tlId][pos][desId][green_index] = 0; //The vehicle waiting time at this junction = 0
		}
		else*/
		for(int t=0; t < num_posmovs; t++) {		// For All tl', pos'
	
			curPosMov = posMovs[t];
			curPMTlId = curPosMov.sign.getId() /*EJUST: tlId --> sign.getId() */;
				
			curPMPos  = (int) Math.ceil (curPosMov.pos); /*EJUST: cast to int*/
	
			PEntry P = new PEntry(tlId, pos_int, desId, light, curPMTlId, curPMPos);
			int p_index = pTable[tlId][pos_int][desId].indexOf(P);
	
			if(p_index >= 0) {								
					
				P = (PEntry) pTable[tlId][pos_int][desId].elementAt(p_index);
				
				R = rewardFunction(tlId, pos, curPMTlId, curPosMov.pos); 
					
				/*EJUST*/
				R_AverageJunctionWaitingTime = rewardFunctionAverageJunctionWaitingTime(tlId, curPMTlId);
				R_FlowRate = rewardFunctionFlowRate(tlId, curPMTlId); 
				R_Safety = rewardFunctionSafety(tlId, pos, light, curPMTlId, curPosMov.pos); 				 						
					
				/*EJUST: Speed reward = delta p in every time step.*/
				if (curPMTlId!=tlId){ 
					weightFlowRate = weightFunctionFlowRate(curPosMov.sign.getLane());
					R_AverageSpeed = curPosMov.sign.getLane().getCompleteLength() - curPosMov.pos + pos;
				}
				else //weightFlowRate = 1 ==> R_FlowRate = 0
					R_AverageSpeed = pos - curPosMov.pos;
				
				/*EJUST: If the vehicle waits at its position (leads to high ATT), then it is penalized by the reward value.*/
				R_AverageTripTime = Math.pow(2, -R_AverageSpeed); 	
				
				V = vTable[curPMTlId][curPMPos][desId];
				
				/*EJUST*/
				V_MultiObjective = vTableMultiObjective[curPMTlId][curPMPos][desId]; 					
				V_AverageTripTime = vTableAverageTripTime[curPMTlId][curPMPos][desId];
				V_AverageSpeed = vTableAverageSpeed[curPMTlId][curPMPos][desId];
				V_AverageJunctionWaitingTime = vTableAverageJunctionWaitingTime[curPMTlId][curPMPos]; 
					
				/*EJUST*/
				chance = P.getChance();
					
				Q += chance * (R + (gamma * V));
					
				/*EJUST*/
				Q_MultiObjective += chance * (
						R +  
						R_AverageTripTime + 
						R_AverageJunctionWaitingTime + 
						weightFlowRate * R_FlowRate + 
						//R_Safety +  
						(gamma * V_MultiObjective));
										
				/*EJUST: Reward = 1 as we always count 1 time step either when the vehicle is stopping or moving.*/
				Q_AverageTripTime += chance*(1 + (gamma * V_AverageTripTime)); 					
				Q_AverageSpeed += chance*(R_AverageSpeed + (gamma * V_AverageSpeed));
				
				/*EJUST*/
				chance_AJWT = 0;
				for (int i = 0; i < pTable[tlId][pos_int].length; i++){
					PEntry P_AJWT = new PEntry(tlId, pos_int, i, light, curPMTlId, curPMPos);
					int p_index_AJWT = pTable[tlId][pos_int][i].indexOf(P_AJWT);			
					if(p_index_AJWT >= 0) {							
						P_AJWT = (PEntry) pTable[tlId][pos_int][i].elementAt(p_index_AJWT);
						chance_AJWT+=P_AJWT.getChance();
					}
				}
				Q_AverageJunctionWaitingTime += chance_AJWT * (R + (gamma * V_AverageJunctionWaitingTime));
			}
			// Else P(..)=0, thus will not add anything in the summation
		}
		
		/*POMDPGLD*/
		if(qTable[tlId][pos_int][desId][light ? green_index : red_index] != 0 && Q == 0 ) { SimModel.state_zeros++ ;  }
		if(qTable[tlId][pos_int][desId][light ? green_index : red_index] == 0 && Q != 0 ) { SimModel.state_zeros-- ;  }
		
		qTable[tlId][pos_int][desId][light?green_index:red_index]=Q;
				
		/*EJUST*/
		qTableMultiObjective[tlId][pos_int][desId][light?green_index:red_index]=Q_MultiObjective; 
		qTableAverageTripTime[tlId][pos_int][desId][light?green_index:red_index]=Q_AverageTripTime; 
		qTableAverageJunctionWaitingTime[tlId][pos_int][light?green_index:red_index]=Q_AverageJunctionWaitingTime; 
	}

	protected void recalcV(int tlId, int pos, int desId)
	{	
		//  V([tl,p,d]) = Sum (L) [P(L|(tl,p,d))Q([tl,p,d],L)]

		float qRed		= qTable[tlId][pos][desId][red_index];
		
		/*EJUST*/
		float qRedMultiObjective  = qTableMultiObjective[tlId][pos][desId][red_index]; 
		float qRedAverageTripTime = qTableAverageTripTime[tlId][pos][desId][red_index];
		float qRedAverageSpeed = qTableAverageSpeed[tlId][pos][desId][red_index];
		float qRedAverageJunctionWaitingTime = qTableAverageJunctionWaitingTime[tlId][pos][red_index]; 
		
		float qGreen	= qTable[tlId][pos][desId][green_index];
		
		/*EJUST*/
		float qGreenMultiObjective	= qTableMultiObjective[tlId][pos][desId][green_index]; 
		float qGreenAverageTripTime	= qTableAverageTripTime[tlId][pos][desId][green_index];
		float qGreenAverageSpeed	= qTableAverageSpeed[tlId][pos][desId][green_index];
		float qGreenAverageJunctionWaitingTime	= qTableAverageJunctionWaitingTime[tlId][pos][green_index]; 
		
		float[] pGR 	= calcPGR(tlId,pos,desId);
		float pGreen	= pGR[green_index];
		float pRed		= pGR[red_index];

		vTable[tlId][pos][desId] = (pGreen*qGreen) + (pRed*qRed);
		
		/*EJUST*/
		vTableMultiObjective[tlId][pos][desId] = (pGreen*qGreenMultiObjective) + (pRed*qRedMultiObjective); 
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

		int psize = pTable[tlId][pos][desId].size()-1;

		for(; psize>=0; psize--) {
			PEntry cur = (PEntry) pTable[tlId][pos][desId].elementAt(psize);
			if(cur.light==green){
				countG += cur.getSameSituation();
			}
			else{
				countR += cur.getSameSituation();
			}
		}

		counters[green_index] = (float)(countG/(countG+countR)); 
		counters[red_index] = (float)(countR/(countG+countR)); 		
		
		return counters;
	}

	/*EJUST: If the vehicle waits at its position (leads to high ATWT), then it is penalized by the reward value*/
	protected int rewardFunction(int tlId, double pos, int tlNewId, double posNew) {		
		if (tlId!=tlNewId || pos-posNew > STOP_SPEED_MS*TIMESTEP_S /*EJUST replaced: pos != posNew*/ /*|| light*/)
			return 0;		
		return 1; //tlId==tlNewId and pos-posNew <= SPEED_ONE_TENTH_MS*TIMESTEP_S /*EJUST replaced: pos == posNew*/  /*L = Red*/		
		/*EJUST: 
		 * The reward on this form represents: Minimize the Average Trip Waiting Time 
		 * */
	}
	
	/*EJUST: If the vehicle waits at its position (leads to high ATT), then it is penalized by the reward value.*/
	protected double rewardFunctionAverageTripTime(int tlId, double pos, boolean light, int tlNewId, double posNew) {		
		if (tlId!=tlNewId) // L = Green. tl<>tl’ then for sure pos<>pos'. 
			return /*1*/-Math.pow(2, -(posNew-pos)); 
		/*If posNew>>pos ==> vehilce makes small step ==> Q(Green) = 1 ==> Medium Gain
		 *If posNew ~ pos ==> vehilce makes huge step ==> Q(Green) = 0 ==> Higher Gain*/
		
		//if (!light){ // L = Red
			return Math.pow(2, posNew-pos); 
			/* If posNew<<pos ==> vehilce makes huge step ==> Q(Red), Q(Green) = 0 ==> Lower Gain
			 * If posNew ~ pos ==> vehilce makes small step ==> Q(Red), Q(Green) = 1 ==> The same as ATWT */
		//}

		//return 0;
		/*EJUST: 
		 * The reward on this form represents: Minimize the Average Trip Time 
		 * */		
	}


	/*EJUST: If the vehicle waits at the current lane, then will be penalized by the reward value.*/
	protected int rewardFunctionAverageJunctionWaitingTime(int tlId, int tlNewId) {		
		if (tlId==tlNewId) //Junction waiting time will increase due to the current lane has red light or congestion
			return 1; // tl=tl’ regardless the pos		
		return 0; // L = Green. tl<>tl’ then for sure pos<>pos'. 
		
		/*EJUST: 
		 * The reward on this form represents the following:
		 * Minimize the Average Junction Waiting Time 
		 * 		Two cases: either staying in the same junction (regardless the position) or crossing the Junction
		 * Maximize the expected number of junction crossings
		 * Minimize the expected number of vehicle stops (satisfying a green wave)
		 * 
		 * No need to change the structure of the states table but sum on all possible destinations
		 * */		
	}

	/*EJUST: Green: If there is high congestion in the next lane, then the vehicle is penalized by the reward value*/
	protected double rewardFunctionFlowRate(int tlId, int tlNewId) {		
		if (tlId!=tlNewId) // L = Green. tl<>tl’ then for sure pos<>pos'.
			return 1;		
		return 0;		
		/*EJUST: 
		 * The reward on this form represents the following:
		 * 	Maximize the flow rate
		 *  Minimize the congestion in next lane
		 *  Minimize the length of waiting road users in the next lane
		 */		
	}
	
	/*EJUST: If the vehicle stays at the same position, then will be rewarded by the reward value*/
	protected double rewardFunctionSafety(int tlId, double pos, boolean light, int tlNewId, double posNew) {
		
		if (tlId!=tlNewId) // L = Green. tl<>tl’ then for sure pos<>pos'.
			return Math.pow(2, -(posNew-pos));  //If posNew>>pos ==> vehilce make small step ==> Q(Green) = 0 ==> Higher Gain		
		
		if (!light){ // L = Red
			return Math.pow(2, posNew-pos); //If posNew<<pos ==> vehilce makes huge step ==> Q(Red) = 0 ==> Lower Gain
		}
		else { // L = Green and tlId==tlNewId
			return 1-Math.pow(2, posNew-pos); //If posNew<<pos ==> vehilce makes huge step ==> Q(Green) = 1 ==> Lower Gain
		}
		
		/*EJUST: 
		 * The reward on this form represents the following:
		 * 	Maximize the safety
		 *  Minimize the speed (minimize the distance travelled per unit time)
		 */
	}
	
	/*EJUST*/
	protected double weightFunctionFlowRate(Drivelane dlaneNew){
		float percWaiting = (float) dlaneNew.getNumBlocksWaiting() / (float) dlaneNew.getLength();
		if (percWaiting <= threshold)
			return 0;
		else if (percWaiting > threshold && percWaiting <= 1)
			return 10 * (percWaiting - threshold); //e.g. 10 * (9/10 - 0.8) = 1
		else //if (percWaiting > 1)
			return 2;
	}
	
	public float getVValue(Sign sign, Node des, int pos) {
		return vTable[sign.getId()][pos][des.getId()];
	}

	/*POMDPGLD*/
	public void printValues(int tlID) {
		System.out.print("\n**********************");
		System.out.print("\nV\t");
		for(int i = 0; i < vTableMultiObjective[tlID].length; i++)
		{
			System.out.print(vTableMultiObjective[tlID][i][0] + " ");
		}

		System.out.print("\nQ:green\t");
		for(int i = 0; i < qTableMultiObjective[tlID].length; i++)
		{
			System.out.print(qTableMultiObjective[tlID][i][0][green_index] + " ");
		}

		System.out.print("\nQ:red\t");
		for(int i = 0; i < qTableMultiObjective[tlID].length; i++)
		{
			System.out.print(qTableMultiObjective[tlID][i][0][red_index] + " ");
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
		if(b) c.setStatus("Using HEC on TC1TLCOptMultiObjective");
		else c.setStatus("Using TC1TLCOptMultiObjective without HEC");
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
		}

		public CountEntry ()
		{ 
			// Empty constructor for loading
		}

		public void incrementValue() {
			value++;
		}

		// Returns how often this situation has occurred
		public long getValue() {
			return value;
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
			
		boolean light;

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

		public double getChance() {				
			return getSameSituation()/getSameStartSituation();						
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
		
		qTableMultiObjective=(float[][][][])XMLArray.loadArray(this,loader); /*EJUST*/
		qTableAverageTripTime=(float[][][][])XMLArray.loadArray(this,loader); /*EJUST*/
		qTableAverageJunctionWaitingTime=(float[][][])XMLArray.loadArray(this,loader); /*EJUST*/
		
		vTable=(float[][][])XMLArray.loadArray(this,loader);
		
		vTableMultiObjective=(float[][][])XMLArray.loadArray(this,loader); /*EJUST*/
		vTableAverageTripTime=(float[][][])XMLArray.loadArray(this,loader); /*EJUST*/
		vTableAverageJunctionWaitingTime=(float[][])XMLArray.loadArray(this,loader); /*EJUST*/
		
		count=(Vector[][][])XMLArray.loadArray(this,loader,this);
		pTable=(Vector[][][])XMLArray.loadArray(this,loader,this);
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	
		super.saveChilds(saver);
		XMLArray.saveArray(qTable,this,saver,"q-table");
		
		XMLArray.saveArray(qTableMultiObjective,this,saver,"q-table-multi-objective"); /*EJUST*/
		XMLArray.saveArray(qTableAverageTripTime,this,saver,"q-table-average-trip-time"); /*EJUST*/
		XMLArray.saveArray(qTableAverageJunctionWaitingTime,this,saver,"q-table-average-junction-waiting-time"); /*EJUST*/
		
		XMLArray.saveArray(vTable,this,saver,"v-table");
		
		XMLArray.saveArray(vTableMultiObjective,this,saver,"v-table-multi-objective"); /*EJUST*/
		XMLArray.saveArray(vTableAverageTripTime,this,saver,"v-table-average-trip-time"); /*EJUST*/ 
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
		System.out.println("Called TC1TLC-opt instantiation assistant ??");
		return CountEntry.class.equals(request) ||	PEntry.class.equals(request);
	}

	public Object createInstance (Class request) throws ClassNotFoundException,InstantiationException,IllegalAccessException
	{ 	
		System.out.println("Called TC1TLC-opt instantiation assistant");
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
			throw new ClassNotFoundException("TC1 IntstantiationAssistant cannot make instances of "+ request);
		}
	}
}