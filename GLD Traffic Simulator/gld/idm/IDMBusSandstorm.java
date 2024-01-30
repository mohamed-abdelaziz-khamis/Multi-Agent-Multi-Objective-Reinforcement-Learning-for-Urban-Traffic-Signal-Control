package gld.idm;

/**EJUST
 *IDMBusSandstorm customized to work with GLD*/
public class IDMBusSandstorm extends IDMGLD implements MicroModelGLD, Constants{

    public IDMBusSandstorm(){
	//    System.out.println("in Cstr of class IDMTruck Sandstorm (no own ve calc)");

    	/*EJUST: trucks are characterized by low values of v0, a, and b*/
        v0=(1-REDUCTION_FACTOR_SANDSTORM)*V0_INIT_TRUCK_KMH/3.6; 
        delta=4.0;
        
        a=(1-REDUCTION_FACTOR_SANDSTORM)*A_INIT_TRUCK_MSII;
        b=(1-REDUCTION_FACTOR_SANDSTORM)*B_INIT_TRUCK_MSII;
        s0=(1+REDUCTION_FACTOR_SANDSTORM)*S0_INIT_TRUCK_M;
        s1=(1+REDUCTION_FACTOR_SANDSTORM)*S1_INIT_TRUCK_M;
        T=(1+REDUCTION_FACTOR_SANDSTORM)*T_INIT_TRUCK_S; /*EJUST: careful drivers drive at a high safety time headway T*/
        
        sqrtab=Math.sqrt(a*b);
        
        initialize();
    }
}

