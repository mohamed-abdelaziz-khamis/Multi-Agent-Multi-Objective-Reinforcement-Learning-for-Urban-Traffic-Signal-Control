package gld.idm;

import gld.infra.Roaduser;


/** Representation of a general micromodel for single-lane longitudinal dynamics.
Besides the IDM variants (classes IDM*), also other models such as the OVM (optimal-velocity model) could be developed.
*/

/*EJUST*/
public interface MicroModelGLD {
	public double Veq(double dx);
    public double calcAcc(Roaduser backRoaduser, Roaduser frontRoaduser);
}