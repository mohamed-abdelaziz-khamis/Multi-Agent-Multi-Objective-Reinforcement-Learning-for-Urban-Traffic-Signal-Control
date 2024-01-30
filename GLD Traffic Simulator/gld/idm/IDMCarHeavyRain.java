package gld.idm;

/**EJUST
 * IDMCarHeavyRain customized to work with GLD*/
public class IDMCarHeavyRain extends IDMGLD implements MicroModelGLD, Constants{


    public IDMCarHeavyRain(){
		//    System.out.println("in Cstr of class IDMCar Heavy Rain (no own ve calc)");
	
		v0=(1-REDUCTION_FACTOR_HEAVY_RAIN)*V0_INIT_KMH/3.6;
		delta=4.0;
		a=(1-REDUCTION_FACTOR_HEAVY_RAIN)*A_INIT_MSII;  //1
		b=(1-REDUCTION_FACTOR_HEAVY_RAIN)*B_INIT_MSII;  //1.0
		s0=(1+REDUCTION_FACTOR_HEAVY_RAIN)*S0_INIT_M;
		s1=(1+REDUCTION_FACTOR_HEAVY_RAIN)*S1_INIT_M;
		T=(1+REDUCTION_FACTOR_HEAVY_RAIN)*T_INIT_S;  //1.5
		sqrtab=Math.sqrt(a*b);
		
		initialize();
    }
}