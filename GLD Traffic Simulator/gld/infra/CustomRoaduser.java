
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

import java.awt.*;
import java.util.*;
import java.io.*;
import java.applet.*;

import gld.idm.MicroModelGLD;
import gld.idm.LaneChangeGLD;
import gld.xml.*;

/**
 * 
 * @author Group Datastructures
 * @version 1.0
 *
 * This class implements customizable roadusers.
 * It can represent cars, busses and bicycles.
 */

public class CustomRoaduser extends Roaduser
{
	protected int vehicle;
  	protected int driver;
  	protected int[] passengers = { 1 };
  	protected double speed = CustomFactory.getSpeed(this); /*EJUST: int --> double*/
  	protected double prevSpeed; /*EJUST*/

	/**EJUST*/
	public MicroModelGLD model;
	public LaneChangeGLD lanechange;
    public double tdelay=0.0;
    public double Tdelay=1.6;
	/*EJUST*/
	
	public CustomRoaduser(Node start, Node dest, double pos /*EJUST: int --> double*/, 
						MicroModelGLD model /*EJUST*/, LaneChangeGLD lanechange /*EJUST*/) {
		super(start, dest, pos);
        this.model = model; /*EJUST*/
        this.lanechange = lanechange; /*EJUST*/
	}
	
	/** Empty constructor for loading */
	public CustomRoaduser() { }

	/** Returns the ID of the vehicle. */	
	public int getVehicle() { return vehicle; }
	/** Returns the name of the vehicle. */
	public String getVehicleName() { return CustomFactory.getVehicleName(vehicle); }
	/** Sets the ID of the vehicle. */
	public void setVehicle(int v) { vehicle = v; }

	/** Returns the ID of the driver. */
	public int getDriver() { return driver; }
	/** Returns the name of the driver. */
	public String getDriverName() { return CustomFactory.getPersonName(driver); }
	/** Sets the ID of the driver. */
	public void setDriver(int d) { driver = d; }

	/** Returns an array of passenger IDs. */
	public int[] getPassengers() { return passengers; }
	public int getNumPassengers() { return passengers.length; }
	/** Sets the list of passengers. */
	public void setPassengers(int[] p) { passengers = p; }

	/** Returns the speed of this custom. */
	public double getSpeed() { return CustomFactory.getSpeed(this); } /*EJUST: int --> double*/
	
	/** EJUST: Returns the previous speed of this custom. */
	public double getPrevSpeed() { return prevSpeed; }
	
	/*POMDPGLD*/
    public void updateSpeed() {
        double rnd =  Roaduser.RND.nextDouble();
        prevSpeed = speed; //EJUST
        if(speed == /*SBC: 1--> 5*/5) {  speed = (rnd > (double)0.5 )? /*SBC: 2--> 10*/10 : /*SBC: 1--> 5*/5; };
        if(speed == /*SBC: 2--> 10*/10) {  speed = (rnd > (double)0.8 )? ((rnd > 0.85)? /*SBC: 3--> 15*/15 : /*SBC: 1--> 5*/5) : /*SBC: 2--> 10*/10; }
        if(speed == /*SBC: 3--> 15*/15) {  speed = (rnd > (double)0.8 )? /*SBC: 2--> 10*/10 : /*SBC: 3--> 15*/15; };

     }
    
	/**EJUST
	 * Sets the length of the vehicle*/
	public void setLength(int newLength) { CustomFactory.setLength(this, newLength);}
    
	//SBC
	/** sets a new speed of the vehicle */
	public void setSpeed(double newSpeed) { CustomFactory.setSpeed(this, newSpeed); } /*EJUST: int --> double*/
	
	/** Returns the roaduser acceleration of this custom */
	public double getAcceleration() { return CustomFactory.getAcceleration(this); } /*EJUST: int --> double*/
	
	/** let this custom accelerate */
	public double accelerate(double goalSpeed, double dt /*EJUST*/) /*EJUST: int --> double*/
	{   
		prevSpeed = speed; //EJUST
		return CustomFactory.accelerate(this, goalSpeed, dt); 	
	} 
	
	/** let this custom deaccelerate */
	public double deaccelerate(double goalSpeed, double dt /*EJUST*/) /*EJUST: int --> double*/
	{ 
		prevSpeed = speed; //EJUST
		return CustomFactory.deaccelerate(this, goalSpeed, dt); 
	} 
	
	/** adjust speed to goalspeed, return the actual speed changed to */
	public double adjustSpeedTo(double goalSpeed, double dt /*EJUST*/) /*EJUST: int --> double*/
	{ 
		return CustomFactory.adjustSpeedTo(this, goalSpeed, dt); 
	} 
	
	public int getPlatoonValue() {return 1; }

	/** SBC: returns the stopdistance of the vehicle */
	public double getStopDistance() { return CustomFactory.getStopDistance(this); } /*EJUST: int --> double*/ 

	/**EJUST let this custom accelerate following the IDM*/
    public void accelerate(Roaduser frontRoaduser, double dt){
    	prevSpeed = speed;
    	CustomFactory.accelerate(this, frontRoaduser, dt);}
    
    /** EJUST let this custom change lane following the IDM*/
    public boolean change(boolean leftLane, Roaduser frontRoaduserOld, Roaduser frontRoaduserNew, Roaduser backRoaduserNew){
    	return CustomFactory.change(leftLane, this, frontRoaduserOld, frontRoaduserNew, backRoaduserNew);
    }
    public MicroModelGLD model(){return CustomFactory.model(this);}
    
    public void setModel(MicroModelGLD model){ CustomFactory.setModel(this, model);}
    
    /** EJUST: Check whether it is the suitable time to change lane of this custom*/
    public boolean timeToChange(double dt){return CustomFactory.timeToChange(this, dt);}
	
	/** Returns the length of this custom. */
	public int getLength() { return CustomFactory.getLength(this); }
	/** Returns the roaduser type of this custom. */
	public int getType() { return CustomFactory.getType(this); }
	/** Returns the name of this custom. */
	public String getName() { return CustomFactory.getName(this); }
	/** Returns the description of this custom. */
	public String getDescription() { return CustomFactory.getDescription(this); }
	/** Returns the picture of this custom. */
	public String getPicture() { return CustomFactory.getPicture(this); }
	/** Returns the sound of this custom. */
	public String getSound() { return CustomFactory.getSound(this); }

	
	public void paint(Graphics g, int x, int y, float zf) {
		paint(g,x,y,zf,(double)0.0);
	}

	public void paint(Graphics g, int x, int y, float zf, double dlangle)
	{
    	g.setColor(CustomFactory.getColor(this));
    	double angle = dlangle - Math.toRadians(45.0);
    	int width = CustomFactory.getGraphicalWidth(this);
    	int length = CustomFactory.getGraphicalLength(this);
    
    	double corr = 1 - (width > length ? (double)length / (double)width : (double)width / (double)length);
    	
    	int[] cx = new int[4];
    	cx[0] = (int)(Math.round((double)x + Math.sin(angle) * width));
    	cx[1] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(90.0)) * width));
    	cx[2] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(180.0) + Math.atan(corr)) * length));
    	cx[3] = (int)(Math.round((double)x + Math.sin(angle + Math.toRadians(270.0) - Math.atan(corr)) * length));

    	int[] cy = new int[4];
    	cy[0] = (int)(Math.round((double)y + Math.cos(angle) * width));
    	cy[1] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(90.0)) * width));
    	cy[2] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(180.0) + Math.atan(corr)) * length));
    	cy[3] = (int)(Math.round((double)y + Math.cos(angle + Math.toRadians(270.0) - Math.atan(corr)) * length));

    g.fillPolygon(cx,cy,4);
  }

	
	// Specific XMLSerializable implementation 
 	public String getXMLName() {
 		return parentName+".roaduser-custom";
 	}
 	public void load (XMLElement myElement, XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
 	{
 		super.load(myElement, loader);
 		vehicle = myElement.getAttribute("vehicle").getIntValue();
 		driver = myElement.getAttribute("driver").getIntValue();
		passengers = (int[])XMLArray.loadArray(this, loader);
	}

 	public XMLElement saveSelf () throws XMLCannotSaveException
 	{
 		XMLElement result = super.saveSelf();
 		result.addAttribute(new XMLAttribute("vehicle", vehicle));
 		result.addAttribute(new XMLAttribute("driver", driver));
 		return result;
 	}

 	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
 	{
 		super.saveChilds(saver);
 		XMLArray.saveArray(passengers, this, saver,"passengers");
	}
}