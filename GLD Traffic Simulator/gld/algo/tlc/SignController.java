
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

package gld.algo.tlc;

import gld.GLDSim;
import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Junction;
import gld.infra.Node;
import gld.infra.NonTLJunction;
import gld.infra.Road;
import gld.infra.Sign;
import gld.utils.Arrayutils;
import gld.xml.XMLArray;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;

import java.io.IOException;
import java.util.Random;

/**
 *
 * This is the class for the sign controller. Here is decided how each sign should work.
 * Reward values are gathered for each sign, when set on 'Green' asking the applied TLController.
 * Non-TrafficLight signs can also be implemented when a Q-value algorithm is implemented for the appropriate
 * type of sign. After those values are gathered, this class will select the most rewarding
 * traffic light configuration for each Node and will set the Signs accordingly.
 * @see gld.infra.Sign
 *
 * @author Group Algorithms
 * @version 1.0
 */
public class SignController implements XMLSerializable
{
	/** The TLcontroller is used to gather Q-values and use them to set all the TLs */
	protected TLController tlcontroller;

	/**
	 * Indicates if traffic lights must be switched safely or not.
	 * Safely means no 2 traffic lights may be turned to green at the same
	 * time, if road users moving from the drive lanes behind those signs
	 * would collide on the node.
	 */
	public static boolean CrossNodesSafely = true;
	protected static final String shortXMLName = "signcontroller";
	protected Infrastructure infra;
	protected int num_nodes;
	//protected int[] chosenConfigs;
	protected Sign[][] currentSC;
	Random generator;
	protected Random random; /*POMDPGLD*/

	/** EJUST: useful in comparing float datatype variables*/
	protected static double EPSILON = 0.0001; 
	
	/** EJUST 
	 * 
	 * where T is a temperature parameter indicating how stochastic we want to make the initial policy. 
	 * For simplicity of presentation, we will use T = 1.
	 * 
	 * When T is large, each action will have approximately the same probability to be selected (i.e., as the uniform probability)
	 * When T is small, actions will be selected proportionally according to their estimated gain.
	 */
	protected static float T=1;
	
	/** EJUST: used when comparing the previous gain of each node with the newly calculated one
	 * If the difference exceeds some threshold for n consecutive time steps (which means a long transient period), 
	 * then use the softmax method (for better exploration)
	 * 
	 * This threshold is used to detect that some traffic light configuration dominates with 20% of its value
	 */
	protected static double THRESHOLD = 0.2;
	
	public SignController(TLController t, Infrastructure i) {
		tlcontroller = t;
		infra = i;
		num_nodes = i.getNumNodes();
		
		generator = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
		random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]); /*POMDPGLD*/
		
		/*chosenConfigs = new int[i.getAllNodes().length];
		for (int j=0; j<chosenConfigs.length; j++) 
			chosenConfigs[j]=-1; 
			// This should be done, otherwise all chosenConfigs are 0. 
			 * This leads to problems, when config 0 is the first time the best config.*/
		
		currentSC = new Sign[num_nodes][0];
	}

	public Infrastructure getInfrastructure() { 
		return infra; 
	}

	public void setInfrastructure(Infrastructure i)
	{
		generator = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]); /*POMDPGLD*/
        random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]); /*POMDPGLD*/
		
		infra = i;
		num_nodes = i.getNumNodes();
		
		/*chosenConfigs = new int[i.getAllNodes().length];
		for (int j=0; j<chosenConfigs.length; j++) 
			chosenConfigs[j]=-1;*/
		
		currentSC = new Sign[num_nodes][0];
	}
	
	public TLController getTLC() { return tlcontroller; }
	public void setTLC(TLController t) { tlcontroller = t; }

	/**
	 * Switch all the signs of all nodes in the infrastructure to their appropriate value.
	 */
	public void switchSigns()
	{
		/*Calculates how every traffic light should be switched*/
		TLDecision[][] decisions = tlcontroller.decideTLs();		
		
		Node node = null;
		
		/*Returns all nodes (including edge nodes)*/
		Node[] nodes = infra.getAllNodes();
		
		if (num_nodes	>	decisions.length)
			System.out.println("SignController switchSigns WARNING : "+"Less decisions than nodes !!!");
		
		for (int i=0; i < num_nodes; i++) {
			node = nodes[i];
			if (node.getType() == Node.JUNCTION) {							
				if (decisions[i].length > 0) {
					//SBC /////////////////////////////////////////////////////////////////
					if (tlcontroller.getKeepSwitchControl()) {
						if (!(node.getKeepTLDFlag()))
							//keepTLDFlag is used to fixate a specific traffic light configuration
							switchTrafficLights((Junction)node, decisions[i]);
					}
					else { 
						switchTrafficLights((Junction)node, decisions[i]);
						//System.out.println("Sign Controller: switch control off"); /*EJUST commented*/
					}
					//SBC/////////////////////////////////////////////////////////////////
				}
				else if (node.getType() == Node.NON_TL )
				{
					switchNonTrafficLights((NonTLJunction)node, decisions[i]);
				}			
			}
		}
	}
	
	/*POMDPGLD*/
	public void reset() {
        generator = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
        random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
    }

	/**
	 * Switch the non-TLsigns to their appropriate values according to normal traffic rules
	 * On a normal junction traffic from the right gets priority
	 * @param node The node involved.
	 */
	private void switchNonTrafficLights(NonTLJunction node, TLDecision[] dec)
	{
		System.out.println("NonTLSwitch");
		
		//Sign[][] signConfigs = node.getSignConfigs() ;
		Sign[] signs = node.getSigns() ;
		boolean[] mayDrive = { false, false, false, false } ;
		
		//Check for traffic from the right
		Road[] roads = node.getAllRoads(); //In clockwise order I hope/think ;)
		Road right = roads[3] ;//Start with last road as being right
		
		Drivelane[] lanes ;
		boolean alpha = false ;

		for ( int i=0 ; i < roads.length ; i++ )//length is always 4
		{
			alpha = right.getAlphaNode() == node ;//Check which are the incoming lanes
			lanes = alpha ? right.getAlphaLanes() : right.getBetaLanes() ;
			
			for ( int  j=0; j < lanes.length ; j++ ) 
			{
				if ( lanes[j].getNumRoadusersWaiting() > 0 ) 
				{
					mayDrive[i] = false ;//Traffic is coming from the right
				}
				else 
				{
					mayDrive[i] = true ;
				}
			}
		}

		//Random random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]) ; /*POMDPGLD*/
		
		boolean deadlock = true ;//All roads are waiting for eachother
		int choosenRoad = -1 ;
		
		//length is always 4
		for (int i=0; i<roads.length; i++)	
		{
			if(deadlock && mayDrive[i])	
			{
				deadlock = false ;
				choosenRoad = i ;
			}
		}
		
		if (deadlock)	{
			choosenRoad = (int)Math.floor(random.nextFloat()*4) ;
		}
		
		//Now switch the lights
		lanes = null ;
		
		try 
		{
			lanes = node.getInboundLanes() ;
		}
		catch (InfraException e) 
		{
			System.out.println("An error occured while setting non-Traffic lights") ;
		}

		for ( int i=0 ; i < lanes.length ; i++ ) 
		{
			if ( lanes[i].getRoad() == roads[i] )
			{
				signs[i].setState( true ) ;
			}
			else 
			{
				signs[i].setState( false ) ;
			}
		}
	}

	/**
	 * Switch the TLsigns to their appropriate values
	 * The configuration with highest total gain is chosen
	 * @param dec The decision array consists of the generated values.
	 */
	public void switchTrafficLights(Junction node, TLDecision[] dec)
	{	
		//System.out.println("Switching TL's on junction " + node.getId());
		
		/* EJUST: 
		 * This boolean is used to check whether to use in this time step 
		 * the network-level "default" epsilon-greedy exploration
		 * or to use the adaptive (to transient state) softmax exploration 
		 * (i.e., probabilistic decision making according to gain values) 
		 * Only used with the Hybrid exploration enhanced multi-objective bayesian traffic light controller 
		 * */
		boolean useSoftmaxExploration = false;
		
		Sign[][] signConfs = node.getSignConfigs(); //possible green light configurations at a node
		Sign[] signs = node.getSigns();

		int num_sc = signConfs.length;

		Sign[][] possibleSC = new Sign[num_sc][];
		int p_index = 0; //possible green light configurations index 

		float maxGain = Float.NEGATIVE_INFINITY;//EJUST commented: Float.MIN_VALUE;		
		float gain;

		Sign[] thisSC;
		int num_thissc;
		
		int num_dec = dec.length;

		/*EJUST: For probabilistic decision making according to gains*/
		float gainTotal = 0;
		float[] gainThreshold = new float[num_sc];
		float[] gainList = new float[num_sc];
		
		for (int i=0; i < num_sc /*Typically there are 8 possible sign configurations*/; i++) {
			gain = 0;
			thisSC = signConfs[i]; //number of signs in this sign configuration
			num_thissc = thisSC.length; 

			// Summation of all gains in this SignConfig
			for (int j=0; j < num_thissc /*Typically there are two signs in this sign configuration*/; j++) {
				for (int k=0; k < num_dec /*Typically there are 8 decisions each of one traffic light: dec[k].getTL()*/; k++) {
					if (dec[k].getTL() == thisSC[j]) {
						gain += dec[k].getGain();
					}
				}
			}
			
			/* EJUST:
			 * Dr. Walid proposed new decision making strategy: Probabilistic decision making according to gains
			 * The proposed method is made for better adaptation in (exploration and decision making)
			 * Better exploration especially in transient periods, i.e., not just depending on the small epsilon-greedy exploration.
			 * Better decision making, by avoiding the domination of the traffic light configuration of the highest gain.
			 */ 		
			gainTotal += Math.exp(gain/T);
			gainThreshold[i] = gainTotal;
			gainList[i] = gain;
				
			// System.out.println("gain: " + gain + ", max gain: " + maxGain);
			if (tlcontroller.getRandomRun() 
			   /* In case of using the Hybrid Exploration and this time step was scheduled to be random (when using an epsilon exploration)
			 	* then all sign configurations are "possible" sign configurations,
			 	* i.e., have equal chance to be chosen */ 
					|| Math.abs(gain-maxGain) < EPSILON /*EJUST commented: gain = maxGain*/) 
			{
				possibleSC[p_index] = thisSC;
				p_index++;
			}
			else if (gain > maxGain) {			// If the gain of this SignConfig is better than the Max till now...
				possibleSC[0] = thisSC;
				p_index = 1;
				maxGain = gain;
			}
		}
		
		/*EJUST*/
		boolean previousNodeTransient = false;
		try {
			Drivelane[] inboundLanes = node.getAvailableInboundLanes();
							
			for (int i=0; i < inboundLanes.length; i++){
				//Returns the Node that this Drivelane comes from
				Junction node_previous = (Junction) inboundLanes[i].getNodeComesFrom();				
				
				//Returns an array of all available inbound lanes supporting roadusers of given type that lead to the given outbound lane.
				//lanesLeadingTo = node_previous.getAvailableLanesLeadingTo(inboundLanes[i], inboundLanes[i].getType());
				/* It is not preferable to check the transient state on the lane level, 
				   but rather, we prefer to check the transient state similar to the current node 
				   in which the transient state is checked on the node level 
				   because the single lane gain (or even a specific traffic light configuration gain) 
				   is normally increasing and decreasing every time step even with fixed generation rate
				   i.e., when traffic light turns red --> gain increases (in order to accordingly switch green), 
				   and when traffic light turns green --> gain decreases (in order to accordingly switch red again), and so on*/
				
				if(node_previous.getTransientPeriod() >= 10){
					previousNodeTransient = true;
					break;
				}									
			}
		} 
		catch (InfraException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		catch (ClassCastException e){
			// TODO Auto-generated catch block
			//e.printStackTrace();			
		}
		
		boolean currentNodeTransient = (node.getTransientPeriod() >= 10)?true:false;
		
		/* EJUST: 
		 * This part means that this junction does not reach its steady state yet, 
		 * due to either a transient period or an accident (such that in steady state the gain value almost becomes fixed), 
		 * thus for better exploration, we propose to use the softmax exploration 
		 * instead of the network-level "default" epsilon-greedy exploration.
		 * 
		 * Note that when the threshold value is very large, the exploration will remain epsilon 
		 * (i.e., useSoftmaxExploration will remain false as default)
		 * it is almost impossible that this MUCH sudden change happens during just one time step
		 * 
		 * When the threshold value is very small, the exploration will always be softmax (i.e., either in steady state or transient) 
		 * because the difference between any two successive gain values will be greater than the very small value of the threshold
		 */
				
		if (currentNodeTransient /*current node does not reach steady state yet*/ || previousNodeTransient /*previous node does not reach steady state yet*/){
		/* In all other controllers that do not study the transient period behavior (as we did), 
		 * node.getTransientPeriod() will be equal zero (initial value)
		 * thus, will not enter in this part of code that is dedicated to the Hybrid Exploration Controller (i.e., probabilistic decision making)
		 */	
			/* No need for checking whether one gain dominate the other gain
			 * It is sufficient to check whether the current node or one of its neighbors are transient
			 * Initially, all system is transient, then use softmax (which is good in not allowing the main road to dominate)
			 * -->Another point of view: No if the same generation rate in all directions then better to use epsilon
			 * At the middle, if the system is transient but due to a congestion in the main road, thus it is unfair in this situation to use softmax 
			 * because in this case the main road have to dominate 
			 * -->Another point of view: It will dominate but without complete blockage to the other side when using epsilon exploration
			 * */
			float absoluteDiff = 0;
			for (int i = 0; i < num_sc - 1; i++) {
				for (int j = i+1; j < num_sc /*Typically there are 8 sign configurations*/; j++){
					absoluteDiff = Math.abs(gainList[i]-gainList[j]);
					if (absoluteDiff >= THRESHOLD*Math.abs(gainList[i]) ||
							absoluteDiff >= THRESHOLD*Math.abs(gainList[j])){  
						/* At least one gain dominates with somehow a big threshold (i.e., does not become green with slight domination)*/											
						useSoftmaxExploration = true;
						break;
					}
					/* PS: When all gains are almost equal (which is a rare case in real life because there is always a main road that dominates)
					 * i.e., for all i, j: Math.abs(gainList[i]-gainList[j]) < THRESHOLD*Math.abs(gainList[i])
					 * it is better to use the epsilon-greedy exploration, 
					 * such that the softmax exploration causes higher oscillation between the sign configurations of equal gains
					 */
				}
				if (useSoftmaxExploration == true)
					break;
			}
		}
		
		float uniformRND = generator.nextFloat();
		/*EJUST: Probabilistic decision making according to gains*/	
		if (useSoftmaxExploration){
			for (int i=0; i < num_sc; i++){		
				if(uniformRND < gainThreshold[i]/gainTotal){
					possibleSC[0] = signConfs[i];
					p_index = 1;
					break;
				}
			}
		}	
		
		/*  SBC commented the functionality of: 
		 *  if all gains are zeros or negatives (that was happening when p_index == 0), choose random configuration
		 *  And instead SBC set all signs to red
		 * 	
		 *  int num_signs = signs.length;
		 *	for (int i=0; i < num_signs; i++)
		 *		signs[i].setState(false); 
		 *  
		 *  That is made because in SBC controllers gains can never be negatives (because gains represent number of vehicles)
		 *  Also if all gains are zeros no need to choose one at random (again, gains represent number of vehicles)
		 */		
		if(p_index == 0) { //SBC commented this if statement
			possibleSC[0] = signConfs[(int) Math.round(uniformRND*(num_sc-1))];
			p_index = 1;
		}
		
		//if (p_index > 0) { //SBC added this if statement
			
			int desSCId = (int) Math.round(uniformRND*(p_index-1));
			
			if(desSCId < 0) 
			{
				desSCId = 0;				
				System.out.println("Dear sir.\n It seems your absolutely fabulous TrafficLightController algorithm " +
									"caused quite a mess in this program. " +
									"Could you be so kind to clean it up?\n" +
									"Thank you very much, sincerely\n" +
									"The Green Light District Team");
			}

			Sign[] desiredSC = possibleSC[desSCId];

			if(CrossNodesSafely) 
			{				
				Sign[] overlapSC = calcOverlap(desiredSC, currentSC[node.getId()]);

				//EJUST comment: Close all signs (including the current configuration) for setting a new configuration
				int num_signs = signs.length;				
				for (int i=0; i < num_signs; i++)
					signs[i].setState(false);
				
				if(true || node.areOtherTailsFree(overlapSC)) 
				{
					currentSC[node.getId()] = desiredSC;
				}
				else 
				{
					currentSC[node.getId()] = overlapSC;
				}

				int num_overlap = overlapSC.length;				
				for(int i=0; i < num_overlap; i++) 
				{
					overlapSC[i].setState(true);
					
					//SBC
					overlapSC[i].setTimeStepSwitched(tlcontroller.getCurTimeStep());
					//System.out.println("Sign controller id: "+overlapSC[i].getId()+" TimeStep: "+ tlcontroller.getCurTimeStep());
				}
			}

			/*EJUST commented:
			 * The MorevtsOptimLongClear controller implements a LONG clearance time, i.e., not just the clearance if no cars in all lanes 
			 * Despite, any controller in the MoreVTS_1.1_Beta version can have only one/no sign be green at a time 
			 * that does not exist in the GLD latest versions due to the following part of code;
			 * i.e., comment it if we need to activate the CrossNodeSafely effect*/
			for(int i=0;i<num_dec;i++) 
			{
				dec[i].getTL().setState(false);
			}

			int num_destl = possibleSC[desSCId].length;
			
			for(int i=0; i<num_destl; i++) 
			{
				desiredSC[i].setState(true);
			}
		//}
	}

	protected Sign[] calcOverlap(Sign[] ar1, Sign[] ar2) 
	{
		Sign[] outp = null;
		if(ar1 != null && ar2 != null) 
		{
			int num_ar1 = ar1.length;
			int num_ar2 = ar2.length;
			outp = new Sign[(num_ar1<num_ar2)?num_ar1:num_ar2];
			int outp_index = 0;
			for(int i=0;i<num_ar1;i++) 
			{
				for(int j=0;j<num_ar2;j++) 
				{
					if(ar1[i].getId() == ar2[j].getId()) 
					{
						outp[outp_index] = ar1[i];
						outp_index++;
					}
				}
			}
			outp = (Sign[]) Arrayutils.cropArray(outp,outp_index);
		}
		else 
		{
			outp = new Sign[0];
		}
		return outp;
	}

	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{	
		currentSC=(Sign[][])XMLArray.loadArray(this,loader);
	}

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	
		XMLElement result=new XMLElement(shortXMLName);
		result.setName(shortXMLName);
		return result;
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ 	
		for ( int x=0; x < currentSC.length ; x++)
		{	
			for ( int y=0; y < currentSC[x].length ; y++ )
			{	
				currentSC[x][y].setParentName(getXMLName());
			}
		}
		XMLArray.saveArray(currentSC,this,saver,"current-sc");
	}

	public String getXMLName ()
	{ 	
		return "model."+shortXMLName;
	}

	public void setParentName (String parentName) throws XMLTreeException
	{	
		throw new XMLTreeException("Attempt to change fixed parentName of a SignController class.");
	}
}