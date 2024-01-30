package gld.infra;

import java.util.Vector;
import java.util.LinkedList;
import java.util.ListIterator;
import gld.GLDSim;
import java.util.Random;
import java.util.HashMap;
import gld.sim.SimModel;
import gld.utils.QuickSort;

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
public class Beliefstate
{
    /* the drivelane this beliefstate belongs to */
    private Drivelane drivelane;

    /* all roadusers on the lane in order of appearance */
    private LinkedList roadusers;
    /* visible roadusers */
    private Vector visibleRoaduserIDs;
    /* the Observed Roaduser queues for each Roaduser */
    private Vector oQueues;
    /* associated probabilities where each car can be the length of cars == the length of pQueues*/
    private Vector pQueues;
    /* the possible states in the vector */
    private Vector states;
    /* associated probability of each state in the state vector */
    private Vector pStates;
    /* randomizer */
    protected Random RND = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);

    private int numVisibleCars;

    private LinkedList mlQueue;
    private LinkedList[] allQueues;

    /* Definitions */
    public final static int ALLINFRONT = 0;
    public final static int MOSTLIKELYSTATE = 1;
    public final static int MOSTLIKELYQUEUE = 2;
    public final static int QMDP = 3;


    // doesnt really work...
    public final static int ACTIONVOTING = 4;


    /* standard method for constructing beliefstates is the Most likely one */
    protected int method_used = MOSTLIKELYQUEUE;


    public Beliefstate(Drivelane lane, Beliefstate oldBeliefstate)
    {
        drivelane = lane;
        roadusers = oldBeliefstate.getRoadusers();
        oQueues = oldBeliefstate.getRoaduserObservations();

        update(MOSTLIKELYQUEUE);
    }

    public Beliefstate(Drivelane lane, Beliefstate oldBeliefstate, int method)
    {
        drivelane = lane;
        roadusers = oldBeliefstate.getRoadusers();
        oQueues = oldBeliefstate.getRoaduserObservations();

        update(method);
    }

    public Beliefstate(Drivelane lane)
    {
        drivelane = lane;
        roadusers = new LinkedList();
        oQueues = new Vector();
    }

    public void update()
    {
        update(method_used);
    }

    public void update(int method)
    {
        this.method_used = method;
        states = new Vector();
        pStates = new Vector();
        updatePQueues();
        if(method != MOSTLIKELYSTATE ) {
            calcBeliefstate();
        }
        updateQueues();
    }


    private void updatePQueues()
    {
        pQueues = new Vector();
        for(int i = 0; i < oQueues.size(); i++)
        {
            pQueues.addElement(((ObservedQueue)oQueues.get(i)).calculatePQueue());
        }
    }

    public int getNumVisibleCars() {
        return numVisibleCars;
    }


    private void calcBeliefstate()
            throws NumberFormatException
    {
        Vector curStates = new Vector();
        Vector curPStates = new Vector();
        Vector curPPos = new Vector();

        // the to be constructed state vector, with the roadusers on it
        states = new Vector();
        // the probabilities of each constructed state in the state vector
        pStates = new Vector();

        int numCars = roadusers.size();
        if(method_used == ACTIONVOTING)
        {
            // Since action voting is about a vote for those waiting or not, if
            // there isnt a single car waiting, we can skip them all, since the TLC wont do anything
            // with non waiting RU's, and it will give some speedup.
            numCars = cars_FilterAV();
        }

        numVisibleCars = 0;
        visibleRoaduserIDs = new Vector();
        // for all roadusers on the lane in order of apearance
        for(int i = 0; i < numCars; i++)
        {
            // newCar is used to put the current RU on every permutation of the beliefstate.
            Object newCar = roadusers.get(i);

            boolean ruDetected = ((ObservedQueue)oQueues.get(i)).getRuDetection();
            if(ruDetected) {
                numVisibleCars++;
                visibleRoaduserIDs.add(new Integer(i));
                // temp values for this run
                Vector newState = new Vector();
                Vector newPState = new Vector();
                Vector newPPos = new Vector();

                // the last possible position this car can be on the drivelane, since we must have room for all cars on the lane
                int lastPossiblePos = ((roadusers.size() - i) * 2) - 1;

                // statistics on lilyhoods of the position of this roaduser
                double[] pQueue = ((double[])pQueues.get(i)).clone();

                if(method_used == MOSTLIKELYQUEUE)
                {
                    // To speed up the algorithm, if we are using ML, we only have to get permutations of the ML probabilities,
                    // not of the remaining less likely ones.
                    pQueue = pQueue_FilterMLQ(pQueue);
                }

                if(method_used == ALLINFRONT)
                {
                    // this is the method that throws away all possibilities and puts all probability mass in front of the queue.
                    pQueue = pQueue_FilterAIF(pQueue);
                }

                /* calculate the cumalative sum of this pQueue, for more efficient lookups.
                   it is used for excess probabilities of this Roaduser, but used in a cached way:

                   excessprobabilities:
                   // if we cannot put this Roaduser on a spot on the road where his prob > 0 because another roaduser is
                   // already there or his prevous RU is still to come, add these probabilities to excessProb, so the likelihoods
                   // are summed and added to the first possible spot where we can add this Roaduser */
                double[] cumSumPQueue = calcCumSum(pQueue);

                /* calulate a skip list to not waste too much computing power to the very insignificant positions on the road */
                // if no skiplist is required the threshold can be set to 0:
                // double skip_threshold = 0;
                
                double skip_threshold = (((double)numCars * 2) / (double)pQueue.length);
                
                boolean[] skipListPQueue = skiplistFilter(pQueue, skip_threshold);
               


                //Initial beliefstate vector
                if(curStates.size() == 0)
                {
                    // add the first elements to curLane, curPLane and curPPos
                    curStates.addElement(new Object[pQueue.length]);
                    curPStates.addElement(new Double(1));
                    curPPos.addElement(new Integer(pQueue.length - 1));
                }

                // for all the previously found permutation of the beliefstate, form new instances.
                for(int v = 0; v < curStates.size(); v++)
                {
                    // start is the startpoint from where to look, the last car added on this beliefstate instance was on start+1.
                    int start = ((Integer)curPPos.get(v)).intValue();
                    boolean foundCar = false;

                    for(int j = start; j >= lastPossiblePos; j--)
                    {
                        // yoda and pYoda are temp variables used for constructing permutations from this previous beliefstate instance
                        Object[] yoda = ((Object[])curStates.get(v)).clone();
                        Double pYoda = new Double(((Double)curPStates.get(v)).doubleValue());

                        /* if we reached the position of the last possible pos, we need to check if there was any excess probability
                           of the remaining places, if there is count those to this position.
                         */
                        if(j == lastPossiblePos)
                        {
                            if(yoda[j] != null)
                            {
                                throw new NumberFormatException(
                                        "There was already an instance of a roaduser on the last possible " +
                                        "position of this roaduser... this should never be able to occur.");
                            }
                            yoda[j] = newCar;

                            if(foundCar == false)
                            {
                                // this is aparently the only possible spot for this car... the probability should be one
                                newState.addElement(yoda);
                                newPState.addElement(new Double(1 * pYoda.doubleValue()));
                                newPPos.addElement(new Integer(j - 2));
                            }
                            else if((1 - cumSumPQueue[j] + pQueue[j]) > 0)
                            {
                                // 1 - cumsum + the chance on current spot is the probability + the excess probabilty of this spot
                                // if its not zero, add a car permutation to the beliefstate with the right chance.
                                newState.addElement(yoda);
                                newPState.addElement(new Double((1 - cumSumPQueue[j] + pQueue[j]) *
                                        pYoda.doubleValue()));
                                newPPos.addElement(new Integer(j - 2));
                            }

                            // for the idea, this is the last iteration for j, so no need to mark this car to be found.
                        }
                        /* if there is a possibility in pQueue of the current car to be there and the last car has been seen,
                         make a possible Beliefstate if the last car isnt on that spot of the lane already;
                           Calculate the likelihood accordingly.
                         */
                        else if(pQueue[j] > 0 && yoda[j] == null)
                        {

                            yoda[j] = newCar;

                            if(foundCar == false)
                            {
                                // we werent able to place the car before, so also add summed excess probabilities to the pValue
                                newState.addElement(yoda);
                                newPState.addElement(new Double((cumSumPQueue[j]) *
                                        pYoda.doubleValue()));
                                newPPos.addElement(new Integer(j - 2));
                            }
                            else if(skipListPQueue[j] == false)
                            {
                                // we already found this car and added his excess probabilities, so now just add his probabilities
                                // from only the current spot,
                                // also the spot must not be blacklisted by the skiplist, else we skip it due to insignificance
                                newState.addElement(yoda);
                                newPState.addElement(new Double((pQueue[j]) * pYoda.doubleValue()));
                                newPPos.addElement(new Integer(j - 2));
                            }
                            // add information on the last spot taken, including 2nd spot a car uses on the lane, +1 to start
                            // thus substract 2, so the for loop can start immediately on j-2.

                            foundCar = true;
                        }
                        /* if there were excess probabilities of this car being up front on the lane while waiting for the previous
                           car put it on the direct (empty) spot after the previous car */
                        else if(cumSumPQueue[j] > 0 && yoda[j] == null && foundCar == false)
                        {
                            yoda[j] = newCar;
                            newState.addElement(yoda);
                            newPState.addElement(new Double(cumSumPQueue[j] * pYoda.doubleValue()));
                            newPPos.addElement(new Integer(j - 2));

                            foundCar = true;
                        }
                        /* if we found a car, but the probabilities are 0, it means we are done with this car on this virtual lane */
                        else if(pQueue[j] == 0 && foundCar == true)
                        {
                            break;
                        }

                    }

                }
                curStates = newState;
                curPStates = newPState;
                curPPos = newPPos;
            }

        }


        // set all found lanes and probabilities to the internal variables, which are used by the methods,
        // to actually form a beliefstate (ML, voted or Q-MDP)
        states = curStates;
        pStates = curPStates;

        // for some nice statistics on the size of the beliefState while running...
        if(pStates.size() > 0)
        {
            double totalProb = 0;
            for(int i = 0; i < pStates.size(); i++)
            {
                totalProb += ((Double)pStates.get(i)).doubleValue();
            }
            SimModel.numBeliefstates++;
            SimModel.tempBeliefStateProb = ((SimModel.tempBeliefStateProb *
                                             (SimModel.numBeliefstates - 1)) + totalProb) /
                                           (double)SimModel.numBeliefstates;
        }

        SimModel.tempBeliefStateSize += states.size();

    }

    public Vector getAllProbabilityDistributions() {
        int length = drivelane.getCompleteLength();
        Vector result = new Vector();
        double totalSum = 0;
        double totalMaxSum = 0;
        int maxQueue =-1;
        Vector maxQueues = null;
        double maxProb  = 0;
        if( pStates != null ) {
            for(int i = 0; i < pStates.size(); i++)
            {
                double prob = ((Double)pStates.get(i)).doubleValue();
                if(method_used == MOSTLIKELYQUEUE)
                {
                    if(prob == maxProb)
                    {
                        if(maxQueues == null)
                        {
                            maxQueues = new Vector();
                            maxQueues.add(new Integer(maxQueue));
                            maxQueue = -1;
                        }
                        maxQueues.add(new Integer(i));
                        totalMaxSum += prob;
                    }
                    else if(prob > maxProb)
                    {
                        maxProb = prob;
                        maxQueues = null;
                        maxQueue = i;
                        totalMaxSum = prob;
                    }
                }
                totalSum += prob;
            }

            for(int i = 0; i < roadusers.size(); i++)
            {
                Roaduser ru = (Roaduser)roadusers.get(i);
                double[] probs = (double[])pQueues.get(i);
                if(((ObservedQueue)oQueues.get(i)).getRuDetection()) {
                    if(method_used == MOSTLIKELYSTATE)
                    {
                        for(int j = 0; j < drivelane.getCompleteLength(); j++)
                        {
                            double prob = probs[j];
                            if(prob > 0)
                            {
                                ObservedRoaduser r = new ObservedRoaduser(0, length - 1 - j, ru);
                                r.setTimesSeen(prob);
                                result.add(r);
                            }
                        }
                    }
                    else
                    {
                        for(int j = 0; j < drivelane.getCompleteLength(); j++)
                        {
                            double stateProb = 0;
                            if(maxQueue > -1)
                            {
                                Object[] queue = (Object[])states.get(maxQueue);
                                if(queue[j] == ru)
                                {
                                    stateProb = 1;
                                }
                            }
                            else if(maxQueues != null)
                            {
                                for(int k = 0; k < maxQueues.size(); k++)
                                {
                                    int index = ((Integer)maxQueues.get(k)).intValue();
                                    Object[] queue = (Object[])states.get(index);
                                    if(queue[j] == ru)
                                    {
                                        stateProb += ((Double)pStates.get(index)).doubleValue();
                                    }
                                }
                                stateProb /= totalMaxSum;
                            }
                            else
                            {
                                for(int k = 0; k < states.size(); k++)
                                {
                                    Object[] queue = (Object[])states.get(k);
                                    if(queue[j] == ru)
                                    {
                                        stateProb += ((Double)pStates.get(k)).doubleValue();
                                    }
                                }
                                stateProb /= totalSum;
                            }

                            if(stateProb > 0)
                            {
                                ObservedRoaduser r = new ObservedRoaduser(0, length - 1 - j, ru);
                                r.setTimesSeen(stateProb);
                                result.add(r);
                            }
                        }
                    }
                }//end ru detection
            }
        }
        return result;
    }


    /* Returns a list of all the possible configurations of queues in the simulator's
     queue format, each of them can be used as if it was a real queue found by the simulator itself */
    private LinkedList[] getQueues()
            throws InfraException
    {

        LinkedList[] llresults = new LinkedList[states.size()];
        int index = 0;
//        boolean error = false;
        for(int i = 0; i < llresults.length; i++)
        {
            LinkedList result = new LinkedList();
            //int index = ((Integer)indexes.get(i)).intValue();

            Object[] queue = (Object[])states.get(index);
            index++;
            ListIterator vRUs = roadusers.listIterator();
            ListIterator vCQs = oQueues.listIterator();
            int speed = 0;

            for(int j = queue.length - 1; j >= 0; j--)
            {
                Roaduser ruOnQueue = (Roaduser)queue[j];

                if(ruOnQueue != null)
                {
                    Roaduser vRU = (Roaduser)vRUs.next();
                    ObservedQueue vCQ = (ObservedQueue)vCQs.next();

                    while(!vCQ.getRuDetection()) {
                        vRU = (Roaduser)vRUs.next();
                        vCQ = (ObservedQueue)vCQs.next();
                    }

                    if(!vRU.equals(ruOnQueue))
                    {
                        //Beliefstate Debugging
                        //System.out.println("curtimeStep: "+ruOnQueue.getTimeStepAsked());
                        //System.out.println("vRU.pos="+vRU.getPosition()+",.tlID="+vRU.getPrevSign()+" onQueue.pos="+ruOnQueue.getPosition()+",.tlID="+ruOnQueue.getPrevSign());
                        //error= true;
                        throw new InfraException("Roadusers are not equal, something has gone wrong");
                    }



                    speed = vCQ.getSpeedOnQueue(j);

                    ObservedRoaduser vc = new ObservedRoaduser(speed, -j + queue.length - 1, vRU);
                    result.addLast(vc);
                }

            }
            llresults[i] = result;
        }
/*        if(error) {
            System.out.println("Beliefstate DUMP:");
            gld.infra.Test.Printqueue(this);
            System.out.println("Beliefstate probabilities:");
            gld.infra.Test.printAllProbs(this);
          }*/
        return llresults;
    }

    /* This function returns the cashed most Likely Queue, which is constructed differently for each method */

    public LinkedList getQueue(PODrivelanes dl)
            throws InfraException
    {
        // mlQueue has been cached in the other methods, (updateQueues) to reflect the preferred method.
        // only av queues is not cached...
        if(method_used == MOSTLIKELYQUEUE || method_used == ALLINFRONT ||
           method_used == MOSTLIKELYSTATE || method_used == QMDP      )
        {

            return mlQueue;
        }


        if(method_used == ACTIONVOTING)
        {
            return getActionVotedQueue(dl);
        }

        throw new IllegalStateException("No valid method has been selected for the beliefstate");
    }


    /* when there are more than one most likely queues, it chooses one randomly */
    private LinkedList getMostLikelyQueue()
            throws InfraException
    {
        if(allQueues.length > 0)
        {
            return allQueues[RND.nextInt(allQueues.length)];
        }

        return null;
    }

    /* picks the queue with the highest probability from all Queues, when there are more tied for highest,
       it picks a random one from those */
    private LinkedList getMostLikelyQueueFromAllQueues()
            throws InfraException
    {
        if(pStates.size() > 0) {
            double maxProb = 0;
            int maxNr = -1;
            Vector maxNrs = null;
            for(int i = 0; i < pStates.size(); i++)
            {
               double prob = ((Double)pStates.get(i)).doubleValue();
               if(prob > maxProb) {
                   maxProb = prob;
                   maxNr = i;
                   maxNrs = null;
               }
               else if(prob == maxProb) {
                   if( maxNrs == null ) {
                       maxNrs = new Vector();
                       maxNrs.add(new Integer(maxNr));
                   }
                   maxNrs.add(new Integer(i));
               }
            }

            if( maxNrs == null && maxNr > -1) {
                return allQueues[maxNr];
            }
            else if(maxNrs != null) {
                maxNr = ((Integer)maxNrs.get(RND.nextInt(maxNrs.size()))).intValue();
                return allQueues[maxNr];
            }
        }
        return null;
    }


    private LinkedList getMostLikelyState()
            throws InfraException
    {
        LinkedList mlStates = new LinkedList();
        int laneLength = drivelane.getCompleteLength()-1;
        for(int i = 0; i < roadusers.size(); i++)
        {
           Roaduser ru = (Roaduser)roadusers.get(i);

           if(((ObservedQueue)oQueues.get(i)).getRuDetection()) {
               double[] ruProbs = (double[])pQueues.get(i);
               double maxProb = 0;
               int maxNr = -1;
               Vector maxNrs = null;
               for(int j = 0; j < ruProbs.length; j++)
               {
                   double prob = ruProbs[j];
                   if(prob > maxProb)
                   {
                       maxProb = prob;
                       maxNr = j;
                       maxNrs = null;
                   }
                   else if(prob > 0 && prob == maxProb)
                   {
                       if(maxNrs == null)
                       {
                           maxNrs = new Vector();
                           maxNrs.add(new Integer(maxNr));
                       }
                       maxNrs.add(new Integer(j));
                   }
               }

               ObservedQueue vq = (ObservedQueue)oQueues.get(i);

               if(maxNrs == null && maxNr > -1)
               {
                   int pos = laneLength - maxNr;
                   ObservedRoaduser vru = new ObservedRoaduser(vq.getSpeedOnQueue(maxNr), pos, ru);
                   mlStates.add(vru);
               }
               else if(maxNrs != null)
               {
                   maxNr = ((Integer)maxNrs.get(RND.nextInt(maxNrs.size()))).intValue();
                   int pos = laneLength - maxNr;
                   ObservedRoaduser vru = new ObservedRoaduser(vq.getSpeedOnQueue(maxNr), pos, ru);
                   mlStates.add(vru);
               }
           }//ru detection

        } //FOR roadusers


        return mlStates;
    }

    // The queue which has the most number of votes from the collection grouped by the number of road users waiting
    private LinkedList getActionVotedQueue(PODrivelanes dl)
            throws InfraException
    {
        LinkedList[] avQueues = getQueues();

        if(avQueues.length > 0)
        {
            HashMap votes = new HashMap();
            for(int i = 0; i < avQueues.length; i++)
            {
                Integer key = new Integer(dl.getNumVRoadusersWaiting(avQueues[i]));

                Vector value = (Vector)votes.get(key);
                if(value == null)
                {
                    value = new Vector();
                }
                value.addElement(new Integer(i));
                votes.remove(key);
                votes.put(key, value);
            }
            Object[] keys = votes.keySet().toArray();
            double mostVotes = 0;
            Object mostVotedKey = null;

            for(int i = 0; i < keys.length; i++)
            {
                double curVotes = 0;
                for(int j = 0; j < ((Vector)votes.get(keys[i])).size(); j++)
                {
                    curVotes +=
                            ((Double)pStates.get(((Integer)((Vector)votes.get(keys[i])).get(j)).intValue())).
                            doubleValue();
                }
                if(curVotes > mostVotes)
                {
                    mostVotedKey = keys[i];
                    mostVotes = curVotes;
                }

            }
            int numCandidates = ((Vector)votes.get(mostVotedKey)).size();

            return avQueues[((Integer)((Vector)votes.get(mostVotedKey)).get(RND.nextInt(
                    numCandidates))).intValue()];

        }
        return null;
    }

    private void updateQueues() {
        try {
            mlQueue = new LinkedList();
            // MLS is too different from MLQ, if we use MLS we take a different approach
            // on getting the mlQueue and allQueues, which other methods may use
            if(method_used == MOSTLIKELYSTATE) {
                mlQueue = getMostLikelyState();
                if(mlQueue.size() > 0) {
                    allQueues = new LinkedList[1];
                    allQueues[0] = mlQueue;
                }
                else {
                    allQueues = new LinkedList[0];
                }
            }
            // When using MLQ use that mlQueue directly which has been found there
            // if we use another method, we must get the mostLikely queue from that queue set then
            else if( states.size() > 0 ) {
                allQueues = getQueues();
                if(method_used == MOSTLIKELYQUEUE)
                {
                    mlQueue = getMostLikelyQueue();
                }
                else
                {
                    mlQueue = getMostLikelyQueueFromAllQueues();
                }
            }
            // empty state space, seems there were no roadusers, thus no queue.
            else {
                allQueues = new LinkedList[0];
            }
        }
        catch(InfraException e) {
            e.printStackTrace();
        }
    }

    public Vector getVisibleRoaduserIDs() {
        return visibleRoaduserIDs;
    }

    public ListIterator getMLQIterator() {
        return mlQueue.listIterator();
    }

    /* gets the ObservedRoaduser of Roaduser ru from the ML beliefstate since the last update,
       returns null if ru left the lane; */
    public ObservedRoaduser getMLObservedRoaduser(Roaduser ru) throws InfraException {
        if( mlQueue != null ) {
            for(int i = 0; i < mlQueue.size(); i++)
            {
                ObservedRoaduser or = (ObservedRoaduser)mlQueue.get(i);
                if(or.getRoaduser() == ru)
                {
                    return or;
                }
            }
        }

        return null;
    }


    public LinkedList getRoadusers()
    {
        return roadusers;
    }

    public ListIterator getRoadusersIterator()
    {
        return roadusers.listIterator();
    }

    public void moveRoaduser(ListIterator it, int noise_method)
    {
        moveRoaduser(it.previousIndex(), noise_method);
    }

    public void moveRoaduser(int nr, int noise_method)
    {
        ObservedQueue vc = (ObservedQueue)oQueues.get(nr);
        vc.update(noise_method);
        oQueues.set(nr, vc);
    }

    public void removeRoaduser(ListIterator roaduserIt)
    {
        int index = roaduserIt.previousIndex();
        oQueues.remove(index);
        roaduserIt.remove();
    }

    public Vector getRoaduserObservations()
    {
        return oQueues;
    }

    public void addRoaduserAtEnd(Roaduser ru)
    {
        roadusers.add(ru);
        oQueues.addElement(new ObservedQueue(ru, drivelane));
    }

    public void addRoaduser(Roaduser ru, int posQueue, int posLane)
    {
        posLane = drivelane.getCompleteLength() - posLane;
        roadusers.add(posQueue, ru);
        oQueues.add(posQueue, new ObservedQueue(ru, drivelane, posLane));
    }

    public Roaduser removeFirstRoaduser()
    {
        return removeRoaduser(0);
    }

    public boolean removeRoaduser(Roaduser ru)
    {
        int index;
        if((index = roadusers.indexOf(ru)) > -1)
        {
            oQueues.remove(index);
            return true;
        }
        return false;
    }

    public Roaduser removeRoaduser(int pos)
    {
        Roaduser ru = (Roaduser)roadusers.remove(pos);
        oQueues.remove(pos);
        return ru;
    }

    public Vector getBeliefstateVector()
    {
        return states;
    }

    public Vector getBeliefstateProbabilityVector()
    {
        return pStates;
    }

    // used for QMDP in the TLCdecision function
    public double getRoaduserPosition(int previous, int beliefstate, int nrRoaduser) /*EJUST: int --> double*/
    {
        Object[] curBeliefState = (Object[])states.get(beliefstate);

        previous = curBeliefState.length - 1 - previous;

        for(int i = previous; i >= 0; i--)
        {
            if(curBeliefState[i] == roadusers.get(nrRoaduser))
            {
                return curBeliefState.length - 1 - i;
            }

        }
        System.out.println("getRoaduserPosition(" + previous + "," + beliefstate + "," + nrRoaduser +
                           "): WARNING: position not found (laneID: " + drivelane.getId() + ")");
        return 0;
    }

    public double[] getPQueue(int i)
    {
        return(double[])pQueues.get(i);
    }


    private double[] calcCumSum(double[] p)
    {
        double[] ret = new double[p.length];
        double cumsum = 0;
        for(int i = p.length - 1; i >= 0; i--)
        {
            cumsum += p[i];
            ret[i] = cumsum;
        }
        return ret;
    }


    /* A filter which checks which place on the road has the highest likelyhood,
       it sets all but the one highest to zero. */
    private double[] pQueue_FilterMLQ(double[] p)
    {

        double maxValue = 0;
        for(int j = 0; j < p.length; j++)
        {
            if(p[j] > maxValue)
            {
                maxValue = p[j];
            }
        }

        for(int j = 0; j < p.length; j++)
        {
            if(p[j] != maxValue)
            {
                p[j] = 0;
            }
        }
        return p;
    }

    /* A filter that ignores all previous set probabilities and places new probabilities in front of the queue
       such that all roadusers seem to be waiting. */
    private double[] pQueue_FilterAIF(double[] p)
    {

        for(int j = 0; j < p.length - 1; j++)
        {
            p[j] = 0;
        }

        p[p.length - 1] = 1;
        return p;
    }


    private int cars_FilterAV()
    {
        int noCars = 0;
        try
        {
            int p = ((double[])pQueues.get(0)).length;

            for(int k = 0; k < roadusers.size(); k++)
            {
                boolean waiting = false;
                p = p - 3;

                if(p < 0)
                {
                    p = 0;
                }

                for(int l = ((double[])pQueues.get(k)).length - k - 1; l >= p; l--)
                {
                    if(((double[])pQueues.get(k))[l] != 0)
                    {
                        waiting = true;
                    }
                }

                if(waiting == false)
                {
                    break;
                }
                noCars++;
            }
        }
        catch(Exception e)
        {}
        return noCars;
    }

    /** a filter to not process the positions on a road which contributes to less than the threshold to the
        probability density */
    private boolean[] skiplistFilter(double[] p, double threshold)
    {
        boolean[] sl = new boolean[p.length];

        if(method_used == MOSTLIKELYQUEUE)
        {
            // if its the MOSTLIKELY classifier, dont wast more time than nessicary...
            for(int i = 0; i < p.length; i++)
            {
                sl[i] = false;
            }
            return sl;
        }

        HashMap lookuptable = new HashMap();
        for(int i = 0; i < p.length; i++)
        {
            // Initial value for the skip list, is to not skip the item
            sl[i] = false;

            if(p[i] > 0 && p[i] < threshold)
            {
                // a possible candidate to skip.
                Object key = new Double(p[i]);
                Object value = new Integer(i);
                Object prev = lookuptable.get(key);
                if(prev != null)
                {
                    Vector values;
                    if(prev instanceof Integer)
                    {
                        // already exist it seems, put it in an array.
                        values = new Vector(4);
                        // add previous item
                        values.add(prev);
                        // add new item
                        values.add(value);
                    }
                    else
                    {
                        // seems we already are dealing with a previous created vector, just add an element then
                        values = (Vector)prev;
                        // add new item
                        values.add(value);
                    }
                    // put it on the lookuptable
                    lookuptable.put(key, values);
                }
                else
                {
                    // did not exist jet, so just put the sigleton
                    lookuptable.put(key, value);
                }
            }
        }

        Object[] candidates = lookuptable.keySet().toArray();
        QuickSort.quicksort(candidates, true);

        double summedCandidates = 0;
        outer:for(int i = 0; i < candidates.length; i++)
        {

            summedCandidates += ((Double)candidates[i]).doubleValue();
            if(summedCandidates > threshold)
            {
                break;
            }

            Object storedValue = lookuptable.get(candidates[i]);
            if(storedValue instanceof Vector)
            {
                // multiple values are stored, we must repeat checking for subsequent values
                // first the first one, we already checked it to be below threshold
                Vector sv = (Vector)storedValue;
                sl[((Integer)sv.get(0)).intValue()] = true;

                for(int j = 1; j < sv.size(); j++)
                {
                    summedCandidates += ((Double)candidates[i]).doubleValue();
                    if(summedCandidates > threshold)
                    {
                        break outer;
                    }

                    //check succes add to skip list
                    sl[((Integer)sv.get(j)).intValue()] = true;

                }
            }
            else
            {
                // its a one sigleton, so just process directly.
                sl[((Integer)storedValue).intValue()] = true;
            }
        }

        return sl;
    }


}
