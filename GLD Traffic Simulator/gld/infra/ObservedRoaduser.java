package gld.infra;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
 
 /*POMDPGLD*/
public class ObservedRoaduser
{
    private Roaduser ru;
    private double speed; /*EJUST: int --> double*/
    private double timesSeen = 1.0;
    private double pos;	/*EJUST: int --> double*/
    private boolean isDetected = true;

    public ObservedRoaduser(double speed, double pos, Roaduser ru) /*EJUST: int --> double*/
    {
        this.ru = ru;
        this.speed = speed;
        this.pos = pos;
    }


    public ObservedRoaduser(double speed, double pos, double seen, boolean isDetected) /*EJUST: int --> double*/
    {
        this.speed = speed;
        this.pos = pos;
        this.timesSeen = seen;
        this.isDetected = isDetected;
    }

    public ObservedRoaduser(ObservedRoaduser or) {
        this.speed = or.getSpeed();
        this.pos = or.getPos();
        this.timesSeen = or.getTimesSeen();
        this.ru = or.getRoaduser();
    }

    public double getSpeed() { /*EJUST: int --> double*/
        return speed;
    }

    public double getPos() { /*EJUST: int --> double*/
        return pos;
    }

    public void setDectection(boolean isDectected) {
        this.isDetected = isDectected;
    }

    public boolean getDectection() {
        return isDetected;
    }


    public Roaduser getRoaduser() {
        return ru;
    }


    public void setPos(double pos) { /*EJUST: int --> double*/
        this.pos = pos;
    }

    public double getTimesSeen() {
        return timesSeen;
    }

    public void setTimesSeen(double seen) {
        timesSeen = seen;
    }


    /*EJUST*/
	public double getStopDistance() { /*EJUST: int --> double*/
		return ru.getStopDistance();
	}	
}
