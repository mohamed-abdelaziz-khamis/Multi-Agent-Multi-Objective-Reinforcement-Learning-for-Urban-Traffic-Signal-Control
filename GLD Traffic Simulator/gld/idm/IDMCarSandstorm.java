package gld.idm;

/**EJUST
 * IDMCarSandstorm customized to work with GLD*/
public class IDMCarSandstorm extends IDMGLD implements MicroModelGLD, Constants{


    public IDMCarSandstorm(){
		//    System.out.println("in Cstr of class IDMCar Sandstorm (no own ve calc)");
	
		v0=(1-REDUCTION_FACTOR_SANDSTORM)*V0_INIT_KMH/3.6;
		delta=4.0;
		
		a=(1-REDUCTION_FACTOR_SANDSTORM)*A_INIT_MSII;  //1
		b=(1-REDUCTION_FACTOR_SANDSTORM)*B_INIT_MSII;  //1.0
		s0=(1+REDUCTION_FACTOR_SANDSTORM)*S0_INIT_M;
		s1=(1+REDUCTION_FACTOR_SANDSTORM)*S1_INIT_M;
		T=(1+REDUCTION_FACTOR_SANDSTORM)*T_INIT_S;  //1.5
		
		sqrtab=Math.sqrt(a*b);
		
		initialize();
    }
}