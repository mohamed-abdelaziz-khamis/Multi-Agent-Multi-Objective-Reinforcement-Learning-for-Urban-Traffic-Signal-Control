package gld.algo.tlc;

import gld.*;
import gld.sim.*;
import gld.algo.tlc.*;
import gld.infra.*;
import gld.utils.*;
import gld.xml.*;

import java.io.IOException;
import java.util.*;
import java.awt.Point;

/**
 * controller gives green for configuration with most cars before traffic lights
 *
 * @author Seung Bae Cools
 * @version 1.0
 */
public class MorevtsCountingCars extends TLController 
{
	//public int[][] tlphases;
	protected int PERIOD = 60 ;
	protected double VISIBLE = 80; /*EJUST: int --> double*/
	protected int TETA = 5;
	
	public final static String shortXMLName="CountingCars";
	
	public MorevtsCountingCars(Infrastructure infra) 
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
	 * This implementation sets the Q-values according to the curTimeStep count
	 * if phase of trafficlight is same as value p, sign has to be switched
	 * * when phase = PERIOD           red -> green, green -> red
	 */	
	public TLDecision[][] decideTLs()
	{	
		// System.out.println("Current time step = "+
		// (getInfrastructure().getCurTimeStep()));
		TLDecision curDec;
		Drivelane curLane;
		
		for (int i = 0; i < tld.length; i++) { // for all nodes
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				
				curDec = tld[i][j];
				curLane = tld[i][j].getTL().getLane();
				int cntr = countRoadusers(curLane);
				//currentLane = tld[i][j].getTL().getLane();
				// if phase == p then set qvalue to 0
				if (cntr >= TETA)
					curDec.setGain(cntr);
				else
					curDec.setGain(0);				
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