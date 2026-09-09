package org.jlab.clas.timeline.histograms;
import java.util.*;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.GraphErrors;
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
  public int Nmuons, Nmuontrigs;
  public float rfPeriod, rfoffset1, rfoffset2;
  public int rf_large_integer;
  int[] Nmuonpairs, Ntrackspair, Ntrackspairpn, Ntrackspairnp;
  boolean[] trigger_bits;
  public float EB, Ebeam;
  public float RFtime1, RFtime2, startTime, BCG;
  public long TriggerWord;
  public float STT;
  public int trig_part_ind, trig_sect, trig_track_ind;
  public int trig_muon_sect;
  public float trig_HTCC_theta;
  public int e_part_ind, e_sect, e_track_ind, hasLTCC, ngammas, pip_part_ind, pip_track_ind, pip_sect, pim_part_ind, pim_track_ind, pim_sect, foundCVT, CVTcharge;
  public int found_e_FMM, found_eTraj, found_eHTCC;
  public float[] e_FMMmom, e_FMMtheta, e_FMMphi, e_FMMvz;
  public float e_ecal_T_PCAL, e_ecal_T_ECIN, e_ecal_T_ECOU;
  public float e_mom, e_theta, e_phi, e_vx, e_vy, e_vz, e_Ivy, e_Ivz, e_ecal_X, e_ecal_Y, e_ecal_Z, e_ecal_E, e_track_chi2, e_vert_time, e_vert_time_RF, e_Q2, e_xB, e_W;
  public float e_HTCC, e_LTCC, e_pcal_e, e_etot_e, e_TOF_X, e_TOF_Y, e_TOF_Z, e_HTCC_X, e_HTCC_Y, e_HTCC_Z, e_HTCC_tX, e_HTCC_tY, e_HTCC_tZ, e_HTCC_nphe;
  public float e_DCR1_X, e_DCR1_Y, e_DCR1_Z, e_DCR2_X, e_DCR2_Y, e_DCR2_Z, e_DCR3_X, e_DCR3_Y, e_DCR3_Z;
  public float g1_e, g1_theta, g1_phi, g2_e, g2_theta, g2_phi;
  public float pip_mom, pip_theta, pip_phi, pip_vx, pip_vy, pip_vz, pip_vert_time, pip_beta, pip_track_chi2;
  public float pim_mom, pim_theta, pim_phi, pim_vx, pim_vy, pim_vz, pim_vert_time, pim_beta, pim_track_chi2;
  public float CVT_mom, CVT_theta, CVT_phi, CVT_vz, CVT_chi2, CVT_pathlength;
  public int CVT_ndf;
  public LorentzVector VB, VT, Ve, VG1, VG2, VPI0, VPIP, VPIM;

  public H1F H_trig_sector_count;          // used in ratio_to_trigger()

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
    Nmuons=0;Nmuontrigs=0;
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

    Ntrackspair = new int[3];
    Nmuonpairs = new int[3];
    Ntrackspairpn = new int[6];
    Ntrackspairnp = new int[6];

    float tofvt1 = 0;
    float tofvt2 = 300;
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

    for(int s=0;s<7;s++){
      for(int it=0;it<10;it++){
        float thetaMin = 5+2.0f*it;
        float thetaMax = 5+2.0f*(it+1);
      }
    }
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
    H_e_LTCC_nphe = new H1F("H_e_LTCC_nphe","H_e_LTCC_nphe",100,0,100);
    H_e_LTCC_nphe.setTitle("electron LTCC nphe");
    H_e_LTCC_nphe.setTitleX("nphe");
    H_e_LTCC_xy = new H2F("H_e_LTCC_xy","H_e_LTCC_xy",100,-400,400,100,-400,400);
    H_e_LTCC_xy.setTitle("electron LTCC Y vs X");
    H_e_LTCC_xy.setTitleX("X_{ltcc}");
    H_e_LTCC_xy.setTitleY("Y_{ltcc}");
    H_e_HTCC_nphe = new H1F("H_e_HTCC_nphe","H_e_HTCC_nphe",100,0,100);
    H_e_HTCC_nphe.setTitle("electron HTCC nphe");
    H_e_HTCC_nphe.setTitleX("nphe");
    H_e_HTCC_xy = new H2F("H_e_HTCC_xy","H_e_HTCC_xy",100,-120,120,120,-120,120);
    H_e_HTCC_xy.setTitle("electron HTCC Y vs X");
    H_e_HTCC_xy.setTitleX("X_{htcc}");
    H_e_HTCC_xy.setTitleY("Y_{htcc}");
    H_e_HTCC_txy = new H2F("H_e_HTCC_txy", "H_e_HTCC_txy", 200, -100, 100, 200, -100, 100);
    H_e_HTCC_txy.setTitle("electron trajectory HTCC Y vs X");
    H_e_HTCC_txy.setTitleX("Xtraj_{htcc}");
    H_e_HTCC_txy.setTitleY("Ytraj_{htcc}");
    H_e_HTCC_nphe_txy = new H2F("H_e_HTCC_nphe_xy", "H_e_HTCC_nphe_xy", 200, -100, 100, 200, -100, 100);
    H_e_HTCC_nphe_txy.setTitle("electron HTCC mean_nphe Y vs X");
    H_e_HTCC_nphe_txy.setTitleX("X_{htcc}");
    H_e_HTCC_nphe_txy.setTitleY("Y_{htcc}");

    H_e_xB_Q2 = new H2F("H_e_xB_Q2","H_e_xB_Q2",100,0,1,100,0,EB*1.2f);
    H_e_xB_Q2.setTitle("Q^2 vs x_B");
    H_e_xB_Q2.setTitleX("x_B");
    H_e_xB_Q2.setTitleY("Q^2");
    H_e_W_Q2 = new H2F("H_e_W_Q2","H_e_W_Q2",100,0.5,EB/2.5,100,0,EB*1.2f);
    if(EB==7.0f)H_e_W_Q2 = new H2F("H_e_W_Q2","H_e_W_Q2",100,0.5,3.5,100,0,EB);
    if(EB==6.0f)H_e_W_Q2 = new H2F("H_e_W_Q2","H_e_W_Q2",100,0.5,3.5,100,0,EB);
    if(EB==2.5f)H_e_W_Q2 = new H2F("H_e_W_Q2","H_e_W_Q2",100,0.5,2,100,0,EB);
    H_e_W_Q2.setTitle("Q^2 vs W");
    H_e_W_Q2.setTitleX("W");
    H_e_W_Q2.setTitleY("Q^2");
    H_e_xB_W = new H2F("H_e_xB_W","H_e_xB_W",100,0,1,100,0.5,EB/2.5);
    if(EB==7.0f)H_e_xB_W = new H2F("H_e_xB_W","H_e_xB_W",100,0,1,100,0.5,3.5);
    if(EB==2.5f)H_e_xB_W = new H2F("H_e_xB_W","H_e_xB_W",100,0,1,100,0.5,2);
    H_e_xB_W.setTitle("x_B vs W");
    H_e_xB_W.setTitleX("x_B");
    H_e_xB_W.setTitleY("W");
    H_e_Q2 = new H1F("H_e_Q2","H_e_Q2",100,0,EB*1.2f);
    H_e_Q2.setTitle("electron Q^2");
    H_e_Q2.setTitleX("Q^2 (GeV^2)");
    H_e_xB = new H1F("H_e_xB","H_e_xB",100,0,1);
    H_e_xB.setTitle("electron xB");
    H_e_xB.setTitleX("x_{B}");
    H_e_W = new H1F("H_e_W","H_e_W",100,0.5,EB/2.5);
    if(EB==7.0f)H_e_W = new H1F("H_e_W","H_e_W",100,0.5,3.5);
    if(EB==6.0f)H_e_W = new H1F("H_e_W","H_e_W",100,0.5,3.5);
    if(EB==2.5f)H_e_W = new H1F("H_e_W","H_e_W",100,0.5,2);
    H_e_W.setTitle("electron W");
    H_e_W.setTitleX("W (GeV)");

    H_e_vz = new H1F("H_e_vz","H_e_vz",200,-25,50);
    H_e_vz.setTitle("electron longitudinal vertex");
    H_e_vz.setTitleX("v_{z} (cm)");
    H_e_vz_S1 = new H1F("H_e_vz_S1","H_e_vz_S1",100,-25,25);
    H_e_vz_S1.setTitle("electron S1 longitudinal vertex");
    H_e_vz_S1.setTitleX("v_{z} (cm)");
    H_e_vz_S2 = new H1F("H_e_vz_S2","H_e_vz_S2",100,-25,25);
    H_e_vz_S2.setTitle("electron S2 longitudinal vertex");
    H_e_vz_S2.setTitleX("v_{z} (cm)");
    H_e_vz_S3 = new H1F("H_e_vz_S3","H_e_vz_S3",100,-25,25);
    H_e_vz_S3.setTitle("electron S3 longitudinal vertex");
    H_e_vz_S3.setTitleX("v_{z} (cm)");
    H_e_vz_S4 = new H1F("H_e_vz_S4","H_e_vz_S4",100,-25,25);
    H_e_vz_S4.setTitle("electron S4 longitudinal vertex");
    H_e_vz_S4.setTitleX("v_{z} (cm)");
    H_e_vz_S5 = new H1F("H_e_vz_S5","H_e_vz_S5",100,-25,25);
    H_e_vz_S5.setTitle("electron S5 longitudinal vertex");
    H_e_vz_S5.setTitleX("v_{z} (cm)");
    H_e_vz_S6 = new H1F("H_e_vz_S6","H_e_vz_S6",100,-25,25);
    H_e_vz_S6.setTitle("electron S6 longitudinal vertex");
    H_e_vz_S6.setTitleX("v_{z} (cm)");
    e_FMMmom = new float[6];
    e_FMMtheta = new float[6];
    e_FMMphi = new float[6];
    e_FMMvz = new float[6];
    H_e_FMMmom_mom = new H2F[6][4];
    H_e_FMMtheta_theta = new H2F[6][4];
    H_e_FMMphi_phi = new H2F[6][4];
    H_e_FMMvz_vz = new H2F[6][4];
    for(int s=0;s<6;s++)for(int iP=0;iP<4;iP++){
      H_e_FMMmom_mom[s][iP] = new H2F(String.format("H_e_FMMmom_mom_S%d_p%d",s+1,iP+1),String.format("H_e_FMMmom_mom_S%d_p%d",s+1,iP+1),100,0.75,EB,100,0.75,EB);
      H_e_FMMmom_mom[s][iP].setTitle("FMM mom vs DC mom");
      H_e_FMMmom_mom[s][iP].setTitleX("DC mom");
      H_e_FMMmom_mom[s][iP].setTitleY("FMM mom");
      H_e_FMMtheta_theta[s][iP] = new H2F(String.format("H_e_FMMtheta_theta_S%d_p%d",s+1,iP+1),String.format("H_e_FMMtheta_theta_S%d_p%d",s+1,iP+1),100,0,40,100,0,180);
      H_e_FMMtheta_theta[s][iP].setTitle("FMM #theta vs DC #theta");
      H_e_FMMtheta_theta[s][iP].setTitle("DC #theta");
      H_e_FMMtheta_theta[s][iP].setTitle("FMM #theta");
      H_e_FMMphi_phi[s][iP] = new H2F(String.format("H_e_FMMphi_phi_S%d_p%d",s+1,iP+1),String.format("H_e_FMMphi_phi_S%d_p%d",s+1,iP+1),100,-180,180,100,-180,180);
      H_e_FMMphi_phi[s][iP].setTitle("FMM #phi vs DC #phi");
      H_e_FMMphi_phi[s][iP].setTitle("DC #phi");
      H_e_FMMphi_phi[s][iP].setTitle("FMM #phi");
      H_e_FMMvz_vz[s][iP] = new H2F(String.format("H_e_FMMvz_vz_S%d_p%d",s+1,iP+1),String.format("H_e_FMMvz_vz_S%d_p%d",s+1,iP+1),100,-25,25,100,-25,25);
      H_e_FMMvz_vz[s][iP].setTitle("FMM vz vs DC vz");
      H_e_FMMvz_vz[s][iP].setTitleX("DC vz (cm)");
      H_e_FMMvz_vz[s][iP].setTitleY("FMM vz (cm)");
    }

    H_e_FMMvz_S1 = new H1F("H_e_FMMvz_S1","H_e_FMMvz_S1",100,-25,25);
    H_e_FMMvz_S1.setTitle("electron S1 longitudinal vertex");
    H_e_FMMvz_S1.setTitleX("v_{z} (cm)");
    H_e_FMMvz_S1.setLineColor(2);
    H_e_FMMvz_S2 = new H1F("H_e_FMMvz_S2","H_e_FMMvz_S2",100,-25,25);
    H_e_FMMvz_S2.setTitle("electron S2 longitudinal vertex");
    H_e_FMMvz_S2.setTitleX("v_{z} (cm)");
    H_e_FMMvz_S2.setLineColor(2);
    H_e_FMMvz_S3 = new H1F("H_e_FMMvz_S3","H_e_FMMvz_S3",100,-25,25);
    H_e_FMMvz_S3.setTitle("electron S3 longitudinal vertex");
    H_e_FMMvz_S3.setTitleX("v_{z} (cm)");
    H_e_FMMvz_S3.setLineColor(2);
    H_e_FMMvz_S4 = new H1F("H_e_FMMvz_S4","H_e_FMMvz_S4",100,-25,25);
    H_e_FMMvz_S4.setTitle("electron S4 longitudinal vertex");
    H_e_FMMvz_S4.setTitleX("v_{z} (cm)");
    H_e_FMMvz_S4.setLineColor(2);
    H_e_FMMvz_S5 = new H1F("H_e_FMMvz_S5","H_e_FMMvz_S5",100,-25,25);
    H_e_FMMvz_S5.setTitle("electron S5 longitudinal vertex");
    H_e_FMMvz_S5.setTitleX("v_{z} (cm)");
    H_e_FMMvz_S5.setLineColor(2);
    H_e_FMMvz_S6 = new H1F("H_e_FMMvz_S6","H_e_FMMvz_S6",100,-25,25);
    H_e_FMMvz_S6.setTitle("electron S6 longitudinal vertex");
    H_e_FMMvz_S6.setTitleX("v_{z} (cm)");
    H_e_FMMvz_S6.setLineColor(2);
    H_e_vxy = new H2F("H_e_vxy","H_e_vxy",100,-5,5,100,-5,5);
    H_e_vxy.setTitle("electron transverse vertex");
    H_e_vxy.setTitleX("v_{x} (cm)");
    H_e_vxy.setTitleY("v_{y} (cm)");
    H_e_vz_p = new H2F("H_e_vz_p","H_e_vz_p",100,0,11,100,-25,50);
    H_e_vz_p.setTitle("electron vz vs #p");
    H_e_vz_p.setTitleX("p (GeV/c)");
    H_e_vz_p.setTitleY("v_{z} (cm)");

    H_dcm_theta_phi = new H2F("H_dcm_theta_phi","H_dcm_theta_phi",100,-180,180,100,0,40);
    H_dcm_theta_phi.setTitle("DC neg theta vs phi");
    H_dcm_theta_phi.setTitleX("#phi (^o)");
    H_dcm_theta_phi.setTitleY("#theta (^o)");
    H_dcm_theta_mom = new H2F("H_dcm_theta_mom","H_dcm_theta_mom",100,0,EB,100,0,40);
    H_dcm_theta_mom.setTitle("DC neg theta vs mom");
    H_dcm_theta_mom.setTitleX("p (GeV/c)");
    H_dcm_theta_mom.setTitleY("#theta (^o)");
    H_dcm_phi_mom = new H2F("H_dcm_phi_mom","H_dcm_phi_mom",100,0,EB,100,-180,180);
    H_dcm_phi_mom.setTitle("DC neg phi vs mom");
    H_dcm_phi_mom.setTitleX("p (GeV/c)");
    H_dcm_phi_mom.setTitleY("#phi (^o)");
    H_dcm_vz_phi = new H2F("H_dcm_vz_phi","H_dcm_vz_phi",100,-180,180,100,-25,25);
    H_dcm_vz_phi.setTitle("DC neg vz vs #phi");
    H_dcm_vz_phi.setTitleX("#phi");
    H_dcm_vz_phi.setTitleY("v_{z} (cm)");
    H_dcm_vz_p = new H2F("H_dcm_vz_p","H_dcm_vz_p",100,0,EB,100,-25,25);
    H_dcm_vz_p.setTitle("DC neg vz vs #p");
    H_dcm_vz_p.setTitleX("p (GeV/c)");
    H_dcm_vz_p.setTitleY("v_{z} (cm)");
    H_dcm_vz_theta = new H2F("H_dcm_vz_theta","H_dcm_vz_theta",100,0,40,100,-25,25);
    H_dcm_vz_theta.setTitle("DC neg vz vs #theta");
    H_dcm_vz_theta.setTitleX("#theta");
    H_dcm_vz_theta.setTitleY("v_{z} (cm)");
    H_dcm_W = new H1F("H_dcm_W","H_dcm_W",100,0,5);
    H_dcm_W.setTitle("All neg DC tracks W");
    H_dcm_W.setTitleX("W (GeV)");
    H_dcm_W_zoom = new H1F("H_dcm_W_zoom","H_dcm_W_zoom",100,0.738,1.138);
    H_dcm_W_zoom.setTitle("All neg DC tracks W");
    H_dcm_W_zoom.setTitleX("W (GeV)");
    H_dcm_R1th_R1ph = new H2F("H_dcm_R1th_R1ph","H_dcm_R1th_R1ph",100,-180,180,100,0,40);
    H_dcm_R1th_R1ph.setTitle("DC neg R1 #theta vs #phi");
    H_dcm_R1th_R1ph.setTitleX("R1 #phi (^o)");
    H_dcm_R1th_R1ph.setTitleY("R1 #theta (^o)");
    H_dcm_R1the_mom = new H2F("H_dcm_R1the_mom","H_dcm_R1the_mom",100,0,EB,100,0,40);
    H_dcm_R1the_mom.setTitle("DC neg R1 #theta vs p");
    H_dcm_R1the_mom.setTitleX("p (GeV)");
    H_dcm_R1the_mom.setTitleY("R1 #theta (^o)");
    H_dcm_R1ph_mom = new H2F("H_dcm_R1ph_mom","H_dcm_R1ph_mom",100,0,EB,100,-180,180);
    H_dcm_R1ph_mom.setTitle("DC neg R1 #phi vs p");
    H_dcm_R1ph_mom.setTitleX("p (GeV)");
    H_dcm_R1ph_mom.setTitleY("#phi (^o)");
    H_dcm_pvz_phi = new H2F("H_dcm_pvz_phi","H_dcm_pvz_phi",100,-180,180,100,-25,25);
    H_dcm_pvz_phi.setTitle("DC neg proj vz vs #phi");
    H_dcm_pvz_phi.setTitleX("R1 #phi (^o)");
    H_dcm_pvz_phi.setTitleY("proj vz (cm)");
    H_dcm_pvz_p = new H2F("H_dcm_pvz_p","H_dcm_pvz_p",100,0,EB,100,-25,25);
    H_dcm_pvz_p.setTitle("DC neg proj vz vs p");
    H_dcm_pvz_p.setTitleX("p (GeV)");
    H_dcm_pvz_p.setTitleY("proj vz (cm)");
    H_dcm_pvz_theta = new H2F("H_dcm_pvz_theta","H_dcm_pvz_theta",100,0,40,100,-25,25);
    H_dcm_pvz_theta.setTitle("DC neg proj vz vs #theta");
    H_dcm_pvz_theta.setTitleX("R1 #theta (^o)");
    H_dcm_pvz_theta.setTitleY("proj vz (cm)");
    H_dcm_pvt_pvz = new H2F("H_dcm_pvt_pvz","H_dcm_pvt_pvz",100,-25,25,100,-10,10);
    H_dcm_pvt_pvz.setTitle("DC neg projected vertex");
    H_dcm_pvt_pvz.setTitleX("proj vz (cm)");
    H_dcm_pvt_pvz.setTitleY("proj vy (cm)");
    H_dcm_phiK_mom = new H2F("H_dcm_phiK_mom","H_dcm_phiK_mom",100,0,EB,100,-30,30);
    H_dcm_phiK_mom.setTitle("DC neg #phi kick vs mom");
    H_dcm_phiK_mom.setTitleX("p (GeV)");
    H_dcm_phiK_mom.setTitleY("#Delta#phi (^o)");

    H_negHBTrk_sect = new H1F("H_negHBTrk_sect","H_negHBTrk_sect",6,0.5,6.5);
    H_negHBTrk_sect.setTitle("Neg Tracks per sect");
    H_negHBTrk_sect.setTitleX("Sector number");
    H_posHBTrk_sect = new H1F("H_posHBTrk_sect","H_posHBTrk_sect",6,0.5,6.5);
    H_posHBTrk_sect.setTitle("Pos Tracks per sect");
    H_posHBTrk_sect.setTitleX("Sector number");

    H_negTBTrk_sect = new H1F("H_negTBTrk_sect","H_negTBTrk_sect",6,0.5,6.5);
    H_negTBTrk_sect.setTitle("Neg Tracks per sect");
    H_negTBTrk_sect.setTitleX("Sector number");
    H_posTBTrk_sect = new H1F("H_posTBTrk_sect","H_posTBTrk_sect",6,0.5,6.5);
    H_posTBTrk_sect.setTitle("Pos Tracks per sect");
    H_posTBTrk_sect.setTitleX("Sector number");

    H_negRECHB_sect = new H1F("H_negRECHB_sect","H_negRECHB_sect",6,0.5,6.5);
    H_negRECHB_sect.setTitle("Neg RECHB per sect");
    H_negRECHB_sect.setTitleX("Sector number");
    H_posRECHB_sect = new H1F("H_posRECHB_sect","H_posRECHB_sect",6,0.5,6.5);
    H_posRECHB_sect.setTitle("Pos RECHB per sect");
    H_posRECHB_sect.setTitleX("Sector number");

    H_negREC_sect = new H1F("H_negREC_sect","H_negREC_sect",6,0.5,6.5);
    H_negREC_sect.setTitle("Neg REC per sect");
    H_negREC_sect.setTitleX("Sector number");
    H_posREC_sect = new H1F("H_posREC_sect","H_posREC_sect",6,0.5,6.5);
    H_posREC_sect.setTitle("Pos REC per sect");
    H_posREC_sect.setTitleX("Sector number");


    H_dcp_theta_phi = new H2F("H_dcp_theta_phi","H_dcp_theta_phi",100,-180,180,100,0,40);
    H_dcp_theta_phi.setTitle("DC pos theta vs phi");
    H_dcp_theta_phi.setTitleX("#phi (^o)");
    H_dcp_theta_phi.setTitleY("#theta (^o)");
    H_dcp_theta_mom = new H2F("H_dcp_theta_mom","H_dcp_theta_mom",100,0,EB,100,0,40);
    H_dcp_theta_mom.setTitle("DC pos theta vs mom");
    H_dcp_theta_mom.setTitleX("p (GeV/c)");
    H_dcp_theta_mom.setTitleY("#theta (^o)");
    H_dcp_phi_mom = new H2F("H_dcp_phi_mom","H_dcp_phi_mom",100,0,EB,100,-180,180);
    H_dcp_phi_mom.setTitle("DC pos phi vs mom");
    H_dcp_phi_mom.setTitleX("p (GeV/c)");
    H_dcp_phi_mom.setTitleY("#phi (^o)");
    H_dcp_vz_phi = new H2F("H_dcp_vz_phi","H_dcp_vz_phi",100,-180,180,100,-25,25);
    H_dcp_vz_phi.setTitle("DC pos vz vs #phi");
    H_dcp_vz_phi.setTitleX("#phi");
    H_dcp_vz_phi.setTitleY("v_{z} (cm)");
    H_dcp_vz_p = new H2F("H_dcp_vz_p","H_dcp_vz_p",100,0,EB,100,-25,25);
    H_dcp_vz_p.setTitle("DC pos vz vs #p");
    H_dcp_vz_p.setTitleX("p (GeV/c)");
    H_dcp_vz_p.setTitleY("v_{z} (cm)");
    H_dcp_vz_theta = new H2F("H_dcp_vz_theta","H_dcp_vz_theta",100,0,40,100,-25,25);
    H_dcp_vz_theta.setTitle("DC pos vz vs #theta");
    H_dcp_vz_theta.setTitleX("#theta");
    H_dcp_vz_theta.setTitleY("v_{z} (cm)");
    H_dcp_R1th_R1ph = new H2F("H_dcp_R1th_R1ph","H_dcp_R1th_R1ph",100,-180,180,100,0,40);
    H_dcp_R1th_R1ph.setTitle("DC pos #theta vs #phi");
    H_dcp_R1th_R1ph.setTitleX("R1 #phi (^o)");
    H_dcp_R1th_R1ph.setTitleY("R1 #theta (^o)");
    H_dcp_R1the_mom = new H2F("H_dcp_R1the_mom","H_dcp_R1the_mom",100,0,EB,100,0,40);
    H_dcp_R1the_mom.setTitle("DC pos #theta vs p");
    H_dcp_R1the_mom.setTitleX("p (GeV)");
    H_dcp_R1the_mom.setTitleY("R1 #theta (^o)");
    H_dcp_R1ph_mom = new H2F("H_dcp_R1ph_mom","H_dcp_R1ph_mom",100,0,EB,100,-180,180);
    H_dcp_R1ph_mom.setTitle("DC pos R1 #phi vs p");
    H_dcp_R1ph_mom.setTitleX("p (GeV)");
    H_dcp_R1ph_mom.setTitleY("R1 #phi (^o)");
    H_dcp_pvz_phi = new H2F("H_dcp_pvz_phi","H_dcp_pvz_phi",100,-180,180,100,-25,25);
    H_dcp_pvz_phi.setTitle("DC pos proj vz vs #phi");
    H_dcp_pvz_phi.setTitleX("R1 #phi (^o)");
    H_dcp_pvz_phi.setTitleY("proj vz (cm)");
    H_dcp_pvz_p = new H2F("H_dcp_pvz_p","H_dcp_pvz_p",100,0,EB,100,-25,25);
    H_dcp_pvz_p.setTitle("DC pos proj vz vs p");
    H_dcp_pvz_p.setTitleX("p (GeV)");
    H_dcp_pvz_p.setTitleY("proj vz (cm)");
    H_dcp_pvz_theta = new H2F("H_dcp_pvz_theta","H_dcp_pvz_theta",100,0,40,100,-25,25);
    H_dcp_pvz_theta.setTitle("DC pos proj vz vs #theta");
    H_dcp_pvz_theta.setTitleX("R1 #theta (^o)");
    H_dcp_pvz_theta.setTitleY("proj vz (cm)");
    H_dcp_pvt_pvz = new H2F("H_dcp_pvt_pvz","H_dcp_pvt_pvz",100,-25,25,100,-10,10);
    H_dcp_pvt_pvz.setTitle("DC pos projected vertex");
    H_dcp_pvt_pvz.setTitleX("proj vz (cm)");
    H_dcp_pvt_pvz.setTitleY("proj vy (cm)");
    H_dcp_phiK_mom = new H2F("H_dcp_phiK_mom","H_dcp_phiK_mom",100,0,EB,100,-30,30);
    H_dcp_phiK_mom.setTitle("DC pos #phi kick vs mom");
    H_dcp_phiK_mom.setTitleX("p (GeV)");
    H_dcp_phiK_mom.setTitleY("#Delta#phi (^o)");

    H2_dcm_vz_phi = new H2F("H2_dcm_vz_phi","H2_dcm_vz_phi",36,-180,180,100,20,35);
    //H2_dcm_vz_phi.setTitle("DC neg vz vs #phi, |#theta-22.5|<2.5");
    H2_dcm_vz_phi.setTitle("DC neg vz vs #phi");
    H2_dcm_vz_phi.setTitleX("#phi");
    H2_dcm_vz_phi.setTitleY("v_{z} (cm)");
    H2_dcp_vz_phi = new H2F("H2_dcp_vz_phi","H2_dcp_vz_phi",36,-180,180,100,20,35);
    //H2_dcp_vz_phi.setTitle("DC pos vz vs #phi, |#theta-22.5|<2.5");
    H2_dcp_vz_phi.setTitle("DC pos vz vs #phi");
    H2_dcp_vz_phi.setTitleX("#phi");
    H2_dcp_vz_phi.setTitleY("v_{z} (cm)");

    H_dcm_chi2 = new H1F[7];
    H_R1phiDm_mom = new H2F[7];
    H_R1_dcm_XY = new H2F[7];
    H_R2_dcm_XY = new H2F[7];
    H_R3_dcm_XY = new H2F[7];
    H_R1_dcm_uXY = new H2F[7];
    H_R2_dcm_uXY = new H2F[7];
    H_R3_dcm_uXY = new H2F[7];
    H_dcm_vz = new H1F[7];
    for(int s=0;s<7;s++){
      H_dcm_chi2[s] = new H1F(String.format("H_dcm_chi2_S%d",s+1),String.format("S%d #chi^2 DC neg",s+1),100,0,500);
      H_R1phiDm_mom[s] = new H2F(String.format("H_R1phiDm_mom_%d",s+1),String.format("H_R1phiDm_mom_%d",s+1),100,0,EB,100,-30,30);
      H_R1_dcm_XY[s] = new H2F(String.format("H_R1_dcm_XY_s%d",s+1),String.format("H_R1_dcm_XY_s%d",s+1),200,-100,50,200,-100,100);
      H_R2_dcm_XY[s] = new H2F(String.format("H_R2_dcm_XY_s%d",s+1),String.format("H_R2_dcm_XY_s%d",s+1),200,-150,150,200,-150,150);
      H_R3_dcm_XY[s] = new H2F(String.format("H_R3_dcm_XY_s%d",s+1),String.format("H_R3_dcm_XY_s%d",s+1),200,-200,150,200,-200,200);
      H_R1_dcm_uXY[s] = new H2F(String.format("H_R1_dcm_uXY_s%d",s+1),String.format("H_R1_dcm_uXY_s%d",s+1),200,-1,1,200,-1,1);
      H_R2_dcm_uXY[s] = new H2F(String.format("H_R2_dcm_uXY_s%d",s+1),String.format("H_R2_dcm_uXY_s%d",s+1),200,-1,1,200,-1,1);
      H_R3_dcm_uXY[s] = new H2F(String.format("H_R3_dcm_uXY_s%d",s+1),String.format("H_R3_dcm_uXY_s%d",s+1),200,-1,1,200,-1,1);
      H_dcm_vz[s] = new H1F(String.format("H_dcm_vz_s%d",s+1),String.format("H_dcm_vz_s%d",s+1),100,-25,25);
      H_dcm_vz[s].setTitle(String.format("S%d vz DC neg mom>1.5 GeV",s+1));
    }

    H_dcp_chi2 = new H1F[7];
    H_R1phiDp_mom = new H2F[7];
    H_R1_dcp_XY = new H2F[7];
    H_R2_dcp_XY = new H2F[7];
    H_R3_dcp_XY = new H2F[7];
    H_R1_dcp_uXY = new H2F[7];
    H_R2_dcp_uXY = new H2F[7];
    H_R3_dcp_uXY = new H2F[7];
    H_dcp_vz = new H1F[7];
    for(int s=0;s<7;s++){
      H_dcp_chi2[s] = new H1F(String.format("H_dcp_chi2_S%d",s+1),String.format("S%d #chi^2 DC pos",s+1),100,0,500);
      H_R1phiDp_mom[s] = new H2F(String.format("H_R1phiDp_mom_%d",s+1),String.format("H_R1phiDp_mom_%d",s+1),100,0,EB,100,-30,30);
      H_R1_dcp_XY[s] = new H2F(String.format("H_R1_dcp_XY_s%d",s+1),String.format("H_R1_dcp_XY_s%d",s+1),200,-100,50,200,-100,100);
      H_R2_dcp_XY[s] = new H2F(String.format("H_R2_dcp_XY_s%d",s+1),String.format("H_R2_dcp_XY_s%d",s+1),200,-150,150,200,-150,150);
      H_R3_dcp_XY[s] = new H2F(String.format("H_R3_dcp_XY_s%d",s+1),String.format("H_R3_dcp_XY_s%d",s+1),200,-200,150,200,-200,200);
      H_R1_dcp_uXY[s] = new H2F(String.format("H_R1_dcp_uXY_s%d",s+1),String.format("H_R1_dcp_uXY_s%d",s+1),200,-1,1,200,-1,1);
      H_R2_dcp_uXY[s] = new H2F(String.format("H_R2_dcp_uXY_s%d",s+1),String.format("H_R2_dcp_uXY_s%d",s+1),200,-1,1,200,-1,1);
      H_R3_dcp_uXY[s] = new H2F(String.format("H_R3_dcp_uXY_s%d",s+1),String.format("H_R3_dcp_uXY_s%d",s+1),200,-1,1,200,-1,1);
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

    G_accCharge = new GraphErrors();
    G_accCharge.setMarkerSize(1);
    G_accCharge.setTitleX("event number");
    G_accCharge.setTitleY("acc charge");
    G_accCharge.setTitle("acc charge");
    G_accCharge.addPoint(0, 0, 0, 0);
    G_FCcur_evn = new GraphErrors();
    G_FCcur_evn.setMarkerSize(1);
    G_FCcur_evn.setTitleX("event number");
    G_FCcur_evn.setTitleY("FCup current (nA)");
    G_FCcur_evn.setTitle("FCup current vs event number");
    G_gatedFCcur_evn = new GraphErrors();
    G_gatedFCcur_evn.setMarkerSize(1);
    G_gatedFCcur_evn.setTitleX("event number");
    G_gatedFCcur_evn.setTitleY("gated FC current (nA)");
    G_gatedFCcur_evn.setTitle("gated FC current vs event number");
    G_FC_live_ratio = new GraphErrors();
    G_FC_live_ratio.setMarkerSize(1);
    G_FC_live_ratio.setTitle("Ratios of beam currents");
    G_FC_live_ratio.setTitleX("event number");
    G_FC_live_ratio.setTitleY("gated / ungated");
    G_Clock_evn = new GraphErrors();
    G_Clock_evn.setMarkerSize(1);
    G_Clock_evn.setTitleX("event number");
    G_Clock_evn.setTitleY("Clock");
    G_Clock_evn.setTitle("Clock vs event number");
    G_gatedClock_evn = new GraphErrors();
    G_gatedClock_evn.setMarkerSize(1);
    G_gatedClock_evn.setTitleX("event number");
    G_gatedClock_evn.setTitleY("gated Clock");
    G_gatedClock_evn.setTitle("gated Clock vs event number");
    G_Clock_ratio = new GraphErrors();
    G_Clock_ratio.setMarkerSize(1);
    G_Clock_ratio.setTitle("Clock livetime");
    G_Clock_ratio.setTitleX("event number");
    G_Clock_ratio.setTitleY("gated / ungated");
    G_FCcur_evn.addPoint(0, 0, 0, 0);
    G_gatedFCcur_evn.addPoint(0, 0, 0, 0);
    G_FC_live_ratio.addPoint(0, 0, 0, 0);
    G_FC_live_ratio.addPoint(0.5,0.5,0,0);
    G_FC_live_ratio.addPoint(1,1,0,0);
    G_Clock_evn.addPoint(0,0,0,0);
    G_gatedClock_evn.addPoint(0,0,0,0);
    G_Clock_ratio.addPoint(0,0,0,0);
    G_Clock_ratio.addPoint(1,1,0,0);
  }

  public static float Phi_Calculator(float[] elec_4v, float[] prot_4v, float beam_energy) {
    float phi;
    float[] beam_4v = new float[4];
    float[] q_4v = new float[4];
    float[] lepto_3v = new float[3];
    float[] hadro_3v = new float[3];

    float proton_azimuth;
    float lept_norm, hadr_norm, scalar_product;
    phi = 0;
    beam_4v[0] = beam_energy;
    beam_4v[1] = 0;
    beam_4v[2] = 0;
    beam_4v[3] = beam_energy;

    proton_azimuth = 0;
    lept_norm = 0;
    hadr_norm = 0;
    scalar_product = 0;

    for (int i = 0; i < 4; i++) {
      q_4v[i] = beam_4v[i] - elec_4v[i];
    }

    for (int i = 1; i < 4; i++) {
      lepto_3v[i - 1] = beam_4v[1 + (i % 3)] * elec_4v[1 + ((i + 1) % 3)] - elec_4v[1 + (i % 3)] * beam_4v[1 + ((1 + i) % 3)];
      hadro_3v[i - 1] = prot_4v[1 + (i % 3)] * q_4v[1 + ((i + 1) % 3)] - q_4v[1 + (i % 3)] * prot_4v[1 + ((1 + i) % 3)];
      proton_azimuth += lepto_3v[i - 1] * prot_4v[i];
      scalar_product += lepto_3v[i - 1] * hadro_3v[i - 1];
      lept_norm += lepto_3v[i - 1] * lepto_3v[i - 1];
      hadr_norm += hadro_3v[i - 1] * hadro_3v[i - 1];
    }

    if (lept_norm > 0 && hadr_norm > 0) {
      if (scalar_product < Math.sqrt(lept_norm * hadr_norm)) {
        phi = (float) Math.toDegrees(Math.acos(scalar_product / Math.sqrt(lept_norm * hadr_norm)));
      }
      if (proton_azimuth > 0) {
        phi = 360 - phi;
      }
    }

    if (phi > 180) {
      phi = phi - 360;
    }
    if (phi < -180) {
      phi = phi + 360;
    }
    return phi;
  }

  public double Vangle(Vector3 v1, Vector3 v2){
    double res = 0;
    double l1 = v1.mag();
    double l2 = v2.mag();
    double prod = v1.dot(v2);
    if( l1 * l2 !=0 && Math.abs(prod)<l1*l2 )res = Math.toDegrees( Math.acos(prod/(l1*l2) ) );
    return res;
  }

  public void makeValidateRoads(DataBank bank){
    for (int k = 0; k < bank.rows(); k++) {
      float px = bank.getFloat("px", k);
      float py = bank.getFloat("py", k);
      float pz = bank.getFloat("pz", k);
      float mom = (float) Math.sqrt(px * px + py * py + pz * pz);
      float theta = (float) Math.toDegrees(Math.atan2(Math.sqrt(px * px + py * py), pz));
      int status = bank.getShort("status", k);
    }
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
      float px = bank.getFloat("px" , k);
      float py = bank.getFloat("py" , k);
      float pz = bank.getFloat("pz" , k);
      float vx = bank.getFloat("vx" , k);
      float vy = bank.getFloat("vy" , k);
      float vz = bank.getFloat("vz" , k);
      float mom2 = px*px+py*py+pz*pz;
      float mom = (float)Math.sqrt(px*px+py*py+pz*pz);
      float mass = mom2*(1/(beta*beta)-1);
      int status = bank.getShort("status", k);
      if (status<0) status = -status;
      boolean Forward = (status<4000);
      boolean Central = (status>=4000);

      float theta = (float)Math.toDegrees(Math.acos(pz/mom));
      float phi = (float)Math.toDegrees(Math.atan2(py,px));

      int sector = 0;
      if(q!=0 && Trackbank!=null){
        for(int l=0;l<Trackbank.rows() && sector==0 ;l++)if(Trackbank.getInt("pindex",l)==k)sector=Trackbank.getInt("sector",l);
      }
      if(q==0 && ECALbank!=null){
        for(int l=0;l<ECALbank.rows() && sector==0 ;l++)if(ECALbank.getInt("pindex",l)==k)sector=ECALbank.getInt("sector",l);
      }
      if(Forward&&sector>0){
        if(pid==2212){
          H_trig_sector_prot.fill(sector);
          H_trig_sector_prot_rat.fill(sector);
        }
        else if(pid==211){
          H_trig_sector_piplus.fill(sector);
          H_trig_sector_piplus_rat.fill(sector);
        }
        else if(pid==-211){
          H_trig_sector_piminus.fill(sector);
          H_trig_sector_piminus_rat.fill(sector);
        }
        else if(pid==321){
          H_trig_sector_kplus.fill(sector);
          H_trig_sector_kplus_rat.fill(sector);
        }
        else if(pid==-321){
          H_trig_sector_kminus.fill(sector);
          H_trig_sector_kminus_rat.fill(sector);
        }
        else if(pid==22){
          H_trig_sector_photon.fill(sector);
          H_trig_sector_photon_rat.fill(sector);
        }
        else if(pid==2112){
          H_trig_sector_neutron.fill(sector);
          H_trig_sector_neutron_rat.fill(sector);
        }
        else if(pid==45){
          H_trig_sector_deut.fill(sector);
          H_trig_sector_deut_rat.fill(sector);
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
          if (q>0 && pid==45 && mass < 5.) H_trig_central_deut_rat.fill(1);
          if (q!=0 && mom>0. && mom<10.) {
            H_CD_vz_mom.fill(mom,vz);
            H_CD_vz_theta.fill(theta,vz);
            H_CD_vz_phi.fill(phi,vz);
            H_CD_vx.fill(vx);
            H_CD_vy.fill(vy);
            H_CD_vz.fill(vz);
            H_CD_vx_vy.fill(vx,vy);
            H_CD_vx_vz.fill(vx,vz);
            H_CD_vz_vy.fill(vy,vz);
          }
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
          H_e_HTCC_nphe.fill(bank.getFloat("nphe", k));
          H_e_HTCC_xy.fill(bank.getFloat("x", k), bank.getFloat("y", k));
          e_HTCC = (float) bank.getFloat("nphe", k);
          e_HTCC_X = bank.getFloat("x", k);
          e_HTCC_Y = bank.getFloat("y", k);
          e_HTCC_Z = bank.getFloat("z", k);

        }
        else if (bank.getByte("detector", k) == 16) {
          hasLTCC = 1;
          H_e_LTCC_nphe.fill(bank.getFloat("nphe", k));
          H_e_LTCC_xy.fill(bank.getFloat("x", k), bank.getFloat("y", k));
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
          H_e_TOF_xy.fill(bank.getFloat("x", k), bank.getFloat("y", k));
          H_e_TOF_t_path.fill(bank.getFloat("time", k), bank.getFloat("path", k));
          e_vert_time = bank.getFloat("time", k) - bank.getFloat("path", k) / 29.98f;
          float time2 = (e_vert_time - RFtime2 + (rf_large_integer + 0.5f) * rfPeriod) % rfPeriod;
          time2 -= rfPeriod / 2;
          e_vert_time_RF = e_vert_time - vt;
          H_e_vt1.fill(e_vert_time_RF);
          H_e_vt2.fill(time2);
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

  public void fillOtherTOF(DataBank bank){
    for(int k = 0; k < bank.rows(); k++){
      short pind = bank.getShort("pindex",k);
      if(pind!=e_part_ind){
        H_o_TOF.fill(e_vert_time , bank.getFloat("time",k) - bank.getFloat("path",k)/29.98f);
        H_o_vt.fill(bank.getFloat("time",k) - bank.getFloat("path",k)/29.98f - e_vert_time);
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
    if (found_eTraj == 1 && found_eHTCC == 1) {
      H_e_HTCC_txy.fill(e_HTCC_tX,e_HTCC_tY);
      H_e_HTCC_nphe_txy.fill(e_HTCC_tX,e_HTCC_tY,e_HTCC_nphe);
    }
  }

  public void getTrigTBTrack(DataBank bank, DataBank recBank){
    for(int k = 0; k < bank.rows(); k++){
      if(recBank.getShort("pindex",k)==trig_part_ind && recBank.getByte("detector",k)==6)trig_track_ind = recBank.getShort("index",k);
    }
    if(trig_track_ind>-1 && trig_sect == bank.getInt("sector", trig_track_ind) ){
      e_track_chi2 = bank.getFloat("chi2" , trig_track_ind);
      e_sect = bank.getInt("sector", trig_track_ind);
      e_DCR1_X = bank.getFloat("c1_x" , trig_track_ind);
      e_DCR1_Y = bank.getFloat("c1_y" , trig_track_ind);
      e_DCR1_Z = bank.getFloat("c1_z" , trig_track_ind);
      e_DCR3_X = bank.getFloat("c3_x" , trig_track_ind);
      e_DCR3_Y = bank.getFloat("c3_y" , trig_track_ind);
      e_DCR3_Z = bank.getFloat("c3_z" , trig_track_ind);
      e_DCR2_X = bank.getFloat("t1_x" , trig_track_ind);
      e_DCR2_Y = bank.getFloat("t1_y" , trig_track_ind);
      e_DCR2_Z = bank.getFloat("t1_z" , trig_track_ind);
      Vector3 DCR1POS = new Vector3(e_DCR2_X,e_DCR2_Y,e_DCR2_Z);
      Vector3 DCR1DIR = new Vector3(bank.getFloat("t1_px" , trig_track_ind),bank.getFloat("t1_py" , trig_track_ind),bank.getFloat("t1_pz" , trig_track_ind));
      DCR1POS.rotateZ( -3.141597f*(e_sect-1)/3f );
      DCR1DIR.rotateZ(-3.141597f * (e_sect - 1) / 3f);
      float er1X = (float) DCR1POS.x();
      float er1Y = (float) DCR1POS.y();
      float er1Z = (float) DCR1POS.z();
      float er1dX = (float) DCR1DIR.x();
      float er1dY = (float) DCR1DIR.y();
      float er1dZ = (float) DCR1DIR.z();
      e_Ivy = er1Y + er1dY * (0f - er1X) / er1dX;
      e_Ivz = er1Z + er1dZ * (0f - er1X) / er1dX;
      H_trig_sector_elec.fill(e_sect);
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


  public int makeMuonPairTrigPurity(DataBank bank, DataEvent event){
    int[] sectorp;
    int[] sectorn;
    sectorn = new int[6];
    sectorp = new int[6];
    int tbit;

    for (int j=0; j<3; j++) {
      Ntrackspair[j]=0;
      Nmuonpairs[j]=0;
    }

    for (int k = 0; k < bank.rows(); k++) {
      byte q = bank.getByte("charge", k);
      int status = bank.getShort("status", k);
      if (status < 0) {
        status = -status;
      }
      boolean inDC = (status >= 2000 && status < 4000);//Only forward detectors; CND is >=4000
      if (inDC && isDCmatch(event, k) > 0) {
        int sect = isDCmatch(event, k);
        float energy_ecal_E = 0;
        float energy_ecalout_E = 0;
        float energy_pcal_E = 0;
        if (userTimeBased && event.hasBank("REC::Calorimeter")) {
          DataBank ECALbank = event.getBank("REC::Calorimeter");
          for (int l = 0; l < ECALbank.rows(); l++) {
            if (ECALbank.getShort("pindex", l) == k) {
              if (ECALbank.getInt("layer", l) == 1) {
                energy_pcal_E = ECALbank.getFloat("energy", l);
              }
              if (ECALbank.getInt("layer", l) == 4 || ECALbank.getInt("layer", l) == 7) {
                energy_ecal_E += ECALbank.getFloat("energy", l);
              }
              if (ECALbank.getInt("layer", l) == 7) {
                energy_ecalout_E = ECALbank.getFloat("energy", l);
              }
            }
          }
          if (energy_ecal_E > 0.0) {
            if (trigger_bits[sect + 6]) {
              H_muontrig_ecal_en_neg_S[sect - 1].fill(energy_ecal_E * 1000);
            }
            if (sect <= 3 && trigger_bits[sect + 9]) {
              H_muontrig_ecal_en_pos_S[sect - 1].fill(energy_ecal_E * 1000);
            }
            if (sect >= 4 && trigger_bits[sect + 3]) {
              H_muontrig_ecal_en_pos_S[sect - 1].fill(energy_ecal_E * 1000);
            }
          }
          if (energy_pcal_E > 0.0) {
            if (trigger_bits[sect + 6]) {
              H_muontrig_pcal_en_neg_S[sect - 1].fill(energy_pcal_E * 1000);
            }
            if (sect >= 1 && sect <= 3 && trigger_bits[sect + 9]) {
              H_muontrig_pcal_en_pos_S[sect - 1].fill(energy_pcal_E * 1000);
            }
            if (sect <= 6 && sect >= 4 && trigger_bits[sect + 3]) {
              H_muontrig_pcal_en_pos_S[sect - 1].fill(energy_pcal_E * 1000);
            }
          }
          if (energy_ecal_E >= 0. && energy_pcal_E >= 0.) {
            if (q == 1) {
              sectorp[sect - 1]++;
            }
            if (q == -1) {
              sectorn[sect - 1]++;
            }
          }
          if (energy_ecal_E > 0. && energy_ecalout_E > 0.) {
            if (trigger_bits[sect + 6]) {
              H_muontrig_ECECOUT_en_S[sect - 1].fill(energy_ecal_E * 1000, energy_ecalout_E * 1000.);
            }
          }
        }
      }
    }

    for (int kk = 0; kk < 3; kk++) {
      tbit = kk + 7;
      if (RunDependentCut.runIsBefore(runNum, 6296, true)) {
        if (trigger_bits[tbit]) {
          if ((sectorp[kk] + sectorn[kk]) >= 1 && (sectorp[kk + 3] + sectorn[kk + 3]) >= 1) {
            H_trig_sector_muon.fill(kk + 1);
            H_trig_sector_muon_rat.fill(kk + 1);
            Nmuonpairs[kk]++;
            Nmuons++;
            Ntrackspair[kk] = sectorp[kk] + sectorn[kk + 3];
          }
        }
      } else if (RunDependentCut.runIsInRange(runNum, 6296, 11000, false)) {
        if (trigger_bits[tbit] || trigger_bits[tbit + 3]) {
          if ((sectorp[kk] + sectorn[kk]) >= 1 && (sectorp[kk + 3] + sectorn[kk + 3]) >= 1) {
            H_trig_sector_muon.fill(kk + 1);
            H_trig_sector_muon_rat.fill(kk + 1);
            Nmuonpairs[kk]++;
            Nmuons++;
            Ntrackspair[kk] = sectorp[kk] + sectorn[kk + 3];
          }
        }
      } else if (RunDependentCut.runIsAfter(runNum, 11000, true)) {
        if (trigger_bits[tbit] || trigger_bits[tbit + 3]) {
          if ((sectorp[kk] >= 1 && (sectorn[kk + 3]) >= 1) || (sectorn[kk] >= 1 && (sectorp[kk + 3]) >= 1)) {
            H_trig_sector_muon.fill(kk + 1);
            H_trig_sector_muon_rat.fill(kk + 1);
            Nmuonpairs[kk]++;
            Nmuons++;
            Ntrackspairpn[kk] = sectorp[kk] + sectorn[kk + 3];
            Ntrackspairnp[kk] = sectorn[kk] + sectorp[kk + 3];
          }
        }
      }
    }
    return 1;
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

  public void fillHBDCbanks(DataBank bank){
    for(int k = 0; k < bank.rows(); k++){
      if(bank.getByte("q",k)<0 && e_sect!=bank.getInt("sector", k) )H_negHBTrk_sect.fill(bank.getInt("sector", k));
      if(bank.getByte("q",k)>0 && e_sect!=bank.getInt("sector", k) )H_posHBTrk_sect.fill(bank.getInt("sector", k));
    }
  }

  public void fillTBDCbanks(DataBank bank){
    for(int k = 0; k < bank.rows(); k++){
      if(bank.getByte("q",k)<0 && e_sect!=bank.getInt("sector", k) )H_negTBTrk_sect.fill(bank.getInt("sector", k));
      if(bank.getByte("q",k)>0 && e_sect!=bank.getInt("sector", k) )H_posTBTrk_sect.fill(bank.getInt("sector", k));
    }
  }

  public void fillRECHBsects(DataBank event, DataBank track){
    for(int k = 0; k < event.rows(); k++){
      if(event.getByte("charge",k)!=0){
        float chi2pid = event.getFloat("chi2pid",k);
        int trkI = -1;for(int l=0;l<track.rows();l++)if(track.getInt("pindex",l)==k)trkI=l;
        if(trkI>-1 && chi2pid<20 && track.getInt("sector",trkI)!=e_sect){
          if(event.getByte("charge",k)<0)H_negRECHB_sect.fill(track.getInt("sector",trkI));
          if(event.getByte("charge",k)>0)H_posRECHB_sect.fill(track.getInt("sector",trkI));
        }
      }
    }
  }

  public void fillRECsects(DataBank event, DataBank track){
    for(int k = 0; k < event.rows(); k++){
      if(event.getByte("charge",k)!=0){
        float chi2pid = event.getFloat("chi2pid",k);
        int trkI = -1;for(int l=0;l<track.rows();l++)if(track.getInt("pindex",l)==k)trkI=l;
        if(trkI>-1 && chi2pid<20 && track.getInt("sector",trkI)!=e_sect){
          if(event.getByte("charge",k)<0)H_negREC_sect.fill(track.getInt("sector",trkI));
          if(event.getByte("charge",k)>0)H_posREC_sect.fill(track.getInt("sector",trkI));
        }
      }
    }
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
      float theta = (float)Math.toDegrees(Math.atan2(Math.sqrt(px*px+py*py), pz));
      float phi = (float)Math.toDegrees(Math.atan2(py, px));
      double Q2 = (float)4 * Ebeam * mom * Math.pow(Math.sin(Math.toRadians(theta) / 2),2);
      float  W = (float)Math.sqrt(0.938*0.938 + 2*0.938*(Ebeam-mom) - Q2 );
      float radR1P = (float)Math.sqrt(bank.getFloat("c1_y",k)*bank.getFloat("c1_y",k)+bank.getFloat("c1_x",k)*bank.getFloat("c1_x",k));
      float thetaR1P = (float)Math.toDegrees(Math.atan2(radR1P,bank.getFloat("c1_z",k)));
      float phiR1P = (float)Math.toDegrees(Math.atan2(bank.getFloat("c1_y",k),bank.getFloat("c1_x",k)));
      float phiR1D = (float)Math.toDegrees(Math.atan2(bank.getFloat("c1_uy",k),bank.getFloat("c1_ux",k)));

      float DCR2_X = bank.getFloat("t1_x" , k);
      float DCR2_Y = bank.getFloat("t1_y" , k);
      float DCR2_Z = bank.getFloat("t1_z" , k);
      Vector3 DCR1POS = new Vector3(DCR2_X,DCR2_Y,DCR2_Z);
      Vector3 DCR1DIR = new Vector3(bank.getFloat("t1_px" , k),bank.getFloat("t1_py" , k),bank.getFloat("t1_pz" , k));
      DCR1POS.rotateZ( -3.141597f*s/3f );
      DCR1DIR.rotateZ( -3.141597f*s/3f );
      float r1X = (float)DCR1POS.x();
      float r1Y = (float)DCR1POS.y();
      float r1Z = (float)DCR1POS.z();
      float r1dX = (float)DCR1DIR.x();
      float r1dY = (float)DCR1DIR.y();
      float r1dZ = (float)DCR1DIR.z();
      float Ivy = r1Y + r1dY * (0f-r1X) / r1dX;
      float Ivz = r1Z + r1dZ * (0f-r1X) / r1dX;

      while(phiR1D>30)phiR1D-=60;
      while(phiR1D<-30)phiR1D+=60;
      if(bank.getByte("q",k)<0 && mom>0.15 && bank.getFloat("chi2" , k)<5000){
        H_dcm_theta_phi.fill(phi,theta);
        H_dcm_theta_mom.fill(mom,theta);
        H_dcm_phi_mom.fill(mom,phi);
        H_dcm_vz_phi.fill(phi,vz);
        H_dcm_vz_p.fill(mom,vz);
        H_dcm_vz_theta.fill(theta,vz);
        H_dcm_W.fill(W);
        H_dcm_W_zoom.fill(W);
        H2_dcm_vz_phi.fill(phi,vz);
        H_dcm_R1th_R1ph.fill(phiR1P,thetaR1P);
        H_dcm_R1the_mom.fill(mom,thetaR1P);
        H_dcm_R1ph_mom.fill(mom,phiR1P);
        H_dcm_pvz_phi.fill(phiR1P,Ivz);
        H_dcm_pvz_p.fill(mom,Ivz);
        H_dcm_pvz_theta.fill(thetaR1P,Ivz);
        H_dcm_pvt_pvz.fill(Ivz,Ivy);
        H_dcm_phiK_mom.fill(mom,phiR1P-phi);
        if(mom>0.15
            && bXos.getFloat("x",Xind1)*bXos.getFloat("y",Xind1)!=0
            && bXos.getFloat("x",Xind2)*bXos.getFloat("y",Xind2)!=0
            && bXos.getFloat("x",Xind3)*bXos.getFloat("y",Xind3)!=0
          ){
          H_dcm_vz[s].fill(vz);
          H_dcm_chi2[s].fill(bank.getFloat("chi2" , k));
          H_R1_dcm_XY[s].fill(bXos.getFloat("x",Xind1),bXos.getFloat("y",Xind1));
          H_R2_dcm_XY[s].fill(bXos.getFloat("x",Xind2),bXos.getFloat("y",Xind2));
          H_R3_dcm_XY[s].fill(bXos.getFloat("x",Xind3),bXos.getFloat("y",Xind3));
          H_R1_dcm_uXY[s].fill(bXos.getFloat("ux",Xind1),bXos.getFloat("uy",Xind1));
          H_R2_dcm_uXY[s].fill(bXos.getFloat("ux",Xind2),bXos.getFloat("uy",Xind2));
          H_R3_dcm_uXY[s].fill(bXos.getFloat("ux",Xind3),bXos.getFloat("uy",Xind3));
          H_R1phiDm_mom[s].fill(mom,phiR1D);
          H_dcm_vz[6].fill(vz);
          H_dcm_chi2[6].fill(bank.getFloat("chi2" , k));
          H_R1_dcm_XY[6].fill(bXos.getFloat("x",Xind1),bXos.getFloat("y",Xind1));
          H_R2_dcm_XY[6].fill(bXos.getFloat("x",Xind2),bXos.getFloat("y",Xind2));
          H_R3_dcm_XY[6].fill(bXos.getFloat("x",Xind3),bXos.getFloat("y",Xind3));
          H_R1_dcm_uXY[6].fill(bXos.getFloat("ux",Xind1),bXos.getFloat("uy",Xind1));
          H_R2_dcm_uXY[6].fill(bXos.getFloat("ux",Xind2),bXos.getFloat("uy",Xind2));
          H_R3_dcm_uXY[6].fill(bXos.getFloat("ux",Xind3),bXos.getFloat("uy",Xind3));
          H_R1phiDm_mom[6].fill(mom,phiR1D);
          }
      }
      if(bank.getByte("q",k)>0 && mom>0.15 && bank.getFloat("chi2" , k)<5000){
        H_dcp_theta_phi.fill(phi,theta);
        H_dcp_theta_mom.fill(mom,theta);
        H_dcp_phi_mom.fill(mom,phi);
        H_dcp_vz_phi.fill(phi,vz);
        H_dcp_vz_p.fill(mom,vz);
        H_dcp_vz_theta.fill(theta,vz);
        H2_dcp_vz_phi.fill(phi,vz);
        H_dcp_R1th_R1ph.fill(phiR1P,thetaR1P);
        H_dcp_R1the_mom.fill(mom,thetaR1P);
        H_dcp_R1ph_mom.fill(mom,phiR1P);
        H_dcp_pvz_phi.fill(phiR1P,Ivz);
        H_dcp_pvz_p.fill(mom,Ivz);
        H_dcp_pvz_theta.fill(thetaR1P,Ivz);
        H_dcp_pvt_pvz.fill(Ivz,Ivy);
        H_dcp_phiK_mom.fill(mom,phiR1P-phi);
        if(mom>0.15
            && bXos.getFloat("x",Xind1)*bXos.getFloat("y",Xind1)!=0
            && bXos.getFloat("x",Xind2)*bXos.getFloat("y",Xind2)!=0
            && bXos.getFloat("x",Xind3)*bXos.getFloat("y",Xind3)!=0
          ){
          H_dcp_vz[s].fill(vz);
          H_dcp_chi2[s].fill(bank.getFloat("chi2" , k));
          H_R1_dcp_XY[s].fill(bXos.getFloat("x",Xind1),bXos.getFloat("y",Xind1));
          H_R2_dcp_XY[s].fill(bXos.getFloat("x",Xind2),bXos.getFloat("y",Xind2));
          H_R3_dcp_XY[s].fill(bXos.getFloat("x",Xind3),bXos.getFloat("y",Xind3));
          H_R1_dcp_uXY[s].fill(bXos.getFloat("ux",Xind1),bXos.getFloat("uy",Xind1));
          H_R2_dcp_uXY[s].fill(bXos.getFloat("ux",Xind2),bXos.getFloat("uy",Xind2));
          H_R3_dcp_uXY[s].fill(bXos.getFloat("ux",Xind3),bXos.getFloat("uy",Xind3));
          H_R1phiDp_mom[s].fill(mom,phiR1D);
          H_dcp_vz[6].fill(vz);
          H_dcp_chi2[6].fill(bank.getFloat("chi2" , k));
          H_R1_dcp_XY[6].fill(bXos.getFloat("x",Xind1),bXos.getFloat("y",Xind1));
          H_R2_dcp_XY[6].fill(bXos.getFloat("x",Xind2),bXos.getFloat("y",Xind2));
          H_R3_dcp_XY[6].fill(bXos.getFloat("x",Xind3),bXos.getFloat("y",Xind3));
          H_R1_dcp_uXY[6].fill(bXos.getFloat("ux",Xind1),bXos.getFloat("uy",Xind1));
          H_R2_dcp_uXY[6].fill(bXos.getFloat("ux",Xind2),bXos.getFloat("uy",Xind2));
          H_R3_dcp_uXY[6].fill(bXos.getFloat("ux",Xind3),bXos.getFloat("uy",Xind3));
          H_R1phiDp_mom[6].fill(mom,phiR1D);
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
        H_g1_tf.fill(g1_phi,g1_theta);
        H_g2_tf.fill(g2_phi,g2_theta);
        H_g1_te.fill(g1_e,g1_theta);
        H_g2_te.fill(g2_e,g2_theta);
        VPI0 = new LorentzVector(0,0,0,0);
        VPI0.add(VG1);
        VPI0.add(VG2);
        H_gg_open_a.fill(VPI0.e() , Vangle(VG1.vect(),VG2.vect()) );
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
      float d0 = bank.getFloat("d0", k);
      float chi2 = bank.getFloat("chi2", k);
      float pathlength = bank.getFloat("pathlength", k);
      int ndf = bank.getInt("ndf", k);
      double vx = -d0 * Math.sin(phi0);
      double vy = d0 * Math.cos(phi0);
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
        } else {
          H_CVT_d0_neg.fill(d0U);
          H_CVT_absd0_neg.fill(Math.abs(d0U));
        }
      }
      H_CVT_charge.fill(q);
      H_CVT_d0.fill(d0);
      H_CVT_vz_mom.fill(mom,z0);
      H_CVT_vz_theta.fill(theta,z0);
      H_CVT_vz_phi.fill(phi0,z0);
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
      H_CVT_phi.fill(phi0);
      H_CVT_theta.fill(theta);
      H_CVT_vz.fill(z0);
      H_CVT_vx.fill(vx);
      H_CVT_vy.fill(vy);
      H_CVT_vx_vy.fill(vx,vy);
      H_CVT_vx_vz.fill(vx,z0);
      H_CVT_vz_vy.fill(vy,z0);
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

  public void fillTrigECAL(DataBank bank){
    int[] NhitsSect = new int[6];
    float[] ETOT = new float[6];
    float[] ECAL = new float[6];
    float E1=0;float E2=0;
    H_Nclust_ev.fill(bank.rows());
    for(int k = 0; k < 6; k++){
      NhitsSect[k]=0;
      ETOT[k]=0;
      ECAL[k]=0;
    }
    for(int k = 0; k < bank.rows(); k++){
      int layer = bank.getInt("layer", k);
      float e = bank.getFloat("energy", k);
      int sect = bank.getInt("sector", k);
      if(layer==1 && e>0.005)NhitsSect[sect-1]++;
      if(layer!=1)ECAL[sect-1]+=e;
      ETOT[sect-1]+=e;
      if(E1<e)E1=e;
    }
    for(int k = 0; k < bank.rows(); k++){
      float e = bank.getFloat("energy", k);
      if(E2<e && e<E1)E2=e;
    }
    if(bank.rows()>0)H_clust1_E.fill(E1);
    if(bank.rows()>1)H_clust2_E.fill(E2);
    for(int k = 0; k < bank.rows(); k++){
      int layer = bank.getInt("layer", k);
      float e = bank.getFloat("energy", k);
      float x = bank.getFloat("x", k);
      float y = bank.getFloat("y", k);
      int sect = bank.getInt("sector", k);
      if(layer==1 && e>0.005){
        if(testTriggerSector(1) &&sect==1&&NhitsSect[sect-1]==1){
          H_trig_S1_ETOT_E.fill(ETOT[sect-1]);
          if(ECAL[sect-1]>0)H_trig_S1_ECAL_E.fill(ECAL[sect-1]);
          H_trig_S1_PCAL_E.fill(e);H_trig_S1_PCAL_XY.fill(x,y);
        }
        else if(testTriggerSector(2) &&sect==2&&NhitsSect[sect-1]==1){
          H_trig_S2_ETOT_E.fill(ETOT[sect-1]);
          if(ECAL[sect-1]>0)H_trig_S2_ECAL_E.fill(ECAL[sect-1]);
          H_trig_S2_PCAL_E.fill(e);H_trig_S2_PCAL_XY.fill(x,y);
        }
        else if(testTriggerSector(3) &&sect==3&&NhitsSect[sect-1]==1){
          H_trig_S3_ETOT_E.fill(ETOT[sect-1]);
          if(ECAL[sect-1]>0)H_trig_S3_ECAL_E.fill(ECAL[sect-1]);
          H_trig_S3_PCAL_E.fill(e);H_trig_S3_PCAL_XY.fill(x,y);
        }
        else if(testTriggerSector(4) &&sect==4&&NhitsSect[sect-1]==1){
          H_trig_S4_ETOT_E.fill(ETOT[sect-1]);
          if(ECAL[sect-1]>0)H_trig_S4_ECAL_E.fill(ECAL[sect-1]);
          H_trig_S4_PCAL_E.fill(e);H_trig_S4_PCAL_XY.fill(x,y);
        }
        else if(testTriggerSector(5) &&sect==5&&NhitsSect[sect-1]==1){
          H_trig_S5_ETOT_E.fill(ETOT[sect-1]);
          if(ECAL[sect-1]>0)H_trig_S5_ECAL_E.fill(ECAL[sect-1]);
          H_trig_S5_PCAL_E.fill(e);H_trig_S5_PCAL_XY.fill(x,y);
        }
        else if(testTriggerSector(6) &&sect==6&&NhitsSect[sect-1]==1){
          H_trig_S6_ETOT_E.fill(ETOT[sect-1]);
          if(ECAL[sect-1]>0)H_trig_S6_ECAL_E.fill(ECAL[sect-1]);
          H_trig_S6_PCAL_E.fill(e);H_trig_S6_PCAL_XY.fill(x,y);
        }
      }
    }
  }

  public void fillTrigHTCC(DataBank bank, DataEvent event){
    int[] NhitsSect = new int[6];
    boolean[] TrkSect = new boolean[6];
    for(int k = 0; k < 6; k++){NhitsSect[k]=0;TrkSect[k]=false;}
    float NPHE=0;
    if(event.hasBank("HitBasedTrkg::HBTracks")){
      for(int k=0;k<event.getBank("HitBasedTrkg::HBTracks").rows(); k++){
        TrkSect[event.getBank("HitBasedTrkg::HBTracks").getInt("sector", k)-1]=true;
      }
    }
    for(int k = 0; k < bank.rows(); k++){
      float nphe = bank.getFloat("nphe", k);
      float x = bank.getFloat("x", k);
      float y = bank.getFloat("y", k);
      float htccphi = (float)Math.toDegrees(Math.atan2(y,x));
      int sect=0;
      if(nphe>NPHE)NPHE=nphe;
      if(Math.abs(htccphi -   0)<29)sect=1;
      if(Math.abs(htccphi -  60)<29)sect=2;
      if(Math.abs(htccphi - 120)<29)sect=3;
      if(Math.abs(htccphi - 180)<29)sect=4;
      if(Math.abs(htccphi + 180)<29)sect=4;
      if(Math.abs(htccphi + 120)<29)sect=5;
      if(Math.abs(htccphi +  60)<29)sect=6;
      if(nphe>0 && sect>0)NhitsSect[sect-1]++;
    }
    for(int k = 0; k < bank.rows(); k++){
      float nphe = bank.getFloat("nphe", k);
      float x = bank.getFloat("x", k);
      float y = bank.getFloat("y", k);
      float htccphi = (float)Math.toDegrees(Math.atan2(y,x));
      int sect=0;
      if(Math.abs(htccphi -   0)<29)sect=1;
      if(Math.abs(htccphi -  60)<29)sect=2;
      if(Math.abs(htccphi - 120)<29)sect=3;
      if(Math.abs(htccphi - 180)<29)sect=4;
      if(Math.abs(htccphi + 180)<29)sect=4;
      if(Math.abs(htccphi + 120)<29)sect=5;
      if(Math.abs(htccphi +  60)<29)sect=6;
      trig_HTCC_theta = (float)Math.toDegrees(Math.atan2(Math.sqrt(x*x+y*y),bank.getFloat("z", k)));
      // int trig_HTCC_ring;
      // if(sect>0 && testTriggerSector(sect) && NhitsSect[sect-1]==1){
      //   if(trig_HTCC_theta<10)trig_HTCC_ring=1;
      //   else if(trig_HTCC_theta>15 && trig_HTCC_theta<17)trig_HTCC_ring=2;
      //   else if(trig_HTCC_theta>20 && trig_HTCC_theta<25)trig_HTCC_ring=3;
      //   else if(trig_HTCC_theta>25 && trig_HTCC_theta<30)trig_HTCC_ring=4;
      // }
      if(true /*trig_HTCC_ring==4*/){
        if(sect==1 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S1_HTCC_n.fill(nphe);H_trig_S1_HTCC_N.fill(NPHE);H_trig_S1_HTCC_XY.fill(x,y);}
        else if(sect==2 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S2_HTCC_n.fill(nphe);H_trig_S2_HTCC_N.fill(NPHE);H_trig_S2_HTCC_XY.fill(x,y);}
        else if(sect==3 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S3_HTCC_n.fill(nphe);H_trig_S3_HTCC_N.fill(NPHE);H_trig_S3_HTCC_XY.fill(x,y);}
        else if(sect==4 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S4_HTCC_n.fill(nphe);H_trig_S4_HTCC_N.fill(NPHE);H_trig_S4_HTCC_XY.fill(x,y);}
        else if(sect==5 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S5_HTCC_n.fill(nphe);H_trig_S5_HTCC_N.fill(NPHE);H_trig_S5_HTCC_XY.fill(x,y);}
        else if(sect==6 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S6_HTCC_n.fill(nphe);H_trig_S6_HTCC_N.fill(NPHE);H_trig_S6_HTCC_XY.fill(x,y);}
        if(sect==1 && testTriggerSector(sect) && NhitsSect[sect-1]==1 && TrkSect[sect-1] ){H_trig_S1_HTCC_N_track.fill(NPHE);}
        else if(sect==2 && testTriggerSector(sect) && NhitsSect[sect-1]==1 && TrkSect[sect-1] ){H_trig_S2_HTCC_N_track.fill(NPHE);}
        else if(sect==3 && testTriggerSector(sect) && NhitsSect[sect-1]==1 && TrkSect[sect-1] ){H_trig_S3_HTCC_N_track.fill(NPHE);}
        else if(sect==4 && testTriggerSector(sect) && NhitsSect[sect-1]==1 && TrkSect[sect-1] ){H_trig_S4_HTCC_N_track.fill(NPHE);}
        else if(sect==5 && testTriggerSector(sect) && NhitsSect[sect-1]==1 && TrkSect[sect-1] ){H_trig_S5_HTCC_N_track.fill(NPHE);}
        else if(sect==6 && testTriggerSector(sect) && NhitsSect[sect-1]==1 && TrkSect[sect-1] ){H_trig_S6_HTCC_N_track.fill(NPHE);}
      }
    }
  }

  public void checkTrigECAL(DataBank DCbank, DataBank CCbank){
    for(int k = 0; k < DCbank.rows(); k++) {
      float px = DCbank.getFloat("p0_x", k);
      float py = DCbank.getFloat("p0_y", k);
      float pz = DCbank.getFloat("p0_z", k);
      float vz = DCbank.getFloat("Vtx0_z", k);
      int DCsect = DCbank.getInt("sector", k);
      float chi2 = DCbank.getFloat("chi2", k);
      float mom = (float) Math.sqrt(px * px + py * py + pz * pz);
      float theta = (float) Math.toDegrees(Math.atan2(Math.sqrt(px * px + py * py), pz));
      float phi = (float) Math.toDegrees(Math.atan2(py, px));
      if (DCbank.getByte("q", k) < 0 && mom > Ebeam * 0.15 && theta > 8 && chi2 < 1500 && Math.abs(vz) < 20) {
        for (int l = 0; l < CCbank.rows(); l++) {
          float nphe = CCbank.getFloat("nphe", l);
          if (nphe > 15) {
            if (DCsect == 1 && !testTriggerSector(1)) {
              missTrig_S1_ft.fill(phi, theta);
              missTrig_S1_mt.fill(mom, theta);
              missTrig_S1_mf.fill(mom, phi);
            }
            else if (DCsect == 2 && !testTriggerSector(2)) {
              missTrig_S2_ft.fill(phi, theta);
              missTrig_S2_mt.fill(mom, theta);
              missTrig_S2_mf.fill(mom, phi);
            }
            else if (DCsect == 3 && !testTriggerSector(3)) {
              missTrig_S3_ft.fill(phi, theta);
              missTrig_S3_mt.fill(mom, theta);
              missTrig_S3_mf.fill(mom, phi);
            }
            else if (DCsect == 4 && !testTriggerSector(4)) {
              missTrig_S4_ft.fill(phi, theta);
              missTrig_S4_mt.fill(mom, theta);
              missTrig_S4_mf.fill(mom, phi);
            }
            else if (DCsect == 5 && !testTriggerSector(5)) {
              missTrig_S5_ft.fill(phi, theta);
              missTrig_S5_mt.fill(mom, theta);
              missTrig_S5_mf.fill(mom, phi);
            }
            else if (DCsect == 6 && !testTriggerSector(6)) {
              missTrig_S6_ft.fill(phi, theta);
              missTrig_S6_mt.fill(mom, theta);
              missTrig_S6_mf.fill(mom, phi);
            }
          }
        }
      }
    }
  }

  public void fillTOFHists(DataBank bankTOF, DataBank bankDC){
    for(int k = 0; k < bankTOF.rows(); k++){
      int trackid = bankTOF.getInt("trackid",     k);
      int sector  = bankTOF.getInt("sector",      k);
      int layer   = bankTOF.getInt("layer",       k);
      float time  = bankTOF.getFloat("time",      k);
      float pathLength = bankTOF.getFloat("pathLength", k);
      if(layer==2)for(int l = 0; l < bankDC.rows(); l++){
        if(trackid == bankDC.getInt("id", l) ){
          float px = bankDC.getFloat("p0_x", l);
          float py = bankDC.getFloat("p0_y", l);
          float pz = bankDC.getFloat("p0_z", l);
          int q = bankDC.getInt("q", l);
          float chi2 = bankDC.getFloat("chi2", l);
          float vert_time = time - pathLength/29.98f;
          float mom = (float)Math.sqrt(px*px+py*py+pz*pz);
          if(chi2<250 && q<0){
            if(sector==1){
              H_TOF_vt_S1m.fill(vert_time);
              H_TOF_vt_mom_S1m.fill(mom,vert_time);
            }
            else if(sector==2){
              H_TOF_vt_S2m.fill(vert_time);
              H_TOF_vt_mom_S2m.fill(mom,vert_time);
            }
            else if(sector==3){
              H_TOF_vt_S3m.fill(vert_time);
              H_TOF_vt_mom_S3m.fill(mom,vert_time);
            }
            else if(sector==4){
              H_TOF_vt_S4m.fill(vert_time);
              H_TOF_vt_mom_S4m.fill(mom,vert_time);
            }
            else if(sector==5){
              H_TOF_vt_S5m.fill(vert_time);
              H_TOF_vt_mom_S5m.fill(mom,vert_time);
            }
            else if(sector==6){
              H_TOF_vt_S6m.fill(vert_time);
              H_TOF_vt_mom_S6m.fill(mom,vert_time);
            }
          }
          if(chi2<250 && q>0){
            if(sector==1){
              H_TOF_vt_S1p.fill(vert_time);
              H_TOF_vt_mom_S1p.fill(mom,vert_time);
            }
            else if(sector==2){
              H_TOF_vt_S2p.fill(vert_time);
              H_TOF_vt_mom_S2p.fill(mom,vert_time);
            }
            else if(sector==3){
              H_TOF_vt_S3p.fill(vert_time);
              H_TOF_vt_mom_S3p.fill(mom,vert_time);
            }
            else if(sector==4){
              H_TOF_vt_S4p.fill(vert_time);
              H_TOF_vt_mom_S4p.fill(mom,vert_time);
            }
            else if(sector==5){
              H_TOF_vt_S5p.fill(vert_time);
              H_TOF_vt_mom_S5p.fill(mom,vert_time);
            }
            else if(sector==6){
              H_TOF_vt_S6p.fill(vert_time);
              H_TOF_vt_mom_S6p.fill(mom,vert_time);
            }
          }
        }
      }
    }
  }
  public void fillEvent(DataBank recEv){
    G_accCharge.addPoint(Nevts,recEv.getFloat("beamCharge",0),0,0);
  }

  public void readScalers(DataBank rawScaler){
    // channel 0 FCups for new code and channel 32 for old code
    int chan = 0;
    // channel 2 clock slot 0 ungated slot 1 gated
    int chan2 = 2;
    float foundClock = -1;
    float foundGatedClock = -1;
    float foundFCup = -1;
    float foundGatedFCup = -1;
    boolean isLong = true;
    for (int k = 0; k < rawScaler.rows(); k++) {
      if (rawScaler.getShort("channel", k) == chan && rawScaler.getByte("slot", k) == 0) {
        if (isLong) {
          foundGatedFCup = (float) rawScaler.getLong("value", k);
        } else {
          foundGatedFCup = (float) rawScaler.getInt("value", k);
        }
      }
      if (rawScaler.getShort("channel", k) == chan && rawScaler.getByte("slot", k) == 1) {
        if (isLong) {
          foundFCup = rawScaler.getLong("value", k);
        } else {
          foundFCup = rawScaler.getInt("value", k);
        }
      }
      if (rawScaler.getShort("channel", k) == chan2 && rawScaler.getByte("slot", k) == 0) {
        if (isLong) {
          foundGatedClock = rawScaler.getLong("value", k);
        } else {
          foundGatedClock = rawScaler.getInt("value", k);
        }
      }
      if (rawScaler.getShort("channel", k) == chan2 && rawScaler.getByte("slot", k) == 1) {
        if (isLong) {
          foundClock = rawScaler.getLong("value", k);
        } else {
          foundClock = rawScaler.getInt("value", k);
        }
      }
    }
    if (!(foundFCup > 0 && foundGatedFCup > 0)) {
      chan = 32;
      for (int k = 0; k < rawScaler.rows(); k++) {
        if (rawScaler.getShort("channel", k) == chan && rawScaler.getByte("slot", k) == 0) {
          if (isLong) {
            foundGatedFCup = (float) rawScaler.getLong("value", k);
          } else {
            foundGatedFCup = (float) rawScaler.getInt("value", k);
          }
        }
        if (rawScaler.getShort("channel", k) == chan && rawScaler.getByte("slot", k) == 1) {
          if (isLong) {
            foundFCup = rawScaler.getLong("value", k);
          } else {
            foundFCup = rawScaler.getInt("value", k);
          }
        }
      }
    }
    if (foundFCup > -1 && foundGatedFCup > -1) {
      float FCtrueFreq = scalerToHertz(foundFCup);
      float gatedFCtrueFreq = scalerToHertz(foundGatedFCup);
      float beamCurrent = HertzTonA(FCtrueFreq);
      float gatedCurrent = HertzTonA(gatedFCtrueFreq);
      if (beamCurrent > 0) {
        G_FCcur_evn.addPoint(Nevts, beamCurrent, 0, 0);
      }
      if (gatedCurrent > 0) {
        G_gatedFCcur_evn.addPoint(Nevts, gatedCurrent, 0, 0);
      }
      if (beamCurrent > 0 && gatedCurrent > 0) {
        G_FC_live_ratio.addPoint(Nevts, gatedCurrent / beamCurrent, 0, 0);
      }
    }
    if (foundClock > -1 && foundGatedClock > -1) {
      float ClockFreq = scalerToHertz(foundClock);
      float gatedClockFreq = scalerToHertz(foundGatedClock);
      if (ClockFreq > 0) {
        G_Clock_evn.addPoint(Nevts, ClockFreq, 0, 0);
      }
      if (gatedClockFreq > 0) {
        G_gatedClock_evn.addPoint(Nevts, gatedClockFreq, 0, 0);
      }
      if (ClockFreq > 0 && gatedClockFreq > 0) {
        G_Clock_ratio.addPoint(Nevts, gatedClockFreq / ClockFreq, 0, 0);
      }
    }
  }

  public float scalerToHertz(float val) {
    return val / (0.03333f - 0.0005f);// 30 Hz minus 0.5 ms dead for Helicity
  }

  public float HertzTonA(float freq) {
    return (freq - 100f)/906.2f * 10.2f;
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

    if(trigger_bits[31])H_rand_trig_sector_count.fill(7);
    if (RunDependentCut.runIsBefore(runNum, 6296, true)) {
      if(trigger_bits[7]||trigger_bits[8]||trigger_bits[9])Nmuontrigs++;
      if(trigger_bits[7])H_muon_trig_sector_count.fill(1);
      if(trigger_bits[8])H_muon_trig_sector_count.fill(2);
      if(trigger_bits[9])H_muon_trig_sector_count.fill(3);
    }
    else if (RunDependentCut.runIsAfter(runNum, 6296, false)) {
      if(trigger_bits[7]||trigger_bits[8]||trigger_bits[9] || trigger_bits[10] || trigger_bits[11] || trigger_bits[12]) Nmuontrigs++;
      if(trigger_bits[7] || trigger_bits[10])H_muon_trig_sector_count.fill(1);
      if(trigger_bits[8] || trigger_bits[11])H_muon_trig_sector_count.fill(2);
      if(trigger_bits[9] || trigger_bits[12])H_muon_trig_sector_count.fill(3);
    }

    DataBank eventBank = null, partBank = null, trackBank = null, trackDetBank = null, ecalBank = null, cherenkovBank = null, scintillBank = null, crossBank = null;
    DataBank TrajBank = null;

    if(userTimeBased){
      if(event.hasBank("REC::Event"))eventBank = event.getBank("REC::Event");
      if(event.hasBank("REC::Particle"))partBank = event.getBank("REC::Particle");
      if(event.hasBank("REC::Track"))trackBank = event.getBank("REC::Track");
      if(event.hasBank("TimeBasedTrkg::TBTracks"))trackDetBank = event.getBank("TimeBasedTrkg::TBTracks");
      if(event.hasBank("REC::Calorimeter")) ecalBank = event.getBank("REC::Calorimeter");
      if(event.hasBank("REC::Cherenkov"))cherenkovBank = event.getBank("REC::Cherenkov");
      if(event.hasBank("REC::Scintillator"))scintillBank = event.getBank("REC::Scintillator");
      if(event.hasBank("TimeBasedTrkg::TBCrosses"))crossBank = event.getBank("TimeBasedTrkg::TBCrosses");
    }
    if(!userTimeBased){
      if(event.hasBank("RECHB::Event"))eventBank = event.getBank("RECHB::Event");
      if(event.hasBank("RECHB::Particle"))partBank = event.getBank("RECHB::Particle");
      if(event.hasBank("RECHB::Track"))trackBank = event.getBank("RECHB::Track");
      if(event.hasBank("HitBasedTrkg::HBTracks"))trackDetBank = event.getBank("HitBasedTrkg::HBTracks");
      if(event.hasBank("RECHB::Calorimeter")) ecalBank = event.getBank("RECHB::Calorimeter");
      if(event.hasBank("RECHB::Cherenkov"))cherenkovBank = event.getBank("RECHB::Cherenkov");
      if(event.hasBank("RECHB::Scintillator"))scintillBank = event.getBank("RECHB::Scintillator");
      if(event.hasBank("HitBasedTrkg::HBCrosses"))crossBank = event.getBank("HitBasedTrkg::HBCrosses");
    }

    if(event.hasBank("REC::Traj"))TrajBank = event.getBank("REC::Traj");

    if(event.hasBank("RAW::scaler"))readScalers(event.getBank("RAW::scaler"));
    if(eventBank != null)fillEvent(eventBank);

    if(partBank!=null)trig_part_ind = makeTrigElectron(partBank,event);
    if(event.hasBank("HTCC::rec"))fillTrigHTCC(event.getBank("HTCC::rec"),event);
    if(event.hasBank("ECAL::clusters"))fillTrigECAL(event.getBank("ECAL::clusters"));
    if(trackBank!=null&&trackDetBank!=null)getTrigTBTrack(trackDetBank,trackBank);
    if(partBank!=null)makeTrigOthers(partBank,event);
    if(partBank!=null && scintillBank!=null) makeRFHistograms(partBank, scintillBank);
    if(partBank!=null)makeMuonPairTrigPurity(partBank,event);
    if(trackDetBank!=null && event.hasBank("HTCC::rec"))checkTrigECAL(trackDetBank,event.getBank("HTCC::rec"));
    if(trackDetBank!=null && event.hasBank("FTOF::hits"))fillTOFHists(event.getBank("FTOF::hits") ,trackDetBank);

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
      makeValidateRoads(partBank);
    }
    if(e_part_ind==-1)return;
    Nelecs++;
    if(trackBank!=null)fillEBTrack(trackBank);
    found_e_FMM=0;
    LorentzVector VGS = new LorentzVector(0, 0, 0, 0);
    VGS.add(VB);
    VGS.sub(Ve);
    e_Q2 = (float) -VGS.mass2();
    e_xB = e_Q2 / (2f * 0.93827f * (Ebeam - e_mom));
    e_W = (float) Math.sqrt(0.93827f * 0.93827f + e_Q2 * (1f / e_xB - 1f));

    if (ecalBank != null) {
      getElecEBECal(ecalBank);
    }
    if (cherenkovBank != null) {
      getElecEBCC(cherenkovBank);
    }
    if (cherenkovBank != null && TrajBank != null) {
      fillTraj_HTCC(TrajBank, cherenkovBank);
    }
    if (scintillBank != null) {
      if (partBank != null) {
        getElecEBTOF(scintillBank, partBank);
      }
      fillOtherTOF(scintillBank);
    }
    if (trackDetBank != null) {
      getTBTrack(trackDetBank);
      if (crossBank != null) {
        fillDCbanks(trackDetBank, crossBank);
      }
    }

    if (event.hasBank("RECHB::Event") && event.hasBank("RECHB::Track")) {
      fillRECHBsects(event.getBank("RECHB::Particle"), event.getBank("RECHB::Track"));
    }
    if (event.hasBank("REC::Event") && event.hasBank("REC::Track")) {
      fillRECsects(event.getBank("REC::Particle"), event.getBank("REC::Track"));
    }

    if (event.hasBank("HitBasedTrkg::HBTracks")) {
      fillHBDCbanks(event.getBank("HitBasedTrkg::HBTracks"));
    }
    if (event.hasBank("TimeBasedTrkg::TBTracks")) {
      fillTBDCbanks(event.getBank("TimeBasedTrkg::TBTracks"));
    }

    if (e_mom > Ebeam * 0.025 && e_ecal_E / e_mom > 0.15 && e_Q2 > 1.2 * 0.1 * Ebeam / 7 && trig_track_ind > -1 && e_sect == trig_sect) {

      H_e_theta_phi.fill(e_phi, e_theta);
      H_e_theta_mom.fill(e_mom, e_theta);

      if (e_sect > 0 && e_sect < 7) {

        if (trigger_bits[31]) {
          H_rand_trig_sector_count.fill(e_sect);
        }

        H_e_theta_mom_S[e_sect - 1].fill(e_mom, e_theta);

        if (testTriggerSector(e_sect)) {

          H_trig_theta_mom_S[e_sect - 1].fill(e_mom, e_theta);
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
          H_trig_phi_mom_S[e_sect - 1].fill(e_mom, elec_phi_sect);
          H_trig_theta_phi_S[e_sect - 1].fill(elec_phi_sect, e_theta);
          H_e_W_phi_S[e_sect - 1].fill(elec_phi_sect, e_W);
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
      H_e_vz.fill(e_vz);
      if (e_sect == 1) {
        H_e_vz_S1.fill(e_vz);
      }
      else if (e_sect == 2) {
        H_e_vz_S2.fill(e_vz);
      }
      else if (e_sect == 3) {
        H_e_vz_S3.fill(e_vz);
      }
      else if (e_sect == 4) {
        H_e_vz_S4.fill(e_vz);
      }
      else if (e_sect == 5) {
        H_e_vz_S5.fill(e_vz);
      }
      else if (e_sect == 6) {
        H_e_vz_S6.fill(e_vz);
      }
      if (found_e_FMM == 1) {
        for (int iP = 0; iP < 4; iP++) {
          H_e_FMMmom_mom[e_sect - 1][iP].fill(e_mom, e_FMMmom[iP]);
          H_e_FMMtheta_theta[e_sect - 1][iP].fill(e_theta, e_FMMtheta[iP]);
          H_e_FMMphi_phi[e_sect - 1][iP].fill(e_phi, e_FMMphi[iP]);
          H_e_FMMvz_vz[e_sect - 1][iP].fill(e_vz, e_FMMvz[iP]);
        }
        if (e_sect == 1) {
          H_e_FMMvz_S1.fill(e_FMMvz[1]);
        }
        else if (e_sect == 2) {
          H_e_FMMvz_S2.fill(e_FMMvz[1]);
        }
        else if (e_sect == 3) {
          H_e_FMMvz_S3.fill(e_FMMvz[1]);
        }
        else if (e_sect == 4) {
          H_e_FMMvz_S4.fill(e_FMMvz[1]);
        }
        else if (e_sect == 5) {
          H_e_FMMvz_S5.fill(e_FMMvz[1]);
        }
        else if (e_sect == 6) {
          H_e_FMMvz_S6.fill(e_FMMvz[1]);
        }
      }
      H_e_vz_p.fill(e_mom, e_vz);
      H_e_vxy.fill(e_vx, e_vy);
      H_e_xB_Q2.fill(e_xB, e_Q2);
      H_e_W_Q2.fill(e_W, e_Q2);
      H_e_xB_W.fill(e_xB, e_W);
      H_e_Q2.fill(e_Q2);
      H_e_xB.fill(e_xB);
      H_e_W.fill(e_W);
      if (e_sect > 0 && e_sect < 7 && testTriggerSector(e_sect)) {
        H_e_W_S[e_sect - 1].fill(e_W);
        H_e_Q2_S[e_sect - 1].fill(e_Q2);
      }
      if (pim_part_ind == -1 && pip_part_ind > -1 && Math.abs(pip_vert_time - e_vert_time) < 5 && Math.abs(pip_beta - 1) < 0.1 && pip_track_chi2 < 500 && e_track_chi2 < 500) {
        H_pip_beta_p.fill(pip_mom, pip_beta);
      }
      if (pim_part_ind == -1 && pip_part_ind > -1 && Math.abs(pip_vert_time - e_vert_time) < 5 && Math.abs(pip_beta - 1) < (0.01 + 0.025 / pip_mom)
          && pip_track_chi2 < 2000 && e_track_chi2 < 2000 && pip_mom > 1) {
        LorentzVector VNeutr = new LorentzVector(0, 0, 0, 0);
        VNeutr.add(VB);
        VNeutr.add(VT);
        VNeutr.sub(Ve);
        VNeutr.sub(VPIP);
        H_MM_epip.fill(VNeutr.mass());
        H_MM_epip_zoom.fill(VNeutr.mass());
        if (pip_sect > 0 && pip_sect < 7) {
          H_MM_epip_Spip[pip_sect - 1].fill(VNeutr.mass());
        }
        if (e_sect > 0 && e_sect < 7) {
          H_MM_epip_Se[e_sect - 1].fill(VNeutr.mass());
        }
        H_pip_theta_phi.fill(pip_phi, pip_theta);
        H_pip_theta_mom.fill(pip_mom, pip_theta);
        H_pip_phi_mom.fill(pip_mom, pip_phi);
        H_pip_vz_phi.fill(pip_phi, pip_vz);
        H_pip_vz_theta.fill(pip_theta, pip_vz);
        H_pip_vz_mom.fill(pip_mom, pip_vz);
        H_pip_e_vt.fill(e_vert_time, pip_vert_time);
        H_MM_epip_phi.fill(pip_phi, VNeutr.mass());
        H_pip_beta2_p.fill(pip_mom, pip_beta);
        H_pip_vtd.fill(pip_vert_time - e_vert_time);
        H_pip_vtd_mom.fill(pip_mom, pip_vert_time - e_vert_time);
        H_pip_vtd_theta.fill(pip_theta, pip_vert_time - e_vert_time);
        H_pip_vtd_phi.fill(pip_phi, pip_vert_time - e_vert_time);
        H_pip_vz_ve.fill(e_vz, pip_vz);
        H_pip_vz_ve_diff.fill(e_vz - pip_vz);
        H_pip_vz_ve_diff_mom.fill(pip_mom, e_vz - pip_vz);
        H_pip_vz_ve_diff_theta.fill(pip_theta, e_vz - pip_vz);
        H_pip_vz_ve_diff_phi.fill(pip_phi, e_vz - pip_vz);
        float DelPhi = pip_phi - e_phi - 180;
        while (DelPhi > 180) {
          DelPhi -= 360;
        }
        while (DelPhi < -180) {
          DelPhi += 360;
        }
        H_pip_Dphi.fill(DelPhi);
        H_pip_vz_ve_diff_Dphi.fill(DelPhi, e_vz - pip_vz);
        H_epip_e_theta_phi.fill(e_phi, e_theta);
        H_epip_e_theta_mom.fill(e_mom, e_theta);
        H_epip_e_phi_mom.fill(e_mom, e_phi);
        H_epip_xB_Q2.fill(e_xB, e_Q2);
        H_epip_e_W_Q2.fill(e_W, e_Q2);
        float[] elec_4v = {(float) Ve.e(), (float) Ve.px(), (float) Ve.py(), (float) Ve.pz()};
        float[] neut_4v = {(float) VNeutr.e(), (float) VNeutr.px(), (float) VNeutr.py(), (float) VNeutr.pz()};
        float epip_phi = Phi_Calculator(elec_4v, neut_4v, 10.6f);
        VNeutr.sub(VT);
        float epip_t = (float) -VNeutr.mass2();
        H_epip_e_t_phi.fill(epip_phi, epip_t);
          }

      //electron-pi minus
      if( pip_part_ind==-1 && pim_part_ind>-1 && Math.abs(pim_vert_time-e_vert_time)<5 && Math.abs(pim_beta-1) <(0.01 + 0.025/pim_mom)
          && pim_track_chi2<2000 && e_track_chi2<2000 && pim_mom>1)
      {
        H_pim_vtd.fill(pim_vert_time-e_vert_time);
      }

      //two pions
      if(pim_part_ind>-1 && pip_part_ind>-1 && pim_track_chi2<750 && pip_track_chi2<750 && e_track_chi2<750){
        LorentzVector VRHO = new LorentzVector(0,0,0,0);
        VRHO.add(VPIP);
        VRHO.add(VPIM);
        LorentzVector VPROT = new LorentzVector(0,0,0,0);
        VPROT.add(VB);
        VPROT.add(VT);
        VPROT.sub(Ve);
        VPROT.sub(VRHO);
        if(pip_beta>0.95 && pim_beta>0.9){
          H_rho_prot.fill(VRHO.mass(),VPROT.mass());
          H_rho_pip_beta.fill(pip_mom,pip_beta);
          H_rho_pim_beta.fill(pim_mom,pim_beta);
          H_rho_IM.fill(VRHO.mass());
          H_rho_MM.fill(VPROT.mass());
          H_rho_Q2_xB.fill(e_xB,e_Q2);
          H_rho_Q2_W.fill(e_W,e_Q2);
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
        float CVT_eth = 2 * 57.296f * (float) Math.atan(0.93827 / ((Ebeam + 0.93827) * Math.tan((CVT_theta) / 57.296)));

        float chi2cut = 200;
        int NDFcut = 2;
        float vzCut = 25;
        float vz0 = 0;
        boolean vzCutIs = Math.abs(e_vz - CVT_vz - vz0) < vzCut;
        boolean PhiCutIs = true;
        boolean NDFcutIs = CVT_ndf > NDFcut;
        boolean chi2CutIs = CVT_chi2 < chi2cut;
        boolean pathCutIs = true;
        boolean ThetaCut = true;
        boolean CVT_elast = true;
        if (PhiCutIs && NDFcutIs && chi2CutIs && pathCutIs && ThetaCut && CVT_elast) {
          H_CVT_e_corr_vz.fill(e_vz, CVT_vz);
        }
        if (vzCutIs && NDFcutIs && chi2CutIs && pathCutIs && ThetaCut && CVT_elast) {
          H_CVT_e_corr_phi.fill(e_phi, CVT_phi);
        }
        if (vzCutIs && PhiCutIs && chi2CutIs && pathCutIs && ThetaCut && CVT_elast) {
          H_CVT_ndf.fill(CVT_ndf);
        }
        if (vzCutIs && PhiCutIs && NDFcutIs && pathCutIs && ThetaCut && CVT_elast) {
          H_CVT_chi2.fill(CVT_chi2);
        }
        if (vzCutIs && PhiCutIs && NDFcutIs && chi2CutIs && pathCutIs && ThetaCut && CVT_elast) {
          H_CVT_p.fill(CVT_mom);
          H_CVT_t.fill(CVT_theta);
          H_CVT_f.fill(CVT_phi);
          H_CVT_z.fill(CVT_vz);
          H_CVT_ft.fill(CVT_phi, CVT_theta);
          H_CVT_pt.fill(CVT_theta, CVT_mom);
          H_CVT_pf.fill(CVT_phi, CVT_mom);
          H_CVT_zf.fill(CVT_phi, CVT_vz);
          H_CVT_zp.fill(CVT_mom, CVT_vz);
          H_CVT_zt.fill(CVT_theta, CVT_vz);
          H_CVT_e_vz_diff.fill(e_vz - CVT_vz);
          H_CVT_e_phi_diff.fill(phiDiff);
          H_CVT_pathlength.fill(CVT_pathlength);
          float CVT_emom = Ebeam / (1 + 2 * Ebeam / 0.93827f * (float) Math.pow(Math.sin(CVT_eth / (2 * 57.296)), 2));
        }
      }
    }
  }

  public void write() {

    ratio_to_trigger();

    TDirectory dirout = new TDirectory();
    dirout.mkdir("/elec/");
    dirout.cd("/elec/");
    dirout.addDataSet(H_e_vz_p, H_e_HTCC_xy, H_e_HTCC_nphe, H_e_HTCC_nphe_txy, H_e_LTCC_xy, H_e_LTCC_nphe, H_e_TOF_xy);
    dirout.addDataSet(H_e_vt1, H_e_vt2, H_e_vz, H_e_TOF_t_path, H_o_TOF, H_o_vt);
    dirout.addDataSet(H_e_vz_S1, H_e_vz_S2, H_e_vz_S3, H_e_vz_S4, H_e_vz_S5, H_e_vz_S6);
    dirout.addDataSet(H_e_xB, H_e_xB_Q2, H_e_W_Q2);
    for (int s = 0; s < 6; s++) {
      dirout.addDataSet(H_e_theta_mom_S[s], H_e_W_S[s], H_e_W_phi_S[s]);
    }
    for (int s = 0; s < 6; s++) {
      dirout.addDataSet(H_trig_theta_mom_S[s]);
      dirout.addDataSet(H_trig_phi_mom_S[s]);
      dirout.addDataSet(H_trig_theta_phi_S[s]);
      dirout.addDataSet(H_trig_vz_mom_S[s]);
      dirout.addDataSet(H_trig_ECALsampl_S[s]);
      dirout.addDataSet(H_trig_PCAL_vt_S[s]);
      dirout.addDataSet(H_trig_ECIN_vt_S[s]);
      dirout.addDataSet(H_trig_ECOU_vt_S[s]);
      dirout.addDataSet(H_trig_LTCCn_theta_S[s]);
    }
    dirout.mkdir("/tof/");
    dirout.cd("/tof/");
    dirout.addDataSet(H_TOF_vt_S1m,H_TOF_vt_S2m,H_TOF_vt_S3m,H_TOF_vt_S4m,H_TOF_vt_S5m,H_TOF_vt_S6m);
    dirout.addDataSet(H_TOF_vt_S1p,H_TOF_vt_S2p,H_TOF_vt_S3p,H_TOF_vt_S4p,H_TOF_vt_S5p,H_TOF_vt_S6p);
    dirout.addDataSet(H_pip_vtd, H_pim_vtd);
    dirout.mkdir("/dc/");
    dirout.cd("/dc/");
    dirout.addDataSet(H_dcm_theta_phi,H_dcm_theta_mom,H_dcm_phi_mom,H_dcm_vz_phi,H_dcm_vz_p,H_dcm_vz_theta);
    dirout.addDataSet(H_dcm_R1th_R1ph,H_dcm_R1the_mom,H_dcm_R1ph_mom,H_dcm_pvz_phi,H_dcm_pvz_p,H_dcm_pvz_theta);
    dirout.addDataSet(H_dcp_theta_phi,H_dcp_theta_mom,H_dcp_phi_mom,H_dcp_vz_phi,H_dcp_vz_p,H_dcp_vz_theta);
    dirout.addDataSet(H_dcp_R1th_R1ph,H_dcp_R1the_mom,H_dcp_R1ph_mom,H_dcp_pvz_phi,H_dcp_pvz_p,H_dcp_pvz_theta);
    dirout.addDataSet(H_negHBTrk_sect,H_negTBTrk_sect,H_posHBTrk_sect,H_posTBTrk_sect,H_dcm_phiK_mom,H_dcp_phiK_mom,H_dcm_pvt_pvz,H_dcp_pvt_pvz);
    dirout.addDataSet(H_negRECHB_sect , H_posRECHB_sect , H_negREC_sect , H_posREC_sect);
    for(int s=0;s<6;s++)dirout.addDataSet(H_dcp_vz[s],H_dcp_chi2[s],H_dcm_vz[s],H_dcm_chi2[s],H_dce_chi2[s]);
    dirout.mkdir("/trig/");
    dirout.cd("/trig/");
    dirout.addDataSet(H_trig_sector_count,H_trig_sector_elec,H_trig_sector_elec_rat,H_rand_trig_sector_count,H_Nclust_ev,H_clust1_E,H_clust2_E);
    dirout.addDataSet(H_trig_sector_prot, H_trig_sector_piplus, H_trig_sector_piminus, H_trig_sector_kplus, H_trig_sector_kminus, H_trig_sector_photon, H_trig_sector_neutron);
    dirout.addDataSet(H_muon_trig_sector_count, H_trig_sector_muon, H_trig_sector_muon_rat, H_trig_sector_muontrack, H_trig_sector_muontrack_rat);
    if (H_trig_sector_deut != null) {
      dirout.addDataSet(H_trig_sector_deut);
    }
    dirout.addDataSet(H_trig_sector_prot_rat, H_trig_sector_piplus_rat, H_trig_sector_piminus_rat, H_trig_sector_kplus_rat, H_trig_sector_kminus_rat, H_trig_sector_photon_rat, H_trig_sector_neutron_rat, H_trig_sector_positive_rat, H_trig_sector_negative_rat, H_trig_sector_neutral_rat);
    if (H_trig_sector_deut_rat != null) {
      dirout.addDataSet(H_trig_sector_deut_rat);
    }
    dirout.addDataSet(H_trig_central_prot_rat, H_trig_central_deut_rat, H_trig_central_piplus_rat, H_trig_central_piminus_rat, H_trig_central_kplus_rat, H_trig_central_kminus_rat);

    dirout.addDataSet(H_trig_S1_ETOT_E, H_trig_S2_ETOT_E, H_trig_S3_ETOT_E, H_trig_S4_ETOT_E, H_trig_S5_ETOT_E, H_trig_S6_ETOT_E);
    dirout.addDataSet(H_trig_S1_ECAL_E, H_trig_S2_ECAL_E, H_trig_S3_ECAL_E, H_trig_S4_ECAL_E, H_trig_S5_ECAL_E, H_trig_S6_ECAL_E);
    dirout.addDataSet(H_trig_S1_PCAL_E, H_trig_S2_PCAL_E, H_trig_S3_PCAL_E, H_trig_S4_PCAL_E, H_trig_S5_PCAL_E, H_trig_S6_PCAL_E);
    dirout.addDataSet(H_trig_S1_PCAL_XY, H_trig_S2_PCAL_XY, H_trig_S3_PCAL_XY, H_trig_S4_PCAL_XY, H_trig_S5_PCAL_XY, H_trig_S6_PCAL_XY);
    dirout.addDataSet(H_trig_S1_HTCC_n, H_trig_S2_HTCC_n, H_trig_S3_HTCC_n, H_trig_S4_HTCC_n, H_trig_S5_HTCC_n, H_trig_S6_HTCC_n);
    dirout.addDataSet(H_trig_S1_HTCC_N, H_trig_S2_HTCC_N, H_trig_S3_HTCC_N, H_trig_S4_HTCC_N, H_trig_S5_HTCC_N, H_trig_S6_HTCC_N);
    dirout.addDataSet(H_trig_S1_HTCC_XY, H_trig_S2_HTCC_XY, H_trig_S3_HTCC_XY, H_trig_S4_HTCC_XY, H_trig_S5_HTCC_XY, H_trig_S6_HTCC_XY);
    dirout.mkdir("/gg/");
    dirout.cd("/gg/");
    dirout.addDataSet(H_gg_m, H_gg_open_a, H_g1_tf, H_g1_te, H_g2_tf, H_g2_te);
    dirout.mkdir("/cvt/");
    dirout.cd("/cvt/");
    dirout.addDataSet(H_CVT_chi2, H_CVT_ndf,H_CVT_ft,H_CVT_pt,H_CVT_pf,H_CVT_zf,H_CVT_zp,H_CVT_zt,H_CVT_e_corr_vz);
    dirout.addDataSet(H_CVT_z, H_CVT_z_pos, H_CVT_z_neg, H_CVT_d0_pos, H_CVT_d0_neg, H_CVT_absd0_pos, H_CVT_absd0_neg, H_CVT_chi2_pos, H_CVT_chi2_neg);
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
    H_trig_sector_photon_rat.divide(H_trig_sector_count);
    H_trig_sector_neutron_rat.divide(H_trig_sector_count);
    H_trig_sector_deut_rat.divide(H_trig_sector_count);
    H_trig_sector_positive_rat.divide(H_trig_sector_count);
    H_trig_sector_negative_rat.divide(H_trig_sector_count);
    H_trig_sector_neutral_rat.divide(H_trig_sector_count);
    H_trig_sector_muon_rat.divide(H_muon_trig_sector_count);

    H_trig_central_prot_rat.divide(Ntrigs);
    H_trig_central_piplus_rat.divide(Ntrigs);
    H_trig_central_piminus_rat.divide(Ntrigs);
    H_trig_central_kplus_rat.divide(Ntrigs);
    H_trig_central_kminus_rat.divide(Ntrigs);
    H_trig_central_deut_rat.divide(Ntrigs);
    hpostrks_rat.divide(Ntrigs);
    hnegtrks_rat.divide(Ntrigs);
  }

}
