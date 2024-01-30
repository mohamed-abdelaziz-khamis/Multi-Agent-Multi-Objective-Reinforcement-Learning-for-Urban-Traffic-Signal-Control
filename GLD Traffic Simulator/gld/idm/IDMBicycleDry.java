package gld.idm;

/**EJUST
 * IDMBicycle customized to work with GLD*/
public class IDMBicycleDry extends IDMGLD implements MicroModelGLD, Constants{


    public IDMBicycleDry(){
	//    System.out.println("in Cstr of class IDMBicycle (no own ve calc)");

        v0=V0_INIT_BICYCLE_KMH/3.6;
        delta=4.0;
        a=A_INIT_BICYCLE_MSII;
        b=B_INIT_BICYCLE_MSII; //Bicycles are characterized by low values of v0, a, and b,
        s0=S0_INIT_BICYCLE_M;
        s1=S1_INIT_BICYCLE_M;
        T=T_INIT_BICYCLE_S; //careful drivers drive at a high safety time headway T,
        sqrtab=Math.sqrt(a*b);
        
        initialize();
    }
}
