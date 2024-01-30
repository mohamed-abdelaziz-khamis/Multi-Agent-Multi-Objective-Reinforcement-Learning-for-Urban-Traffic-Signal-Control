
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University 
 * Copyright of the TC2 algorithm (C) Marco Wiering, Utrecht University
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

package gld.algo.tlc;

import gld.infra.Sign;


public class PosMov
{	
	public Sign sign;  /*EJUST: int tlId --> Sign sign*/
	public double pos; /*EJUST: int --> double*/ 
	
	public PosMov(Sign _sign /*EJUST: int _tlId --> Sign _sign*/, double _pos /*EJUST: int --> double*/ ) {
		sign = _sign; /*EJUST: tlId = _tlId --> sign = _sign*/
		pos  = _pos;
	}
}
