package gld;

import java.text.DecimalFormat;

import javax.swing.JSlider;

/**
 * <b>Programm:</b> WaveJNI<br>
 * <b>Copyright:</b> 2002 Andreas Gohr, Frank Schubert, Milan Altenburg<br>
 * <b>License:</b> GPL2 or higher<br>
 * <br>
 * <b>Info:</b> This JSlider uses doubles for its values
 */
public class DoubleJSlider extends JSlider{

	private double minorTickSpacing;
	/**
	 * Constructor
	 */
	public DoubleJSlider(double min, double max, double val, double minorTickSpacing){
		super();		
		setDoubleMinorTickSpacing(minorTickSpacing); //EJUST
		setDoubleMinimum(min);
		setDoubleMaximum(max);
		setDoubleValue(val);		
	}

	/**
	 * returns Maximum in double precision
	 */
	public double getDoubleMaximum() {
		return getMaximum()* getDoubleMinorTickSpacing();
	}

	/**
	 * returns Minimum in double precision
	 */
	public double getDoubleMinimum() {
		return getMinimum()* getDoubleMinorTickSpacing();
	}

	/**
	 * returns Value in double precision
	 */
	public double getDoubleValue() {
		return getValue() * getDoubleMinorTickSpacing();
	}
	
	/**
	 * EJUST: returns MinorTickSpacing in double precision
	 */
	public double getDoubleMinorTickSpacing() {
		return minorTickSpacing;
	}
	
	/**
	 * sets Maximum in double precision
	 */
	public void setDoubleMaximum(double max) {
		setMaximum((int)(max/getDoubleMinorTickSpacing()));
	}

	/**
	 * sets Minimum in double precision
	 */
	public void setDoubleMinimum(double min) {
		setMinimum((int)(min/getDoubleMinorTickSpacing()));
	}

	/**
	 * sets Value in double precision
	 */
	public void setDoubleValue(double val) {
		setValue((int)(val/getDoubleMinorTickSpacing()));		
	}

	/**
	 * EJUST: sets MinorTickSpacing in double precision
	 */
	public void setDoubleMinorTickSpacing(double _minorTickSpacing) {
		minorTickSpacing = _minorTickSpacing;
	}
}

