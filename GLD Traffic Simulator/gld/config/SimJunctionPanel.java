
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

import gld.Controller;
import gld.GLDException;
import gld.infra.Junction;
import gld.infra.Node;
import gld.infra.Road;
import gld.infra.RoaduserFactory;
import gld.sim.SimController;
import gld.sim.stats.TrackerFactory;
import gld.utils.Hyperlink;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 *
 * @author Group GUI
 * @version 1.0
 */

public class SimJunctionPanel extends ConfigPanel implements ActionListener, ItemListener
{
	Junction junction;

	Hyperlink[] roadLinks;
	Hyperlink adLink, awtLink /*EJUST*/, rucLink;
	Label nrsigns, width, adLabel, awtLabel /*EJUST*/;
	
	//SBC
	Label gwStartLabel, gwFinishLabel, phaseDiff;
	
	Choice typeList;
	Checkbox awtType;
	
	public SimJunctionPanel(ConfigDialog cd, Junction j) {
		super(cd);
		
		String[] dirs = { "north", "east", "south", "west" };
		
		roadLinks = new Hyperlink[4];
		
		for (int i=0; i < 4; i++) {
			Label lab = new Label("Road " + dirs[i] + ": ");
			lab.setBounds(0, i * 20, 100, 20);
			add(lab);
			
			roadLinks[i] = new Hyperlink();
			roadLinks[i].addActionListener(this);
			roadLinks[i].setBounds(100, i * 20, 100, 20);
			add(roadLinks[i]);
		}
		
		nrsigns = new Label();
		nrsigns.setBounds(200, 0, 200, 20);
		add(nrsigns);
		
		width = new Label();
		width.setBounds(200, 20, 200, 20);
		add(width);

		adLink = new Hyperlink("Track average delay");
		adLink.addActionListener(this);
		adLink.setBounds(0, 100, 200, 20);
		add(adLink);

		/*EJUST*/
		awtLink = new Hyperlink("Track average waiting time");
		awtLink.addActionListener(this);
		awtLink.setBounds(0, 120, 200, 20);
		add(awtLink);
		
		rucLink = new Hyperlink("Track roadusers crossed");
		rucLink.addActionListener(this);
		rucLink.setBounds(0, 140 /*EJUST comment 120*/, 200, 20);
		add(rucLink);
		
		Label lab = new Label("Average delay for");
		lab.setBounds(0, 170 /*EJUST comment 150*/, 150, 20);
		add(lab);
		
		/*EJUST*/
		lab = new Label("Average waiting time for");
		lab.setBounds(0, 190, 150, 20);
		add(lab);
		
		typeList = new Choice();
		int nr = RoaduserFactory.statArrayLength();
		for (int i=0; i < nr; i++)
			typeList.add(RoaduserFactory.getDescByStatIndex(i));
		
		typeList.addItemListener(this);
		typeList.setBounds(150,  170 /*EJUST comment 150*/, 100, 20);
		add(typeList);
		
		adLabel = new Label();
		adLabel.setBounds(260, 170 /*EJUST comment 150*/, 100, 20);
		add(adLabel);
		
		/*EJUST*/
		awtLabel = new Label();
		awtLabel.setBounds(260, 190, 100, 20);
		add(awtLabel);
		
		awtType = new Checkbox("Show of last " + Node.STAT_NUM_DATA + " roadusers");
		awtType.addItemListener(this);
		awtType.setBounds(0, 215 /*EJUST comment 175*/, 200, 20);
		add(awtType);

		//SBC
		gwStartLabel = new Label();
		gwStartLabel.setBounds(0, 235 /*EJUST comment 195*/, 250, 20);
		add(gwStartLabel);
		
		gwFinishLabel = new Label();
		gwFinishLabel.setBounds(0, 255 /*EJUST comment 215*/, 250, 20);
		add(gwFinishLabel);
		
		phaseDiff = new Label();
		phaseDiff.setBounds(0, 275 /*EJUST comment 235*/, 250, 20);
		add(phaseDiff);
		//SBC

		setJunction(j);
	}

	public void reset() {
		adLabel.setText("is " + junction.getStatistics()[typeList.getSelectedIndex()].getAvgDelay(!awtType.getState()));
		
		/*EJUST*/
		awtLabel.setText("is " + junction.getStatistics()[typeList.getSelectedIndex()].getAvgWaitingTime(!awtType.getState()));
	}

	public void setJunction(Junction j) {
		junction = j;
		confd.setTitle(junction.getName());
		reset();

		Road[] roads = junction.getAllRoads();
		
		for (int i=0; i < 4; i++) {
			if (roads[i] != null) {
				roadLinks[i].setText(roads[i].getName());
				roadLinks[i].setEnabled(true);
			}
			else {
				roadLinks[i].setText("null");
				roadLinks[i].setEnabled(false);
			}
		}

		nrsigns.setText("Junction has " + junction.getNumRealSigns() + " traffic lights");
		width.setText("Junction is " + junction.getWidth() + " units width in lanes"); /*EJUST: units wide --> units width in lanes*/
		
		//SBC
		gwStartLabel.setText("Junction is start of green wave:  " + junction.isGreenWaveStart());
		gwFinishLabel.setText("Junction is finish of green wave: " + junction.isGreenWaveFinish());
		phaseDiff.setText("This junction has a phasediff = " + junction.getPhaseDiff());
		//SBC
	}

	public void actionPerformed(ActionEvent e)
	{
		SimController sc = (SimController)confd.getController();

		Object source = e.getSource();
		for (int i=0; i < 4; i++)
			if (source == roadLinks[i]) confd.selectObject(junction.getAllRoads()[i]);
		
		try {
			if (source == adLink)
				TrackerFactory.showTracker(sc.getSimModel(), sc, junction, TrackerFactory.JUNCTION_DELAY);
			
			/*EJUST*/
			if (source == awtLink)
				TrackerFactory.showTracker(sc.getSimModel(), sc, junction, TrackerFactory.JUNCTION_WAITING_TIME);
			
			else if (source == rucLink)
				TrackerFactory.showTracker(sc.getSimModel(), sc, junction, TrackerFactory.JUNCTION_ROADUSERS);
		}
		catch (GLDException ex) {
			Controller.reportError(ex);
		}
	}
	
	public void itemStateChanged(ItemEvent e) {
		reset();
	}
}