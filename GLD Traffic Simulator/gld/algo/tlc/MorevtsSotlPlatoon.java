package gld.algo.tlc;

import gld.infra.Drivelane;
import gld.infra.Infrastructure;
import gld.infra.Node;
import gld.infra.Roaduser;
import gld.infra.Sign;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;

import java.util.LinkedList;
import java.util.ListIterator;

public class MorevtsSotlPlatoon extends TLController {

	protected int PHASE_MIN = 20; //EJUST: 5 --> 20
	 /* SBC: Phase_min = 5 time steps = 5 seconds. 1 time step = 1 second
	 * EJUST IDM: 1 time step = 0.25 seconds then we need 20 time steps
	 * */
	protected int VISIBLE = 80;
	protected int OMEGA = 25;
	protected int MU = 3;
	protected int TETA = 50;
	
	public final static String shortXMLName="Sotl-Platoon";
	
	public MorevtsSotlPlatoon(Infrastructure infra) 
	{	
		super(infra);
		super.setKeepSwitchControl(true);
	}
	
	public void setInfrastructure(Infrastructure i) 
	{	
		super.setInfrastructure(i);
	}
	
	public void setTeta(int teta) {
		TETA=teta;
		System.out.println("#######  sotl-platoon teta set to "+teta);
	}
	public int getTeta() {
		return TETA;	
	}
	
	public void setPhaseMin(int pm) {
		PHASE_MIN = pm;
		System.out.println("#######  sotl-platoon phase_min set to "+pm);
	}
	
	public int getPhaseMin() {
		return PHASE_MIN;
	}
	
	public int countRoadusers(Drivelane lane, double range /*EJUST: int --> double*/) {
		int cntr = 0;
		Roaduser ru;
		boolean stop = false;
		LinkedList queue = lane.getQueue();
		ListIterator li = queue.listIterator();
		while (li.hasNext() && !stop) {
			try 
			{
				ru = (Roaduser) li.next();
			} 
			catch (Exception e) 
			{
				// When this exception is thrown you removed the first element
				// of the queue, therefore re-create the iterator.
				System.out.println("CME");
				li = queue.listIterator();
				continue;
			}
			if (ru.getPosition() <= range) 
				cntr++;
			else 
				stop = true;
		}
		return cntr;
	}
	
	/** 
	 * if traffic light was switched green at last timeStep 
	 * 	- sets kappa of tld to 0
	 * 	- resets minimal phase to PHASE_MIN
	 * */
	public void updateTLDs() {
		
		//System.out.println("#################################################");	
		for (int i = 0; i < tld.length; i++) { // for all nodes
			boolean switched = false;
			boolean platoonCrossing = false;
			int platoonRoaduserCount = 0;
			int nrOfPlatoonLanes = 0;
			int platoonRuCntAvg = 0;
			
			for (int j = 0; j < tld[i].length; j++)
				if (tld[i][j].getTL().getTimeStepSwitched()==getCurTimeStep()-1)
					switched = true;
			
			// set flag and reset kappa
			if(switched) {
				if(!tld[i][0].getTL().getNode().getKeepTLDFlag()) {
					
					tld[i][0].getTL().getNode().setKeepTLDFlag(true);
					
					tld[i][0].getTL().getNode().setPhaseMinimal(PHASE_MIN);
					
					for (int j = 0; j < tld[i].length; j++) 
						if (tld[i][j].getTL().getState())
							tld[i][j].setKappa(0);
				}
			}
			
			// check if platoon is crossing. 
			for (int j = 0; j < tld[i].length; j++)
				// if tl is green
				if (tld[i][j].getTL().getState()) {
					platoonRoaduserCount += countRoadusers(tld[i][j].getTL().getLane(),OMEGA);
					nrOfPlatoonLanes += 1;
				}
			
			if (nrOfPlatoonLanes>0) {
				platoonRuCntAvg = platoonRoaduserCount/nrOfPlatoonLanes;
				if (platoonRuCntAvg >= 1 && platoonRuCntAvg <= MU){
					//System.out.println("platoon is crossing with avg value = "+platoonRuCntAvg);
					platoonCrossing = true;
				}
				if(tld[i].length>=1){
					tld[i][0].getTL().getNode().setPlatoonCrossing(platoonCrossing);
					/*System.out.println("["+getCurTimeStep()+"]"
							+"node: "+tld[i][0].getTL().getNode().getId()
							+" # platoon crossing: "+platoonCrossing
							+" # lanes: "+nrOfPlatoonLanes
							+" # count: "+platoonRuCntAvg);*/
				}
			}
		}	
	}
	
	/** 
	 * if trafficlight was switched green at last timeStep 
	 * 	- sets kappa of tld to 0
	 * 	- resets minimal phase to PHASE_MIN
	 * */
	public TLDecision[][] decideTLs()
	{	
		// System.out.println("Current time step = "+
		// (getInfrastructure().getCurTimeStep()));
		TLDecision currentDec;
		Drivelane curLane;
		
		//System.out.println("#### TLDATA-"+this.getCurTimeStep()+" ####");
		
		//adjust data after last timeStep
		updateTLDs();
		//
		
		for (int i = 0; i < tld.length; i++) { // for all nodes
			Node currentNode = null;
			for (int j = 0; j < tld[i].length; j++)
				if (currentNode == null) currentNode= tld[i][j].getTL().getNode();
			
			if (currentNode != null && currentNode.getKeepTLDFlag()) {
				currentNode.decrPhaseMinimal();
				//System.out.println("Decr PHASEMIN for node "+ tld[i][0].getTL().getNode().getId());		
			}
			
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				
				currentDec = tld[i][j];
				curLane = tld[i][j].getTL().getLane();
				int cntr = countRoadusers(curLane,VISIBLE);

				if (!tld[i][j].getTL().getState())
					currentDec.addKappa(cntr);

				if (currentNode.getPhaseMinimal() <= 0 && !currentNode.isPlatoonCrossing()	&& currentDec.getKappa() >= TETA) {
					//System.out.println("REQUEST APPROVED FOR NODE "+ currentNode.getId());
					currentDec.setGain(currentDec.getKappa());
					currentNode.setKeepTLDFlag(false);
					// curDec.setQValue(1);
					/*System.out.println("+ PROPOSITION <C-"+getCurTimeStep()+
							"> <Node: "+tld[i][j].getTL().getNode().getId()+
							"> <tl: "+currentDec.getTL().getId()+
							"> <kappa: "+currentDec.getKappa()+
							"> <Q = " + tld[i][j].getQValue() +
							"> <wait: "+currentDec.getPhaseMinimal()+">");*/
				}
				else 
				{
					currentDec.setGain(0);
				/*	System.out.println("- REJECTED <C-"+getCurTimeStep()+
							"> <Node: "+tld[i][j].getTL().getNode().getId()+
							"> <tl: "+curDec.getTL().getId()+
							"> <kappa: "+curDec.getKappa()+
							"> <Q = " + tld[i][j].getQValue() +
							"> <wait: "+curDec.getPhaseMinimal()+">");*/
				}
			}
		}
		return tld;
	}
	
/*	*//** 
	 * if trafficlight was switched green at last timeStep 
	 * 	- sets kappa of tld to 0
	 * 	- resets minimal phase to PHASE_MIN
	 * *//*
	public void updateTLDs() {
		//System.out.println("#################################################");	
		for (int i = 0; i < tld.length; i++) { // for all nodes
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				
		//		if(tld[i][j].getTL().getNode().isAllRed())
		//			tld[i][j].getTL().getNode().setAllRed(false);
				
				if (tld[i][j].getTL().getTimeStepSwitched()==getCurTimeStep()-1) {
					System.out.println("SWITCHED <C-"+tld[i][j].getTL().getTimeStepSwitched()+
							"> <Node: "+tld[i][j].getTL().getNode().getId()+
							"> <tl: "+tld[i][j].getTL().getId()+
							"> <kappa = " + tld[i][j].getKappa()+
							"> <Q = " + tld[i][j].getQValue() +
							"> <wait: " + tld[i][j].getPhaseMinimal()+">");
					if (!tld[i][j].isFrozen()) {
						tld[i][j].setFrozen(true);
						tld[i][j].setPhaseMinimal(PHASE_MIN);
						tld[i][j].setKappa(0);	
						tld[i][j].getTL().getNode().setKeepTLDFlag(true);
					} else {
						int cntr = countRoadusers(tld[i][j].getTL().getLane(),OMEGA);
						if(tld[i][j].getPhaseMinimal()==1 && cntr>=1 && cntr<=MU)
						{
							tld[i][j].setQValue(tld[i][j].getQValue()+VISIBLE);
							tld[i][j].setPhaseMinimal(tld[i][j].getPhaseMinimal()+1);
							System.out.println("PLATOON-ACTION <C-"+tld[i][j].getTL().getTimeStepSwitched()+
									"> <Node: "+tld[i][j].getTL().getNode().getId()+
									"> <tl: "+tld[i][j].getTL().getId()+
									"> <kappa = " + tld[i][j].getKappa()+
									"> <Q = " + tld[i][j].getQValue() +
									"> <wait: " + tld[i][j].getPhaseMinimal()+
									"> <count: " + cntr + ">");
						}
						else
							tld[i][j].setQValue(tld[i][j].getQValue()+VISIBLE);
					}
				}
			}
		}	
	}
	*//**
	 * when kappa >= teta => green & kappa=0
	 * 
	 * 2 RESTRICTIONS
	 * 	- CHECK IF PLATOON IS NOT CROSSING THROUGH
	 * 	- RESTRICTION 1 IS NOT TAKEN INTO ACCOUNT IF THERE ARE MORE THAN MU CARS APPROACHING
	 *//*	
	public TLDecision[][] decideTLs()
	{	// System.out.println("Current time step = "+
		// (getInfrastructure().getCurTimeStep()));
		TLDecision curDec;
		Drivelane curLane;
		
		//System.out.println("#### TLDATA-"+this.getCurTimeStep()+" ####");
		
		updateTLDs();
		
		for (int i = 0; i < tld.length; i++) { // for all nodes
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				
				curDec = tld[i][j];
				curLane = tld[i][j].getTL().getLane();
				int cntr = countRoadusers(curLane,VISIBLE);
				if (!tld[i][j].getTL().getState())
					curDec.addKappa(cntr);
				
				//System.out.println("TL-"+curDec.getTL().getId()+" counter: " + cntr+"\tkappa: "+curDec.getKappa());
				
				if (curDec.isFrozen()) {
					curDec.decrPhaseMinimal();
				//	if (!curDec.isFrozen()) {
				//		curDec.getTL().getNode().setAllRed(true);
				//		//System.out.println("node set to all red");
				//	}
				}
				else if (curDec.getKappa() >= TETA) {
					curDec.setQValue(curDec.getKappa());
					tld[i][j].getTL().getNode().setKeepTLDFlag(false);
					//curDec.setQValue(1);
					System.out.println("+ PROPOSITION <C-"+getCurTimeStep()+
							"> <Node: "+tld[i][j].getTL().getNode().getId()+
							"> <tl: "+curDec.getTL().getId()+
							"> <kappa: "+curDec.getKappa()+
							"> <Q = " + tld[i][j].getQValue() +
							"> <wait: "+curDec.getPhaseMinimal()+">");
				}
				else 
				{
					curDec.setQValue(0);
					System.out.println("- REJECTED <C-"+getCurTimeStep()+
							"> <Node: "+tld[i][j].getTL().getNode().getId()+
							"> <tl: "+curDec.getTL().getId()+
							"> <kappa: "+curDec.getKappa()+
							"> <Q = " + tld[i][j].getQValue() +
							"> <wait: "+curDec.getPhaseMinimal()+">");
				}
			}
		}
		return tld;
	}*/

	public void updateRoaduserMove(
			Roaduser _ru,
			Drivelane _prevlane,
			Sign _prevsign,
			double _prevpos, /*EJUST: int-->double*/
			Drivelane _dlanenow,
			Sign _signnow,
			double _posnow, /*EJUST: int-->double*/
			PosMov[] posMovs,
			//Drivelane[] _possiblelanes,
			//Point[] _ranges,
			Drivelane _desiredLane) {
		// Not needed
		}
	
	// XMLSerializable implementation
	public XMLElement saveSelf () throws XMLCannotSaveException
	{	
		XMLElement result=super.saveSelf();
		result.setName(shortXMLName);
		return result;
	}
  
 	public String getXMLName ()
	{ 	
 		return "model."+shortXMLName;
	}
}