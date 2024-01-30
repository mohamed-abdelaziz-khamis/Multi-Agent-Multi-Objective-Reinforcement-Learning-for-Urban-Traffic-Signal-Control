/*-----------------------------------------------------------------------
 * Copyright (C) 2011 Mohamed A. Khamis, EJUST
 *
 * This program (class) is free software.
 * You may redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation (version 2 or later).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *------------------------------------------------------------------------*/

package gld.config;

import gld.idm.AdjustmentMethods;
import gld.idm.Constants;
import gld.idm.IDMBicycleDry;
import gld.idm.IDMBicycleHeavyFog;
import gld.idm.IDMBicycleHeavyRain;
import gld.idm.IDMBicycleLightFog;
import gld.idm.IDMBicycleLightRain;
import gld.idm.IDMBicycleNormalRain;
import gld.idm.IDMBicycleSandstorm;
import gld.idm.IDMBusDry;
import gld.idm.IDMBusHeavyFog;
import gld.idm.IDMBusHeavyRain;
import gld.idm.IDMBusLightFog;
import gld.idm.IDMBusLightRain;
import gld.idm.IDMBusNormalRain;
import gld.idm.IDMBusSandstorm;
import gld.idm.IDMCarDry;
import gld.idm.IDMCarHeavyFog;
import gld.idm.IDMCarHeavyRain;
import gld.idm.IDMCarLightFog;
import gld.idm.IDMCarLightRain;
import gld.idm.IDMCarNormalRain;
import gld.idm.IDMCarSandstorm;
import gld.idm.IDMGLD;
import gld.idm.Language;
import gld.sim.SimModel;

import java.awt.CardLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;

public class SimIDMPanel extends ConfigPanel implements Constants {

	private static final long serialVersionUID = 1L;

	SimModel model;
	ConfigDialog cd;

	// controlled variables with init. values

	private double p_factor=0.; // lanechanging: politeness factor
	private double deltaB=0.2; // lanechanging: changing threshold

	/*EJUST: Light Rain Weather Conditions*/
	private double IDMv0_LightRain = (1-REDUCTION_FACTOR_LIGHT_RAIN)*V0_INIT_KMH;
	private double IDMT_LightRain = (int)(100*(1+REDUCTION_FACTOR_LIGHT_RAIN)*T_INIT_S)/100.;
	private double IDMa_LightRain = (int)(100*(1-REDUCTION_FACTOR_LIGHT_RAIN)*A_INIT_MSII)/100.;
	private double IDMb_LightRain = (int)(100*(1-REDUCTION_FACTOR_LIGHT_RAIN)*B_INIT_MSII)/100.;
	private double IDMs0_LightRain = (int)(100*(1+REDUCTION_FACTOR_LIGHT_RAIN)*S0_INIT_M)/100.;
	private double IDMs1_LightRain = (int)(100*(1+REDUCTION_FACTOR_LIGHT_RAIN)*S1_INIT_M)/100.;
	
	/*EJUST: Normal Rain Weather Conditions*/
	private double IDMv0_NormalRain = (1-REDUCTION_FACTOR_NORMAL_RAIN)*V0_INIT_KMH;
	private double IDMT_NormalRain = (int)(100*(1+REDUCTION_FACTOR_NORMAL_RAIN)*T_INIT_S)/100.;
	private double IDMa_NormalRain = (int)(100*(1-REDUCTION_FACTOR_NORMAL_RAIN)*A_INIT_MSII)/100.;
	private double IDMb_NormalRain = (int)(100*(1-REDUCTION_FACTOR_NORMAL_RAIN)*B_INIT_MSII)/100.;
	private double IDMs0_NormalRain = (int)(100*(1+REDUCTION_FACTOR_NORMAL_RAIN)*S0_INIT_M)/100.;
	private double IDMs1_NormalRain = (int)(100*(1+REDUCTION_FACTOR_NORMAL_RAIN)*S1_INIT_M)/100.;
	
	/*EJUST: Heavy Rain Weather Conditions*/
	private double IDMv0_HeavyRain = (1-REDUCTION_FACTOR_HEAVY_RAIN)*V0_INIT_KMH;
	private double IDMT_HeavyRain = (int)(100*(1+REDUCTION_FACTOR_HEAVY_RAIN)*T_INIT_S)/100.;
	private double IDMa_HeavyRain = (int)(100*(1-REDUCTION_FACTOR_HEAVY_RAIN)*A_INIT_MSII)/100.;
	private double IDMb_HeavyRain = (int)(100*(1-REDUCTION_FACTOR_HEAVY_RAIN)*B_INIT_MSII)/100.;
	private double IDMs0_HeavyRain = (int)(100*(1+REDUCTION_FACTOR_HEAVY_RAIN)*S0_INIT_M)/100.;
	private double IDMs1_HeavyRain = (int)(100*(1+REDUCTION_FACTOR_HEAVY_RAIN)*S1_INIT_M)/100.;

	/*EJUST: Light Fog Weather Conditions*/
	private double IDMv0_LightFog = (1-REDUCTION_FACTOR_LIGHT_FOG)*V0_INIT_KMH;
	private double IDMT_LightFog = (int)(100*(1+REDUCTION_FACTOR_LIGHT_FOG)*T_INIT_S)/100.;
	private double IDMa_LightFog = (int)(100*(1-REDUCTION_FACTOR_LIGHT_FOG)*A_INIT_MSII)/100.;
	private double IDMb_LightFog = (int)(100*(1-REDUCTION_FACTOR_LIGHT_FOG)*B_INIT_MSII)/100.;
	private double IDMs0_LightFog = (int)(100*(1+REDUCTION_FACTOR_LIGHT_FOG)*S0_INIT_M)/100.;
	private double IDMs1_LightFog = (int)(100*(1+REDUCTION_FACTOR_LIGHT_FOG)*S1_INIT_M)/100.;
	
	/*EJUST: Heavy Fog Weather Conditions*/
	private double IDMv0_HeavyFog = (1-REDUCTION_FACTOR_HEAVY_FOG)*V0_INIT_KMH;
	private double IDMT_HeavyFog = (int)(100*(1+REDUCTION_FACTOR_HEAVY_FOG)*T_INIT_S)/100.;
	private double IDMa_HeavyFog = (int)(100*(1-REDUCTION_FACTOR_HEAVY_FOG)*A_INIT_MSII)/100.;
	private double IDMb_HeavyFog = (int)(100*(1-REDUCTION_FACTOR_HEAVY_FOG)*B_INIT_MSII)/100.;
	private double IDMs0_HeavyFog = (int)(100*(1+REDUCTION_FACTOR_HEAVY_FOG)*S0_INIT_M)/100.;
	private double IDMs1_HeavyFog = (int)(100*(1+REDUCTION_FACTOR_HEAVY_FOG)*S1_INIT_M)/100.;

	/*EJUST: Sandstorm Weather Conditions*/
	private double IDMv0_Sandstorm = (1-REDUCTION_FACTOR_SANDSTORM)*V0_INIT_KMH;
	private double IDMT_Sandstorm = (int)(100*(1+REDUCTION_FACTOR_SANDSTORM)*T_INIT_S)/100.;
	private double IDMa_Sandstorm = (int)(100*(1-REDUCTION_FACTOR_SANDSTORM)*A_INIT_MSII)/100.;
	private double IDMb_Sandstorm = (int)(100*(1-REDUCTION_FACTOR_SANDSTORM)*B_INIT_MSII)/100.;
	private double IDMs0_Sandstorm = (int)(100*(1+REDUCTION_FACTOR_SANDSTORM)*S0_INIT_M)/100.;
	private double IDMs1_Sandstorm = (int)(100*(1+REDUCTION_FACTOR_SANDSTORM)*S1_INIT_M)/100.;
	
	// martin jan 05
	private IDMGLD idmCar; 		//=new IDMCarGLD(); // default params in constructor call
	private IDMGLD idmBus; 		//=new IDMBusGLD(); // default params in constructor call
	private IDMGLD idmBicycle; //=new IDMBicycleGLD(); // default params in constructor call
			
	/*EJUST: Light Rain Weather Conditions*/
	private IDMGLD idmCarLightRain;
	private IDMGLD idmBusLightRain;
	private IDMGLD idmBicycleLightRain;
	
	/*EJUST: Normal Rain Weather Conditions*/
	private IDMGLD idmCarNormalRain;
	private IDMGLD idmBusNormalRain;
	private IDMGLD idmBicycleNormalRain;
	
	/*EJUST: Heavy Rain Weather Conditions*/
	private IDMGLD idmCarHeavyRain;
	private IDMGLD idmBusHeavyRain;
	private IDMGLD idmBicycleHeavyRain;
	
	/*EJUST: Light Fog Weather Conditions*/
	private IDMGLD idmCarLightFog;
	private IDMGLD idmBusLightFog;
	private IDMGLD idmBicycleLightFog;
	
	/*EJUST: Heavy Fog Weather Conditions*/
	private IDMGLD idmCarHeavyFog;
	private IDMGLD idmBusHeavyFog;
	private IDMGLD idmBicycleHeavyFog;

	/*EJUST: Sandstorm Weather Conditions*/
	private IDMGLD idmCarSandstorm;
	private IDMGLD idmBusSandstorm;
	private IDMGLD idmBicycleSandstorm;
	
	/*EJUST: label weather conditions*/
	private Label label_weatherConditions=new Label("Road Weather Conditions:"); //represents the weather condition
	
	/*EJUST: Labels of weather radio buttons*/
	private String str_radioButton_dry;
	private String str_radioButton_lightRain;
	private String str_radioButton_normalRain;
	private String str_radioButton_heavyRain;
	private String str_radioButton_lightFog;
	private String str_radioButton_heavyFog;
	private String str_radioButton_sandstorm;
	
	/*EJUST: Checkbox group of weather conditions*/
	CheckboxGroup checkboxGroup_weather=new CheckboxGroup();
	
	/*EJUST: weather radio buttons*/
	private Checkbox radioButton_dry;
	private Checkbox radioButton_lightRain;
	private Checkbox radioButton_normalRain;
	private Checkbox radioButton_heavyRain;
	private Checkbox radioButton_lightFog;
	private Checkbox radioButton_heavyFog;
	private Checkbox radioButton_sandstorm;
	
	// text parameters, labels
	private Label label_p_factor2; //noetig!! //politeness factor
	private Label label_deltaB2;
	
	private Label label_IDMv0 = new Label(String.valueOf(V0_INIT_KMH)+" km/h");
	private Label label_IDMT = new Label(String.valueOf(T_INIT_S)+" s");
	private Label label_IDMa = new Label(String.valueOf(A_INIT_MSII)+" m/s^2");
	private Label label_IDMb = new Label(String.valueOf(B_INIT_MSII)+" m/s^2");
	private Label label_IDMs0 = new Label(String.valueOf(S0_INIT_M)+" m");
	private Label label_IDMs1 = new Label(String.valueOf(S1_INIT_M)+" m");

	/*EJUST: Light Rain Scrollbars*/
	private Label label_IDMv0_LightRain = new Label(String.valueOf(IDMv0_LightRain)+" km/h");
	private Label label_IDMT_LightRain = new Label(String.valueOf(IDMT_LightRain)+" s");
	private Label label_IDMa_LightRain = new Label(String.valueOf(IDMa_LightRain)+" m/s^2");
	private Label label_IDMb_LightRain = new Label(String.valueOf(IDMb_LightRain)+" m/s^2");
	private Label label_IDMs0_LightRain = new Label(String.valueOf(IDMs0_LightRain)+" m");
	private Label label_IDMs1_LightRain = new Label(String.valueOf(IDMs1_LightRain)+" m");

	/*EJUST: Normal Rain Scrollbars*/
	private Label label_IDMv0_NormalRain = new Label(String.valueOf(IDMv0_NormalRain)+" km/h");
	private Label label_IDMT_NormalRain = new Label(String.valueOf(IDMT_NormalRain)+" s");
	private Label label_IDMa_NormalRain = new Label(String.valueOf(IDMa_NormalRain)+" m/s^2");
	private Label label_IDMb_NormalRain = new Label(String.valueOf(IDMb_NormalRain)+" m/s^2");
	private Label label_IDMs0_NormalRain = new Label(String.valueOf(IDMs0_NormalRain)+" m");
	private Label label_IDMs1_NormalRain = new Label(String.valueOf(IDMs1_NormalRain)+" m");

	/*EJUST: Heavy Rain Scrollbars*/
	private Label label_IDMv0_HeavyRain = new Label(String.valueOf(IDMv0_HeavyRain)+" km/h");
	private Label label_IDMT_HeavyRain = new Label(String.valueOf(IDMT_HeavyRain)+" s");
	private Label label_IDMa_HeavyRain = new Label(String.valueOf(IDMa_HeavyRain)+" m/s^2");
	private Label label_IDMb_HeavyRain = new Label(String.valueOf(IDMb_HeavyRain)+" m/s^2");
	private Label label_IDMs0_HeavyRain = new Label(String.valueOf(IDMs0_HeavyRain)+" m");
	private Label label_IDMs1_HeavyRain = new Label(String.valueOf(IDMs1_HeavyRain)+" m");

	/*EJUST: Light Fog Scrollbars*/
	private Label label_IDMv0_LightFog = new Label(String.valueOf(IDMv0_LightFog)+" km/h");
	private Label label_IDMT_LightFog = new Label(String.valueOf(IDMT_LightFog)+" s");
	private Label label_IDMa_LightFog = new Label(String.valueOf(IDMa_LightFog)+" m/s^2");
	private Label label_IDMb_LightFog = new Label(String.valueOf(IDMb_LightFog)+" m/s^2");
	private Label label_IDMs0_LightFog = new Label(String.valueOf(IDMs0_LightFog)+" m");
	private Label label_IDMs1_LightFog = new Label(String.valueOf(IDMs1_LightFog)+" m");

	/*EJUST: Heavy Fog Scrollbars*/
	private Label label_IDMv0_HeavyFog = new Label(String.valueOf(IDMv0_HeavyFog)+" km/h");
	private Label label_IDMT_HeavyFog = new Label(String.valueOf(IDMT_HeavyFog)+" s");
	private Label label_IDMa_HeavyFog = new Label(String.valueOf(IDMa_HeavyFog)+" m/s^2");
	private Label label_IDMb_HeavyFog = new Label(String.valueOf(IDMb_HeavyFog)+" m/s^2");
	private Label label_IDMs0_HeavyFog = new Label(String.valueOf(IDMs0_HeavyFog)+" m");
	private Label label_IDMs1_HeavyFog = new Label(String.valueOf(IDMs1_HeavyFog)+" m");
	
	/*EJUST: Sandstorm Scrollbars*/
	private Label label_IDMv0_Sandstorm = new Label(String.valueOf(IDMv0_Sandstorm)+" km/h");
	private Label label_IDMT_Sandstorm = new Label(String.valueOf(IDMT_Sandstorm)+" s");
	private Label label_IDMa_Sandstorm = new Label(String.valueOf(IDMa_Sandstorm)+" m/s^2");
	private Label label_IDMb_Sandstorm = new Label(String.valueOf(IDMb_Sandstorm)+" m/s^2");
	private Label label_IDMs0_Sandstorm = new Label(String.valueOf(IDMs0_Sandstorm)+" m");
	private Label label_IDMs1_Sandstorm = new Label(String.valueOf(IDMs1_Sandstorm)+" m");
	
	private String str_polite;
	private String str_db; //Bias to the right lane: delta b = Threshold athr

	private String str_IDMv0;
	private String str_IDMT;
	private String str_IDMa;
	private String str_IDMb;
	private String str_IDMs0;
	private String str_IDMs1;

	// interactive control (1:1 to corresponding labels)

	private Scrollbar sb_p_factor2;
	private Scrollbar sb_deltaB2; //Ramp scenario 2: Bias to the right lane: delta b = Threshold athr

	private Scrollbar sb_IDMv0 = AdjustmentMethods.getSB(V0_MIN_KMH,V0_MAX_KMH,V0_INIT_KMH, false);
	private Scrollbar sb_IDMT = AdjustmentMethods.getSB(T_MIN_S,T_MAX_S,T_INIT_S, false);
	private Scrollbar sb_IDMa = AdjustmentMethods.getSB(A_MIN_MSII,A_MAX_MSII,A_INIT_MSII, false);
	private Scrollbar sb_IDMb = AdjustmentMethods.getSB(B_MIN_MSII,B_MAX_MSII,B_INIT_MSII, false);
	private Scrollbar sb_IDMs0 = AdjustmentMethods.getSB(S0_MIN_M,S0_MAX_M,S0_INIT_M, false);
	private Scrollbar sb_IDMs1 = AdjustmentMethods.getSB(S1_MIN_M,S1_MAX_M,S1_INIT_M, false);

	/*EJUST: Light Rain Scrollbars*/
	private Scrollbar sb_IDMv0_LightRain = AdjustmentMethods.getSB(V0_MIN_KMH,V0_MAX_KMH,IDMv0_LightRain, false);
	private Scrollbar sb_IDMT_LightRain = AdjustmentMethods.getSB(T_MIN_S,T_MAX_S,IDMT_LightRain, false);
	private Scrollbar sb_IDMa_LightRain = AdjustmentMethods.getSB(A_MIN_MSII,A_MAX_MSII,IDMa_LightRain, false);
	private Scrollbar sb_IDMb_LightRain = AdjustmentMethods.getSB(B_MIN_MSII,B_MAX_MSII,IDMb_LightRain, false);
	private Scrollbar sb_IDMs0_LightRain = AdjustmentMethods.getSB(S0_MIN_M,S0_MAX_M,IDMs0_LightRain, false);
	private Scrollbar sb_IDMs1_LightRain = AdjustmentMethods.getSB(S1_MIN_M,S1_MAX_M,IDMs1_LightRain, false);

	/*EJUST: Normal Rain Scrollbars*/
	private Scrollbar sb_IDMv0_NormalRain = AdjustmentMethods.getSB(V0_MIN_KMH,V0_MAX_KMH,IDMv0_NormalRain, false);
	private Scrollbar sb_IDMT_NormalRain = AdjustmentMethods.getSB(T_MIN_S,T_MAX_S,IDMT_NormalRain, false);
	private Scrollbar sb_IDMa_NormalRain = AdjustmentMethods.getSB(A_MIN_MSII,A_MAX_MSII,IDMa_NormalRain, false);
	private Scrollbar sb_IDMb_NormalRain = AdjustmentMethods.getSB(B_MIN_MSII,B_MAX_MSII,IDMb_NormalRain, false);
	private Scrollbar sb_IDMs0_NormalRain = AdjustmentMethods.getSB(S0_MIN_M,S0_MAX_M,IDMs0_NormalRain, false);
	private Scrollbar sb_IDMs1_NormalRain = AdjustmentMethods.getSB(S1_MIN_M,S1_MAX_M,IDMs1_NormalRain, false);

	/*EJUST: Heavy Rain Scrollbars*/
	private Scrollbar sb_IDMv0_HeavyRain = AdjustmentMethods.getSB(V0_MIN_KMH,V0_MAX_KMH,IDMv0_HeavyRain, false);
	private Scrollbar sb_IDMT_HeavyRain = AdjustmentMethods.getSB(T_MIN_S,T_MAX_S,IDMT_HeavyRain, false);
	private Scrollbar sb_IDMa_HeavyRain = AdjustmentMethods.getSB(A_MIN_MSII,A_MAX_MSII,IDMa_HeavyRain, false);
	private Scrollbar sb_IDMb_HeavyRain = AdjustmentMethods.getSB(B_MIN_MSII,B_MAX_MSII,IDMb_HeavyRain, false);
	private Scrollbar sb_IDMs0_HeavyRain = AdjustmentMethods.getSB(S0_MIN_M,S0_MAX_M,IDMs0_HeavyRain, false);
	private Scrollbar sb_IDMs1_HeavyRain = AdjustmentMethods.getSB(S1_MIN_M,S1_MAX_M,IDMs1_HeavyRain, false);

	/*EJUST: Light Fog Scrollbars*/
	private Scrollbar sb_IDMv0_LightFog = AdjustmentMethods.getSB(V0_MIN_KMH,V0_MAX_KMH,IDMv0_LightFog, false);
	private Scrollbar sb_IDMT_LightFog = AdjustmentMethods.getSB(T_MIN_S,T_MAX_S,IDMT_LightFog, false);
	private Scrollbar sb_IDMa_LightFog = AdjustmentMethods.getSB(A_MIN_MSII,A_MAX_MSII,IDMa_LightFog, false);
	private Scrollbar sb_IDMb_LightFog = AdjustmentMethods.getSB(B_MIN_MSII,B_MAX_MSII,IDMb_LightFog, false);
	private Scrollbar sb_IDMs0_LightFog = AdjustmentMethods.getSB(S0_MIN_M,S0_MAX_M,IDMs0_LightFog, false);
	private Scrollbar sb_IDMs1_LightFog = AdjustmentMethods.getSB(S1_MIN_M,S1_MAX_M,IDMs1_LightFog, false);

	/*EJUST: Heavy Fog Scrollbars*/
	private Scrollbar sb_IDMv0_HeavyFog = AdjustmentMethods.getSB(V0_MIN_KMH,V0_MAX_KMH,IDMv0_HeavyFog, false);
	private Scrollbar sb_IDMT_HeavyFog = AdjustmentMethods.getSB(T_MIN_S,T_MAX_S,IDMT_HeavyFog, false);
	private Scrollbar sb_IDMa_HeavyFog = AdjustmentMethods.getSB(A_MIN_MSII,A_MAX_MSII,IDMa_HeavyFog, false);
	private Scrollbar sb_IDMb_HeavyFog = AdjustmentMethods.getSB(B_MIN_MSII,B_MAX_MSII,IDMb_HeavyFog, false);
	private Scrollbar sb_IDMs0_HeavyFog = AdjustmentMethods.getSB(S0_MIN_M,S0_MAX_M,IDMs0_HeavyFog, false);
	private Scrollbar sb_IDMs1_HeavyFog = AdjustmentMethods.getSB(S1_MIN_M,S1_MAX_M,IDMs1_HeavyFog, false);

	/*EJUST: Sandstorm Scrollbars*/
	private Scrollbar sb_IDMv0_Sandstorm = AdjustmentMethods.getSB(V0_MIN_KMH,V0_MAX_KMH,IDMv0_Sandstorm, false);
	private Scrollbar sb_IDMT_Sandstorm = AdjustmentMethods.getSB(T_MIN_S,T_MAX_S,IDMT_Sandstorm, false);
	private Scrollbar sb_IDMa_Sandstorm = AdjustmentMethods.getSB(A_MIN_MSII,A_MAX_MSII,IDMa_Sandstorm, false);
	private Scrollbar sb_IDMb_Sandstorm = AdjustmentMethods.getSB(B_MIN_MSII,B_MAX_MSII,IDMb_Sandstorm, false);
	private Scrollbar sb_IDMs0_Sandstorm = AdjustmentMethods.getSB(S0_MIN_M,S0_MAX_M,IDMs0_Sandstorm, false);
	private Scrollbar sb_IDMs1_Sandstorm = AdjustmentMethods.getSB(S1_MIN_M,S1_MAX_M,IDMs1_Sandstorm, false);

	// graphical components

	private int textHeight; // in pixels

	/*EJUST: Weather Radiobuttons panel*/
	private Panel pWeatherRadiobuttons=new Panel(); // panel weather conditions

	private Panel pRamp =new Panel(); // scrollbars Ramp scenario 2
	private Panel pScrollbars=new Panel(); // scrollbar field

	private Panel pIDMparams=new Panel(); // extra scrollbars for IDM params
	
	private Panel pIDMparamsLightRain=new Panel(); 	//EJUST: IDM params Light Rain panel
	private Panel pIDMparamsNormalRain=new Panel(); //EJUST: IDM params Normal Rain panel
	private Panel pIDMparamsHeavyRain=new Panel(); 	//EJUST: IDM params Heavy Rain panel
	private Panel pIDMparamsLightFog=new Panel(); 	//EJUST: IDM params Light Fog panel
	private Panel pIDMparamsHeavyFog=new Panel(); 	//EJUST: IDM params Heavy Fog panel
	private Panel pIDMparamsSandstorm=new Panel(); 	//EJUST: IDM params Sandstorm panel
		
	private Panel pIDMScrollbars=new Panel(); /*EJUST: IDM Scrollbars panel*/

	private CardLayout cardLayout = new CardLayout(0,10); // for pScrollbars
	
	private CardLayout IDMcardLayout = new CardLayout(0,10); // EJUST: for pIDMScrollbars

	public SimIDMPanel(SimModel m) {
		
		super(new ConfigDialog(m.getSimController()));
		this.cd = super.getConfigDialog();
		model = m;

		init();

		// !! sb_p_factorRamp, sb_deltaBRamp, label_*
		sb_p_factor2.setValue((int)(100*p_factor)); //scrollbar Ramp scenario 2: politeness factor
		label_p_factor2.setText (String.valueOf(p_factor));
		
		sb_deltaB2.setValue((int)(100*deltaB));
		label_deltaB2.setText (String.valueOf(deltaB));
		
		makeGlobalLayout();
		
        cd.setTitle("Intelligent-Driver Model (IDM) Settings");
	}

	public void setVisible(boolean b) {
		super.setVisible(b);
		cd.setConfigPanel(this);
		cd.setVisible(b);
	}

	private Language lang;
	
	public void init(){

		lang = Language.getInstance();
		
		setLanguage();
		
		// init.
		idmCar = new IDMCarDry();
		idmBus = new IDMBusDry();
		idmBicycle = new IDMBicycleDry();

		/*EJUST: Light Rain weather conditions*/
		idmCarLightRain = new IDMCarLightRain();
		idmBusLightRain = new IDMBusLightRain();
		idmBicycleLightRain = new IDMBicycleLightRain();
		
		/*EJUST: Normal Rain weather conditions*/
		idmCarNormalRain = new IDMCarNormalRain();
		idmBusNormalRain = new IDMBusNormalRain();
		idmBicycleNormalRain = new IDMBicycleNormalRain();
		
		/*EJUST: Heavy Rain weather conditions*/
		idmCarHeavyRain = new IDMCarHeavyRain();
		idmBusHeavyRain = new IDMBusHeavyRain();
		idmBicycleHeavyRain = new IDMBicycleHeavyRain();
		
		/*EJUST: Light Fog weather conditions*/
		idmCarLightFog = new IDMCarLightFog();
		idmBusLightFog = new IDMBusLightFog();
		idmBicycleLightFog = new IDMBicycleLightFog();
		
		/*EJUST: Heavy Fog weather conditions*/
		idmCarHeavyFog = new IDMCarHeavyFog();
		idmBusHeavyFog = new IDMBusHeavyFog();
		idmBicycleHeavyFog = new IDMBicycleHeavyFog();
		
		/*EJUST: Sandstorm weather conditions*/
		idmCarSandstorm = new IDMCarSandstorm();
		idmBusSandstorm = new IDMBusSandstorm();
		idmBicycleSandstorm = new IDMBicycleSandstorm();
		//############################################
		//setFonts();
		//############################################

		/*EJUST*/
		pWeatherRadiobuttons.setLayout(new GridLayout(3, 3));

		pRamp.setLayout(new GridBagLayout());
		
		//martin jan05
		pIDMparams.setLayout(new GridBagLayout());
		
		/*EJUST: Weather Panels*/
		pIDMparamsLightRain.setLayout(new GridBagLayout());
		pIDMparamsNormalRain.setLayout(new GridBagLayout());
		pIDMparamsHeavyRain.setLayout(new GridBagLayout());
		pIDMparamsLightFog.setLayout(new GridBagLayout());
		pIDMparamsHeavyFog.setLayout(new GridBagLayout());
		pIDMparamsSandstorm.setLayout(new GridBagLayout());

		// CardLayout cardLayout = new CardLayout(10,10) above;
		// Only 1 component visible: select with
		// cardLayout.show(component,string);
		// string = same as in the cardLayout.add method

		pScrollbars.setLayout(cardLayout);
		//pScrollbars.setBackground(SIM_BG_COLOR); 			// as sim backgr
		//pScrollbars.setBackground(new Color(100,255,50)); // as sim backgr
		//pScrollbars.setBackground(Color.BUTTON_COLOR); 	// if outside

		pIDMScrollbars.setLayout(IDMcardLayout);
		// #######################################
		// Define scrollbars
		// #######################################
	
		int p_factor100 = (int) (p_factor*100); //politness factor
		int deltaB100 = (int) (100.0*deltaB);

		sb_p_factor2 = getSB(100*POLITENESS_MIN, 100*POLITENESS_MAX, p_factor100); //scrollbar Ramp scenario 2: politeness factor
		sb_deltaB2 = getSB(0, (int)(100*DELTAB_MAX), deltaB100);
		
		// #######################################
		// Make Layout for scrollbars for 1 panel OnRamp
		// #######################################

		GridBagConstraints gbconstr = new GridBagConstraints();

		// 1th column: Variable names

		gbconstr.insets = new Insets(SB_SPACEY,SB_SPACEX,SB_SPACEY,SB_SPACEX);
		// (N,W,S,E)
		gbconstr.gridx = 0;
		gbconstr.gridy = 0;
		gbconstr.gridwidth=1;
		gbconstr.gridheight=1;
		gbconstr.fill = GridBagConstraints.NONE;
		gbconstr.anchor = GridBagConstraints.EAST;
		gbconstr.weightx = 0.;
		gbconstr.weighty = 0.;

		pRamp.add(new Label(str_polite), gbconstr); //!! str_rmppolite

		gbconstr.gridx = 0;
		gbconstr.gridy = 1;

		pRamp.add(new Label(str_db), gbconstr); //!! str_rmpdb
		
		// 2th column: actual scrollbars

		gbconstr.gridx = 1;
		gbconstr.gridy = 0;
		gbconstr.weightx = 1.;
		gbconstr.fill = GridBagConstraints.HORIZONTAL;
		gbconstr.anchor = GridBagConstraints.CENTER;

		pRamp.add(sb_p_factor2,gbconstr); // !! sb_p_factorRamp

		gbconstr.gridx = 1;
		gbconstr.gridy = 1;

		pRamp.add(sb_deltaB2,gbconstr); //!! sb_deltaBRamp
		/*EJUST: IDM
		Threshold athr	 				0.2 m/s2	 Must be below the lowest acceleration ability (IDM parameter a) of any vehicle type
		Bias to the right lane Delta b	0.2 m/s2	 Only for European traffic rules
		EJUST: IDM*/
	
		// 3th column: Actual values + units

		gbconstr.gridx = 2;
		gbconstr.gridy = 0;
		gbconstr.weightx = 0.;
		gbconstr.fill = GridBagConstraints.NONE;
		gbconstr.anchor = GridBagConstraints.WEST;

		pRamp.add(label_p_factor2= new Label( //!! label_p_factorRamp 
				String.valueOf(p_factor)), gbconstr); //!! p_factorRamp

		gbconstr.gridx = 2;
		gbconstr.gridy = 1;

		pRamp.add(label_deltaB2 = new Label( //!! label_deltaBRamp
				String.valueOf(deltaB)+" m/s^2"), gbconstr); //!! deltaBRamp

		//###########################################
		// martin jan05

		AdjustmentMethods.addScrollbar(pIDMparams, 1, sb_IDMv0,str_IDMv0,label_IDMv0);
		AdjustmentMethods.addScrollbar(pIDMparams, 2, sb_IDMa,str_IDMa,label_IDMa);
		AdjustmentMethods.addScrollbar(pIDMparams, 3, sb_IDMb,str_IDMb,label_IDMb);
		AdjustmentMethods.addScrollbar(pIDMparams, 4, sb_IDMT,str_IDMT,label_IDMT);
		AdjustmentMethods.addScrollbar(pIDMparams, 5, sb_IDMs0,str_IDMs0,label_IDMs0);
		AdjustmentMethods.addScrollbar(pIDMparams, 6, sb_IDMs1,str_IDMs1,label_IDMs1);		
		
		/*EJUST: Light Rain Scrollbars*/
		AdjustmentMethods.addScrollbar(pIDMparamsLightRain, 1, sb_IDMv0_LightRain,str_IDMv0,label_IDMv0_LightRain);
		AdjustmentMethods.addScrollbar(pIDMparamsLightRain, 2, sb_IDMa_LightRain,str_IDMa,label_IDMa_LightRain);
		AdjustmentMethods.addScrollbar(pIDMparamsLightRain, 3, sb_IDMb_LightRain,str_IDMb,label_IDMb_LightRain);
		AdjustmentMethods.addScrollbar(pIDMparamsLightRain, 4, sb_IDMT_LightRain,str_IDMT,label_IDMT_LightRain);
		AdjustmentMethods.addScrollbar(pIDMparamsLightRain, 5, sb_IDMs0_LightRain,str_IDMs0,label_IDMs0_LightRain);
		AdjustmentMethods.addScrollbar(pIDMparamsLightRain, 6, sb_IDMs1_LightRain,str_IDMs1,label_IDMs1_LightRain);		
		
		/*EJUST: Normal Rain Scrollbars*/
		AdjustmentMethods.addScrollbar(pIDMparamsNormalRain, 1, sb_IDMv0_NormalRain,str_IDMv0,label_IDMv0_NormalRain);
		AdjustmentMethods.addScrollbar(pIDMparamsNormalRain, 2, sb_IDMa_NormalRain,str_IDMa,label_IDMa_NormalRain);
		AdjustmentMethods.addScrollbar(pIDMparamsNormalRain, 3, sb_IDMb_NormalRain,str_IDMb,label_IDMb_NormalRain);
		AdjustmentMethods.addScrollbar(pIDMparamsNormalRain, 4, sb_IDMT_NormalRain,str_IDMT,label_IDMT_NormalRain);
		AdjustmentMethods.addScrollbar(pIDMparamsNormalRain, 5, sb_IDMs0_NormalRain,str_IDMs0,label_IDMs0_NormalRain);
		AdjustmentMethods.addScrollbar(pIDMparamsNormalRain, 6, sb_IDMs1_NormalRain,str_IDMs1,label_IDMs1_NormalRain);
		
		/*EJUST: Heavy Rain Scrollbars*/
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyRain, 1, sb_IDMv0_HeavyRain,str_IDMv0,label_IDMv0_HeavyRain);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyRain, 2, sb_IDMa_HeavyRain,str_IDMa,label_IDMa_HeavyRain);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyRain, 3, sb_IDMb_HeavyRain,str_IDMb,label_IDMb_HeavyRain);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyRain, 4, sb_IDMT_HeavyRain,str_IDMT,label_IDMT_HeavyRain);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyRain, 5, sb_IDMs0_HeavyRain,str_IDMs0,label_IDMs0_HeavyRain);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyRain, 6, sb_IDMs1_HeavyRain,str_IDMs1,label_IDMs1_HeavyRain);
		
		/*EJUST: Light Fog Scrollbars*/
		AdjustmentMethods.addScrollbar(pIDMparamsLightFog, 1, sb_IDMv0_LightFog,str_IDMv0,label_IDMv0_LightFog);
		AdjustmentMethods.addScrollbar(pIDMparamsLightFog, 2, sb_IDMa_LightFog,str_IDMa,label_IDMa_LightFog);
		AdjustmentMethods.addScrollbar(pIDMparamsLightFog, 3, sb_IDMb_LightFog,str_IDMb,label_IDMb_LightFog);
		AdjustmentMethods.addScrollbar(pIDMparamsLightFog, 4, sb_IDMT_LightFog,str_IDMT,label_IDMT_LightFog);
		AdjustmentMethods.addScrollbar(pIDMparamsLightFog, 5, sb_IDMs0_LightFog,str_IDMs0,label_IDMs0_LightFog);
		AdjustmentMethods.addScrollbar(pIDMparamsLightFog, 6, sb_IDMs1_LightFog,str_IDMs1,label_IDMs1_LightFog);
		
		/*EJUST: Heavy Fog Scrollbars*/
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyFog, 1, sb_IDMv0_HeavyFog,str_IDMv0,label_IDMv0_HeavyFog);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyFog, 2, sb_IDMa_HeavyFog,str_IDMa,label_IDMa_HeavyFog);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyFog, 3, sb_IDMb_HeavyFog,str_IDMb,label_IDMb_HeavyFog);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyFog, 4, sb_IDMT_HeavyFog,str_IDMT,label_IDMT_HeavyFog);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyFog, 5, sb_IDMs0_HeavyFog,str_IDMs0,label_IDMs0_HeavyFog);
		AdjustmentMethods.addScrollbar(pIDMparamsHeavyFog, 6, sb_IDMs1_HeavyFog,str_IDMs1,label_IDMs1_HeavyFog);
		
		/*EJUST: Sandstorm Scrollbars*/
		AdjustmentMethods.addScrollbar(pIDMparamsSandstorm, 1, sb_IDMv0_Sandstorm,str_IDMv0,label_IDMv0_Sandstorm);
		AdjustmentMethods.addScrollbar(pIDMparamsSandstorm, 2, sb_IDMa_Sandstorm,str_IDMa,label_IDMa_Sandstorm);
		AdjustmentMethods.addScrollbar(pIDMparamsSandstorm, 3, sb_IDMb_Sandstorm,str_IDMb,label_IDMb_Sandstorm);
		AdjustmentMethods.addScrollbar(pIDMparamsSandstorm, 4, sb_IDMT_Sandstorm,str_IDMT,label_IDMT_Sandstorm);
		AdjustmentMethods.addScrollbar(pIDMparamsSandstorm, 5, sb_IDMs0_Sandstorm,str_IDMs0,label_IDMs0_Sandstorm);
		AdjustmentMethods.addScrollbar(pIDMparamsSandstorm, 6, sb_IDMs1_Sandstorm,str_IDMs1,label_IDMs1_Sandstorm);
		//###########################################
		
		pScrollbars.add("onRamp",pRamp);

		/*EJUST: Add the IDM params weather panels on pIDMScrollbars*/
		pIDMScrollbars.add(str_radioButton_dry,pIDMparams);
		pIDMScrollbars.add(str_radioButton_lightRain,pIDMparamsLightRain);
		pIDMScrollbars.add(str_radioButton_normalRain,pIDMparamsNormalRain);
		pIDMScrollbars.add(str_radioButton_heavyRain,pIDMparamsHeavyRain);
		pIDMScrollbars.add(str_radioButton_lightFog,pIDMparamsLightFog);
		pIDMScrollbars.add(str_radioButton_heavyFog,pIDMparamsHeavyFog);
		pIDMScrollbars.add(str_radioButton_sandstorm,pIDMparamsSandstorm);
		
		// #######################################
		// EJUST: Define radio buttons
		// #######################################
		
		radioButton_dry = new Checkbox(str_radioButton_dry, checkboxGroup_weather, true);
		radioButton_lightRain = new Checkbox(str_radioButton_lightRain, checkboxGroup_weather, false);
		radioButton_normalRain = new Checkbox(str_radioButton_normalRain, checkboxGroup_weather, false);
		radioButton_heavyRain = new Checkbox(str_radioButton_heavyRain, checkboxGroup_weather, false);
		radioButton_lightFog = new Checkbox(str_radioButton_lightFog, checkboxGroup_weather, false);
		radioButton_heavyFog = new Checkbox(str_radioButton_heavyFog, checkboxGroup_weather, false);
		radioButton_sandstorm = new Checkbox(str_radioButton_sandstorm, checkboxGroup_weather, false);
		
		pWeatherRadiobuttons.add(label_weatherConditions);
		pWeatherRadiobuttons.add(radioButton_dry);
		pWeatherRadiobuttons.add(radioButton_lightRain);
		pWeatherRadiobuttons.add(radioButton_normalRain);
		pWeatherRadiobuttons.add(radioButton_heavyRain);
		pWeatherRadiobuttons.add(radioButton_lightFog);
		pWeatherRadiobuttons.add(radioButton_heavyFog);
		pWeatherRadiobuttons.add(radioButton_sandstorm);

	} // end init

	//###########################################

	private void makeGlobalLayout(){
		
		int width = getConfigDialog().getWidth();
		int height = getConfigDialog().getHeight();
		
		int usedWidth = width - 2*MARGIN;
		int usedHeight = height - 2*MARGIN;
		textHeight = (int)(REL_TEXTHEIGHT * width);

		int buttWinHeight = (int)(2.2*textHeight+3*MARGIN);
		int buttWinTop = getInsets().top;

		int textWinHeight = (SHOW_TEXT) ? TEXTWINDOW_HEIGHT : 0;

		// not clear why I must subtract addtl. getInsets().top below
		int simWinHeight = usedHeight - buttWinHeight- textWinHeight - getInsets().top;
		int simWinTop = buttWinTop + buttWinHeight;
		int simSize = (simWinHeight<usedWidth) ? simWinHeight : usedWidth;

		/*EJUST*/
		int rbWidth = (int) (usedWidth);
		int rbHeight = (int) (0.15*simWinHeight);
		/*EJUST*/
		
		int sbWidth = (int)(0.95*usedWidth);
		int sbHeight = (int)(0.25*simWinHeight);

		int sb_IDMwidth=sbWidth; // as circular; no 2 cases!
		int sb_IDMheight=(int)(0.35*simWinHeight);;

		// only one IDMparams panel, cannot be changed!

		/*EJUST*/
		int rbLeft = (int)(usedWidth - rbWidth);
		int rbTop = simWinTop;
		/*EJUST*/
		
		int sbLeft = (int)(0.95*usedWidth - sbWidth);
		int sbTop = rbTop + (int)(0.5*(simSize - 1.2*sbHeight - sb_IDMheight)) - (int)(0.2*sbHeight);
		
		int sb_IDMleft=sbLeft+(int)(0.5*(sbWidth - sb_IDMwidth));
		int sb_IDMtop=sbTop+(int)(1*sbHeight);
		
		setLayout (null);

		pWeatherRadiobuttons.setBounds(rbLeft, rbTop, rbWidth,rbHeight); /*EJUST*/
		
		pScrollbars.setBounds(sbLeft,sbTop, sbWidth,sbHeight);
		pIDMScrollbars.setBounds(sb_IDMleft,sb_IDMtop, sb_IDMwidth,sb_IDMheight); //pIDMScrollbars instead of pIDMparams

		//###########################################
		// Martin jan05
		//!!! nicht mal mit voellig neuem Panel geht dyn. Skalierung!!
		//=> muesste die alten erst loeschen/uns. machen => Leck
		// => vergiss es

		// optional (un)sichtbarmachen;
		//pIDMparams.setVisible(false);
		//pIDMparams.setVisible(true);
		//###########################################


		// <martin nov0> scrollbars flowcontrol und IDM hier deaktivieren!!
		// z.B. sinnvoll, IDM bars zu deaktivieren,
		// wenn Constants.SHOW_INSET_DIAG=true
		
		add(pWeatherRadiobuttons); /*EJUST*/
		add(pScrollbars);
		add(pIDMScrollbars); //EJUST: pIDMScrollbars instead of pIDMparams 
		// <martin nov0> scrollbar IDM hier
	
		cardLayout.show(pScrollbars,"onRamp");
		
		/*EJUST: Show the default IDM param panel (pIDMparams of dry weather condition) */
		IDMcardLayout.show(pIDMScrollbars, str_radioButton_dry);
	}
	// end makeGlobalLayout

	private final static boolean sbBroken = (new Scrollbar(Scrollbar.HORIZONTAL,20,10,0,20).getValue() != 20);

	private Scrollbar getSB (int min, int max, int init){
		final int inc = 1;
		return new Scrollbar(Scrollbar.HORIZONTAL, init, inc, min, max +(sbBroken?inc:0));
	}

	public boolean handleEvent (Event evt) {
		//System.out.println("MicroSim.handleEvent(evt): evt.target ="+evt.target);
		switch (evt.id) {
		case Event.SCROLL_LINE_UP:
		case Event.SCROLL_LINE_DOWN:
		case Event.SCROLL_PAGE_UP:
		case Event.SCROLL_PAGE_DOWN:
		case Event.SCROLL_ABSOLUTE:

			if (evt.target == sb_p_factor2){ //scrollbar Ramp scenario 2: politness factor
				int newval = sb_p_factor2.getValue();
				double p = newval/100.0;
				if (p != p_factor) {
					p_factor = p;
					label_p_factor2.setText (String.valueOf (p));
				}
				model.newValues(p_factor, deltaB);
			}
			else if (evt.target == sb_deltaB2){ //scrollbar Ramp scenario 2: delta b
				int newval = sb_deltaB2.getValue();
				double p = newval/100.0;
				if (p != deltaB) {
					deltaB = p;
					label_deltaB2.setText (String.valueOf (p)+" m/s^2");
				}
				model.newValues(p_factor, deltaB);
			}

			//#####################################################
			//martin jan05

			else if (evt.target == sb_IDMv0) {
				double newval = AdjustmentMethods.getVariableFromSliderpos(sb_IDMv0.getValue(), V0_MIN_KMH,V0_MAX_KMH, false);
				idmCar.v0 = newval/3.6;     /*EJUST: SBC Changes size of blocks to model speed in m/s */
				label_IDMv0.setText (String.valueOf ((int)newval)+" km/h");
				model.changeIDMCarParameters(idmCar);
			}

			else if (evt.target == sb_IDMT) {
				idmCar.T = AdjustmentMethods.getVariableFromSliderpos(sb_IDMT.getValue(), T_MIN_S, T_MAX_S, false);
				String str_T=String.valueOf ((int)idmCar.T)+"."+String.valueOf(((int)(10*idmCar.T))%10);
				label_IDMT.setText (str_T+" s");
				model.changeIDMCarParameters(idmCar);
			}

			else if (evt.target == sb_IDMa) {
				idmCar.a = AdjustmentMethods.getVariableFromSliderpos(sb_IDMa.getValue(), A_MIN_MSII, A_MAX_MSII, false);
				String str_a=String.valueOf ((int)idmCar.a)+"."+String.valueOf(((int)(10*idmCar.a))%10);
				label_IDMa.setText (str_a+" m/s^2");
				model.changeIDMCarParameters(idmCar);
			}

			else if (evt.target == sb_IDMb) {
				idmCar.b = AdjustmentMethods.getVariableFromSliderpos(sb_IDMb.getValue(), B_MIN_MSII, B_MAX_MSII, false);
				String str_b=String.valueOf ((int)idmCar.b)+"."+String.valueOf(((int)(10*idmCar.b))%10);
				label_IDMb.setText (str_b+" m/s^2");
				model.changeIDMCarParameters(idmCar);
			}

			else if (evt.target == sb_IDMs0) {
				idmCar.s0 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs0.getValue(), S0_MIN_M,S0_MAX_M, false);
				idmBus.s0=idmBicycle.s0=idmCar.s0;
				label_IDMs0.setText (String.valueOf ((int)idmCar.s0)+" m");
				model.changeIDMCarParameters(idmCar);
				model.changeIDMBusParameters(idmBus);
				model.changeIDMBicycleParameters(idmBicycle);
			}
			else if (evt.target == sb_IDMs1) {
				idmCar.s1 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs1.getValue(), S1_MIN_M,S1_MAX_M, false);
				idmBus.s1=idmBicycle.s1=idmCar.s1;
				label_IDMs1.setText (String.valueOf ((int)idmCar.s1)+" m");
				model.changeIDMCarParameters(idmCar);
				model.changeIDMBusParameters(idmBus);
				model.changeIDMBicycleParameters(idmBicycle);
			}
			
			/*EJUST: Light Rain Scrollbars*/
			else if (evt.target == sb_IDMv0_LightRain) {
				double newvalLightRain = AdjustmentMethods.getVariableFromSliderpos(sb_IDMv0_LightRain.getValue(), V0_MIN_KMH,V0_MAX_KMH, false);
				idmCarLightRain.v0 = newvalLightRain/3.6;     /*EJUST: SBC Changes size of blocks to model speed in m/s */
				label_IDMv0_LightRain.setText (String.valueOf ((int)newvalLightRain)+" km/h");
				model.changeIDMCarParametersLightRain(idmCarLightRain);
			}
			else if (evt.target == sb_IDMT_LightRain) {
				idmCarLightRain.T = AdjustmentMethods.getVariableFromSliderpos(sb_IDMT_LightRain.getValue(), T_MIN_S, T_MAX_S, false);
				String str_T_LightRain=String.valueOf ((int)idmCarLightRain.T)+"."+String.valueOf(((int)(10*idmCarLightRain.T))%10);
				label_IDMT_LightRain.setText (str_T_LightRain+" s");
				model.changeIDMCarParametersLightRain(idmCarLightRain);
			}

			else if (evt.target == sb_IDMa_LightRain) {
				idmCarLightRain.a = AdjustmentMethods.getVariableFromSliderpos(sb_IDMa_LightRain.getValue(), A_MIN_MSII, A_MAX_MSII, false);
				String str_a_LightRain=String.valueOf ((int)idmCarLightRain.a)+"."+String.valueOf(((int)(10*idmCarLightRain.a))%10);
				label_IDMa_LightRain.setText (str_a_LightRain+" m/s^2");
				model.changeIDMCarParametersLightRain(idmCarLightRain);
			}

			else if (evt.target == sb_IDMb_LightRain) {
				idmCarLightRain.b = AdjustmentMethods.getVariableFromSliderpos(sb_IDMb_LightRain.getValue(), B_MIN_MSII, B_MAX_MSII, false);
				String str_b_LightRain=String.valueOf ((int)idmCarLightRain.b)+"."+String.valueOf(((int)(10*idmCarLightRain.b))%10);
				label_IDMb_LightRain.setText (str_b_LightRain+" m/s^2");
				model.changeIDMCarParametersLightRain(idmCarLightRain);
			}

			else if (evt.target == sb_IDMs0_LightRain) {
				idmCarLightRain.s0 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs0_LightRain.getValue(), S0_MIN_M,S0_MAX_M, false);
				idmBusLightRain.s0=idmBicycleLightRain.s0=idmCarLightRain.s0;
				label_IDMs0_LightRain.setText (String.valueOf ((int)idmCarLightRain.s0)+" m");
				model.changeIDMCarParametersLightRain(idmCarLightRain);
				model.changeIDMBusParametersLightRain(idmBusLightRain);
				model.changeIDMBicycleParametersLightRain(idmBicycleLightRain);
			}
			else if (evt.target == sb_IDMs1_LightRain) {
				idmCarLightRain.s1 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs1_LightRain.getValue(), S1_MIN_M,S1_MAX_M, false);
				idmBusLightRain.s1=idmBicycleLightRain.s1=idmCarLightRain.s1;
				label_IDMs1_LightRain.setText (String.valueOf ((int)idmCarLightRain.s1)+" m");
				model.changeIDMCarParametersLightRain(idmCarLightRain);
				model.changeIDMBusParametersLightRain(idmBusLightRain);
				model.changeIDMBicycleParametersLightRain(idmBicycleLightRain);
			}
			
			/*EJUST: Normal Rain Scrollbars*/
			else if (evt.target == sb_IDMv0_NormalRain) {
				double newvalNormalRain = AdjustmentMethods.getVariableFromSliderpos(sb_IDMv0_NormalRain.getValue(), V0_MIN_KMH,V0_MAX_KMH, false);
				idmCarNormalRain.v0 = newvalNormalRain/3.6;     /*EJUST: SBC Changes size of blocks to model speed in m/s */
				label_IDMv0_NormalRain.setText (String.valueOf ((int)newvalNormalRain)+" km/h");
				model.changeIDMCarParametersNormalRain(idmCarNormalRain);
			}

			else if (evt.target == sb_IDMT_NormalRain) {
				idmCarNormalRain.T = AdjustmentMethods.getVariableFromSliderpos(sb_IDMT_NormalRain.getValue(), T_MIN_S, T_MAX_S, false);
				String str_T_NormalRain=String.valueOf ((int)idmCarNormalRain.T)+"."+String.valueOf(((int)(10*idmCarNormalRain.T))%10);
				label_IDMT_NormalRain.setText (str_T_NormalRain+" s");
				model.changeIDMCarParametersNormalRain(idmCarNormalRain);
			}

			else if (evt.target == sb_IDMa_NormalRain) {
				idmCarNormalRain.a = AdjustmentMethods.getVariableFromSliderpos(sb_IDMa_NormalRain.getValue(), A_MIN_MSII, A_MAX_MSII, false);
				String str_a_NormalRain=String.valueOf ((int)idmCarNormalRain.a)+"."+String.valueOf(((int)(10*idmCarNormalRain.a))%10);
				label_IDMa_NormalRain.setText (str_a_NormalRain+" m/s^2");
				model.changeIDMCarParametersNormalRain(idmCarNormalRain);
			}

			else if (evt.target == sb_IDMb_NormalRain) {
				idmCarNormalRain.b = AdjustmentMethods.getVariableFromSliderpos(sb_IDMb_NormalRain.getValue(), B_MIN_MSII, B_MAX_MSII, false);
				String str_b_NormalRain=String.valueOf ((int)idmCarNormalRain.b)+"."+String.valueOf(((int)(10*idmCarNormalRain.b))%10);
				label_IDMb_NormalRain.setText (str_b_NormalRain+" m/s^2");
				model.changeIDMCarParametersNormalRain(idmCarNormalRain);
			}

			else if (evt.target == sb_IDMs0_NormalRain) {
				idmCarNormalRain.s0 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs0_NormalRain.getValue(), S0_MIN_M,S0_MAX_M, false);
				idmBusNormalRain.s0=idmBicycleNormalRain.s0=idmCarNormalRain.s0;
				label_IDMs0_NormalRain.setText (String.valueOf ((int)idmCarNormalRain.s0)+" m");
				model.changeIDMCarParametersNormalRain(idmCarNormalRain);
				model.changeIDMBusParametersNormalRain(idmBusNormalRain);
				model.changeIDMBicycleParametersNormalRain(idmBicycleNormalRain);
			}
			else if (evt.target == sb_IDMs1_NormalRain) {
				idmCarNormalRain.s1 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs1_NormalRain.getValue(), S1_MIN_M,S1_MAX_M, false);
				idmBusNormalRain.s1=idmBicycleNormalRain.s1=idmCarNormalRain.s1;
				label_IDMs1_NormalRain.setText (String.valueOf ((int)idmCarNormalRain.s1)+" m");
				model.changeIDMCarParametersNormalRain(idmCarNormalRain);
				model.changeIDMBusParametersNormalRain(idmBusNormalRain);
				model.changeIDMBicycleParametersNormalRain(idmBicycleNormalRain);
			}

			/*EJUST: Heavy Rain Scrollbars*/
			else if (evt.target == sb_IDMv0_HeavyRain) {
				double newvalHeavyRain = AdjustmentMethods.getVariableFromSliderpos(sb_IDMv0_HeavyRain.getValue(), V0_MIN_KMH,V0_MAX_KMH, false);
				idmCarHeavyRain.v0 = newvalHeavyRain/3.6;     /*EJUST: SBC Changes size of blocks to model speed in m/s */
				label_IDMv0_HeavyRain.setText (String.valueOf((int)newvalHeavyRain)+" km/h");
				model.changeIDMCarParametersHeavyRain(idmCarHeavyRain);
			}

			else if (evt.target == sb_IDMT_HeavyRain) {
				idmCarHeavyRain.T = AdjustmentMethods.getVariableFromSliderpos(sb_IDMT_HeavyRain.getValue(), T_MIN_S, T_MAX_S, false);
				String str_T_HeavyRain=String.valueOf ((int)idmCarHeavyRain.T)+"."+String.valueOf(((int)(10*idmCarHeavyRain.T))%10);
				label_IDMT_HeavyRain.setText (str_T_HeavyRain+" s");
				model.changeIDMCarParametersHeavyRain(idmCarHeavyRain);
			}

			else if (evt.target == sb_IDMa_HeavyRain) {
				idmCarHeavyRain.a = AdjustmentMethods.getVariableFromSliderpos(sb_IDMa_HeavyRain.getValue(), A_MIN_MSII, A_MAX_MSII, false);
				String str_a_HeavyRain=String.valueOf ((int)idmCarHeavyRain.a)+"."+String.valueOf(((int)(10*idmCarHeavyRain.a))%10);
				label_IDMa_HeavyRain.setText (str_a_HeavyRain+" m/s^2");
				model.changeIDMCarParametersHeavyRain(idmCarHeavyRain);
			}

			else if (evt.target == sb_IDMb_HeavyRain) {
				idmCarHeavyRain.b = AdjustmentMethods.getVariableFromSliderpos(sb_IDMb_HeavyRain.getValue(), B_MIN_MSII, B_MAX_MSII, false);
				String str_b=String.valueOf ((int)idmCarHeavyRain.b)+"."+String.valueOf(((int)(10*idmCarHeavyRain.b))%10);
				label_IDMb_HeavyRain.setText (str_b+" m/s^2");
				model.changeIDMCarParametersHeavyRain(idmCarHeavyRain);
			}

			else if (evt.target == sb_IDMs0_HeavyRain) {
				idmCarHeavyRain.s0 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs0_HeavyRain.getValue(), S0_MIN_M,S0_MAX_M, false);
				idmBusHeavyRain.s0=idmBicycleHeavyRain.s0=idmCarHeavyRain.s0;
				label_IDMs0_HeavyRain.setText (String.valueOf ((int)idmCarHeavyRain.s0)+" m");
				model.changeIDMCarParametersHeavyRain(idmCarHeavyRain);
				model.changeIDMBusParametersHeavyRain(idmBusHeavyRain);
				model.changeIDMBicycleParametersHeavyRain(idmBicycleHeavyRain);
			}
			else if (evt.target == sb_IDMs1_HeavyRain) {
				idmCarHeavyRain.s1 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs1_HeavyRain.getValue(), S1_MIN_M,S1_MAX_M, false);
				idmBusHeavyRain.s1=idmBicycleHeavyRain.s1=idmCarHeavyRain.s1;
				label_IDMs1_HeavyRain.setText (String.valueOf ((int)idmCarHeavyRain.s1)+" m");
				model.changeIDMCarParametersHeavyRain(idmCarHeavyRain);
				model.changeIDMBusParametersHeavyRain(idmBusHeavyRain);
				model.changeIDMBicycleParametersHeavyRain(idmBicycleHeavyRain);
			}

			/*EJUST: Light Fog Scrollbars*/
			else if (evt.target == sb_IDMv0_LightFog) {
				double newvalLightFog = AdjustmentMethods.getVariableFromSliderpos(sb_IDMv0_LightFog.getValue(), V0_MIN_KMH,V0_MAX_KMH, false);
				idmCarLightFog.v0 = newvalLightFog/3.6;     /*EJUST: SBC Changes size of blocks to model speed in m/s */
				label_IDMv0_LightFog.setText (String.valueOf ((int)newvalLightFog)+" km/h");
				model.changeIDMCarParametersLightFog(idmCarLightFog);
			}

			else if (evt.target == sb_IDMT_LightFog) {
				idmCarLightFog.T = AdjustmentMethods.getVariableFromSliderpos(sb_IDMT_LightFog.getValue(), T_MIN_S, T_MAX_S, false);
				String str_T_LightFog=String.valueOf ((int)idmCarLightFog.T)+"."+String.valueOf(((int)(10*idmCarLightFog.T))%10);
				label_IDMT_LightFog.setText (str_T_LightFog+" s");
				model.changeIDMCarParametersLightFog(idmCarLightFog);
			}

			else if (evt.target == sb_IDMa_LightFog) {
				idmCarLightFog.a = AdjustmentMethods.getVariableFromSliderpos(sb_IDMa_LightFog.getValue(), A_MIN_MSII, A_MAX_MSII, false);
				String str_a=String.valueOf ((int)idmCarLightFog.a)+"."+String.valueOf(((int)(10*idmCarLightFog.a))%10);
				label_IDMa_LightFog.setText (str_a+" m/s^2");
				model.changeIDMCarParametersLightFog(idmCarLightFog);
			}

			else if (evt.target == sb_IDMb_LightFog) {
				idmCarLightFog.b = AdjustmentMethods.getVariableFromSliderpos(sb_IDMb_LightFog.getValue(), B_MIN_MSII, B_MAX_MSII, false);
				String str_b=String.valueOf ((int)idmCarLightFog.b)+"."+String.valueOf(((int)(10*idmCarLightFog.b))%10);
				label_IDMb_LightFog.setText (str_b+" m/s^2");
				model.changeIDMCarParametersLightFog(idmCarLightFog);
			}

			else if (evt.target == sb_IDMs0_LightFog) {
				idmCarLightFog.s0 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs0_LightFog.getValue(), S0_MIN_M,S0_MAX_M, false);
				idmBusLightFog.s0=idmBicycleLightFog.s0=idmCarLightFog.s0;
				label_IDMs0_LightFog.setText (String.valueOf ((int)idmCarLightFog.s0)+" m");
				model.changeIDMCarParametersLightFog(idmCarLightFog);
				model.changeIDMBusParametersLightFog(idmBusLightFog);
				model.changeIDMBicycleParametersLightFog(idmBicycleLightFog);
			}
			else if (evt.target == sb_IDMs1_LightFog) {
				idmCarLightFog.s1 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs1_LightFog.getValue(), S1_MIN_M,S1_MAX_M, false);
				idmBusLightFog.s1=idmBicycleLightFog.s1=idmCarLightFog.s1;
				label_IDMs1_LightFog.setText (String.valueOf ((int)idmCarLightFog.s1)+" m");
				model.changeIDMCarParametersLightFog(idmCarLightFog);
				model.changeIDMBusParametersLightFog(idmBusLightFog);
				model.changeIDMBicycleParametersLightFog(idmBicycleLightFog);
			}

			/*EJUST: Heavy Fog Scrollbars*/
			else if (evt.target == sb_IDMv0_HeavyFog) {
				double newvalHeavyFog = AdjustmentMethods.getVariableFromSliderpos(sb_IDMv0_HeavyFog.getValue(), V0_MIN_KMH,V0_MAX_KMH, false);
				idmCarHeavyFog.v0 = newvalHeavyFog/3.6;     /*EJUST: SBC Changes size of blocks to model speed in m/s */
				label_IDMv0_HeavyFog.setText (String.valueOf ((int)newvalHeavyFog)+" km/h");
				model.changeIDMCarParametersHeavyFog(idmCarHeavyFog);
			}

			else if (evt.target == sb_IDMT_HeavyFog) {
				idmCarHeavyFog.T = AdjustmentMethods.getVariableFromSliderpos(sb_IDMT_HeavyFog.getValue(), T_MIN_S, T_MAX_S, false);
				String str_T_HeavyFog=String.valueOf ((int)idmCarHeavyFog.T)+"."+String.valueOf(((int)(10*idmCarHeavyFog.T))%10);
				label_IDMT_HeavyFog.setText (str_T_HeavyFog+" s");
				model.changeIDMCarParametersHeavyFog(idmCarHeavyFog);
			}

			else if (evt.target == sb_IDMa_HeavyFog) {
				idmCarHeavyFog.a = AdjustmentMethods.getVariableFromSliderpos(sb_IDMa_HeavyFog.getValue(), A_MIN_MSII, A_MAX_MSII, false);
				String str_a=String.valueOf ((int)idmCarHeavyFog.a)+"."+String.valueOf(((int)(10*idmCarHeavyFog.a))%10);
				label_IDMa_HeavyFog.setText (str_a+" m/s^2");
				model.changeIDMCarParametersHeavyFog(idmCarHeavyFog);
			}

			else if (evt.target == sb_IDMb_HeavyFog) {
				idmCarHeavyFog.b = AdjustmentMethods.getVariableFromSliderpos(sb_IDMb_HeavyFog.getValue(), B_MIN_MSII, B_MAX_MSII, false);
				String str_b=String.valueOf ((int)idmCarHeavyFog.b)+"."+String.valueOf(((int)(10*idmCarHeavyFog.b))%10);
				label_IDMb_HeavyFog.setText (str_b+" m/s^2");
				model.changeIDMCarParametersHeavyFog(idmCarHeavyFog);
			}

			else if (evt.target == sb_IDMs0_HeavyFog) {
				idmCarHeavyFog.s0 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs0_HeavyFog.getValue(), S0_MIN_M,S0_MAX_M, false);
				idmBusHeavyFog.s0=idmBicycleHeavyFog.s0=idmCarHeavyFog.s0;
				label_IDMs0_HeavyFog.setText (String.valueOf ((int)idmCarHeavyFog.s0)+" m");
				model.changeIDMCarParametersHeavyFog(idmCarHeavyFog);
				model.changeIDMBusParametersHeavyFog(idmBusHeavyFog);
				model.changeIDMBicycleParametersHeavyFog(idmBicycleHeavyFog);
			}
			else if (evt.target == sb_IDMs1_HeavyFog) {
				idmCarHeavyFog.s1 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs1_HeavyFog.getValue(), S1_MIN_M,S1_MAX_M, false);
				idmBusHeavyFog.s1=idmBicycleHeavyFog.s1=idmCarHeavyFog.s1;
				label_IDMs1_HeavyFog.setText (String.valueOf ((int)idmCarHeavyFog.s1)+" m");
				model.changeIDMCarParametersHeavyFog(idmCarHeavyFog);
				model.changeIDMBusParametersHeavyFog(idmBusHeavyFog);
				model.changeIDMBicycleParametersHeavyFog(idmBicycleHeavyFog);
			}

			/*EJUST: Sandstorm Scrollbars*/
			else if (evt.target == sb_IDMv0_Sandstorm) {
				double newvalSandstorm = AdjustmentMethods.getVariableFromSliderpos(sb_IDMv0_Sandstorm.getValue(), V0_MIN_KMH,V0_MAX_KMH, false);
				idmCarSandstorm.v0 = newvalSandstorm/3.6;     /*EJUST: SBC Changes size of blocks to model speed in m/s */
				label_IDMv0_Sandstorm.setText (String.valueOf ((int)newvalSandstorm)+" km/h");
				model.changeIDMCarParametersSandstorm(idmCarSandstorm);
			}

			else if (evt.target == sb_IDMT_Sandstorm) {
				idmCarSandstorm.T = AdjustmentMethods.getVariableFromSliderpos(sb_IDMT_Sandstorm.getValue(), T_MIN_S, T_MAX_S, false);
				String str_T_Sandstorm=String.valueOf ((int)idmCarSandstorm.T)+"."+String.valueOf(((int)(10*idmCarSandstorm.T))%10);
				label_IDMT_Sandstorm.setText (str_T_Sandstorm+" s");
				model.changeIDMCarParametersSandstorm(idmCarSandstorm);
			}

			else if (evt.target == sb_IDMa_Sandstorm) {
				idmCarSandstorm.a = AdjustmentMethods.getVariableFromSliderpos(sb_IDMa_Sandstorm.getValue(), A_MIN_MSII, A_MAX_MSII, false);
				String str_a_Sandstorm=String.valueOf ((int)idmCarSandstorm.a)+"."+String.valueOf(((int)(10*idmCarSandstorm.a))%10);
				label_IDMa_Sandstorm.setText (str_a_Sandstorm+" m/s^2");
				model.changeIDMCarParametersSandstorm(idmCarSandstorm);
			}

			else if (evt.target == sb_IDMb_Sandstorm) {
				idmCarSandstorm.b = AdjustmentMethods.getVariableFromSliderpos(sb_IDMb_Sandstorm.getValue(), B_MIN_MSII, B_MAX_MSII, false);
				String str_b_Sandstorm=String.valueOf ((int)idmCarSandstorm.b)+"."+String.valueOf(((int)(10*idmCarSandstorm.b))%10);
				label_IDMb_Sandstorm.setText (str_b_Sandstorm+" m/s^2");
				model.changeIDMCarParametersSandstorm(idmCarSandstorm);
			}

			else if (evt.target == sb_IDMs0_Sandstorm) {
				idmCarSandstorm.s0 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs0_Sandstorm.getValue(), S0_MIN_M,S0_MAX_M, false);
				idmBusSandstorm.s0=idmBicycleSandstorm.s0=idmCarSandstorm.s0;
				label_IDMs0_Sandstorm.setText (String.valueOf ((int)idmCarSandstorm.s0)+" m");
				model.changeIDMCarParametersSandstorm(idmCarSandstorm);
				model.changeIDMBusParametersSandstorm(idmBusSandstorm);
				model.changeIDMBicycleParametersSandstorm(idmBicycleSandstorm);
			}
			else if (evt.target == sb_IDMs1_Sandstorm) {
				idmCarSandstorm.s1 = AdjustmentMethods.getVariableFromSliderpos(sb_IDMs1_Sandstorm.getValue(), S1_MIN_M,S1_MAX_M, false);
				idmBusSandstorm.s1=idmBicycleSandstorm.s1=idmCarSandstorm.s1;
				label_IDMs1_Sandstorm.setText (String.valueOf ((int)idmCarSandstorm.s1)+" m");
				model.changeIDMCarParametersSandstorm(idmCarSandstorm);
				model.changeIDMBusParametersSandstorm(idmBusSandstorm);
				model.changeIDMBicycleParametersSandstorm(idmBicycleSandstorm);
			}
			//##############################################
		}
		return super.handleEvent(evt);
	}
	
	//###########################################################

	public boolean action(Event event, Object arg){
		IDMcardLayout.show(pIDMScrollbars, checkboxGroup_weather.getSelectedCheckbox().getLabel());
		
		return true;
	}
	
	private void setLanguage(){
		str_IDMv0 = lang.getSpeedLimitIDM();
		str_IDMT = lang.getDesiredHeadwayIDM();
		str_IDMa = lang.getDesiredAccelIDM();
		str_IDMb = lang.getDesiredDecelIDM();
		str_IDMs0 = lang.getMinGapIDM();
		str_IDMs1 = lang.getS1IDM();

		str_polite = lang.getMobilPoliteness();
		str_db = lang.getMobilThreshold();
		
		str_radioButton_dry = lang.getDryName();
		str_radioButton_lightRain = lang.getLightRainName();
		str_radioButton_normalRain = lang.getNormalRainName();
		str_radioButton_heavyRain = lang.getHeavyRainName();
		str_radioButton_lightFog = lang.getLightFogName();
		str_radioButton_heavyFog = lang.getHeavyFogName();
		str_radioButton_sandstorm = lang.getSandstormName();
	}
}