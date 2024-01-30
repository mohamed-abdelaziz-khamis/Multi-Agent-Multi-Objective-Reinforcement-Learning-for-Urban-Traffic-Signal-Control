package gld;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * 
 * @author EJUST: borrow methods: getValue, collectValuesToAvg, and writeToFile from SBC
 *  - collect the statistics (e.g., waiting times) of different simulations and export the avg value for every timeStep to file
 */
public class DataProcessorEJUST {
	
	private static int recordCount = 781;
	private static int simCount = 10;
	private static float[] avgValues = new float[recordCount];

	/**
	 * get the value out of the record and return it as float
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
	 *  - collect the values of the files (simCount files) 
	 *  - sum of corresponding values in array 
	 *  - divide by simCount
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
			String path = bpath + "_run-" + i + ".dat";
			// System.out.println("Collecting file "+path);
			try {				
				BufferedReader br = new BufferedReader(new FileReader(path));

				for (int j = 0; j < 11 /*EJUST: 9--> 11*/; j++) 
				{
					br.readLine();
				}

				for (int k = 0; k < avgValues.length; k++) 
				{
					if ((record = br.readLine()) != null) 
					{
						avgValues[k] += getValue(record, 2);
					}
				}
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		} // end for (i)

		// normalize
		for (int i = 0; i < avgValues.length; i++) {
			avgValues[i] = avgValues[i] / simCount;
		}
	}
	
	/**
	 * will write output to file with given path
	 * 
	 * @param path
	 */
	private static void writeToFile(String path) {
		try 
		{
			PrintWriter out = new PrintWriter(new FileWriter(new File(path)));
			for (int i = 0; i < avgValues.length; i++) 
			{
				out.println(avgValues[i]);
			}
			out.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] params) 
	{
		String logPath = "log";
		File   logDir  = new File(logPath);
		String[] tlController = {
//				"MO hybrid exploration based on current-neighbour nodes AA",
//				"MO hybrid exploration based on current-neighbour nodes GW",
//				"MO hybrid exploration based on current-neighbour nodes FR",
//				"MO hybrid exploration based on current-neighbour nodes AJWT",
//				"MO hybrid exploration based on current-neighbour nodes ATT",
//				"MO hybrid exploration based on current-neighbour nodes ATWT"
//				"MO with epsilon exploration",
//				"MO with hybrid exploration based on neighboring junctions",
//				"MO with hybrid exploration based on current junctions",
				"TC1TLCOpt",				
				"MorevtsSotlPlatoon",
				"MO hybrid exploration current-neighboring junctions",
//				"SL1TLC",
				"ACGJ1"
	
								 };
		// for all controllers
		for (int i = 0; i < tlController.length; i++) 
		{			
			// average junction waiting time			
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "average junction waiting time");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_average junction waiting time_AVG.dat");	
			
			// average speed			
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "average speed");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_average speed_AVG.dat");	
			
			// average trip absolute stops count			
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "average trip absolute stops count");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_average trip absolute stops count_AVG.dat");	
			
			// average trip stops count			
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "average trip stops count");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_average trip stops count_AVG.dat");
			
			// average trip time			
			//collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "average trip time");
			//writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_average trip time_AVG.dat");
			
			// average trip waiting time			
			//collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "average trip waiting time");
			//writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_average trip waiting time_AVG.dat");
			
			// colearn average junction waiting time			
			//collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "colearn average junction waiting time");
			//writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_colearn average junction waiting time_AVG.dat");
			
			// colearn average speed			
			//collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "colearn average speed");
			//writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_colearn average speed_AVG.dat");
			
			// colearn average trip time			
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "colearn average trip time");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_colearn average trip time_AVG.dat");
			
			// colearn average trip waiting time			
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "colearn average trip waiting time");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_colearn average trip waiting time_AVG.dat");
			
			// percentage of roadusers arrived as opposed to total roadusers entered
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "percentage of roadusers arrived as opposed to total roadusers entered");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_percentage of roadusers arrived as opposed to total roadusers entered_AVG.dat");
			
			// percentage of roadusers rejected as opposed to total roadusers generated
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "percentage of roadusers rejected as opposed to total roadusers generated");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_percentage of roadusers rejected as opposed to total roadusers generated_AVG.dat");
			
			// average num of roadusers waiting of all lanes
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "average num of roadusers waiting of all lanes");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_average num of roadusers waiting of all lanes_AVG.dat");
			
			// maximum num of roadusers waiting of all lanes
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "maximum num of roadusers waiting of all lanes");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_maximum num of roadusers waiting of all lanes_AVG.dat");
			
			// total arrived roadusers			
			collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "total arrived roadusers");
			writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_total arrived roadusers_AVG.dat");
			
			// total roadusers not arrived yet			
			//collectValuesToAvg(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "total roadusers not arrived yet");
			//writeToFile(logDir + "/" + "_tlc-" + tlController[i] + "_view-" + "_total roadusers not arrived yet_AVG.dat");		
		}
	}
}