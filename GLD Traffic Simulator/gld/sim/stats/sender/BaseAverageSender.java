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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.ResourceBundle;



/**
* CVS Log
* $Id: BaseAverageSender.java,v 1.1 2006/08/04 21:18:53 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public abstract class BaseAverageSender implements AverageSender{
	
	public abstract PrintWriter getSender() throws UnknownHostException , IOException ;
	public abstract void closeSender(PrintWriter out);
	protected ResourceBundle rb = null;
	/**
	 * @param rb
	 */
	public void setResourceBundle(ResourceBundle _rb) {
		rb = _rb;
		
	}
	
	
}
