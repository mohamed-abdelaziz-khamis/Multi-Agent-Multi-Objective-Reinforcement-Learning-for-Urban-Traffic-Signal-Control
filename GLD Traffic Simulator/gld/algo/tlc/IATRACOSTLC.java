/*
 * Created on 12/08/2004
 *
 */
package gld.algo.tlc;

import gld.algo.tlc.iatracos.SignConfigInterval;
import gld.algo.tlc.iatracos.listener.BaseSignConfigListener;
import gld.algo.tlc.iatracos.listener.SignConfigListenerFactory;
import gld.infra.Drivelane;
import gld.infra.Infrastructure;
import gld.infra.Junction;
import gld.infra.Node;
import gld.infra.Roaduser;
import gld.infra.Sign;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ResourceBundle;

/**
 * @author mpastorino
 * This class converts the interval times established from I-Atracos system to 
 * priorities for every traffic light control in GLD.
 * 
 */
public class IATRACOSTLC extends TLController {

	protected final static String shortXMLName = "tlc-iatracos";
	private SignConfigInterval tls[][];
	private BaseSignConfigListener signConfigListener;
	private ResourceBundle rb;
	private TLController tlcDefault;
	private String algorithmKey;

	private static final float GREEN = 1f;
	private static final float RED = 0f;

	public IATRACOSTLC(Infrastructure infra) {
		super(infra);
	}

	public void setInfrastructure(Infrastructure i) {
		this.infra = i;
		rb = ResourceBundle.getBundle("iAtracosTLC");
		algorithmKey = "edu.utn.frba.iatracos.tlc.default";
		try {
			this.tlcDefault = (TLController) Class.forName(rb.getString(algorithmKey)).getConstructor(new Class[] { Infrastructure.class }).newInstance(new Object[] { i });
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (InvocationTargetException e1) {
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		tld = createDecisionArray(i);
		try {
			initializeTLS();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TLDecision[][] decideTLs() {
		this.parseData();
		if (this.tls != null) {
			this.updatePriorities();
			return tld;
		}
		return this.tlcDefault.decideTLs();
	}

	/**
	 * This method read the sign configuration file and store in memory the sign configurations interval. 
	 * @throws Exception
	 */
	private void initializeTLS() throws Exception {
		if (tld.length == 0) {
			return;
		}
		if (this.signConfigListener == null) {
			this.signConfigListener =
				SignConfigListenerFactory.getSignConfigListener(this.rb);
			this.signConfigListener.start();
		}
		this.tls = this.signConfigListener.getTld();
	}

	/**
	 * This method rereads the sign configuration updating the sign configuration values.
	 * @throws Exception
	 */
	private void parseData() {
		this.tls = this.signConfigListener.getTld();
	}

	private void initializeTLD() {
		for (int i = 0; i < tld.length; i++)
			for (int j = 0; j < tld[i].length; j++)
				tld[i][j].setGain(RED);
	}

	private void updatePriorities() {
		this.initializeTLD();
		int nodesLenght = tls.length;
		int signConfLenght;
		Node node = null;
		Node[] nodes = super.infra.getAllNodes();
		for (int i = 0; i < nodesLenght; i++) {
			node = nodes[i];
			if (node instanceof Junction) {
				signConfLenght = tls[i].length;
				SignConfigInterval signConfigInterval = null;
				for (int j = 0; j < signConfLenght; j++) {
					signConfigInterval = tls[i][j];
					if (this.isActiveConfig(signConfigInterval)) {
						this.doStep((Junction) node, signConfigInterval);
						if (signConfigInterval.isOver()) {
							signConfigInterval.refresh();
							this.changeActiveCongiration(signConfigInterval,this.getNextConfiguration(tls[i],signConfigInterval));
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @param signConfigInterval
	 * @return true if the sign configuration is active, else false.
	 */
	private boolean isActiveConfig(SignConfigInterval signConfigInterval) {
		return signConfigInterval.isActive();
	}

	/**
	 * Converts the sign configuration interval times into traffic light priorities. 
	 * Moreover, it decreases the actual time. Once the sign configuration is processed (doStep() was executed), 
	 * if the sign configuration is over, that is refreshed.
	 * @param node
	 * @param signConfigInterval
	 * @return true if the sign configuration was refreshed, else false.
	 */
	private void doStep(Junction node, SignConfigInterval signConfigInterval) {
		Sign[][] signConfs = node.getSignConfigs();
		Sign[] signs = node.getSigns();
		Sign[] signConf = signConfs[signConfigInterval.getId()];
		for (int i = 0; i < signConf.length; i++) {
			for (int j = 0; j < tld[node.getId()].length; j++) {
				if (signConf[i].getId()
						== tld[node.getId()][j].getTL().getId()) {
					tld[node.getId()][j].setGain(GREEN);
					break;
				}
			}
		}
		signConfigInterval.doStep();
	}

	private void changeActiveCongiration(SignConfigInterval activeConfig, SignConfigInterval nextConfig) {
		activeConfig.setNotActive();
		nextConfig.setActive();
	}

	private SignConfigInterval getNextConfiguration(SignConfigInterval[] signConfigIntervals, SignConfigInterval activeConfig) {
		SignConfigInterval[] result = new SignConfigInterval[signConfigIntervals.length];
		System.arraycopy(signConfigIntervals,0,result,0,signConfigIntervals.length);

		Comparator signConfigComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				SignConfigInterval signConfigInterval1 = (SignConfigInterval) o1;
				SignConfigInterval signConfigInterval2 = (SignConfigInterval) o2;

				if (signConfigInterval1.getActivationOrder() < signConfigInterval2.getActivationOrder()) {
					return -1;
				} else if (signConfigInterval1.getActivationOrder()	> signConfigInterval2.getActivationOrder()) {
					return 1;
				} else if (signConfigInterval1.getId() < signConfigInterval2.getId()) {
					return -1;
				} else if (signConfigInterval1.getId() > signConfigInterval2.getId()) {
					return 1;
				} else {
					return -1;
				}
			}
		};

		Arrays.sort(result, signConfigComparator);
		int i;
		for (i = 0; i < result.length; i++) {
			if (result[i].getId() == activeConfig.getId()) {
				break;
			}
		}

		return result[(i + 1) % signConfigIntervals.length];
	}

	public void updateRoaduserMove(
			Roaduser _ru,
			Drivelane _prevlane,
			Sign _prevsign,
			double _prevpos /*EJUST: int --> double*/,
			Drivelane _dlanenow,
			Sign _signnow,
			double _posnow /*EJUST: int --> double*/,
			PosMov[] posMovs,
			//Drivelane[] _possiblelanes,
			//Point[] _ranges,
			Drivelane _desiredLane) {
	}

	public String getXMLName() {
		return "NothingTLC";
	}
}