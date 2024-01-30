package gld.algo.tlc;

import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Roaduser;
import gld.infra.Sign;
import gld.infra.TrafficLight;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;

import java.awt.Point;

public class MorevtsMarchingWetstraatTLC extends TLController {

	protected TrafficLight[] arteryTLs;
	protected TrafficLight[] nonArteryTLs;
	//public int[][] tlphases;
	protected int PERIOD = 90;
	
	public final static String shortXMLName="Marching control for wetstraat";
	
	public MorevtsMarchingWetstraatTLC(Infrastructure infra) 
	{	
		super(infra);
	}
	
	public void setInfrastructure(Infrastructure i) 
	{	super.setInfrastructure(i);
	}
	
	/** sorts the traffic lights in 2 arrays arteryTLs and nonArteryTLs */
/*	public void searchArtery() {
		int ac = 0;
		int nc = 0;
		for (int i=0; i < tld.length; i++) {
			for (int j=0; j < tld[i].length; j++) {
				if (tld[i][j].getTL().getLane().isPrimary()) { 
					arteryTLs[ac] = tld[i][j].getTL();
					ac++;
				} else {
					nonArteryTLs[nc] = tld[i][j].getTL();
					nc++;
				}
			}
		}
	}*/
	
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
		int curTimeStep = getCurTimeStep();
		int phase = curTimeStep % PERIOD;
		
		for (int i = 0; i < tld.length; i++) { // for all nodes
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				
				curDec = tld[i][j];
				curLane = tld[i][j].getTL().getLane();
				
				// currentLane = tld[i][j].getTL().getLane();
				// if phase == p then set qvalue to 0

				
					if (curLane.isPrimary()) {
						// turning left on primary lane
						if (phase <= 65) 							
								curDec.setGain(5);
						else
							curDec.setGain(0);
					}
					else // current lane is non-primary
					{
						// it's primary lane time
						if (phase >65)
							curDec.setGain(5);
						else 
							curDec.setGain(0);
						
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