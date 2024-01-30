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

import gld.DoubleJSlider;
import gld.distributions.DistributionFactory;
import gld.distributions.ParameterValue;
import gld.idm.WeatherFactory;
import gld.infra.EdgeNode;
import gld.infra.RoaduserFactory;
import gld.infra.SpawnFrequencyTimeSteps;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.ItemSelectable;
import java.awt.Label;
import java.awt.List;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 *
 * @author Group GUI
 * @version 1.0
 */
//(DOAS 05)
public class SimDynamicSpawnPanel extends ConfigPanel implements ActionListener, ItemListener, ChangeListener /*EJUST*/
{
	EdgeNode eNode;

	List[] ruList;
	int numItems = 0;
	Choice spawnTypes;

	TextField spawnFreq;
	TextField timeStepInput;

	Button setSpawn;
	Button deleteSpawn;

	Vector[] ruTimeStepsLists;

	int deleteType = -1;
	int deleteTimeStep = -1;

	//EJUST
	Choice distributionTypes;

	double[][] 			parameterSpecs = DistributionFactory.getParameterSpecs();
	int[][] 			allParameters = DistributionFactory.getDistributionParameters();
	String[] 			distributionDescs = DistributionFactory.getDistributionTypeDescs();
	DoubleJSlider[] 	parameterSliders = {};
	Label[] 			parameterLabels = {};
	Label[] 			parameterValues = {};	
	ParameterValue[] 	paramValue = {}; /** The value of each parameter */
	int vpos;
	//EJUST
	
	//(DOAS 05)
	public SimDynamicSpawnPanel(ConfigDialog cd, EdgeNode e)
	{
		super(cd);
		eNode = e;

		String[] descs = RoaduserFactory.getConcreteTypeDescs();

		numItems = descs.length;
		ruList = new List[numItems];

		for (int i=0; i < numItems; i++)
		{
			int pos = 0;
			if (i % 2 != 0) pos = 220; /*EJUST: pos was first 200. To have size for Distribution(param1=v1, param2=v2,...)*/;
			Label lab = new Label("Time steps list for " + descs[i]);
			lab.setBounds(pos, ((int)i / 2) * 100 /*EJUST: 70*/, 200, 20);
			add(lab);

			ruList[i] = new List();
			ruList[i].setBounds(pos, ((int)i / 2) * 100 /*EJUST: 70*/+ 25, 210 /*EJUST: 150*/, 70 /*EJUST: 40*/); //To have size for Distribution(param1=v1, param2=v2,...)
			ruList[i].addItemListener(this);
			add(ruList[i]);

		}

		vpos = 100 /*EJUST: 70*/ * ((int) Math.ceil((double)numItems/ 2)) ;

		Label lab = new Label("Set Dynamic spawnfrequency for");
		lab.setBounds(0, vpos + 10, 200, 20);
		add(lab);

		spawnTypes = new Choice();
		spawnTypes.addItemListener(this);

		for (int i=0; i < descs.length; i++)
			spawnTypes.addItem(descs[i]);

		spawnTypes.setBounds(0, vpos + 35, 100, 20);
		add(spawnTypes);

		lab = new Label("on time step");
		lab.setBounds(110 /*EJUST: 105*/, vpos + 35, 90 /*EJUST: 50*/, 20);
		add(lab);

		timeStepInput = new TextField();
		timeStepInput.setBounds(200 /*EJUST: 160*/, vpos + 35, 60, 20);
		timeStepInput.addActionListener(this);
		add(timeStepInput);

		lab = new Label("is");
		lab.setBounds(265 /*EJUST: 225*/, vpos + 35, 20, 20);
		add(lab);

		spawnFreq = new TextField();
		spawnFreq.setBounds(290 /*EJUST: 250*/, vpos + 35, 40, 20);
		spawnFreq.addActionListener(this);
		add(spawnFreq);

		setSpawn = new Button("Add");
		setSpawn.addActionListener(this);
		setSpawn.setBounds(335 /*EJUST: 295*/, vpos + 35, 50, 20);
		add(setSpawn);

		//EJUST
		lab = new Label("Or Interarrival times follow Distribution");
		lab.setBounds(0, vpos + 65, 250, 20);
		add(lab);

		distributionTypes = new Choice();
		distributionTypes.addItemListener(this);

		for (int i=0; i < distributionDescs.length; i++)
			distributionTypes.addItem(distributionDescs[i]);

		distributionTypes.setBounds(290, vpos + 65, 100, 20);
		add(distributionTypes);

		loadDistributionParameters();
		//EJUST

		deleteSpawn = new Button("Delete timeStep 0 from type " + descs[0]);
		deleteSpawn.setVisible(false);
		deleteSpawn.addActionListener(this);
		deleteSpawn.setBounds(0,vpos + 120/*EJUST: 70*/, 250,25);
		add(deleteSpawn);

		reset();
	}

	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(Color.black);
		//int vpos = 70 * ((int) Math.ceil((double)numItems/ 2)); /*Commented by EJUST*/
		g.drawLine(0, vpos, ConfigDialog.PANEL_WIDTH, vpos);
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

	public void reset() {
		int [] types = RoaduserFactory.getConcreteTypes();
		ruTimeStepsLists = new Vector[types.length];
		boolean containsAnyItem = false;
		for (int i = 0; i < types.length; i++) {
			ruList[i].removeAll();
			Vector dSpawnList = eNode.dSpawnTimeStepsForRu(types[i]);
			for (int j = 0; j < dSpawnList.size(); j++) {
				SpawnFrequencyTimeSteps sf = (SpawnFrequencyTimeSteps)dSpawnList.get(j);
				ruList[i].add(sf.toString());
				containsAnyItem = true;
			}
			ruTimeStepsLists[i] = dSpawnList;
		}
		if (containsAnyItem == false)
		{
			deleteSpawn.setVisible(false);
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == setSpawn)
		{	
			try {
				int timeStep = Integer.parseInt(timeStepInput.getText());			
				try {
					float fr = -1; //EJUST
					if (!spawnFreq.getText().trim().isEmpty()){ //EJUST
						fr = Float.parseFloat(spawnFreq.getText());
						if (fr < 0) confd.showError("Spawn frequency must be greater than or equal zero."); //EJUST
						else eNode.addDSpawnTimeSteps(getSpawnType(), timeStep, fr, 
								getDistributionType() /*EJUST*/, paramValue  /*EJUST*/,
								WeatherFactory.DRY /*EJUST*/);
					}
					else eNode.addDSpawnTimeSteps(getSpawnType(), timeStep, fr, 
								getDistributionType() /*EJUST*/, paramValue  /*EJUST*/,
								WeatherFactory.DRY /*EJUST*/);
				}
				catch (NumberFormatException ex) {
					confd.showError("You must enter a float in the Spawn frequencies box.");
				}
			}
			catch (NumberFormatException ex) {
				confd.showError("You must enter an Integer in the TimeSteps box.");
			}
		}
		else if (source == deleteSpawn)
		{
			SpawnFrequencyTimeSteps sf = (SpawnFrequencyTimeSteps)ruTimeStepsLists[deleteType].get(deleteTimeStep);
			eNode.deleteDSpawnTimeSteps(sf.ruType, sf.timeStep);
		}
		reset();
	}

	public void itemStateChanged(ItemEvent e) {
		ItemSelectable es = e.getItemSelectable();
		
		if (es == distributionTypes) loadDistributionParameters();   //EJUST             	

		else for (int i = 0; i < ruList.length; i++) {
			if (es == ruList[i])
			{
				if(deleteType > -1 && deleteTimeStep > -1) ruList[deleteType].deselect(deleteTimeStep);
				deleteType = i;
				deleteTimeStep = ruList[i].getSelectedIndex();
				if (deleteTimeStep > -1)
				{
					int timeStep = ((SpawnFrequencyTimeSteps) ruTimeStepsLists[deleteType].get(deleteTimeStep)).timeStep;
					String[] descs = RoaduserFactory.getConcreteTypeDescs();
					deleteSpawn.setLabel("Delete spawn at timeStep " + timeStep + " for type " + descs[deleteType]);
					deleteSpawn.setVisible(true);
				}
				else
					deleteSpawn.setVisible(false);
			}
		}
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
				parameterSliders[i].setBounds(i*140, vpos + 85, 90, 40);	                    
				add(parameterSliders[i]);

				parameterLabels[i] = new Label(DistributionFactory.getParameterDescription(parameterIndex)+ " =");
				parameterLabels[i].setBounds(i*140+90, vpos + 85, 17, 40);
				add(parameterLabels[i]);

				paramValue[i] = new ParameterValue();
				paramValue[i].value = parameterSliders[i].getDoubleValue();
				paramValue[i].parameterIndex = parameterIndex; 
				parameterValues[i] = new Label(new DecimalFormat("0.0").format(paramValue[i].value));
				parameterValues[i].setBounds(i*140+107, vpos + 85, 33, 40);
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
}
