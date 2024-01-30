
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


import gld.infra.Drivelane;
import gld.infra.Roaduser;
import gld.infra.RoaduserFactory;
import gld.infra.Sign;
import gld.utils.DoubleUtils;
import gld.utils.Hyperlink;

import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;


/**
 *
 * @author Group GUI
 * @version 1.0
 */

public class SimDrivelanePanel extends ConfigPanel implements ActionListener, ItemListener
{
	Drivelane lane;

	Hyperlink alphaLink, betaLink, roadLink;

	/**EJUST: length of the lane*/
	Label length;
	
	/**EJUST: tail of the lane*/
	Label tailLength;
	
	/**EJUST: complete length of the lane*/
	Label completeLength;
	
	Label sign, allows;

	Queue queue;
	Checkbox queueType;
	ScrollPane sp;

    HistogramPanel histPlane; /*POMDPGLD*/

	public SimDrivelanePanel(ConfigDialog cd, Drivelane l) {
		super(cd);

		Label rlab = new Label("Part of:");
		rlab.setBounds(0, 0, 80 /*EJUST comment 100*/, 20);
		add(rlab);
		
		roadLink = new Hyperlink();
		roadLink.addActionListener(this);
		roadLink.setBounds(80 /*EJUST comment 100*/, 0, 100, 20);
		add(roadLink);
		
		Label alab = new Label("Leads to:");
		alab.setBounds(0, 20, 80 /*EJUST comment 100*/, 20);
		add(alab);
		
		alphaLink = new Hyperlink();
		alphaLink.addActionListener(this);
		alphaLink.setBounds(80 /*EJUST comment 100*/, 20, 100, 20);
		add(alphaLink);

		Label blab = new Label("Comes from:");
		blab.setBounds(0, 40, 80 /*EJUST comment 100*/, 20);
		add(blab);
		
		betaLink = new Hyperlink();
		betaLink.addActionListener(this);
		betaLink.setBounds(80 /*EJUST comment 100*/, 40, 100, 20);
		add(betaLink);

		//EJUST
		length = new Label();
		length.setBounds(0, 70, 180, 20);
		add(length);
		
		//EJUST
		tailLength = new Label();
		tailLength.setBounds(0, 90, 180, 20);
		add(tailLength);
		
		//EJUST
		completeLength = new Label();
		completeLength.setBounds(0, 110, 180, 20);
		add(completeLength);
		
		sign = new Label();
		sign.setBounds(0, 130 /*EJUST comment 70*/, 180 /*EJUST comment 200*/, 20);
		add(sign);

		allows = new Label();
		allows.setBounds(0, 150 /*EJUST comment 90*/, 180 /*EJUST comment 200*/, 20);
		add(allows);
		
		/*POMDPGLD*/
		histPlane = new HistogramPanel(lane);
        histPlane.setBounds(0, 210 /*EJUST comment 120*/, 200, 115);
        add(histPlane);
		/*POMDPGLD*/
				
		queue = new Queue();
		sp = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
		sp.setBounds(180 /*EJUST comment 210*/, 0, 250 /*EJUST comment 150*/, 200);
		sp.add(queue);
		add(sp);

		queueType = new Checkbox("Show free spaces");
		queueType.addItemListener(this);
		queueType.setBounds(210, 210, 150, 20);
		add(queueType);

		setLane(l);
	}
	
	private class Queue extends Panel
	{
		Label[] labels = { };
		Hyperlink[] links = { };
		int counter; // internal counter used by addString and addLink
		public Queue() {
			setLayout(null);
		}
		
		public synchronized void setLane(Drivelane lane) {
			removeAll();
			
			int len = lane.getCompleteLength();
			links = new Hyperlink[len];
			labels = new Label[len];
			Hyperlink link;
			Label lab;
			
			for (int i=0; i < len; i++) {
				link = new Hyperlink();
				link.addActionListener(new RULinkListener(null));
				link.setEnabled(false);
				link.setVisible(false);
				link.setBounds(0, i * 20, 230 /*EJUST comment 130*/ , 20);
				add(link); 
				links[i] = link;
				
				lab = new Label();
				lab.setBounds(0, i * 20, 230 /*EJUST comment 130*/ , 20);
				lab.setVisible(false);
				add(lab);
				labels[i] = lab;
			}
			reset();
		}

		public synchronized void reset() {
			int resetTryCount = 10;
			boolean done = false;
			
			while (resetTryCount>0 && !done) try
			{
				boolean sf = queueType.getState();
				ListIterator li = lane.getQueue().listIterator();
				int pos = 0;
				Roaduser ru;
				counter = 0;

				while (li.hasNext())
				{
					ru = (Roaduser)li.next();
					if (sf) {
						while (ru.getPosition() > pos) {
							addString(pos + ": Free block");
							pos++;
						} // while
					} // if					
					addLink("p: " + DoubleUtils.truncateDouble(ru.getPosition(), 4) + 
							/*EJUST*/ ", s: " + DoubleUtils.truncateDouble(ru.getSpeed(), 4)+
							/*EJUST*/ ", d: " + DoubleUtils.truncateDouble(ru.getDistance(), 4)+
							": " + ru.getName(), ru);
					pos += ru.getLength();
					if (sf) {
						for (int i=1; i < ru.getLength(); i++)
							addString(pos + ": -");
					} // if					
				} // while
				
				if (pos < lane.getCompleteLength() && sf) {
					while (pos < lane.getCompleteLength()) {
						addString(pos + ": Free block");
						pos++;
					} // while
				} // if

				for (int i=counter; i < labels.length; i++) {
					labels[i].setVisible(false);
					labels[i].repaint();
					links[i].setEnabled(false);
					links[i].setVisible(false);
					links[i].repaint();
				} // for
				done = true;
			} // try
			
			// SimModel thread changed the queue while we were updating, try again.
			catch (ConcurrentModificationException e) {
				done = false;
				resetTryCount--;
				reset();
			}
			catch (NullPointerException e) {
				done = false;
				resetTryCount--;
				reset();
			}
			doLayout();
		}
		
		private void addString(String text) {
			Label lab = labels[counter];
			lab.setVisible(true);
			links[counter].setVisible(false);
			links[counter].setEnabled(false);
			if (!lab.getText().equals(text)) lab.setText(text);
			counter++;
		}
		
		private void addLink(String text, Roaduser ru) {
			Hyperlink link = links[counter];
			link.setVisible(true);
			link.setEnabled(true);
			labels[counter].setVisible(false);

			if (!link.getText().equals(text)) link.setText(text);
			((RULinkListener)link.getActionListeners()[0]).setRoaduser(ru);
			
			counter++;
		}

		private class RULinkListener implements ActionListener
		{
			Roaduser ru;
			public RULinkListener(Roaduser ru) { this.ru = ru; }
			public void setRoaduser(Roaduser ru) { this.ru = ru; }
			public void actionPerformed(ActionEvent e) { confd.showRoaduser(ru); }
		}
		
		public Dimension getPreferredSize() { return new Dimension(230 /*EJUST comment 130*/, counter * 20); }
	}

	public void reset() {
		if (lane.getSign().getType() == Sign.NO_SIGN)
			sign.setText("Drivelane has no trafficlight");
		else
			sign.setText(lane.getNumRoadusersWaiting() + " waiting for trafficlight");


		queue.reset();
		
		/*histPlane.reset(); POMDPGLD*/
		
		sp.doLayout();
	}

	public void setLane(Drivelane l) {
		lane = l;
		confd.setTitle(lane.getName());
		queue.setLane(lane);
		
		 /*histPlane.setLane(lane);POMDPGLD*/
		
		reset();
		
		alphaLink.setText(lane.getNodeLeadsTo().getName());
		betaLink.setText(lane.getNodeComesFrom().getName());
		roadLink.setText(lane.getRoad().getName());
		
		//EJUST
		length.setText("Drivelane is " + lane.getLength() + " units long");
		tailLength.setText("Drivelane tail is " + lane.getTailLength() + " units long");
		completeLength.setText("Total length is " + lane.getCompleteLength() + " units long");
		
		allows.setText("Drivelane allows " + RoaduserFactory.getDescByType(lane.getType()));
	}

	public void itemStateChanged(ItemEvent e) {
		reset();		
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == alphaLink)
			confd.selectObject(lane.getNodeLeadsTo());
		else if (source == betaLink)
			confd.selectObject(lane.getNodeComesFrom());
		else if (source == roadLink)
			confd.selectObject(lane.getRoad());
	}
}