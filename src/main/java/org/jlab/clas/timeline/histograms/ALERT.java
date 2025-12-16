package org.jlab.clas.timeline.histograms;

import java.util.*;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
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
  public H1F[] ATOF_Time;
  public H1F[] ADC, AHDC_RESIDUAL, AHDC_TIME;//AHDC-related-histograms
  private H1F bits;

  public IndexedTable rfTable;

  public ConstantsManager ccdb;
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

    ATOF_Time = new H1F[660];// ATOF Time Histograms

    for (int index = 0; index < 660; index++) {
      int sector    = 0;
      int layer     = 0;
      int component = 0;

      sector    = index / (11 * 4);
      layer     = (index % (11 * 4)) / 11;
      component = index % 11;

      ATOF_Time[index] = new H1F(String.format("ATOF_Time_sector%d_layer%d_component%d", sector, layer, component), String.format("ATOF Time sector%d layer%d component%d", sector, layer, component), 300, 85, 100);
      ATOF_Time[index].setTitleX("ATOF Time (ns)");
      ATOF_Time[index].setTitleY("Counts");
      ATOF_Time[index].setFillColor(4);
    }

    //AHDC ADC Histograms
    ADC           = new H1F[576];
    AHDC_RESIDUAL = new H1F[8];
    AHDC_TIME     = new H1F[576];

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
      AHDC_TIME[index] = new H1F(String.format("AHDC_TIME_layer%d_wire_number%d", layer, wire_number), String.format("AHDC Time layer %d wire number%d", layer, wire_number), 450, -10.f, 440.0f);
      AHDC_TIME[index].setTitleX("AHDC TIME (ns)");
      AHDC_TIME[index].setTitleY("Counts");
      AHDC_TIME[index].setFillColor(4);
    }

    for (int k=0; k<8; k++){

      AHDC_RESIDUAL[k] = new H1F(String.format("AHDC_RESIDUAL_layer%d", layer_encoding[k]), String.format("AHDC Residual layer%d", layer_encoding[k]), 300, -20.0f, 10.0f);
      AHDC_RESIDUAL[k].setTitleX("AHDC RESIDUAL (mm)");
      AHDC_RESIDUAL[k].setTitleY("Counts");
      AHDC_RESIDUAL[k].setFillColor(4);
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
      int component   = ahdc_hits.getInt("wire", loop);
      float time      = (float) ahdc_hits.getDouble("time", loop);
      float residual  = (float) ahdc_hits.getDouble("residual", loop);
      int index = 0;

      layer = superlayer * 10 + layer;
      int layer_number = Arrays.asList(boxed_encoding).indexOf(layer) + 1;
      index = component - 1 + layer_wires_cumulative[layer_number - 1];

      if (Math.signum(residual) != 0) AHDC_RESIDUAL[layer_number - 1].fill(residual);
      AHDC_TIME[index].fill(time);
    }
  }

  public void fillATOF_hits(DataBank atof_hits) {
    int rows = atof_hits.rows();
    for (int loop = 0; loop < rows; loop++) {
      int sector    = atof_hits.getInt("sector", loop);
      int layer     = atof_hits.getInt("layer", loop);
      int component = atof_hits.getInt("component", loop);
      float time       = atof_hits.getFloat("time", loop);
      int index     = sector * 44 + layer * 11 + component;

      ATOF_Time[index].fill(time);

    }
  }

  public void processEvent(DataEvent event) {

    DataBank recBankEB = null;
    DataBank recEvenEB = null;
    DataBank runConfig = null;
    DataBank atof_hits  = null;
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
    if (event.hasBank("ATOF::hits")) {
      atof_hits = event.getBank("ATOF::hits");
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

    if (atof_hits != null) {
      fillATOF_hits(atof_hits);
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
    for (int index = 0; index < 660; index++) {
      dirout.addDataSet(ATOF_Time[index]);
    }
    for (int index = 0; index < 576; index++) {
      dirout.addDataSet(ADC[index], AHDC_TIME[index]);
    }
    for (int index = 0; index < 8; index++){
      dirout.addDataSet(AHDC_RESIDUAL[index]);
    }

    dirout.mkdir("/TRIGGER/");
    dirout.cd("/TRIGGER/");
    dirout.addDataSet(bits);
    if(runNum>0) dirout.writeFile(outputDir+"/out_ALERT_"+runNum+".hipo");
    else         dirout.writeFile(outputDir+"/out_ALERT.hipo");
  }

}
