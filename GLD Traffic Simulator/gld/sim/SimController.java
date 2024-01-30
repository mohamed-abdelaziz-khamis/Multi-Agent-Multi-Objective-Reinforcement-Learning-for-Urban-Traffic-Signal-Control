
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

import gld.Controller;
import gld.GLDException;
import gld.GLDSim;
import gld.GLDToolBar;
import gld.InvalidFilenameException;
import gld.Model;
import gld.PopupException;
import gld.Selectable;
import gld.algo.dp.DPFactory;
import gld.algo.tlc.Colearning;
import gld.algo.tlc.HECinterface;
import gld.algo.tlc.TC1TLCOpt;
import gld.algo.tlc.TLCFactory;
import gld.algo.tlc.TLController;
import gld.config.POOptionsFactory;
import gld.config.SimGlobalSpawnratePanel;
import gld.config.SimIDMPanel;
import gld.config.StuckCarsDialog;
import gld.distributions.DistributionFactory;
import gld.distributions.ParameterValue;
import gld.edit.EditController;
import gld.edit.EditModel;
import gld.edit.Validation;
import gld.idm.WeatherFactory;
import gld.infra.Drivelane;
import gld.infra.DrivelaneFactory;
import gld.infra.EdgeNode;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Junction;
import gld.infra.LessSimpleInfra;
import gld.infra.NetTunnelTest1;
import gld.infra.NetTunnelTest2;
import gld.infra.Node;
import gld.infra.SimpleInfra;
import gld.infra.SpawnFrequency;
import gld.sim.stats.StatisticsController;
import gld.sim.stats.StatisticsOverlay;
import gld.sim.stats.TrackerFactory;
import gld.sim.stats.TrackingController;
import gld.sim.stats.TrackingView;
import gld.sim.sync.SyncSignalReceiver;
import gld.sim.sync.SyncSignalReceiverFactory;
import gld.sim.sync.SynchronizationProtocolKeys;
import gld.utils.Arrayutils;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLTreeException;
import gld.xml.XMLUtils;

import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.MenuBar;
import java.awt.PopupMenu;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.Vector;

/**
 * The main controller for the simulation part of the application.
 *
 * @author Group GUI
 * @version 1.0
 */

public class SimController extends Controller implements Observer
{
	protected EditController editController = null;
	protected SimMenuBar menuBar;


	public static final String[] speedTexts = { "Low", "Medium", "High", "Maximum", "Light Speed" };
	public static final int[] speedSettings = { 250 /*EJUST replaced: 1000*/, 50/*EJUST replaced: 400*/, 25/*EJUST replaced: 50*/, 10, 0 };

	protected Choice speedChoice;
	protected StatisticsOverlay statsOverlay;

	protected boolean quitAfterSeries = false;  // Quit after series of experiments (DOAS 06)

	protected boolean hecAddon; //(DOAS 05)
	protected boolean accidents = false; // (DOAS 06) 	By default on. This must correspond to the SimMenuBar setting.
	/*POMDPGLD: true --> false*/
	protected boolean rerouting = false; //(DOAS 06)
	/*POMDPGLD: true --> false*/

	/*POMDPGLD*/
	private String logPath = "log";

	// DOAS 06: Removal of stuck cars.
	protected boolean removeStuckCars = false;
	protected int maxWaitingTime = 20; /*EJUST comment: Max Waiting Time before stuck cars removal*/
	protected int penalty = 0;
	protected StuckCarsDialog stuckCarsDialog;

	/*POMDPGLD*/
	protected SimGlobalSpawnratePanel simGlobalSpawnratePanel;

	protected static int maxRuWaitingQueue = 0;//EJUST commented = SimModel.LOCK_THRESHOLD;
	
	/*EJUST:
	 * Set the Maximum Allowed Number of Waiting Vehicles to 0, 
	 * as it is unreasonable to have two queues one inside the lane and one outside the lane (i.e., in the edge node). 
	 * 
	 * There exists a problem that the waiting road users in the edge nodes do not share in 
	 * the Average Trip Waiting Time performance index as they did not enter the network and consequently did not complete a trip.
	 * 
	 * So, the waiting road users outside the lane (queued in the edge nodes) should be rejected 
	 * 
	 * We track the number of rejected road users (the ones that are generated but not entered the city and will not enter anymore) 
	 * and track these rejected vehicles happen in which edge nodes of the network.
	 * */

	//Gaston
	protected SimModelSpawnFreqsUpdate threadSpawnFreq;
	protected LinkedList spawnFreqsList = null;
	//protected HashMap spawnFreqsHM = null;

	/*POMDPGLD*/
	protected boolean consoleApplication = false;
	protected boolean popups = true;
	protected boolean resume = false;


	/*EJUST*/
	protected SimIDMPanel simIDMPanel;

	/**
	 * Creates the main frame.
	 *
	 * @param m The <code>SimModel</code> to be controlled.
	 */
	public SimController(SimModel m, boolean splash) 
	{
		super(m, splash);

		/*POMDPGLD*/
		this.consoleApplication = consoleApplication;
		setVisible(!consoleApplication);
		controllerInit(m);

	}

	public void controllerInit(SimModel m)
	{
		setSimModel(m);
		m.setSimController(this);

		speedChoice = new Choice();
		Enumeration e = Arrayutils.getEnumeration(speedTexts);
		while(e.hasMoreElements())
		{
			speedChoice.add((String)(e.nextElement()));
		}

		setSpeed((int)(speedTexts.length / 2));
		setTimeStepCounterEnabled(true);

		statsOverlay = new StatisticsOverlay(view, m.getInfrastructure());

		setTLC(0, 0);
		setDrivingPolicy(0);

	}

	/*============================================*/
	/* GET and SET methods                        */
	/*============================================*/

	/** Returns the current <code>SimModel</code> */
	public SimModel getSimModel() { return (SimModel)model; }

	public static int getMaxRuWaitingQueue() { return maxRuWaitingQueue; }

	public static void setMaxRuWaitingQueue(int num) { maxRuWaitingQueue = num; }

	/** Sets a new <code>SimModel</code> to be controlled */
	public void setSimModel(SimModel m) { model = m; }

	/** Enables or disables the timeStep counter. */
	public void setTimeStepCounterEnabled(boolean b)
	{	
		if(b)
		getSimModel().addObserver(this);
		else {
			setStatus("TimeStep counter disabled at timeStep " + getSimModel().getCurTimeStep() + ".");
			getSimModel().deleteObserver(this);
		}
	}

	//(DOAS 06)
	public void setQuitAfterSeries(boolean value){
		quitAfterSeries = value;
	}

	//(DOAS 05)
	public void setHecAddon(boolean b)
	{
		hecAddon = b;
		try
		{
			HECinterface tlc = (HECinterface)((SimModel) model).getTLController();
			tlc.setHecAddon(b, this);
		}
		catch(Exception e)
		{}

	}

	//(DOAS 05)
	public boolean getHecAddon()
	{
		return hecAddon;
	}


	//Set the accidents mode (DOAS 06)
	public void setAccidents(boolean b)
	{
		accidents = b;
	}

	//Are the accidents ON?(DOAS 06)
	public boolean getAccidents()
	{
		return accidents;
	}

	// DOAS 06 set the removal of stuck cars.
	public void setRemoveStuckCars(boolean b)
	{
		setRemoveStuckCars(b, false);
	}

	// DOAS 06 set the removal of stuck cars.
	public void setRemoveStuckCars(boolean b, boolean quiet){
		if (b && !quiet)
		{
			stuckCarsDialog = new StuckCarsDialog(this);
			stuckCarsDialog.setVisible(true);
		}

		removeStuckCars = b;
	}


	// DOAS 06 do we remove stuck cars?
	public boolean getRemoveStuckCars()
	{
		return removeStuckCars;
	}

	// DOAS 06 set how long a car may wait
	public void setMaxWaitingTime(int time)
	{
		maxWaitingTime = time;
	}

	// DOAS 06 how long may a car wait before it is stuck.
	public int getMaxWaitingTime()
	{
		return maxWaitingTime;
	}

	// DOAS 06 set a penalty for a removed car.
	public void setPenalty(int p)
	{
		penalty = p;
	}

	// DOAS 06 get a penalty for a removed car.
	public int getPenalty()
	{
		return penalty;
	}

	//Set the rerouting mode (DOAS 06)
	public void setRerouting(boolean on)
	{
		rerouting = on;
	}

	//Are we rerouting?(DOAS 06)
	public boolean getRerouting()
	{
		return rerouting;
	}

	/*POMDPGLD*/
	//(RM 06) Set the type of drivelanes we are going to use, convert drivelanes if nessecary
	public void setDrivelanetype(int type)
	{
		SimModel m = getSimModel();
		int oldtype = Model.dltype;
		try
		{
			m.reset();
			Model.dltype = type;
			m.getInfrastructure().convertAllLanes(m);
		}
		catch(SimulationRunningException e)
		{
			m.pause();
			setStatus(e.getMessage());
		}
		catch(InfraException e)
		{
			System.out.print(e.getMessage());
			Model.dltype = oldtype;
		}
		menuBar.dlMenu.setCurrentItem();
	}

	public int getDrivelaneType() {
		return Model.dltype;
	}

	public void setResumeOption(boolean b) {
		if(b) {
			System.out.println("Set Resume option on.");
		}
		resume = b;
	}

	public void setLogPath(String path)
	{
		logPath = path;
	}

	public void setPartiallyObservableLearning(boolean b)
	{
		getSimModel().usePO = b;
	}

	public void setGlobalSpawnrate(int timeStep/*EJUST*/, float freq,
									int distributionType /*EJUST*/, ParameterValue[] paramValue /*EJUST*/,
									int weatherCondition /*EJUST*/)
	{
		SimModel.globalSpawnrate = freq;
		SimModel.globalDistributionType = distributionType; /*EJUST*/
		SimModel.globalParamValue = paramValue; /*EJUST*/
		SimModel.globalWeatherCondition = weatherCondition; /*EJUST*/
		
		String spawnrates;
		if (freq >= 0)   //EJUST
			
			spawnrates =  new String("At time step " + timeStep + ": " + freq + 
					" ," + WeatherFactory.getWeatherConditionDescription(weatherCondition) /*EJUST*/);
		
		else	//EJUST 
		{
			String paramValues = "";
			
			for (int i=0; i < paramValue.length; i++)
				paramValues += "," + DistributionFactory.getParameterDescription(paramValue[i].parameterIndex) + "=" + paramValue[i].value;
			
			spawnrates =  new String("At time step " + timeStep + ": " + DistributionFactory.getDistributionTypeDescription(distributionType) + paramValues + 
					" ," + WeatherFactory.getWeatherConditionDescription(weatherCondition) /*EJUST*/);
		}
		
		/*EJUST commented
		setStatus("Updating all static spawnrates to: " + freq + "...");*/
		setStatus("Updating all dynamic spawnrates to: " + spawnrates + "...");
		
		EdgeNode[] edges = model.getInfrastructure().getEdgeNodes_();
		for(int i = 0; i < edges.length; i++)
		{
			SpawnFrequency[] freqs = edges[i].getSpawnFrequencies();
			for(int j = 0; j < freqs.length; j++)
			{
				if(freqs[j].freq > 0)
				{
					//EJUST commented static spawnrates
					 // model.setSpawnFrequency(edges[i], freqs[j].ruType, freq, distributionType /*EJUST*/, paramValue /*EJUST*/, weatherCondition /*EJUST*/);
					 
					//EJUST added dynamic spawnrates
					edges[i].addDSpawnTimeSteps(freqs[j].ruType, timeStep, freq, distributionType, paramValue, weatherCondition);
				}
			}
		}
		
		/*EJUST commented
		setStatus("Updated all static spawnrates to: " + freq);*/
		setStatus("Updated all dynamic spawnrates to: " + spawnrates);
		//System.out.println("Updated all static spawnrates to: " + freq);
		
	}   
	/*POMDPGLD*/   

	/*EJUST*/
	public void deleteGlobalDSpawnTimeSteps(int timeStep)
	{
		SimModel model = getSimModel();
		EdgeNode[] edges = model.getInfrastructure().getEdgeNodes_();
		for(int i = 0; i < edges.length; i++)
		{
			SpawnFrequency[] freqs = edges[i].getSpawnFrequencies();
			for(int j = 0; j < freqs.length; j++)
			{
				edges[i].deleteDSpawnTimeSteps(freqs[j].ruType, timeStep);
			}
		}
	}   
	  
	/*EJUST*/
	public Vector globalDSpawnTimeSteps()
	{		
		SimModel model = getSimModel();
		EdgeNode[] edges = model.getInfrastructure().getEdgeNodes_();
		SpawnFrequency[] freqs = edges[edges.length-1].getSpawnFrequencies();
		return edges[edges.length-1].dSpawnTimeStepsForRu(freqs[freqs.length-1].ruType);
	}
	
	/*============================================*/
	/* Load and save                              */
	/*============================================*/


	public void load(XMLElement myElement,XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
	{	
		super.load(myElement,loader);
		// TODO restore menu options/choices in GUI
		statsOverlay = new StatisticsOverlay(view,getSimModel().getInfrastructure());
		if (XMLUtils.getLastName(statsOverlay).equals(loader.getNextElementName()))
		{	
			System.out.println("Loading stats");
			loader.load(this, statsOverlay);
		}
	}

	public XMLElement saveSelf() throws XMLCannotSaveException{	
		
		XMLElement result = super.saveSelf();
		/* This code is buggy
			result.addAttribute(new XMLAttribute("saved-by", "simulator"));
		 	result.addAttribute(new XMLAttribute("tlc-category", menuBar.getTLCMenu().getCategory()));
			result.addAttribute(new XMLAttribute("tlc-number", menuBar.getTLCMenu().getTLC()));
			result.addAttribute(new XMLAttribute("driving-policy", menuBar.getDPMenu().getSelectedIndex()));
			result.addAttribute(new XMLAttribute("speed", speedChoice.getSelectedIndex()));
		 */
		return result;
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ 	
		saver.saveObject(statsOverlay);
	}

	public void doSave(String filename) throws InvalidFilenameException, Exception
	{	
		if(!filename.endsWith(".sim") )
			throw new InvalidFilenameException("Filename must have .sim extension.");
		setStatus("Saving simulation to " + filename);
		XMLSaver saver=new XMLSaver(new File(filename));
		saveAll(saver,getSimModel());
		saver.close();
		setStatus("Saved simulation to " + filename);
	}

	public void doLoad(String filename) throws InvalidFilenameException, Exception
	{	
		if(!filename.endsWith(".infra") && !filename.endsWith(".sim"))
			throw new InvalidFilenameException("You can only load .infra and .sim files.");
		stop();

		//Gaston
		//new WaitSynchronizationSignal().start();

		TrackerFactory.purgeTrackers();
		XMLLoader loader=new XMLLoader(new File(filename));
		loadAll(loader,getSimModel());
		newInfrastructure(model.getInfrastructure());
		loader.close();

		//GASTON: AGREGUE ESTE CODIGO PARA CARGAR TODOS LOS NODOS DISPONIBLES CUANDO CARGA LA RED EL GLDSIM
		//Add this code to load all available nodes NETWORK LOAD WHEN THE GLDSIM
		Infrastructure infra = model.getInfrastructure();
		Node[] nodos = infra.getAllNodes();
		for (int i=0;i<nodos.length;i++){
			if (nodos[i] instanceof Junction){
				((Junction)nodos[i]).updateAllAvailableRoads();
			}
		}

		/*POMDPGLD*/
		model.dltype = DrivelaneFactory.getType(model.getInfrastructure().getAllInboundLanes());
		menuBar.dlMenu.select(model.dltype);
	}


	//Gaston
	public class WaitSynchronizationSignal extends Thread implements SynchronizationProtocolKeys{

		private String sessionId = null;
		private boolean alive = true;
		public void run(){
			HashMap signalInfo = null;
			ResourceBundle rb = getSimModel().getResourceBundle();
			SyncSignalReceiver receiver = SyncSignalReceiverFactory.getSignalReceiver(rb);

			while(alive)
			{
				try {
					signalInfo = receiver.getSignalInfo(sessionId,rb);
					if (threadSpawnFreq != null){
						System.out.println("Preparing to re-synchronize");
						threadSpawnFreq.destroy();
					}
					getSimModel().restartSpawnFreqs();
					threadSpawnFreq = new SimModelSpawnFreqsUpdate();
					long interval = Long.parseLong((String)signalInfo.get(INTERVAL));
					if (sessionId == null)
						sessionId = (String)signalInfo.get(SESSIONID);
					if (interval > 0)
						threadSpawnFreq.setSleep_time(interval);
					else
						System.out.println("Error in the information received by the Synchronization signal");
					threadSpawnFreq.start();		


				} catch (NumberFormatException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}			
		}
	}

	//Gaston
	public class SimModelSpawnFreqsUpdate extends Thread
	{
		protected long sleep_time = 0;
		protected boolean alive = true;
		public SimModelSpawnFreqsUpdate(){
		
		}

		public void run( ) {
			while (alive){
				try {
					getSimModel().updateSpawnFreqs();
					sleep(sleep_time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		/**
		 * @return
		 */
		public long getSleep_time() {
			return sleep_time;
		}

		/**
		 * @param i
		 */
		public void setSleep_time(long i) {
			sleep_time = i;
		}

		public void destroy(){
			alive = false;
		}
	}


	/*	Iterator iFreqs = spawnFreqsList.iterator();
				HashMap spawnFreqs = null;
				int nroFranja = 0;
				while (true) {
					try {

						System.out.println("Se levanto el Thread!!");
						if (!iFreqs.hasNext()){
							iFreqs = spawnFreqsList.iterator();
							nroFranja = 0;
						}
						nroFranja ++;
						System.out.println("Franja Horaria: " + nroFranja);
						spawnFreqs = (HashMap)iFreqs.next();

						Infrastructure infra = model.getInfrastructure();
						Node[] nodos = infra.getAllNodes();
						int k = 0;
						for (int i=0;i<nodos.length;i++){
							if (nodos[i] instanceof EdgeNode){
								EdgeNode en = ((EdgeNode)nodos[i]);
								String[] freqs = (String[])spawnFreqs.get(Integer.toString(en.getId()));
								((SimModel)model).setSpawnFrequency(en,1,Float.parseFloat(freqs[0]));
								((SimModel)model).setSpawnFrequency(en,2,Float.parseFloat(freqs[1]));
								((SimModel)model).setSpawnFrequency(en,3,Float.parseFloat(freqs[2]));
							}
						}
						sleep(30000);
					} catch (Exception e) {
						System.out.println("Se lanz� una excepci�n");
						e.printStackTrace();
					}
				}		
			}*/

	/*============================================*/
	/* Miscellanous                               */
	/*============================================*/

	/** Called by observable SimModel (if view enabled). */
	public void update(Observable o, Object arg)
	{
		SimModel model = (SimModel)o;
		int timeStep      = model.getCurTimeStep();
		int numWaiting = model.getCurNumWaiting();
		int curSeries  = model.getCurSeries(); // DOAS 06

		/*POMDPGLD*/
		int maxWaiting = (model.runCalibrationSeries) ? model.maxWaitingAllowed : maxRuWaitingQueue;                              

		if (timeStep!=0) {
			
			String status = "Time step: " + timeStep + ", Num Roadusers waiting to enter town: " + numWaiting + 
			" max(" + maxWaiting /*POMDPGLD: maxRuWaitingQueue*/ + 
			") seed:" + GLDSim.seriesSeed[GLDSim.seriesSeedIndex];
			
			// DOAS 06: Series information in status bar added
			if (model.runSeries && /*Added by POMDPGLD*/ curSeries > 0) 
				status = "Series: " + curSeries + " of " + model.getNumSeries() + ", " + status;

			/*POMDPGLD*/
			else if(model.runCalibrationSeries && curSeries > 0)
			{
				status = "Calibration Series: " + curSeries + " of " + model.numCalSeries +
				", Current spawnrate: " + model.currentSpawnrate + ", " + status;
			}

			if(model.dltype == DrivelaneFactory.PO_DRIVELANE)
			{
				status = status + " Cur / Avg / Max beliefStateSize: ( " +
				model.currentBeliefStateSize + " / " + model.averageBeliefStateSize + "/ " + model.maxBeliefStateSize + " )";
				int c = (int)(model.currentBeliefStateProb * 100);
				int a = (int)(model.averageBeliefStateProb * 100);
				int m = (int)(model.minBeliefStateProb * 100);
				status = status + " %: ( " + c + "% / " + a + "% / " + m + "% )";
			}
			/*POMDPGLD*/                    

			setStatus(status);
		}
	}

	/** Returns the name of this controller extension. */
	protected String appName() { return "simulator"; }

	protected MenuBar createMenuBar() {
		menuBar = new SimMenuBar(this, speedTexts);
		return menuBar;
	}

	protected GLDToolBar createToolBar() {
		return new SimToolBar(this);
	}


	/*============================================*/
	/* Invoked by Listeners                       */
	/*============================================*/

	/**
	 * Opens the statistics viewer.
	 */
	public void showStatistics()
	{	
		new StatisticsController(getSimModel(), this);
	}

	/**
	 * Shows the tracking window.
	 */
	public void showTracker(int type)
	{	
		try	{TrackerFactory.showTracker(getSimModel(), this, type);	}
		catch(GLDException e) {	reportError(e.fillInStackTrace());		}
	}

	/** Enables the statistics overlay */
	public void enableOverlay() {
		statsOverlay = new StatisticsOverlay(view, getSimModel().getInfrastructure());
		getSimModel().addObserver(statsOverlay);
		view.addOverlay(statsOverlay);
	}

	/** Enables the statistics overlay */
	public void disableOverlay() {
		getSimModel().deleteObserver(statsOverlay);
		view.remOverlay(statsOverlay);
	}

	/*POMDPGLD*/
	/**
	 * Opens the config tool for a global spawnrate (RM 06)
	 */
	public void showSpawnrateConfig()
	{
		if(simGlobalSpawnratePanel == null)
		{
			simGlobalSpawnratePanel = new SimGlobalSpawnratePanel((SimModel)model);
		}
		simGlobalSpawnratePanel.setVisible(true);
	}

	public void setNoiseMethod(int noiseMethod)
	{
		setPOoption(0, noiseMethod);
		menuBar.poMenu.setNoiseMethod(noiseMethod);
	}

	public void setBeliefstateType(int beliefstateType)
	{
		setPOoption(1, beliefstateType);
		menuBar.poMenu.setBeliefState(beliefstateType);
	}

	public void setDrivingBehaviour(int driving_behaviour)
	{
		setPOoption(2, driving_behaviour);
		menuBar.poMenu.setRuBehaviour(driving_behaviour);
	}

	public void setUseAllRoadusers(boolean use_Roadusers)
	{
		getSimModel().useAllRoadusers = use_Roadusers;
	}

	public void togglePopups(boolean popups)
	{
		this.popups = popups;
	}
	/*POMDPGLD*/

	public void setDrivingPolicy(int dp)
	{	
		try	{	
			getSimModel().setDrivingPolicy((new DPFactory(getSimModel(), getSimModel().getTLController())).getInstance(dp));	
		}
		catch (Exception e)	{	
			reportError(e);	
		}
	}

	public void setTLC(int cat, int nr)
	{	
		setColearningEnabled(cat == 1);
		try {
			SimModel sm = getSimModel();
			TLCFactory tlcf = new TLCFactory(sm.getInfrastructure(), sm.getRandom());
			TLController tlc = tlcf.genTLC(cat, nr);
			tlc.showSettings(this);
			sm.setTLController(tlc);
			setColearningEnabled((tlc instanceof Colearning));
			setHecAddon(getHecAddon());
		}
		catch (GLDException e) {
			reportError(e.fillInStackTrace());
		}
	}

	/*POMDPGLD*/
	public void setPOoption(int cat, int nr)
	{
		//SimModel sm = getSimModel();
		//stop();

		POOptionsFactory.setOption(cat, nr);
	}

	private void setColearningEnabled(boolean b) {
		if (!b && menuBar.getDPMenu().getSelectedIndex() == DPFactory.COLEARNING) 
		{
			menuBar.getDPMenu().select(DPFactory.SHORTEST_PATH);
			setDrivingPolicy(DPFactory.SHORTEST_PATH);
		}
		((CheckboxMenuItem)menuBar.getDPMenu().getItem(DPFactory.COLEARNING)).setEnabled(b);
	}

	/*POMDPGLD*/
	public void use_SCOMDP(boolean b) {
		SimModel.use_SCOMDP = b;
	}

	public void setSCOMDPVersion(int version) {
		SimModel.SCOMDP_version = version;
	}


	public void use_FSR(boolean b) {
		SimModel.use_fixed_speed_randomizer = b;
	}
	/*POMDPGLD*/


	/** Shows the file properties dialog */
	public void showFilePropertiesDialog()
	{
		String simName = getSimModel().getSimName();
		Infrastructure infra = getSimModel().getInfrastructure();
		String comments = infra.getComments();

		SimPropDialog propDialog = new SimPropDialog(this, simName, comments);

		propDialog.show();
		if(propDialog.ok())	{
			getSimModel().setSimName(propDialog.getSimName());
			infra.setComments(propDialog.getComments());
		}
		this.setStatus("Simulation \"" + getSimModel().getSimName() + "\".");
	}

	/** Creates a right-click popup-menu for the given object */
	public PopupMenu getPopupMenuFor(Selectable obj) throws PopupException {
		SimPopupMenuFactory pmf = new SimPopupMenuFactory(this);
		return pmf.getPopupMenuFor(obj);
	}

	/** Returns the filename of the currently loaded file */
	public String getCurrentFilename() {
		return currentFilename;
	}

	/** Sets the speed of the simulation */
	public void setSpeed(int speed) {
		((SimToolBar)toolBar).getSpeed().select(speed);
		menuBar.getSpeedMenu().select(speed);
		getSimModel().setSpeed(speedSettings[speed]);
	}

	/** Makes model do one step */
	public void doStep() { getSimModel().doStep(); }

	/** GASTON: CUTS THE TRAFFIC */
	public void disableTraffic(Drivelane lane) { 
		this.pause();
		getSimModel().disableTraffic(lane); 
		if (!validateInfra()){
			enableTraffic(lane);
		}
		this.unpause();
	}

	/** GASTON: ALLOW TRAFFIC */
	public void enableTraffic(Drivelane lane) {
		this.pause();
		getSimModel().enableTraffic(lane);
		if (!validateInfra()){
			disableTraffic(lane);
		}
		this.unpause();
	}

	/** Pause the simulation */
	public void pause() {
		setStatus("Paused at time step " + getSimModel().getCurTimeStep() + ".");
		getSimModel().pause();
	}

	/** Resumes or starts the simulation */
	public void unpause() {
		setStatus("Simulation running.");
		getSimModel().unpause();
	}

	/** Stops the simulation and resets the infrastructure */
	public void stop() {
		int timeStep=getSimModel().getCurTimeStep() ;
		if (timeStep!=0)
			setStatus("Stopped at time step " + timeStep /*EJUST*/ + ".");
		try {
			getSimModel().pause();
			getSimModel().reset();
		}
		catch (SimulationRunningException ex) {
			reportError(ex.fillInStackTrace());
			getSimModel().unpause();
		}
	}

	/** Starts a series of 10 simulations */
	public void runSeries() {
		
		setStatus("Running a Series of simulations.");
		
		TrackerFactory.purgeTrackers();
		
		try	{
			
			//EJUST commented: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_QUEUE);
			//EJUST commented: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.BELIEFSTATE_SIZE); //POMDPGLD
			//EJUST, POMDPGLD commented: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_DELAY);
			
			/*EJUST: tracks the total trip waiting time experienced by all arrived roadusers*/
			//TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_WAITING_TIME);
			
			/*EJUST: tracks the total colearn trip waiting time experienced by all arrived and not arrived roadusers*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_COLEARN_WAITING_TIME);
			
			/*EJUST: tracks the total trip time experienced by all arrived roadusers*/
			//TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_TRIP_TIME);
			
			/*EJUST: tracks the total colearn trip time experienced by all arrived and not arrived roadusers*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_COLEARN_TRIP_TIME);
			
			/*EJUST comment: tracks the total number of roadusers that have arrived
			 * It represents the number of roadusers that entered and exited (throughput)*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_ROADUSERS); 
			
			/*EJUST: tracks the total number of roadusers that have not arrived yet
			 * It represents the number of roadusers that entered and not exit yet*/
			//TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_ROADUSERS_NOT_ARRIVED_YET); 
						
			/*EJUST: tracks the percentage of roadusers arrived as opposed to all road users entered
			 * It represents the relative throughput*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.ARRIVED_ROADUSERS_PERCENTAGE); 

			/*EJUST: tracks the percentage of roadusers rejected as opposed to all road users generated*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.REJECTED_ROADUSERS_PERCENTAGE); 
						
			//EJUST commented*: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_JUNCTION_DELAY);
			
			/*EJUST: tracks the total waiting time for all junction crossings*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_JUNCTION_WAITING_TIME);

			/*EJUST: tracks the total colearn waiting time for all junction crossings and roadusers not crossed yet*/
			//TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_COLEARN_JUNCTION_WAITING_TIME);
			
			/*EJUST: tracks the total average speed for all roadusers that have/haven't arrived yet*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_AVERAGE_SPEED);
			
			/*EJUST: tracks the total colearn average speed for all roadusers that have/haven't arrived yet*/
			//TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_COLEARN_AVERAGE_SPEED);
			
			//EJUST commented: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.AVERAGE_LANE_LOAD); //POMDPGLD
			//EJUST commented: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.MAX_LANE_LOAD); //POMDPGLD
			//EJUST commented: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.STATE_ZEROS); //POMDPGLD
			
			/*EJUST: tracks the average number of roadusers waiting for the Sign of all Drivelanes.*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.AVERAGE_LANE_NUM_ROADUSERS_WAITING);
			
			/*EJUST: tracks the maximum number of roadusers waiting for the Sign of all Drivelanes.*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.MAX_LANE_NUM_ROADUSERS_WAITING);
			
			/*EJUST: tracks the total trip absolute stops count experienced by all arrived roadusers*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_ABSOLUTE_STOPS_COUNT);
			
			/*EJUST: tracks the total trip stops count experienced by all arrived roadusers*/
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_STOPS_COUNT);
			
			//POMDPGLD commented: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.ACCIDENTS_COUNT); //DOAS 2006
			//POMDPGLD commented: TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.REMOVEDCARS_COUNT); //DOAS 2006
			
			if(popups) //POMDPGLD
			{
				TrackerFactory.disableTrackerViews();
			}
		}
		catch(GLDException e) {
			reportError(e.fillInStackTrace());
		}
		
		menuBar.setViewEnabled(false);
		menuBar.setTimeStepCounterEnabled(false);
		this.setViewEnabled(false);

		//DOAS 06 : show timeStep counter(set to true)
		this.setTimeStepCounterEnabled(true);
		
		/*EJUST: Make the simulator time the same as the model time
			sleepTime = 250; //POMDPGLD makes this value 0 "Light Speed"
			We change this value 100-->250 milliseconds as TIMESTEP_S = 0.25
		 */
		this.setSpeed((0)); 
		/*EJUST comment: 
		 * 0: "Low" i.e., sleepTime = 250 milliseconds 
		 * 1: "Medium" i.e., sleepTime = 100 milliseconds
		 * speedSettings.length-1: "Light Speed" i.e., sleepTime = 0 milliseconds 
		 * */
		
		getSimModel().runSeries();
	}

	/*POMDPGLD*/
	public void runCalibration()
	{
		TrackerFactory.purgeTrackers();
		try
		{
			if(model.dltype == DrivelaneFactory.PO_DRIVELANE)
			{
				TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.BELIEFSTATE_SIZE);
				TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.BELIEFSTATE_COVERAGE);
			}
			
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.TOTAL_QUEUE);
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.AVERAGE_LANE_LOAD);
			TrackerFactory.showTracker(getSimModel(), this, TrackerFactory.MAX_LANE_LOAD);
			if(popups)
			{
				TrackerFactory.enableTrackerViews();
			}
		}

		catch(GLDException e)
		{
			reportError(e.fillInStackTrace());
		}

		menuBar.setViewEnabled(false);
		this.setViewEnabled(false);
		this.setSpeed((speedSettings.length - 1));
		this.setTimeStepCounterEnabled(true);
		getSimModel().runCalibration();
	}

	/*POMDPGLD*/
	public void endCalibration(int timeStep/*EJUST*/, float foundCalibration, 
								int distributionType /*EJUST*/, ParameterValue[] paramValue /*EJUST*/,
								int weatherCondition /*EJUST*/)
	{
		if(quitAfterSeries)
		{
			System.out.println(">>>> Found Spawnrate: " + foundCalibration + " <<<<");
			quit();
		}
		getSimModel().stepSizeHalved = false;
		setStatus("Maximum spawnrate was: " + foundCalibration);
		setGlobalSpawnrate(timeStep/*EJUST*/, foundCalibration, distributionType /*EJUST*/, paramValue /*EJUST*/, weatherCondition /*EJUST*/);
		// stop the run
		getSimModel().pause();
		try
		{
			getSimModel().reset();
		}
		catch(SimulationRunningException e)
		{
			
		}
	}
	
	/*POMDPGLD*/
	public void nextCalibration()
	{
		int curSeries = getSimModel().getCurSeries();

		if(curSeries < getSimModel().numCalSeries)
		{
			// stop the run
			getSimModel().pause();
			try
			{
				getSimModel().reset();
			}
			catch(SimulationRunningException e)
			{
				
			}

			// set next seed for the series
			getSimModel().nextCurSeries();

			// begin this run
			getSimModel().unpause();
		}
		else
		{
			getSimModel().stopCalSeries();
		}
	}
	
	public void nextSeries() {
		getSimModel().pause();
		int curSeries = getSimModel().getCurSeries();

		/*POMDPGLD*/
//		boolean foundStartSerie = false;
//		boolean noSerieFound = false;
//		boolean skippedSeries = false;

		String logPath = "log";

		/*POMDPGLD*/
//		if(resume) {
//			// resume option is set, we have to check if the data files already exist for this run.
//			long seed = GLDSim.seriesSeed[GLDSim.seriesSeedIndex];
//			File logDir = new File(logPath);
//			if(logDir.exists() && logDir.isDirectory()) {
//				File[] files = logDir.listFiles();
//				if(curSeries == 0) {
//					getSimModel().nextCurSeries();
//					curSeries = getSimModel().getCurSeries();
//					seed = GLDSim.seriesSeed[GLDSim.seriesSeedIndex];
//				}
//
//				while(foundStartSerie == false) {
//					boolean nextSerie = false;
//					for(int i = 0; i < files.length; i++)
//					{
//						String filename = files[i].toString();
//						String match = "seed-"+Long.toString(seed)+"_";
//						if(!nextSerie && filename.indexOf(match) > 0)
//						{
//
//							System.out.println("Run " + curSeries + " previously completed, skipping...");
//							nextSerie = true;
//						}
//					}
//
//					if(curSeries >= getSimModel().getNumSeries()) {
//						// no more series...
//						noSerieFound = true;
//						foundStartSerie = true;
//					}
//					else if(nextSerie) {
//						skippedSeries = true;
//						getSimModel().nextCurSeries();
//						curSeries = getSimModel().getCurSeries();
//						seed = GLDSim.seriesSeed[GLDSim.seriesSeedIndex];
//					}
//					else {
//						noSerieFound = true;
//						foundStartSerie = true;
//					}
//				}
//			}
//		}

		// If we have data, save it
		if(curSeries > 0 /*&& !skippedSeries POMDPGLD*/) {
			String simName = getSimModel().getSimName();
			String tlcName = TLCFactory.getDescription(TLCFactory.getNumberByXMLTagName(XMLUtils.getLastName(getSimModel().getTLController().getXMLName())));
			String dpName  = DPFactory.getDescription(DPFactory.getNumberByXMLTagName(XMLUtils.getLastName(getSimModel().getDrivingPolicy().getXMLName())));
			TrackingController[] tca = TrackerFactory.getTrackingControllers();

			// DOAS 06:
			Date   date    = new Date();
			File   logDir  = new File(logPath);

			if (logDir.isFile())  logPath = "";   // log dir cannot be a file
			if (!logDir.exists()) logDir.mkdir(); // create log dir if doesn't exist


			/*POMDPGLD*/
//			if (getSimModel().dataFileName.compareTo("") != 0 && curSeries > 0) {
//				saveData(getSimModel().dataFileName + curSeries);
//			}


			for(int i=0;i<tca.length;i++) {
				TrackingView tv = tca[i].getTrackingView();
				String filename = logDir + "/"
							   /*EJUST commented + "seed-"    + GLDSim.seriesSeed[GLDSim.seriesSeedIndex]*/
				                                 + "_tlc-" + "MorevtsSotlPlatoon"//tlcName
				               /*EJUST commented + "_drivep-"  + dpName */
				                                 + "_view-"  + tv.getDescription()
				                                 + "_run-"   + curSeries
				               /*EJUST commented + "_acc-" + getAccidents() 
				                                 + "_stuck-" + (getRemoveStuckCars() ? ("" + getPenalty()) : "false") 
				                                 + "_rerout-" + getRerouting() 
				                                 + "_time-"  + date.getTime() */

				                                 + ".dat";
				
				try { 
					tv.saveData(filename,getSimModel()); 
				}
				catch(IOException exc) { 
					showError("Couldn't save statistical data from series!");
				}
			}
		}

		/*POMDPGLD*/
//		if( curSeries > 0 && !skippedSeries) {
//			System.out.println("\nCompleted run " + curSeries);
//		}
//
//
//		if( noSerieFound ) {
//			foundStartSerie = false;
//		}

		// If we have more runs to run, do so.
		if (curSeries < getSimModel().getNumSeries()) {
			setStatus("Running a series of simulations, currently at: "+curSeries);
			try { 
				getSimModel().reset(); 
			}
			catch(SimulationRunningException e) {
				
			}

			/*POMDPGLD*/
//			if( !skippedSeries ) {
//				getSimModel().nextCurSeries();
//				curSeries = getSimModel().getCurSeries();
//			}
//			setStatus("Running a series of simulations, currently at: " + curSeries);
//			System.out.println("Running a series of simulations, currently at: " + curSeries);

			getSimModel().nextCurSeries();
			getSimModel().unpause();
		}
		else {
			setStatus("Done running Series of simulations.");
			getSimModel().stopSeries();
			TrackerFactory.purgeTrackers();

			if(quitAfterSeries){
				quit();
			}
		}
	}

	/*POMDPGLD*/
	public void saveData(String filename)
	{

		ArrayList data = new ArrayList();
		data.add(((TC1TLCOpt)getSimModel().getTLController()).getCount());
		data.add(((TC1TLCOpt)getSimModel().getTLController()).getpTable());
		data.add(((TC1TLCOpt)getSimModel().getTLController()).getqTable());
		data.add(((TC1TLCOpt)getSimModel().getTLController()).getvTable());

		//serialize the List
		FileOutputStream file = null;
		ObjectOutputStream output = null;
		try
		{
			file = new FileOutputStream(filename);
			output = new ObjectOutputStream(file);
			output.writeObject(data);
		}
		catch(IOException exception)
		{
			System.err.println(exception);
		}
		finally
		{
			//flush and close all streams
			try
			{
				if(output != null)
				{
					//This method writes any buffered output to the underlying
					//output stream, flushes the stream, then closes it
					output.close();
				}
			}
			catch(IOException exception)
			{
				//tried our best
			}
		}
	}

	/*POMDPGLD*/
	public void loadData(String filename)
	{
		TC1TLCOpt tlc1 = (TC1TLCOpt)getSimModel().getTLController();
		FileInputStream inFile = null;
		ObjectInputStream input = null;
		try
		{
			inFile = new FileInputStream(filename);
			input = new ObjectInputStream(inFile);
			//deserialize the List
			ArrayList recoveredData = (ArrayList)input.readObject();
			//display its data
			tlc1.setCount((Vector[][][])recoveredData.get(0));
			tlc1.setpTable((Vector[][][])recoveredData.get(1));
			tlc1.setqTable((float[][][][])recoveredData.get(2));
			tlc1.setvTable((float[][][])recoveredData.get(3));
		}
		catch(IOException exception)
		{
			System.err.println(exception);
		}
		catch(ClassNotFoundException exception)
		{
			System.err.println(exception);
		}
		finally
		{
			//flush and close all streams
			try
			{
				if(input != null)
				{
					//closes the underlying input stream, which here is inFile
					input.close();
				}
			}
			catch(IOException exception)
			{
				//tried our best
			}
		}
	}


	/** Opens the editor */
	public void openEditor() {
		if (editController == null) editController = new EditController(new EditModel(), false);
		editController.show();
		editController.requestFocus();
	}

	/** Set temp debug infra */
	protected void setInfra(int nr)
	{	
		Infrastructure infra;
		switch (nr)
		{	
			case 1 : infra=new SimpleInfra(); break;
			case 2 : infra=new LessSimpleInfra(); break;
			case 3 : infra=new NetTunnelTest1(); break;
			case 4 : infra=new NetTunnelTest2(); break;
			default: infra=new Infrastructure(); break;
		}
		try
		{	
			Vector errors=(new Validation(infra)).validate();
			if(!errors.isEmpty())
				showError(errors.toString());
		}
		catch (InfraException e)
		{	
			reportError(e);
		}
		getSimModel().setCurTimeStep(0);
		newInfrastructure(infra);
	}

	/**EJUST*/
	/**
	 * Opens the config tool for an Intelligent Driver Model
	 */
	public void showIDMConfig() {
		if(simIDMPanel == null)
		{
			simIDMPanel = new SimIDMPanel((SimModel)model);
		}
		simIDMPanel.setVisible(true);
	}
}