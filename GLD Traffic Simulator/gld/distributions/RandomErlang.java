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

/** 
 * @description A Program to simulate the values generated by Erlang (K, Lambda) random variable 
 * using a Exponential random variable  (Convolution method)
 * */
public class RandomErlang {

	/**
	 * @param Mu > 0 rate
	 * @param K: # of phases = Number of Independent Identically Distributed Exponential Random Variables
	 */
	public static double getErlang (int k, double mu){				
		double erlang = 0;
		for (int i=1; i<=k; i++){
			erlang += RandomExponential.getExponential(mu/k);
		}
		return erlang;
	}
}