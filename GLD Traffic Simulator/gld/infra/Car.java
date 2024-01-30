
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

import gld.sim.SimModel;

import gld.idm.MicroModelGLD;
import gld.idm.LaneChangeGLD;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

/**
 * A Car is the standard menace on our Roads.
 *
 * @author Group Datastructures
 * @version 1.0
 *
 * A Car is the standard menace on our Roads.
 */

public class Car extends Automobile 
{
	protected final int type = RoaduserFactory.getTypeByDesc("Car");
	
	protected final int passengers = 1;
	
	//SBC
	protected final int length = 3; //2;
	protected final double speed_max = 20; 		/*EJUST: int --> double*/
	protected double speed_current = 10; //2; 	/*EJUST: int --> double*/
	protected double speed_previous = 10; 	  	/*EJUST*/
	protected double acceleration = 4; 			/*EJUST: int --> double*/
	protected double stopforce = 8; 			/*EJUST: int --> double*/	  
	protected int platoon_value = 1;	
	
	/*EJUST*/
	public MicroModelGLD model;
	public LaneChangeGLD lanechange;
    public double tdelay=0.0;
    public double Tdelay=1.6;
	
	/*POMDPGLD*/
	protected int driving_behaviour = SimModel.car_driving_behaviour;
	
	public Car(Node new_startNode, Node new_destNode, double pos /*EJUST: int --> double*/, 
				MicroModelGLD model /*EJUST*/, LaneChangeGLD lanechange /*EJUST*/) {
		
		super(new_startNode, new_destNode, pos);
		color = RoaduserFactory.getColorByType(type);
		
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		
		if(r==0) r = (int)(Math.random() * 160);
		if(g==0) g = (int)(Math.random() * 160);
		if(b==0) b = (int)(Math.random() * 160);
		
		color = new Color(r,g,b);
        headcolor = new Color(0,255,0);
        
        this.model = model; /*EJUST*/
        this.lanechange = lanechange; /*EJUST*/
	}

	/** Empty constructor for loading */
	public Car() { }

	public String getName() { return "Car"; }

	/** Returns the speed of this Roaduser in blocks per timeStep */
	public double getSpeed() { return speed_current; } /*EJUST: int --> double*/
	
	/** EJUST: Returns the previous speed of this Roaduser in blocks per timeStep */
	public double getPrevSpeed() { return speed_previous; }
	
	/*POMDPGLD*/
    public void updateSpeed() {
    	
        double rnd;
        
        if(SimModel.use_fixed_speed_randomizer) {
            Random FixedRND = new Random((int)Math.ceil(position)); /*EJUST: cast to int*/
            rnd = FixedRND.nextDouble();
        }
        else {
            rnd = Roaduser.RND.nextDouble();
        }
        
        double nspeed = speed_current; /*EJUST: int --> double*/

        if( driving_behaviour == Roaduser.SPETHIAL ) {
             if(speed_current == /*SBC: 1-->5*/ 5 ) {  nspeed = (rnd > (double)0.7000 )? /*SBC: 2-->10*/ 10 : /*SBC: 1-->5*/ 5; }
             else if(speed_current == /*SBC: 2-->10*/ 10) {  nspeed = (rnd > (double)0.8000 )? ((rnd > 0.9000)? /*SBC: 3-->15*/ 15 : /*SBC: 1-->5*/ 5) : /*SBC: 2-->10*/ 10; }
             else if(speed_current == /*SBC: 3-->15*/ 15) {  nspeed = (rnd > (double)0.8000 )? ((rnd > 0.9000)? /*SBC: 4-->20*/ 20 : /*SBC: 2-->10*/ 10) : /*SBC: 3-->15*/ 15; }
             else if(speed_current == /*SBC: 4-->20*/ 20) {  nspeed = (rnd > (double)0.7000 )? /*SBC: 3-->15*/ 15 : /*SBC: 4-->20*/ 20; }
        }

        if( driving_behaviour == Roaduser.UNIFORM ) {
             if(speed_current == /*SBC: 1-->5*/ 5) {  nspeed = (rnd > (double)0.5000 )? /*SBC: 2-->10*/ 10 : /*SBC: 1-->5*/ 5; }
             else if(speed_current == /*SBC: 2-->10*/ 10) {  nspeed = (rnd > (double)0.5000 )? ((rnd > 0.7500)? /*SBC: 3-->15*/ 15 : /*SBC: 1-->5*/ 5) : /*SBC: 2-->10*/ 10; }
             else if(speed_current == /*SBC: 3-->15*/ 15) {  nspeed = (rnd > (double)0.5000 )? /*SBC: 2-->10*/ 10 : /*SBC: 3-->15*/ 15; }
        }

        if( driving_behaviour == Roaduser.GAUSSIAN ) {
            if(speed_current == /*SBC: 1-->5*/ 5)
            {
                nspeed = (rnd > (double)0.8808) ? /*SBC: 2-->10*/ 10 : /*SBC: 1-->5*/ 5;
            }
            else if(speed_current == /*SBC: 2-->10*/ 10)
            {
                nspeed = (rnd > (double)0.7870) ? ((rnd > 0.8935) ? /*SBC: 3-->15*/ 15 : /*SBC: 1-->5*/ 5) : /*SBC: 2-->10*/ 10;
            }
            else if(speed_current == /*SBC: 3-->15*/ 15)
            {
                nspeed = (rnd > (double)0.8808) ? /*SBC: 2-->10*/ 10 : /*SBC: 3-->15*/ 15;
            }
        }
//        if (speed != nspeed) { System.out.println("Roaduser "+ this + " changed speed from " + speed + " to "+ nspeed); }
        speed_previous = speed_current; //EJUST
        speed_current = nspeed;
        this.headcolor = (speed_current == /*SBC: 1-->5*/ 5)? Color.RED : ((speed_current == /*SBC: 2-->10*/ 10 )? Color.ORANGE : Color.GREEN);
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
	
	//SBC
	public double getStopDistance() { /*EJUST: int --> double*/
		return (speed_current*speed_current)/(2*stopforce);
	}
	
	//SBC
	public int getPlatoonValue() { return platoon_value; }
	

	public int getLength() { return length; }
	public int getType() { return type; }
	public int getNumPassengers() { return passengers; }
	
	//SBC
	public double getAcceleration() { return acceleration; } /*EJUST: int --> double*/

	/*EJUST*/
    public void accelerate(Roaduser frontRoaduser, double dt){    	
    	double acc = model.calcAcc(this, frontRoaduser);
    	/*if (frontRoaduser!=null && this.getPosition() < frontRoaduser.getPosition())
        	System.out.println("accelerate(me): " +
        					"	me: 	" + this.getPosition() +
        					"	frontRoaduser:	" + frontRoaduser.getPosition());*/
    	
    	speed_previous = speed_current;
    	speed_current += acc*dt;
    	if (speed_current < 0){
    		speed_current = 0;
    	}
    }
    
    /*EJUST*/
    public boolean change(boolean leftLane, Roaduser frontRoaduserOld, Roaduser frontRoaduserNew, Roaduser backRoaduserNew){
    	
        /*if (backRoaduserNew!=null &&  backRoaduserNew.getPosition() < frontRoaduserNew.getPosition())
        	System.out.println("change lane: " +
        					"	backRoaduserNew: 	" + backRoaduserNew.getPosition() +
        					"	frontRoaduserNew:	" + frontRoaduserNew.getPosition());*/
    	
    	return lanechange.changeOK(leftLane, this, frontRoaduserOld, frontRoaduserNew, backRoaduserNew);
    }    
    
    /*EJUST*/
    public MicroModelGLD model(){return model;}    
    
    /*EJUST*/
    public void setModel(MicroModelGLD model){ this.model=model;}
    
    /*EJUST*/
    public boolean timeToChange(double dt){
    	tdelay = tdelay+dt;
    	if (tdelay >= Tdelay){
    	   tdelay = tdelay-Tdelay;
    	   return true;
    	}
    	else {return false;}
    }	
	
	public void paint(Graphics g, int x, int y, float zf) {
		paint(g,x,y,zf,(double)0.0);
	}

	public void paint(Graphics g, int x, int y, float zf, double dlangle) {
		g.setColor(color);
		double angle = dlangle - Math.toRadians(45.0);
        double diff = Math.sin(Math.toRadians(45.0));

		int[] cx = new int[4];
                
		cx[0] = (int)(Math.round((double)x + Math.sin(angle - Math.toRadians(45.0))  /* * 3 */ * diff));
        cx[1] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(135.0)) /* * 3 */ * diff));
		cx[2] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(180.0) + Math.atan(0.5)) * 2 /* * 6 */));
		cx[3] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(270.0) - Math.atan(0.5)) * 2 /* * 6 */));

		int[] cy = new int[4];
		cy[0] = (int)(Math.round((double)y + Math.cos(angle - Math.toRadians(45.0)) /* * 3 */ * diff));
		cy[1] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(135.0)) /* * 3 */ * diff));
		cy[2] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(180.0) + Math.atan(0.5)) * 2 /* * 6 */));
		cy[3] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(270.0) - Math.atan(0.5)) * 2 /* * 6 */));

		g.fillPolygon(cx,cy,4);

        g.setColor(headcolor);

        cx[0] = (int)(Math.round((double)x + Math.sin(angle) /* * 3 */));
        cx[1] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(90.0)) /* * 3 */));
        cx[2] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(135.0)) /* * 3 */ * diff));
		cx[3] = (int)(Math.round((double)x + Math.sin(angle - Math.toRadians(45.0)) /* * 3 */ * diff));


        cy[0] = (int)(Math.round((double)y + Math.cos(angle) /* * 3 */));
        cy[1] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(90.0)) /* * 3 */));
        cy[2] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(135.0)) /* * 3 */ * diff));
        cy[3] = (int)(Math.round((double)y + Math.cos(angle - Math.toRadians(45.0)) /* * 3 */ * diff));

        g.fillPolygon(cx,cy,4);

                
		//g.fillRect((int)((x - 3) * zf),(int)((y - 3) * zf),(int) (7 * zf),(int) (7 * zf));
	}


    // Specific XMLSerializable implementation

 	public String getXMLName ()
 	{ 	
 		return parentName+".roaduser-car";
 	}
}
