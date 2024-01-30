package gld.idm;

/**EJUST
 * IDMBicycleSandstorm customized to work with GLD*/
public class IDMBicycleSandstorm extends IDMGLD implements MicroModelGLD, Constants{


    public IDMBicycleSandstorm(){
	//    System.out.println("in Cstr of class IDMBicycle Sandstorm (no own ve calc)");
    	
    	//Bicycles are characterized by low values of v0, a, and b
        v0=(1-REDUCTION_FACTOR_SANDSTORM)*V0_INIT_BICYCLE_KMH/3.6;
        delta=4.0;
        
        a=(1-REDUCTION_FACTOR_SANDSTORM)*A_INIT_BICYCLE_MSII;
        b=(1-REDUCTION_FACTOR_SANDSTORM)*B_INIT_BICYCLE_MSII; 
        s0=(1+REDUCTION_FACTOR_SANDSTORM)*S0_INIT_BICYCLE_M;
        s1=(1+REDUCTION_FACTOR_SANDSTORM)*S1_INIT_BICYCLE_M;
        T=(1+REDUCTION_FACTOR_SANDSTORM)*T_INIT_BICYCLE_S; //careful drivers drive at a high safety time headway T,
        
        sqrtab=Math.sqrt(a*b);
        
        initialize();
    }
}
