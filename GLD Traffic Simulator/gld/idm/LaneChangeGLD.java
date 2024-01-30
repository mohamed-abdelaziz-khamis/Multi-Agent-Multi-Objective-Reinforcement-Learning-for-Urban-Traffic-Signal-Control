package gld.idm;

import gld.infra.Roaduser;
import gld.infra.RoaduserFactory;

// MicroApplet2_0

/**
	Implementation of the lane-changing model
	MOBIL ("Minimizing Overall Brakings Induced by Lane-changes"), 
	see <a href="http://141.30.51.183/~treiber/publications/MOBIL.pdf">
	M. Treiber and D. Helbing, Realistische Mikrosimulation von Strassenverkehr mit einem einfachen Modell </a>,
	16. Symposium "Simulationstechnik ASIM 2002" Rostock, 10.09 -13.09.2002, 
	edited by Djamshid Tavangarian and Rolf Gr\"utzner pp. 514--520.
*/

/*EJUST*/
public class LaneChangeGLD implements Constants{

    // !! p and db and bsave overwritten by set_p, set_db!!

    private double p=P_FACTOR_CAR; 				// politeness factor
    
    private double db=DB_CAR;      				// changing threshold
    /* Must be below the lowest acceleration ability (IDM parameter a) of any vehicle type
     * A_INIT_CAR_MSII = 0.5
     * A_INIT_TRUCK_MSII = 0.4
     * A_INIT_BICYCLE_MSII = 0.3 */
    
    private double gapMin=MAIN_SMIN;        	// minimum safe (net) distance
    
    private double bsave=MAIN_BSAVE;     		// maximum safe braking deceleration for other vehicles
    /*Must be lower than maximum deceleration of about 9 m/s2 */
    
    private double bsaveSelf=MAIN_BSAVE_SELF;   // maximum safe braking deceleration for subject vehicle

    // asymmetric European parts

    //private double biasRight = BIAS_RIGHT_CAR; //bias (m/s^2) to drive right ln
    private double biasRight = 0; // overwritten

    public LaneChangeGLD (int type){
        if(type == RoaduserFactory.BUS){
           System.out.println("in Cstr LaneChange(''Bus''): " +"db=" +db+" bsave="+bsave);
           p=P_FACTOR_TRUCK;  //!! overwritten in sliders
           db=DB_TRUCK;  //!! overwritten in sliders
           gapMin=MAIN_SMIN_TRUCK;
           bsave=MAIN_BSAVE_TRUCK;
           biasRight=BIAS_RIGHT_TRUCK;
        }
        else if(type == RoaduserFactory.BICYCLE){
            System.out.println("in Cstr LaneChange(''Bicycle''): " +"db=" +db+" bsave="+bsave);
            p=P_FACTOR_BICYCLE;  //!! overwritten in sliders
            db=DB_BICYCLE;  //!! overwritten in sliders
            gapMin=MAIN_SMIN_BICYCLE;
            bsave=MAIN_BSAVE_BICYCLE;
            biasRight=BIAS_RIGHT_BICYCLE;
         }
        else{         // default
          System.out.println("in Cstr of LaneChange(''Car''): " +"db=" +db+" bsave="+bsave);
        }
    }

    public LaneChangeGLD (double p, double db, double gapMin, double bsave, double biasRight){
        this(p,db);
        assert bsave <= MAX_BRAKING;
        this.gapMin=gapMin;
        this.bsave=bsave;
        this.biasRight=biasRight;
        //System.out.println("Cstr of LaneChange(5 args): db="+db+" bsave="+bsave);
    }

    public LaneChangeGLD (double p, double db){
        assert bsaveSelf <= MAX_BRAKING;
        this.p=p;
        this.db=db;
        //System.out.println("Cstr of LaneChange(5 args): db="+db+" bsave="+bsave);
    }

    public void set_p(double p){
    	this.p=p;
    	//System.out.println("LaneChange.set_p: p="+p);
    }
    
    public void set_db(double db){
    	this.db=db;
    	//System.out.println("LaneChange.set_db: db="+db);
    }
    
    public double get_p(){
    	return(p);
    }
    
    public double get_db(){
    	return(db);
    }
    
    public void set_gapMin(double gapMin){
    	this.gapMin=gapMin;
    	//System.out.println("LaneChange.set_gapMin: gapMin="+gapMin);
    }

    public void set_bsave(double bsave){
        assert bsave <= MAX_BRAKING;
        this.bsave=bsave;
        //System.out.println("LaneChange.set_bsave: bsave="+bsave);
    }

    public void set_biasRight(double bias){
    	this.biasRight=bias;
    	//System.out.println("LaneChange.set_biasRight: biasRight="+biasRight);
    }

    public boolean changeOK(boolean left, Roaduser me, Roaduser frontRoaduserOld, Roaduser frontRoaduserNew, Roaduser backRoaduserNew) {

        // frontRoaduserOld: front roaduser on old lane; 
        // frontRoaduserNew, backRoaduserNew: front, back roadusers on new lane; 
        
    	double gapFront = me.getPosition();
    	double others_disadv = 0;
    	
    	if (frontRoaduserNew!=null)
    		gapFront -= frontRoaduserNew.getPosition() + frontRoaduserNew.getLength();
        
    	/*if (gapFront < 0)
        	System.out.println(" acc'(M'): " +
				"	me position on current lane: 	" + me.getPosition() +
				"	front roaduser position on new lane:	" + frontRoaduserNew!=null?frontRoaduserNew.getPosition():0);*/
        
        if (gapFront <= gapMin) {
        	//System.out.println("gapFront:	" + gapFront + " <= gapMin:	" + gapMin);	
            return false;
        }
        
        double my_acc = me.model().calcAcc(me, frontRoaduserNew);
        
        if (my_acc < -bsaveSelf) {
         	//System.out.println("my_acc:	" + my_acc + " < -bsaveSelf:	" + -bsaveSelf);
            return false;
        }
        
        if (backRoaduserNew!=null){
	        double gapBack = backRoaduserNew.getPosition() - me.getPosition() - me.getLength();
	        
	        /*if (gapBack < 0)
	        	System.out.println(" acc'(B'): " +
	        					"	back roaduser position on new lane: 	" + backRoaduserNew.getPosition() +
	        					"	me position on current lane:	" + me.getPosition()); */
	        
	         if (gapBack <= gapMin) {
	        	//System.out.println("gapBack:	" + gapBack + " <= gapMin:	" + gapMin);
	            return false;
	         }
	
	        // safety criterion (a > -bsave); 
	        double bNew_acc = backRoaduserNew.model().calcAcc(backRoaduserNew, me);
	        
	        if (bNew_acc < -bsave) {
	       	  	//System.out.println("bNew_acc:	" + bNew_acc + " < -bsave:	" + -bsave);
	            return false;
	        }
	        
	        others_disadv = backRoaduserNew.model().calcAcc(backRoaduserNew, frontRoaduserNew)/*acc(B')*/ - bNew_acc /*acc'(B')*/;
	        
	        /*if (backRoaduserNew.getPosition() - frontRoaduserNew.getPosition() - frontRoaduserNew.getLength()<0)
	        	System.out.println(" acc(B'): " +
	        					"	back roaduser on new: 	" + backRoaduserNew.getPosition() +
	        					"	front roaduser on new lane:	" + frontRoaduserNew.getPosition()); */
        }        

        // incentive criterion (always model of BACK vehicle used!!)
        // works also for on-ramp: on-ramp has 1 lane with index 0
        // === LEFT on main road -> strong desired to change = 
        // large positive biasRight for lcModel of ramp vehicles

        double my_adv  = my_acc /*acc'(M')*/- me.model().calcAcc(me, frontRoaduserOld) /*acc(M)*/ + ((left) ? 1 : -1) * biasRight;

        /*if (me.getPosition() - frontRoaduserOld.getPosition() - frontRoaduserOld.getLength()<0)
        	System.out.println(" acc(M): " +
        					"	me position on current lane: 	" + me.getPosition() +
        					"	front roaduser on old lane:	" + frontRoaduserOld.getPosition()); */

        if (others_disadv < 0) {
            others_disadv = 0;
        }
        
        return my_adv - p*others_disadv > db /*a_thr*/ ? true : false;
    }
}