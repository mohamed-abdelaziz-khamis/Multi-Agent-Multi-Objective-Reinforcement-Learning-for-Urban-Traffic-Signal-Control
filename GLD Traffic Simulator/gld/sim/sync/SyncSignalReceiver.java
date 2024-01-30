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
package gld.sim.sync;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;



/**
* CVS Log
* $Id: SyncSignalReceiver.java,v 1.1 2006/08/04 21:18:51 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public interface SyncSignalReceiver{
	public HashMap getSignalInfo(String sessionId, ResourceBundle rb) throws IOException;
	
}
