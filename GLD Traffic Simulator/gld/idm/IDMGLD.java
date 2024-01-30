package gld.idm;

import gld.infra.Roaduser;

/**
	Basis class for the microscopic traffic model IDM 
	(intelligent-driver model, see <a href="http://xxx.uni-augsburg.de/abs/cond-mat/0002177">
	M. Treiber, A. Hennecke, and D. Helbing, 
	Congested Traffic States in Empirical Observations and Microscopic Simulations, 
	Phys. Rev. E 62, 1805 (2000)].</a>
	<br><br>
	    The classes IDMCar, IDMTruck, etc are concrete realisations of this class for trucks, cars, etc. 
*/

/**EJUST
 * The same as IDM but customized to work with GLD*/
public abstract class IDMGLD implements MicroModelGLD{

	/*desired velocity when driving on a free road, v0*/
	public double v0; 
    
	/*acceleration exponent, delta.*/
	public double delta; 
	
	/*acceleration in everyday traffic, a*/
    public double a; 
    
    /*"comfortable" braking deceleration in everyday traffic, b*/
    public double b; 
    
    /*minimum bumper-to-bumper distance to the front vehicle, s0*/
    public double s0;
    
    public double s1;
    
    /*desired safety time headway when following other vehicles, T*/
    public double T; 
    
    public double sqrtab;
    
    private static final int ismax=100;          // ve(s)=ve(ismax) for s>ismax assumed
    private double[] veqTab = new double[ismax+1]; // table in steps of ds=1m

    public IDMGLD(){; }

    public void set_v0(double v0){this.v0=v0;}
    public void set_T(double T){this.T=T;}
    public void set_a(double a){this.a=a;}
    public void set_b(double b){this.b=b;}
    public void set_s0(double s0){this.s0=s0;}
    public void set_s1(double s1){this.s1=s1;}

    public void set_params(IDMGLD idm){
		this.set_v0(idm.v0);
		this.set_a(idm.a);
		this.set_b(idm.b);
		this.set_T(idm.T);
		this.set_s0(idm.s0);
		this.set_s1(idm.s1);
    }

    // calculate table of equilibrium velocity (s) with relaxation method;
    // veqTab[i] = equilibrium velocity for s=i*ds with ds=1 m, i=0..100

    public void initialize(){
        final double dt=0.5;             // relaxation with timestep=0.5 s
        final double kmax=20;            // number of iterations in rlaxation
	
        veqTab[0]=0.0;
        for (int is=1; is<=ismax ; is++){     // table in steps of ds=1 (m)

        	double Ve=veqTab[is-1];
            
        	// if(is>=ismax-3){System.out.println("is="+is+" Ve="+Ve);}

        	for (int k=0; k<kmax; k++){
        			double s_star = s0 + s1*Math.sqrt(Ve/v0)+Ve*T;	    
        			double acc= a * (1 - Math.pow((Ve/v0),delta) - (s_star*s_star)/(is*is) );
        			Ve=Ve+acc*dt;
        			if (Ve<0.0){Ve=0.0;}
        	}
	    
        	veqTab[is]=Ve;
        }
        
        System.out.println("IDM.initialize():" + "  veqTab[0]="+veqTab[0] + ", veqTab["+ismax+"]="+veqTab[ismax] );
    }

    // function for equilibrium velocity using above table; ve(s>ismax)=ve(ismax)
    public double Veq(double dx){
    	int is = (int) dx;
    	
    	double V=0.0;
    	
    	if (is < ismax){
    		double rest=dx-((double) is);
    		V = (1-rest)*veqTab[is] + rest*veqTab[is+1];
    	}
    	
    	if (is>=ismax){ V=veqTab[ismax];}
    	
    	if (is<=0){V=0.0;}
    		return V;
    }
    
    public double calcAcc(Roaduser backRoaduser, Roaduser frontRoaduser){
    	
    	double ru_speed = backRoaduser.getSpeed();
    	double delta_v, s = 0;
    	double s_star_raw, s_star = s0, acc;
    	
    	/* The acceleration is divided into a "desired" acceleration a [1-(v/v0)^delta] on a free road, 
    	 * and braking decelerations induced by the front vehicle. 
    	 * The acceleration on a free road decreases from the initial acceleration a to zero when approaching the "desired velocity" v0. */
    	acc = a * (1 - Math.pow((ru_speed/v0),delta));
    	
    	if (frontRoaduser!=null){
    		
    		delta_v = ru_speed-frontRoaduser.getSpeed();
    		
    		s = backRoaduser.getPosition()-frontRoaduser.getPosition()-frontRoaduser.getLength(); 
    		
    		s_star_raw = s0 + 
    					//s1*Math.sqrt(ru_speed/v0)+
    					ru_speed*T+
    					(ru_speed*delta_v)/(2*sqrtab);
    		
    		s_star = (s_star_raw > s0) ? s_star_raw : s0;
    		
    		acc -= a * (s_star*s_star)/(s*s);    		
    	}
       /* 
    	if (acc < -Constants.MAX_BRAKING) {
            acc = -Constants.MAX_BRAKING;
        }
    	
       	    	
    	double newVel = backRoaduser.getSpeed() + acc * 0.25;
    	if (newVel < 0){
    		newVel = 0;
    	}
    	double newPos = backRoaduser.getPosition() - newVel * 0.25;    	
    	
        if (frontRoaduser!=null){
        	double newFrontPos = frontRoaduser.getPosition() - frontRoaduser.getSpeed() * 0.25;
        	if((newFrontPos + frontRoaduser.getLength()) > newPos)
        		System.out.println("v0:	"+v0+"	back speed:	"+backRoaduser.getSpeed()+
					"	new back speed:	"+ newVel+
	        		"	back position:	"+backRoaduser.getPosition()+
					"	new back position:	"+ newPos+
					"	front length:	"+frontRoaduser.getLength()+
	        		"	front speed:	"+frontRoaduser.getSpeed()+
	        		"	front position:	"+frontRoaduser.getPosition()+
	        		"	new front position:	"+newFrontPos+				
					"	s:	"+s+"	s_star:	"+s_star+"	acc:	"+acc);
        }
        else
           {
            	System.out.println("v0:	"+v0+"	back speed:	"+backRoaduser.getSpeed()+
    				"	new back speed:	"+ newVel+
    	        	"	back position:	"+backRoaduser.getPosition()+
    				"	new back position:	"+ newPos+
    				"	front length:	null"+
    				"	front speed:	null"+
    	        	"	front position:	null"+
    	        	"	new front position:	null"+				
    				"	s:	null"+"	s_star:	null"+"	acc:	"+acc);
            }*/
        return acc;	
    }
 }
