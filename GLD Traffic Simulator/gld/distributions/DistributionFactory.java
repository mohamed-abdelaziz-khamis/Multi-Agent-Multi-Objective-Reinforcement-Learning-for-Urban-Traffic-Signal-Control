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

import gld.utils.StringUtils;

import java.util.NoSuchElementException;

public class DistributionFactory {

	/** All the types of Distribution */
	protected static final int
	UNIFORM=0,
	TRIANGULAR=1,
	GAUSSIAN=2,
	WEIBULL=3,
	EXPONENTIAL=4,
	ERLANG=5;

	/** All the Parameters */
	protected static final int
	EMPTY=-1,
	A=0,
	B=1,
	C=2,
	MEAN=3,
	VARIANCE=4,
	ALPHA=5,
	BETA=6,
	LAMBDA=7,
	K=8,
	MU=9;

	/** All Distribution types. */
	protected static final int[] distributionTypes = {UNIFORM,TRIANGULAR,GAUSSIAN, WEIBULL, EXPONENTIAL, ERLANG};

	/** Descriptions of all Distribution types, corresponding to the types in <code>distributionTypes</code>. */
	protected static final String[] distributionTypeDescs = {
		"Uniform", 		//Uniform
		"Triangular", 	//Triangular
		"Gaussian", 	//Gaussian
		"Weibull", 		//Weibull
		"Exponential", 	//Exponential
		"Erlang"	 	//Erlang		
	};

	/** Returns all types of distributions. */
	public static int[] getDistributionTypes() { return distributionTypes; }

	/** Returns descriptions of all types of distributions. */
	public static String[] getDistributionTypeDescs() { return distributionTypeDescs; }

  	/** Look up the description of a Distribution by its id
	  * @param distributionId The id of the distribution
	  * @returns The description
	  * @throws NoSuchElementException If there is no distribution with the 
	  *	    specified id.
	*/
	public static String getDistributionTypeDescription (int distributionId)
	{ 	return (String)(StringUtils.lookUpNumber(distributionTypeDescs,distributionId));
	}
	
  	/** Gets the ID of a distribution type from its name */
  	public static int getDistributionTypeIdByName(String distributionName)
  	{ 	
  		return StringUtils.getIndexObject(distributionTypeDescs,distributionName);
  	}

	protected static final String[] parameterDescs = 
	{			
		"a", //Triangular distribution: 0, ∞
		"b", //Triangular distribution: a < b
		"c", //Triangular distribution: a <= c <= b

		"μ", //Gaussian distribution: μ belongs to R — mean (location)
		"σ", //Gaussian distribution: σ2 > 0 — variance (squared scale)

		"α",  //Weibull distribution: α > 0 scale (real)
		"β",  //Weibull distribution: β > 0 shape (real)

		"λ",  //Exponential distribution: λ > 0 rate, or inverse scale

		"k",  //Erlang distribution: k belongs to N shape	
		"μ"   //Erlang distribution: λ > 0 rate (real)
	};

	// min, max, value, minorTickSpacing
	protected static final double[][] parameterSpecs = 
	{			
		//{0, 20, 10, 1}, //"a" 
		//{60, 100, 80, 1}, //"b" 
		//{20, 60, 40, 1}, //"c" 

		{0, 2, 1, 0.1}, //"a"
		{4, 6, 5, 0.1}, //"b"
		{2, 4, 3, 0.1}, //"c" 
		
		{2, 20, 10, 0.1}, //"μ" 
		{0.5, 5, 1, 0.1}, //"σ" 

		{1, 20, 1, 0.1}, //"α"
		{0.5, 20, 1, 0.1}, //"β"

		{0.5, 2, 1, 0.1}, //"λ"

		{1, 10, 5, 1}, //"k"
		{0.5, 2, 1, 0.1}  //"μ"
	};

	protected static final int[][] distributionParameters = {
		{A, B},
		{A, B, C},
		{MEAN, VARIANCE},
		{ALPHA, BETA},
		{LAMBDA},
		{K, MU}
	};

	/** Returns an array of parameter descriptions */
	public static String[] getParameterDescs() { return parameterDescs; }

  	/** Look up the description of a Parameter by its id
	  * @param parameterId The id of the parameter
	  * @returns The description
	  * @throws NoSuchElementException If there is no parameter with the
	  *	    specified id.
	*/
 	public static String getParameterDescription (int parameterId)
 	{ 	return (String)(StringUtils.lookUpNumber(parameterDescs,parameterId));
 	}
 	
	/** Returns an array of parameter specs */
	public static double[][] getParameterSpecs() { return parameterSpecs; }

	/** Returns an array of parameter numbers for each distribution. */
	public static int[][] getDistributionParameters()
	{ 	return distributionParameters;
	}

	/** Gets a random number of a distribution by its Id and parameters. 
	 */
	public static double generateRandomNumber (int distributionType, ParameterValue[] paramValue) throws DistributionException
	{
		switch (distributionType) {
		case UNIFORM : return RandomUniform.getUniform(paramValue[0].value, paramValue[1].value);
		case TRIANGULAR : return RandomTriangular.getTriangular(paramValue[0].value, paramValue[1].value, paramValue[2].value);
		case GAUSSIAN : return RandomGaussian.getGaussian(paramValue[0].value, paramValue[1].value);
		case WEIBULL : return RandomWeibull.getWeibull(paramValue[0].value, paramValue[1].value);
		case EXPONENTIAL : return RandomExponential.getExponential(paramValue[0].value);
		case ERLANG : return RandomErlang.getErlang((int)paramValue[0].value, paramValue[1].value);
		}
		throw new DistributionException
		("The DistributionFactory can't make Distribution's of type "+distributionType);
	}
}
