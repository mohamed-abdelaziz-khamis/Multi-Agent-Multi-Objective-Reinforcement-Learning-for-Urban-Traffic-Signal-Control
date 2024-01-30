package gld.idm;

/**EJUST
 * IDMCarLightFog customized to work with GLD*/
public class IDMCarLightFog extends IDMGLD implements MicroModelGLD, Constants{


    public IDMCarLightFog(){
		//    System.out.println("in Cstr of class IDMCar Light Fog (no own ve calc)");
	
		v0=(1-REDUCTION_FACTOR_LIGHT_FOG)*V0_INIT_KMH/3.6;
		delta=4.0;
		
		a=(1-REDUCTION_FACTOR_LIGHT_FOG)*A_INIT_MSII;  //1
		b=(1-REDUCTION_FACTOR_LIGHT_FOG)*B_INIT_MSII;  //1.0
		s0=(1+REDUCTION_FACTOR_LIGHT_FOG)*S0_INIT_M;
		s1=(1+REDUCTION_FACTOR_LIGHT_FOG)*S1_INIT_M;
		T=(1+REDUCTION_FACTOR_LIGHT_FOG)*T_INIT_S;  //1.5
		
		sqrtab=Math.sqrt(a*b);
		
		initialize();
    }
}