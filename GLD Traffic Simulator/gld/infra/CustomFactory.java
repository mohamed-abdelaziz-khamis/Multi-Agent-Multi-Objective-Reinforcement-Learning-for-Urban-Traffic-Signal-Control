
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

import gld.GLDSim;
import gld.idm.LaneChangeGLD;
import gld.idm.MicroModelGLD;

import java.awt.Color;
import java.util.Random;

/**
 *
<<<<<<< CustomFactory.java
=======
 * @author Group Datastructures
 * @version 1.0
 *
>>>>>>> 1.8
 * This class generates CustomRoadusers.
 * This first version supports vehicles, drivers and passengers.
 * However, it only really uses vehicles and drivers;
 * passengers are just an added extra.
 * It uses the vehicle ID and driver ID as the primary key.
 * So if you want to create a unique custom, you need to
 * give it a unique driver and vehicle.
 *
 * A vehicle has a speed, length, name and determines the roaduser type.
 * The speed can be modified by the driver, but not the length,
 * since traffic rules do not allow a car to become longer than x meters.
 *
 * A driver has a name, and it can modify the speed of the vehicle.
 * For example, an old lady will modify the speed of a bicycle by -1.
 * An aggressive truck driver might modify the speed of a truck by +2.
 *
 * The SupportedTypes array maps every RoaduserFactory type that is supported to
 * a Custom index.
 * The TypeVehicle array maps every Custom type index to an array of Vehicle types.
 *
 * The VehicleName array gives every vehicle type a name.
 * The VehicleDriver array maps every vehicle type to a list of possible drivers.
 * From this list is then a driver picked with a chance determined by the PersonChance table.
 * The VehiclePassenger table maps every vehicle type to a list of possible passengers.
 * From this list are then picked a few passengers. If it is null, no passengers are allowed.
 *
 * The VehicleProperties table has the following meaning:
 * - Chance this vehicle has to appear in the sim.
 * - Number of these vehicles currently in the sim.
 * - Maximum number of these vehicles in the sim, 0 is unlimited.
 * - Length in blocks of the vehicle.
 * - Graphical length of the vehicle in pixels.
 * - Graphical width of the vehicle in pixels.
 * - Speed of the vehicle in blocks.
 * - Maximum number of passengers.
 * - Roaduser type.
 *
 * The PersonName array gives every person a name. Might be useful :-)
 *
 * The PersonProperties have the following meaning:
 * - Chance this person has to appear in the sim.
 * - Number of these people currently in simulation
 * - Maximum number of these people in sim, 0 is unlimited. Set to 1 to create unique person.
 * - Speed modifier.
 *
 * The VehicleDriverName table gives every vehicle, with every possible driver a name.
 * If any part is null, a default name is generated using the vehicle name and driver name.
 *
 * The VehicleDriverDescription table gives every vehicle, with every possible driver a description.
 * If any part is null, a default description is generated using the vehicle name,
 * driver name, passenger names, current speed and position on the map.
 *
 * The VehicleDriverPicture table gives every vehicle, with every possible driver a picture.
 * If any part is null, a default picture is used.
 *
 * The VehicleDriverSound table gives every vehicle, with every possible driver an audio clip.
 * If any part is null, a default clip is used.
 *
 * @author Group Datastructures
 * @version 0.02
 *
 */

public class CustomFactory
{
	protected static final int PROP_CHANCE = 0;
	protected static final int PROP_NR_CURRENT = 1;
	protected static final int PROP_NR_MAX = 2;
	
	//SBC: 
	protected static final int PROP_SPEED_MAX = 3;
	//protected static final int PROP_SPEED = 3;
	protected static final int PROP_SPEED_CURRENT = 4;
	protected static final int PROP_ACCELERATION = 5;
	protected static final int PROP_STOPFORCE = 6;
	
	
	protected static final int PROP_LENGTH = 7; 		//4;
	protected static final int PROP_GR_LENGTH = 8; 		//5;
	protected static final int PROP_GR_WIDTH = 9; 		//6;
	protected static final int PROP_NR_PASSENGERS = 10; //7;
	protected static final int PROP_ROADUSER_TYPE = 11; //8;
	protected static final boolean useOnlyNormalCars = false;

	protected static Random rnd; 


	protected static final int[] SupportedTypes = {
		RoaduserFactory.CAR,
		RoaduserFactory.BUS,
		RoaduserFactory.BICYCLE
	};
	protected static final int[][] TypeVehicle = {
		{ 0, 3, 4, 5, 6, 7, 8 },
		{ 1 },
		{ 2 }
	};

	protected static final String[] VehicleName = {
		"Generic Car",
		"Generic Bus",
		"Generic Bicycle",
		"KITT",
		"DeLoran",
		"Smart",
		"Limousine",
		"Limousine",
		"GLD Cabrio",
	};

	/**
	// chance, nr current, max nr, speed, length, graph length, - width, nr passengers, ru type
	protected static final int[][] VehicleProperties = {
	// 0	{ 100, 0, 0, 2, 2, 6, 3, 3, RoaduserFactory.CAR },
	// 1	{ 100, 0, 0, 2, 3, 10, 3, 50, RoaduserFactory.BUS },
	// 2	{ 100, 0, 0, 1, 1, 2, 1, 1, RoaduserFactory.BICYCLE },
	// 3	{ 100, 0, 0, 2, 2, 6, 3, 1, RoaduserFactory.CAR },
	// 4	{ 100, 0, 0, 2, 2, 6, 3, 1, RoaduserFactory.CAR },
	// 5	{ 100, 0, 0, 2, 1, 3, 3, 1, RoaduserFactory.CAR },
	// 6	{ 100, 0, 1, 3, 5, 16, 4, 4, RoaduserFactory.CAR}, // Limo 1
	// 7	{ 100, 0, 1, 3, 5, 16, 4, 4, RoaduserFactory.CAR}, // Limo 2
	// 8	{ 100, 0, 0, 2, 2, 6, 3, 1, RoaduserFactory.CAR }, // GLD Cabrio
	};*/

	//SBC
	// chance, nr current, max nr,    
	//       speed_max, speed_cur, acceleration, stopforce,    
	//          length, graph length, - width, nr passengers, ru type
	/** 
	 * 1) chance:
	 * 2) nr current
	 * 3) max nr
	 * 4) maximum speed
	 * 5) current speed
	 * 6) acceleration: this value should be 0, it is rather a property of the driver
	 * 7) stopforce: this is the maximum possible deacceleration of the vehicle
	 * 8) length
	 * 9) graph length
	 * 10) graph width
	 * 11) nr of passengers
	 * 12) roaduser type
	 */
	protected static final double[][] VehicleProperties = { /*EJUST: int --> double*/
		/*     1   2  3      4  5  6   7     8   9  10  11         12            */
	/* 0*/	{ 100, 0, 0,    10, 2, 0,  8,    2,  10, 5, 3,  RoaduserFactory.CAR },
	/* 1*/	{ 100, 0, 0,    10, 2, 0,  8,    3,  10, 5, 50, RoaduserFactory.BUS },
	/* 2*/	{ 100, 0, 0,     5, 2, 0, 15,    1,  10, 5, 1,  RoaduserFactory.BICYCLE },
	/* 3*/	{ 100, 0, 0,    10, 2, 0,  8,    3,  10, 5, 1,  RoaduserFactory.CAR },
	/* 4*/	{ 100, 0, 0,    10, 2, 0,  8,    2,  10, 5, 1,  RoaduserFactory.CAR },
	/* 5*/	{ 100, 0, 0,    10, 2, 0,  8,    1,  10, 5, 1,  RoaduserFactory.CAR },
	/* 6*/	{ 100, 0, 1,    15, 2, 0,  8,    5,  10, 5, 4,  RoaduserFactory.CAR }, // Limo 1
	/* 7*/	{ 100, 0, 1,    15, 2, 0,  8,    5,  10, 5, 4,  RoaduserFactory.CAR }, // Limo 2
	/* 8*/	{ 100, 0, 0,    10, 2, 0,  8,    2,  10, 5, 1,  RoaduserFactory.CAR }, // GLD Cabrio
	};

	
	protected static final int[][] VehicleDriver = {
		{ 0 },
		{ 1 },
		{ 2 },
		{ 3 },
		{ 4 },
		{ 5, 6, 7 },
		{ 8 },
		{ 9 },
		{ 10,11,12,13,14,15,16,17,18,19,20,21,22,},
	};
	protected static final int[][] VehiclePassenger = {
		null, null, null
	};

	protected static final String[] PersonName = {
		"Johnny Bravo",
		"Eek the Cat",
		"Ash Catchem from PalletTown",
		"Micheal",
		"Marty McFly",
		"Dame van de reklame",
		"Pietje Puk",
		"De buurman",
		"Meneer de Voorzitter Jilles V.",
		"Meneer de Vice-Voorzitter Chaim Z.",
		"Arne",
		"Chaim",
		"Gommaar",
		"Hans Bodlaender",
		"Jilles",
		"Joep",
		"Marco Wiering",
		"Matei",
		"Matthijs",
		"Pepijn",
		"Rene",
		"Robert",
		"Sietse",
	};
	
	/*
	// chance, nr current, nr max, speed
	protected static final int[][] PersonProperties = {
		{ 100, 0, 0, 0 },
		{ 100, 0, 0, 0 },
		{ 100, 0, 0, 0 },
		{ 100, 0, 0, 0 },
		{ 100, 0, 0, 0 },
		{ 100, 0, 0, 0 },
		{ 100, 0, 0, 0 },
		{ 100, 0, 0, 0 },
		{ 100, 0, 1, 0 },
		{ 100, 0, 1, 0 },
		{ 100, 0, 1, 0 }, // Arne
		{ 100, 0, 1, 0 }, // Chaim
		{ 100, 0, 1, 0 }, // Gommaar
		{ 100, 0, 1, 0 }, // Hans
		{ 100, 0, 1, 0 }, // Jilles
		{ 100, 0, 1, 0 }, // Joep
		{ 100, 0, 1, 0 }, // Marco
		{ 100, 0, 1, 0 }, // Matei
		{ 100, 0, 1, 0 }, // Matthijs
		{ 100, 0, 1, 0 }, // Pepijn
		{   0, 0, 1, 0 }, // Rene
		{ 100, 0, 1, 0 }, // Robert
		{ 100, 0, 1, 0 }, // Sietse
	};*/
	
	//SBC
	// chance, nr current, nr max,    speed_max, speed_cur, acc
	protected static final int[][] PersonProperties = {
		{ 100, 0, 0,    0, 0, 3 },
		{ 100, 0, 0,    0, 0, 3 },
		{ 100, 0, 0,    0, 0, 3 },
		{ 100, 0, 0,    0, 0, 3 },
		{ 100, 0, 0,    0, 0, 3 },
		{ 100, 0, 0,    0, 0, 3 },
		{ 100, 0, 0,    0, 0, 3 },
		{ 100, 0, 0,    0, 0, 3 },
		{ 100, 0, 1,    0, 0, 3 },
		{ 100, 0, 1,    0, 0, 3 }, 
		{ 100, 0, 1,    0, 0, 3 }, // Arne
		{ 100, 0, 1,    0, 0, 3 }, // Chaim
		{ 100, 0, 1,    0, 0, 3 }, // Gommaar
		{ 100, 0, 1,    0, 0, 3 }, // Hans
		{ 100, 0, 1,    0, 0, 3 }, // Jilles
		{ 100, 0, 1,    0, 0, 3 }, // Joep
		{ 100, 0, 1,    0, 0, 3 }, // Marco
		{ 100, 0, 1,    0, 0, 3 }, // Matei
		{ 100, 0, 1,    0, 0, 3 }, // Matthijs
		{ 100, 0, 1,    0, 0, 3 }, // Pepijn
		{   0, 0, 1,    0, 0, 3 }, // Rene
		{ 100, 0, 1,    0, 0, 3 }, // Robert
		{ 100, 0, 1,    0, 0, 3 }, // Sietse
	};



	protected static final String[][] VehicleDriverName = {
		null,
		null,
		null,
		{ "Knight Rider" },
		{ "Back to the Future IV" },
		null,
		null,
		null,
		{ "GLD0r" },
	};

	protected static final String[][] VehicleDriverDescription = {
		null,
		null,
		null,
		{ "Tja de serie is weer op TV he" },
		null,
		null,
		null,
		null,
		null,
	};

	protected static final String[][] VehicleDriverPicture = {
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
	};

	protected static final String[][] VehicleDriverSound = {
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
	};

	// use just 1 item to have all vehicles have same color, independent of driver
	protected static final int[][] VehicleDriverColor = {
		{ getRGB(0, 0, 255) },
		{ getRGB(0, 255, 0) },
		{ getRGB(255, 0, 0) },
		{ getRGB(0, 0, 0) },
		{ getRGB(128, 128, 128) },
		{ getRGB(255, 0, 0) },
		{ getRGB(250,250,250) },
		{ getRGB(0,0,0) },
		{ getRGB(0,255,0) },
	};



	// Used by functions to cache chances
	protected static double[] VehicleChances = new double[VehicleProperties.length]; /*EJUST: float --> double*/
	protected static double[] PersonChances = new double[PersonProperties.length];

	public static void reset() {
		for (int i=0; i < VehicleChances.length; i++) {
			VehicleChances[i] = 1.0f;
			VehicleProperties[i][PROP_NR_CURRENT] = 0;
		}
		for (int i=0; i < PersonChances.length; i++) {
			PersonChances[i] = 1.0f;
			PersonProperties[i][PROP_NR_CURRENT] = 0;
		}
		rnd = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
	}

	public static void removeCustom(CustomRoaduser ru) {
		int vehicle = ru.getVehicle();
		int driver = ru.getDriver();
		VehicleProperties[vehicle][PROP_NR_CURRENT]--;
		PersonProperties[driver][PROP_NR_CURRENT]--;
		if (VehicleProperties[vehicle][PROP_NR_MAX] != 0)
			VehicleChances[vehicle] = 
				(float) ((VehicleProperties[vehicle][PROP_NR_MAX] -
				VehicleProperties[vehicle][PROP_NR_CURRENT]) /
				VehicleProperties[vehicle][PROP_NR_MAX]); 
		else
			VehicleChances[vehicle] = 1.0f;

		if (PersonProperties[driver][PROP_NR_MAX] != 0)
			PersonChances[driver] =
				(PersonProperties[driver][PROP_NR_MAX] -
				PersonProperties[driver][PROP_NR_CURRENT]) /
				PersonProperties[driver][PROP_NR_MAX];
		else
			PersonChances[driver] = 1.0f;
	}

	public static Roaduser genRoaduser(int type, Node start, Node dest, int pos, MicroModelGLD model/*EJUST*/, LaneChangeGLD laneChange /*EJUST*/) throws InfraException
	{
        if(useOnlyNormalCars)
              return new Car(start, dest, pos, model /*EJUST*/, laneChange/*EJUST*/);
		int ti = -1;
		for (int i=0; i < SupportedTypes.length; i++) {
			if (SupportedTypes[i] == type) ti = i;
		}
		if (ti == -1) return null; // unsupported roaduser type

		CustomRoaduser ru = new CustomRoaduser(start, dest, pos, model/*EJUST*/, laneChange/*EJUST*/);
		int[] vehicles = TypeVehicle[ti];
		double chance = 0; /*EJUST: float --> double*/
		double vc = 0;	/*EJUST: float --> double*/
		int vehicle = -1;
		for (int i=0; i < vehicles.length; i++) {
			vehicle = vehicles[i];
			chance += VehicleProperties[vehicle][PROP_CHANCE] * VehicleChances[vehicle];
		}
		double pick = rnd.nextDouble() * chance; /*EJUST: float --> double*/
		chance = 0;
		for (int i=0; i < vehicles.length; i++) {
			vehicle = vehicles[i];
			vc = VehicleProperties[vehicle][PROP_CHANCE] * VehicleChances[vehicle];
			if (pick >= chance && pick <= chance + vc) {
				VehicleProperties[vehicle][PROP_NR_CURRENT]++;
				if (VehicleProperties[vehicle][PROP_NR_MAX] != 0)
					VehicleChances[vehicle] =
						(VehicleProperties[vehicle][PROP_NR_MAX] -
							VehicleProperties[vehicle][PROP_NR_CURRENT]) /
							VehicleProperties[vehicle][PROP_NR_MAX];
				else
					VehicleChances[vehicle] = 1.0f;

				i = vehicles.length; // break from loop
			}
			chance += vc;
		}

		ru.setVehicle(vehicle);

		int[] drivers = VehicleDriver[vehicle];
		int driver = -1;
		double dc = 0; /*EJUST: float --> double*/
		double totalchance = 0; /*EJUST: float --> double*/
		for (int i=0; i < drivers.length; i++) {
			driver = drivers[i];
			totalchance += PersonProperties[driver][PROP_CHANCE] * PersonChances[driver];
		}

		// workaround. If this evals to true, there's a bug.
		if (totalchance == 0.0f) {
			System.out.println("Chance is 0 for " + VehicleName[vehicle]);
			return genRoaduser(type, start, dest, pos, model /*EJUST*/, laneChange/*EJUST*/);
		}
		
		pick = rnd.nextFloat() * totalchance;
		chance = 0;
		for (int i=0; i < drivers.length; i++) {
			driver = drivers[i];
			dc = PersonProperties[driver][PROP_CHANCE] * PersonChances[driver];
			if (pick >= chance && pick <= chance + dc) { // equal too happens never anyway
				PersonProperties[driver][PROP_NR_CURRENT]++;
				if (PersonProperties[driver][PROP_NR_MAX] != 0)
					PersonChances[driver] =
						(PersonProperties[driver][PROP_NR_MAX] -
							PersonProperties[driver][PROP_NR_CURRENT]) /
							PersonProperties[driver][PROP_NR_MAX];
				else
					PersonChances[driver] = 1.0f;
				// just used up last not-unlimeted person for this vehicle.
				// Don't generate this vehicle anymore until one is removed from sim.
				if (totalchance == dc && PersonProperties[driver][PROP_NR_MAX] != 0) {
					VehicleChances[vehicle] = 0.0f;
				}

				i = drivers.length; // break from loop
			}
			chance += dc;
		}
		ru.setDriver(driver);
		return ru;
	}

	public static Roaduser genRoaduser(int type) throws InfraException {
		return genRoaduser(type, null, null, 0, null /*EJUST*/, null /*EJUST*/);
	}

	/** Returns the speed of given custom. */
	public static double getSpeed(CustomRoaduser ru) { /*EJUST: int --> double*/
		return VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT] +
			PersonProperties[ru.getDriver()][PROP_SPEED_CURRENT];
	}

	
	//SBC
	public static void setSpeed(CustomRoaduser ru, double newSpeed) { /*EJUST: int --> double*/
		VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT] = newSpeed;
	}

	/** (SBC) Returns the roaduser acceleration of given custom */
	public static double getAcceleration(CustomRoaduser ru) { /*EJUST: int --> double*/
		return VehicleProperties[ru.getVehicle()][PROP_ACCELERATION] +
		PersonProperties[ru.getDriver()][PROP_ACCELERATION];
	}
	
	/** (SBC) Let given custom accelerate */
	public static double accelerate(CustomRoaduser ru, double goalSpeed, double dt /*EJUST*/) { /*EJUST: int --> double*/
		double speedCur = VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT]; /*EJUST: int --> double*/
		double speedMax = VehicleProperties[ru.getVehicle()][PROP_SPEED_MAX]; /*EJUST: int --> double*/
		double acc = VehicleProperties[ru.getVehicle()][PROP_ACCELERATION]; /*EJUST: int --> double*/
		if (speedCur + acc*dt <= speedMax) {
			if (speedCur + acc*dt > goalSpeed)
				VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT] = goalSpeed;
			else 
				VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT] = speedCur + acc*dt;
		}
		else VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT] = speedMax;
		return VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT];
	}
	
	/** (SBC) Let given custom deaccelerate */
	public static double deaccelerate(CustomRoaduser ru, double goalSpeed, double dt /*EJUST*/) { /*EJUST: int --> double*/
		double speedCur = VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT]; /*EJUST: int --> double*/
		double speedMax = VehicleProperties[ru.getVehicle()][PROP_SPEED_MAX]; /*EJUST: int --> double*/
		double acc = VehicleProperties[ru.getVehicle()][PROP_ACCELERATION]; /*EJUST: int --> double*/
		if (speedCur - acc*dt >= 0) {
			if (speedCur - acc*dt < goalSpeed)
				VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT] = goalSpeed;
			else
				VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT] = speedCur - acc*dt;
		}
		else VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT] = 0;
		return VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT];
	}
	
	/** (SBC) adjust speed to goalSpeed, return the speed that has been changed to */
	public static double adjustSpeedTo(CustomRoaduser ru, double goalSpeed, double dt /*EJUST*/) { /*EJUST: int --> double*/
		double speedCur = VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT]; /*EJUST: int --> double*/
		if (speedCur < goalSpeed)
			return accelerate(ru,goalSpeed, dt);
		else return deaccelerate(ru,goalSpeed, dt);
	}
	
	/** (SBC) returns the maximum possible deacceleration */
	public static double getStopForce(CustomRoaduser ru) { /*EJUST: int --> double*/
		return VehicleProperties[ru.getVehicle()][PROP_STOPFORCE];
	}
	/** (SBC) returns the stopdistance of the custom */
	public static double getStopDistance(CustomRoaduser ru) { /*EJUST: int --> double*/
		double speedCur = getSpeed(ru); /*EJUST: int --> double*/
		double stopforce = getStopForce(ru); /*EJUST: int --> double*/		
		return (speedCur*speedCur)/(2*stopforce);
	}
	
	/** (EJUST) sets the acceleration of the custom following the IDM*/
	public static void accelerate(CustomRoaduser ru, Roaduser frontRoaduser, double dt) {
    	double acc=ru.model.calcAcc(ru, frontRoaduser);
    	VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT]+= acc*dt;
    	if (VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT]<0){
    		VehicleProperties[ru.getVehicle()][PROP_SPEED_CURRENT]=0;
    	}
	}
	/** (EJUST) change lane of the custom following the IDM*/
	public static boolean change(boolean leftLane, CustomRoaduser ru, Roaduser frontRoaduserOld, Roaduser frontRoaduserNew, Roaduser backRoaduserNew){
		return ru.lanechange.changeOK(leftLane, ru, frontRoaduserOld, frontRoaduserNew, backRoaduserNew);
	}

	/** (EJUST) sets the length of given custom.*/
	public static void setLength(CustomRoaduser ru, int newLength) {
		VehicleProperties[ru.getVehicle()][PROP_LENGTH] = newLength;
	}

	/** (EJUST) check whether it is the suitable time to change lane of this custom*/
	public static boolean timeToChange(CustomRoaduser ru, double dt) {
    	ru.tdelay=ru.tdelay+dt;
    	if (ru.tdelay>=ru.Tdelay){
    	   ru.tdelay=ru.tdelay-ru.Tdelay;
    	   return true;
    	}
    	else {return false;}
	}
	
	/** (EJUST) returns the model of this custom following the IDM*/
	public static MicroModelGLD model(CustomRoaduser ru) {
    	return ru.model;
	}
	
	/** (EJUST) sets the model of this custom following the IDM*/
	public static void setModel(CustomRoaduser ru, MicroModelGLD model) {ru.setModel(model);}
	
	/** Returns the length of given custom. */
	public static int getLength(CustomRoaduser ru) {
		return (int) VehicleProperties[ru.getVehicle()][PROP_LENGTH]; /*EJUST: cast to int*/
	}

	/** Returns the roaduser type of given custom. */
	public static int getType(CustomRoaduser ru) {
		return (int) VehicleProperties[ru.getVehicle()][PROP_ROADUSER_TYPE]; /*EJUST: cast to int*/
	}

	/** Returns the graphical length of given custom. */
	public static int getGraphicalLength(CustomRoaduser ru) {
		return (int) (VehicleProperties[ru.getVehicle()][PROP_GR_LENGTH] /*SBC*/ / 3); /*EJUST: cast to int*/
	}
	/** Returns the graphical width of given custom. */
	public static int getGraphicalWidth(CustomRoaduser ru) {
		return (int) (VehicleProperties[ru.getVehicle()][PROP_GR_WIDTH] /*SBC*/ / 3); /*EJUST: cast to int*/
	}
	/** Returns the color used for drawing of given custom. */
	public static Color getColor(CustomRoaduser ru) {
		int[] colors = VehicleDriverColor[ru.getVehicle()];
		if (colors != null) {
			if (colors.length == 1) return new Color(colors[0]);
			return new Color(colors[ru.getDriver()]);
		}
		return new Color(0, 0, 255);
	}

	/** Returns the name of given vehicle type. */
	public static String getVehicleName(int vehicle) { return VehicleName[vehicle]; }

	/** Returns the name of given person. */
	public static String getPersonName(int person) { return PersonName[person]; }

	/** Returns the name of given custom. */
	public static String getName(CustomRoaduser ru) {
		String[] vs = VehicleDriverName[ru.getVehicle()];
		if (vs != null) {
			if (vs.length == 1) return vs[0];
			String s = vs[ru.getDriver()];
			if (s != null) return s;
		}
		return VehicleName[ru.getVehicle()];
	}

	/** Returns the description of given custom. */
	public static String getDescription(CustomRoaduser ru) {
		String[] vd = VehicleDriverDescription[ru.getVehicle()];
		if (vd != null) {
			if (vd.length == 1) return vd[0];
			String s = vd[ru.getDriver()];
			if (s != null) return s;
		}
		return "This is " + getPersonName(ru.getDriver()) +
			" using vehicle " + getVehicleName(ru.getVehicle()) + "\n" +
			ru.getNumPassengers() + " passengers\n" +
			"at speed " + ru.getSpeed();
	}

	/** Returns the URL of the picture of given custom. */
	public static String getPicture(CustomRoaduser ru) {
		String[] vp = VehicleDriverPicture[ru.getVehicle()];
		if (vp != null) {
			if (vp.length == 1) return vp[0];
			String s = vp[ru.getDriver()];
			if (s != null) return s;
		}
		return null;
	}
	/** Returns the URL of the audio clip of given custom. */
	public static String getSound(CustomRoaduser ru) {
		String[] vs = VehicleDriverSound[ru.getVehicle()];
		if (vs != null) {
			if (vs.length == 1) return vs[0];
			String s = vs[ru.getDriver()];
			if (s != null) return s;
		}
		return null;
	}

	private static int getRGB(int red, int green, int blue) {
		return (new Color(red, green, blue)).getRGB();
	}
}
