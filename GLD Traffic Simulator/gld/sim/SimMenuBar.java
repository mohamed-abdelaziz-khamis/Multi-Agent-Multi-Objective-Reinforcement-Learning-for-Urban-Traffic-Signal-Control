
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

package gld.sim;

import gld.FileMenu;
import gld.HelpMenu;
import gld.Model;
import gld.algo.dp.DPFactory;
import gld.algo.tlc.TLCFactory;
import gld.config.POOptionsFactory;
import gld.infra.DrivelaneFactory;
import gld.sim.stats.TrackerFactory;
import gld.utils.CheckMenu;

import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

/**
 *
 * The MenuBar for the editor
 *
 * @author Group GUI
 * @version 1.0
 */

public class SimMenuBar extends MenuBar {
	SimController controller;

	SpeedMenu speedMenu;
	TLCMenu tlcMenu;
	DPMenu dpMenu;

	DLMenu dlMenu; //(RM 06) /*POMDPGLD: Drive lane type menu*/
	POMenu poMenu; //(RM 06) /*POMDPGLD: Partial Observability menu*/	

	CheckboxMenuItem viewEnabled;
	CheckboxMenuItem timeStepCounterEnabled;
	CheckboxMenuItem hecAddon;        //(DOAS 05)
	CheckboxMenuItem accidentsOn;     //(DOAS 06)
	CheckboxMenuItem removeStuckCars; //(DOAS 06)
	CheckboxMenuItem reroutingOn;     //(DOAS 06)

	public SimMenuBar(SimController sc, String[] speedTexts) {

		String[] trackers = {
				"Total waiting queue length",
				"Total roadusers arrived",
				"Total roadusers not arrived yet", //EJUST
				"Total average speed of all roadusers", //EJUST
				"Total colearn average speed of all roadusers", //EJUST
				"Average trip delay",
				"Average trip waiting time", //EJUST
				"Colearn average trip waiting time", //EJUST
				"Average trip time", //EJUST
				"Colearn average trip time", //EJUST
				"Average junction delay",
				"Average junction waiting time", //EJUST
				"Colearn average junction waiting time", //EJUST
				"Percentage of roadusers arrived/entered (%)", // EJUST
				"Percentage of roadusers rejected/generated (%)", // EJUST
				"Average number of roadusers waiting", //EJUST
				"Maximum number of roadusers waiting", //EJUST
				"Average trip absolute stops count", //EJUST
				"Average trip stops count", //EJUST
				"Number of accidents",   // DOAS 06
				"Number of removed cars" // DOAS 06
		};

		String[] dps = DPFactory.getDescriptions();

		controller = sc;

		Menu menu; 
		MenuItem item;

		add(new FileMenu(controller, false));

		/*  Simulation */

		menu = new Menu("Simulation");
		add(menu);
		SimListener simListener = new SimListener();

		item = new MenuItem("Do one step", new MenuShortcut(KeyEvent.VK_D));
		menu.add(item);
		item.addActionListener(simListener);

		item = new MenuItem("Run", new MenuShortcut(KeyEvent.VK_R));
		menu.add(item);
		item.addActionListener(simListener);

		item = new MenuItem("Pause", new MenuShortcut(KeyEvent.VK_U));
		menu.add(item);
		item.addActionListener(simListener);

		item = new MenuItem("Stop", new MenuShortcut(KeyEvent.VK_P));
		menu.add(item);
		item.addActionListener(simListener);

		item = new MenuItem("Run Series", new MenuShortcut(KeyEvent.VK_S));
		menu.add(item);
		item.addActionListener(simListener);

		/*POMDPGLD*/
		//RM 06
		menu.add(new MenuItem("-"));

		item = new MenuItem("Calibrate spawnrates");
		menu.add(item);
		item.addActionListener(simListener);

		item = new MenuItem("Set global spawnrate");
		menu.add(item);
		item.addActionListener(simListener);
		/*POMDPGLD*/

		/*EJUST*/
		//Intelligent Driver Model
		menu.add(new MenuItem("-"));
		item = new MenuItem("Intelligent Driver Model");
		menu.add(item);
		item.addActionListener(simListener);
		menu.add(new MenuItem("-"));
		/*EJUST*/

		/* Speed */

		speedMenu = new SpeedMenu(speedTexts);
		menu.add(speedMenu);

		/* Statistics */

		menu = new Menu("Statistics");
		add(menu);
		StatsListener statsListener = new StatsListener();

		item = new MenuItem("Show statistics", new MenuShortcut(KeyEvent.VK_T));
		menu.add(item);
		item.addActionListener(statsListener);

		Menu submenu = new Menu("Track");

		for(int i=0; i<trackers.length; i++) {
			item = new MenuItem(trackers[i]);
			submenu.add(item);
			item.addActionListener(statsListener);
		}
		menu.add(submenu);

		menu.add(new MenuItem("-"));

		CheckboxMenuItem citem = new CheckboxMenuItem("Toggle in-view statistics", false);
		menu.add(citem);
		citem.addItemListener(statsListener);

		/* Options */

		menu = new Menu("Options");
		add(menu);
		OptionMenuListener ol = new OptionMenuListener();

		viewEnabled = new CheckboxMenuItem("Toggle view", true);
		menu.add(viewEnabled);
		viewEnabled.setName("view");
		viewEnabled.addItemListener(ol);

		timeStepCounterEnabled = new CheckboxMenuItem("Toggle time step counter", true);
		menu.add(timeStepCounterEnabled);
		timeStepCounterEnabled.setName("timestepcounter");
		timeStepCounterEnabled.addItemListener(ol);

		menu.add(new MenuItem("-"));

		tlcMenu = new TLCMenu();
		menu.add(tlcMenu);

		dpMenu = new DPMenu(dps);
		menu.add(dpMenu);

		//(DOAS 05)
		hecAddon = new CheckboxMenuItem("Toggle HEC(if supported by TLC)", false);
		menu.add(hecAddon);
		hecAddon.setName("hec");
		hecAddon.addItemListener(ol);
		menu.add(new MenuItem("-"));

		/*POMDPGLD*/
		//(RM 06)
		dlMenu = new DLMenu();
		menu.add(dlMenu);
		menu.add(new MenuItem("-"));

		//(RM 06)
		poMenu = new POMenu();
		menu.add(poMenu);
		menu.add(new MenuItem("-"));
		/*POMDPGLD*/

		// (DOAS 06)
		accidentsOn = new CheckboxMenuItem("Accidents", false /*POMDPGLD: true --> false*/);  // The default check value must correspond to the default value in the SimController
		menu.add(accidentsOn);
		accidentsOn.setName("accidents");
		accidentsOn.addItemListener(ol);
		menu.add(new MenuItem("-"));

		// (DOAS 06)
		removeStuckCars = new CheckboxMenuItem("Remove stuck cars", false);  // The default check value must correspond to the default value in the SimController
		menu.add(removeStuckCars);
		removeStuckCars.setName("removeStuckCars");
		removeStuckCars.addItemListener(ol);
		menu.add(new MenuItem("-"));

		// (DOAS 06)
		reroutingOn = new CheckboxMenuItem("Rerouting", false /*POMDPGLD: true --> false*/);  // The default check value must correspond to the default value in the SimController
		menu.add(reroutingOn);
		reroutingOn.setName("reroutingOn");
		reroutingOn.addItemListener(ol);
		menu.add(new MenuItem("-"));

		item = new MenuItem("Open editor", new MenuShortcut(KeyEvent.VK_E));
		menu.add(item);
		item.addActionListener(ol);

		item = new MenuItem("Settings...");
		menu.add(item);
		item.addActionListener(ol);

		add(new HelpMenu(controller));
	}

	public SpeedMenu getSpeedMenu() { return speedMenu; }
	public TLCMenu getTLCMenu() { return tlcMenu; }
	public DPMenu getDPMenu() { return dpMenu; }

	protected void setViewEnabled(boolean b) {
		viewEnabled.setState(b);
	}

	protected void setTimeStepCounterEnabled(boolean b) {
		timeStepCounterEnabled.setState(b);
	}

	public class SpeedMenu extends CheckMenu implements ItemListener {
		public SpeedMenu(String[] texts) {
			super("Speed", texts);
			addItemListener(this);
			select(1);
		}

		public void itemStateChanged(ItemEvent e) {
			controller.setSpeed(getSelectedIndex());
		}
	}

	/*POMDPGLD*/
	//(RM 06)  Menu to control which types of drivelanes are to be used
	public class DLMenu extends CheckMenu implements ItemListener
	{
		public DLMenu()
		{
			super("Drivelanes", DrivelaneFactory.dlDescs);
			addItemListener(this);
			select(Model.dltype);
		}

		public void setCurrentItem() {
			select(Model.dltype);
		}

		public void itemStateChanged(ItemEvent e)
		{
			if(getSelectedIndex() != Model.dltype) {
				controller.setDrivelanetype(getSelectedIndex());
				select(Model.dltype);
			}
		}

	}

	//(RM 06)  Menu to control which types of drivelanes are to be used
	public class POMenu extends Menu implements ItemListener
	{
		String [] cat_names = POOptionsFactory.cat_names;
		String [][] cats = POOptionsFactory.cats;

		CheckMenu[] submenus;
		int selectedMenu = -1;

		public POMenu()
		{
			super("Partially Observability options");
			submenus = new CheckMenu[cat_names.length];
			CheckMenu subsubmenu;
			String[] texts;

			for(int i = 0; i < cats.length; i++)
			{
				texts = new String[cats[i].length];
				for(int j = 0; j < texts.length; j++)
				{
					texts[j] = cats[i][j];
				}
				subsubmenu = new CheckMenu(cat_names[i], texts);
				add(subsubmenu);
				subsubmenu.addItemListener(this);

				submenus[i] = subsubmenu;
			}

			setPOoption(new int[]{ POOptionsFactory.defaultN, POOptionsFactory.defaultB, POOptionsFactory.defaultC});
		}


		public void itemStateChanged(ItemEvent e)
		{
			CheckMenu cm = (CheckMenu) e.getItemSelectable();
			for (int i = 0; i < submenus.length; i++)
			{
				if (submenus[i] == cm)
				{
					selectedMenu = i;
					controller.setPOoption(i, cm.getSelectedIndex());
				}
			}
		}

		public void setNoiseMethod(int i) {
			submenus[0].deselectAll();
			submenus[0].select(i);
		}

		public void setBeliefState(int i) {
			submenus[1].deselectAll();
			submenus[1].select(i);
		}

		public void setRuBehaviour(int i) {
			submenus[2].deselectAll();
			submenus[2].select(i);
		}

		public void setPOoption(int [] options) {
			for (int i = 0; i < submenus.length; i++)
			{
				submenus[i].deselectAll();
				submenus[i].select(options[i]);
			}
		}
	}
	/*POMDPGLD*/

	public class TLCMenu extends Menu implements ItemListener {
		CheckMenu[] submenus;
		int selectedMenu = -1;

		public TLCMenu() {
			super("Traffic light controller");

			String[] tlcCats = TLCFactory.getCategoryDescs();
			String[] tlcDescs = TLCFactory.getTLCDescriptions();
			int[][] allTLCs = TLCFactory.getCategoryTLCs();

			submenus = new CheckMenu[tlcCats.length];
			CheckMenu subsubmenu;
			String[] texts;

			for (int i=0; i < tlcCats.length; i++) {
				texts = new String[allTLCs[i].length];
				
				for (int j=0; j < texts.length; j++)
					texts[j] = tlcDescs[allTLCs[i][j]];
				
				subsubmenu = new CheckMenu(tlcCats[i], texts);
				add(subsubmenu);
				subsubmenu.addItemListener(this);

				submenus[i] = subsubmenu;
			}

			setTLC(0, 0);
		}

		public void itemStateChanged(ItemEvent e) {
			CheckMenu cm = (CheckMenu)e.getItemSelectable();
			for (int i=0; i < submenus.length; i++) {
				if (submenus[i] == cm) {
					selectedMenu = i;
					controller.setTLC(i, cm.getSelectedIndex());
				} else submenus[i].deselectAll();
			}
		}

		public void setTLC(int cat, int tlc) {
			for (int i=0; i < submenus.length; i++)
				submenus[i].deselectAll();
			submenus[cat].select(tlc);
		}

		public int getCategory() {
			return selectedMenu;
		}
		public int getTLC() {
			return submenus[selectedMenu].getSelectedIndex();
		}
	}

	public class DPMenu extends CheckMenu implements ItemListener {
		public DPMenu(String[] dps) {
			super("Driving policy", dps);
			addItemListener(this);
			select(0);
		}

		public void itemStateChanged(ItemEvent e) {
			controller.setDrivingPolicy(getSelectedIndex());
		}
	}


	/*============================================*/
	/* Listeners                                  */
	/*============================================*/

	/** Listens to the "Statistics" menu */
	public class StatsListener implements ActionListener, ItemListener {
		public void actionPerformed(ActionEvent e) {
			String sel = e.getActionCommand();

			if(controller.getSimModel().getInfrastructure().getNumNodes() == 0) {
				controller.showError("Please load an infrastructure or simulation before opening any statistics windows.");
				return;
			}

			if (sel.equals("Show statistics")) controller.showStatistics();
			
			else if (sel.equals("Total waiting queue length")) controller.showTracker(TrackerFactory.TOTAL_QUEUE);
			else if (sel.equals("Total roadusers arrived")) controller.showTracker(TrackerFactory.TOTAL_ROADUSERS);
			else if (sel.equals("Total roadusers not arrived yet")) controller.showTracker(TrackerFactory.TOTAL_ROADUSERS_NOT_ARRIVED_YET); //EJUST
			else if (sel.equals("Total average speed of all roadusers")) controller.showTracker(TrackerFactory.TOTAL_AVERAGE_SPEED); //EJUST
			else if (sel.equals("Total colearn average speed of all roadusers")) controller.showTracker(TrackerFactory.TOTAL_COLEARN_AVERAGE_SPEED); //EJUST
			
			else if (sel.equals("Percentage of roadusers arrived/entered (%)")) controller.showTracker(TrackerFactory.ARRIVED_ROADUSERS_PERCENTAGE); //EJUST
			
			else if (sel.equals("Percentage of roadusers rejected/generated (%)")) controller.showTracker(TrackerFactory.REJECTED_ROADUSERS_PERCENTAGE); //EJUST
			
			else if (sel.equals("Average trip delay")) controller.showTracker(TrackerFactory.TOTAL_DELAY);			
			else if (sel.equals("Average trip waiting time")) controller.showTracker(TrackerFactory.TOTAL_WAITING_TIME); //EJUST
						
			else if (sel.equals("Colearn average trip waiting time")) controller.showTracker(TrackerFactory.TOTAL_COLEARN_WAITING_TIME); //EJUST
			else if (sel.equals("Average trip time")) controller.showTracker(TrackerFactory.TOTAL_TRIP_TIME); //EJUST
			else if (sel.equals("Colearn average trip time")) controller.showTracker(TrackerFactory.TOTAL_COLEARN_TRIP_TIME); //EJUST
			
			else if (sel.equals("Average junction delay")) controller.showTracker(TrackerFactory.TOTAL_JUNCTION_DELAY);
			
			else if (sel.equals("Average junction waiting time")) controller.showTracker(TrackerFactory.TOTAL_JUNCTION_WAITING_TIME); //EJUST
			else if (sel.equals("Colearn average junction waiting time")) controller.showTracker(TrackerFactory.TOTAL_COLEARN_JUNCTION_WAITING_TIME); //EJUST
			
			else if (sel.equals("Average number of roadusers waiting")) controller.showTracker(TrackerFactory.AVERAGE_LANE_NUM_ROADUSERS_WAITING); //EJUST
			else if (sel.equals("Maximum number of roadusers waiting")) controller.showTracker(TrackerFactory.MAX_LANE_NUM_ROADUSERS_WAITING); //EJUST
			
			else if (sel.equals("Average trip absolute stops count")) controller.showTracker(TrackerFactory.TOTAL_ABSOLUTE_STOPS_COUNT); //EJUST
			else if (sel.equals("Average trip stops count")) controller.showTracker(TrackerFactory.TOTAL_STOPS_COUNT); //EJUST
			
			else if (sel.equals("Number of accidents")) controller.showTracker(TrackerFactory.ACCIDENTS_COUNT); // DOAS 06
			else if (sel.equals("Number of removed cars")) controller.showTracker(TrackerFactory.REMOVEDCARS_COUNT); // DOAS 06
		}

		public void itemStateChanged(ItemEvent e) {
			CheckboxMenuItem item = (CheckboxMenuItem)e.getItemSelectable();
			if(item.getState()) controller.enableOverlay();
			else controller.disableOverlay();
		}
	}


	/** Listens to the "Options" menu */
	public class OptionMenuListener implements ActionListener, ItemListener {
		public void actionPerformed(ActionEvent e) {
			String ac = e.getActionCommand();
			if (ac == "Open editor") controller.openEditor();
			else if (ac == "Settings...") controller.showSettings();
		}

		public void itemStateChanged(ItemEvent e) {
			CheckboxMenuItem item = (CheckboxMenuItem)e.getItemSelectable();
			boolean enable = item.getState();
			if(item.getName().equals("view"))
				controller.setViewEnabled(enable);
			else if(item.getName().equals("hec"))       //(DOAS 05)
				controller.setHecAddon(enable);
			else if(item.getName().equals("accidents")) //(DOAS 06)
				controller.setAccidents(enable);
			else if(item.getName().equals("removeStuckCars")) //(DOAS 06)
				controller.setRemoveStuckCars(enable);
			else if(item.getName().equals("reroutingOn")) //(DOAS 06)
				controller.setRerouting(enable);
			else
				controller.setTimeStepCounterEnabled(enable);
		}
	}


	/** Listens to the "Simulation" menu */
	public class SimListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String sel = e.getActionCommand();
			if (sel.equals("Do one step")) controller.doStep();
			else if (sel.equals("Run")) controller.unpause();
			else if (sel.equals("Pause")) controller.pause();
			else if (sel.equals("Stop")) controller.stop();
			else if (sel.equals("Run Series")) controller.runSeries();

			/*POMDPGLD*/
			else if (sel.equals("Calibrate spawnrates"))
				controller.runCalibration();
			else if (sel.equals("Set global spawnrate"))
				controller.showSpawnrateConfig();
			/*POMDPGLD*/

			//EJUST
			else if (sel.equals("Intelligent Driver Model"))
				controller.showIDMConfig();
		}
	}
}
