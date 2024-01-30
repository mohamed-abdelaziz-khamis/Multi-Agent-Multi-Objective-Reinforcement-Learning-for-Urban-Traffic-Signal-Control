
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

import java.util.Random;

/**
 *
 * The main class that start the GLD simulator
 *
 * @author Group Model
 * @version 1.0
 */

public class GLDSim
{
        public static int seriesSeedIndex = 0;
        public final static long [] seriesSeed = {
                                           0, 
                                           1000, 
                                           /*POMDPGLD*/ 2000,  3000,  4000,  5000,  6000,  7000, /*POMDPGLD*/ 8000, 
                                           //6000, 7000, 4000, 
                                           9000, 
                                           10000, 11000, 
                                           12000,13000,14000 /*POMDPGLD*/, 15000, 16000, 17000, 18000, 19000,
                                           20000, 21000, 22000, 23000, 24000, 25000, 26000, 27000, 28000, 29000,
                                           30000 /*POMDPGLD*/
                                           // 0 wordt gebruikt om het programma op te starten,
                                           //   eerste run heeft dus seed index 1, tweede 2 enz.
                                           //3000, 4000 lopen vast
        };

        /*POMDPGLD*/
        // RND used for support libraries that are not normally reset when a new run is set.
        public static Random RND = new Random(seriesSeed[seriesSeedIndex]);
        
        public static void main (String[] params)
	{	(new GLDStarter(params,GLDStarter.SIMULATOR)).start();
	}
}
