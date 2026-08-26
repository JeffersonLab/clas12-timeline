package org.jlab.clas.timeline.histograms;

import java.util.*;
import org.jlab.clas.pdg.PhysicsConstants;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.ConstantsManager;

// Issue 510: Removed unused histograms to save only called by timeline step.
// For the full version, please refer to the previous version, such as e1b4bf2d0f70ade26bf70f67bab26554a62e6511.

public class DCandFTOF {
  boolean userTimeBased;
  public int runNum;
  public String outputDir;
  public boolean hasRF;
  public float RFTime, rfoffset1, rfoffset2;
  public float rfPeriod;
  public float[] mean_tdiff;
  public int[] nev;
  public int rf_large_integer;
  public int e_part_ind;
  public H1F[][] p1a_edep;                     // related timeline: ['ftof_edep_p1a_largeangles', 'ftof_edep_p1a_midangles', 'ftof_edep_p1a_smallangles']
  public H1F[][] p1b_edep;                     // related timeline: ['ftof_edep_p1b_largeangles', 'ftof_edep_p1b_midangles', 'ftof_edep_p1b_smallangles']
  public H1F[] p2_edep;                        // related timeline: ['ftof_edep_p2']
  public H1F[] p1a_tdcadc_dt;                  // related timeline: ['ftof_tdcadc_p1a']
  public H1F[] p1b_tdcadc_dt;                  // related timeline: ['ftof_tdcadc_p1b']
  public H1F[] p2_tdcadc_dt;                   // related timeline: ['ftof_tdcadc_p2']
  public H1F[] ftof_ctof_vtdiff;               // related timeline: ['ftof_ctof_vtdiff']
  public H1F[] p1a_dt_calib_all;               // related timeline: ['ftof_time_p1a']
  public H1F[] p1b_dt_calib_all;               // related timeline: ['ftof_time_p1b']
  public H1F[] p2_dt_calib_all;                // related timeline: ['ftof_time_p2']
  public H2F[][] DC_residuals_trkDoca;         // related timeline: ['dc_residuals_sec', 'dc_residuals_sec_sl']
  public H1F[][] DC_time;                      // related timeline: ['dc_time_sec_sl', 'dc_t0_sec_sl', 'dc_tmax_sec_sl']
  public H1F[][] DC_time_even;                 // related timeline: ['dc_t0_even_sec_sl']
  public H1F[][] DC_time_odd;                  // related timeline: ['dc_t0_odd_sec_sl']
  public H2F[][] DC_residuals_trkDoca_rescut;  // related timeline: ['dc_residuals_sec_sl_rescut']
  public float p1a_counter_thickness, p1b_counter_thickness, p2_counter_thickness;
  public int phase_offset;
  public long timestamp;

  public IndexedTable InverseTranslationTable;
  public IndexedTable calibrationTranslationTable;
  public IndexedTable rfTable, rfTableOffset;
  public IndexedTable ftofTable, ctofTable;
  public ConstantsManager ccdb;

  public DCandFTOF(int reqrunNum, String reqOutputDir, boolean reqTimeBased) {
    runNum = reqrunNum;userTimeBased=reqTimeBased;
    outputDir = reqOutputDir;

    rfPeriod = 4.008f;
    ccdb = new ConstantsManager();
    ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo","/calibration/eb/rf/config","/calibration/eb/rf/offset","/calibration/ftof/time_jitter"}));
    rfTable = ccdb.getConstants(runNum,"/calibration/eb/rf/config");
    if (rfTable.hasEntry(1, 1, 1)){
      System.out.println(String.format("RF period from ccdb for run %d: %f",runNum,rfTable.getDoubleValue("clock",1,1,1)));
      rfPeriod = (float)rfTable.getDoubleValue("clock",1,1,1);
    }
    rf_large_integer = 1000;
    rfTableOffset = ccdb.getConstants(runNum,"/calibration/eb/rf/offset");
    if (rfTableOffset.hasEntry(1, 1, 1)){
      rfoffset1 = (float)rfTableOffset.getDoubleValue("offset",1,1,1);
      rfoffset2 = (float)rfTableOffset.getDoubleValue("offset",1,1,2);
      System.out.println(String.format("RF1 offset from ccdb for run %d: %f",runNum,rfoffset1));
      System.out.println(String.format("RF2 offset from ccdb for run %d: %f",runNum,rfoffset2));
    }
    phase_offset = 0;
    ftofTable = ccdb.getConstants(runNum,"/calibration/ftof/time_jitter");
    if (ftofTable.hasEntry(0, 0, 0)){
      phase_offset = (int)ftofTable.getDoubleValue("phase",0,0,0);
      System.out.println(String.format("Phase Offset %d: %d",runNum,phase_offset));
    }
    p1a_counter_thickness = 5.0f; //cm
                                  //phase_offset = 3; //RGA Fall 2018, RGB Spring 2019, RGA Spring 2019
                                  //phase_offset = 1; //Engineering Run, RGA Spring 2018
    p1b_counter_thickness = 6.0f; //cm
    p2_counter_thickness = 5.0f; //cm

    p1a_dt_calib_all = new H1F[6];
    p1b_dt_calib_all = new H1F[6];
    p2_dt_calib_all = new H1F[6];
    p1a_edep = new H1F[6][3];
    p1b_edep = new H1F[6][3];
    p2_edep = new H1F[6];
    p1b_tdcadc_dt = new H1F[6];
    p1a_tdcadc_dt = new H1F[6];
    p2_tdcadc_dt = new H1F[6];
    DC_residuals_trkDoca = new H2F[6][6];
    DC_residuals_trkDoca_rescut = new H2F[6][6];
    DC_time = new H1F[6][6];
    DC_time_even = new H1F[6][6];
    DC_time_odd = new H1F[6][6];

    ftof_ctof_vtdiff = new H1F[6];

    mean_tdiff = new float[6];
    nev = new int[6];

    for(int s=0;s<6;s++){

      mean_tdiff[s] = 0.f;
      nev[s] = 0;

      //FTOF vertex time differences are to be plotted in 25-ps-wide bins
      p1a_dt_calib_all[s] = new H1F(String.format("p1a_dt_S%d",s+1),String.format("p1a_dt_S%d",s+1),160,-2.000,2.000);
      p1a_dt_calib_all[s].setTitle(String.format("p1a S%d FTOF vertex t - RFTime",s+1));
      p1a_dt_calib_all[s].setTitleX("FTOF vertex t - RFTime (ns)");
      p1a_dt_calib_all[s].setTitleY("counts");
      p1b_dt_calib_all[s] = new H1F(String.format("p1b_dt_S%d",s+1),String.format("p1b_dt_S%d",s+1),160,-2.000,2.000);
      p1b_dt_calib_all[s].setTitle(String.format("p1b S%d FTOF vertex t - RFTime",s+1));
      p1b_dt_calib_all[s].setTitleX("FTOF vertex t - RFTime (ns)");
      p1b_dt_calib_all[s].setTitleY("counts");
      p2_dt_calib_all[s] = new H1F(String.format("p2_dt_S%d",s+1),String.format("p2_dt_S%d",s+1),960,-12,12);
      p2_dt_calib_all[s].setTitle(String.format("p2 S%d FTOF vertex t - RFTime",s+1));
      p2_dt_calib_all[s].setTitleX("FTOF vertex t - RFTime (ns)");
      p2_dt_calib_all[s].setTitleY("counts");

      float[] DCcellsizeSL = {0.9f,0.9f,1.3f,1.3f,2.0f,2.0f};

      p1a_edep[s][0] = new H1F(String.format("p1a_edep_smallangles_S%d",s+1),"p1a_edep_smallangles",100,0.,30.);
      p1a_edep[s][0].setTitle(String.format("p1a PathLCorrected Edep, small angles, S%d",s+1));
      p1a_edep[s][0].setTitleX("E (MeV)");
      p1a_edep[s][0].setTitleY("counts");
      p1a_edep[s][1] = new H1F(String.format("p1a_edep_midangles_S%d",s+1),"p1a_edep_midangles",100,0.,30.);
      p1a_edep[s][1].setTitle(String.format("p1a PathLCorrected Edep, mid angles, S%d",s+1));
      p1a_edep[s][1].setTitleX("E (MeV)");
      p1a_edep[s][1].setTitleY("counts");
      p1a_edep[s][2] = new H1F(String.format("p1a_edep_largeangles_S%d",s+1),"p1a_edep_largeangles",100,0.,30.);
      p1a_edep[s][2].setTitle(String.format("p1a PathLCorrected Edep, large angles, S%d",s+1));
      p1a_edep[s][2].setTitleX("E (MeV)");
      p1a_edep[s][2].setTitleY("counts");

      p1b_edep[s][0] = new H1F(String.format("p1b_edep_smallangles_S%d",s+1),"p1b_edep_smallangles",100,0.,30.);
      p1b_edep[s][0].setTitle(String.format("p1b PathLCorrected Edep, small angles, S%d",s+1));
      p1b_edep[s][0].setTitleX("E (MeV)");
      p1b_edep[s][0].setTitleY("counts");
      p1b_edep[s][1] = new H1F(String.format("p1b_edep_midangles_S%d",s+1),"p1b_edep_midangles",100,0.,30.);
      p1b_edep[s][1].setTitle(String.format("p1b PathLCorrected Edep, mid angles, S%d",s+1));
      p1b_edep[s][1].setTitleX("E (MeV)");
      p1b_edep[s][1].setTitleY("counts");
      p1b_edep[s][2] = new H1F(String.format("p1b_edep_largeangles_S%d",s+1),"p1b_edep_largeangles",100,0.,30.);
      p1b_edep[s][2].setTitle(String.format("p1b PathLCorrected Edep, large angles, S%d",s+1));
      p1b_edep[s][2].setTitleX("E (MeV)");
      p1b_edep[s][2].setTitleY("counts");

      p2_edep[s] = new H1F(String.format("p2_edep_S%d",s+1),"p2_edep",100,0.,30.);
      p2_edep[s].setTitle(String.format("p2 PathLCorrected Edep, S%d",s+1));
      p2_edep[s].setTitleX("E (MeV)");
      p2_edep[s].setTitleY("counts");

      p1a_tdcadc_dt[s] =  new H1F(String.format("p1a_tdcadc_dt_S%d",s+1),"p1a_tdcadc",15750,-30.000,600.000);
      p1a_tdcadc_dt[s].setTitle(String.format("p1a t_tdc-t_fadc, S%d",s+1));
      p1a_tdcadc_dt[s].setTitleX("t_tdc-t_fadc (ns)");
      p1a_tdcadc_dt[s].setTitleY("counts");

      p1b_tdcadc_dt[s] =  new H1F(String.format("p1b_tdcadc_dt_S%d",s+1),"p1b_tdcadc",15750,-30.000,600.000);
      p1b_tdcadc_dt[s].setTitle(String.format("p1b t_tdc-t_fadc, S%d",s+1));
      p1b_tdcadc_dt[s].setTitleX("t_tdc-t_fadc (ns)");
      p1b_tdcadc_dt[s].setTitleY("counts");

      p2_tdcadc_dt[s] =  new H1F(String.format("p2_tdcadc_dt_S%d",s+1),"p2_tdcadc",15750,-30.000,600.000);
      p2_tdcadc_dt[s].setTitle(String.format("p2 t_tdc-t_fadc, S%d",s+1));
      p2_tdcadc_dt[s].setTitleX("t_tdc-t_fadc (ns)");
      p2_tdcadc_dt[s].setTitleY("counts");

      ftof_ctof_vtdiff[s] = new H1F(String.format("ftof-ctof_vtdiff_S%d",s+1),"ftof-ctof_vtdiff",300,-5.,5.);
      ftof_ctof_vtdiff[s].setTitle(String.format("FTOFvt - CTOFvt, S%d",s+1));
      ftof_ctof_vtdiff[s].setTitleX("FTOFvt - CTOFvt (ns)");
      ftof_ctof_vtdiff[s].setTitleY("counts");

      for(int sl=0;sl<6;sl++){
        DC_residuals_trkDoca[s][sl] = new H2F(String.format("DC_residuals_trkDoca_%d_%d",s+1,sl+1),String.format("DC_residuals_trkDoca_%d_%d",s+1,sl+1),100,0,DCcellsizeSL[sl],400,-0.5,0.5);
        DC_residuals_trkDoca[s][sl].setTitle(String.format("DC residuals S%d SL%d",s+1,sl+1));
        DC_residuals_trkDoca[s][sl].setTitleX("DOCA (cm)");
        DC_residuals_trkDoca[s][sl].setTitleY("residual (cm)");
        DC_residuals_trkDoca_rescut[s][sl] = new H2F(String.format("DC_residuals_trkDoca_rescut_%d_%d",s+1,sl+1),String.format("DC_residuals_trkDoca_rescut_%d_%d",s+1,sl+1),100,0,DCcellsizeSL[sl],400,-0.5,0.5);
        DC_residuals_trkDoca_rescut[s][sl].setTitle(String.format("DC residuals S%d SL%d",s+1,sl+1));
        DC_residuals_trkDoca_rescut[s][sl].setTitleX("DOCA (cm)");
        DC_residuals_trkDoca_rescut[s][sl].setTitleY("residual (cm)");
        DC_time[s][sl] = new H1F(String.format("DC_Time_%d_%d",s+1,sl+1),String.format("DC_Time_%d_%d",s+1,sl+1),200,-100,1000);
        DC_time[s][sl].setTitle(String.format("DC Time S%d SL%d",s+1,sl+1));
        DC_time[s][sl].setTitleX("time (ns)");
        DC_time[s][sl].setLineWidth(4);
        DC_time_even[s][sl] = new H1F(String.format("DC_Time_even_%d_%d",s+1,sl+1),String.format("DC_Time_even_%d_%d",s+1,sl+1),200,-100,1000);
        DC_time_even[s][sl].setTitle(String.format("DC Time S%d SL%d",s+1,sl+1));
        DC_time_even[s][sl].setTitleX("time (ns)");
        DC_time_even[s][sl].setLineWidth(4);
        DC_time_odd[s][sl] = new H1F(String.format("DC_Time_odd_%d_%d",s+1,sl+1),String.format("DC_Time_odd_%d_%d",s+1,sl+1),200,-100,1000);
        DC_time_odd[s][sl].setTitle(String.format("DC Time S%d SL%d",s+1,sl+1));
        DC_time_odd[s][sl].setTitleX("time (ns)");
        DC_time_odd[s][sl].setLineWidth(4);
      }
    }
  }
  /*
     {"name":"sector",	   "id":3, "type":"int8",   "info":"DC sector"},
     {"name":"superlayer",   "id":4, "type":"int8",  "info":"DC superlayer (1...6)"},
     {"name":"doca",		 "id":8,  "type":"float", "info":"doca of the hit calculated from TDC (in cm)"},
     {"name":"trkDoca",	  "id":10,  "type":"float", "info":"track doca of the hit (in cm)"},
     {"name":"timeResidual", "id":11,  "type":"float", "info":"time residual of the hit (in cm)"},
     */
  public void fillDC(DataBank DCB, DataBank RunConfig, DataBank RecPart){
    for(int r=0;r<DCB.rows();r++){
      int s = DCB.getInt("sector",r)-1;
      int sl = DCB.getInt("superlayer",r)-1;
      float trkDoca = DCB.getFloat("trkDoca",r);
      float timeResidual = DCB.getFloat("timeResidual",r);
      float time = DCB.getFloat("time",r);
      double betacutvalue = 0.9;
      double fitresidualcut = 1000; //microns

      //Determine alpha cut
      double alphacutvalue = 30;
      double bFieldVal = (double) DCB.getFloat("B", r);
      int polarity = (int)Math.signum(RunConfig.getFloat("torus",0));
      // alpha in the bank is corrected for B field.  To fill the alpha bin use the uncorrected value
      double theta0 = Math.toDegrees(Math.acos(1-0.02*bFieldVal));
      double alphaRadUncor = DCB.getFloat("Alpha", r)+ polarity*theta0;
      boolean alphacutpass = false;
      if(alphaRadUncor> -1*alphacutvalue &&  alphaRadUncor< alphacutvalue) {
        alphacutpass = true;
      }
      boolean leadingelectron = false;
      int leadingparticlepid = RecPart.getInt("pid", 0);
      if (leadingparticlepid == 11) leadingelectron = true;

      long timestamp = RunConfig.getLong("timestamp", 0);

      if(s>-1&&s<6&&sl>-1&&sl<6 && leadingelectron){
        // if (otherregions||region2) {


        //Add extra cuts on hits from DC4gui. TrkID, beta, alphacut, TFlight (maybe PID?, needs REC::Event here)
        if (DCB.getByte("trkID", r) > 0 && DCB.getFloat("beta", r) > betacutvalue &&
            DCB.getFloat("TFlight",r) > 0 && alphacutpass == true
           )
        {

          DC_residuals_trkDoca[s][sl].fill(trkDoca,timeResidual);
          DC_time[s][sl].fill(time);

          if( timestamp%2 == 0) {//even time stamps
            //sector and superlayer need to go from 1-6
            DC_time_even[s][sl].fill(time);
          }
          else {//odd time stamps
            //sector and superlayer need to go from 1-6
            DC_time_odd[s][sl].fill(time);
          }
          //Apply also fitresidual cut, factor 0.0001 to convert to cm from microns
          if (DCB.getFloat("fitResidual",r) < 0.0001 * fitresidualcut) {
            DC_residuals_trkDoca_rescut[s][sl].fill(trkDoca,timeResidual);
          }
        }
        }
        else if (s>-1&&s<6&&sl>-1&&sl<6 && leadingelectron==false) {}
        else System.out.println("sector "+(s+1)+" superlayer "+(sl+1));
      }
    }

    public void fillTOFadctdcHists(DataBank ftofadc, DataBank ftoftdc) {
      for (int r=0;r<ftoftdc.rows();r++) {
        int sector_tdc = ftoftdc.getInt("sector",r);
        int layer_tdc = ftoftdc.getInt("layer",r);
        int component_tdc = ftoftdc.getInt("component",r);
        int order = ftoftdc.getByte("order",r)-2;
        int tdc_pmt = (component_tdc-1)*2+order+1;
        int TDC = ftoftdc.getInt("TDC",r);
        for (int j=0;j<ftofadc.rows();j++) {
          int sector_adc = ftofadc.getInt("sector",j);
          int layer_adc = ftofadc.getInt("layer",j);
          int component_adc = ftofadc.getInt("component",j);
          int order_adc = ftofadc.getByte("order",j);
          int adc_pmt = (component_adc-1)*2+order_adc+1;
          float time_adc = ftofadc.getFloat("time",j);
          if (sector_adc == sector_tdc && layer_adc == layer_tdc && component_adc == component_tdc && adc_pmt == tdc_pmt) {
            int triggerPhaseTOF = (int)((timestamp + phase_offset)%6);
            float time_tdc = (float)TDC*0.02345f - (float)triggerPhaseTOF*4.f;
            float time_diff = time_tdc - time_adc;
            if (layer_adc == 1 && triggerPhaseTOF!=0) {p1a_tdcadc_dt[sector_adc-1].fill(time_diff);}
            if (layer_adc == 2 && triggerPhaseTOF!=0) {p1b_tdcadc_dt[sector_adc-1].fill(time_diff);}
            if (layer_adc == 3 && triggerPhaseTOF!=0) {p2_tdcadc_dt[sector_adc-1].fill(time_diff);}

          }
        }
      }
    }

    public void fillTOFCalibHists(DataBank part, DataBank sc, DataBank scextras, DataBank trk){
      // 11 Oct 2020, Trigger particle should be excluded, i.e. start loop from second particle in REC::Particle
      // 22 Dec 2020, positrons added to p1a and p1b; momentum, energy deposition, and reduced track chi2 added per Daniel's request
      // 8 Jan 2021, another set of histograms added - does not contain the trigger track (k=0). These histos are used to track 4-ns offsets. In the timeline, only the centroid of the distribution should be included. The calibration monitoring histos are changed to contain the trigger track.
      // 18 Jan 2022, energy/path is now taken from REC::ScintExtras, FTOF::Hits is not used anymore
      for(int k=0;k<part.rows();k++) {
        int pid = part.getInt("pid",k);
        float px = part.getFloat("px",k);
        float py = part.getFloat("py",k);
        float pz = part.getFloat("pz",k);
        float vz = part.getFloat("vz",k);
        float vt = part.getFloat("vt",k);
        float mom = (float)Math.sqrt(px*px+py*py+pz*pz);
        float theta = (float)Math.toDegrees(Math.acos(pz/mom));
        float reducedchi2 = 10000.f;
        float energy = -100.f;

        for (int j=0;j<trk.rows();j++) {
          if (trk.getShort("pindex",j)==k) {
            reducedchi2 = trk.getFloat("chi2",j)/trk.getShort("NDF",j);
          }
        }

        for (int j=0;j<sc.rows();j++) {
          if (sc.getShort("pindex",j)==k) {
            if (sc.getByte("detector",j)==12 && e_part_ind != -1) {
              float dedx = scextras.getFloat("dedx",j);
              int sector = sc.getInt("sector",j);
              float time = sc.getFloat("time", j);
              float pathlength = sc.getFloat("path",j);
              float timediff = -10.f;
              float flighttime = -10.0f;
              // float vcor = -10.0f;
              // electron's case
              if (pid == 11) {
                flighttime = pathlength/29.98f;
                // vcor = vz/29.98f;
              }
              // pi+, pi- case
              else if (pid == 211 || pid == -211) {
                flighttime = pathlength/(float)(29.98f * mom/Math.sqrt(mom*mom+0.13957f*0.13957f));
                // vcor = vz/(float)(29.98f * mom/Math.sqrt(mom*mom+0.13957f*0.13957f));
              }
              //proton case
              else if (pid == 2212) {
                flighttime = pathlength/(float)(29.98f * mom/Math.sqrt(mom*mom+0.93827f*0.93827f));
                // vcor = vz/(float)(29.98f * mom/Math.sqrt(mom*mom+0.93827f*0.93827f));
              }
              //otherwise skip
              else continue;

              timediff = (float) (time - flighttime) - vt;

              if (sc.getByte("layer",j)==1){
                energy = dedx*p1a_counter_thickness;
              }
              if (sc.getByte("layer",j)==2){
                energy = dedx*p1b_counter_thickness;
              }
              if (sc.getByte("layer",j)==3){
                energy = dedx*p2_counter_thickness;
              }

              // panel 1a and 1b, use e-, pi+, pi-
              // 22 Dec 2020: Cuts aligned with calib suite as per Dan's info, e+ added
              if (pid == 11 || pid == -11 || pid == 211 || pid == -211){
                if (sc.getByte("layer",j)==1){
                  if (mom > 0.4 && mom < 10 && energy > 0.5 && reducedchi2 < 75) {
                    p1a_dt_calib_all[sector-1].fill(timediff);
                  }
                  if (energy > 2.){
                    if (theta <= 11.) p1a_edep[sector-1][0].fill(energy);
                    if (theta > 11. && theta <=23) p1a_edep[sector-1][1].fill(energy);
                    if (theta > 23.) p1a_edep[sector-1][2].fill(energy);
                  }
                }
                if (sc.getByte("layer",j)==2){
                  if (mom > 0.4 && mom < 10 && energy > 0.5 && reducedchi2 < 75) {
                    p1b_dt_calib_all[sector-1].fill(timediff);
                  }
                  if (energy > 2.){
                    if (theta <= 11.) p1b_edep[sector-1][0].fill(energy);
                    if (theta > 11. && theta <=23) p1b_edep[sector-1][1].fill(energy);
                    if (theta > 23.) p1b_edep[sector-1][2].fill(energy);
                  }
                }
              }

              // panel 2, use p, pi+, p-; cuts are from calibration suite
              // 22 Dec 2020: Cuts aligned with calib suite as per Dan's info
              if (pid == 2212 || pid == 211 || pid == -211) {
                if (sc.getByte("layer",j)==3){
                  if (energy > 0.5 && vz > -10. && vz < 5.0 && mom > 0.4 && mom <10. && reducedchi2 < 5000.) {
                    p2_dt_calib_all[sector-1].fill(timediff);
                  }
                  if (energy >2.){
                    p2_edep[sector-1].fill(energy);
                  }
                }
              }
            }
          }
        }
      }
    }


    //To track 2-ns shifts, plot CTOF_vtime - FTOF_vtime, Apr 2023
    public void fillCTOFFTOFTiming(DataBank part, DataBank sc, DataBank trk, DataBank ctof, DataBank cvttrk){
      int N_electrons_FD=0;
      int N_pim_CD=0;
      ArrayList<Double> FTOF_vtime = new ArrayList<Double>();
      ArrayList<Double> CTOF_vtime = new ArrayList<Double>();

      int sector = 0;
      for(int k=0;k<part.rows();k++) {
        int pid = part.getInt("pid",k);
        int status = part.getShort("status", k);
        if (status<0) status = -status;
        boolean inDC = (status>=2000 && status<4000);
        boolean inCVT = (status>=4000 && status < 8000);

        for (int j=0;j<sc.rows();j++) {
          if (sc.getShort("pindex",j)==k) {
            if (sc.getByte("detector",j)==12) {
              sector = sc.getInt("sector",j);
              double time = sc.getFloat("time", j);
              double pathlength = sc.getFloat("path",j);
              double flighttime = -10.0f;
              if (pid == 11 && inDC) {
                flighttime = pathlength/PhysicsConstants.speedOfLight();
                FTOF_vtime.add(time - flighttime);
                N_electrons_FD++;
              }
            }
            if (sc.getByte("detector",j)==4) {
              double t = -100.0f;
              double p = -100.0f;
              double mom_cvt = -100.0f;
              double beta_ctof;
              double ctof_vtime = -100.0;
              for (int iCTOF=0;iCTOF<ctof.rows();iCTOF++){
                int trackid = ctof.getInt("trkID",iCTOF);
                int iCVT = -1;
                for (int i = 0; i < cvttrk.rows(); i++) {
                  if (cvttrk.getShort("ID",i)==trackid) {
                    iCVT = i;
                    break;
                  }
                }
                if (iCTOF == sc.getShort("index",j)) {
                  if(!Float.isNaN(ctof.getFloat("energy",iCTOF)) && trackid>-1 && cvttrk.getInt("q", iCVT) < 0.){
                    t = ctof.getFloat("time",iCTOF);
                    p = ctof.getFloat("pathLength",iCTOF);
                    mom_cvt = cvttrk.getFloat("p",iCVT);
                    beta_ctof = mom_cvt/Math.sqrt(Math.pow(mom_cvt,2)+Math.pow(PhysicsConstants.massPionCharged(),2));
                    ctof_vtime = t - p/beta_ctof/PhysicsConstants.speedOfLight();
                  }
                }
              }
              if (pid == -211 && inCVT && ctof_vtime!=-100.) {
                CTOF_vtime.add(ctof_vtime);
                N_pim_CD++;
              }


              }
            }
            }
          }
          if (N_electrons_FD >= 1 && N_pim_CD >= 1 && sector != 0) {
            for (int n=0;n<N_electrons_FD;n++) {
              for (int nn=0;nn<N_pim_CD;nn++) {
                ftof_ctof_vtdiff[sector-1].fill(FTOF_vtime.get(n)-CTOF_vtime.get(nn));
              }
            }
          }

        }



        public void fillTOFHists(DataBank tofB, DataBank DCB){
          for(int r=0;r<tofB.rows();r++){
            float thisTime = tofB.getFloat("time", r) - tofB.getFloat("pathLength", r)/29.98f - RFTime;
            thisTime = (thisTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
            thisTime = thisTime - rfPeriod/2;
            float thisChi2 = 999;
            float thisMom = 0;
            float thisVz = 0;
            boolean foundTrk = false;
            for(int s=0;s<DCB.rows() && !foundTrk;s++){
              if(DCB.getInt("id",s) == tofB.getInt("trackid",r) ){
                thisChi2 = DCB.getFloat("chi2",s);
                thisMom=0;
                thisMom += DCB.getFloat("p0_x",s)*DCB.getFloat("p0_x",s);
                thisMom += DCB.getFloat("p0_y",s)*DCB.getFloat("p0_y",s);
                thisMom += DCB.getFloat("p0_z",s)*DCB.getFloat("p0_z",s);
                thisMom = (float)Math.sqrt(thisMom);
                thisVz = DCB.getFloat("Vtx0_z",s);
                // 200 MeV
                // 400 chi2
                // 30 cm vz
                if(thisChi2 < 500 && thisMom > 0.8 && Math.abs(thisVz)<15 )foundTrk = true;
              }
            }
          }
        }

        public void fillRFTime(DataBank RFB){
          for(int r=0;r<RFB.rows() && !hasRF;r++){
            if(RFB.getInt("id",r)==1){
              hasRF=true;
              RFTime = RFB.getFloat("time",r) + rfoffset1;
            }
          }
        }

        public int makeElectron(DataBank bank){
          int found_electron = 0;
          for(int k = 0; k < bank.rows(); k++){
            int pid = bank.getInt("pid", k);
            int status = bank.getShort("status", k);
            if (status<0) status = -status;
            boolean inDC = (status>=2000 && status<4000);
            if( inDC && pid == 11 && found_electron == 0){
              found_electron = 1;
              return k;
            }
          }
          return -1;
        }


        public void processEvent(DataEvent event) {
          hasRF = false;
          e_part_ind = -1;
          DataBank trackDetBank = null, hitBank = null, partBank = null, scintillator = null, scintextras = null;
          DataBank ctofhits = null, cvttrack = null, tofadc = null, toftdc = null, track = null, configbank = null;
          if(userTimeBased){
            if(event.hasBank("TimeBasedTrkg::TBTracks"))trackDetBank = event.getBank("TimeBasedTrkg::TBTracks");
            if(event.hasBank("TimeBasedTrkg::TBHits")){hitBank = event.getBank("TimeBasedTrkg::TBHits");}
            if(event.hasBank("REC::Particle"))partBank = event.getBank("REC::Particle");
            if(event.hasBank("REC::Scintillator"))scintillator = event.getBank("REC::Scintillator");
            if(event.hasBank("REC::Track"))track = event.getBank("REC::Track");
          }
          if(!userTimeBased){
            if(event.hasBank("HitBasedTrkg::HBTracks"))trackDetBank = event.getBank("HitBasedTrkg::HBTracks");
            if(event.hasBank("HitBasedTrkg::HBHits"))hitBank = event.getBank("HitBasedTrkg::HBHits");
            if(event.hasBank("RECHB::Particle"))partBank = event.getBank("RECHB::Particle");
            if(event.hasBank("RECHB::Scintillator"))scintillator = event.getBank("RECHB::Scintillator");
            if(event.hasBank("RECHB::Track"))track = event.getBank("REC::Track");
          }

          if(event.hasBank("REC::ScintExtras")) scintextras = event.getBank("REC::ScintExtras");
          if(event.hasBank("FTOF::adc")) tofadc = event.getBank("FTOF::adc");
          if(event.hasBank("FTOF::tdc")) toftdc = event.getBank("FTOF::tdc");
          if(event.hasBank("CTOF::hits")) ctofhits = event.getBank("CTOF::hits");
          if(event.hasBank("CVTRec::Tracks")) cvttrack = event.getBank("CVTRec::Tracks");

          if(event.hasBank("RUN::rf"))fillRFTime(event.getBank("RUN::rf"));
          if(event.hasBank("RUN::config")) {
            timestamp = event.getBank("RUN::config").getLong("timestamp",0);
            configbank = event.getBank("RUN::config");
          }
          if(!hasRF)return;
          if(partBank!=null) e_part_ind = makeElectron(partBank);
          if(event.hasBank("FTOF::hits") && trackDetBank!=null)fillTOFHists(event.getBank("FTOF::hits") , trackDetBank);
          if (partBank!=null && scintillator!=null && scintextras!=null && track!=null) fillTOFCalibHists(partBank,scintillator,scintextras,track);
          if (partBank!=null && scintillator!=null && track!=null && ctofhits!=null && cvttrack!=null) fillCTOFFTOFTiming(partBank,scintillator,track,ctofhits,cvttrack);

          if(userTimeBased && hitBank!=null && event.hasBank("RUN::config") && event.hasBank("REC::Particle") )fillDC(hitBank, configbank, partBank);
          if(toftdc!=null && tofadc!=null) fillTOFadctdcHists(tofadc,toftdc);
        }

        public void write() {
          TDirectory dirout = new TDirectory();
          dirout.mkdir("/tof/");
          dirout.cd("/tof/");
          for(int s=0;s<6;s++){
            dirout.addDataSet(p1a_dt_calib_all[s],p1b_dt_calib_all[s],p2_dt_calib_all[s],p2_edep[s]);
            dirout.addDataSet(p1a_tdcadc_dt[s], p1b_tdcadc_dt[s], p2_tdcadc_dt[s], ftof_ctof_vtdiff[s]);
            for (int i=0;i<3;i++) {
              dirout.addDataSet(p1a_edep[s][i],p1b_edep[s][i]);
            }
          }
          dirout.mkdir("/dc/");
          dirout.cd("/dc/");
          for(int s=0;s<6;s++)for(int sl=0;sl<6;sl++){
            dirout.addDataSet(DC_residuals_trkDoca[s][sl],DC_time[s][sl]);
            dirout.addDataSet(DC_time_even[s][sl],DC_time_odd[s][sl]);
            dirout.addDataSet(DC_residuals_trkDoca_rescut[s][sl]);
          }
          if(runNum>0) dirout.writeFile(outputDir+"/out_TOF_"+runNum+".hipo");
          else         dirout.writeFile(outputDir+"/out_TOF.hipo");
        }
}
