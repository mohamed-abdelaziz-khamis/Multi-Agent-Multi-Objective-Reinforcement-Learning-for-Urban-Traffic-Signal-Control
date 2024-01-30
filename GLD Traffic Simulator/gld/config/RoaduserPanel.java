
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


import gld.infra.Roaduser;
import gld.utils.DoubleUtils;
import gld.utils.Hyperlink;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 *
 * @author Group GUI
 * @version 1.0
 */

public class RoaduserPanel extends ConfigPanel implements ActionListener
{
	Roaduser ru;
	Hyperlink start, dest;
	Label vehicle, driver, delay, waitingTime/*EJUST*/, distance/*EJUST*/, length, speed, stopdistance;
	TextArea description;
	Image picture;

	
	public RoaduserPanel(ConfigDialog cd, Roaduser r) {
		super(cd);
		
		setLayout(null);
		
		Label lab;
		
		lab = new Label("Source:");
		lab.setBounds(0, 0, 80, 20);
		add(lab);
		start = new Hyperlink();
		start.addActionListener(this);
		start.setBounds(80, 0, 100, 20);
		add(start);
		
		
		lab = new Label("Destination:");
		lab.setBounds(0, 20, 80, 20);
		add(lab);
		dest = new Hyperlink();
		dest.addActionListener(this);
		dest.setBounds(80, 20, 100, 20);
		add(dest);
		
		
		lab = new Label("Delay:");
		lab.setBounds(0, 40, 80, 20);
		add(lab);
		delay = new Label();
		delay.setBounds(80, 40, 100, 20);
		add(delay);
		
		/*EJUST*/
		lab = new Label("Waiting Time:");
		lab.setBounds(0, 60, 80, 20);
		add(lab);
		waitingTime = new Label();
		waitingTime.setBounds(80, 60, 100, 20);
		add(waitingTime);
		
		/*EJUST*/
		lab = new Label("Distance:");
		lab.setBounds(0, 80, 80, 20);
		add(lab);
		distance = new Label();
		distance.setBounds(80, 80, 100, 20);
		add(distance);
		
		lab = new Label("Length:");
		lab.setBounds(0, 100 /*EJUST comment 60*/, 80, 20);
		add(lab);
		length = new Label();
		length.setBounds(80, 100 /*EJUST comment 60*/, 100, 20);
		add(length);
		
		
		lab = new Label("Speed:");
		lab.setBounds(0, 120 /*EJUST comment 80*/, 80, 20);
		add(lab);
		speed = new Label();
		speed.setBounds(80, 120 /*EJUST comment 80*/, 100, 20);
		add(speed);
		
		// (SBC) added
		lab = new Label("Stopdistance:");
		lab.setBounds(0, 140 /*EJUST comment 100*/, 80, 20);
		add(lab);
		stopdistance = new Label();
		stopdistance.setBounds(80, 140 /*EJUST comment 100*/, 100, 20);
		add(stopdistance);
		
		lab = new Label("Vehicle:");
		lab.setBounds(0, 160 /*EJUST comment 120*/, 80, 20); //SBC: 100-->120
		add(lab);
		vehicle = new Label();
		vehicle.setBounds(80, 160 /*EJUST comment 120*/, 200, 20); //SBC: 100-->120
		add(vehicle);
		
		lab = new Label("Driver:");
		lab.setBounds(0, 180 /*EJUST comment 140*/, 80, 20); //SBC: 120-->140
		add(lab);
		driver = new Label();
		driver.setBounds(80, 180 /*EJUST comment 140*/, 200, 20); //SBC: 120-->140
		add(driver);
		
		lab = new Label("Description:");
		lab.setBounds(0, 200 /*EJUST comment 160*/, 80, 20); //SBC: 140-->160
		add(lab);
		
		description = new TextArea("", 1, 1, TextArea.SCROLLBARS_NONE);
		description.setEditable(false);
		description.setBounds(0, 220 /*EJUST comment 180*/, 180, 80); //SBC: 160-->180
		add(description);

		setRoaduser(r);
	}

	public void reset() {
		delay.setText("" + DoubleUtils.truncateDouble(ru.getDelay(), 4)); /*EJUST: Truncate Double by 4*/
		waitingTime.setText("" + ru.getWaitingTime()); /*EJUST*/
		distance.setText("" + DoubleUtils.truncateDouble(ru.getDistance(), 4)); /*EJUST*/
		
		/*EJUST: Initially the speed was called in the setRoaduser(r) function
		 * I moved it in the reset() to be updated every timestep
		 * Marco Wiering GLD has fixed speed*/
		speed.setText("" + DoubleUtils.truncateDouble(ru.getSpeed(), 4));
		
		/*EJUST: Initially the stopdistance was called in the setRoaduser(r) function
		 * I moved it in the reset() to be updated every timestep
		 * */
		stopdistance.setText("" + DoubleUtils.truncateDouble(ru.getStopDistance(), 4)); /*EJUST*/
	}

	public void setRoaduser(Roaduser r) {
		ru = r;
		confd.setTitle(ru.getName());
		reset();
		
		start.setText(ru.getStartNode().getName());
		dest.setText(ru.getDestNode().getName());
		
		length.setText("" + ru.getLength());

		
		vehicle.setText(ru.getVehicleName());
		driver.setText(ru.getDriverName());
		description.setText(ru.getDescription());

		String s = ru.getPicture();
		if (s != null) {
	  		Toolkit tk = Toolkit.getDefaultToolkit();
	  		picture = tk.getImage(s);
		}
		else picture = null;
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		if (picture != null)
			g.drawImage(picture, 240 /*EJUST comment 200*/, 0, this);
		else
			g.drawString("No picture available", 240 /*EJUST comment 200*/, 20);
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == start) confd.selectObject(ru.getStartNode());
		else confd.selectObject(ru.getDestNode());
	}
}