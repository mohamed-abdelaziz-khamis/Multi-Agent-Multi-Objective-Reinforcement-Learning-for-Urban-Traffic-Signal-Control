
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
 * The Bus. Red, Big, Mean and Lean people moving machine.
 *
 * @author Group Datastructures
 * @version 1.0
 *
 */

public class Bus extends Automobile
{
	protected final int type = RoaduserFactory.getTypeByDesc("Bus");

	protected final int passengers = 10;

	//SBC
	protected final int length = 10; //3;
	protected final double speed_max = 20; /*EJUST: int --> double*/
	protected double speed_current = 10;//2; /*EJUST: int --> double*/
	protected double speed_previous = 10; 	 /*EJUST*/
	protected final double acceleration = 3; /*EJUST: int --> double*/
	protected final double stopforce = 7; /*EJUST: int --> double*/
	protected final int platoon_value = 2;

	/**EJUST*/
	public MicroModelGLD model;
	public LaneChangeGLD lanechange;
    public double tdelay=0.0;
    public double Tdelay=1.9;
	/*EJUST*/
	
	public Bus(Node new_startNode, Node new_destNode, double pos /*EJUST: int --> double*/, 
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
	
	public Bus() { }
	
	public String getName() { return "Bus"; }
	
	/** Returns the speed of this Roaduser in blocks per timeStep */
	public double getSpeed() { return speed_current; } /*EJUST: int --> double*/
	
	/** EJUST: Returns the previous speed of this Roaduser in blocks per timeStep */
	public double getPrevSpeed() { return speed_previous; }
	
	/*POMDPGLD*/
    public void updateSpeed() {
        double rnd =  Roaduser.RND.nextDouble();
        speed_previous = speed_current; //EJUST
        if(speed_current == /*SBC: 1-->5*/ 5) {  speed_current = (rnd > (double)0.5 )? /*SBC: 2-->10*/ 10 : /*SBC: 1-->5*/ 5; };
        if(speed_current == /*SBC: 2-->10*/ 10) {  speed_current = (rnd > (double)0.8 )? ((rnd > 0.85)? /*SBC: 3-->15*/ 15 : /*SBC: 1-->5*/ 5) : /*SBC: 2-->10*/ 10; }
        if(speed_current == /*SBC: 3-->15*/ 15) {  speed_current = (rnd > (double)0.8 )? /*SBC: 2-->10*/ 10 : /*SBC: 3-->15*/ 15; };
     }
	
	//SBC
	public void setSpeed(double newSpeed) { speed_current = newSpeed; } /*EJUST: int --> double*/
	
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
	public int getType() { return type; }
	public int getNumPassengers() { return passengers; }	
	
	//SBC 
	public double getAcceleration() { return acceleration; } /*EJUST: int --> double*/
	public double getStopDistance() {return (speed_current*speed_current)/(2*stopforce);} /*EJUST: int --> double*/
	public int getPlatoonValue() { return platoon_value; }

	/*EJUST*/
    public void accelerate(Roaduser frontRoaduser, double dt){
    	double acc=model.calcAcc(this, frontRoaduser);
    	speed_previous = speed_current;
    	speed_current += acc*dt;
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
	
	public void paint(Graphics g, int x, int y, float zf, double dlangle) {
		g.setColor(color);
		double angle = dlangle - Math.toRadians(45.0);
		int[] cx = new int[4];
		cx[0] = (int)(Math.round((double)x + Math.sin(angle)/* * 3 */));
		cx[1] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(90.0)) /* * 3 */));
		cx[2] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(180.0) + Math.atan(1 - 1.0 / 3.0)) * 3 /* * 10 */));
		cx[3] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(270.0) - Math.atan(1 - 1.0 / 3.0)) * 3 /* * 10 */));
		
		int[] cy = new int[4];
		cy[0] = (int)(Math.round((double)y + Math.cos(angle)/* * 3 */));
		cy[1] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(90.0)) /* * 3 */));
		cy[2] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(180.0) + Math.atan(1 - 1.0 / 3.0)) * 3 /* * 10 */));
		cy[3] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(270.0) - Math.atan(1 - 1.0 / 3.0)) * 3 /* * 10 */));
		
		g.fillPolygon(cx,cy,4);
	}
	
	// Specific XMLSerializable implementation 
    
	public String getXMLName ()
	{ 	
		return parentName+".roaduser-bus";
	}
}