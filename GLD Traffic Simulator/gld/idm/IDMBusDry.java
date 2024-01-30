package gld.idm;

/**EJUST
 * The same as IDMBus but customized to work with GLD*/
public class IDMBusDry extends IDMGLD implements MicroModelGLD, Constants{

    public IDMBusDry(){
	//    System.out.println("in Cstr of class IDMTruck (no own ve calc)");

        v0=V0_INIT_TRUCK_KMH/3.6; /*EJUST: trucks are characterized by low values of v0*/
        delta=4.0;
        a=A_INIT_TRUCK_MSII;
        b=B_INIT_TRUCK_MSII;
        s0=S0_INIT_TRUCK_M;
        s1=S1_INIT_TRUCK_M;
        T=T_INIT_TRUCK_S; /*EJUST: careful drivers drive at a high safety time headway T*/
        sqrtab=Math.sqrt(a*b);
        
        initialize();
    }
}

