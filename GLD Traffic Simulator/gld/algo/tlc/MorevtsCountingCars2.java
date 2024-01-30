package gld.algo.tlc;

import gld.infra.Drivelane;
import gld.infra.Infrastructure;
import gld.infra.Roaduser;
import gld.infra.Sign;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;

import java.awt.Point;
import java.util.LinkedList;
import java.util.ListIterator;
/**
 * controller gives green for configuration with most cars before traffic lights 
 * with a minimal phase for the green light
 *
 * @author Seung Bae Cools
 * @version 1.0
 */
public class MorevtsCountingCars2 extends TLController 
{
	protected int PERIOD = 60 ;
	protected double VISIBLE = 80; /*EJUST: int --> double*/
	protected int PHASE_MIN = 10;
	protected int TETA = 300;
	
	public final static String shortXMLName="CountingCars2";
	
	public MorevtsCountingCars2(Infrastructure infra) 
	{	
		super(infra);
	}
	
	public void setInfrastructure(Infrastructure i) 
	{	
		super.setInfrastructure(i);
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
	 * decides which trafficlights has to be green
	 */	
	public TLDecision[][] decideTLs()
	{	
		TLDecision curDec;
		Drivelane curLane;
		
		for (int i = 0; i < tld.length; i++) { // for all nodes
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				curDec = tld[i][j];
				curLane = tld[i][j].getTL().getLane();
				int cntr = countRoadusers(curLane);
				//curDec.addKappa(cntr);
				if (curDec.getPhaseMinimal() == 0) { 
					//if (curDec.getKappa() > TETA) {
					//curDec.setQValue(curDec.getKappa());
					curDec.setGain(cntr);
					curDec.setPhaseMinimal(PHASE_MIN);
					//curDec.setKappa(0);
					// kappa should be reset when configuration is chosen --> see signcontroller
					//}
				} 
				else {
					curDec.decrPhaseMinimal();
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