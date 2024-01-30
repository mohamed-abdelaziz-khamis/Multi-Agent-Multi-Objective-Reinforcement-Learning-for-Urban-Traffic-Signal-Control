package gld.idm;

/**EJUST
 *IDMBusLightFog customized to work with GLD*/
public class IDMBusLightFog extends IDMGLD implements MicroModelGLD, Constants{

    public IDMBusLightFog(){
	//    System.out.println("in Cstr of class IDMTruck Light Fog (no own ve calc)");

    	/*EJUST: trucks are characterized by low values of v0, a, and b*/
        v0=(1-REDUCTION_FACTOR_LIGHT_FOG)*V0_INIT_TRUCK_KMH/3.6; 
        delta=4.0;
        a=(1-REDUCTION_FACTOR_LIGHT_FOG)*A_INIT_TRUCK_MSII;
        b=(1-REDUCTION_FACTOR_LIGHT_FOG)*B_INIT_TRUCK_MSII;
        s0=(1+REDUCTION_FACTOR_LIGHT_FOG)*S0_INIT_TRUCK_M;
        s1=(1+REDUCTION_FACTOR_LIGHT_FOG)*S1_INIT_TRUCK_M;
        T=(1+REDUCTION_FACTOR_LIGHT_FOG)*T_INIT_TRUCK_S; /*EJUST: careful drivers drive at a high safety time headway T*/
        sqrtab=Math.sqrt(a*b);
        
        initialize();
    }
}

