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

import gld.GLDSim;

import java.util.Random;

/**
 * 
 * @description Returns the next pseudorandom, uniformly distributed double value 
 * between a and b from this random number generator's sequence. 
 * 
 * If u is a value sampled from the standard uniform distribution, 
 * then the value a + (b − a)u follows the uniform distribution parametrised by a and b
 *
 */
public class RandomUniform {
	/**
	 * @description this method is used to implement a random number generator simulates the values
	 * generated by a uniform random variable
	 * 
	 * @param a: [0, infinite]
	 * @param b: a < b
	 */
	public static Random random = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
	public static double getUniform (double a, double b){		
		return a + (b - a)*random.nextDouble();		
	}
}
