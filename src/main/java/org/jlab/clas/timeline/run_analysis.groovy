package org.jlab.clas.timeline.analysis
import org.jlab.clas.timeline.util.RunDependentCut

import org.jlab.groot.data.TDirectory

// define timeline engines
def engines = [
  out_ALERT: [
   new alert_atof_tdc_sector_0_4(),
   new alert_atof_tdc_sector_5_9(),
   new alert_atof_tdc_sector_10_14(),
   new alert_atof_tdc_minus_start_time_sector_0_4(),
   new alert_atof_tdc_minus_start_time_sector_5_9(),
   new alert_atof_tdc_minus_start_time_sector_10_14(),
   new alert_atof_tot_sector_0_4(),
   new alert_atof_tot_sector_5_9(),
   new alert_atof_tot_sector_10_14(),
   new alert_start_time(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_0(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_1(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_2(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_3(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_4(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_5(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_6(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_7(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_8(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_9(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_10(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_11(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_12(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_13(),
  //  new alert_atof_tdc_minus_start_time_vs_tot_sector_14(),
   new alert_ahdc_adc_layer_number1(),
   new alert_ahdc_adc_layer_number2(),
   new alert_ahdc_adc_layer_number3(),
   new alert_ahdc_adc_layer_number4(),
   new alert_ahdc_adc_layer_number5(),
   new alert_ahdc_adc_layer_number6(),
   new alert_ahdc_adc_layer_number7(),
   new alert_ahdc_adc_layer_number8(),
   new alert_ahdc_time_layer_number1(),
   new alert_ahdc_residual()
   ],
//  out_BAND: [new band_adccor(),
//    new band_lasertime(),
//    new band_meantimeadc(),
//    new band_meantimetdc()],
//  out_monitor: [new bmt_Occupancy(),
//    new bmt_OnTrkLayers(),
//    new bst_Occupancy(),
//    new bst_OnTrkLayers(),
//    new central_Km_num(),
//    new central_pim_num(),
//    new central_prot_num(),
//    new central_Kp_num(),
//    new central_pip_num(),
//    new cvt_Vz_negative(),
//    new cvt_Vz_positive(),
//    new cvt_chi2_elec(),
//    new cvt_chi2_neg(),
//    new cvt_chi2_pos(),
//    new cvt_chi2norm(),
//    new cvt_ndf(),
//    new cvt_p(),
//    new cvt_pathlen(),
//    new cvt_pt(),
//    new cvt_trks(),
//    new cvt_trks_neg(),
//    new cvt_trks_neg_rat(),
//    new cvt_trks_pos(),
//    new cvt_trks_pos_rat(),
//    new cvt_d0_mean_pos(),
//    new cvt_d0_sigma_pos(),
//    new cvt_d0_max_pos(),
//    new rat_Km_num(),
//    new rat_neg_num(),
//    new rat_pos_num(),
//    new rat_Kp_num(),
//    new rat_neu_num(),
//    new rat_prot_num(),
//    new rat_elec_num(),
//    new rat_pim_num(),
//    new rat_muon_num(),
//    new rat_pip_num(),
//    new forward_Tracking_Elechi2(),
//    new forward_Tracking_EleVz(),
//    new forward_Tracking_Poschi2(),
//    new forward_Tracking_PosVz(),
//    new forward_Tracking_Negchi2(),
//    new forward_Tracking_NegVz(),
//    new ec_Sampl(),
//    new ec_gg_m(),
//    new ec_pcal_time(),
//    new ec_ecin_time(),
//    new ec_ecou_time(),
//    new ltcc_nphe_sector(),
//    new rftime_diff(),
//    new rftime_pim_FD(),
//    new rftime_pim_CD(),
//    new rftime_pip_FD(),
//    new rftime_pip_CD(),
//    new rftime_elec_FD(),
//    new rftime_diff_corrected(),
//    new rftime_prot_FD(),
//    new rftime_prot_CD(),
//    new epics_xy(),
//    new epics_hall_weather()],
//  out_CND: [new cnd_MIPS_dE_dz(),
//    new cnd_time_neg_vtP(),
//    new cnd_zdiff()],
//  out_CTOF: [new ctof_edep(),
//    new ctof_time(),
//    new ctof_tdcadc(),
//  ],
//  out_FT: [new ftc_pi0_mass(),
//    new ftc_time_charged(),
//    new ftc_time_neutral(),
//    new fth_MIPS_energy(),
//    new fth_MIPS_time(),
//    new fth_MIPS_energy_board(),
//    new fth_MIPS_time_board()],
//  out_HTCC: [new htcc_nphe_ring_sector(),
//    new htcc_nphe_sector(),
//    new htcc_vtimediff(),
//    new htcc_vtimediff_sector(),
//    new htcc_vtimediff_sector_ring(),
//    new htcc_npheAll()],
//  out_LTCC: [new ltcc_had_nphe_sector()],
//  out_TOF: [new ftof_edep_p1a_smallangles(),
//    new ftof_edep_p1a_midangles(),
//    new ftof_edep_p1a_largeangles(),
//    new ftof_edep_p1b_smallangles(),
//    new ftof_edep_p1b_midangles(),
//    new ftof_edep_p1b_largeangles(),
//    new ftof_edep_p2(),
//    new ftof_time_p1a(),
//    new ftof_time_p1b(),
//    new ftof_time_p2(),
//    new ftof_tdcadc_p1a(),
//    new ftof_tdcadc_p1b(),
//    new ftof_tdcadc_p2(),
//    new ftof_ctof_vtdiff(),
//    new dc_residuals_sec(),
//    new dc_residuals_sec_sl(),
//    //new dc_residuals_sec_rescut(),
//    new dc_residuals_sec_sl_rescut(),
//    new dc_t0_sec_sl(),
//    new dc_t0_even_sec_sl(),
//    new dc_t0_odd_sec_sl(),
//    new dc_tmax_sec_sl()],
//  out_RICH: [new rich_dt_m(),
//       new rich_trk_m(),
//       new rich_etac_dir_m(),
//       new rich_etac_plan_m(),
//       new rich_etac_sphe_m(),
//       new rich_npho_dir_m(),
//       new rich_npho_plan_m(),
//       new rich_npho_sphe_m(),
//       new rich_npim_m(),
//       new rich_npip_m(),
//       new rich_nkm_m(),
//       new rich_nkp_m(),
//       new rich_npro_m(),
//       new rich_npbar_m()],
//  out_HELICITY: [new helicity()],
//  out_TRIGGER: [new trigger()],
]


// parse arguments
if(args.any{it=="--timelines"}) {
  engines.values().flatten().each{
    println(it.getClass().getSimpleName())
  }
  System.exit(0)
}
if(args.length != 2) {
  System.err.println "ARGUMENTS: [timeline] [input_dir]"
  System.err.println "use --timelines for a list of available timelines"
  System.exit(101)
}
def (timelineArg, inputDirArg) = args

// check the timeline argument
def eng = engines.collectMany{key,engs->engs.collect{[key,it]}}
  .find{name,eng->eng.getClass().getSimpleName()==timelineArg}
if(eng == null) {
  System.err.println("error: timeline '$timelineArg' is not defined")
  System.exit(100)
}

// get list of input HIPO histogram files
def (name,engine) = eng
def inputDir = new File(inputDirArg)
println([name,timelineArg,engine.getClass().getSimpleName(),inputDir])
def fnames = []
inputDir.traverse {
  if(it.name.endsWith('.hipo') && it.name.contains(name))
    fnames.add(it.absolutePath)
}

// loop over input HIPO histogram files
def allow_timeline = false
fnames.sort().each{ fname ->
  try{
    println("debug: "+engine.getClass().getSimpleName()+" started $fname")

    // get run number from directory name
    def dname = fname.split('/')[-2]
    def m = dname =~ /\d+/
    def run = m[0].toInteger()

    // exclude certain run ranges from certain timelines
    def allow_run = true
    def dataset = RunDependentCut.findDataset(run)
    if(dataset == 'rgl') {
      if( timelineArg ==~ /^bmt.*/ ||
          timelineArg ==~ /^bst.*/ ||
          timelineArg ==~ /^cen.*/ ||
          timelineArg ==~ /^cvt.*/ ) { allow_run = false }
    }
    else { // not RG-L
      if(timelineArg ==~ /^alert.*/) { allow_run = false }
    }

    // run the analysis for this run
    if(allow_run) {
      allow_timeline = true // allow the timeline if at least one run is allowed
      TDirectory dir = new TDirectory()
      dir.readFile(fname)
      engine.processRun(dir, run)
      println("debug: "+engine.getClass().getSimpleName()+" finished $fname")
    }
    else {
      println("debug: "+engine.getClass().getSimpleName()+" excludes run $run")
    }

  } catch(Exception ex) {
    System.err.println("error: "+engine.getClass().getSimpleName()+" didn't process $fname, due to exception:")
    ex.printStackTrace()
    System.exit(100)
  }
}

// write the timeline HIPO file
if(allow_timeline) {
  engine.write()
  println("debug: "+engine.getClass().getSimpleName()+" ended")
}
else {
  println("debug: "+engine.getClass().getSimpleName()+" was not produced, since all runs were excluded")
}
