/*
 * Created on Sep 5, 2004
 * Universidad Tecnológica Nacional - Facultad Regional Buenos Aires
 * Proyecto Final 2004
 * I-ATraCoS - Grupo :
 * - Brey, Gustavo
 * - Escobar, Gaston
 * - Espinosa, Marisa
 * - Pastorino, Marcelo
 * All right reserved
 * Class Description
 * 
 */
package gld.sim.stats.sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;



/**
* CVS Log
* $Id: StdoutAverageSender.java,v 1.1 2006/08/04 21:18:53 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class StdoutAverageSender extends BaseAverageSender {
	
	
	



	public PrintWriter getSender() {
		
		PrintWriter out = null;

		//Guard everything in a try-finally to make
		//sure that the socket is closed:
		try {
			//va a intentar crear el PrintWriter
			System.out.println("will try to create the PrintWriter");
				
			out = new PrintWriter(System.out);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			return out;
		}
		
	}

	/* (non-Javadoc)
	 * @see gld.sim.stats.sender.BaseAverageSender#closeSender()
	 */
	public void closeSender(PrintWriter out) {
		//I do nothing with this PrintWriter, thus I am able to go on writing to the stdout.

	}

}
