package gld.idm;

/**EJUST
 * IDMBicycleLightFog customized to work with GLD*/
public class IDMBicycleLightFog extends IDMGLD implements MicroModelGLD, Constants{


    public IDMBicycleLightFog(){
	//    System.out.println("in Cstr of class IDMBicycle Light Fog (no own ve calc)");
    	
    	//Bicycles are characterized by low values of v0, a, and b
        v0=(1-REDUCTION_FACTOR_LIGHT_FOG)*V0_INIT_BICYCLE_KMH/3.6;
        delta=4.0;
        a=(1-REDUCTION_FACTOR_LIGHT_FOG)*A_INIT_BICYCLE_MSII;
        b=(1-REDUCTION_FACTOR_LIGHT_FOG)*B_INIT_BICYCLE_MSII; 
        s0=(1+REDUCTION_FACTOR_LIGHT_FOG)*S0_INIT_BICYCLE_M;
        s1=(1+REDUCTION_FACTOR_LIGHT_FOG)*S1_INIT_BICYCLE_M;
        T=(1+REDUCTION_FACTOR_LIGHT_FOG)*T_INIT_BICYCLE_S; //careful drivers drive at a high safety time headway T,
        sqrtab=Math.sqrt(a*b);
        
        initialize();
    }
}
