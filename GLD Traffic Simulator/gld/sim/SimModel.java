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

package gld.sim;

import gld.GLDSim;
import gld.Model;
import gld.algo.dp.DPFactory;
import gld.algo.dp.DrivingPolicy;
import gld.algo.edit.ShortestPathCalculator;
import gld.algo.tlc.PosMov;
import gld.algo.tlc.SignController;
import gld.algo.tlc.TCColearnPerformanceIndex;
import gld.algo.tlc.TCRL;
import gld.algo.tlc.TLCFactory;
import gld.algo.tlc.TLController;
import gld.config.POOptionsFactory;
import gld.distributions.DistributionException;
import gld.distributions.DistributionFactory;
import gld.distributions.ParameterValue;
import gld.distributions.RandomGaussian;
import gld.distributions.RandomUniform;
import gld.idm.Constants;
import gld.idm.IDMBicycleDry;
import gld.idm.IDMBicycleHeavyFog;
import gld.idm.IDMBicycleHeavyRain;
import gld.idm.IDMBicycleLightFog;
import gld.idm.IDMBicycleLightRain;
import gld.idm.IDMBicycleNormalRain;
import gld.idm.IDMBicycleSandstorm;
import gld.idm.IDMBusDry;
import gld.idm.IDMBusHeavyFog;
import gld.idm.IDMBusHeavyRain;
import gld.idm.IDMBusLightFog;
import gld.idm.IDMBusLightRain;
import gld.idm.IDMBusNormalRain;
import gld.idm.IDMBusSandstorm;
import gld.idm.IDMCarDry;
import gld.idm.IDMCarHeavyFog;
import gld.idm.IDMCarHeavyRain;
import gld.idm.IDMCarLightFog;
import gld.idm.IDMCarLightRain;
import gld.idm.IDMCarNormalRain;
import gld.idm.IDMCarSandstorm;
import gld.idm.IDMGLD;
import gld.idm.LaneChangeGLD;
import gld.idm.MicroModelGLD;
import gld.idm.WeatherFactory;
import gld.infra.Beliefstate;
import gld.infra.DestFrequency;
import gld.infra.Drivelane;
import gld.infra.DrivelaneFactory;
import gld.infra.EdgeNode;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Junction;
import gld.infra.Node;
import gld.infra.ObservedRoaduser;
import gld.infra.PODrivelanes;
import gld.infra.Road;
import gld.infra.Roaduser;
import gld.infra.RoaduserFactory;
import gld.infra.Sign;
import gld.infra.SpawnFrequency;
import gld.infra.SpecialNode;
import gld.sim.stats.StatisticsController;
import gld.sim.stats.TrackerFactory;
import gld.utils.Arrayutils;
import gld.utils.NumberDispenser;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * The heart of the simulation.
 *
 * @author Group Model
 * @version 1.0
 */

public class SimModel extends Model implements XMLSerializable, Constants /*EJUST*/
{
	private static final String statsTimeStepsKey = "gld.sim.stats.sender.timeSteps";
	/** The pseudo-random-number-generator we need in this simulation */
	protected Random generator;
	/** The second thread that runs the actual simulation */
	protected SimModelThread thread;
	/** The SimController */
	protected SimController controller;
	/** The current timeStep we're in */
	protected int curTimeStep;
	/* number of roadusers waiting in queues at all edges */
	protected int numWaiting = 0;
	/** The Driving Policy in this Simulation */
	protected static DrivingPolicy dp;

	/** The TrafficLightControlling Algorithm */
	protected TLController tlc;
	/** The Thing that makes all Trafficlights shudder */
	protected SignController sgnctrl;
	/** Name of the simulation */
	protected String simName = "untitled";
	/** A boolean to keep track if this sim has already run (jvm initialization) */
	protected boolean hasRun = false;
	/** Indicates if roadusers cross nodes or jump over them. */
	public static boolean CrossNodes = true;
	/** Indicates whether we are running a series of simulations */
	protected boolean runSeries = false;
	protected boolean locked = false;
	/** The number of steps each of the simulations in a series should make */
	protected static int numSeriesSteps = 50000; //Original Value: 50000
	
	protected static int LOCK_THRESHOLD = 15000; /*POMDPGLD change this value from 10000 to 15000; */
	
	
	/** EJUST: Position update time step*/
	protected static final double dt = TIMESTEP_S;
	
	protected static int numSeries = 10; // DOAS 06: 10 series per test config
	protected int curSeries = 0;


	/**GASTON: Indicates whether the StatisticsSender thread must run or not */
	boolean active = false;
	protected StatisticsSender statsSenderThread = null;
	protected LinkedList<HashMap<String, String[]>> spawnFreqsList = null;
	protected Iterator<HashMap<String, String[]>> iFreqs = null;
	protected int nroFranja = 0;

	protected String file_franjas_key = "gld.sim.timeInterval.source"; //e.g. resources/franjas.xml
	protected ResourceBundle rb = null;


	/*POMDPGLD*/
	public static float globalSpawnrate = -1;
	public static int globalDistributionType; /*EJUST*/
	public static ParameterValue[] globalParamValue; /*EJUST*/

	public static int globalWeatherCondition; /*EJUST*/
	
	//calibration variables
	// aantal halveringen van de calibratiemethode
	// Translation: number of half-rings of the calibration method
	protected final int maxCalibrationDepth = 4;

	// aantal calibratie time steps per run
	// Translation: Calibration of time steps per run
	protected static int numCalSteps = 5000;
	public int addedCalSteps = 0;

	// aantal calibratie runs per stap
	// Translation: number of calibration runs per step
	protected static int numCalSeries = 5;

	// interne calibratie variabelen
	// Translation: Internal calibration variables
	protected boolean runCalibrationSeries = false;
	protected boolean calSerieSuccesful = true;
	protected boolean stepSizeHalved = false;
	protected float currentStepSize;
	protected float currentSpawnrate;
	protected int currentDistributionType; /*EJUST*/
	protected ParameterValue[] currentParamValue; /*EJUST*/
	protected int currentWeatherCondition; /*EJUST*/
	
	protected int currentCalDepth = 0;
	protected int maxWaitingAllowed = 0; /*EJUST commented: 15000*/
	protected float lastSuccesfulSpawnrate = 0;

	protected String dataFileName = "";
	protected Random error_generator;
	protected static double chanceOnErrRU = 0;
	public static double sensor_noise = 0.5;
	public static boolean use_sensor_noise = false;

	// config variables ... need to get a menu option
	public static int noise_method = POOptionsFactory.noise_methods[POOptionsFactory.defaultN];
	public static int beliefstate_method = POOptionsFactory.beliefstate_types[POOptionsFactory.defaultB];
	public static int car_driving_behaviour = POOptionsFactory.car_driving_behaviours[POOptionsFactory.defaultC];
	public static boolean usePO = false; //(tlc instanceof POTLC) && SimModel.dltype == DrivelaneFactory.PO_DRIVELANE;
	public static boolean useAllRoadusers = false; /*EJUST: true --> false*/

	public static boolean use_SCOMDP = false;
	public static int SCOMDP_version = 1;
	public static boolean use_fixed_speed_randomizer = false;

	// statistics
	public int waitingsizeFailures = 0;
	public static int tempBeliefStateSize = 0;
	public static int currentBeliefStateSize = 0;
	public static int averageBeliefStateSize = 0;
	public static int maxBeliefStateSize = 0;

	public static double tempBeliefStateProb = 0;
	public static double currentBeliefStateProb = 0;
	public static double averageBeliefStateProb = 0;
	public static double minBeliefStateProb = 0;
	public static int numBeliefstates = 0;

	public double averageLaneLoad = 0;
	public double averageLaneNumRoadusersWaiting = 0; //EJUST
	public double maxLaneLoad = 0;
	public int maxLaneNumRoadusersWaiting = 0; //EJUST

	private double laneLoad = 0;
	private int laneNumRoadusersWaiting = 0; //EJUST
	private int lanesProcessed = 0;
	private double tempMaxLaneLoad = 0;
	private int tempMaxLaneNumRoadusersWaiting = 0; //EJUST
	private double maxAverageLaneLoad = 0;
	private double maxAverageLaneNumRoadusersWaiting = 0; //EJUST

	public int lastMaxMeasure = 0;
	public int lastIncrease = 0;
	public int currentIncrease = 0;

	public static int state_zeros = 0;
	public static int Max_state_zeros = 0;
	/*POMDPGLD*/

	/*EJUST*/
	protected MicroModelGLD idmCar =new IDMCarDry();
	protected MicroModelGLD idmBus=new IDMBusDry();
	protected MicroModelGLD idmBicycle=new IDMBicycleDry();

	/*EJUST: Light Rain Weather Conditions*/
	protected MicroModelGLD idmCarLightRain =new IDMCarLightRain();
	protected MicroModelGLD idmBusLightRain=new IDMBusLightRain();
	protected MicroModelGLD idmBicycleLightRain=new IDMBicycleLightRain();
	
	/*EJUST: Normal Rain Weather Conditions*/
	protected MicroModelGLD idmCarNormalRain =new IDMCarNormalRain();
	protected MicroModelGLD idmBusNormalRain=new IDMBusNormalRain();
	protected MicroModelGLD idmBicycleNormalRain=new IDMBicycleNormalRain();

	/*EJUST: Heavy Rain Weather Conditions*/
	protected MicroModelGLD idmCarHeavyRain =new IDMCarHeavyRain();
	protected MicroModelGLD idmBusHeavyRain=new IDMBusHeavyRain();
	protected MicroModelGLD idmBicycleHeavyRain=new IDMBicycleHeavyRain();
	
	/*EJUST: Light Fog Weather Conditions*/
	protected MicroModelGLD idmCarLightFog =new IDMCarLightFog();
	protected MicroModelGLD idmBusLightFog=new IDMBusLightFog();
	protected MicroModelGLD idmBicycleLightFog=new IDMBicycleLightFog();

	/*EJUST: Heavy Fog Weather Conditions*/
	protected MicroModelGLD idmCarHeavyFog =new IDMCarHeavyFog();
	protected MicroModelGLD idmBusHeavyFog=new IDMBusHeavyFog();
	protected MicroModelGLD idmBicycleHeavyFog=new IDMBicycleHeavyFog();

	/*EJUST: Sandstorm Weather Conditions*/
	protected MicroModelGLD idmCarSandstorm =new IDMCarSandstorm();
	protected MicroModelGLD idmBusSandstorm=new IDMBusSandstorm();
	protected MicroModelGLD idmBicycleSandstorm=new IDMBicycleSandstorm();


	//!!! bicycle (=> vw for better impl!)
	protected LaneChangeGLD cute=new LaneChangeGLD(P_FACTOR_BICYCLE, DB_BICYCLE, MAIN_SMIN_BICYCLE, MAIN_BSAVE_BICYCLE, BIAS_RIGHT_BICYCLE);

	//!!! bus (=> vw for better impl!)
	protected LaneChangeGLD polite=new LaneChangeGLD(P_FACTOR_TRUCK, DB_TRUCK, MAIN_SMIN_TRUCK, MAIN_BSAVE_TRUCK, BIAS_RIGHT_TRUCK);

	//!!! car (=> vw for better impl!)
	protected LaneChangeGLD inconsiderate=new LaneChangeGLD(P_FACTOR_CAR, DB_CAR, MAIN_SMIN, MAIN_BSAVE, BIAS_RIGHT_CAR);
	/*EJUST*/

	/**
	 * Creates second thread
	 */
	public SimModel()
	{
		thread = new SimModelThread();
		thread.start();

		curTimeStep = 0;
		
		GLDSim.seriesSeed[GLDSim.seriesSeedIndex] = System.currentTimeMillis(); /*EJUST*/
		
		generator = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);

		/*POMDPGLD*/
		error_generator = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
		GLDSim.RND = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);

		sgnctrl = new SignController(tlc, infra);

		//GASTON: getting resource bundle
		rb = ResourceBundle.getBundle("simulation");

		//GASTON: creating thread to send statistics
		statsSenderThread = new StatisticsSender();
		statsSenderThread.setModel(this);
		statsSenderThread.start();
	}

	//(DOAS 06)
	public SimController getSimController(){
		return controller;
	}

	public void setSimController(SimController sc)
	{
		controller = sc;
	}

	public void setInfrastructure(Infrastructure i)
	{
		pause();

		super.setInfrastructure(i);
		if (tlc != null)
		{
			tlc.setInfrastructure(i);
		}
		if (sgnctrl != null)
		{
			sgnctrl.setInfrastructure(i);
		}
	}

	/*POMDPGLD*/
	public static boolean ruDetected() {
		if(SimModel.use_sensor_noise && gld.GLDSim.RND.nextDouble() < SimModel.sensor_noise) {
			return false;
		}
		return true;
	}
	
	/*POMDPGLD*/
	public void setDataFileName(String filename)
	{
		dataFileName = filename;
	}
	

	/** Returns the current timeStep */
	public int getCurTimeStep()
	{
		return curTimeStep;
	}

	/** Sets the current timeStep */
	public void setCurTimeStep(int c)
	{
		curTimeStep = c;
		tlc.setCurTimeStep(c);  //SBC
	}

	/** Returns the current timeStep */
	public int getCurNumWaiting()
	{
		return numWaiting;
	}

	/** Sets the current timeStep */
	public void setCurNumWaiting(int c)
	{
		numWaiting = c;
	}


	/** Returns the current Driving Policy */
	public static DrivingPolicy getDrivingPolicy()
	{
		return dp;
	}

	/** Sets the current DrivTLController */
	public void setDrivingPolicy(DrivingPolicy _dp)
	{
		dp = _dp;
	}

	/** Returns the current TLController */
	public TLController getTLController()
	{
		return tlc;
	}

	/** Sets the current TLController */
	public void setTLController(TLController _tlc)
	{
		tlc = _tlc;
		sgnctrl.setTLC(tlc);
	}

	/** Returns the random number generator */
	public Random getRandom()
	{
		return generator;
	}

	/** Sets the random number generator */
	public void setRandom(Random r)
	{
		generator = r;
	}

	/** Returns the name of the simulation */
	public String getSimName()
	{
		return simName;
	}

	/** Sets the name of the simulation */
	public void setSimName(String s)
	{
		simName = s;
	}

	/** Returns the pseudo-random-number generator of this Model */
	public Random getRNGen()
	{
		return generator;
	}

	/** Sets spawn frequency for given node and ru type. */
	public void setSpawnFrequency(EdgeNode en, int rutype, float newspawn, 
			int distributionType /*EJUST*/, ParameterValue[] paramValue /*EJUST*/,
			int weatherCondition /*EJUST*/)
	{
		en.setSpawnFrequency(rutype, newspawn, distributionType /*EJUST*/, paramValue /*EJUST*/, weatherCondition /*EJUST*/);
		setChanged();
		notifyObservers();
	}

	
	/** POMDPGLD: update counters for the average and max lane Loads, 
	 * EJUST:  update counters for the average and max lane number of roadusers waiting */

	public void updateLaneStatistics()
	{
		averageLaneLoad = laneLoad / (double)lanesProcessed;
		averageLaneNumRoadusersWaiting = laneNumRoadusersWaiting / (double)lanesProcessed; //EJUST
		
		maxAverageLaneLoad = (averageLaneLoad > maxAverageLaneLoad) ? averageLaneLoad : maxAverageLaneLoad;
		maxAverageLaneNumRoadusersWaiting = (averageLaneNumRoadusersWaiting > maxAverageLaneNumRoadusersWaiting) ? 
											averageLaneNumRoadusersWaiting : maxAverageLaneNumRoadusersWaiting; //EJUST
		
		maxLaneLoad = tempMaxLaneLoad;
		maxLaneNumRoadusersWaiting = tempMaxLaneNumRoadusersWaiting; //EJUST
		
		laneLoad = 0;
		laneNumRoadusersWaiting = 0; //EJUST
		
		tempMaxLaneLoad = 0;
		tempMaxLaneNumRoadusersWaiting = 0; //EJUST
		
		lanesProcessed = 0;
	}

	/** POMDPGLD: resets counters for the average and max lane Loads,
	 * EJUST: resets counters for the average and max lane number of roadusers waiting */

	public void resetLaneStatistics()
	{
		averageLaneLoad = 0;
		averageLaneNumRoadusersWaiting = 0; //EJUST
		
		maxAverageLaneLoad = 0;
		maxAverageLaneNumRoadusersWaiting = 0; //EJUST
		
		tempMaxLaneLoad = 0;
		tempMaxLaneNumRoadusersWaiting = 0; //EJUST
		
		maxLaneLoad = 0;
		maxLaneNumRoadusersWaiting = 0; //EJUST
		
		laneLoad = 0;
		laneNumRoadusersWaiting = 0; //EJUST
		
		lanesProcessed = 0;
	}

	/** POMDPGLD*/
	public void resetIncreaseStatistics()
	{
		lastMaxMeasure = 0;
		lastIncrease = 0;
		lastIncrease = 0;
	}
	
	/** EJUST*/
	public void resetProbabilisticSpawningFrequency()
	{
		SpecialNode[] specialNodes = infra.getSpecialNodes();
		EdgeNode edge;
		int num_edges = specialNodes.length;
		for (int i = 0; i < num_edges; i++)
		{			
			if (!(specialNodes[i] instanceof EdgeNode))
			{
				break;
			}
			else
			{
				edge = (EdgeNode) (specialNodes[i]);
			}
			SpawnFrequency[] freqs = edge.getSpawnFrequencies();
			int num_freqs = freqs.length;
			for (int j = 0; j < num_freqs; j++)
			{
				//The timeStep of the last spawned roaduser of type ruType
				freqs[j].timeStepOfLastArrival = -1;  

				// The generated random number after the last spawned roaduser of type ruType
				freqs[j].lastGeneratedRandomNumber = 1;
			}
		}
	}

	/** POMDPGLD*/
	public void updateIncreaseStatistics()
	{
		if(runCalibrationSeries)
		{
			if((getCurTimeStep() % 50) == 0)
			{
				SpecialNode[] specialNodes = getInfrastructure().getSpecialNodes();
				int sample = 0;

				for(int i = 0; i < specialNodes.length; i++)
				{
					sample += specialNodes[i].getWaitingQueueLength();
				}

				int currentIncrease = sample - lastMaxMeasure;
				lastMaxMeasure = (sample > lastMaxMeasure) ? sample : lastMaxMeasure;
				lastIncrease = currentIncrease;
				if(lastIncrease > 0)
				{
					addedCalSteps += 500;
					System.out.println("Added 500 steps");
				}
			}
		}
	}

	/** POMDPGLD: update counters for the Beliefstate **/
	public void updateBeliefstateCounters()
	{
		if(dltype == DrivelaneFactory.PO_DRIVELANE)
		{
			currentBeliefStateSize = tempBeliefStateSize;
			if(currentBeliefStateSize > maxBeliefStateSize)
			{
				maxBeliefStateSize = currentBeliefStateSize;
			}
			averageBeliefStateSize = ((averageBeliefStateSize * (curTimeStep - 1) + currentBeliefStateSize) / curTimeStep);
			tempBeliefStateSize = 0;
		}
	}

	/** POMDPGLD: update counters for the Beliefstate Probabilities **/
	public void updateBeliefstateProbCounters()
	{
		if(dltype == DrivelaneFactory.PO_DRIVELANE)
		{
			currentBeliefStateProb = tempBeliefStateProb;
			if(currentBeliefStateProb < minBeliefStateProb)
			{
				minBeliefStateProb = currentBeliefStateProb;
			}
			averageBeliefStateProb = ((averageBeliefStateProb * (curTimeStep - 1) + currentBeliefStateProb) / curTimeStep);
			tempBeliefStateProb = 0;
			numBeliefstates = 0;
		}
	}

	/** POMDPGLD: reset counters for the Beliefstate **/
	public void resetBeliefstateProbCounters()
	{
		if(dltype == DrivelaneFactory.PO_DRIVELANE)
		{
			currentBeliefStateProb = 0;
			averageBeliefStateProb = 0;
			tempBeliefStateProb = 0;
			minBeliefStateProb = 0;
			numBeliefstates = 0;
		}
	}

	/** POMDPGLD: reset counters for the Beliefstate **/
	public void resetBeliefstateCounters()
	{
		if(dltype == DrivelaneFactory.PO_DRIVELANE)
		{
			currentBeliefStateSize = 0;
			averageBeliefStateSize = 0;
			tempBeliefStateSize = 0;
			maxBeliefStateSize = 0;
		}
	}

	/**
	 * Stops the simulation.
	 * This should only be called when the program exits.
	 * To start a new simulation, the simulation should be paused
	 * with a call to pause(), then followed by a call to reset(),
	 * and finally resumed with unpause().
	 */
	public void stop()
	{
		thread.die();
	}

	/**
	 * Pauses the simulation
	 */
	public void pause()
	{
		thread.pause();
	}

	/**
	 * Unpauses the simulation
	 */
	public void unpause()
	{
		thread.unpause();
	}

	public boolean isRunning()
	{
		return thread.isRunning();
	}

	public void runSeries()
	{
		curSeries = 0;
		runSeries = true;
		nextSeries();
	}

	/*POMDPGLD*/
	public void runCalibration()
	{
		currentStepSize = (float)0.1;
		currentSpawnrate = (float)0.1;
		currentCalDepth = 0;
		if(globalSpawnrate != -1)
		{
			currentSpawnrate = globalSpawnrate;
		}
		else /*EJUST*/
		{
			currentDistributionType = globalDistributionType;
			currentParamValue = globalParamValue;
		}
		currentWeatherCondition = globalWeatherCondition; /*EJUST*/
		controller.setGlobalSpawnrate(curTimeStep/*EJUST*/, 
				currentSpawnrate, currentDistributionType /*EJUST*/, currentParamValue /*EJUST*/,
				currentWeatherCondition /*EJUST*/);

		curSeries = 0;
		runCalibrationSeries = true;

		calSerieSuccesful = true;
		System.out.print ("Starting calibration run, with current stepsize: " + currentStepSize + " and spawnrate: ");
		if(currentSpawnrate != -1) /*EJUST*/
			System.out.println(currentSpawnrate);
		else{ /*EJUST*/
			String paramValues = "";
			for (int i=0; i<currentParamValue.length; i++)
				paramValues += "," + DistributionFactory.getParameterDescription(currentParamValue[i].parameterIndex) + "=" + currentParamValue[i].value;
			System.out.println(DistributionFactory.getDistributionTypeDescription(currentDistributionType) + paramValues);
		}
		nextCalibration();
	}

	/*POMDPGLD*/
	public void updateCalStep()
	{
		if(stepSizeHalved == true && calSerieSuccesful)
		{
			currentCalDepth++;
			if(currentCalDepth > maxCalibrationDepth)
			{
				// breaking out of the calibration recursion, ending the calibration
				System.out.println("Succesful run, maximum Calibration depth reached, ending Calibration...");
				controller.endCalibration(curTimeStep/*EJUST*/, currentSpawnrate, 
						currentDistributionType /*EJUST*/, currentParamValue /*EJUST*/,
						currentWeatherCondition /*EJUST*/);
			}
			else
			{
				currentStepSize *= 0.5;
				lastSuccesfulSpawnrate = currentSpawnrate;
				// increase the spawn rate and do it again
				currentSpawnrate += currentStepSize;

				// NEXT run!
				curSeries = 0;
				runCalibrationSeries = true;

				calSerieSuccesful = true;
				controller.setGlobalSpawnrate(curTimeStep/*EJUST*/, currentSpawnrate, 
						currentDistributionType /*EJUST*/, currentParamValue /*EJUST*/,
						currentWeatherCondition /*EJUST*/);
				System.out.println("Succesful run, decreasing step size to " + currentStepSize + ", trying spawnrate " + currentSpawnrate);
				nextCalibration();
			}
		}
		else if(calSerieSuccesful)
		{
			lastSuccesfulSpawnrate = currentSpawnrate;
			// ran series with no roadusers waiting, increase spawnrate by stepsize
			currentSpawnrate += currentStepSize;

			//NEXT run!
			curSeries = 0;
			runCalibrationSeries = true;

			calSerieSuccesful = true;
			controller.setGlobalSpawnrate(curTimeStep/*EJUST*/, currentSpawnrate, 
					currentDistributionType /*EJUST*/, currentParamValue /*EJUST*/,
					currentWeatherCondition /*EJUST*/);
			System.out.println("Successful run, increasing spawnrate with current stepsize " +	currentStepSize + " to " + currentSpawnrate);
			nextCalibration();
		}
		else
		{
			// did we dig enough?
			currentCalDepth++;
			if(currentCalDepth > maxCalibrationDepth)
			{
				// breaking out of the calibration recursion, ending the calibration
				System.out.println("Failed run, maximum Calibration depth reached, ending Calibration...");
				controller.endCalibration(curTimeStep/*EJUST*/, lastSuccesfulSpawnrate, 
						currentDistributionType /*EJUST*/, currentParamValue /*EJUST*/,
						currentWeatherCondition /*EJUST*/);
			}
			else
			{
				// guess not: backtrack.
				currentSpawnrate -= currentStepSize;

				// last series were too much, decrease step size by half
				currentStepSize *= 0.5;
				stepSizeHalved = true;

				// increase the spawnrate and do it again
				currentSpawnrate += currentStepSize;

				// NEXT run!
				curSeries = 0;
				runCalibrationSeries = true;

				calSerieSuccesful = true;
				controller.setGlobalSpawnrate(curTimeStep/*EJUST*/, currentSpawnrate, 
						currentDistributionType /*EJUST*/, currentParamValue /*EJUST*/,
						currentWeatherCondition /*EJUST*/);
				System.out.println("Failed run, decreasing step size to " + currentStepSize + ", trying spawnrate " + currentSpawnrate);
				nextCalibration();

			}
		}
	}

	/*POMDPGLD*/
	public void nextCalibration()
	{
		resetBeliefstateCounters();
		resetBeliefstateProbCounters();
		resetLaneStatistics();
		resetIncreaseStatistics();
		
		/*EJUST*/
		resetProbabilisticSpawningFrequency();
		
		controller.nextCalibration();
	}

	public void nextSeries()
	{
		/*POMDPGLD*/
		resetBeliefstateCounters();
		resetBeliefstateProbCounters();
		resetLaneStatistics();
		resetIncreaseStatistics();
		
		/*EJUST*/
		resetProbabilisticSpawningFrequency();
		
		controller.nextSeries();
	}

	public void lockedSeries()
	{
		pause();
		for (; curTimeStep < numSeriesSteps; curTimeStep++)
		{
			setChanged();
			notifyObservers();

			/*POMDPGLD*/
			printProgressBar(48, numSeriesSteps, curTimeStep);
		}
		locked = false;
		nextSeries();
	}

	public void stopSeries()
	{
		/*POMDPGLD*/
		waitingsizeFailures = 0;

		curSeries = 0;
		GLDSim.seriesSeedIndex = 0;
		runSeries = false;
	}

	/*POMDPGLD*/
	public void stopCalSeries()
	{
		curSeries = 0;
		GLDSim.seriesSeedIndex = 0;
		runCalibrationSeries = false;
		updateCalStep();
	}

	public void nextCurSeries()
	{
		curSeries++;
		GLDSim.seriesSeedIndex = curSeries;
		
		/*EJUST*/		
		GLDSim.seriesSeed[GLDSim.seriesSeedIndex] = System.currentTimeMillis();
	}

	public int getCurSeries()
	{
		return curSeries;
	}

	public boolean isRunSeries()
	{
		return runSeries;
	}

	public int getNumSeries()
	{
		return numSeries;
	}

	//(DOAS 06)
	public void setNumSeries(int value){
		numSeries = value;
	}

	//(DOAS 06)
	public void setSeriesSteps(int value){
		numSeriesSteps = value;
	}

	/*POMDPGLD*/
	//(RM 06)
	public void setCalNumSeries(int value)
	{
		numCalSeries = value;
	}

	//(RM 06)
	public void setCalSeriesSteps(int value)
	{
		numCalSteps = value;
	}

	//(RM 06)
	public void setCalMaxWaitingRu(int value)
	{
		maxWaitingAllowed = value;
	}

	public int getMaxCalSteps()
	{
		if(maxAverageLaneLoad > 0.4)
		{
			return numCalSteps * 4;
		}
		if(maxAverageLaneLoad > 0.3)
		{
			return numCalSteps * 3;
		}
		if(maxAverageLaneLoad > 0.2)
		{
			return numCalSteps * 2;
		}

		return numCalSteps;
	}

	public int getMaxCalSeries()
	{
		if(maxAverageLaneLoad > 0.4)
		{
			return 5;
		}
		if(maxAverageLaneLoad > 0.3)
		{
			return 4;
		}
		if(maxAverageLaneLoad > 0.2)
		{
			return 3;
		}

		return 2;
	}
	/*POMDPGLD*/

	/**
	 * Resets data
	 */
	public void reset() throws SimulationRunningException
	{
		if (thread.isRunning())
		{
			throw new SimulationRunningException("Cannot reset data while simulation is running.");
		}
		infra.reset();
		tlc.reset();
		dp.reset(); //driving policy might also need being reset (DOAS 06)

		sgnctrl.reset(); /*POMDPGLD*/
		curTimeStep = 0;
		generator = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);


		/*POMDPGLD*/
		error_generator = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
		GLDSim.RND = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);


		/*EJUST*/
		RandomUniform.random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
		RandomGaussian.random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);

		
		TrackerFactory.resetTrackers();

		setChanged();
		notifyObservers();
	}

	//GASTON
	public void disableTraffic(Drivelane lane){
		try {
			Road actual_road = lane.getRoad();
			actual_road.setEnabled(false);
			Node[] nodes = infra.getAllNodes();
			for (int i=0;i<nodes.length;i++){
				nodes[i].remAllPaths();
				if (nodes[i] instanceof Junction){
					((Junction)nodes[i]).updateAllAvailableRoads();
				}
			}
			ShortestPathCalculator spcalculator = new ShortestPathCalculator();
			spcalculator.calcAllShortestPaths(infra);
			/*Validation validation = new Validation(infra);
			validation.validate();*/
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	//GASTON
	public void enableTraffic(Drivelane lane){
		try {
			Road actual_road = lane.getRoad();
			actual_road.setEnabled(true);
			Node[] nodes = infra.getAllNodes();
			for (int i=0;i<nodes.length;i++){
				nodes[i].remAllPaths();
				if (nodes[i] instanceof Junction){
					((Junction)nodes[i]).updateAllAvailableRoads();
				}
			}
			ShortestPathCalculator spcalculator = new ShortestPathCalculator();
			spcalculator.calcAllShortestPaths(infra);
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	/** Does 1 step in the simulation. All cars move, pedestrians get squashed etc... */
	public void doStep()
	{
		curTimeStep++;
		
		//SBC
		tlc.setCurTimeStep(curTimeStep);
		
		infra.setCurTimeStep(curTimeStep);    //(DOAS 06) so that the Infrastructure.curTimeStep is not zero all the time

		//GASTON:
		int statisticsInterval = Integer.parseInt(rb.getString(statsTimeStepsKey));
		if (curTimeStep% statisticsInterval == 0)
		{
			//GASTON:we try to export runtime information
			sendStatistics();			
		}

		boolean structureChanged = false;
		// (DOAS 06) Setting added to the menu
		if ( this.controller.getAccidents() ) 
		{
			structureChanged = infra.disableRandomLane(); // (DOAS 05) these two functions enable the use of 'accidents'
			structureChanged |= infra.enableRandomLane();  // removing those two lines will revert the program to the normal version
		}
		
		if (structureChanged && this.controller.getRerouting())
		{
			// the paths have to be recalculated, because the structure changed (DOAS 06)
			ShortestPathCalculator calc = new ShortestPathCalculator();
			try
			{
				calc.calcAllShortestPaths(infra);
			} 
			catch (InfraException e)
			{
				e.printStackTrace();
			}
		}

		if (!hasRun)
		{
			initialize();
			hasRun = true;
		}
		
		try
		{
			cityDoStep();
		}
		
		catch (Exception /*StackOverflowError*/ s)
		{
			System.out.println("java.lang.StackOverflowError: " + s.getMessage());
			nextSeries();
		}
		
		setChanged();
		notifyObservers();

		printProgressBar(48, numSeriesSteps, curTimeStep); /*POMDPGLD*/

		if (runSeries && curTimeStep >= numSeriesSteps)
		{
			/* POMDPGLD: DEBUG info..
            try
            {
			   Object lane = infra.getAllInboundLanes().get(1);
			   if(lane instanceof PODrivelane)
			   {
			       printB(((PODrivelane)lane).getBeliefstate());
			   }
			}
			catch(Exception e)
			{
			   e.printStackTrace();
			}
			 */
			nextSeries();
		}

		/*POMDPGLD*/
		updateIncreaseStatistics();
		
		if(runCalibrationSeries && getCurNumWaiting() > maxWaitingAllowed)
		{
			calSerieSuccesful = false;
			stopCalSeries();
		}
		
		if(runCalibrationSeries && (curTimeStep >= numCalSteps + addedCalSteps || curTimeStep >= numSeriesSteps))
		{
			numCalSeries = 1;
			nextCalibration();
		}
		/*POMDPGLD*/


		if (locked && runSeries)
		{
			lockedSeries();
		}
	}

	public void initialize()
	{
		SAVE_STATS = true;
		Enumeration e = Arrayutils.getEnumeration(infra.getSpecialNodes());
		while (e.hasMoreElements())
		{
			((SpecialNode) (e.nextElement())).start();
		}
		//GASTON: we load the different spawning frequencies
		spawnFreqsList = new LinkedList<HashMap<String, String[]>>();
		InputSource source = new InputSource(rb.getString(file_franjas_key));
		DOMParser parser = new DOMParser();
		try {
			parser.parse(source);
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Document doc = parser.getDocument();
		NodeList nodeList = doc.getElementsByTagName("franja");
		for (int i=0;i<nodeList.getLength();i++){
			//System.out.println(nodeList.item(i).getNodeName()); /*EJUST commented*/
			NodeList edgeNodesList = nodeList.item(i).getChildNodes();
			HashMap<String, String[]> spawnFreqsHM = new HashMap<String, String[]>();
			for (int j=0;j<edgeNodesList.getLength() ;j++){
				if (!edgeNodesList.item(j).getNodeName().equals("edgenode")) continue;
				String nodeid = null;
				NamedNodeMap nnm = edgeNodesList.item(j).getAttributes();
				if(nnm != null )
				{
					int len = nnm.getLength() ;
					Attr attr;
					for ( int a = 0; a< len; a++ )
					{
						attr = (Attr)nnm.item(a);
						//System.out.print(' ' + attr.getNodeName() + "=\"" + attr.getNodeValue() +  '"' ); /*EJUST commented*/
						if (attr.getNodeName().equals("id")) nodeid = attr.getNodeValue();
					}

					if (nodeid != null){
						NodeList spawnFreqsList = edgeNodesList.item(j).getChildNodes();
						String[] freqs = {"0.0","0.0","0.0"};

						for (int k=0;k < spawnFreqsList.getLength();k++)
						{
							org.w3c.dom.Node nodo= spawnFreqsList.item(k);
							int type = nodo.getNodeType();
							//if (type == org.w3c.dom.Node.)
							String nodeName = nodo.getNodeName();
							//String nodeValue = nodo.getNodeValue();
							//String nodeName = spawnFreqsList.item(k).getNodeName();
							//String nodeValue = spawnFreqsList.item(k).getFirstChild().getNodeValue();
							if (nodeName.equals("car")){
								org.w3c.dom.Node child = nodo.getFirstChild();
								freqs[0] = child.getNodeValue();
							}
							if (nodeName.equals("bus")){
								org.w3c.dom.Node child = nodo.getFirstChild();
								freqs[1] = child.getNodeValue();
							}
							if (nodeName.equals("bicycle")){
								org.w3c.dom.Node child = nodo.getFirstChild();
								freqs[2] = child.getNodeValue();
							}
						} 
						spawnFreqsHM.put(nodeid,freqs);	
					}
				}				
			}
			//System.out.println(" "); //EJUST
			spawnFreqsList.add(spawnFreqsHM);
		} 
		iFreqs = spawnFreqsList.iterator();
		//updateSpawnFreqs();
	}

	/** Gets the speed of the simulation */
	public int getSpeed()
	{
		return thread.getSleepTime();
	}

	/** Sets the speed of the simulation */
	public void setSpeed(int s)
	{
		thread.setSleepTime(s);
	}


	protected void cityDoStep()
	{
		try
		{
			specialNodesDoStep();
			moveAllRoadusers();
			spawnNewRoadusers();
			sgnctrl.switchSigns();

			/*POMDPGLD*/
			updateLaneStatistics();
			updateBeliefstateCounters();
			updateBeliefstateProbCounters();

		}
		catch (Exception e)
		{
			System.out.println("The simulator made a problem at time step:	" + curTimeStep /*EJUST*/);
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	protected void specialNodesDoStep() throws Exception
	{
		Enumeration specialNodes = Arrayutils.getEnumeration(infra.getSpecialNodes());
		while (specialNodes.hasMoreElements())
		{
			((SpecialNode) (specialNodes.nextElement())).doStep(this);
		}
	}

	/**
	 *
	 * moving all Roadusers to their new places
	 */
	public void moveAllRoadusers() throws InfraException
	{
		// Line below is faster than the obvious alternative
		Enumeration lanes = Arrayutils.getEnumeration(infra.getAllInboundLanes().toArray());
		Drivelane lane;

		while (lanes.hasMoreElements())
		{
			lane = (Drivelane) (lanes.nextElement());

			/* POMDPGLD: going to calculate the laneLoad statistics (best to calculate this before everyone moves) 
			 * EJUST: going to calculate the laneNumRoadusersWaiting statistics */
			if(lane.getSign().getType() == Sign.TRAFFICLIGHT)
			{
				double load = ((double)lane.getNumBlocksTaken() / (double)lane.getCompleteLength());
				int numRoadusersWaiting = lane.getNumRoadusersWaiting(); //EJUST
				
				if(tempMaxLaneLoad < load)
				{
					tempMaxLaneLoad = load;
				}
				if(tempMaxLaneNumRoadusersWaiting < numRoadusersWaiting) //EJUST
				{
					tempMaxLaneNumRoadusersWaiting = numRoadusersWaiting; //EJUST
				}
				
				laneLoad += load;
				laneNumRoadusersWaiting += numRoadusersWaiting; //EJUST
				
				lanesProcessed++;
			}

			// First you should check whether they are already moved ......
			if (lane.getTimeStepMoved() != curTimeStep)
			{
				moveLane(lane);
			}
		}
	}

	/**
	 * moving all roadusers from one lane to their new places
	 *
	 * @author Jilles V, Arne K, Chaim Z and Siets el S
	 * @param lane The lane whose roadusers should be moved
	 * @version 1.0
	 */
	/* EJUST commented: 
	 * @param callingLanes Vector of drivelanes, for recursive internal use only, 
	 * this parameter should have the value null, when called from the outside
	 * 
	 * Now is replaced by a flag indicating whether the drive lane is checked for movement before
	 * */
	protected void moveLane(Drivelane lane) throws InfraException
	{
		// (SBC) check the roadusers if they have to change speed on this lane
		//checkRoaduserSpeed(lane); 
		
		/*EJUST*/
		accelerate(lane); 
		//changeLanes(lane);

		LinkedList<Roaduser> queue;
		ListIterator<Roaduser> li;

		/*POMDPGLD*/
		ListIterator<ObservedRoaduser> liPO = null;
		ListIterator<ObservedRoaduser> liMLQ = null;
		
		Drivelane sourceLane, destLane = null;
		Node node;
		Sign sign;
		Roaduser ru;

		/*POMDPGLD*/
		ObservedRoaduser oRu = null;
		Beliefstate bfs = null;

		int ru_des, ru_type, ru_len;
		
		double ru_pos, ru_speed; /*EJUST: int --> double*/
	
		/*EJUST*/
		EdgeNode ru_startNode; 
		SpawnFrequency ru_startNodeSpawnFrequency; 
		Hashtable spawnTimeStepsHash; 
		
		//GASTON: we calculate the statistics for the lane, we will do it in every timeStep.
		lane.processStats();
		
		// POMDPGLD: Partially observability variables, need for learning the model with MLQ
		double ru_po_pos = -1;	/*EJUST: int --> double*/
		double ru_po_speed = -1; /*EJUST: int --> double*/

		sign = lane.getSign();
		queue = lane.getQueue();
		li = queue.listIterator();

		/*POMDPGLD*/
		if(lane instanceof PODrivelanes)
		{
			bfs = ((PODrivelanes)lane).getBeliefstate();
			liPO = bfs.getRoadusersIterator();
			if(usePO)
			{
				liMLQ = ((PODrivelanes)lane).getFutureMLIterator();
			}
		}

		//(DOAS 06) This must be in the beginning!!!!
		lane.setTimeStepAsked(curTimeStep);
		
		/*EJUST: Resets the colearn statistics for this drivelane, we will do it in every timeStep*/
		lane.resetDrivelaneColearnStats();

		/*EJUST: Resets the speed statistics for this drivelane, we will do it in every timeStep*/
		lane.resetDrivelaneSpeedStats();
		
		while (li.hasNext())
		{
			try
			{
				ru = (Roaduser) li.next();								
				
				/*POMDPGLD*/
				if(lane instanceof PODrivelanes)
				{
					liPO.next();
					if(usePO && liMLQ != null && liMLQ.hasNext() )
					{
						oRu = (ObservedRoaduser)liMLQ.next();
					}
					else
					{
						oRu = null;
					}
				}
				
				//POMDPGLD: Early error hacks for sensor information, uses the chanceOnErrRU variable.
				if(error_generator.nextDouble() < chanceOnErrRU)
				{
					if(error_generator.nextDouble() < 0.5)
					{
						ru.setVisibility(ru.NOT_DETECTED);
					}
					else
					{
						ru.setVisibility(ru.DOUBLE_DETECTED);
					}
				}
				else
				{
					ru.setVisibility(ru.DETECTED);
				}				

			}
			// When this exception is thrown you removed the first element of the queue, therefore re-create the iterator.
			catch (NoSuchElementException /* POMDPGLD: Changed the exception type that was: Exception*/ e)
			{
				li = queue.listIterator();

				/*POMDPGLD*/
				if(lane instanceof PODrivelanes)
				{
					liPO = bfs.getRoadusersIterator();
					if(usePO)
					{
						liMLQ = ((PODrivelanes)lane).getFutureMLIterator();
					}
				}

				continue;
			}

			// Only attempt to move this RU when it hasn't already
			if (!ru.didMove(curTimeStep))
			{
				// DOAS 06
				// No need to reset color
				// ru.setColor(new Color(0, 0, 255));
				boolean edgeNodeReached = false;

				ru.setTimeStepAsked(curTimeStep); //Sets the last timeStep this Roaduser was asked its movements 	
				node = sign.getNode();
				
				ru_pos = ru.getPosition();
				ru_speed = ru.getSpeed();
				ru_type = ru.getType();
				ru_len = ru.getLength();
				ru_des = ru.getDestNode().getId();	
				
				/*EJUST*/
				ru_startNode = (EdgeNode)ru.getStartNode();
				spawnTimeStepsHash = ru_startNode.getSpawnTimeStepsHash();
				ru_startNodeSpawnFrequency = ru_startNode.getSpawnFrequency(ru_type);
				if(spawnTimeStepsHash.containsKey(curTimeStep))
					ru.setModel(getIDMType(ru_type, ru_startNodeSpawnFrequency.weatherCondition));
				
				/*POMDPGLD*/
				PosMov[] posMovs = null;
				ru_po_pos = -1;				
				
				if(usePO)
				{
					if(oRu != null)
					{
						ru_po_pos = oRu.getPos();
						ru_po_speed = oRu.getSpeed();
						posMovs = calcPoPosMovs(node, sign, lane, oRu, liMLQ);
					}
				}
				else
				{
					// non PO manner of getting methods
					posMovs = calcPosMovs(node, sign, lane, ru, li); //Original code
				}
				/*POMDPGLD*/


				ru.setInQueueForSign(false);

				if (lane.getFreeUnitsInFront(ru) < ru_speed*dt) // DOAS 06: Speed of car = 2. //SBC: 10
				{
					ru.setInQueueForSign(true);
				}

				/* EJUST: drivelaneEngageQueueTime = -1 means either due to the initial setting 
				 * (i.e., so far no waiting time for this roaduser in this lane) or 
				 * due to this roaduser just arrived/crossed a junction so still does not engage any new queue
				 */
				if (ru.getDrivelaneEngageQueueTime() == -1){
					if (lane.updateWaitingPosition(ru)){ //Returns if this ru is waiting in this lane
						ru.setDrivelaneEngageQueueTime(curTimeStep);												
						if (sign.getType()==Sign.TRAFFICLIGHT){
							ru.incrementAbsoluteStopsCount();
							ru.incrementStopsCount();
						}												
					}
				}
				else if (sign.getType()==Sign.TRAFFICLIGHT && ru.getPrevSpeed()> STOP_SPEED_MS && ru_speed <= STOP_SPEED_MS)
					ru.incrementStopsCount();
				
				// Handle Roadusers that possibly can cross a Node: a roaduser with speed
				// equal or greater than the number of positions between him and the junction.
				if (ru_pos - ru_speed*dt < 0)
				{
					// Handle Roadusers that get to Special Nodes
					if (node instanceof SpecialNode)
					{
						if (ru_pos <= STOP_SPEED_MS*dt /*EJUST replaced: ru_pos==0*/ 
							|| lane.getPosFree(li, 0, ru_len, ru_speed, ru) <= STOP_SPEED_MS*dt /*EJUST replaced: getPosFree==0*/)							
						{
							/* EJUST comment:
							 * If the roaduser is standing at position zero or 
							 * the best reachable position for the supplied roaduser on the Queue is zero
							 * then calculate the roaduser delay*/
							
							node.processStats(ru, curTimeStep, sign);
							
							ru.setPosition(-1);
							ru.setPrevSign(-1);
							ru.setPrevSignLastPosition(-1); /*EJUST*/							
							li.remove();

							/*POMDPGLD*/
							if(lane instanceof PODrivelanes)
							{
								// if we are not building the model from PO, we also need to remove this
								bfs.removeRoaduser(liPO);
							}

							ru.setWaitPos(sign.getId(), sign.mayDrive(), ru_pos);
							ru.setInQueueForSign(false);

							if(usePO && oRu != null && ru_po_pos > -1) /*POMDPGLD*/
							{
								liMLQ.remove(); /*POMDPGLD*/
								ru.addDistance(ru_po_pos); /*EJUST*/
								tlc.updateRoaduserMove(ru, lane, sign, ru_po_pos, null, null, 0, posMovs, null); 
							}
							else if(!usePO) /*POMDPGLD*/
							{
								ru.addDistance(ru_pos); /*EJUST*/
								tlc.updateRoaduserMove(ru, lane, sign, ru_pos, null, null, 0, posMovs, null);
							}

							//Hook method that is called by the infra when a roaduser reaches this node
							((SpecialNode) (node)).enter(ru);
							ru = null;
							edgeNodeReached = true;							
						}
					}

					// Handle Roadusers that are (or nearly) at a Sign
					// Returns true if the Roaduser at the start of the Drivelane may cross the Node.
					else if (lane.getSign().mayDrive())
					{
						if (ru_pos <= STOP_SPEED_MS*dt /*EJUST replaced: ru_pos==0*/
						 || lane.getPosFree(li, 0, ru_len, ru_speed, ru) <= STOP_SPEED_MS*dt /*EJUST replaced: getPosFree==0*/)	
						{ 							
							// Can cross-check
							destLane = dp.getDirection(ru, lane, node); // HACK

							//GASTON: I added these lines to check whether we could find a path or not
							if (destLane==null){
								//if we could not find an alternative path for the road users, we erase them from the lane.
								lane.getQueue().clear();
								return;									
							}
							//FIN DEL CHEQUEO

							//if(destLane!= null) /*EJUST commented*/
							//{
								// Check if there is room on the node
								if (destLane.isLastPosFree(ru_len))
								{
									try
									{
										// Let the RU touch the Sign to refresh/unload some statistical data
										node.processStats(ru, curTimeStep, sign); 										
										
										destLane.addRoaduserAtEnd(ru);
										/*EJUST: Moved PrevSign settings from here*/
										li.remove(); // Remove the RU from the present lane, and place it on the destination lane

										/*POMDPGLD*/
										if(lane instanceof PODrivelanes)
										{
											bfs.removeRoaduser(liPO);
										}

										ru.setWaitPos(sign.getId(), sign.mayDrive(), ru_pos);
										ru.setInQueueForSign(false);
										ru.setTimeStepMoved(curTimeStep);
																				
										/*POMDPGLD*/
										if(usePO && oRu != null && ru_po_pos > -1)
										{
											liMLQ.remove();
											ru.addDistance(ru.getLength() + ru_po_pos); /*EJUST*/
											tlc.updateRoaduserMove(ru, lane, sign, ru_po_pos, destLane, destLane.getSign(), destLane.getCompleteLength() - ru.getLength(), posMovs, destLane);
										}										
										else if(!usePO)/*POMDPGLD*/
										{											
											ru.addDistance(destLane.getCompleteLength() - ru.getPosition() + ru_pos); /*EJUST*/
											//Original code
											tlc.updateRoaduserMove(ru, lane, sign, ru_pos, destLane, destLane.getSign(), ru.getPosition(), posMovs, destLane);
										}
										
										/*EJUST: Moved PrevSign settings to here*/
										ru.setPrevSign(lane.getSign().getId());
										ru.setPrevSignLastPosition(ru_pos); /*EJUST*/                                        
									}
									catch (Exception e)
									{
										System.out.println("Something screwd up in SimModel.moveLane where a Roaduser is about to cross" +
														    " at time step: " + curTimeStep /*EJUST*/);
										e.printStackTrace();
									}
								}
								else
								{ 
									// Otherwise, check if the next lane should move, and then do just that
									// Because currently there is no room on the next lane
									if (curTimeStep != destLane.getTimeStepAsked() && curTimeStep != destLane.getTimeStepMoved())
									{ 
										// If the position is not free, then check if it already moved this turn, if not:
										moveLane(destLane); // System.out.println("Waiting for another lane to move..");
									}
									
									if (destLane.isLastPosFree(ru_len))
									{ 
										// Ok now the lane that should have moved, moved so try again .........
										try
										{
											// Let the RU touch the Sign to refresh/unload some statistical data
											node.processStats(ru, curTimeStep, sign);
											
											destLane.addRoaduserAtEnd(ru);
											/*EJUST: Moved PrevSign settings from here*/
											li.remove();  // Remove the RU from the present lane, and place it on the destination lane

											/*POMDPGLD*/
											if(lane instanceof PODrivelanes)
											{
												bfs.removeRoaduser(liPO);
											}

											ru.setWaitPos(sign.getId(),sign.mayDrive(), ru_pos);											
											ru.setInQueueForSign(false);
											ru.setTimeStepMoved(curTimeStep);

											/*POMDPGLD*/
											if(usePO && oRu != null && ru_po_pos > -1)
											{
												liMLQ.remove();
												ru.addDistance(ru.getLength() + ru_po_pos); /*EJUST*/
												tlc.updateRoaduserMove(ru, lane, sign, ru_po_pos, destLane, destLane.getSign(), destLane.getCompleteLength() - ru.getLength(), posMovs, destLane);
											}											
											else if(!usePO)/*POMDPGLD*/
											{												
												ru.addDistance(destLane.getCompleteLength() - ru.getPosition() + ru_pos); /*EJUST*/
												//Original code
												tlc.updateRoaduserMove(ru, lane, sign, ru_pos, destLane, destLane.getSign(), ru.getPosition(), posMovs, destLane);
											}
											
											/*EJUST: Moved PrevSign settings to here*/
											ru.setPrevSign(lane.getSign().getId());
											ru.setPrevSignLastPosition(ru_pos);/*EJUST*/																						
										}
										catch (Exception e)
										{
											System.out.println("Something screwd up in SimModel.moveLane where a Roaduser is about to cross" +
												    			" at time step: " + curTimeStep /*EJUST*/);
											e.printStackTrace();											
										}
									}
									else
									{ 																				
										// Apparently no space was created, so we're still here.
										if (moveRoaduserOnLane(li, ru, ru_speed,lane) > STOP_SPEED_MS*dt /*EJUST replaced: moves > 0*/)
										{
											ru.setWaitPos(sign.getId(), sign.mayDrive(), ru_pos);
										}

										// POMDPGLD: Update for partially observable drivelanes
										if(lane instanceof PODrivelanes)
										{
											bfs.moveRoaduser(liPO, noise_method);
										}

										destLane = lane; /*EJUST*/										
										if(usePO && oRu != null && ru_po_pos > -1)/*POMDPGLD*/
										{
											moveRoaduserOnLane(liMLQ, oRu, ru_po_speed, lane);
											ru.addDistance(ru_po_pos - oRu.getPos()); /*EJUST*/
											tlc.updateRoaduserMove(ru, lane, sign, ru_po_pos, lane, sign, oRu.getPos(), posMovs, null);
										}
										else if(!usePO)/*POMDPGLD*/
										{											
											ru.addDistance(ru_pos - ru.getPosition()); /*EJUST*/
											//Original code
											tlc.updateRoaduserMove(ru, lane, sign, ru_pos, lane, sign, ru.getPosition(), posMovs, null);
										}										
									}
								}
							//}
						}
					}
					else
					{ 
						/*EJUST:*/
						  System.out.println("This code is unreachable when using the IDM acceleration model, "+ 
						  					   "because here new_position = ru_pos - ru_speed*dt < 0 while Light is red");
						
						/* Light==red, Try to move user as far as it can go on this lane. Update it's move. */
						if (moveRoaduserOnLane(li, ru, ru_speed, lane) > STOP_SPEED_MS*dt /*EJUST replaced: moves > 0*/)
						{
							ru.setWaitPos(sign.getId(), sign.mayDrive(), ru_pos);
						}

						// POMDPGLD: Update for partially observable drivelanes
						if(lane instanceof PODrivelanes)
						{
							bfs.moveRoaduser(liPO, noise_method);
						}

						destLane = lane; /*EJUST*/
						if(usePO && oRu != null && ru_po_pos > -1)/*POMDPGLD*/
						{
							moveRoaduserOnLane(liMLQ, oRu, ru_po_speed, lane);
							ru.addDistance(ru_po_pos - oRu.getPos()); /*EJUST*/
							tlc.updateRoaduserMove(ru, lane, sign, ru_po_pos, lane, sign, oRu.getPos(), posMovs, null);
						}
						else if(!usePO)/*POMDPGLD*/
						{							
							ru.addDistance(ru_pos - ru.getPosition()); /*EJUST*/
							//Original code
							tlc.updateRoaduserMove(ru, lane, sign, ru_pos, lane, sign, ru.getPosition(), posMovs, null);
						}						
					}
				}
				/* Roaduser impossibly can cross a sign. 
				 * The maximum amount of space per speed is traveled */
				else
				{
					if (moveRoaduserOnLane(li, ru, ru_speed, lane) > STOP_SPEED_MS*dt /*EJUST replaced: moves > 0*/)
					{
						ru.setWaitPos(sign.getId(), sign.mayDrive(), ru_pos);
					}
					
					ru.setTimeStepMoved(curTimeStep);

					// POMDPGLD: Update for partially observable drivelanes
					if(lane instanceof PODrivelanes)
					{
						bfs.moveRoaduser(liPO, noise_method);
					}

					/*EJUST
					if (node instanceof SpecialNode)
						System.out.println("I am on my way towards an edge node" +
											"as Dr. Walid said seems like there exist a virtual sign at the end of this lane.");
					*/			
					destLane = lane; /*EJUST*/
					if(usePO && oRu != null && ru_po_pos > -1) /*POMDPGLD*/
					{
						moveRoaduserOnLane(liMLQ, oRu, ru_po_speed, lane);
						ru.addDistance(ru_po_pos - oRu.getPos()); /*EJUST*/
						tlc.updateRoaduserMove(ru, lane, sign, ru_po_pos, lane, sign, oRu.getPos(), posMovs, null);
					}
					else if(!usePO) /*POMDPGLD*/
					{						
						ru.addDistance(ru_pos - ru.getPosition()); /*EJUST*/
						//Original Code
						tlc.updateRoaduserMove(ru, lane, sign, ru_pos, lane, sign, ru.getPosition(), posMovs, null);
					}					
				}

				/*POMDPGLD*/
				// Update Roadusers speed for their next move, those that are waiting for a sign are affected only in the way
				// that their internal max speed is altered, those that are driving can change their speed accordingly
				if(usePO /*EJUST*/ && ru != null)
				{
					ru.updateSpeed();
				}	

				// DOAS 06: this code block checks whether the current road user has
				// been stuck for more than x time steps, if so this car and all other waiting
				// behind it will be removed. A penalty will be given to the learner, since
				// stuck cars aren't nice.

				//DEBUG DOAS 06
				/*if (!edgeNodeReached && curTimeStep - ru.getTimeStepMoved() > controller.getMaxWaitingTime())
				{
                     System.out.println("I'd remove a car now " + lane.getNodeLeadsTo().getId());
                     controller.pause();
                }*/

				if (this.controller.getRemoveStuckCars() && !edgeNodeReached && curTimeStep - ru.getTimeStepMoved() > controller.getMaxWaitingTime())
				{
					//System.out.println("Car seems to be stuck, removing car.");
					//ru.addDelay(controller.getPenalty());
					//node.processStats(ru, curTimeStep, sign);
					ru.setPosition(-1);
					ru.setPrevSign(-1);
					ru.setPrevSignLastPosition(-1);/*EJUST*/
					li.remove();

					/*POMDPGLD*/
					// Update for partially observable drivelanes
					if(lane instanceof PODrivelanes)
					{
						bfs.removeRoaduser(liPO);
					}

					ru.setWaitPos(sign.getId(), sign.mayDrive(), ru_pos);
					ru.setInQueueForSign(false);
					
					//update learners (punishment for removed cars) (DOAS 06)
					if(tlc instanceof TCRL){
						//System.out.println("We have penalty: " + controller.getPenalty());
						((TCRL) tlc).updateRoaduserMove(ru, lane, sign, ru_pos, lane, sign, 0, posMovs, null, controller.getPenalty());
					} 
					else {
						tlc.updateRoaduserMove(ru, lane, sign, ru_pos, lane, sign, 0, posMovs, null);
					}

					ru = null;
					// increment counter of cars removed.
					infra.removedCarsIncrement();
				}
				
				// POMDPGLD: Update Roadusers speed for their next move, those that are waiting for a sign are affected only in the way
				// that their internal max speed is altered, those that are driving can change their speed accordingly

				//				if (ru != null)
				//				{
				//					ru.setTimeStepMoved(curTimeStep);
				//				}
				
				/*EJUST*/
				if (destLane!=null && destLane.getSign()!=null)
				{					
					if (tlc instanceof TCColearnPerformanceIndex)
					{ 
						TCColearnPerformanceIndex tlc_colearn = (TCColearnPerformanceIndex) tlc; 
						int  ru_pos_now = (int)Math.ceil(ru.getPosition());
						Sign ru_sign_now = destLane.getSign();
						int  ru_signId_now = ru_sign_now.getId(); 

						if(sign.getType()==Sign.TRAFFICLIGHT && 
								(ru_sign_now.getType()==Sign.TRAFFICLIGHT || ru_sign_now.getType()==Sign.NO_SIGN))
						{								
							double expectedTripWaitingTime, expectedTripTime, expectedDistance;
						
							expectedTripWaitingTime = tlc_colearn.getExpectedTripWaitingTime(ru_signId_now, ru_pos_now, ru_des);
							expectedTripTime = tlc_colearn.getExpectedTripTime(ru_signId_now, ru_pos_now, ru_des);
							expectedDistance = tlc_colearn.getExpectedDistance(ru_signId_now, ru_pos_now, ru_des);							
					
							//if(!edgeNodeReached): Sure satisfied once destLane!=null
							lane.processColearnTripStatistics(ru, curTimeStep, expectedTripWaitingTime, expectedTripTime, expectedDistance); 
							
							if (ru_sign_now.getType()==Sign.TRAFFICLIGHT){ //ru_sign_now.getNode() instanceof Junction
								double expectedJunctionWaitingTime = tlc_colearn.getExpectedJunctionWaitingTime(ru_signId_now, ru_pos_now);
								lane.processColearnJunctionWaitingTime(ru, curTimeStep, expectedJunctionWaitingTime);
							}
						}
					}	
					lane.processSpeedStatistics(ru, curTimeStep);
					destLane=null;
				}									
			}
			else
			{
				// DOAS 06: No need to repaint the vehicle.
				// ru.setColor(new Color(0, 0, 0));
			}
		}
		
		lane.setTimeStepMoved(curTimeStep);
	}

	/** EJUST
	 * All roadusers on this lane should be checked if they have to change speed. 
	 * @param lane the lane that has to be checked
	 */

	protected void accelerate(Drivelane lane){
		LinkedList<Roaduser> queue;
		ListIterator<Roaduser> li;
		queue = lane.getQueue();
		li = queue.listIterator();

		Roaduser ru;
		Roaduser frontRoaduser = null;

		while (li.hasNext()) {
			try {
				ru = (Roaduser) li.next();
			} catch (Exception e) {
				// When this exception is thrown you removed the first element
				// of the queue, therefore re-create the iterator.
				System.out.println("CME");
				li = queue.listIterator();				
				continue;
			}

			if (frontRoaduser == null) // road user is first road user on the lane 
				frontRoaduser = lane.getFrontRoaduserOfFirstRoaduser(ru);
	        
			ru.accelerate(frontRoaduser, dt); //check distance to front road user and adjust speed
			
			frontRoaduser = ru;
		}
	}

	/**EJUST
	 * Check the new drive lane
	 * */
	protected boolean checkDrivelane(Drivelane testLane, Drivelane origLane, Roaduser ru, 
									 Drivelane[] shortest, Drivelane nextDL) throws InfraException
	{
		if (testLane==null) 
			return false;

		/* Test if the drivelane has the same or more targets as the original dl.
		 * As the original lane may be directed right or straight
		 * And the left lane may be directed left
		 * So, if the roaduser change lane it will not reach to its destination*/
		boolean found = true;

		int direction = dp.determineDirection(testLane, nextDL);
		
		/* The directions Roadusers switch lanes to: left:0, straight ahead:1 and right:2.*/
		if (origLane.getTarget(direction) && !testLane.getTarget(direction)) 
			found = false;
		
		for (int i=0; i<shortest.length; i++) 
			if (shortest[i]==testLane) 
				found = true;
		
		if (!found) 
			return false;

		// Check whether this roaduser may enter that road, i.e. the types should be correct.
		if ((testLane.getType() & ru.getType())==0) 
			return false;

		/* Is the position free?*/
		if (!testLane.isPosFree(ru.getPosition(),ru.getLength())) 
			return false;
		
		/* Is the next position free?, otherwise it is useless...
		 * May be ru is at position 0 in the origLane, but testLane next is much free than origLane next.
		if (ru.getPosition()==0) 
			return false;*/
		
		if (testLane.getNumRoadusersWaiting()>0)
			if (!testLane.isPosFree(ru.getPosition()-1,1)) 
				return false;

		return true;		
	}

	/** EJUST
	 * All road users on this lane should be checked if they have to change lane. 
	 * @param lane the lane that has to be checked
	 */

	protected void changeLanes(Drivelane lane){
		
		LinkedList<Roaduser> queue;
		ListIterator<Roaduser> li;
		
		queue = lane.getQueue();
		li = queue.listIterator();
		
		Sign sign = lane.getSign();
		Node node = sign.getNode();
		
		Road r = lane.getRoad();
		Drivelane[] lanes=r.getAlphaLanes(); //lane.getRoad().getInboundLanes(lane.getNodeLeadsTo());
		
		//int oldLaneId = lane.getId();
		
		// search for this lane
		int index=-1;
		for (int i=0; i<lanes.length; i++) 
			if (lanes[i]==lane) 
				index = i;
		
		if (index==-1) 
		{
			lanes = r.getBetaLanes();
			for (int i=0; i<lanes.length; i++) 
				if (lanes[i]==lane) 
					index=i;
		}

		Drivelane leftDL = null, rightDL = null;

		if (index>0) 
			leftDL = lanes[index-1];
		if (index<lanes.length-1) 
			rightDL= lanes[index+1];

		Roaduser ru;	
		Roaduser frontRoaduserOld = null;
		boolean sameLane=true;

		while (li.hasNext()) {
			try {
				ru = (Roaduser) li.next();
			} catch (Exception e) {
				// When this exception is thrown you removed the first element
				// of the queue, therefore re-create the iterator.
				System.out.println("CME");
				li = queue.listIterator();
				continue;
			}

			if (frontRoaduserOld == null) // road user is first road user on the lane 
				frontRoaduserOld = lane.getFrontRoaduserOfFirstRoaduser(ru);

			if (ru.timeToChange(dt)){
				try {
					boolean goToLeftLane=false;
					Roaduser frontRoaduserLeft = null, frontRoaduserRight;
					Roaduser backRoaduserLeft, backRoaduserRight;

					Drivelane[] shortest = node.getShortestPaths(ru.getDestNode().getId(), ru.getType());					
	                
					Drivelane[] lanesleadingfrom = node.getAvailableLanesLeadingFrom(lane, ru.getType()); 
	                //Drivelane[] lanesleadingfrom = node.getOutboundLanes();
					
					Drivelane nextDL = dp.getDirectionLane(ru, lane, lanesleadingfrom, shortest);					
					//Drivelane nextDL = dp.getDirection(ru, lane, node);
					
					//for (int i=0; i<lanes.length;i++){
					sameLane = true;
					//if (oldLaneId == lanes[i].getId()){
					//if (i > 0){ 
					//leftDL = lanes[i-1];
					if (checkDrivelane(leftDL, lane, ru, shortest, nextDL))
					{
						frontRoaduserLeft = leftDL.getFrontRoaduserOnNewLane(ru);
						//frontRoaduserLeft = null: Means ru will be the first in the new lane and moving towards an edge node
						
						backRoaduserLeft = leftDL.getBackRoaduserOnNewLane(ru);
						//backRoaduserLeft = null: Means ru will be the last in the new lane and coming from an edge node
						
						if (ru.change(false, frontRoaduserOld, frontRoaduserLeft, backRoaduserLeft)) 
							goToLeftLane = true;						
					}
					
					//if(i < lanes.length-1){
					//rightDL = lanes[i+1];
					if (checkDrivelane(rightDL, lane, ru, shortest, nextDL))
					{ 
						frontRoaduserRight = rightDL.getFrontRoaduserOnNewLane(ru);
						//frontRoaduserRight = null: Means ru will be the first in the new lane and moving towards an edge node
						
						backRoaduserRight = rightDL.getBackRoaduserOnNewLane(ru);
						//backRoaduserRight = null: Means ru will be the last in the new lane and coming from an edge node
							
						if(goToLeftLane){
							if (ru.change(true, frontRoaduserLeft, frontRoaduserRight, backRoaduserRight)){ 
								li.remove();
								rightDL.addRoaduser(ru, ru.getPosition());											
							}						
							else{ 
								li.remove();
								leftDL.addRoaduser(ru, ru.getPosition());												
							}
							sameLane = false;						
						}						
						else{ //goToLeftLane = false;
							if (ru.change(true, frontRoaduserOld, frontRoaduserRight, backRoaduserRight)){ 
								li.remove();
								rightDL.addRoaduser(ru, ru.getPosition());
								sameLane = false;
							}											
						}													
					}
					else if (goToLeftLane){
						li.remove();
						leftDL.addRoaduser(ru, ru.getPosition());												
						sameLane = false;
					}
				//	break;
				//}							
				//}
				} catch (InfraException e) {
					e.printStackTrace();
				}
				if (sameLane)  
					frontRoaduserOld = ru;
			}			
		}
	}

	/** SBC
	 * All roadusers on this lane should be checked if they have to change speed. 
	 * @param lane the lane that has to be checked
	 */
	protected void checkRoaduserSpeed(Drivelane lane) {
		LinkedList<Roaduser> queue;
		ListIterator<Roaduser> li;
		Sign sign;

		sign = lane.getSign();
		queue = lane.getQueue();
		li = queue.listIterator();

		Roaduser ru;
		Roaduser prv = null;

		while (li.hasNext()) {
			try {
				ru = (Roaduser) li.next();
			} catch (Exception e) {
				// When this exception is thrown you removed the first element
				// of the queue, therefore re-create the iterator.
				System.out.println("CME");
				li = queue.listIterator();
				continue;
			}

			// (SBC) check distance to previous roaduser and adjust speed
			// Previous should be 'ru'
			//prv = (Roaduser) li.previous();

			//if (prv == ru && li.hasPrevious()) 
			if (prv != null)
			{ // roaduser is not the first one
				//prv = (Roaduser) li.previous();
				double prv_pos = prv.getPosition(); /*EJUST: int --> double*/
				int prv_length = prv.getLength();
				double prv_speed = prv.getSpeed(); /*EJUST: int --> double*/
				//prv = (Roaduser) li.next();

				double s = ru.getPosition() - (prv_pos + prv_length); /*EJUST*/

				// check distance
				if (s > 2 * ru.getStopDistance())
					// if distance to the previous big enough, speed up
					ru.adjustSpeedTo(lane.getSpeedMaxAllowed(), dt);
				else {
					if (s < ru.getStopDistance())
						ru.adjustSpeedTo(0, dt); // a little slower than the previous
					else
						// if at good distance, follow the car in front
						ru.adjustSpeedTo(prv_speed, dt);
				}

				// prv = (Roaduser) li.next();
			} 
			else 
			{   // roaduser is first roaduser on the lane

				// slow down if approaching red light
				if (lane.getSign().getType()==Sign.TRAFFICLIGHT && !lane.getSign().getState()) {
					if (ru.getPosition() < 4) // first roaduser just in front of red light
						ru.adjustSpeedTo(0, dt);
					else if (ru.getPosition() < 2 * ru.getStopDistance())
						ru.adjustSpeedTo(0, dt);
					else ru.adjustSpeedTo(lane.getSpeedMaxAllowed(), dt);
				} 
				else {
					// light is green
					// check if it can cross junction and if there is place left. 
					// if the roaduser cannot cross the juction because of no place on destination lane, it has to stop
					ru.adjustSpeedTo(lane.getSpeedMaxAllowed(), dt);
					try {
						int ru_type, ru_des, des_length;
						double des_pos; /*EJUST: int --> double*/
						Drivelane destLane;
						ru_type = ru.getType();
						ru_des = ru.getDestNode().getId();

						Drivelane[] shortestpaths = lane.getSign().getNode().getShortestPaths(ru_des, ru_type);

						Drivelane[] lanesleadingfrom = lane.getSign().getNode().getAvailableLanesLeadingFrom(lane, ru_type);
						
						destLane = dp.getDirectionLane(ru, lane, lanesleadingfrom, shortestpaths);

						LinkedList destqueue = destLane.getQueue();
						Roaduser destru = (Roaduser)destqueue.getLast();
						des_length = destLane.getLength();
						des_pos = destru.getPosition();
						
						if ( ru.getPosition() < lane.getSpeedMaxAllowed()*dt && des_pos > des_length + ru.getPosition() - ru.getSpeed()*dt ) 
							ru.adjustSpeedTo(0, dt);
						
						else ru.adjustSpeedTo(lane.getSpeedMaxAllowed(), dt);
					} 
					catch (Exception e) {
					
					}					
				}
			}// ///////////////////////////////////////////////////////////////////////////
			prv = ru;
		}
	}

	/**
	 * Moves the supplied roaduser on it's present lane as far as it can go to the best reacheable free position on the Queue given in the ListIterator
	 *
	 * @param li The Queue of this Drivelane represented in a ListIterator. li.previous() is the current RoadUser
	 * @param ru The Roaduser to be moved
	 * @param speed_left the number of 'moves' this Roaduser has left in this turn
	 * @param lane The present lane
	 *  
	 * @return the number of blocks the Roaduser can move ahead
	 */
	protected double moveRoaduserOnLane(ListIterator li, Roaduser ru, double speed_left /*EJUST: int --> double*/,	Drivelane lane)
	{
		double ru_pos = ru.getPosition(); /*EJUST: int --> double*/

		//SBC
		//int ru_speed = ru.getSpeed();

		int ru_len = ru.getLength();

		//SBC, EJUST: int --> double
		double ru_stopdistance = 0; /*EJUST commented: ru.getStopDistance()*/

		/*EJUST: int --> double*/
		double best_pos = ru_pos; 
		
		/*EJUST: int --> double*/
		double max_pos = ru_pos; 
		
		/*EJUST: int --> double*/		
		double target_pos = (ru_pos - speed_left*dt > 0) ? ru_pos - speed_left*dt : 0; 
		
		int waitsteps;

		//System.out.println("Targetpos:"+target_pos+" and hasPrev:"+li.hasPrevious());

		// Previous should be 'ru'
		Roaduser prv = (Roaduser) li.previous();

		if (prv == ru && li.hasPrevious()) // roaduser not first
		{ 
			/* has car in front */
			prv = (Roaduser) li.previous(); /*  named prv */

			max_pos = prv.getPosition() + prv.getLength() + ru_stopdistance /*SBC: stopdistance support*/;
			
			if (max_pos < target_pos)
			{
				best_pos = target_pos;
			}
			else
			{
				System.out.println("How IDM return new position value: ru_pos - speed_left*dt" +
									" that collides with the front vehicle max_pos >= target_pos: " +
									" max_pos: " + max_pos +
									" target position: " + target_pos);/**/
				
				best_pos = max_pos;
			}
			li.next();
			//System.out.println("RU had previous, now bestpos ="+best_pos);
		}
		else
		{
			best_pos = target_pos;
		}

		li.next();
		
		if(best_pos != ru_pos) 
		{ 
			/* has no car in front, advance to your best pos */
			// The Roaduser can advance some positions
			ru.setPosition(best_pos);
			return (speed_left*dt - (ru_pos - best_pos));
		}
		else
		{ 
			/* best_pos == ru_pos, or, you cant move. */
			return speed_left*dt;
		}
	}

	/** POMDPGLD
	 * Moves the supplied roaduser on it's present lane as far as it can go to the best reacheable free position on the Queue given in the ListIterator
	 *
	 * @param li The Queue of this Drivelane represented in a ListIterator. li.previous() is the current RoadUser
	 * @param ru The Roaduser to be moved
	 * @param speed_left the number of 'moves' this Roaduser has left in this turn
	 * @param lane The present lane
	 *  
	 * @return the number of blocks the Roaduser can move ahead
	 */
	protected double moveRoaduserOnLane(ListIterator li, ObservedRoaduser ru, double speed_left /*EJUST: int --> double*/, Drivelane lane)
	{
		double ru_pos = ru.getPos(); /*EJUST: int --> double*/
		int ru_len = ru.getRoaduser().getLength();

		//SBC, EJUST: int --> double
		double ru_stopdistance = 0; /*EJUST commented: ru.getStopDistance()*/

		/*EJUST: int --> double*/
		double best_pos = ru_pos;
		
		/*EJUST: int --> double*/
		double max_pos = ru_pos;
		
		/*EJUST: int --> double*/
		double target_pos = (ru_pos - speed_left*dt > 0) ? ru_pos - speed_left*dt : 0; 
		
		int waitsteps;
		//System.out.println("Targetpos:"+target_pos+" and hasPrev:"+li.hasPrevious());

		// Previous should be 'ru'
		ObservedRoaduser prv = (ObservedRoaduser)li.previous();

		if(prv == ru && li.hasPrevious())
		{
			/* has car in front */
			prv = (ObservedRoaduser)li.previous(); /*  named prv */
			double prv_pos = prv.getPos(); /*EJUST: int --> double*/
			max_pos = prv_pos + prv.getRoaduser().getLength()  + ru_stopdistance; // SBC: stopdistance support;
			if(max_pos < target_pos)
			{
				best_pos = target_pos;
			}
			else
			{
				best_pos = max_pos;
			}
			li.next();
			//System.out.println("RU had previous, now bestpos ="+best_pos);
		}
		else
		{
			best_pos = target_pos;
		}

		li.next(); // Setting the ListIterator back in the position we got it like.
		
		if(best_pos != ru_pos) 
		{
			/* has no car in front, advance to your best pos */
			// The Roaduser can advance some positions
			ru.setPos(best_pos);
			return(speed_left*dt - (ru_pos - best_pos));
		}
		else
		{
			/* best_pos == ru_pos, or, you cant move. */
			return speed_left*dt;
		}
	}


	protected PosMov[] calcPosMovs(Node node, Sign sign, Drivelane lane, Roaduser ru, ListIterator li)
	{
		// =======================================
		// Calculating the ranges per drivelane to where roaduser could get to
		// =======================================
		double ru_pos = ru.getPosition(); /*EJUST: int --> double*/
		double ru_speed = ru.getSpeed(); /*EJUST: int --> double*/
		
		int ru_len = ru.getLength();
		int ru_type = ru.getType();
		int ru_des = ru.getDestNode().getId();

		Vector<PosMov> vPosMovs = new Vector<PosMov>();
		int tlId = sign.getId();

		// Get the position closest to the Sign the RU can reach
		double bestPos = lane.getPosFree(li, ru_pos, ru_len, ru_speed, ru); /*EJUST: int --> double*/
		for (double z = ru_pos; z >= bestPos; z--) /*EJUST: int --> double*/
		{   
			/*EJUST: 
			 * This part is used by the controller so we descreatize the location by decrementing 1
			 * from the current roaduser position till reaching the best position
			 * 
			 * When calculating the possible next positions the roaduser can reach within a specific lane 
			 * we descreatize the locations from the current roaduser position 
			 * till the best position the roaduser can reach according to its speed*/
			
			vPosMovs.addElement(new PosMov(sign /*EJUST: tlId --> sign */, z));
		}

		/*EJUST: int --> double*/
		double speedLeft = ru_speed - ru_pos/dt; // ru_pos as that is the number of units to be moven to the Sign

		// Now figure out the other possible lanes
		if(bestPos <= STOP_SPEED_MS*dt /*EJUST replaced: bestPos==0*/ 
				&& speedLeft > STOP_SPEED_MS /*EJUST replaced: speedLeft > 0*/)
		{
			Drivelane[] possiblelanes = node.getShortestPaths(ru_des, ru_type);
			int lanes = possiblelanes.length;
			for (int j = 0; j < lanes; j++)
			{
				// For each possible lane
				Drivelane testLane = possiblelanes[j];

				if (testLane.isLastPosFree(ru_len))
				{
					bestPos = -1;
					speedLeft = speedLeft > ru_len/dt ? speedLeft - ru_len/dt : 0;
					int worstPos = testLane.getCompleteLength() - ru_len;
					int tltlId = testLane.getId();

					// We kunnen ervanuitgaan dat we nooit 'echt' op de drivelane springen
					// We kunnen wel 'naar' deze drivelane springen
					// dwz, de posities op de node tot max(lane.length-ru_len,lane.clength-speedleft) zijn vrij om te gaan

					/* We can assume that we never even drive on the lane to jump
					 * We can "to" drive the lane to jump
					 * say, the positions on the node to max (lane.length-ru_len, lane.clength-speed left) are free to go
					 * */
					bestPos = Math.max(testLane.getCompleteLength() - ru_len - speedLeft*dt, testLane.getLength() - ru_len);
					for (double k = worstPos; k >= bestPos; k--) /*EJUST: int --> double*/
					{
						vPosMovs.addElement(new PosMov(testLane.getSign() /*EJUST: tltlId --> testLane.getSign()*/, k));
					}
				}
			}
		}
		// Fuck it, we aint got the power to cross, so don't even bother calculating further..
		PosMov[] posMovs = new PosMov[vPosMovs.size()];
		vPosMovs.copyInto(posMovs);
		vPosMovs = null;

		return posMovs;
	}


	/*POMDPGLD*/
	protected PosMov[] calcPoPosMovs(Node node, Sign sign, Drivelane lane, ObservedRoaduser ru, ListIterator li)
	{
		// =======================================
		// Calculating the ranges per drivelane to where roaduser could get to
		// =======================================
		double ru_pos = ru.getPos(); /*EJUST: int --> double*/
		double ru_speed = ru.getSpeed(); /*EJUST: int --> double*/
		
		int ru_len = ru.getRoaduser().getLength();
		int ru_type = ru.getRoaduser().getType();
		int ru_des = 0;

		Vector vPosMovs = new Vector();
		int tlId = sign.getId();

		// Get the position closest to the Sign the RU can reach
		double bestPos = ((PODrivelanes)lane).getPosFree(li, ru_pos, ru_len, ru_speed, ru); /*EJUST: int --> double*/
		
		for(double z = ru_pos; z >= bestPos; z--) /*EJUST: int --> double*/
		{			
			/*EJUST: 
			 * This part is used by the controller so we descreatize the location by decrementing 1
			 * from the current road user position till reaching the best position*/
			
			vPosMovs.addElement(new PosMov(sign /*EJUST: tlId --> sign*/, z));
		}

		/*EJUST: int --> double*/
		double speedLeft = ru_speed - ru_pos/dt; // ru_pos as that is the number of units to be moven to the Sign

		// Now figure out the other possible lanes
		if(bestPos <= STOP_SPEED_MS*dt /*EJUST replaced: bestPos==0*/ 
			&& speedLeft > STOP_SPEED_MS /*EJUST replaced: speedLeft>0*/)
		{
			Drivelane[] possiblelanes = node.getShortestPaths(ru_des, ru_type);
			int lanes = possiblelanes.length;
			for(int j = 0; j < lanes; j++)
			{
				// For each possible lane
				Drivelane testLane = possiblelanes[j];

				if(testLane.isLastPosFree(ru_len))
				{
					bestPos = -1;
					speedLeft = speedLeft > ru_len/dt ? speedLeft - ru_len/dt : 0;
					int worstPos = testLane.getCompleteLength() - ru_len;
					int tltlId = testLane.getId();

					// We kunnen ervanuitgaan dat we nooit 'echt' op de drivelane springen
					// We kunnen wel 'naar' deze drivelane springen
					// dwz, de posities op de node tot max(lane.length-ru_len,lane.clength-speedleft) zijn vrij om te gaan
					bestPos = Math.max(testLane.getCompleteLength() - ru_len - speedLeft*dt , testLane.getLength() - ru_len);
					for(double k = worstPos; k >= bestPos; k--) /*EJUST: int --> double*/
					{
						vPosMovs.addElement(new PosMov(testLane.getSign() /*EJUST: tltlId --> testLane.getSign()*/, k));
					}
				}
			}
		}
		// Fuck it, we aint got the power to cross, so don't even bother calculating further..
		PosMov[] posMovs = new PosMov[vPosMovs.size()];
		vPosMovs.copyInto(posMovs);
		vPosMovs = null;

		return posMovs;
	}


	/** New road users are placed on the roads when necessary. 
	 * When roads are full, new road users are queued.
	 */
	public void spawnNewRoadusers() throws InfraException, ClassNotFoundException
	{
		SpecialNode[] specialNodes = infra.getSpecialNodes();
		LinkedList<Roaduser> wqueue;
		ListIterator<Roaduser> list;
		EdgeNode edge;
		Roaduser r;
		int num_edges = specialNodes.length;
		int total_queue = 0;

		MicroModelGLD idm; /*EJUST*/
		LaneChangeGLD laneChange; /*EJUST*/

		for (int i = 0; i < num_edges; i++)
		{
			
			if (!(specialNodes[i] instanceof EdgeNode))
			{
				break;
			}
			else
			{
				edge = (EdgeNode) (specialNodes[i]);
			}
			
			boolean placed = false;
			wqueue = edge.getWaitingQueue(); //Returns the queue with waiting road users for this node
			int wqsize = wqueue.size();
			list = wqueue.listIterator();

			while (list.hasNext())
			{
				total_queue++;
				r = (Roaduser) list.next();
				if (placeRoaduser(r, edge))
				{
					//EJUST comment: When the road user is placed on the given edge node, 
					// remove it from the waiting road users queue 
					list.remove();
				}
			}

			SpawnFrequency[] freqs = edge.getSpawnFrequencies();
			DestFrequency[][] destfreqs = edge.getDestFrequencies();
			int num_freqs = freqs.length;
			int cur_index;
			int[] freqIndexes = new int[num_freqs];

			for (int nrs = 0; nrs < num_freqs; nrs++)
			{
				freqIndexes[nrs] = nrs; //Shuffle the indexes
			}

			Arrayutils.randomizeIntArray(freqIndexes, generator);

			for (int j = 0; j < num_freqs; j++)
			{
				//First try to place new road users on the road.
				cur_index = freqIndexes[j];
				if (freqs[cur_index].freq >=0 /*EJUST*/ ? 
						freqs[cur_index].freq >= generator.nextFloat(): 
							(int)freqs[cur_index].lastGeneratedRandomNumber == 1 ? true: /*EJUST*/
								freqs[cur_index].timeStepOfLastArrival+(int)Math.ceil(freqs[cur_index].lastGeneratedRandomNumber) 
								== curTimeStep /*EJUST*/)
				{
					int ruType = freqs[cur_index].ruType;
					int weatherCondition = freqs[cur_index].weatherCondition; /*EJUST*/
					
					/* Spawn road user of type freqs[i].ruType to a random destination.
					 * When all drivelanes are full the road users are queued.			*/
					SpecialNode dest = getRandomDestination(specialNodes, edge, ruType, destfreqs);

				
					/*EJUST*/						
					idm = getIDMType(ruType, weatherCondition);
										
					laneChange = (ruType == RoaduserFactory.CAR)? inconsiderate : (ruType == RoaduserFactory.BUS)? polite : cute;
					
					r = RoaduserFactory.genRoaduser(ruType, edge, dest, 0, idm /*EJUST*/, laneChange /*EJUST*/);
					infra.generatedRoadusersIncrement();//EJUST Statistics
					edge.generatedRoadusersIncrement();//EJUST Statistics
					r.setDrivelaneStartTime(curTimeStep);
					//System.out.println("curTimeStep:	"+ curTimeStep);


					if (freqs[cur_index].freq >=0 /*EJUST*/){
						freqs[cur_index].lastGeneratedRandomNumber = 1; /*EJUST*/
					}
					else { /*EJUST*/
						freqs[cur_index].timeStepOfLastArrival = curTimeStep; /*EJUST*/
						try {
							freqs[cur_index].lastGeneratedRandomNumber = 
								generateRandomNumber(freqs[cur_index].distributionType, freqs[cur_index].paramValue);
						} catch (DistributionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} /*EJUST*/
					}

					// Add R in queue if there is no place
					if (!placeRoaduser(r, edge))						
					{
						/*EJUST: 
						 * Old Scenario: 
						 * When the road user can not be placed on the given edge node, 
						 * insert it into the waiting road users queue if maximum waiting road users is not reached yet.
						 * 
						 * New Scenario:
						 * Set the Maximum Allowed Number of Waiting Vehicles to 0, 
						 * as it is unreasonable to have two queues one inside the lane and one outside the lane (i.e., in the edge node).
						 * 
						 * We track the number of rejected road users (the ones that are generated but not entered the city 
						 * and will not enter anymore) and track these rejected vehicles happen in which edge nodes of the network.
						 */
						
						/*if(wqsize < 375) {
						 list.add(r);
						 wqsize++;
						 } else {
						 break;
						 }*/
						if (total_queue < SimController.getMaxRuWaitingQueue())
						{
							list.add(r);
							wqsize++;
						}
						else
						{
							break;
						}
					}
				}
			}
		}

		setCurNumWaiting(total_queue);
		if (total_queue >= SimModel.LOCK_THRESHOLD /*EJUST comment: SimController.getMaxRuWaitingQueue()*/)
		{
			locked = true;
		}
	}

	/**
	 * @param ruType
	 * @param weatherCondition
	 * @return idm
	 * @author EJUST
	 */
	protected MicroModelGLD getIDMType(int ruType, int weatherCondition) {
		MicroModelGLD idm;
		idm = (ruType == RoaduserFactory.CAR)? 
				(weatherCondition == WeatherFactory.DRY)?idmCar:
				(weatherCondition == WeatherFactory.LIGHT_RAIN)?idmCarLightRain:
				(weatherCondition == WeatherFactory.NORMAL_RAIN)?idmCarNormalRain:
				(weatherCondition == WeatherFactory.HEAVY_RAIN)?idmCarHeavyRain:
				(weatherCondition == WeatherFactory.LIGHT_FOG)?idmCarLightFog:
				(weatherCondition == WeatherFactory.HEAVY_FOG)?idmCarHeavyFog:idmCarSandstorm						
			:(ruType == RoaduserFactory.BUS)?
				(weatherCondition == WeatherFactory.DRY)?idmBus:
				(weatherCondition == WeatherFactory.LIGHT_RAIN)?idmBusLightRain:
				(weatherCondition == WeatherFactory.NORMAL_RAIN)?idmBusNormalRain:
				(weatherCondition == WeatherFactory.HEAVY_RAIN)?idmBusHeavyRain:
				(weatherCondition == WeatherFactory.LIGHT_FOG)?idmBusLightFog:
				(weatherCondition == WeatherFactory.HEAVY_FOG)?idmBusHeavyFog:idmBusSandstorm
			 :(weatherCondition == WeatherFactory.DRY)?idmBicycle:
				(weatherCondition == WeatherFactory.LIGHT_RAIN)?idmBicycleLightRain:
				(weatherCondition == WeatherFactory.NORMAL_RAIN)?idmBicycleNormalRain:
				(weatherCondition == WeatherFactory.HEAVY_RAIN)?idmBicycleHeavyRain:
				(weatherCondition == WeatherFactory.LIGHT_FOG)?idmBicycleLightFog:
				(weatherCondition == WeatherFactory.HEAVY_FOG)?idmBicycleHeavyFog:idmBicycleSandstorm;
		return idm;
	}

	/*EJUST*/
	private double generateRandomNumber(int distributionType, ParameterValue[] paramValue) throws DistributionException {		
		return DistributionFactory.generateRandomNumber(distributionType,paramValue);
	}

	/** A road user is placed on the given edge node. When road is full the ru is queued */
	private boolean placeRoaduser(Roaduser r, SpecialNode edge)
	{
		Drivelane found = findDrivelaneForRU(r, edge);
		if (found == null)
		{
			return false;
		}
		else
		{
			// There is room for me!
			try
			{
				//System.out.println("Adding RU with type:"+r.getType()+" to lane:"+found.getSign().getId()+" going to Node:"+found.getNodeLeadsTo().getId()+" at pos:"+found.getNodeLeadsTo().isConnectedAt(found.getRoad())+" with type:"+found.getType());
				found.addRoaduserAtEnd(r, found.getLength() - r.getLength());
				r.addDelay(curTimeStep - r.getDrivelaneStartTime());
				r.setDrivelaneStartTime(curTimeStep);
							
				r.setTripStartTime(curTimeStep); //EJUST
				r.setSpeed(r.model().Veq(found.getLength() - r.getLength() )); //EJUST
				//System.out.println("delay	"+ (curTimeStep - r.getDrivelaneStartTime()) +"	curTimeStep:	"+ curTimeStep);
				infra.enteredCarsIncrement();//DOAS 06 Statistics blahhh
				edge.enteredCarsIncrement(); /*EJUST*/
				return true;
			}
			catch (Exception e)
			{
				return false;
			}
		}
	}

	private Drivelane findDrivelaneForRU(Roaduser r, SpecialNode e)
	{
		SpecialNode dest = (SpecialNode) r.getDestNode();
		Drivelane[] lanes = (Drivelane[]) e.getShortestPaths(dest.getId(), r.getType()).clone();
		Arrayutils.randomizeArray(lanes);
		int num_lanes = lanes.length;
		for (int i = 0; i < num_lanes; i++)
		{
			if (lanes[i].isLastPosFree(r.getLength()))
			{
				return lanes[i];
			}
		}
		//System.out.println("Couldnt place RU");
		return null;
	}

	/** Get a completely random destination, don't choose moi*/
	public SpecialNode getRandomDestination(SpecialNode moi) throws InfraException
	{
		SpecialNode[] dests = infra.getSpecialNodes();
		if (dests.length < 2)
		{
			throw new InfraException("Cannot choose random destination. Not enough special nodes.");
		}
		SpecialNode result;
		while (moi == (result = dests[(int) (generator.nextFloat() * dests.length)]))
		{
			;
		}
		return result;
	}

	/*Choose a destination*/
	private SpecialNode getRandomDestination(SpecialNode[] dests, SpecialNode here, int ruType, DestFrequency[][] destfreqs)
	{
		//GASTON: ESTA FUNCION DECIDE EL DESTINO. HABR?A QUE CAMBIAR destfreqs para influir en la decisi?n
		//destIds: va a tener los ids de los posibles nodos destino
		/*destfreqs: es una matriz con las frecuencias(una fila por cada nodo de tipo EdgeNode, 1 columna por cada ruType (RoadUserType(ej: auto)) ). 
		Cada nodo tiene una matriz destfreqs indicando las frecuencias hacia cada nodo destino posible

		// GASTON: this decides the destination. WOULD CHANGE destfreqs to influence the decision
		// destIds: going to have the ids of the possible destination nodes
		/ * destfreqs: a matrix of frequencies (one row for each node type EdgeNode, 1 column for each ruType (RoadUserType (eg, self))).
		Each node has a matrix indicating the frequency destfreqs to each possible destination node*/

		int[] destIds = here.getShortestPathDestinations(ruType);
		float choice = generator.nextFloat();
		float total = 0f;

		/*All frequencies are between 0 and 1, but their total can be greater than 1*/
		for (int i = 0; i < destIds.length; i++)
		{	
			//sumo todas las frecuencias (entre todos los nodos destino posible) para el ruType correspondiente
			for (int j = 0; j < destfreqs[i].length; j++)
			{
				if (destfreqs[destIds[i]][j].ruType == ruType)
				{
					total += destfreqs[destIds[i]][j].freq;
				}
			}
		}

		float sumSoFar = 0f;
		int j = 0;
		int index = 0;
		boolean foundIndex = false;
		while (j < destIds.length && !foundIndex)
		{
			//ac? se termina de decidir a qu? nodo destino se ir?
			for (int i = 0; i < destfreqs[j].length; i++)
			{
				if (destfreqs[destIds[j]][i].ruType == ruType)
				{
					float now = (destfreqs[destIds[j]][i].freq) / total;
					if (now + sumSoFar >= choice)
					{
						foundIndex = true;
						index = j;
					}
					else
					{
						sumSoFar += now;
					}
				}
			}
			j++;
		}
		return dests[destIds[index]];
	}

	/*Get a random index out of the lanes*/
	private int getRandomLaneNr(Drivelane[] lanes)
	{
		int ind = (int) Math.floor(generator.nextFloat() * (lanes.length));
		while (ind != lanes.length)
		{
			ind = (int) Math.floor(generator.nextFloat() * (lanes.length));
		}
		return ind;
	}

	/*EJUST*/
	public void newValues (double p_factor, double deltaB) {
		inconsiderate.set_p(p_factor);
		inconsiderate.set_db(deltaB);
		polite.set_p(p_factor);
		cute.set_p(p_factor);
	} // newValues

	// change externally IDM parameters ONLY for cars
	public void changeIDMCarParameters(IDMGLD idm){
		((IDMGLD)(idmCar)).set_params(idm);
	}

	// change externally IDM parameters ONLY for buses
	public void changeIDMBusParameters(IDMGLD idm){
		((IDMGLD)(idmBus)).set_params(idm);
	}

	// change externally IDM parameters ONLY for bicycles
	public void changeIDMBicycleParameters(IDMGLD idm){
		((IDMGLD)(idmBicycle)).set_params(idm);
	}
	
	// Light Rain: change externally IDM parameters ONLY for cars
	public void changeIDMCarParametersLightRain(IDMGLD idm){
		((IDMGLD)(idmCarLightRain)).set_params(idm);
	}

	// Light Rain: change externally IDM parameters ONLY for buses
	public void changeIDMBusParametersLightRain(IDMGLD idm){
		((IDMGLD)(idmBusLightRain)).set_params(idm);
	}

	// Light Rain: change externally IDM parameters ONLY for bicycles
	public void changeIDMBicycleParametersLightRain(IDMGLD idm){
		((IDMGLD)(idmBicycleLightRain)).set_params(idm);
	}

	// Normal Rain: change externally IDM parameters ONLY for cars
	public void changeIDMCarParametersNormalRain(IDMGLD idm){
		((IDMGLD)(idmCarNormalRain)).set_params(idm);
	}

	// Normal Rain: change externally IDM parameters ONLY for buses
	public void changeIDMBusParametersNormalRain(IDMGLD idm){
		((IDMGLD)(idmBusNormalRain)).set_params(idm);
	}

	// Normal Rain: change externally IDM parameters ONLY for bicycles
	public void changeIDMBicycleParametersNormalRain(IDMGLD idm){
		((IDMGLD)(idmBicycleNormalRain)).set_params(idm);
	}

	// Heavy Rain: change externally IDM parameters ONLY for cars
	public void changeIDMCarParametersHeavyRain(IDMGLD idm){
		((IDMGLD)(idmCarHeavyRain)).set_params(idm);
	}

	// Heavy Rain: change externally IDM parameters ONLY for buses
	public void changeIDMBusParametersHeavyRain(IDMGLD idm){
		((IDMGLD)(idmBusHeavyRain)).set_params(idm);
	}

	// Heavy Rain: change externally IDM parameters ONLY for bicycles
	public void changeIDMBicycleParametersHeavyRain(IDMGLD idm){
		((IDMGLD)(idmBicycleHeavyRain)).set_params(idm);
	}

	// Light Fog: change externally IDM parameters ONLY for cars
	public void changeIDMCarParametersLightFog(IDMGLD idm){
		((IDMGLD)(idmCarLightFog)).set_params(idm);
	}

	// Light Fog: change externally IDM parameters ONLY for buses
	public void changeIDMBusParametersLightFog(IDMGLD idm){
		((IDMGLD)(idmBusLightFog)).set_params(idm);
	}

	// Light Fog: change externally IDM parameters ONLY for bicycles
	public void changeIDMBicycleParametersLightFog(IDMGLD idm){
		((IDMGLD)(idmBicycleLightFog)).set_params(idm);
	}

	// Heavy Fog: change externally IDM parameters ONLY for cars
	public void changeIDMCarParametersHeavyFog(IDMGLD idm){
		((IDMGLD)(idmCarHeavyFog)).set_params(idm);
	}

	// Heavy Fog: change externally IDM parameters ONLY for buses
	public void changeIDMBusParametersHeavyFog(IDMGLD idm){
		((IDMGLD)(idmBusHeavyFog)).set_params(idm);
	}

	// Heavy Fog: change externally IDM parameters ONLY for bicycles
	public void changeIDMBicycleParametersHeavyFog(IDMGLD idm){
		((IDMGLD)(idmBicycleHeavyFog)).set_params(idm);
	}

	// Sandstorm: change externally IDM parameters ONLY for cars
	public void changeIDMCarParametersSandstorm(IDMGLD idm){
		((IDMGLD)(idmCarSandstorm)).set_params(idm);
	}

	// Snowy Sticking: change externally IDM parameters ONLY for buses
	public void changeIDMBusParametersSandstorm(IDMGLD idm){
		((IDMGLD)(idmBusSandstorm)).set_params(idm);
	}

	// Snowy Sticking: change externally IDM parameters ONLY for bicycles
	public void changeIDMBicycleParametersSandstorm(IDMGLD idm){
		((IDMGLD)(idmBicycleSandstorm)).set_params(idm);
	}

	/*EJUST*/

	/**
	 *
	 * The second thread that runs the simulation.
	 *
	 * @author Joep Moritz
	 * @version 1.0
	 */
	public class SimModelThread extends Thread
	{
		/** Is the thread suspended? */
		private volatile boolean suspended;
		/** Is the thread alive? If this is set to false, the thread will die gracefully */
		private volatile boolean alive;
		
		/** The time in milliseconds this thread sleeps after a call to doStep() */
		private int sleepTime = 250; /*POMDPGLD makes this value 0 "Light Speed" */
		/*EJUST: change this value 100-->250 milliseconds as TIMESTEP_S  = 0.25*/

		/** Returns the current sleep time */
		public int getSleepTime()
		{
			return sleepTime;
		}

		/** Sets the sleep time */
		public void setSleepTime(int s)
		{
			sleepTime = s;
		}

		/**
		 * Starts the thread.
		 */
		public SimModelThread()
		{
			alive = true;
			suspended = true;
		}

		/**
		 * Suspends the thread.
		 */
		public synchronized void pause()
		{
			suspended = true;
		}

		/**
		 * Resumes the thread.
		 */
		public synchronized void unpause()
		{
			suspended = false;
			notify();
		}

		/**
		 * Stops the thread. Invoked when the program exitst.
		 * This method cannot be named stop().
		 */
		public synchronized void die()
		{
			alive = false;
			interrupt();
		}

		/**
		 * Returns true if the thread is not suspended and not dead
		 */
		public boolean isRunning()
		{
			return!suspended && alive;
		}

		/**
		 * Invokes Model.doStep() and sleeps for sleepTime milliseconds
		 */

		public void run()
		{
			while (alive)
			{
				try
				{
					sleep(sleepTime);
					synchronized (this)
					{
						while (suspended && alive)
						{
							wait();
						}
					}
					doStep();
					//					} catch (InterruptedException e) { }
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("Exception: " + e.getMessage());
					locked = true;
				}
			}
		}
	}

	/*POMDPGLD*/
	public void printProgressBar(int length, int maxSteps, int curTimeStep) {
		// ProgressBar ****
		if(((int)Math.floor(maxSteps / length)) == 0)
		{
			int maxDots = (int)Math.ceil(maxSteps / length);
			int minDots = (int)Math.floor(maxSteps / length);
			int numDots = 0;

			if(curTimeStep % 2 == 0)
			{
				int rest = 48 -	(((int)Math.ceil(curTimeStep / 2)) * maxDots + ((int)Math.floor(curTimeStep / 2)) * minDots);
				numDots = (rest < maxDots) ? rest : maxDots;
			}
			else
			{
				int rest = 48 - (((int)Math.floor(curTimeStep / 2)) * maxDots + ((int)Math.ceil(curTimeStep / 2)) * minDots);
				numDots = (rest < minDots) ? rest : minDots;
			}

			for(int p = 0; p < numDots; p++)
			{
				System.out.print(".");
			}
		}
		else if(((SimController)controller).consoleApplication &&
				curTimeStep % ((int)Math.floor(maxSteps / length)) == 0)
		{
			System.out.print(".");
		}
		// END progressbarr
	}
	/*POMDPGLD*/

	// Some XMLSerializable stuff

	public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
	{
		super.load(myElement, loader);
		setInfrastructure(infra);
		Dictionary loadDictionary;
		try
		{
			loadDictionary = infra.getMainDictionary();
		}
		catch (InfraException ohNo)
		{
			throw new XMLInvalidInputException
			(
					"This is weird. The infra can't make a dictionary for the second " +
					"stage loader of the algorithms. Like : " + ohNo);
		}
		Dictionary infraDictionary = new Hashtable();
		infraDictionary.put("infra", infra);
		loadDictionary.put("infra", infraDictionary);
		boolean savedBySim = ("simulator").equals(myElement.getAttribute("saved-by").getValue());
		if (savedBySim)
		{
			thread.setSleepTime(myElement.getAttribute("speed").getIntValue());
			simName = myElement.getAttribute("sim-name").getValue();
			curTimeStep = myElement.getAttribute("current-timeStep").getIntValue();
			TLCFactory factory = new TLCFactory(infra);
			tlc = null;

			try
			{
				tlc = factory.getInstanceForLoad(factory.getNumberByXMLTagName(loader.getNextElementName()));
				loader.load(this, tlc);
				System.out.println("Loaded TLC " + tlc.getXMLName());
			}
			catch (InfraException e2)
			{
				throw new XMLInvalidInputException("Problem while TLC algorithm was processing infrastructure :" +e2);
			}
			tlc.loadSecondStage(loadDictionary);
			DPFactory dpFactory = new DPFactory(this, tlc);
			try
			{
				dp = dpFactory.getInstance(dpFactory.getNumberByXMLTagName(loader.getNextElementName()));
				loader.load(this, dp);
				System.out.println("Loaded DP " + dp.getXMLName());
			}
			catch (ClassNotFoundException e)
			{
				throw new XMLInvalidInputException("Problem with creating DP in SimModel." +"Could not generate instance of DP type :" + e);
			}
			dp.loadSecondStage(loadDictionary);
			loader.load(this, sgnctrl);
			sgnctrl.setTLC(tlc);
		}
		else
		{
			curTimeStep = 0;
		} while (loader.getNextElementName().equals("dispenser"))
		{
			loader.load(this, new NumberDispenser());
		}
	}

	public XMLElement saveSelf()
	{
		XMLElement result = super.saveSelf();
		result.addAttribute(new XMLAttribute("sim-name", simName));
		result.addAttribute(new XMLAttribute("saved-by", "simulator"));
		result.addAttribute(new XMLAttribute("speed", thread.getSleepTime()));
		result.addAttribute(new XMLAttribute("current-timeStep", curTimeStep));
		return result;
	}

	public void saveChilds(XMLSaver saver) throws IOException, XMLTreeException, XMLCannotSaveException
	{
		super.saveChilds(saver);
		System.out.println("Saving TLC " + tlc.getXMLName());
		saver.saveObject(tlc);
		System.out.println("Saving DP " + dp.getXMLName());
		saver.saveObject(dp);
		saver.saveObject(sgnctrl);
	}

	//GASTON: 
	protected void restartSpawnFreqs(){
		if (! hasRun)
		{	
			hasRun=true;
			initialize();
		}
		iFreqs = spawnFreqsList.iterator();
		nroFranja = 0;
	}


	//GASTON
	protected void updateSpawnFreqs(){

		if (! hasRun)
		{	
			hasRun=true;
			initialize();
		}

		HashMap spawnFreqs = null;

		//while (true) {
		try {

			System.out.println("Will change frequencies!!"); //Se van a cambiar las frecuencias
			if (!iFreqs.hasNext()){
				iFreqs = spawnFreqsList.iterator();
				nroFranja = 0;
			}
			nroFranja ++;
			System.out.println("Time Interval: " + nroFranja); //Franja Horaria
			spawnFreqs = (HashMap)iFreqs.next();


			Node[] nodos = infra.getAllNodes();
			int k = 0;
			for (int i=0;i<nodos.length;i++){

				if (nodos[i] instanceof EdgeNode){
					EdgeNode en = ((EdgeNode)nodos[i]);
					String[] freqs = (String[])spawnFreqs.get(Integer.toString(en.getId()));
					int distributionType = -1; //EJUST
					ParameterValue[] paramValue = null; /* EJUST: The value of each parameter */
					int weatherCondition = WeatherFactory.DRY; /*EJUST*/
					setSpawnFrequency(en,1,Float.parseFloat(freqs[0]), distributionType /*EJUST*/, paramValue/*EJUST*/, weatherCondition/*EJUST*/);
					setSpawnFrequency(en,2,Float.parseFloat(freqs[1]), distributionType /*EJUST*/, paramValue/*EJUST*/, weatherCondition/*EJUST*/);
					setSpawnFrequency(en,3,Float.parseFloat(freqs[2]), distributionType /*EJUST*/, paramValue/*EJUST*/, weatherCondition/*EJUST*/);
				}
				//we could reinit the statistics, so they are not influenced by the old behaviour
				/*anyway, we will not reinit them here, they will be reinitiated after we export the
				 * statistics of the current simulation
				 */ 
				/*
					nodos[i].initStats();
					Drivelane[] lanes = nodos[i].getAllLanes();
					for (int j = 0; j < lanes.length; j++) {
						Drivelane drivelane = lanes[j];
						drivelane.initStats();
					}*/
			}

		} catch (Exception e) {
			System.out.println("Threw an exception.");//Se lanz? una excepci?n
			e.printStackTrace();
		}
		
		infra.reset();
	}



	/**
	 * GASTON: Send the statistics to the DLC module.
	 */
	public void sendStatistics()
	{
		active = true;
		//new StatisticsController(this);
	}


	//GASTON:
	public class StatisticsSender extends Thread{

		SimModel model = null;


		boolean alive = true;


		public void run() {
			while (alive) {

				try {
					synchronized(this){
						while (!active)
							sleep(500);
					}

					//new StatisticsController(model,rb);
					active = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}


		public SimModel getModel() {
			return model;
		}

		/**
		 * @param b
		 */
		public void setAlive(boolean b) {
			alive = b;
		}

		/**
		 * @param model
		 */
		public void setModel(SimModel model) {
			this.model = model;
		}

	}

	/**
	 * @return
	 */
	public ResourceBundle getResourceBundle() {
		return rb;
	}

	/**
	 * @param bundle
	 */
	public void setResourceBundle(ResourceBundle bundle) {
		rb = bundle;
	}
}