package gld.algo.tlc;

import gld.infra.Drivelane;
import gld.infra.Infrastructure;
import gld.infra.Roaduser;
import gld.infra.Sign;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;

import java.util.LinkedList;
import java.util.ListIterator;


/**
 * implements the sotl-request Control algorithm 
 * (see paper "Self-Organizing Traffic Lights" of Carlos Gershenson)
 *
 * @author Seung Bae Cools
 * @version 1.0
 */
public class MorevtsSotlRequest extends TLController 
{
	protected int PERIOD = 60 ;
	protected double VISIBLE = 80; /*EJUST: int --> double*/
	protected int TETA = 5;
	
	public final static String shortXMLName="Sotl-Request";
	
	public MorevtsSotlRequest(Infrastructure infra) 
	{	
		super(infra);
		super.setKeepSwitchControl(true);
	}
	
	public void setInfrastructure(Infrastructure i) 
	{	
		super.setInfrastructure(i);
	}
	
	public void setTeta(int teta) 
	{
		TETA=teta;
		System.out.println("#######  sotl-request teta set to "+teta);
	}
	public int getTeta() 
	{
		return TETA;
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
	
	/** sets kappa of tld to 0 if traffic light was switched green last timeStep */
	public void updateKappa() {

		for (int i = 0; i < tld.length; i++) { // for all nodes
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				
				if (tld[i][j].getTL().getTimeStepSwitched()==getCurTimeStep()-1) {
					//System.out.println("tl-"+tld[i][j].getTL().getId()+" timeStep-"+tld[i][j].getTL().getTimeStepSwitched() +" switched with kappa = " + tld[i][j].getKappa());
					tld[i][j].setKappa(0);
					tld[i][j].getTL().getNode().setKeepTLDFlag(true);
					//nodes[i].setKeepTLDFlag(true);
					//System.out.println("sotlrequest -> flag = true");
				}
				
			}
		}
	}
	/**
	 * when kappa >= teta 
	 * 		Switch => green & Set kappa=0
	 */	
	public TLDecision[][] decideTLs()
	{	
		// System.out.println("Current time step = "+
		// (getInfrastructure().getCurTimeStep()));
		TLDecision curDec;
		Drivelane curLane;
		
		//System.out.println("#### TLDATA-"+this.getCurTimeStep()+" ####");
		
		updateKappa();

		for (int i = 0; i < tld.length; i++) { // for all nodes
			//System.out.println("node " + i);
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				
				curDec = tld[i][j];
				curLane = tld[i][j].getTL().getLane();
				int cntr = countRoadusers(curLane);
				if (!tld[i][j].getTL().getState())
					curDec.addKappa(cntr);
				// currentLane = tld[i][j].getTL().getLane();
				// if phase == p then set qvalue to 0
				
				//System.out.println("TL-"+curDec.getTL().getId()+" counter: " + cntr+"\tkappa: "+curDec.getKappa());
				
				if (curDec.getKappa() >= TETA)
				{
					curDec.setGain(curDec.getKappa());
					tld[i][j].getTL().getNode().setKeepTLDFlag(false);
					//nodes[i].setKeepTLDFlag(false);
					System.out.println("sotlrequest->flag=false"+" node "+i);
				}
				else curDec.setGain(0);
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