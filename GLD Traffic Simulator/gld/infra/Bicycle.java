
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

package gld.infra;

import gld.idm.LaneChangeGLD;
import gld.idm.MicroModelGLD;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Cycling through our world on two wheels. Aint it great to be alive?
 * 
 * @author Group Datastructures
 * @version 1.0
 *
 */

public class Bicycle extends Roaduser
{
	protected final int type = RoaduserFactory.getTypeByDesc("Bicycle");

	protected final int passengers = 1;

	//SBC
	protected final int length = 2; //1;
	protected final double speed_max = 6; /*EJUST: int --> double*/
	protected double speed_current = 4; //1; /*EJUST: int --> double*/
	protected double speed_previous = 4;  /*EJUST*/
	protected final double acceleration = 2; /*EJUST: int --> double*/
	protected final double stopforce = 3; /*EJUST: int --> double*/

	/**EJUST*/
	public MicroModelGLD model;
    public LaneChangeGLD lanechange;
    public double tdelay=0.0;
    public double Tdelay=2.2;
    /*EJUST*/
	
	public Bicycle(Node new_startNode, Node new_destNode, int pos /*EJUST: int --> double*/, 
					MicroModelGLD model /*EJUST*/, LaneChangeGLD lanechange /*EJUST*/) {
		super(new_startNode, new_destNode, pos);
		// make color little bit more random
		color = RoaduserFactory.getColorByType(type);
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		if(r==0) r = (int)(Math.random() * 160);
		if(g==0) g = (int)(Math.random() * 160);
		if(b==0) b = (int)(Math.random() * 160);
		color = new Color(r,g,b);
		this.model = model; /*EJUST*/
		this.lanechange = lanechange; /*EJUST*/
	}
	
	/** Empty constructor for loading 
	 */
	public Bicycle() {}
	
	public String getName() { return "Bicycle"; }
	
	public void updateSpeed() { } /*POMDPGLD*/
	
	/** Returns the speed of this Roaduser in blocks per timeStep */
	public double getSpeed() { return speed_current; } /*EJUST: int --> double*/
	
	/** EJUST: Returns the previous speed of this Roaduser in blocks per timeStep */
	public double getPrevSpeed() { return speed_previous; }
	
	//SBC
	public void setSpeed(double newSpeed) { speed_current = newSpeed; }
	
	//SBC
	public double accelerate(double goalSpeed, double dt /*EJUST*/) /*EJUST: int --> double*/ 
	{ 
		speed_previous = speed_current; //EJUST
		if (speed_current + acceleration*dt <= speed_max) {
			if (speed_current + acceleration*dt > goalSpeed)
				speed_current = goalSpeed;
			else 
				speed_current = speed_current + acceleration*dt;
		}
		else 
			speed_current = speed_max;
		return speed_current;
	}
	
	//SBC
	public double deaccelerate(double goalSpeed, double dt /*EJUST*/) /*EJUST: int --> double*/
	{
		speed_previous = speed_current; //EJUST
		if (speed_current - stopforce*dt >= 0) {
			if (speed_current - stopforce*dt < goalSpeed)				
				speed_current = goalSpeed;
			else
				speed_current = speed_current - stopforce*dt;
		}
		else
			speed_current = 0;		
		return speed_current;
	}
	
	//SBC
	public double adjustSpeedTo(double goalSpeed, double dt /*EJUST*/) { /*EJUST: int --> double*/
		if (speed_current < goalSpeed)
			return accelerate(goalSpeed, dt);
		else return deaccelerate(goalSpeed, dt);
	}
	
	public int getLength() { return length; }
	public int getNumPassengers() { return passengers; }	
	public int getType() { return type; }

	//SBC
	public double getAcceleration() { return acceleration; } /*EJUST: int --> double*/
	public double getStopDistance() { return (speed_current*speed_current)/(2*stopforce);} /*EJUST: int --> double*/
	public int getPlatoonValue() {return 1;}
	
    /*EJUST*/
    public void accelerate(Roaduser frontRoaduser, double dt){
    	double acc=model.calcAcc(this, frontRoaduser);
    	speed_previous = speed_current;
    	speed_current+=acc*dt;
    	if (speed_current<0){
    		speed_current=0;
    	}
    }
    
    /*EJUST*/
    public boolean change(boolean leftLane, Roaduser frontRoaduserOld, Roaduser frontRoaduserNew, Roaduser backRoaduserNew){
    	return lanechange.changeOK(leftLane, this, frontRoaduserOld, frontRoaduserNew, backRoaduserNew);
    }
    
    /*EJUST*/
    public MicroModelGLD model(){return model;}
    
    /*EJUST*/
    public void setModel(MicroModelGLD model){ this.model=model;}
    
    /*EJUST*/
    public boolean timeToChange(double dt){
    	tdelay=tdelay+dt;
    	if (tdelay>=Tdelay){
    	   tdelay=tdelay-Tdelay;
    	   return true;
    	}
    	else {return false;}
     }
    
    
	public void paint(Graphics g, int x, int y, float zf) {}
	
	public void paint(Graphics g, int x, int y, float zf, double dlangle)
	{
		g.setColor(color);
    	double angle = dlangle - Math.toRadians(45.0);
    	int[] cx = new int[4];
    	cx[0] = (int)(Math.round((double)x + Math.sin(angle)));
    	cx[1] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(90.0))));
    	cx[2] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(180.0) + Math.atan(0.5))/* * 2*/));
    	cx[3] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(270.0) - Math.atan(0.5))/* * 2*/));

    	int[] cy = new int[4];
    	cy[0] = (int)(Math.round((double)y + Math.cos(angle)));
    	cy[1] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(90.0))));
    	cy[2] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(180.0) + Math.atan(0.5))/* * 2*/));
    	cy[3] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(270.0) - Math.atan(0.5))/* * 2*/));

    	g.fillPolygon(cx,cy,4);
	}
	
    // Specific XMLSerializable implementation 
    
 	public String getXMLName ()
 	{ 	
 		return parentName+".roaduser-bicycle";
 	}
}