/*
 * Created on Sep 5, 2004
 * Universidad Tecnol�gica Nacional - Facultad Regional Buenos Aires
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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
* CVS Log
* $Id: NullAverageSender.java,v 1.1 2006/08/04 21:18:53 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class NullAverageSender extends BaseAverageSender {

	public PrintWriter getSender() {

		PrintWriter out = null;

		//Guard everything in a try-finally to make
		//sure that the socket is closed:
		try {
			out = new PrintWriter(new ByteArrayOutputStream());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return out;
		}

	}
	
	/* (non-Javadoc)
	 * @see gld.sim.stats.sender.BaseAverageSender#closeSender()
	 */
	public void closeSender(PrintWriter out) {
		out.close();

	}	
	

}
