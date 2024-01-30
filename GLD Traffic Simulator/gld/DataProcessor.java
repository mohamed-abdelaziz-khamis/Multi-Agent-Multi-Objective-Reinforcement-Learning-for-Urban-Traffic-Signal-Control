package gld;

import java.io.*;
import java.util.*;

/**
 * 
 * @author secools
 *  - collect the waiting times of different simulations and export the
 * avg value for every timeStep to file
 *  - collect data from different avg-files and put them together in one file,
 * seperated with a tab
 */
public class DataProcessor {
	private static int recordCount = 901;

	private static int simCount = 5;

	private static String[] output = new String[recordCount];

	private static String[] waitingOutput = null;

	private static float[] avgValues = new float[recordCount];

	private static int[] avgWaiting = null;

	/**
	 * get the value out af the record and return it as float
	 * 
	 * @return the value in float-type
	 */
	private static float getValue(String record, int tokenNr) {
		StringTokenizer st = new StringTokenizer(record);
		float value;
		for (int i = 1; i < tokenNr; i++) {
			st.nextToken();
		}
		value = Float.parseFloat(st.nextToken());
		return value;
	}

	/**
	 * @param bpath
	 *            path to file (base name)
	 *  - collect the values of the waitingfiles (simCount files) -
	 * sum of corresponding values in array - divide by simCount
	 * 
	 * will set avgValues
	 * 
	 */
	private static void collectWaitingToAvg(String bpath) {
		String record = null;

		// reset avgValues
		for (int i = 0; i < avgWaiting.length; i++) {
			avgWaiting[i] = 0;
		}
		// collect values
		for (int i = 1; i <= simCount; i++) {
			String path = bpath + "_" + i + ".txt";
			// System.out.println("Collecting file "+path);
			try {
				BufferedReader br = new BufferedReader(new FileReader(path));

				for (int j = 0; j < 2; j++) {
					br.readLine();
				}

				for (int k = 0; k < avgWaiting.length; k++) {
					if ((record = br.readLine()) != null) {
						avgWaiting[k] += getValue(record, 3);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // end for (i)

		// normalize
		for (int i = 0; i < avgWaiting.length; i++) {
			avgWaiting[i] = avgWaiting[i] / simCount;
		}
		System.out.println("collectWaitingToAvg done, normalized");
	}

	/**
	 * @param bpath
	 *            path to file (base name)
	 *  - collect the values of the files (simCount files) - sum of
	 * corresponding values in array - divide by simCount
	 * 
	 * will set avgValues
	 * 
	 */
	private static void collectValuesToAvg(String bpath) {
		String record = null;
		// reset avgValues
		for (int i = 0; i < avgValues.length; i++) {
			avgValues[i] = 0f;
		}
		// collect values
		for (int i = 1; i <= simCount; i++) {
			String path = bpath + "_" + i + ".txt";
			// System.out.println("Collecting file "+path);
			try {
				BufferedReader br = new BufferedReader(new FileReader(path));

				for (int j = 0; j < 9; j++) {
					br.readLine();
				}

				for (int k = 0; k < avgValues.length; k++) {
					if ((record = br.readLine()) != null) {
						avgValues[k] += getValue(record, 2);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // end for (i)

		// normalize
		for (int i = 0; i < avgValues.length; i++) {
			avgValues[i] = avgValues[i] / simCount;
		}
	}

	/**
	 * will fill output from given files with their path in paths
	 * 
	 * @param bpaths
	 */
	private static void prepareFile(String[] bpaths) {
		// reset output
		for (int i = 0; i < output.length; i++) {
			output[i] = "";
		}

		// calculate avg values of all files
		for (int i = 0; i < bpaths.length; i++) {
			collectValuesToAvg(bpaths[i]);
			// put avg values to output
			for (int j = 0; j < avgValues.length; j++) {
				output[j] += avgValues[j];
				output[j] += "\t";
			}
		}
	}

	/**
	 * will fill waitingOutput from given file
	 * 
	 * @param bpaths
	 */
	private static void prepareWaitingFile(String[] bpaths) {
		// set lenght arrays
		int linecntr = 0;
		boolean more = true;
		try {
			BufferedReader br = new BufferedReader(new FileReader(bpaths[0]
					+ "_1.txt"));

			for (int j = 0; j < 2; j++) {
				br.readLine();
			}

			while (more) {
				if ((br.readLine()) != null)
					linecntr += 1;
				else
					more = false;

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		avgWaiting = new int[linecntr];
		waitingOutput = new String[linecntr];

		System.out.println("prepareWaitingFile: init done, linecntr: "
				+ linecntr);
		// reset waitingOutput
		for (int i = 0; i < waitingOutput.length; i++) {
			waitingOutput[i] = "";
		}

		// calculate avg waiting values
		for (int i = 0; i < bpaths.length; i++) {
			collectWaitingToAvg(bpaths[i]);
			for (int j = 0; j < avgWaiting.length; j++) {
				waitingOutput[j] += avgWaiting[j];
				waitingOutput[j] += "\t";
			}
		}
	}

	/**
	 * will write output to file with given path
	 * 
	 * @param path
	 */
	private static void writeToFile(String path) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(new File(path)));
			for (int i = 0; i < output.length; i++) {
				out.println(output[i]);
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeWaitingAvgToFile(String path) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(new File(path)));
			for (int i = 0; i < waitingOutput.length; i++) {
				out.println(waitingOutput[i]);
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] params) {
		String[] map = { 
				"E:\\laatstesim_map1hd_phase\\map1_2nodes_hd"//,
		// "E:\\Wetstraat\\map3_wetstraat_01",
		// "E:\\Wetstraat\\map3_wetstraat_02-03-04",
		// "E:\\Wetstraat\\map3_wetstraat_05",
		// "E:\\Wetstraat\\map3_wetstraat_06-12",
		// "E:\\Wetstraat\\platoonPhaseMin\\map3_wetstraat_07"//,
		// "E:\\Wetstraat\\map3_wetstraat_08-09"//,
		// "E:\\Wetstraat\\map3_wetstraat_10-17-18",
		// "E:\\Wetstraat\\map3_wetstraat_11-15-16",
		// "E:\\Wetstraat\\map3_wetstraat_13-14-19",
		// "E:\\Wetstraat\\map3_wetstraat_20",
		// "E:\\Wetstraat\\map3_wetstraat_21-22",
		// "E:\\Wetstraat\\map3_wetstraat_23",

		};

		String[] tlController = {/*"sotl-request",*/ "sotl-phase", "sotl-platoon" };
		int[] tlControllerId = { 4             , 5           , 6 };
		int[][] tetas = {
				//{}, // request
				{60, 70, 80, 90, 100, 110}, // phase
				{} // platoon
		};
		int[] phaseMins = { 1, 5, 10, 15, 20 };

		String[] paths = null;

		// prepareWaitingFile(map[0]+"_"+tlController[0]+"_WaitingQueues");
		// writeWaitingAvgToFile(map[0]+"_"+tlController[0]+"_WaitingQueues_AVG");
		// ///////////////////////////////////////////////////////////////////////////////////////////
		// for all different frequencies
		// for (int i = 0; i < map.length; i++) {
		// //for all controllers
		// System.out.println("#");
		// for (int j = 0; j < tlControllerId.length; j++) {
		//				
		//				
		// for (int t = 0; t<tetas[j].length; t++) {
		// if (j <=1) { // for marching and optim
		// paths = new String[2];
		// paths[0]=map[i]+"_"+tlController[j]+"_ATWT";
		// paths[1]=map[i]+"_"+tlController[j]+"_AJWT";
		// prepareFile(paths);
		// writeToFile(map[i]+"_"+tlController[j]+"_ALL");
		// // waiting files
		// paths = new String[1];
		// paths[0]=map[i]+"_"+tlController[j]+"_WaitingQueues";
		// prepareWaitingFile(paths);
		// writeWaitingAvgToFile(map[i]+"_"+tlController[j]+"_WaitingQueues_AVG");
		// }
		// else
		// {
		// ATWT
		/*
		 * if (j==2) paths = new String[5]; // for request else paths = new
		 * String[7]; // for phase and platoon
		 * 
		 * for (int t=0; t<tetas[j].length; t++) {
		 * paths[t]=map[i]+"_"+tlController[j]+"_teta"+tetas[j][t]+"_ATWT"; }
		 * prepareFile(paths);
		 * writeToFile(map[i]+"_"+tlController[j]+"_ALL_ATWT");
		 *  // AJWT if (j==2) paths = new String[5]; // for request else paths =
		 * new String[7]; // for phase and platoon
		 * 
		 * for (int t=0; t<tetas[j].length; t++) {
		 * paths[t]=map[i]+"_"+tlController[j]+"_teta"+tetas[j][t]+"_AJWT"; }
		 * prepareFile(paths);
		 * writeToFile(map[i]+"_"+tlController[j]+"_ALL_AJWT");
		 *  // WaitingQueues if (j==2) paths = new String[5]; // for request
		 * else paths = new String[7]; // for phase and platoon
		 * 
		 * for (int t=0; t<tetas[j].length; t++) {
		 * paths[t]=map[i]+"_"+tlController[j]+"_teta"+tetas[j][t]+"_WaitingQueues"; }
		 * prepareWaitingFile(paths);
		 * writeWaitingAvgToFile(map[i]+"_"+tlController[j]+"_ALL_WaitingQueues_AVG");
		 * 
		 */
		// for all maps
		for (int i = 0; i < map.length; i++) {
			// for all controllers
			System.out.println("#");
			for (int j = 0; j < tlControllerId.length; j++) {
				if (false && j==0) { // only for request
					// ATWT
					paths = new String[tetas[j].length]; 
					for (int t = 0; t < tetas[j].length; t++) {
						paths[t] = map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_ATWT";
					}
					prepareFile(paths);
					writeToFile(map[i] + "_" + tlController[j] + "_ALL_ATWT.txt");
					
					// AJWT
					paths = new String[tetas[j].length];
					for (int t = 0; t < tetas[j].length; t++) {
						paths[t] = map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_AJWT";
					}
					prepareFile(paths);
					writeToFile(map[i] + "_" + tlController[j] + "_ALL_AJWT.txt");
					
					
					// WaitingQueues
					paths = new String[tetas[j].length];
					for (int t = 0; t < tetas[j].length; t++) {
						paths[t] = map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_WaitingQueues";
					}
					prepareWaitingFile(paths);
					writeWaitingAvgToFile(map[i] + "_" + tlController[j] + "_ALL_WaitingQueues_AVG.txt");
				}
				else { 
					// for all tetas
					for (int t = 0; t < tetas[j].length; t++) {				
					// ATWT
						paths = new String[phaseMins.length]; 
						for (int pm = 0; pm < phaseMins.length; pm++) {
							paths[pm] = map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_PM" + phaseMins[pm] + "_ATWT";
						}
						prepareFile(paths);
						writeToFile(map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_ALL_ATWT.txt");
					

					// AJWT
						paths = new String[phaseMins.length];
						for (int pm = 0; pm < phaseMins.length; pm++) {
							paths[pm] = map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_PM" + phaseMins[pm] + "_AJWT";
						}
						prepareFile(paths);
						writeToFile(map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_ALL_AJWT.txt");
					
						
					// WaitingQueues
						paths = new String[phaseMins.length];
						for (int pm = 0; pm < phaseMins.length; pm++) {
							paths[pm] = map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_PM" + phaseMins[pm] + "_WaitingQueues";
						}
						prepareWaitingFile(paths);
						writeWaitingAvgToFile(map[i] + "_" + tlController[j] + "_teta" + tetas[j][t] + "_ALL_WaitingQueues_AVG.txt");
				}
				// }
			}
		}

	}// end main
	}
}
