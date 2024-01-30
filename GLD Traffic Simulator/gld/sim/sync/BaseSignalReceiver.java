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
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;



/**
* CVS Log
* $Id: BaseSignalReceiver.java,v 1.1 2006/08/04 21:18:51 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public abstract class BaseSignalReceiver implements SyncSignalReceiver{
	
	
	public abstract HashMap getSignalInfo(String sessionId, ResourceBundle rb) throws IOException;
	protected ResourceBundle rb = null;
	
	
	public void setResourceBundle(ResourceBundle _rb) {
		rb = _rb;
	
	}
	
}
