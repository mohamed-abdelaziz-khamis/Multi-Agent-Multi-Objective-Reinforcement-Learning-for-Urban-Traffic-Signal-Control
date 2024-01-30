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
 
 

 
package gld.sim.stats.tracks;

import gld.sim.stats.tracks.bind.ObjectFactory;
import gld.sim.stats.tracks.bind.Track;
import gld.sim.stats.tracks.bind.Tracks;

import java.io.PrintWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
* CVS Log
* $Id: JAXBStatisticsUtils.java,v 1.1 2006/08/04 21:18:54 secools Exp $
* @author $Author: secools $
* @version $Revision: 1.1 $
**/
public class JAXBStatisticsUtils {

	private JAXBContext jaxbContext;
	private ObjectFactory objectFactory;
	private Tracks tracks;
	private static String packageName = "gld.sim.stats.tracks.bind";
	private String xmlFileName = "e:/temp/temp.xml";

	public JAXBStatisticsUtils() {
		createContextAndObjectFactory();
		createTracks();
	}

	private void createContextAndObjectFactory() {
		try {
			jaxbContext = JAXBContext.newInstance(packageName);
			objectFactory = new ObjectFactory();
		} catch (JAXBException e) {
			System.out.println("There was this problem creating a context " + e);
		}
	}

	private void createTracks() {
		try {
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setValidating(true);
			tracks = makeNewTracks("");
			
		} catch (JAXBException e) {
			System.out.println(
				"There is this problem with unmarshalling: " + e);
		}
	}

	public void persistTracks(PrintWriter out) {
		try {
			if (jaxbContext.createValidator().validate(tracks)) {
				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.setProperty(
					Marshaller.JAXB_FORMATTED_OUTPUT,
					new Boolean(true));
					
					
					
					try {
						//Se van a enviar las estadisticas
						//System.out.println("Will send the statistics"); /*EJUST commented*/
						marshaller.marshal(tracks, out);
					}
					catch (Exception e) {
						e.printStackTrace();
					}	
				
			}
		} catch (JAXBException e) {
			System.out.println(
				"There was this problem persisting the item: " + e);
		}

	}

	public Track makeNewTrack(String id, float ruWaiting) {
		Track newTrack = null;
		try {
			newTrack = objectFactory.createTrack();

			newTrack.setId(id);
			newTrack.setRoadUsersWaiting(ruWaiting);
		} catch (JAXBException e) {
			System.out.println(
				"There was this problem creating a new item: " + e);
		}
		return newTrack;
	}
	
	public Tracks makeNewTracks(String timeStamp) {
		Tracks newTracks = null;
		try {
			newTracks = objectFactory.createTracks();

			newTracks.setTimestamp(timeStamp);
			
		} catch (JAXBException e) {
			System.out.println(
				"There was this problem creating a new item: " + e);
		}
		return newTracks;
	}	
	
	
	/*public Subentry makeNewTask(String description, int time) {
		Subentry newTask = null;
		try {
			newTask = objectFactory.createSubentry();
			newTask.setDescription(description);
			newTask.setTimeEstimate(time);
		} catch (JAXBException e) {
			System.out.println(
				"There was this problem creating a new task: " + e);
		}
		return newTask;
	}*/
	
	
	public void addItem(Track track) {
		tracks.getTrack().add(track);
		//persistTracks();
	}

}
