package gld.idm;

/**EJUST
 * IDMBicycleNormalRain customized to work with GLD*/
public class IDMBicycleNormalRain extends IDMGLD implements MicroModelGLD, Constants{


    public IDMBicycleNormalRain(){
	//    System.out.println("in Cstr of class IDMBicycle Normal Rain (no own ve calc)");
    	
    	//Bicycles are characterized by low values of v0, a, and b
        v0=(1-REDUCTION_FACTOR_NORMAL_RAIN)*V0_INIT_BICYCLE_KMH/3.6;
        delta=4.0;
        a=(1-REDUCTION_FACTOR_NORMAL_RAIN)*A_INIT_BICYCLE_MSII;
        b=(1-REDUCTION_FACTOR_NORMAL_RAIN)*B_INIT_BICYCLE_MSII; 
        s0=(1+REDUCTION_FACTOR_NORMAL_RAIN)*S0_INIT_BICYCLE_M;
        s1=(1+REDUCTION_FACTOR_NORMAL_RAIN)*S1_INIT_BICYCLE_M;
        T=(1+REDUCTION_FACTOR_NORMAL_RAIN)*T_INIT_BICYCLE_S; //careful drivers drive at a high safety time headway T,
        sqrtab=Math.sqrt(a*b);
        
        initialize();
    }
}
