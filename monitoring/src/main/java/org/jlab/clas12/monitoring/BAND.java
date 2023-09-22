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
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.ConstantsManager;

public class BAND{
	boolean userTimeBased;
	int runNum;
        String outputDir;
	boolean[] trigger_bits;
	public float EBeam;
	public float starttime;
	public float rfPeriod, rfoffset1, rfoffset2;
	public int rf_large_integer;
        public int e_part_ind, e_sect, e_track_ind, pip_part_ind, pipm_part_ind, pip_sect, pim_sect;
        public float RFtime, e_mom, e_theta, e_phi, e_vx, e_vy, e_vz, e_ecal_X, e_ecal_Y, e_ecal_Z, e_ecal_E, e_track_chi2, e_vert_time, e_vert_time_RF, e_Q2, e_xB, e_W;

	public H1F[] H_BAND_adcCor, H_BAND_meantimeadc, H_BAND_meantimetdc, H_BAND_lasertimeadc;
	public float speedoflight;

	public IndexedTable InverseTranslationTable;
	public IndexedTable calibrationTranslationTable;
	public IndexedTable rfTable, rfTableOffset;
	public ConstantsManager ccdb;


	public BAND(int reqR, String reqOutputDir, float reqEb, boolean reqTimeBased){
        	runNum = reqR;userTimeBased=reqTimeBased;
                outputDir = reqOutputDir;
		EBeam = 2.2f;
		if(reqEb>0 && reqEb<4)EBeam=2.22f;
		if(reqEb>4 && reqEb<7.1)EBeam=6.42f;
		if(reqEb>7.1 && reqEb<9)EBeam=7.55f;
		if(reqEb>9)EBeam=10.6f;
		EBeam = reqEb;
		trigger_bits = new boolean[32];

		H_BAND_adcCor = new H1F[2];
		H_BAND_meantimeadc = new H1F[2];
		H_BAND_meantimetdc = new H1F[2];
  	H_BAND_lasertimeadc = new H1F[2];
		speedoflight = 29.9792458f;

		rfPeriod = 4.008f;
		ccdb = new ConstantsManager();
		ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo","/calibration/eb/rf/config","/calibration/eb/rf/offset"}));
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

		for(int s=0;s<2;s++){
			H_BAND_adcCor[s] = new H1F(String.format("H_BAND_ADC_LR_SectorCombination%d",s+1),String.format("H_BAND_ADC_LR_SectorCombination %d",s+1),200,1.,5001.);
          		H_BAND_adcCor[s].setTitleX("sqrt( adcLcorr * adcRcorr )");
           		H_BAND_adcCor[s].setTitleY("events");
			H_BAND_meantimeadc[s] = new H1F(String.format("H_BAND_MeanTimeFADC_SectorCombination%d",s+1),String.format("H_BAND_MeanTimeFADC_SectorCombination %d",s+1),200,-100.,301.);
			H_BAND_meantimeadc[s].setTitleX("meantimeFadc - STT - sqrt(x^2+y^2+z^2)/c (ns)");
			H_BAND_meantimeadc[s].setTitleY("events");
			H_BAND_meantimetdc[s] = new H1F(String.format("H_BAND_MeanTimeTDC_SectorCombination%d",s+1),String.format("H_BAND_MeanTimeTDC_SectorCombination %d",s+1),350,-50.,650.);
            		H_BAND_meantimetdc[s].setTitleX("meantimeTDC -  STT - sqrt(x^2+y^2+z^2)/c (ns)");
            		H_BAND_meantimetdc[s].setTitleY("events");
			H_BAND_lasertimeadc[s] = new H1F(String.format("H_BAND_LaserTimeFADC_SectorCombination%d",s+1),String.format("H_BAND_LaserTimeFADC_SectorCombination %d",s+1),400,300,700.);
            		H_BAND_lasertimeadc[s].setTitleX("meantimeFADC (ns)");
            		H_BAND_lasertimeadc[s].setTitleY("events");
		}
	}

	public void fill_Histograms_Hits(DataBank bankhits, int lasercondition) {
		if (lasercondition == 1) { //for laser hits
			for(int k = 0; k < bankhits.rows(); k++){
				float time_fadc;
				int sect = 0;

				sect = bankhits.getInt("sector",k);

  			time_fadc = bankhits.getFloat("time",k);

				if (sect == 3 || sect == 4) {
					H_BAND_lasertimeadc[0].fill(time_fadc);
				}
				if (sect == 1 || sect == 2 || sect == 5) {
					H_BAND_lasertimeadc[1].fill(time_fadc);
				}
			}

		}
		else {
			for(int k = 0; k < bankhits.rows(); k++){
				float time_fadc;
				float time_tdc;
				float x, y, z, L;
				int sect = 0;
				float histo1, histo2, histo3;
				int status = 0;
				sect = bankhits.getInt("sector",k);

			  histo1 = bankhits.getFloat("energy",k);

		    x = bankhits.getFloat("x",k);
		    y = bankhits.getFloat("y",k);
		    z = bankhits.getFloat("z",k);
		    time_fadc = bankhits.getFloat("timeFadc",k);
		    time_tdc = bankhits.getFloat("time",k);
				status = bankhits.getInt("status",k);
				L = (float)Math.sqrt(x*x+y*y+z*z);
				histo2 = time_fadc - starttime - L/speedoflight;
				histo3 = time_tdc - starttime - L/speedoflight;

				if ( (sect == 3 || sect == 4) && status == 0) {
					H_BAND_adcCor[0].fill(histo1);
					H_BAND_meantimeadc[0].fill(histo2);
					H_BAND_meantimetdc[0].fill(histo3);
				}
				if ( (sect == 1 || sect == 2 || sect == 5) && status == 0) {
					H_BAND_adcCor[1].fill(histo1);
					H_BAND_meantimeadc[1].fill(histo2);
					H_BAND_meantimetdc[1].fill(histo3);
				}
			}
		}
	}


	public void processEvent(DataEvent event){
		e_part_ind = -1;
		RFtime=0;
		starttime = 0;
		if(event.hasBank("RUN::config")){
				DataBank confbank = event.getBank("RUN::config");
				long TriggerWord = confbank.getLong("trigger",0);
				for (int i = 31; i >= 0; i--) {trigger_bits[i] = (TriggerWord & (1 << i)) != 0;}
				if(event.hasBank("RUN::rf")){
					for(int r=0;r<event.getBank("RUN::rf").rows();r++){
						if(event.getBank("RUN::rf").getInt("id",r)==1)RFtime=event.getBank("RUN::rf").getFloat("time",r) + rfoffset1;
					}
				}
				DataBank partBank = null, trackBank = null, trackDetBank = null, ecalBank = null, cherenkovBank = null, scintillBank = null;
				DataBank trajBank = null, ltccadcBank = null, ltccClusters = null, bandhits = null, bandlaser = null;
				if(userTimeBased){
				if(event.hasBank("REC::Particle")) partBank = event.getBank("REC::Particle");
				if(event.hasBank("REC::Event")) starttime = event.getBank("REC::Event").getFloat("startTime",0);
				if(event.hasBank("REC::Track"))trackBank = event.getBank("REC::Track");
				if(event.hasBank("TimeBasedTrkg::TBTracks"))trackDetBank = event.getBank("TimeBasedTrkg::TBTracks");
				if(event.hasBank("REC::Calorimeter")) ecalBank = event.getBank("REC::Calorimeter");
				if(event.hasBank("REC::Cherenkov"))cherenkovBank = event.getBank("REC::Cherenkov");
				if(event.hasBank("REC::Scintillator"))scintillBank = event.getBank("REC::Scintillator");
				if(event.hasBank("REC::Traj"))trajBank = event.getBank("REC::Traj");
				if(event.hasBank("LTCC::adc"))ltccadcBank = event.getBank("LTCC::adc");
				if(event.hasBank("LTCC::clusters"))ltccClusters = event.getBank("LTCC::clusters");
				if(event.hasBank("BAND::hits")) {
					bandhits = event.getBank("BAND::hits");
				}
				if(event.hasBank("BAND::laser")) {
					bandlaser = event.getBank("BAND::laser");
				}

			}
				if(!userTimeBased){
				if(event.hasBank("RECHB::Particle"))partBank = event.getBank("RECHB::Particle");
				if(event.hasBank("RECHB::Track"))trackBank = event.getBank("RECHB::Track");
				if(event.hasBank("HitBasedTrkg::HBTracks"))trackDetBank = event.getBank("HitBasedTrkg::HBTracks");
				if(event.hasBank("RECHB::Calorimeter")) ecalBank = event.getBank("RECHB::Calorimeter");
				if(event.hasBank("RECHB::Cherenkov"))cherenkovBank = event.getBank("RECHB::Cherenkov");
				if(event.hasBank("RECHB::Scintillator"))scintillBank = event.getBank("RECHB::Scintillator");
				if(event.hasBank("RECHB::Traj"))trajBank = event.getBank("RECHB::Traj");
				if(event.hasBank("LTCC::adc"))ltccadcBank = event.getBank("LTCC::adc");
				if(event.hasBank("LTCC::clusters"))ltccClusters = event.getBank("LTCC::clusters");
				if(event.hasBank("BAND::hits")) bandhits = event.getBank("BAND::hits");
				if(event.hasBank("BAND::laser")) bandlaser = event.getBank("BAND::laser");
                	}

			if(bandhits!=null) {
				fill_Histograms_Hits(bandhits, 0); //Fill only histograms for real BAND hits
			}
			if(bandlaser!=null) {
				fill_Histograms_Hits(bandlaser,1); //Fill only histograms for BAND laser hits
			}
		}
	}

        public void plot() {
		EmbeddedCanvas can_BAND  = new EmbeddedCanvas();
		can_BAND.setSize(2500,2000);
		can_BAND.divide(2,2);
		can_BAND.setAxisTitleSize(24);
		can_BAND.setAxisFontSize(30);
		can_BAND.setTitleSize(24);

		H_BAND_adcCor[0].setLineColor(2);
		H_BAND_meantimeadc[0].setLineColor(2);
		H_BAND_meantimetdc[0].setLineColor(2);
		H_BAND_lasertimeadc[0].setLineColor(2);
		H_BAND_adcCor[1].setLineColor(4);
        H_BAND_meantimeadc[1].setLineColor(4);
        H_BAND_meantimetdc[1].setLineColor(4);
        H_BAND_lasertimeadc[1].setLineColor(4);
		can_BAND.cd(0);can_BAND.draw(H_BAND_adcCor[0]);can_BAND.draw(H_BAND_adcCor[1],"same");
		can_BAND.cd(1);can_BAND.draw(H_BAND_meantimeadc[0]);can_BAND.draw(H_BAND_meantimeadc[1],"same");
		can_BAND.cd(2);can_BAND.draw(H_BAND_meantimetdc[0]);can_BAND.draw(H_BAND_meantimetdc[1],"same");
		can_BAND.cd(3);can_BAND.draw(H_BAND_lasertimeadc[0]);can_BAND.draw(H_BAND_lasertimeadc[1],"same");
                can_BAND.save(String.format(outputDir+"/BAND.png"));
                System.out.println(String.format("saved "+outputDir+"/BAND.png"));

	}
	public static void main(String[] args) {
                System.setProperty("java.awt.headless", "true");
                GStyle.setPalette("kRainBow");
                int count = 0;
                int runNum = 0;
		boolean useTB = true;
                String filelist = "list_of_files.txt";
                if(args.length>0)runNum=Integer.parseInt(args[0]);
                if(args.length>1)filelist = args[1];
                int maxevents = 20000000;
                if(args.length>2)maxevents=Integer.parseInt(args[2]);
		float Eb =6.42f;//10.6f;
                if(args.length>3)Eb=Float.parseFloat(args[3]);
		if(args.length>4)if(Integer.parseInt(args[4])==0)useTB=false;
                String outputDir = runNum > 0 ? "plots"+runNum : "plots";
                BAND ana = new BAND(runNum,outputDir,Eb,useTB);
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
                for (String runstrg : toProcessFileNames) if( count<maxevents ){
                        progresscount++;
                        System.out.println(String.format(">>>>>>>>>>>>>>>> %s",runstrg));
                        File varTmpDir = new File(runstrg);
                        if(!varTmpDir.exists()){System.out.println("FILE DOES NOT EXIST");continue;}
                        System.out.println("READING NOW "+runstrg);
                        HipoDataSource reader = new HipoDataSource();
                        reader.open(runstrg);
                        int filecount = 0;
                        while(reader.hasEvent()&& count<maxevents ) {
                                DataEvent event = reader.getNextEvent();
                                ana.processEvent(event);
                                filecount++;count++;
                                if(count%10000 == 0) System.out.println(count/1000 + "k events (this is BAND analysis on "+runstrg+") ; progress : "+progresscount+"/"+filetot);
                        }
                        reader.close();
                }
                System.out.println("Total : " + count + " events");
                ana.plot();
                ana.write();
        }

	public void write() {
		TDirectory dirout = new TDirectory();
		dirout.mkdir("/BAND/");
		dirout.cd("/BAND/");
		for(int j=0;j<2;j++){
			dirout.addDataSet(H_BAND_adcCor[j], H_BAND_meantimeadc[j], H_BAND_meantimetdc[j], H_BAND_lasertimeadc[j]);
		}
                if(runNum>0) dirout.writeFile(outputDir+"/out_BAND_"+runNum+".hipo");
                else         dirout.writeFile(outputDir+"/out_BAND.hipo");
	}

}
