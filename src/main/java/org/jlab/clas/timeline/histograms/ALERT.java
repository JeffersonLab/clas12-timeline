package org.jlab.clas.timeline.histograms;

import java.util.*;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.ConstantsManager;
import java.lang.Math.signum;
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
  public H1F[] TDC, TDC_minus_start_time, TOT; //ATOF-related histograms
  public H2F[] TDC_minus_start_time_vs_TOT;
  public H1F START_TIME;//ATOF-related histogram
  public H1F[] ADC, AHDC_RESIDUAL, AHDC_TIME;//AHDC-related-histograms
  private H1F bits;

  public IndexedTable rfTable;

  public ConstantsManager ccdb;
  private float tdc_bin_time = 0.015625f; // ns/bin
  // private int[] layer_wires             = {47,  56,  56,  72,  72,  87,  87,  99};
  private int[] layer_wires_cumulative  = {0, 47, 103, 159, 231, 303, 390, 477, 576};
  private int[] layer_encoding          = {11, 21, 22, 31, 32, 41, 42, 51};
  private Integer[] boxed_encoding = Arrays.stream(layer_encoding).boxed().toArray(Integer[]::new);

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
    TDC_minus_start_time = new H1F[720];
    TOT = new H1F[720];
    TDC_minus_start_time_vs_TOT = new H2F[720];

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

      float tdc_offset = 0.0f;
      if (reqrunNum<21331) tdc_offset = 250.0f;

      TDC[index] = new H1F(String.format("TDC_sector%d_layer%d_component%d_order%d", sector, layer, component, order), String.format("TDC sector%d layer%d component%d order%d", sector, layer, component, order), 200, tdc_offset + 150.0, tdc_offset+ 250.0);
      TDC[index].setTitleX("TDC (ns)");
      TDC[index].setTitleY("Counts");
      TDC[index].setFillColor(4);
      TDC_minus_start_time[index] = new H1F(String.format("TDC_minus_start_time_sector%d_layer%d_component%d_order%d", sector, layer, component, order), String.format("TDC - start time sector%d layer%d component%d order%d", sector, layer, component, order), 200, tdc_offset + 50.0, tdc_offset + 150.0);
      TDC_minus_start_time[index].setTitleX("TDC - start time (ns)");
      TDC_minus_start_time[index].setTitleY("Counts");
      TDC_minus_start_time[index].setFillColor(4);
      TOT[index] = new H1F(String.format("TOT_sector%d_layer%d_component%d_order%d", sector, layer, component, order), String.format("TOT sector%d layer%d component%d order%d", sector, layer, component, order), 700, 0.0, 70.0);
      TOT[index].setTitleX("TOT (ns)");
      TOT[index].setTitleY("Counts");
      TOT[index].setFillColor(4);
      TOT[index] = new H1F(String.format("TOT_sector%d_layer%d_component%d_order%d", sector, layer, component, order), String.format("TOT sector%d layer%d component%d order%d", sector, layer, component, order), 700, 0.0, 70.0);
      TDC_minus_start_time_vs_TOT[index] = new H2F(String.format("TDC_minus_start_time_vs_TOT_sector%d_layer%d_component_%d_order_%d", sector, layer, component, order), String.format("TDC minus start time vs TOT sector%d layer%d component%d order%d", sector, layer, component, order), 70, 0.0, 70.0, 40, tdc_offset + 80.0, tdc_offset + 120.0);
      TDC_minus_start_time_vs_TOT[index].setTitleX("TOT (ns)");
      TDC_minus_start_time_vs_TOT[index].setTitleY("TDC - start time (ns)");
    }

    START_TIME = new H1F("start time","start time", 80, 80.0, 120.0);
    START_TIME.setTitle("Event start time when the start time is defined and the trigger particle is an electron");
    START_TIME.setTitleX("start time (ns)");

    //AHDC ADC Histograms
    ADC           = new H1F[576];
    AHDC_RESIDUAL = new H1F[8];
    AHDC_TIME     = new H1F[8];

    for (int index = 0; index<576; index++) {
      int layer = 0;
      int wire_number = 0;

      for (int j=0; j<8; j++){
        if (index < layer_wires_cumulative[j+1]){
          layer = layer_encoding[j];
          wire_number = index + 1 - layer_wires_cumulative[j];
          break;
        }
      }
      ADC[index] = new H1F(String.format("ADC_layer%d_wire_number%d", layer, wire_number), String.format("ADC layer%d wire number%d", layer, wire_number), 516, 0.0, 3612.0);
      ADC[index].setTitleX("ADC");
      ADC[index].setTitleY("Counts");
      ADC[index].setFillColor(4);
    }

    for (int k=0; k<8; k++){
      AHDC_RESIDUAL[k] = new H1F(String.format("AHDC_RESIDUAL_layer%d", k), String.format("AHDC Residual layer%d", k), 61, -6, 0.1);
      AHDC_RESIDUAL[k].setTitleX("AHDC RESIDUAL");
      AHDC_RESIDUAL[k].setTitleY("Counts");
      AHDC_RESIDUAL[k].setFillColor(4);

      AHDC_TIME[k] = new H1F(String.format("AHDC_TIME_layer%d", k), String.format("AHDC Time layer%d", k), 450, -400, 50.0);
      AHDC_TIME[k].setTitleX("AHDC TIME");
      AHDC_TIME[k].setTitleY("Counts");
      AHDC_TIME[k].setFillColor(4);
    }

    // Trigger bits
    bits = new H1F("bits", "bits",65,0,65);
    bits.getDataX(0);
    bits.getEntries();
    bits.getMaximumBin();
    bits.getAxis().getNBins();

  }

  public void fillAHDC(DataBank ahdc_adc) {
    int rows = ahdc_adc.rows();
    for (int loop = 0; loop < rows; loop++) {
      int layer        = ahdc_adc.getInt("layer", loop);
      int component    = ahdc_adc.getInt("component", loop);
      int adc          = ahdc_adc.getInt("ADC", loop);
      int index = 0;

      int layer_number = Arrays.asList(boxed_encoding).indexOf(layer) + 1;
      index = component - 1 + layer_wires_cumulative[layer_number - 1];

      ADC[index].fill(adc);
    }
  }

  public void fillAHDC_hits(DataBank ahdc_hits) {
    int rows = ahdc_hits.rows();
    for (int loop = 0; loop < rows; loop++) {
      int layer       = ahdc_hits.getByte("layer", loop);
      int superlayer  = ahdc_hits.getByte("superlayer", loop);
      float time      = (float) ahdc_hits.getDouble("time", loop);
      float residual  = (float) ahdc_hits.getDouble("residual", loop);

      layer = superlayer * 10 + layer;
      int layer_number = Arrays.asList(boxed_encoding).indexOf(layer);

      if (Math.signum(residual) != 0) AHDC_RESIDUAL[layer_number].fill(residual);
      AHDC_TIME[layer_number].fill(time);
    }
  }


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
      if (startTime!=-1000.0 && triggerPID == 11){
        START_TIME.fill(startTime);
        TDC_minus_start_time[index].fill(tdc*tdc_bin_time - startTime);
        TDC_minus_start_time_vs_TOT[index].fill(tot*tdc_bin_time, tdc*tdc_bin_time - startTime);
      }
      TOT[index].fill(tot*tdc_bin_time);
    }
  }

  public void processEvent(DataEvent event) {

    DataBank recBankEB = null;
    DataBank recEvenEB = null;
    DataBank runConfig = null;
    DataBank atof_tdc  = null;
    DataBank ahdc_adc  = null;
    DataBank ahdc_hits = null;

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

    if (event.hasBank("AHDC::hits")){
      ahdc_hits = event.getBank("AHDC::hits");
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

    if (ahdc_adc != null) {
      fillAHDC(ahdc_adc);
    }

    if (ahdc_hits != null) {
      fillAHDC_hits(ahdc_hits);
    }

  }

  public void write() {
    TDirectory dirout = new TDirectory();
    dirout.mkdir("/ALERT/");
    dirout.cd("/ALERT/");
    for (int index = 0; index < 720; index++) {
      dirout.addDataSet(TDC[index], TDC_minus_start_time[index], TOT[index], TDC_minus_start_time_vs_TOT[index]);//atof histograms
    }
    for (int index = 0; index < 576; index++) {
      dirout.addDataSet(ADC[index]);
    }
    for (int index = 0; index < 8; index++){
      dirout.addDataSet(AHDC_RESIDUAL[index], AHDC_TIME[index]);
    }

    dirout.addDataSet(START_TIME);
    dirout.mkdir("/TRIGGER/");
    dirout.cd("/TRIGGER/");
    dirout.addDataSet(bits);
    if(runNum>0) dirout.writeFile(outputDir+"/out_ALERT_"+runNum+".hipo");
    else         dirout.writeFile(outputDir+"/out_ALERT.hipo");
  }

}
