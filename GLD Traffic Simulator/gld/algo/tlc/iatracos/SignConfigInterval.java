/*
 * Created on 16/09/2004
 *
 */
package gld.algo.tlc.iatracos;

/**
 * @author mpastorino
 * SignConfigInterval stores the information about a sign configuration.  
 */
public class SignConfigInterval {

	private boolean active;
	private int id;
	private int actualTime;
	private int intervalTime;
	private int activationOrder;

	public SignConfigInterval(int id) {
		this.id = id;
	}

	public void addOffset(int offset) {
		this.actualTime += offset;
	}

	/**
	 * When a sign configuration is active. The actual time decreases until 0, 
	 * then the next sign configuration will be the active.
	 * @return true if the sign configuration is active.
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * @return the actual time of the sign configuration.
	 */
	public int getActualTime() {
		return this.actualTime;
	}

	/**
	 * @return the id of the sign configuration.
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @return the interval time of the sign configuration.
	 */
	public int getIntervalTime() {
		return this.intervalTime;
	}

	/**
	 * Sets the sign configuration to active
	 */
	public void setActive() {
		this.active = true;
	}

	/**
	 * Sets the sign configuration to not active
	 */
	public void setNotActive() {
		this.active = false;
	}

	/**
	 * @param i
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Initializes the sign configuration setting the actual time and the interval time to the given value.
	 * @param interval
	 */
	public void initialize(int interval) {
		this.intervalTime = this.actualTime = interval;
	}

	/**
	 * Updates the sign configuration setting the new interval time to the given value. 
	 * If the sign configration is not active then it is refreshed. 
	 * @param interval
	 */
	public void update(int interval) {
		this.intervalTime = interval;
		if (!this.isActive()) {
			this.refresh();
		}
	}

	/**
	 * Decreases the actual time one step.
	 */
	public void doStep() {
		this.actualTime--;
	}

	/**
	 * @return true if the sign configuration is over (the actual time is less or equals than 0).
	 */
	public boolean isOver() {
		return (this.actualTime <= 0);
	}

	/**
	 * Refreshes the actual time with the interval time.
	 */
	public void refresh() {
		this.actualTime = this.intervalTime;
	}
	/**
	 * @return the next configuration
	 */
	public int getActivationOrder() {
		return this.activationOrder;
	}

	/**
	 * Sets the next configuration
	 * @param i
	 */
	public void setActivationOrder(int activationOrder) {
		this.activationOrder = activationOrder;
	}

}
