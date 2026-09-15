package org.jlab.clas.timeline.histograms;
import java.util.*;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.clas.physics.Vector3;
import org.jlab.clas.physics.LorentzVector;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.clas.timeline.util.RunDependentCut;

// Issue 510: Removed unused histograms to save only called by timeline step.
// For the full version, please refer to the previous version, such as e1b4bf2d0f70ade26bf70f67bab26554a62e6511.

/**
 * General Monitoring histograms (a.k.a. General Monolith)
 */
public class GeneralMon {

  /**
   * Assume 1+6 bit groupings, one for each bit in the given
   * offsets, and determine whether the given sector triggered.
   * @param trigger event trigger word
   * @param sector CLAS12 sector in question
   * @param offsets mask of electron bit group offsets
   * @return whether the given sector triggered 
   */
  static boolean testTriggerSector(long trigger, int sector, long offsets) {
    for (int i=0; i<32-6; i++) {
      if (0 != (offsets & (1<<i)) ) {
        if (0 != (trigger & ( 1 << (i+sector)))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Determine whether the given sector triggered.
   * @param sector CLAS sector in question
   * @return whether the given sector triggered 
   */
  boolean testTriggerSector(int sector) {
    // FIXME:  move to CCDB

    if (RunDependentCut.findDataset(runNum) == "rgd") {
      // RG-D:   used three different primary electron triggers (0/7/14):
      return testTriggerSector(TriggerWord, sector, 0x4081);
    }
    if (RunDependentCut.runIsInRange(runNum, 16043, 16078, false)) {
      // RG-C 2.2 GeV:  non-standard primary electron trigger:
      return testTriggerSector(TriggerWord, sector, 0x4081);
    }
    if (RunDependentCut.runIsInRange(runNum, 22731, 23016, false)) {
      // RG-L 2.2 GeV: no sector specific trigger bit is set
      return true;
    }
    // Default:  the primary electron trigger is the first (7) bits:
    return testTriggerSector(TriggerWord, sector, 0x1);
  }

  boolean userTimeBased;
  int Nevts, Nelecs, Ntrigs, runNum;
  public String outputDir;
  public float rfPeriod, rfoffset1, rfoffset2;
  public int rf_large_integer;
  boolean[] trigger_bits;
  public float EB, Ebeam;
  public float RFtime1, RFtime2, startTime, BCG;
  public long TriggerWord;
  public float STT;
  public int trig_part_ind, trig_sect, trig_track_ind;
  public int trig_muon_sect;
  public int e_part_ind, e_sect, e_track_ind, hasLTCC, ngammas, pip_part_ind, pip_track_ind, pip_sect, pim_part_ind, pim_track_ind, pim_sect, foundCVT, CVTcharge;
  public int found_eTraj, found_eHTCC;
  public float e_ecal_T_PCAL, e_ecal_T_ECIN, e_ecal_T_ECOU;
  public float e_mom, e_theta, e_phi, e_vx, e_vy, e_vz, e_ecal_X, e_ecal_Y, e_ecal_Z, e_ecal_E, e_track_chi2, e_vert_time, e_vert_time_RF, e_Q2;
  public float e_HTCC, e_LTCC, e_pcal_e, e_etot_e, e_TOF_X, e_TOF_Y, e_TOF_Z, e_HTCC_X, e_HTCC_Y, e_HTCC_Z, e_HTCC_tX, e_HTCC_tY, e_HTCC_tZ, e_HTCC_nphe;
  public float g1_e, g1_theta, g1_phi, g2_e, g2_theta, g2_phi;
  public float pip_mom, pip_theta, pip_phi, pip_vx, pip_vy, pip_vz, pip_vert_time, pip_beta, pip_track_chi2;
  public float pim_mom, pim_theta, pim_phi, pim_vx, pim_vy, pim_vz, pim_vert_time, pim_beta, pim_track_chi2;
  public float CVT_mom, CVT_theta, CVT_phi, CVT_vz, CVT_chi2, CVT_pathlength;
  public int CVT_ndf;
  public LorentzVector VB, VT, Ve, VG1, VG2, VPI0, VPIP, VPIM;

  public H1F H_trig_sector_count;          // used in ratio_to_trigger()
  public H1F H_muon_trig_sector_count;     // used in ratio_to_trigger()

  public H1F[] H_trig_PCAL_vt_S;           // related timeline: ['ec_pcal_time']
  public H1F[] H_trig_ECIN_vt_S;           // related timeline: ['ec_ecin_time']
  public H1F[] H_trig_ECOU_vt_S;           // related timeline: ['ec_ecou_time']
  public H2F[] H_trig_vz_mom_S;            // related timeline: ['forward_Tracking_EleVz']
  public H2F[] H_trig_ECALsampl_S;         // related timeline: ['ec_Sampl']
  public H2F[] H_trig_LTCCn_theta_S;       // related timeline: ['ltcc_nphe_sector']
  public H1F[] H_dcm_vz;                   // related timeline: ['forward_Tracking_NegVz']
  public H1F[] H_dcm_chi2;                 // related timeline: ['forward_Tracking_Negchi2', 'forward_Tracking_Poschi2']
  public H1F[] H_dcp_vz;                   // related timeline: ['forward_Tracking_PosVz']
  public H1F[] H_dce_chi2;                 // related timeline: ['forward_Tracking_Elechi2']
  public H1F H_gg_m;                       // related timeline: ['ec_gg_m']
  public H1F H_CVT_chi2;                   // related timeline: ['cvt_chi2_elec']
  public H1F H_CVT_z_pos;                  // related timeline: ['cvt_Vz_positive']
  public H1F H_CVT_z_neg;                  // related timeline: ['cvt_Vz_negative']
  public H1F H_CVT_d0_pos;                 // related timeline: ['cvt_d0_mean_pos', 'cvt_d0_sigma_pos']
  public H1F H_CVT_absd0_pos;              // related timeline: ['cvt_d0_max_pos']
  public H1F H_CVT_chi2_pos;               // related timeline: ['cvt_chi2_pos']
  public H1F H_CVT_chi2_neg;               // related timeline: ['cvt_chi2_neg']
  public H1F H_trig_sector_elec_rat;       // related timeline: ['rat_elec_num']
  public H1F H_trig_sector_muon_rat;       // related timeline: ['rat_muon_num']
  public H1F H_trig_sector_prot_rat;       // related timeline: ['rat_prot_num']
  public H1F H_trig_sector_piplus_rat;     // related timeline: ['rat_pip_num']
  public H1F H_trig_sector_piminus_rat;    // related timeline: ['rat_pim_num']
  public H1F H_trig_sector_kplus_rat;      // related timeline: ['rat_Kp_num']
  public H1F H_trig_sector_kminus_rat;     // related timeline: ['rat_Km_num']
  public H1F H_trig_sector_positive_rat;   // related timeline: ['rat_pos_num']
  public H1F H_trig_sector_negative_rat;   // related timeline: ['rat_neg_num']
  public H1F H_trig_sector_neutral_rat;    // related timeline: ['rat_neu_num']
  public H1F[] H_e_RFtime1_FD_S;           // related timeline: ['rftime_elec_FD']
  public H1F[] H_pip_RFtime1_FD_S;         // related timeline: ['rftime_pip_FD']
  public H1F[] H_pim_RFtime1_FD_S;         // related timeline: ['rftime_pim_FD']
  public H1F[] H_p_RFtime1_FD_S;           // related timeline: ['rftime_prot_FD']
  public H1F H_pip_RFtime1_CD;             // related timeline: ['rftime_pip_CD']
  public H1F H_pim_RFtime1_CD;             // related timeline: ['rftime_pim_CD']
  public H1F H_p_RFtime1_CD;               // related timeline: ['rftime_prot_CD']
  public H1F H_RFtimediff;                 // related timeline: ['rftime_diff']
  public H1F H_RFtimediff_corrected;       // related timeline: ['rftime_diff_corrected']
  public H1F hbstOccupancy;                // related timeline: ['bst_Occupancy']
  public H1F hbmtOccupancy;                // related timeline: ['bmt_Occupancy']
  public H1F htrks;                        // related timeline: ['cvt_trks']
  public H1F hpostrks;                     // related timeline: ['cvt_trks_pos']
  public H1F hnegtrks;                     // related timeline: ['cvt_trks_neg']
  public H1F hndf;                         // related timeline: ['cvt_ndf']
  public H1F hchi2norm;                    // related timeline: ['cvt_chi2norm']
  public H1F hp;                           // related timeline: ['cvt_p']
  public H1F hpt;                          // related timeline: ['cvt_pt']
  public H1F hpathlen;                     // related timeline: ['cvt_pathlen']
  public H1F hbstOnTrkLayers;              // related timeline: ['bst_OnTrkLayers']
  public H1F hbmtOnTrkLayers;              // related timeline: ['bmt_OnTrkLayers']
  public H1F hpostrks_rat;                 // related timeline: ['cvt_trks_pos_rat']
  public H1F hnegtrks_rat;                 // related timeline: ['cvt_trks_neg_rat']
  public H1F H_trig_central_prot_rat;      // related timeline: ['central_prot_num']
  public H1F H_trig_central_piplus_rat;    // related timeline: ['central_pip_num']
  public H1F H_trig_central_piminus_rat;   // related timeline: ['central_pim_num']
  public H1F H_trig_central_kplus_rat;     // related timeline: ['central_Kp_num']
  public H1F H_trig_central_kminus_rat;    // related timeline: ['central_Km_num']

  public IndexedTable InverseTranslationTable;
  public IndexedTable calibrationTranslationTable;
  public IndexedTable rfTable, rfTableOffset;
  public ConstantsManager ccdb;

  public GeneralMon(int reqrunNum, String reqOutputDir, float reqEB, boolean reqTimeBased) {

    runNum = reqrunNum;EB=reqEB;userTimeBased=reqTimeBased;
    outputDir = reqOutputDir;
    Nevts=0;Nelecs=0;Ntrigs=0;
    found_eTraj = 0;
    found_eHTCC = 0;
    trigger_bits = new boolean[32];
    Ebeam = EB;
    System.out.println("Beam energy = "+Ebeam);
    String choiceTracking = " warning! Unspecified tracking";
    if(userTimeBased)choiceTracking=" using TIME BASED tracking";
    if(!userTimeBased)choiceTracking=" using HIT BASED tracking";
    System.out.println("Eb="+Ebeam+" (EB="+EB+") , run="+runNum+" , "+choiceTracking);
    try {
      Thread.sleep(10000);// in ms
    }catch (Exception e) {
      System.out.println(e);
    }


    rfPeriod = 4.008f;
    ccdb = new ConstantsManager();
    ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo", "/calibration/eb/rf/config", "/calibration/eb/rf/offset"}));
    rfTable = ccdb.getConstants(runNum, "/calibration/eb/rf/config");
    if (rfTable.hasEntry(1, 1, 1)) {
      System.out.println(String.format("RF period from ccdb for run %d: %f", runNum, rfTable.getDoubleValue("clock", 1, 1, 1)));
      rfPeriod = (float) rfTable.getDoubleValue("clock", 1, 1, 1);
    }
    rf_large_integer = 1000;

    rfTableOffset = ccdb.getConstants(runNum,"/calibration/eb/rf/offset");
    if (rfTableOffset.hasEntry(1, 1, 1)){
      rfoffset1 = (float)rfTableOffset.getDoubleValue("offset",1,1,1);
      rfoffset2 = (float)rfTableOffset.getDoubleValue("offset",1,1,2);
      System.out.println(String.format("RF1 offset from ccdb for run %d: %f",runNum,rfoffset1));
      System.out.println(String.format("RF2 offset from ccdb for run %d: %f",runNum,rfoffset2));
    }


    //Initializing rf histograms.
    H_RFtimediff = new H1F("H_RFtimediff","H_RFtimediff",5000,-5.,5.);
    H_RFtimediff.setTitle("RF time difference (1-2)");
    H_RFtimediff.setTitleX("RF1-RF2 (ns)");
    H_RFtimediff_corrected = new H1F("H_RFtimediff_corrected","H_RFtimediff_corrected",5000,-5.,5.);
    H_RFtimediff_corrected.setTitle("RF time difference (1-2), offset corrected");
    H_RFtimediff_corrected.setTitleX("RF1+rfoffset1-RF2-rfoffset2 (ns)");
    H_e_RFtime1_FD_S = new H1F[6];
    H_pip_RFtime1_FD_S = new H1F[6];
    H_pim_RFtime1_FD_S = new H1F[6];
    H_p_RFtime1_FD_S = new H1F[6];
    for(int i=0;i<6;i++){
      H_e_RFtime1_FD_S[i] = new H1F(String.format("H_e_RFtime1_S%d",i+1),String.format("H_e_RFtime1_S%d",i+1),1000,-5.,5.);
      H_e_RFtime1_FD_S[i].setTitle(String.format("FD elec vertex_t - RF1_t, S%d",i+1));
      H_e_RFtime1_FD_S[i].setTitleX("v_t-RF1_t (ns)");
      H_pip_RFtime1_FD_S[i] = new H1F(String.format("H_pip_RFtime1_S%d",i+1),String.format("H_pip_RFtime1_S%d",i+1),1000,-5.,5.);
      H_pip_RFtime1_FD_S[i].setTitle(String.format("FD #pi^+ vertex_t - RF1_t, S%d",i+1));
      H_pip_RFtime1_FD_S[i].setTitleX("v_t-RF1_t (ns)");
      H_pim_RFtime1_FD_S[i] = new H1F(String.format("H_pim_RFtime1_S%d",i+1),String.format("H_pim_RFtime1_S%d",i+1),1000,-5.,5.);
      H_pim_RFtime1_FD_S[i].setTitle(String.format("FD #pi^- vertex_t - RF1_t, S%d",i+1));
      H_pim_RFtime1_FD_S[i].setTitleX("v_t-RF1_t (ns)");
      H_p_RFtime1_FD_S[i] = new H1F(String.format("H_p_RFtime1_S%d",i+1),String.format("H_p_RFtime1_S%d",i+1),1000,-5.,5.);
      H_p_RFtime1_FD_S[i].setTitle(String.format("FD prot vertex_t - RF1_t, S%d",i+1));
      H_p_RFtime1_FD_S[i].setTitleX("v_t-RF1_t (ns)");
    }
    H_pip_RFtime1_CD = new H1F("H_pip_RFtime1","H_pip_RFtime1",1000,-5.,5.);
    H_pip_RFtime1_CD.setTitle("CD #pi^+ vertex_t - RF1_t");
    H_pip_RFtime1_CD.setTitleX("v_t-RF1_t (ns)");
    H_pim_RFtime1_CD = new H1F("H_pim_RFtime1","H_pim_RFtime1",1000,-5.,5.);
    H_pim_RFtime1_CD.setTitle("CD #pi^- vertex_t - RF1_t");
    H_pim_RFtime1_CD.setTitleX("v_t-RF1_t (ns)");
    H_p_RFtime1_CD = new H1F("H_p_RFtime1","H_p_RFtime1",1000,-5.,5.);
    H_p_RFtime1_CD.setTitle("CD prot vertex_t - RF1_t");
    H_p_RFtime1_CD.setTitleX("v_t-RF1_t (ns)");


    H_trig_sector_count = new H1F("H_trig_sector_count","H_trig_sector_count",6,0.5,6.5);
    H_trig_sector_count.setTitle("N trigs per sect");
    H_trig_sector_count.setTitleX("Sector number");
    H_muon_trig_sector_count = new H1F("H_muon_trig_sector_count","H_muon_trig_sector_count",3,0.5,3.5);
    H_muon_trig_sector_count.setTitle("N muon trigs per sect-pair");
    H_muon_trig_sector_count.setTitleX("Sector-pair number");
    H_trig_sector_elec_rat = new H1F("H_trig_sector_elec_rat","H_trig_sector_elec_rat",6,0.5,6.5);
    H_trig_sector_elec_rat.setTitle("N elec / trig vs sector");
    H_trig_sector_elec_rat.setTitleX("Sector number");
    H_trig_sector_muon_rat = new H1F("H_trig_sector_muon_rat","H_trig_sector_muon_rat",3,0.5,3.5);
    H_trig_sector_muon_rat.setTitle("N muon / trig vs sector");
    H_trig_sector_muon_rat.setTitleX("Sector-pair number");
    H_trig_sector_prot_rat = new H1F("H_trig_sector_prot_rat","H_trig_sector_prot_rat",6,0.5,6.5);
    H_trig_sector_prot_rat.setTitle("FD prot / trig per sect");
    H_trig_sector_prot_rat.setTitleX("Sector number");
    H_trig_sector_piplus_rat = new H1F("H_trig_sector_piplus_rat","H_trig_sector_piplus_rat",6,0.5,6.5);
    H_trig_sector_piplus_rat.setTitle("FD #pi+ / trig per sect");
    H_trig_sector_piplus_rat.setTitleX("Sector number");
    H_trig_sector_piminus_rat = new H1F("H_trig_sector_piminus_rat","H_trig_sector_piminus_rat",6,0.5,6.5);
    H_trig_sector_piminus_rat.setTitle("FD #pi- / trig per sect");
    H_trig_sector_piminus_rat.setTitleX("Sector number");
    H_trig_sector_kplus_rat = new H1F("H_trig_sector_kplus_rat","H_trig_sector_kplus_rat",6,0.5,6.5);
    H_trig_sector_kplus_rat.setTitle("FD K+ / trig per sect");
    H_trig_sector_kplus_rat.setTitleX("Sector number");
    H_trig_sector_kminus_rat = new H1F("H_trig_sector_kminus_rat","H_trig_sector_kminus_rat",6,0.5,6.5);
    H_trig_sector_kminus_rat.setTitle("FD K- / trig per sect");
    H_trig_sector_kminus_rat.setTitleX("Sector number");
    H_trig_sector_positive_rat = new H1F("H_trig_sector_positive_rat","H_trig_sector_positive_rat",6,0.5,6.5);
    H_trig_sector_positive_rat.setTitle("FD positive / trig per sect");
    H_trig_sector_positive_rat.setTitleX("Sector number");
    H_trig_sector_negative_rat = new H1F("H_trig_sector_negative_rat","H_trig_sector_negative_rat",6,0.5,6.5);
    H_trig_sector_negative_rat.setTitle("FD negative / trig per sect");
    H_trig_sector_negative_rat.setTitleX("Sector number");
    H_trig_sector_neutral_rat = new H1F("H_trig_sector_neutral_rat","H_trig_sector_neutral_rat",6,0.5,6.5);
    H_trig_sector_neutral_rat.setTitle("FD neutral / trig per sect");
    H_trig_sector_neutral_rat.setTitleX("Sector number");

    H_trig_central_prot_rat = new H1F("H_trig_central_prot_rat","H_trig_central_prot_rat",1,0.5,1.5);
    H_trig_central_prot_rat.setTitle("CD prot/ trig");
    H_trig_central_prot_rat.setTitleX("All sectors");
    H_trig_central_piplus_rat = new H1F("H_trig_central_piplus_rat","H_trig_central_piplus_rat",1,0.5,1.5);
    H_trig_central_piplus_rat.setTitle("CD #pi+ / trig");
    H_trig_central_piplus_rat.setTitleX("All sectors");
    H_trig_central_piminus_rat = new H1F("H_trig_central_piminus_rat","H_trig_central_piminus_rat",1,0.5,1.5);
    H_trig_central_piminus_rat.setTitle("CD #pi- / trig");
    H_trig_central_piminus_rat.setTitleX("All sectors");
    H_trig_central_kplus_rat = new H1F("H_trig_central_kplus_rat","H_trig_central_kplus_rat",1,0.5,1.5);
    H_trig_central_kplus_rat.setTitle("CD K+ / trig");
    H_trig_central_kplus_rat.setTitleX("All sectors");
    H_trig_central_kminus_rat = new H1F("H_trig_central_kminus_rat","H_trig_central_kminus_rat",1,0.5,1.5);
    H_trig_central_kminus_rat.setTitle("CD K- / trig");
    H_trig_central_kminus_rat.setTitleX("All sectors");


    H_CVT_z_pos = new H1F("H_CVT_z_pos","H_CVT_z_pos",100,-25,25);
    H_CVT_z_pos.setTitle("CVT z vertex for positives");
    H_CVT_z_pos.setTitleX("z (cm)");
    H_CVT_z_neg = new H1F("H_CVT_z_neg","H_CVT_z_neg",100,-25,25);
    H_CVT_z_neg.setTitle("CVT z vertex for negatives");
    H_CVT_z_neg.setTitleX("z (cm)");
    H_CVT_d0_pos = new H1F("H_CVT_d0_pos","H_CVT_d0_pos",200,-1.0, 1.0);
    H_CVT_d0_pos.setTitle("CVT d0 vertex for positives");
    H_CVT_d0_pos.setTitleX("d0 (cm)");
    H_CVT_absd0_pos = new H1F("H_CVT_absd0_pos","H_CVT_absd0_pos",400, 0.0, 2.0);
    H_CVT_absd0_pos.setTitle("CVT |d0| vertex for positives");
    H_CVT_absd0_pos.setTitleX("|d0| (cm)");
    H_CVT_chi2 = new H1F("H_CVT_chi2","H_CVT_chi2",100,0,200);
    H_CVT_chi2.setTitle("CVT #chi^2 for electrons");
    H_CVT_chi2.setTitleX("#chi^2");
    H_CVT_chi2_pos = new H1F("H_CVT_chi2_pos","H_CVT_chi2_pos",100,0,200);
    H_CVT_chi2_pos.setTitle("CVT #chi^2 for positives");
    H_CVT_chi2_pos.setTitleX("#chi^2");
    H_CVT_chi2_neg = new H1F("H_CVT_chi2_neg","H_CVT_chi2_neg",100,0,200);
    H_CVT_chi2_neg.setTitle("CVT #chi^2 for negatives");
    H_CVT_chi2_neg.setTitleX("#chi^2");

    H_gg_m = new H1F("H_gg_m","H_gg_m",100,0,0.7);
    H_gg_m.setTitle("#gamma#gamma invariant mass");
    H_gg_m.setTitleX("m_{#gamma#gamma} (GeV)");

    VB = new LorentzVector(0,0,Ebeam,Ebeam);
    VT = new LorentzVector(0,0,0,0.93827);
    H_trig_vz_mom_S = new H2F[6];
    H_trig_ECALsampl_S = new H2F[6];
    H_trig_PCAL_vt_S = new H1F[6];
    H_trig_ECIN_vt_S = new H1F[6];
    H_trig_ECOU_vt_S = new H1F[6];
    H_trig_LTCCn_theta_S = new H2F[6];

    for(int s=0;s<6;s++){
      H_trig_vz_mom_S[s] = new H2F(String.format("H_trig_vz_mom_S%d",s+1),String.format("H_trig_vz_mom_S%d",s+1),100,0,EB,200,-50,50);
      H_trig_vz_mom_S[s].setTitle(String.format("e sect %d",s+1));
      H_trig_vz_mom_S[s].setTitleX("p (GeV)");
      H_trig_vz_mom_S[s].setTitleY("vz (cm)");
      H_trig_ECALsampl_S[s] = new H2F(String.format("H_trig_ECALsampl_S%d",s+1),String.format("H_trig_ECALsampl_S%d",s+1),100,0,EB,100,0,0.5);
      H_trig_ECALsampl_S[s].setTitle(String.format("e sect %d",s+1));
      H_trig_ECALsampl_S[s].setTitleX("p (GeV)");
      H_trig_ECALsampl_S[s].setTitleY("ECAL sampling");

      H_trig_PCAL_vt_S[s] = new H1F(String.format("H_trig_PCAL_vt_S%d",s+1),String.format("H_trig_PCAL_vt_S%d",s+1),100,-3,3);
      H_trig_PCAL_vt_S[s].setTitle(String.format("e sect %d",s+1));
      H_trig_PCAL_vt_S[s].setTitleX("e- pcal residual (ns)");

      H_trig_ECIN_vt_S[s] = new H1F(String.format("H_trig_ECIN_vt_S%d",s+1),String.format("H_trig_ECIN_vt_S%d",s+1),100,-3,3);
      H_trig_ECIN_vt_S[s].setTitle(String.format("e sect %d",s+1));
      H_trig_ECIN_vt_S[s].setTitleX("e- ecin residual (ns)");

      H_trig_ECOU_vt_S[s] = new H1F(String.format("H_trig_ECOU_vt_S%d",s+1),String.format("H_trig_ECOU_vt_S%d",s+1),100,-3,3);
      H_trig_ECOU_vt_S[s].setTitle(String.format("e sect %d",s+1));
      H_trig_ECOU_vt_S[s].setTitleX("e- ecou residual (ns)");

      H_trig_LTCCn_theta_S[s] = new H2F(String.format("H_trig_LTCCn_theta_S%d",s+1),String.format("H_trig_LTCCn_theta_S%d",s+1),100,0,45,100,0,100);
      H_trig_LTCCn_theta_S[s].setTitle(String.format(String.format("e sect %d",s+1)));
      H_trig_LTCCn_theta_S[s].setTitleX("#theta (^o)");
      H_trig_LTCCn_theta_S[s].setTitleY("LTCC nphe");
    }

    H_dcm_chi2 = new H1F[7];
    H_dcm_vz = new H1F[7];
    for(int s=0;s<7;s++){
      H_dcm_chi2[s] = new H1F(String.format("H_dcm_chi2_S%d",s+1),String.format("S%d #chi^2 DC neg",s+1),100,0,500);
      H_dcm_vz[s] = new H1F(String.format("H_dcm_vz_s%d",s+1),String.format("H_dcm_vz_s%d",s+1),100,-25,25);
      H_dcm_vz[s].setTitle(String.format("S%d vz DC neg mom>1.5 GeV",s+1));
    }

    H_dcp_vz = new H1F[7];
    for(int s=0;s<7;s++){
      H_dcp_vz[s] = new H1F(String.format("H_dcp_vz_s%d",s+1),String.format("H_dcp_vz_s%d",s+1),100,-25,25);
      H_dcp_vz[s].setTitle(String.format("S%d vz DC pos mom>1.5 GeV",s+1));
    }
    H_dce_chi2 = new H1F[6];
    for(int s=0;s<6;s++){
      H_dce_chi2[s] = new H1F(String.format("H_dce_chi2_S%d",s+1),String.format("S%d #chi^2 DC elec",s+1),100,0,500);
    }

    hbstOccupancy = new H1F("hbstOccupancy", 100,0,10);
    hbstOccupancy.setTitle("BST Occupancy");
    hbstOccupancy.setTitleX("BST Occupancy (%)");
    hbmtOccupancy = new H1F("hbmtOccupancy", 100,0,10);
    hbmtOccupancy.setTitle("BMT Occupancy");
    hbmtOccupancy.setTitleX("BMT Occupancy (%)");
    htrks = new H1F("htrks", 11, -0.5, 10.5);
    htrks.setTitle("CVT No Tracks");
    htrks.setTitleX("CVT No Tracks");
    hpostrks = new H1F("hpostrks", 11, -0.5, 10.5);
    hpostrks.setTitle("CVT No Positive Tracks");
    hpostrks.setTitleX("CVT No Positive Tracks");
    hnegtrks = new H1F("hnegtrks", 11, -0.5, 10.5);
    hnegtrks.setTitle("CVT No Negative Tracks");
    hnegtrks.setTitleX("CVT No Negative Tracks");
    hpostrks_rat = new H1F("hpostrks_rat", 11, -0.5, 10.5);
    hpostrks_rat.setTitle("CVT Positive Tracks/ trigger");
    hpostrks_rat.setTitleX("CVT Positive Tracks/ trigger");
    hnegtrks_rat = new H1F("hnegtrks_rat", 11, -0.5, 10.5);
    hnegtrks_rat.setTitle("CVT Negative Tracks/ trigger");
    hnegtrks_rat.setTitleX("CVT Negative Tracks/ trigger");
    hndf = new H1F("hndf", 10, 0, 10);
    hndf.setTitle("CVT track ndf");
    hndf.setTitleX("CVT track ndf");
    hchi2norm = new H1F("hchi2norm", 100, 0, 100);
    hchi2norm.setTitle("CVT track chi2norm");
    hchi2norm.setTitleX("CVT track chi2/ndf");
    hp = new H1F("hp", 100, 0, 2);
    hp.setTitle("CVT track momentum");
    hp.setTitleX("CVT track momentum (GeV/c)");
    hpt = new H1F("hpt", 100, 0, 2);
    hpt.setTitle("CVT track transverse momentum");
    hpt.setTitleX("CVT track transverse momentum (GeV/c)");
    hpathlen = new H1F("hpathlen", 100, 0, 70);
    hpathlen.setTitle("CVT pathlength");
    hpathlen.setTitleX("CVT pathlength (cm)");
    hbstOnTrkLayers = new H1F("hbstOnTrkLayers", 11, -0.5, 10.5);
    hbstOnTrkLayers.setTitle("BST Layers per Track");
    hbstOnTrkLayers.setTitleX("BST Layers per Track");
    hbmtOnTrkLayers = new H1F("hbmtOnTrkLayers", 11, -0.5, 10.5);
    hbmtOnTrkLayers.setTitle("BMT Layers per Track");
    hbmtOnTrkLayers.setTitleX("BMT Layers per Track");

  }

  public double Vangle(Vector3 v1, Vector3 v2){
    double res = 0;
    double l1 = v1.mag();
    double l2 = v2.mag();
    double prod = v1.dot(v2);
    if( l1 * l2 !=0 && Math.abs(prod)<l1*l2 )res = Math.toDegrees( Math.acos(prod/(l1*l2) ) );
    return res;
  }


  public int makePiPlusPID(DataBank bank){
    boolean foundelec = false;
    int npositives = 0;
    int nnegatives = 0;
    float mybeta = 0;
    for(int k = 0; k < bank.rows(); k++){
      int pid = bank.getInt("pid", k);
      int status = bank.getShort("status", k);
      if (status<0) status = -status;
      byte q = bank.getByte("charge", k);
      float thisbeta = bank.getFloat("beta", k);
      boolean inDC = (status>=2000 && status<4000);
      if(inDC && pid==11)foundelec=true;
      if(inDC && q<0&&thisbeta>0)nnegatives++;
      if(inDC && npositives==0&&q>0&&thisbeta>0)mybeta=thisbeta;
      if(inDC && q>0&&thisbeta>0)npositives++;
    }
    if(foundelec && nnegatives==1 && npositives==1 && mybeta>0){
      for(int k = 0; k < bank.rows(); k++){
        byte q = bank.getByte("charge", k);
        float px = bank.getFloat("px", k);
        float py = bank.getFloat("py", k);
        float pz = bank.getFloat("pz", k);
        pip_mom = (float)Math.sqrt(px*px+py*py+pz*pz);
        pip_theta = (float)Math.toDegrees(Math.acos(pz/pip_mom));
        pip_phi = (float)Math.toDegrees(Math.atan2(py,px));
        pip_vx = bank.getFloat("vx", k);
        pip_vy = bank.getFloat("vy", k);
        pip_vz = bank.getFloat("vz", k);
        pip_beta = bank.getFloat("beta", k);
        if( q>0 && pip_mom>0.5 && pip_theta<40 && pip_theta>5 && pip_beta>0){
          VPIP = new LorentzVector(px,py,pz,Math.sqrt(pip_mom*pip_mom+0.139*0.139));
          return k;
        }
      }
    }
    return -1;
  }

  public int makePiMinusPID(DataBank bank){
    boolean foundelec = false;
    int npositives = 0;
    int nnegatives = 0;
    float mybeta = 0;
    for(int k = 0; k < bank.rows(); k++){
      int pid = bank.getInt("pid", k);
      int status = bank.getShort("status", k);
      if (status<0) status = -status;
      byte q = bank.getByte("charge", k);
      float thisbeta = bank.getFloat("beta", k);
      boolean inDC = (status>=2000 && status<4000);
      if(inDC && pid==11)foundelec=true;
      if(inDC && q<0&&thisbeta>0)nnegatives++;
      if(inDC && npositives==0&&q>0&&thisbeta>0)mybeta=thisbeta;
      if(inDC && q>0&&thisbeta>0)npositives++;
    }
    if(foundelec && nnegatives==2 && mybeta>0){
      for(int k = 0; k < bank.rows(); k++){
        int pid = bank.getInt("pid", k);
        byte q = bank.getByte("charge", k);
        float px = bank.getFloat("px", k);
        float py = bank.getFloat("py", k);
        float pz = bank.getFloat("pz", k);
        pim_mom = (float)Math.sqrt(px*px+py*py+pz*pz);
        pim_theta = (float)Math.toDegrees(Math.acos(pz/pim_mom));
        pim_phi = (float)Math.toDegrees(Math.atan2(py,px));
        pim_vx = bank.getFloat("vx", k);
        pim_vy = bank.getFloat("vy", k);
        pim_vz = bank.getFloat("vz", k);
        pim_beta = bank.getFloat("beta", k);

        if( q<0 && pim_mom>0.5 && pim_theta<40 && pim_theta>5 && pim_beta>0 && pid!=11){
          VPIM = new LorentzVector(px,py,pz,Math.sqrt(pim_mom*pim_mom+0.139*0.139));
          return k;
        }
      }
    }
    return -1;
  }

  public int makePiPlusPimPID(DataBank bank){
    boolean foundelec = false;
    int npositives = 0;
    int nnegatives = 0;
    float mybetap = 0;
    for(int k = 0; k < bank.rows(); k++){
      int pid = bank.getInt("pid", k);
      byte q = bank.getByte("charge", k);
      float thisbeta = bank.getFloat("beta", k);
      int status = bank.getShort("status", k);
      if (status<0) status = -status;
      boolean inDC = (status>=2000 && status<4000);
      if(inDC && pid==11)foundelec=true;
      if(inDC && q<0&&thisbeta>0)nnegatives++;
      if(inDC && npositives==0&&q>0&&thisbeta>0)mybetap=thisbeta;
      if(inDC && q>0&&thisbeta>0)npositives++;
    }

    if(foundelec && nnegatives==2 && npositives>0 && npositives<3 && mybetap>0 ){
      for(int k = 0; k < bank.rows(); k++){
        int pid = bank.getInt("pid", k);
        byte q = bank.getByte("charge", k);
        int status = bank.getShort("status", k);
        if (status<0) status = -status;
        boolean inDC = (status>=2000 && status<4000);
        if(inDC && q>0){
          float px = bank.getFloat("px", k);
          float py = bank.getFloat("py", k);
          float pz = bank.getFloat("pz", k);
          pip_mom = (float)Math.sqrt(px*px+py*py+pz*pz);
          pip_theta = (float)Math.toDegrees(Math.acos(pz/pip_mom));
          pip_phi = (float)Math.toDegrees(Math.atan2(py,px));
          pip_vx = bank.getFloat("vx", k);
          pip_vy = bank.getFloat("vy", k);
          pip_vz = bank.getFloat("vz", k);
          pip_beta = bank.getFloat("beta", k);
          if(pip_mom>0.5 && pip_theta<40 && pip_theta>8 && pip_beta>0){
            VPIP = new LorentzVector(px,py,pz,Math.sqrt(pip_mom*pip_mom+0.139*0.139));
            pip_part_ind = k;
          }
        }
        if(inDC && q<0&&pid!=11){
          float px = bank.getFloat("px", k);
          float py = bank.getFloat("py", k);
          float pz = bank.getFloat("pz", k);
          pim_mom = (float)Math.sqrt(px*px+py*py+pz*pz);
          pim_theta = (float)Math.toDegrees(Math.acos(pz/pip_mom));
          pim_phi = (float)Math.toDegrees(Math.atan2(py,px));
          pim_vx = bank.getFloat("vx", k);
          pim_vy = bank.getFloat("vy", k);
          pim_vz = bank.getFloat("vz", k);
          pim_beta = bank.getFloat("beta", k);
          if(pim_mom>0.5 && pim_theta<40 && pim_theta>8 && pim_beta>0){
            VPIM = new LorentzVector(px,py,pz,Math.sqrt(pim_mom*pim_mom+0.139*0.139));
            pim_part_ind = k;
          }
        }
      }
    }
    return -1;
  }
  public int makeElectron(DataBank bank){
    for(int k = 0; k < bank.rows(); k++){
      int pid = bank.getInt("pid", k);
      float px = bank.getFloat("px", k);
      float py = bank.getFloat("py", k);
      float pz = bank.getFloat("pz", k);
      int status = bank.getShort("status", k);
      STT = bank.getFloat("vt",k);
      if (status<0) status = -status;
      boolean inDC = (status>=2000 && status<4000);
      e_mom = (float)Math.sqrt(px*px+py*py+pz*pz);
      e_theta = (float)Math.toDegrees(Math.acos(pz/e_mom));
      e_vz = bank.getFloat("vz", k);
      if( inDC && pid == 11 ){
        e_phi = (float)Math.toDegrees(Math.atan2(py,px));
        e_vx = bank.getFloat("vx", k);
        e_vy = bank.getFloat("vy", k);
        Ve = new LorentzVector(px,py,pz,e_mom);
        return k;
      }
    }
    return -1;
  }

  public int makeTrigElectron(DataBank bank, DataEvent event){
    for(int k = 0; k < bank.rows(); k++){
      int pid = bank.getInt("pid", k);
      float px = bank.getFloat("px", k);
      float py = bank.getFloat("py", k);
      float pz = bank.getFloat("pz", k);
      int status = bank.getShort("status", k);
      if (status<0) status = -status;
      boolean inDC = (status>=2000 && status<4000);
      e_mom = (float)Math.sqrt(px*px+py*py+pz*pz);
      e_theta = (float)Math.toDegrees(Math.acos(pz/e_mom));
      e_vz = bank.getFloat("vz", k);
      if( pid == 11 && inDC ){
        if(userTimeBased && event.hasBank("REC::Calorimeter")){
          DataBank ECALbank = event.getBank("REC::Calorimeter");
          for(int l = 0; l < ECALbank.rows(); l++) {
            if(ECALbank.getShort("pindex",l)==k) {
              if(ECALbank.getInt("layer",l)==1) {
                trig_sect=ECALbank.getByte("sector",l);
              }
            }
          }
        }
        if(!userTimeBased && event.hasBank("RECHB::Calorimeter")){
          DataBank ECALbank = event.getBank("RECHB::Calorimeter");
          for(int l = 0; l < ECALbank.rows(); l++) {
            if(ECALbank.getShort("pindex",l)==k){
              if(ECALbank.getInt("layer",l)==1) {
                trig_sect=ECALbank.getByte("sector",l);
              }
            }
          }
        }
        e_phi = (float)Math.toDegrees(Math.atan2(py,px));
        e_vx = bank.getFloat("vx", k);
        e_vy = bank.getFloat("vy", k);
        Ve = new LorentzVector(px,py,pz,e_mom);
        return k;
      }
    }
    return -1;
  }

  public void makeRFHistograms(DataBank particle, DataBank scintillator) {
    for (int k = 0; k < particle.rows(); k++) {
      int pid = particle.getInt("pid", k);
      float px = particle.getFloat("px", k);
      float py = particle.getFloat("py", k);
      float pz = particle.getFloat("pz", k);
      float vt = particle.getFloat("vt", k);
      float mom2 = px * px + py * py + pz * pz;
      float mom = (float) Math.sqrt(px * px + py * py + pz * pz);
      int status = particle.getShort("status", k);
      if (status < 0) {
        status = -status;
      }
      boolean Forward = (status < 4000);
      boolean Central = (status >= 4000);

      float en, DCbeta, Cbeta, timediff, p_vert_time, pi_vert_time;

      float mass_pion = 0.13957061f;
      float mass_proton = 0.9382720814f;

      for (int kk = 0; kk < scintillator.rows(); kk++) {
        short pind = scintillator.getShort("pindex", kk);
        int sector = scintillator.getInt("sector", kk);

        if (Forward && (pind == k) && (scintillator.getByte("detector", kk) == 12)) {
          if (pid == 2212) {
            en = (float) Math.sqrt(mom2 + mass_proton * mass_proton);
            DCbeta = mom / en;
            p_vert_time = scintillator.getFloat("time", kk) - scintillator.getFloat("path", kk) / (29.98f * DCbeta);
            timediff = p_vert_time - vt;
            H_p_RFtime1_FD_S[sector - 1].fill(timediff);
          }
          else if (pid == 211) {
            en = (float) Math.sqrt(mom2 + mass_pion * mass_pion);
            DCbeta = mom / en;
            pi_vert_time = scintillator.getFloat("time", kk) - scintillator.getFloat("path", kk) / (29.98f * DCbeta);
            timediff = pi_vert_time - vt;
            H_pip_RFtime1_FD_S[sector - 1].fill(timediff);
          }
          else if (pid == -211) {
            en = (float) Math.sqrt(mom2 + mass_pion * mass_pion);
            DCbeta = mom / en;
            pi_vert_time = scintillator.getFloat("time", kk) - scintillator.getFloat("path", kk) / (29.98f * DCbeta);
            timediff = pi_vert_time - vt;
            H_pim_RFtime1_FD_S[sector - 1].fill(timediff);
          }

        }
        if (Central && (pind == k) && (scintillator.getByte("detector", kk) == 4)) {
          if (pid == 2212) {
            en = (float) Math.sqrt(mom2 + mass_proton * mass_proton);
            Cbeta = mom / en;
            p_vert_time = scintillator.getFloat("time", kk) - scintillator.getFloat("path", kk) / (29.98f * Cbeta);
            timediff = p_vert_time - vt;
            H_p_RFtime1_CD.fill(timediff);
          }
          else if (pid == 211) {
            en = (float) Math.sqrt(mom2 + mass_pion * mass_pion);
            Cbeta = mom / en;
            pi_vert_time = scintillator.getFloat("time", kk) - scintillator.getFloat("path", kk) / (29.98f * Cbeta);
            timediff = pi_vert_time - vt;
            H_pip_RFtime1_CD.fill(timediff);
          }
          else if (pid == -211) {
            en = (float) Math.sqrt(mom2 + mass_pion * mass_pion);
            Cbeta = mom / en;
            pi_vert_time = scintillator.getFloat("time", kk) - scintillator.getFloat("path", kk) / (29.98f * Cbeta);
            timediff = pi_vert_time - vt;
            H_pim_RFtime1_CD.fill(timediff);
          }
        }
      }
    }
  }

  public void makeTrigOthers(DataBank bank, DataEvent event){
    DataBank ECALbank = null;
    DataBank Trackbank = null;
    if(userTimeBased && event.hasBank("REC::Calorimeter"))ECALbank = event.getBank("REC::Calorimeter");
    if(userTimeBased && event.hasBank("REC::Track"))Trackbank = event.getBank("REC::Track");
    if(!userTimeBased && event.hasBank("RECHB::Calorimeter"))ECALbank = event.getBank("RECHB::Calorimeter");
    if(!userTimeBased && event.hasBank("RECHB::Track"))Trackbank = event.getBank("RECHB::Track");
    for(int k = 0; k < bank.rows(); k++){
      int pid = bank.getInt("pid", k);
      byte q = bank.getByte("charge", k);
      float beta = bank.getFloat("beta", k);
      int status = bank.getShort("status", k);
      if (status<0) status = -status;
      boolean Forward = (status<4000);
      boolean Central = (status>=4000);


      int sector = 0;
      if(q!=0 && Trackbank!=null){
        for(int l=0;l<Trackbank.rows() && sector==0 ;l++)if(Trackbank.getInt("pindex",l)==k)sector=Trackbank.getInt("sector",l);
      }
      if(q==0 && ECALbank!=null){
        for(int l=0;l<ECALbank.rows() && sector==0 ;l++)if(ECALbank.getInt("pindex",l)==k)sector=ECALbank.getInt("sector",l);
      }
      if(Forward&&sector>0){
        if(pid==2212){
          H_trig_sector_prot_rat.fill(sector);
        }
        else if(pid==211){
          H_trig_sector_piplus_rat.fill(sector);
        }
        else if(pid==-211){
          H_trig_sector_piminus_rat.fill(sector);
        }
        else if(pid==321){
          H_trig_sector_kplus_rat.fill(sector);
        }
        else if(pid==-321){
          H_trig_sector_kminus_rat.fill(sector);
        }
        if (q>0){
          H_trig_sector_positive_rat.fill(sector);
        }
        else if (q==0){
          H_trig_sector_neutral_rat.fill(sector);
        }
        else if (q<0){
          H_trig_sector_negative_rat.fill(sector);
        }
      }
      if (Central){
        if (beta > 0. && beta < 1.05) {
          if (q>0 && pid==2212) H_trig_central_prot_rat.fill(1);
          if (q>0 && pid==211) H_trig_central_piplus_rat.fill(1);
          if (q<0 && pid==-211) H_trig_central_piminus_rat.fill(1);
          if (q>0 && pid==321) H_trig_central_kplus_rat.fill(1);
          if (q<0 && pid==-321) H_trig_central_kminus_rat.fill(1);
        }
      }
    }
  }

  public void getElecEBECal(DataBank bank){
    for(int k = 0; k < bank.rows(); k++){
      int det = bank.getInt("layer", k);
      short pind = bank.getShort("pindex",k);
      if(pind==e_part_ind){
        if (det == 1) {
          e_ecal_X = bank.getFloat("x", k);
          e_ecal_Y = bank.getFloat("y", k);
          e_ecal_Z = bank.getFloat("z", k);
          e_ecal_E += bank.getFloat("energy", k);
          e_pcal_e += bank.getFloat("energy", k);
          e_sect = bank.getByte("sector", k);
          float path = bank.getFloat("path", k);
          e_ecal_T_PCAL = bank.getFloat("time", k) - path / 29.98f - STT;
        }
        else if (det == 4) {
          e_ecal_E += bank.getFloat("energy", k);
          e_etot_e += bank.getFloat("energy", k);
          float path = bank.getFloat("path", k);
          e_ecal_T_ECIN = bank.getFloat("time", k) - path / 29.98f - STT;
        }
        else if (det == 7) {
          e_ecal_E += bank.getFloat("energy", k);
          e_etot_e += bank.getFloat("energy", k);
          float path = bank.getFloat("path", k);
          e_ecal_T_ECOU = bank.getFloat("time", k) - path / 29.98f - STT;
        }
      }
    }
  }

  public void getElecEBCC(DataBank bank){
    for(int k = 0; k < bank.rows(); k++){
      short pind = bank.getShort("pindex",k);
      if (pind==e_part_ind) {
        if(bank.getByte("detector", k) == 15) {
          e_HTCC = (float) bank.getFloat("nphe", k);
          e_HTCC_X = bank.getFloat("x", k);
          e_HTCC_Y = bank.getFloat("y", k);
          e_HTCC_Z = bank.getFloat("z", k);

        }
        else if (bank.getByte("detector", k) == 16) {
          hasLTCC = 1;
          e_LTCC = (float) bank.getFloat("nphe", k);
        }
      }
    }
  }

  public void getElecEBTOF(DataBank bank, DataBank particle){
    for(int k = 0; k < bank.rows(); k++){
      short pind = bank.getShort("pindex",k);
      if(pind==e_part_ind) {
        if (bank.getFloat("energy",k)>5){
          float vt = particle.getFloat("vt", e_part_ind);
          e_vert_time = bank.getFloat("time", k) - bank.getFloat("path", k) / 29.98f;
          e_vert_time_RF = e_vert_time - vt;
          H_e_RFtime1_FD_S[e_sect - 1].fill(e_vert_time_RF);
          e_TOF_X = bank.getFloat("x", k);
          e_TOF_Y = bank.getFloat("y", k);
          e_TOF_Z = bank.getFloat("z", k);
        }
      }
      else if(pind==pip_part_ind){
        float epip = (float)Math.sqrt( pip_mom*pip_mom + 0.139f*0.139f );
        float pipDCbeta = pip_mom/epip;
        pip_vert_time = bank.getFloat("time",k)-bank.getFloat("path",k)/ (29.98f * pipDCbeta) ;
      }
      else if(pind==pim_part_ind){
        float epim = (float)Math.sqrt( pim_mom*pim_mom + 0.139f*0.139f );
        float pimDCbeta = pim_mom/epim;
        pim_vert_time = bank.getFloat("time",k)-bank.getFloat("path",k)/ (29.98f * pimDCbeta) ;
      }
    }
  }


  public void fillEBTrack(DataBank bank){
    e_track_ind=-1;pip_track_ind=-1;pim_track_ind=-1;
    for(int k = 0; k < bank.rows(); k++){
      short pind = bank.getShort("pindex",k);
      if(pind==e_part_ind){
        e_track_chi2 = 	bank.getFloat("chi2",k);
        e_track_ind = bank.getShort("index",k);
      }
      else if(pind==pip_part_ind){
        pip_track_chi2 = bank.getFloat("chi2",k);
        pip_track_ind = bank.getShort("index",k);
      }
      else if(pind==pim_part_ind){
        pim_track_chi2 = bank.getFloat("chi2",k);
        pim_track_ind = bank.getShort("index",k);
      }
    }
  }

  public void fillTraj_HTCC(DataBank trajBank, DataBank htcc){
    for(int r=0;r<trajBank.rows();r++){
      if(trajBank.getShort("pindex",r)==e_part_ind){
        found_eTraj=1;
        if(trajBank.getInt("detector",r) == 15) {
          e_HTCC_tX = trajBank.getFloat("x",r);
          e_HTCC_tY = trajBank.getFloat("y",r);
          e_HTCC_tZ = trajBank.getFloat("z",r);
        }
      }
    }
    for(int r=0;r<htcc.rows();r++){
      if(htcc.getShort("pindex",r)==e_part_ind){
        if(htcc.getByte("detector",r)==15){
          found_eHTCC = 1;
          e_HTCC_nphe = htcc.getFloat("nphe",r);
        }
      }
    }
  }

  public void getTrigTBTrack(DataBank bank, DataBank recBank){
    for(int k = 0; k < bank.rows(); k++){
      if(recBank.getShort("pindex",k)==trig_part_ind && recBank.getByte("detector",k)==6)trig_track_ind = recBank.getShort("index",k);
    }
    if(trig_track_ind>-1 && trig_sect == bank.getInt("sector", trig_track_ind) ){
      e_track_chi2 = bank.getFloat("chi2" , trig_track_ind);
      e_sect = bank.getInt("sector", trig_track_ind);
      H_trig_sector_elec_rat.fill(e_sect);
    }
  }

  public int isDCmatch(DataEvent event, int index){
    int sectordc = -1;
    if (userTimeBased && event.hasBank("REC::Track")) {
      DataBank DCbank = event.getBank("REC::Track");
      for (int l = 0; l < DCbank.rows(); l++) {
        if (DCbank.getShort("pindex", l) == index && DCbank.getInt("detector", l) == 6) {
          sectordc = DCbank.getByte("sector", l);
        }
      }
    }
    return sectordc;
  }



  public void getTBTrack(DataBank bank){
    if(e_track_ind>-1 && e_track_ind<bank.rows()){
      e_track_chi2 = bank.getFloat("chi2" , e_track_ind);
      e_sect = bank.getInt("sector", e_track_ind);
      H_dce_chi2[e_sect-1].fill(e_track_chi2);
    }
    if(pip_track_ind>-1 && pip_track_ind<bank.rows())pip_sect = bank.getInt("sector", pip_track_ind);
    if(pim_track_ind>-1 && pim_track_ind<bank.rows())pim_sect = bank.getInt("sector", pim_track_ind);
  }



  public void fillDCbanks(DataBank bank, DataBank bXos){
    for(int k = 0; k < bank.rows(); k++){
      float px   = bank.getFloat("p0_x"  , k);
      float py   = bank.getFloat("p0_y"  , k);
      float pz   = bank.getFloat("p0_z"  , k);
      float vz   = bank.getFloat("Vtx0_z", k);
      int s = bank.getInt("sector",k)-1;
      int Xind1 = -1;
      int Xind2 = -1;
      int Xind3 = -1;
      for(int l=0;l<bXos.rows();l++){
        if(bXos.getInt("id",l)==bank.getInt("Cross1_ID",k))Xind1=l;
        if(bXos.getInt("id",l)==bank.getInt("Cross2_ID",k))Xind2=l;
        if(bXos.getInt("id",l)==bank.getInt("Cross3_ID",k))Xind3=l;
      }
      float mom = (float)Math.sqrt(px*px+py*py+pz*pz);

      if(bank.getByte("q",k)<0 && mom>0.15 && bank.getFloat("chi2" , k)<5000){
        if(mom>0.15
            && bXos.getFloat("x",Xind1)*bXos.getFloat("y",Xind1)!=0
            && bXos.getFloat("x",Xind2)*bXos.getFloat("y",Xind2)!=0
            && bXos.getFloat("x",Xind3)*bXos.getFloat("y",Xind3)!=0
          ){
          H_dcm_vz[s].fill(vz);
          H_dcm_chi2[s].fill(bank.getFloat("chi2" , k));
          H_dcm_vz[6].fill(vz);
          H_dcm_chi2[6].fill(bank.getFloat("chi2" , k));
          }
      }
      if(bank.getByte("q",k)>0 && mom>0.15 && bank.getFloat("chi2" , k)<5000){
        if(mom>0.15
            && bXos.getFloat("x",Xind1)*bXos.getFloat("y",Xind1)!=0
            && bXos.getFloat("x",Xind2)*bXos.getFloat("y",Xind2)!=0
            && bXos.getFloat("x",Xind3)*bXos.getFloat("y",Xind3)!=0
          ){
          H_dcp_vz[s].fill(vz);
          H_dcp_vz[6].fill(vz);
          }
      }
    }
  }

  public void makePhotons(DataBank bank, DataEvent event){
    ngammas=0;
    int ig1=-1,ig2=-1;
    float eg1=0,eg2=0;
    for(int k = 0; k < bank.rows(); k++){
      int pid = bank.getInt("pid", k);
      if( pid == 22 ){
        float px = bank.getFloat("px", k);
        float py = bank.getFloat("py", k);
        float pz = bank.getFloat("pz", k);
        float eg = (float)Math.sqrt(px*px+py*py+pz*pz);
        float tg = (float)Math.toDegrees(Math.acos(pz/eg));
        if(eg>Ebeam*0.08 && tg>4.25){
          ngammas++;
          if(eg>eg1){ig1=k;eg1=eg;}
        }
      }
    }
    if(ig1>-1 && ngammas>1)for(int k = 0; k < bank.rows(); k++){
      int pid = bank.getInt("pid", k);
      if( k!=ig1 && pid == 22 ){
        float px = bank.getFloat("px", k);
        float py = bank.getFloat("py", k);
        float pz = bank.getFloat("pz", k);
        float eg = (float)Math.sqrt(px*px+py*py+pz*pz);
        float tg = (float)Math.toDegrees(Math.acos(pz/eg));
        if(eg>Ebeam*0.08 && tg>4.25){
          if(eg>eg2){ig2=k;eg2=eg;}
        }
      }
    }
    if(ngammas==2 && ig1>-1 && ig2>-1){
      float px = bank.getFloat("px", ig1);
      float py = bank.getFloat("py", ig1);
      float pz = bank.getFloat("pz", ig1);
      float eg = (float)Math.sqrt(px*px+py*py+pz*pz);
      float tg = (float)Math.toDegrees(Math.acos(pz/eg));
      float fg = (float)Math.toDegrees(Math.atan2(py,px));
      g1_e = eg;
      g1_theta = tg;
      g1_phi = fg;
      VG1 = new LorentzVector(px,py,pz,eg);
      px = bank.getFloat("px", ig2);
      py = bank.getFloat("py", ig2);
      pz = bank.getFloat("pz", ig2);
      eg = (float)Math.sqrt(px*px+py*py+pz*pz);
      tg = (float)Math.toDegrees(Math.acos(pz/eg));
      fg = (float)Math.toDegrees(Math.atan2(py,px));
      g2_e = eg;
      g2_theta = tg;
      g2_phi = fg;
      VG2 = new LorentzVector(px,py,pz,eg);
      if(Vangle(VG1.vect(),VG2.vect())>1.5 && Vangle(VG1.vect(),VG2.vect())> 8*(1-(g1_e+g2_e)/4) ){}
      if(Vangle(VG1.vect(),VG2.vect())>1.5 && Vangle(VG1.vect(),VG2.vect())> 8*(1-(g1_e+g2_e)/5) ){
        VPI0 = new LorentzVector(0,0,0,0);
        VPI0.add(VG1);
        VPI0.add(VG2);
        H_gg_m.fill(VPI0.mass());
      }
    }
  }

  public void makeCVT(DataBank bank, DataBank ubank, DataBank bstclusters){
    int tracks = bank.rows();
    htrks.fill(tracks);
    int tracksPos = 0;
    int tracksNeg = 0;
    for(int k = 0; k < bank.rows(); k++){
      float mom = bank.getFloat("p", k);
      float momt = bank.getFloat("pt", k);
      float tandip = bank.getFloat("tandip", k);
      float phi0 = bank.getFloat("phi0", k);
      float z0 = bank.getFloat("z0", k);
      float chi2 = bank.getFloat("chi2", k);
      float pathlength = bank.getFloat("pathlength", k);
      int ndf = bank.getInt("ndf", k);
      int q = bank.getInt("q", k);
      int cvttrack = bank.getInt("ID", k);

      float ptU = ubank.getFloat("pt", k);
      float d0U = ubank.getFloat("d0", k);
      float chi2U = ubank.getFloat("chi2", k);
      int ndfU = ubank.getInt("ndf", k);

      phi0 = (float) Math.toDegrees(phi0);
      float pz = momt * tandip;
      float theta = (float) Math.toDegrees(Math.acos(pz / Math.sqrt(pz * pz + momt * momt)));

      if (ndfU > 2 && chi2U / ndfU < 30 && ptU > 0.2) {
        if (q > 0) {
          H_CVT_d0_pos.fill(d0U);
          H_CVT_absd0_pos.fill(Math.abs(d0U));
        }
      }
      if (q > 0){
        tracksPos++;
        H_CVT_chi2_pos.fill(chi2);
        H_CVT_z_pos.fill(z0);
      }
      else if (q < 0){
        tracksNeg++;
        H_CVT_chi2_neg.fill(chi2);
        H_CVT_z_neg.fill(z0);
      }
      hndf.fill(ndf);
      hp.fill(mom);
      hpt.fill(momt);
      hpathlen.fill(pathlength);
      float chi2norm = chi2 / (float) ndf;
      hchi2norm.fill(chi2norm);
      int bmtOntrackLayers = 0;

      for (int i = 1; i < 10; ++i) {
        int crossId = bank.getShort("Cross" + i + "_ID", k);
        if (crossId == 0) continue;
        if (crossId >= 1000) bmtOntrackLayers++;
      }

      int N_BST_clusters = 0;
      for (int n = 0; n<bstclusters.rows(); n++) {
        if (bstclusters.getInt("trkID",n)!=-1 && bstclusters.getInt("trkID",n) == cvttrack) {
          N_BST_clusters++;
        }	
      }

      int bstOntrackLayers = N_BST_clusters;
      hbstOnTrkLayers.fill(bstOntrackLayers);
      hbmtOnTrkLayers.fill(bmtOntrackLayers);
      if(mom>0.15 && chi2<20000 && theta>0 && theta <180 && Math.abs(z0)<25){
        if(foundCVT==0){
          foundCVT = 1;
          CVT_mom = mom;
          CVT_theta = theta;
          CVT_phi = phi0;
          CVT_vz = z0;
          CVTcharge = bank.getInt("q", k);
          CVT_chi2 = chi2;
          CVT_ndf = ndf;
          CVT_pathlength = pathlength;
        }
      }
    }
    hpostrks.fill(tracksPos);
    hnegtrks.fill(tracksNeg);
    hpostrks_rat.fill(tracksPos);
    hnegtrks_rat.fill(tracksNeg);
  }



  public void processEvent(DataEvent event) {
    trig_part_ind=-1;e_part_ind=-1;
    Nevts++;
    e_sect=0;foundCVT=0;
    e_ecal_E = 0;e_pcal_e=0;e_etot_e=0;hasLTCC=0;e_ecal_T_PCAL=-1000;e_ecal_T_ECIN=-1000;e_ecal_T_ECOU=-1000;
    trig_track_ind = -1;e_track_ind = -1;pip_part_ind = -1;pim_part_ind = -1;

    float BSTCHANNELS = 21504;
    float BMTCHANNELS = 15000;

    if(event.hasBank("RUN::rf")){
      RFtime1=0;
      RFtime2=0;
      for(int r=0;r<event.getBank("RUN::rf").rows();r++){
        if(event.getBank("RUN::rf").getInt("id",r)==1)RFtime1=event.getBank("RUN::rf").getFloat("time",r);
        else RFtime2=event.getBank("RUN::rf").getFloat("time",r);
      }

      H_RFtimediff.fill((RFtime1-RFtime2+1000*rfPeriod) % rfPeriod);
      H_RFtimediff_corrected.fill((RFtime1-RFtime2+(rfoffset1 - rfoffset2)+1000*rfPeriod) % rfPeriod);
      RFtime1+=rfoffset1;
      RFtime2+=rfoffset2;
    }
    for(int i=1;i<7;i++)trigger_bits[i]=false;
    if(event.hasBank("RUN::config")){
      DataBank bank = event.getBank("RUN::config");
      TriggerWord = bank.getLong("trigger",0);
      int length=0;
      for (int i = 31; i >= 0; i--) {
        trigger_bits[i] = (TriggerWord & (1 << i)) != 0;
        if(length==0 && trigger_bits[i])
          length=i+1;
      }
    }
    if(trigger_bits[1]||trigger_bits[2]||trigger_bits[3]||trigger_bits[4]||trigger_bits[5]||trigger_bits[6])Ntrigs++;
    for (int i=1; i<=6; ++i) {
      if (testTriggerSector(i)) {
        H_trig_sector_count.fill(i);
      }
    }

    if (RunDependentCut.runIsBefore(runNum, 6296, true)) {
      if(trigger_bits[7])H_muon_trig_sector_count.fill(1);
      if(trigger_bits[8])H_muon_trig_sector_count.fill(2);
      if(trigger_bits[9])H_muon_trig_sector_count.fill(3);
    }
    else if (RunDependentCut.runIsAfter(runNum, 6296, false)) {
      if(trigger_bits[7] || trigger_bits[10])H_muon_trig_sector_count.fill(1);
      if(trigger_bits[8] || trigger_bits[11])H_muon_trig_sector_count.fill(2);
      if(trigger_bits[9] || trigger_bits[12])H_muon_trig_sector_count.fill(3);
    }

    DataBank partBank = null, trackBank = null, trackDetBank = null, ecalBank = null, cherenkovBank = null, scintillBank = null, crossBank = null;
    DataBank TrajBank = null;

    if(userTimeBased){
      if(event.hasBank("REC::Particle"))partBank = event.getBank("REC::Particle");
      if(event.hasBank("REC::Track"))trackBank = event.getBank("REC::Track");
      if(event.hasBank("TimeBasedTrkg::TBTracks"))trackDetBank = event.getBank("TimeBasedTrkg::TBTracks");
      if(event.hasBank("REC::Calorimeter")) ecalBank = event.getBank("REC::Calorimeter");
      if(event.hasBank("REC::Cherenkov"))cherenkovBank = event.getBank("REC::Cherenkov");
      if(event.hasBank("REC::Scintillator"))scintillBank = event.getBank("REC::Scintillator");
      if(event.hasBank("TimeBasedTrkg::TBCrosses"))crossBank = event.getBank("TimeBasedTrkg::TBCrosses");
    }
    if(!userTimeBased){
      if(event.hasBank("RECHB::Particle"))partBank = event.getBank("RECHB::Particle");
      if(event.hasBank("RECHB::Track"))trackBank = event.getBank("RECHB::Track");
      if(event.hasBank("HitBasedTrkg::HBTracks"))trackDetBank = event.getBank("HitBasedTrkg::HBTracks");
      if(event.hasBank("RECHB::Calorimeter")) ecalBank = event.getBank("RECHB::Calorimeter");
      if(event.hasBank("RECHB::Cherenkov"))cherenkovBank = event.getBank("RECHB::Cherenkov");
      if(event.hasBank("RECHB::Scintillator"))scintillBank = event.getBank("RECHB::Scintillator");
      if(event.hasBank("HitBasedTrkg::HBCrosses"))crossBank = event.getBank("HitBasedTrkg::HBCrosses");
    }

    if(event.hasBank("REC::Traj"))TrajBank = event.getBank("REC::Traj");

    if(partBank!=null)trig_part_ind = makeTrigElectron(partBank,event);
    if(trackBank!=null&&trackDetBank!=null)getTrigTBTrack(trackDetBank,trackBank);
    if(partBank!=null)makeTrigOthers(partBank,event);
    if(partBank!=null && scintillBank!=null) makeRFHistograms(partBank, scintillBank);

    if(event.hasBank("BST::adc")) {
      DataBank bstHitBank = event.getBank("BST::adc");
      int bstHits = 0;
      for (int loop = 0; loop < bstHitBank.rows(); loop++) {
        if (bstHitBank.getInt("ADC", loop) != -1) {
          bstHits++;
        }
      }
      float bstOccupancy = 100 * bstHits / BSTCHANNELS;
      hbstOccupancy.fill(bstOccupancy);
    }

    if (event.hasBank("BMT::adc")) {
      DataBank bmtHitBank = event.getBank("BMT::adc");
      int bmtHits = 0;
      for (int loop = 0; loop < bmtHitBank.rows(); loop++) {
        if (bmtHitBank.getInt("ADC", loop) > 0) {
          bmtHits++;
        }
      }
      float bmtOccupancy = 100 * bmtHits / BMTCHANNELS;
      hbmtOccupancy.fill(bmtOccupancy);
    }

    if(event.hasBank("CVTRec::Tracks") && event.hasBank("CVTRec::UTracks") && event.hasBank("BSTRec::Clusters"))
      makeCVT(event.getBank("CVTRec::Tracks"), event.getBank("CVTRec::UTracks"), event.getBank("BSTRec::Clusters"));

    if(partBank!=null){
      e_part_ind = makeElectron(partBank);
      makePhotons(partBank,event);
      pip_part_ind = makePiPlusPID(partBank);
      pim_part_ind = makePiMinusPID(partBank);
      makePiPlusPimPID(partBank);
    }
    if(e_part_ind==-1)return;
    Nelecs++;
    if(trackBank!=null)fillEBTrack(trackBank);
    LorentzVector VGS = new LorentzVector(0, 0, 0, 0);
    VGS.add(VB);
    VGS.sub(Ve);
    e_Q2 = (float) -VGS.mass2();

    if (ecalBank != null) {
      getElecEBECal(ecalBank);
    }
    if (cherenkovBank != null) {
      getElecEBCC(cherenkovBank);
    }
    if (cherenkovBank != null && TrajBank != null) {
      fillTraj_HTCC(TrajBank, cherenkovBank);
    }
    if (trackDetBank != null) {
      getTBTrack(trackDetBank);
      if (crossBank != null) {
        fillDCbanks(trackDetBank, crossBank);
      }
    }

    if (e_mom > Ebeam * 0.025 && e_ecal_E / e_mom > 0.15 && e_Q2 > 1.2 * 0.1 * Ebeam / 7 && trig_track_ind > -1 && e_sect == trig_sect) {


      if (e_sect > 0 && e_sect < 7) {
        if (testTriggerSector(e_sect)) {

          float solenoid_scale = -1.0f;
          float elec_phi_sect = e_phi;
          if (e_sect > 3 && elec_phi_sect < 0) {
            elec_phi_sect += 360;
          }
          elec_phi_sect += 30f + solenoid_scale * 35f / e_mom;
          elec_phi_sect -= 60f * (e_sect - 1);
          while (elec_phi_sect > 60) {
            elec_phi_sect -= 60;
          }
          elec_phi_sect -= 30f + solenoid_scale * 35f / e_mom;
          H_trig_vz_mom_S[e_sect - 1].fill(e_mom, e_vz);
          H_trig_ECALsampl_S[e_sect - 1].fill(e_mom, e_ecal_E / e_mom);
          H_trig_PCAL_vt_S[e_sect - 1].fill(e_ecal_T_PCAL);
          H_trig_ECIN_vt_S[e_sect - 1].fill(e_ecal_T_ECIN);
          H_trig_ECOU_vt_S[e_sect - 1].fill(e_ecal_T_ECOU);
          if (hasLTCC == 1) {
            H_trig_LTCCn_theta_S[e_sect - 1].fill(e_theta, e_LTCC);
          }
        }
      }
      if(foundCVT> 0) {
        float phiDiff = e_phi - CVT_phi - 180;
        while (phiDiff > 180) {
          phiDiff -= 360;
        }
        while (phiDiff < -180) {
          phiDiff += 360;
        }
        int NDFcut = 2;
        float vzCut = 25;
        float vz0 = 0;
        boolean vzCutIs = Math.abs(e_vz - CVT_vz - vz0) < vzCut;
        boolean PhiCutIs = true;
        boolean NDFcutIs = CVT_ndf > NDFcut;
        boolean pathCutIs = true;
        boolean ThetaCut = true;
        boolean CVT_elast = true;
        if (vzCutIs && PhiCutIs && NDFcutIs && pathCutIs && ThetaCut && CVT_elast) {
          H_CVT_chi2.fill(CVT_chi2);
        }
      }
    }
  }

  public void write() {

    ratio_to_trigger();

    TDirectory dirout = new TDirectory();
    dirout.mkdir("/elec/");
    dirout.cd("/elec/");
    for (int s = 0; s < 6; s++) {
      dirout.addDataSet(H_trig_vz_mom_S[s]);
      dirout.addDataSet(H_trig_ECALsampl_S[s]);
      dirout.addDataSet(H_trig_PCAL_vt_S[s]);
      dirout.addDataSet(H_trig_ECIN_vt_S[s]);
      dirout.addDataSet(H_trig_ECOU_vt_S[s]);
      dirout.addDataSet(H_trig_LTCCn_theta_S[s]);
    }
    dirout.mkdir("/dc/");
    dirout.cd("/dc/");
    for(int s=0;s<6;s++)dirout.addDataSet(H_dcp_vz[s],H_dcm_vz[s],H_dcm_chi2[s],H_dce_chi2[s]);
    dirout.mkdir("/trig/");
    dirout.cd("/trig/");
    dirout.addDataSet(H_trig_sector_elec_rat);
    dirout.addDataSet(H_muon_trig_sector_count, H_trig_sector_muon_rat);
    dirout.addDataSet(H_trig_sector_prot_rat, H_trig_sector_piplus_rat, H_trig_sector_piminus_rat, H_trig_sector_kplus_rat, H_trig_sector_kminus_rat,  H_trig_sector_positive_rat, H_trig_sector_negative_rat, H_trig_sector_neutral_rat);
    dirout.addDataSet(H_trig_central_prot_rat, H_trig_central_piplus_rat, H_trig_central_piminus_rat, H_trig_central_kplus_rat, H_trig_central_kminus_rat);

    dirout.mkdir("/gg/");
    dirout.cd("/gg/");
    dirout.addDataSet(H_gg_m);
    dirout.mkdir("/cvt/");
    dirout.cd("/cvt/");
    dirout.addDataSet(H_CVT_chi2);
    dirout.addDataSet(H_CVT_z_pos, H_CVT_z_neg, H_CVT_d0_pos, H_CVT_absd0_pos, H_CVT_chi2_pos, H_CVT_chi2_neg);
    dirout.addDataSet(hbstOccupancy,hbmtOccupancy,htrks,hpostrks,hnegtrks,hndf,hchi2norm,hp,hpt,hpathlen,hbstOnTrkLayers,hbmtOnTrkLayers,hpostrks_rat, hnegtrks_rat);
    dirout.mkdir("/RF/");
    dirout.cd("/RF/");
    for (int s = 0; s < 6; s++) {
      dirout.addDataSet(H_e_RFtime1_FD_S[s]);
      dirout.addDataSet(H_pip_RFtime1_FD_S[s]);
      dirout.addDataSet(H_pim_RFtime1_FD_S[s]);
      dirout.addDataSet(H_p_RFtime1_FD_S[s]);
    }
    dirout.addDataSet(H_RFtimediff, H_pip_RFtime1_CD, H_pim_RFtime1_CD, H_p_RFtime1_CD, H_RFtimediff_corrected);

    if (runNum > 0) {
      dirout.writeFile(outputDir + "/out_monitor_" + runNum + ".hipo");
    } else {
      dirout.writeFile(outputDir + "/out_monitor.hipo");
    }
  }

  public void ratio_to_trigger(){
    H_trig_sector_elec_rat.divide(H_trig_sector_count);
    H_trig_sector_prot_rat.divide(H_trig_sector_count);
    H_trig_sector_piplus_rat.divide(H_trig_sector_count);
    H_trig_sector_piminus_rat.divide(H_trig_sector_count);
    H_trig_sector_kplus_rat.divide(H_trig_sector_count);
    H_trig_sector_kminus_rat.divide(H_trig_sector_count);
    H_trig_sector_positive_rat.divide(H_trig_sector_count);
    H_trig_sector_negative_rat.divide(H_trig_sector_count);
    H_trig_sector_neutral_rat.divide(H_trig_sector_count);
    H_trig_sector_muon_rat.divide(H_muon_trig_sector_count);

    H_trig_central_prot_rat.divide(Ntrigs);
    H_trig_central_piplus_rat.divide(Ntrigs);
    H_trig_central_piminus_rat.divide(Ntrigs);
    H_trig_central_kplus_rat.divide(Ntrigs);
    H_trig_central_kminus_rat.divide(Ntrigs);
    hpostrks_rat.divide(Ntrigs);
    hnegtrks_rat.divide(Ntrigs);
  }

}
