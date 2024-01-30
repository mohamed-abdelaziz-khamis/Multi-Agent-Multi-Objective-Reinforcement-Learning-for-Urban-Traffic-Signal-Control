/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University
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

package gld.infra;

import gld.GLDException;
import gld.GLDSim;
import gld.Model;
import gld.SelectionStarter;
import gld.algo.edit.ShortestPathCalculator;
import gld.edit.Validation;
import gld.infra.Drivelane.DrivelaneColearnStatistics;
import gld.infra.Drivelane.DrivelaneSpeedStatistics;
import gld.infra.Node.NodeStatistics;
import gld.utils.ArrayEnumeration;
import gld.utils.Arrayutils;
import gld.utils.NumberDispenser;
import gld.xml.XMLArray;
import gld.xml.XMLAttribute;
import gld.xml.XMLCannotSaveException;
import gld.xml.XMLElement;
import gld.xml.XMLInvalidInputException;
import gld.xml.XMLLoader;
import gld.xml.XMLSaver;
import gld.xml.XMLSerializable;
import gld.xml.XMLTreeException;
import gld.xml.XMLUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;


/**
 *
 * The encapsulating class
 *
 * @author Group Datastructures
 * @version 1.0
 */

public class Infrastructure implements XMLSerializable, SelectionStarter
{
    /** All nodes in this infrastructure, including edge nodes */
    protected Node[] allNodes;
    /** All exit/entry nodes in this infrastructure */
    protected SpecialNode[] specialNodes;
    /** All nodes that are not EdgeNodes */
    protected Junction[] junctions;
    /** Meta-data provided by the user */
    protected String title, author, comments;
    /** The infrastructure version of this implementation. For debugging. */
    protected final int version = 1;
    /** The size of this infrastructure, in pixels */
    protected Dimension size;
    
    /** All the inbound lanes on all the Nodes in our Infrastructure */
    protected Vector<Drivelane> allLanes;
    
    /** Number dispenser for sign id's */
    protected NumberDispenser signNumbers = new NumberDispenser();
    /** The current timeStep we're in, mainly for Nodes to have access to this data*/
    protected int curTimeStep;
    protected int curSeries;
    /** DOAS 06: tracking the number of cars removed */
    protected int removedCars = 0;
    /** DOAS 06: tracking the number of cars entered */
    protected int enteredCars = 0;

    
    /** EJUST: tracking the number of roadusers generated */
    protected int generatedRoadusers = 0;

    
    /** List of disabled Lanes */
    protected static Vector disabledLanes = new Vector();
    protected static Vector notYetDisabledLanes = new Vector();
    
    /** Accidents rate (DOAS 06) */
    protected static int accidentsRate = 200;
    
    /** Random class */
    protected Random rnd = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]);
    
    public static final int blockLength = 1; // SBC: original 10 --> 1
    public static final int blockWidth = 10;

    /*POMDPGLD*/
    public int roadsegments = -1;
    
    private Validation validator;

    protected String parentName = "model";

    // This one is temporary (for debugging TC-3)
    public static Hashtable laneDictionary;


    /**
     * Creates a new infrastructure object.
     *
     * @param dim The dimension of the new infrastructure
     */
    public Infrastructure(Dimension dim)
    {
        size = dim;
        allNodes = new Node[0];
        specialNodes = new SpecialNode[0];
        junctions = new Junction[0];
        title = "untitled";
        author = "unknown";
        comments = "";
        curTimeStep = 0;
        validator = new Validation(this);
    }

    /**
     * Creates a new infrastructure object.
     *
     * @param nodes The Nodes this Infrastructure should contain.
     * @param edge The exit/entry nodes this Infrastructure should contain.
     * @param new_size The size of this Infrastructure in pixels x pixels
     */
    public Infrastructure(Node[] nodes, SpecialNode[] special, Dimension new_size)
    {
        allNodes = nodes;
        specialNodes = special;
        junctions = new Junction[allNodes.length - specialNodes.length];
        copyJunctions();
        size = new_size;
        title = "untitled";
        author = "unknown";
        comments = "";
        validator = new Validation(this);
    }

    /** Constructor for loading */
    public Infrastructure()
    {
        allNodes = new Node[0];
        specialNodes = new SpecialNode[0];
        junctions = new Junction[0];
        allLanes = new Vector();
        title = "untitled";
        author = "";
        comments = "";
        size = new Dimension(5000, 3000);
        validator = new Validation(this);
    }

    // Function that disables a random lane, and updates all targets to that lane so that
    // cars wont be able to go there anymore. It will store the default values in the particular
    // drivelanes so that when the lane becomes available again the original target will be restored
    // thus letting cars be able to go there again. (DOAS 05)
    /// @return True, if any lane was actually disabled (DOAS 06)

    public boolean disableRandomLane()
    {
        if (rnd.nextInt(accidentsRate) != 0)
        {
            return false;
        }

        // there must be a finite number of trials, so that this does not end in an infinite loop,
        // when there is no accident possible (DOAS 06)
        if(notYetDisabledLanes.size() == 0) return false;
        int maxTrialsCount = java.lang.Math.max(notYetDisabledLanes.size() / 4, 1);
        //while (true)
        for(int trial = 0; trial < maxTrialsCount; trial++)
        {
            Drivelane toBeDisabledLane = null;
            // probability of an accident goes higher with the number of cars on the lane (DOAS 06)
            // upper bound of the timeSteps count because the simulation must not be slown down by infinite timeSteps
            for(int tr = 0; tr < 16; tr++){
                int randint = rnd.nextInt(notYetDisabledLanes.size());
                toBeDisabledLane = (Drivelane) notYetDisabledLanes.get(randint);
                if(rnd.nextInt(toBeDisabledLane.getLength()) < toBeDisabledLane.getNumBlocksTaken()){
                    break;
                }
            }

            int disabledLaneIndex = disabledLanes.size();   //index of the first disabled lane in the disabledLanes vector
            try
            { // Junctions only So no roads going from an edgenode or a non junction node will be able to be
                //disabled
                Junction intersection /*kruispunt*/ = (Junction) toBeDisabledLane.getNodeComesFrom();

                // Throws exception if the lane is leading to an edgenode, disabling the possibility that access
                // to an edge Node will be blocked
                Junction nextIntersection /*nextKruispunt*/ = (Junction) toBeDisabledLane.getNodeLeadsTo();

                // Throws an exception if one more accident would block the junction
                // (there would be no way out for incoming cars - dead-end) (DOAS 06)
                /*kruispunt*/intersection.increaseAccidentsCount();

                Road ro = toBeDisabledLane.getRoad();
                //System.out.println(ro.getName() + " has been disabled [" + curTimeStep + "]");
                Drivelane[] otherlanes;

                // Determine if shared lanes (in the same direction) are on the alpha or beta Lane towards kruispunt
                if (ro.getAlphaNode() == toBeDisabledLane.getNodeComesFrom())
                {
                    otherlanes = ro.getBetaLanes();
                }
                else
                {
                    otherlanes = ro.getAlphaLanes();
                }

                // Disable all lanes on the road shared by the target lane to be disabled
                for (int i = 0; i < otherlanes.length; i++)
                {
                    disabledLanes.add((Object) otherlanes[i]);
                    notYetDisabledLanes.remove((Object) otherlanes[i]);
                }

                Road[] incomingRoads = /*kruispunt*/intersection.getAllRoads();

                // check which numbers or outgoing roads is the disabled one
                int ro_num = 0;
                for (int i = 0; i < 4; i++)
                {
                    if (incomingRoads[i] != null && incomingRoads[i] == ro)
                    {
                        ro_num = i;
                        break;  //(DOAS 06)
                    }
                }
                
                // disable for all non outgoing roads the target to the disabled one
                for (int i = 0; i < 4; i++)
                {
                    if (incomingRoads[i] != null && i != ro_num)
                    {
                        // this incoming road ri.
                        Road ri = incomingRoads[i];

                        // check relative direction between ri and ro
                        int dir = Node.getDirection(i, ro_num); // DIR: 1: left, 2: straight, 3: right

                        // be sure only to get the lanes leading to the crossing to have their targets disabled
                        Drivelane[] il;
                        if (ri.getAlphaNode() == /*kruispunt*/intersection)
                        {
                            //Road heeft kruispunt als Alpha node
                        	//Road has intersection as the Alpha node
                            il = ri.getAlphaLanes();
                        }
                        else
                        {
                            il = ri.getBetaLanes();
                        }

                        // change the correct targets.
                        for (int j = 0; j < il.length; j++)
                        {
                            il[j].setTarget(dir - 1, false, false); // DIR: 0: left, 1: straight, 2: right

                            //check if there is still a way out of the road, or if we need to make another route
                            int ndir = dir - 1;
                            boolean allFalse = true;
                            for (int k = 0; k < 3; k++)
                            {
                                if (k != ndir && il[j].getTarget(k) == true)
                                {
                                    //Only if DL is already leading to another junction allFalse be set to false
                                    int d = (k + 1);
                                    //no. of target lane:
                                    int t = (i + d) % 4;
                                    Node nextnode;
                                    if(incomingRoads[t].getAlphaNode() == /*kruispunt*/intersection)
                                        nextnode = incomingRoads[t].getBetaNode();
                                    else
                                        nextnode = incomingRoads[t].getAlphaNode();
                                    if(nextnode.getType() == Node.JUNCTION)
                                       allFalse = false;
                                }
                            }
                            //Cars can go nowhere now, we need to open another way
                            if (allFalse)
                            {
                                for (int k = 0; k < 3; k++)
                                {
                                    if (k != ndir)
                                    {

                                        //k ranges from 0-2, while dirs range from 1-3: 1: left, 2: straight, 3: right
                                        int d = (k + 1);

                                        //no of target road:
                                        int t = (i + d) % 4;

                                        //check if target road is not disabled we are still on lane nr i.

                                        //possible lanes:
                                        Drivelane[] pl;
                                        if (incomingRoads[t].getAlphaNode() ==  /*kruispunt*/intersection)
                                        {
                                            pl = incomingRoads[t].getBetaLanes();
                                        }
                                        else
                                        {
                                            pl = incomingRoads[t].getAlphaLanes();
                                        }

                                        //check if drivelanes are not disabled yet (DOAS 05)
                                        if (!disabledLanes.contains(pl[0]))
                                        {
                                            il[j].setTarget(k, true, false); //create target to destination lane at drivelane j.
                                        }
                                    }
                                }
                            } // All False
                        } // Change targets

                    }
                }// Everything disabled towards disabled road


                // the results of the check must be used (DOAS 06)
                boolean infraOK = true;
                try{
                    //Check if everything is still okay about the Infrastructure
                    Vector errors = validator.validate();
                    if(errors.size() > 0){
                        infraOK = false;
                    }
                }catch(InfraException e){
                    infraOK = false;
                }

                if(!infraOK){
                    //System.out.println(ro.getName() + " cannot be disabled. Enabled again.");
                    enableLane(disabledLaneIndex);
                    continue;   //try another lane
                }


                return true;
            }
            catch (Exception e) // If there is an exception The drivelane will not be disabled
            {}
        }
        
        return false;
    }

    // opposite of the disabled function (DOAS 05)
    /// @return True, if some lane was enabled (DOAS 06)

    public boolean enableRandomLane()
    {
        if (disabledLanes.size() == 0 || (rnd.nextInt(300) + 1 > 1 * disabledLanes.size()))
        {
            return false;
        }
        int numLanes = disabledLanes.size();
        
        // in case something goes weird, only the finite number of trials is to be performed (DOAS 06)
        for(int i = 0; i < 10; i++)
        {
            try
            {
                enableLane(rnd.nextInt(numLanes));
                return true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        return false;
    }

    protected void enableLane(int disabledLaneIndex) throws InfraException{
        // Junctions only
        Drivelane toBeEnabledLane = (Drivelane) disabledLanes.get(disabledLaneIndex);
        Junction kruispunt = (Junction) toBeEnabledLane.getNodeComesFrom();

        kruispunt.decreaseAccidentsCount();

        Road ro = toBeEnabledLane.getRoad();
        //System.out.println(ro.getName() + " has been enabled [" + curTimeStep + "]");
        Drivelane[] otherlanes;
        if (ro.getAlphaNode() == toBeEnabledLane.getNodeComesFrom())
        {
            otherlanes = ro.getBetaLanes();
        }
        else
        {
            otherlanes = ro.getAlphaLanes();
        }

        for (int i = 0; i < otherlanes.length; i++)
        {
            disabledLanes.remove((Object) otherlanes[i]);
            notYetDisabledLanes.add((Object) otherlanes[i]);
        }
        Road[] incomingRoads = kruispunt.getAllRoads();
        int ro_num = 0;
        for (int i = 0; i < 4; i++)
        {
            if (incomingRoads[i] != null && incomingRoads[i] == ro)
            {
                ro_num = i;
            }
        }
        for (int i = 0; i < 4; i++)
        {
            if (incomingRoads[i] != null && i != ro_num)
            {
                Road ri = incomingRoads[i];
                int dir = Node.getDirection(i, ro_num);
                Drivelane[] il;

                if (ri.getAlphaNode() == kruispunt)
                { //Road heeft kruispunt als Alpha node
                    il = ri.getAlphaLanes();
                }
                else
                { 
                	//Road heeft kruispunt als Beta node
                    il = ri.getBetaLanes();
                }
                for (int j = 0; j < il.length; j++)
                {
                    for (int k = 0; k < 3; k++) //Recover all original targets
                    {
                        il[j].setTarget(k, false, true); // DIR: 3: left, 2: streight, 1: right


                        //k ranges from 0-2, while dirs range from 1-3: 1: left, 2: streight, 3: right
                        int d = (k + 1);

                        //no of target lane:
                        int t = (i + d) % 4;

                        //check if that road exists - there may exist a junction with only three roads (DOAS 06)
                        if(incomingRoads[t] == null)
                            continue;
                        
                        //check if target lane is not disabled we are still on lane nr i.

                        //possible lanes:
                        Drivelane[] pl;
                        if (incomingRoads[t].getAlphaNode() ==  kruispunt)
                        {
                            pl = incomingRoads[t].getBetaLanes();
                        }
                        else
                        {
                            pl = incomingRoads[t].getAlphaLanes();
                        }
                        // If there was another disabled lane and the original points were reset
                        // It will get disabled again.
                        if(il[j].getTarget(k) && disabledLanes.contains(pl[0]))
                           il[j].setTarget(k, false, false);

                    }
                }
            }
        }
        //Check if everything is still okay about the Infrastructure
        validator.validate();

        ShortestPathCalculator calc = new ShortestPathCalculator();
        calc.calcAllShortestPaths(this);

    }

    /** Checks if the lane leads to the accident area. (DOAS 06)
     *  Accident area means that at least one of the lanes reachable from the lane is disabled.
     */
    public boolean leadsToAccidentArea(Roaduser ru, Drivelane lane){
        try{
            Drivelane[] lanes = lane.getNodeLeadsTo().getLanesLeadingFrom(lane, ru.getType());

            for(int i = 0; i < lanes.length; i++){
                if(disabledLanes.contains(lanes[i])){
                    return true;
                }
            }
        }catch(InfraException e){
            e.printStackTrace();
        }

        return false;
    }


    /*============================================*/
    /* Basic GET and SET methods                  */
    /*============================================*/

    /** Returns the title. */
    public String getTitle()
    {
        return title;
    }

    /** Sets the title. */
    public void setTitle(String s)
    {
        title = s;
    }

    /** Returns the author. */
    public String getAuthor()
    {
        return author;
    }

    /** Sets the author. */
    public void setAuthor(String s)
    {
        author = s;
    }

    /** Returns the comments. */
    public String getComments()
    {
        return comments;
    }

    /** Sets the comments. */
    public void setComments(String s)
    {
        comments = s;
    }

    /** Returns all exit/entry nodes */
    public SpecialNode[] getSpecialNodes()
    {
        return specialNodes;
    }

    /** Sets all exit/entry nodes */
    public void setSpecialNodes(SpecialNode[] nodes)
    {
        specialNodes = nodes;
    }

    /** Returns the Junctions of this infrastructure. */
    public Junction[] getJunctions()
    {
        return junctions;
    }

    /** Sets all junctions. */
    public void setJunctions(Junction[] _junctions)
    {
        junctions = junctions;
    }

    /** Returns all nodes (including edge nodes) */
    public Node[] getAllNodes()
    {
        return allNodes;
    }

    /** Sets all nodes (including edge nodes) */
    public void setAllNodes(Node[] nodes)
    {
        allNodes = nodes;
    }

    /** Returns the size of this infrastructure in pixels */
    public Dimension getSize()
    {
        return size;
    }

    /** Sets the size of this infrastructure in pixels */
    public void setSize(Dimension s)
    {
        size = s;
    }

    /** Returns the number of nodes */
    public int getNumNodes()
    {
        return allNodes.length;
    }

    /** Returns the number of edgenodes */
    public int getNumSpecialNodes()
    {
        return specialNodes.length;
    }

    /** Returns the number of junctions */
    public int getNumJunctions()
    {
        return junctions.length;
    }

    /** Returns the number of inbound lanes 
     * @author EJUST*/
    public int getNumInboundLanes()
    {
        return allLanes.size();
    }
    
    public static Vector getDisabledLanes()
    {
        return disabledLanes;
    }

    /** Sets the current timeStep */
    public void setCurTimeStep(int c)
    {
        curTimeStep = c;
    }

    /** Returns the current timeStep */
    public int getCurTimeStep()
    {
        return curTimeStep;
    }

    /** Returns the total number of signs in the infrastructure */
    public int getTotalNumSigns()
    {
        //count signs
        int result = 0;
        int num_nodes = allNodes.length;
        for (int i = 0; i < num_nodes; i++)
        {
            result += allNodes[i].getNumSigns();
        }
        return result;
    }

    /**
     * Returns an array containing all lane colearning statistics of the infrastructure.
     * The index in the array corresponds to the Inbound Lane id.
     * @author EJUST
     */
    public DrivelaneColearnStatistics[][] getDrivelaneColearnStatistics()
    {
    	//allLanes: All the inbound lanes on all the Nodes in our Infrastructure 
    	DrivelaneColearnStatistics[][] stats = new DrivelaneColearnStatistics[allLanes.size()][];
        for (int i = 0; i < stats.length; i++)
        {
            stats[i] = allLanes.get(i).getColearnStatistics();
        }
        return stats;
    }
    
    /**
     * Returns an array containing all lane speed statistics of the infrastructure.
     * The index in the array corresponds to the Inbound Lane id.
     * @author EJUST
     */
    public DrivelaneSpeedStatistics[][] getDrivelaneSpeedStatistics()
    {
    	//allLanes: All the inbound lanes on all the Nodes in our Infrastructure 
    	DrivelaneSpeedStatistics[][] stats = new DrivelaneSpeedStatistics[allLanes.size()][];
        for (int i = 0; i < stats.length; i++)
        {
            stats[i] = allLanes.get(i).getSpeedStatistics();
        }
        return stats;
    }    
    
    /**
     * Returns an array containing all statistics of the infrastructure.
     * The index in the array corresponds to the Node id.
     */
    public NodeStatistics[][] getNodeStatistics()
    {
        NodeStatistics[][] stats = new NodeStatistics[allNodes.length][];
        for (int i = 0; i < stats.length; i++)
        {
            stats[i] = allNodes[i].getStatistics();
        }
        return stats;
    }

    /**
     * Returns an array containing all statistics of all EdgeNodes.
     * The index in the array corresponds to the EdgeNode id.
     */
    public NodeStatistics[][] getEdgeNodeStatistics()
    {
        NodeStatistics[][] stats = new NodeStatistics[specialNodes.length][];
        for (int i = 0; i < stats.length; i++)
        {
            stats[i] = specialNodes[i].getStatistics();
        }
        return stats;
    }


    
     /* EJUST comment: If there are 15 nodes: From 0 to 9: edge node Id's and From 10 to 14: junction Id's
     *  Then junctions[0] corresponds to nodes[10]; Where index 0 = 10 (first Junction_id) - 10 (edgeNodes.length)
     */
    /**
     * Returns an array containing all statistics of all Junctions.
     * The index in the array corresponds to (Junction_id - edgeNodes.length).
     * */
    public NodeStatistics[][] getJunctionStatistics()
    {
        NodeStatistics[][] stats = new NodeStatistics[junctions.length][];
        for (int i = 0; i < stats.length; i++)
        {
            stats[i] = junctions[i].getStatistics();
        }
        return stats;
    }

    /**
     *  Returns the current count of accidents. (DOAS 06)
     */
    public int getAccidentsCount(){
        return disabledLanes.size() / 2;    //there are two lanes disabled per accident
    }

    /**
     *  DOAS 06: Returns the total number of cars removed.
     * (used for statistics purposes)
     */
    public int getRemovedCarsCount()
    {
        return removedCars;
    }

    /**
     *  DOAS 06: Returns the total number of cars entered into the network
     * (used for statistics purposes)
     */

    public int getEnteredCarsCount()
    {
        return enteredCars;

    }
    
    /**
     *  EJUST: Returns the total number of road users generated into the network
     * (used for statistics purposes)
     */

    public int getGeneratedRoadusersCount()
    {
        return generatedRoadusers;

    }

    /**
     *  DOAS 06: increment number of cars removed from network
     * (used for statistics purposes)
     */

    public void removedCarsIncrement()
    {
        removedCars++;
    }

    /**
     *  DOAS 06: increment number of cars entered the network
     * (used for statistics purposes)
     */

    public void enteredCarsIncrement()
      {
          enteredCars++;
      }

    /**
     *  EJUST: increment number of road users generated in the network
     * (used for statistics purposes)
     */

    public void generatedRoadusersIncrement()
      {
    	generatedRoadusers++;
      }


    /** Calculates the total size of this infrastructure and adds a small border */
    // TODO needs updating to move turn coords
    private Dimension calcSize()
    {
        Rectangle rect = new Rectangle();
        Node node;
        Road[] roads;
        Point p;
        for (int i = 0; i < allNodes.length; i++)
        {
            node = allNodes[i];
            roads = node.getAlphaRoads();
            rect.add(node.getBounds());
            for (int j = 0; j < roads.length; j++)
            {
                rect.add(roads[j].getBounds());
            }
        }
        int dx = (int) ( -rect.width / 2 - rect.x);
        int dy = (int) ( -rect.height / 2 - rect.y);
        for (int i = 0; i < allNodes.length; i++)
        {
            p = allNodes[i].getCoord();
            p.x += dx;
            p.y += dy;
        }
        return new Dimension(rect.width + 100, rect.height + 100);
    }

    /** Gets the EdgeNodes in this Infrastructure. Before using this method
     * think twice if you don't actually need the getSpecialNodes() method.
     * The underscore in the function name was added to emphasize that you
     * probably need another method now.
     */
    public EdgeNode[] getEdgeNodes_()
    {
        Enumeration e = Arrayutils.getEnumeration(specialNodes);
        Vector result = new Vector();
        Node tmp;
        while (e.hasMoreElements())
        {
            if ((tmp = (Node) e.nextElement()) instanceof EdgeNode)
            {
                result.add(tmp);
            }
        }
        EdgeNode[] resultArray = new EdgeNode[result.size()];
        result.toArray(resultArray);
        return resultArray;
    }

    /** Gets the number of EdgeNodes in the infrastructure */
    public int getNumEdgeNodes_()
    {
        return getEdgeNodes_().length;
    }

    /** Get the accidents rate (DOAS 06)
     */

    public static int getAccidentsRate(){
        return accidentsRate;
    }

    /** Set the accidents rate (DOAS 06)
     */
    public static void setAccidentsRate(int rate){
        accidentsRate = rate;
    }


    /*============================================*/
    /* Selectable                                 */
    /*============================================*/


    public boolean hasChildren()
    {
        return getNumNodes() > 0;
    }

    public Enumeration getChildren()
    {
        return new ArrayEnumeration(getAllNodes());
    }


    /*============================================*/
    /* MODIFYING DATA                             */
    /*============================================*/

    /** Adds a node to the infrastructure */
    public void addNode(Node node)
    {
        node.setId(allNodes.length);
        allNodes = (Node[]) Arrayutils.addElement(allNodes, node);
        if (node instanceof SpecialNode)
        {
            specialNodes = (SpecialNode[]) Arrayutils.addElement(specialNodes, node);
        }
        if (node instanceof Junction)
        {
            junctions = (Junction[]) Arrayutils.addElement(junctions, node);
        }
    }

    /** Removes a node from the infrastructure */
    public void remNode(Node node) throws InfraException
    {
        allNodes = (Node[]) Arrayutils.remElement(allNodes, node);
        if (node instanceof SpecialNode)
        {
            specialNodes = (SpecialNode[]) Arrayutils.remElement(specialNodes, node);
        }
        if (node instanceof Junction)
        {
            junctions = (Junction[]) Arrayutils.remElement(junctions, node);
        }
    }

    /**
     * Resets the entire data structure to allow a new simulation to start
     * This will remove all Roadusers and set all Signs to their default
     * positions, as well as reset all timeStepMoved and timeStepAsked counters.
     * @see Node#reset()
     */
    public void reset()
    {
        CustomFactory.reset();
        for (int i = 0; i < allNodes.length; i++)
        {
            allNodes[i].reset();
        }

        //(DOAS 06)
        while(disabledLanes.size() > 0){    //Because of some race condition issues only this kind of timeStep seems to work
            try{
                enableLane(0);
            }catch(InfraException e){
                e.printStackTrace();
            }
        }
        this.removedCars = 0; //DOAS 06 (reset the number of cars removed from model)
        this.enteredCars = 0; //DOAS 06 (reset the number of cars entered into model)
        this.generatedRoadusers = 0; //EJUST (reset the number of road users generated into model)
        
        rnd = new Random(GLDSim.seriesSeed[GLDSim.seriesSeedIndex]); /*EJUST*/
    }

    /**
     *  Resets the shortest paths informations (DOAS 06)
     */
    public void resetShortestPaths(){
        for (int i = 0; i < allNodes.length; i++)
        {
            allNodes[i].zapShortestPaths();
        }
    }

    public void cachInboundLanes() throws InfraException
    {
        int num_nodes = allNodes.length;
        allLanes = new Vector(num_nodes * 3);
        Drivelane[] temp;
        int num_temp;

        for (int i = 0; i < num_nodes; i++)
        {
            temp = allNodes[i].getInboundLanes();
            num_temp = temp.length;
            for (int j = 0; j < num_temp; j++)
            {
                allLanes.add(temp[j]);
            }
        }
    }
    
    /*POMDPGLD*/
    //(RM 06)
    public void convertAllLanes(Model m) throws InfraException
    {
        int num_nodes = allNodes.length;
        
        for (int i = 0; i < num_nodes; i++)
        {
            Road[] alphaRoads = allNodes[i].getAlphaRoads();
            for (int j = 0; j < alphaRoads.length; j++)
            {
                alphaRoads[j].convertLanes(m);
            }

        }
        
        for (int i = 0; i < num_nodes; i++)
        {
            allNodes[i].updateLanes();
        }
        
        // the paths have to be recalculated
        ShortestPathCalculator calc = new ShortestPathCalculator();
        
        try
        {
            calc.calcAllShortestPaths(this);
        }
        catch (InfraException e)
        {
            e.printStackTrace();
        }

        allLanes = null;
        cachInboundLanes();

    }

    /*POMDPGLD*/
    public int getNumRoadsegments() {
        if(roadsegments == -1) {
            int rs = 0;
            for(int i = 0; i < allLanes.size(); i++)
            {
                rs += ((Drivelane)allLanes.get(i)).getCompleteLength();
            }
            roadsegments = rs;
        }
        return roadsegments;
    }
   

    public Vector getAllInboundLanes() throws InfraException
    {
        if (allLanes == null)
        {
            cachInboundLanes();
        }
        return (Vector) allLanes.clone();
    }
    
    /*============================================*/
    /* LOAD AND SAVE                              */
    /*============================================*/


    public void prepareSave() throws GLDException
    {
        cachInboundLanes();
        size = calcSize();
    }


    public void load(XMLElement myElement, XMLLoader loader) throws XMLTreeException, IOException, XMLInvalidInputException
    { 
    	// Load parameters
        title = myElement.getAttribute("title").getValue();
        author = myElement.getAttribute("author").getValue();
        comments = myElement.getAttribute("comments").getValue();
        size = new Dimension(myElement.getAttribute("width").getIntValue(), myElement.getAttribute("height").getIntValue());

        allLanes = (Vector) XMLArray.loadArray(this, loader);

        notYetDisabledLanes = (Vector) allLanes.clone();
        
        allNodes = (Node[]) XMLArray.loadArray(this, loader);
        
        specialNodes = new SpecialNode[myElement.getAttribute("num-specialnodes").getIntValue()];
        junctions = new Junction[allNodes.length - specialNodes.length];
        copySpecialNodes();
        copyJunctions();
		
        // Internal second stage load of child objects
        Dictionary mainDictionary;
        try
        {
            mainDictionary = getMainDictionary();
        }
        catch (InfraException e)
        {
            throw new XMLInvalidInputException("Problem with internal 2nd stage load of infra :" + e);
        }
        
        XMLUtils.loadSecondStage(allLanes.elements(), mainDictionary);
        XMLUtils.loadSecondStage(Arrayutils.getEnumeration(allNodes), mainDictionary);
    }

    public XMLElement saveSelf()
    {
        XMLElement result = new XMLElement("infrastructure");
        result.addAttribute(new XMLAttribute("title", title));
        result.addAttribute(new XMLAttribute("author", author));
        result.addAttribute(new XMLAttribute("comments", comments));
        result.addAttribute(new XMLAttribute("height", size.height));
        result.addAttribute(new XMLAttribute("width", size.width));
        result.addAttribute(new XMLAttribute("file-version", version));
        result.addAttribute(new XMLAttribute("num-specialnodes", specialNodes.length));
        laneDictionary = (Hashtable) (getLaneSignDictionary());
        return result;
    }


    public void saveChilds(XMLSaver saver) throws XMLTreeException, IOException, XMLCannotSaveException
    {
        XMLArray.saveArray(allLanes, this, saver, "lanes");
        XMLArray.saveArray(allNodes, this, saver, "nodes");
    }

    public String getXMLName()
    {
        return parentName + ".infrastructure";
    }

    public void setParentName(String parentName)
    {
        this.parentName = parentName;
    }


    public Dictionary getMainDictionary() throws InfraException
    {
        Dictionary result = new Hashtable();
        result.put("lane", getLaneSignDictionary());
        result.put("node", getNodeDictionary());
        result.put("road", getRoadDictionary());
        return result;
    }

    protected Dictionary getLaneSignDictionary()
    {
        Dictionary result = new Hashtable();
        Enumeration lanes = allLanes.elements();
        Drivelane tmp;
        while (lanes.hasMoreElements())
        {
            tmp = (Drivelane) (lanes.nextElement());
            result.put(new Integer(tmp.getId()), tmp);
        }
        return result;
    }

    protected Dictionary getNodeDictionary()
    {
        Dictionary result = new Hashtable();
        Enumeration nodes = new ArrayEnumeration(allNodes);
        Node tmp;
        while (nodes.hasMoreElements())
        {
            tmp = (Node) (nodes.nextElement());
            result.put(new Integer(tmp.getId()), tmp);
        }
        return result;
    }

    protected Dictionary getRoadDictionary() throws InfraException
    {
        Dictionary result = new Hashtable();
        Enumeration nodes = new ArrayEnumeration(allNodes), roads;
        Node tmp;
        Road road;
        while (nodes.hasMoreElements())
        {
            tmp = (Node) (nodes.nextElement());
            if (tmp instanceof SpecialNode)
            {
                addAlphaRoads(result, (SpecialNode) (tmp));
            }
            else if (tmp instanceof Junction)
            {
                addAlphaRoads(result, (Junction) (tmp));
            }
            else
            {
                throw new InfraException
                        ("Unknown type of node : " + tmp.getName());
            }
        }
        return result;
    }

    protected void copySpecialNodes()
    {
        int specialNodePos = 0;
        for (int t = 0; t < allNodes.length; t++)
        {
            if (allNodes[t] instanceof SpecialNode)
            {
                specialNodes[specialNodePos++] = (SpecialNode) (allNodes[t]);
            }
        }
    }

    protected void copyJunctions()
    {
        int junctionPos = 0;
        for (int t = 0; t < allNodes.length; t++)
        {
            if (allNodes[t] instanceof Junction)
            {
                junctions[junctionPos++] = (Junction) (allNodes[t]);
            }
        }
    }

    protected void addAlphaRoads(Dictionary d, SpecialNode n)
    {
        if (n.getAlpha())
        {
            d.put(new Integer(n.getRoad().getId()), n.getRoad());
        }
    }

    protected void addAlphaRoads(Dictionary d, Junction n)
    {
        Enumeration roads = new ArrayEnumeration(n.getAlphaRoads());
        while (roads.hasMoreElements())
        {
            Road road = (Road) (roads.nextElement());
            d.put(new Integer(road.getId()), road);
        }
    }
}
