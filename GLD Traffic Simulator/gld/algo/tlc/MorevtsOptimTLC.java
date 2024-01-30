package gld.algo.tlc;
import gld.idm.Constants;
import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Node;
import gld.infra.Roaduser;
import gld.infra.Sign;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;

import java.util.Vector;

/**
 * implements the Optim Control algorithm 
 * (see paper "Self-Organizing Traffic Lights" of Carlos Gershenson)
 *
 * @author Seung Bae Cools
 * @version 1.0
 */

public class MorevtsOptimTLC extends TLController implements Constants /*EJUST*/ {

	public int PERIOD = 60 ;
	
	public final static String shortXMLName="Green Waves (Optim)";
	
	/** EJUST: Position update time step*/
	protected static final double dt = TIMESTEP_S;
	
	public MorevtsOptimTLC(Infrastructure infra) 
	{	
		super(infra);
		//setPhaseDiffs();
	}
	
	public void setInfrastructure(Infrastructure infra) {
		super.setInfrastructure(infra);
		
		int phaseDiff = 0;
		int length;
		double speed, diff; /*EJUST: int --> double*/

		Node[] nodes;
		Node start = null, finish = null;
		Node junction = null;
		Drivelane[] lanes;
		Object[] path;
		Vector gwPath = new Vector();
		Vector visitedNodes = new Vector();
		int gwIndex = 0;
		boolean stop = false;
		//System.out.println("MorevtsOptimTLC: Testpoint 1");
		// search Green Wave Start junction, finish junction
		nodes = infra.getAllNodes();
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].getType() == Node.JUNCTION) {
				if (nodes[i].isGreenWaveStart())
					start = nodes[i];
				if (nodes[i].isGreenWaveFinish())
					finish = nodes[i];
			}
		}
		if (start == null || finish == null) {
			System.out.println("MorevtsOptimTLC: no starting/finishing junction for green wave");
		}
		//System.out.println("MorevtsOptimTLC: Testpoint 2");
		
		try {
			// find primary path from start to finish/////////////////////////////////////////
			junction = start;
			visitedNodes.add(start);
			//System.out.println("MorevtsOptimTLC: searching path, node id: " + junction.getId());
			while (junction.getId() != finish.getId()) {			
				lanes = junction.getOutboundLanes();
				stop = false;
				for (int i = 0; i < lanes.length && !stop; i++) {
					//System.out.println("MorevtsOptimTLC: searching ...");
					if (lanes[i].isPrimary() 
							&& !visitedNodes.contains(lanes[i].getNodeLeadsTo())
							&& lanes[i].getNodeLeadsTo().isGreenWaveNode()) 
					{
						gwPath.add(lanes[i]);
						gwIndex++;
						junction = lanes[i].getNodeLeadsTo();
						visitedNodes.add(lanes[i].getNodeLeadsTo());
						stop = true;
					}
				}
				System.out.println("MorevtsOptimTLC: searching path, node id: " + junction.getId());
			}
			System.out.println("MorevtsOptimTLC: path length = " + gwPath.size());
			//System.out.println("MorevtsOptimTLC: Testpoint 3");
			path =  gwPath.toArray();
			//System.out.println("MorevtsOptimTLC: Testpoint 4");
			// set phaseDiffs of junctions
			for (int i = 0; i < path.length; i++) {
				length = ((Drivelane)path[i]).getLength();
				speed = ((Drivelane)path[i]).getSpeedMaxAllowed();
				diff = length / (speed*dt /*EJUST*/);
				phaseDiff += diff + 3;
				junction = ((Drivelane)path[i]).getNodeLeadsTo();
				if (junction.getType() == Node.JUNCTION)
					((Drivelane)path[i]).getNodeLeadsTo().setPhaseDiff(phaseDiff);
			}

		} catch (InfraException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This implementation sets the Q-values according to the curTimeStep count
	 * if phase of trafficlight is same as value p, sign has to be switched and the phase reset.
	 */	
	public TLDecision[][] decideTLs()
	{	
		//System.out.println("Current time step = "+ (getInfrastructure().getCurTimeStep()));
		TLDecision curDec;
		Drivelane curLane;
		int curTimeStep = getCurTimeStep();
		int phase;
		
		for (int i = 0; i < tld.length; i++) { // for all nodes
			for (int j = 0; j < tld[i].length; j++) { // for all inbound lanes in node
				phase = (curTimeStep - tld[i][j].getTL().getNode().getPhaseDiff()) % PERIOD;
				curDec = tld[i][j];
				curLane = tld[i][j].getTL().getLane();
				
				//currentLane = tld[i][j].getTL().getLane();
				// if phase == p then set qvalue to 0
				
				try {				
					if (curLane.isPrimary()) {
						// turning left on primary lane
						if (phase <= PERIOD / 4) {
							if (curLane.getTarget(0)) {
								curDec.setGain(5);
							}
							if (curLane.getTarget(1) || curLane.getTarget(2))
								curDec.setGain(0);
						}
						// straight on or turning right on primary lane
						else if (phase <= PERIOD / 2) {
							if (curLane.getTarget(0))
								curDec.setGain(0);
							if (curLane.getTarget(1) || curLane.getTarget(2))
								curDec.setGain(5);
						}
						// phase > (PERIOD / 2)
						else
							curDec.setGain(0);
					}
					else // current lane is non-primary
					{
						// it's primary lane time
						if (phase <= PERIOD / 2)
							curDec.setGain(0);
						// turning left on non-primary lane
						else if (phase <= (3 * PERIOD) / 4) {
							if (curLane.getTarget(0))
								curDec.setGain(5);
							if (curLane.getTarget(1) || curLane.getTarget(2))
								curDec.setGain(0);
						}
						// straight on or turning right on non-primary lane
						else if (phase <= PERIOD) {
							if (curLane.getTarget(0))
								curDec.setGain(0);
							if (curLane.getTarget(1) || curLane.getTarget(2))
								curDec.setGain(5);
						}
					}
				
				} //try
				catch (InfraException e) 
				{
					e.printStackTrace(); 
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