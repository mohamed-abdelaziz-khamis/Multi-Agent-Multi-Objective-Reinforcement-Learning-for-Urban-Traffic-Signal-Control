package gld.idm;

/**EJUST
 * IDMCarLightRain customized to work with GLD*/
public class IDMCarLightRain extends IDMGLD implements MicroModelGLD, Constants{


    public IDMCarLightRain(){
		//    System.out.println("in Cstr of class IDMCar Light Rain (no own ve calc)");
	
		v0=(1-REDUCTION_FACTOR_LIGHT_RAIN)*V0_INIT_KMH/3.6;
		delta=4.0;
		a=(1-REDUCTION_FACTOR_LIGHT_RAIN)*A_INIT_MSII;  //1
		b=(1-REDUCTION_FACTOR_LIGHT_RAIN)*B_INIT_MSII;  //1.0
		s0=(1+REDUCTION_FACTOR_LIGHT_RAIN)*S0_INIT_M;
		s1=(1+REDUCTION_FACTOR_LIGHT_RAIN)*S1_INIT_M;
		T=(1+REDUCTION_FACTOR_LIGHT_RAIN)*T_INIT_S;  //1.5
		
		sqrtab=Math.sqrt(a*b);
		
		initialize();
    }
}