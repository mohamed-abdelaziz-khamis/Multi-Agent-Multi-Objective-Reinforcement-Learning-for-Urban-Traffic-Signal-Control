
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

import gld.infra.RoaduserFactory;
import gld.infra.Node.NodeStatistics;

import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * Extension of StatisticsView showing the statistics in a table.
 *
 * @author Group GUI
 * @version 1.0
 */

public class StatsTableView extends StatisticsView
{
	public final static int ROW_HEIGHT = 20;
	public final static int COL_WIDTH = 100;
	public final static int SPACING = 5;

	protected int numRows, numCols;
	
	protected final static String[] colDescs = {"node", "ru type", "# roadusers", 
												"avg delay", 
												"avg waiting time" /*EJUST*/,
												"avg trip time" /*EJUST*/
												};
	
	
	public StatsTableView(StatisticsController parent, StatisticsModel stats)
	{
		super(parent, stats);
	}
	
	public void paintStats(Graphics g)
	{
		NodeStatistics[][] nodeStats = stats.getNodeStatistics();
		
		g.setColor(Color.black);
		
		//Draw the table itself
		for(int i=0; i<=numCols; i++)
			g.drawLine(paintArea.x + i*COL_WIDTH, paintArea.y, paintArea.x + i*COL_WIDTH, paintArea.y + numRows * ROW_HEIGHT);
		
		for(int i=0; i<=numRows; i++)
			g.drawLine(paintArea.x, paintArea.y + i*ROW_HEIGHT, paintArea.x + numCols*COL_WIDTH, paintArea.y + i*ROW_HEIGHT);
	
		for(int i=0; i<numCols; i++)
			g.drawString(colDescs[i+horScroll], paintArea.x + i*COL_WIDTH + SPACING, paintArea.y + ROW_HEIGHT - SPACING);

		//Now draw the data in the table
		for(int row=1; row<numRows; row++)
		{
			String[] data = getTableData(nodeStats,row-1);
			for(int col=0; col<numCols; col++)
				g.drawString(data[col], paintArea.x + col * COL_WIDTH + SPACING, paintArea.y + (row+1) * ROW_HEIGHT - SPACING);
		}
	}
	
	/** 
	* Returns the text to be shown in a table row.
	* @param nodeStats Where the data should come from.
	* @param row The row where the data goes.
	*/
	protected String[] getTableData(NodeStatistics[][] nodeStats, int row)
	{
		int id = (verScroll + row) / nodeStats[0].length;
		int statIndex = (verScroll + row) % nodeStats[0].length;
		
		NodeStatistics ns = nodeStats[id][statIndex];
			
		String[] data = new String[numCols];
		
		for(int i=0; i<numCols; i++)
			data[i] = getCellData(horScroll + i, id, statIndex, ns);
		
		return data;
	}
	
	protected String getCellData(int col, int id, int statIndex, NodeStatistics ns)
	{
		switch(col)
		{
			case 0: return (id < stats.getNumSpecialNodes() ? "Special node " : "Junction ") + id;
			case 1: return RoaduserFactory.getDescByStatIndex(statIndex);
			case 2: return ns.getTotalRoadusers() + "";
			case 3: return ns.getAvgDelay(stats.getAllTimeAvg()) + "";
			case 4: return ns.getAvgWaitingTime(stats.getAllTimeAvg()) + ""; /*EJUST*/
			case 5: return ns.getAvgTripTime(stats.getAllTimeAvg()) + ""; /*EJUST*/
		}
		return "";
	}
	
	protected void paintAreaChanged()
	{
		int rowsPossible = Math.max(paintArea.height / ROW_HEIGHT, 1);
		int colsPossible = Math.max(paintArea.width / COL_WIDTH, 1);
		
		NodeStatistics[][] ns = stats.getNodeStatistics();
		
		int rowsNeeded = ns.length * ns[0].length + 1;
		int colsNeeded = colDescs.length; /*EJUST*/
		
		parent.setScrollMax(5 - colsPossible, 1 + rowsNeeded - rowsPossible);
		
		numCols = Math.min(colsPossible, colsNeeded - horScroll);
		numRows = Math.min(rowsPossible, rowsNeeded - verScroll);
	}
}