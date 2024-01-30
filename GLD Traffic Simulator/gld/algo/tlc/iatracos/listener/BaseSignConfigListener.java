/*
 * Created on Sep 5, 2004
 * Universidad Tecnológica Nacional - Facultad Regional Buenos Aires
 * Proyecto Final 2004
 * I-ATraCoS - Grupo :
 * - Brey, Gustavo
 * - Escobar, Gaston
 * - Espinosa, Marisa
 * - Pastorino, Marcelo
 * All right reserved
 * Class Description
 * 
 */
package gld.algo.tlc.iatracos.listener;

import gld.algo.tlc.TLController;
import gld.algo.tlc.TLDecision;
import gld.algo.tlc.iatracos.SignConfigInterval;
import gld.infra.Infrastructure;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ResourceBundle;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
* CVS Log
* $Id: BaseSignConfigListener.java,v 1.1 2006/08/04 21:18:45 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public abstract class BaseSignConfigListener extends Thread {

	private SignConfigInterval[][] tld;
	protected ResourceBundle rb;

	public void setResourceBundle(ResourceBundle rb) {
		this.rb = rb;
	}

	public SignConfigInterval[][] getTld() {
		return this.tld;
	}

	public abstract void readData();

	public abstract void createListener();

	public abstract void stopListener();

	private SignConfigInterval[][] initializeTLD(String xml) throws Exception {
		SignConfigInterval[][] tls;
		StringReader input = new StringReader(xml);
		DOMParser parser = new DOMParser();
		parser.parse(new InputSource(input));
		Document doc = parser.getDocument();
		NodeList trafficLightNodes = doc.getElementsByTagName("node");
		int trafficLightNodesLength = trafficLightNodes.getLength();
		tls = new SignConfigInterval[trafficLightNodesLength][];
		// For every node sets the sign configration
		for (int i = 0; i < trafficLightNodesLength; i++) {
			NamedNodeMap nodeAttributes =
				trafficLightNodes.item(i).getAttributes();
			int offset = 0;
			if (nodeAttributes != null) {
				Attr attr;
				attr = (Attr) nodeAttributes.getNamedItem("offset");
				offset = Integer.valueOf(attr.getNodeValue()).intValue();
			}
			NodeList signConfs = trafficLightNodes.item(i).getChildNodes();
			int id = 0;
			int interval;
			int activationOrder;
			int active;
			int signConfsLenght = (signConfs.getLength() - 1) / 2;
			tls[i] = new SignConfigInterval[signConfsLenght];
			// Every sign configuration is initialized
			for (int j = 0; j < signConfs.getLength(); j++) {
				NamedNodeMap signConfAttributes =
					signConfs.item(j).getAttributes();
				if (signConfAttributes != null) {
					Attr attr;
					attr = (Attr) signConfAttributes.getNamedItem("id");
					id = Integer.valueOf(attr.getNodeValue()).intValue();
					SignConfigInterval signConfigInterval =
						new SignConfigInterval(id);
					attr = (Attr) signConfAttributes.getNamedItem("interval");
					interval = Integer.valueOf(attr.getNodeValue()).intValue();
					signConfigInterval.initialize(interval);
					attr = (Attr) signConfAttributes.getNamedItem("order");
					activationOrder =
						Integer.valueOf(attr.getNodeValue()).intValue();
					signConfigInterval.setActivationOrder(activationOrder);
					if (activationOrder == 0) {
						signConfigInterval.setActive();
						signConfigInterval.addOffset(offset);
					} else {
						signConfigInterval.setNotActive();
					}

					tls[i][id] = signConfigInterval;
				}
			}
		}
		return tls;
	}

	//	private SignConfigInterval[][] updateTLD(String xml) throws Exception {
	//		SignConfigInterval[][] tls = new SignConfigInterval[this.tld.length][]; 
	//		System.arraycopy(this.tld, 0, tls, 0, this.tld.length);
	//		StringReader input = new StringReader(xml);
	//		DOMParser parser = new DOMParser();
	//		parser.parse(new InputSource(input));
	//		Document doc = parser.getDocument();
	//		NodeList trafficLightNodes = doc.getElementsByTagName("node");
	//		int trafficLightNodesLength = trafficLightNodes.getLength();
	//		// For every node sets the sign configration
	//		for (int i = 0; i < trafficLightNodesLength; i++) {
	//			NodeList signConfs = trafficLightNodes.item(i).getChildNodes();
	//			int id = 0;
	//			int interval;
	//			int activationOrder;
	//			int active;
	//			int signConfsLenght = (signConfs.getLength() - 1) / 2;
	//			// Every sign configuration is initialized
	//			for (int j = 0; j < signConfs.getLength(); j++) {
	//				NamedNodeMap signConfAttributes =
	//					signConfs.item(j).getAttributes();
	//				if (signConfAttributes != null) {
	//					Attr attr;
	//					attr = (Attr) signConfAttributes.getNamedItem("id");
	//					id = Integer.valueOf(attr.getNodeValue()).intValue();
	//					SignConfigInterval signConfigInterval = tls[i][id];
	//					attr =
	//						(Attr) signConfAttributes.getNamedItem("interval");
	//					interval =
	//						Integer.valueOf(attr.getNodeValue()).intValue();
	//					signConfigInterval.update(interval);
	//					attr = (Attr) signConfAttributes.getNamedItem("order");
	//					activationOrder =
	//						Integer.valueOf(attr.getNodeValue()).intValue();
	//					signConfigInterval.setActivationOrder(activationOrder);
	//					tls[i][id] = signConfigInterval;
	//				}
	//			}
	//		}
	//		return tls;
	//	}

	protected void populateSignConfigInterval(InputStream is)
		throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		StringBuffer stringBuffer = null;
		String line = in.readLine();
		while (line != null) {
			while (!line.equals("01111110")) {
			}
			stringBuffer = new StringBuffer();
			line = in.readLine();
			while ((line != null) && (!line.equals("01111110"))) {
				stringBuffer.append(line);
				line = in.readLine();
			}
			System.out.println(stringBuffer.toString());
			this.tld = this.initializeTLD(stringBuffer.toString());
			line = in.readLine();
		}
		
	}
}
