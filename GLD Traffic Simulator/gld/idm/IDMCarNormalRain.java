package gld.idm;

/**EJUST
 * IDMCarNormalRain customized to work with GLD*/
public class IDMCarNormalRain extends IDMGLD implements MicroModelGLD, Constants{


    public IDMCarNormalRain(){
		//    System.out.println("in Cstr of class IDMCar NormalRain (no own ve calc)");
	
		v0=(1-REDUCTION_FACTOR_NORMAL_RAIN)*V0_INIT_KMH/3.6;
		delta=4.0;
		a=(1-REDUCTION_FACTOR_NORMAL_RAIN)*A_INIT_MSII;  //1
		b=(1-REDUCTION_FACTOR_NORMAL_RAIN)*B_INIT_MSII;  //1.0
		s0=(1+REDUCTION_FACTOR_NORMAL_RAIN)*S0_INIT_M;
		s1=(1+REDUCTION_FACTOR_NORMAL_RAIN)*S1_INIT_M;
		T=(1+REDUCTION_FACTOR_NORMAL_RAIN)*T_INIT_S;  //1.5
		
		sqrtab=Math.sqrt(a*b);
		
		initialize();
    }
}