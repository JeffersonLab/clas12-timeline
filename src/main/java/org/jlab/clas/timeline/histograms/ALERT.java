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
  public H1F[] ATOF_Time, ATOF_Time_sl, ATOF_z, ATOF_z_sl, ATOF_z_c4, ATOF_z_c4_sl;
  public H1F[] AHDC_RESIDUAL, AHDC_RESIDUAL_LR;//AHDC-related-histograms
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

    ATOF_Time = new H1F[11];// ATOF Time Histograms
    ATOF_Time_sl = new H1F[660];// ATOF Time Histograms
    ATOF_z = new H1F[1];// ATOF Time Histograms
    ATOF_z_c4 = new H1F[1];// ATOF Time Histograms
    ATOF_z_sl = new H1F[60];// ATOF z Histograms
    ATOF_z_c4_sl = new H1F[60];// ATOF z Histograms
      
    for (int component = 0; component < 11; component++) {
      ATOF_Time[component] = new H1F(String.format("ATOF_Time_component%02d", component), String.format("ATOF Time component%02d", component), 100, -5, 5);
      ATOF_Time[component].setTitleX("ATOF Time (ns)");
      ATOF_Time[component].setTitleY("Counts");
      ATOF_Time[component].setFillColor(4);
    }
      ATOF_z[0]= new H1F(String.format("ATOF_z_combined"), String.format("ATOF z"), 300,-300,300);
      ATOF_z[0].setTitleX("ATOF z (mm)");
      ATOF_z[0].setTitleY("Counts");
      ATOF_z[0].setFillColor(4);
      ATOF_z_c4[0]= new H1F(String.format("ATOF_z_combined_c4"), String.format("ATOF z with c4"), 40,-200,200);
      ATOF_z_c4[0].setTitleX("ATOF z (mm)");
      ATOF_z_c4[0].setTitleY("Counts");
      ATOF_z_c4[0].setFillColor(4);
    for(int sector=0;sector<15;sector++){
          for(int layer=0;layer<4;layer++){
              int gsector=sector*4+layer;
              ATOF_z_sl[gsector] = new H1F(String.format("ATOF_z_sector%02d_layer%02d", sector,layer), String.format("ATOF z sector%02d layer %2d", sector,layer), 300,-300,300);
              ATOF_z_sl[gsector].setTitleX("ATOF z (mm)");
              ATOF_z_sl[gsector].setTitleY("Counts");
              ATOF_z_sl[gsector].setFillColor(4);

              ATOF_z_c4_sl[gsector] = new H1F(String.format("ATOF_z_c4_sector%02d_layer%02d", sector,layer), String.format("ATOF z with C4 sector%02d layer %2d", sector,layer), 40,-200,200);
              ATOF_z_c4_sl[gsector].setTitleX("ATOF z (mm)");
              ATOF_z_c4_sl[gsector].setTitleY("Counts");
              ATOF_z_c4_sl[gsector].setFillColor(4);
              
              for (int component = 0; component < 11; component++) {
                  int gcomponent = gsector*11+component;
                  ATOF_Time_sl[gcomponent] = new H1F(String.format("ATOF_Time_sector%02d_layer%02d_component%02d",sector,layer, component), String.format("ATOF Time sector%02d layer%02d component%02d", sector,layer,component), 100, -5, 5);
                  ATOF_Time_sl[gcomponent].setTitleX("ATOF Time (ns)");
                  ATOF_Time_sl[gcomponent].setTitleY("Counts");
                  ATOF_Time_sl[gcomponent].setFillColor(4);
              }


          }
    }


    //AHDC ADC Histograms
    AHDC_RESIDUAL = new H1F[576];
    AHDC_RESIDUAL_LR = new H1F[576];

    for (int index = 0; index<576; index++) {
      int layer_number = 0;
      int wire_number = 0;

      for (int j=0; j<8; j++){
        if (index < layer_wires_cumulative[j+1]){
          layer_number = j + 1;
          wire_number = index + 1 - layer_wires_cumulative[j];
          break;
        }
      }
      AHDC_RESIDUAL[index] = new H1F(String.format("AHDC_RESIDUAL_layer%d_wire_number%02d", layer_number, wire_number), String.format("AHDC Residual layer%d wire number%02d", layer_number, wire_number), 100, -3.0f, 3.0f);
      AHDC_RESIDUAL[index].setTitleX("AHDC RESIDUAL (mm)");
      AHDC_RESIDUAL[index].setTitleY("Counts");
      AHDC_RESIDUAL[index].setFillColor(4);
      AHDC_RESIDUAL_LR[index] = new H1F(String.format("AHDC_RESIDUAL_LR_layer%d_wire_number%02d", layer_number, wire_number), String.format("AHDC Residual SL layer%d wire number%02d", layer_number, wire_number), 100, -3.0f, 3.0f);
      AHDC_RESIDUAL_LR[index].setTitleX("AHDC RESIDUAL LR (mm)");
      AHDC_RESIDUAL_LR[index].setTitleY("Counts");
      AHDC_RESIDUAL_LR[index].setFillColor(4);
    }

    // Trigger bits
    bits = new H1F("bits", "bits",65,0,65);
    bits.getDataX(0);
    bits.getEntries();
    bits.getMaximumBin();
    bits.getAxis().getNBins();

  }

  public void fillAHDC_hits(DataBank ahdc_kftrack, DataBank ahdc_hits) {
    /**
    * Fill the histograms related to AHDC::hits (ex) residual)
    * for hits that satisfy `good_track` condition.
    * A good track is associated with many hits.
    * While how many is subject to more study, the RG-L's suggestions were 5 or 6
    * —essentially the higest number that ensures a good amount of statistics.
    * Here, we start with "6" being the lower bound of n_hits.
    * `good_track`: the number of rows with the same AHDC::hits:trackid is greater than or equal to 7.
    * Instead of looping over AHDC::hits:trackid, AHDC::kftrack:n_hits was used to define `good_track`.
    * `track_matching`: AHDC::hits:trackid == `AHDC::kftrack:nhits
    * Then, AHDC::hits:trackid == AHDC::kftrack:trackid is required for the sanity check.
    * The dominant AHDC::hits entries are when AHDC::hits:trackid==-1, where there is no associated AHDC::kftrack.
    * `exists_track`: AHDC::hits:trackid==-1
    * The `no_track` condition is redundant because `good_track` ensures `~no_track`.
    * But, for the first time commit, SL prefers to check `~no_track`
    * SL also keeps `no_track_legacy`, which means that
    * `no_track_legacy`: Both AHDC::hits:residual and AHDC::hits:residual_LR are nonzero.
    * Note: `exists_track and `no_track_legacy` can be either removed or changed to assert statement, for more advanced AHDC reconstruction.
    * 
    * @param ahdc_kftrack AHDC::kftrack bank in the same event
    * @param ahdc_hits    AHDC::hits bank in the same event
    */


    int kftrack_rows = ahdc_kftrack.rows();
    for (int kftrack_loop = 0; kftrack_loop < kftrack_rows; kftrack_loop++) {
      int kftrack_n_hits  = ahdc_kftrack.getInt("n_hits", kftrack_loop);
      int kftrack_trackid = ahdc_kftrack.getInt("trackid", kftrack_loop);
      if (kftrack_n_hits <= 6 ) continue; // `good_track`
      int hit_rows = ahdc_hits.rows();
      for (int hit_loop = 0; hit_loop < hit_rows; hit_loop++) {
        int hit_layer       = ahdc_hits.getByte("layer", hit_loop);
        int hit_superlayer  = ahdc_hits.getByte("superlayer", hit_loop);
        int hit_component   = ahdc_hits.getInt("wire", hit_loop);
        int hit_trackid     = ahdc_hits.getInt("trackid", hit_loop);

        if (kftrack_trackid != hit_trackid) continue; // `track_matching`
        if (hit_trackid == -1) continue; // `exists_track`

        float hit_residual  = (float) ahdc_hits.getDouble("residual", hit_loop);
        float hit_residual_LR  = (float) ahdc_hits.getDouble("residual_LR", hit_loop);
        
        int index = 0;
    
        hit_layer = hit_superlayer * 10 + hit_layer;
        int hit_layer_number = Arrays.asList(boxed_encoding).indexOf(hit_layer) + 1;
        index = hit_component - 1 + layer_wires_cumulative[hit_layer_number - 1];
    
        if (Math.signum(hit_residual) * Math.signum(hit_residual_LR) == 0) continue; // `no_track_legacy`
        AHDC_RESIDUAL[index].fill(hit_residual);
        AHDC_RESIDUAL_LR[index].fill(hit_residual_LR);
      }
    }
  }

  public void fillATOF_hits(DataBank atof_hits) {
    int rows = atof_hits.rows();

    // First pass: collect which gsectors have a hit on component 4
    Set<Integer> gsectors_with_c4 = new HashSet<>();
    Map<Integer, Float> barTimeByGsector = new HashMap<>();
    for (int loop = 0; loop < rows; loop++) {
      if (atof_hits.getInt("component", loop) == 4) {
        int sector  = atof_hits.getInt("sector", loop);
        int layer   = atof_hits.getInt("layer",  loop);
        float time  = atof_hits.getFloat("time", loop);
        if (Math.abs(time) < 1) {
          gsectors_with_c4.add(sector * 4 + layer);
        }
      }
      if (atof_hits.getInt("component", loop) == 10) {
          int sector = atof_hits.getInt("sector", loop);
          int layer  = atof_hits.getInt("layer",  loop);
          float time = atof_hits.getFloat("time", loop);
          int gsector = sector * 4 + layer;
          barTimeByGsector.put(gsector, time);
      }
    }

    // Second pass: fill all histograms
    for (int loop = 0; loop < rows; loop++) {
      int sector     = atof_hits.getInt("sector",    loop);
      int layer      = atof_hits.getInt("layer",     loop);
      int component  = atof_hits.getInt("component", loop);
      float time     = atof_hits.getFloat("time",    loop);
      int gsector    = sector * 4 + layer;
      int gcomponent = gsector * 11 + component;

      Float barTime = barTimeByGsector.get(gsector);
      if (barTime != null && Math.abs(barTime) < 5) {
          ATOF_Time[component].fill(time);
          ATOF_Time_sl[gcomponent].fill(time);
      }

      if (component == 10 && Math.abs(time) < 10) {
        float z = atof_hits.getFloat("z", loop);
        ATOF_z[0].fill(z);
        ATOF_z_sl[gsector].fill(z);
        if (gsectors_with_c4.contains(gsector)) {
          ATOF_z_c4[0].fill(z);
          ATOF_z_c4_sl[gsector].fill(z);
        }
      }
    }
  }

  public void processEvent(DataEvent event) {

    DataBank recBankEB = null;
    DataBank recEvenEB = null;
    DataBank runConfig = null;
    DataBank atof_hits = null;
    DataBank ahdc_kftrack = null;
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
    if (event.hasBank("AHDC::kftrack")){
      ahdc_kftrack = event.getBank("AHDC::kftrack");
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

    if (ahdc_kftrack != null && ahdc_hits != null) {
      fillAHDC_hits(ahdc_kftrack, ahdc_hits);
    }

  }

  public void write() {
    TDirectory dirout = new TDirectory();
    dirout.mkdir("/ALERT/");
    dirout.cd("/ALERT/");
    for (int component = 0; component < 11; component++) {
      dirout.addDataSet(ATOF_Time[component]);
    }
    dirout.addDataSet(ATOF_z[0]);
    dirout.addDataSet(ATOF_z_c4[0]);
    for (int gsector = 0; gsector < 60; gsector++) {
      dirout.addDataSet(ATOF_z_sl[gsector]);
      dirout.addDataSet(ATOF_z_c4_sl[gsector]);
    }
    for (int gcomponent = 0; gcomponent < 660; gcomponent++) {
      dirout.addDataSet(ATOF_Time_sl[gcomponent]);
    }
    for (int index = 0; index < 576; index++) {
      dirout.addDataSet(AHDC_RESIDUAL[index], AHDC_RESIDUAL_LR[index]);
    }

    dirout.mkdir("/TRIGGER/");
    dirout.cd("/TRIGGER/");
    dirout.addDataSet(bits);
    if(runNum>0) dirout.writeFile(outputDir+"/out_ALERT_"+runNum+".hipo");
    else         dirout.writeFile(outputDir+"/out_ALERT.hipo");
  }

}
