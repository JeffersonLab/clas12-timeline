package org.jlab.clas12.monitoring;
import java.io.*;
import java.util.*;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.TDirectory;
import org.jlab.clas.physics.Vector3;
import org.jlab.clas.physics.LorentzVector;
import org.jlab.groot.base.GStyle;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.ConstantsManager;

public class monitor2p2GeV {

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
        if (runNum > 18300 && runNum <= 19131) {
            // RG-D:   used three different primary electron triggers (0/7/14):
            return testTriggerSector(TriggerWord, sector, 0x4081);
        }
        if (runNum > 16043 && runNum < 16078) {
            // RG-C 2.2 GeV:  non-standard primary electron trigger:
            return testTriggerSector(TriggerWord, sector, 0x4081);
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
	public int trig_part_ind, trig_sect, trig_track_ind, trig_HTCC_ring;
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

	public GraphErrors G_FCcur_evn, G_gatedFCcur_evn, G_FC_live_ratio, G_accCharge;
	public GraphErrors G_Clock_evn, G_gatedClock_evn, G_Clock_ratio;

	public H1F[][] H_trig_phi_theta_S;
	public H1F[] H_trig_PCAL_vt_S, H_trig_ECIN_vt_S, H_trig_ECOU_vt_S;
	public H2F[] H_trig_theta_mom_S, H_trig_phi_mom_S, H_trig_theta_phi_S, H_trig_vz_mom_S, H_trig_vy_vz_S, H_trig_vz_theta_S;
	public H2F[] H_trig_ECALsampl_S, H_trig_PCALECAL_S, H_trig_HTCCn_theta_S, H_trig_LTCCn_theta_S;
    public H2F[] H_trig_ECAL_pos_S, H_trig_TOF_pos_S, H_trig_HTCC_pos_S, H_trig_DCR1_pos_S, H_trig_DCR2_pos_S, H_trig_DCR3_pos_S;
	public H1F[] H_trig_S_HTCC_theta;

	public H2F[] H_e_theta_mom_S;
	public H1F[] H_e_W_S;
	public H1F[] H_e_Q2_S;
	public H2F[] H_e_W_phi_S;
    public H2F H_e_theta_phi, H_e_theta_mom, H_e_phi_mom, H_XY_ECal, H_ESampl_ECal, H_e_LTCC_xy, H_e_HTCC_xy, H_e_HTCC_txy, H_e_HTCC_nphe_txy, H_e_vxy, H_e_vz_phi, H_e_vz_p, H_e_vz_theta, H_e_TOF_xy, H_e_TOF_t_path;
    public H2F H_e_xB_Q2, H_e_W_Q2, H_e_xB_W;
	public H1F H_e_W, H_e_Q2, H_e_xB, H_e_vz, H_e_LTCC_nphe, H_e_HTCC_nphe, H_e_vt1, H_e_vt2;

    public H2F[] H_muontrig_theta_mom_S;
    public H2F H_muontrig_theta_phi, H_muontrig_theta_mom, H_muontrig_phi_mom, H_muontrig_XY_ECal, H_muontrig_ESampl_ECal, H_muontrig_LTCC_xy, H_muontrig_HTCC_xy, H_muontrig_HTCC_txy;
	public H2F H_muontrig_HTCC_nphe_txy, H_muontrig_vxy, H_muontrig_vz_phi, H_muontrig_vz_p, H_muontrig_vz_theta, H_muontrig_TOF_xy, H_muontrig_TOF_t_path;
    public H1F H_muontrig_vz, H_muontrig_LTCC_nphe, H_muontrig_HTCC_nphe;
	public H1F[] H_muontrig_ecal_en_neg_S, H_muontrig_ecal_en_pos_S, H_muontrig_pcal_en_neg_S, H_muontrig_pcal_en_pos_S;
	public H2F[] H_muontrig_ECECOUT_en_S;

	public H2F H_positive_theta_mom, H_negative_theta_mom, H_electron_theta_mom;

	public H1F H_e_vz_S1, H_e_vz_S2, H_e_vz_S3, H_e_vz_S4, H_e_vz_S5, H_e_vz_S6;
	public H1F H_e_FMMvz_S1, H_e_FMMvz_S2, H_e_FMMvz_S3, H_e_FMMvz_S4, H_e_FMMvz_S5, H_e_FMMvz_S6;
	public H2F[][] H_e_FMMmom_mom, H_e_FMMtheta_theta, H_e_FMMphi_phi, H_e_FMMvz_vz;
	public H2F H_o_TOF;
	public H1F H_o_vt;

	public H2F H_dcm_theta_phi, H_dcm_theta_mom, H_dcm_phi_mom, H_dcm_vz_phi, H_dcm_vz_p, H_dcm_vz_theta, H_dcm_phiK_mom;
	public H2F H_dcm_R1th_R1ph, H_dcm_R1the_mom, H_dcm_R1ph_mom, H_dcm_pvz_phi, H_dcm_pvz_p, H_dcm_pvz_theta, H_dcm_pvt_pvz;
	public H2F H_dcp_theta_phi, H_dcp_theta_mom, H_dcp_phi_mom, H_dcp_vz_phi, H_dcp_vz_p, H_dcp_vz_theta, H_dcp_phiK_mom;
	public H2F H_dcp_R1th_R1ph, H_dcp_R1the_mom, H_dcp_R1ph_mom, H_dcp_pvz_phi, H_dcp_pvz_p, H_dcp_pvz_theta, H_dcp_pvt_pvz;
	public H2F H2_dcm_vz_phi, H2_dcp_vz_phi;
	public H1F H_dcm_W, H_dcm_W_zoom;

	public H1F H_negHBTrk_sect, H_posHBTrk_sect, H_negRECHB_sect, H_posRECHB_sect;
	public H1F H_negTBTrk_sect, H_posTBTrk_sect, H_negREC_sect, H_posREC_sect;

	public H1F[] H_dcm_vz, H_dcm_chi2;
	public H2F[] H_R1_dcm_XY, H_R2_dcm_XY, H_R3_dcm_XY, H_R1_dcm_uXY, H_R2_dcm_uXY, H_R3_dcm_uXY;
	public H2F[] H_R1phiDm_mom;

	public H1F[] H_dcp_vz, H_dcp_chi2;
	public H2F[] H_R1_dcp_XY, H_R2_dcp_XY, H_R3_dcp_XY, H_R1_dcp_uXY, H_R2_dcp_uXY, H_R3_dcp_uXY;
	public H2F[] H_R1phiDp_mom;

	public H1F[] H_dce_chi2;
	public GraphErrors g_m_ESampl_ECal, g_s_ESampl_ECal;

	public H2F H_gg_open_a, H_g1_tf, H_g2_tf, H_g1_te, H_g2_te;
	public H1F H_gg_m;

	public H2F H_CVT_ft, H_CVT_pt, H_CVT_pf, H_CVT_zf, H_CVT_zp, H_CVT_zt;
	public H1F H_CVT_p, H_CVT_t, H_CVT_f, H_CVT_z, H_CVT_chi2, H_CVT_ndf, H_CVT_pathlength;
	public H1F H_CVT_z_pos, H_CVT_z_neg, H_CVT_d0_pos, H_CVT_d0_neg, H_CVT_absd0_pos, H_CVT_absd0_neg, H_CVT_chi2_pos, H_CVT_chi2_neg;
	public H1F H_CVT_d0, H_CVT_charge;
	public H2F H_CVT_vz_mom, H_CVT_vz_phi, H_CVT_vz_theta, H_CVT_vx_vy, H_CVT_vx_vz, H_CVT_vz_vy;
	public H1F H_CVT_mom, H_CVT_theta, H_CVT_phi, H_CVT_vz, H_CVT_vx, H_CVT_vy;
	public H1F H_CD_vx, H_CD_vy, H_CD_vz;
	public H2F H_CD_vz_mom, H_CD_vz_phi, H_CD_vz_theta, H_CD_vx_vy, H_CD_vx_vz, H_CD_vz_vy; 

	public H1F[] H_MM_epip_Spip, H_MM_epip_Se;
	public H1F H_MM_epip, H_MM_epip_zoom, H_pip_vtd, H_pip_vz_ve_diff, H_pip_Dphi;
	public H1F H_pim_vtd;
	public H2F H_pip_theta_phi, H_pip_theta_mom, H_pip_phi_mom, H_pip_vz_phi, H_pip_vz_theta, H_pip_vz_mom, H_pip_e_vt, H_pip_vz_ve;
	public H2F H_pip_vz_ve_diff_mom, H_pip_vz_ve_diff_theta, H_pip_vz_ve_diff_phi, H_pip_vz_ve_diff_Dphi;
	public H2F H_MM_epip_phi, H_pip_beta_p, H_pip_beta2_p, H_pip_vtd_mom, H_pip_vtd_theta, H_pip_vtd_phi;
	public H2F H_epip_e_theta_phi, H_epip_e_theta_mom, H_epip_e_phi_mom, H_epip_xB_Q2, H_epip_e_W_Q2, H_epip_e_t_phi;

	public H1F H_rho_IM, H_rho_MM;
	public H2F H_rho_Q2_xB, H_rho_Q2_W;
	public H2F H_rho_prot, H_rho_pip_beta, H_rho_pim_beta;

	public H1F H_trig_sector_count, H_trig_sector_elec, H_trig_sector_elec_rat, H_rand_trig_sector_count;
    public H1F H_muon_trig_sector_count, H_trig_sector_muon, H_trig_sector_muon_rat, H_trig_sector_muontrack, H_trig_sector_muontrack_rat;
	public H1F H_trig_sector_prot, H_trig_sector_piplus, H_trig_sector_piminus, H_trig_sector_kplus, H_trig_sector_kminus, H_trig_sector_photon, H_trig_sector_neutron, H_trig_sector_deut;
	public H1F H_trig_sector_prot_rat, H_trig_sector_piplus_rat, H_trig_sector_piminus_rat, H_trig_sector_kplus_rat, H_trig_sector_kminus_rat, H_trig_sector_photon_rat, H_trig_sector_neutron_rat, H_trig_sector_deut_rat;
	public H1F H_trig_sector_positive_rat, H_trig_sector_negative_rat, H_trig_sector_neutral_rat;
	public H1F H_Nclust_ev, H_clust1_E, H_clust2_E;
	public H1F H_trig_S1_ETOT_E, H_trig_S1_ECAL_E, H_trig_S1_PCAL_E, H_trig_S1_HTCC_n, H_trig_S1_HTCC_N, H_trig_S1_HTCC_N_track;
	public H1F H_trig_S2_ETOT_E, H_trig_S2_ECAL_E, H_trig_S2_PCAL_E, H_trig_S2_HTCC_n, H_trig_S2_HTCC_N, H_trig_S2_HTCC_N_track;
	public H1F H_trig_S3_ETOT_E, H_trig_S3_ECAL_E, H_trig_S3_PCAL_E, H_trig_S3_HTCC_n, H_trig_S3_HTCC_N, H_trig_S3_HTCC_N_track;
	public H1F H_trig_S4_ETOT_E, H_trig_S4_ECAL_E, H_trig_S4_PCAL_E, H_trig_S4_HTCC_n, H_trig_S4_HTCC_N, H_trig_S4_HTCC_N_track;
	public H1F H_trig_S5_ETOT_E, H_trig_S5_ECAL_E, H_trig_S5_PCAL_E, H_trig_S5_HTCC_n, H_trig_S5_HTCC_N, H_trig_S5_HTCC_N_track;
	public H1F H_trig_S6_ETOT_E, H_trig_S6_ECAL_E, H_trig_S6_PCAL_E, H_trig_S6_HTCC_n, H_trig_S6_HTCC_N, H_trig_S6_HTCC_N_track;
	public H2F H_trig_S1_PCAL_XY, H_trig_S1_HTCC_XY;
	public H2F H_trig_S2_PCAL_XY, H_trig_S2_HTCC_XY;
	public H2F H_trig_S3_PCAL_XY, H_trig_S3_HTCC_XY;
	public H2F H_trig_S4_PCAL_XY, H_trig_S4_HTCC_XY;
	public H2F H_trig_S5_PCAL_XY, H_trig_S5_HTCC_XY;
	public H2F H_trig_S6_PCAL_XY, H_trig_S6_HTCC_XY;
	public H2F missTrig_S1_ft, missTrig_S1_mt, missTrig_S1_mf;
	public H2F missTrig_S2_ft, missTrig_S2_mt, missTrig_S2_mf;
	public H2F missTrig_S3_ft, missTrig_S3_mt, missTrig_S3_mf;
	public H2F missTrig_S4_ft, missTrig_S4_mt, missTrig_S4_mf;
	public H2F missTrig_S5_ft, missTrig_S5_mt, missTrig_S5_mf;
	public H2F missTrig_S6_ft, missTrig_S6_mt, missTrig_S6_mf;
	public H1F PCAL_Thresh_S1, PCAL_Thresh_S2, PCAL_Thresh_S3, PCAL_Thresh_S4, PCAL_Thresh_S5, PCAL_Thresh_S6;
	public H2F ETOT_Sampl_S1, ETOT_Sampl_S2, ETOT_Sampl_S3, ETOT_Sampl_S4, ETOT_Sampl_S5, ETOT_Sampl_S6;

	public H1F H_TOF_vt_S1m, H_TOF_vt_S2m, H_TOF_vt_S3m, H_TOF_vt_S4m, H_TOF_vt_S5m, H_TOF_vt_S6m;
	public H2F H_TOF_vt_mom_S1m, H_TOF_vt_mom_S2m, H_TOF_vt_mom_S3m, H_TOF_vt_mom_S4m, H_TOF_vt_mom_S5m, H_TOF_vt_mom_S6m;
	public H1F H_TOF_vt_S1p, H_TOF_vt_S2p, H_TOF_vt_S3p, H_TOF_vt_S4p, H_TOF_vt_S5p, H_TOF_vt_S6p;
	public H2F H_TOF_vt_mom_S1p, H_TOF_vt_mom_S2p, H_TOF_vt_mom_S3p, H_TOF_vt_mom_S4p, H_TOF_vt_mom_S5p, H_TOF_vt_mom_S6p;

	public H2F H_CVT_e_corr_vz, H_CVT_e_corr_phi, H_CVT_corr_e_theta;
	public H2F H_elast_e_p_th, H_elast_W_sect, H_CVT_corr_e_mom;
	public H1F H_CVT_e_vz_diff, H_CVT_e_phi_diff, H_elast_W;

	public H1F[] H_e_RFtime1_FD_S , H_pip_RFtime1_FD_S, H_pim_RFtime1_FD_S, H_p_RFtime1_FD_S;
	public H1F H_pip_RFtime1_CD, H_pim_RFtime1_CD, H_p_RFtime1_CD;
	public H1F  H_RFtimediff, H_RFtimediff_corrected;

	public H1F hbstOccupancy,hbmtOccupancy,htrks,hpostrks,hnegtrks,hndf,hchi2norm,hp,hpt,hpathlen,hbstOnTrkLayers,hbmtOnTrkLayers;
	public H1F hpostrks_rat, hnegtrks_rat;
	public H1F H_trig_central_prot_rat, H_trig_central_deut_rat, H_trig_central_piplus_rat,H_trig_central_piminus_rat,H_trig_central_kplus_rat,H_trig_central_kminus_rat;

	public IndexedTable InverseTranslationTable;
    public IndexedTable calibrationTranslationTable;
    public IndexedTable rfTable, rfTableOffset;
    public ConstantsManager ccdb;

	public monitor2p2GeV(int reqrunNum, String reqOutputDir, float reqEB, boolean reqTimeBased) {
        
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
		float rfPeriod = 4.008f;
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

		//Initialiing Two-sector trigger histograms
		H_muontrig_ecal_en_neg_S = new H1F[6];
		H_muontrig_ecal_en_pos_S = new H1F[6];
        H_muontrig_pcal_en_neg_S = new H1F[6];
		H_muontrig_pcal_en_pos_S = new H1F[6];
		H_muontrig_ECECOUT_en_S = new H2F[6];
        for (int i = 0; i < 6; i++) {
            H_muontrig_ecal_en_neg_S[i] = new H1F(String.format("H_muontrig_ECAL_Energy_NegS%d", i + 1), String.format("H_muontrig_ECAL_Energy_NegS%d", i + 1), 1000, -0.5, 200.5);
            H_muontrig_ecal_en_neg_S[i].setTitle(String.format("Two-Sector Trig ECAL_En Neg, S%d", i + 1));
            H_muontrig_ecal_en_neg_S[i].setTitleX("E_ecal (MeV)");
            H_muontrig_ecal_en_pos_S[i] = new H1F(String.format("H_muontrig_ECAL_Energy_PosS%d", i + 1), String.format("H_muontrig_ECAL_Energy_Pos_S%d", i + 1), 1000, -0.5, 200.5);
            H_muontrig_ecal_en_pos_S[i].setTitle(String.format("Two-Sector Trig ECAL_En Pos, S%d", i + 1));
            H_muontrig_ecal_en_pos_S[i].setTitleX("E_ecal (MeV)");
            H_muontrig_pcal_en_neg_S[i] = new H1F(String.format("H_muontrig_PCAL_Energy_NegS%d", i + 1), String.format("H_muontrig_PCAL_Energy_NegS%d", i + 1), 1000, -0.5, 100.5);
            H_muontrig_pcal_en_neg_S[i].setTitle(String.format("Two-Sector Trig PCAL_En Neg, S%d", i + 1));
            H_muontrig_pcal_en_neg_S[i].setTitleX("E_pcal (MeV)");
            H_muontrig_pcal_en_pos_S[i] = new H1F(String.format("H_muontrig_PCAL_Energy_PosS%d", i + 1), String.format("H_muontrig_PCAL_Energy_PosS%d", i + 1), 1000, -0.5, 100.5);
            H_muontrig_pcal_en_pos_S[i].setTitle(String.format("Two-Sector Trig PCAL_En Pos, S%d", i + 1));
            H_muontrig_pcal_en_pos_S[i].setTitleX("E_pcal (MeV)");
            H_muontrig_ECECOUT_en_S[i] = new H2F("H_muontrig_EC-ECOUT_en_S", "H_muontrig_EC-ECOUT_en_S", 200, 0, 400, 200, 0, 200);
            H_muontrig_ECECOUT_en_S[i].setTitle("ECout vs EC");
            H_muontrig_ECECOUT_en_S[i].setTitleX("EC_energy (MeV)");
            H_muontrig_ECECOUT_en_S[i].setTitleY("ECout_energy (MeV)");
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

		H_TOF_vt_S1m = new H1F("H_TOF_vt_S1n","H_TOF_vt_S1n",100,tofvt1,tofvt2);
		H_TOF_vt_S1m.setTitle("S1 neg TOF vert t");
		H_TOF_vt_S1m.setTitleX("vert t (ns)");
		H_TOF_vt_S2m = new H1F("H_TOF_vt_S2n","H_TOF_vt_S2n",100,tofvt1,tofvt2);
		H_TOF_vt_S2m.setTitle("S2 neg TOF vert t");
		H_TOF_vt_S2m.setTitleX("vert t (ns)");
		H_TOF_vt_S3m = new H1F("H_TOF_vt_S3n","H_TOF_vt_S3n",100,tofvt1,tofvt2);
		H_TOF_vt_S3m.setTitle("S3 neg TOF vert t");
		H_TOF_vt_S3m.setTitleX("vert t (ns)");
		H_TOF_vt_S4m = new H1F("H_TOF_vt_S4n","H_TOF_vt_S4n",100,tofvt1,tofvt2);
		H_TOF_vt_S4m.setTitle("S4 neg TOF vert t");
		H_TOF_vt_S4m.setTitleX("vert t (ns)");
		H_TOF_vt_S5m = new H1F("H_TOF_vt_S5n","H_TOF_vt_S5n",100,tofvt1,tofvt2);
		H_TOF_vt_S5m.setTitle("S5 neg TOF vert t");
		H_TOF_vt_S5m.setTitleX("vert t (ns)");
		H_TOF_vt_S6m = new H1F("H_TOF_vt_S6n","H_TOF_vt_S6n",100,tofvt1,tofvt2);
		H_TOF_vt_S6m.setTitle("S6 neg TOF vert t");
		H_TOF_vt_S6m.setTitleX("vert t (ns)");
		H_TOF_vt_mom_S1m = new H2F("H_TOF_vt_mom_S1n","H_TOF_vt_mom_S1n",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S1m.setTitle("S1 neg TOF vert t vs mom");
		H_TOF_vt_mom_S1m.setTitleX("p (GeV)");
		H_TOF_vt_mom_S1m.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S2m = new H2F("H_TOF_vt_mom_S2n","H_TOF_vt_mom_S2n",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S2m.setTitle("S2 neg TOF vert t vs mom");
		H_TOF_vt_mom_S2m.setTitleX("p (GeV)");
		H_TOF_vt_mom_S2m.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S3m = new H2F("H_TOF_vt_mom_S3n","H_TOF_vt_mom_S3n",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S3m.setTitle("S3 neg TOF vert t vs mom");
		H_TOF_vt_mom_S3m.setTitleX("p (GeV)");
		H_TOF_vt_mom_S3m.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S4m = new H2F("H_TOF_vt_mom_S4n","H_TOF_vt_mom_S4n",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S4m.setTitle("S4 neg TOF vert t vs mom");
		H_TOF_vt_mom_S4m.setTitleX("p (GeV)");
		H_TOF_vt_mom_S4m.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S5m = new H2F("H_TOF_vt_mom_S5n","H_TOF_vt_mom_S5n",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S5m.setTitle("S5 neg TOF vert t vs mom");
		H_TOF_vt_mom_S5m.setTitleX("p (GeV)");
		H_TOF_vt_mom_S5m.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S6m = new H2F("H_TOF_vt_mom_S6n","H_TOF_vt_mom_S6n",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S6m.setTitle("S6 neg TOF vert t vs mom");
		H_TOF_vt_mom_S6m.setTitleX("p (GeV)");
		H_TOF_vt_mom_S6m.setTitleY("vert t (ns)");

		H_TOF_vt_S1p = new H1F("H_TOF_vt_S1p","H_TOF_vt_S1p",100,tofvt1,tofvt2);
		H_TOF_vt_S1p.setTitle("S1 pos TOF vert t");
		H_TOF_vt_S1p.setTitleX("vert t (ns)");
		H_TOF_vt_S2p = new H1F("H_TOF_vt_S2p","H_TOF_vt_S2p",100,tofvt1,tofvt2);
		H_TOF_vt_S2p.setTitle("S2 pos TOF vert t");
		H_TOF_vt_S2p.setTitleX("vert t (ns)");
		H_TOF_vt_S3p = new H1F("H_TOF_vt_S3p","H_TOF_vt_S3p",100,tofvt1,tofvt2);
		H_TOF_vt_S3p.setTitle("S3 pos TOF vert t");
		H_TOF_vt_S3p.setTitleX("vert t (ns)");
		H_TOF_vt_S4p = new H1F("H_TOF_vt_S4p","H_TOF_vt_S4p",100,tofvt1,tofvt2);
		H_TOF_vt_S4p.setTitle("S4 pos TOF vert t");
		H_TOF_vt_S4p.setTitleX("vert t (ns)");
		H_TOF_vt_S5p = new H1F("H_TOF_vt_S5p","H_TOF_vt_S5p",100,tofvt1,tofvt2);
		H_TOF_vt_S5p.setTitle("S5 pos TOF vert t");
		H_TOF_vt_S5p.setTitleX("vert t (ns)");
		H_TOF_vt_S6p = new H1F("H_TOF_vt_S6p","H_TOF_vt_S6p",100,tofvt1,tofvt2);
		H_TOF_vt_S6p.setTitle("S6 pos TOF vert t");
		H_TOF_vt_S6p.setTitleX("vert t (ns)");
		H_TOF_vt_mom_S1p = new H2F("H_TOF_vt_mom_S1p","H_TOF_vt_mom_S1p",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S1p.setTitle("S1 pos TOF vert t vs mom");
		H_TOF_vt_mom_S1p.setTitleX("p (GeV)");
		H_TOF_vt_mom_S1p.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S2p = new H2F("H_TOF_vt_mom_S2p","H_TOF_vt_mom_S2p",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S2p.setTitle("S2 pos TOF vert t vs mom");
		H_TOF_vt_mom_S2p.setTitleX("p (GeV)");
		H_TOF_vt_mom_S2p.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S3p = new H2F("H_TOF_vt_mom_S3p","H_TOF_vt_mom_S3p",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S3p.setTitle("S3 pos TOF vert t vs mom");
		H_TOF_vt_mom_S3p.setTitleX("p (GeV)");
		H_TOF_vt_mom_S3p.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S4p = new H2F("H_TOF_vt_mom_S4p","H_TOF_vt_mom_S4p",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S4p.setTitle("S4 pos TOF vert t vs mom");
		H_TOF_vt_mom_S4p.setTitleX("p (GeV)");
		H_TOF_vt_mom_S4p.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S5p = new H2F("H_TOF_vt_mom_S5p","H_TOF_vt_mom_S5p",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S5p.setTitle("S5 pos TOF vert t vs mom");
		H_TOF_vt_mom_S5p.setTitleX("p (GeV)");
		H_TOF_vt_mom_S5p.setTitleY("vert t (ns)");
		H_TOF_vt_mom_S6p = new H2F("H_TOF_vt_mom_S6p","H_TOF_vt_mom_S6p",100,0,EB,100,tofvt1,tofvt2);
		H_TOF_vt_mom_S6p.setTitle("S6 pos TOF vert t vs mom");
		H_TOF_vt_mom_S6p.setTitleX("p (GeV)");
		H_TOF_vt_mom_S6p.setTitleY("vert t (ns)");

		H_Nclust_ev = new H1F("H_Nclust_ev","H_Nclust_ev",11,-0.5,10.5);
		H_Nclust_ev.setTitle("N clust events");
		H_Nclust_ev.setTitleX("N clust");
		H_clust1_E = new H1F("H_clust1_E","H_clust1_E",100,0,1.5);
		H_clust1_E.setTitle("1st cluster energy");
		H_clust1_E.setTitleX("E (GeV)");
		H_clust2_E = new H1F("H_clust2_E","H_clust2_E",100,0,1.5);
		H_clust2_E.setTitle("2nd cluster energy");
		H_clust2_E.setTitleX("E (GeV)");
		H_trig_sector_count = new H1F("H_trig_sector_count","H_trig_sector_count",6,0.5,6.5);
		H_trig_sector_count.setTitle("N trigs per sect");
		H_trig_sector_count.setTitleX("Sector number");
        H_muon_trig_sector_count = new H1F("H_muon_trig_sector_count","H_muon_trig_sector_count",3,0.5,3.5);
        H_muon_trig_sector_count.setTitle("N muon trigs per sect-pair");
        H_muon_trig_sector_count.setTitleX("Sector-pair number");
		H_rand_trig_sector_count = new H1F("H_rand_trig_sector_count","H_rand_trig_sector_count",7,0.5,7.5);
		H_rand_trig_sector_count.setTitle("N rand trigs per sect");
		H_rand_trig_sector_count.setTitleX("Sector number");
		H_trig_sector_elec = new H1F("H_trig_sector_elec","H_trig_sector_elec",6,0.5,6.5);
		H_trig_sector_elec.setTitle("N elec per sect");
		H_trig_sector_elec.setTitleX("Sector number");
        H_trig_sector_muon = new H1F("H_trig_sector_muon","H_trig_sector_muon",3,0.5,3.5);
        H_trig_sector_muon.setTitle("N muon per sect");
        H_trig_sector_muon.setTitleX("Sector number");
        H_trig_sector_muontrack = new H1F("H_trig_sector_muontrack","H_trig_sector_muontrack",3,0.5,3.5);
        H_trig_sector_muontrack.setTitle("N muonpairs trigger per sect");
        H_trig_sector_muontrack.setTitleX("Sector number");
		H_trig_sector_elec_rat = new H1F("H_trig_sector_elec_rat","H_trig_sector_elec_rat",6,0.5,6.5);
		H_trig_sector_elec_rat.setTitle("N elec / trig vs sector");
		H_trig_sector_elec_rat.setTitleX("Sector number");
        H_trig_sector_muon_rat = new H1F("H_trig_sector_muon_rat","H_trig_sector_muon_rat",3,0.5,3.5);
        H_trig_sector_muon_rat.setTitle("N muon / trig vs sector");
        H_trig_sector_muon_rat.setTitleX("Sector-pair number");
        H_trig_sector_muontrack_rat = new H1F("H_trig_sector_muontrack_rat","H_trig_sector_muontrack_rat",3,0.5,3.5);
        H_trig_sector_muontrack_rat.setTitle("N muon trig / N muon vs sector");
        H_trig_sector_muontrack_rat.setTitleX("Sector-pair number");

		H_trig_sector_prot = new H1F("H_trig_sector_prot","H_trig_sector_prot",6,0.5,6.5);
		H_trig_sector_piplus = new H1F("H_trig_sector_piplus","H_trig_sector_piplus",6,0.5,6.5);
		H_trig_sector_piminus = new H1F("H_trig_sector_piminus","H_trig_sector_piminus",6,0.5,6.5);
		H_trig_sector_kplus = new H1F("H_trig_sector_kplus","H_trig_sector_kplus",6,0.5,6.5);
		H_trig_sector_kminus = new H1F("H_trig_sector_kminus","H_trig_sector_kminus",6,0.5,6.5);
		H_trig_sector_photon = new H1F("H_trig_sector_photon","H_trig_sector_photon",6,0.5,6.5);
		H_trig_sector_neutron = new H1F("H_trig_sector_neutron","H_trig_sector_neutron",6,0.5,6.5);
        H_trig_sector_deut = new H1F("H_trig_sector_deuteron","H_trig_sector_deuteron",6,0.5,6.5);
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
		H_trig_sector_photon_rat = new H1F("H_trig_sector_photon_rat","H_trig_sector_photon_rat",6,0.5,6.5);
		H_trig_sector_photon_rat.setTitle("FD #gamma / trig per sect");
		H_trig_sector_photon_rat.setTitleX("Sector number");
		H_trig_sector_neutron_rat = new H1F("H_trig_sector_neutron_rat","H_trig_sector_neutron_rat",6,0.5,6.5);
        H_trig_sector_deut_rat = new H1F("H_trig_sector_deut_rat","H_trig_sector_deut_rat",6,0.5,6.5);
        H_trig_sector_deut_rat.setTitle("FD deut / trig per sect");
        H_trig_sector_deut_rat.setTitleX("Sector number");
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
		H_trig_central_deut_rat = new H1F("H_trig_central_deut_rat","H_trig_central_deut_rat",1,0.5,1.5);
        H_trig_central_deut_rat.setTitle("CD deut / trig");
        H_trig_central_deut_rat.setTitleX("All sectors");

		H_CD_vz_mom = new H2F("H_CD_vz_mom","H_CD_vz_mom",100,0,3.5,100,-25.,25.);
		H_CD_vz_mom.setTitle("CD z vertex vs mom");
		H_CD_vz_mom.setTitleX("p (GeV/c");
		H_CD_vz_mom.setTitleY("z (cm)");
		H_CD_vz_theta = new H2F("H_CD_vz_theta","H_CD_vz_theta",100,0,180.,100, -25., 25.);
        H_CD_vz_theta.setTitle("CD z vertex vs theta");
        H_CD_vz_theta.setTitleX("#theta (^o)");
        H_CD_vz_theta.setTitleY("z (cm)");
        H_CD_vz_phi = new H2F("H_CD_vz_phi", "H_CD_vz_phi", 200, -180., 180., 100, -25., 25.);
        H_CD_vz_phi.setTitle("CD z vertex vs phi");
        H_CD_vz_phi.setTitleX("#phi (^o)");
        H_CD_vz_phi.setTitleY("z (cm)");
        H_CD_vx = new H1F("H_CD_vx", 200, -10., 10.);
        H_CD_vx.setTitle("CD particle x vertex");
        H_CD_vx.setTitleX("x (cm)");
        H_CD_vz = new H1F("H_CD_vz", 200, -25., 25.);
        H_CD_vz.setTitle("CD particle z vertex");
        H_CD_vz.setTitleX("z (cm)");
        H_CD_vy = new H1F("H_CD_vy", 200, -10., 10.);
        H_CD_vy.setTitle("CD particle y vertex");
        H_CD_vy.setTitleX("y (cm)");
        H_CD_vx_vy = new H2F("H_CD_vx_vy", "H_CD_vx_vy", 100, -25., 25, 100, -25., 25.);
        H_CD_vx_vy.setTitle("CD particle vertex vy vs vx");
        H_CD_vx_vy.setTitleX("x (cm)");
        H_CD_vx_vy.setTitleY("y (cm)");
        H_CD_vx_vz = new H2F("H_CD_vx_vz", "H_CD_vx_vz", 100, -25., 25, 100, -25., 25.);
        H_CD_vx_vz.setTitle("CD particle vertex vz vs vx");
        H_CD_vx_vz.setTitleX("x (cm)");
        H_CD_vx_vz.setTitleY("z (cm)");
        H_CD_vz_vy = new H2F("H_CD_vz_vy", "H_CD_vz_vy", 100, -25., 25, 100, -25., 25.);
        H_CD_vz_vy.setTitle("CD particle vertex vz vs vy");
        H_CD_vz_vy.setTitleX("y (cm)");
        H_CD_vz_vy.setTitleY("z (cm)");

        H_CVT_vz_mom = new H2F("H_CVT_vz_mom", "H_CVT_vz_mom", 100, 0, 3.5, 100, -25., 25.);
        H_CVT_vz_mom.setTitle("CVT z vertex vs mom");
        H_CVT_vz_mom.setTitleX("p (GeV/c");
        H_CVT_vz_mom.setTitleY("z (cm)");
        H_CVT_vz_theta = new H2F("H_CVT_vz_theta", "H_CVT_vz_theta", 100, 0, 180., 100, -25., 25.);
        H_CVT_vz_theta.setTitle("CVT z vertex vs theta");
        H_CVT_vz_theta.setTitleX("#theta (^o)");
        H_CVT_vz_theta.setTitleY("z (cm)");
        H_CVT_vz_phi = new H2F("H_CVT_vz_phi", "H_CVT_vz_phi", 200, -180., 180., 100, -25., 25.);
        H_CVT_vz_phi.setTitle("CVT z vertex vs phi");
        H_CVT_vz_phi.setTitleX("#phi (^o)");
        H_CVT_vz_phi.setTitleY("z (cm)");
        H_CVT_vx = new H1F("H_CVT_vx", 200, -5., 5.);
        H_CVT_vx.setTitle("CVT x vertex");
        H_CVT_vx.setTitleX("x (cm)");
        H_CVT_vz = new H1F("H_CVT_vz", 200, -25., 25.);
        H_CVT_vz.setTitle("CVT z vertex");
        H_CVT_vz.setTitleX("z (cm)");
        H_CVT_vy = new H1F("H_CVT_vy", 200, -5., 5.);
        H_CVT_vy.setTitle("CVT y vertex");
        H_CVT_vy.setTitleX("y (cm)");
        H_CVT_vx_vy = new H2F("H_CVT_vx_vy", "H_CVT_vx_vy", 100, -25., 25, 100, -25., 25.);
        H_CVT_vx_vy.setTitle("CVT vertex  y vs x");
        H_CVT_vx_vy.setTitleX("x (cm)");
        H_CVT_vx_vy.setTitleY("y (cm)");
        H_CVT_vx_vz = new H2F("H_CVT_vx_vz", "H_CVT_vx_vz", 100, -25., 25, 100, -25., 25.);
        H_CVT_vx_vz.setTitle("CVT vertex z vs x");
        H_CVT_vx_vz.setTitleX("x (cm)");
        H_CVT_vx_vz.setTitleY("z (cm)");
        H_CVT_vz_vy = new H2F("H_CVT_vz_vy", "H_CVT_vz_vy", 100, -25., 25, 100, -25., 25.);
        H_CVT_vz_vy.setTitle("CVT vertex z vs y");
        H_CVT_vz_vy.setTitleX("y (cm)");
        H_CVT_vz_vy.setTitleY("z (cm)");

		PCAL_Thresh_S1 = new H1F("PCAL_Thresh_S1","PCAL_Thresh_S1",100,0,0.5);
		PCAL_Thresh_S1.setTitle("PCAL E S1");
		PCAL_Thresh_S1.setTitleX("E (GeV)");
		PCAL_Thresh_S2 = new H1F("PCAL_Thresh_S2","PCAL_Thresh_S2",100,0,0.5);
		PCAL_Thresh_S2.setTitle("PCAL E S2");
		PCAL_Thresh_S2.setTitleX("E (GeV)");
		PCAL_Thresh_S3 = new H1F("PCAL_Thresh_S3","PCAL_Thresh_S3",100,0,0.5);
		PCAL_Thresh_S3.setTitle("PCAL E S3");
		PCAL_Thresh_S3.setTitleX("E (GeV)");
		PCAL_Thresh_S4 = new H1F("PCAL_Thresh_S4","PCAL_Thresh_S4",100,0,0.5);
		PCAL_Thresh_S4.setTitle("PCAL E S4");
		PCAL_Thresh_S4.setTitleX("E (GeV)");
		PCAL_Thresh_S5 = new H1F("PCAL_Thresh_S5","PCAL_Thresh_S5",100,0,0.5);
		PCAL_Thresh_S5.setTitle("PCAL E S5");
		PCAL_Thresh_S5.setTitleX("E (GeV)");
		PCAL_Thresh_S6 = new H1F("PCAL_Thresh_S6","PCAL_Thresh_S6",100,0,0.5);
		PCAL_Thresh_S6.setTitle("PCAL E S6");
		PCAL_Thresh_S6.setTitleX("E (GeV)");
		ETOT_Sampl_S1 = new H2F("ETOT_Sampl_S1","ETOT_Sampl_S1",100,0,EB,100,0,0.5);
		ETOT_Sampl_S1.setTitle("ETOT sampling S1");
		ETOT_Sampl_S1.setTitleX("p (GeV)");
		ETOT_Sampl_S1.setTitleY("ETOT/p");
		ETOT_Sampl_S2 = new H2F("ETOT_Sampl_S2","ETOT_Sampl_S2",100,0,EB,100,0,0.5);
		ETOT_Sampl_S2.setTitle("ETOT sampling S2");
		ETOT_Sampl_S2.setTitleX("p (GeV)");
		ETOT_Sampl_S2.setTitleY("ETOT/p");
		ETOT_Sampl_S3 = new H2F("ETOT_Sampl_S3","ETOT_Sampl_S3",100,0,EB,100,0,0.5);
		ETOT_Sampl_S3.setTitle("ETOT sampling S3");
		ETOT_Sampl_S3.setTitleX("p (GeV)");
		ETOT_Sampl_S3.setTitleY("ETOT/p");
		ETOT_Sampl_S4 = new H2F("ETOT_Sampl_S4","ETOT_Sampl_S4",100,0,EB,100,0,0.5);
		ETOT_Sampl_S4.setTitle("ETOT sampling S4");
		ETOT_Sampl_S4.setTitleX("p (GeV)");
		ETOT_Sampl_S4.setTitleY("ETOT/p");
		ETOT_Sampl_S5 = new H2F("ETOT_Sampl_S5","ETOT_Sampl_S5",100,0,EB,100,0,0.5);
		ETOT_Sampl_S5.setTitle("ETOT sampling S5");
		ETOT_Sampl_S5.setTitleX("p (GeV)");
		ETOT_Sampl_S5.setTitleY("ETOT/p");
		ETOT_Sampl_S6 = new H2F("ETOT_Sampl_S6","ETOT_Sampl_S6",100,0,EB,100,0,0.5);
		ETOT_Sampl_S6.setTitle("ETOT sampling S6");
		ETOT_Sampl_S6.setTitleX("p (GeV)");
		ETOT_Sampl_S6.setTitleY("ETOT/p");

		missTrig_S1_ft = new H2F("missTrig_S1_ft","missTrig_S1_ft",100,-180,180,100,0,40);
		missTrig_S1_ft.setTitle("Miss trig S1");
		missTrig_S1_ft.setTitleX("#phi");
		missTrig_S1_ft.setTitleY("#theta");
		missTrig_S2_ft = new H2F("missTrig_S2_ft","missTrig_S2_ft",100,-180,180,100,0,40);
		missTrig_S2_ft.setTitle("Miss trig S2");
		missTrig_S2_ft.setTitleX("#phi");
		missTrig_S2_ft.setTitleY("#theta");
		missTrig_S3_ft = new H2F("missTrig_S3_ft","missTrig_S3_ft",100,-180,180,100,0,40);
		missTrig_S3_ft.setTitle("Miss trig S3");
		missTrig_S3_ft.setTitleX("#phi");
		missTrig_S3_ft.setTitleY("#theta");
		missTrig_S4_ft = new H2F("missTrig_S4_ft","missTrig_S4_ft",100,-180,180,100,0,40);
		missTrig_S4_ft.setTitle("Miss trig S4");
		missTrig_S4_ft.setTitleX("#phi");
		missTrig_S4_ft.setTitleY("#theta");
		missTrig_S5_ft = new H2F("missTrig_S5_ft","missTrig_S5_ft",100,-180,180,100,0,40);
		missTrig_S5_ft.setTitle("Miss trig S5");
		missTrig_S5_ft.setTitleX("#phi");
		missTrig_S5_ft.setTitleY("#theta");
		missTrig_S6_ft = new H2F("missTrig_S6_ft","missTrig_S6_ft",100,-180,180,100,0,40);
		missTrig_S6_ft.setTitle("Miss trig S6");
		missTrig_S6_ft.setTitleX("#phi");
		missTrig_S6_ft.setTitleY("#theta");
		missTrig_S1_mt = new H2F("missTrig_S1_mt","missTrig_S1_mt",100,0,EB,100,0,40);
		missTrig_S1_mt.setTitle("Miss trig S1");
		missTrig_S1_mt.setTitleX("p (GeV)");
		missTrig_S1_mt.setTitleY("#theta");
		missTrig_S2_mt = new H2F("missTrig_S2_mt","missTrig_S2_mt",100,0,EB,100,0,40);
		missTrig_S2_mt.setTitle("Miss trig S2");
		missTrig_S2_mt.setTitleX("p (GeV)");
		missTrig_S2_mt.setTitleY("#theta");
		missTrig_S3_mt = new H2F("missTrig_S3_mt","missTrig_S3_mt",100,0,EB,100,0,40);
		missTrig_S3_mt.setTitle("Miss trig S3");
		missTrig_S3_mt.setTitleX("p (GeV)");
		missTrig_S3_mt.setTitleY("#theta");
		missTrig_S4_mt = new H2F("missTrig_S4_mt","missTrig_S4_mt",100,0,EB,100,0,40);
		missTrig_S4_mt.setTitle("Miss trig S4");
		missTrig_S4_mt.setTitleX("p (GeV)");
		missTrig_S4_mt.setTitleY("#theta");
		missTrig_S5_mt = new H2F("missTrig_S5_mt","missTrig_S5_mt",100,0,EB,100,0,40);
		missTrig_S5_mt.setTitle("Miss trig S5");
		missTrig_S5_mt.setTitleX("p (GeV)");
		missTrig_S5_mt.setTitleY("#theta");
		missTrig_S6_mt = new H2F("missTrig_S6_mt","missTrig_S6_mt",100,0,EB,100,0,40);
		missTrig_S6_mt.setTitle("Miss trig S6");
		missTrig_S6_mt.setTitleX("p (GeV)");
		missTrig_S6_mt.setTitleY("#theta");
		missTrig_S1_mf = new H2F("missTrig_S1_mf","missTrig_S1_mf",100,0,EB,100,-180,180);
		missTrig_S1_mf.setTitle("Miss Trig S1");
		missTrig_S1_mf.setTitleX("p (GeV)");
		missTrig_S1_mf.setTitleY("#phi");
		missTrig_S2_mf = new H2F("missTrig_S2_mf","missTrig_S2_mf",100,0,EB,100,-180,180);
		missTrig_S2_mf.setTitle("Miss Trig S2");
		missTrig_S2_mf.setTitleX("p (GeV)");
		missTrig_S2_mf.setTitleY("#phi");
		missTrig_S3_mf = new H2F("missTrig_S3_mf","missTrig_S3_mf",100,0,EB,100,-180,180);
		missTrig_S3_mf.setTitle("Miss Trig S3");
		missTrig_S3_mf.setTitleX("p (GeV)");
		missTrig_S3_mf.setTitleY("#phi");
		missTrig_S4_mf = new H2F("missTrig_S4_mf","missTrig_S4_mf",100,0,EB,100,-180,180);
		missTrig_S4_mf.setTitle("Miss Trig S4");
		missTrig_S4_mf.setTitleX("p (GeV)");
		missTrig_S4_mf.setTitleY("#phi");
		missTrig_S5_mf = new H2F("missTrig_S5_mf","missTrig_S5_mf",100,0,EB,100,-180,180);
		missTrig_S5_mf.setTitle("Miss Trig S5");
		missTrig_S5_mf.setTitleX("p (GeV)");
		missTrig_S5_mf.setTitleY("#phi");
		missTrig_S6_mf = new H2F("missTrig_S6_mf","missTrig_S6_mf",100,0,EB,100,-180,180);
		missTrig_S6_mf.setTitle("Miss Trig S6");
		missTrig_S6_mf.setTitleX("p (GeV)");
		missTrig_S6_mf.setTitleY("#phi");

		H_trig_S1_ETOT_E = new H1F("H_trig_S1_ETOT_E","H_trig_S1_ETOT_E",100,0,0.75);
		H_trig_S1_ETOT_E.setTitle("ETOT E S1 trig bit");
		H_trig_S1_ETOT_E.setTitleX("E_{ETOT} (GeV)");
		H_trig_S2_ETOT_E = new H1F("H_trig_S2_ETOT_E","H_trig_S2_ETOT_E",100,0,0.75);
		H_trig_S2_ETOT_E.setTitle("ETOT E S2 trig bit");
		H_trig_S2_ETOT_E.setTitleX("E_{ETOT} (GeV)");
		H_trig_S3_ETOT_E = new H1F("H_trig_S3_ETOT_E","H_trig_S3_ETOT_E",100,0,0.75);
		H_trig_S3_ETOT_E.setTitle("ETOT E S3 trig bit");
		H_trig_S3_ETOT_E.setTitleX("E_{ETOT} (GeV)");
		H_trig_S4_ETOT_E = new H1F("H_trig_S4_ETOT_E","H_trig_S4_ETOT_E",100,0,0.75);
		H_trig_S4_ETOT_E.setTitle("ETOT E S4 trig bit");
		H_trig_S4_ETOT_E.setTitleX("E_{ETOT} (GeV)");
		H_trig_S5_ETOT_E = new H1F("H_trig_S5_ETOT_E","H_trig_S5_ETOT_E",100,0,0.75);
		H_trig_S5_ETOT_E.setTitle("ETOT E S5 trig bit");
		H_trig_S5_ETOT_E.setTitleX("E_{ETOT} (GeV)");
		H_trig_S6_ETOT_E = new H1F("H_trig_S6_ETOT_E","H_trig_S6_ETOT_E",100,0,0.75);
		H_trig_S6_ETOT_E.setTitle("ETOT E S6 trig bit");
		H_trig_S6_ETOT_E.setTitleX("E_{ETOT} (GeV)");
		H_trig_S1_ECAL_E = new H1F("H_trig_S1_ECAL_E","H_trig_S1_ECAL_E",100,0,0.25);
		H_trig_S1_ECAL_E.setTitle("ECAL E S1 trig bit");
		H_trig_S1_ECAL_E.setTitleX("E_{ECAL} (GeV)");
		H_trig_S2_ECAL_E = new H1F("H_trig_S2_ECAL_E","H_trig_S2_ECAL_E",100,0,0.25);
		H_trig_S2_ECAL_E.setTitle("ECAL E S2 trig bit");
		H_trig_S2_ECAL_E.setTitleX("E_{ECAL} (GeV)");
		H_trig_S3_ECAL_E = new H1F("H_trig_S3_ECAL_E","H_trig_S3_ECAL_E",100,0,0.25);
		H_trig_S3_ECAL_E.setTitle("ECAL E S3 trig bit");
		H_trig_S3_ECAL_E.setTitleX("E_{ECAL} (GeV)");
		H_trig_S4_ECAL_E = new H1F("H_trig_S4_ECAL_E","H_trig_S4_ECAL_E",100,0,0.25);
		H_trig_S4_ECAL_E.setTitle("ECAL E S4 trig bit");
		H_trig_S4_ECAL_E.setTitleX("E_{ECAL} (GeV)");
		H_trig_S5_ECAL_E = new H1F("H_trig_S5_ECAL_E","H_trig_S5_ECAL_E",100,0,0.25);
		H_trig_S5_ECAL_E.setTitle("ECAL E S5 trig bit");
		H_trig_S5_ECAL_E.setTitleX("E_{ECAL} (GeV)");
		H_trig_S6_ECAL_E = new H1F("H_trig_S6_ECAL_E","H_trig_S6_ECAL_E",100,0,0.25);
		H_trig_S6_ECAL_E.setTitle("ECAL E S6 trig bit");
		H_trig_S6_ECAL_E.setTitleX("E_{ECAL} (GeV)");
		H_trig_S1_PCAL_E = new H1F("H_trig_S1_PCAL_E","H_trig_S1_PCAL_E",100,0,0.5);
		H_trig_S1_PCAL_E.setTitle("PCAL E S1 trig bit");
		H_trig_S1_PCAL_E.setTitleX("E_{ECAL} (GeV)");
		H_trig_S2_PCAL_E = new H1F("H_trig_S2_PCAL_E","H_trig_S2_PCAL_E",100,0,0.5);
		H_trig_S2_PCAL_E.setTitle("PCAL E S2 trig bit");
		H_trig_S2_PCAL_E.setTitleX("E_{PCAL} (GeV)");
		H_trig_S3_PCAL_E = new H1F("H_trig_S3_PCAL_E","H_trig_S3_PCAL_E",100,0,0.5);
		H_trig_S3_PCAL_E.setTitle("PCAL E S3 trig bit");
		H_trig_S3_PCAL_E.setTitleX("E_{PCAL} (GeV)");
		H_trig_S4_PCAL_E = new H1F("H_trig_S4_PCAL_E","H_trig_S4_PCAL_E",100,0,0.5);
		H_trig_S4_PCAL_E.setTitle("PCAL E S4 trig bit");
		H_trig_S4_PCAL_E.setTitleX("E_{PCAL} (GeV)");
		H_trig_S5_PCAL_E = new H1F("H_trig_S5_PCAL_E","H_trig_S5_PCAL_E",100,0,0.5);
		H_trig_S5_PCAL_E.setTitle("PCAL E S5 trig bit");
		H_trig_S5_PCAL_E.setTitleX("E_{PCAL} (GeV)");
		H_trig_S6_PCAL_E = new H1F("H_trig_S6_PCAL_E","H_trig_S6_PCAL_E",100,0,0.5);
		H_trig_S6_PCAL_E.setTitle("PCAL E S6 trig bit");
		H_trig_S6_PCAL_E.setTitleX("E_{PCAL} (GeV)");
		H_trig_S1_HTCC_n = new H1F("H_trig_S1_HTCC_n","H_trig_S1_HTCC_n",300,0,30);
		H_trig_S1_HTCC_n.setTitle("HTCC nphe S1 trig bit");
		H_trig_S1_HTCC_n.setTitleX("nphe");
		H_trig_S2_HTCC_n = new H1F("H_trig_S2_HTCC_n","H_trig_S2_HTCC_n",300,0,30);
		H_trig_S2_HTCC_n.setTitle("HTCC nphe S2 trig bit");
		H_trig_S2_HTCC_n.setTitleX("nphe");
		H_trig_S3_HTCC_n = new H1F("H_trig_S3_HTCC_n","H_trig_S3_HTCC_n",300,0,30);
		H_trig_S3_HTCC_n.setTitle("HTCC nphe S3 trig bit");
		H_trig_S3_HTCC_n.setTitleX("nphe");
		H_trig_S4_HTCC_n = new H1F("H_trig_S4_HTCC_n","H_trig_S4_HTCC_n",300,0,30);
		H_trig_S4_HTCC_n.setTitle("HTCC nphe S4 trig bit");
		H_trig_S4_HTCC_n.setTitleX("nphe");
		H_trig_S5_HTCC_n = new H1F("H_trig_S5_HTCC_n","H_trig_S5_HTCC_n",300,0,30);
		H_trig_S5_HTCC_n.setTitle("HTCC nphe S5 trig bit");
		H_trig_S5_HTCC_n.setTitleX("nphe");
		H_trig_S6_HTCC_n = new H1F("H_trig_S6_HTCC_n","H_trig_S6_HTCC_n",300,0,30);
		H_trig_S6_HTCC_n.setTitle("HTCC nphe S6 trig bit");
		H_trig_S6_HTCC_n.setTitleX("nphe");
		H_trig_S1_HTCC_N = new H1F("H_trig_S1_HTCC_N","H_trig_S1_HTCC_N",300,0,30);
		H_trig_S1_HTCC_N.setTitle("HTCC nphe (hig) S1 trig bit");
		H_trig_S1_HTCC_N.setTitleX("nphe");
		H_trig_S2_HTCC_N = new H1F("H_trig_S2_HTCC_N","H_trig_S2_HTCC_N",300,0,30);
		H_trig_S2_HTCC_N.setTitle("HTCC nphe (hig) S2 trig bit");
		H_trig_S2_HTCC_N.setTitleX("nphe");
		H_trig_S3_HTCC_N = new H1F("H_trig_S3_HTCC_N","H_trig_S3_HTCC_N",300,0,30);
		H_trig_S3_HTCC_N.setTitle("HTCC nphe (hig) S3 trig bit");
		H_trig_S3_HTCC_N.setTitleX("nphe");
		H_trig_S4_HTCC_N = new H1F("H_trig_S4_HTCC_N","H_trig_S4_HTCC_N",300,0,30);
		H_trig_S4_HTCC_N.setTitle("HTCC nphe (hig) S4 trig bit");
		H_trig_S4_HTCC_N.setTitleX("nphe");
		H_trig_S5_HTCC_N = new H1F("H_trig_S5_HTCC_N","H_trig_S5_HTCC_N",300,0,30);
		H_trig_S5_HTCC_N.setTitle("HTCC nphe (hig) S5 trig bit");
		H_trig_S5_HTCC_N.setTitleX("nphe");
		H_trig_S6_HTCC_N = new H1F("H_trig_S6_HTCC_N","H_trig_S6_HTCC_N",300,0,30);
		H_trig_S6_HTCC_N.setTitle("HTCC nphe (hig) S6 trig bit");
		H_trig_S6_HTCC_N.setTitleX("nphe");
		H_trig_S1_HTCC_N_track = new H1F("H_trig_S1_HTCC_N_track","H_trig_S1_HTCC_N_track",300,0,30);
		H_trig_S1_HTCC_N_track.setTitle("HTCC nphe (hig+trk) S1 trig bit");
		H_trig_S1_HTCC_N_track.setTitleX("nphe");
		H_trig_S2_HTCC_N_track = new H1F("H_trig_S2_HTCC_N_track","H_trig_S2_HTCC_N_track",300,0,30);
		H_trig_S2_HTCC_N_track.setTitle("HTCC nphe (hig+trk) S2 trig bit");
		H_trig_S2_HTCC_N_track.setTitleX("nphe");
		H_trig_S3_HTCC_N_track = new H1F("H_trig_S3_HTCC_N_track","H_trig_S3_HTCC_N_track",300,0,30);
		H_trig_S3_HTCC_N_track.setTitle("HTCC nphe (hig+trk) S3 trig bit");
		H_trig_S3_HTCC_N_track.setTitleX("nphe");
		H_trig_S4_HTCC_N_track = new H1F("H_trig_S4_HTCC_N_track","H_trig_S4_HTCC_N_track",300,0,30);
		H_trig_S4_HTCC_N_track.setTitle("HTCC nphe (hig+trk) S4 trig bit");
		H_trig_S4_HTCC_N_track.setTitleX("nphe");
		H_trig_S5_HTCC_N_track = new H1F("H_trig_S5_HTCC_N_track","H_trig_S5_HTCC_N_track",300,0,30);
		H_trig_S5_HTCC_N_track.setTitle("HTCC nphe (hig+trk) S5 trig bit");
		H_trig_S5_HTCC_N_track.setTitleX("nphe");
		H_trig_S6_HTCC_N_track = new H1F("H_trig_S6_HTCC_N_track","H_trig_S6_HTCC_N_track",300,0,30);
		H_trig_S6_HTCC_N_track.setTitle("HTCC nphe (hig+trk) S6 trig bit");
		H_trig_S6_HTCC_N_track.setTitleX("nphe");
		H_trig_S1_PCAL_XY = new H2F("H_trig_S1_PCAL_XY","H_trig_S1_PCAL_XY",100,-400,400,100,-400,400);
		H_trig_S1_PCAL_XY.setTitle("PCAL XY S1 trig bit");
		H_trig_S1_PCAL_XY.setTitleX("X (cm)");
		H_trig_S1_PCAL_XY.setTitleY("Y (cm)");
		H_trig_S2_PCAL_XY = new H2F("H_trig_S2_PCAL_XY","H_trig_S2_PCAL_XY",100,-400,400,100,-400,400);
		H_trig_S2_PCAL_XY.setTitle("PCAL XY S2 trig bit");
		H_trig_S2_PCAL_XY.setTitleX("X (cm)");
		H_trig_S2_PCAL_XY.setTitleY("Y (cm)");
		H_trig_S3_PCAL_XY = new H2F("H_trig_S3_PCAL_XY","H_trig_S3_PCAL_XY",100,-400,400,100,-400,400);
		H_trig_S3_PCAL_XY.setTitle("PCAL XY S3 trig bit");
		H_trig_S3_PCAL_XY.setTitleX("X (cm)");
		H_trig_S3_PCAL_XY.setTitleY("Y (cm)");
		H_trig_S4_PCAL_XY = new H2F("H_trig_S4_PCAL_XY","H_trig_S4_PCAL_XY",100,-400,400,100,-400,400);
		H_trig_S4_PCAL_XY.setTitle("PCAL XY S4 trig bit");
		H_trig_S4_PCAL_XY.setTitleX("X (cm)");
		H_trig_S4_PCAL_XY.setTitleY("Y (cm)");
		H_trig_S5_PCAL_XY = new H2F("H_trig_S5_PCAL_XY","H_trig_S5_PCAL_XY",100,-400,400,100,-400,400);
		H_trig_S5_PCAL_XY.setTitle("PCAL XY S5 trig bit");
		H_trig_S5_PCAL_XY.setTitleX("X (cm)");
		H_trig_S5_PCAL_XY.setTitleY("Y (cm)");
		H_trig_S6_PCAL_XY = new H2F("H_trig_S6_PCAL_XY","H_trig_S6_PCAL_XY",100,-400,400,100,-400,400);
		H_trig_S6_PCAL_XY.setTitle("PCAL XY S6 trig bit");
		H_trig_S6_PCAL_XY.setTitleX("X (cm)");
		H_trig_S6_PCAL_XY.setTitleY("Y (cm)");
		H_trig_S1_HTCC_XY = new H2F("H_trig_S1_HTCC_XY","H_trig_S1_HTCC_XY",50,-120,120,50,-120,120);
		H_trig_S1_HTCC_XY.setTitle("HTCC XY S1 trig bit");
		H_trig_S1_HTCC_XY.setTitleX("X (cm)");
		H_trig_S1_HTCC_XY.setTitleY("Y (cm)");
		H_trig_S2_HTCC_XY = new H2F("H_trig_S2_HTCC_XY","H_trig_S2_HTCC_XY",50,-120,120,50,-120,120);
		H_trig_S2_HTCC_XY.setTitle("HTCC XY S2 trig bit");
		H_trig_S2_HTCC_XY.setTitleX("X (cm)");
		H_trig_S2_HTCC_XY.setTitleY("Y (cm)");
		H_trig_S3_HTCC_XY = new H2F("H_trig_S3_HTCC_XY","H_trig_S3_HTCC_XY",50,-120,120,50,-120,120);
		H_trig_S3_HTCC_XY.setTitle("HTCC XY S3 trig bit");
		H_trig_S3_HTCC_XY.setTitleX("X (cm)");
		H_trig_S3_HTCC_XY.setTitleY("Y (cm)");
		H_trig_S4_HTCC_XY = new H2F("H_trig_S4_HTCC_XY","H_trig_S4_HTCC_XY",50,-120,120,50,-120,120);
		H_trig_S4_HTCC_XY.setTitle("HTCC XY S4 trig bit");
		H_trig_S4_HTCC_XY.setTitleX("X (cm)");
		H_trig_S4_HTCC_XY.setTitleY("Y (cm)");
		H_trig_S5_HTCC_XY = new H2F("H_trig_S5_HTCC_XY","H_trig_S5_HTCC_XY",50,-120,120,50,-120,120);
		H_trig_S5_HTCC_XY.setTitle("HTCC XY S5 trig bit");
		H_trig_S5_HTCC_XY.setTitleX("X (cm)");
		H_trig_S5_HTCC_XY.setTitleY("Y (cm)");
		H_trig_S6_HTCC_XY = new H2F("H_trig_S6_HTCC_XY","H_trig_S6_HTCC_XY",50,-120,120,50,-120,120);
		H_trig_S6_HTCC_XY.setTitle("HTCC XY S6 trig bit");
		H_trig_S6_HTCC_XY.setTitleX("X (cm)");
		H_trig_S6_HTCC_XY.setTitleY("Y (cm)");

		H_pip_vz_ve = new H2F("H_pip_vz_ve","H_pip_vz_ve",100,-5,15,100,-10,20);
		H_pip_vz_ve.setTitle("#pi^+ vz vs e vz");
		H_pip_vz_ve.setTitleX("e vz (cm)");
		H_pip_vz_ve.setTitleY("#pi^+ vz (cm)");
		H_pip_vz_ve_diff = new H1F("H_pip_vz_ve_diff","H_pip_vz_ve_diff",100,-10,20);
		H_pip_vz_ve_diff.setTitle("#pi^+ e vz diff");
		H_pip_vz_ve_diff.setTitleX("#Delta vz (cm)");
		H_pip_vz_ve_diff_mom = new H2F("H_pip_vz_ve_diff_mom","H_pip_vz_ve_diff_mom",100,0,6,100,-10,20);
		H_pip_vz_ve_diff_mom.setTitle("#pi^+ e vz diff vs mom");
		H_pip_vz_ve_diff_mom.setTitleX("#pi^+ mom (GeV)");
		H_pip_vz_ve_diff_mom.setTitleY("#Delta vz (cm)");
		H_pip_vz_ve_diff_theta = new H2F("H_pip_vz_ve_diff_theta","H_pip_vz_ve_diff_theta",100,0,40,100,-10,20);
		H_pip_vz_ve_diff_theta.setTitle("#pi^+ e vz diff vs #theta");
		H_pip_vz_ve_diff_theta.setTitleX("#theta (^o)");
		H_pip_vz_ve_diff_theta.setTitleY("#Delta vz (cm)");
		H_pip_vz_ve_diff_phi = new H2F("H_pip_vz_ve_diff_phi","H_pip_vz_ve_diff_phi",100,-180,180,100,-10,20);
		H_pip_vz_ve_diff_phi.setTitle("#pi^+ e vz diff vs #phi");
		H_pip_vz_ve_diff_phi.setTitleX("#phi (^o)");
		H_pip_vz_ve_diff_phi.setTitleY("#Delta vz (cm)");
		H_pip_vz_ve_diff_Dphi = new H2F("H_pip_vz_ve_diff_Dphi","H_pip_vz_ve_diff_Dphi",100,-180,180,100,-10,20);
		H_pip_vz_ve_diff_Dphi.setTitle("#pi^+ e vz diff vs #Delta#phi");
		H_pip_vz_ve_diff_Dphi.setTitleX("#Delta#phi (^o)");
		H_pip_vz_ve_diff_Dphi.setTitleY("#Delta vz (cm)");
		H_pip_Dphi = new H1F("H_pip_Dphi","H_pip_Dphi",100,-180,180);
		H_pip_Dphi.setTitle("#pi^+ e #Delta#phi");
		H_pip_Dphi.setTitleX("#Delta#phi (^o)");

		H_MM_epip_Spip = new H1F[6];
		H_MM_epip_Se = new H1F[6];
		for(int i=0;i<6;i++){
			H_MM_epip_Spip[i] = new H1F(String.format("H_MM_epip_Spip%d",i+1),String.format("H_MM_epip_Spip%d",i+1),100,0,4);
			H_MM_epip_Spip[i].setTitle(String.format("pi^+ S%d MM #pi^+ vs #phi",i+1));
			H_MM_epip_Spip[i].setTitleX("MM_{e#pi^+} (GeV)");
			H_MM_epip_Se[i] = new H1F(String.format("H_MM_epip_Se%d",i+1),String.format("H_MM_epip_Se%d",i+1),100,0,4);
			H_MM_epip_Se[i].setTitle(String.format("e S%d MM #pi^+ vs #phi",i+1));
			H_MM_epip_Se[i].setTitleX("MM_{e#pi^+} (GeV)");
		}
		H_pip_vtd = new H1F("H_pip_vtd","H_pip_vtd",100,-5,5);
		H_pip_vtd.setTitle("Vertex time difference e #pi^+");
		H_pip_vtd.setTitleX("#Delta t_{v} (ns)");
		H_pim_vtd = new H1F("H_pim_vtd","H_pim_vtd",100,-5,5);
		H_pim_vtd.setTitle("Vertex time difference e #pi^-");
		H_pim_vtd.setTitleX("#Delta t_{v} (ns)");
		H_MM_epip_phi = new H2F("H_MM_epip_phi","H_MM_epip_phi",100,-180,180,100,-1,5);
		H_MM_epip_phi.setTitle("Missing mass #pi^+ vs #phi");
		H_MM_epip_phi.setTitleX("#phi (^o)");
		H_MM_epip_phi.setTitleY("MM_{e#pi^+} (GeV)");
		H_pip_beta_p = new H2F("H_pip_beta_p","H_pip_beta_p",100,0,EB,100,0.9,1.1);
		H_pip_beta_p.setTitle("#pi^+ #beta vs momentum");
		H_pip_beta_p.setTitleX("p (GeV)");
		H_pip_beta_p.setTitleY("#beta");
		H_pip_beta2_p = new H2F("H_pip_beta2_p","H_pip_beta2_p",100,0,EB,100,0.9,1.1);
		H_pip_beta2_p.setTitle("#pi^+ #beta vs momentum");
		H_pip_beta2_p.setTitleX("p (GeV)");
		H_pip_beta2_p.setTitleY("#beta");
		H_pip_vtd_mom = new H2F("H_pip_vtd_mom","H_pip_vtd_mom",100,0,EB,100,-2,2);
		H_pip_vtd_mom.setTitle("#pi^+ time diff e #pi^+ vs mom");
		H_pip_vtd_mom.setTitleX("p (GeV)");
		H_pip_vtd_mom.setTitleY("#Delta t_{v} (ns)");
		H_pip_vtd_theta = new H2F("H_pip_vtd_theta","H_pip_vtd_theta",100,0,40,100,-2,2);
		H_pip_vtd_theta.setTitle("#pi^+ time diff e #pi^+ vs #theta");
		H_pip_vtd_theta.setTitleX("#theta (^o)");
		H_pip_vtd_theta.setTitleY("#Delta t_{v} (ns)");
		H_pip_vtd_phi = new H2F("H_pip_vtd_phi","H_pip_vtd_phi",100,-180,180,100,-2,2);
		H_pip_vtd_phi.setTitle("#pi^+ time diff e #pi^+ vs #phi");
		H_pip_vtd_phi.setTitleX("#phi (^o)");
		H_pip_vtd_phi.setTitleY("#Delta t_{v} (ns)");

		H_pip_theta_phi = new H2F("H_pip_theta_phi","H_pip_theta_phi",100,-180,180,100,0,40);
		H_pip_theta_phi.setTitle("#pi^+ #theta vs #phi");
		H_pip_theta_phi.setTitleX("#phi (^o)");
		H_pip_theta_phi.setTitleY("#theta (^o)");
		H_pip_theta_mom = new H2F("H_pip_theta_mom","H_pip_theta_mom",100,0,EB,100,0,40);
		H_pip_theta_mom.setTitle("#pi^+ #theta vs mom");
		H_pip_theta_mom.setTitleX("p (GeV)");
		H_pip_theta_mom.setTitleY("#theta");
		H_pip_phi_mom = new H2F("H_pip_phi_mom","H_pip_phi_mom",100,0,EB,100,-180,180);
		H_pip_phi_mom.setTitle("#pi^+ #phi vs mom");
		H_pip_phi_mom.setTitleX("p (GeV)");
		H_pip_phi_mom.setTitleY("#phi (^o)");
		H_pip_vz_phi = new H2F("H_pip_vz_phi","H_pip_vz_phi",100,-180,180,100,-20,20);
		H_pip_vz_phi.setTitle("#pi^+ vz vs #phi");
		H_pip_vz_phi.setTitleX("#phi (^o)");
		H_pip_vz_phi.setTitleY("vz (cm)");
		H_pip_vz_theta = new H2F("H_pip_vz_theta","H_pip_vz_theta",100,0,40,100,-20,20);
		H_pip_vz_theta.setTitle("#pi^+ vz vs #theta");
		H_pip_vz_theta.setTitleX("#theta (^o)");
		H_pip_vz_theta.setTitleY("vz (cm)");
		H_pip_vz_mom = new H2F("H_pip_vz_mom","H_pip_vz_mom",100,0,EB,100,-20,20);
		H_pip_vz_mom.setTitle("#pi^+ vz vs mom");
		H_pip_vz_mom.setTitleX("p (GeV)");
		H_pip_vz_mom.setTitleY("vz (cm)");
		H_pip_e_vt = new H2F("H_pip_e_vt","H_pip_e_vt",100,525,600,100,525,600);
		H_pip_e_vt.setTitle("#pi^+ vs e vertex times");
		H_pip_e_vt.setTitleX("e t_{v} (ns)");
		H_pip_e_vt.setTitleY("#pi^+ t_{v} (ns)");
		H_epip_e_theta_phi = new H2F("H_epip_e_theta_phi","H_epip_e_theta_phi",100,-180,180,100,0,40);
		H_epip_e_theta_phi.setTitle("e #theta vs #phi");
		H_epip_e_theta_phi.setTitleX("#phi (^o)");
		H_epip_e_theta_phi.setTitleY("#theta (^o)");
		H_epip_e_theta_mom = new H2F("H_epip_e_theta_mom","H_epip_e_theta_mom",100,0,EB,100,0,40);
		H_epip_e_theta_mom.setTitle("e #theta vs mom");
		H_epip_e_theta_mom.setTitleX("p (GeV)");
		H_epip_e_theta_mom.setTitleY("#theta (^o)");
		H_epip_e_phi_mom = new H2F("H_epip_e_phi_mom","H_epip_e_phi_mom",100,0,EB,100,-180,180);
		H_epip_e_phi_mom.setTitle("e #phi vs mom");
		H_epip_e_phi_mom.setTitleX("p (GeV)");
		H_epip_e_phi_mom.setTitleY("#phi (^o)");
		H_epip_xB_Q2 = new H2F("H_epip_xB_Q2","H_epip_xB_Q2",100,0,1,100,0,EB);
		H_epip_xB_Q2.setTitle("Q^2 vs x_B");
		H_epip_xB_Q2.setTitleX("x_B");
		H_epip_xB_Q2.setTitleY("Q^2");
		H_epip_e_W_Q2 = new H2F("H_epip_e_W_Q2","H_epip_e_W_Q2",100,0,5,100,0,EB);
		H_epip_e_W_Q2.setTitle("Q^2 vs W");
		H_epip_e_W_Q2.setTitleX("W");
		H_epip_e_W_Q2.setTitleY("Q^2");
		H_epip_e_t_phi = new H2F("H_epip_e_t_phi","H_epip_e_t_phi",100,-180,180,100,0,5);
		H_epip_e_t_phi.setTitle("-t vs #phi");
		H_epip_e_t_phi.setTitleX("#phi (^o)");
		H_epip_e_t_phi.setTitleY("-t (GeV)");
		H_MM_epip_zoom = new H1F("H_MM_epip_phi_zoom","H_MM_epip_phi_zoom",100,0,2);
		H_MM_epip_zoom.setTitle("Missing mass e#pi^+");
		H_MM_epip_zoom.setTitleX("MM_{e#pi^+} (GeV)");
		H_MM_epip = new H1F("H_MM_epip","H_MM_epip",100,-1,5);
		H_MM_epip.setTitle("Missing Mass e#pi^+");
		H_MM_epip.setTitleX("MM_{e#pi^+} (GeV)");

		H_rho_prot = new H2F("H_rho_prot","H_rho_prot",100,0,2,100,0,4);
		H_rho_prot.setTitle("MM e#pi^+#pi^- vs IM #pi^+#pi^-");
		H_rho_prot.setTitleX("IM #pi^+#pi^-");
		H_rho_prot.setTitleY("MM e#pi^+#pi^-");
		H_rho_pip_beta = new H2F("H_rho_pip_beta","H_rho_pip_beta",100,0,8,100,0.9,1.2);
		H_rho_pip_beta.setTitle("#pi^+ #beta vs p");
		H_rho_pip_beta.setTitleX("p (GeV)");
		H_rho_pip_beta.setTitleY("#beta");
		H_rho_pim_beta = new H2F("H_rho_pim_beta","H_rho_pim_beta",100,0,8,100,0.9,1.2);
		H_rho_pim_beta.setTitle("#pi^- #beta vs p");
		H_rho_pim_beta.setTitleX("p (GeV)");
		H_rho_pim_beta.setTitleY("#beta");
		H_rho_IM = new H1F("H_rho_IM","H_rho_IM",100,0,2);
		H_rho_IM.setTitle("IM #pi^+#pi^-");
		H_rho_IM.setTitleX("IM (GeV)");
		H_rho_MM = new H1F("H_rho_IM","H_rho_IM",100,0,4);
		H_rho_MM.setTitle("MM #pi^+#pi^-");
        H_rho_MM.setTitleX("MM (GeV)");
        H_rho_Q2_xB = new H2F("H_rho_Q2_xB", "H_rho_Q2_xB", 100, 0, 1, 100, 0, EB);
        H_rho_Q2_xB.setTitle("Q^2 vs xB");
        H_rho_Q2_xB.setTitleX("Q^2 (GeV^2)");
        H_rho_Q2_xB.setTitleY("xB");
        H_rho_Q2_W = new H2F("H_rho_Q2_W", "H_rho_Q2_W", 100, 0, 5, 100, 0, EB);
        H_rho_Q2_W.setTitle("Q^2 vs W");
        H_rho_Q2_W.setTitleX("W (GeV)");
        H_rho_Q2_W.setTitleY("Q^2 (GeV^2)");

		H_CVT_ft = new H2F("H_CVT_ft","H_CVT_ft",100,-180,180,100,0,180);
		H_CVT_ft.setTitle("CVT #phi vs #theta");
		H_CVT_ft.setTitleX("#phi (^o)");
		H_CVT_ft.setTitleY("#theta (^o)");
		H_CVT_pt = new H2F("H_CVT_pt","H_CVT_pt",100,0,180,100,0,3.5);
		H_CVT_pt.setTitle("CVT p vs #theta");
		H_CVT_pt.setTitleX("#theta (^o)");
		H_CVT_pt.setTitleY("p (GeV)");
		H_CVT_pf = new H2F("H_CVT_pf","H_CVT_pf",100,-180,180,100,0,3.5);
		H_CVT_pf.setTitle("CVT p vs #phi");
		H_CVT_pf.setTitleX("#phi (^o)");
		H_CVT_pf.setTitleY("p (GeV)");
		H_CVT_zf = new H2F("H_CVT_zf","H_CVT_zf",100,-180,180,100,-25,25);
		H_CVT_zf.setTitle("CVT z vs #phi");
		H_CVT_zf.setTitleX("#phi (^o)");
		H_CVT_zf.setTitleY("z (cm)");
		H_CVT_zp = new H2F("H_CVT_zp","H_CVT_zp",100,0,3.5,100,-25,25);
		H_CVT_zp.setTitle("CVT z vs p");
		H_CVT_zp.setTitleX("p (GeV)");
		H_CVT_zp.setTitleY("z (cm)");
		H_CVT_zt = new H2F("H_CVT_zt","H_CVT_zt",100,0,180,100,-25,25);
		H_CVT_zt.setTitle("CVT z vs #theta");
		H_CVT_zt.setTitleX("#theta (^o)");
		H_CVT_zt.setTitleY("z (cm)");
		H_CVT_p = new H1F("H_CVT_p","H_CVT_p",100,0,3.5);
		H_CVT_p.setTitle("CVT p");
		H_CVT_p.setTitleX("p (GeV)");
		H_CVT_t = new H1F("H_CVT_t","H_CVT_t",100,0,180);
		H_CVT_t.setTitle("CVT #theta");
		H_CVT_t.setTitleX("#theta (^o)");
		H_CVT_f = new H1F("H_CVT_f","H_CVT_f",100,-180,180);
		H_CVT_f.setTitle("CVT #phi");
		H_CVT_f.setTitleX("#phi (^o)");
		H_CVT_z = new H1F("H_CVT_z","H_CVT_z",100,-25,25);
		H_CVT_z.setTitle("CVT z vertex");
		H_CVT_z.setTitleX("z (cm)");
		H_CVT_z_pos = new H1F("H_CVT_z_pos","H_CVT_z_pos",100,-25,25);
		H_CVT_z_pos.setTitle("CVT z vertex for positives");
		H_CVT_z_pos.setTitleX("z (cm)");
		H_CVT_z_neg = new H1F("H_CVT_z_neg","H_CVT_z_neg",100,-25,25);
		H_CVT_z_neg.setTitle("CVT z vertex for negatives");
		H_CVT_z_neg.setTitleX("z (cm)");
		H_CVT_d0_pos = new H1F("H_CVT_d0_pos","H_CVT_d0_pos",200,-1.0, 1.0);
		H_CVT_d0_pos.setTitle("CVT d0 vertex for positives");
		H_CVT_d0_pos.setTitleX("d0 (cm)");
		H_CVT_d0_neg = new H1F("H_CVT_d0_neg","H_CVT_d0_neg",200,-1.0, 1.0);
		H_CVT_d0_neg.setTitle("CVT d0 vertex for negatives");
		H_CVT_d0_neg.setTitleX("d0 (cm)");
		H_CVT_absd0_pos = new H1F("H_CVT_absd0_pos","H_CVT_absd0_pos",400, 0.0, 2.0);
		H_CVT_absd0_pos.setTitle("CVT |d0| vertex for positives");
		H_CVT_absd0_pos.setTitleX("|d0| (cm)");
		H_CVT_absd0_neg = new H1F("H_CVT_absd0_neg","H_CVT_absd0_neg",400, 0.0, 2.0);
		H_CVT_absd0_neg.setTitle("CVT |d0| vertex for negatives");
		H_CVT_absd0_neg.setTitleX("|d0| (cm)");
		H_CVT_e_corr_vz = new H2F("H_CVT_e_corr_vz","H_CVT_e_corr_vz",100,-25,25,100,-25,25);
		H_CVT_e_corr_vz.setTitle("Vertex correlation");
		H_CVT_e_corr_vz.setTitleX("vz e (cm)");
		H_CVT_e_corr_vz.setTitleY("vz CVT (cm)");
		H_CVT_e_corr_phi = new H2F("H_CVT_e_corr_phi","H_CVT_e_corr_phi",100,-180,180,100,-180,180);
		H_CVT_e_corr_phi.setTitle("#phi correlation");
		H_CVT_e_corr_phi.setTitleX("#phi e (^o)");
		H_CVT_e_corr_phi.setTitleY("#phi CVT (^o)");
		H_CVT_e_vz_diff = new H1F("H_CVT_e_vz_diff","H_CVT_e_vz_diff",100,-25,25);
		H_CVT_e_vz_diff.setTitle("e CVT #Delta vz");
		H_CVT_e_vz_diff.setTitleX("#Delta vz (cm)");
		H_CVT_e_phi_diff = new H1F("H_CVT_e_phi_diff","H_CVT_e_phi_diff",100,-90,90);
		H_CVT_e_phi_diff.setTitle("e CVT #Delta#phi");
		H_CVT_e_phi_diff.setTitleX("#Delta#phi (^o)");
		H_CVT_corr_e_theta = new H2F("H_CVT_e_corr_theta","H_CVT_e_corr_theta",100,20,100,100,0,15);
		H_CVT_corr_e_theta.setTitle("e CVT #theta correlations");
		H_CVT_corr_e_theta.setTitleX("CVT #theta (^o)");
		H_CVT_corr_e_theta.setTitleY("e #theta (^o)");
		H_CVT_chi2 = new H1F("H_CVT_chi2","H_CVT_chi2",100,0,200);
		H_CVT_chi2.setTitle("CVT #chi^2 for electrons");
		H_CVT_chi2.setTitleX("#chi^2");
		H_CVT_chi2_pos = new H1F("H_CVT_chi2_pos","H_CVT_chi2_pos",100,0,200);
		H_CVT_chi2_pos.setTitle("CVT #chi^2 for positives");
		H_CVT_chi2_pos.setTitleX("#chi^2");
		H_CVT_chi2_neg = new H1F("H_CVT_chi2_neg","H_CVT_chi2_neg",100,0,200);
		H_CVT_chi2_neg.setTitle("CVT #chi^2 for negatives");
		H_CVT_chi2_neg.setTitleX("#chi^2");
		H_CVT_d0 = new H1F("H_CVT_d0", "H_CVT_d0", 100, 0, 0.5);
        H_CVT_d0.setTitle("CVTRec::Track d0, All tracks ");
        H_CVT_d0.setTitleX("d0 (cm)");
        H_CVT_charge = new H1F("H_CVT_charge", "H_CVT_charge", 10, -5.5, 5.5);
        H_CVT_charge.setTitle("CVT track charge");
        H_CVT_charge.setTitleX("q/e");
        H_CVT_vz_mom = new H2F("H_CVT_vz_mom", "H_CVT_vz_mom", 100, 0, 3.5, 100, -25., 25.);
        H_CVT_vz_mom.setTitle("CVT z vertex vs mom");
        H_CVT_vz_mom.setTitleX("p (GeV/c");
        H_CVT_vz_mom.setTitleY("z (cm)");
        H_CVT_vz_theta = new H2F("H_CVT_vz_theta", "H_CVT_vz_theta", 100, 0, 180., 100, -25., 25.);
        H_CVT_vz_theta.setTitle("CVT z vertex vs theta");
        H_CVT_vz_theta.setTitleX("#theta (^o)");
        H_CVT_vz_theta.setTitleY("z (cm)");
        H_CVT_vz_phi = new H2F("H_CVT_vz_phi", "H_CVT_vz_phi", 200, -180., 180., 100, -25., 25.);
        H_CVT_vz_phi.setTitle("CVT z vertex vs phi");
        H_CVT_vz_phi.setTitleX("#phi (^o)");
        H_CVT_vz_phi.setTitleY("z (cm)");
        H_CVT_phi = new H1F("H_CVT_phi", "H_CVT_phi", 100, -180, 180);
        H_CVT_phi.setTitle("CVT #phi");
        H_CVT_phi.setTitleX("#phi (^o)");
        H_CVT_theta = new H1F("H_CVT_theta", "H_CVT_theta", 100, 0, 180);
        H_CVT_theta.setTitle("CVT #theta");
        H_CVT_theta.setTitleX("#theta (^o)");

	  	H_CVT_ndf = new H1F("H_CVT_ndf","H_CVT_ndf",10,0.5,10.5);
		H_CVT_ndf.setTitle("CVT NDF");
		H_CVT_ndf.setTitleX("NDF");
		H_CVT_pathlength = new H1F("H_CVT_pathlength","H_CVT_pathlength",100,20,75);
		H_CVT_pathlength.setTitle("CVT pathlegnth");
		H_CVT_pathlength.setTitleX("path (cm)");
		H_elast_e_p_th = new H2F("H_elast_e_p_th","H_elast_e_p_th",100,0,EB,100,0,25);
		H_elast_e_p_th.setTitle("e elastic #theta vs p");
		H_elast_e_p_th.setTitleX("p (GeV)");
		H_elast_e_p_th.setTitleY("#theta (^o)");
		H_elast_W_sect = new H2F("H_elast_W_sect","H_elast_W_sect",6,0.5,6.5,100,0,2);
		H_elast_W_sect.setTitle("elastic W vs sect");
		H_elast_W_sect.setTitleX("e sect");
		H_elast_W_sect.setTitleY("W (GeV)");
		H_elast_W = new H1F("H_elast_W","H_elast_W",100,0,2);
		H_elast_W.setTitle("elastic W");
		H_elast_W.setTitleX("W (GeV)");
		H_CVT_corr_e_mom = new H2F("H_CVT_corr_e_mom","H_CVT_corr_e_mom",100,EB/2,EB,100,EB/2,EB);
		H_CVT_corr_e_mom.setTitle("e mom rec vs calc");
		H_CVT_corr_e_mom.setTitleX("e mom calc (GeV)");
		H_CVT_corr_e_mom.setTitleY("e mom mes (GeV)");

		H_g1_tf = new H2F("H_g1_tf","H_g1_tf",100,-180,180,100,0,40);
		H_g1_tf.setTitle("#gamma1 #theta vs #phi");
		H_g1_tf.setTitleX("#phi (^o)");
		H_g1_tf.setTitleY("#theta (^o)");
		H_g2_tf = new H2F("H_g2_tf","H_g2_tf",100,-180,180,100,0,40);
		H_g2_tf.setTitle("#gamma2 #theta vs #phi");
		H_g2_tf.setTitleX("#phi (^o)");
		H_g2_tf.setTitleY("#theta (^o)");
		H_g1_te = new H2F("H_g1_te","H_g1_te",100,0,EB/2,100,0,40);
		H_g1_te.setTitle("#gamma1 #theta vs E");
		H_g1_te.setTitleX("E (GeV)");
		H_g1_te.setTitleY("#theta (^o)");
		H_g2_te = new H2F("H_g2_te","H_g2_te",100,0,EB/2,100,0,40);
		H_g2_te.setTitle("#gamma2 #theta vs E");
		H_g2_te.setTitleX("E (GeV)");
		H_g2_te.setTitleY("#theta (^o)");
		H_gg_open_a = new H2F("H_gg_open_a","H_gg_open_a",100,0,EB,100,0,30);
		H_gg_open_a.setTitle("#gamma#gamma opening angle vs E");
		H_gg_open_a.setTitleY("#theta_{#gamma#gamma} (^o)");
		H_gg_open_a.setTitleX("E_{#gamma#gamma} (GeV)");
		H_gg_m = new H1F("H_gg_m","H_gg_m",100,0,0.7);
		H_gg_m.setTitle("#gamma#gamma invariant mass");
		H_gg_m.setTitleX("m_{#gamma#gamma} (GeV)");
		H_e_TOF_xy = new H2F("H_e_TOF_xy","H_e_TOF_xy",100,-400,400,100,-400,400);
		H_e_TOF_xy.setTitle("electron TOF Y vs X");
		H_e_TOF_xy.setTitleX("X (cm)");
		H_e_TOF_xy.setTitleY("Y (cm)");
		H_e_TOF_t_path = new H2F("H_e_TOF_t_path","H_e_TOF_t_path",100,tofvt1+24,tofvt2+24,100,600,775);
		H_e_TOF_t_path.setTitle("electron path vs TOF");
		H_e_TOF_t_path.setTitleX("TOF (ns)");
		H_e_TOF_t_path.setTitleY("path (cm)");
		H_e_vt1 = new H1F("H_e_vt1","H_e_vt1",100,-1,1);
		H_e_vt1.setTitle("electron vertex time");
		H_e_vt1.setTitleX("t (ns)");
		H_e_vt2 = new H1F("H_e_vt2","H_e_vt2",100,-1,1);
		H_e_vt2.setTitle("electron vertex time");
		H_e_vt2.setTitleX("t (ns)");
		H_o_TOF = new H2F("H_o_TOF","H_o_TOF",500,tofvt1,tofvt2,500,tofvt1,tofvt2);
		H_o_TOF.setTitle("vertex time others vs electron");
		H_o_TOF.setTitleX("elec v_t");
		H_o_TOF.setTitleY("others v_t");
		H_o_vt = new H1F("H_o_vt","H_o_vt",10000,-10,10);
		H_o_vt.setTitle("others vertex time");
		H_o_vt.setTitleX("t (ns)");

        VB = new LorentzVector(0,0,Ebeam,Ebeam);
		VT = new LorentzVector(0,0,0,0.93827);
		H_e_theta_phi = new H2F("H_e_theta_phi","H_e_theta_phi",100,-180,180,100,0,40);
		H_e_theta_phi.setTitle("electron theta vs phi");
		H_e_theta_phi.setTitleX("#phi (^o)");
		H_e_theta_phi.setTitleY("#theta (^o)");
		H_e_theta_mom_S = new H2F[6];
		H_trig_theta_mom_S = new H2F[6];
		H_trig_phi_mom_S = new H2F[6];
		H_trig_phi_theta_S = new H1F[7][10];
		H_trig_theta_phi_S = new H2F[6];
		H_trig_vz_mom_S = new H2F[6];
		H_trig_vy_vz_S = new H2F[6];
		H_trig_vz_theta_S = new H2F[6];
		H_trig_ECALsampl_S = new H2F[6];
		H_trig_PCAL_vt_S = new H1F[6];
		H_trig_ECIN_vt_S = new H1F[6];
        H_trig_ECOU_vt_S = new H1F[6];
        H_trig_PCALECAL_S = new H2F[6];
        H_trig_HTCCn_theta_S = new H2F[6];
        H_trig_LTCCn_theta_S = new H2F[6];
        H_trig_ECAL_pos_S = new H2F[7];
        H_trig_TOF_pos_S = new H2F[7];
        H_trig_HTCC_pos_S = new H2F[7];
        H_trig_DCR1_pos_S = new H2F[7];
        H_trig_DCR2_pos_S = new H2F[7];
        H_trig_DCR3_pos_S = new H2F[7];
        H_trig_S_HTCC_theta = new H1F[6];
        H_e_W_S = new H1F[6];
		H_e_Q2_S = new H1F[6];
		H_e_W_phi_S = new H2F[6];

		for(int s=0;s<7;s++){
			for(int it=0;it<10;it++){
				float thetaMin = 5+2.0f*it;
				float thetaMax = 5+2.0f*(it+1);
				H_trig_phi_theta_S[s][it] = new H1F(String.format("H_trig_phi_theta_t%d_S%d",it+1,s+1),String.format("H_trig_phi_theta_t%d_S%d",it+1,s+1),100,-45,45);
				H_trig_phi_theta_S[s][it].setTitle(String.format("e sect %d, %.1f<#theta<%.1f",s+1,thetaMin,thetaMax));
				H_trig_phi_theta_S[s][it].setTitleX("#phi DCR1 (^o)");
			}
		}
		for(int s=0;s<6;s++){
			H_trig_S_HTCC_theta[s] = new H1F(String.format("H_trig_S_HTCC_theta_%d",s+1),String.format("H_trig_S_HTCC_theta_%d",s+1),100,0,30);
			H_trig_S_HTCC_theta[s].setTitle(String.format("HTCC #theta S%d",s+1));
			H_trig_S_HTCC_theta[s].setTitle("HTCC #theta (^o)");
			H_e_theta_mom_S[s] = new H2F(String.format("H_e_theta_mom_S%d",s+1),String.format("H_e_theta_mom_S%d",s+1),100,0.75,EB,100,0,40);
			H_e_theta_mom_S[s].setTitle(String.format("electron theta vs mom S%d",s+1));
			H_e_theta_mom_S[s].setTitleX("p (GeV/c)");
			H_e_theta_mom_S[s].setTitleY("#theta (^o)");
			H_e_W_S[s] = new H1F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,0.5,EB/2.5);
			if(EB==7.0f)H_e_W_S[s] = new H1F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,0.5,3.2);
			if(EB==6.0f)H_e_W_S[s] = new H1F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,0.5,3.5);
			if(EB==2.5f)H_e_W_S[s] = new H1F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,0.5,2);
			H_e_W_S[s].setTitle(String.format("e W S%d",s+1));
			H_e_W_S[s].setTitleX("W (GeV)");
			H_e_Q2_S[s] = new H1F(String.format("H_e_Q2_S%d",s+1),String.format("H_e_Q2_S%d",s+1),100,0.,EB*1.0f);
			H_e_Q2_S[s].setTitle(String.format("e Q^2 S%d",s+1));
            H_e_Q2_S[s].setTitleX("Q^22 (GeV^2)");
			H_e_W_phi_S[s] = new H2F(String.format("H_e_W_phi_S%d",s+1),String.format("H_e_W_phi_S%d",s+1),100,-45,45,100,0.5,EB/2.5);
			if(EB==7.0f)H_e_W_phi_S[s] = new H2F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,-45,45,100,0.5,3.2);
			if(EB==6.0f)H_e_W_phi_S[s] = new H2F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,-45,45,100,0.5,3.5);
			if(EB==2.5f)H_e_W_phi_S[s] = new H2F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,-45,45,100,0.5,2);
			H_e_W_phi_S[s].setTitle(String.format("e W vs #phi S%d",s+1));
			H_e_W_phi_S[s].setTitleX("#phi (^o)");
			H_e_W_phi_S[s].setTitleY("W (GeV)");
			H_trig_theta_mom_S[s] = new H2F(String.format("H_trig_theta_mom_S%d",s+1),String.format("H_trig_theta_mom_S%d",s+1),100,0,EB,100,0,45);
			H_trig_theta_mom_S[s].setTitle(String.format("e sect %d",s+1));
			H_trig_theta_mom_S[s].setTitleX("p (GeV)");
			H_trig_theta_mom_S[s].setTitleY("#theta (^o)");
			H_trig_phi_mom_S[s] = new H2F(String.format("H_trig_phi_mom_S%d",s+1),String.format("H_trig_phi_mom_S%d",s+1),100,0,EB,100,-45,45);
			H_trig_phi_mom_S[s].setTitle(String.format("e sect %d",s+1));
			H_trig_phi_mom_S[s].setTitleX("p (GeV)");
			H_trig_phi_mom_S[s].setTitleY("#phi (^o)");
			H_trig_theta_phi_S[s] = new H2F(String.format("H_trig_theta_phi_S%d",s+1),String.format("H_trig_theta_phi_S%d",s+1),100,-45,45,100,0,45);
			H_trig_theta_phi_S[s].setTitle(String.format("e sect %d",s+1));
			H_trig_theta_phi_S[s].setTitleX("#phi (^o)");
			H_trig_theta_phi_S[s].setTitleY("#theta (^o)");
			H_trig_vz_mom_S[s] = new H2F(String.format("H_trig_vz_mom_S%d",s+1),String.format("H_trig_vz_mom_S%d",s+1),100,0,EB,100,-25,50);
			H_trig_vz_mom_S[s].setTitle(String.format("e sect %d",s+1));
			H_trig_vz_mom_S[s].setTitleX("p (GeV)");
			H_trig_vz_mom_S[s].setTitleY("vz (cm)");
			H_trig_vy_vz_S[s] = new H2F(String.format("H_trig_vy_vz_S%d",s+1),String.format("H_trig_vy_vz_S%d",s+1),100,-25,50,100,-10,10);
			H_trig_vy_vz_S[s].setTitle(String.format("e sect %d",s+1));
			H_trig_vy_vz_S[s].setTitleX("vz (cm)");
			H_trig_vy_vz_S[s].setTitleY("vy (cm)");
			H_trig_vz_theta_S[s] = new H2F(String.format("H_trig_vz_theta_S%d",s+1),String.format("H_trig_vz_theta_S%d",s+1),100,0,45,100,-25,50);
			H_trig_vz_theta_S[s].setTitle(String.format("e sect %d",s+1));
			H_trig_vz_theta_S[s].setTitleX("#theta (^o)");
			H_trig_vz_theta_S[s].setTitleY("vz (cm)");
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

            H_trig_PCALECAL_S[s] = new H2F(String.format("H_trig_PCALECAL_S%d", s + 1), String.format("H_trig_PCALECAL_S%d", s + 1), 100, 0, 1.5, 100, 0, 1.5);
            H_trig_PCALECAL_S[s].setTitle(String.format("e sect %d", s + 1));
            H_trig_PCALECAL_S[s].setTitleX("E PCAL (GeV)");
            H_trig_PCALECAL_S[s].setTitleY("E ECAL (GeV)");
            H_trig_HTCCn_theta_S[s] = new H2F(String.format("H_trig_HTCCn_theta_S%d", s + 1), String.format("H_trig_HTCCn_theta_S%d", s + 1), 100, 0, 45, 100, 0, 100);
            H_trig_HTCCn_theta_S[s].setTitle(String.format("e sect %d", s + 1));
            H_trig_HTCCn_theta_S[s].setTitleX("#theta (^o)");
            H_trig_HTCCn_theta_S[s].setTitleY("HTCC nphe");
            H_trig_LTCCn_theta_S[s] = new H2F(String.format("H_trig_LTCCn_theta_S%d",s+1),String.format("H_trig_LTCCn_theta_S%d",s+1),100,0,45,100,0,100);
			H_trig_LTCCn_theta_S[s].setTitle(String.format(String.format("e sect %d",s+1)));
			H_trig_LTCCn_theta_S[s].setTitleX("#theta (^o)");
			H_trig_LTCCn_theta_S[s].setTitleY("LTCC nphe");
		}
        for (int s = 0; s < 7; s++) {
            H_trig_ECAL_pos_S[s] = new H2F(String.format("H_trig_ECAL_pos_S%d", s + 1), String.format("H_trig_ECAL_pos_S%d", s + 1), 100, 0, 400, 100, -200, 200);
            H_trig_ECAL_pos_S[s].setTitle(String.format("PCAL e sect %d", s + 1));
            H_trig_ECAL_pos_S[s].setTitleX("X (cm)");
            H_trig_ECAL_pos_S[s].setTitleY("Y (cm)");
            H_trig_TOF_pos_S[s] = new H2F(String.format("H_trig_TOF_pos_S%d", s + 1), String.format("H_trig_TOF_pos_S%d", s + 1), 100, 0, 400, 100, -200, 200);
            H_trig_TOF_pos_S[s].setTitle(String.format("FTOF e sect %d", s + 1));
            H_trig_TOF_pos_S[s].setTitleX("X (cm)");
            H_trig_TOF_pos_S[s].setTitleY("Y (cm)");
            H_trig_HTCC_pos_S[s] = new H2F(String.format("H_trig_HTCC_pos_S%d", s + 1), String.format("H_trig_HTCC_pos_S%d", s + 1), 30, 0, 120, 30, -60, 60);
            H_trig_HTCC_pos_S[s].setTitle(String.format("HTCC e sect %d", s + 1));
            H_trig_HTCC_pos_S[s].setTitleX("X (cm)");
            H_trig_HTCC_pos_S[s].setTitleY("Y (cm)");
            H_trig_DCR1_pos_S[s] = new H2F(String.format("H_trig_DCR1_pos_S%d", s + 1), String.format("H_trig_DCR1_pos_S%d", s + 1), 100, 0, 400, 100, -200, 200);
            H_trig_DCR1_pos_S[s].setTitle(String.format("DCR1 e sect %d", s + 1));
            H_trig_DCR1_pos_S[s].setTitleX("X (cm)");
            H_trig_DCR1_pos_S[s].setTitleY("Y (cm)");
            H_trig_DCR2_pos_S[s] = new H2F(String.format("H_trig_DCR2_pos_S%d", s + 1), String.format("H_trig_DCR2_pos_S%d", s + 1), 100, 0, 100, 100, -50, 50);
            H_trig_DCR2_pos_S[s].setTitle(String.format("DCR2 e sect %d", s + 1));
            H_trig_DCR2_pos_S[s].setTitleX("X (cm)");
            H_trig_DCR2_pos_S[s].setTitleY("Y (cm)");
            H_trig_DCR3_pos_S[s] = new H2F(String.format("H_trig_DCR3_pos_S%d", s + 1), String.format("H_trig_DCR3_pos_S%d", s + 1), 100, 0, 400, 100, -200, 200);
            H_trig_DCR3_pos_S[s].setTitle(String.format("DCR3 e sect %d", s + 1));
            H_trig_DCR3_pos_S[s].setTitleX("X (cm)");
            H_trig_DCR3_pos_S[s].setTitleY("Y (cm)");
        }
		H_e_theta_mom = new H2F("H_e_theta_mom","H_e_theta_mom",100,0.75,EB,100,0,40);
		H_e_theta_mom.setTitle("electron theta vs mom");
		H_e_theta_mom.setTitleX("p (GeV/c)");
		H_e_theta_mom.setTitleY("#theta (^o)");
		H_e_phi_mom = new H2F("H_e_phi_mom","H_e_phi_mom",100,0.75,EB,100,-180,180);
		H_e_phi_mom.setTitle("electron #phi vs mom");
		H_e_phi_mom.setTitleX("p (GeV/c)");
		H_e_phi_mom.setTitleY("#phi (^o)");
		H_positive_theta_mom = new H2F("H_positive_theta_mom", "H_positive_theta_mom", 100, 0.75, EB, 150, 0, 60);
        H_positive_theta_mom.setTitle("positive particles' theta vs mom");
        H_positive_theta_mom.setTitleX("p (GeV/c)");
        H_positive_theta_mom.setTitleY("#theta (^o)");
        H_negative_theta_mom = new H2F("H_negative_theta_mom", "H_negative_theta_mom", 100, 0.75, EB, 150, 0, 60);
        H_negative_theta_mom.setTitle("negative particles' theta vs mom");
        H_negative_theta_mom.setTitleX("p (GeV/c)");
        H_negative_theta_mom.setTitleY("#theta (^o)");
        H_electron_theta_mom = new H2F("H_electron_theta_mom", "H_electron_theta_mom", 100, 0.75, EB, 150, 0, 60);
        H_electron_theta_mom.setTitle("Electron particles' theta vs mom");
        H_electron_theta_mom.setTitleX("p (GeV/c)");
        H_electron_theta_mom.setTitleY("#theta (^o)");
		H_XY_ECal = new H2F("H_XY_ECal", "H_XY_ECal", 100, -400, 400, 100, -400, 400);
        H_XY_ECal.setTitle("Electron ECAL POS");
        H_XY_ECal.setTitleX("X (cm)");
        H_XY_ECal.setTitleY("Y (cm)");
        H_ESampl_ECal = new H2F("H_ESampl_ECal", "H_ESampl_ECal", 100, 0.75, EB, 100, 0, 0.5);
        H_ESampl_ECal.setTitle("Electron ECAL Sampling Fraction");
        H_ESampl_ECal.setTitleX("p (GeV/c)");
        H_ESampl_ECal.setTitleY("Edep/p");
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
		H_e_vz_phi = new H2F("H_e_vz_phi","H_e_vz_phi",100,-180,180,100,-25,50);
		H_e_vz_phi.setTitle("electron vz vs #phi");
		H_e_vz_phi.setTitleX("#phi");
		H_e_vz_phi.setTitleY("v_{z} (cm)");
		H_e_vz_p = new H2F("H_e_vz_p","H_e_vz_p",100,0,11,100,-25,50);
		H_e_vz_p.setTitle("electron vz vs #p");
		H_e_vz_p.setTitleX("p (GeV/c)");
		H_e_vz_p.setTitleY("v_{z} (cm)");
		H_e_vz_theta = new H2F("H_e_vz_theta","H_e_vz_theta",100,0,40,100,-25,50);
		H_e_vz_theta.setTitle("electron vz vs #theta");
		H_e_vz_theta.setTitleX("#theta");
		H_e_vz_theta.setTitleY("v_{z} (cm)");

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

	public int makePiPlus(DataBank bank){
		for(int k = 0; k < bank.rows(); k++){
			float px = bank.getFloat("p0_x" , k);
			float py = bank.getFloat("p0_y" , k);
			float pz = bank.getFloat("p0_z" , k);
			float vz = bank.getFloat("Vtx0_z", k);
			float mom = (float)Math.sqrt(px*px+py*py+pz*pz);
			float theta = (float)Math.toDegrees(Math.atan2(Math.sqrt(px*px+py*py), pz));
			float phi = (float)Math.toDegrees(Math.atan2(py, px));
			if(bank.getByte("q",k)>0){
				pip_mom = mom;pip_theta=theta;pip_phi=phi;pip_vz=vz;pip_vx=0;pip_vy=0;
				VPIP = new LorentzVector(px,py,pz,Math.sqrt(pip_mom*pip_mom+0.139*0.139));
				return k;
			}
		}
		return -1;
	}

	public void makeValidateRoads(DataBank bank){
        for (int k = 0; k < bank.rows(); k++) {
            float px = bank.getFloat("px", k);
            float py = bank.getFloat("py", k);
            float pz = bank.getFloat("pz", k);
            float mom = (float) Math.sqrt(px * px + py * py + pz * pz);
            float theta = (float) Math.toDegrees(Math.atan2(Math.sqrt(px * px + py * py), pz));
            int status = bank.getShort("status", k);
            boolean Forward = (status < 4000);
            if (Forward) {
                if (bank.getByte("charge", k) > 0) {
                    H_positive_theta_mom.fill(mom, theta);
                }
                else if (bank.getByte("charge", k) < 0) {
                    H_negative_theta_mom.fill(mom, theta);
                }
                if (bank.getInt("pid", k) == 11) {
                    H_electron_theta_mom.fill(mom, theta);
                }
            }
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
				int pid = bank.getInt("pid", k);
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
				float e_ecal_E=0;
				if(userTimeBased && event.hasBank("REC::Calorimeter")){
					DataBank ECALbank = event.getBank("REC::Calorimeter");
					for(int l = 0; l < ECALbank.rows(); l++) {
                        if(ECALbank.getShort("pindex",l)==k) {
                            if(ECALbank.getInt("layer",l)==1) {
                                trig_sect=ECALbank.getByte("sector",l);
                            }
                        }
						e_ecal_E += ECALbank.getFloat("energy",l);
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
						e_ecal_E += ECALbank.getFloat("energy",l);
					}
				}
				float HTCCnphe = 0;
				if(userTimeBased && event.hasBank("REC::Cherenkov")){
					DataBank HTCCbank = event.getBank("REC::Cherenkov");
					for(int l = 0; l < HTCCbank.rows(); l++){
						if(HTCCbank.getShort("pindex",l)==k && HTCCbank.getInt("detector",l)==15){
							HTCCnphe = HTCCbank.getFloat("nphe",l);
						}
					}
				}
				if(!userTimeBased && event.hasBank("RECHB::Cherenkov")){
					DataBank HTCCbank = event.getBank("RECHB::Cherenkov");
					for(int l = 0; l < HTCCbank.rows(); l++){
						if(HTCCbank.getShort("pindex",l)==k && HTCCbank.getInt("detector",l)==15){
							HTCCnphe = HTCCbank.getFloat("nphe",l);
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

	public void fillTraj(DataBank bank){
		for(int iI=0;iI<bank.rows();iI++){
			if(bank.getInt("detector",iI)==8 && bank.getShort("trkId",iI)==e_track_ind){
				found_e_FMM = 1;
				float px = bank.getFloat("px", iI);
				float py = bank.getFloat("py", iI);
				float pz = bank.getFloat("pz", iI);
				e_FMMmom[bank.getInt("layer",iI)-1] = (float)Math.sqrt(px*px+py*py+pz*pz);
				e_FMMtheta[bank.getInt("layer",iI)-1] = (float)Math.toDegrees(Math.acos(pz/e_FMMmom[bank.getInt("layer",iI)-1]));
				e_FMMphi[bank.getInt("layer",iI)-1] = (float)Math.toDegrees(Math.atan2(py,px));
				e_FMMvz[bank.getInt("layer",iI)-1] = bank.getFloat("z",iI);
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

    public void getElectronTriggerInfo(DataBank trackbank, DataBank htccbank, DataBank calbank, DataBank ftofbank, DataBank part) {
        for (int k = 0; k < trackbank.rows(); k++) {
            if (trackbank.getByte("q", k) == -1) {
            }
        }
    }

    public int isFTOFmatch(DataEvent event, int index) {
        int sectorftof = -1;
        if (userTimeBased && event.hasBank("REC::Scintillator")) {
            DataBank FTOFbank = event.getBank("REC::Scintillator");
            for (int l = 0; l < FTOFbank.rows(); l++) {
                if (FTOFbank.getShort("pindex", l) == index && FTOFbank.getInt("detector", l) == 12) {
                    if (FTOFbank.getInt("layer", l) == 2) {
                        sectorftof = FTOFbank.getByte("sector", l);
                    }
                }
            }
        }
        return sectorftof;
    }

    public int isECALmatch(DataEvent event, int index) {
        int sectorin = -1;
        int sectorout = -1;
        int retsector = -1;
        if (userTimeBased && event.hasBank("REC::Calorimeter")) {
            DataBank ECALbank = event.getBank("REC::Calorimeter");
            for (int l = 0; l < ECALbank.rows(); l++) {
                if (ECALbank.getShort("pindex", l) == index && ECALbank.getInt("detector", l) == 7) {
                    if (ECALbank.getInt("layer", l) == 4) {
                        sectorin = ECALbank.getByte("sector", l);
                    }
                    else if (ECALbank.getInt("layer", l) == 7) {
                        sectorout = ECALbank.getByte("sector", l);
                    }
                }
            }
        }
        if (sectorin > 0) {
            retsector = sectorin;
        }
        if (sectorout > 0) {
            retsector = sectorout;
        }
        return retsector;
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
            if (runNum <= 6296) {
                if (trigger_bits[tbit]) {
                    if ((sectorp[kk] + sectorn[kk]) >= 1 && (sectorp[kk + 3] + sectorn[kk + 3]) >= 1) {
                        H_trig_sector_muon.fill(kk + 1);
                        H_trig_sector_muon_rat.fill(kk + 1);
                        Nmuonpairs[kk]++;
                        Nmuons++;
                        Ntrackspair[kk] = sectorp[kk] + sectorn[kk + 3];
                    }
                }
            } else if (runNum > 6296 && runNum < 11000) {
                if (trigger_bits[tbit] || trigger_bits[tbit + 3]) {
                    if ((sectorp[kk] + sectorn[kk]) >= 1 && (sectorp[kk + 3] + sectorn[kk + 3]) >= 1) {
                        H_trig_sector_muon.fill(kk + 1);
                        H_trig_sector_muon_rat.fill(kk + 1);
                        Nmuonpairs[kk]++;
                        Nmuons++;
                        Ntrackspair[kk] = sectorp[kk] + sectorn[kk + 3];
                    }
                }
            } else if (runNum >= 11000) {
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
			if(sect>0 && testTriggerSector(sect) && NhitsSect[sect-1]==1){
				if(trig_HTCC_theta<10)trig_HTCC_ring=1;
				else if(trig_HTCC_theta>15 && trig_HTCC_theta<17)trig_HTCC_ring=2;
				else if(trig_HTCC_theta>20 && trig_HTCC_theta<25)trig_HTCC_ring=3;
				else if(trig_HTCC_theta>25 && trig_HTCC_theta<30)trig_HTCC_ring=4;
			}
			if(true || trig_HTCC_ring==4){
				if(sect==1 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S1_HTCC_n.fill(nphe);H_trig_S1_HTCC_N.fill(NPHE);H_trig_S1_HTCC_XY.fill(x,y);}
                else if(sect==2 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S2_HTCC_n.fill(nphe);H_trig_S2_HTCC_N.fill(NPHE);H_trig_S2_HTCC_XY.fill(x,y);}
                else if(sect==3 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S3_HTCC_n.fill(nphe);H_trig_S3_HTCC_N.fill(NPHE);H_trig_S3_HTCC_XY.fill(x,y);}
                else if(sect==4 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S4_HTCC_n.fill(nphe);H_trig_S4_HTCC_N.fill(NPHE);H_trig_S4_HTCC_XY.fill(x,y);}
                else if(sect==5 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S5_HTCC_n.fill(nphe);H_trig_S5_HTCC_N.fill(NPHE);H_trig_S5_HTCC_XY.fill(x,y);}
                else if(sect==6 && testTriggerSector(sect) && NhitsSect[sect-1]==1){H_trig_S6_HTCC_n.fill(nphe);H_trig_S6_HTCC_N.fill(NPHE);H_trig_S6_HTCC_XY.fill(x,y);}
				if(sect>0)H_trig_S_HTCC_theta[sect-1].fill(trig_HTCC_theta);
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

	public void fillECAL(DataBank bank){

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

	public int getNelecs(){return this.Nelecs;}
	public int getNtrigs(){return this.Ntrigs;}
	
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
		if (runNum <= 6296) {
            if(trigger_bits[7]||trigger_bits[8]||trigger_bits[9])Nmuontrigs++;
            if(trigger_bits[7])H_muon_trig_sector_count.fill(1);
			if(trigger_bits[8])H_muon_trig_sector_count.fill(2);
			if(trigger_bits[9])H_muon_trig_sector_count.fill(3);
		}
		else if (runNum > 6296) {
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

		trig_HTCC_ring=0;
		if(partBank!=null)trig_part_ind = makeTrigElectron(partBank,event);
		if(event.hasBank("HTCC::rec"))fillTrigHTCC(event.getBank("HTCC::rec"),event);
        if(event.hasBank("ECAL::clusters"))fillTrigECAL(event.getBank("ECAL::clusters"));
		if(trackBank!=null&&trackDetBank!=null)getTrigTBTrack(trackDetBank,trackBank);
		if(partBank!=null)makeTrigOthers(partBank,event);
		if(partBank!=null && scintillBank!=null) makeRFHistograms(partBank, scintillBank);
		if(partBank!=null)makeMuonPairTrigPurity(partBank,event);
		if(ecalBank!=null) fillECAL(ecalBank);
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
                    H_trig_vy_vz_S[e_sect - 1].fill(e_Ivz, e_Ivy);
                    H_trig_vz_theta_S[e_sect - 1].fill(e_theta, e_vz);
                    H_trig_ECALsampl_S[e_sect - 1].fill(e_mom, e_ecal_E / e_mom);
                    H_trig_PCAL_vt_S[e_sect - 1].fill(e_ecal_T_PCAL);
                    H_trig_ECIN_vt_S[e_sect - 1].fill(e_ecal_T_ECIN);
                    H_trig_ECOU_vt_S[e_sect - 1].fill(e_ecal_T_ECOU);
                    H_trig_PCALECAL_S[e_sect - 1].fill(e_pcal_e, e_etot_e);
                    H_trig_HTCCn_theta_S[e_sect - 1].fill(e_theta, e_HTCC);
                    if (hasLTCC == 1) {
                        H_trig_LTCCn_theta_S[e_sect - 1].fill(e_theta, e_LTCC);
                    }

                    Vector3 vECALpos = new Vector3(e_ecal_X, e_ecal_Y, e_ecal_Z);
                    vECALpos.rotateZ(-3.141597f * (e_sect - 1) / 3f);
                    H_trig_ECAL_pos_S[e_sect - 1].fill(vECALpos.x(), vECALpos.y());
                    H_trig_ECAL_pos_S[6].fill(vECALpos.x(), vECALpos.y(), 0.166f);

                    Vector3 vFTOFpos = new Vector3(e_TOF_X, e_TOF_Y, e_TOF_Z);
                    vFTOFpos.rotateZ(-3.141597f * (e_sect - 1) / 3f);
                    H_trig_TOF_pos_S[e_sect - 1].fill(vFTOFpos.x(), vFTOFpos.y());
                    H_trig_TOF_pos_S[6].fill(vFTOFpos.x(), vFTOFpos.y(), 0.166f);

                    Vector3 vHTCCpos = new Vector3(e_HTCC_X, e_HTCC_Y, e_HTCC_Z);
                    vHTCCpos.rotateZ(-3.141597f * (e_sect - 1) / 3f);
                    H_trig_HTCC_pos_S[e_sect - 1].fill(vHTCCpos.x(), vHTCCpos.y());
                    H_trig_HTCC_pos_S[6].fill(vHTCCpos.x(), vHTCCpos.y(), 0.166f);

                    Vector3 vDCR1pos = new Vector3(e_DCR1_X, e_DCR1_Y, e_DCR1_Z);
                    vDCR1pos.rotateZ(-3.141597f * (e_sect - 1) / 3f);
                    H_trig_DCR1_pos_S[e_sect - 1].fill(vDCR1pos.x(), vDCR1pos.y());
                    H_trig_DCR1_pos_S[6].fill(vDCR1pos.x(), vDCR1pos.y(), 0.166f);

                    Vector3 vDCR2pos = new Vector3(e_DCR2_X, e_DCR2_Y, e_DCR2_Z);
                    vDCR2pos.rotateZ(-3.141597f * (e_sect - 1) / 3f);
                    H_trig_DCR2_pos_S[e_sect - 1].fill(vDCR2pos.x(), vDCR2pos.y());
                    H_trig_DCR2_pos_S[6].fill(vDCR2pos.x(), vDCR2pos.y(), 0.166f);

                    Vector3 vDCR3pos = new Vector3(e_DCR3_X, e_DCR3_Y, e_DCR3_Z);
                    vDCR3pos.rotateZ(-3.141597f * (e_sect - 1) / 3f);
                    H_trig_DCR3_pos_S[e_sect - 1].fill(vDCR3pos.x(), vDCR3pos.y());
                    H_trig_DCR3_pos_S[6].fill(vDCR3pos.x(), vDCR3pos.y(), 0.166f);

                    int th_bin = (int) Math.floor((e_theta - 5.0f) / 2.0f);
                    if (th_bin > -1 && th_bin < 10) {
                        H_trig_phi_theta_S[e_sect - 1][th_bin].fill(Math.toDegrees(vDCR1pos.phi()));
                        H_trig_phi_theta_S[6][th_bin].fill(Math.toDegrees(vDCR1pos.phi()));
                    }
                }
            }
            H_e_phi_mom.fill(e_mom, e_phi);
            H_XY_ECal.fill(e_ecal_X, e_ecal_Y);
            H_ESampl_ECal.fill(e_mom, e_ecal_E / e_mom);
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
            H_e_vz_phi.fill(e_phi, e_vz);
            H_e_vz_p.fill(e_mom, e_vz);
            H_e_vz_theta.fill(e_theta, e_vz);
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
                    H_CVT_corr_e_theta.fill(CVT_theta, e_theta);
                    H_CVT_pathlength.fill(CVT_pathlength);
                    H_elast_e_p_th.fill(e_mom, e_theta);
                    H_elast_W_sect.fill(e_sect, e_W);
                    H_elast_W.fill(e_W);
                    float CVT_emom = Ebeam / (1 + 2 * Ebeam / 0.93827f * (float) Math.pow(Math.sin(CVT_eth / (2 * 57.296)), 2));
                    H_CVT_corr_e_mom.fill(CVT_emom, e_mom);
                }
            }
        }
	}

    public void plot() {

		EmbeddedCanvas can_Verification = new EmbeddedCanvas();
        can_Verification.setSize(2400, 1600);
        can_Verification.divide(3, 2);
        can_Verification.setAxisTitleSize(24);
        can_Verification.setAxisFontSize(24);
        can_Verification.setTitleSize(24);
        can_Verification.cd(0);
        can_Verification.draw(H_positive_theta_mom);
        can_Verification.getPad(0).getAxisZ().setLog(true);
        can_Verification.cd(1);
        can_Verification.draw(H_negative_theta_mom);
        can_Verification.getPad(1).getAxisZ().setLog(true);
        can_Verification.cd(2);
        can_Verification.draw(H_electron_theta_mom);
        can_Verification.getPad(2).getAxisZ().setLog(true);
        can_Verification.cd(3);
        can_Verification.draw(H_positive_theta_mom);
        can_Verification.cd(4);
        can_Verification.draw(H_negative_theta_mom);
        can_Verification.cd(5);
        can_Verification.draw(H_electron_theta_mom);
        can_Verification.save(String.format(outputDir + "/Verification.png"));
        System.out.println(String.format("save " + outputDir + "/Verification.png"));

        EmbeddedCanvas can_TOF = new EmbeddedCanvas();
        can_TOF.setSize(2400, 1600);
        can_TOF.divide(6, 2);
        can_TOF.setAxisTitleSize(24);
		can_TOF.setAxisFontSize(24);
		can_TOF.setTitleSize(24);
		can_TOF.cd(0);can_TOF.draw(H_TOF_vt_mom_S1m);
		can_TOF.cd(1);can_TOF.draw(H_TOF_vt_mom_S2m);
		can_TOF.cd(2);can_TOF.draw(H_TOF_vt_mom_S3m);
		can_TOF.cd(3);can_TOF.draw(H_TOF_vt_mom_S4m);
		can_TOF.cd(4);
        can_TOF.draw(H_TOF_vt_mom_S5m);
        can_TOF.cd(5);
        can_TOF.draw(H_TOF_vt_mom_S6m);
        can_TOF.cd(6);
        can_TOF.draw(H_TOF_vt_mom_S1p);
        can_TOF.cd(7);
        can_TOF.draw(H_TOF_vt_mom_S2p);
        can_TOF.cd(8);
        can_TOF.draw(H_TOF_vt_mom_S3p);
        can_TOF.cd(9);
        can_TOF.draw(H_TOF_vt_mom_S4p);
        can_TOF.cd(10);
        can_TOF.draw(H_TOF_vt_mom_S5p);
        can_TOF.cd(11);
        can_TOF.draw(H_TOF_vt_mom_S6p);
        can_TOF.save(String.format(outputDir + "/TOF.png"));
        System.out.println(String.format("save " + outputDir + "/TOF.png"));

        EmbeddedCanvas can_2pis = new EmbeddedCanvas();
        can_2pis.setSize(2800, 1400);
        can_2pis.divide(4, 2);
        can_2pis.setAxisTitleSize(24);
        can_2pis.setAxisFontSize(24);
        can_2pis.setTitleSize(24);
        can_2pis.cd(0);
        can_2pis.draw(H_rho_Q2_xB);
        can_2pis.cd(1);
        can_2pis.draw(H_rho_Q2_W);
        can_2pis.cd(2);
        can_2pis.draw(H_rho_pip_beta);
        can_2pis.cd(3);
        can_2pis.draw(H_rho_pim_beta);
        can_2pis.cd(4);
        can_2pis.draw(H_rho_prot);
        can_2pis.cd(5);
        can_2pis.draw(H_rho_IM);
        can_2pis.cd(6);
        can_2pis.draw(H_rho_MM);
        can_2pis.save(String.format(outputDir + "/two_pions.png"));
        System.out.println(String.format("save "+outputDir+"/two_pions.png"));

		EmbeddedCanvas can_miss_trig = new EmbeddedCanvas();
		can_miss_trig.setSize(2400,1200);
		can_miss_trig.divide(6,3);
		can_miss_trig.setAxisTitleSize(24);
		can_miss_trig.setAxisFontSize(24);
		can_miss_trig.setTitleSize(24);
		can_miss_trig.cd(0);can_miss_trig.draw(missTrig_S1_ft);
		can_miss_trig.cd(1);can_miss_trig.draw(missTrig_S2_ft);
		can_miss_trig.cd(2);can_miss_trig.draw(missTrig_S3_ft);
		can_miss_trig.cd(3);can_miss_trig.draw(missTrig_S4_ft);
		can_miss_trig.cd(4);can_miss_trig.draw(missTrig_S5_ft);
		can_miss_trig.cd(5);can_miss_trig.draw(missTrig_S6_ft);
		can_miss_trig.cd(6);can_miss_trig.draw(missTrig_S1_mt);
		can_miss_trig.cd(7);can_miss_trig.draw(missTrig_S2_mt);
		can_miss_trig.cd(8);can_miss_trig.draw(missTrig_S3_mt);
		can_miss_trig.cd(9);
        can_miss_trig.draw(missTrig_S4_mt);
        can_miss_trig.cd(10);
        can_miss_trig.draw(missTrig_S5_mt);
        can_miss_trig.cd(11);
        can_miss_trig.draw(missTrig_S6_mt);
        can_miss_trig.cd(12);
        can_miss_trig.draw(missTrig_S1_mf);
        can_miss_trig.cd(13);
        can_miss_trig.draw(missTrig_S2_mf);
        can_miss_trig.cd(14);
        can_miss_trig.draw(missTrig_S3_mf);
        can_miss_trig.cd(15);
        can_miss_trig.draw(missTrig_S4_mf);
        can_miss_trig.cd(16);
        can_miss_trig.draw(missTrig_S5_mf);
        can_miss_trig.cd(17);
        can_miss_trig.draw(missTrig_S6_mf);
        can_miss_trig.save(String.format(outputDir + "/miss_trig.png"));
        System.out.println(String.format("saved " + outputDir + "/miss_trig.png"));

        EmbeddedCanvas can_trig_sect = new EmbeddedCanvas();
        can_trig_sect.setSize(2800, 4400);
        can_trig_sect.divide(6, 9);
        can_trig_sect.setAxisTitleSize(24);
        can_trig_sect.setAxisFontSize(24);
        can_trig_sect.setTitleSize(24);
        can_trig_sect.cd(0);
        can_trig_sect.draw(H_trig_sector_count);
        can_trig_sect.cd(1);
        can_trig_sect.draw(H_trig_sector_elec);
        can_trig_sect.cd(2);
        can_trig_sect.draw(H_trig_sector_elec_rat);
        if (G_FCcur_evn.getDataSize(0) > 2) {
            can_trig_sect.cd(3);
        }
        can_trig_sect.draw(G_FCcur_evn);
        if (G_FC_live_ratio.getDataSize(0) > 2) {
            can_trig_sect.cd(4);
        }
        can_trig_sect.draw(G_FC_live_ratio);
        if (G_Clock_ratio.getDataSize(0) > 2) {
            can_trig_sect.cd(5);
        }
        can_trig_sect.draw(G_Clock_ratio);
        can_trig_sect.cd(6);
        can_trig_sect.draw(H_trig_S1_ETOT_E);
        can_trig_sect.cd(7);
        can_trig_sect.draw(H_trig_S2_ETOT_E);
        can_trig_sect.cd(8);
        can_trig_sect.draw(H_trig_S3_ETOT_E);
        can_trig_sect.cd(9);
        can_trig_sect.draw(H_trig_S4_ETOT_E);
        can_trig_sect.cd(10);
        can_trig_sect.draw(H_trig_S5_ETOT_E);
        can_trig_sect.cd(11);
        can_trig_sect.draw(H_trig_S6_ETOT_E);
        can_trig_sect.cd(12);
        can_trig_sect.draw(H_trig_S1_ECAL_E);
        can_trig_sect.cd(13);
        can_trig_sect.draw(H_trig_S2_ECAL_E);
        can_trig_sect.cd(14);
        can_trig_sect.draw(H_trig_S3_ECAL_E);
        can_trig_sect.cd(15);
        can_trig_sect.draw(H_trig_S4_ECAL_E);
        can_trig_sect.cd(16);
        can_trig_sect.draw(H_trig_S5_ECAL_E);
        can_trig_sect.cd(17);
        can_trig_sect.draw(H_trig_S6_ECAL_E);
        can_trig_sect.cd(18);
        can_trig_sect.draw(H_trig_S1_PCAL_E);
        can_trig_sect.cd(19);
        can_trig_sect.draw(H_trig_S2_PCAL_E);
        can_trig_sect.cd(20);
        can_trig_sect.draw(H_trig_S3_PCAL_E);
        can_trig_sect.cd(21);
        can_trig_sect.draw(H_trig_S4_PCAL_E);
        can_trig_sect.cd(22);
        can_trig_sect.draw(H_trig_S5_PCAL_E);
        can_trig_sect.cd(23);
        can_trig_sect.draw(H_trig_S6_PCAL_E);
        can_trig_sect.cd(24);
        can_trig_sect.draw(H_trig_S1_PCAL_XY);
        can_trig_sect.draw(H_trig_S2_PCAL_XY,"same");
		can_trig_sect.draw(H_trig_S3_PCAL_XY,"same");
		can_trig_sect.draw(H_trig_S4_PCAL_XY,"same");
		can_trig_sect.draw(H_trig_S5_PCAL_XY,"same");
		can_trig_sect.draw(H_trig_S6_PCAL_XY, "same");
        can_trig_sect.cd(25);
        can_trig_sect.draw(H_trig_S1_HTCC_XY);
        can_trig_sect.draw(H_trig_S2_HTCC_XY, "same");
        can_trig_sect.draw(H_trig_S3_HTCC_XY, "same");
        can_trig_sect.draw(H_trig_S4_HTCC_XY, "same");
        can_trig_sect.draw(H_trig_S5_HTCC_XY, "same");
        can_trig_sect.draw(H_trig_S6_HTCC_XY, "same");
        can_trig_sect.cd(30);
        can_trig_sect.draw(H_trig_S1_HTCC_n);
        can_trig_sect.cd(31);
        can_trig_sect.draw(H_trig_S2_HTCC_n);
        can_trig_sect.cd(32);
        can_trig_sect.draw(H_trig_S3_HTCC_n);
        can_trig_sect.cd(33);
        can_trig_sect.draw(H_trig_S4_HTCC_n);
        can_trig_sect.cd(34);
        can_trig_sect.draw(H_trig_S5_HTCC_n);
        can_trig_sect.cd(35);
        can_trig_sect.draw(H_trig_S6_HTCC_n);
        H_trig_S1_HTCC_N_track.setLineColor(2);
        H_trig_S2_HTCC_N_track.setLineColor(2);
        H_trig_S3_HTCC_N_track.setLineColor(2);
        H_trig_S4_HTCC_N_track.setLineColor(2);
        H_trig_S5_HTCC_N_track.setLineColor(2);
        H_trig_S6_HTCC_N_track.setLineColor(2);
        can_trig_sect.cd(36);
        can_trig_sect.draw(H_trig_sector_prot_rat);
        can_trig_sect.cd(37);
        can_trig_sect.draw(H_trig_sector_piplus_rat);
        can_trig_sect.cd(38);
        can_trig_sect.draw(H_trig_sector_piminus_rat);
        can_trig_sect.cd(39);
        can_trig_sect.draw(H_trig_sector_kplus_rat);
		can_trig_sect.cd(40);can_trig_sect.draw(H_trig_sector_kminus_rat);
		can_trig_sect.cd(41);
        can_trig_sect.draw(H_trig_sector_photon_rat);
        can_trig_sect.cd(42);
        can_trig_sect.draw(H_trig_sector_deut_rat);
        can_trig_sect.cd(43);
        can_trig_sect.draw(H_muon_trig_sector_count);
        can_trig_sect.cd(44);
        can_trig_sect.draw(H_trig_sector_muon_rat);
        can_trig_sect.cd(45);
        can_trig_sect.draw(H_trig_sector_positive_rat);
        can_trig_sect.cd(46);
        can_trig_sect.draw(H_trig_sector_negative_rat);
        can_trig_sect.cd(47);
        can_trig_sect.draw(H_trig_sector_neutral_rat);

        can_trig_sect.cd(48);
        can_trig_sect.draw(H_trig_central_prot_rat);
        can_trig_sect.cd(49);
        can_trig_sect.draw(H_trig_central_piplus_rat);
        can_trig_sect.cd(50);
        can_trig_sect.draw(H_trig_central_piminus_rat);
        can_trig_sect.cd(51);
        can_trig_sect.draw(H_trig_central_kplus_rat);
        can_trig_sect.cd(52);
        can_trig_sect.draw(H_trig_central_kminus_rat);
        can_trig_sect.cd(53);
        can_trig_sect.draw(H_trig_central_deut_rat);

        can_trig_sect.save(String.format(outputDir + "/trig_sect.png"));
        System.out.println(String.format("save " + outputDir + "/trig_sect.png"));

        EmbeddedCanvas can_twosecttrig = new EmbeddedCanvas();
        can_twosecttrig.setSize(3000, 2000);
        can_twosecttrig.divide(6, 5);
        can_twosecttrig.setAxisTitleSize(24);
        can_twosecttrig.setAxisFontSize(24);
        can_twosecttrig.setTitleSize(24);
        for (int s = 0; s < 6; s++) {
            can_twosecttrig.cd(s);
            can_twosecttrig.draw(H_muontrig_ecal_en_neg_S[s]);
            can_twosecttrig.cd(s + 6);
            can_twosecttrig.draw(H_muontrig_ecal_en_pos_S[s]);
            can_twosecttrig.cd(s + 12);
            can_twosecttrig.draw(H_muontrig_pcal_en_neg_S[s]);
            can_twosecttrig.cd(s + 18);
            can_twosecttrig.draw(H_muontrig_pcal_en_pos_S[s]);
            can_twosecttrig.cd(s + 24);
            can_twosecttrig.draw(H_muontrig_ECECOUT_en_S[s]);
        }

        can_twosecttrig.save(String.format(outputDir + "/twosect_trig.png"));
        System.out.println(String.format("save " + outputDir + "/twosect_trig.png"));

        EmbeddedCanvas can_e_pip = new EmbeddedCanvas();
        can_e_pip.setSize(3500, 3000);
        can_e_pip.divide(7, 6);
        can_e_pip.setAxisTitleSize(24);
        can_e_pip.setAxisFontSize(24);
        can_e_pip.setTitleSize(24);
        can_e_pip.cd(0);
        can_e_pip.draw(H_epip_e_theta_phi);
        can_e_pip.cd(1);
        can_e_pip.draw(H_epip_e_theta_mom);
        can_e_pip.cd(2);
        can_e_pip.draw(H_epip_e_phi_mom);
        can_e_pip.cd(3);
        can_e_pip.draw(H_epip_xB_Q2);
        can_e_pip.cd(4);
        can_e_pip.draw(H_epip_e_W_Q2);
        can_e_pip.cd(5);
        can_e_pip.draw(H_epip_e_t_phi);
        can_e_pip.cd(6);
        can_e_pip.draw(H_pip_Dphi);

		can_e_pip.cd(7);can_e_pip.draw(H_pip_theta_phi);
		can_e_pip.cd(8);can_e_pip.draw(H_pip_theta_mom);
		can_e_pip.cd(9);can_e_pip.draw(H_pip_phi_mom);
		can_e_pip.cd(10);can_e_pip.draw(H_pip_vz_phi);
		can_e_pip.cd(11);can_e_pip.draw(H_pip_vz_theta);
		can_e_pip.cd(12);can_e_pip.draw(H_pip_vz_mom);
		can_e_pip.cd(13);can_e_pip.draw(H_pip_vz_ve_diff_theta);

		can_e_pip.cd(14);can_e_pip.draw(H_pip_vtd_mom);
		can_e_pip.cd(15);can_e_pip.draw(H_pip_vtd_theta);
		can_e_pip.cd(16);can_e_pip.draw(H_pip_vtd_phi);
		can_e_pip.cd(17);can_e_pip.draw(H_pip_vz_ve);
		can_e_pip.cd(18);can_e_pip.draw(H_pip_vz_ve_diff);
		can_e_pip.cd(19);can_e_pip.draw(H_pip_vz_ve_diff_mom);
		can_e_pip.cd(20);can_e_pip.draw(H_pip_vz_ve_diff_phi);

		can_e_pip.cd(21);can_e_pip.draw(H_pip_beta_p);
		can_e_pip.cd(22);can_e_pip.draw(H_pip_beta2_p);
		can_e_pip.cd(23);can_e_pip.draw(H_MM_epip);
		can_e_pip.cd(24);can_e_pip.draw(H_MM_epip_zoom);
		can_e_pip.cd(25);can_e_pip.draw(H_MM_epip_phi);
		can_e_pip.cd(26);can_e_pip.draw(H_pip_e_vt);
		can_e_pip.cd(27);can_e_pip.draw(H_pip_vz_ve_diff_Dphi);

		for(int i=0;i<6;i++){
			can_e_pip.cd(28+i);can_e_pip.draw(H_MM_epip_Spip[i]);
		}
		can_e_pip.cd(34); can_e_pip.draw(H_pip_vtd);
		for(int i=0;i<6;i++){
			can_e_pip.cd(35+i);can_e_pip.draw(H_MM_epip_Se[i]);
		}
		can_e_pip.cd(41); can_e_pip.draw(H_pim_vtd);

        can_e_pip.save(String.format(outputDir+"/e_pip.png"));
        System.out.println(String.format("save "+outputDir+"/e_pip.png"));

		EmbeddedCanvas can_CVT_elastic = new EmbeddedCanvas();
		can_CVT_elastic.setSize(3500,1500);
		can_CVT_elastic.divide(7,3);
		can_CVT_elastic.setAxisTitleSize(24);
		can_CVT_elastic.setAxisFontSize(24);
		can_CVT_elastic.setTitleSize(24);
		can_CVT_elastic.cd(0);can_CVT_elastic.draw(H_CVT_p);
		can_CVT_elastic.cd(1);can_CVT_elastic.draw(H_CVT_t);
		can_CVT_elastic.cd(2);can_CVT_elastic.draw(H_CVT_f);
		can_CVT_elastic.cd(3);can_CVT_elastic.draw(H_CVT_z);
		can_CVT_elastic.cd(4);can_CVT_elastic.draw(H_CVT_e_vz_diff);
		can_CVT_elastic.cd(5);can_CVT_elastic.draw(H_CVT_chi2);
		can_CVT_elastic.cd(6);can_CVT_elastic.draw(H_elast_e_p_th);

		can_CVT_elastic.cd(7);can_CVT_elastic.draw(H_CVT_ft);
		can_CVT_elastic.cd(8);can_CVT_elastic.draw(H_CVT_pt);
		can_CVT_elastic.cd(9);can_CVT_elastic.draw(H_CVT_pf);
		can_CVT_elastic.cd(10);can_CVT_elastic.draw(H_CVT_zf);
		can_CVT_elastic.cd(11);can_CVT_elastic.draw(H_CVT_e_phi_diff);
		can_CVT_elastic.cd(12);can_CVT_elastic.draw(H_CVT_ndf);
		can_CVT_elastic.getPad(12).getAxisY().setLog(true);
		can_CVT_elastic.cd(13);can_CVT_elastic.draw(H_elast_W_sect);

		can_CVT_elastic.cd(14);can_CVT_elastic.draw(H_CVT_zp);
		can_CVT_elastic.cd(15);can_CVT_elastic.draw(H_CVT_zt);
		can_CVT_elastic.cd(16);can_CVT_elastic.draw(H_CVT_e_corr_vz);
		can_CVT_elastic.cd(17);can_CVT_elastic.draw(H_CVT_e_corr_phi);
		can_CVT_elastic.cd(18);can_CVT_elastic.draw(H_CVT_corr_e_theta);
		F1D elast_corr_Sang = new F1D("elast_corr_Sang",String.format("2*57.296*atan( 0.93827/(("+Ebeam+"+0.93827)*tan( (x+17)/57.296)) )"),50,65);
		elast_corr_Sang.setLineWidth(2);elast_corr_Sang.setLineColor(2);
		F1D elast_corr_ang = new F1D("elast_corr_ang","2*57.296*atan( 0.93827/(("+Ebeam+"+0.93827)*tan( x/57.296)) )",37,65);
		elast_corr_ang.setLineWidth(2);elast_corr_ang.setLineColor(2);
		can_CVT_elastic.draw(elast_corr_ang,"same");
		can_CVT_elastic.cd(19);can_CVT_elastic.draw(H_CVT_pathlength);
		can_CVT_elastic.cd(20);can_CVT_elastic.draw(H_CVT_corr_e_mom);
		can_CVT_elastic.cd(21);can_CVT_elastic.draw(H_elast_W);

        can_CVT_elastic.save(String.format(outputDir+"/cvt_elastic.png"));
        System.out.println(String.format("save "+outputDir+"/cvt_elastic.png"));

		EmbeddedCanvas can_CVT = new EmbeddedCanvas();
        can_CVT.setSize(3500, 3000);
        can_CVT.divide(6, 4);
        can_CVT.setAxisTitleSize(36);
        can_CVT.setAxisFontSize(36);
        can_CVT.setTitleSize(36);
        can_CVT.cd(0);
        can_CVT.draw(H_CVT_d0);
        can_CVT.cd(1);
        can_CVT.draw(hbstOccupancy);
        can_CVT.cd(2);
        can_CVT.draw(hbmtOccupancy);
        can_CVT.cd(3);
        can_CVT.draw(htrks);
        can_CVT.cd(4);
        can_CVT.draw(hpostrks);
        can_CVT.cd(5);
        can_CVT.draw(hnegtrks);
        can_CVT.cd(6);
        can_CVT.draw(hbstOnTrkLayers);
        can_CVT.cd(7);
        can_CVT.draw(hbmtOnTrkLayers);
        can_CVT.cd(8);
        can_CVT.draw(H_CVT_charge);
        can_CVT.cd(9);
        can_CVT.draw(H_CVT_vz_mom);
        can_CVT.cd(10);
        can_CVT.draw(H_CVT_vz_phi);
        can_CVT.cd(11);
        can_CVT.draw(H_CVT_vz_theta);
        can_CVT.cd(12);
        can_CVT.draw(H_CVT_vx);
        can_CVT.cd(13);
        can_CVT.draw(H_CVT_vy);
        can_CVT.cd(14);
        can_CVT.draw(H_CVT_vz);
        can_CVT.cd(15);
        can_CVT.draw(H_CVT_vx_vy);
        can_CVT.cd(16);
        can_CVT.draw(H_CVT_vx_vz);
        can_CVT.cd(17);
        can_CVT.draw(H_CVT_vz_vy);

        can_CVT.save(String.format(outputDir + "/cvt.png"));
        System.out.println(String.format("save " + outputDir + "/cvt.png"));

        EmbeddedCanvas can_gg = new EmbeddedCanvas();
        can_gg.setSize(1500, 1000);
        can_gg.divide(3, 2);
        can_gg.setAxisTitleSize(18);
		can_gg.setAxisFontSize(18);
		can_gg.setTitleSize(18);
		can_gg.cd(0);can_gg.draw(H_gg_open_a);
		F1D pi0openangle = new F1D("pi0openangle","2*57.296*asin(0.135/x)",1,6);
		pi0openangle.setLineWidth(1);pi0openangle.setLineColor(2);
		can_gg.draw(pi0openangle,"same");
		can_gg.cd(3);can_gg.draw(H_gg_m);
		can_gg.cd(1);can_gg.draw(H_g1_tf);
		can_gg.cd(2);can_gg.draw(H_g1_te);
		can_gg.cd(4);can_gg.draw(H_g2_tf);
		can_gg.cd(5);can_gg.draw(H_g2_te);
        can_gg.save(String.format(outputDir + "/gg.png"));
        System.out.println(String.format("save " + outputDir + "/gg.png"));

        EmbeddedCanvas can_e_ecal = new EmbeddedCanvas();
        can_e_ecal.setSize(5900, 4200);
        can_e_ecal.divide(7, 5);
        can_e_ecal.setAxisTitleSize(24);
        can_e_ecal.setAxisFontSize(24);
        can_e_ecal.setTitleSize(24);
        can_e_ecal.cd(0);
        can_e_ecal.draw(H_e_theta_phi);
        can_e_ecal.cd(1);
        can_e_ecal.draw(H_e_theta_mom);
        F1D elasticElec = new F1D("elasticElec", String.format("2*57.296*asin(sqrt( 0.938*(" + Ebeam + "-x)/(2*" + Ebeam + "*x) ))"), 0.5, Ebeam);
        elasticElec.setLineWidth(1);
        elasticElec.setLineColor(2);
        can_e_ecal.draw(elasticElec, "same");
        F1D Q2_1 = new F1D("Q2=0.5", String.format("acos((2*" + Ebeam + "*x-0.5)/(2*" + Ebeam + "*x))*180./3.141597"), 0.1, Ebeam);
        Q2_1.setLineWidth(3);
        Q2_1.setLineColor(2);
        can_e_ecal.draw(Q2_1, "same");
        F1D Q2_2 = new F1D("Q2=1.0", String.format("acos((2*" + Ebeam + "*x-1.)/(2*" + Ebeam + "*x))*180./3.141597"), 0.1, Ebeam);
        Q2_2.setLineWidth(3);
        Q2_2.setLineColor(2);
        can_e_ecal.draw(Q2_2, "same");
        F1D Q2_3 = new F1D("Q2=2.0", String.format("acos((2*" + Ebeam + "*x-2.)/(2*" + Ebeam + "*x))*180./3.141597"), 0.1, Ebeam);
        Q2_3.setLineWidth(3);
        Q2_3.setLineColor(2);
        can_e_ecal.draw(Q2_3, "same");
        can_e_ecal.getPad(1).getAxisY().setRange(0, 40);
        can_e_ecal.cd(2);
        can_e_ecal.draw(H_e_phi_mom);

        can_e_ecal.cd(3);
        can_e_ecal.draw(H_e_vz_phi);
        can_e_ecal.cd(4);
        can_e_ecal.draw(H_e_vz_theta);
        can_e_ecal.cd(5);
        can_e_ecal.draw(H_e_vz_p);

        can_e_ecal.cd(6);
        can_e_ecal.draw(H_XY_ECal);
        can_e_ecal.cd(11);
        can_e_ecal.draw(H_ESampl_ECal);
        can_e_ecal.getPad(11).getAxisY().setRange(0.15, 0.5);

        H_e_HTCC_nphe_txy.divide(H_e_HTCC_txy);
		can_e_ecal.getPad(34).getAxisZ().setRange(5,25);
		can_e_ecal.cd(34);can_e_ecal.draw(H_e_HTCC_nphe_txy);

		can_e_ecal.cd(7);can_e_ecal.draw(H_e_LTCC_xy);
		can_e_ecal.cd(12);can_e_ecal.draw(H_e_LTCC_nphe);

		can_e_ecal.cd(8);can_e_ecal.draw(H_e_TOF_xy);
		can_e_ecal.cd(13);can_e_ecal.draw(H_e_vt1);

		can_e_ecal.cd(10);can_e_ecal.draw(H_e_vz);
		can_e_ecal.cd(9);can_e_ecal.draw(H_e_TOF_t_path);
		can_e_ecal.cd(14);can_e_ecal.draw(H_o_TOF);
		can_e_ecal.cd(15);can_e_ecal.draw(H_o_vt);

		can_e_ecal.cd(16);can_e_ecal.draw(H_e_vz_S1);
		can_e_ecal.cd(17);can_e_ecal.draw(H_e_vz_S2);
		can_e_ecal.cd(18);can_e_ecal.draw(H_e_vz_S3);
		can_e_ecal.cd(19);can_e_ecal.draw(H_e_vz_S4);
		can_e_ecal.cd(20);
        can_e_ecal.draw(H_e_vz_S5);
        can_e_ecal.cd(21);
        can_e_ecal.draw(H_e_vz_S6);

        for (int s = 0; s < 6; s++) {
            can_e_ecal.cd(22 + s);
            can_e_ecal.draw(H_e_theta_mom_S[s]);
            can_e_ecal.draw(elasticElec, "same");
            can_e_ecal.getPad(22 + s).getAxisY().setRange(0, 40);
        }
        for (int s = 0; s < 6; s++) {
            can_e_ecal.cd(28 + s);
            can_e_ecal.draw(H_e_W_S[s]);
        }
        can_e_ecal.save(String.format(outputDir + "/e_rec_mon.png"));
        System.out.println(String.format("save " + outputDir + "/e_rec_mon.png"));

        EmbeddedCanvas can_RF = new EmbeddedCanvas();
        can_RF.setSize(3600, 2400);
        can_RF.divide(6, 5);
        can_RF.setAxisTitleSize(24);
        can_RF.setAxisFontSize(24);
        can_RF.setTitleSize(24);
        for (int s = 0; s < 6; s++) {
            can_RF.cd(s);
            can_RF.draw(H_e_RFtime1_FD_S[s]);
            can_RF.cd(6 + s);
            can_RF.draw(H_pip_RFtime1_FD_S[s]);
            can_RF.cd(12 + s);
            can_RF.draw(H_pim_RFtime1_FD_S[s]);
            can_RF.cd(18 + s);
            can_RF.draw(H_p_RFtime1_FD_S[s]);
        }
        can_RF.cd(25);
        can_RF.draw(H_pip_RFtime1_CD);
		can_RF.cd(26);can_RF.draw(H_pim_RFtime1_CD);
		can_RF.cd(27);can_RF.draw(H_p_RFtime1_CD);
        can_RF.cd(28);
        can_RF.draw(H_RFtimediff);
        can_RF.cd(29);
        can_RF.draw(H_RFtimediff_corrected);
        can_RF.save(String.format(outputDir + "/RF.png"));
        System.out.println(String.format("save " + outputDir + "/RF.png"));

        for (int iP = 0; iP < 4; iP++) {
            EmbeddedCanvas can_e_FMM = new EmbeddedCanvas();
            can_e_FMM.setSize(3600, 2400);
            can_e_FMM.divide(6, 4);
            can_e_FMM.setAxisTitleSize(24);
            can_e_FMM.setAxisFontSize(24);
            can_e_FMM.setTitleSize(24);
            for (int s = 0; s < 6; s++) {
                can_e_FMM.cd(s);
                can_e_FMM.draw(H_e_FMMmom_mom[s][iP]);
                can_e_FMM.cd(6 + s);
                can_e_FMM.draw(H_e_FMMtheta_theta[s][iP]);
                can_e_FMM.cd(12 + s);
                can_e_FMM.draw(H_e_FMMphi_phi[s][iP]);
                can_e_FMM.cd(18 + s);
                can_e_FMM.draw(H_e_FMMvz_vz[s][iP]);
            }
            can_e_FMM.save(String.format(outputDir + "/e_FMM_mon" + iP + ".png"));
            System.out.println(String.format("save " + outputDir + "/e_FMM_mon" + iP + ".png"));
        }

		EmbeddedCanvas can_e_phi_theta = new EmbeddedCanvas();
		can_e_phi_theta.setSize(3500,5000);
		can_e_phi_theta.divide(7,10);
		can_e_phi_theta.setAxisTitleSize(24);
		can_e_phi_theta.setAxisFontSize(24);
		can_e_phi_theta.setTitleSize(24);
		for(int s=0;s<7;s++)for(int it=0;it<10;it++){
			can_e_phi_theta.cd(s+7*it);can_e_phi_theta.draw(H_trig_phi_theta_S[s][it]);
		}
        can_e_phi_theta.save(String.format(outputDir+"/e_phi_sects.png"));
        System.out.println(String.format("save "+outputDir+"/e_phi_sects.png"));

		EmbeddedCanvas can_e_sect = new EmbeddedCanvas();
		can_e_sect.setSize(3000,5500);
		can_e_sect.divide(6,10);
		can_e_sect.setAxisTitleSize(24);
		can_e_sect.setAxisFontSize(24);
		can_e_sect.setTitleSize(24);
		for(int s=0;s<6;s++){
			can_e_sect.cd(s);can_e_sect.draw(H_trig_phi_mom_S[s]);
			can_e_sect.cd(6+s);can_e_sect.draw(H_trig_theta_phi_S[s]);
			can_e_sect.cd(12+s);can_e_sect.draw(H_trig_vz_mom_S[s]);
			can_e_sect.cd(18+s);can_e_sect.draw(H_trig_vy_vz_S[s]);
			can_e_sect.cd(24+s);can_e_sect.draw(H_trig_vz_theta_S[s]);
			can_e_sect.cd(30+s);can_e_sect.draw(H_trig_ECALsampl_S[s]);
            can_e_sect.cd(36+s);can_e_sect.draw(H_trig_PCALECAL_S[s]);
			can_e_sect.cd(42+s);can_e_sect.draw(H_trig_HTCCn_theta_S[s]);
			can_e_sect.cd(48);can_e_sect.draw(H_trig_LTCCn_theta_S[1]);
			can_e_sect.cd(49);can_e_sect.draw(H_trig_LTCCn_theta_S[2]);
			can_e_sect.cd(50);can_e_sect.draw(H_trig_LTCCn_theta_S[4]);
			can_e_sect.cd(51);can_e_sect.draw(H_trig_LTCCn_theta_S[5]);
			can_e_sect.cd(54+s);can_e_sect.draw(H_dce_chi2[s]);
		}
        can_e_sect.save(String.format(outputDir+"/e_sects.png"));
        System.out.println(String.format("save "+outputDir+"/e_sects.png"));

		EmbeddedCanvas can_e_sect_proj = new EmbeddedCanvas();
		can_e_sect_proj.setSize(3000,4500);
		can_e_sect_proj.divide(6,9);
		can_e_sect_proj.setAxisTitleSize(24);
		can_e_sect_proj.setAxisFontSize(24);
		can_e_sect_proj.setTitleSize(24);
		for(int s=0;s<6;s++){
			H1F Hp = H_trig_theta_mom_S[s].projectionX();
			Hp.setTitle(String.format("mom distribution S%d",s+1));
			Hp.setTitleX("p (GeV)");
			can_e_sect_proj.cd(s);can_e_sect_proj.draw(Hp);
			Hp = H_trig_theta_mom_S[s].projectionY();
			Hp.setTitle(String.format("#theta distribution S%d",s+1));
			Hp.setTitleX("#theta (^o)");
			can_e_sect_proj.cd(6+s);can_e_sect_proj.draw(Hp);
			Hp = H_trig_theta_phi_S[s].projectionX();
			Hp.setTitle(String.format("#phi distribution S%d",s+1));
			Hp.setTitleX("#phi (^o)");
			can_e_sect_proj.cd(12+s);can_e_sect_proj.draw(Hp);
			Hp = H_trig_vz_mom_S[s].projectionY();
			Hp.setTitle(String.format("vz distribution S%d",s+1));
			Hp.setTitleX("vz (cm)");
			can_e_sect_proj.cd(18+s);can_e_sect_proj.draw(Hp);
			Hp = H_trig_vy_vz_S[s].projectionX();
			Hp.setTitle(String.format("vz interpolated S%d",s+1));
			Hp.setTitleX("vz (cm)");
			can_e_sect_proj.cd(24+s);can_e_sect_proj.draw(Hp);
			Hp = H_trig_PCALECAL_S[s].projectionX();
			Hp.setTitle(String.format("PCAL distribution S%d",s+1));
			Hp.setTitleX("PCAL (GeV)");
			can_e_sect_proj.cd(30+s);can_e_sect_proj.draw(Hp);
			Hp = H_trig_PCALECAL_S[s].projectionY();
			Hp.setTitle(String.format("ECAL distribution S%d",s+1));
			Hp.setTitleX("ECAL (GeV)");
			can_e_sect_proj.cd(36+s);can_e_sect_proj.draw(Hp);
			Hp = H_trig_HTCCn_theta_S[s].projectionY();
			Hp.setTitle(String.format("HTCC distribution S%d",s+1));
			Hp.setTitleX("nphe");
			can_e_sect_proj.cd(42+s);can_e_sect_proj.draw(Hp);
			Hp = H_trig_LTCCn_theta_S[s].projectionY();
			Hp.setTitle(String.format("LTCC distribution S%d",s+1));
			Hp.setTitleX("nphe");
			can_e_sect_proj.cd(48+s);can_e_sect_proj.draw(Hp);
		}
        can_e_sect_proj.save(String.format(outputDir + "/e_sects_proj.png"));
        System.out.println(String.format("save " + outputDir + "/e_sects_proj.png"));

        EmbeddedCanvas can_e_pos_sect = new EmbeddedCanvas();
        can_e_pos_sect.setSize(3500, 3000);
        can_e_pos_sect.divide(7, 6);
        can_e_pos_sect.setAxisTitleSize(24);
        can_e_pos_sect.setAxisFontSize(24);
        can_e_pos_sect.setTitleSize(24);
        for (int s = 0; s < 7; s++) {
            can_e_pos_sect.cd(s);
            can_e_pos_sect.draw(H_trig_ECAL_pos_S[s]);
            can_e_pos_sect.cd(7 + s);
            can_e_pos_sect.draw(H_trig_TOF_pos_S[s]);
            can_e_pos_sect.cd(14 + s);
            can_e_pos_sect.draw(H_trig_HTCC_pos_S[s]);
            can_e_pos_sect.cd(21 + s);
            can_e_pos_sect.draw(H_trig_DCR1_pos_S[s]);
            can_e_pos_sect.cd(28 + s);
            can_e_pos_sect.draw(H_trig_DCR2_pos_S[s]);
            can_e_pos_sect.cd(35 + s);
            can_e_pos_sect.draw(H_trig_DCR3_pos_S[s]);
        }
        can_e_pos_sect.save(String.format(outputDir + "/e_pos_sects.png"));
        System.out.println(String.format("save " + outputDir + "/e_pos_sects.png"));

        EmbeddedCanvas can_e_posrat_sect = new EmbeddedCanvas();
        can_e_posrat_sect.setSize(3500, 3000);
        can_e_posrat_sect.divide(7, 6);
        can_e_posrat_sect.setAxisTitleSize(24);
        can_e_posrat_sect.setAxisFontSize(24);
        can_e_posrat_sect.setTitleSize(24);
        for (int s = 0; s < 6; s++) {
            H_trig_ECAL_pos_S[s].divide(H_trig_ECAL_pos_S[6]);
            H_trig_TOF_pos_S[s].divide(H_trig_TOF_pos_S[6]);
            H_trig_HTCC_pos_S[s].divide(H_trig_HTCC_pos_S[6]);
            H_trig_DCR1_pos_S[s].divide(H_trig_DCR1_pos_S[6]);
            H_trig_DCR2_pos_S[s].divide(H_trig_DCR2_pos_S[6]);
            H_trig_DCR3_pos_S[s].divide(H_trig_DCR3_pos_S[6]);
        }
        for (int s = 0; s < 7; s++) {
            can_e_posrat_sect.cd(s);
            can_e_posrat_sect.draw(H_trig_ECAL_pos_S[s]);
            if (s < 6) {
                can_e_posrat_sect.getPad(s).getAxisZ().setRange(0.5, 1.5);
            }
            can_e_posrat_sect.cd(7 + s);
            can_e_posrat_sect.draw(H_trig_TOF_pos_S[s]);
            if (s < 6) {
                can_e_posrat_sect.getPad(7 + s).getAxisZ().setRange(0.5, 1.5);
            }
            can_e_posrat_sect.cd(14 + s);
            can_e_posrat_sect.draw(H_trig_HTCC_pos_S[s]);
            if (s < 6) {
                can_e_posrat_sect.getPad(14 + s).getAxisZ().setRange(0.5, 1.5);
            }
            can_e_posrat_sect.cd(21 + s);
            can_e_posrat_sect.draw(H_trig_DCR1_pos_S[s]);
            if (s < 6) {
                can_e_posrat_sect.getPad(21 + s).getAxisZ().setRange(0.5, 1.5);
            }
            can_e_posrat_sect.cd(28 + s);
            can_e_posrat_sect.draw(H_trig_DCR2_pos_S[s]);
            if (s < 6) {
                can_e_posrat_sect.getPad(28 + s).getAxisZ().setRange(0.5, 1.5);
            }
            can_e_posrat_sect.cd(35 + s);
            can_e_posrat_sect.draw(H_trig_DCR3_pos_S[s]);
            if (s < 6) {
                can_e_posrat_sect.getPad(35 + s).getAxisZ().setRange(0.5, 1.5);
            }
        }
        can_e_posrat_sect.save(String.format(outputDir + "/e_ratio_sects.png"));
        System.out.println(String.format("save " + outputDir + "/e_ratio_sects.png"));

        EmbeddedCanvas can_e_phys = new EmbeddedCanvas();
        can_e_phys.setSize(6000, 1500);
        can_e_phys.divide(6, 3);
        can_e_phys.setAxisTitleSize(18);
        can_e_phys.setAxisFontSize(18);
        can_e_phys.setTitleSize(18);
        can_e_phys.cd(0);
        can_e_phys.draw(H_e_xB_Q2);
        can_e_phys.getPad(0).getAxisZ().setLog(true);
        can_e_phys.cd(1);
        can_e_phys.draw(H_e_W_Q2);
        can_e_phys.getPad(1).getAxisZ().setLog(true);
        can_e_phys.cd(2);
        can_e_phys.draw(H_e_xB_W);
        can_e_phys.getPad(2).getAxisZ().setLog(true);
        for (int s = 0; s < 6; s++) {
            can_e_phys.cd(s + 6);
            can_e_phys.draw(H_e_Q2_S[s]);
            can_e_phys.getPad(s + 6).getAxisY().setLog(true);
        }
        for (int s = 0; s < 6; s++) {
            can_e_phys.cd(s + 12);
            can_e_phys.draw(H_e_Q2_S[s]);
        }
        can_e_phys.save(String.format(outputDir + "/e_phys.png"));
        System.out.println(String.format("save " + outputDir + "/e_phys.png"));

        EmbeddedCanvas can_dc_mon = new EmbeddedCanvas();
		can_dc_mon.setSize(3600,3000);
		can_dc_mon.divide(6,3);
		can_dc_mon.setAxisTitleSize(24);
		can_dc_mon.setAxisFontSize(24);
		can_dc_mon.setTitleSize(24);
		can_dc_mon.cd(0);can_dc_mon.draw(H_dcm_theta_phi);
		can_dc_mon.cd(1);can_dc_mon.draw(H_dcm_theta_mom);
		F1D elasticline = new F1D("elasticline",String.format("2*57.296*asin(sqrt( 0.938*("+Ebeam+"-x)/(2*"+Ebeam+"*x) ))"),Ebeam*0.5,Ebeam);
		elasticline.setLineWidth(3);elasticline.setLineColor(2);
		can_dc_mon.draw(elasticline,"same");
		F1D elasticlineL = new F1D("elasticlineL",String.format("2*57.296*asin(sqrt( 0.938*("+(Ebeam*0.9)+"-x)/(2*"+(Ebeam*0.9)+"*x) ))"),Ebeam*0.5,(Ebeam*0.9));
		elasticlineL.setLineWidth(3);elasticlineL.setLineColor(2);
		can_dc_mon.draw(elasticline,"same");
		can_dc_mon.draw(elasticlineL,"same");
		can_dc_mon.getPad(1).getAxisY().setRange(0, 40);
		can_dc_mon.cd(2);can_dc_mon.draw(H_dcm_phi_mom);
		can_dc_mon.cd(3);can_dc_mon.draw(H_dcm_vz_phi);
		can_dc_mon.cd(4);can_dc_mon.draw(H_dcm_vz_p);
		can_dc_mon.cd(5);can_dc_mon.draw(H_dcm_vz_theta);

		can_dc_mon.cd(6);can_dc_mon.draw(H_dcp_theta_phi);
		can_dc_mon.cd(7);can_dc_mon.draw(H_dcp_theta_mom);
		can_dc_mon.cd(8);can_dc_mon.draw(H_dcp_phi_mom);
		can_dc_mon.cd(9);can_dc_mon.draw(H_dcp_vz_phi);
		can_dc_mon.cd(10);can_dc_mon.draw(H_dcp_vz_p);
		can_dc_mon.cd(11);can_dc_mon.draw(H_dcp_vz_theta);

		for(int i=0;i<H_negHBTrk_sect.getAxis().getNBins(); i++){
			double bincontent =  H_negHBTrk_sect.getBinContent(i);
			H_negHBTrk_sect.setBinContent(i,bincontent/Nelecs);
		}
		for(int i=0;i<H_posHBTrk_sect.getAxis().getNBins(); i++){
			double bincontent =  H_posHBTrk_sect.getBinContent(i);
			H_posHBTrk_sect.setBinContent(i,bincontent/Nelecs);
		}
		for(int i=0;i<H_negTBTrk_sect.getAxis().getNBins(); i++){
			double bincontent =  H_negTBTrk_sect.getBinContent(i);
			H_negTBTrk_sect.setBinContent(i,bincontent/Nelecs);
		}
		for(int i=0;i<H_posTBTrk_sect.getAxis().getNBins(); i++){
			double bincontent =  H_posTBTrk_sect.getBinContent(i);
			H_posTBTrk_sect.setBinContent(i,bincontent/Nelecs);
		}

		for(int i=0;i<H_negRECHB_sect.getAxis().getNBins(); i++){
			double bincontent = H_negRECHB_sect.getBinContent(i);
			H_negRECHB_sect.setBinContent(i,bincontent/Nelecs);
		}
		for(int i=0;i<H_posRECHB_sect.getAxis().getNBins(); i++){
			double bincontent = H_posRECHB_sect.getBinContent(i);
			H_posRECHB_sect.setBinContent(i,bincontent/Nelecs);
		}
		for(int i=0;i<H_negREC_sect.getAxis().getNBins(); i++){
			double bincontent = H_negREC_sect.getBinContent(i);
			H_negREC_sect.setBinContent(i,bincontent/Nelecs);
		}
		for(int i=0;i<H_posREC_sect.getAxis().getNBins(); i++){
			double bincontent = H_posREC_sect.getBinContent(i);
			H_posREC_sect.setBinContent(i,bincontent/Nelecs);
		}
		can_dc_mon.cd(12);
		can_dc_mon.draw(H_negTBTrk_sect);
		can_dc_mon.draw(H_negREC_sect,"same");
		can_dc_mon.cd(13);//can_dc_mon.draw(H_posHBTrk_sect);
		can_dc_mon.draw(H_posTBTrk_sect);
		can_dc_mon.draw(H_posREC_sect,"same");
        can_dc_mon.save(String.format(outputDir+"/dc_rec_mon.png"));
        System.out.println(String.format("save "+outputDir+"/dc_rec_mon.png"));

		EmbeddedCanvas can_dcm_vz_phi = new EmbeddedCanvas();
		can_dcm_vz_phi.setSize(4200,5400);
		can_dcm_vz_phi.divide(7,9);
		can_dcm_vz_phi.setAxisTitleSize(24);
		can_dcm_vz_phi.setAxisFontSize(24);
		can_dcm_vz_phi.setTitleSize(24);
		for(int s=0;s<7;s++){
			can_dcm_vz_phi.cd(0+s);can_dcm_vz_phi.draw(H_R1_dcm_XY[s]);
			can_dcm_vz_phi.cd(7+s);can_dcm_vz_phi.draw(H_R2_dcm_XY[s]);
			can_dcm_vz_phi.cd(14+s);can_dcm_vz_phi.draw(H_R3_dcm_XY[s]);
			can_dcm_vz_phi.cd(21+s);can_dcm_vz_phi.draw(H_R1_dcm_uXY[s]);
			can_dcm_vz_phi.cd(28+s);can_dcm_vz_phi.draw(H_R2_dcm_uXY[s]);
			can_dcm_vz_phi.cd(35+s);can_dcm_vz_phi.draw(H_R3_dcm_uXY[s]);
			can_dcm_vz_phi.cd(42+s);can_dcm_vz_phi.draw(H_dcm_vz[s]);
			can_dcm_vz_phi.cd(49+s);can_dcm_vz_phi.draw(H_dcm_chi2[s]);
			can_dcm_vz_phi.cd(56+s);can_dcm_vz_phi.draw(H_R1phiDm_mom[s]);
		}
        can_dcm_vz_phi.save(String.format(outputDir+"/dc_m_vz_phi.png"));
        System.out.println(String.format("save "+outputDir+"/dc_m_vz_phi.png"));

		EmbeddedCanvas can_dcp_vz_phi = new EmbeddedCanvas();
		can_dcp_vz_phi.setSize(4200,5400);
		can_dcp_vz_phi.divide(7,9);
		can_dcp_vz_phi.setAxisTitleSize(24);
		can_dcp_vz_phi.setAxisFontSize(24);
		can_dcp_vz_phi.setTitleSize(24);
		for(int s=0;s<7;s++){
			can_dcp_vz_phi.cd(0+s);can_dcp_vz_phi.draw(H_R1_dcp_XY[s]);
			can_dcp_vz_phi.cd(7+s);can_dcp_vz_phi.draw(H_R2_dcp_XY[s]);
			can_dcp_vz_phi.cd(14+s);can_dcp_vz_phi.draw(H_R3_dcp_XY[s]);
			can_dcp_vz_phi.cd(21+s);can_dcp_vz_phi.draw(H_R1_dcp_uXY[s]);
			can_dcp_vz_phi.cd(28+s);can_dcp_vz_phi.draw(H_R2_dcp_uXY[s]);
			can_dcp_vz_phi.cd(35+s);can_dcp_vz_phi.draw(H_R3_dcp_uXY[s]);
			can_dcp_vz_phi.cd(42+s);can_dcp_vz_phi.draw(H_dcp_vz[s]);
			can_dcp_vz_phi.cd(49+s);can_dcp_vz_phi.draw(H_dcp_chi2[s]);
			can_dcp_vz_phi.cd(56+s);can_dcp_vz_phi.draw(H_R1phiDp_mom[s]);
		}
        can_dcp_vz_phi.save(String.format(outputDir+"/dc_p_vz_phi.png"));
        System.out.println(String.format("save "+outputDir+"/dc_p_vz_phi.png"));
    }
        
    public void write() {

		TDirectory verify = new TDirectory();
        verify.mkdir("/roads");
        verify.cd("/roads");
        verify.addDataSet(H_positive_theta_mom, H_negative_theta_mom, H_electron_theta_mom);

        if (runNum > 0) {
            verify.writeFile(outputDir + "/verify_distributions_" + runNum + ".hipo");
        } else {
            verify.writeFile(outputDir + "/verify_distributions.hipo");
        }

        TDirectory dirout = new TDirectory();
        dirout.mkdir("/elec/");
        dirout.cd("/elec/");
        dirout.addDataSet(H_e_phi_mom, H_e_vz_phi, H_e_vz_theta, H_e_vz_p, H_XY_ECal, H_ESampl_ECal, H_e_HTCC_xy, H_e_HTCC_nphe, H_e_HTCC_nphe_txy, H_e_LTCC_xy, H_e_LTCC_nphe, H_e_TOF_xy);
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
			dirout.addDataSet(H_trig_vy_vz_S[s]);
			dirout.addDataSet(H_trig_vz_theta_S[s]);
			dirout.addDataSet(H_trig_ECALsampl_S[s]);
			dirout.addDataSet(H_trig_PCAL_vt_S[s]);
			dirout.addDataSet(H_trig_ECIN_vt_S[s]);
			dirout.addDataSet(H_trig_ECOU_vt_S[s]);
			dirout.addDataSet(H_trig_PCALECAL_S[s]);
			dirout.addDataSet(H_trig_HTCCn_theta_S[s]);
			dirout.addDataSet(H_trig_LTCCn_theta_S[s]);
		}
		for(int s=0;s<6;s++)for(int it=0;it<10;it++)dirout.addDataSet(H_trig_phi_theta_S[s][it]);
		G_FCcur_evn.setName("G_FCcur_evn");
		G_gatedFCcur_evn.setName("G_gatedFCcur_evn");
		G_FC_live_ratio.setName("G_FC_live_ratio");
		G_Clock_evn.setName("G_Clock_evn");
		G_gatedClock_evn.setName("G_gatedClock_evn");
		G_Clock_ratio.setName("G_Clock_ratio");
		dirout.addDataSet(G_FCcur_evn,G_gatedFCcur_evn,G_FC_live_ratio,G_Clock_evn,G_gatedClock_evn,G_Clock_ratio);
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

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GStyle.setPalette("kRainBow");
        int count = 0;
        int runNum = 0;
        boolean useTB = true;
        String filelist = "list_of_files.txt";
        if (args.length > 0) {
            runNum = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            filelist = args[1];
        }
        long maxevents = 50000000L;
        if (args.length > 2) {
            maxevents = Integer.parseInt(args[2]);
        }
        float Eb = 10.2f;
        if (args.length > 3) {
            Eb = Float.parseFloat(args[3]);
        }
        if (args.length > 4) {
            if (Integer.parseInt(args[4]) == 0) {
                useTB = false;
            }
        }
        String outputDir = runNum > 0 ? "plots" + runNum : "plots";
        monitor2p2GeV ana = new monitor2p2GeV(runNum, outputDir, Eb, useTB);
        List<String> toProcessFileNames = new ArrayList<>();
        File file = new File(filelist);
        Scanner read;
        try {
            read = new Scanner(file);
            do {
                String filename = read.next();
                toProcessFileNames.add(filename);

            } while (read.hasNext());
            read.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(100);
        }
        int progresscount = 0;
        int filetot = toProcessFileNames.size();
        for (String runstrg : toProcessFileNames)
            if (count < maxevents) {
                progresscount++;
                System.out.println(String.format(">>>>>>>>>>>>>>>> monitor2p2GeV %s", runstrg));
                File varTmpDir = new File(runstrg);
                if (!varTmpDir.exists()) {
                    System.out.println("FILE DOES NOT EXIST");
                    continue;
                }
                System.out.println("READING NOW " + runstrg);
                HipoDataSource reader = new HipoDataSource();
                reader.open(runstrg);
                int filecount = 0;
                while (reader.hasEvent() && count < maxevents) {
                    DataEvent event = reader.getNextEvent();
                    ana.processEvent(event);
                    filecount++;
                    count++;
                    if (count % 10000 == 0) {
                        int ntrigs = ana.getNtrigs();
                        int nelecs = ana.getNelecs();
                        float ratio = 0f;
                        if (ntrigs > 0) {
                            ratio = 100f * nelecs / ntrigs;
                        }
                        String diagnost = String.format("N elecs=%d N trigs=%d , ratio %1.2f%% ; progress : %d/%d", nelecs, ntrigs, ratio, progresscount, filetot);
                        System.out.println(count / 1000 + "k events, (monitor2p2GeV running on " + runstrg + ") " + diagnost);
                    }
                }
                reader.close();
            }
        System.out.println("Total : " + count + " events");
        ana.ratio_to_trigger();
        ana.write();
        ana.plot();
    }

}
