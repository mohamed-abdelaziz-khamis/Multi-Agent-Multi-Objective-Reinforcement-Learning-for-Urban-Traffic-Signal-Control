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

import java.util.ResourceBundle;

/**
* CVS Log
* $Id: AverageSenderFactory.java,v 1.1 2006/08/04 21:18:53 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class AverageSenderFactory {
	private static AverageSender averageSender;
	private static String classKey = "gld.sim.stats.sender.factory";
	
	private AverageSenderFactory(){}
	
	public static AverageSender getAverageSender(ResourceBundle rb){
		
		if(averageSender==null){
			try {
				BaseAverageSender baseAverageSender= (BaseAverageSender) Class.forName(rb.getString(classKey)).newInstance();
				baseAverageSender.setResourceBundle(rb);
				averageSender = baseAverageSender;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			
			//averageSender = new FileAverageSender();
			//averageSender = new SocketAverageSender();
			//averageSender = new StdoutAverageSender();
			//averageSender = new NullAverageSender();
		}
		return averageSender;
	}
}
