package org.jlab.clas.timeline.histograms;

import java.util.*;

import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.utils.groups.IndexedTable;
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
      DataBank bandhits = null, bandlaser = null;
      if(userTimeBased){
        if(event.hasBank("REC::Event")) starttime = event.getBank("REC::Event").getFloat("startTime",0);
        if(event.hasBank("BAND::hits")) {
          bandhits = event.getBank("BAND::hits");
        }
        if(event.hasBank("BAND::laser")) {
          bandlaser = event.getBank("BAND::laser");
        }

      }
      if(!userTimeBased){
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
