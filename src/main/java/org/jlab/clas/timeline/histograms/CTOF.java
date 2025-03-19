package org.jlab.clas.timeline.histograms;

import java.util.*;
import org.jlab.clas.pdg.PhysicsConstants;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.detector.base.DetectorType;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.ConstantsManager;

public class CTOF {
  boolean userTimeBased;
  public int runNum;
  public String outputDir;
  public int rf_large_integer;
  public boolean BackToBack;
  public float STT, RFT, MinCTOF, MaxCTOF, minSTT, maxSTT;
  public int NbinsCTOF;
  public float[] CTOF_shft;
  public double rfPeriod, rfoffset1, rfoffset2;
  public int e_part_ind;
  public int counter, counterm;

  public H2F H_CTOF_pos, H_CTOF_edep_phi, H_CTOF_edep_z, H_CTOF_path_mom;
  public H2F H_CTOF_edep_pad_neg, H_CTOF_edep_pad_pos;
  public H1F H_CTOF_tdcadc_dt;
  public H2F H_vz_DC_CVT, H_phi_DC_CVT, H_CVT_CTOF_phi, H_CVT_CTOF_z, H_CVT_t_STT, H_CVT_t_pad;
  public H1F[] H_CVT_t;
  public H1F H_CVT_t_pos, H_CVT_t_neg;

  public float CTOF_counter_thickness;
  public int phase_offset;
  public long timestamp;
  public int evn;

  //for timeline
  public H1F H_CTOF_pos_mass, H_CTOF_neg_mass;
  public H2F H_CTOF_vt_pim;
  public H1F H_CTOF_edep_pim;
  public H1F[] H_CTOF_edep_neg;

  public IndexedTable InverseTranslationTable;
  public IndexedTable calibrationTranslationTable;
  public IndexedTable rfTable, rfTableOffset;
  public IndexedTable targetTable;
  public double targetPos;

  public ConstantsManager ccdb;

  public CTOF(int reqrunNum, String reqOutputDir, boolean reqTimeBased) {
    runNum = reqrunNum;userTimeBased=reqTimeBased;
    outputDir = reqOutputDir;
    counter = 0;
    counterm = 0;

    CTOF_counter_thickness = 3.0f; //cm
    phase_offset = 3; //RGA Fall 2018, RGB Spring 2019, RGA Spring 2019
    rfPeriod = 4.008;
    ccdb = new ConstantsManager();
    ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo","/calibration/eb/rf/config","/calibration/eb/rf/offset","/geometry/target"}));
    rfTable = ccdb.getConstants(runNum,"/calibration/eb/rf/config");
    if (rfTable.hasEntry(1, 1, 1)){
      System.out.println(String.format("RF period from ccdb for run %d: %f",runNum,rfTable.getDoubleValue("clock",1,1,1)));
      rfPeriod = rfTable.getDoubleValue("clock",1,1,1);
    }
    rf_large_integer = 1000;
    rfTableOffset = ccdb.getConstants(runNum,"/calibration/eb/rf/offset");
    if (rfTableOffset.hasEntry(1, 1, 1)){
      rfoffset1 = (float)rfTableOffset.getDoubleValue("offset",1,1,1);
      rfoffset2 = (float)rfTableOffset.getDoubleValue("offset",1,1,2);
      System.out.println(String.format("RF1 offset from ccdb for run %d: %f",runNum,rfoffset1));
      System.out.println(String.format("RF2 offset from ccdb for run %d: %f",runNum,rfoffset2));
    }

    targetTable = ccdb.getConstants(runNum,"/geometry/target");
    // targetPos = (float) targetTable.getDoubleValue("position",0,0,0);
    targetPos = -3.0; //for some reasons, targetPos from ccdb is just 0.

    CTOF_shft = new float[]{0.00f , -300.51f , -297.72f , -295.05f , -301.03f , -298.19f , -295.41f , -299.74f , -298.30f , -295.21f ,
      -300.22f , -297.35f , -295.28f , -299.14f , 0.00f , -294.51f , -298.80f , -298.30f , -295.49f , -298.81f ,
      -297.23f , -294.44f , -298.96f , -296.27f , -295.30f , -298.74f , -296.44f , -293.98f , -299.07f , -296.08f ,
      -293.52f , -297.96f , -296.42f , -294.40f , -298.90f , -297.14f , -295.28f , -299.03f , -297.71f , -294.35f ,
      -298.81f , -296.75f , -293.77f , -298.96f , -297.65f , -294.67f , -299.73f , -297.76f , -295.87f , -297.10f};//2476


    H_CTOF_edep_neg = new H1F[49];
    for(int p=0;p<48;p++){
      H_CTOF_edep_neg[p] = new H1F(String.format("PathLCorrected Edep_p%d",p+1),String.format("PathLCorrected Edep_p%d",p+1),150,0.,30.);
      H_CTOF_edep_neg[p].setTitleX("E (MeV)");
      H_CTOF_edep_neg[p].setTitleY("counts");
    }
    H_CTOF_edep_neg[48] = new H1F("PathLCorrected Edep","PathLCorrected Edep",150,0.,30.);
    H_CTOF_edep_neg[48].setTitleX("E (MeV)");
    H_CTOF_edep_neg[48].setTitleY("counts");

    H_CTOF_pos = new H2F("H_CTOF_pos","H_CTOF_pos",50,-180,180,100,-10,5);
    //H_CTOF_pos = new H2F("H_CTOF_pos","H_CTOF_pos",50,-180,180,100,-20,10);
    H_CTOF_pos.setTitle("CTOF hits z vs #phi");
    H_CTOF_pos.setTitleX("#phi (^o)");
    H_CTOF_pos.setTitleY("z/10 (cm)");
    H_CTOF_edep_phi = new H2F("H_CTOF_edep_phi","H_CTOF_edep_phi",50,-180,180,100,0,30);
    H_CTOF_edep_phi.setTitle("CTOF PathLCorrected Edep vs #phi");
    H_CTOF_edep_phi.setTitleX("#phi (^o)");
    H_CTOF_edep_phi.setTitleY("E (MeV)");
    H_CTOF_edep_pad_pos = new H2F("H_CTOF_edep_phi","H_CTOF_edep_phi",50,0.5,50.5,100,0,30);
    H_CTOF_edep_pad_pos.setTitle("CTOF PathLCorrected Edep vs pad, pos. tracks");
    H_CTOF_edep_pad_pos.setTitleX("pad");
    H_CTOF_edep_pad_pos.setTitleY("E (MeV)");
    H_CTOF_edep_pad_neg = new H2F("H_CTOF_edep_phi","H_CTOF_edep_phi",50,0.5,50.5,100,0,30);
    H_CTOF_edep_pad_neg.setTitle("CTOF PathLCorrected Edep vs pad, neg. tracks");
    H_CTOF_edep_pad_neg.setTitleX("pad");
    H_CTOF_edep_pad_neg.setTitleY("E (MeV)");
    H_CTOF_edep_z = new H2F("H_CTOF_edep_z","H_CTOF_edep_z",100,-10,5,100,0,30);
    H_CTOF_edep_z.setTitle("CTOF PathLCorrected Edep vs z");
    H_CTOF_edep_z.setTitleX("z/10 (cm)");
    H_CTOF_edep_z.setTitleY("E (MeV)");
    H_CTOF_tdcadc_dt = new H1F("CTOF TDC-ADC Time Difference","CTOF TDC-ADC Time Difference",4750,-10.,180.);
    H_CTOF_tdcadc_dt.setTitle("CTOF TDC_time-ADC_time");
    H_CTOF_tdcadc_dt.setTitleX("Delta_t (ns)");
    H_CTOF_tdcadc_dt.setTitleY("counts");
    H_vz_DC_CVT = new H2F("H_vz_DC_CVT","H_vz_DC_CVT",100,-20,20,100,-20,20);
    H_vz_DC_CVT.setTitle("CVT vz vs DC vz");
    H_vz_DC_CVT.setTitleX("DC vz (cm)");
    H_vz_DC_CVT.setTitleY("CVT vz (cm)");
    H_phi_DC_CVT = new H2F("H_phi_DC_CVT","H_phi_DC_CVT",100,-180,180,100,-180,180);
    H_phi_DC_CVT.setTitle("CVZ #phi vs DC #phi");
    H_phi_DC_CVT.setTitleX("DC #phi (^o)");
    H_phi_DC_CVT.setTitleY("CVT #phi (^o)");
    H_CTOF_path_mom = new H2F("H_CTOF_path_mom","H_CTOF_path_mom",100,0,2,100,20,60);
    H_CTOF_path_mom.setTitle("CTOF path vs mom");
    H_CTOF_path_mom.setTitleX("p (GeV)");
    H_CTOF_path_mom.setTitleY("CTOF path (cm)");
    H_CVT_CTOF_phi = new H2F("H_CVT_CTOF_phi","H_CVT_CTOF_phi",100,-180,180,50,-180,180);
    H_CVT_CTOF_phi.setTitle("CVT CTOF #Delta #phi");
    H_CVT_CTOF_phi.setTitleX("CVT #phi (^o)");
    H_CVT_CTOF_phi.setTitleY("CTOF #phi (^o)");
    H_CVT_CTOF_z = new H2F("H_CVT_CTOF_z","H_CVT_CTOF_z",100,-10,5,100,-10,5);
    H_CVT_CTOF_z.setTitle("CVT CTOF #Delta z");
    H_CVT_CTOF_z.setTitleX("CVT z/10 (cm)");
    H_CVT_CTOF_z.setTitleY("CTOF z/10 (cm)");
    minSTT = 100;maxSTT=200;
    if(runNum>0&&runNum<3210){
      minSTT = 540;maxSTT=590;
    }
    NbinsCTOF = 400; // 25 ps per bin
    MinCTOF   = -5;
    MaxCTOF   = 5;
    H_CVT_t_STT = new H2F("H_CVT_t_STT","H_CVT_t_STT",100,0.,maxSTT,100,0.,maxSTT+(MinCTOF+MaxCTOF)/2);
    H_CVT_t_STT.setTitle("CTOF vertex t vs Pion vertex t, neg. tracks");
    H_CVT_t_STT.setTitleX("Pion vertex t (ns)");
    H_CVT_t_STT.setTitleY("CTOF vertex t (ns)");
    try {
      Thread.sleep(5000);// in ms
    }catch (Exception e) {
      System.out.println(e);
    }
    System.out.println("CTOF range "+MinCTOF+" to "+MaxCTOF);
    H_CVT_t_pad = new H2F("H_CVT_t_pad","H_CVT_t_pad",50,0.5,50.5,NbinsCTOF,MinCTOF,MaxCTOF);
    H_CVT_t_pad.setTitle("CTOF vertex t - Pion vertex t  vs pad, neg. tracks");
    H_CVT_t_pad.setTitleX("pad");
    H_CVT_t_pad.setTitleY("CTOF vertex t - Pion vertex t (ns)");
    H_CVT_t = new H1F[49];
    for(int p=0;p<49;p++){
      H_CVT_t[p] = new H1F(String.format("H_CVT_t_p%d",p+1),String.format("H_CVT_t_p%d",p+1),NbinsCTOF,MinCTOF,MaxCTOF);
      H_CVT_t[p].setTitle(String.format("pad %d CTOF vertex t - STT, neg. tracks",p+1));
      H_CVT_t[p].setTitleX("Delta_t (ns)");
    }
    H_CVT_t[48].setTitle("all pad, CTOF vertex t - STT, neg. tracks");
    H_CVT_t_pos = new H1F("H_CVT_t_pos","H_CVT_t_pos",NbinsCTOF,MinCTOF,MaxCTOF);
    H_CVT_t_pos.setTitle("All CTOF pads, CTOF vertex t - Pion vertex t, pos. tracks");
    H_CVT_t_pos.setTitleX("CTOF vertex t - Pion vertex t (ns)");

    H_CVT_t_neg = new H1F("H_CVT_t_neg","H_CVT_t_neg",NbinsCTOF,MinCTOF,MaxCTOF);
    H_CVT_t_neg.setTitle("All CTOF pads, CTOF vertex t - Pion vertex t, neg. tracks");
    H_CVT_t_neg.setTitleX("CTOF vertex t - Pion vertex t (ns)");

    H_CTOF_pos_mass = new H1F("H_CTOF_pos_mass","H_CTOF_pos_mass",100,-0.5,3.5);
    H_CTOF_pos_mass.setTitle("pos Mass^2");
    H_CTOF_pos_mass.setTitleX("M^2 (GeV^2)");
    H_CTOF_neg_mass = new H1F("H_CTOF_neg_mass","H_CTOF_neg_mass",100,-0.5,2.0);
    H_CTOF_neg_mass.setTitle("neg Mass^2");
    H_CTOF_neg_mass.setTitleX("M^2 (GeV^2)");
    H_CTOF_vt_pim = new H2F("H_CTOF_vt_pim","H_CTOF_vt_pim",100,-rfPeriod/2,rfPeriod/2,NbinsCTOF,MinCTOF,MaxCTOF);
    H_CTOF_vt_pim.setTitle("CTOF MIP (pi-) vertex time");
    H_CTOF_vt_pim.setTitleX("vertex time - RFTime (ns)");
    H_CTOF_vt_pim.setTitleY("vertex time - STTime (ns)");
    H_CTOF_edep_pim = new H1F("H_CTOF_edep_pim","H_CTOF_edep_pim",100,0,150);
    H_CTOF_edep_pim.setTitle("CTOF MIP (pi-) PathLCorrected Edep");
    H_CTOF_edep_pim.setTitleX("E (MeV)");
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
        double cx   = CVTbank.getFloat("c_x",iCVT);
        double cy   = CVTbank.getFloat("c_y",iCVT);
        double cz   = CVTbank.getFloat("c_z",iCVT);
        double cphi = Math.toDegrees(Math.atan2(cy, cx));
        int    charge = CVTbank.getInt("q",iCVT);
        double chi2   = CVTbank.getFloat("chi2", iCVT)/CVTbank.getShort("ndf", iCVT);
        double phi0   = Math.toDegrees(CVTbank.getFloat("phi0", iCVT));
        double z0     = CVTbank.getFloat("z0", iCVT);
        double beta   = mom/Math.sqrt(mom*mom+Math.pow(PhysicsConstants.massPionCharged(),2));
        double path   = CVTbank.getFloat("pathlength", iCVT);

        double CTOFbeta  = path/(PhysicsConstants.speedOfLight()*(time-vt));
        double CTOFmass  = mom * mom * ( 1/(CTOFbeta*CTOFbeta) - 1);
        double CTOFedep  = energy*CTOF_counter_thickness/pathlthroughbar;
        double CTOFvtime = time - path/beta/PhysicsConstants.speedOfLight();
        double CTOFdtime = vt - CTOFvtime;

        H_CVT_CTOF_phi.fill(cphi, phi0);
        H_CVT_CTOF_z.fill(cz, z0);

        if (charge < 0 && e_part_ind != -1) {
          H_CTOF_pos.fill(phi0, z0);
          H_CTOF_edep_phi.fill(phi0, CTOFedep);
          H_CTOF_edep_z.fill(z0, CTOFedep);
          H_CTOF_path_mom.fill(mom, path);
          H_CTOF_edep_pad_neg.fill(paddle, CTOFedep);
          //Signed off by Dan on 29 April 2020;
          if (mom > 0.4 && mom<3.0 && chi2 < 30) {
            H_CVT_t_pad.fill(paddle, CTOFdtime);
            //This is the CTOF vertex time difference histogram to be fitted and timelined
            H_CVT_t_neg.fill(CTOFdtime);
            H_CVT_t[paddle - 1].fill(CTOFdtime);
            H_CVT_t[48].fill(CTOFdtime);
            H_CVT_t_STT.fill(vt, CTOFvtime);
          }
          H_CTOF_neg_mass.fill(CTOFmass);
          H_CTOF_edep_neg[paddle - 1].fill(CTOFedep);
          H_CTOF_edep_neg[48].fill(CTOFedep);
          //pi- fiducial cut borrowing from Pierre's CND
          //						if (Math.sqrt(Math.abs(CTOFmass))<0.38 && CTOFmass>-0.35*0.35){
          double thisTime = CTOFdtime - RFT;
          thisTime = (thisTime + (rf_large_integer + 0.5) * rfPeriod) % rfPeriod;
          thisTime = thisTime - rfPeriod / 2;
          H_CTOF_vt_pim.fill(thisTime, CTOFdtime);
          H_CTOF_edep_pim.fill(CTOFedep);
          //						}			
        }
        if (charge > 0 && e_part_ind != -1) {
          H_CTOF_edep_pad_pos.fill(paddle, CTOFedep);
          if (mom > 0.4 && mom<3.0  && chi2 < 30) {
            H_CVT_t_pos.fill(CTOFdtime);
          }
          H_CTOF_pos_mass.fill(CTOFmass);
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
    dirout.addDataSet(H_CVT_t_pad,H_CTOF_edep_phi);
    for(int p=0;p<49;p++){
      dirout.addDataSet(H_CVT_t[p]);
      dirout.addDataSet(H_CTOF_edep_neg[p]);
    }
    dirout.addDataSet(H_CVT_t_pos, H_CVT_t_neg);
    dirout.addDataSet(H_CTOF_pos_mass, H_CTOF_neg_mass, H_CTOF_vt_pim, H_CTOF_edep_pim);
    dirout.addDataSet(H_CVT_t_neg,H_CTOF_tdcadc_dt);

    if(runNum>0) dirout.writeFile(outputDir+"/out_CTOF_"+runNum+".hipo");
    else         dirout.writeFile(outputDir+"/out_CTOF.hipo");
  }   

}
