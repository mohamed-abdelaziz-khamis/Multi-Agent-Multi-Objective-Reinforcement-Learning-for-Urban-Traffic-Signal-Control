package gld.idm;

/**EJUST
 * The same as IDMCar but customized to work with GLD*/
public class IDMCarDry extends IDMGLD implements MicroModelGLD, Constants{


    public IDMCarDry(){
		//    System.out.println("in Cstr of class IDMCar (no own ve calc)");
	
	    /* aggressive ("pushy") drivers are characterized by a low T 
	     * in connection with high values of v0, a, and b.*/	
		v0=V0_INIT_KMH/3.6;
		delta=4.0;
		a=A_INIT_MSII;  //1
		b=B_INIT_MSII;  //1.0
		s0=S0_INIT_M;
		s1=S1_INIT_M;
		T =T_INIT_S;  //1.5
		sqrtab=Math.sqrt(a*b);
		
		initialize();
    }
}
