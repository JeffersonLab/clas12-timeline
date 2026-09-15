package org.jlab.clas.timeline.histograms;

import org.jlab.clas.pdg.PhysicsConstants;

import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.detector.base.DetectorType;

// Issue 510: Removed unused histograms to save only called by timeline step.
// For the full version, please refer to the previous version, such as e1b4bf2d0f70ade26bf70f67bab26554a62e6511.

public class CTOF {
  boolean userTimeBased;
  public int runNum;
  public String outputDir;
  public int rf_large_integer;
  public boolean BackToBack;
  public float STT, RFT, MinCTOF, MaxCTOF, minSTT, maxSTT;
  public int NbinsCTOF;
  public double rfPeriod, rfoffset1, rfoffset2;
  public int e_part_ind;
  public int counter, counterm;

  public float CTOF_counter_thickness;
  public int phase_offset;
  public long timestamp;
  public int evn;

  public H1F H_CTOF_tdcadc_dt;     // related timeline: ['ctof_tdcadc']
  public H1F H_CVT_t_neg;          // related timeline: ['ctof_time']
  public H1F[] H_CTOF_edep_neg;    // related timeline: ['ctof_edep']

  public CTOF(int reqrunNum, String reqOutputDir, boolean reqTimeBased) {
    runNum = reqrunNum;userTimeBased=reqTimeBased;
    outputDir = reqOutputDir;
    counter = 0;
    counterm = 0;

    CTOF_counter_thickness = 3.0f; //cm
    phase_offset = 3; //RGA Fall 2018, RGB Spring 2019, RGA Spring 2019

    H_CTOF_edep_neg = new H1F[49];
    for(int p=0;p<48;p++){
      H_CTOF_edep_neg[p] = new H1F(String.format("PathLCorrected Edep_p%d",p+1),String.format("PathLCorrected Edep_p%d",p+1),150,0.,30.);
      H_CTOF_edep_neg[p].setTitleX("E (MeV)");
      H_CTOF_edep_neg[p].setTitleY("counts");
    }
    H_CTOF_edep_neg[48] = new H1F("PathLCorrected Edep","PathLCorrected Edep",150,0.,30.);
    H_CTOF_edep_neg[48].setTitleX("E (MeV)");
    H_CTOF_edep_neg[48].setTitleY("counts");

    H_CTOF_tdcadc_dt = new H1F("CTOF TDC-ADC Time Difference","CTOF TDC-ADC Time Difference",4750,-10.,180.);
    H_CTOF_tdcadc_dt.setTitle("CTOF TDC_time-ADC_time");
    H_CTOF_tdcadc_dt.setTitleX("Delta_t (ns)");
    H_CTOF_tdcadc_dt.setTitleY("counts");

    minSTT = 100;maxSTT=200;
    if(runNum>0&&runNum<3210){
      minSTT = 540;maxSTT=590;
    }
    NbinsCTOF = 400; // 25 ps per bin
    MinCTOF   = -5;
    MaxCTOF   = 5;
    try {
      Thread.sleep(5000);// in ms
    }catch (Exception e) {
      System.out.println(e);
    }
    System.out.println("CTOF range "+MinCTOF+" to "+MaxCTOF);

    H_CVT_t_neg = new H1F("H_CVT_t_neg","H_CVT_t_neg",NbinsCTOF,MinCTOF,MaxCTOF);
    H_CVT_t_neg.setTitle("All CTOF pads, CTOF vertex t - Pion vertex t, neg. tracks");
    H_CVT_t_neg.setTitleX("CTOF vertex t - Pion vertex t (ns)");
  }

  public void FillCVTCTOF(DataBank CVTbank, DataBank CTOFbank, DataBank partBank, DataBank trackBank){
    for(int iCTOF=0;iCTOF<CTOFbank.rows();iCTOF++){
      double energy          = CTOFbank.getFloat("energy", iCTOF);
      double time            = CTOFbank.getFloat("time", iCTOF);
      int    paddle          = CTOFbank.getShort("component", iCTOF);
      double pathlthroughbar = CTOFbank.getFloat("pathLengthThruBar", iCTOF);
      int trackid            = CTOFbank.getShort("trkID", iCTOF);
      if(trackid<=0) continue;

      // Implemented from Calibration Suite
      // Find the matching CVTRec::Tracks bank
      int iCVT = -1;
      for (int i = 0; i < CVTbank.rows(); i++) {
        if (CVTbank.getShort("ID",i) == trackid) {
          iCVT = i;
          break;
        }
      }
      // Find the pindex
      int pindex = -1;
      double vt  = -999;
      for (int i = 0; i < trackBank.rows(); i++) {
        int detector = trackBank.getByte("detector",i);
        int index    = trackBank.getShort("index",i);
        if(detector == DetectorType.CVT.getDetectorId() && index == iCVT) {
          pindex = trackBank.getShort("pindex", i);
          vt     = partBank.getFloat("vt", pindex);
          break;
        }
      }

      if(energy>0.5){
        double mom  = CVTbank.getFloat("p",iCVT);
        int    charge = CVTbank.getInt("q",iCVT);
        double chi2   = CVTbank.getFloat("chi2", iCVT)/CVTbank.getShort("ndf", iCVT);
        double beta   = mom/Math.sqrt(mom*mom+Math.pow(PhysicsConstants.massPionCharged(),2));
        double path   = CVTbank.getFloat("pathlength", iCVT);

        double CTOFedep  = energy*CTOF_counter_thickness/pathlthroughbar;
        double CTOFvtime = time - path/beta/PhysicsConstants.speedOfLight();
        double CTOFdtime = vt - CTOFvtime;

        if (charge < 0 && e_part_ind != -1) {
          //Cross-checked with Daniel S. Carman on 29 April 2020;
          if (mom > 0.4 && mom<3.0 && chi2 < 30) {
            //This is the CTOF vertex time difference histogram to be fitted and timelined
            H_CVT_t_neg.fill(CTOFdtime);
          }
          H_CTOF_edep_neg[paddle - 1].fill(CTOFedep);
          H_CTOF_edep_neg[48].fill(CTOFedep);
        }
      }
    }
  }

  public void fillCTOFadctdcHist(DataBank ctofadc, DataBank ctoftdc) {
    for (int r=0;r<ctoftdc.rows();r++) {
      int sector_tdc = ctoftdc.getInt("sector",r);
      int layer_tdc = ctoftdc.getInt("layer",r);
      int component_tdc = ctoftdc.getInt("component",r);
      int order = ctoftdc.getByte("order",r)-2;
      int tdc_pmt = (component_tdc-1)*2+order+1;
      int TDC = ctoftdc.getInt("TDC",r);
      for (int j=0;j<ctofadc.rows();j++) {
        int sector_adc = ctofadc.getInt("sector",j);
        int layer_adc = ctofadc.getInt("layer",j);
        int component_adc = ctofadc.getInt("component",j);
        int order_adc = ctofadc.getByte("order",j);
        int adc_pmt = (component_adc-1)*2+order_adc+1;
        float time_adc = ctofadc.getFloat("time",j);
        if (sector_adc == sector_tdc && layer_adc == layer_tdc && component_adc == component_tdc && adc_pmt == tdc_pmt) {
          int triggerPhaseTOF = (int) ((timestamp + phase_offset)%6);
          float time_tdc = (float)TDC*0.02345f - (float)triggerPhaseTOF*4.f;
          float time_diff = time_tdc - time_adc;
          H_CTOF_tdcadc_dt.fill(time_diff);
        }
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
    BackToBack = false;
    e_part_ind=-1;
    DataBank eventBank = null, partBank = null, trackBank = null;
    DataBank tofadc = null, toftdc = null;
    if(userTimeBased){
      if(event.hasBank("REC::Event"))eventBank = event.getBank("REC::Event");
      if(event.hasBank("REC::Particle"))partBank = event.getBank("REC::Particle");
      if(event.hasBank("REC::Track"))trackBank = event.getBank("REC::Track");
    }
    if(!userTimeBased){
      if(event.hasBank("RECHB::Event"))eventBank = event.getBank("RECHB::Event");
      if(event.hasBank("RECHB::Particle"))partBank = event.getBank("RECHB::Particle");
      if(event.hasBank("RECHB::Track"))trackBank = event.getBank("RECHB::Track");
    }

    if(eventBank!=null)STT = eventBank.getFloat("startTime",0);
    if(eventBank!=null)RFT = eventBank.getFloat("RFTime",0); else return;
    if(event.hasBank("RUN::config")){
      evn = event.getBank("RUN::config").getInt("event",0);
      timestamp = event.getBank("RUN::config").getLong("timestamp",0);
    }
    if(event.hasBank("CTOF::adc")) tofadc = event.getBank("CTOF::adc");
    if(event.hasBank("CTOF::tdc")) toftdc = event.getBank("CTOF::tdc");
    if(partBank!=null) e_part_ind = makeElectron(partBank);
    if(event.hasBank("CVTRec::Tracks") && event.hasBank("CTOF::hits") && partBank!=null && trackBank!=null) FillCVTCTOF(event.getBank("CVTRec::Tracks"),event.getBank("CTOF::hits"), partBank, trackBank);
    if(toftdc!=null && tofadc!=null) fillCTOFadctdcHist(tofadc,toftdc);
  }

  public void write(){
    TDirectory dirout = new TDirectory();
    dirout.mkdir("/ctof/");
    dirout.cd("/ctof/");
    for(int p=0;p<49;p++){
      dirout.addDataSet(H_CTOF_edep_neg[p]);
    }
    dirout.addDataSet(H_CVT_t_neg, H_CTOF_tdcadc_dt);

    if(runNum>0) dirout.writeFile(outputDir+"/out_CTOF_"+runNum+".hipo");
    else         dirout.writeFile(outputDir+"/out_CTOF.hipo");
  }   

}
