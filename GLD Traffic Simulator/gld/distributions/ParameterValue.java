/*-----------------------------------------------------------------------
 * Copyright (C) 2011 Mohamed A. Khamis, EJUST
 *
 * This program (class) is free software.
 * You may redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation (version 2 or later).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *------------------------------------------------------------------------*/

package gld.distributions;

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
* Contains a value for a certain parameter.
*
* @author EJUST
 * @version 1.0
*/
public class ParameterValue implements XMLSerializable
{
	public int parameterIndex;
	public double value;
	protected String parentName="model.infrastructure.node.dspawnfreq";
	
	/** Creates an empty instance. */
	public ParameterValue()
	{ // For loading
	}

	/** 
	* Creates an instance initiated with given parameters.
	* @param _parameterIndex Parameter Index.
	* @param _value Parameter Value.
	*/
	public ParameterValue(int _parameterIndex, double _value) 
	{
		parameterIndex = _parameterIndex;
		value = _value;			
	}
	
	// XML Serializable implementation
	
	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException {
		parameterIndex = myElement.getAttribute("param-index").getIntValue();
		value   = myElement.getAttribute("value").getDoubleValue();
	}
	
	public XMLElement saveSelf () throws XMLCannotSaveException
	{ XMLElement result=new XMLElement("paramvalue");
	  result.addAttribute(new XMLAttribute("param-index",parameterIndex));
	  result.addAttribute(new XMLAttribute("value",value));
	  return result;
	}
 
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ // A spawnfrequency has no child objects
	}

	public String getXMLName ()
	{ return parentName+".paramvalue";
	}

	public void setParentName (String newParentName)
	{	this.parentName=newParentName; 
	}	
}