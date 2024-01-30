package gld.infra;

import gld.idm.Constants;
import gld.sim.SimModel;

import java.util.Vector;

/*POMDPGLD*/
public class ObservedQueue implements Constants /*EJUST*/
{
    private Roaduser ru;
    private Drivelane lane;
    private Vector[] oQueue;
    /*virtual possible individual Ru queues (includes: all possible speed/positions)*/
    private double speed; /*EJUST: int-->double*/

    public final static int NOCHANGE = 0;
    public final static int GAUSSIAN = 1;
    public final static int UNIFORM = 2;
    
	/** EJUST: Position update time step*/
	protected static final double dt = TIMESTEP_S;
	
    public ObservedQueue(Roaduser ru, Drivelane lane)
    {
        this.ru = ru;
        this.lane = lane;
        this.speed = ru.getSpeed();
        this.oQueue = new Vector[lane.getCompleteLength()];
        boolean detection = SimModel.ruDetected();


        oQueue[1] = new Vector();
        oQueue[1].add(new ObservedRoaduser(speed, 0, 1, detection));
    }

    public ObservedQueue(Roaduser ru, Drivelane lane, int pos)
    {
        this.ru = ru;
        this.lane = lane;
        this.speed = ru.getSpeed();
        this.oQueue = new Vector[lane.getCompleteLength()];

        oQueue[pos] = new Vector();
        oQueue[pos].add(new ObservedRoaduser(speed, pos, 1,true));
    }

    public boolean getRuDetection() {
        if( oQueue[1] != null && ! oQueue[1].isEmpty() ) {
           ObservedRoaduser oru = (ObservedRoaduser) oQueue[1].firstElement();
           return oru.getDectection();
        }
        return true;
    }

    public double[] calculatePQueue()
    {
        double totalPossibleSolutions = 0;
        double[] pQueue = new double[lane.getCompleteLength()];

        for(int i = 0; i < oQueue.length; i++)
        {
            if(oQueue[i] != null)
            {
                for(int j = 0; j < oQueue[i].size(); j++)
                {
                    ObservedRoaduser temp = (ObservedRoaduser)oQueue[i].get(j);
                    totalPossibleSolutions += temp.getTimesSeen();
                }
            }
        }

        for(int i = 0; i < oQueue.length; i++)
        {
            if(oQueue[i] != null)
            {
                for(int j = 0; j < oQueue[i].size(); j++)
                {
                    pQueue[i] +=
                            (((ObservedRoaduser)oQueue[i].get(j)).getTimesSeen() / totalPossibleSolutions);

                }
            }
            else
            {
                pQueue[i] = 0;
            }
        }
        return pQueue;
    }

    public void update(int type)
    {
        Vector[] newoQueue = new Vector[oQueue.length];

        if(ru.getPosition() > STOP_SPEED_MS*dt /*EJUST replaced: ru.getPosition() > 0 */  
        		|| !SimModel.ruDetected()) // the only sensoric info we may use... checking if this Roaduser is standing on the last spot
        {
            int maxPos = oQueue.length - 1;
            for(int i = 0; i < oQueue.length; i++) /*for the whole queue*/
            {
                if(oQueue[i] != null)
                {
                    Vector oRus = oQueue[i];
                    for(int j = 0; j < oRus.size(); j++) /*for all Rus*/
                    {
                        ObservedRoaduser oRu = (ObservedRoaduser)oRus.get(j);
                        double speed = oRu.getSpeed(); /*EJUST: int-->double*/
                        double chanceOnCurPos = oRu.getTimesSeen();
                        boolean ruDetection = oRu.getDectection();

                        if (ruDetection ) {
                            ObservedRoaduser[] posMovs = getPosMovs(type, speed, i, maxPos, chanceOnCurPos);

                            for(int k = 0; k < posMovs.length; k++)
                            /*for all moves*/
                            {

                                if(newoQueue[(int)Math.ceil(posMovs[k].getPos())] != null) /*EJUST: cast to int */
                                {
                                    boolean sameRu = false;
                                    for(int n = 0; n < newoQueue[(int)Math.ceil(posMovs[k].getPos())].size(); n++) /*EJUST: cast to int*/
                                    /*for all possible positions*/
                                    {
                                        ObservedRoaduser preoRu = ((ObservedRoaduser)newoQueue[(int)Math.ceil(posMovs[k].getPos())].get(n));
                                        /*EJUST: cast to int */
                                        
                                        if(Math.abs(preoRu.getSpeed()-posMovs[k].getSpeed())<=STOP_SPEED_MS /*EJUST replaced: speed1==speed2*/)
                                        {
                                            sameRu = true;
                                            preoRu.setTimesSeen(preoRu.getTimesSeen() + (posMovs[k].getTimesSeen()));
                                            newoQueue[(int)Math.ceil(posMovs[k].getPos())].set(n, preoRu); /*EJUST: cast to int */
                                        }
                                    }
                                    /*add Ru to queue*/
                                    if(!sameRu)
                                    {
                                        newoQueue[(int)Math.ceil(posMovs[k].getPos())].add(posMovs[k]); /*EJUST: cast to int */
                                    }
                                }
                                else
                                {
                                    newoQueue[(int)Math.ceil(posMovs[k].getPos())] = new Vector(); /*EJUST: cast to int */
                                    newoQueue[(int)Math.ceil(posMovs[k].getPos())].add(posMovs[k]); /*EJUST: cast to int */
                                }
                            }
                        }
                        else {
                            // ru is not detected, add it again on the new queue
                            newoQueue[i] = new Vector();
                            newoQueue[i].add(oRu);
                        }
                    }
                }
            }
        }
        else
        {
            // sensor at end gives 100% ru at end.

                newoQueue[oQueue.length - 1] = new Vector();
                newoQueue[oQueue.length - 1].addElement(new ObservedRoaduser(ru.getSpeed(), oQueue.length - 1, 1, true));


        }
        oQueue = newoQueue;

    }


    private ObservedRoaduser[] getPosMovs(int type, double speed, double pos, double maxPos, double chance) /*EJUST: int-->double*/
    {
        /*keep in mind the Ru can switch speed*/
        ObservedRoaduser[] posMovs;
        Vector temp = new Vector();

        switch(type)
        {
            case ObservedQueue.NOCHANGE:

                if(!(1-(speed - 1) > STOP_SPEED_MS  /* EJUST replaced: (speed - 1) < 1 */) && 
                	maxPos - (pos + speed*dt - 1) > STOP_SPEED_MS*dt  /* EJUST replaced: ( (pos + speed*dt - 1) < maxPos*/)
                {
                    temp.addElement(new ObservedRoaduser(speed - 1, pos + speed*dt - 1, 0, true));
                }

                if( maxPos - (pos + speed*dt) > STOP_SPEED_MS*dt  /* EJUST replaced: ( (pos + speed*dt) < maxPos*/)
                {
                    temp.addElement(new ObservedRoaduser(speed, pos + speed*dt, 1,true));
                }
                if(!((speed + 1)-4 > STOP_SPEED_MS) /* EJUST replaced: (speed + 1) > 4 */ && 
                		maxPos - (pos + speed*dt + 1) > STOP_SPEED_MS*dt /* EJUST replaced: (pos + speed*dt + 1) < maxPos */)
                {
                    temp.addElement(new ObservedRoaduser(speed + 1, pos + speed*dt + 1, 0,true));
                }

                break;

            case ObservedQueue.GAUSSIAN:

                if(!(1-(speed - 1) > STOP_SPEED_MS) /*EJUST replaced: (speed-1) < 1*/&& 
                	maxPos - (pos + speed*dt - 1) > STOP_SPEED_MS*dt /*EJUST replaced: (pos + speed*dt - 1) < maxPos*/)
                {
                    temp.addElement(new ObservedRoaduser(speed - 1, pos + speed*dt - 1, 0.1065 ,true));
                }
                if(maxPos - (pos + speed*dt) > STOP_SPEED_MS*dt /*EJUST replaced: (pos + speed*dt) < maxPos */)
                {
                    temp.addElement(new ObservedRoaduser(speed, pos + speed*dt, 0.7870 ,true));
                }
                if(!((speed + 1)-4 > STOP_SPEED_MS /*EJUST replaced: (speed+1) > 4*/) && 
                		 maxPos - (pos + speed*dt + 1) > STOP_SPEED_MS*dt /*EJUST replaced: (pos + speed*dt + 1) < maxPos */)
                {
                    temp.addElement(new ObservedRoaduser(speed + 1, pos + speed*dt + 1, 0.1065 ,true));
                }


                break;

            case ObservedQueue.UNIFORM:
            default:

                if(!( 1 - (speed - 1) > STOP_SPEED_MS  /*EJUST replaced: (speed - 1) < 1*/) && 
                		maxPos - (pos + speed*dt - 1) > STOP_SPEED_MS*dt /*EJUST replaced: (pos + speed*dt - 1) < maxPos */)
                {
                    temp.addElement(new ObservedRoaduser(speed - 1, pos + speed*dt - 1, 0.3333 ,true));
                }
                if( maxPos - (pos + speed*dt) > STOP_SPEED_MS*dt /*EJUST replaced: (pos + speed*dt) < maxPos */)
                {
                    temp.addElement(new ObservedRoaduser(speed, pos + speed*dt, 0.3333 ,true));
                }
                if(!((speed + 1)-4 > STOP_SPEED_MS /*EJUST replaced: (speed+1) > 4*/) && 
                	 maxPos - (pos + speed*dt + 1) > STOP_SPEED_MS*dt /*EJUST replaced: (pos + speed*dt + 1) < maxPos */)
                {
                    temp.addElement(new ObservedRoaduser(speed + 1, pos + speed*dt + 1, 0.3333 ,true));
                }


                break;

        }

        // NORMALIZATION

        // seems this Ru already reached the maxPos or is enqueued in waiting
        if(temp.size() == 0)
        {
            temp.addElement(new ObservedRoaduser(speed, maxPos-1, 1, true));
        }

        // else normalize over the posMovs found so the sum up to 1
        else
        {
            double summedP = 0;
            for(int i = 0; i < temp.size(); i++)
            {
                ObservedRoaduser vc = (ObservedRoaduser)temp.get(i);
                summedP += vc.getTimesSeen();
            }

            // no divisions by zero
            if(summedP > 0) {
                for(int i = 0; i < temp.size(); i++)
                {
                    ObservedRoaduser vc = (ObservedRoaduser)temp.get(i);

                    vc.setTimesSeen(vc.getTimesSeen() / summedP);
                    temp.set(i, vc);
                }
            }
            else {
                temp.addElement(new ObservedRoaduser(speed, maxPos, 1, true));
            }

        }

        posMovs = new ObservedRoaduser[temp.size()];
        for(int i = 0; i < temp.size(); i++)
        {
            ObservedRoaduser vc = (ObservedRoaduser)temp.get(i);
            vc.setTimesSeen(vc.getTimesSeen() * chance);
            posMovs[i] = vc;
        }

        return posMovs;
    }

    public int getSpeedOnQueue(int pos)
    {
        double speed = 0;
        double impact = 0;
        boolean speedisZero = false;
        if(oQueue[pos] == null )
        {
            speedisZero = true;
        }
        else
        {
            for(int i = 0; i < oQueue[pos].size(); i++)
            {
               ObservedRoaduser vc = (ObservedRoaduser)oQueue[pos].get(i);

               speed += (vc.getSpeed() * vc.getTimesSeen());
               impact += vc.getTimesSeen();

            }
            if(speed<=STOP_SPEED_MS /*EJUST replaced: speed==0*/) {
                speedisZero = true;
            }
        }


        if(speedisZero)
        {
            for(int i = oQueue.length-1; i >= pos ; i--)
            {
                if( oQueue[i] != null ) {
                    for(int j = 0; j < oQueue[i].size(); j++)
                    {
                        ObservedRoaduser vc = (ObservedRoaduser)oQueue[i].get(j);
                        speed += (vc.getSpeed() * vc.getTimesSeen());
                        impact += vc.getTimesSeen();

                    }
                }
            }
        }


        return((int)Math.round(speed / impact));
    }


}
