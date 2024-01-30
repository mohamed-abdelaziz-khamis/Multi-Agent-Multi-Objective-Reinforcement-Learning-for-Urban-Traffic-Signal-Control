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

import java.util.ResourceBundle;

/**
* CVS Log
* $Id: SyncSignalReceiverFactory.java,v 1.1 2006/08/04 21:18:51 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class SyncSignalReceiverFactory {
	private static SyncSignalReceiver syncSignalReceiver;
	private static String classKey = "gld.synchronization.listener.factory";
	private SyncSignalReceiverFactory(){}
	
	public static SyncSignalReceiver getSignalReceiver(ResourceBundle rb){
		
		if(syncSignalReceiver==null){
			try {
				BaseSignalReceiver baseSignalReceiver = (BaseSignalReceiver) Class.forName(rb.getString(classKey)).newInstance();
				baseSignalReceiver.setResourceBundle(rb);
				syncSignalReceiver = baseSignalReceiver;
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
			
			
			
			
			//syncSignalReceiver = new SockeSignalReceiver();
		}
		return syncSignalReceiver;
	}
}
