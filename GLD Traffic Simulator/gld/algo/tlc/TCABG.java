/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University
 * Copyright of the TC1 algorithm (C) Marco Wiering, Utrecht University
 *
 * This program (Green Light District) is free software.
 * You may redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation (version 2 or later).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * See the documentation of Green Light District for further information.
 *------------------------------------------------------------------------*/

package gld.algo.tlc;

import gld.Controller;
import gld.GLDSim;
import gld.algo.dp.DrivingPolicy;
import gld.algo.heuristic.HEC;
import gld.idm.Constants;
import gld.infra.Drivelane;
import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.infra.Node;
import gld.infra.Roaduser;
import gld.infra.Sign;
import gld.infra.TrafficLight;
import gld.sim.SimModel;
import gld.xml.InstantiationAssistant;
import gld.xml.XMLArray;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;

import java.awt.Color;
import java.io.IOException;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

/**
 *
 * An extra state has been added to the implementation of TC1TCLOPT.
 * It is, in principle, very similar to the CBG, it just has an accident bit instead of congestion bit.
 *
 * If true -> state will be [][][][1][] else [][][][0][];
 * Colearning hasn't been implementend correctly as well, in fact it hasn't changed since the TC1TLCOpt version.
 * @author Project group DOAS uva 2006
 * @version 1.0
 */
public class TCABG extends TCRL implements Colearning,InstantiationAssistant, HECinterface, Constants /*EJUST*/
{
    // TLC vars
    protected Infrastructure infrastructure;
    protected TrafficLight[][] tls;
    protected Node[] allnodes;
    protected Vector allsigns;
    protected int num_nodes;
    protected boolean hecAddon = false;

    // TC1 vars
    protected Vector count[][][][], pTable[][][][];
    protected float[][][][][] qTable; //sign, pos, des, isAccident(no=0, yes=1), color (red=0, green=1)
    protected float[][][][] vTable;
    protected static float gamma = 0.90f; //Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
    protected final static boolean red = false, green = true;
    protected final static int green_index = 0, red_index = 1;
    protected final static String shortXMLName = "tlc-abg";
    protected static float random_chance = 0.01f; //A random gain setting is chosen instead of the on the TLC dictates with this chance
    private Random random_number;

    /**
     * The constructor for TL controllers
     * @param The model being used.
     */

    public TCABG(Infrastructure infra) throws InfraException
    {
        super(infra);
    }

    public void setInfrastructure(Infrastructure infra)
    {
        super.setInfrastructure(infra);
        try
        {
            Node[] nodes = infra.getAllNodes();
            allnodes = nodes;
            num_nodes = nodes.length;

            int numSigns = infra.getAllInboundLanes().size();
            allsigns = new Vector(numSigns);

            for (int i = 0; i < nodes.length; i++)
            {
                Drivelane[] allInboundLanes = nodes[i].getInboundLanes();
                for (int j = 0; j < allInboundLanes.length; j++)
                {
                    allsigns.add(allInboundLanes[j].getSign().getId(), (Object) allInboundLanes[j].getSign());
                }
            }

            qTable = new float[numSigns][][][][];
            vTable = new float[numSigns][][][];
            count = new Vector[numSigns][][][];
            pTable = new Vector[numSigns][][][];

            int num_specialnodes = infra.getNumSpecialNodes();

            for (int i = 0; i < num_nodes; i++)
            {
                Node n = nodes[i];
                Drivelane[] dls = n.getInboundLanes();
                for (int j = 0; j < dls.length; j++)
                {
                    Drivelane d = dls[j];
                    Sign s = d.getSign();
                    int id = s.getId();
                    int num_pos_on_dl = d.getCompleteLength();

                    qTable[id] = new float[num_pos_on_dl][][][];
                    vTable[id] = new float[num_pos_on_dl][][];
                    count[id] = new Vector[num_pos_on_dl][][];
                    pTable[id] = new Vector[num_pos_on_dl][][];

                    for (int k = 0; k < num_pos_on_dl; k++)
                    {
                        qTable[id][k] = new float[num_specialnodes][][];
                        vTable[id][k] = new float[num_specialnodes][];
                        count[id][k] = new Vector[num_specialnodes][];
                        pTable[id][k] = new Vector[num_specialnodes][];

                        for (int m = 0; m < num_specialnodes; m++)
                        {

                            int isAccident = 2;
                            qTable[id][k][m] = new float[isAccident][];
                            vTable[id][k][m] = new float[isAccident];
                            count[id][k][m] = new Vector[isAccident];
                            pTable[id][k][m] = new Vector[isAccident];

                            for (int accident = 0; accident < isAccident;
                                               accident++)
                            {
                                qTable[id][k][m][accident] = new float[2];
                                qTable[id][k][m][accident][0] = 0.0f;
                                qTable[id][k][m][accident][1] = 0.0f;
                                vTable[id][k][m][accident] = 0.0f;
                                count[id][k][m][accident] = new Vector();
                                pTable[id][k][m][accident] = new Vector();
                            }

                        }
                    }
                }
            }
        }
        catch (Exception e)
        {}
        random_number = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
    }

    /**
     * Calculates how every traffic light should be switched
     * Per node, per sign the waiting roadusers are passed and per each roaduser the gain is calculated.
     * @param The TLDecision is a tuple consisting of a traffic light and a reward (Q) value, for it to be green
     * @see gld.algo.tlc.TLDecision
     */
    public TLDecision[][] decideTLs()
    {
        /* gain = 0
         * For each TL
         *  For each Roaduser waiting
         *   gain = gain + pf*(Q([tl,pos,des,isAccident],red) - Q([tl,pos,des,isAccident],green))
         */
        int num_dec, waitingsize, pos, tlId, desId;
        float gain, passenger_factor;
        Sign tl;
        Drivelane lane;
        Roaduser ru;
        ListIterator queue;
        Node destination;

        //Determine wheter it should be random or not
        boolean randomrun = false;
        if (random_number.nextFloat() < random_chance)
        {
            randomrun = true;
        }

        // For all Nodes
        for (int i = 0; i < num_nodes; i++)
        {
            num_dec = tld[i].length;
            // For all Trafficlights
            for (int j = 0; j < num_dec; j++)
            {
                tl = tld[i][j].getTL();
                tlId = tl.getId();
                lane = tld[i][j].getTL().getLane();

                waitingsize = lane.getNumRoadusersWaiting();
                queue = lane.getQueue().listIterator();
                gain = 0;

                // For each waiting Roaduser
                for (int k = 0; k < waitingsize; k++)
                {
                    ru = (Roaduser) queue.next();
                    
					/*EJUST: Position pos will be considered by the controller while vehicle is on its way from position pos to position pos-1*/
					pos = (int)Math.ceil(ru.getPosition());

                    
                    desId = ru.getDestNode().getId();
                    passenger_factor = ru.getNumPassengers();

                    //HEC Addon: Congestion weight factor used in calculation of the gain.
                    float congestion = (float) 1.0;
                    if (hecAddon == true)
                    {
                        congestion = HEC.getCongestion(ru, lane, tl.getNode());
                    }

                    int isAccidentInt = infra.leadsToAccidentArea(ru, lane) ? 1 : 0;

                    // Add the pf*(Q([tl,pos,des,isAccident],red)-Q([tl,pos,des,isAccident],green))
                    gain += passenger_factor * congestion *
                            (qTable[tlId][pos][desId][isAccidentInt][red_index] -
                             qTable[tlId][pos][desId][isAccidentInt][
                             green_index]); //red - green
                }

                // Debug info generator
                if (trackNode != -1 && i == trackNode)
                {
                    Drivelane currentlane2 = tld[i][j].getTL().getLane();
                    boolean[] targets = currentlane2.getTargets();
                    System.out.println("node: " + i + " light: " + j +
                                       " gain: " + gain +
                                       " " + targets[0] + " " + targets[1] +
                                       " " +
                                       targets[2] + " " +
                                       currentlane2.getNumRoadusersWaiting());
                }

                // If this is a random run, set all gains randomly
                if (randomrun)
                {
                    gain = random_number.nextFloat();
                }

                if (gain > 1000.0 || gain < -1000.0f)
                {
                    System.out.println("Gain might be too high? : " + gain);
                }
                
                tld[i][j].setGain(gain);
            }
        }
        return tld;
    }

    public void updateRoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, double prevpos /*EJUST: int --> double*/, 
    		Drivelane dlanenow, Sign signnow, double posnow /*EJUST: int --> double*/, 
    		PosMov[] posMovs, Drivelane desired, int penalty)
    {
        // Roaduser has just left the building!
        if (dlanenow == null || signnow == null)
        {
            return;
        }

        //Is there an accident somewhere?
        int isAccidentInt = (infra.leadsToAccidentArea(ru, prevlane) ? 1 : 0); // total screw
        if (isAccidentInt == 1)
        {
            ru.setHeadColor(new Color(255, 0, 0));
        }
        else
        {
            ru.setHeadColor(new Color(0, 255, 0));
        }

        //This ordening is important for the execution of the algorithm!

        if (prevsign.getType() == Sign.TRAFFICLIGHT && 
        	(signnow.getType() == Sign.TRAFFICLIGHT || 
        	 signnow.getType() == Sign.NO_SIGN))
        {
            int tlId = prevsign.getId();
            int desId = ru.getDestNode().getId();
            
			int prevpos_int = (int)Math.ceil(prevpos); /*EJUST: cast to int*/
			int posnow_int =  (int)Math.ceil(posnow); /*EJUST: cast to int*/
			 
            recalcP(tlId, prevpos_int, desId, prevsign.mayDrive(), signnow.getId(), posnow_int, isAccidentInt);
            recalcQ(tlId, prevpos, desId, prevsign.mayDrive(), signnow.getId(), posnow, posMovs, isAccidentInt, 
            		prevlane, ru, penalty);
            recalcV(tlId, prevpos_int, desId, isAccidentInt);
        }
    }

    protected void recalcP(int tlId, int pos, int desId, boolean light,
                           int tlNewId, int posNew, int isAccident)
    {
        // - First create a CountEntry, find if it exists, and if not add it.
        CountEntry thisSituation = new CountEntry(tlId, pos, desId, light, tlNewId, posNew, isAccident);
        int c_index = count[tlId][pos][desId][isAccident].indexOf(thisSituation);
        if (c_index >= 0)
        {
            // Entry found
            thisSituation = (CountEntry) count[tlId][pos][desId][isAccident].elementAt(c_index);
            thisSituation.incrementValue();
        }
        else
        {
            // Entry not found
            count[tlId][pos][desId][isAccident].addElement(thisSituation);
        }

        // We now know how often this exact situation has occurred
        // - Calculate the chance
        long sameSituation = thisSituation.getValue();
        long sameStartSituation = 0;

        CountEntry curC;
        int num_c = count[tlId][pos][desId][isAccident].size();
        for (int i = 0; i < num_c; i++)
        {
            curC = (CountEntry) count[tlId][pos][desId][isAccident].elementAt(i);
            sameStartSituation += curC.sameStartSituation(thisSituation);
        }

        // - Update this chance
        // Calculate the new P(L|(tl,pos,des,isAccident))
        // P(L|(tl,pos,des,isAccident))	= P([tl,pos,des,isAccident],L)/P([tl,pos,des,isAccident])
        //						= #([tl,pos,des,isAccident],L)/#([tl,pos,des,isAccident])
        // Niet duidelijk of dit P([tl,p,d,isAccident],L,[*,*]) of P([tl,p,d,isAccident],L,[tl,d]) moet zijn
        // Oftewel, kans op deze transitie of kans om te wachten!

        PEntry thisChance = new PEntry(tlId, pos, desId, light, tlNewId, posNew, isAccident);
        int p_index = pTable[tlId][pos][desId][isAccident].indexOf(thisChance);

        if (p_index >= 0)
        {
            thisChance = (PEntry) pTable[tlId][pos][desId][isAccident].elementAt(p_index);
        }
        else
        {
            pTable[tlId][pos][desId][isAccident].addElement(thisChance);
            p_index = pTable[tlId][pos][desId][isAccident].indexOf(thisChance);
        }

        thisChance.setSameSituation(sameSituation);
        thisChance.setSameStartSituation(sameStartSituation);

        // - Update rest of the Chance Table
        int num_p = pTable[tlId][pos][desId][isAccident].size();
        PEntry curP;
        for (int i = 0; i < num_p; i++)
        {
            curP = (PEntry) pTable[tlId][pos][desId][isAccident].elementAt(i);
            if (curP.sameStartSituation(thisSituation) && i != p_index)
            {
                curP.addSameStartSituation();
            }
        }
    }

    /**
     * Calculation of the Q-value given the current conditions for this roaduser.
     * 
     * @param tlId previous node (junction)
     * @param pos position in queue
     * @param desId destination node
     * @param light traffic light condition (red/green)
     * @param tlNewId current node
     * @param posNew new position in queue
     * @param posMovs moved positions
     * @param isAccident whether there is an accident
     * @param dlnow current drivelane
     * @param ru roaduser
     */
    protected void recalcQ(int tlId, double pos /*EJUST: int --> double*/, 
    					   int desId, boolean light, int tlNewId, double posNew /*EJUST: int --> double*/, 
    					   PosMov[] posMovs, int isAccident, Drivelane dlnow, Roaduser ru, int penalty)
    {
        // Q([tl,p,d,isAccident],L)	= Sum(tl', p') [P([tl,p,d,isAccident],L,[tl',p'])(R([tl,p,isAccident],[tl',p'])+ yV([tl',p',d,isAccident']))


        // First gather All tl' and p' in one array
        int num_posmovs = posMovs.length;

        PosMov curPosMov;
        int curPMTlId, curPMPos, curNextisAccident = isAccident;
        Sign curNextSign = dlnow.getSign();
        float R = 0, V = 0, Q = penalty;

        int pos_int = (int)Math.ceil(pos); /*EJUST: cast to int*/
        
        for (int t = 0; t < num_posmovs; t++)
        { 
        	// For All tl', pos'
            curPosMov = posMovs[t];
            curPMTlId = curPosMov.sign.getId() /*EJUST: tlId --> sign.getId() */;
            
			curPMPos  = (int) Math.ceil (curPosMov.pos); /*EJUST: cast to int*/

            if (curPMTlId != tlId)
            { 
            	// In case RU just crossed a junction, a next sign has to be determined.
                Drivelane dlnext = getNextDrivelaneByRu(ru, dlnow);
                if (dlnext != null)
                {
                    curNextSign = (Sign) dlnext.getSign();
                    curNextisAccident = infra.leadsToAccidentArea(ru, curNextSign.getLane())? 1 : 0;
                }
                else
                {
                    curNextisAccident = 0;
                }
            }
            else
            {
                curNextSign = dlnow.getSign();
                curNextisAccident = isAccident;
            }

            PEntry P = new PEntry(tlId, pos_int, desId, light, curPMTlId, curPMPos, isAccident);
            int p_index = pTable[tlId][pos_int][desId][isAccident].indexOf(P);

            if (p_index >= 0)
            {
                P = (PEntry) pTable[tlId][pos_int][desId][isAccident].elementAt(p_index);
                R = rewardFunction(tlId, pos, curPMTlId, curPosMov.pos, isAccident);
                V = vTable[curPMTlId][curPMPos][desId][curNextisAccident];

                Q += P.getChance() * (R + (gamma * V));
            }
            // Else P(..)=0, thus will not add anything in the summation
        }
        qTable[tlId][pos_int][desId][isAccident][light ? green_index : red_index] = Q;
    }

    protected void recalcV(int tlId, int pos, int desId, int isAccident)
    { 
    		//  V([tl,p,d,isAccident]) = Sum (L) [P(L|(tl,p,d,isAccident))Q([tl,p,d,isAccident],L)] ??
        float qRed = qTable[tlId][pos][desId][isAccident][red_index];
        float qGreen = qTable[tlId][pos][desId][isAccident][green_index];
        float[] pGR = calcPGR(tlId, pos, desId, isAccident);
        float pGreen = pGR[green_index];
        float pRed = pGR[red_index];

        vTable[tlId][pos][desId][isAccident] = (pGreen * qGreen) + (pRed * qRed);
    }

    /*
     ==========================================================================
     Additional methods, used by the recalc methods
     ==========================================================================
     */


    protected float[] calcPGR(int tlId, int pos, int desId, int isAccident)
    {
        float[] counters = new float[2];
        double countR = 0, countG = 0;

        int psize = pTable[tlId][pos][desId][isAccident].size() - 1;
        for (; psize >= 0; psize--)
        {
            PEntry cur = (PEntry) pTable[tlId][pos][desId][isAccident].elementAt(psize);
            if (cur.light == green)
            {
                countG += cur.getSameSituation();
            }
            else
            {
                countR += cur.getSameSituation();
            }
        }
        counters[green_index] = (float) (countG / (countG + countR));
        counters[red_index] = (float) (countR / (countG + countR));
        return counters;
    }

    protected int rewardFunction(int tlId, double pos /*EJUST: int --> double*/, 
    							 int tlNewId, double posNew /*EJUST: int --> double*/,int isAccident)
    {
        if (isAccident == 1)
        {
            return 1;
        }

        if (tlId != tlNewId || (pos-posNew) > STOP_SPEED_MS*TIMESTEP_S /*EJUST replaced: pos != posNew*/)
        {
            return 0;
        }
        return 1;
    }

    public float getVValue(Sign sign, Node des, int pos)
    {
        int isAccident = 0; // something useful could be done here with isAccident?
        return vTable[sign.getId()][pos][des.getId()][isAccident];
    }

    public float getColearnValue(Sign now, Sign sign, Node des, int pos)
    {
        return getVValue(sign, des, pos);
    }

    public void setHecAddon(boolean b, Controller c)
    {
        if (b) c.setStatus("Using HEC on ABG");
        else c.setStatus("Using ABG without HEC");
        hecAddon = b;
    }



    /*
     ==========================================================================
     Internal Classes to provide a way to put entries into the tables
     ==========================================================================
     */



    public Drivelane getNextDrivelaneByRu(Roaduser ru, Drivelane currentLane)
    {
        DrivingPolicy dp = SimModel.getDrivingPolicy();
        Drivelane destLane;
        try
        {
            destLane = dp.getDirection(ru, currentLane,currentLane.getSign().getNode());
            return destLane;
        }
        catch (InfraException e)
        {
            System.out.println(e.getMessage());
        }
        return null;

    }

    public class CountEntry implements XMLSerializable
    {
        // CountEntry vars
        int tlId, pos, desId, tlNewId, posNew, isAccident;
        long value;
        boolean light;

        // XML vars
        String parentName = "model.tlc";

        CountEntry(int _tlId, int _pos, int _desId, boolean _light,
                   int _tlNewId,
                   int _posNew, int _isAccident)
        {
            tlId = _tlId; // The Sign the RU was at
            pos = _pos; // The position the RU was at
            desId = _desId; // The SpecialNode the RU is travelling to
            light = _light; // The colour of the Sign the RU is at now
            tlNewId = _tlNewId; // The Sign the RU is at now
            posNew = _posNew; // The position the RU is on now
            isAccident = _isAccident; // ID of the next sign(accident there?)
            value = 1; // How often this situation has occurred
        }

        public CountEntry()
        { // Empty constructor for loading
        }

        public void incrementValue()
        {
            value++;
        }

        // Returns how often this situation has occurred
        public long getValue()
        {
            return value;
        }

        public boolean equals(Object other)
        {
            if (other != null && other instanceof CountEntry)
            {
                CountEntry countnew = (CountEntry) other;
                if (countnew.tlId != tlId)
                {
                    return false;
                }
                if (countnew.pos != pos)
                {
                    return false;
                }
                if (countnew.desId != desId)
                {
                    return false;
                }
                if (countnew.light != light)
                {
                    return false;
                }
                if (countnew.tlNewId != tlNewId)
                {
                    return false;
                }
                if (countnew.posNew != posNew)
                {
                    return false;
                }
                if (countnew.isAccident != isAccident)
                {
                    return false;
                }
                return true;
            }
            return false;
        }

        // Retuns the count-value if the situations match
        public long sameSituation(CountEntry other)
        {
            if (equals(other))
            {
                return value;
            }
            else
            {
                return 0;
            }
        }

        // Retuns the count-value if the startingsituations match
        public long sameStartSituation(CountEntry other)
        {
            if (other.tlId == tlId && other.pos == pos && other.desId == desId &&
                other.light == light && other.isAccident == isAccident)
            {
                return value;
            }
            else
            {
                return 0;
            }
        }

        // XMLSerializable implementation of CountEntry
        public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
        {
            pos = myElement.getAttribute("pos").getIntValue();
            tlId = myElement.getAttribute("tl-id").getIntValue();
            desId = myElement.getAttribute("des-id").getIntValue();
            light = myElement.getAttribute("light").getBoolValue();
            tlNewId = myElement.getAttribute("new-tl-id").getIntValue();
            posNew = myElement.getAttribute("new-pos").getIntValue();
            isAccident = myElement.getAttribute("next-sign-id").getIntValue();
            value = myElement.getAttribute("value").getLongValue();
        }

        public XMLElement saveSelf() throws XMLCannotSaveException
        {
            XMLElement result = new XMLElement("count");
            result.addAttribute(new XMLAttribute("pos", pos));
            result.addAttribute(new XMLAttribute("tl-id", tlId));
            result.addAttribute(new XMLAttribute("des-id", desId));
            result.addAttribute(new XMLAttribute("light", light));
            result.addAttribute(new XMLAttribute("new-tl-id", tlNewId));
            result.addAttribute(new XMLAttribute("new-pos", posNew));
            result.addAttribute(new XMLAttribute("next-sign-id", isAccident));
            result.addAttribute(new XMLAttribute("value", value));
            return result;
        }

        public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException
        { // A count entry has no child objects
        }

        public String getXMLName()
        {
            return parentName + ".count";
        }

        public void setParentName(String parentName)
        {
            this.parentName = parentName;
        }
    }


    public class PEntry implements XMLSerializable
    {
        // PEntry vars
        int pos, posNew, tlId, tlNewId, desId, isAccident;
        double sameStartSituation, sameSituation;
        boolean light;

        // XML vars
        String parentName = "model.tlc";

        PEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew, int _isAccident)
        {
            tlId = _tlId; // The Sign the RU was at
            pos = _pos; // The position the RU was at
            desId = _desId; // The SpecialNode the RU is travelling to
            light = _light; // The colour of the Sign the RU is at now
            tlNewId = _tlNewId; // The Sign the RU is at now
            posNew = _posNew; // The position the RU is on now
            isAccident = _isAccident;
            sameStartSituation = 0; // How often this situation has occurred
            sameSituation = 0;
        }

        public PEntry()
        { 
        	// Empty constructor for loading
        }

        public void addSameStartSituation()
        {
            sameStartSituation++;
        }

        public void setSameStartSituation(long s)
        {
            sameStartSituation = s;
        }

        public void setSameSituation(long s)
        {
            sameSituation = s;
        }

        public double getSameStartSituation()
        {
            return sameStartSituation;
        }

        public double getSameSituation()
        {
            return sameSituation;
        }

        public double getChance()
        {
            return getSameSituation() / getSameStartSituation();
        }

        public boolean equals(Object other)
        {
            if (other != null && other instanceof PEntry)
            {
                PEntry pnew = (PEntry) other;
                if (pnew.tlId != tlId)
                {
                    return false;
                }
                if (pnew.pos != pos)
                {
                    return false;
                }
                if (pnew.desId != desId)
                {
                    return false;
                }
                if (pnew.light != light)
                {
                    return false;
                }
                if (pnew.tlNewId != tlNewId)
                {
                    return false;
                }
                if (pnew.posNew != posNew)
                {
                    return false;
                }
                if (pnew.isAccident != isAccident)
                {
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean sameSituation(CountEntry other)
        {
            return equals(other);
        }

        public boolean sameStartSituation(CountEntry other)
        {
            if (other.tlId == tlId && other.pos == pos && other.desId == desId &&
                other.light == light && other.isAccident == isAccident)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        // XMLSerializable implementation of PEntry
        public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
        {
            pos = myElement.getAttribute("pos").getIntValue();
            tlId = myElement.getAttribute("tl-id").getIntValue();
            desId = myElement.getAttribute("des-id").getIntValue();
            light = myElement.getAttribute("light").getBoolValue();
            tlNewId = myElement.getAttribute("new-tl-id").getIntValue();
            posNew = myElement.getAttribute("new-pos").getIntValue();
            isAccident = myElement.getAttribute("next-sign-id").getIntValue();
            sameStartSituation = myElement.getAttribute("same-startsituation").getLongValue();
            sameSituation = myElement.getAttribute("same-situation").getLongValue();
        }

        public XMLElement saveSelf() throws XMLCannotSaveException
        {
            XMLElement result = new XMLElement("pval");
            result.addAttribute(new XMLAttribute("pos", pos));
            result.addAttribute(new XMLAttribute("tl-id", tlId));
            result.addAttribute(new XMLAttribute("des-id", desId));
            result.addAttribute(new XMLAttribute("light", light));
            result.addAttribute(new XMLAttribute("new-tl-id", tlNewId));
            result.addAttribute(new XMLAttribute("new-pos", posNew));
            result.addAttribute(new XMLAttribute("next-sign-id", isAccident));
            result.addAttribute(new XMLAttribute("same-startsituation", sameStartSituation));
            result.addAttribute(new XMLAttribute("same-situation", sameSituation));
            return result;
        }

        public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException
        { 
        	// A PEntry has no child objects
        }

        public void setParentName(String parentName)
        {
            this.parentName = parentName;
        }

        public String getXMLName()
        {
            return parentName + ".pval";
        }
    }


    public void showSettings(Controller c)
    {
        String[] descs = {"Gamma (discount factor)", "Random decision chance"};
        float[] floats = {gamma, random_chance};
        TLCSettings settings = new TLCSettings(descs, null, floats);

        settings = doSettingsDialog(c, settings);
        gamma = settings.floats[0];
        random_chance = settings.floats[1];
    }

    // XMLSerializable, SecondStageLoader and InstantiationAssistant implementation

    public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
    {
        super.load(myElement, loader);
        gamma = myElement.getAttribute("gamma").getFloatValue();
        random_chance = myElement.getAttribute("random-chance").getFloatValue();
        qTable = (float[][][][][]) XMLArray.loadArray(this, loader);
        vTable = (float[][][][]) XMLArray.loadArray(this, loader);
        count = (Vector[][][][]) XMLArray.loadArray(this, loader, this);
        pTable = (Vector[][][][]) XMLArray.loadArray(this, loader, this);
    }

    public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException,
            XMLCannotSaveException
    {
        super.saveChilds(saver);
        XMLArray.saveArray(qTable, this, saver, "q-table");
        XMLArray.saveArray(vTable, this, saver, "v-table");
        XMLArray.saveArray(count, this, saver, "counts");
        XMLArray.saveArray(pTable, this, saver, "p-table");
    }

    public XMLElement saveSelf() throws XMLCannotSaveException
    {
        XMLElement result = super.saveSelf();
        result.setName(shortXMLName);
        result.addAttribute(new XMLAttribute("random-chance", random_chance));
        result.addAttribute(new XMLAttribute("gamma", gamma));
        return result;
    }

    public String getXMLName()
    {
        return "model." + shortXMLName;
    }

    public boolean canCreateInstance(Class request)
    {
        System.out.println("Called TCABG instantiation assistant ??");
        return CountEntry.class.equals(request) ||
                PEntry.class.equals(request);
    }

    public Object createInstance(Class request) throws
            ClassNotFoundException, InstantiationException,
            IllegalAccessException
    {
        System.out.println("Called TCABG instantiation assistant");
        if (CountEntry.class.equals(request))
        {
            return new CountEntry();
        }
        else if (PEntry.class.equals(request))
        {
            return new PEntry();
        }
        else
        {
            throw new ClassNotFoundException
                    ("TCABG IntstantiationAssistant cannot make instances of " +
                     request);
        }
    }
}
