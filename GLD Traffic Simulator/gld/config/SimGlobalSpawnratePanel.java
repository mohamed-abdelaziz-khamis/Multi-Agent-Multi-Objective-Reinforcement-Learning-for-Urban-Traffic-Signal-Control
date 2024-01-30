package gld.config;

import gld.DoubleJSlider;
import gld.distributions.DistributionFactory;
import gld.distributions.ParameterValue;
import gld.idm.Language;
import gld.idm.WeatherFactory;
import gld.infra.SpawnFrequencyTimeSteps;
import gld.sim.SimModel;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.ItemSelectable;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
public class SimGlobalSpawnratePanel extends ConfigPanel implements ActionListener, ItemListener /*EJUST*/, ChangeListener /*EJUST*/
{
	Button setSR;
	SimModel model;
	TextField input;
	ConfigDialog cd;

	//EJUST	
	List ruList;

	TextField timeStepInput;

	Button setSpawn;
	Button deleteSpawn;

	Vector ruTimeStepsLists;

	int deleteTimeStep = -1;

	//=====================
	Choice distributionTypes;
	double[][] 			parameterSpecs = DistributionFactory.getParameterSpecs();
	int[][] 			allParameters = DistributionFactory.getDistributionParameters();
	String[] 			distributionDescs = DistributionFactory.getDistributionTypeDescs();
	DoubleJSlider[] 	parameterSliders = {};
	Label[] 			parameterLabels = {};
	Label[] 			parameterValues = {};	
	ParameterValue[] 	paramValue = {}; /** The value of each parameter */
	int vpos;
	//EJUST

	/*EJUST: Labels of weather radio buttons*/
	private String str_radioButton_dry;
	private String str_radioButton_lightRain;
	private String str_radioButton_normalRain;
	private String str_radioButton_heavyRain;
	private String str_radioButton_lightFog;
	private String str_radioButton_heavyFog;
	private String str_radioButton_sandstorm;
	
	/*EJUST: Weather label and checkbox group*/
	private Label label_weatherConditions; //represents the weather condition
	private CheckboxGroup checkboxGroup_weather;
	
	/*EJUST: Weather radio buttons*/
	private Checkbox radioButton_dry;
	private Checkbox radioButton_lightRain;
	private Checkbox radioButton_normalRain;
	private Checkbox radioButton_heavyRain;
	private Checkbox radioButton_lightFog;
	private Checkbox radioButton_heavyFog;
	private Checkbox radioButton_sandstorm;
	
	/*EJUST: Weather conditions panel*/
	private Panel pWeatherConditions;
	
	private Language lang; 	/*EJUST*/
	
	public SimGlobalSpawnratePanel(SimModel m)
	{
		super(new ConfigDialog(m.getSimController()));

		this.cd = super.getConfigDialog();
		model = m;
		
		lang = Language.getInstance(); 	/*EJUST*/
		
		setLanguage(); 	/*EJUST*/

		Label explain = new Label("Set a global spawnrate for all edgenodes");
		explain.setBounds(0,20,300,20);
		add(explain);

		//EJUST
		ruList = new List();
		ruList.setBounds(0, 40, 300, 70); //To have size for Distribution(param1=v1, param2=v2,...)
		ruList.addItemListener(this);
		add(ruList);

		vpos = 140;

		Label lab = new Label("Set Dynamic spawnfrequency for all roadusers types");
		lab.setBounds(0, vpos + 10, 300, 20);
		add(lab);

		lab = new Label("on time step");
		lab.setBounds(110, vpos + 35, 90, 20);
		add(lab);

		timeStepInput = new TextField();
		timeStepInput.setBounds(200, vpos + 35, 60, 20);
		timeStepInput.addActionListener(this);
		add(timeStepInput);

		lab = new Label("is");
		lab.setBounds(265, vpos + 35, 20, 20);
		add(lab);

		//EJUST

		input = new TextField();
		input.setBounds(290,vpos + 35,40,20);
		if(model.globalSpawnrate != -1) {
			input.setText(new Float(model.globalSpawnrate).toString());
		}
		add(input);

		setSR = new Button("Set");
		setSR.setBounds(335,vpos + 35, 50,20);
		setSR.addActionListener(this);
		add(setSR);

		//EJUST
		explain = new Label("Or Interarrival times follow Distribution");
		explain.setBounds(0, vpos + 65, 250, 20);
		add(explain);

		distributionTypes = new Choice();
		distributionTypes.addItemListener(this);

		for (int i=0; i < distributionDescs.length; i++)
			distributionTypes.addItem(distributionDescs[i]);

		distributionTypes.setBounds(290, vpos + 65, 100, 20);
		add(distributionTypes);

		loadDistributionParameters();


		deleteSpawn = new Button("Delete timeStep 0 from all types ");
		deleteSpawn.setVisible(false);
		deleteSpawn.addActionListener(this);
		deleteSpawn.setBounds(0,vpos + 120, 250,25);
		add(deleteSpawn);

		reset();

		//EJUST

		/*EJUST: Weather label and checkbox group*/
		label_weatherConditions=new Label("Road Weather Conditions:"); //represents the weather condition
		checkboxGroup_weather=new CheckboxGroup();
		
		/*EJUST: Weather radio buttons*/
		radioButton_dry = new Checkbox(str_radioButton_dry, checkboxGroup_weather, true);
		radioButton_lightRain = new Checkbox(str_radioButton_lightRain, checkboxGroup_weather, false);
		radioButton_normalRain = new Checkbox(str_radioButton_normalRain, checkboxGroup_weather, false);
		radioButton_heavyRain = new Checkbox(str_radioButton_heavyRain, checkboxGroup_weather, false);
		radioButton_lightFog = new Checkbox(str_radioButton_lightFog, checkboxGroup_weather, false);
		radioButton_heavyFog = new Checkbox(str_radioButton_heavyFog, checkboxGroup_weather, false);
		radioButton_sandstorm = new Checkbox(str_radioButton_sandstorm, checkboxGroup_weather, false);
		
		/*EJUST: Weather conditions panel*/
		pWeatherConditions=new Panel();		
		pWeatherConditions.setLayout(new GridLayout(3, 3));
		pWeatherConditions.add(label_weatherConditions);
		pWeatherConditions.add(radioButton_dry);
		pWeatherConditions.add(radioButton_lightRain);
		pWeatherConditions.add(radioButton_normalRain);
		pWeatherConditions.add(radioButton_heavyRain);
		pWeatherConditions.add(radioButton_lightFog);
		pWeatherConditions.add(radioButton_heavyFog);
		pWeatherConditions.add(radioButton_sandstorm);
		pWeatherConditions.setBounds(0,vpos + 150, 450,60);
		add(pWeatherConditions);
		/*EJUST*/
		
		cd.setTitle("Global Spawnrate Setting");
	}

	//EJUST
	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(Color.black);
		//int vpos = 70 * ((int) Math.ceil((double)numItems/ 2)); /*Commented by EJUST*/
		g.drawLine(0, vpos, ConfigDialog.PANEL_WIDTH, vpos);
	}

	//EJUST
	public void setVisible(boolean b) {
		super.setVisible(b);
		cd.setConfigPanel(this);
		cd.setVisible(b);
	}


	/** EJUST: Returns the currently selected distribution type */
	public int getDistributionType() {
		int[] types = DistributionFactory.getDistributionTypes();
		return types[distributionTypes.getSelectedIndex()];
	}

	//EJUST
	public void reset() {

		ruTimeStepsLists = new Vector();
		boolean containsAnyItem = false;

			ruList.removeAll();
			Vector dSpawnList = model.getSimController().globalDSpawnTimeSteps();
			for (int j = 0; j < dSpawnList.size(); j++) {
				SpawnFrequencyTimeSteps sf = (SpawnFrequencyTimeSteps)dSpawnList.get(j);
				ruList.add(sf.toString());
				containsAnyItem = true;
			}
			ruTimeStepsLists = dSpawnList;
		
		if (containsAnyItem == false)
		{
			deleteSpawn.setVisible(false);
		}
	}


	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == setSR)
		{
			/*EJUST: weather condition*/
			int weatherCondition = 	radioButton_dry.getState()?WeatherFactory.DRY:
									radioButton_lightRain.getState()?WeatherFactory.LIGHT_RAIN:
									radioButton_normalRain.getState()?WeatherFactory.NORMAL_RAIN:
									radioButton_heavyRain.getState()?WeatherFactory.HEAVY_RAIN:
									radioButton_lightFog.getState()?WeatherFactory.LIGHT_FOG:
									radioButton_heavyFog.getState()?WeatherFactory.HEAVY_FOG:
									WeatherFactory.SANDSTORM;
			try {
				int timeStep = Integer.parseInt(timeStepInput.getText());	//EJUST			
				try {
					setSR.setEnabled(false);                
					float freq = -1; //EJUST
					if (!input.getText().trim().isEmpty()){ //EJUST
						freq = Float.parseFloat(input.getText());
						if (freq < 0) confd.showError("Spawn frequency must be greater than or equal zero."); //EJUST
						else model.getSimController().setGlobalSpawnrate(timeStep /*EJUST*/, freq, 
								getDistributionType() /*EJUST*/, paramValue  /*EJUST*/,
								weatherCondition /*EJUST*/);
					}
					else model.getSimController().setGlobalSpawnrate(timeStep /*EJUST*/, freq,
								getDistributionType() /*EJUST*/, paramValue  /*EJUST*/,
								weatherCondition /*EJUST*/); 
					setSR.setEnabled(true);
				}
				catch (NumberFormatException ex) { /*EJUST*/
					confd.showError("You must enter a float in the Spawn frequencies box.");
				}
			}
			catch(NumberFormatException ex) {
				confd.showError("You must enter an Integer in the TimeSteps box.");
			}
		}
		else if (e.getSource() == deleteSpawn) /*EJUST*/
		{
			SpawnFrequencyTimeSteps sf = (SpawnFrequencyTimeSteps)ruTimeStepsLists.get(deleteTimeStep);
			model.getSimController().deleteGlobalDSpawnTimeSteps(sf.timeStep);
		}
		reset(); /*EJUST*/
	}

	//EJUST
	public void itemStateChanged(ItemEvent e) {
		ItemSelectable es = e.getItemSelectable();

		if (es == distributionTypes) loadDistributionParameters();  
		
		else {
			if (es == ruList)
			{
				if(deleteTimeStep > -1) ruList.deselect(deleteTimeStep);
				
				deleteTimeStep = ruList.getSelectedIndex();
				if (deleteTimeStep > -1)
				{
					int timeStep = ((SpawnFrequencyTimeSteps) ruTimeStepsLists.get(deleteTimeStep)).timeStep;
					deleteSpawn.setLabel("Delete spawn at timeStep " + timeStep + " for all types ");
					deleteSpawn.setVisible(true);
				}
				else
					deleteSpawn.setVisible(false);
			}
		}
	}

	//EJUST
	private void loadDistributionParameters() {
		for (int i=0; i <parameterSliders.length; i++){
			remove(parameterSliders[i]);
			remove(parameterLabels[i]);
			remove(parameterValues[i]);
		}

		int distributionIndex = distributionTypes.getSelectedIndex();

		if (allParameters[distributionIndex][0] == -1){                		
			parameterSliders = new DoubleJSlider[0];
			parameterLabels = new Label[0];
			parameterValues = new Label[0];
		}
		else{
			parameterSliders = new DoubleJSlider[allParameters[distributionIndex].length];
			parameterLabels = new Label[allParameters[distributionIndex].length];
			parameterValues = new Label[allParameters[distributionIndex].length];
			paramValue = new ParameterValue[allParameters[distributionIndex].length];
			int parameterIndex;
			double min, max, value, minorTickSpacing;
			for (int i=0; i < parameterSliders.length; i++){
				parameterIndex = allParameters[distributionIndex][i];
				min = parameterSpecs[parameterIndex][0];
				max = parameterSpecs[parameterIndex][1];
				value = parameterSpecs[parameterIndex][2];
				minorTickSpacing = parameterSpecs[parameterIndex][3];
				parameterSliders[i] = new DoubleJSlider(min, max, value, minorTickSpacing);	                    
				parameterSliders[i].addChangeListener(this);	                                        				
				parameterSliders[i].setBounds(i*140, vpos + 85, 90, 40);	                    
				add(parameterSliders[i]);

				parameterLabels[i] = new Label(DistributionFactory.getParameterDescription(parameterIndex)+ " =");
				parameterLabels[i].setBounds(i*140+90, vpos + 85, 17, 40);
				add(parameterLabels[i]);

				paramValue[i] = new ParameterValue();
				paramValue[i].value = parameterSliders[i].getDoubleValue();
				paramValue[i].parameterIndex = parameterIndex; 
				parameterValues[i] = new Label(new DecimalFormat("0.0").format(paramValue[i].value));
				parameterValues[i].setBounds(i*140+107, vpos + 85, 33, 40);
				add(parameterValues[i]);
			}                		
		}
	}

	//EJUST
	public void stateChanged(ChangeEvent e) {
		DoubleJSlider source = (DoubleJSlider)e.getSource();
		if (!source.getValueIsAdjusting())
			for (int i = 0; i < parameterSliders.length; i++)
				if (source == parameterSliders[i]){
					paramValue[i].value = parameterSliders[i].getDoubleValue();
					paramValue[i].parameterIndex = allParameters[distributionTypes.getSelectedIndex()][i];
					parameterValues[i].setText(new DecimalFormat("0.0").format(paramValue[i].value));
				}
	}
	
	//EJUST
	private void setLanguage(){	
		str_radioButton_dry = lang.getDryName();
		str_radioButton_lightRain = lang.getLightRainName();
		str_radioButton_normalRain = lang.getNormalRainName();
		str_radioButton_heavyRain = lang.getHeavyRainName();
		str_radioButton_lightFog = lang.getLightFogName();
		str_radioButton_heavyFog = lang.getHeavyFogName();
		str_radioButton_sandstorm = lang.getSandstormName();
	}
}