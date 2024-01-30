package gld;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import gld.infra.Infrastructure;
import gld.infra.SpecialNode;
import gld.sim.*;
import gld.sim.stats.AllJunctionsDelayTrackingView;
import gld.sim.stats.ExtendedTrackingView;
import gld.sim.stats.StatisticsController;
import gld.sim.stats.TotalDelayTrackingView;
import gld.sim.stats.TrackerFactory;

public class GLDSimTester {
	
	public static void main (String[] params)
	{			
		String[] map = {  
//				"E:\\wetstraat_phase\\map3_wetstraat_07",
				"E:\\wetstraat_phase\\map3_wetstraat_08-09"//,
//				"E:\\wetstraat_phase\\map3_wetstraat_10-17-18",
//				"E:\\wetstraat_phase\\map3_wetstraat_11-15-16",
//				"E:\\wetstraat_phase\\map3_wetstraat_13-14-19"
//				"E:\\WetstraatCheckups\\map3_wetstraat_02-03-04",
//				"E:\\WetstraatCheckups\\map3_wetstraat_05",
//				"E:\\laatstesim_map1hd_phase\\map1_2nodes_hd"//,
//				"E:\\WetstraatCheckups\\map3_wetstraat_07"//,
//				"E:\\WetstraatCheckups\\map3_wetstraat_08-09"//,
//				"E:\\WetstraatCheckups\\map3_wetstraat_10-17-18",
//				"E:\\WetstraatCheckups\\map3_wetstraat_11-15-16",
//				"E:\\WetstraatCheckups\\map3_wetstraat_13-14-19"//,
//				"E:\\WetstraatCheckups\\map3_wetstraat_20",
//				"E:\\WetstraatCheckups\\map3_wetstraat_21-22"
//				"E:\\WetstraatCheckups\\map3_wetstraat_23"
		};

		
		String[] tlController = {"sotl-request", "sotl-phase", "sotl-platoon"};
		int[] tlControllerId=   {4             , 5           , 6             };
		int[][] tetas= { 
				{ }, //request
				{5, 10, 20, 30, 40, 50}, //phase
				{} //platoon			
				};
		int[] phaseMins= {1 , 5, 10, 15, 20};
			
		int simCount = 1;
		
		GLDStarter gldStarter = new GLDStarter(params,GLDStarter.SIMULATOR);
		SimController simController = (SimController) gldStarter.getController();
	
		// for all different frequencies
		for (int i = 0; i < map.length; i++) {
				
			try{
				simController.doLoad(map[i]+".sim");
			}
			catch (Exception e) {	}
		
			TotalDelayTrackingView viewATWT = new TotalDelayTrackingView(simController.getSimModel().getCurTimeStep(), simController.getSimModel());
			TrackerFactory.genExtTracker(simController.getSimModel(),simController, viewATWT);
			AllJunctionsDelayTrackingView viewAJWT = new AllJunctionsDelayTrackingView(simController.getSimModel().getCurTimeStep(), simController.getSimModel());
			TrackerFactory.genExtTracker(simController.getSimModel(),simController, viewAJWT);
		
		
			
////////////////////////////////////////////////////////////////////////////////////////////	for request		
			for (int simNr=1; simNr<=simCount; simNr++) {		
			
			simController.setSpeed(3); // speed to maximum
			simController.setTLC(0, tlControllerId[0]); // tlc to optim
			//////////////////////////////////////////////////////////////
			

			for (int t=0; t<tetas[0].length; t++) {
					
						
					simController.getSimModel().getTLController().setTeta(tetas[0][t]);
					simController.unpause(); // start simulation
					
					while (simController.getSimModel().getCurTimeStep() < 3605) {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
						
						}
					}
					// export data ATWT AJWT waiting time
					try {
						viewATWT.saveData(map[i]+"_"+tlController[0]+"_teta"+tetas[0][t]+"_ATWT_"+simNr+".txt",
								simController.getSimModel());
						viewAJWT.saveData(map[i]+"_"+tlController[0]+"_teta"+tetas[0][t]+"_AJWT_"+simNr+".txt",
								simController.getSimModel());

						/////////////// getting queues of nodes
						SpecialNode[] specialNodes = simController.getSimModel().infra.getSpecialNodes();
						PrintWriter out=new PrintWriter(new FileWriter(new File(map[i]+"_"+tlController[0]+"_teta"+tetas[0][t]+"_WaitingQueues_"+simNr+".txt")));
						out.println("# Special-nodes waiting queue lengths"); out.println("#");
						String ln;
						for(int k=0; k<specialNodes.length; k++) {
							ln = "Node " + specialNodes[k].getId() + "\t" + specialNodes[k].getWaitingQueueLength();
							out.println(ln);
						}
						out.close();
						////////////////////////////////////////////					
						///////// reset all
						simController.getSimModel().reset();
					
						} catch (Exception e) {
						}
						
					
				} // for (t)
			
			// for controllers phase and platoon
			for (int j = 1; j < tlControllerId.length; j++) {
				
				simController.setSpeed(3); // speed to maximum
				simController.setTLC(0, tlControllerId[j]); // tlc to optim
				//////////////////////////////////////////////////////////////
				

				for (int t=0; t<tetas[j].length; t++) {
						for (int pm=0; pm<phaseMins.length; pm++) {
							
						simController.getSimModel().getTLController().setTeta(tetas[j][t]);
						simController.getSimModel().getTLController().setPhaseMin(phaseMins[pm]);
						simController.unpause(); // start simulation
						
						while (simController.getSimModel().getCurTimeStep() < 3605) {
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
							
							}
						}
						// export data ATWT AJWT waiting time
						try {
							viewATWT.saveData(map[i]+"_"+tlController[j]+"_teta"+tetas[j][t]+"_PM"+phaseMins[pm]+"_ATWT_"+simNr+".txt",
									simController.getSimModel());
							viewAJWT.saveData(map[i]+"_"+tlController[j]+"_teta"+tetas[j][t]+"_PM"+phaseMins[pm]+"_AJWT_"+simNr+".txt",
									simController.getSimModel());

							/////////////// getting queues of nodes
							SpecialNode[] specialNodes = simController.getSimModel().infra.getSpecialNodes();
							PrintWriter out=new PrintWriter(new FileWriter(new File(map[i]+"_"+tlController[j]+"_teta"+tetas[j][t]+"_PM"+phaseMins[pm]+"_WaitingQueues_"+simNr+".txt")));
							out.println("# Special-nodes waiting queue lengths"); out.println("#");
							String ln;
							for(int k=0; k<specialNodes.length; k++) {
								ln = "Node " + specialNodes[k].getId() + "\t" + specialNodes[k].getWaitingQueueLength();
								out.println(ln);
							}
							out.close();
							////////////////////////////////////////////					
							///////// reset all
							simController.getSimModel().reset();
						
							} catch (Exception e) {
							}
							
						} // for (pm)	
					} // for (t)

				
			
		
			}
			}
		
		}

	}	
}
