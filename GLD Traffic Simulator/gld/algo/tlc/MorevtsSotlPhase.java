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

public class MorevtsSotlPhase extends TLController {

	protected int PHASE_MIN = 5;
	protected double VISIBLE = 80; /*EJUST: int --> double*/
	protected int TETA = 5;
	
	public final static String shortXMLName="Sotl-Phase";
	
	public MorevtsSotlPhase(Infrastructure infra) 
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
		System.out.println("#######  sotl-phase teta set to "+teta);
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
	
	public int countRoadusers(Drivelane lane) {
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
			if (ru.getPosition() <= VISIBLE) 
				cntr++;
			else 
				stop = true;
		}
		return cntr;
	}
	
	/** 
	 * if trafficlight was switched green at last timeStep 
	 * 	- sets kappa of tld to 0
	 * 	- resets minimal phase to PHASE_MIN
	 * */
	public void updateTLDs() {
		
		//System.out.println("#################################################");	
		for (int i = 0; i < tld.length; i++) { // for all nodes
			boolean switched = false;
			
			for (int j = 0; j < tld[i].length; j++)
				if (tld[i][j].getTL().getTimeStepSwitched()==getCurTimeStep()-1)
					switched = true;
			
			if(switched) {
				if(!tld[i][0].getTL().getNode().getKeepTLDFlag()) {
					
					tld[i][0].getTL().getNode().setKeepTLDFlag(true);
					
					tld[i][0].getTL().getNode().setPhaseMinimal(PHASE_MIN);
					
					for (int j = 0; j < tld[i].length; j++) 
						if (tld[i][j].getTL().getState())
							tld[i][j].setKappa(0);
				
				}
			}
		}	
	}
	
	/**
	 * when kappa >= teta 
	 * 		=> green & kappa=0
	 */	
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
				int cntr = countRoadusers(curLane);

				if (!tld[i][j].getTL().getState())
					currentDec.addKappa(cntr);

				if (currentNode.getPhaseMinimal() <= 0 && currentDec.getKappa() >= TETA) {
					//EJUST comment: 
					//Note that there may be only one lane that satisfy this condition: currentDec.getKappa() >= TETA
					
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
			Drivelane _desiredLane) 
	{
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