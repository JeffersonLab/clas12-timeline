package org.jlab.clas12.monitoring;
import java.io.*;
import java.util.*;

import org.jlab.groot.math.*;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.math.F1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.TDirectory;
import org.jlab.clas.physics.Vector3;
import org.jlab.clas.physics.LorentzVector;
import org.jlab.groot.base.GStyle;

public class deuterontarget {
	boolean userTimeBased;
	int Nevts, Nelecs, Ntrigs, runNum;
        public String outputDir;
	public int trig_sect, trig_track_ind,trig_part_ind;
	boolean[] trigger_bits;
	public float EB, Ebeam;
	public float RFtime1, RFtime2, startTime, BCG;public long TriggerWord;
	public int e_part_ind, e_sect, e_track_ind, hasLTCC, ngammas, pip_part_ind, pip_track_ind, pip_sect, pim_part_ind, pin_part_ind, pim_track_ind, pim_sect;
        public int pin_track_ind, pin_sect;
	public float e_mom, e_theta, e_phi, e_vx, e_vy, e_vz, e_Ivy, e_Ivz, e_ecal_X, e_ecal_Y, e_ecal_Z, e_ecal_E, e_track_chi2, e_vert_time, e_vert_time_RF, e_Q2, e_xB, e_W;
	public float e_HTCC, e_LTCC, e_pcal_e, e_etot_e, e_TOF_X, e_TOF_Y, e_TOF_Z, e_HTCC_X, e_HTCC_Y, e_HTCC_Z, e_HTCC_tX, e_HTCC_tY, e_HTCC_tZ, e_HTCC_nphe;
	public float e_DCR1_X, e_DCR1_Y, e_DCR1_Z, e_DCR2_X, e_DCR2_Y, e_DCR2_Z, e_DCR3_X, e_DCR3_Y, e_DCR3_Z;
	public float g1_e, g1_theta, g1_phi, g2_e, g2_theta, g2_phi;
	public float pip_mom, pip_theta, pip_phi, pip_vx, pip_vy, pip_vz, pip_vert_time, pip_beta, pip_track_chi2;
	public float pim_mom, pim_theta, pim_phi, pim_vx, pim_vy, pim_vz, pim_vert_time, pim_beta, pim_track_chi2;
	public float pin_mom, pin_theta, pin_phi, pin_vx, pin_vy, pin_vz, pin_vert_time, pin_beta, pin_track_chi2;
	public LorentzVector VB, VPT, VDT, VNT, Ve, VG1, VG2, VPI0, VPIP, VPIM, VP, VPIN;

	public H2F[] H_e_theta_mom_S;
	public H1F[] H_e_W_S;
	public H2F[] H_e_W_phi_S;
        public H2F H_e_theta_phi, H_e_theta_mom, H_e_phi_mom, H_XY_ECal, H_ESampl_ECal, H_e_LTCC_xy, H_e_HTCC_xy, H_e_HTCC_txy, H_e_HTCC_nphe_txy, H_e_vxy, H_e_vz_phi, H_e_vz_p, H_e_vz_theta, H_e_TOF_xy, H_e_TOF_t_path;
        public H2F H_e_xB_Q2, H_e_W_Q2, H_e_xB_W;
	public H1F H_e_W, H_e_Q2, H_e_xB, H_e_vz, H_e_LTCC_nphe, H_e_HTCC_nphe, H_e_vt1, H_e_vt2;

	public H2F H_gg_open_a, H_g1_tf, H_g2_tf, H_g1_te, H_g2_te;
	public H1F H_gg_m;


	public H1F[] H_MM_epip_Spip, H_MM_epip_Se;
	public H1F H_MM_epip, H_MM_epip_zoom, H_pip_vtd, H_pip_vz_ve_diff, H_pip_Dphi;
	public H2F H_pip_theta_phi, H_pip_theta_mom, H_pip_phi_mom, H_pip_vz_phi, H_pip_vz_theta, H_pip_vz_mom, H_pip_e_vt, H_pip_vz_ve;
	public H2F H_pip_vz_ve_diff_mom, H_pip_vz_ve_diff_theta, H_pip_vz_ve_diff_phi, H_pip_vz_ve_diff_Dphi;
	public H2F H_MM_epip_phi, H_pip_beta_p, H_pip_beta2_p, H_pip_vtd_mom, H_pip_vtd_theta, H_pip_vtd_phi;
	public H2F H_epip_e_theta_phi, H_epip_e_theta_mom, H_epip_e_phi_mom, H_epip_xB_Q2, H_epip_e_W_Q2, H_epip_e_t_phi;

	public H1F[] H_MM_epin_Spin, H_MM_epin_Se;
        public H1F H_MM_epin, H_MM_epin_zoom, H_pin_vtd, H_pin_vz_ve_diff, H_pin_Dphi;
        public H2F H_pin_theta_phi, H_pin_theta_mom, H_pin_phi_mom, H_pin_vz_phi, H_pin_vz_theta, H_pin_vz_mom, H_pin_e_vt, H_pin_vz_ve;
        public H2F H_pin_vz_ve_diff_mom, H_pin_vz_ve_diff_theta, H_pin_vz_ve_diff_phi, H_pin_vz_ve_diff_Dphi;
        public H2F H_MM_epin_phi, H_pin_beta_p, H_pin_beta2_p, H_pin_vtd_mom, H_pin_vtd_theta, H_pin_vtd_phi;
        public H2F H_epin_e_theta_phi, H_epin_e_theta_mom, H_epin_e_phi_mom, H_epin_xB_Q2, H_epin_e_W_Q2, H_epin_e_t_phi;

	public H1F H_rho_IM, H_rho_MM, H_rho_MMD;
	public H2F H_rho_Q2_xB, H_rho_Q2_W, H_rho_MMMM;
	public H2F H_rho_prot, H_rho_pip_beta, H_rho_pim_beta, H_rho_deut;

	public deuterontarget(int reqrunNum, String reqOutputDir, float reqEB, boolean reqTimeBased) {
		runNum = reqrunNum;EB=reqEB;userTimeBased=reqTimeBased;
                outputDir = reqOutputDir;
		Nevts=0;Nelecs=0;Ntrigs=0;
		trigger_bits = new boolean[32];
		Ebeam = 2.22f;
                if(reqEB>0 && reqEB<4)Ebeam=2.22f;
                if(reqEB>4 && reqEB<7.1)Ebeam=6.42f;
                if(reqEB>7.1 && reqEB<9)Ebeam=7.55f;
                if(reqEB>9)Ebeam=10.6f;
		String choiceTracking = " warning! Unspecified tracking";
		if(userTimeBased)choiceTracking=" using TIME BASED tracking";
		if(!userTimeBased)choiceTracking=" using HIT BASED tracking";
		System.out.println("Eb="+Ebeam+" (EB="+EB+") , run="+runNum+" , "+choiceTracking);
		float tofvt1 = 100, tofvt2 = 300;
		if(runNum>0 && runNum<3210){
			tofvt1 = 540;tofvt2=590;
			System.out.println("old trigger timing");
		}
		else System.out.println("new trigger timing");
		System.out.println("TOF range "+tofvt1+" to "+tofvt2);
		try {
			Thread.sleep(10000);// in ms
		}catch (Exception e) {
			System.out.println(e);
		}

		H_pin_vz_ve = new H2F("H_pin_vz_ve","H_pin_vz_ve",100,-5,15,100,-10,20);
		H_pin_vz_ve.setTitle("#pi^+ vz vs e vz");
		H_pin_vz_ve.setTitleX("e vz (cm)");
		H_pin_vz_ve.setTitleY("#pi^+ vz (cm)");
		H_pin_vz_ve_diff = new H1F("H_pin_vz_ve_diff","H_pin_vz_ve_diff",100,-10,20);
		H_pin_vz_ve_diff.setTitle("#pi^+ e vz diff");
		H_pin_vz_ve_diff.setTitleX("#Delta vz (cm)");
		H_pin_vz_ve_diff_mom = new H2F("H_pin_vz_ve_diff_mom","H_pin_vz_ve_diff_mom",100,0,6,100,-10,20);
		H_pin_vz_ve_diff_mom.setTitle("#pi^+ e vz diff vs mom");
		H_pin_vz_ve_diff_mom.setTitleX("#pi^+ mom (GeV)");
		H_pin_vz_ve_diff_mom.setTitleY("#Delta vz (cm)");
		H_pin_vz_ve_diff_theta = new H2F("H_pin_vz_ve_diff_theta","H_pin_vz_ve_diff_theta",100,0,40,100,-10,20);
		H_pin_vz_ve_diff_theta.setTitle("#pi^+ e vz diff vs #theta");
		H_pin_vz_ve_diff_theta.setTitleX("#theta (^o)");
		H_pin_vz_ve_diff_theta.setTitleY("#Delta vz (cm)");
		H_pin_vz_ve_diff_phi = new H2F("H_pin_vz_ve_diff_phi","H_pin_vz_ve_diff_phi",100,-180,180,100,-10,20);
		H_pin_vz_ve_diff_phi.setTitle("#pi^+ e vz diff vs #phi");
		H_pin_vz_ve_diff_phi.setTitleX("#phi (^o)");
		H_pin_vz_ve_diff_phi.setTitleY("#Delta vz (cm)");
		H_pin_vz_ve_diff_Dphi = new H2F("H_pin_vz_ve_diff_Dphi","H_pin_vz_ve_diff_Dphi",100,-180,180,100,-10,20);
		H_pin_vz_ve_diff_Dphi.setTitle("#pi^+ e vz diff vs #Delta#phi");
		H_pin_vz_ve_diff_Dphi.setTitleX("#Delta#phi (^o)");
		H_pin_vz_ve_diff_Dphi.setTitleY("#Delta vz (cm)");
		H_pin_Dphi = new H1F("H_pin_Dphi","H_pin_Dphi",100,-180,180);
		H_pin_Dphi.setTitle("#pi^+ e #Delta#phi");
		H_pin_Dphi.setTitleX("#Delta#phi (^o)");

		H_MM_epin_Spin = new H1F[6];
		H_MM_epin_Se = new H1F[6];
		for(int i=0;i<6;i++){
			H_MM_epin_Spin[i] = new H1F(String.format("H_MM_epin_Spin%d",i+1),String.format("H_MM_epin_Spin%d",i+1),100,0,4);
			H_MM_epin_Spin[i].setTitle(String.format("pi^- S%d MM #pi^- vs #phi",i+1));
			H_MM_epin_Spin[i].setTitleX("MM_{e#pi^-} (GeV)");
			H_MM_epin_Se[i] = new H1F(String.format("H_MM_epim_Se%d",i+1),String.format("H_MM_epim_Se%d",i+1),100,0,4);
			H_MM_epin_Se[i].setTitle(String.format("e S%d MM #pi^- vs #phi",i+1));
			H_MM_epin_Se[i].setTitleX("MM_{e#pi^-} (GeV)");
		}
		H_pin_vtd = new H1F("H_pim_vtd","H_pim_vtd",100,-5,5);
		H_pin_vtd.setTitle("Vertex time difference e #pi^-");
		H_pin_vtd.setTitle("#Delta t_{v} (ns)");
		H_MM_epin_phi = new H2F("H_MM_epim_phi","H_MM_epim_phi",100,-180,180,100,-1,5);
		H_MM_epin_phi.setTitle("Missing mass #pi^- vs #phi");
		H_MM_epin_phi.setTitleX("#phi (^o)");
		H_MM_epin_phi.setTitleY("MM_{e#pi^-} (GeV)");
		H_pin_beta_p = new H2F("H_pin_beta_p","H_pin_beta_p",100,0,EB,100,0.9,1.1);
		H_pin_beta_p.setTitle("#pi^- #beta vs momentum");
		H_pin_beta_p.setTitleX("p (GeV)");
		H_pin_beta_p.setTitleY("#beta");
		H_pin_beta2_p = new H2F("H_pin_beta2_p","H_pin_beta2_p",100,0,EB,100,0.9,1.1);
		H_pin_beta2_p.setTitle("#pi^- #beta vs momentum");
		H_pin_beta2_p.setTitleX("p (GeV)");
		H_pin_beta2_p.setTitleY("#beta");
		H_pin_vtd_mom = new H2F("H_pin_vtd_mom","H_pin_vtd_mom",100,0,EB,100,-2,2);
		H_pin_vtd_mom.setTitle("#pi^- time diff e #pi^- vs mom");
		H_pin_vtd_mom.setTitleX("p (GeV)");
		H_pin_vtd_mom.setTitleY("#Delta t_{v} (ns)");
		H_pin_vtd_theta = new H2F("H_pin_vtd_theta","H_pin_vtd_theta",100,0,40,100,-2,2);
		H_pin_vtd_theta.setTitle("#pi^- time diff e #pi^- vs #theta");
		H_pin_vtd_theta.setTitleX("#theta (^o)");
		H_pin_vtd_theta.setTitleY("#Delta t_{v} (ns)");
		H_pin_vtd_phi = new H2F("H_pin_vtd_phi","H_pin_vtd_phi",100,-180,180,100,-2,2);
		H_pin_vtd_phi.setTitle("#pi^- time diff e #pi^- vs #phi");
		H_pin_vtd_phi.setTitleX("#phi (^o)");
		H_pin_vtd_phi.setTitleY("#Delta t_{v} (ns)");

		H_pin_theta_phi = new H2F("H_pin_theta_phi","H_pin_theta_phi",100,-180,180,100,0,40);
		H_pin_theta_phi.setTitle("#pi^- #theta vs #phi");
		H_pin_theta_phi.setTitleX("#phi (^o)");
		H_pin_theta_phi.setTitleY("#theta (^o)");
		H_pin_theta_mom = new H2F("H_pin_theta_mom","H_pin_theta_mom",100,0,EB,100,0,40);
		H_pin_theta_mom.setTitle("#pi^- #theta vs mom");
		H_pin_theta_mom.setTitleX("p (GeV)");
		H_pin_theta_mom.setTitleY("#theta");
		H_pin_phi_mom = new H2F("H_pin_phi_mom","H_pin_phi_mom",100,0,EB,100,-180,180);
		H_pin_phi_mom.setTitle("#pi^- #phi vs mom");
		H_pin_phi_mom.setTitleX("p (GeV)");
		H_pin_phi_mom.setTitleY("#phi (^o)");
		H_pin_vz_phi = new H2F("H_pin_vz_phi","H_pin_vz_phi",100,-180,180,100,-20,20);
		H_pin_vz_phi.setTitle("#pi^- vz vs #phi");
		H_pin_vz_phi.setTitleX("#phi (^o)");
		H_pin_vz_phi.setTitleY("vz (cm)");
		H_pin_vz_theta = new H2F("H_pin_vz_theta","H_pin_vz_theta",100,0,40,100,-20,20);
		H_pin_vz_theta.setTitle("#pi^- vz vs #theta");
		H_pin_vz_theta.setTitleX("#theta (^o)");
		H_pin_vz_theta.setTitleY("vz (cm)");
		H_pin_vz_mom = new H2F("H_pin_vz_mom","H_pin_vz_mom",100,0,EB,100,-20,20);
		H_pin_vz_mom.setTitle("#pi^- vz vs mom");
		H_pin_vz_mom.setTitleX("p (GeV)");
		H_pin_vz_mom.setTitleY("vz (cm)");
		H_pin_e_vt = new H2F("H_pin_e_vt","H_pin_e_vt",100,525,600,100,525,600);
		H_pin_e_vt.setTitle("#pi^- vs e vertex times");
		H_pin_e_vt.setTitleX("e t_{v} (ns)");
		H_pin_e_vt.setTitleY("#pi^- t_{v} (ns)");
		H_epin_e_theta_phi = new H2F("H_epin_e_theta_phi","H_epin_e_theta_phi",100,-180,180,100,0,40);
		H_epin_e_theta_phi.setTitle("e #theta vs #phi");
		H_epin_e_theta_phi.setTitleX("#phi (^o)");
		H_epin_e_theta_phi.setTitleY("#theta (^o)");
		H_epin_e_theta_mom = new H2F("H_epin_e_theta_mom","H_epin_e_theta_mom",100,0,EB,100,0,40);
		H_epin_e_theta_mom.setTitle("e #theta vs mom");
		H_epin_e_theta_mom.setTitleX("p (GeV)");
		H_epin_e_theta_mom.setTitleY("#theta (^o)");
		H_epin_e_phi_mom = new H2F("H_epin_e_phi_mom","H_epin_e_phi_mom",100,0,EB,100,-180,180);
		H_epin_e_phi_mom.setTitle("e #phi vs mom");
		H_epin_e_phi_mom.setTitleX("p (GeV)");
		H_epin_e_phi_mom.setTitleY("#phi (^o)");
		H_epin_xB_Q2 = new H2F("H_epin_xB_Q2","H_epin_xB_Q2",100,0,1,100,0,EB);
		H_epin_xB_Q2.setTitle("Q^2 vs x_B");
		H_epin_xB_Q2.setTitleX("x_B");
		H_epin_xB_Q2.setTitleY("Q^2");
		H_epin_e_W_Q2 = new H2F("H_epin_e_W_Q2","H_epin_e_W_Q2",100,0,5,100,0,EB);
		H_epin_e_W_Q2.setTitle("Q^2 vs W");
		H_epin_e_W_Q2.setTitleX("W");
		H_epin_e_W_Q2.setTitleY("Q^2");
		H_epin_e_t_phi = new H2F("H_epin_e_t_phi","H_epin_e_t_phi",100,-180,180,100,0,5);
		H_epin_e_t_phi.setTitle("-t vs #phi");
		H_epin_e_t_phi.setTitleX("#phi (^o)");
		H_epin_e_t_phi.setTitleY("-t (GeV)");
		H_MM_epin_zoom = new H1F("H_MM_epin_phi_zoom","H_MM_epin_phi_zoom",100,0,2);
		H_MM_epin_zoom.setTitle("Missing mass e#pi^-");
		H_MM_epin_zoom.setTitleX("MM_{e#pi^-} (GeV)");
		H_MM_epin = new H1F("H_MM_epin","H_MM_epin",100,-1,5);
		H_MM_epin.setTitle("Missing Mass e#pi^-");
		H_MM_epin.setTitleX("MM_{e#pi^-} (GeV)");

		H_rho_prot = new H2F("H_rho_prot","H_rho_prot",100,0,2,100,0,4);
		H_rho_prot.setTitle("MM e#pi^+#pi^-(P Targ) vs IM #pi^+#pi^-");
		H_rho_prot.setTitleX("IM #pi^+#pi^-");
		H_rho_prot.setTitleY("MM e#pi^+#pi^-");
		H_rho_deut = new H2F("H_rho_deut","H_rho_deut",100,0,2,100,0,4);
                H_rho_deut.setTitle("MM e#pi^+#pi^-(D Targ) vs IM #pi^+#pi^-");
                H_rho_deut.setTitleX("IM #pi^+#pi^-");
                H_rho_deut.setTitleY("MM e#pi^+#pi^-");
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
                H_rho_MMD = new H1F("H_rho_MM","H_rho_MM",100,0,4);
                H_rho_MMD.setTitle("MM #pi^+#pi^-(D target)");
                H_rho_MMD.setTitleX("MM (GeV)");
                H_rho_MMMM = new H2F("H_rho_MM(D,P)","H_rho_MM(D,P)",100,0,4,150,0,6);
                H_rho_MMMM.setTitle("MM #pi^+#pi^-(D target) vs MM #pi^+#pi^-(P target)");
                H_rho_MMMM.setTitleX("MM prottarg (GeV)");
		H_rho_MMMM.setTitleY("MM deuttarg (GeV)");
	       	H_rho_Q2_xB = new H2F("H_rho_Q2_xB","H_rho_Q2_xB",100,0,1,100,0,EB);
		H_rho_Q2_xB.setTitle("Q^2 vs xB");
		H_rho_Q2_xB.setTitleX("Q^2 (GeV^2)");
		H_rho_Q2_xB.setTitleY("xB");
	       	H_rho_Q2_W = new H2F("H_rho_Q2_W","H_rho_Q2_W",100,0,5,100,0,EB);
		H_rho_Q2_W.setTitle("Q^2 vs W");
		H_rho_Q2_W.setTitleX("W (GeV)");
		H_rho_Q2_W.setTitleY("Q^2 (GeV^2)");

		H_e_vt1 = new H1F("H_e_vt1","H_e_vt1",100,-1,1);
                H_e_vt1.setTitle("electron vertex time");
                H_e_vt1.setTitleX("t (ns)");
                H_e_vt2 = new H1F("H_e_vt2","H_e_vt2",100,-1,1);
                H_e_vt2.setTitle("electron vertex time");
                H_e_vt2.setTitleX("t (ns)");


        	VB = new LorentzVector(0,0,Ebeam,Ebeam);
		VPT = new LorentzVector(0,0,0,0.93827);
		VDT = new LorentzVector(0,0,0,1.87705);
		VNT = new LorentzVector(0,0,0,0.9395654133);
		H_e_theta_phi = new H2F("H_e_theta_phi","H_e_theta_phi",100,-180,180,100,0,40);
		H_e_theta_phi.setTitle("electron theta vs phi");
		H_e_theta_phi.setTitleX("#phi (^o)");
		H_e_theta_phi.setTitleY("#theta (^o)");
		H_e_theta_mom_S = new H2F[6];
		H_e_W_S = new H1F[6];
                H_e_W_phi_S = new H2F[6];

		for(int s=0;s<6;s++){
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
			H_e_W_phi_S[s] = new H2F(String.format("H_e_W_phi_S%d",s+1),String.format("H_e_W_phi_S%d",s+1),100,-45,45,100,0.5,EB/2.5);
			if(EB==7.0f)H_e_W_phi_S[s] = new H2F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,-45,45,100,0.5,3.2);
			if(EB==6.0f)H_e_W_phi_S[s] = new H2F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,-45,45,100,0.5,3.5);
			if(EB==2.5f)H_e_W_phi_S[s] = new H2F(String.format("H_e_W_S%d",s+1),String.format("H_e_W_S%d",s+1),100,-45,45,100,0.5,2);
			H_e_W_phi_S[s].setTitle(String.format("e W vs #phi S%d",s+1));
			H_e_W_phi_S[s].setTitleX("#phi (^o)");
			H_e_W_phi_S[s].setTitleY("W (GeV)");
		}
		H_e_theta_mom = new H2F("H_e_theta_mom","H_e_theta_mom",100,0.75,EB,100,0,40);
		H_e_theta_mom.setTitle("electron theta vs mom");
		H_e_theta_mom.setTitleX("p (GeV/c)");
		H_e_theta_mom.setTitleY("#theta (^o)");
		H_e_phi_mom = new H2F("H_e_phi_mom","H_e_phi_mom",100,0.75,EB,100,-180,180);
		H_e_phi_mom.setTitle("electron #phi vs mom");
		H_e_phi_mom.setTitleX("p (GeV/c)");
		H_e_phi_mom.setTitleY("#phi (^o)");

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

	}

        /////////////////////////////////////////////////

        public static float Phi_Calculator(float[] elec_4v, float[] prot_4v, float beam_energy){
                float phi;//result for output
                float[] beam_4v = new  float[4];
                float[]  q_4v = new float[4];
                float[] lepto_3v = new float[3];
                float[] hadro_3v = new float[3];

                float proton_azimuth;
                float lept_norm, hadr_norm, scalar_product, cross_product;
                phi =0;
                beam_4v[0] = beam_energy; beam_4v[1] = 0; beam_4v[2] = 0; beam_4v[3] = beam_energy;

                proton_azimuth = 0;
                lept_norm = 0;
                hadr_norm = 0;
                scalar_product = 0;
                cross_product = 0;

                for(int i=0;i<4;i++)q_4v[i]=beam_4v[i]-elec_4v[i];

                for(int i=1;i<4;i++){
                        lepto_3v[i-1] = beam_4v[1+(i%3)]*elec_4v[1+((i+1)%3)] - elec_4v[1+(i%3)]*beam_4v[1+((1+i)%3)];
                        hadro_3v[i-1] = prot_4v[1+(i%3)]*q_4v[1+((i+1)%3)] - q_4v[1+(i%3)]*prot_4v[1+((1+i)%3)];
                        proton_azimuth += lepto_3v[i-1]*prot_4v[i];
                        scalar_product += lepto_3v[i-1]*hadro_3v[i-1];
                        lept_norm += lepto_3v[i-1]*lepto_3v[i-1];
                        hadr_norm += hadro_3v[i-1]*hadro_3v[i-1];
                }

                if(lept_norm>0&&hadr_norm>0){
                        if(scalar_product<Math.sqrt(lept_norm*hadr_norm))phi = (float)Math.toDegrees(Math.acos(scalar_product/ Math.sqrt(lept_norm*hadr_norm)));
                        if(proton_azimuth>0)phi = 360-phi;
                }

                if(phi>180)phi=phi-360;
                if(phi<-180)phi=phi+360;
                return phi;
        }
	/////////////////////////////////////////////////
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
				if( pid == 211 && pip_mom>0.5 && pip_theta<40 && pip_theta>6){ }

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
                                pin_mom = (float)Math.sqrt(px*px+py*py+pz*pz);
                                pin_theta = (float)Math.toDegrees(Math.acos(pz/pin_mom));
                                pin_phi = (float)Math.toDegrees(Math.atan2(py,px));
                                pin_vx = bank.getFloat("vx", k);
                                pin_vy = bank.getFloat("vy", k);
                                pin_vz = bank.getFloat("vz", k);
                                pin_beta = bank.getFloat("beta", k);

                                if( q<0 && pin_mom>0.5 && pin_theta<40 && pin_theta>5 && pin_beta>0 && pid!=11){
                                        VPIN = new LorentzVector(px,py,pz,Math.sqrt(pin_mom*pin_mom+0.139*0.139));
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
		float mybetan = 0;
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
			if(inDC && pid!=11&&nnegatives<2&&q<0&&thisbeta>0)mybetan=thisbeta;
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
					//System.out.println(pim_mom+" , "+pim_theta+" , "+pim_beta);
					if(pim_mom>0.5 && pim_theta<40 && pim_theta>8 && pim_beta>0){
						VPIM = new LorentzVector(px,py,pz,Math.sqrt(pim_mom*pim_mom+0.139*0.139));
						pim_part_ind = k;
					}
				}
			}
		}
		return -1;
	}

        public void getElecEBTOF(DataBank bank){
                for(int k = 0; k < bank.rows(); k++){
                        short pind = bank.getShort("pindex",k);
                        if(pind==e_part_ind && bank.getFloat("energy",k)>5){
                                e_vert_time = bank.getFloat("time",k) - bank.getFloat("path",k)/29.98f;
                                float time1 = (e_vert_time-RFtime1+1.002f)%2.004f;time1 -= 1.002f;
                                float time2 = (e_vert_time-RFtime2+1.002f)%2.004f;time2 -= 1.002f;
                                e_vert_time_RF = time1;
                                H_e_vt1.fill(e_vert_time_RF);
                                H_e_vt2.fill(time2);
                        }
                        if(pind==pin_part_ind){
                                float epin = (float)Math.sqrt( pin_mom*pin_mom + 0.139f*0.139f );
                                float pinDCbeta = pin_mom/epin;
                                pin_vert_time = bank.getFloat("time",k)-bank.getFloat("path",k)/ (29.98f * pinDCbeta) ;
                        }
                }
        }

	public void getTBTrack(DataBank bank){
                if(e_track_ind>-1 && e_track_ind<bank.rows()){
                         e_track_chi2 = bank.getFloat("chi2" , e_track_ind);
                         e_sect = bank.getInt("sector", e_track_ind);
                 }
                 if(pip_track_ind>-1 && pip_track_ind<bank.rows())pip_sect = bank.getInt("sector", pip_track_ind);
                 if(pim_track_ind>-1 && pim_track_ind<bank.rows())pim_sect = bank.getInt("sector", pim_track_ind);
		 if(pin_track_ind>-1 && pin_track_ind<bank.rows())pin_sect = bank.getInt("sector", pin_track_ind);
        }

        public void getTrigTBTrack(DataBank bank, DataBank recBank){
                 for(int k = 0; k < bank.rows(); k++){
                        if(recBank.getShort("pindex",k)==trig_part_ind && recBank.getByte("detector",k)==6)trig_track_ind = recBank.getShort("index",k);
                 }
                 if(trig_track_ind>-1 && trig_sect == bank.getInt("sector", trig_track_ind) ){
                         e_track_chi2 = bank.getFloat("chi2" , trig_track_ind);
                         e_sect = bank.getInt("sector", trig_track_ind);
                 }
        }


	public int makeElectron(DataBank bank){
		for(int k = 0; k < bank.rows(); k++){
			int pid = bank.getInt("pid", k);
			byte q = bank.getByte("charge", k);
			float px = bank.getFloat("px", k);
			float py = bank.getFloat("py", k);
			float pz = bank.getFloat("pz", k);
			int status = bank.getShort("status", k);
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
			byte q = bank.getByte("charge", k);
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
				e_ecal_E=0;
				if(userTimeBased && event.hasBank("REC::Calorimeter")){
					DataBank ECALbank = event.getBank("REC::Calorimeter");
					for(int l = 0; l < ECALbank.rows(); l++)if(ECALbank.getShort("pindex",l)==k){
						if(ECALbank.getInt("layer",l)==1)trig_sect=ECALbank.getByte("sector",l);
						e_ecal_E += ECALbank.getFloat("energy",l);
					}
				}
				if(!userTimeBased && event.hasBank("RECHB::Calorimeter")){
					DataBank ECALbank = event.getBank("RECHB::Calorimeter");
					for(int l = 0; l < ECALbank.rows(); l++)if(ECALbank.getShort("pindex",l)==k){
						if(ECALbank.getInt("layer",l)==1)trig_sect=ECALbank.getByte("sector",l);
						e_ecal_E += ECALbank.getFloat("energy",l);
					}
				}
				//int HTCCnphe = 0;
				float HTCCnphe = 0;
				if(userTimeBased && event.hasBank("REC::Cherenkov")){
					DataBank HTCCbank = event.getBank("REC::Cherenkov");
					for(int l = 0; l < HTCCbank.rows(); l++){
						if(HTCCbank.getShort("pindex",l)==k && HTCCbank.getInt("detector",l)==15){
							//HTCCnphe = HTCCbank.getInt("nphe",l);
							HTCCnphe = HTCCbank.getFloat("nphe",l);
						}
					}
				}
				if(!userTimeBased && event.hasBank("RECHB::Cherenkov")){
					DataBank HTCCbank = event.getBank("RECHB::Cherenkov");
					for(int l = 0; l < HTCCbank.rows(); l++){
						if(HTCCbank.getShort("pindex",l)==k && HTCCbank.getInt("detector",l)==15){
							//HTCCnphe = HTCCbank.getInt("nphe",l);
							HTCCnphe = HTCCbank.getFloat("nphe",l);
						}
					}
				}
				if( HTCCnphe>1 && e_ecal_E/e_mom > 0.18){}
				if( true || (HTCCnphe>1 && e_ecal_E/e_mom > 0.15) ){
					e_phi = (float)Math.toDegrees(Math.atan2(py,px));
					e_vx = bank.getFloat("vx", k);
					e_vy = bank.getFloat("vy", k);
					Ve = new LorentzVector(px,py,pz,e_mom);
					return k;
				}
			}
		}
		return -1;
	}

        public void processEvent(DataEvent event) {
		trig_part_ind=-1;e_part_ind=-1;
		Nevts++;
		e_sect=0;
		e_ecal_E = 0;e_pcal_e=0;e_etot_e=0;
		trig_track_ind = -1;e_track_ind = -1;pip_part_ind = -1;pim_part_ind = -1;
		if(event.hasBank("RUN::rf")){
			RFtime1=0;
			for(int r=0;r<event.getBank("RUN::rf").rows();r++){
				if(event.getBank("RUN::rf").getInt("id",r)==1)RFtime1=event.getBank("RUN::rf").getFloat("time",r);
			}
			RFtime2 = 0f;//bank.getFloat("time",1);
		}
		for(int i=1;i<7;i++)trigger_bits[i]=false;
		if(event.hasBank("RUN::config")){
			DataBank bank = event.getBank("RUN::config");
			TriggerWord = bank.getLong("trigger",0);
			//startTime   = bank.getFloat("startTime",0);
			String TriggerString = Long.toBinaryString(TriggerWord);
			int length=0;
			for (int i = 31; i >= 0; i--) {trigger_bits[i] = (TriggerWord & (1 << i)) != 0;if(length==0 && trigger_bits[i])length=i+1;}

			int[] compare_bits = new int[32];
			for (int i = 0; i < 32; i++) {compare_bits[31-i] = trigger_bits[i] ? 1 : 0 ;}
			if(false){
				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				System.out.print("TriggerWord = " + TriggerWord + " = " + TriggerString + " = ");
				for(int i=32-length;i<32;i++)System.out.print(compare_bits[i]);
				System.out.print("\n");
				System.out.print("TriggerWord = " + TriggerWord + " = ");
				for(int i=32-length;i<32;i++)System.out.print(compare_bits[i]);
				System.out.print("\n");
				System.out.print("Length : " + length + " Trigger bits ID : ");
				for(int i=0;i<32;i++)if(trigger_bits[i])System.out.print(i + " ");
				System.out.print("\n");
			}
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

		if(partBank!=null)trig_part_ind = makeTrigElectron(partBank,event);
		if(trackBank!=null&&trackDetBank!=null)getTrigTBTrack(trackDetBank,trackBank);

		if(partBank!=null){
			e_part_ind = makeElectron(partBank);
			pip_part_ind = makePiPlusPID(partBank);
			pin_part_ind = makePiMinusPID(partBank);
			makePiPlusPimPID(partBank);
		}
		if(e_part_ind==-1)return;

		Nelecs++;
                LorentzVector VGS = new LorentzVector(0,0,0,0);
                VGS.add(VB);
                VGS.sub(Ve);
                e_Q2 = (float) -VGS.mass2();
                e_xB = e_Q2/(2f*0.93827f*(Ebeam-e_mom));
                e_W  = (float) Math.sqrt(0.93827f*0.93827f + e_Q2*(1f/e_xB-1f) );

		if(scintillBank!=null){
                        getElecEBTOF(scintillBank);
                }

		if(trackDetBank!=null){
                        getTBTrack(trackDetBank);
                }

		if(e_mom>Ebeam*0.025 && e_ecal_E/e_mom > 0.15 && e_Q2>1.2 *0.1 * Ebeam/7 && trig_track_ind>-1 && e_sect==trig_sect){
			H_e_theta_phi.fill(e_phi,e_theta);
			H_e_theta_mom.fill(e_mom,e_theta);
			H_e_phi_mom.fill(e_mom,e_phi);
			H_e_vz_phi.fill(e_phi,e_vz);
			H_e_vz_p.fill(e_mom,e_vz);
			H_e_vz_theta.fill(e_theta,e_vz);
			H_e_vxy.fill(e_vx,e_vy);
			H_e_xB_Q2.fill(e_xB,e_Q2);
			H_e_W_Q2.fill(e_W,e_Q2);
			H_e_xB_W.fill(e_xB,e_W);
                        H_e_Q2.fill(e_Q2);
			H_e_xB.fill(e_xB);
			H_e_W.fill(e_W);
			//pip_part_ind==-1 suggested as condition
			if(pin_part_ind>-1 && Math.abs(pin_vert_time-e_vert_time)<5 && Math.abs(pin_beta-1) <0.1 && pin_track_chi2<500 && e_track_chi2<500){
                                H_pin_beta_p.fill(pin_mom,pin_beta);
                        }


			if( pin_part_ind>-1 && Math.abs(pin_vert_time-e_vert_time)<5 && Math.abs(pin_beta-1) <(0.01 + 0.025/pin_mom)
					&& pin_track_chi2<2000 && e_track_chi2<2000 && pin_mom>1
			  ){
				LorentzVector VNeutr = new LorentzVector(0,0,0,0);
				VNeutr.add(VB);
				VNeutr.add(VNT);
				VNeutr.sub(Ve);
				VNeutr.sub(VPIN);
				H_MM_epin.fill(VNeutr.mass());
				H_MM_epin_zoom.fill(VNeutr.mass());
				if(pin_sect>0&&pin_sect<7)H_MM_epin_Spin[pin_sect-1].fill(VNeutr.mass());
				if(e_sect>0&&e_sect<7)H_MM_epin_Se[e_sect-1].fill(VNeutr.mass());
				H_pin_theta_phi.fill(pin_phi,pin_theta);
				H_pin_theta_mom.fill(pin_mom,pin_theta);
				H_pin_phi_mom.fill(pin_mom,pin_phi);
				H_pin_vz_phi.fill(pin_phi,pin_vz);
				H_pin_vz_theta.fill(pin_theta,pin_vz);
				H_pin_vz_mom.fill(pin_mom,pin_vz);
				H_pin_e_vt.fill(e_vert_time,pin_vert_time);
				H_MM_epin_phi.fill(pin_phi,VNeutr.mass());
				H_pin_beta2_p.fill(pin_mom,pin_beta);
				H_pin_vtd.fill(pin_vert_time-e_vert_time);
				H_pin_vtd_mom.fill(pin_mom,pin_vert_time-e_vert_time);
				H_pin_vtd_theta.fill(pin_theta,pin_vert_time-e_vert_time);
				H_pin_vtd_phi.fill(pin_phi,pin_vert_time-e_vert_time);
				H_pin_vz_ve.fill(e_vz,pin_vz);
				H_pin_vz_ve_diff.fill(e_vz-pin_vz);
				H_pin_vz_ve_diff_mom.fill(pin_mom,e_vz-pin_vz);
				H_pin_vz_ve_diff_theta.fill(pin_theta,e_vz-pin_vz);
				H_pin_vz_ve_diff_phi.fill(pin_phi,e_vz-pin_vz);
				float DelPhi = pin_phi-e_phi-180;
				while(DelPhi>180)DelPhi-=360;
				while(DelPhi<-180)DelPhi+=360;
				H_pin_Dphi.fill(DelPhi);
				H_pin_vz_ve_diff_Dphi.fill(DelPhi,e_vz-pin_vz);
				H_epin_e_theta_phi.fill(e_phi,e_theta);
				H_epin_e_theta_mom.fill(e_mom,e_theta);
				H_epin_e_phi_mom.fill(e_mom,e_phi);
				H_epin_xB_Q2.fill(e_xB,e_Q2);
				H_epin_e_W_Q2.fill(e_W,e_Q2);
				float[] elec_4v = {(float)Ve.e(),(float)Ve.px(),(float)Ve.py(),(float)Ve.pz()};
				float[] neut_4v = {(float)VNeutr.e(),(float)VNeutr.px(),(float)VNeutr.py(),(float)VNeutr.pz()};
				float epin_phi = Phi_Calculator(elec_4v,neut_4v, 10.6f);
				VNeutr.sub(VNT);
				float epin_t = (float) -VNeutr.mass2();
				H_epin_e_t_phi.fill(epin_phi,epin_t);
			}
			if(pim_part_ind>-1 && pip_part_ind>-1 && pim_track_chi2<750 && pip_track_chi2<750 && e_track_chi2<750){
				LorentzVector VRHO = new LorentzVector(0,0,0,0);
				VRHO.add(VPIP);
				VRHO.add(VPIM);
				LorentzVector VPROT = new LorentzVector(0,0,0,0);
				LorentzVector VDEUT = new LorentzVector(0,0,0,0);
				VPROT.add(VB);
				VPROT.add(VPT);
				VPROT.sub(Ve);
				VPROT.sub(VRHO);
				VDEUT.add(VB);
                                VDEUT.add(VDT);
                                VDEUT.sub(Ve);
                                VDEUT.sub(VRHO);
				if(pip_beta>0.95 && pim_beta>0.9){
					H_rho_prot.fill(VRHO.mass(),VPROT.mass());
					H_rho_deut.fill(VRHO.mass(),VDEUT.mass());
					H_rho_pip_beta.fill(pip_mom,pip_beta);
					H_rho_pim_beta.fill(pim_mom,pim_beta);
					H_rho_IM.fill(VRHO.mass());
					H_rho_MM.fill(VPROT.mass());
					H_rho_MMD.fill(VDEUT.mass());
					H_rho_MMMM.fill(VPROT.mass(),VDEUT.mass());
					H_rho_Q2_xB.fill(e_xB,e_Q2);
					H_rho_Q2_W.fill(e_W,e_Q2);
				}
			}
		}
	}


        public void plot() {


		EmbeddedCanvas can_2pis = new EmbeddedCanvas();
		can_2pis.setSize(2800,1400);
		can_2pis.divide(4,3);
		can_2pis.setAxisTitleSize(24);
		can_2pis.setAxisFontSize(24);
		can_2pis.setTitleSize(24);
		can_2pis.cd(0);can_2pis.draw(H_rho_Q2_xB);
		can_2pis.cd(1);can_2pis.draw(H_rho_Q2_W);
		can_2pis.cd(2);can_2pis.draw(H_rho_pip_beta);
		can_2pis.cd(3);can_2pis.draw(H_rho_pim_beta);
		can_2pis.cd(4);can_2pis.draw(H_rho_prot);
		can_2pis.cd(5);can_2pis.draw(H_rho_IM);
		can_2pis.cd(6);can_2pis.draw(H_rho_MM);
		can_2pis.cd(7);can_2pis.draw(H_rho_MMD);
		can_2pis.cd(8);can_2pis.draw(H_rho_deut);
		can_2pis.cd(9);can_2pis.draw(H_rho_MMMM);
                can_2pis.save(String.format(outputDir+"/two_pions.png"));
                System.out.println(String.format("save "+outputDir+"/two_pions.png"));

		EmbeddedCanvas can_e_pin = new EmbeddedCanvas();
		can_e_pin.setSize(3500,3000);
		can_e_pin.divide(7,6);
		can_e_pin.setAxisTitleSize(24);
		can_e_pin.setAxisFontSize(24);
		can_e_pin.setTitleSize(24);
		can_e_pin.cd(0);can_e_pin.draw(H_epin_e_theta_phi);
		can_e_pin.cd(1);can_e_pin.draw(H_epin_e_theta_mom);
		can_e_pin.cd(2);can_e_pin.draw(H_epin_e_phi_mom);
		can_e_pin.cd(3);can_e_pin.draw(H_epin_xB_Q2);
		can_e_pin.cd(4);can_e_pin.draw(H_epin_e_W_Q2);
		can_e_pin.cd(5);can_e_pin.draw(H_epin_e_t_phi);
		can_e_pin.cd(6);can_e_pin.draw(H_pin_Dphi);

		can_e_pin.cd(7);can_e_pin.draw(H_pin_theta_phi);
		can_e_pin.cd(8);can_e_pin.draw(H_pin_theta_mom);
		can_e_pin.cd(9);can_e_pin.draw(H_pin_phi_mom);
		can_e_pin.cd(10);can_e_pin.draw(H_pin_vz_phi);
		can_e_pin.cd(11);can_e_pin.draw(H_pin_vz_theta);
		can_e_pin.cd(12);can_e_pin.draw(H_pin_vz_mom);
		can_e_pin.cd(13);can_e_pin.draw(H_pin_vz_ve_diff_theta);

		can_e_pin.cd(14);can_e_pin.draw(H_pin_vtd_mom);
		can_e_pin.cd(15);can_e_pin.draw(H_pin_vtd_theta);
		can_e_pin.cd(16);can_e_pin.draw(H_pin_vtd_phi);
		can_e_pin.cd(17);can_e_pin.draw(H_pin_vz_ve);
		can_e_pin.cd(18);can_e_pin.draw(H_pin_vz_ve_diff);
		can_e_pin.cd(19);can_e_pin.draw(H_pin_vz_ve_diff_mom);
		can_e_pin.cd(20);can_e_pin.draw(H_pin_vz_ve_diff_phi);

		can_e_pin.cd(21);can_e_pin.draw(H_pin_beta_p);
		can_e_pin.cd(22);can_e_pin.draw(H_pin_beta2_p);
		can_e_pin.cd(23);can_e_pin.draw(H_MM_epin);
		can_e_pin.cd(24);can_e_pin.draw(H_MM_epin_zoom);
		can_e_pin.cd(25);can_e_pin.draw(H_MM_epin_phi);
		can_e_pin.cd(26);can_e_pin.draw(H_pin_e_vt);
		can_e_pin.cd(27);can_e_pin.draw(H_pin_vz_ve_diff_Dphi);

		for(int i=0;i<6;i++){
			can_e_pin.cd(28+i);can_e_pin.draw(H_MM_epin_Spin[i]);
		}
		for(int i=0;i<6;i++){
			can_e_pin.cd(35+i);can_e_pin.draw(H_MM_epin_Se[i]);
		}

                can_e_pin.save(String.format(outputDir+"/e_pin.png"));
                System.out.println(String.format("save "+outputDir+"/e_pin.png"));
	}

        public void write() {
                TDirectory dirout = new TDirectory();
		dirout.mkdir("/elec/");
		dirout.cd("/elec/");
		dirout.addDataSet(H_e_phi_mom,H_e_vz_phi,H_e_vz_theta,H_e_vz_p);
		dirout.addDataSet(H_e_xB,H_e_xB_Q2,H_e_W_Q2);
		for(int s=0;s<6;s++){
			dirout.addDataSet(H_e_theta_mom_S[s],H_e_W_S[s],H_e_W_phi_S[s]);
		}

                if(runNum>0) dirout.writeFile(outputDir+"/out_deuterontarget_"+runNum+".hipo");
                else         dirout.writeFile(outputDir+"/out_deuterontarget.hipo");

        }
////////////////////////////////////////////////
        public static void main(String[] args) {
                System.setProperty("java.awt.headless", "true");
		GStyle.setPalette("kRainBow");
                int count = 0;
		int runNum = 0;
		boolean useTB = true;
		String filelist = "list_of_files.txt";
		if(args.length>0)runNum=Integer.parseInt(args[0]);
		if(args.length>1)filelist = args[1];
                long maxevents = 50000000L;
                if(args.length>2)maxevents=Integer.parseInt(args[2]);
                float Eb = 10.6f;
                if(args.length>3)Eb=Float.parseFloat(args[3]);
		if(args.length>4)if(Integer.parseInt(args[4])==0)useTB=false;
                String outputDir = runNum > 0 ? "plots"+runNum : "plots";
		deuterontarget ana = new deuterontarget(runNum,outputDir,Eb,useTB);
		List<String> toProcessFileNames = new ArrayList<String>();
		File file = new File(filelist);
		Scanner read;
		try {
			read = new Scanner(file);
			do {
				String filename = read.next();
				toProcessFileNames.add(filename);

			}while (read.hasNext());
			read.close();
		}catch(IOException e){
			e.printStackTrace();
                        System.exit(100);
		}
		int progresscount=0;int filetot = toProcessFileNames.size();
		for (String runstrg : toProcessFileNames) if(count<maxevents){
			progresscount++;
			System.out.println(String.format(">>>>>>>>>>>>>>>> deuterontarget %s",runstrg));
			File varTmpDir = new File(runstrg);
			if(!varTmpDir.exists()){System.out.println("FILE DOES NOT EXIST");continue;}
			System.out.println("READING NOW "+runstrg);
			HipoDataSource reader = new HipoDataSource();
			reader.open(runstrg);
			int filecount = 0;
			while(reader.hasEvent() && count<maxevents) {
				DataEvent event = reader.getNextEvent();
				ana.processEvent(event);
				filecount++;count++;
				if(count%10000 == 0){
					System.out.println(count/1000 + "k events, (deuterontarget running on "+runstrg+") ");
				}
			}
			reader.close();
		}
		System.out.println("Total : " + count + " events");
		//ana.write();
		ana.plot();
        }
}
