package gld.config;

import java.awt.Graphics;
import java.awt.Canvas;
import java.awt.Color;
import gld.infra.PODrivelanes;
import gld.infra.Drivelane;
import java.awt.SystemColor;
import java.util.Vector;
import java.util.LinkedList;
import gld.infra.Roaduser;
import gld.infra.ObservedRoaduser;
import gld.infra.Beliefstate;
import gld.infra.Sign;
import java.util.ConcurrentModificationException;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
 /*POMDPGLD*/
public class HistogramPanel extends Canvas
{
    Drivelane drivelane;
    int numblocks = 0;
    int binwidth = 0;
    int height = 140;
    int bottomMargin = 20;

    Vector[] bins;

    public HistogramPanel(Drivelane lane)
    {
        drivelane = lane;
        if( drivelane != null ) {
            numblocks = drivelane.getCompleteLength();
            binwidth = getWidth() / numblocks;
        }

        this.setBackground(SystemColor.control);
    }

    public void setLane(Drivelane lane) {
        drivelane = lane;
        reset();
    }

    public void reset() {


            if(drivelane != null && drivelane.getSign().getType() == Sign.TRAFFICLIGHT)
            {
                numblocks = drivelane.getCompleteLength();
                binwidth = getWidth() / numblocks;
                bins = new Vector[numblocks];
                if(drivelane instanceof PODrivelanes)
                {
                    Beliefstate bfs = ((PODrivelanes)drivelane).getBeliefstate();
                    Vector ruProbs = bfs.getAllProbabilityDistributions();
                    for(int i = 0; i < ruProbs.size(); i++)
                    {
                        ObservedRoaduser vru = (ObservedRoaduser)ruProbs.get(i);
                        int pos = (int) Math.ceil(vru.getPos()); /*EJUST: case to int*/
                        if(bins[pos] == null)
                        {
                            bins[pos] = new Vector();
                        }
                        bins[pos].add(vru);
                    }
                }
                else
                {
                    LinkedList queue = drivelane.getQueue();
                    for(int i = 0; i < queue.size(); i++)
                    {
                        Roaduser ru = (Roaduser)queue.get(i);
                        /*EJUST: Position pos will be considered by the controller while vehicle is on its way from position pos to position pos-1*/
                        int pos = (int) Math.ceil(ru.getPosition());
                        if(bins[pos] == null)
                        {
                            bins[pos] = new Vector();
                        }
                        ObservedRoaduser vru = new ObservedRoaduser(0, 0, ru);
                        vru.setTimesSeen(1);
                        bins[pos].add(vru);
                    }
                }

            }
            else
            {
                //if no drivelane, or a drivelane without a trafficlight attached is selected
                //we dont want a histogram be painted...
                numblocks = 0;

            } // try

        repaint();
    }




    public void paint(Graphics g) {
        height = getHeight() - bottomMargin;

        for(int i = 0; i < numblocks; i++)
        {
            g.setColor(Color.white);
            g.fillRect(binwidth*i,0,binwidth,height -1);
            Vector data = bins[i];
            if( data != null ) {
                double subBinWidth = ((double)(binwidth-2)/(double)data.size());
                int usedBinWidth = 0;
                for(int j = 0; j < data.size(); j++)
                {
                    ObservedRoaduser vru = (ObservedRoaduser)data.get(j);
                    double prob = vru.getTimesSeen();
                    int subHeight = (int)((1 - prob) * height);
                    Color c = vru.getRoaduser().getColor();
                    g.setColor(c);
                    if(j == data.size() -1) {
                        g.fillRect(binwidth*i+ usedBinWidth+1, subHeight, binwidth-1,height -1 -subHeight);
                    }
                    else if(j % 2 == 0) {
                        g.fillRect(binwidth*i+ usedBinWidth+1, subHeight, (int)Math.ceil(subBinWidth),height -1 -subHeight);
                        usedBinWidth += (int)Math.ceil(subBinWidth);
                    }
                    else {
                        g.fillRect(binwidth*i+ usedBinWidth+1, subHeight, (int)subBinWidth,height -1 -subHeight);
                        usedBinWidth += (int)subBinWidth;
                    }

                }
            }
            g.setColor(Color.black);
            g.drawRect(binwidth*i,0,binwidth,height -1);
            if( i % 2 == 0) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setFont(new Font("Monospace", Font.PLAIN, 10));
                FontRenderContext frc = g2.getFontRenderContext();
                String s = Integer.toString(i);
                Rectangle2D bounds = g2.getFont().getStringBounds(s, frc);
                float width = (float) bounds.getWidth();
                float centerX = ((float)(binwidth*i))+((float)binwidth/(float)2);
                int baselineY = height+10;
                g2.drawString(s, Math.round(centerX - width / 2), baselineY);
            }
        }

    }
}
