
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University
 *
 * This program (Green Light District) is free software.
 * You may redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation (version 2 or later).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * See the documentation of Green Light District for further information.
 *------------------------------------------------------------------------*/

package gld.config;



import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gld.*;
import gld.sim.*;
import gld.distributions.DistributionFactory;
import gld.distributions.ParameterValue;
import gld.idm.WeatherFactory;
import gld.infra.*;
import gld.utils.*;
import gld.sim.stats.*;

/**
 *
 * @author Group GUI
 * @version 1.0
 */

public class SimEdgeNodePanel extends ConfigPanel implements ItemListener, ActionListener, ChangeListener /*EJUST*/
{
	EdgeNode edgenode;

	TextField spawnFreq;
	Choice spawnTypes;
	Button setSpawn;

	Hyperlink wqlLink, tdLink, twtLink /*EJUST*/, tascLink /*EJUST*/, tscLink /*EJUST*/, ttLink /*EJUST*/,  
				ruaLink, rurLink /*EJUST: roadusers rejected/generated */, roadLink, nodeLink, dSpawnLink;
	Label[] queue;

	//EJUST
	Choice distributionTypes;

	double[][] parameterSpecs = DistributionFactory.getParameterSpecs();
	int[][] allParameters = DistributionFactory.getDistributionParameters();
	String[] distributionDescs = DistributionFactory.getDistributionTypeDescs();
	DoubleJSlider[] parameterSliders = {};
	Label[] parameterLabels = {};
	Label[] parameterValues = {};	
	ParameterValue[] paramValue = {}; /** The value of each parameter */
	//EJUST
	
	public SimEdgeNodePanel(ConfigDialog cd, EdgeNode e) {
		
		super(cd);

		Label clab = new Label("Connects:");
		clab.setBounds(0, 0, 100, 20);
		add(clab);

		roadLink = new Hyperlink();
		roadLink.addActionListener(this);
		roadLink.setBounds(100, 0, 100, 20);
		add(roadLink);

		Label wlab = new Label("With:");
		wlab.setBounds(0, 20, 100, 20);
		add(wlab);

		nodeLink = new Hyperlink();
		nodeLink.addActionListener(this);
		nodeLink.setBounds(100, 20, 100, 20);
		add(nodeLink);


		wqlLink = new Hyperlink("Track waiting queue length");
		wqlLink.addActionListener(this);
		wqlLink.setBounds(0, 50, 200, 20);
		add(wqlLink);

		tdLink = new Hyperlink("Track trip delay");
		tdLink.addActionListener(this);
		tdLink.setBounds(0, 70, 200, 20);
		add(tdLink);

		/*EJUST*/
		twtLink = new Hyperlink("Track trip waiting time");
		twtLink.addActionListener(this);
		twtLink.setBounds(0, 90, 200, 20);
		add(twtLink);
		
		/*EJUST*/
		tascLink = new Hyperlink("Track trip absolute stops count");
		tascLink.addActionListener(this);
		tascLink.setBounds(0, 110, 200, 20);
		add(tascLink);
		
		/*EJUST*/
		tscLink = new Hyperlink("Track trip stops count");
		tscLink.addActionListener(this);
		tscLink.setBounds(0, 130, 200, 20);
		add(tscLink);
		
		/*EJUST*/
		ttLink = new Hyperlink("Track trip time");
		ttLink.addActionListener(this);
		ttLink.setBounds(0, 150, 200, 20);
		add(ttLink);
		
		ruaLink = new Hyperlink("Track roadusers arrived");
		ruaLink.addActionListener(this);
		ruaLink.setBounds(0, 170 /*EJUST: comment 90*/, 200, 20);
		add(ruaLink);

		/*EJUST*/
		rurLink = new Hyperlink("Track roadusers rejected/generated (%)");
		rurLink.addActionListener(this);
		rurLink.setBounds(0, 190, 220, 20);
		add(rurLink);

		Label lab = new Label("Spawnfrequency for");
		lab.setBounds(0, 220 /*EJUST: comment 120*/, 120, 20);
		add(lab);

		spawnTypes = new Choice();
		spawnTypes.addItemListener(this);

		String[] descs = RoaduserFactory.getConcreteTypeDescs();
		for (int i=0; i < descs.length; i++)
			spawnTypes.addItem(descs[i]);

		spawnTypes.setBounds(0, 240 /*EJUST: comment 140*/, 100, 20);
		add(spawnTypes);

		lab = new Label("is");
		lab.setBounds(105, 240 /*EJUST: comment 140*/, 15, 20);
		add(lab);

		spawnFreq = new TextField();
		spawnFreq.setBounds(120, 240 /*EJUST: comment 140*/, 40, 20);
		spawnFreq.addActionListener(this);
		add(spawnFreq);

		setSpawn = new Button("Set");
		setSpawn.addActionListener(this);
		setSpawn.setBounds(170, 240 /*EJUST: comment 140*/, 50, 20);
		add(setSpawn);

		
		//EJUST
		lab = new Label("Or Interarrival times follow Distribution");
		lab.setBounds(0, 265, 250, 20);
		add(lab);

		distributionTypes = new Choice();
		distributionTypes.addItemListener(this);

		for (int i=0; i < distributionDescs.length; i++)
			distributionTypes.addItem(distributionDescs[i]);

		distributionTypes.setBounds(250, 265, 100, 20);
		add(distributionTypes);

		loadDistributionParameters();
		//EJUST
		
		//(DOAS 05)
		dSpawnLink = new Hyperlink("Dynamic Spawnfrequencies");
		dSpawnLink.addActionListener(this);
		dSpawnLink.setBounds(0, 320 /*EJUST comment 165*/, 220, 20);
		add(dSpawnLink);


		lab = new Label("Waiting in queue:");
		lab.setBounds(200, 0, 150, 20);
		add(lab);

		int nrtypes = RoaduserFactory.statArrayLength();
		queue = new Label[nrtypes];
		for (int i=0; i < nrtypes; i++) {
			lab = new Label();
			lab.setBounds(200, i * 20 + 20, 150, 20);
			add(lab);
			queue[i] = lab;
		}

		setEdgeNode(e);
	}

	public void reset() {
		setSpawnFreq();
		
		try {
			int nrtypes = RoaduserFactory.statArrayLength();
			int[] nrwaiting = new int[nrtypes];
			Roaduser ru;
			
			ListIterator li = edgenode.getWaitingQueue().listIterator();
			while (li.hasNext()) {
				ru = (Roaduser)li.next();
				nrwaiting[RoaduserFactory.getStatIndexByType(ru.getType())]++;
				nrwaiting[0]++;
			}
			
			for (int i=0; i < nrtypes; i++) {
				queue[i].setText(nrwaiting[i] + " - " + RoaduserFactory.getDescByStatIndex(i));
			}
		}
		
		// SimModel thread changed the queue while we were updating, try again.
		catch (ConcurrentModificationException e) {
			reset();
		}
	}
	
	public void setSpawnFreq() {
		int type = getSpawnType();
		SpawnFrequency spawnFrequency = edgenode.getSpawnFrequency(type); //EJUST
		float freq = spawnFrequency.freq; //EJUST: edgenode.getSpawnFrequency(type);
		//spawnFreq.setText("" + (freq > 0 ? freq : 0));
		spawnFreq.setText("" + (freq >= 0 ? freq : "")); /*EJUST*/			
	}

	public void setSpawnType() {
		
		SimModel sm = (SimModel)confd.getController().getModel();
		try {	
			float fr = -1; //EJUST
			if (!spawnFreq.getText().trim().isEmpty()){ //EJUST
				fr = Float.parseFloat(spawnFreq.getText());
				if (fr < 0) confd.showError("Spawn frequency must be greater than or equal zero."); //EJUST
				else sm.setSpawnFrequency(edgenode, getSpawnType(), fr, 
						getDistributionType() /*EJUST*/, paramValue  /*EJUST*/,
						WeatherFactory.DRY /*EJUST*/);
			}
			else sm.setSpawnFrequency(edgenode, getSpawnType(), fr, 
					getDistributionType() /*EJUST*/, paramValue  /*EJUST*/,
					WeatherFactory.DRY /*EJUST*/);
		}
		catch (NumberFormatException ex) {
			confd.showError("You must enter a float in the Spawn frequencies box.");
		}
	}


	public void setEdgeNode(EdgeNode e) {
		edgenode = e;
		confd.setTitle(edgenode.getName());
		reset();
		setSpawnType();

		Road road = edgenode.getRoad();
		if (road != null) {
			roadLink.setText(road.getName());
			roadLink.setEnabled(true);
			nodeLink.setText(road.getOtherNode(edgenode).getName());
			nodeLink.setEnabled(true);
		}
		else {
			roadLink.setText("null");
			roadLink.setEnabled(false);
			nodeLink.setText("null");
			nodeLink.setEnabled(false);
		}
	}

	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		if (source == spawnTypes) setSpawnFreq();
		else if (source == distributionTypes) loadDistributionParameters();   //EJUST             	
	}

	//EJUST
	private void loadDistributionParameters() {
		for (int i=0; i <parameterSliders.length; i++){
			remove(parameterSliders[i]);
			remove(parameterLabels[i]);
			remove(parameterValues[i]);
		}

		int distributionIndex = distributionTypes.getSelectedIndex();

		if (allParameters[distributionIndex][0] == -1){                		
			parameterSliders = new DoubleJSlider[0];
			parameterLabels = new Label[0];
			parameterValues = new Label[0];
		}
		else{
			parameterSliders = new DoubleJSlider[allParameters[distributionIndex].length];
			parameterLabels = new Label[allParameters[distributionIndex].length];
			parameterValues = new Label[allParameters[distributionIndex].length];
			paramValue = new ParameterValue[allParameters[distributionIndex].length];
			int parameterIndex;
			double min, max, value, minorTickSpacing;
			
			for (int i=0; i < parameterSliders.length; i++){
				
				parameterIndex = allParameters[distributionIndex][i];
				min = parameterSpecs[parameterIndex][0];
				max = parameterSpecs[parameterIndex][1];
				value = parameterSpecs[parameterIndex][2];
				minorTickSpacing = parameterSpecs[parameterIndex][3];
				
				parameterSliders[i] = new DoubleJSlider(min, max, value, minorTickSpacing);	                    
				parameterSliders[i].addChangeListener(this);	                                        				
				parameterSliders[i].setBounds(i*140, 285, 90, 40);	                    
				add(parameterSliders[i]);

				parameterLabels[i] = new Label(DistributionFactory.getParameterDescription(parameterIndex)+ " =");
				parameterLabels[i].setBounds(i*140+90, 285, 17, 40);
				add(parameterLabels[i]);

				paramValue[i] = new ParameterValue();
				paramValue[i].value = parameterSliders[i].getDoubleValue();
				paramValue[i].parameterIndex = parameterIndex; 
				parameterValues[i] = new Label(new DecimalFormat("0.0").format(paramValue[i].value));
				parameterValues[i].setBounds(i*140+107, 285, 33, 40);
				add(parameterValues[i]);
			}                		
		}
	}

	//EJUST
	public void stateChanged(ChangeEvent e) {
		DoubleJSlider source = (DoubleJSlider)e.getSource();
		if (!source.getValueIsAdjusting())
			for (int i = 0; i < parameterSliders.length; i++)
				if (source == parameterSliders[i]){
					paramValue[i].value = parameterSliders[i].getDoubleValue();
					paramValue[i].parameterIndex = allParameters[distributionTypes.getSelectedIndex()][i];
					parameterValues[i].setText(new DecimalFormat("0.0").format(paramValue[i].value));
				}
	}

	/** Returns the currently selected roaduser type */
	public int getSpawnType() {
		int[] types = RoaduserFactory.getConcreteTypes();
		return types[spawnTypes.getSelectedIndex()];
	}

	/** EJUST: Returns the currently selected distribution type */
	public int getDistributionType() {
		int[] types = DistributionFactory.getDistributionTypes();
		return types[distributionTypes.getSelectedIndex()];
	}

	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		if (source == setSpawn || source == spawnFreq) setSpawnType();
		else if (source == wqlLink) track(TrackerFactory.SPECIAL_QUEUE);
		else if (source == tdLink) track(TrackerFactory.SPECIAL_DELAY);		
		else if (source == twtLink) track(TrackerFactory.SPECIAL_WAITING_TIME); /*EJUST*/
		else if (source == tascLink) track(TrackerFactory.SPECIAL_ABSOLUTE_STOPS_COUNT); /*EJUST*/
		else if (source == tscLink) track(TrackerFactory.SPECIAL_STOPS_COUNT); /*EJUST*/
		else if (source == ttLink) track(TrackerFactory.SPECIAL_TRIP_TIME); /*EJUST*/
		else if (source == ruaLink) track(TrackerFactory.SPECIAL_ROADUSERS);
		else if (source == rurLink) track(TrackerFactory.SPECIAL_REJECTED_ROADUSERS); /*EJUST*/
		else if (source == roadLink) confd.selectObject(edgenode.getRoad());
		else if (source == nodeLink) confd.selectObject(edgenode.getRoad().getOtherNode(edgenode));
		else if (source == dSpawnLink) confd.setConfigPanel(new SimDynamicSpawnPanel(confd,edgenode)); //(DOAS 05)
	}

	public void track(int type) {
		SimController sc = (SimController)confd.getController();
		try {
			TrackerFactory.showTracker(sc.getSimModel(), sc, edgenode, type);
		}
		catch (GLDException ex) {
			Controller.reportError(ex);
		}
	}
}
