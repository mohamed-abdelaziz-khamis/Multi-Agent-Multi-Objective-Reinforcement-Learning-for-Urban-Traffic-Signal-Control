package gld.idm;

/**EJUST
 * IDMCarHeavyFog customized to work with GLD*/
public class IDMCarHeavyFog extends IDMGLD implements MicroModelGLD, Constants{


    public IDMCarHeavyFog(){
		//    System.out.println("in Cstr of class IDMCar Heavy Fog (no own ve calc)");
	
		v0=(1-REDUCTION_FACTOR_HEAVY_FOG)*V0_INIT_KMH/3.6;
		delta=4.0;
		
		a=(1-REDUCTION_FACTOR_HEAVY_FOG)*A_INIT_MSII;  //1
		b=(1-REDUCTION_FACTOR_HEAVY_FOG)*B_INIT_MSII;  //1.0
		s0=(1+REDUCTION_FACTOR_HEAVY_FOG)*S0_INIT_M;
		s1=(1+REDUCTION_FACTOR_HEAVY_FOG)*S1_INIT_M;
		T=(1+REDUCTION_FACTOR_HEAVY_FOG)*T_INIT_S;  //1.5
		
		sqrtab=Math.sqrt(a*b);
		
		initialize();
    }
}