package gld.infra;

import gld.idm.Constants;
import gld.sim.SimModel;
import gld.utils.ListEnumeration;
import gld.xml.XMLArray;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLSaver;
import gld.xml.XMLTreeException;
import gld.xml.XMLUtils;

import java.io.IOException;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
 /*POMDPGLD*/
public class PODrivelane extends Drivelane implements PODrivelanes, Constants /*EJUST*/
{
    public static final String shortXMLName = "dl-po";

    /* Current beliefstate on the lane */
    private Beliefstate beliefstate;
    /* the current most likely queue */
    private LinkedList mlQueue;
    /*the probable followup of the mlQueue if no roadusers have moved, futureMLQueue is equal to the mlQueue*/
    private LinkedList futureMLQueue;


    public PODrivelane(Drivelane _lane)
    {
        super(_lane);
        queue = new LinkedList();
        beliefstate = new Beliefstate(this);

        laneType = DrivelaneFactory.PO_DRIVELANE;
    }


    public PODrivelane(Road _road)
    {
        super(_road);
        queue = new LinkedList();
        beliefstate = new Beliefstate(this);

        laneType = DrivelaneFactory.PO_DRIVELANE;
    }

    /** Empty constructor for loading */
    public PODrivelane()
    {
    	 super();    /*Added by EJUST*/	
    }

    public LinkedList getPOQueue()
    {
        return mlQueue;
    }

    public ListIterator getFutureMLIterator() {
        if( futureMLQueue != null )
           return futureMLQueue.listIterator();
        return null;
    }

    public LinkedList getFutureMLQueue() {
        return futureMLQueue;
    }

    public void resetFutureMLQueue() {
        futureMLQueue = new LinkedList();
        for(int i = 0; i < mlQueue.size(); i++)
        {
            futureMLQueue.add(new ObservedRoaduser((ObservedRoaduser)mlQueue.get(i)));
        }
    }


    /*public LinkedList[] getPOQueues() throws InfraException
    {
        return beliefstate.getQueues();
    }*/

    public Beliefstate getBeliefstate()
    {
        return beliefstate;
    }



    /*============================================*/
    /* LOAD and SAVE                              */
    /*============================================*/

    /* XML saver, doesnt save any extra data than original, in PO, there wil be an extra element,
     old files will thus be loaded as Fully observable, the way they were intended to load*/
    public XMLElement saveSelf() throws XMLCannotSaveException
    {
        XMLElement result = new XMLElement("lane");
        result.addAttribute(new XMLAttribute("id", Id));
        //  result.addAttribute(new XMLAttribute("lane-type", DrivelaneFactory.PO_DRIVELANE));
        result.addAttribute(new XMLAttribute("timeStep-moved", timeStepMoved));
        result.addAttribute(new XMLAttribute("timeStep-asked", timeStepAsked));
        result.addAttribute(new XMLAttribute("road-id", road.getId()));
        result.addAttribute(new XMLAttribute("type", type));
        return result;
    }


    public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException
    {
        XMLArray.saveArray(targets, this, saver, "targets");
        saver.saveObject(sign);
        XMLUtils.setParentName(new ListEnumeration(queue), getXMLName());
        XMLArray.saveArray(queue, this, saver, "queue");
    }


    public void loadSecondStage(Dictionary dictionaries) throws XMLInvalidInputException, XMLTreeException
    {
        road = (Road)(((Dictionary)(dictionaries.get("road"))).get(new Integer(loadData.roadId)));
        sign.loadSecondStage(dictionaries);
        XMLUtils.loadSecondStage(new ListEnumeration(queue), dictionaries);
        beliefstate = new Beliefstate(this);
    }


    /*============================================*/
    /* MODIFYING DATA                             */
    /*============================================*/


    public void setBeliefstate(Beliefstate bfs)
    {
        beliefstate = bfs;
    }

    public void updateBeliefstate(int method) throws InfraException
    {
        beliefstate.update(method);
        mlQueue = beliefstate.getQueue(this);
        if( SimModel.usePO ) {
            resetFutureMLQueue();
        }
    }




    /**
     * Adds a Roaduser at the end of this lane
     *
     * @param ru The roaduser to add
     * @param pos The position where the roadusers should be added
     * @throws InfraException if the roaduser could not be added
     */
    public void addRoaduserAtEnd(Roaduser ru, double pos /*EJUST: int --> double*/) throws InfraException
    {
        if(!queue.isEmpty())
        {
            Roaduser last = (Roaduser)queue.getLast();
            if(last.getPosition() + last.getLength() <= pos)
            {
                ru.setPosition(pos);
                queue.addLast(ru);
                beliefstate.addRoaduserAtEnd(ru);
                return;
            }
            else
            {
                throw new InfraException("Position taken.");
            }
        }
        ru.setPosition(pos);

        beliefstate.addRoaduserAtEnd(ru);
        queue.addLast(ru);

    }

    /**
     * Resets this Drivelane.
     * This will remove all Roadusers on this lane,
     * reset the timeStepMoved and timeStepAsked counters,
     * and reset the sign.
     * @see Sign#reset()
     */

    public void reset()
    {
        //System.out.println("Resetting lane " + Id);
        resetTargets();
        queue = new LinkedList();
        beliefstate = new Beliefstate(this);

        timeStepMoved = -1;
        timeStepAsked = -1;
        sign.reset();
    }


    /**
     * Adds a Roaduser at a given position to the lane
     *
     * @param ru The roaduser to add
     * @param pos The position at which to add the roaduser
     * @throws InfraException if the position is taken by another roaduser
     */
    public void addRoaduser(Roaduser ru, int pos) throws InfraException
    {
        if(!queue.isEmpty())
        {
            ListIterator li = queue.listIterator();
            Roaduser r = null;
            while(li.hasNext())
            {
                r = (Roaduser)li.next();
                if(r.getPosition() <= pos &&  r.getLength() + r.getPosition() > pos)
                {
                    throw new InfraException("Position taken");
                }
                if(r.getPosition() > pos)
                {
                    if(ru.getLength() > r.getPosition() - pos)
                    {
                        throw new InfraException("Position taken");
                    }
                    li.add(ru);
                    beliefstate.addRoaduser(ru,  li.nextIndex() - 1, pos);
                    break;
                }
            }
            if(pos >= r.getPosition() + r.getLength())
            {
                queue.addLast(ru);
                beliefstate.addRoaduserAtEnd(ru);
            }
        }
        else
        {
            queue.addLast(ru);
            beliefstate.addRoaduserAtEnd(ru);
        }
        ru.setPosition(pos);
    }


    /*============================================*/
    /* COMPLEX GET                                */
    /*============================================*/
    public int getNumVRoadusers() {
        if (SimModel.beliefstate_method == Beliefstate.QMDP) {
            return beliefstate.getNumVisibleCars();
        }
        else {
        	if(mlQueue == null)
            {
               return 0;
            }
            return mlQueue.size();
        }
    }


    public int getNumVRoadusersWaiting() {
        if (SimModel.beliefstate_method == Beliefstate.QMDP) {
            return beliefstate.getNumVisibleCars();
        }

        return getNumVRoadusersWaiting(mlQueue);
    }


    public int getNumVRoadusersWaiting(LinkedList mlQueue)
    {
        if(mlQueue == null)
        {
            return 0;
        }
        if(SimModel.useAllRoadusers) {
            return mlQueue.size();
        }

        /*if(mlQueue.size() < 2)
        {
            return mlQueue.size();
        }
        return 2;*/

        ListIterator li = mlQueue.listIterator();
        Roaduser ru = null;
        ObservedRoaduser vru = null;
        double pos = 0; 		 /*EJUST: int --> double*/
        double ru_pos, ru_speed; /*EJUST: int --> double*/
        int count = 0;
        double cnt_step = 0; 	 /*EJUST: int --> double*/
        
        while(li.hasNext())
        {
            vru = (ObservedRoaduser)li.next();
            ru = vru.getRoaduser();

            /* EJUST commented
            ru_pos = vru.getPos();
            ru_speed = vru.getSpeed();


            // was:
            // if(ru_pos > pos) return count;
            // nu: waar ru terecht kan komen, moet nog rekening worden gehouden met inloop vakjes
            if(ru_pos - ru_speed*dt > pos - cnt_step)
            {
                return count; // Wont be able to wait.
            }
            else if(ru_pos - ru_speed*dt <= pos - cnt_step)
            {
                cnt_step += ru_pos - pos; // The free blocks ahead of ru, if everyone moves on.
                pos = ru_pos + (ru.getLength()-1);
                count++;
            }
            */
			
			/*EJUST*/
			if(ru.getSpeed()>STOP_SPEED_MS) 
				return count;
			else count++;
        }
        return count;
    }

    /**
     * Returns the best reachable position for the supplied Roaduser on the Queue given in the ListIterator
     *
     * @param li The Queue of this Drivelane represented in a ListIterator. li.previous() is the current RoadUser
     * @param position The position on the Drivelane of the Roaduser
     * @param length The amount of blocks that have to be free
     * @param speed_left the number of 'moves' this Roaduser has left in this turn
     * @param ru The Roaduser to be checked
     * @return the number of blocks the Roaduser can move ahead
     */
    public double getPosFree(ListIterator li, double position /*EJUST: int --> double*/, 
    						int length, double speed_left /*EJUST: int --> double*/, 
    						ObservedRoaduser ru)
    {
		//SBC, EJUST: int --> double
		double ru_stopdistance = 0; /*EJUST commented: ru.getStopDistance()*/
		
		/*EJUST: int --> double*/
    	double best_pos = position;
    	
    	/*EJUST: int --> double*/
        double max_pos = position;
                
        /*EJUST: int --> double*/       
		double target_pos =  (position - speed_left*dt > 0)? (position - speed_left*dt) : 0; 

        // Previous should be 'ru'
        ObservedRoaduser prv = (ObservedRoaduser) li.previous();

        if (prv == ru && li.hasPrevious()) // roaduser not first
        {
        	/* has car in front */
        	prv = (ObservedRoaduser) li.previous();
            
        	max_pos = prv.getPos() + prv.getRoaduser().getLength()+ ru_stopdistance /*SBC: stopdistance support*/;
            
        	if (max_pos < target_pos)
            {
                best_pos = target_pos;
            }
            else
            {
                best_pos = max_pos;
            }
            li.next();
        }
        else
        {
            best_pos = target_pos;
        }

        li.next(); // Setting the ListIterator back in the position we got it like.

        if (best_pos != position){
        	// The Roaduser can advance some positions
            return best_pos; /*EJUST: int --> double*/
        }
        else
        {
            return 0;
        }
    }
}
