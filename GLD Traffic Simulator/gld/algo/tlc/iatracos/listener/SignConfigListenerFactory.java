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
package gld.algo.tlc.iatracos.listener;

import java.util.ResourceBundle;

/**
* CVS Log
* $Id: SignConfigListenerFactory.java,v 1.1 2006/08/04 21:18:46 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class SignConfigListenerFactory {
	private static String classKey = "gld.algo.tlc.iatracos.listener.factory";

	private SignConfigListenerFactory() {
	}

	public static BaseSignConfigListener getSignConfigListener(ResourceBundle rb) {
		BaseSignConfigListener signConfigListener = null;
		try {
			signConfigListener =
				(BaseSignConfigListener) Class
					.forName(rb.getString(classKey))
					.newInstance();
			signConfigListener.setResourceBundle(rb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return signConfigListener;
	}
}
