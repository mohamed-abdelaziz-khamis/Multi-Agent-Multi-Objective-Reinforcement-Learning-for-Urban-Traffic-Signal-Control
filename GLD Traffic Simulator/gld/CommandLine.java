/*
 * CommandLine.java
 *
 * Created on January 27, 2006, 7:59 PM
 *
 */

package gld;

import gld.algo.dp.DPFactory;
import gld.algo.tlc.HECinterface;
import gld.algo.tlc.TC1TLCOpt;
import gld.algo.tlc.TLCFactory;
import gld.algo.tlc.TLController;
import gld.distributions.DistributionFactory;
import gld.distributions.ParameterValue;
import gld.idm.WeatherFactory;
import gld.infra.Beliefstate;
import gld.infra.DrivelaneFactory;
import gld.infra.ObservedQueue;
import gld.infra.Roaduser;
import gld.sim.SimController;
import gld.sim.SimModel;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author DOAS 06
 */
public class CommandLine {

	
	static int[][] allParameters = DistributionFactory.getDistributionParameters(); /*EJUST*/
	
	/** Creates a new instance of CommandLine */
	public CommandLine() {
	}

	/** Parses command line arguments into a hash map <name(string), value(string)>
	 * 
	 *  Arguments are expected to be in the form "name1=value1 name2=value2".
	 */
	private static HashMap processArguments(String[] args){
		HashMap arguments = new HashMap();

		for(int i = 0; i <  args.length; i++){
			String[] s = args[i].split("=");
			arguments.put(s[0].toLowerCase(), s[1]);
		}

		return arguments;
	}

	private static void printHelp(){
		System.err.println("This is a command line interface of the Green Light District simulator. " +
		"It runs a series of experiments and exits.");
		System.err.println("Parameters are supposed to be in the form 'name=value'");
		System.err.println("The following paramters are recognized:");
		System.err.println("\tinfra=path OBLIGATORY. Path to the file with infrastructure.");
		System.err.println("\tlog=path Path to the directory where the log output is stored."); /*POMDPGLD*/
		System.err.println("\ttlc=xml_name XML name of the traffic light controller to be used.");
		System.err.println("\tset_spawnrate=value set the spawnrate of all nodes to value"); /*POMDPGLD*/
		System.err.println("\tdistribution_type=name Name of the distribution type to be used."); /*EJUST*/
		System.err.println("\tparameter=value Value of the Parameter."); /*EJUST*/
		System.err.println("\tweather_condition=name Name of the weather condition to be used."); /*EJUST*/
		System.err.println("\thec=on|off Toggle HEC add-on on/off.");
		System.out.println("\taccidents=on|off Toggle accidents on/off.");
		System.err.println("\tremove_stuck_cars=on|off Toggle removing stuck cars on/off.");
		System.err.println("\tmax_waiting_time=value Threshold for stuck cars removing.");
		System.err.println("\tpenalty=value Penalty for removed stuck car.");
		System.err.println("\trerouting=on|off Toggle rerouting on/off.");
		System.err.println("\tdriving_policy=xml_name XML name of the driving policy.");


		/*POMDPGLD*/
		System.err.println();
		System.err.println("\tpo_lanes=on|off Toggle partially observed lanes on/off.");
		System.err.println("\tpo_learning=on|off Toggle partially observed learning on/off.");
		System.err.println("\tdriving_behaviour=value Driving behaviour: 0 for NOCHANGE, 1 for GAUSSIAN, 2 for UNIFORM, 3 for SPECIAL");
		System.err.println(
		"\tbeliefstate_type=value Used beliefstate_type: 0 for AIF, 1 for MLS, 2 for MLQ, 3 for QMDP");
		System.err.println(
		"\tnoise_method=value Noise method used: 0 for NOCHANGE, 1 for GAUSSIAN, 2 for UNIFORM");
		System.err.println(
		"\tscomdp=on|off to use SCOMDP controller instead of a COMDP, po_lanes has to be turned off.");
		System.err.println(
		"\tscomdp_version=value set the version of the SCOMDP controller. 1 or 2.");

		System.err.println(
		"\tfsr=on|off to use the Fixed Speed Randomizer option with the different controllers.");

		System.err.println(
		"\trun_series=on|off If we are running series true/false, default is true");
		System.err.println();
		/*POMDPGLD*/


		System.err.println("\tseries=value Number of series to be run.");
		System.err.println("\tseries_steps=value Number of timeSteps in one serie.");
		System.err.println();

		/*POMDPGLD*/
		System.err.println(
		"\tcalibrate_spawnrate=on|off If we need to calibrate the spawnrate true/false");

		System.err.println("\tcal_series=value Number of calibration series to be run.");
		System.err.println("\tcal_series_steps=value Number of time steps in one calibration serie.");
		System.err.println(
		"\tcal_max_waiting_ru=value The maximum roadusers that may wait to enter to keep running the serie.");
		System.err.println();
		System.err.println(
		"\tuse_popups=on|off toggles the use of statistical popups on or off, default is off");
		System.err.println(
		"\tshow_mainscreen=on|off toggles the use of the gui screen on or off, default is off");
		System.err.println(
		"\tresume_series=on|off toggles the option for resuming on or off, default is off");
		System.err.println(
		"\tuse_sensor_error=on|off toggles the option for sensor error on or off, default is off");
		System.err.println(
		"\tsensor_error=value sets the sensor error to value, default is 0.5");
		System.err.println();
		/*POMDPGLD*/

		System.err.println("On|off values may be also set to true|false.");
		System.err.println("Unrecognized parameters are passed to the traffic light controller.");
	}

	/** Returns true, if the parameter is set to "true" o "on".
	 */
	private static boolean isOn(String parameter){
		return parameter.equals("true") || parameter.equals("on");
	}

	public static void main(String[] args){
		SimModel simModel = new SimModel();		
		HashMap arguments = processArguments(args);
		
		/*POMDPGLD*/
        String showMain = (String)arguments.remove("show_mainscreen");
        boolean isConsoleAp = true;
        if(showMain != null)
        {
            isConsoleAp = (isOn(showMain) == false);
        }
		/*POMDPGLD*/
		
		
		SimController simController = new SimController(simModel, false);

		String infra = (String) arguments.remove("infra");
		if(infra != null){
			simController.tryLoad(infra);
		} else {
			printHelp();
			System.exit(1);
		}

        try  /*POMDPGLD*/
        {
            String use_popups = (String)arguments.remove("use_popups");
            if(use_popups != null && isOn(use_popups))
            {
                System.out.println("using popups");
                simController.togglePopups(true);
            }
            else
            {
                simController.togglePopups(false);
            }

            String allRoadusers = (String)arguments.remove("use_all_roadusers");
            if(allRoadusers != null)
            {
                simController.setUseAllRoadusers(isOn(allRoadusers));
                System.out.println("Using " +
                                   ((isOn(allRoadusers)) ? "all roadusers" :
                                    "only the potentially waiting roadusers") +
                                   " for decisions");
            }

            String poLanes = (String)arguments.remove("po_lanes");
            if(poLanes != null && isOn(poLanes))
            {
                simController.setDrivelanetype(DrivelaneFactory.PO_DRIVELANE);
                System.out.println("Converted lanes to support Partially observabilty");
            }

            String poLearning = (String)arguments.remove("po_learning");
            if(poLearning != null)
            {
                simController.setPartiallyObservableLearning(isOn(poLearning));
                System.out.println("Turned learning under partially observability " +  ((isOn(poLearning)) ? "on" : "off"));
            }

            String setSpawnRate = (String)arguments.remove("set_spawnrate");
            if(setSpawnRate != null)
            {
            	            	
            	int distributionIndex = -1; /* EJUST: The index of the distribution */
            	ParameterValue[] paramValue = {}; /* EJUST: The value of each parameter */
            	
            	if (Float.parseFloat(setSpawnRate)!=-1){ /*EJUST*/                	
                	System.out.println("Set global spawnrate to: " + setSpawnRate);
                }
                else /*EJUST*/{
                	String distributionName = (String)arguments.remove("distribution_type");
                	
                	String paramValues = "";
        			
        			String parameterDescription = "";
        			String parameterValue = "";        			     		
        			
        			if(distributionName != null){
        				distributionIndex = DistributionFactory.getDistributionTypeIdByName(distributionName);
        				paramValue = new ParameterValue[allParameters[distributionIndex].length];
        				
        				for (int i=0; i< paramValue.length; i++){
        					paramValue[i].parameterIndex = allParameters[distributionIndex][i];        					
        					parameterDescription = DistributionFactory.getParameterDescription(paramValue[i].parameterIndex);
        					parameterValue = (String)arguments.remove(parameterDescription);
        					if (parameterValue != null){
        						paramValue[i].value = Double.parseDouble(parameterValue);
        						paramValues += "," + parameterDescription + "=" + parameterValue;
        					}
        				}
        				System.out.println("Set global spawnrate to: " + distributionName + paramValues);
        			}        			
                }/*EJUST*/
            	
            	int weatherCondition = 0;
            	String weatherName = (String)arguments.remove("weather_condition"); /*EJUST*/            	
            	if(weatherName != null)
            		weatherCondition = WeatherFactory.getWeatherConditionIdByName(weatherName); /*EJUST*/
            	
                simController.setGlobalSpawnrate(0/*EJUST*/, Float.parseFloat(setSpawnRate), 
                		distributionIndex /*EJUST*/, paramValue /*EJUST*/,
                		weatherCondition /*EJUST*/);
            }
            
            String drivingBehaviour = (String)arguments.remove("driving_behaviour");
            if(drivingBehaviour != null)
            {
                switch(Integer.parseInt(drivingBehaviour))
                {
                    case 0:
                        simController.setDrivingBehaviour(Roaduser.NOCHANGE);
                        System.out.println("Set roaduser behaviour to nochange");
                        break;
                    case 1:
                        simController.setDrivingBehaviour(Roaduser.GAUSSIAN);
                        System.out.println("Set roaduser behaviour to gaussian");
                        break;
                    case 2:
                        simController.setDrivingBehaviour(Roaduser.UNIFORM);
                        System.out.println("Set roaduser behaviour to uniform");
                        break;
                    case 3:
                        simController.setDrivingBehaviour(Roaduser.SPETHIAL);
                        System.out.println("Set roaduser behaviour to 'special'");
                        break;

                    default:
                        System.err.println("Illegal argument for driving_behaviour: " + drivingBehaviour);
                        printHelp();
                        System.exit(1);
                }

            }

            String noiseMethod = (String)arguments.remove("noise_method");
            if(noiseMethod != null)
            {
                switch(Integer.parseInt(noiseMethod))
                {
                    case 0:
                        simController.setNoiseMethod(ObservedQueue.NOCHANGE);
                        System.out.println("Set noise_method to nochange");
                        break;
                    case 1:
                        simController.setNoiseMethod(ObservedQueue.GAUSSIAN);
                        System.out.println("Set noise_method to gaussian");
                        break;
                    case 2:
                        simController.setNoiseMethod(ObservedQueue.UNIFORM);
                        System.out.println("Set noise_method to uniform");
                        break;
                    default:
                        System.err.println("Illegal argument for noise_method: " + noiseMethod);
                        printHelp();
                        System.exit(1);
                }

            }

            String beliefstateType = (String)arguments.remove("beliefstate_type");
            if(beliefstateType != null)
            {
                switch(Integer.parseInt(beliefstateType))
                {
                    case 0:
                        simController.setBeliefstateType(Beliefstate.ALLINFRONT);
                        System.out.println("Set beliefstate_type to All in Front");
                        break;

                    case 1:
                        simController.setBeliefstateType(Beliefstate.MOSTLIKELYSTATE);
                        System.out.println("Set beliefstate_type to Most Likely State");
                        break;
                    case 2:
                        simController.setBeliefstateType(Beliefstate.MOSTLIKELYQUEUE);
                        System.out.println("Set beliefstate_type to Most Likely Queue");
                        break;
                    case 3:
                        simController.setBeliefstateType(Beliefstate.QMDP);
                        System.out.println("Set beliefstate_type to QMDP");
                        break;
                    default:
                        System.err.println("Illegal argument for beliefstate_type: " +  beliefstateType);
                        printHelp();
                        System.exit(1);
                }
            }

            String use_sensor_error = (String)arguments.remove("use_sensor_error");
            String sensor_err = (String)arguments.remove("sensor_error");

            if(use_sensor_error != null)
            {

                SimModel.use_sensor_noise = isOn(use_sensor_error);
                if(isOn(use_sensor_error) && sensor_err != null)
                {
                    SimModel.sensor_noise = Double.parseDouble(sensor_err);
                    System.out.println("Set sensor error to:" + SimModel.sensor_noise);
                }

            } /*POMDPGLD*/
            

			String tlcName = (String) arguments.remove("tlc");
			TLController tlc = null;
			if(tlcName != null){
				TLCFactory tlcFactory = new TLCFactory(simModel.getInfrastructure());

				tlc = tlcFactory.getInstanceForLoad(tlcFactory.getNumberByXMLTagName(tlcName) );

				tlc.loadArgs(arguments);
				System.out.println("Loaded TLC " + tlc.getXMLName()); /*POMDPGLD*/
				simModel.setTLController(tlc);
			}

			String hec = (String) arguments.remove("hec");
			if(hec != null){
				//simController.setHecAddon(isOn(hec));
				
                if(tlc instanceof HECinterface && isOn(hec)) /*POMDPGLD*/
                {
                    simController.setHecAddon(true); 
                    System.out.println("Loaded HEC addon"); 
                }
			}

			
			/*POMDPGLD*/
            String SCOMDP = (String)arguments.remove("scomdp");
            if(SCOMDP != null)
            {
                    if(isOn(SCOMDP) && simController.getDrivelaneType() != DrivelaneFactory.PO_DRIVELANE) {
                        System.out.println("Using Spread COMDP");
                        simController.use_SCOMDP(isOn(SCOMDP));
                    }
            }

            String SCOMDPversion = (String)arguments.remove("scomdp_version");
            if(SCOMDPversion != null)
            {
                if(Integer.parseInt(SCOMDPversion) > 1)
                {
                    System.out.println("Setting SCOMDP version to 2.");
                    simController.setSCOMDPVersion(2);
                }
            }


            String FSR = (String)arguments.remove("fsr");
            if(FSR != null)
            {
                    if(isOn(FSR)) {
                        System.out.println("Using Fixed Speed Randomizer for Roaduser Speed changes");
                        simController.use_FSR(isOn(FSR));
                    }
            }
			/*POMDPGLD*/
			
			
			String accidents = (String) arguments.remove("accidents");
			if(accidents != null){
				simController.setAccidents(isOn(accidents));
				System.out.println("Turned accidents " + ((isOn(accidents)) ? "on" : "off")); /*POMDPGLD*/
			}

			String removeStuckCars = (String) arguments.remove("remove_stuck_cars");
			if(removeStuckCars != null){
				simController.setRemoveStuckCars(isOn(removeStuckCars), true);
				System.out.println("Turned stuck car removal " +
                        ((isOn(removeStuckCars)) ? "on" : "off")); /*POMDPGLD*/
			}

			String maxWaitingTime = (String) arguments.remove("max_waiting_time");
			if(maxWaitingTime != null){
				simController.setMaxWaitingTime(Integer.parseInt(maxWaitingTime));
				System.out.println("Set maxwaiting type to: " + maxWaitingTime); /*POMDPGLD*/
			} 

			String penalty = (String) arguments.remove("penalty");
			if(penalty != null){
				simController.setPenalty(Integer.parseInt(penalty));
				System.out.println("Set penalty to:" + maxWaitingTime);  /*POMDPGLD*/
			}

			String rerouting = (String) arguments.remove("rerouting");
			if(rerouting != null){
				simController.setRerouting(isOn(rerouting));
				System.out.println("Turned rerouting " + ((isOn(rerouting)) ? "on" : "off")); /*POMDPGLD*/
			}

			String drivingPolicy = (String) arguments.remove("driving_policy");
			if(drivingPolicy != null){
				DPFactory dpFactory = new DPFactory(simModel, tlc);			
                int dp = dpFactory.getNumberByXMLTagName(drivingPolicy);
                simController.setDrivingPolicy(dp);
                System.out.println("Loaded DP " + dpFactory.getDescription(dp)); /*POMDPGLD*/
			}

			String series = (String) arguments.remove("series");
			if(series != null){
				simModel.setNumSeries(Integer.parseInt(series));
				System.out.println("Number of series: " + series); /*POMDPGLD*/
			}

			/*POMDPGLD*/
            String calSeries = (String)arguments.remove("cal_series");
            if(calSeries != null)
            {
                simModel.setCalNumSeries(Integer.parseInt(calSeries));
                System.out.println("Number of calibration series, per spawnrate adjustment: " +
                                   calSeries);
            }
            /*POMDPGLD*/
			
			String seriesSteps = (String) arguments.remove("series_steps");
			if(seriesSteps != null){
				simModel.setSeriesSteps(Integer.parseInt(seriesSteps));
				System.out.println("Number of time steps in one serie step: " + seriesSteps); /*POMDPGLD*/
			}

			
			/*POMDPGLD*/
            String calSeriesSteps = (String)arguments.remove("cal_series_steps");
            if(calSeriesSteps != null)
            {
                simModel.setSeriesSteps(Integer.parseInt(calSeriesSteps));
                System.out.println("Number of time steps in one calibration serie step: " +
                                   calSeriesSteps);
            }

            String calMaxWaitingRu = (String)arguments.remove("cal_max_waiting_ru");
            if(calMaxWaitingRu != null)
            {
                simModel.setCalMaxWaitingRu(Integer.parseInt(calMaxWaitingRu));
                System.out.println("Maximum amount of waiting cars per time step during calibration: " +
                                   calMaxWaitingRu);
            }

            String logPath = (String)arguments.remove("log");
            if(logPath != null)
            {
                simController.setLogPath(logPath);
                System.out.println("logPath set to: " + logPath);
            }

            String saveData = (String)arguments.remove("save_data_file");
            if(saveData != null && simModel.getTLController() instanceof TC1TLCOpt)
            {
                simModel.setDataFileName(saveData);
            }

            String resume = (String)arguments.remove("resume_series");
            if(resume != null)
            {
                simController.setResumeOption(isOn(resume));
            }

            String loadData = (String)arguments.remove("load_data_file");
            if(loadData != null && simModel.getTLController() instanceof TC1TLCOpt)
            {
                simController.loadData(loadData);
            }
			/*POMDPGLD*/
			
			
			simController.setQuitAfterSeries(true);

			
			/*POMDPGLD*/
            simController.setSpeed(4); // lightspeed...

            String runSeries = (String)arguments.remove("run_series");
            String calibrateSpawnrate = (String)arguments.remove("calibrate_spawnrate");
			/*POMDPGLD*/

            // POMDPGLD: any arguments that have not been removed are caught here.
			for(Iterator it = arguments.keySet().iterator(); it.hasNext(); ){
				System.err.println("Unknown parameter '" + it.next() + "'");
			}

            if(calibrateSpawnrate != null && isOn(calibrateSpawnrate)) /*POMDPGLD*/
            {
                System.out.println("Starting Calibration");
                simController.runCalibration();
            }
            else /*POMDPGLD*/
            {
                if(runSeries == null || isOn(runSeries))
                {
                    System.out.println("Starting to run a series");
                    simController.runSeries();
                }
                else if(isConsoleAp)
                {
                    System.err.print("Its set to be an console application, yet there isnt an action set.");
                    System.err.print("Use option show_mainscreen=on for the interactive gui with predifined options.");
                    printHelp();
                    System.exit(1);
                }
                else
                {
                    simController.getSimModel().reset();
                }
            }
        }
        catch(Exception e){
			e.printStackTrace();
			System.err.println("Sorry, something bad happened.");
		}
	}
}
