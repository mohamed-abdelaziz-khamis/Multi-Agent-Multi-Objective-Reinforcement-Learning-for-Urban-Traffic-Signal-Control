

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

import gld.distributions.DistributionFactory;
import gld.distributions.ParameterValue;
import gld.idm.WeatherFactory;
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
 * Contains a destination frequency for a certain roaduser type.
 *
 * @author Group Datastructures
 * @version 1.0
 */
//(DOAS 05)
public class SpawnFrequencyTimeSteps implements XMLSerializable
{
	public int ruType;
	
	public float freq;
	
	public int timeStep;

	/** EJUST: The distribution type*/
	public int distributionType;

	/** EJUST: The value of each parameter */
	protected ParameterValue[] paramValue = { }; 

	/** EJUST: The weather condition*/
	public int weatherCondition;
	
	protected String parentName="model.infrastructure.node";

	/** Empty constructor for loading */
	public SpawnFrequencyTimeSteps()
	{

	}

	/**
	 * Creates an instance initiated with given parameters.
	 * @param _ruType Roaduser type.
	 * @param _freq Initial frequency.
	 * @param _timeStep The timeStep for which edge should change the frequency _freq for roadusertype _ruType 
	 * @param _destributionType Interarrival Distribution type.
	 * @param _paramValue Distribution Parameters Values.
	 * @param _weatherCondition Weather condition
	 */
	public SpawnFrequencyTimeSteps(int _ruType, int _timeStep, float _freq, 
			int _distributionType /*EJUST*/, ParameterValue[] _paramValue /*EJUST*/,
			int _weatherCondition /*EJUST*/)
	{
		ruType = _ruType;
		freq = _freq;
		timeStep = _timeStep;
		distributionType = _distributionType; /*EJUST*/
		paramValue = _paramValue; /*EJUST*/
		weatherCondition = _weatherCondition; /*EJUST*/
	}

	// XML Serializable implementation

	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{ 	
		ruType=myElement.getAttribute("ru-type").getIntValue();
		timeStep=myElement.getAttribute("timeStep").getIntValue();
		freq=myElement.getAttribute("freq").getFloatValue();

		if(freq==-1){
			distributionType=myElement.getAttribute("distribution-type").getIntValue(); /*EJUST*/ 
			paramValue=(ParameterValue[])XMLArray.loadArray(this,loader); /*EJUST*/
		}
		
		try /*EJUST*/{
			weatherCondition=myElement.getAttribute("weather-condition").getIntValue(); /*EJUST*/
		}
		catch (Exception e){
			System.out.println(e.getMessage()+ "\n Due to new XML entry, safe to ignore the first time when loading older files.");
		}
	}

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	
		XMLElement result=new XMLElement("dspawnfreq");
		result.addAttribute(new XMLAttribute("ru-type",ruType));
		result.addAttribute(new XMLAttribute("timeStep",timeStep));
		result.addAttribute(new XMLAttribute("freq",freq));

		if (freq==-1){ /*EJUST*/
			result.addAttribute(new XMLAttribute("distribution-type",distributionType)); /*EJUST*/
		}

		result.addAttribute(new XMLAttribute("weather-condition",weatherCondition)); /*EJUST*/
		return result;
	}

	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ 	// A spawnfrequencytimeStep has no child objects

		if (freq==-1 && paramValue.length>0) {/*EJUST*/			
			for (int i=0; i<paramValue.length; i++ ) /*EJUST*/
				paramValue[i].setParentName(getXMLName()); /*EJUST*/
			XMLArray.saveArray(paramValue,this,saver,"parameter-values"); /*EJUST*/
		}
	}

	public String getXMLName ()
	{ 	
		return parentName+".dspawnfreq";
	}

	public void setParentName (String newParentName)
	{	
		this.parentName=parentName;
	}

	public String toString() 
	{
		if (freq>=0)   //EJUST
			return new String("At TimeStep " + timeStep + ": " + freq + 
					" ," + WeatherFactory.getWeatherConditionDescription(weatherCondition) /*EJUST*/);
		else	//EJUST 
		{
			String paramValues = "";
			for (int i=0; i<paramValue.length; i++)
				paramValues += "," + DistributionFactory.getParameterDescription(paramValue[i].parameterIndex) + "=" + paramValue[i].value;
			return new String("At TimeStep " + timeStep + ": " + DistributionFactory.getDistributionTypeDescription(distributionType) + paramValues + 
					" ," + WeatherFactory.getWeatherConditionDescription(weatherCondition) /*EJUST*/);
		}
	}
}