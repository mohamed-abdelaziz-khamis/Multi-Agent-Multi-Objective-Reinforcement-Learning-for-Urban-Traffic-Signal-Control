package gld.idm;

/**EJUST
 *IDMBusNormalRain customized to work with GLD*/
public class IDMBusNormalRain extends IDMGLD implements MicroModelGLD, Constants{

    public IDMBusNormalRain(){
	//    System.out.println("in Cstr of class IDMTruck Normal Rain (no own ve calc)");

    	/*EJUST: trucks are characterized by low values of v0, a, and b*/
        v0=(1-REDUCTION_FACTOR_NORMAL_RAIN)*V0_INIT_TRUCK_KMH/3.6; 
        delta=4.0;
        a=(1-REDUCTION_FACTOR_NORMAL_RAIN)*A_INIT_TRUCK_MSII;
        b=(1-REDUCTION_FACTOR_NORMAL_RAIN)*B_INIT_TRUCK_MSII;
        s0=(1+REDUCTION_FACTOR_NORMAL_RAIN)*S0_INIT_TRUCK_M;
        s1=(1+REDUCTION_FACTOR_NORMAL_RAIN)*S1_INIT_TRUCK_M;
        T=(1+REDUCTION_FACTOR_NORMAL_RAIN)*T_INIT_TRUCK_S; /*EJUST: careful drivers drive at a high safety time headway T*/
        
        sqrtab=Math.sqrt(a*b);
        
        initialize();
    }
}

