package org.jlab.clas.timeline.histograms;

import java.util.*;

import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.ConstantsManager;
/**
 *
 * @author sangbaek
 */
public class ALERT {

  boolean userTimeBased;
  public int runNum, triggerPID;
  public long trigger_word;
  public String outputDir;

  public boolean hasRF;
  public double startTime, rfTime;

  public double rfPeriod;
  public int rf_large_integer;

  //Hodoscope
  public H1F[] TDC, TDC_wrt_start_time, TOT;; //ATOF
  public H1F[] ADC;//AHDC
  private H1F bits;

  public IndexedTable rfTable;

  public ConstantsManager ccdb;
  private float tdc_bin_time = 0.015625f; // ns/bin

  public ALERT(int reqrunNum, String reqOutputDir, float reqEb, boolean reqTimeBased) {
    runNum = reqrunNum;
    outputDir = reqOutputDir;
    userTimeBased = reqTimeBased;

    startTime = -1000;
    rfTime = -1000;
    triggerPID = 0;

    rfPeriod = 4.008;
    ccdb = new ConstantsManager();
    ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo", "/calibration/eb/rf/config"}));
    rfTable = ccdb.getConstants(runNum, "/calibration/eb/rf/config");
    if (rfTable.hasEntry(1, 1, 1)) {
      System.out.println(String.format("RF period from ccdb for run %d: %f", runNum, rfTable.getDoubleValue("clock", 1, 1, 1)));
      rfPeriod = rfTable.getDoubleValue("clock", 1, 1, 1);
    }
    rf_large_integer = 1000;

    //ATOF TDC Histograms
    TDC = new H1F[720];
    TDC_wrt_start_time = new H1F[720];
    TOT = new H1F[720];

    for (int index = 0; index < 720; index++) {
      int sector    = 0;
      int layer     = 0;
      int component = 0;
      int order     = 0;

      sector    = index / (12 * 4);
      layer     = (index % (12 * 4)) / 12;
      component = index % 12;
      order = 0;
      if (component == 11){
        component = 10;
        order = 1;
      }
      TDC[index] = new H1F(String.format("TDC_sector%d_layer%d_component%d_order%d", sector, layer, component, order), String.format("TDC sector%d layer%d component%d order%d", sector, layer, component, order), 550, 0.0, 550.0);
      TDC[index].setTitleX("TDC (ns)");
      TDC[index].setTitleY("Counts");
      TDC[index].setFillColor(4);
      TDC_wrt_start_time[index] = new H1F(String.format("TDC_wrt_start_time_sector%d_layer%d_component%d_order%d", sector, layer, component, order), String.format("TDC - start time sector%d layer%d component%d order%d", sector, layer, component, order), 550, 0.0, 550.0);
      TDC_wrt_start_time[index].setTitleX("TDC - start time (ns)");
      TDC_wrt_start_time[index].setTitleY("Counts");
      TDC_wrt_start_time[index].setFillColor(4);
      TOT[index] = new H1F(String.format("TOT_sector%d_layer%d_component%d_order%d", sector, layer, component, order), String.format("TOT sector%d layer%d component%d order%d", sector, layer, component, order), 500, 0.0, 50000.0);
      TOT[index].setTitleX("TOT (ns)");
      TOT[index].setTitleY("Counts");
      TOT[index].setFillColor(4);
    }

    // //AHDC ADC Histograms
    // ADC = new H1F[576];

    // for (int index = 0; index<576; index++) {
    //   int layer = 0;

    // }

    // Trigger bits
    bits = new H1F("bits", "bits",65,0,65);
    bits.getDataX(0);
    bits.getEntries();
    bits.getMaximumBin();
    bits.getAxis().getNBins();

  }

  // public void fillAHDC(DataBank atof_adc) {
  // }

  public void fillATOF(DataBank atof_tdc) {
    int rows = atof_tdc.rows();
    for (int loop = 0; loop < rows; loop++) {
      int sector    = atof_tdc.getInt("sector", loop);
      int layer     = atof_tdc.getInt("layer", loop);
      int component = atof_tdc.getInt("component", loop);
      int order     = atof_tdc.getInt("order", loop);
      int tdc       = atof_tdc.getInt("TDC", loop);
      int tot       = atof_tdc.getInt("ToT", loop);
      int index     = sector * 48 + layer * 12 + component + order;

      TDC[index].fill(tdc*tdc_bin_time);
      if (startTime!=-1000.0 && triggerPID == 11) TDC_wrt_start_time[index].fill(tdc*tdc_bin_time - startTime);
      TOT[index].fill(tot*tdc_bin_time);
    }
  }

  public void processEvent(DataEvent event) {

    DataBank recBankEB = null;
    DataBank recEvenEB = null;
    DataBank runConfig = null;
    DataBank atof_tdc = null;
    DataBank ahdc_adc = null;

    if (event.hasBank("REC::Particle")) {
      recBankEB = event.getBank("REC::Particle");
    }
    if (event.hasBank("REC::Event")) {
      recEvenEB = event.getBank("REC::Event");
    }
    if (event.hasBank("RUN::config")) {
      runConfig = event.getBank("RUN::config");
    }
    if (event.hasBank("ATOF::tdc")) {
      atof_tdc = event.getBank("ATOF::tdc");
    }

    if (event.hasBank("AHDC::adc")) {
      ahdc_adc = event.getBank("AHDC::adc");
    }

    if (runConfig!= null){
      trigger_word = runConfig.getLong("trigger", 0);
      bits.fill(64);
      for (int i=0; i<64; ++i){
        if ( 1 == ((trigger_word>>i)&1) ) {
          bits.fill(i);
        }
      }
    }

    if (recEvenEB != null) {
      startTime = recEvenEB.getFloat("startTime", 0);
      rfTime = recEvenEB.getFloat("RFTime", 0);
    }

    //Get trigger particle
    if (recBankEB != null) {
      triggerPID = recBankEB.getInt("pid", 0);
    }

    if (atof_tdc != null) {
      fillATOF(atof_tdc);
    }

  }

  public void write() {
    TDirectory dirout = new TDirectory();
    dirout.mkdir("/ALERT/");
    dirout.cd("/ALERT/");
    for (int index = 0; index < 720; index++) {
      dirout.addDataSet(TDC[index], TDC_wrt_start_time[index], TOT[index]);//atof histograms
    }
    dirout.mkdir("/TRIGGER/");
    dirout.cd("/TRIGGER/");
    dirout.addDataSet(bits);
    if(runNum>0) dirout.writeFile(outputDir+"/out_ALERT_"+runNum+".hipo");
    else         dirout.writeFile(outputDir+"/out_ALERT.hipo");
  }

}
