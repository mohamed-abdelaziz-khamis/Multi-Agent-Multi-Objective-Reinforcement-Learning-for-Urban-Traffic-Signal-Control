
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

import gld.GLDException;
import gld.distributions.DistributionFactory;
import gld.distributions.ParameterValue;
import gld.idm.WeatherFactory;
import gld.sim.SimModel;
import gld.xml.XMLArray;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLTreeException;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * EdgeNode, a node used as starting and end point for Roadusers.
 *
 * @author Group Datastructures
 * @version 1.0
 */

public class EdgeNode extends SpecialNode
{
	/** The type of this node */
	protected static final int type = Node.EDGE;	
	/** The frequency at which various roadusers spawn */
	protected SpawnFrequency[] spawnFreq = { };
	/** The frequency at which various roadusers spawn at given timeSteps.*/
	private Hashtable spawnTimeStepsHash = new Hashtable();

	/** The frequency with which spawned roadusers choose specific destinations */
	protected DestFrequency[][] destFreq = {{}};
	
	public EdgeNode() {}

	public EdgeNode(Point _coord)
	{
		super(_coord);
	}


	/*============================================*/
	/* LOAD and SAVE                              */
	/*============================================*/

	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{	
		super.load(myElement,loader);
		spawnFreq=(SpawnFrequency[])XMLArray.loadArray(this,loader);
		destFreq=(DestFrequency[][])XMLArray.loadArray(this,loader);
	
		try
		{
			SpawnFrequencyTimeSteps[] dSpawnFreq = (SpawnFrequencyTimeSteps[])XMLArray.loadArray(this,loader);
			setSpawnTimeStepsHash(new Hashtable());
			for (int i = 0; i < dSpawnFreq.length; i++)
			{
				Integer key = new Integer(dSpawnFreq[i].timeStep);
				if (getSpawnTimeStepsHash().get(key) == null)
				{
					getSpawnTimeStepsHash().put(key, new Vector( ));
				}
	
				((Vector)getSpawnTimeStepsHash().get(key)).add((Object)dSpawnFreq[i]);
	
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage()+ "\n Due to new XML entry, safe to ignore the first time when loading older files.");
		}
	}

	public XMLElement saveSelf () throws XMLCannotSaveException {
		XMLElement result=super.saveSelf();
		result.setName("node-edge");
		return result;
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	
		super.saveChilds(saver);

		Vector temp = new Vector();	
	
		for (Enumeration e = getSpawnTimeStepsHash().keys(); e.hasMoreElements();)
		{
			Vector hashelem = (Vector)getSpawnTimeStepsHash().get(e.nextElement());
			temp.addAll(hashelem);
		}
		SpawnFrequencyTimeSteps[] dSpawnArray = new SpawnFrequencyTimeSteps[temp.size()];
	
		for (int j = 0; j < temp.size(); j++) {
			dSpawnArray[j] = (SpawnFrequencyTimeSteps)temp.get(j);
		}
	
		XMLArray.saveArray(spawnFreq,this,saver,"spawn-frequencies");
		XMLArray.saveArray(destFreq,this,saver,"dest-frequencies");
		XMLArray.saveArray(dSpawnArray,this,saver,"dspawn-frequencies");
	}

	public String getXMLName ()
	{ 	
		return parentName+".node-edge";
	}

	class TwoStageLoaderData {
		int roadId;
	}

	public void addDSpawnTimeSteps (int _rutype, int _timeStep, 
			float _freq, int _distributionType /*EJUST*/, ParameterValue[] _paramValue /*EJUST*/, 
			int _weatherCondition /*EJUST*/)
	{
		SpawnFrequencyTimeSteps sf = new SpawnFrequencyTimeSteps(_rutype, _timeStep, 
				_freq, _distributionType /*EJUST*/, _paramValue /*EJUST*/, 
				_weatherCondition /*EJUST*/);
		
		Integer key = new Integer(_timeStep);
		if (getSpawnTimeStepsHash().get(key) == null)
		{
			getSpawnTimeStepsHash().put(key, new Vector( ));
		}
		((Vector)getSpawnTimeStepsHash().get(key)).add((Object)sf);
	}

	public void deleteDSpawnTimeSteps (int _rutype, int _timeStep)
	{
		Vector cyvec = (Vector)getSpawnTimeStepsHash().get(new Integer(_timeStep));
		for (int i = 0; i < cyvec.size(); i++)
		{
			SpawnFrequencyTimeSteps elem = (SpawnFrequencyTimeSteps)cyvec.get(i);
			if (elem.ruType == _rutype)
				cyvec.remove(i);
		}
	}

	public Vector dSpawnTimeStepsForRu(int _rutype)
	{
		Vector dSpawnVec = new Vector();
		for (Enumeration e = getSpawnTimeStepsHash().keys(); e.hasMoreElements();)
		{
			Vector hashelem = (Vector)getSpawnTimeStepsHash().get(e.nextElement());
			for (int i = 0; i < hashelem.size(); i++)
			{
				SpawnFrequencyTimeSteps sf = (SpawnFrequencyTimeSteps)hashelem.get(i);
				if (sf.ruType == _rutype)
				{
					dSpawnVec.add((Object)sf);
				}
			}
		}
		return dSpawnVec;
	}

	public void doStep (SimModel model)
	{
		Integer curTimeStep = new Integer(model.getCurTimeStep());
		if(getSpawnTimeStepsHash().containsKey(curTimeStep))
		{
			Vector sfcsCurTimeStep = (Vector)getSpawnTimeStepsHash().get(curTimeStep);
			String paramValues; //EJUST
			for (int i = 0; i < sfcsCurTimeStep.size(); i++)
			{
				SpawnFrequencyTimeSteps sfcs = (SpawnFrequencyTimeSteps)sfcsCurTimeStep.get(i);
				setSpawnFrequency(sfcs.ruType, sfcs.freq, 
						sfcs.distributionType /*EJUST*/, sfcs.paramValue /*EJUST*/, sfcs.weatherCondition /*EJUST*/);
				if (sfcs.freq>=0) //EJUST
					System.out.println("Time step: "+ sfcs.timeStep + " Changed SpawnFrequency for " + getName() + 
							" for type " + sfcs.ruType + " to: " + sfcs.freq + 
							" Weather Condition: " + WeatherFactory.getWeatherConditionDescription(sfcs.weatherCondition) /*EJUST*/);
				else {/*EJUST*/
					paramValues = "";
					for (int j=0; j<sfcs.paramValue.length; j++)
						paramValues += "," + DistributionFactory.getParameterDescription(sfcs.paramValue[j].parameterIndex) + "=" + sfcs.paramValue[j].value;					
					System.out.println("Time step: "+ sfcs.timeStep + " Changed SpawnFrequency for " + getName() + 
							" for type " + sfcs.ruType +
							" to: " + DistributionFactory.getDistributionTypeDescription(sfcs.distributionType) + paramValues+ 
							" Weather Condition: " + WeatherFactory.getWeatherConditionDescription(sfcs.weatherCondition));
				}
			}
		}
	}

	/*============================================*/
	/* Basic GET and SET methods                  */
	/*============================================*/

	/** Returns the type of this node */
	public int getType() { return type; }

	/** Returns the name of this edgenode. */
	public String getName() { return "Edgenode " + nodeId; }

	/** Returns the array of Spawning Frequencies */
	public SpawnFrequency[] getSpawnFrequencies() { return spawnFreq; }
	/** Sets the Spawning Frequencies */
	public void setSpawnFrequencies(SpawnFrequency[] spawns) { spawnFreq = spawns; }

	/** Returns the array of arrays of Destination Frequencies */
	public DestFrequency[][] getDestFrequencies() { return destFreq; }
	/** Sets the Destination Frequencies */
	public void setDestFrequencies(DestFrequency[][] dests) { destFreq = dests; }

	/** Returns the spawn frequency for the Roadusers of type ruType */
	public SpawnFrequency getSpawnFrequency(int ruType) {
		for (int i=0; i < spawnFreq.length; i++) {
			if (spawnFreq[i].ruType == ruType)
				return spawnFreq[i]; //EJUST: spawnFreq[i].freq
		}
		return null; //EJUST: commented -1
	}

	/** Sets the spawn frequency for Roadusers of type ruType */

	public void setSpawnFrequency(int ruType, float freq, 
			int distributionType /*EJUST*/, ParameterValue[] paramValue /*EJUST*/,
			int weatherCondition /*EJUST*/) {
		for (int i=0; i < spawnFreq.length; i++)
			if (spawnFreq[i].ruType == ruType){
				spawnFreq[i].freq=freq;
				spawnFreq[i].distributionType = distributionType; /*EJUST*/
				spawnFreq[i].paramValue =paramValue; /*EJUST*/
				spawnFreq[i].weatherCondition =weatherCondition; /*EJUST*/
			}
	}

	/**
	 * Set the frequency at which various roadusers spawn at given timeSteps.
	 * @param spawnTimeStepsHash the spawnTimeStepsHash to set
	 * @author EJUST
	 */
	public void setSpawnTimeStepsHash(Hashtable spawnTimeStepsHash) {
		this.spawnTimeStepsHash = spawnTimeStepsHash;
	}

	/**
	 * Get the frequency at which various roadusers spawn at given timeSteps.
	 * @return the spawnTimeStepsHash
	 * @author EJUST
	 */
	public Hashtable getSpawnTimeStepsHash() {
		return spawnTimeStepsHash;
	}
	
	/**
	 * Returns the destination frequency for certain destination edgenode and roaduser type.
	 */
	public float getDestFrequency(int edgeId, int ruType)
	{
		for(int i=0; i<destFreq[edgeId].length; i++)
			if(destFreq[edgeId][i].ruType == ruType)
				return destFreq[edgeId][i].freq;
		return -1;
	}

	/**
	 * Sets the destination frequency for certain destination edgenode and roaduser type.
	 */
	public void setDestFrequency(int edgeId, int ruType, float dest)
	{
		for(int i=0; i<destFreq[edgeId].length; i++)
			if(destFreq[edgeId][i].ruType == ruType)
				destFreq[edgeId][i].freq = dest;
	}

	//SBC
	/** not needed for this class */
	public int getPhaseDiff() { return -1; }
	/** not needed for this class */
	public void setPhaseDiff(int newPhase) { }
	/** not needed for this class */
	public boolean isGreenWaveNode() { return false; }
	/** not needed for this class */
	public void setGreenWaveNode(boolean gw) {}
	/** not needed for this class */
	public boolean isGreenWaveStart() {return false; }
	/** not needed for this class */
	public void setGreenWaveStart(boolean s) {}
	/** not needed for this class */
	public boolean isGreenWaveFinish() {return false;}
	/** not needed for this class */
	public void setGreenWaveFinish(boolean f) {}
	//SBC


	/*============================================*/
	/* Graphics stuff                             */
	/*============================================*/


	public void paint(Graphics g) throws GLDException
	{
		paint(g, 0, 0, 1.0f, 0.0);
	}

	public void paint(Graphics g, int x, int y, float zf) throws GLDException
	{
		paint(g,x,y,zf,0.0);
	}

	public void paint(Graphics g, int x, int y, float zf, double bogus) throws GLDException
	{
		int width = getWidth();
		g.setColor(Color.blue);
		g.drawRect((int)((coord.x + x - 5 * width) * zf), (int)((coord.y + y - 5 * width) * zf), (int)(10 * width * zf), (int)(10 * width * zf));
		if(nodeId != -1)
			g.drawString("" + nodeId,(int)((coord.x + x - 5 * width) * zf) - 10,(int)((coord.y + y - 5 * width) * zf) - 3);
	}
}
