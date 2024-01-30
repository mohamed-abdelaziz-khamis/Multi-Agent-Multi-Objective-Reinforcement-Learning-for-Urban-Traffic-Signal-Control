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



/**
* CVS Log
* $Id: AverageSender.java,v 1.1 2006/08/04 21:18:53 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public interface AverageSender{
	public PrintWriter getSender() throws UnknownHostException , IOException ;
	public void closeSender(PrintWriter out);
}
