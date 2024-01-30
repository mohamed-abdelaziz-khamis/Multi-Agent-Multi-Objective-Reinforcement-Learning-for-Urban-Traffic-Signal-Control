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

package gld.idm;

import gld.utils.StringUtils;

import java.util.NoSuchElementException;

public class WeatherFactory {

	private static Language lang = Language.getInstance(); 	/*EJUST*/
	
	/** All the conditions of Weather */
	public static final int
	DRY=0,
	LIGHT_RAIN=1, 
	NORMAL_RAIN=2,
	HEAVY_RAIN=3,
	LIGHT_FOG=4,
	HEAVY_FOG=5,
	SANDSTORM=6;

	/** All Weather conditions. */
	protected static final int[] weatherConditions = {DRY,LIGHT_RAIN,NORMAL_RAIN, HEAVY_RAIN, LIGHT_FOG, HEAVY_FOG, SANDSTORM};

	/** Descriptions of all Weather conditions, corresponding to the conditions in <code>weatherConditions</code>. */
	protected static final String[] weatherConditionDescs = {
		lang.getDryName(), 				//Dry
		lang.getLightRainName(), 		//Light Rain
		lang.getNormalRainName(), 		//Normal Rain
		lang.getHeavyRainName(), 		//Heavy Rain
		lang.getLightFogName(), 		//Light Fog
		lang.getHeavyFogName(),			//Heavy Fog		
		lang.getSandstormName()			//Sandstorm
	};
	
	/** Returns all conditions of weathers. */
	public static int[] getWeatherConditions() { return weatherConditions; }

	/** Returns descriptions of all conditions of weathers. */
	public static String[] getWeatherConditionDescs() { return weatherConditionDescs; }

  	/** Look up the description of a Weather by its id
	  * @param weatherId The id of the weather
	  * @returns The description
	  * @throws NoSuchElementException If there is no weather with the specified id.
	*/
	public static String getWeatherConditionDescription (int weatherId)
	{ 	return (String)(StringUtils.lookUpNumber(weatherConditionDescs,weatherId));
	}
	
  	/** Gets the ID of a weather condition from its name */
  	public static int getWeatherConditionIdByName(String weatherName)
  	{ 	
  		return StringUtils.getIndexObject(weatherConditionDescs,weatherName);
  	}
}
