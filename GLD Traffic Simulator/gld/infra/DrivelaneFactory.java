package gld.infra;

import gld.Model;
import java.util.Vector;

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
public class DrivelaneFactory
{

    protected Road road;
    protected Model model;

    public static final int
            DRIVELANE = 0,
            PO_DRIVELANE = 1;


    public static final String[] dlDescs =
            {
            "Fully Observable Drivelane",
            "Partially Observable Drivelane"
    };

    protected static final String[] xmlNames =
            {
            Drivelane.shortXMLName,
            PODrivelane.shortXMLName
    };


    public DrivelaneFactory(Model m, Road _road)
    {
        this.model = m;
        this.road = _road;
    }

    /** Gets a new instance of an algorithm by its number. This method
     * is meant to be used for loading.
     */
    public Drivelane getInstance() throws InfraException
    {
        switch (model.dltype)
        {
            case DRIVELANE:
                return new Drivelane(road);
            case PO_DRIVELANE:
                return new PODrivelane(road);
        }
        throw new InfraException
                ("The DrivelaneFactory can't make Drivelanes of type " +    model.dltype);
    }

    public Drivelane convertDrivelane(Drivelane oldlane) throws InfraException
    {
        switch (model.dltype)
        {
            case DRIVELANE:
                return new Drivelane(oldlane);
            case PO_DRIVELANE:
                return new PODrivelane(oldlane);
        }
        throw new InfraException
                ("The DrivelaneFactory can't make Drivelanes of type " + model.dltype);


    }

    public static int getType(Vector AllLanes) {
        try {
            Object lane = AllLanes.get(0);
            if(lane instanceof PODrivelane) return PO_DRIVELANE;
        } catch (Exception e)
        {
           e.printStackTrace();
        }

        return DRIVELANE;
    }
}
