
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

import gld.distributions.ParameterValue;
import gld.xml.XMLArray;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;

import java.io.IOException;

/**
 * Contains a spawning frequency for a certain roaduser type.
 *
 * @author Group Datastructures
 * @version 1.0
 */
public class SpawnFrequency implements XMLSerializable
{
	public int ruType;

	public float freq;

	/** EJUST: The distribution type*/
	public int distributionType;

	/** EJUST: The value of each parameter */
	public ParameterValue[] paramValue = { };

	/** EJUST: The timeStep of the last spawned roaduser of type ruType*/
	public int timeStepOfLastArrival = -1;  

	/** EJUST: The generated random number after the last spawned roaduser of type ruType*/
	public double lastGeneratedRandomNumber = 1; 

	/** EJUST: The weather condition*/
	public int weatherCondition;
	
	protected String parentName="model.infrastructure.node";

	/** Creates an empty instance. */
	public SpawnFrequency ()
	{ 
		// For loading
	}

	/** 
	 * Creates an instance initiated with given parameters.
	 * @param _ruType Roaduser type.
	 * @param _freq Initial frequency.
	 * @param _destributionType Interarrival Distribution type.
	 * @param _paramValue Distribution Parameters Values.
	 * @param _timeStepOfLastArrival Time step of last arrival
	 * @param _lastGeneratedRandomNumber Random number generated after the last user of _ruType Roaduser type.
	 * @param _weatherCondition Weather condition
	 */
	public SpawnFrequency(int _ruType, float _freq, 
			int _distributionType /*EJUST*/, ParameterValue[] _paramValue /*EJUST*/, 
			int _timeStepOfLastArrival /*EJUST*/, double _lastGeneratedRandomNumber /*EJUST*/,
			int _weatherCondition /*EJUST*/) 
	{
		ruType = _ruType;
		freq = _freq;
		distributionType = _distributionType; /*EJUST*/
		paramValue = _paramValue; /*EJUST*/
		timeStepOfLastArrival = _timeStepOfLastArrival; /*EJUST*/
		lastGeneratedRandomNumber = _lastGeneratedRandomNumber; /*EJUST*/
		weatherCondition = _weatherCondition; /*EJUST*/
	}

	// XML Serializable implementation

	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException {

		ruType = myElement.getAttribute("ru-type").getIntValue();
		freq   = myElement.getAttribute("freq").getFloatValue();

		if(freq==-1) /*EJUST*/{
				distributionType = myElement.getAttribute("distribution-type").getIntValue(); /*EJUST*/ 
				paramValue = (ParameterValue[])XMLArray.loadArray(this,loader); /*EJUST*/
				timeStepOfLastArrival = myElement.getAttribute("timeStep-of-last-arrival").getIntValue(); /*EJUST*/
				lastGeneratedRandomNumber = myElement.getAttribute("last-generated-random-number").getDoubleValue(); /*EJUST*/
		}
		
		try /*EJUST*/{
			weatherCondition = myElement.getAttribute("weather-condition").getIntValue(); /*EJUST*/		
		}
		catch (Exception e){
			System.out.println(e.getMessage()+ "\n Due to new XML entry, safe to ignore the first time when loading older files.");
		}
	}

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 
		XMLElement result=new XMLElement("spawnfreq");
		result.addAttribute(new XMLAttribute("ru-type",ruType));
		result.addAttribute(new XMLAttribute("freq",freq));

		if (freq==-1){ /*EJUST*/
			result.addAttribute(new XMLAttribute("distribution-type",distributionType)); /*EJUST*/
			result.addAttribute(new XMLAttribute("timeStep-of-last-arrival",timeStepOfLastArrival)); /*EJUST*/
			result.addAttribute(new XMLAttribute("last-generated-random-number",lastGeneratedRandomNumber)); /*EJUST*/
		}
		
		result.addAttribute(new XMLAttribute("weather-condition",weatherCondition)); /*EJUST*/
		return result;
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ 
		// A spawnfrequency has no child objects
		if (freq==-1 && paramValue.length>0) {/*EJUST*/			
			for (int i=0; i<paramValue.length; i++ ) /*EJUST*/
				paramValue[i].setParentName(getXMLName()); /*EJUST*/
			XMLArray.saveArray(paramValue,this,saver,"parameter-values"); /*EJUST*/
		}
	}

	public String getXMLName ()
	{ 
		return parentName+".spawnfreq";
	}

	public void setParentName (String newParentName)
	{	
		this.parentName=parentName; 
	}
}