
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
/** This class can be used to create instances of Traffic Light Controllers
  * for a specific infrastructure.
  */

import gld.GLDSim;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.utils.StringUtils;

import java.util.NoSuchElementException;
import java.util.Random;

public class TLCFactory
{
	protected Infrastructure infra;
	protected Random random;

	protected static final int
		RANDOM=0,
 		LONGEST_QUEUE=1,
		MOST_CARS=2,
		BEST_FIRST=3,
		RELATIVE_LONGEST_QUEUE=4,
		RLD=5,
		RLD2=6,
		TC_1OPT=7,
		TC_2OPT=8,
		TC_3OPT=9,
		TC1_B1=10,
		TC2_B1=11,
		TC3_B1=12,
		TC_1_DESTLESS=13,
		TC_2_DESTLESS=14,
		TC_3_WORKINPROGRESS=15,
		
		ACGJ_1=16,
		ACGJ_2=17,
		ACGJ_3=18,
		ACGJ_3_FV=19,
		ACGJ_4=20,
		ACGJ_5=21,
		LOCAL=22,
		GENNEURAL=23,
		TC_2FINAL=24,
		TC1_FIX=25,
		RLSARSA1=26,
		RLSARSA2=27,
		RLSARSA3=28,
		RLSARSA4=29,
		RLSARSA5=30,
		RLSARSA6=31,
		TC_CBG=32,  					//(DOAS 05)
        TC_ABG=33,  					//(DOAS 06)
        TC_ACBsG=34,    				//(DOAS 06)
		IATRACOS=35, 					//Gaston
		
		//SBC
		MOREVTS_MARCHING=36, 			
		MOREVTS_OPTIM=37,					
		MOREVTS_COUNTING_CARS=38,		
		MOREVTS_COUNTING_CARS2=39,		
		MOREVTS_SOTL_REQUEST=40,		
		MOREVTS_SOTL_PHASE=41,			
		MOREVTS_SOTL_PLATOON=42,		
		MOREVTS_OPTIM_WETSTRAAT=43,		
		MOREVTS_MARCHING_WETSTRAAT=44,	
		MOREVTS_SOTL_PLATOON2=45,		
		MOREVTS_OPTIM_LONGCLEAR=46,
		
		//EJUST
		TC_1OPT_BAYESIAN=47,
		TC_1OPT_MULTI_OBJECTIVE=48,
		TC_1OPT_MULTI_OBJECTIVE_BAYESIAN=49,
		TC_1OPT_ENHANCED_MULTI_OBJECTIVE_BAYESIAN=50,
		TC_1OPT_HYBRID_EXPLORATION_ENHANCED_MULTI_OBJECTIVE_BAYESIAN=51;


	protected static final String[] tlcDescs = {
		"Random",
		"Longest Queue",
		"Most Cars",
		"Best First",
		"Relative Longest Queue",
		"Red Light District",
		"Red Light District 2",
		"TC-1 Optimized 2.0 (unfixed)",
		"TC-2 Optimized 2.0 (unfixed)",
		"TC-3 Optimized 1.0 (unfixed)",
		"TC-1 Bucket 2.0",
		"TC-2 Bucket 1.0",
		"TC-3 Bucket 1.0",
		"TC-1 Destinationless",
		"TC-2 Destinationless",
		"TC-3 Work In Progress",
		
		"ACGJ-1",
		"ACGJ-2",
		"ACGJ-3",
		"ACGJ-3 Stupidified",
		"ACGJ-4 : Gain Factoring, 2xDNA",
		"ACGJ-4 : Gain Factoring, 1xDNA",
		"Local Hillclimbing",
	    "GenNeural",
    	"TC-2 Final Version",
    	"TC-1 Fixed version",
    	"RL Sarsa 1",
    	"RL Sarsa 2",
    	"RL Sarsa 3",
    	"RL Sarsa 4",
    	"RL Sarsa 5",
    	"RL Sarsa 6",
    	"TC-CBG (Congestion B-Gone)",   					//(DOAS 05)
        "TC-ABG (Accident B-Gone)",      					//(DOAS 06)
        "TC-ACBsG (Accident and Congestion Bs-Gone)",   	//(DOAS 06)
		"iAtracos",											//Gaston
		
		//SBC
		"Marching control",									
		"Optim control",									
		"Self organizing: counting cars",					
		"Self organizing: counting cars with minimal phase",
		"Sotl Request Control",								
		"Sotl Phase Control",								
		"Sotl Platoon Control",								
		"Optim control for Wetstraat",						
		"Marching control for Wetstraat",					
		"Sotl Platoon Controller 2",						
		"Optim control with long clearance time",
		
		//EJUST
		"TC-1 Optimized 2.0 Bayesian",
		"TC-1 Optimized 2.0 Multi-Objective",
		"TC-1 Optimized 2.0 Multi-Objective Bayesian",
		"TC-1 Optimized 2.0 Enhanced Multi-Objective Bayesian",
		"TC-1 Optimized 2.0 Hybrid Exploration Enhanced Multi-Objective Bayesian"
	};

	protected static final String[] xmlNames = {
		RandomTLC.shortXMLName,
		LongestQueueTLC.shortXMLName,
		MostCarsTLC.shortXMLName,
		BestFirstTLC.shortXMLName,
		RelativeLongestQueueTLC.shortXMLName,
		RLDTLC.shortXMLName,
		RLD2TLC.shortXMLName,
		TC1TLCOpt.shortXMLName,
		TC2TLCOpt.shortXMLName,
		TC3Opt.shortXMLName,
		TC1B1.shortXMLName,
		TC2B1.shortXMLName,
		TC3B1.shortXMLName,
		TC1TLCDestless.shortXMLName,
		TC2TLCDestless.shortXMLName,
		TC3TLCWorkInProgress.shortXMLName,
		ACGJ1.shortXMLName,
		ACGJ2.shortXMLName,
		ACGJ3.shortXMLName,
		ACGJ3FixedValue.shortXMLName,
		ACGJ4.shortXMLName,
		ACGJ5.shortXMLName,
		LocalHillTLC.shortXMLName,
    	GenNeuralTLC.shortXMLName,
    	TC2Final.shortXMLName,
    	TC1TLCFix.shortXMLName,
    	SL1TLC.shortXMLName,
    	SL2TLC.shortXMLName,
    	SL3TLC.shortXMLName,
    	SL4TLC.shortXMLName,
    	SL5TLC.shortXMLName,
    	SL6TLC.shortXMLName,
    	TCCBG.shortXMLName, 						//(DOAS 05)
        TCABG.shortXMLName,  						//(DOAS 06)
        TCACBsG.shortXMLName,    					//(DOAS 06)
		IATRACOSTLC.shortXMLName,					//Gaston
		
		//SBC
		MorevtsMarchingTLC.shortXMLName,			
		MorevtsOptimTLC.shortXMLName,				
		MorevtsCountingCars.shortXMLName,			
		MorevtsCountingCars2.shortXMLName,			
		MorevtsSotlRequest.shortXMLName,			
		MorevtsSotlPhase.shortXMLName,				
		MorevtsSotlPlatoon.shortXMLName,			
		MorevtsOptimWetstraatTLC.shortXMLName,		
		MorevtsMarchingWetstraatTLC.shortXMLName,	
		MorevtsSotlPlatoon2.shortXMLName,			
		MorevtsOptimLongClear.shortXMLName,
		
		//EJUST
		TC1TLCOptBayesian.shortXMLName,
		TC1TLCOptMultiObjective.shortXMLName,
		TC1TLCOptMultiObjectiveBayesian.shortXMLName,
		TC1TLCOptEnhancedMultiObjectiveBayesian.shortXMLName,
		TC1TLCOptHybridExplorationEnhancedMultiObjectiveBayesian.shortXMLName
	};


	protected static final String[] categoryDescs = 
		{			
			"Simple Maths", 
			"Complex Maths", 
			"Longest Q-variants", 
			"Reinforcement Learning", 
			"Multi-TLC", 
			"RL Sarsa TLCs", 
			"Genetic", 
			"Neural Network",
			"MoreVTS"				//SBC
		};
	
	protected static final int[][] categoryTLCs = {
		{RANDOM, MOST_CARS, RLD, RLD2, IATRACOS /*Gaston*/},
		{LOCAL, ACGJ_2},
		{LONGEST_QUEUE, RELATIVE_LONGEST_QUEUE, BEST_FIRST},
		{TC1_FIX, TC_1OPT, TC_2OPT, TC_3OPT, TC1_B1, TC2_B1, TC3_B1, TC_1_DESTLESS, TC_2_DESTLESS, TC_3_WORKINPROGRESS,	TC_2FINAL, 
			TC_1OPT_BAYESIAN /*EJUST*/, TC_1OPT_MULTI_OBJECTIVE /*EJUST*/, 
			TC_1OPT_MULTI_OBJECTIVE_BAYESIAN /*EJUST*/, 
			TC_1OPT_ENHANCED_MULTI_OBJECTIVE_BAYESIAN /*EJUST*/,
			TC_1OPT_HYBRID_EXPLORATION_ENHANCED_MULTI_OBJECTIVE_BAYESIAN /*EJUST*/},
//		{TC1_FIX, TC_1OPT, TC_2OPT, TC_3OPT},
                {TC_CBG /*(DOAS 05)*/, TC_ABG/*(DOAS 06)*/, TC_ACBsG/*(DOAS 06)*/},
		{RLSARSA1,RLSARSA2,RLSARSA3,RLSARSA4,RLSARSA5,RLSARSA6},
		{ACGJ_1, ACGJ_3, ACGJ_3_FV, ACGJ_4, ACGJ_5},
		{GENNEURAL},
		/*SBC*/
		{MOREVTS_MARCHING, MOREVTS_OPTIM, MOREVTS_COUNTING_CARS, MOREVTS_COUNTING_CARS2, MOREVTS_SOTL_REQUEST, MOREVTS_SOTL_PHASE, MOREVTS_SOTL_PLATOON, 
			MOREVTS_OPTIM_WETSTRAAT, MOREVTS_MARCHING_WETSTRAAT, MOREVTS_SOTL_PLATOON2, MOREVTS_OPTIM_LONGCLEAR}
	};

	/** Makes a new TLCFactory for a specific infrastructure with a new
	  * random number generator.
	  * @param infra The infrastructure
	 */
  	public TLCFactory(Infrastructure infra)
	{ 	
  		this.infra=infra;
		random=new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
	}

	/** Makes a new TLCFactory for a specific infrastructure
	  * @param random The random number generator which some algorithms use
	  * @param infra The infrastructure
	 */
  	public TLCFactory(Infrastructure infra, Random random)
  	{ 	this.infra=infra;
		this.random=random;
  	}

	/** Looks up the id of a TLC algorithm by its description
	  * @param algoDesc The description of the algorithm
	  * @returns The id of the algorithm
	  * @throws NoSuchElementException If there is no algorithm with that
	  *        description.
	 */
	public static int getId (String algoDesc)
	{ 	return StringUtils.getIndexObject(tlcDescs,algoDesc);
	}

	/** Returns an array of TLC descriptions */
	public static String[] getTLCDescriptions() { return tlcDescs; }

  	/** Look up the description of a TLC algorithm by its id
	  * @param algoId The id of the algorithm
	  * @returns The description
	  * @throws NoSuchElementException If there is no algorithm with the
	  *	    specified id.
	*/
  	public static String getDescription (int algoId)
  	{ 	return (String)(StringUtils.lookUpNumber(tlcDescs,algoId));
  	}

  	/** Returns an array containing the TLC category descriptions. */
  	public static String[] getCategoryDescs()
	{ 	return categoryDescs;
	}

  	/** Returns an array of TLC numbers for each TLC category. */
  	public static int[][] getCategoryTLCs()
	{ 	return categoryTLCs;
	}

  	/** Returns the total number of TLCs currently available. */
 	public static int getNumberOfTLCs()
	{ 	return tlcDescs.length;
	}

  	/** Gets the number of an algorithm from its XML tag name */
  	public static int getNumberByXMLTagName(String tagName)
  	{ 	return StringUtils.getIndexObject(xmlNames,tagName);
  	}

  	/** Returns an instance of a TLC by its description. */
  	public TLController genTLC (String tlcDesc) throws InfraException
	{	return getInstanceForLoad(getId(tlcDesc));
	}

	public TLController genTLC(int cat, int tlc) throws InfraException
	{
		return getInstanceForLoad(categoryTLCs[cat][tlc]);
	}

  	/** Gets a new instance of an algorithm by its number. This method
    	  * is meant to be used for loading.
   	*/
	public TLController getInstanceForLoad (int algoId) throws InfraException
	{
		switch (algoId) {
			case RANDOM : return (random != null ? new RandomTLC(infra, random): new RandomTLC(infra));
			case LONGEST_QUEUE : return new LongestQueueTLC(infra);
			case MOST_CARS : return new MostCarsTLC(infra);
			case BEST_FIRST : return new BestFirstTLC(infra);
			case RELATIVE_LONGEST_QUEUE : return new RelativeLongestQueueTLC(infra);
			case RLD : return new RLDTLC(infra);
			case RLD2: return new RLD2TLC(infra);
			case TC_1OPT : return new TC1TLCOpt(infra);
			case TC_2OPT : return new TC2TLCOpt(infra);
			case TC_3OPT: return new TC3Opt(infra);
			case TC1_B1: return new TC1B1(infra);
			case TC2_B1: return new TC2B1(infra);
			case TC3_B1: return new TC3B1(infra);
			case TC_1_DESTLESS: return new TC1TLCDestless(infra);
			case TC_2_DESTLESS: return new TC2TLCDestless(infra);
			case TC_3_WORKINPROGRESS: return new TC3TLCWorkInProgress(infra);
			case ACGJ_1 : return new ACGJ1(infra);
			case ACGJ_2 : return new ACGJ2(infra);
			case ACGJ_3 : return new ACGJ3(infra);
			case ACGJ_3_FV : return new ACGJ3FixedValue(infra);
			case ACGJ_4 : return new ACGJ4(infra);
			case ACGJ_5 : return new ACGJ4(infra);
			case LOCAL : return new LocalHillTLC(infra);
			case GENNEURAL : return new GenNeuralTLC(infra);
			case TC_2FINAL: return new TC2Final(infra);
			case TC1_FIX: return new TC1TLCFix(infra);
			case RLSARSA1: return new SL1TLC(infra);
			case RLSARSA2: return new SL2TLC(infra);
			case RLSARSA3: return new SL3TLC(infra);
			case RLSARSA4: return new SL4TLC(infra);
			case RLSARSA5: return new SL5TLC(infra);
			case RLSARSA6: return new SL6TLC(infra);
			case TC_CBG: return new TCCBG(infra);   	//(DOAS 05)
            case TC_ABG: return new TCABG(infra);  		//(DOAS 06)
            case TC_ACBsG: return new TCACBsG(infra);   //(DOAS 06)
			
            //Gaston
            case IATRACOS: return new IATRACOSTLC(infra); //throw new InfraException("This controller is not supported:"+algoId);
			
            //SBC
            case MOREVTS_MARCHING: return new MorevtsMarchingTLC(infra);
			case MOREVTS_OPTIM: return new MorevtsOptimTLC(infra);
			case MOREVTS_COUNTING_CARS: return new MorevtsCountingCars(infra);
			case MOREVTS_COUNTING_CARS2: return new MorevtsCountingCars2(infra);
			case MOREVTS_SOTL_REQUEST: return new MorevtsSotlRequest(infra);
			case MOREVTS_SOTL_PHASE: return new MorevtsSotlPhase(infra);
			case MOREVTS_SOTL_PLATOON: return new MorevtsSotlPlatoon(infra);
			case MOREVTS_OPTIM_WETSTRAAT: return new MorevtsOptimWetstraatTLC(infra);
			case MOREVTS_MARCHING_WETSTRAAT: return new MorevtsMarchingWetstraatTLC(infra);
			case MOREVTS_SOTL_PLATOON2: return new MorevtsSotlPlatoon2(infra);
			case MOREVTS_OPTIM_LONGCLEAR: return new MorevtsOptimLongClear(infra);
			
			//EJUST
			case TC_1OPT_BAYESIAN: return new TC1TLCOptBayesian(infra);
			case TC_1OPT_MULTI_OBJECTIVE: return new TC1TLCOptMultiObjective(infra);
			case TC_1OPT_MULTI_OBJECTIVE_BAYESIAN: return new TC1TLCOptMultiObjectiveBayesian(infra);
			case TC_1OPT_ENHANCED_MULTI_OBJECTIVE_BAYESIAN: return new TC1TLCOptEnhancedMultiObjectiveBayesian(infra);
			case TC_1OPT_HYBRID_EXPLORATION_ENHANCED_MULTI_OBJECTIVE_BAYESIAN: return new TC1TLCOptHybridExplorationEnhancedMultiObjectiveBayesian(infra);
		}
	   	throw new InfraException("The TLCFactory can't make TLC's of type "+algoId);
	}
}
