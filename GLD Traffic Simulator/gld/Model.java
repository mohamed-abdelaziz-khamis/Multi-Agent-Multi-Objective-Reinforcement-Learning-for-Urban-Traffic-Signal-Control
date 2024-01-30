
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

package gld;

import gld.infra.DrivelaneFactory;
import gld.infra.Infrastructure;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;

import java.io.IOException;
import java.util.Observable;

/**
 *
 * The heart of the simulation.
 *
 * @author Group Model
 * @version 1.0
 */

public class Model extends Observable implements XMLSerializable
{
	//GASTON: PROB� HACER infra de tipo static con un singleton pero no funcion�, tira una excepci�n cuando intenta reconstruir la red
	////DO PROBE static type below a singleton but it did not work, throws an exception when you try to reconstruct the network
	/**The infrastructure used by the model*/
	protected Infrastructure infra;
	protected String parentName="";

	/*POMDPGLD*/
    /** The Drivelane type for this model */
	public static int dltype = DrivelaneFactory.DRIVELANE;
	
	/** Determines whether statistics are saved and loaded or not. */
	public static boolean SAVE_STATS = false;

	
	/**Creates a model with an empty infrastructure */	
	public Model() {
		infra = new Infrastructure(); 
	}
	
	/**Creates a model with the given infrastructure*/
	public Model(Infrastructure i) {
		infra = i;
	}
	
	/** Returns the current infrastructure */
	public Infrastructure getInfrastructure() { return infra; }
	/** Sets the current infrastructure */
	public void setInfrastructure(Infrastructure i) { infra = i; }
	
	
	

	// XMLSerializable implementation
	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{	
		infra = new Infrastructure();
		SAVE_STATS=myElement.getAttribute("save-stats").getBoolValue();
		loader.load(this,infra);
	}
  
	public XMLElement saveSelf () 
	{ 	
		XMLElement result=new XMLElement("model"); 
		result.addAttribute(new XMLAttribute("save-stats",SAVE_STATS));
		return result;
	}
	
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	
		saver.saveObject(infra);
	}
	
	public String getXMLName () 
	{ 	
		return "model"; 
	}
	
	public void setParentName (String parentName) throws XMLTreeException
	{	
		throw new XMLTreeException("XML root class Model does not support different parent names");
	}
}