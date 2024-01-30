package gld.config;

import gld.infra.ObservedQueue;
import gld.infra.Beliefstate;
import gld.infra.Roaduser;
import gld.sim.SimModel;

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
public class POOptionsFactory
{
    public static final int[] noise_methods = {
            ObservedQueue.NOCHANGE,
            ObservedQueue.GAUSSIAN,
            ObservedQueue.UNIFORM,
    };

    public static final int[] beliefstate_types = {
            Beliefstate.ALLINFRONT,
            Beliefstate.MOSTLIKELYSTATE,
            Beliefstate.MOSTLIKELYQUEUE,
            Beliefstate.QMDP,
    };

    public static final int[] car_driving_behaviours = {
            Roaduser.NOCHANGE,
            Roaduser.GAUSSIAN,
            Roaduser.UNIFORM,
            Roaduser.SPETHIAL
    };

    public static final String [] subPOOptions_N = {"No-change", "Gaussian", "Uniform" };
    public static final String [] subPOOptions_B = {"All In Front", "Most Likely State", "Most Likely Queue","Q-MDP"};
    public static final String [] subPOOptions_C = {"No-change", "Gaussian", "Uniform", "Special" };


    public static final int[][]  cat_types = {noise_methods, beliefstate_types, car_driving_behaviours};
    public static final String [][] cats = {subPOOptions_N, subPOOptions_B, subPOOptions_C};

    public static final String [] cat_names = {"Noise Method", "Beliefstate type", "Car driving Behaviour"};

    public static final int defaultN = 1;
    public static final int defaultB = 0;
    public static final int defaultC = 1;


    public POOptionsFactory()
    {

    }

    public static void setOption(int cat, int nr) {
       switch(cat) {
           //noise Method
           case 0:
               SimModel.noise_method = noise_methods[nr];
               break;
           //Beliefstate type
           case 1:
               SimModel.beliefstate_method = beliefstate_types[nr];
               System.out.println(subPOOptions_B[SimModel.beliefstate_method]);
               break;
           //Car driving behaviour
           case 2:
               SimModel.car_driving_behaviour = car_driving_behaviours[nr];
               break;
       }
    }

}
