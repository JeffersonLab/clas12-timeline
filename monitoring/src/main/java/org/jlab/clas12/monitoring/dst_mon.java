package org.jlab.clas12.monitoring;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

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
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.ConstantsManager;

public class dst_mon {
	public int Nevts, Nelecs, Ntrigs, runNum;
        public String outputDir;
	boolean[] trigger_bits;
	public int[] Ntrigs_sect, Nelecs_sect;
	public float EB, Eb, Mp;
	public float RFT, STT;
	public long TriggerWord;
	public float rfPeriod;
	public int rf_large_integer;

	public LorentzVector VB, VT, Ve, VGS, Vprot, Vpip, VG1, VG2, VPI0;
	public boolean found_eTraj, found_eECAL, found_eFTOF1a, found_eFTOF1b, found_eLTCC, found_eHTCC;
	public int e_part_ind, e_sect, e_FTOF_pad1a, e_FTOF_pad1b, e_HTCC_bin_phi, e_HTCC_bin_theta;
	public float e_mom, e_the, e_phi, e_vx, e_vy, e_vz;
	public float e_xB, e_Q2, e_W;
	public float e_HTCC_tX, e_HTCC_tY, e_HTCC_tZ, e_LTCC_tX, e_LTCC_tY, e_FTOF_tX, e_FTOF_tY, e_PCAL_tX, e_PCAL_tY;
	public float e_DCSL1_tX, e_DCSL1_tY, e_DCSL2_tX, e_DCSL2_tY, e_DCSL3_tX, e_DCSL3_tY, e_DCSL4_tX, e_DCSL4_tY, e_DCSL5_tX, e_DCSL5_tY, e_DCSL6_tX, e_DCSL6_tY;
	public float e_PCAL_X, e_PCAL_Y, e_PCAL_Z, e_PCAL_edep, e_PCAL_t, e_EC_ein, e_EC_eout, e_EC_etot, e_PCAL_path, e_PCAL_vt;
	public float e_FTOF1a_X, e_FTOF1a_Y, e_FTOF1a_Z, e_FTOF1a_edep, e_FTOF1a_t, e_FTOF1a_path, e_FTOF1a_vt;
	public float e_FTOF1b_X, e_FTOF1b_Y, e_FTOF1b_Z, e_FTOF1b_edep, e_FTOF1b_t, e_FTOF1b_path, e_FTOF1b_vt;
	public float e_LTCC_X, e_LTCC_Y, e_LTCC_Z, e_LTCC_t, e_LTCC_nphe, e_LTCC_path, e_LTCC_vt;
	public float e_HTCC_X, e_HTCC_Y, e_HTCC_Z, e_HTCC_theta, e_HTCC_phi, e_HTCC_t, e_HTCC_nphe, e_HTCC_path, e_HTCC_vt;

	public int prot_part_ind;
	public float prot_mom, prot_the, prot_phi, prot_vx, prot_vy, prot_vz, prot_beta;

	public int pim_part_ind, pip_part_ind, pip_FTOF_pad1b;
	public float pip_mom, pip_the, pip_phi, pip_vx, pip_vy, pip_vz, pip_beta, pip_FTOF1b_t, pip_FTOF1b_path, pip_FTOF1b_vt;
	public float pip_FTOF1a_t, pip_FTOF1a_path, pip_FTOF1a_vt;
	public float pim_mom, pim_FTOF1a_t, pim_FTOF1a_path, pim_FTOF1a_vt;
	public float pim_FTOF1b_t, pim_FTOF1b_path, pim_FTOF1b_vt;
	public float thisTime;

	public int G1_part_ind, G2_part_ind, G1_pcal_ind, G2_pcal_ind, G1_cal_layers, G2_cal_layers;
	public float G1_mom, G1_the, G1_phi, G2_mom, G2_the, G2_phi;
	public float G1_pcal_X, G1_pcal_Y, G1_pcal_Z, G1_pcal_t, G1_pcal_R, G1_pcal_vt, G2_pcal_X, G2_pcal_Y, G2_pcal_Z, G2_pcal_t, G2_pcal_R, G2_pcal_vt;
	public float G1_ein_t, G1_eout_t, G2_ein_t, G2_eout_t;

	public float elast_dPhi, elast_EB;
	public float epip_dPhi, epip_MM;
	public float pi0_mass, pi0_E, pi0_the, pi0_phi, pi0_open;

	public H2F H_e_t_f, H_e_p_f, H_e_vz_f, H_e_vt_vz, H_e_vt_p, H_e_vt_t;
	public H2F H_e_PCAL, H_e_FTOF, H_e_LTCC, H_e_DCSL6, H_e_DCSL5, H_e_DCSL4, H_e_DCSL3, H_e_DCSL2, H_e_DCSL1, H_e_HTCC;
    	public H2F H_e_nphe_HTCC, H_e_bin_theta_HTCC, H_e_bin_phi_HTCC, H_e_theta_HTCC, H_e_phi_HTCC;
	public H2F[] H_e_HTCC_cut; 
	public H2F[] H_e_t_p, H_e_vz_t, H_e_vz_p;
	public H2F[] H_e_EC_etot_p, H_e_EC_vt_theta, H_e_EC_XY;
	public H2F[] H_e_FTOF_vt_pad1a, H_e_FTOF_edep_pad1a, H_e_FTOF_XY_pad1a;
	public H2F[] H_e_FTOF_vt_pad1b, H_e_FTOF_edep_pad1b, H_e_FTOF_XY_pad1b;
	public H2F[] H_FTOF_pos_beta_mom_pad1a, H_FTOF_neg_beta_mom_pad1a, H_FTOF_pos_beta_mom_pad1b, H_FTOF_neg_beta_mom_pad1b;
	public H2F[] H_FTOF_pos_mass_mom_pad1a, H_FTOF_pos_mass_the_pad1a, H_FTOF_neg_mass_mom_pad1a, H_FTOF_neg_mass_the_pad1a;
	public H2F[] H_FTOF_pos_mass_mom_pad1b, H_FTOF_pos_mass_the_pad1b, H_FTOF_neg_mass_mom_pad1b, H_FTOF_neg_mass_the_pad1b;
	//from tof_monitor.java for timing and gain calibration, but from Dan's comment
	//use leptons/pions (both charges) for p1a, p1b and all particles (both charges) for p2.
	public H2F[] p1a_pad_vt_elec, p1a_pad_vt_pion, p1b_pad_vt_elec, p1b_pad_vt_pion, p2_pad_vt;
	public H1F[] p1a_pad_edep_elec, p1a_pad_edep_pion, p1b_pad_edep_elec, p1b_pad_edep_pion, p2_pad_edep;

	public H2F[] H_e_LTCC_vt_theta, H_e_LTCC_nphe_theta, H_e_LTCC_XY;
	public H2F[] H_e_HTCC_vt_theta, H_e_HTCC_nphe_theta, H_e_HTCC_XY;
	public H1F[][][] H_e_bin_nphe_HTCC;

	public H2F H_elast_e_th_p, H_elast_p_th_p, H_elast_vz_vz, H_elast_dvz_phi, H_elast_dvz_theta_all, H_elast_dvz_vz;
	public H2F H_elast_Dphi_phi, H_elast_Dphi_theta, H_elast_Dphi_vz, H_elast_EB_phi, H_elast_EB_theta, H_elast_EB_vz ;
	public H2F[] H_elast_W_theta, H_elast_W_Q2, H_elast_inc_W_theta, H_elast_dvz_theta;

	public H2F H_epip_e_th_p, H_epip_p_th_p, H_epip_vz_vz, H_epip_dvz_phi, H_epip_dvz_theta, H_epip_dvz_vz;
	public H2F H_epip_Dphi_phi, H_epip_Dphi_theta, H_epip_Dphi_vz, H_epip_beta_p, H_epip_FTOF1b_dt_epad, H_epip_FTOF1b_dt_pippad;
	public H2F[] H_epip_W_theta, H_epip_inc_W_theta;

	public H2F H_pi0_G1_XY, H_pi0_G1_TR, H_pi0_G1_vt_evt, H_pi0_G1_layer_E;//4
	public H2F H_pi0_G2_XY, H_pi0_G2_TR, H_pi0_G2_vt_evt, H_pi0_G2_layer_E;//8
	public H2F H_pi0_G1_mom_the, H_pi0_G1_phi_the, H_pi0_G2_mom_the, H_pi0_G2_phi_the;//12
	public H2F H_pi0_open_E, H_pi0_E_the, H_pi0_phi_the;//15
    	public H1F H_pi0_mass, H_pi0_G1_layers, H_pi0_G2_layers;//18

	public IndexedTable InverseTranslationTable;
    	public IndexedTable calibrationTranslationTable;
    	public IndexedTable rfTable;
    	public ConstantsManager ccdb;

        public dst_mon(int reqrunNum, String reqOutputDir, float reqEB){
		runNum = reqrunNum;EB=reqEB;
                outputDir = reqOutputDir;
		Nevts=0;Nelecs=0;Ntrigs=0;
		Ntrigs_sect = new int[6];
		Nelecs_sect = new int[6];
		for(int s=0;s<6;s++){Ntrigs_sect[s]=0;Nelecs_sect[s]=0;}
		trigger_bits = new boolean[32];
		//Eb = 2.22f;
		Mp = 0.93827f;
                if(reqEB>0 && reqEB<4)Eb=2.22f;
                //if(reqEB>4 && reqEB<7.6)Eb=6.535f;
                //if(reqEB>4 && reqEB<7.6)Eb=6.42f;
                //if(reqEB>7.6 && reqEB<9)Eb=7.55f;
                //if(reqEB>9)Eb=10.6f;
                Eb = reqEB;
                System.out.println("Eb="+Eb+" (EB="+EB+") , run="+runNum);

		rfPeriod = 4.008f;
                ccdb = new ConstantsManager();
                ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo","/calibration/eb/rf/config"}));
                rfTable = ccdb.getConstants(runNum,"/calibration/eb/rf/config");
                if (rfTable.hasEntry(1, 1, 1)){
                System.out.println(String.format("RF period from ccdb for run %d: %f",runNum,rfTable.getDoubleValue("clock",1,1,1)));
                rfPeriod = (float)rfTable.getDoubleValue("clock",1,1,1);
                }
		rf_large_integer = 1000;

                VB = new LorentzVector(0,0,Eb,Eb);
                VT = new LorentzVector(0,0,0,Mp);

		H_e_t_f = new H2F("H_e_t_f","H_e_t_f",100,-180,180,100,0,40);
		H_e_t_f.setTitle("electron #theta vs #phi");
		H_e_t_f.setTitleX("#phi (^o)");
		H_e_t_f.setTitleY("#theta (^o)");

		H_e_p_f = new H2F("H_e_p_f","H_e_p_f",100,-180,180,100,0,EB);
		H_e_p_f.setTitle("electron p vs #phi");
		H_e_p_f.setTitleX("#phi (^o)");
		H_e_p_f.setTitleY("p (GeV)");

		H_e_vz_f = new H2F("H_e_vz_f","H_e_vz_f",100,-180,180,100,-15,15);
		H_e_vz_f.setTitle("electron vz vs #phi");
		H_e_vz_f.setTitleX("#phi (^o)");
		H_e_vz_f.setTitleY("vz (cm)");

		H_e_vt_vz = new H2F("H_e_vt_vz","H_e_vt_vz",100,-15,15,100,-4,4);
		H_e_vt_vz.setTitle("electron vt vs vz");
		H_e_vt_vz.setTitleX("vz (cm)");
		H_e_vt_vz.setTitleY("vt (ns)");
		H_e_vt_p = new H2F("H_e_vt_p","H_e_vt_p",100,0,EB,100,-4,4);
		H_e_vt_p.setTitle("electron vt vs mom");
		H_e_vt_p.setTitleX("p (GeV)");
		H_e_vt_p.setTitleY("vt (ns)");
		H_e_vt_t = new H2F("H_e_vt_t","H_e_vt_t",100,0,40,100,-4,4);
		H_e_vt_t.setTitle("electron vt vs #theta");
		H_e_vt_t.setTitleX("#theta (^o)");
		H_e_vt_t.setTitleY("vt (ns)");


		H_e_PCAL = new H2F("H_e_PCAL","H_e_PCAL",200,-400,400,200,-400,400);
		H_e_PCAL.setTitle("electron PCAL position");
		H_e_PCAL.setTitleX("X (cm)");
		H_e_PCAL.setTitleY("Y (cm)");
		H_e_FTOF = new H2F("H_e_FTOF","H_e_FTOF",200,-400,400,200,-400,400);
		H_e_FTOF.setTitle("electron FTOF position");
		H_e_FTOF.setTitleX("X (cm)");
		H_e_FTOF.setTitleY("Y (cm)");

		H_e_LTCC = new H2F("H_e_LTCC","H_e_LTCC",200,-400,400,200,-400,400);
		H_e_LTCC.setTitle("electron LTCC position");
		H_e_LTCC.setTitleX("X (cm)");
		H_e_LTCC.setTitleY("Y (cm)");
		H_e_DCSL6 = new H2F("H_e_DCSL6","H_e_DCSL6",200,-300,300,200,-300,300);
		H_e_DCSL6.setTitle("electron DC SL6 position");
		H_e_DCSL6.setTitleX("X (cm)");
		H_e_DCSL6.setTitleY("Y (cm)");
		H_e_DCSL5 = new H2F("H_e_DCSL5","H_e_DCSL5",200,-300,300,200,-300,300);
		H_e_DCSL5.setTitle("electron DC SL5 position");
		H_e_DCSL5.setTitleX("X (cm)");
		H_e_DCSL5.setTitleY("Y (cm)");
		H_e_DCSL4 = new H2F("H_e_DCSL4","H_e_DCSL4",200,-200,200,200,-200,200);
		H_e_DCSL4.setTitle("electron DC SL4 position");
		H_e_DCSL4.setTitleX("X (cm)");
		H_e_DCSL4.setTitleY("Y (cm)");
		H_e_DCSL3 = new H2F("H_e_DCSL3","H_e_DCSL3",200,-200,200,200,-200,200);
		H_e_DCSL3.setTitle("electron DC SL3 position");
		H_e_DCSL3.setTitleX("X (cm)");
		H_e_DCSL3.setTitleY("Y (cm)");
		H_e_DCSL2 = new H2F("H_e_DCSL2","H_e_DCSL2",200,-120,120,200,-120,120);
		H_e_DCSL2.setTitle("electron DC SL2 position");
		H_e_DCSL2.setTitleX("X (cm)");
		H_e_DCSL2.setTitleY("Y (cm)");
		H_e_DCSL1 = new H2F("H_e_DCSL1","H_e_DCSL1",200,-120,120,200,-120,120);
		H_e_DCSL1.setTitle("electron DC SL1 position");
		H_e_DCSL1.setTitleX("X (cm)");
		H_e_DCSL1.setTitleY("Y (cm)");
	
		H_e_HTCC = new H2F("H_e_HTCC","H_e_HTCC",200,-100,100,200,-100,100);
		H_e_HTCC.setTitle("electron HTCC position");
		H_e_HTCC.setTitleX("X (cm)");
		H_e_HTCC.setTitleY("Y (cm)");
		H_e_nphe_HTCC = new H2F("H_e_nphe_HTCC","H_e_nphe_HTCC",200,-100,100,200,-100,100);
		H_e_nphe_HTCC.setTitle("electron HTCC <nphe>");
		H_e_nphe_HTCC.setTitleX("X (cm)");
		H_e_nphe_HTCC.setTitleY("Y (cm)");
		H_e_bin_theta_HTCC = new H2F("H_e_bin_theta_HTCC","H_e_bin_theta_HTCC",200,-100,100,200,-100,100);
		H_e_bin_theta_HTCC.setTitle("electron HTCC #theta bin");
		H_e_bin_theta_HTCC.setTitleX("X (cm)");
		H_e_bin_theta_HTCC.setTitleY("Y (cm)");
		H_e_bin_phi_HTCC = new H2F("H_e_bin_phi_HTCC","H_e_bin_phi_HTCC",200,-100,100,200,-100,100);
		H_e_bin_phi_HTCC.setTitle("electron HTCC #phi bin");
		H_e_bin_phi_HTCC.setTitleX("X (cm)");
		H_e_bin_phi_HTCC.setTitleY("Y (cm)");
		H_e_theta_HTCC = new H2F("H_e_theta_HTCC","H_e_theta_HTCC",200,-100,100,200,-100,100);
		H_e_theta_HTCC.setTitle("electron HTCC <#theta>");
		H_e_theta_HTCC.setTitleX("X (cm)");
		H_e_theta_HTCC.setTitleY("Y (cm)");
		H_e_phi_HTCC = new H2F("H_e_phi_HTCC","H_e_phi_HTCC",200,-100,100,200,-100,100);
		H_e_phi_HTCC.setTitle("electron HTCC <#phi>");
		H_e_phi_HTCC.setTitleX("X (cm)");
		H_e_phi_HTCC.setTitleY("Y (cm)");

		H_e_HTCC_cut = new H2F[10];
		for(int ic=0;ic<10;ic++){
			H_e_HTCC_cut[ic] = new H2F(String.format("H_e_HTCC_cut_%d",ic+1),String.format("H_e_HTCC_cut_%d",ic+1),200,-100,100,200,-100,100);
			H_e_HTCC_cut[ic].setTitle(String.format("e rate at HTCC with nphe>%d",ic+1));
			H_e_HTCC_cut[ic].setTitleX("X (cm)");
			H_e_HTCC_cut[ic].setTitleY("Y (cm)");
		}
		H_e_bin_nphe_HTCC = new H1F[6][15][30];
		for(int s=0;s<6;s++)for(int it=0;it<15;it++)for(int ip=0;ip<30;ip++){
			H_e_bin_nphe_HTCC[s][it][ip] = new H1F(String.format("H_e_bin_nphe_HTCC_%d_%d_%d",s,it,ip),String.format("H_e_bin_nphe_HTCC_%d_%d_%d",s,it,ip),50,0,50);
			H_e_bin_nphe_HTCC[s][it][ip].setTitle(String.format("HTCC nphe s%d #theta%d #phi%d",s+1,it+1,ip+1));
			H_e_bin_nphe_HTCC[s][it][ip].setTitleX("nphe");
		}

		H_e_t_p = new H2F[6];
		H_e_vz_t = new H2F[6];
		H_e_vz_p = new H2F[6];
		//must declare those
		H_e_EC_etot_p = new H2F[6];
		H_e_EC_vt_theta = new H2F[6];
		H_e_EC_XY = new H2F[6];
		H_e_FTOF_vt_pad1a = new H2F[6];
		H_e_FTOF_edep_pad1a = new H2F[6];
		H_e_FTOF_XY_pad1a = new H2F[6];
		H_e_FTOF_vt_pad1b = new H2F[6];
		H_FTOF_pos_beta_mom_pad1a = new H2F[6];
		H_FTOF_neg_beta_mom_pad1a = new H2F[6];
		H_FTOF_pos_beta_mom_pad1b = new H2F[6];
		H_FTOF_neg_beta_mom_pad1b = new H2F[6];
		H_FTOF_pos_mass_mom_pad1a = new H2F[6];
		H_FTOF_pos_mass_the_pad1a = new H2F[6];
		H_FTOF_neg_mass_mom_pad1a = new H2F[6];
		H_FTOF_neg_mass_the_pad1a = new H2F[6];
		H_FTOF_pos_mass_mom_pad1b = new H2F[6];
		H_FTOF_pos_mass_the_pad1b = new H2F[6];
		H_FTOF_neg_mass_mom_pad1b = new H2F[6];
		H_FTOF_neg_mass_the_pad1b = new H2F[6];
		H_e_FTOF_edep_pad1b = new H2F[6];
		H_e_FTOF_XY_pad1b = new H2F[6];
		p1a_pad_vt_elec = new H2F[6];
		p1a_pad_vt_pion = new H2F[6];
		p1b_pad_vt_elec = new H2F[6];
		p1b_pad_vt_pion = new H2F[6];
		p2_pad_vt = new H2F[6];
		p1a_pad_edep_elec = new H1F[6];
		p1a_pad_edep_pion = new H1F[6];
		p1b_pad_edep_elec = new H1F[6];
		p1b_pad_edep_pion = new H1F[6];
		p2_pad_edep = new H1F[6];

		H_e_LTCC_vt_theta = new H2F[6];
		H_e_LTCC_nphe_theta = new H2F[6];
		H_e_LTCC_XY = new H2F[6];
		H_e_HTCC_vt_theta = new H2F[6];
		H_e_HTCC_nphe_theta = new H2F[6];
		H_e_HTCC_XY = new H2F[6];
		for(int s=0;s<6;s++){
			H_e_t_p[s] = new H2F(String.format("H_e_t_p_%d",s+1),String.format("H_e_t_p_%d",s+1),100,0,EB,100,0,40);
			H_e_t_p[s].setTitle("electron #theta vs p S"+(s+1));
			H_e_t_p[s].setTitleX("p (GeV)");
			H_e_t_p[s].setTitleY("#theta (^o)");
			H_e_vz_t[s] = new H2F(String.format("H_e_vz_t_%d",s+1),String.format("H_e_vz_t_%d",s+1),100,0,40,100,-15,15);
			H_e_vz_t[s].setTitle("electron vz vs #theta S"+(s+1));
			H_e_vz_t[s].setTitleX("#theta (^o)");
			H_e_vz_t[s].setTitleY("vz (cm)");
			H_e_vz_p[s] = new H2F(String.format("H_e_vz_p_%d",s+1),String.format("H_e_vz_p_%d",s+1),100,0,EB,100,-15,15);
			H_e_vz_p[s].setTitle("electron vz vs p S"+(s+1));
			H_e_vz_p[s].setTitleX("p (GeV)");
			H_e_vz_p[s].setTitleY("vz (cm)");

			H_e_EC_etot_p[s] = new H2F(String.format("H_e_EC_etot_p_%d",s+1),String.format("H_e_EC_etot_p_%d",s+1),100,0,EB,100,0.1,0.4);
			H_e_EC_etot_p[s].setTitle(String.format("EC Etot/p vs p S%d",s+1));
			H_e_EC_etot_p[s].setTitleX("p (GeV)");
			H_e_EC_etot_p[s].setTitleY("Etot/p");
			//H_e_EC_vt_theta[s] = new H2F(String.format("H_e_EC_vt_theta_%d",s+1),String.format("H_e_EC_vt_theta_%d",s+1),100,0,40,100,80,120);
			H_e_EC_vt_theta[s] = new H2F(String.format("H_e_EC_vt_theta_%d",s+1),String.format("H_e_EC_vt_theta_%d",s+1),100,0,40,100,-4,4);
			H_e_EC_vt_theta[s].setTitle(String.format("EC vt vs #theta S%d",s+1));
			H_e_EC_vt_theta[s].setTitleX("#theta (^o)");
			H_e_EC_vt_theta[s].setTitleY("vt (ns)");
			H_e_EC_XY[s] = new H2F(String.format("H_e_EC_XY_%d",s+1),String.format("H_e_EC_XY_%d",s+1),200,0,400,200,-200,200);
			H_e_EC_XY[s].setTitle(String.format("PCAL Y vs X S%d",s+1));
			H_e_EC_XY[s].setTitleX("X (cm)");
			H_e_EC_XY[s].setTitleY("Y (cm)");
			H_e_FTOF_vt_pad1a[s] = new H2F(String.format("H_e_FTOF_vt_pad1a_%d",s+1),String.format("H_e_FTOF_vt_pad1a_%d",s+1),25,0,25,100,-4,4);
			H_e_FTOF_vt_pad1a[s].setTitle(String.format("FTOF1a vt vs pad S%d",s+1));
			H_e_FTOF_vt_pad1a[s].setTitleX("paddle");
			H_e_FTOF_vt_pad1a[s].setTitleY("vt (ns)");
			H_e_FTOF_edep_pad1a[s] = new H2F(String.format("H_e_FTOF_edep_pad1a_%d",s+1),String.format("H_e_FTOF_edep_pad1a_%d",s+1),25,0,25,100,0,25);
			H_e_FTOF_edep_pad1a[s].setTitle(String.format("FTOF1a Edep vs pad S%d",s+1));
			H_e_FTOF_edep_pad1a[s].setTitleX("paddle");
			H_e_FTOF_edep_pad1a[s].setTitleY("Edep (MeV)");
			H_e_FTOF_XY_pad1a[s] = new H2F(String.format("H_e_FTOF_XY_pad1a_%d",s+1),String.format("H_e_FTOF_XY_pad1a_%d",s+1),50,0,400,100,-200,200);
			H_e_FTOF_XY_pad1a[s].setTitle(String.format("FTOF1a Y vs X S%d",s+1));
			H_e_FTOF_XY_pad1a[s].setTitleX("X (cm)");
			H_e_FTOF_XY_pad1a[s].setTitleY("Y (cm)");
			H_e_FTOF_vt_pad1b[s] = new H2F(String.format("H_e_FTOF_vt_pad1b_%d",s+1),String.format("H_e_FTOF_vt_pad1b_%d",s+1),65,0,65,100,-4,4);
			H_e_FTOF_vt_pad1b[s].setTitle(String.format("FTOF1b vt vs pad S%d",s+1));
			H_e_FTOF_vt_pad1b[s].setTitleX("paddle");
			H_e_FTOF_vt_pad1b[s].setTitleY("vt (ns)");
			H_FTOF_pos_beta_mom_pad1a[s] = new H2F(String.format("H_FTOF_pos_beta_mom_pad1a_%d",s+1),String.format("H_FTOF_pos_beta_mom_pad1a_%d",s+1),100,0,8.,100,0,1.2);
			H_FTOF_pos_beta_mom_pad1a[s].setTitle(String.format("POS TOF1A #beta vs mom S%d",s+1));
			H_FTOF_pos_beta_mom_pad1a[s].setTitleX("p (GeV)");
			H_FTOF_pos_beta_mom_pad1a[s].setTitleY("TOF #beta");
			H_FTOF_neg_beta_mom_pad1a[s] = new H2F(String.format("H_FTOF_pos_beta_neg_pad1a_%d",s+1),String.format("H_FTOF_pos_beta_neg_pad1a_%d",s+1),100,0,8.,100,0,1.2);
			H_FTOF_neg_beta_mom_pad1a[s].setTitle(String.format("NEG TOF1A #beta vs mom S%d",s+1));
			H_FTOF_neg_beta_mom_pad1a[s].setTitleX("p (GeV)");
			H_FTOF_neg_beta_mom_pad1a[s].setTitleY("TOF #beta");
			H_FTOF_pos_beta_mom_pad1b[s] = new H2F(String.format("H_FTOF_pos_beta_mom_pad1b_%d",s+1),String.format("H_FTOF_pos_beta_mom_pad1b_%d",s+1),100,0,8.,100,0,1.2);
			H_FTOF_pos_beta_mom_pad1b[s].setTitle(String.format("POS TOF1B #beta vs mom S%d",s+1));
			H_FTOF_pos_beta_mom_pad1b[s].setTitleX("p (GeV)");
			H_FTOF_pos_beta_mom_pad1b[s].setTitleY("TOF #beta");
			H_FTOF_neg_beta_mom_pad1b[s] = new H2F(String.format("H_FTOF_pos_beta_neg_pad1b_%d",s+1),String.format("H_FTOF_pos_beta_neg_pad1b_%d",s+1),100,0,8.,100,0,1.2);
			H_FTOF_neg_beta_mom_pad1b[s].setTitle(String.format("NEG TOF1B #beta vs mom S%d",s+1));
			H_FTOF_neg_beta_mom_pad1b[s].setTitleX("p (GeV)");
			H_FTOF_neg_beta_mom_pad1b[s].setTitleY("TOF #beta");
			H_FTOF_pos_mass_mom_pad1a[s] = new H2F(String.format("H_FTOF_pos_mass_mom_pad1a_%d",s+1),String.format("H_FTOF_pos_mass_mom_pad1a_%d",s+1),100,0,5,100,-0.5,4.5);
			H_FTOF_pos_mass_mom_pad1a[s].setTitle(String.format("POS Mass^2 vs mom S%d",s+1));
			H_FTOF_pos_mass_mom_pad1a[s].setTitleX("p (GeV)");
			H_FTOF_pos_mass_mom_pad1a[s].setTitleY("M^2 (GeV^2)");
			H_FTOF_pos_mass_the_pad1a[s] = new H2F(String.format("H_FTOF_pos_mass_the_pad1a_%d",s+1),String.format("H_FTOF_pos_mass_the_pad1a_%d",s+1),100,0,45,100,-0.5,3.5);
			H_FTOF_pos_mass_the_pad1a[s].setTitle(String.format("POS Mass^2 vs #theta S%d",s+1));
			H_FTOF_pos_mass_the_pad1a[s].setTitleX("#theta (^o)");
			H_FTOF_pos_mass_the_pad1a[s].setTitleY("M^2 (GeV^2)");
			H_FTOF_neg_mass_mom_pad1a[s] = new H2F(String.format("H_FTOF_neg_mass_mom_pad1a_%d",s+1),String.format("H_FTOF_neg_mass_mom_pad1a_%d",s+1),100,0,5.,100,-0.5,2.);
			H_FTOF_neg_mass_mom_pad1a[s].setTitle(String.format("NEG Mass^2 vs mom S%d",s+1));
			H_FTOF_neg_mass_mom_pad1a[s].setTitleX("p (GeV)");
			H_FTOF_neg_mass_mom_pad1a[s].setTitleY("M^2 (GeV^2)");
			H_FTOF_neg_mass_the_pad1a[s] = new H2F(String.format("H_FTOF_neg_mass_the_pad1a_%d",s+1),String.format("H_FTOF_neg_mass_the_pad1a_%d",s+1),100,0,45,100,-0.5,3.5);
			H_FTOF_neg_mass_the_pad1a[s].setTitle(String.format("NEG Mass^2 vs #theta S%d",s+1));
			H_FTOF_neg_mass_the_pad1a[s].setTitleX("#theta (^o)");
			H_FTOF_neg_mass_the_pad1a[s].setTitleY("M^2 (GeV^2)");

			H_FTOF_pos_mass_mom_pad1b[s] = new H2F(String.format("H_FTOF_pos_mass_mom_pad1b_%d",s+1),String.format("H_FTOF_pos_mass_mom_pad1b_%d",s+1),100,0,5,100,-0.5,4.5);
			H_FTOF_pos_mass_mom_pad1b[s].setTitle(String.format("POS Mass^2 vs mom S%d",s+1));
			H_FTOF_pos_mass_mom_pad1b[s].setTitleX("p (GeV)");
			H_FTOF_pos_mass_mom_pad1b[s].setTitleY("M^2 (GeV^2)");
			H_FTOF_pos_mass_the_pad1b[s] = new H2F(String.format("H_FTOF_pos_mass_the_pad1b_%d",s+1),String.format("H_FTOF_pos_mass_the_pad1b_%d",s+1),100,0,45,100,-0.5,3.5);
			H_FTOF_pos_mass_the_pad1b[s].setTitle(String.format("POS Mass^2 vs #theta S%d",s+1));
			H_FTOF_pos_mass_the_pad1b[s].setTitleX("#theta (^o)");
			H_FTOF_pos_mass_the_pad1b[s].setTitleY("M^2 (GeV^2)");
			H_FTOF_neg_mass_mom_pad1b[s] = new H2F(String.format("H_FTOF_neg_mass_mom_pad1b_%d",s+1),String.format("H_FTOF_neg_mass_mom_pad1b_%d",s+1),100,0,5.,100,-0.5,2.0);
			H_FTOF_neg_mass_mom_pad1b[s].setTitle(String.format("NEG Mass^2 vs mom S%d",s+1));
			H_FTOF_neg_mass_mom_pad1b[s].setTitleX("p (GeV)");
			H_FTOF_neg_mass_mom_pad1b[s].setTitleY("M^2 (GeV^2)");
			H_FTOF_neg_mass_the_pad1b[s] = new H2F(String.format("H_FTOF_neg_mass_the_pad1b_%d",s+1),String.format("H_FTOF_neg_mass_the_pad1b_%d",s+1),100,0,45,100,-0.5,3.5);
			H_FTOF_neg_mass_the_pad1b[s].setTitle(String.format("NEG Mass^2 vs #theta S%d",s+1));
			H_FTOF_neg_mass_the_pad1b[s].setTitleX("#theta (^o)");
			H_FTOF_neg_mass_the_pad1b[s].setTitleY("M^2 (GeV^2)");
			H_e_FTOF_edep_pad1b[s] = new H2F(String.format("H_e_FTOF_edep_pad1b_%d",s+1),String.format("H_e_FTOF_edep_pad1b_%d",s+1),65,0,65,100,0,25);
			H_e_FTOF_edep_pad1b[s].setTitle(String.format("FTOF1b Edep vs pad %d",s+1));
			H_e_FTOF_edep_pad1b[s].setTitleX("paddle");
			H_e_FTOF_edep_pad1b[s].setTitleY("Edep (MeV)");
			H_e_FTOF_XY_pad1b[s] = new H2F(String.format("H_e_FTOF_XY_pad1b_%d",s+1),String.format("H_e_FTOF_XY_pad1b_%d",s+1),100,0,400,100,-200,200);
			H_e_FTOF_XY_pad1b[s].setTitle(String.format("FTOF1b Y vs X S%d",s+1));
			H_e_FTOF_XY_pad1b[s].setTitleX("X (cm)");
			H_e_FTOF_XY_pad1b[s].setTitleY("Y (cm)");
			
			//from tof_monitor
			p1a_pad_vt_elec[s] = new H2F(String.format("p1a_pad_vt_elec_S%d",s+1),String.format("p1a_pad_vt_elec_S%d",s+1),100,-rfPeriod/2,rfPeriod/2,100,-4,4);
			p1a_pad_vt_elec[s].setTitle(String.format("p1a S%d time_elec",s+1));
			p1a_pad_vt_elec[s].setTitleX("vertex time - RFTime (ns)");
			p1a_pad_vt_elec[s].setTitleY("vertex time - STTime (ns)");
			p1a_pad_vt_pion[s] = new H2F(String.format("p1a_pad_vt_pion_S%d",s+1),String.format("p1a_pad_vt_pion_S%d",s+1),100,-rfPeriod/2,rfPeriod/2,100,-4,4);
			p1a_pad_vt_pion[s].setTitle(String.format("p1a S%d time_pion",s+1));
			p1a_pad_vt_pion[s].setTitleX("vertex time - RFTime (ns)");
			p1a_pad_vt_pion[s].setTitleY("vertex time - STTime (ns)");
			p1b_pad_vt_elec[s] = new H2F(String.format("p1b_pad_vt_elec_S%d",s+1),String.format("p1b_pad_vt_elec_S%d",s+1),100,-rfPeriod/2,rfPeriod/2,100,-4,4);
			p1b_pad_vt_elec[s].setTitle(String.format("p1b S%d time_elec",s+1));
			p1b_pad_vt_elec[s].setTitleX("vertex time - RFTime (ns)");
			p1b_pad_vt_elec[s].setTitleY("vertex time - STTime (ns)");
			p1b_pad_vt_pion[s] = new H2F(String.format("p1b_pad_vt_pion_S%d",s+1),String.format("p1b_pad_vt_pion_S%d",s+1),100,-rfPeriod/2,rfPeriod/2,100,-4,4);
			p1b_pad_vt_pion[s].setTitle(String.format("p1b S%d time_pion",s+1));
			p1b_pad_vt_pion[s].setTitleX("vertex time - RFTime (ns)");
			p1b_pad_vt_pion[s].setTitleY("vertex time - STTime (ns)");
			p2_pad_vt[s] = new H2F(String.format("p2_pad_vt_S%d",s+1),String.format("p2_pad_vt_S%d",s+1),100,-rfPeriod/2,rfPeriod/2,100,-4,4);
			p2_pad_vt[s].setTitle(String.format("p2 S%d time",s+1));
			p2_pad_vt[s].setTitleX("vertex time - RFTime (ns)");
			p2_pad_vt[s].setTitleY("vertex time - STTime (ns)");
			p1a_pad_edep_elec[s] = new H1F(String.format("p1a_pad_edep_elec_S%d",s+1),String.format("p1a_pad_edep_elec_S%d",s+1),100,0,50);
			p1a_pad_edep_elec[s].setTitle(String.format("p1a S%d energy_elec",s+1));
			p1a_pad_edep_elec[s].setTitleX("E (MeV)");
			p1a_pad_edep_pion[s] = new H1F(String.format("p1a_pad_edep_pion_S%d",s+1),String.format("p1a_pad_edep_pion_S%d",s+1),100,0,50);
			p1a_pad_edep_pion[s].setTitle(String.format("p1a S%d energy_pion",s+1));
			p1a_pad_edep_pion[s].setTitleX("E (MeV)");
			p1b_pad_edep_elec[s] = new H1F(String.format("p1b_pad_edep_elec_S%d",s+1),String.format("p1b_pad_edep_elec_S%d",s+1),100,0,50);
			p1b_pad_edep_elec[s].setTitle(String.format("p1b S%d energy_elec",s+1));
			p1b_pad_edep_elec[s].setTitleX("E (MeV)");
			p1b_pad_edep_pion[s] = new H1F(String.format("p1b_pad_edep_pion_S%d",s+1),String.format("p1b_pad_edep_pion_S%d",s+1),100,0,50);
			p1b_pad_edep_pion[s].setTitle(String.format("p1b S%d energy_pion",s+1));
			p1b_pad_edep_pion[s].setTitleX("E (MeV)");
			p2_pad_edep[s] = new H1F(String.format("p2_pad_edep_S%d",s+1),String.format("p2_pad_edep_S%d",s+1),100,0,50);
			p2_pad_edep[s].setTitle(String.format("p2 S%d energy",s+1));
			p2_pad_edep[s].setTitleX("E (MeV)");


			//H_e_LTCC_vt_theta[s] = new H2F(String.format("H_e_LTCC_vt_theta_%d",s+1),String.format("H_e_LTCC_vt_theta_%d",s+1),100,0,40,100,-5,5);
			H_e_LTCC_vt_theta[s] = new H2F(String.format("H_e_LTCC_vt_theta_%d",s+1),String.format("H_e_LTCC_vt_theta_%d",s+1),100,0,40,100,-200,150);
			H_e_LTCC_vt_theta[s].setTitle(String.format("LTCC vt vs #theta S%d",s+1));
			H_e_LTCC_vt_theta[s].setTitleX("#theta (^o)");
			H_e_LTCC_vt_theta[s].setTitleY("vt (ns)");
			H_e_LTCC_nphe_theta[s] = new H2F(String.format("H_e_LTCC_nphe_theta_%d",s+1),String.format("H_e_LTCC_nphe_theta_%d",s+1),100,0,40,100,0,50);
			H_e_LTCC_nphe_theta[s].setTitle(String.format("LTCC nphe vs theta S%d",s+1));
			H_e_LTCC_nphe_theta[s].setTitleX("#theta (^o)");
			H_e_LTCC_nphe_theta[s].setTitleY("nphe");
			H_e_LTCC_XY[s] = new H2F(String.format("H_e_LTCC_XY_%d",s+1),String.format("H_e_LTCC_XY_%d",s+1),100,0,400,100,-100,100);
			H_e_LTCC_XY[s].setTitle(String.format("LTCC Y vs X S%d",s+1));
			H_e_LTCC_XY[s].setTitleX("X (cm)");
			H_e_LTCC_XY[s].setTitleY("Y (cm)");
			//H_e_HTCC_vt_theta[s] = new H2F(String.format("H_e_HTCC_vt_theta_%d",s+1),String.format("H_e_HTCC_vt_theta_%d",s+1),100,0,40,100,-5,5);
			H_e_HTCC_vt_theta[s] = new H2F(String.format("H_e_HTCC_vt_theta_%d",s+1),String.format("H_e_HTCC_vt_theta_%d",s+1),100,0,40,100,-100,100);
			H_e_HTCC_vt_theta[s].setTitle(String.format("HTCC vt vs #theta S%d",s+1));
			H_e_HTCC_vt_theta[s].setTitleX("#theta (^o)");
			H_e_HTCC_vt_theta[s].setTitleY("vt (ns)");
			H_e_HTCC_nphe_theta[s] = new H2F(String.format("H_e_HTCC_nphe_theta_%d",s+1),String.format("H_e_HTCC_nphe_theta_%d",s+1),100,0,40,100,0,50);
			H_e_HTCC_nphe_theta[s].setTitle(String.format("HTCC nphe vs theta S%d",s+1));
			H_e_HTCC_nphe_theta[s].setTitleX("#theta (^o)");
			H_e_HTCC_nphe_theta[s].setTitleY("nphe");
			H_e_HTCC_XY[s] = new H2F(String.format("H_e_HTCC_XY_%d",s+1),String.format("H_e_HTCC_XY_%d",s+1),50,0,100,50,-50,50);
			H_e_HTCC_XY[s].setTitle(String.format("HTCC Y vs X S%d",s+1));
			H_e_HTCC_XY[s].setTitleX("X (cm)");
			H_e_HTCC_XY[s].setTitleY("Y (cm)");
		}

		//H_elast_e_th_p = new H2F("H_elast_e_th_p","H_elast_e_th_p",100,0,EB,100,0,40);
		H_elast_e_th_p = new H2F("H_elast_e_th_p","H_elast_e_th_p",100,0,EB,100,5,12);
		H_elast_e_th_p.setTitle("electron #theta vs p");
		H_elast_e_th_p.setTitleX("p (GeV)");
		H_elast_e_th_p.setTitleY("#theta (^o)");
		H_elast_p_th_p = new H2F("H_elast_p_th_p","H_elast_p_th_p",100,0,2,100,50,80);
		H_elast_p_th_p.setTitle("proton #theta vs p");
		H_elast_p_th_p.setTitleX("p (GeV)");
		H_elast_p_th_p.setTitleY("#theta (^o)");
		H_elast_vz_vz = new H2F("H_elast_vz_vz","H_elast_vz_vz",100,-15,5,100,-5,5);
		H_elast_vz_vz.setTitle("elastic vz p vs e");
		H_elast_vz_vz.setTitleX("e vz (cm)");
		H_elast_vz_vz.setTitleY("p vz (cm)");
		H_elast_dvz_phi = new H2F("H_elast_dvz_phi","H_elast_dvz_phi",100,-180,180,100,-5,15);
		H_elast_dvz_phi.setTitle("eleastic #Delta vz vs #phi");
		H_elast_dvz_phi.setTitleX("#phi (^o)");
		H_elast_dvz_phi.setTitleY("#Delta vz (cm)");
		H_elast_dvz_theta_all = new H2F("H_elast_dvz_theta_all","H_elast_dvz_theta_all",100,50,80,100,-5,15);
		H_elast_dvz_theta_all.setTitle("elastic #Delta vz vs #theta");
		H_elast_dvz_theta_all.setTitleX("#theta (^o)");
		H_elast_dvz_theta_all.setTitleY("#Delta vz (cm)");
		H_elast_dvz_vz = new H2F("H_elast_dvz_vz","H_elast_dvz_vz",100,-5,5,100,-5,15);
		H_elast_dvz_vz.setTitle("elastic #Delta vz vs z");
		H_elast_dvz_vz.setTitleX("vz (cm)");
		H_elast_dvz_vz.setTitleY("#Delta vz (cm)");
		H_elast_Dphi_phi = new H2F("H_elast_Dphi_phi","H_elast_Dphi_phi",100,-180,180,100,-10,10);
		H_elast_Dphi_phi.setTitle("#Delta#phi vs #phi");
		H_elast_Dphi_phi.setTitleX("#phi (^o)");
		H_elast_Dphi_phi.setTitleY("#Delta#phi (^o)");
		H_elast_Dphi_theta = new H2F("H_elast_Dphi_theta","H_elast_Dphi_theta",100,50,80,100,-10,10);
		H_elast_Dphi_theta.setTitle("#Delta#phi vs #theta");
		H_elast_Dphi_theta.setTitleX("#theta (^o)");
		H_elast_Dphi_theta.setTitleY("#Delta#phi (^o)");
		H_elast_Dphi_vz = new H2F("H_elast_Dphi_vz","H_elast_Dphi_vz",100,-5,5,100,-10,10);
		H_elast_Dphi_vz.setTitle("#Delta#phi vs vz");
		H_elast_Dphi_vz.setTitleX("vz (cm)");
		H_elast_Dphi_vz.setTitleY("#Delta#phi (^o)");
		H_elast_EB_phi = new H2F("H_elast_EB_phi","H_elast_EB_phi",100,-180,180,100,0.7*EB,1.1*EB);
		H_elast_EB_phi.setTitle("Eb vs #phi");
		H_elast_EB_phi.setTitleX("#phi (^o)");
		H_elast_EB_phi.setTitleY("Eb (GeV)");
		H_elast_EB_theta = new H2F("H_elast_EB_theta","H_elast_EB_theta",100,50,80,100,0.7*EB,1.1*EB);
		H_elast_EB_theta.setTitle("Eb vs #theta");
		H_elast_EB_theta.setTitleX("#theta (^o)");
		H_elast_EB_theta.setTitleY("Eb (GeV)");
		H_elast_EB_vz  = new H2F("H_elast_EB_vz","H_elast_EB_vz",100,-5,5,100,0.7*EB,1.1*EB);
		H_elast_EB_vz.setTitle("Eb vs vz");
		H_elast_EB_vz.setTitleX("vz (cm)");
		H_elast_EB_vz.setTitleY("Eb (GeV)");
	
		H_elast_W_Q2 = new H2F[6];
		H_elast_W_theta = new H2F[6];
		H_elast_inc_W_theta = new H2F[6];
		H_elast_dvz_theta = new H2F[6];
		for(int s=0;s<6;s++){
			H_elast_W_Q2[s] = new H2F(String.format("H_elast_W_Q2_%d",s+1),String.format("H_elast_W_Q2_%d",s+1),400,0,4,100,0,1.5);
			H_elast_W_Q2[s].setTitle(String.format("Q^2 vs W S%d",s+1));
			H_elast_W_Q2[s].setTitleX("W ( GeV)");
			H_elast_W_Q2[s].setTitleY("Q^2 (GeV^2)");
			//H_elast_W_theta[s] = new H2F(String.format("H_elast_W_theta_%d",s+1),String.format("H_elast_W_theta_%d",s+1),100,0,4,100,0,40);
			H_elast_W_theta[s] = new H2F(String.format("H_elast_W_theta_%d",s+1),String.format("H_elast_W_theta_%d",s+1),400,0,4,7,5,12);
			H_elast_W_theta[s].setTitle(String.format("#theta vs W S%d",s+1));
			H_elast_W_theta[s].setTitleX("W ( GeV)");
			H_elast_W_theta[s].setTitleY("#theta (^o)");
			//H_elast_inc_W_theta[s] = new H2F(String.format("H_elast_inc_W_theta_%d",s+1),String.format("H_elast_inc_W_theta_%d",s+1),100,0,4,100,0,40);
			H_elast_inc_W_theta[s] = new H2F(String.format("H_elast_inc_W_theta_%d",s+1),String.format("H_elast_inc_W_theta_%d",s+1),400,0,4,12,5,17);
			H_elast_inc_W_theta[s].setTitle(String.format("#theta vs W S%d",s+1));
			H_elast_inc_W_theta[s].setTitleX("W ( GeV)");
			H_elast_inc_W_theta[s].setTitleY("#theta (^o)");
			H_elast_dvz_theta[s] = new H2F(String.format("H_elast_dvz_theta_%d",s+1),String.format("H_elast_dvz_theta_%d",s+1),100,50,80,100,-5,15);
			H_elast_dvz_theta[s].setTitle("elastic #Delta vz vs #theta");
			H_elast_dvz_theta[s].setTitleX("#theta (^o)");
			H_elast_dvz_theta[s].setTitleY("#Delta vz (cm)");
		}

		H_epip_e_th_p = new H2F("H_epip_e_th_p","H_epip_e_th_p",100,0,EB,100,0,40);
		H_epip_e_th_p.setTitle("electron #theta vs p");
		H_epip_e_th_p.setTitleX("p (GeV)");
		H_epip_e_th_p.setTitleY("#theta (^o)");
		H_epip_p_th_p = new H2F("H_epip_p_th_p","H_epip_p_th_p",100,0,4,100,0,50);
		H_epip_p_th_p.setTitle("pip #theta vs p");
		H_epip_p_th_p.setTitleX("p (GeV)");
		H_epip_p_th_p.setTitleY("#theta (^o)");
		H_epip_vz_vz = new H2F("H_epip_vz_vz","H_epip_vz_vz",100,-15,15,100,-15,15);
		H_epip_vz_vz.setTitle("epipic vz p vs e");
		H_epip_vz_vz.setTitleX("e vz (cm)");
		H_epip_vz_vz.setTitleY("pip vz (cm)");
		H_epip_dvz_phi = new H2F("H_epip_dvz_phi","H_epip_dvz_phi",100,-180,180,100,-15,15);
		H_epip_dvz_phi.setTitle("e pip #Delta vz vs #phi");
		H_epip_dvz_phi.setTitleX("#phi (^o)");
		H_epip_dvz_phi.setTitleY("#Delta vz (cm)");
		H_epip_dvz_theta = new H2F("H_epip_dvz_theta","H_epip_dvz_theta",100,0,80,100,-15,15);
		H_epip_dvz_theta.setTitle("e pip #Delta vz vs #theta");
		H_epip_dvz_theta.setTitleX("#theta (^o)");
		H_epip_dvz_theta.setTitleY("#Delta vz (cm)");
		H_epip_dvz_vz = new H2F("H_epip_dvz_vz","H_epip_dvz_vz",100,-15,15,100,-15,15);
		H_epip_dvz_vz.setTitle("e pip #Delta vz vs z");
		H_epip_dvz_vz.setTitleX("vz (cm)");
		H_epip_dvz_vz.setTitleY("#Delta vz (cm)");
		H_epip_Dphi_phi = new H2F("H_epip_Dphi_phi","H_epip_Dphi_phi",100,-180,180,100,-180,180);
		H_epip_Dphi_phi.setTitle("#Delta#phi vs #phi");
		H_epip_Dphi_phi.setTitleX("#phi (^o)");
		H_epip_Dphi_phi.setTitleY("#Delta#phi (^o)");
		H_epip_Dphi_theta = new H2F("H_epip_Dphi_theta","H_epip_Dphi_theta",100,0,80,100,-180,180);
		H_epip_Dphi_theta.setTitle("#Delta#phi vs #theta");
		H_epip_Dphi_theta.setTitleX("#theta (^o)");
		H_epip_Dphi_theta.setTitleY("#Delta#phi (^o)");
		H_epip_Dphi_vz = new H2F("H_epip_Dphi_vz","H_epip_Dphi_vz",100,-15,15,100,-180,180);
		H_epip_Dphi_vz.setTitle("#Delta#phi vs vz");
		H_epip_Dphi_vz.setTitleX("vz (cm)");
		H_epip_Dphi_vz.setTitleY("#Delta#phi (^o)");

		H_epip_beta_p = new H2F("H_epip_beta_p","H_epip_beta_p",100,0,EB,100,0.8,1.2);
		H_epip_beta_p.setTitle("pip #beta vs p");
		H_epip_beta_p.setTitleX("p (GeV)");
		H_epip_beta_p.setTitleY("#beta");
		H_epip_FTOF1b_dt_epad = new H2F("H_epip_FTOF1b_dt_epad","H_epip_FTOF1b_dt_epad",65,0,65,100,-4,4);
		H_epip_FTOF1b_dt_epad.setTitle("#DeltaFTOF1b vs e pad");
		H_epip_FTOF1b_dt_epad.setTitleX("e pad");
		H_epip_FTOF1b_dt_epad.setTitleY("#Delta t (ns)");
		H_epip_FTOF1b_dt_pippad = new H2F("H_epip_FTOF1b_dt_pippad","H_epip_FTOF1b_dt_pippad",65,0,65,100,-4,4);
		H_epip_FTOF1b_dt_pippad.setTitle("#DeltaFTOF1b vs #pi^+ pad");
		H_epip_FTOF1b_dt_pippad.setTitleX("#pi^+ pad");
		H_epip_FTOF1b_dt_pippad.setTitleY("#Delta t (ns)");
	
		H_epip_W_theta = new H2F[6];
		H_epip_inc_W_theta = new H2F[6];
		for(int s=0;s<6;s++){
			H_epip_W_theta[s] = new H2F(String.format("H_epip_W_theta_%d",s+1),String.format("H_epip_W_theta_%d",s+1),100,0,4,100,0,40);
			H_epip_W_theta[s].setTitle(String.format("#theta vs W S%d",s+1));
			H_epip_W_theta[s].setTitleX("W ( GeV)");
			H_epip_W_theta[s].setTitleY("#theta (^o)");
			H_epip_inc_W_theta[s] = new H2F(String.format("H_epip_inc_W_theta_%d",s+1),String.format("H_epip_inc_W_theta_%d",s+1),100,0,4,100,0,40);
			H_epip_inc_W_theta[s].setTitle(String.format("#theta vs W S%d",s+1));
			H_epip_inc_W_theta[s].setTitleX("W ( GeV)");
			H_epip_inc_W_theta[s].setTitleY("#theta (^o)");
		}

		H_pi0_G1_XY = new H2F("H_pi0_G1_XY","H_pi0_G1_XY",100,-400,400,100,-400,400);
		H_pi0_G1_XY.setTitle("#gamma1 Y vs X");
		H_pi0_G1_XY.setTitleX("X (cm)");
		H_pi0_G1_XY.setTitleY("Y (cm)");
		//H_pi0_G1_TR = new H2F("H_pi0_G1_TR","H_pi0_G1_TR",100,22,25,100,20,30);
		//H_pi0_G1_TR.setTitle("#gamma1 TOF vs path");
		//H_pi0_G1_TR.setTitleX("path (ns)");
		//H_pi0_G1_TR.setTitleY("TOF (ns)");
		H_pi0_G1_TR = new H2F("H_pi0_G1_TR","H_pi0_G1_TR",100,190,210,100,190,210);
		H_pi0_G1_TR.setTitle("#gamma1 TOF vs e TOF");
		H_pi0_G1_TR.setTitleX("e TOF (ns)");
		H_pi0_G1_TR.setTitleY("#gamma1 TOF (ns)");
		H_pi0_G1_vt_evt = new H2F("H_pi0_G1_vt_evt","H_pi0_G1_vt_evt",100,-3.5,3.5,100,-3.5,3.5);
		H_pi0_G1_vt_evt.setTitle("#gamma1 vt vs e vt");
		H_pi0_G1_vt_evt.setTitleX("e vt (ns)");
		H_pi0_G1_vt_evt.setTitleY("#gamma1 vt (ns)");
		H_pi0_G1_layer_E = new H2F("H_pi0_G1_layer_E","H_pi0_G1_layer_E",5,0,5,100,0,EB);
		H_pi0_G1_layer_E.setTitle("#gamma1 E vs n layers");
		H_pi0_G1_layer_E.setTitleX("n layers");
		H_pi0_G1_layer_E.setTitleY("E (GeV)");
		H_pi0_G2_XY = new H2F("H_pi0_G2_XY","H_pi0_G2_XY",100,-400,400,100,-400,400);
		H_pi0_G2_XY.setTitle("#gamma2 Y vs X");
		H_pi0_G2_XY.setTitleX("X (cm)");
		H_pi0_G2_XY.setTitleY("Y (cm)");
		//H_pi0_G2_TR = new H2F("H_pi0_G2_TR","H_pi0_G2_TR",100,22,25,100,20,30);
		//H_pi0_G2_TR.setTitle("#gamma2 TOF vs path");
		//H_pi0_G2_TR.setTitleX("path (ns)");
		//H_pi0_G2_TR.setTitleY("TOF (ns)");
		H_pi0_G2_TR = new H2F("H_pi0_G2_TR","H_pi0_G2_TR",100,190,210,100,190,210);
		H_pi0_G2_TR.setTitle("#gamma2 TOF vs e TOF");
		H_pi0_G2_TR.setTitleX("e TOF (ns)");
		H_pi0_G2_TR.setTitleY("#gamma2 TOF (ns)");
		H_pi0_G2_vt_evt = new H2F("H_pi0_G2_vt_evt","H_pi0_G2_vt_evt",100,-3.5,3.5,100,-3.5,3.5);
		H_pi0_G2_vt_evt.setTitle("#gamma2 vt vs e vr");
		H_pi0_G2_vt_evt.setTitleX("e vt (ns)");
		H_pi0_G2_vt_evt.setTitleY("#gamma2 vt (ns)");
		H_pi0_G2_layer_E= new H2F("H_pi0_G2_layer_E","H_pi0_G2_layer_E",5,0,5,100,0,EB);
		H_pi0_G2_layer_E.setTitle("#gamma2 E vs n layers");
		H_pi0_G2_layer_E.setTitleX("n layers");
		H_pi0_G2_layer_E.setTitleY("E (GeV)");
		H_pi0_G1_mom_the = new H2F("H_pi0_G1_mom_the","H_pi0_G1_mom_the",100,0,EB,100,0,40);
		H_pi0_G1_mom_the.setTitle("#gamma1  #theta vs E");
		H_pi0_G1_mom_the.setTitleX("E (GeV)");
		H_pi0_G1_mom_the.setTitleY("#theta (^o)");
		H_pi0_G1_phi_the = new H2F("H_pi0_G1_phi_the","H_pi0_G1_phi_the",100,-180,180,100,0,40);
		H_pi0_G1_phi_the.setTitle("#gamma1 #theta vs #phi");
		H_pi0_G1_phi_the.setTitleX("#phi (^o)");
		H_pi0_G1_phi_the.setTitleY("#phi (^o)");
		H_pi0_G2_mom_the = new H2F("H_pi0_G2_mom_the","H_pi0_G2_mom_the",100,0,EB,100,0,40);
		H_pi0_G2_mom_the.setTitle("#gamma2 #theta vs E");
		H_pi0_G2_mom_the.setTitleX("E (GeV)");
		H_pi0_G2_mom_the.setTitleY("#theta (^o)");
		H_pi0_G2_phi_the = new H2F("H_pi0_G2_phi_the","H_pi0_G2_phi_the",100,-180,180,100,0,40);
		H_pi0_G2_phi_the.setTitle("#gamma2 #theta vs phi");
		H_pi0_G2_phi_the.setTitleX("#phi (^o)");
		H_pi0_G2_phi_the.setTitleY("#theta (^o)");
		H_pi0_open_E = new H2F("H_pi0_open_E","H_pi0_open_E",100,0,EB,100,0,30);
		H_pi0_open_E.setTitle("#pi^0 open angle vs E");
		H_pi0_open_E.setTitleX("E (GeV)");
		H_pi0_open_E.setTitleY("#theta_#gamma#gamma (^o)");
		H_pi0_E_the = new H2F("H_pi0_E_the","H_pi0_E_the",100,0,EB,100,0,40);
		H_pi0_E_the.setTitle("#pi^0 #theta vs E");
		H_pi0_E_the.setTitleX("E (GeV)");
		H_pi0_E_the.setTitleY("#theta (^o)");
		H_pi0_phi_the = new H2F("H_pi0_phi_the","H_pi0_phi_the",100,-180,180,100,0,40);
		H_pi0_phi_the.setTitle("#pi^0 #theta vs #phi");
		H_pi0_phi_the.setTitleX("#phi (^o)");
		H_pi0_phi_the.setTitleY("#theta (^o)");
		H_pi0_mass = new H1F("H_pi0_mass","H_pi0_mass",100,0,0.5);
		H_pi0_mass.setTitle("#pi^0 mass");
		H_pi0_mass.setTitleX("m_#gamma#gamma (GeV)");
		//H_pi0_mass.setTitleY();
		H_pi0_G1_layers = new H1F("H_pi0_G1_layers","H_pi0_G1_layers",5,0,5);
		H_pi0_G1_layers.setTitle("#gamma1 n layers");
		H_pi0_G1_layers.setTitleX("nlayers");
		H_pi0_G2_layers = new H1F("H_pi0_G2_layers","H_pi0_G2_layers",5,0,5);
		H_pi0_G2_layers.setTitle("#gamma2 n layers");
		H_pi0_G2_layers.setTitleX("nlayers");

	}
        public int getNelecs(){return this.Nelecs;}
        public int getNtrigs(){return this.Ntrigs;}

        public double Vangle(Vector3 v1, Vector3 v2){
                double res = 0;
                double l1 = v1.mag();
                double l2 = v2.mag();
                double prod = v1.dot(v2);
                if( l1 * l2 !=0 && Math.abs(prod)<l1*l2 )res = Math.toDegrees( Math.acos(prod/(l1*l2) ) );
                return res;
        }

        public int getSect(DataBank bank, int partInd){
                for(int k = 0; k < bank.rows(); k++){
                        if(bank.getShort("pindex",k)==partInd)return bank.getInt("sector",k);
                }   
                return -1; 
        }   

	public boolean fillConfBank(DataBank confbank){
		boolean selectTrig = false;
		long TriggerWord = confbank.getLong("trigger",0);
		for (int i = 31; i >= 0; i--) {trigger_bits[i] = (TriggerWord & (1 << i)) != 0;} 
		if(trigger_bits[1] || trigger_bits[2] || trigger_bits[3] || trigger_bits[4] || trigger_bits[5] || trigger_bits[6]){
			selectTrig = true;
			Ntrigs++;
			if(trigger_bits[1])Ntrigs_sect[0]++;
			if(trigger_bits[2])Ntrigs_sect[1]++;
			if(trigger_bits[3])Ntrigs_sect[2]++;
			if(trigger_bits[4])Ntrigs_sect[3]++;
			if(trigger_bits[5])Ntrigs_sect[4]++;
			if(trigger_bits[6])Ntrigs_sect[5]++;
		}
		return selectTrig;
	}
	public void fillRecBank(DataBank recBank){
		STT = recBank.getFloat("startTime",0);
		RFT = recBank.getFloat("RFTime",0);
	}
	public void fillECAL(DataBank bank){
		e_EC_etot = 0;e_PCAL_edep=0;e_EC_ein=0;e_EC_eout=0;
		for(int r=0;r<bank.rows();r++){
			if(bank.getShort("pindex",r)==e_part_ind){
				if(bank.getByte("layer",r)==1){
					found_eECAL = true;
					e_PCAL_X = bank.getFloat("x",r);
					e_PCAL_Y = bank.getFloat("y",r);
					e_PCAL_Z = bank.getFloat("z",r);
					e_PCAL_edep += bank.getFloat("energy",r);
					e_PCAL_t = bank.getFloat("time",r);
					e_PCAL_path = bank.getFloat("path",r);
					e_PCAL_vt = e_PCAL_t - e_PCAL_path/29.98f - STT;
					if(e_sect==0)e_sect = bank.getByte("sector",r);
				}
				if(bank.getByte("layer",r)==4)e_EC_ein += bank.getFloat("energy",r);
				if(bank.getByte("layer",r)==7)e_EC_eout += bank.getFloat("energy",r);
			}
			if(bank.getShort("pindex",r)==G1_part_ind){
				if(bank.getByte("layer",r)==1 && G1_pcal_ind==-1){
					G1_cal_layers++;G1_pcal_ind = r;
					G1_pcal_X = bank.getFloat("x",r);
					G1_pcal_Y = bank.getFloat("y",r);
					G1_pcal_Z = bank.getFloat("z",r);
					G1_pcal_t = bank.getFloat("time",r);
					//G1_pcal_R = G1_pcal_X*G1_pcal_X + G1_pcal_Y*G1_pcal_Y + (G1_pcal_Z-e_vz)*(G1_pcal_Z-e_vz);
					G1_pcal_R = G1_pcal_X*G1_pcal_X + G1_pcal_Y*G1_pcal_Y + G1_pcal_Z*G1_pcal_Z;
					G1_pcal_R = (float)Math.sqrt(G1_pcal_R);
					G1_pcal_vt = G1_pcal_t - G1_pcal_R/29.98f - STT;
				}
				//else if(bank.getByte("layer",r)==1 && G1_pcal_ind!=-1){
				//	System.out.println("error: found a photon with TWO PCAL CLUSTERS");
				//}
				else if(bank.getByte("layer",r)>1)G1_cal_layers++;
			}
			if(bank.getShort("pindex",r)==G2_part_ind){
				if(bank.getByte("layer",r)==1 && G2_pcal_ind==-1){
					G2_cal_layers++;G2_pcal_ind = r;
					G2_pcal_X = bank.getFloat("x",r);
					G2_pcal_Y = bank.getFloat("y",r);
					G2_pcal_Z = bank.getFloat("z",r);
					G2_pcal_t = bank.getFloat("time",r);
					//G2_pcal_R = G2_pcal_X*G2_pcal_X + G2_pcal_Y*G2_pcal_Y + (G2_pcal_Z-e_vz)*(G2_pcal_Z-e_vz);
					G2_pcal_R = G2_pcal_X*G2_pcal_X + G2_pcal_Y*G2_pcal_Y + G2_pcal_Z*G2_pcal_Z;
					G2_pcal_R = (float)Math.sqrt(G2_pcal_R);
					G2_pcal_vt = G2_pcal_t - G2_pcal_R/29.98f - STT;
				}
			}
		}
		e_EC_etot = e_PCAL_edep+e_EC_ein+e_EC_eout;
	}

	public void fillFTOF(DataBank part, DataBank bank){
		for(int r=0;r<bank.rows();r++)if(bank.getByte("detector",r)==12){
			//from Dan's request use 1a - leptons/pions, all charges for p2
			int s = bank.getInt("sector",r)-1;
			//electrons at p1a, p1b
			if(bank.getShort("pindex",r)==e_part_ind){
				if(bank.getByte("layer",r)==1){
					found_eFTOF1a = true;
					e_FTOF_pad1a = bank.getShort("component",r);
					e_FTOF1a_X = bank.getFloat("x",r);
					e_FTOF1a_Y = bank.getFloat("y",r);
					e_FTOF1a_Z = bank.getFloat("z",r);
					e_FTOF1a_edep = bank.getFloat("energy",r);
					e_FTOF1a_t = bank.getFloat("time",r);
					e_FTOF1a_path = bank.getFloat("path",r);
					e_FTOF1a_vt = e_FTOF1a_t - e_FTOF1a_path/29.98f - STT;
					thisTime = e_FTOF1a_t - e_FTOF1a_path/29.98f - RFT;
					thisTime = (thisTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
					thisTime = thisTime - rfPeriod/2;
					p1a_pad_vt_elec[s].fill(thisTime,e_FTOF1a_vt);
					p1a_pad_edep_elec[s].fill(e_FTOF1a_edep);
				}
				if(bank.getByte("layer",r)==2){
					found_eFTOF1b = true;
					e_FTOF_pad1b = bank.getShort("component",r);
					e_FTOF1b_X = bank.getFloat("x",r);
					e_FTOF1b_Y = bank.getFloat("y",r);
					e_FTOF1b_Z = bank.getFloat("z",r);
					e_FTOF1b_edep = bank.getFloat("energy",r);
					e_FTOF1b_t = bank.getFloat("time",r);
					e_FTOF1b_path = bank.getFloat("path",r);
					e_FTOF1b_vt = e_FTOF1b_t - e_FTOF1b_path/29.98f - STT;
					thisTime = e_FTOF1b_t - e_FTOF1b_path/29.98f -  RFT;
					thisTime = (thisTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
					thisTime = thisTime - rfPeriod/2;
					p1b_pad_vt_elec[s].fill(thisTime,e_FTOF1b_vt);
					p1b_pad_edep_elec[s].fill(e_FTOF1b_edep);
				}
			}
			//pi plus at p1a, p1b
			if( bank.getShort("pindex",r)==pip_part_ind && bank.getByte("layer",r)==1 ){
				pip_FTOF1a_t = bank.getFloat("time",r);
				pip_FTOF1a_path = bank.getFloat("path",r);
				float pip_beta = pip_mom/(float)Math.sqrt(pip_mom*pip_mom + 0.13957f*0.13957f);
				pip_FTOF1a_vt = pip_FTOF1a_t - pip_FTOF1a_path / ( pip_beta * 29.98f ) - STT;
				thisTime = pip_FTOF1a_t - pip_FTOF1a_path / ( pip_beta * 29.98f ) - RFT;
				thisTime = (thisTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
				thisTime = thisTime - rfPeriod/2;
				p1a_pad_vt_pion[s].fill(thisTime,pip_FTOF1a_vt);
				p1a_pad_edep_pion[s].fill(bank.getFloat("energy",r));
			}
			if( bank.getShort("pindex",r)==pip_part_ind && bank.getByte("layer",r)==2 ){
				pip_FTOF_pad1b = bank.getShort("component",r);
				pip_FTOF1b_t = bank.getFloat("time",r);
				pip_FTOF1b_path = bank.getFloat("path",r);
				float pip_beta = pip_mom/(float)Math.sqrt(pip_mom*pip_mom + 0.13957f*0.13957f);
				pip_FTOF1b_vt = pip_FTOF1b_t - pip_FTOF1b_path / ( pip_beta * 29.98f ) - STT;
				thisTime = pip_FTOF1b_t - pip_FTOF1b_path / ( pip_beta * 29.98f ) - RFT;
				thisTime = (thisTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
				thisTime = thisTime - rfPeriod/2;
				p1b_pad_vt_pion[s].fill(thisTime, pip_FTOF1b_vt);
				p1b_pad_edep_pion[s].fill(bank.getFloat("energy",r));
			}

			//pi minus at p1a, p1b
			if( bank.getShort("pindex",r)==pim_part_ind && bank.getByte("layer",r)==1 ){
				pim_FTOF1a_t = bank.getFloat("time",r);
				pim_FTOF1a_path = bank.getFloat("path",r);
				float pim_beta = pim_mom/(float)Math.sqrt(pim_mom*pim_mom + 0.13957f*0.13957f);
				pim_FTOF1a_vt = pim_FTOF1a_t - pim_FTOF1a_path / ( pim_beta * 29.98f ) - STT;
				thisTime = pim_FTOF1a_t - pim_FTOF1a_path / ( pim_beta * 29.98f ) -  RFT;
				thisTime = (thisTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
				thisTime = thisTime - rfPeriod/2;
				p1a_pad_vt_pion[s].fill(thisTime, pim_FTOF1a_vt);
				p1a_pad_edep_pion[s].fill(bank.getFloat("energy",r));
			}
			if( bank.getShort("pindex",r)==pim_part_ind && bank.getByte("layer",r)==2 ){
				pim_FTOF1b_t = bank.getFloat("time",r);
				pim_FTOF1b_path = bank.getFloat("path",r);
				float pim_beta = pim_mom/(float)Math.sqrt(pim_mom*pim_mom + 0.13957f*0.13957f);
				pim_FTOF1b_vt = pim_FTOF1b_t - pim_FTOF1b_path / ( pim_beta * 29.98f ) - STT;
				thisTime = pim_FTOF1b_t - pim_FTOF1b_path / ( pim_beta * 29.98f ) -  RFT;
				thisTime = (thisTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
				thisTime = thisTime - rfPeriod/2;
				p1b_pad_vt_pion[s].fill(thisTime, pim_FTOF1b_vt);
				p1b_pad_edep_pion[s].fill(bank.getFloat("energy",r));
			}

			//all particles at p2
			if(bank.getByte("layer",r)==3){
				thisTime = bank.getFloat("time",r) - bank.getFloat("path",r)/29.98f - RFT;
				thisTime = (thisTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
				thisTime = thisTime - rfPeriod/2;		
				p2_pad_vt[s].fill(thisTime, bank.getFloat("time",r) - bank.getFloat("path",r)/29.98f - STT);
				p2_pad_edep[s].fill(bank.getFloat("energy",r));
			}

			if(bank.getShort("pindex",r)>-1 && bank.getShort("pindex",r)<part.rows()){
				byte q = part.getByte("charge", bank.getShort("pindex",r)); 
				float px = part.getFloat("px", bank.getShort("pindex",r)); 
				float py = part.getFloat("py", bank.getShort("pindex",r)); 
				float pz = part.getFloat("pz", bank.getShort("pindex",r)); 
				double mom = Math.sqrt(px*px+py*py+pz*pz);
				double the = Math.toDegrees(Math.acos(pz/mom));
				double TOFbeta = bank.getFloat("path",r)/(29.98f*(bank.getFloat("time",r)-STT));
				double TOFmass = mom * mom * ( 1/(TOFbeta*TOFbeta) - 1);
				if(bank.getByte("layer",r)==1 && q>0 ){
					H_FTOF_pos_beta_mom_pad1a[s].fill(mom,TOFbeta);
					H_FTOF_pos_mass_mom_pad1a[s].fill(mom,TOFmass);
					H_FTOF_pos_mass_the_pad1a[s].fill(the,TOFmass);
				}
				if(bank.getByte("layer",r)==1 && q<0 ){
					H_FTOF_neg_beta_mom_pad1a[s].fill(mom,TOFbeta);
					H_FTOF_neg_mass_mom_pad1a[s].fill(mom,TOFmass);
					H_FTOF_neg_mass_the_pad1a[s].fill(the,TOFmass);
				}
				if(bank.getByte("layer",r)==2 && q>0 ){
					H_FTOF_pos_beta_mom_pad1b[s].fill(mom,TOFbeta);
					H_FTOF_pos_mass_mom_pad1b[s].fill(mom,TOFmass);
					H_FTOF_pos_mass_the_pad1b[s].fill(the,TOFmass);
				}
				if(bank.getByte("layer",r)==2 && q<0 ){
					H_FTOF_neg_beta_mom_pad1b[s].fill(mom,TOFbeta);
					H_FTOF_neg_mass_mom_pad1b[s].fill(mom,TOFmass);
					H_FTOF_neg_mass_the_pad1b[s].fill(the,TOFmass);
				}
			}
		}
	}

	public void fillCerenkov(DataBank bank){
		for(int r=0;r<bank.rows();r++){
			if(bank.getShort("pindex",r)==e_part_ind){
				if(bank.getByte("detector",r)==15){
					found_eHTCC = true;
					e_HTCC_X = bank.getFloat("x",r);
					e_HTCC_Y = bank.getFloat("y",r);
					e_HTCC_Z = bank.getFloat("z",r);
					e_HTCC_t = bank.getFloat("time",r);
					e_HTCC_nphe = bank.getFloat("nphe",r);
					e_HTCC_path = bank.getFloat("path",r);
					e_HTCC_vt = e_HTCC_t - e_HTCC_path/29.98f - STT;
				}
				if(bank.getByte("detector",r)==16){
					found_eLTCC = true;
					e_LTCC_X = bank.getFloat("x",r);
					e_LTCC_Y = bank.getFloat("y",r);
					e_LTCC_Z = bank.getFloat("z",r);
					e_LTCC_t = bank.getFloat("time",r);
					e_LTCC_nphe = bank.getFloat("nphe",r);
					e_LTCC_path = bank.getFloat("path",r);
					e_LTCC_vt = e_LTCC_t - e_LTCC_path/29.98f - STT;
				}
			}
		}
	}
	public void fillTraj(DataBank trajBank){
		for(int r=0;r<trajBank.rows();r++){
			//System.out.println("comparing "+e_part_ind+" and "+trajBank.getInt("pindex",r));
			if(trajBank.getShort("pindex",r)==e_part_ind){
				found_eTraj=true;
				float dstlayer= trajBank.getInt("layer",r);				
				switch(trajBank.getInt("detector",r)) {
					case 15:
						e_HTCC_tX = trajBank.getFloat("x",r);
						e_HTCC_tY = trajBank.getFloat("y",r);
						e_HTCC_tZ = trajBank.getFloat("z",r);
						e_HTCC_phi = (float)Math.toDegrees(Math.atan2(e_HTCC_tY,e_HTCC_tX)) + 30f;
						while(e_HTCC_phi<0)e_HTCC_phi+=60;
						while(e_HTCC_phi>60)e_HTCC_phi-=60;
						float htccR = (float)Math.sqrt(e_HTCC_tX*e_HTCC_tX+e_HTCC_tY*e_HTCC_tY+e_HTCC_tZ*e_HTCC_tZ);
						//System.out.println("htccR="+htccR);
						e_HTCC_theta = (float)Math.toDegrees(Math.acos( e_HTCC_tZ/htccR ));
						e_HTCC_bin_theta = -1;e_HTCC_bin_phi=-1;
						if(e_HTCC_theta>5 && e_HTCC_theta<10)e_HTCC_bin_theta = (int) ((e_HTCC_theta - 5f)/1f);
						if(e_HTCC_theta>10 && e_HTCC_theta<20)e_HTCC_bin_theta = 5 + (int) ((e_HTCC_theta - 10f)/2f);
						if(e_HTCC_theta>20 && e_HTCC_theta<40)e_HTCC_bin_theta = 10 + (int) ((e_HTCC_theta - 20f)/4f);
						if(e_HTCC_phi<20)e_HTCC_bin_phi = (int) (e_HTCC_phi/4f);
						if(e_HTCC_phi>20 && e_HTCC_phi<40)e_HTCC_bin_phi = 5 + (int) ((e_HTCC_phi-20f)/1f);
						if(e_HTCC_phi>40)e_HTCC_bin_phi = 25 + (int) ((e_HTCC_phi-40f)/4f);
					case 6:
						if(dstlayer==6){
							e_DCSL1_tX = trajBank.getFloat("x",r);
							e_DCSL1_tY = trajBank.getFloat("y",r);
						}
						if(dstlayer==12){
							e_DCSL2_tX = trajBank.getFloat("x",r);
							e_DCSL2_tY = trajBank.getFloat("y",r);
						}
						if(dstlayer==18){
							e_DCSL3_tX = trajBank.getFloat("x",r);
							e_DCSL3_tY = trajBank.getFloat("y",r);
						}
						if(dstlayer==24){
							e_DCSL4_tX = trajBank.getFloat("x",r);
							e_DCSL4_tY = trajBank.getFloat("y",r);
						}
						if(dstlayer==30){
							e_DCSL5_tX = trajBank.getFloat("x",r);
							e_DCSL5_tY = trajBank.getFloat("y",r);						
						}
						if(dstlayer==36){
							e_DCSL6_tX = trajBank.getFloat("x",r);
							e_DCSL6_tY = trajBank.getFloat("y",r);
						}
					case 16:
						e_LTCC_tX = trajBank.getFloat("x",r);
						e_LTCC_tY = trajBank.getFloat("y",r);
					case 12:
						e_FTOF_tX = trajBank.getFloat("x",r);
						e_FTOF_tY = trajBank.getFloat("y",r);
					case 7:
						if (dstlayer>0 && dstlayer<4){
							e_PCAL_tX = trajBank.getFloat("x",r);
							e_PCAL_tY = trajBank.getFloat("y",r);
						}
				}
			}
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
                        e_the = (float)Math.toDegrees(Math.acos(pz/e_mom));
                        e_vz = bank.getFloat("vz", k); 
                        //if( pid == 11 && inDC && e_the>6 && Math.abs(e_vz)<200 ){}
                        if( pid == 11 && inDC && Math.abs(e_vz+3)<12 && (e_mom>1.5 || runNum<2600 ) ){
                                e_phi = (float)Math.toDegrees(Math.atan2(py,px));
                                e_vx = bank.getFloat("vx", k); 
                                e_vy = bank.getFloat("vy", k); 
                                Ve = new LorentzVector(px,py,pz,e_mom);
                                VGS = new LorentzVector(0,0,0,0);
                                VGS.add(VB);
                                VGS.sub(Ve);
                                e_Q2 = (float) -VGS.mass2();
                                e_xB = e_Q2/(2f*Mp*(Eb-e_mom));
                                e_W  = (float) Math.sqrt(Mp*Mp + e_Q2*(1f/e_xB-1f) );
                                return k;
                        }   
                }
                return -1;
	}
	public void makeOthers(DataBank bank){
		for(int k = 0; k < bank.rows(); k++){
			int pid = bank.getInt("pid", k);
			float px = bank.getFloat("px", k);
			float py = bank.getFloat("py", k);
			float pz = bank.getFloat("pz", k);
			float vx = bank.getFloat("vx", k);
			float vy = bank.getFloat("vy", k);
			float vz = bank.getFloat("vz", k);
			float be = bank.getFloat("beta", k);
			int status = bank.getShort("status", k);
			if (status<0) status = -status;
			boolean inDC = (status>=2000 && status<4000);
			float mom = (float)Math.sqrt(px*px+py*py+pz*pz);
			float the = (float)Math.toDegrees(Math.acos(pz/mom));
			//if(pid == 211 && pip_part_ind==-1 && inDC && Math.abs(bank.getFloat("chi2pid", k))<3 ){}
			if(pid == 211 && pip_part_ind==-1 && inDC && (mom>1.5||runNum<2600) ){
				pip_part_ind = k;
				pip_mom = mom;
				pip_the = the;
				pip_phi = (float)Math.toDegrees(Math.atan2(py,px));
				pip_vx = vx;pip_vy = vy;pip_vz = vz;
				pip_beta = be;
                                Vpip = new LorentzVector(px,py,pz,Math.sqrt(pip_mom*pip_mom+0.13957f*0.13957f));
			}
			if(pid == -211 && pim_part_ind==-1 && inDC && (mom>1.5||runNum<2600) ){
				pim_part_ind = k;
				pim_mom = mom;
			}
			if(pid == 2212 && prot_part_ind==-1){
				prot_part_ind = k;
				prot_mom = mom;
				prot_the = the;
				prot_phi = (float)Math.toDegrees(Math.atan2(py,px));
				prot_vx = vx;prot_vy = vy;prot_vz = vz;
				prot_beta = be;
                                Vprot = new LorentzVector(px,py,pz,Math.sqrt(prot_mom*prot_mom+0.93827f*0.93827f));
			}
			//if( (pid == 22 || (runNum<2600&&bank.getByte("charge",k)==0) ) && (mom>0.6||runNum<2600) && the>6 && G1_mom < mom ){}
			if( bank.getByte("charge",k)==0 && mom>0.4 && the>6 && G1_mom < mom ){
				G1_part_ind = k;
				G1_mom = mom;
				G1_the = the;
				G1_phi = (float)Math.toDegrees(Math.atan2(py,px));
				VG1 = new LorentzVector(px,py,pz,mom);
			}
			//if( G1_part_ind>-1 && k!=G1_part_ind && (mom>0.6||runNum<2600) && the>6 && (pid == 22 || (runNum<2600&&bank.getByte("charge",k)==0)) 
					//&& G2_mom < mom && mom < G1_mom){}
			if( G1_part_ind>-1 && k!=G1_part_ind && mom>0.4 && the>6 && bank.getByte("charge",k)==0 && G2_mom < mom && mom < G1_mom){
				G2_part_ind = k;
				G2_mom = mom;
				G2_the = the;
				G2_phi = (float)Math.toDegrees(Math.atan2(py,px));
				VG2 = new LorentzVector(px,py,pz,mom);
			}
		}
	}
	public boolean selectElastic(){
		boolean res = false;
		if(prot_part_ind>-1){
			elast_dPhi = prot_phi - e_phi + 180f;
			while(elast_dPhi> 180f)elast_dPhi -= 360f;
			while(elast_dPhi<-180f)elast_dPhi += 360f;
			float tantan = (float) (Math.tan(Ve.theta()/2) * Math.tan(Vprot.theta() ) );
			elast_EB = 0.93827f * (1-tantan)/tantan;

			if(EB<4f &&  prot_the > 60f-30f*prot_mom)res = true;
			if(EB>4f && EB<9f && Math.abs(elast_dPhi)<10f && prot_the > 55f && prot_the > 70f-30f*prot_mom && Math.abs(elast_EB-6.5f)<1.2f )res = true;
			if(EB>9f && Math.abs(elast_dPhi)<10f && prot_the > 70f-30f*prot_mom && Math.abs(elast_EB-10f)<1.2f )res = true;
		}
		return res;
	}
	public boolean select_epip(){
		boolean res = false;
		if(pip_part_ind>-1){
			epip_dPhi = pip_phi - e_phi + 180f;
			while(epip_dPhi> 180f)epip_dPhi -= 360f;
			while(epip_dPhi<-180f)epip_dPhi += 360f;
			LorentzVector VmissN = new LorentzVector(0,0,0,0);
			VmissN.add(VT);
			VmissN.add(VB);
			VmissN.sub(Ve);
			VmissN.sub(Vpip);
			epip_MM = (float)VmissN.mass2();
			res = true;
		}
		return res;
	}
	public boolean select_epi0(){
		boolean res = false;
		if( true
		  //&& ( runNum < 2600 || (Math.abs(e_FTOF1b_vt) < 0.5 && Math.abs(e_vz+5)<5) ) 
		  && G1_part_ind>-1 && G2_part_ind>-1 
		  //&& (runNum<2600 || (e_PCAL_t>190 && e_PCAL_t<210 && Math.abs(e_PCAL_t - G1_pcal_t) < 5 && Math.abs(e_PCAL_t - G2_pcal_t) < 5 ))
		  //&& Math.abs(G1_pcal_vt)<3 && Math.abs(G2_pcal_vt)<3 
		  ){
			VPI0 = new LorentzVector(0,0,0,0);
			VPI0.add(VG1);
			VPI0.add(VG2);
			pi0_mass = (float)VPI0.mass();
			pi0_E = (float)VPI0.e();
		       	pi0_the = (float)Math.toDegrees(VPI0.theta());
			pi0_phi = (float)Math.toDegrees(VPI0.phi());
			pi0_open = (float)Vangle(VG1.vect(),VG2.vect()) ;
			if(runNum > 2600 && pi0_open > 3 && pi0_open>9 * (1 - pi0_E/4) && pi0_the>8 && pi0_mass>0.05 && pi0_mass<0.5)res = true;
			if(runNum < 2600)res = true;
		}
		return res;
	}

	public void resetCounters(){
                e_part_ind = -1;found_eTraj=false;found_eECAL=false;found_eFTOF1a=false;found_eFTOF1b=false;found_eLTCC=false;found_eHTCC=false;
		pim_part_ind = -1; pip_part_ind = -1;pip_FTOF_pad1b = -1;prot_part_ind = -1;G1_part_ind = -1;G2_part_ind = -1;G1_pcal_ind = -1;G2_pcal_ind = -1;
		G1_cal_layers = 0;G2_cal_layers = 0;G1_mom = 0;G2_mom = 0;
	}
        public void processEvent(DataEvent event) {
		resetCounters();
		if(event.hasBank("RUN::config") && fillConfBank(event.getBank("RUN::config")) ){
			if(event.hasBank("REC::Event"))fillRecBank(event.getBank("REC::Event"));
			if(event.hasBank("REC::Particle"))e_part_ind = makeElectron(event.getBank("REC::Particle"));
			if(e_part_ind>-1){
				makeOthers(event.getBank("REC::Particle"));
				e_sect = 0;
				if(event.hasBank("REC::Track"))e_sect = getSect(event.getBank("REC::Track"),e_part_ind);
				if(event.hasBank("REC::Traj"))fillTraj(event.getBank("REC::Traj"));
				if(event.hasBank("REC::Cherenkov"))fillCerenkov(event.getBank("REC::Cherenkov"));
				if(event.hasBank("REC::Scintillator"))fillFTOF(event.getBank("REC::Particle"),event.getBank("REC::Scintillator"));
				if(event.hasBank("REC::Calorimeter"))fillECAL(event.getBank("REC::Calorimeter"));
				//if(e_sect>0 && found_eTraj){}
				if(e_sect>0 ){
					Nelecs++;Nelecs_sect[e_sect-1]++;
					FillHists();
				}
			}//if e_part_ind>-1
		}//event.hasBank("RUN::config")
        }//processEvent
	public void FillHists(){
		H_e_t_f.fill(e_phi,e_the);
		H_e_p_f.fill(e_phi,e_mom);
		H_e_vz_f.fill(e_phi,e_vz);
		H_e_vt_vz.fill(e_vz,e_FTOF1a_vt);
	       	H_e_vt_p.fill(e_mom,e_FTOF1a_vt);
		H_e_vt_t.fill(e_the,e_FTOF1a_vt);

		int s = e_sect-1;
		H_e_t_p[s].fill(e_mom,e_the);
		H_e_vz_t[s].fill(e_the,e_vz);
		H_e_vz_p[s].fill(e_mom,e_vz);
		H_elast_inc_W_theta[s].fill(e_W,e_the);
		H_epip_inc_W_theta[s].fill(e_W,e_the);
		
		if(found_eTraj){
			H_e_PCAL.fill(e_PCAL_tX,e_PCAL_tY);
			H_e_FTOF.fill(e_FTOF_tX,e_FTOF_tY);
			H_e_LTCC.fill(e_LTCC_tX,e_LTCC_tY);
			H_e_DCSL6.fill(e_DCSL6_tX,e_DCSL6_tY);
			H_e_DCSL5.fill(e_DCSL5_tX,e_DCSL5_tY);
			H_e_DCSL4.fill(e_DCSL4_tX,e_DCSL4_tY);
			H_e_DCSL3.fill(e_DCSL3_tX,e_DCSL3_tY);
			H_e_DCSL2.fill(e_DCSL2_tX,e_DCSL2_tY);
			H_e_DCSL1.fill(e_DCSL1_tX,e_DCSL1_tY);
		}
		if(found_eECAL){
			//e_PCAL_X, e_PCAL_Y, e_PCAL_Z, e_PCAL_edep, e_PCAL_t, e_EC_ein, e_EC_eout, e_EC_etot, e_PCAL_path, e_PCAL_vt;
			Vector3 pos = new Vector3(e_PCAL_X,e_PCAL_Y,e_PCAL_Z);pos.rotateZ(-s*Math.toRadians(60));
			H_e_EC_etot_p[s].fill(e_mom,e_EC_etot/e_mom);
			H_e_EC_vt_theta[s].fill(e_the,e_PCAL_vt);
			H_e_EC_XY[s].fill(pos.x(),pos.y());
		}
		if(found_eFTOF1a){
			//e_FTOF1a_X, e_FTOF1a_Y, e_FTOF1a_Z, e_FTOF1a_edep, e_FTOF1a_t, e_FTOF1a_path, e_FTOF1a_vt;
			Vector3 pos = new Vector3(e_FTOF1a_X,e_FTOF1a_Y,e_FTOF1a_Z);pos.rotateZ(-s*Math.toRadians(60));
			H_e_FTOF_vt_pad1a[s].fill(e_FTOF_pad1a,e_FTOF1a_vt);
			H_e_FTOF_edep_pad1a[s].fill(e_FTOF_pad1a,e_FTOF1a_edep);
			H_e_FTOF_XY_pad1a[s].fill(pos.x(),pos.y());
		}
		if(found_eFTOF1b){
			//e_FTOF1b_X, e_FTOF1b_Y, e_FTOF1b_Z, e_FTOF1b_edep, e_FTOF1b_t, e_FTOF1b_path, e_FTOF1b_vt;
			Vector3 pos = new Vector3(e_FTOF1b_X,e_FTOF1b_Y,e_FTOF1b_Z);pos.rotateZ(-s*Math.toRadians(60));
			H_e_FTOF_vt_pad1b[s].fill(e_FTOF_pad1b,e_FTOF1b_vt);
			H_e_FTOF_edep_pad1b[s].fill(e_FTOF_pad1b,e_FTOF1b_edep);
			H_e_FTOF_XY_pad1b[s].fill(pos.x(),pos.y());
		}
		if(found_eLTCC){
			//e_LTCC_X, e_LTCC_Y, e_LTCC_Z, e_LTCC_t, e_LTCC_nphe, e_LTCC_path, e_LTCC_vt;
			Vector3 pos = new Vector3(e_LTCC_X,e_LTCC_Y,e_LTCC_Z);pos.rotateZ(-s*Math.toRadians(60));
			H_e_LTCC_vt_theta[s].fill(e_the,e_LTCC_vt);
			H_e_LTCC_nphe_theta[s].fill(e_the,e_LTCC_nphe);
			H_e_LTCC_XY[s].fill(pos.x(),pos.y());
		}
		if(found_eHTCC){
			//e_HTCC_X, e_HTCC_Y, e_HTCC_Z, e_HTCC_t, e_HTCC_nphe, e_HTCC_path, e_HTCC_vt;
			Vector3 pos = new Vector3(e_HTCC_X,e_HTCC_Y,e_HTCC_Z);pos.rotateZ(-s*Math.toRadians(60));
			H_e_HTCC_vt_theta[s].fill(e_the,e_HTCC_vt);
			H_e_HTCC_nphe_theta[s].fill(e_the,e_HTCC_nphe);
			H_e_HTCC_XY[s].fill(pos.x(),pos.y());
		}
		if(found_eTraj && found_eHTCC){
			H_e_HTCC.fill(e_HTCC_tX,e_HTCC_tY);
			H_e_nphe_HTCC.fill(e_HTCC_tX,e_HTCC_tY,e_HTCC_nphe);
			H_e_bin_theta_HTCC.fill(e_HTCC_tX,e_HTCC_tY,e_HTCC_bin_theta+0.5f);
			H_e_bin_phi_HTCC.fill(e_HTCC_tX,e_HTCC_tY,e_HTCC_bin_phi+0.5f);
			H_e_theta_HTCC.fill(e_HTCC_tX,e_HTCC_tY,e_HTCC_theta);
			H_e_phi_HTCC.fill(e_HTCC_tX,e_HTCC_tY,e_HTCC_phi);
			if(true
			  && e_HTCC_bin_theta>-1 && e_HTCC_bin_theta<15
			  && e_HTCC_bin_phi>-1 && e_HTCC_bin_phi<30
			  )
			H_e_bin_nphe_HTCC[s][e_HTCC_bin_theta][e_HTCC_bin_phi].fill(e_HTCC_nphe);
			for(int ic=0;ic<10;ic++){
				if(e_HTCC_nphe>ic+1)H_e_HTCC_cut[ic].fill(e_HTCC_tX,e_HTCC_tY);
			}
		}

		if(selectElastic()){
			H_elast_e_th_p.fill(e_mom,e_the);
			H_elast_p_th_p.fill(prot_mom,prot_the);
			H_elast_vz_vz.fill(e_vz,prot_vz);
			H_elast_dvz_phi.fill(prot_phi,prot_vz-e_vz);
			H_elast_dvz_theta_all.fill(prot_the,prot_vz-e_vz);
			H_elast_dvz_theta[s].fill(prot_the,prot_vz-e_vz);
			H_elast_dvz_vz.fill(prot_vz,prot_vz-e_vz);
			H_elast_Dphi_phi.fill(prot_phi,elast_dPhi);
			H_elast_Dphi_theta.fill(prot_the,elast_dPhi);
			H_elast_Dphi_vz.fill(prot_vz,elast_dPhi);
			H_elast_EB_phi.fill(prot_phi,elast_EB);
			H_elast_EB_theta.fill(prot_the,elast_EB);
			H_elast_EB_vz.fill(prot_vz,elast_EB);
			H_elast_W_Q2[s].fill(e_W,e_Q2);
			H_elast_W_theta[s].fill(e_W,e_the);
		}
		
		if(select_epip()){
			H_epip_e_th_p.fill(e_mom,e_the);
			H_epip_p_th_p.fill(pip_mom,pip_the);
			H_epip_vz_vz.fill(e_vz,pip_vz);
			H_epip_dvz_phi.fill(pip_phi,pip_vz-e_vz);
			H_epip_dvz_theta.fill(pip_the,pip_vz-e_vz);
			H_epip_dvz_vz.fill(pip_vz,pip_vz-e_vz);
			H_epip_Dphi_phi.fill(pip_phi,epip_dPhi);
			H_epip_Dphi_theta.fill(pip_the,epip_dPhi);
			H_epip_Dphi_vz.fill(pip_vz,epip_dPhi);
			H_epip_beta_p.fill(pip_mom,pip_beta);
			//H_epip_W_theta[s].fill(e_W,e_the);
			H_epip_W_theta[s].fill(epip_MM,e_the);
			if(found_eFTOF1b && pip_FTOF_pad1b>-1){
				H_epip_FTOF1b_dt_epad.fill(    e_FTOF_pad1b, pip_FTOF1b_vt - e_FTOF1b_vt);
				H_epip_FTOF1b_dt_pippad.fill(pip_FTOF_pad1b, pip_FTOF1b_vt - e_FTOF1b_vt);
			}
		}

		if(select_epi0()){
			H_pi0_G1_layers.fill(G1_cal_layers);
			H_pi0_G2_layers.fill(G2_cal_layers);
			H_pi0_G1_XY.fill(G1_pcal_X,G1_pcal_Y);
			//H_pi0_G1_TR.fill(G1_pcal_R/29.92,G1_pcal_t-STT);
			H_pi0_G1_TR.fill(e_PCAL_t,G1_pcal_t);
			H_pi0_G1_vt_evt.fill(e_FTOF1b_vt,G1_pcal_vt);
			H_pi0_G1_layer_E.fill(G1_cal_layers,G1_mom);
			H_pi0_G2_XY.fill(G2_pcal_X,G2_pcal_Y);
			//H_pi0_G2_TR.fill(G2_pcal_R/29.92,G2_pcal_t-STT);
			H_pi0_G2_TR.fill(e_PCAL_t,G2_pcal_t);
			H_pi0_G2_vt_evt.fill(e_FTOF1b_vt,G2_pcal_vt);
			H_pi0_G2_layer_E.fill(G2_cal_layers,G2_mom);

			H_pi0_G1_mom_the.fill(G1_mom,G1_the);
			H_pi0_G1_phi_the.fill(G1_phi,G1_the);
			H_pi0_G2_mom_the.fill(G2_mom,G2_the);
			H_pi0_G2_phi_the.fill(G2_phi,G2_the);
			H_pi0_open_E.fill(pi0_E,pi0_open);
			H_pi0_E_the.fill(pi0_E,pi0_the);
			H_pi0_phi_the.fill(pi0_phi,pi0_the);
			H_pi0_mass.fill(pi0_mass);
		}
	}
	public void plot(){
		EmbeddedCanvas can_e_overview = new EmbeddedCanvas();
		can_e_overview.setSize(3600,1800);
		can_e_overview.divide(6,3);
		can_e_overview.setAxisTitleSize(24);
		can_e_overview.setAxisFontSize(24);
		can_e_overview.setTitleSize(24);
		can_e_overview.cd(0);can_e_overview.draw(H_e_t_f);
		can_e_overview.cd(1);can_e_overview.draw(H_e_p_f);
		can_e_overview.cd(2);can_e_overview.draw(H_e_vz_f);
		can_e_overview.cd(3);can_e_overview.draw(H_e_vt_vz);
		can_e_overview.cd(4);can_e_overview.draw(H_e_vt_p);
		can_e_overview.cd(5);can_e_overview.draw(H_e_vt_t);
		can_e_overview.cd(6);can_e_overview.draw(H_e_DCSL6);
		can_e_overview.cd(7);can_e_overview.draw(H_e_DCSL5);
		can_e_overview.cd(8);can_e_overview.draw(H_e_DCSL4);
		can_e_overview.cd(9);can_e_overview.draw(H_e_DCSL3);
		can_e_overview.cd(10);can_e_overview.draw(H_e_DCSL2);
		can_e_overview.cd(11);can_e_overview.draw(H_e_DCSL1);
		can_e_overview.cd(12);can_e_overview.draw(H_e_PCAL);
		can_e_overview.cd(13);can_e_overview.draw(H_e_FTOF);
		can_e_overview.cd(14);can_e_overview.draw(H_e_LTCC);
		can_e_overview.cd(15);can_e_overview.draw(H_e_HTCC);
		H_e_nphe_HTCC.divide(H_e_HTCC);
		can_e_overview.cd(16);can_e_overview.draw(H_e_nphe_HTCC);
		can_e_overview.getPad(16).getAxisZ().setRange(5,25);
                can_e_overview.save(String.format(outputDir+"/dst_e_overview.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_e_overview.png"));

		H_e_bin_theta_HTCC.divide(H_e_HTCC);
		H_e_bin_phi_HTCC.divide(H_e_HTCC);
		H_e_theta_HTCC.divide(H_e_HTCC);
		H_e_phi_HTCC.divide(H_e_HTCC);

		EmbeddedCanvas can_e_htcc  = new EmbeddedCanvas();
		can_e_htcc.setSize(1800,1200);
		can_e_htcc.divide(3,2);
		can_e_htcc.setAxisTitleSize(24);
		can_e_htcc.setAxisFontSize(24);
		can_e_htcc.setTitleSize(24);
		can_e_htcc.cd(0);can_e_htcc.draw(H_e_HTCC);
		can_e_htcc.cd(1);can_e_htcc.draw(H_e_theta_HTCC);
		can_e_htcc.cd(2);can_e_htcc.draw(H_e_phi_HTCC);
		can_e_htcc.cd(3);can_e_htcc.draw(H_e_nphe_HTCC);
		can_e_htcc.getPad(3).getAxisZ().setRange(5,25);
		can_e_htcc.cd(4);can_e_htcc.draw(H_e_bin_theta_HTCC);
		can_e_htcc.cd(5);can_e_htcc.draw(H_e_bin_phi_HTCC);
                can_e_htcc.save(String.format(outputDir+"/dst_e_HTCC.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_e_HTCC.png"));
		
		for(int ic=0;ic<10;ic++)H_e_HTCC_cut[ic].divide(H_e_HTCC);
		EmbeddedCanvas can_e_htcc_cut = new EmbeddedCanvas();
		can_e_htcc_cut.setSize(3000,1200);
		can_e_htcc_cut.divide(5,2);
		can_e_htcc_cut.setAxisTitleSize(20);
		can_e_htcc_cut.setAxisFontSize(18);
		can_e_htcc_cut.setTitleSize(18);
		for(int ic=0;ic<10;ic++){
			can_e_htcc_cut.cd(ic);can_e_htcc_cut.draw(H_e_HTCC_cut[ic]);
			can_e_htcc_cut.getPad(ic).getAxisZ().setRange(1 - 0.5 *(ic+1)/10 ,1.0);
		}
                can_e_htcc_cut.save(String.format(outputDir+"/dst_e_HTCC_cut.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_e_HTCC_cut.png"));


		for(int s=0;s<6;s++){
			EmbeddedCanvas can_e_htcc_spectr  = new EmbeddedCanvas();
			can_e_htcc_spectr.setSize(14562,4500);
			can_e_htcc_spectr.divide(30,15);
			can_e_htcc_spectr.setAxisTitleSize(24);
			can_e_htcc_spectr.setAxisFontSize(24);
			can_e_htcc_spectr.setTitleSize(24);
			for(int it=0;it<15;it++)for(int ip=0;ip<30;ip++)if(H_e_bin_nphe_HTCC[s][it][ip].getIntegral()>50){
				int pad  = ip + 30*it;
				can_e_htcc_spectr.cd(pad);can_e_htcc_spectr.draw(H_e_bin_nphe_HTCC[s][it][ip]);
			}
                        can_e_htcc_spectr.save(String.format(outputDir+"/dst_e_HTCC_sect"+(s+1)+".png"));
                        System.out.println(String.format("saved "+outputDir+"/dst_e_HTCC_sect"+(s+1)+".png"));
		}

		EmbeddedCanvas can_e_sectors = new EmbeddedCanvas();
		can_e_sectors.setSize(3600,1800);
		can_e_sectors.divide(6,3);
		can_e_sectors.setAxisTitleSize(24);
		can_e_sectors.setAxisFontSize(24);
		can_e_sectors.setTitleSize(24);
		for(int s=0;s<6;s++){
			can_e_sectors.cd(s);can_e_sectors.draw(H_e_t_p[s]);
			can_e_sectors.cd(s+6);can_e_sectors.draw(H_e_vz_t[s]);
			can_e_sectors.cd(s+12);can_e_sectors.draw(H_e_vz_p[s]);
		}
                can_e_sectors.save(String.format(outputDir+"/dst_e_sectors.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_e_sectors.png"));

		EmbeddedCanvas can_e_ECAL = new EmbeddedCanvas();
		can_e_ECAL.setSize(3600,1800);
		can_e_ECAL.divide(6,3);
		can_e_ECAL.setAxisTitleSize(24);
		can_e_ECAL.setAxisFontSize(24);
		can_e_ECAL.setTitleSize(24);
		for(int s=0;s<6;s++){
			can_e_ECAL.cd(s);can_e_ECAL.draw(H_e_EC_etot_p[s]);
			can_e_ECAL.cd(s+6);can_e_ECAL.draw(H_e_EC_vt_theta[s]);
			can_e_ECAL.cd(s+12);can_e_ECAL.draw(H_e_EC_XY[s]);
		}
                can_e_ECAL.save(String.format(outputDir+"/dst_e_ecal.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_e_ecal.png"));

		EmbeddedCanvas can_e_FTOF = new EmbeddedCanvas();
		can_e_FTOF.setSize(3600,3600);
		can_e_FTOF.divide(6,6);
		can_e_FTOF.setAxisTitleSize(24);
		can_e_FTOF.setAxisFontSize(24);
		can_e_FTOF.setTitleSize(24);
		for(int s=0;s<6;s++){
			can_e_FTOF.cd(s);can_e_FTOF.draw(H_e_FTOF_vt_pad1a[s]);
			can_e_FTOF.cd(s+6);can_e_FTOF.draw(H_e_FTOF_vt_pad1b[s]);
			can_e_FTOF.cd(s+12);can_e_FTOF.draw(H_e_FTOF_edep_pad1a[s]);
			can_e_FTOF.cd(s+18);can_e_FTOF.draw(H_e_FTOF_edep_pad1b[s]);
			can_e_FTOF.cd(s+24);can_e_FTOF.draw(H_e_FTOF_XY_pad1a[s]);
			can_e_FTOF.cd(s+30);can_e_FTOF.draw(H_e_FTOF_XY_pad1b[s]);
		}
                can_e_FTOF.save(String.format(outputDir+"/dst_e_FTOF.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_e_FTOF.png"));

		EmbeddedCanvas can_e_FTOF1A_mass = new EmbeddedCanvas();
		can_e_FTOF1A_mass.setSize(3600,4800);
		can_e_FTOF1A_mass.divide(6,6);
		can_e_FTOF1A_mass.setAxisTitleSize(24);
		can_e_FTOF1A_mass.setAxisFontSize(24);
		can_e_FTOF1A_mass.setTitleSize(24);
		for(int s=0;s<6;s++){
			H1F H_FTOF_pos_mass_mom_pad1a_projY = H_FTOF_pos_mass_mom_pad1a[s].projectionY();
			H_FTOF_pos_mass_mom_pad1a_projY.setTitle(String.format("POS TOF1A mass S%d",s+1));
			H_FTOF_pos_mass_mom_pad1a_projY.setTitleX("M (GeV)");
			H1F H_FTOF_neg_mass_mom_pad1a_projY = H_FTOF_neg_mass_mom_pad1a[s].projectionY();
			H_FTOF_neg_mass_mom_pad1a_projY.setTitle(String.format("NEG TOF1A mass S%d",s+1));
			H_FTOF_neg_mass_mom_pad1a_projY.setTitleX("M (GeV)");
			can_e_FTOF1A_mass.cd(s);can_e_FTOF1A_mass.getPad(s).getAxisY().setLog(true);
			can_e_FTOF1A_mass.cd(s);can_e_FTOF1A_mass.draw(H_FTOF_pos_mass_mom_pad1a_projY);
			can_e_FTOF1A_mass.cd(s+6);can_e_FTOF1A_mass.draw(H_FTOF_pos_mass_mom_pad1a[s]);
			can_e_FTOF1A_mass.cd(s+12);can_e_FTOF1A_mass.draw(H_FTOF_pos_beta_mom_pad1a[s]);
			can_e_FTOF1A_mass.cd(s+18);can_e_FTOF1A_mass.draw(H_FTOF_neg_mass_mom_pad1a_projY);
			can_e_FTOF1A_mass.cd(s+24);can_e_FTOF1A_mass.draw(H_FTOF_neg_mass_mom_pad1a[s]);
			can_e_FTOF1A_mass.cd(s+30);can_e_FTOF1A_mass.draw(H_FTOF_neg_beta_mom_pad1a[s]);
		}
                can_e_FTOF1A_mass.save(String.format(outputDir+"/dst_FTOF1A_mass.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_FTOF1A_mass.png"));

		EmbeddedCanvas can_e_FTOF1B_mass = new EmbeddedCanvas();
		can_e_FTOF1B_mass.setSize(3600,4800);
		can_e_FTOF1B_mass.divide(6,6);
		can_e_FTOF1B_mass.setAxisTitleSize(24);
		can_e_FTOF1B_mass.setAxisFontSize(24);
		can_e_FTOF1B_mass.setTitleSize(24);
		for(int s=0;s<6;s++){
			H1F H_FTOF_pos_mass_mom_pad1b_projY = H_FTOF_pos_mass_mom_pad1b[s].projectionY();
			H_FTOF_pos_mass_mom_pad1b_projY.setTitle(String.format("POS TOF1B mass^2 S%d",s+1));
			H_FTOF_pos_mass_mom_pad1b_projY.setTitleX("M^2 (GeV^2)");
			H1F H_FTOF_neg_mass_mom_pad1b_projY = H_FTOF_neg_mass_mom_pad1b[s].projectionY();
			H_FTOF_neg_mass_mom_pad1b_projY.setTitle(String.format("NEG TOF1B mass^2 S%d",s+1));
			H_FTOF_neg_mass_mom_pad1b_projY.setTitleX("M^2 (GeV^2)");
			can_e_FTOF1B_mass.cd(s);can_e_FTOF1B_mass.getPad(s).getAxisY().setLog(true);
			can_e_FTOF1B_mass.cd(s);can_e_FTOF1B_mass.draw(H_FTOF_pos_mass_mom_pad1b_projY);
			can_e_FTOF1B_mass.cd(s+6);can_e_FTOF1B_mass.draw(H_FTOF_pos_mass_mom_pad1b[s]);
			can_e_FTOF1B_mass.cd(s+12);can_e_FTOF1B_mass.draw(H_FTOF_pos_beta_mom_pad1b[s]);
			can_e_FTOF1B_mass.cd(s+18);can_e_FTOF1B_mass.draw(H_FTOF_neg_mass_mom_pad1b_projY);
			can_e_FTOF1B_mass.cd(s+24);can_e_FTOF1B_mass.draw(H_FTOF_neg_mass_mom_pad1b[s]);
			can_e_FTOF1B_mass.cd(s+30);can_e_FTOF1B_mass.draw(H_FTOF_neg_beta_mom_pad1b[s]);
		}
                can_e_FTOF1B_mass.save(String.format(outputDir+"/dst_FTOF1B_mass.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_FTOF1B_mass.png"));

		EmbeddedCanvas can_e_Cerenkov = new EmbeddedCanvas();
		can_e_Cerenkov.setSize(3600,3600);
		can_e_Cerenkov.divide(6,6);
		can_e_Cerenkov.setAxisTitleSize(24);
		can_e_Cerenkov.setAxisFontSize(24);
		can_e_Cerenkov.setTitleSize(24);
		for(int s=0;s<6;s++){
			can_e_Cerenkov.cd(s);can_e_Cerenkov.draw(H_e_LTCC_vt_theta[s]);
			can_e_Cerenkov.cd(s+6);can_e_Cerenkov.draw(H_e_LTCC_nphe_theta[s]);
			can_e_Cerenkov.cd(s+12);can_e_Cerenkov.draw(H_e_LTCC_XY[s]);
			can_e_Cerenkov.cd(s+18);can_e_Cerenkov.draw(H_e_HTCC_vt_theta[s]);
			can_e_Cerenkov.cd(s+24);can_e_Cerenkov.draw(H_e_HTCC_nphe_theta[s]);
			can_e_Cerenkov.cd(s+30);can_e_Cerenkov.draw(H_e_HTCC_XY[s]);
		}
                can_e_Cerenkov.save(String.format(outputDir+"/dst_e_Cerenkov.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_e_Cerenkov.png"));

		EmbeddedCanvas can_elastic = new EmbeddedCanvas();
		can_elastic.setSize(3600,3600);
		can_elastic.divide(6,7);
		can_elastic.setAxisTitleSize(24);
		can_elastic.setAxisFontSize(24);
		can_elastic.setTitleSize(24);
		can_elastic.cd(0);can_elastic.draw(H_elast_e_th_p);
		can_elastic.cd(1);can_elastic.draw(H_elast_p_th_p);
		can_elastic.cd(2);can_elastic.draw(H_elast_vz_vz);
		can_elastic.cd(3);can_elastic.draw(H_elast_dvz_phi);
		can_elastic.cd(4);can_elastic.draw(H_elast_dvz_theta_all);
		can_elastic.cd(5);can_elastic.draw(H_elast_dvz_vz);
		can_elastic.cd(6);can_elastic.draw(H_elast_Dphi_phi);
		can_elastic.cd(7);can_elastic.draw(H_elast_Dphi_theta);
		can_elastic.cd(8);can_elastic.draw(H_elast_Dphi_vz);
		can_elastic.cd(9);can_elastic.draw(H_elast_EB_phi);
		can_elastic.cd(10);can_elastic.draw(H_elast_EB_theta);
		can_elastic.cd(11);can_elastic.draw(H_elast_EB_vz);
		for(int s=0;s<6;s++){
			can_elastic.cd(12+s);can_elastic.draw(H_elast_W_Q2[s]);
			can_elastic.getPad(12+s).getAxisX().setRange(0.5,2.5);
			can_elastic.cd(18+s);can_elastic.draw(H_elast_W_theta[s]);
			can_elastic.getPad(18+s).getAxisX().setRange(0.5,2.5);
			can_elastic.cd(24+s);can_elastic.draw(H_elast_inc_W_theta[s].projectionX());
			can_elastic.cd(24+s);can_elastic.draw(H_elast_W_theta[s].projectionX(),"same");
			can_elastic.getPad(24+s).getAxisX().setRange(0.5,2.5);
			//can_elastic.getPad(18+s).getAxisY().setLog(true);
			can_elastic.cd(30+s);can_elastic.draw(H_elast_W_theta[s].projectionX());
			can_elastic.getPad(30+s).getAxisX().setRange(0.5,2.5);
			can_elastic.cd(36+s);can_elastic.draw(H_elast_inc_W_theta[s]);
			can_elastic.getPad(36+s).getAxisX().setRange(0.5,2.5);
		}
                can_elastic.save(String.format(outputDir+"/dst_elastic.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_elastic.png"));

		EmbeddedCanvas can_epip = new EmbeddedCanvas();
		can_epip.setSize(3600,2400);
		can_epip.divide(6,4);
		can_epip.setAxisTitleSize(24);
		can_epip.setAxisFontSize(24);
		can_epip.setTitleSize(24);
		can_epip.cd(0);can_epip.draw(H_epip_e_th_p);
		can_epip.cd(1);can_epip.draw(H_epip_p_th_p);
		can_epip.cd(2);can_epip.draw(H_epip_vz_vz);
		can_epip.cd(3);can_epip.draw(H_epip_dvz_phi);
		can_epip.cd(4);can_epip.draw(H_epip_dvz_theta);
		can_epip.cd(5);can_epip.draw(H_epip_dvz_vz);
		can_epip.cd(6);can_epip.draw(H_epip_Dphi_phi);
		can_epip.cd(7);can_epip.draw(H_epip_Dphi_theta);
		can_epip.cd(8);can_epip.draw(H_epip_Dphi_vz);
		can_epip.cd(9);can_epip.draw(H_epip_beta_p);
		can_epip.cd(10);can_epip.draw(H_epip_FTOF1b_dt_epad);
		can_epip.cd(11);can_epip.draw(H_epip_FTOF1b_dt_pippad);
		for(int s=0;s<6;s++){
			can_epip.cd(12+s);can_epip.draw(H_epip_W_theta[s]);
			can_epip.cd(18+s);can_epip.draw(H_epip_W_theta[s].projectionX());
			can_epip.getPad(18+s).getAxisX().setRange(0.25,3.25);
		}
                can_epip.save(String.format(outputDir+"/dst_epip.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_epip.png"));

		EmbeddedCanvas can_epi0 = new EmbeddedCanvas();
		can_epi0.setSize(2400,1200);
		can_epi0.divide(6,3);
		can_epi0.setAxisTitleSize(24);
		can_epi0.setAxisFontSize(24);
		can_epi0.setTitleSize(24);
		can_epi0.cd(0);can_epi0.draw(H_pi0_G1_mom_the);
		can_epi0.cd(1);can_epi0.draw(H_pi0_G1_phi_the);
		can_epi0.cd(2);can_epi0.draw(H_pi0_G1_XY);
		can_epi0.cd(3);can_epi0.draw(H_pi0_G1_TR);
		can_epi0.cd(4);can_epi0.draw(H_pi0_G1_vt_evt);
		can_epi0.cd(5);can_epi0.draw(H_pi0_G1_layer_E);
		can_epi0.cd(6);can_epi0.draw(H_pi0_G2_mom_the);
		can_epi0.cd(7);can_epi0.draw(H_pi0_G2_phi_the);
		can_epi0.cd(8);can_epi0.draw(H_pi0_G2_XY);
		can_epi0.cd(9);can_epi0.draw(H_pi0_G2_TR);
		can_epi0.cd(10);can_epi0.draw(H_pi0_G2_vt_evt);
		can_epi0.cd(11);can_epi0.draw(H_pi0_G2_layer_E);
		can_epi0.cd(12);can_epi0.draw(H_pi0_G1_layers);
		can_epi0.cd(13);can_epi0.draw(H_pi0_G2_layers);
		can_epi0.cd(14);can_epi0.draw(H_pi0_open_E);
		can_epi0.cd(15);can_epi0.draw(H_pi0_E_the);
		can_epi0.cd(16);can_epi0.draw(H_pi0_phi_the);
		can_epi0.cd(17);can_epi0.draw(H_pi0_mass);
                can_epi0.save(String.format(outputDir+"/dst_epi0.png"));
                System.out.println(String.format("saved "+outputDir+"/dst_epi0.png"));
	}
	
	public void write(){
        TDirectory dirout = new TDirectory();
        
        dirout.mkdir("/HTCC/");
        dirout.cd("/HTCC/");
		dirout.addDataSet(H_e_HTCC , H_e_nphe_HTCC, H_e_bin_theta_HTCC, H_e_bin_phi_HTCC, H_e_theta_HTCC, H_e_phi_HTCC);
		for(int ic=0;ic<10;ic++)dirout.addDataSet(H_e_HTCC_cut[ic]);
        for(int s=0;s<6;s++)for(int it=0;it<15;it++)for(int ip=0;ip<30;ip++)dirout.addDataSet(H_e_bin_nphe_HTCC[s][it][ip]);
        
		dirout.mkdir("/elastic/");
		dirout.cd("/elastic/");
		for(int s=0;s<6;s++){
			dirout.addDataSet(H_elast_W_theta[s],H_elast_W_Q2[s],H_elast_dvz_theta[s]);
		}
		dirout.addDataSet(H_elast_Dphi_phi,H_elast_dvz_vz,H_elast_dvz_theta_all);
		
		dirout.mkdir("/FTOF/");
		dirout.cd("/FTOF/");
		for(int s=0;s<6;s++){
			dirout.addDataSet(H_FTOF_pos_mass_mom_pad1a[s], H_FTOF_neg_mass_mom_pad1a[s], H_FTOF_pos_mass_mom_pad1b[s], H_FTOF_neg_mass_mom_pad1b[s]);
			dirout.addDataSet(p1a_pad_vt_elec[s],p1a_pad_vt_pion[s],p1b_pad_vt_elec[s],p1b_pad_vt_pion[s],p2_pad_vt[s]);
			dirout.addDataSet(p1a_pad_edep_elec[s],p1a_pad_edep_pion[s],p1b_pad_edep_elec[s],p1b_pad_edep_pion[s],p2_pad_edep[s]);
		}

		dirout.mkdir("/ECAL/");
		dirout.cd("/ECAL/");
		for(int s=0;s<6;s++){
			dirout.addDataSet(H_e_EC_vt_theta[s]);
		}
//		dirout.addDataSet(H_pi0_mass,H_pi0_G1_vt_evt,H_pi0_G2_vt_evt);
		
		if(runNum>0)dirout.writeFile(outputDir+"/dst_mon_"+runNum+".hipo");
               else dirout.writeFile(outputDir+"/dst_mon.hipo");
	}

////////////////////////////////////////////////
        public static void main(String[] args) {
                System.setProperty("java.awt.headless", "true");
                GStyle.setPalette("kRainBow");
                int count = 0;
                int runNum = 0;
                int maxevents = 500000;
                float EB = 7;
		String filelist = "list_of_files.txt";
                if(args.length>0)runNum=Integer.parseInt(args[0]);
		if(args.length>1)filelist = args[1];
                if(args.length>2)maxevents=Integer.parseInt(args[2]);
                if(args.length>3)EB=Float.parseFloat(args[3]);
                String outputDir = runNum > 0 ? "plots"+runNum : "plots";
		dst_mon ana = new dst_mon(runNum,outputDir,EB);
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

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                long startTime = System.currentTimeMillis();
                long previousTime = System.currentTimeMillis();
                for (String runstrg : toProcessFileNames) if(count<maxevents){
                        progresscount++;
                        System.out.println(String.format(">>>>>>>>>>>>>>>> dst_mon %s",runstrg));
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
                                        long nowTime = System.currentTimeMillis();
                                        long elapsedTime = nowTime - previousTime;
                                        long totalTime = nowTime - startTime;
                                        elapsedTime = elapsedTime/1000;
                                        totalTime = totalTime/1000;
                                        Date date = new Date();
					String TimeString = "          time : " + dateFormat.format(date) + " , last elapsed : " + elapsedTime + "s ; total elapsed : " + totalTime + "s";
                                        int ntrigs = ana.getNtrigs();
                                        int nelecs = ana.getNelecs();
                                        float ratio = 0f;if(ntrigs>0)ratio=100f*nelecs/ntrigs;
                                        String diagnost = String.format("N elecs=%dk N trigs=%dk , r=%1.2f%% ; file %d/%d",nelecs/1000,ntrigs/1000,ratio,progresscount,filetot);
                                        //System.out.println(count/1000 + "k events, file "+(fileN+1)+" (dst_mon running on "+runstrg+") "+diagnost);
					//String purity = "                               purity per sector : ";
					String purity = " >>>>>>>>>>>>>>>>>>>  purity per sector : ";
					for(int s=0;s<6;s++){
						int T = ana.Ntrigs_sect[s];
						int E = ana.Nelecs_sect[s];
						float R = 0;
						if(T>0)R=(100.0f*E)/(1.0f*T);
						//purity += "S"+(s+1)+" "+((T/1000))+"k , "+(E/1000)+"k r="+String.format("%1.1f",R)+"  ;  ";
						purity += "S"+(s+1)+" ="+String.format("%1.1f",R)+"%   ";
					}
                                        System.out.println(count/1000 + "k evts, (dst_mon on "+runstrg+") "+diagnost);
					System.out.println(TimeString+purity);
					//System.out.println(purity);
					previousTime = nowTime;
                                }
                        }
                        reader.close();
                }
                System.out.println("Total : " + count + " events");
                ana.plot();
		ana.write();
        }
}

