
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

package gld.sim.stats;

import java.awt.Color;
import java.awt.Graphics;

/**
*
* Extension of StatisticsView showing a summary of all data.
*
* @author Group GUI
* @version 1.0
*/

public class StatsSummaryView extends StatisticsView
{
	protected final static int LINE_HEIGHT = 20;
	protected int x, y;
	
	public StatsSummaryView(StatisticsController parent, StatisticsModel stats)
	{
		super(parent, stats);
		parent.setScrollMax(0,0);
	}
	
	public void paintStats(Graphics g)
	{
		g.setColor(Color.black);
		g.setFont(infoFont);

		y = paintArea.y + LINE_HEIGHT;
				
		infoLine(g, "Nodes: " + stats.getNumSpecialNodes() + " special nodes, " + stats.getNumJunctions() + " junctions, " 
					/*EJUST*/ + stats.getNumInboundLanes() + " inbound lanes");
		
		emptyLine();
		
		infoLine(g, "Total number of roadusers that have arrived at their destinations: " + stats.getRoadusersArrived());
		infoLine(g, "Total number of roadusers that have not arrived at their destinations yet: " + stats.getRoadusersNotArrivedYet()); /*EJUST*/
		infoLine(g, "Total number of roadusers that have arrived at their destinations or not arrived yet: " + stats.getRoadusersTotalCount()); /*EJUST*/
		
		emptyLine();
		
		infoLine(g, "Average trip delay (based on all roadusers arrived): " + stats.getAllTimeTripDelay());
		
		/*EJUST*/
		infoLine(g, "Average trip waiting time (based on all roadusers arrived): " + stats.getAllTimeTripWT());
		infoLine(g, "Average trip waiting time (based on all roadusers not arrived yet): " + stats.getAllTimeColearnTripWT());
		infoLine(g, "Average trip waiting time (based on all roadusers arrived or not arrived yet): " + stats.getAllTimeTotalTripWT());
		
		/*EJUST*/
		emptyLine();
		
		/*EJUST*/
		infoLine(g, "Average trip time (based on all roadusers arrived): " + stats.getAllTimeTripT());
		infoLine(g, "Average trip time (based on all roadusers not arrived yet): " + stats.getAllTimeColearnTripT());
		infoLine(g, "Average trip time (based on all roadusers arrived or not arrived yet): " + stats.getAllTimeTotalTripT());
		
		/*EJUST*/
		emptyLine();
		
		if(stats.getRoadusersArrived() != stats.getLastXTripCount()){
			infoLine(g, "Average trip delay (based on last "+stats.getLastXTripCount()+" roadusers arrived): " + stats.getLastXTripDelay());
			
			infoLine(g, "Average trip waiting time (based on last "+stats.getLastXTripCount()+" roadusers arrived): " + stats.getLastXTripWT()); /*EJUST*/
			infoLine(g, "Average trip time (based on last " + stats.getLastXTripCount() + " roadusers arrived): " + stats.getLastXTripT()); /*EJUST*/
		}
		
		/*EJUST*/
		if(stats.getRoadusersNotArrivedYet() != stats.getLastXTripNotArrivedYetCount()) {			
			infoLine(g, "Average trip waiting time (based on last " + stats.getLastXTripNotArrivedYetCount()+ " roadusers not arrived yet): " + stats.getLastXColearnTripWT());
			infoLine(g, "Average trip time (based on last " + stats.getLastXTripNotArrivedYetCount()+ " roadusers not arrived yet): " + stats.getLastXColearnTripT());
		}
		
		/*EJUST*/
		if(stats.getRoadusersTotalCount() != stats.getLastXTripTotalCount()) {			
			infoLine(g, "Average trip waiting time (based on last " + stats.getLastXTripTotalCount() + " roadusers arrived or not arrived yet): " + stats.getLastXTotalTripWT());
			infoLine(g, "Average trip time (based on last " + stats.getLastXTripTotalCount() + " roadusers arrived or not arrived yet): " + stats.getLastXTotalTripT());
		}
		
		emptyLine(); /*EJUST*/
		
		infoLine(g, "Total number of junction crossings: " + stats.getJunctionCrossings());
		infoLine(g, "Total number of roadusers that have not crossed their current junctions yet: " + stats.getRoadusersNotCrossedYet()); /*EJUST*/
		infoLine(g, "Total number of junction crossings or roadusers not crossed yet: " + stats.getJunctionTotalCount()); /*EJUST*/
		
		emptyLine();
		
		infoLine(g, "Average junction delay (based on all junction crossings): " + stats.getAllTimeJunctionDelay());
		
		/*EJUST*/
		infoLine(g, "Average junction waiting time (based on all junction crossings): " + stats.getAllTimeJunctionWT());
		infoLine(g, "Average junction waiting time (based on all roadusers not crossed yet): " + stats.getAllTimeColearnJunctionWT());
		infoLine(g, "Average junction waiting time (based on all junction crossings or roadusers not crossed yet): " + stats.getAllTimeTotalJunctionWT());
		
		/*EJUST*/
		emptyLine();
		
		if(stats.getJunctionCrossings() != stats.getLastXJunctionCount()){
			infoLine(g, "Average junction delay (based on last "+stats.getLastXJunctionCount()+" junction crossings): " + stats.getLastXJunctionDelay());
			infoLine(g, "Average junction waiting time (based on last "+stats.getLastXJunctionCount()+" junction crossings): " + stats.getLastXJunctionWT()); /*EJUST*/
		}
		
		/*EJUST*/
		if(stats.getRoadusersNotCrossedYet() != stats.getLastXJunctionNotCrossedYetCount()){			
			infoLine(g, "Average junction waiting time (based on last " + stats.getLastXJunctionNotCrossedYetCount() + " roadusers not crossed yet): " + stats.getLastXColearnJunctionWT());
		}
		
		/*EJUST*/
		if(stats.getJunctionTotalCount() != stats.getLastXJunctionTotalCount()){			
			infoLine(g, "Average junction waiting time (based on last " + stats.getLastXJunctionTotalCount() + " junction crossings or roadusers not crossed yet): " + stats.getLastXTotalJunctionWT());
		}
	}
	
	protected void infoLine(Graphics g, String s) {
		g.drawString(s, x, y);
		y += LINE_HEIGHT;
	}
	
	protected void emptyLine() { y += LINE_HEIGHT; }

	protected void paintAreaChanged() {
		x = paintArea.x;
	}
}