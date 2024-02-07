package org.jlab.clas12.monitoring;

import java.io.*;
import java.util.*;
import org.jlab.clas.pdg.PhysicsConstants;

import javax.xml.parsers.DocumentBuilder;

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

public class tof_monitor {
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
	public H2F p1a_pad_occ, p1b_pad_occ, p2_pad_occ;
	public H2F p1a_pad_XY, p1b_pad_XY, p2_pad_XY;
	public H2F[] p1a_pad_vt, p1b_pad_vt, p2_pad_vt;
	public H2F[] p1a_pad_edep, p1b_pad_edep, p2_pad_edep;
	public H1F[][] p1a_edep, p1b_edep;
	public H1F[] p2_edep, p1a_tdcadc_dt, p1b_tdcadc_dt, p2_tdcadc_dt;
	public H1F[] ftof_ctof_vtdiff;
	public H2F[] p1a_pad_dt, p1b_pad_dt, p2_pad_dt;
	public H2F[] p1a_pad_dt_calib, p1b_pad_dt_calib, p2_pad_dt_calib, p1a_pad_dt_4nstrack, p1b_pad_dt_4nstrack;
	public H1F[] p1a_dt_calib_all, p1b_dt_calib_all, p2_dt_calib_all, p1a_dt_4nstrack_all, p1b_dt_4nstrack_all;
	public H2F[][] DC_residuals_trkDoca;
	public H1F[][] DC_residuals, DC_time, DC_time_even, DC_time_odd;
	public H2F[][] DC_residuals_trkDoca_nocut;
	public H1F[][] DC_residuals_nocut, DC_time_nocut;
	public H2F[][] DC_residuals_trkDoca_rescut;
	public H1F[][] DC_residuals_rescut, DC_time_rescut;
    public H2F DC_jitterzero_sec_sl, DC_jitterone_sec_sl, DC_jittertwo_sec_sl;
    public H2F DC_hits_even_ts_sec_sl, DC_hits_odd_ts_sec_sl;
    public H1F DC_jitterdist;
	public F1D[][] f_time_invertedS;

	public float p1a_counter_thickness, p1b_counter_thickness, p2_counter_thickness;
	public int phase_offset;
	public long timestamp;

	public IndexedTable InverseTranslationTable;
	public IndexedTable calibrationTranslationTable;
	public IndexedTable rfTable, rfTableOffset;
	public IndexedTable ftofTable, ctofTable;
	public ConstantsManager ccdb;

	public tof_monitor(int reqrunNum, String reqOutputDir, boolean reqTimeBased) {
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

		p1a_pad_occ = new H2F("p1a_pad_occ","p1a_pad_occ",25,0,25,6,0.5,6.5);
		p1a_pad_occ.setTitle("p1a occupancies");
		p1a_pad_occ.setTitleX("paddle");
		p1a_pad_occ.setTitleY("sector");
		p1b_pad_occ = new H2F("p1b_pad_occ","p1b_pad_occ",65,0,65,6,0.5,6.5);
		p1b_pad_occ.setTitle("p1b occupancies");
		p1b_pad_occ.setTitleX("paddle");
		p1b_pad_occ.setTitleY("sector");
		p2_pad_occ = new H2F("p2_pad_occ","p2_pad_occ",5,1,6,6,0.5,6.5);
		p2_pad_occ.setTitle("p2 occupancies");
		p2_pad_occ.setTitleX("paddle");
		p2_pad_occ.setTitleY("sector");
		p1a_pad_XY = new H2F("p1a_pad_XY","p1a_pad_XY",100,-500,500,100,-500,500);
		p1a_pad_XY.setTitle("p1a position");
		p1a_pad_XY.setTitleX("X (cm)");
		p1a_pad_XY.setTitleY("Y (cm)");
		p1b_pad_XY = new H2F("p1b_pad_XY","p1b_pad_XY",100,-500,500,100,-500,500);
		p1b_pad_XY.setTitle("p1b position");
		p1b_pad_XY.setTitleX("X (cm)");
		p1b_pad_XY.setTitleY("Y (cm)");
		p2_pad_XY = new H2F("p2_pad_XY","p2_pad_XY",100,-500,500,100,-500,500);
		p2_pad_XY.setTitle("p2 position");
		p2_pad_XY.setTitleX("X (cm)");
		p2_pad_XY.setTitleY("Y (cm)");

		p1a_pad_vt = new H2F[6];
		p1b_pad_vt = new H2F[6];
		p2_pad_vt = new H2F[6];
		p1a_pad_edep = new H2F[6];
		p1b_pad_edep = new H2F[6];
		p2_pad_edep = new H2F[6];
		p1a_pad_dt = new H2F[6];
		p1b_pad_dt = new H2F[6];
		p2_pad_dt = new H2F[6];
		p1a_pad_dt_calib = new H2F[6];
		p1b_pad_dt_calib = new H2F[6];
		p1a_pad_dt_4nstrack = new H2F[6];
                p1b_pad_dt_4nstrack = new H2F[6];
		p2_pad_dt_calib = new H2F[6];
		p1a_dt_calib_all = new H1F[6];
		p1b_dt_calib_all = new H1F[6];
		p1a_dt_4nstrack_all = new H1F[6];
                p1b_dt_4nstrack_all = new H1F[6];
		p2_dt_calib_all = new H1F[6];
		p1a_edep = new H1F[6][3];
		p1b_edep = new H1F[6][3];
		p2_edep = new H1F[6];
		p1b_tdcadc_dt = new H1F[6];
		p1a_tdcadc_dt = new H1F[6];
		p2_tdcadc_dt = new H1F[6];
		DC_residuals_trkDoca = new H2F[6][6];
		DC_residuals = new H1F[6][6];
		DC_residuals_trkDoca_rescut = new H2F[6][6];
		DC_residuals_rescut = new H1F[6][6];
		DC_residuals_trkDoca_nocut = new H2F[6][6];
		DC_residuals_nocut = new H1F[6][6];
		DC_time = new H1F[6][6];
		DC_time_even = new H1F[6][6];
		DC_time_odd = new H1F[6][6];
		DC_time_rescut = new H1F[6][6];
		DC_time_nocut = new H1F[6][6];
		//DC jitter histograms, distributions of hits for jitter 0, 1 and 2 and for even and odd event timestamps for each sector/Superlayer
		DC_jitterzero_sec_sl = new H2F("DC_jitterzero_sec_sl","DC_jitterzero_sec_sl",6,0.5,6.5,6,0.5,6.5);
		DC_jitterzero_sec_sl.setTitle("jitter 0 hits per sector and sl");
		DC_jitterzero_sec_sl.setTitleX("sector");
		DC_jitterzero_sec_sl.setTitleY("superlayer");
		DC_jitterone_sec_sl = new H2F("DC_jitterone_sec_sl","DC_jitterone_sec_sl",6,0.5,6.5,6,0.5,6.5);
		DC_jitterone_sec_sl.setTitle("jitter 1 hits per sector and sl");
		DC_jitterone_sec_sl.setTitleX("sector");
		DC_jitterone_sec_sl.setTitleY("superlayer");
		DC_jittertwo_sec_sl = new H2F("DC_jittertwo_sec_sl","DC_jittertwo_sec_sl",6,0.5,6.5,6,0.5,6.5);
		DC_jittertwo_sec_sl.setTitle("jitter 2 hits per sector and sl");
		DC_jittertwo_sec_sl.setTitleX("sector");
		DC_jittertwo_sec_sl.setTitleY("superlayer");
		DC_hits_even_ts_sec_sl = new H2F("DC_hits_even_ts_sec_sl","DC_hits_even_ts_sec_sl",6,0.5,6.5,6,0.5,6.5);
		DC_hits_even_ts_sec_sl.setTitle("hits per sector and sl for even time stamps");
		DC_hits_even_ts_sec_sl.setTitleX("sector");
		DC_hits_even_ts_sec_sl.setTitleY("superlayer");
		DC_hits_odd_ts_sec_sl = new H2F("DC_hits_odd_ts_sec_sl","DC_hits_odd_ts_sec_sl",6,0.5,6.5,6,0.5,6.5);
		DC_hits_odd_ts_sec_sl.setTitle("hits per sector and sl for odd time stamps");
		DC_hits_odd_ts_sec_sl.setTitleX("sector");
		DC_hits_odd_ts_sec_sl.setTitleY("superlayer");
		DC_jitterdist = new H1F("DC_jitterdist","DC_jitterdist",19,-9.5,9.5);
		DC_jitterdist.setTitle("Hits with different jitters");
		DC_jitterdist.setTitleX("jitter");

		f_time_invertedS = new F1D[6][6];

		ftof_ctof_vtdiff = new H1F[6];

		mean_tdiff = new float[6];
		nev = new int[6];

		for(int s=0;s<6;s++){

			mean_tdiff[s] = 0.f;
			nev[s] = 0;

			p1a_pad_vt[s] = new H2F(String.format("p1a_pad_vt_S%d",s+1),String.format("p1a_pad_vt_S%d",s+1),25,0,25,100,-rfPeriod/2,rfPeriod/2);
			p1a_pad_vt[s].setTitle(String.format("p1a S%d time",s+1));
			p1a_pad_vt[s].setTitleX("paddle");
			p1a_pad_vt[s].setTitleY("time");
			p1b_pad_vt[s] = new H2F(String.format("p1b_pad_vt_S%d",s+1),String.format("p1b_pad_vt_S%d",s+1),65,0,65,100,-rfPeriod/2,rfPeriod/2);
			p1b_pad_vt[s].setTitle(String.format("p1b S%d time",s+1));
			p1b_pad_vt[s].setTitleX("paddle");
			p1b_pad_vt[s].setTitleY("time");
			p2_pad_vt[s] = new H2F(String.format("p2_pad_vt_S%d",s+1),String.format("p2_pad_vt_S%d",s+1),5,1,6,100,-rfPeriod/2,rfPeriod/2);
			p2_pad_vt[s].setTitle(String.format("p2 S%d time",s+1));
			p2_pad_vt[s].setTitleX("paddle");
			p2_pad_vt[s].setTitleY("time");
			p1a_pad_edep[s] = new H2F(String.format("p1a_pad_edep_S%d",s+1),String.format("p1a_pad_edep_S%d",s+1),25,0,25,100,0,50);
			p1a_pad_edep[s].setTitle(String.format("p1a S%d energy",s+1));
			p1a_pad_edep[s].setTitleX("paddle");
			p1a_pad_edep[s].setTitleY("E (MeV)");
			p1b_pad_edep[s] = new H2F(String.format("p1b_pad_edep_S%d",s+1),String.format("p1b_pad_edep_S%d",s+1),65,0,65,100,0,50);
			p1b_pad_edep[s].setTitle(String.format("p1b S%d energy",s+1));
			p1b_pad_edep[s].setTitleX("paddle");
			p1b_pad_edep[s].setTitleY("E (MeV)");
			p2_pad_edep[s] = new H2F(String.format("p2_pad_edep_S%d",s+1),String.format("p2_pad_edep_S%d",s+1),5,1,6,100,0,50);
			p2_pad_edep[s].setTitle(String.format("p2 S%d energy",s+1));
			p2_pad_edep[s].setTitleX("paddle");
			p2_pad_edep[s].setTitleY("E (MeV)");
			p1a_pad_dt[s] = new H2F(String.format("p1a_pad_dt_S%d",s+1),String.format("p1a_pad_dt_S%d",s+1),25,0,25,100,-2.004,2.004);
			p1a_pad_dt[s].setTitle(String.format("p1a S%d #delta t",s+1));
			p1a_pad_dt[s].setTitleX("paddle");
			p1a_pad_dt[s].setTitleY("time");
			p1b_pad_dt[s] = new H2F(String.format("p1b_pad_dt_S%d",s+1),String.format("p1b_pad_dt_S%d",s+1),65,0,65,100,-2.004,2.004);
			p1b_pad_dt[s].setTitle(String.format("p1b S%d #delta t",s+1));
			p1b_pad_dt[s].setTitleX("paddle");
			p1b_pad_dt[s].setTitleY("time");
			p2_pad_dt[s] = new H2F(String.format("p2_pad_dt_S%d",s+1),String.format("p2_pad_dt_S%d",s+1),5,1,6,100,-12,12);
			p2_pad_dt[s].setTitle(String.format("p2 S%d #delta t",s+1));
			p2_pad_dt[s].setTitleX("paddle");
			p2_pad_dt[s].setTitleY("time");
//FTOF vertex time differences are to be plotted in 25-ps-wide bins
			p1a_pad_dt_calib[s] = new H2F(String.format("p1a_pad_dt_S%d",s+1),String.format("p1a_pad_dt_S%d",s+1),25,0,25,160,-2.000,2.000);
			p1a_pad_dt_calib[s].setTitle(String.format("p1a S%d FTOF vertex t - RFTime",s+1));
			p1a_pad_dt_calib[s].setTitleX("paddle");
			p1a_pad_dt_calib[s].setTitleY("FTOF vertex t - RFTime (ns)");
			p1b_pad_dt_calib[s] = new H2F(String.format("p1b_pad_dt_S%d",s+1),String.format("p1b_pad_dt_S%d",s+1),65,0,65,160,-2.000,2.000);
			p1b_pad_dt_calib[s].setTitle(String.format("p1b S%d FTOF vertex t - RFTime",s+1));
			p1b_pad_dt_calib[s].setTitleX("paddle");
			p1b_pad_dt_calib[s].setTitleY("FTOF vertex t - RFTime (ns)");
			p2_pad_dt_calib[s] = new H2F(String.format("p2_pad_dt_S%d",s+1),String.format("p2_pad_dt_S%d",s+1),5,1,6,960,-12,12);
			p2_pad_dt_calib[s].setTitle(String.format("p2 S%d FTOF vertex t - RFTime",s+1));
			p2_pad_dt_calib[s].setTitleX("paddle");
			p2_pad_dt_calib[s].setTitleY("FTOF vertex t - RFTime (ns)");
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
			p1a_pad_dt_4nstrack[s] = new H2F(String.format("p1a_pad_dt_notriggertrack_S%d",s+1),String.format("p1a_pad_dt_notriggertrack_S%d",s+1),25,0,25,160,-2.000,2.000);
                        p1a_pad_dt_4nstrack[s].setTitle(String.format("p1a S%d FTOF vertex t - RFTime, No trigger track",s+1));
                        p1a_pad_dt_4nstrack[s].setTitleX("paddle");
                        p1a_pad_dt_4nstrack[s].setTitleY("FTOF vertex t - RFTime (ns)");
                        p1b_pad_dt_4nstrack[s] = new H2F(String.format("p1b_pad_dt_notriggertrack_S%d",s+1),String.format("p1b_pad_dt_notriggertrack_S%d",s+1),65,0,65,160,-2.000,2.000);
                        p1b_pad_dt_4nstrack[s].setTitle(String.format("p1b S%d FTOF vertex t - RFTime, No trigger track",s+1));
                        p1b_pad_dt_4nstrack[s].setTitleX("paddle");
                        p1b_pad_dt_4nstrack[s].setTitleY("FTOF vertex t - RFTime (ns)");
			p1a_dt_4nstrack_all[s] = new H1F(String.format("p1a_dt_notriggertrack_S%d",s+1),String.format("p1a_dt_notriggertrack_S%d",s+1),160,-2.000,2.000);
                        p1a_dt_4nstrack_all[s].setTitle(String.format("p1a S%d FTOF vertex t - RFTime, No trigger track",s+1));
                        p1a_dt_4nstrack_all[s].setTitleX("FTOF vertex t - RFTime (ns)");
                        p1a_dt_4nstrack_all[s].setTitleY("counts");
                        p1b_dt_4nstrack_all[s] = new H1F(String.format("p1b_dt_notriggertrack_S%d",s+1),String.format("p1b_dt_notriggertrack_S%d",s+1),160,-2.000,2.000);
                        p1b_dt_4nstrack_all[s].setTitle(String.format("p1b S%d FTOF vertex t - RFTime, No trigger track",s+1));
                        p1b_dt_4nstrack_all[s].setTitleX("FTOF vertex t - RFTime (ns)");
                        p1b_dt_4nstrack_all[s].setTitleY("counts");

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
				DC_residuals_trkDoca_nocut[s][sl] = new H2F(String.format("DC_residuals_trkDoca_nocut_%d_%d",s+1,sl+1),String.format("DC_residuals_trkDoca_nocut_%d_%d",s+1,sl+1),100,0,DCcellsizeSL[sl],400,-0.5,0.5);
				DC_residuals_trkDoca_nocut[s][sl].setTitle(String.format("DC residuals S%d SL%d",s+1,sl+1));
				DC_residuals_trkDoca_nocut[s][sl].setTitleX("DOCA (cm)");
				DC_residuals_trkDoca_nocut[s][sl].setTitleY("residual (cm)");
				DC_residuals_trkDoca_rescut[s][sl] = new H2F(String.format("DC_residuals_trkDoca_rescut_%d_%d",s+1,sl+1),String.format("DC_residuals_trkDoca_rescut_%d_%d",s+1,sl+1),100,0,DCcellsizeSL[sl],400,-0.5,0.5);
				DC_residuals_trkDoca_rescut[s][sl].setTitle(String.format("DC residuals S%d SL%d",s+1,sl+1));
				DC_residuals_trkDoca_rescut[s][sl].setTitleX("DOCA (cm)");
				DC_residuals_trkDoca_rescut[s][sl].setTitleY("residual (cm)");
				DC_residuals[s][sl] = new H1F(String.format("DC_residuals_%d_%d",s+1,sl+1),String.format("DC_residuals_%d_%d",s+1,sl+1),400,-0.5,0.5);
				DC_residuals[s][sl].setTitle(String.format("DC residuals S%d SL%d",s+1,sl+1));
				DC_residuals[s][sl].setTitleX("residual (cm)");
				DC_residuals_nocut[s][sl] = new H1F(String.format("DC_residuals_nocut_%d_%d",s+1,sl+1),String.format("DC_residuals_nocut_%d_%d",s+1,sl+1),400,-0.5,0.5);
				DC_residuals_nocut[s][sl].setTitle(String.format("DC residuals S%d SL%d",s+1,sl+1));
				DC_residuals_nocut[s][sl].setTitleX("residual (cm)");
				DC_residuals_rescut[s][sl] = new H1F(String.format("DC_residuals_rescut_%d_%d",s+1,sl+1),String.format("DC_residuals_rescut_%d_%d",s+1,sl+1),400,-0.5,0.5);
				DC_residuals_rescut[s][sl].setTitle(String.format("DC residuals S%d SL%d",s+1,sl+1));
				DC_residuals_rescut[s][sl].setTitleX("residual (cm)");
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
				DC_time_nocut[s][sl] = new H1F(String.format("DC_Time_nocut_%d_%d",s+1,sl+1),String.format("DC_Time_nocut_%d_%d",s+1,sl+1),200,-100,1000);
			   	DC_time_nocut[s][sl].setTitle(String.format("DC Time S%d SL%d",s+1,sl+1));
			   	DC_time_nocut[s][sl].setTitleX("time (ns)");
				DC_time_nocut[s][sl].setLineWidth(4);
				DC_time_rescut[s][sl] = new H1F(String.format("DC_Time_rescut_%d_%d",s+1,sl+1),String.format("DC_Time_rescut_%d_%d",s+1,sl+1),200,-100,1000);
			   	DC_time_rescut[s][sl].setTitle(String.format("DC Time S%d SL%d",s+1,sl+1));
			   	DC_time_rescut[s][sl].setTitleX("time (ns)");
				DC_time_rescut[s][sl].setLineWidth(4);
				f_time_invertedS[s][sl] = new F1D(String.format("Inverted_S_%d_%d",s+1,sl+1),"[p0]/(1+exp(-[p1]*(x-[p2])))",-100,1000);
				f_time_invertedS[s][sl].setOptStat("111111");
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
			float dDoca = DCB.getFloat("dDoca",r);
			float time = DCB.getFloat("time",r);
			int jitter = DCB.getInt("jitter",r);
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

			// float field = DCB.getFloat("B",r); //removing per DC experts' request
			if(s>-1&&s<6&&sl>-1&&sl<6 && leadingelectron){
				// boolean otherregions = (sl<2 || sl>3);
				// boolean region2 = ((sl==2||sl==3) && field<0.5);
				// if (otherregions||region2) {

				//Fill Histograms with no extra cut
				DC_residuals_trkDoca_nocut[s][sl].fill(trkDoca,timeResidual);
				DC_residuals_nocut[s][sl].fill(timeResidual);
				DC_time_nocut[s][sl].fill(time);

				//Add extra cuts on hits from DC4gui. TrkID, beta, alphacut, TFlight (maybe PID?, needs REC::Event here)
				if (DCB.getByte("trkID", r) > 0 && DCB.getFloat("beta", r) > betacutvalue &&
					 DCB.getFloat("TFlight",r) > 0 && alphacutpass == true
						)
				{

					DC_residuals_trkDoca[s][sl].fill(trkDoca,timeResidual);
					DC_residuals[s][sl].fill(timeResidual);
					DC_time[s][sl].fill(time);
					DC_jitterdist.fill(jitter);

					if( timestamp%2 == 0) {//even time stamps
						DC_time_even[s][sl].fill(time);
						//sector and superlayer need to go from 1-6
						DC_hits_even_ts_sec_sl.fill(s+1,sl+1);
						if (jitter == 0) {
							DC_jitterzero_sec_sl.fill(s+1,sl+1);
						}
						if (jitter == 2 || jitter == -2 || jitter == 8 || jitter == -8) {
							DC_jittertwo_sec_sl.fill(s+1,sl+1);
						}
					}
					else {//odd time stamps
						DC_time_odd[s][sl].fill(time);
						//sector and superlayer need to go from 1-6
						DC_hits_odd_ts_sec_sl.fill(s+1,sl+1);
						if (jitter == 1 || jitter == -1 || jitter == 4 || jitter == -4) {
							DC_jitterone_sec_sl.fill(s+1,sl+1);
						}
					}
				//Apply also fitresidual cut, factor 0.0001 to convert to cm from microns
					if (DCB.getFloat("fitResidual",r) < 0.0001 * fitresidualcut) {
						DC_residuals_trkDoca_rescut[s][sl].fill(trkDoca,timeResidual);
						DC_residuals_rescut[s][sl].fill(timeResidual);
						DC_time_rescut[s][sl].fill(time);
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
//System.out.println("TDC component " +component_tdc+ "; ADC component" +component_adc+ "; Layer ADC "+layer_adc+ "; Layer TDC "+layer_tdc+"; Sector ADC "+sector_adc+"; Sector TDC "+sector_tdc+"; TDC bank value "+TDC+"; ADC bank value "+time_adc);
//System.out.println("TDC PMT "+tdc_pmt+" ADC PMT "+adc_pmt);
//System.out.println("TimeStamp "+timestamp+"; Int Trigger Phase "+triggerPhaseTOF+"; Float Trigger Phase "+(float)triggerPhaseTOF+"; time_tdc "+time_tdc+" time_adc "+time_adc+"; Time diff "+time_diff);
//System.out.println(" ");
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
			byte charge = part.getByte("charge",k);
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
						int pad = sc.getInt("component", j);
						int sector = sc.getInt("sector",j);
						float time = sc.getFloat("time", j);
						float pathlength = sc.getFloat("path",j);
						float timediff = -10.f;
						float flighttime = -10.0f;
						float vcor = -10.0f;
						// electron's case
						if (pid == 11) {
							flighttime = pathlength/29.98f;
							vcor = vz/29.98f;
						}
						// pi+, pi- case
						else if (pid == 211 || pid == -211) {
							flighttime = pathlength/(float)(29.98f * mom/Math.sqrt(mom*mom+0.13957f*0.13957f));
							vcor = vz/(float)(29.98f * mom/Math.sqrt(mom*mom+0.13957f*0.13957f));
						}
						//proton case
						else if (pid == 2212) {
							flighttime = pathlength/(float)(29.98f * mom/Math.sqrt(mom*mom+0.93827f*0.93827f));
							vcor = vz/(float)(29.98f * mom/Math.sqrt(mom*mom+0.93827f*0.93827f));
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
									p1a_pad_dt_calib[sector-1].fill(pad,timediff);
									p1a_dt_calib_all[sector-1].fill(timediff);
									if (k!=0) {
										p1a_pad_dt_4nstrack[sector-1].fill(pad,timediff);
                                                                        	p1a_dt_4nstrack_all[sector-1].fill(timediff);
									}
								}
								if (energy > 2.){
									if (theta <= 11.) p1a_edep[sector-1][0].fill(energy);
									if (theta > 11. && theta <=23) p1a_edep[sector-1][1].fill(energy);
									if (theta > 23.) p1a_edep[sector-1][2].fill(energy);
								}
							}
							if (sc.getByte("layer",j)==2){
								if (mom > 0.4 && mom < 10 && energy > 0.5 && reducedchi2 < 75) {
									p1b_pad_dt_calib[sector-1].fill(pad,timediff);
									p1b_dt_calib_all[sector-1].fill(timediff);
									if (k!=0) {
                                                                                p1b_pad_dt_4nstrack[sector-1].fill(pad,timediff);
                                                                                p1b_dt_4nstrack_all[sector-1].fill(timediff);
                                                                        }
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
									p2_pad_dt_calib[sector-1].fill(pad,timediff);
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
		int index_ftof = 0;
		int index_ctof = 0;
                for(int k=0;k<part.rows();k++) {
                        byte charge = part.getByte("charge",k);
                        int pid = part.getInt("pid",k);
                        double px = part.getFloat("px",k);
                        double py = part.getFloat("py",k);
                        double pz = part.getFloat("pz",k);
                        double vz = part.getFloat("vz",k);
                        double vt = part.getFloat("vt",k);
                        double mom = Math.sqrt(px*px+py*py+pz*pz);
                        double theta = Math.toDegrees(Math.acos(pz/mom));

                        double reducedchi2 = 10000.f;
                        double energy = -100.f;

			double track_redchi2 = 1000.f;

                        int status = part.getShort("status", k);
                        if (status<0) status = -status;
                        boolean inDC = (status>=2000 && status<4000);
                        boolean inCVT = (status>=4000 && status < 8000);

                        for (int j=0;j<trk.rows();j++) {
                                if (trk.getShort("pindex",j)==k) {
                                        reducedchi2 = trk.getFloat("chi2",j)/trk.getShort("NDF",j);
                                }
                        }

                        for (int j=0;j<sc.rows();j++) {
                                if (sc.getShort("pindex",j)==k) {
                                        if (sc.getByte("detector",j)==12) {
                                                int pad = sc.getInt("component", j);
                                                sector = sc.getInt("sector",j);
                                                double time = sc.getFloat("time", j);
                                                double pathlength = sc.getFloat("path",j);
						energy = sc.getFloat("energy",j);
                                                double flighttime = -10.0f;
						//System.out.println(String.format(" Sector FTOF: "+sector+" inDC: "+inDC+" PID: "+pid+" Momentum "+mom+" theta: "+theta+" Reduced chi2: "+reducedchi2+"\n"));
                                                //if (pid == 11 && inDC && mom > 0.4 && mom < 10. && energy > 0.5 && reducedchi2 < 75 && theta <= 11.) {
						if (pid == 11 && inDC) {
                                                        flighttime = pathlength/PhysicsConstants.speedOfLight();
                                                        FTOF_vtime.add(time - flighttime);
							//System.out.println(String.format(" Sector FTOF: "+sector+"\n"));
							N_electrons_FD++;

                                                }
                                        }
                                        if (sc.getByte("detector",j)==4) {
						double t = -100.0f;
						double p = -100.0f;
						double mom_cvt = -100.0f;
						double energy_ctof = sc.getFloat("energy",j);
						int pad_ctof = sc.getInt("component", j);
						int sector_ctof = sc.getInt("sector",j);
						int paddle_ctof = -100;
						int sec_ctof = -100;
						double e = -100.0f;
						double beta, beta_ctof;
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
									e = ctof.getFloat("energy", iCTOF);
                                                                        paddle_ctof = ctof.getInt("component",iCTOF);
                                                                        track_redchi2 = cvttrk.getFloat("chi2", iCVT)/cvttrk.getShort("ndf", iCVT);
									sec_ctof = ctof.getInt("sector",iCTOF);
                                                                        t = ctof.getFloat("time",iCTOF);
                                                                        p = ctof.getFloat("pathLength",iCTOF);
                                                                        mom_cvt = cvttrk.getFloat("p",iCVT);
									beta_ctof = mom_cvt/Math.sqrt(Math.pow(mom_cvt,2)+Math.pow(PhysicsConstants.massPionCharged(),2));
                                                                        ctof_vtime = t - p/beta_ctof/PhysicsConstants.speedOfLight();
                                                                }
                                                        }
                                                }
                                                double pathlength_ctof = sc.getFloat("path",j);
                                                double time_ctof = sc.getFloat("time",j);
                                                double flighttime_ctof = -10.0f;
                                                double timediff_ctof = -10.f;
                                                //if (pid == -211 && inCVT && mom>0.4 && mom < 3.0 && track_redchi2 < 30. && e > 0.5 && paddle_ctof >= 13 && paddle_ctof <= 24) {
						if (pid == -211 && inCVT && ctof_vtime!=-100.) {
							beta = mom/Math.sqrt(Math.pow(mom,2)+Math.pow(PhysicsConstants.massPionCharged(),2));
                                                        flighttime_ctof = pathlength_ctof/beta/PhysicsConstants.speedOfLight();
                                                        timediff_ctof = (time_ctof - flighttime_ctof);
							CTOF_vtime.add(ctof_vtime);
							N_pim_CD++;
//System.out.println(String.format("Part momentum: "+mom+" CVT momentum: "+mom_cvt+" Scintillator time: "+time_ctof+" CTOF time: "+t+" Scntillator pathlength: "+pathlength_ctof+" CTOF pathlength: "+p+" VertTDiff_SCBank: "+timediff_ctof+" VertTDiff_CTOFBank: "+CTOF_vtime+"\n"));
//System.out.println(String.format("CTOF pad: "+paddle_ctof+" SC pad: "+pad_ctof+" Energy CTOF: "+e+" Energy SC: "+energy_ctof+" Sector CTOF: "+sec_ctof+" Sector SC: "+sector_ctof+"\n"));
                                                }


                                        }
                                }
                        }
                }
		if (N_electrons_FD >= 1 && N_pim_CD >= 1 && sector != 0) {
			for (int n=0;n<N_electrons_FD;n++) {
				for (int nn=0;nn<N_pim_CD;nn++) {
			//System.out.println(String.format("Number electrons FD within cuts: "+N_electrons_FD+" Number of pi- CD within cuts: "+N_pim_CD+"\n"));
					//System.out.println(String.format("FTOF vtime: "+FTOF_vtime.get(n)+" CTOF_vtime: "+CTOF_vtime.get(nn)+"\n"));
					ftof_ctof_vtdiff[sector-1].fill(FTOF_vtime.get(n)-CTOF_vtime.get(nn));
				}
			}
		}

        }



	public void fillTOFHists(DataBank tofB, DataBank DCB){
		for(int r=0;r<tofB.rows();r++){
			int layer = tofB.getInt("layer", r);
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
			if(foundTrk && tofB.getFloat("energy", r) > 2.0 ){
				//TEMPORARY TEST FOR PION VERTEX TIME
				float flightTime = tofB.getFloat("pathLength", r)/(float)( 29.98f * thisMom/Math.sqrt(thisMom*thisMom + 0.13957f*0.13957f) );
				float thisPionTime = tofB.getFloat("time", r) - flightTime - RFTime;
				thisPionTime = (thisPionTime+(rf_large_integer+0.5f)*rfPeriod) % rfPeriod;
				thisPionTime = thisPionTime - rfPeriod/2;
				//System.out.println("compare vextex times : "+thisTime+" =? "+thisPionTime+" , diff = "+(thisTime-thisPionTime));
				//thisTime = thisPionTime;
				thisVz = 0;
				if(layer==1){//panel 1A
					p1a_pad_occ.fill( tofB.getInt("component", r) , tofB.getInt("sector", r) );
					p1a_pad_XY.fill( tofB.getFloat("x",r) , tofB.getFloat("y",r) );
					p1a_pad_vt[tofB.getInt("sector", r)-1].fill( tofB.getInt("component", r) , thisTime-thisVz/29.98f );
					p1a_pad_edep[tofB.getInt("sector", r)-1].fill( tofB.getInt("component", r) , tofB.getFloat("energy", r) );
				}
				if(layer==2){//panel 1B
					p1b_pad_occ.fill( tofB.getInt("component", r) , tofB.getInt("sector", r) );
					p1b_pad_XY.fill( tofB.getFloat("x",r) , tofB.getFloat("y",r) );
					p1b_pad_vt[tofB.getInt("sector", r)-1].fill( tofB.getInt("component", r) , thisTime-thisVz/29.98f );
					p1b_pad_edep[tofB.getInt("sector", r)-1].fill( tofB.getInt("component", r) , tofB.getFloat("energy", r) );
				}
				if(layer==3){//panel 2
					p2_pad_occ.fill( tofB.getInt("component", r) , tofB.getInt("sector", r) );
					p2_pad_XY.fill( tofB.getFloat("x",r) , tofB.getFloat("y",r) );
					p2_pad_vt[tofB.getInt("sector", r)-1].fill( tofB.getInt("component", r) , thisTime-thisVz/29.98f );
					p2_pad_edep[tofB.getInt("sector", r)-1].fill( tofB.getInt("component", r) , tofB.getFloat("energy", r) );
				}
			}
		}
		for(int h1=0;h1<tofB.rows();h1++){
			float thisChi2 = 999;
			float thisMom = 0;
			boolean foundTrk1 = false;int iTrk1=-1;
			boolean foundTrk2 = false;int iTrk2=-1;
			if(tofB.getInt("layer", h1)==1&&tofB.getFloat("energy", h1)>1.0)for(int t=0;t<DCB.rows() && !foundTrk1;t++){
				if(DCB.getInt("id",t) == tofB.getInt("trackid",h1) ){
					thisChi2 = DCB.getFloat("chi2",t);
					thisMom=0;
					thisMom += DCB.getFloat("p0_x",t)*DCB.getFloat("p0_x",t);
					thisMom += DCB.getFloat("p0_y",t)*DCB.getFloat("p0_y",t);
					thisMom += DCB.getFloat("p0_z",t)*DCB.getFloat("p0_z",t);
					thisMom = (float)Math.sqrt(thisMom);
					if(thisChi2 < 5000 && thisMom > 0.8 && Math.abs(DCB.getFloat("Vtx0_z",t))<15 ){foundTrk1 = true;iTrk1=t;}
				}
			}
			if(foundTrk1)for(int h2=0;h2<tofB.rows();h2++){
				if(h1!=h2 && tofB.getInt("layer", h2)!=1 && tofB.getInt("sector", h2)!=tofB.getInt("sector", h1) && tofB.getFloat("energy", h2)>1.0 )
				  for(int t=0;t<DCB.rows() && !foundTrk2;t++){
					if(t!=iTrk1 && DCB.getInt("id",t) == tofB.getInt("trackid",h2) ){
						thisChi2 = DCB.getFloat("chi2",t);
						thisMom=0;
						thisMom += DCB.getFloat("p0_x",t)*DCB.getFloat("p0_x",t);
						thisMom += DCB.getFloat("p0_y",t)*DCB.getFloat("p0_y",t);
						thisMom += DCB.getFloat("p0_z",t)*DCB.getFloat("p0_z",t);
						thisMom = (float)Math.sqrt(thisMom);
						if(thisChi2 < 5000 && thisMom > 0.8 && Math.abs(DCB.getFloat("Vtx0_z",t))<15 ){foundTrk2 = true;iTrk2=t;}
					}
				}
				if(foundTrk2){
					float thisTime = tofB.getFloat("time", h1) - tofB.getFloat("time", h2) - (tofB.getFloat("pathLength",h1)-tofB.getFloat("pathLength",h2))/29.98f;;
					if(thisTime!=0){
						p1a_pad_dt[tofB.getInt("sector",h1)-1].fill( tofB.getInt("component", h1) , thisTime );
						if(tofB.getInt("layer", h2)==2)p1b_pad_dt[tofB.getInt("sector",h2)-1].fill( tofB.getInt("component", h2) , thisTime );
						if(tofB.getInt("layer", h2)==3)p2_pad_dt[tofB.getInt("sector",h2)-1].fill( tofB.getInt("component", h2) , thisTime );
					}
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
			byte q = bank.getByte("charge", k);
			float px = bank.getFloat("px", k);
			float py = bank.getFloat("py", k);
			float pz = bank.getFloat("pz", k);
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
		DataBank trackDetBank = null, hitBank = null, partBank = null, tofhits = null, scintillator = null, scintextras = null;
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
		if(event.hasBank("FTOF::hits")) tofhits = event.getBank("FTOF::hits");
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

	public void initInvertedSFitPar(int slayer, F1D function) {
		double min = 100.;
		double max = 220.;
		if (slayer == 1) {
			min = 100.; max = 240.;
			function.setParameter(1,-0.038); function.setParLimits(1,-0.01,-0.06);
			function.setParameter(2,118.); function.setParLimits(2,100.,150.);
		}
		if (slayer == 2) {
			min = 120.; max = 240.;
			function.setParameter(1,-0.040); function.setParLimits(1,-0.01,-0.06);
			function.setParameter(2,136.); function.setParLimits(2,100.,200.);
		}
		if (slayer == 3) {
			min = 200.; max = 450.;
			function.setParameter(1,-0.030);function.setParLimits(1,-0.01,-0.05);
			function.setParameter(2,320.); function.setParLimits(2,200.,500.);
		}
		if (slayer == 4) {
			min = 200.; max = 500.;
			function.setParameter(1,-0.023); function.setParLimits(1,-0.01,-0.05);
			function.setParameter(2,350.); function.setParLimits(2,200.,500.);
		}
		if (slayer == 5) {
			min = 400.; max = 700.;
			function.setParameter(1,-0.024);function.setParLimits(1,-0.01,-0.05);
			function.setParameter(2,623.); function.setParLimits(2,500.,700.);
		}
		if (slayer == 6) {
			min = 480.; max = 700.;
			function.setParameter(1,-0.034); function.setParLimits(1,-0.01,-0.05);
			function.setParameter(2,683.); function.setParLimits(2,500.,750.);
		}
		function.setRange(min,max);
		function.setLineColor(2);
		function.setLineWidth(4);
	}

	public void analyze() {
		for(int sl=1;sl<=6;sl++) {
			for (int l=0;l<6;l++) {
				initInvertedSFitPar(sl,f_time_invertedS[l][sl-1]);
				DataFitter.fit(f_time_invertedS[l][sl-1],DC_time[l][sl-1],"QL");
				//System.out.println("Fit DC Time for sector "+l+ " superlayer " +sl+ "complete");
				//System.out.println(" ");System.out.println(" ");
				DC_time[l][sl-1].setFunction(null);
			}
		}
	}

	public void plot() {
		EmbeddedCanvas can_TOF_occ = new EmbeddedCanvas();
		can_TOF_occ.setSize(3000,5000);
		can_TOF_occ.divide(6,9);
		can_TOF_occ.setAxisTitleSize(18);
		can_TOF_occ.setAxisFontSize(18);
		can_TOF_occ.setTitleSize(18);
		for(int s=0;s<6;s++){
			can_TOF_occ.cd(s);can_TOF_occ.draw(p1a_pad_vt[s]);
			//can_TOF_occ.getPad(6+s).getAxisZ().setLog(true);
			can_TOF_occ.cd(6+s);can_TOF_occ.draw(p1b_pad_vt[s]);
			//can_TOF_occ.getPad(12+s).getAxisZ().setLog(true);
			can_TOF_occ.cd(12+s);can_TOF_occ.draw(p2_pad_vt[s]);
			//can_TOF_occ.getPad(12+s).getAxisZ().setLog(true);
			can_TOF_occ.cd(18+s);can_TOF_occ.draw(p1a_pad_edep[s]);
			//can_TOF_occ.getPad(24+s).getAxisZ().setLog(true);
			can_TOF_occ.cd(24+s);can_TOF_occ.draw(p1b_pad_edep[s]);
			//can_TOF_occ.getPad(30+s).getAxisZ().setLog(true);
			can_TOF_occ.cd(30+s);can_TOF_occ.draw(p2_pad_edep[s]);
			//can_TOF_occ.getPad(36+s).getAxisZ().setLog(true);
			can_TOF_occ.cd(36+s);can_TOF_occ.draw(p1a_pad_dt[s]);
			//can_TOF_occ.getPad(42+s).getAxisZ().setLog(true);
			can_TOF_occ.cd(42+s);can_TOF_occ.draw(p1b_pad_dt[s]);
			//can_TOF_occ.getPad(48+s).getAxisZ().setLog(true);
			can_TOF_occ.cd(48+s);can_TOF_occ.draw(p2_pad_dt[s]);
			//can_TOF_occ.getPad(54+s).getAxisZ().setLog(true);
		}
                can_TOF_occ.save(String.format(outputDir+"/TOF_cal.png"));
                System.out.println(String.format("saved "+outputDir+"/TOF_cal.png"));

		EmbeddedCanvas can_TOF_calib = new EmbeddedCanvas();
		can_TOF_calib.setSize(3000,5000);
		can_TOF_calib.divide(6,13);
		can_TOF_calib.setAxisTitleSize(18);
		can_TOF_calib.setAxisFontSize(18);
		can_TOF_calib.setTitleSize(18);
		for(int s=0;s<6;s++){
			can_TOF_calib.cd(s);can_TOF_calib.draw(p1a_pad_dt_calib[s]);
			can_TOF_calib.cd(s+6);can_TOF_calib.draw(p1a_dt_calib_all[s]);
			can_TOF_calib.cd(s+12);can_TOF_calib.draw(p1b_pad_dt_calib[s]);
			can_TOF_calib.cd(s+18);can_TOF_calib.draw(p1b_dt_calib_all[s]);
			can_TOF_calib.cd(s+24);can_TOF_calib.draw(p2_pad_dt_calib[s]);
			can_TOF_calib.cd(s+30);can_TOF_calib.draw(p2_dt_calib_all[s]);
			for (int k=0;k<3;k++) {
				can_TOF_calib.cd(6*s+36+k);can_TOF_calib.draw(p1a_edep[s][k]);
				can_TOF_calib.cd(6*s+36+k+3);can_TOF_calib.draw(p1b_edep[s][k]);
			}
			can_TOF_calib.cd(s+72);can_TOF_calib.draw(p2_edep[s]);
		}

		EmbeddedCanvas can_FTOF_CTOF = new EmbeddedCanvas();
                can_FTOF_CTOF.setSize(3000,500);
                can_FTOF_CTOF.divide(6,1);
                can_FTOF_CTOF.setAxisTitleSize(18);
                can_FTOF_CTOF.setAxisFontSize(18);
                can_FTOF_CTOF.setTitleSize(18);
                for(int s=0;s<6;s++){
                	can_FTOF_CTOF.cd(s);can_FTOF_CTOF.draw(ftof_ctof_vtdiff[s]);
		}

                can_TOF_calib.save(String.format(outputDir+"/TOF_calib.png"));
                can_FTOF_CTOF.save(String.format(outputDir+"/FTOF_CTOF.png"));
                System.out.println(String.format("saved "+outputDir+"/TOF_calib.png"));
                System.out.println(String.format("saved "+outputDir+"/FTOF_CTOF.png"));

		EmbeddedCanvas can_TOF_4nstrack = new EmbeddedCanvas();
                can_TOF_4nstrack.setSize(3000,5000);
                can_TOF_4nstrack.divide(6,13);
                can_TOF_4nstrack.setAxisTitleSize(18);
                can_TOF_4nstrack.setAxisFontSize(18);
                can_TOF_4nstrack.setTitleSize(18);
                for(int s=0;s<6;s++){
                        can_TOF_4nstrack.cd(s);can_TOF_4nstrack.draw(p1a_pad_dt_4nstrack[s]);
                        can_TOF_4nstrack.cd(s+6);can_TOF_4nstrack.draw(p1a_dt_4nstrack_all[s]);
                        can_TOF_4nstrack.cd(s+12);can_TOF_4nstrack.draw(p1b_pad_dt_4nstrack[s]);
                        can_TOF_4nstrack.cd(s+18);can_TOF_4nstrack.draw(p1b_dt_4nstrack_all[s]);
                }

                can_TOF_4nstrack.save(String.format(outputDir+"/TOF_4nstrack.png"));
                System.out.println(String.format("saved "+outputDir+"/TOF_4nstrack.png"));

		EmbeddedCanvas can_TOF_ADCTDC = new EmbeddedCanvas();
		can_TOF_ADCTDC.setSize(3000,3000);
		can_TOF_ADCTDC.divide(6,3);
		can_TOF_ADCTDC.setAxisTitleSize(18);
		can_TOF_ADCTDC.setAxisFontSize(18);
		can_TOF_ADCTDC.setTitleSize(18);
		for(int s=0;s<6;s++){
			can_TOF_ADCTDC.cd(s);can_TOF_ADCTDC.draw(p1a_tdcadc_dt[s]);
			can_TOF_ADCTDC.cd(s+6);can_TOF_ADCTDC.draw(p1b_tdcadc_dt[s]);
			can_TOF_ADCTDC.cd(s+12);can_TOF_ADCTDC.draw(p2_tdcadc_dt[s]);
		}
                can_TOF_ADCTDC.save(String.format(outputDir+"/TOF_adctdc_timediff.png"));
                System.out.println(String.format("saved "+outputDir+"/TOF_adctdc_timediff.png"));

		EmbeddedCanvas can_DC_resd_trkDoca  = new EmbeddedCanvas();
		can_DC_resd_trkDoca.setSize(3000,3000);
		can_DC_resd_trkDoca.divide(6,6);
		can_DC_resd_trkDoca.setAxisTitleSize(18);
		can_DC_resd_trkDoca.setAxisFontSize(18);
		can_DC_resd_trkDoca.setTitleSize(18);
		for(int s=0;s<6;s++)for(int sl=0;sl<6;sl++){
			can_DC_resd_trkDoca.cd(sl + 6*s);
			can_DC_resd_trkDoca.getPad(sl + 6*s).getAxisZ().setLog(true);
			can_DC_resd_trkDoca.draw(DC_residuals_trkDoca[s][sl]);
		}
                can_DC_resd_trkDoca.save(String.format(outputDir+"/DC_resd_trkDoca.png"));
                System.out.println(String.format("saved "+outputDir+"/DC_resd_trkDoca.png"));

		EmbeddedCanvas can_DC_resd  = new EmbeddedCanvas();
		can_DC_resd.setSize(3000,3000);
		can_DC_resd.divide(6,6);
		can_DC_resd.setAxisTitleSize(18);
		can_DC_resd.setAxisFontSize(18);
		can_DC_resd.setTitleSize(18);
		for(int s=0;s<6;s++)for(int sl=0;sl<6;sl++){
			can_DC_resd.cd(sl + 6*s);
			can_DC_resd.draw(DC_residuals[s][sl]);
		}
                can_DC_resd.save(String.format(outputDir+"/DC_resd.png"));
                System.out.println(String.format("saved "+outputDir+"/DC_resd.png"));

		EmbeddedCanvas can_DC_time  = new EmbeddedCanvas();
		can_DC_time.setSize(3000,3000);
		can_DC_time.divide(6,6);
		can_DC_time.setAxisTitleSize(18);
		can_DC_time.setAxisFontSize(18);
		can_DC_time.setTitleSize(18);
		for(int s=0;s<6;s++)for(int sl=0;sl<6;sl++){
				can_DC_time.cd(sl + 6*s);
				can_DC_time.draw(DC_time[s][sl]);can_DC_time.draw(f_time_invertedS[s][sl],"same");
		}
                can_DC_time.save(String.format(outputDir+"/DC_time.png"));
                System.out.println(String.format("saved "+outputDir+"/DC_time.png"));
	}

	public void write() {
		TDirectory dirout = new TDirectory();
		dirout.mkdir("/tof/");
		dirout.cd("/tof/");
		for(int s=0;s<6;s++){
			dirout.addDataSet(p1a_pad_vt[s],p1b_pad_vt[s],p2_pad_vt[s],p1a_pad_dt[s],p1b_pad_dt[s],p2_pad_dt[s]);
			dirout.addDataSet(p1a_pad_dt_calib[s],p1b_pad_dt_calib[s],p2_pad_dt_calib[s],p1a_dt_calib_all[s],p1b_dt_calib_all[s],p2_dt_calib_all[s],p2_edep[s]);
			dirout.addDataSet(p1a_pad_dt_4nstrack[s],p1b_pad_dt_4nstrack[s],p1a_dt_4nstrack_all[s],p1b_dt_4nstrack_all[s]);
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
			dirout.addDataSet(DC_residuals_trkDoca_rescut[s][sl],DC_time_rescut[s][sl]);
			dirout.addDataSet(DC_residuals_trkDoca_nocut[s][sl],DC_time_nocut[s][sl]);
		}
		dirout.addDataSet(DC_jitterzero_sec_sl, DC_jitterone_sec_sl, DC_jittertwo_sec_sl);
		dirout.addDataSet(DC_hits_even_ts_sec_sl, DC_hits_odd_ts_sec_sl,DC_jitterdist);
                if(runNum>0) dirout.writeFile(outputDir+"/out_TOF_"+runNum+".hipo");
                else         dirout.writeFile(outputDir+"/out_TOF.hipo");
	}
////////////////////////////////////////////////
	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "true");
		GStyle.setPalette("kRainBow");
		int count = 0;
		int runNum = 0;
		boolean useTB = true;
		String filelist = "list_of_files.txt";
		if(args.length>0)runNum = Integer.parseInt(args[0]);
		if(args.length>1)filelist = args[1];
		if(args.length>2)if(Integer.parseInt(args[2])==0)useTB=false;
                String outputDir = runNum > 0 ? "plots"+runNum : "plots";
		tof_monitor ana = new tof_monitor(runNum,outputDir,useTB);
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
		int maxevents = 50000000;
		if(args.length>2)maxevents=Integer.parseInt(args[2]);
		int progresscount=0;int filetot = toProcessFileNames.size();
		for (String runstrg : toProcessFileNames) if(count<maxevents){
			progresscount++;
			System.out.println(String.format(">>>>>>>>>>>>>>>> tof_monitor %s",runstrg));
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
				if(count%10000 == 0) System.out.println(count/1000 + "k events (this is tof_monitor on "+runstrg+") progress : " + progresscount+"/"+filetot);
			}
			reader.close();
		}
		System.out.println("Total : " + count + " events");
		ana.analyze();
		ana.plot();
		ana.write();
	}
}
