package gld.infra;

import java.util.LinkedList;
import java.util.Vector;
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
public interface PODrivelanes
{
    public void setBeliefstate(Beliefstate bfs);

    public Beliefstate getBeliefstate();

    public void updateBeliefstate(int method_used) throws InfraException;

    public int getNumVRoadusers();

    public int getNumVRoadusersWaiting();

    public int getNumVRoadusersWaiting(LinkedList POQueue);

    public LinkedList getPOQueue();

    //public LinkedList[] getPOQueues() throws InfraException;

    /*EJUST: int --> double*/
    public double getPosFree(ListIterator li, double position /*EJUST: int --> double*/,
    						int length, double speed_left /*EJUST: int --> double*/,
    						ObservedRoaduser ru);

    public ListIterator getFutureMLIterator();

    public LinkedList getFutureMLQueue();


  //  public Vector getObservedQueues();

  //  public void setObservedQueues(Vector vcq);


}
