package org.jlab.clas.timeline.analysis

import org.jlab.groot.data.TDirectory

def engines = [
  out_BAND: [new band_adccor(),
    new band_lasertime(),
    new band_meantimeadc(),
    new band_meantimetdc()],
  out_monitor: [new bmt_Occupancy(),
    new bmt_OnTrkLayers(),
    new bst_Occupancy(),
    new bst_OnTrkLayers(),
    new central_Km_num(),
    new central_pim_num(),
    new central_prot_num(),
    new central_Kp_num(),
    new central_pip_num(),
    new cvt_Vz_negative(),
    new cvt_Vz_positive(),
    new cvt_chi2_elec(),
    new cvt_chi2_neg(),
    new cvt_chi2_pos(),
    new cvt_chi2norm(),
    new cvt_ndf(),
    new cvt_p(),
    new cvt_pathlen(),
    new cvt_pt(),
    new cvt_trks(),
    new cvt_trks_neg(),
    new cvt_trks_neg_rat(),
    new cvt_trks_pos(),
    new cvt_trks_pos_rat(),
    new cvt_d0_mean_pos(),
    new cvt_d0_sigma_pos(),
    new cvt_d0_max_pos(),
    new rat_Km_num(),
    new rat_neg_num(),
    new rat_pos_num(),
    new rat_Kp_num(),
    new rat_neu_num(),
    new rat_prot_num(),
    new rat_elec_num(),
    new rat_pim_num(),
    new rat_muon_num(),
    new rat_pip_num(),
    new forward_Tracking_Elechi2(),
    new forward_Tracking_EleVz(),
    new forward_Tracking_Poschi2(),
    new forward_Tracking_PosVz(),
    new forward_Tracking_Negchi2(),
    new forward_Tracking_NegVz(),
    new ec_Sampl(),
    new ec_gg_m(),
    new ec_pcal_time(),
    new ec_ecin_time(),
    new ec_ecou_time(),
    new ltcc_nphe_sector(),
    new rftime_diff(),
    new rftime_pim_FD(),
    new rftime_pim_CD(),
    new rftime_pip_FD(),
    new rftime_pip_CD(),
    new rftime_elec_FD(),
    new rftime_diff_corrected(),
    new rftime_prot_FD(),
    new rftime_prot_CD(),
    new epics_xy(),
    new epics_hall_weather()],
  out_CND: [new cnd_MIPS_dE_dz(),
    new cnd_time_neg_vtP(),
    new cnd_zdiff()],
  out_CTOF: [new ctof_edep(),
    new ctof_time(),
    new ctof_tdcadc(),
  ],
  out_FT: [new ftc_pi0_mass(),
    new ftc_time_charged(),
    new ftc_time_neutral(),
    new fth_MIPS_energy(),
    new fth_MIPS_time(),
    new fth_MIPS_energy_board(),
    new fth_MIPS_time_board()],
  out_HTCC: [new htcc_nphe_ring_sector(),
    new htcc_nphe_sector(),
    new htcc_vtimediff(),
    new htcc_vtimediff_sector(),
    new htcc_vtimediff_sector_ring(),
    new htcc_npheAll()],
  out_LTCC: [new ltcc_had_nphe_sector()],
  out_TOF: [new ftof_edep_p1a_smallangles(),
    new ftof_edep_p1a_midangles(),
    new ftof_edep_p1a_largeangles(),
    new ftof_edep_p1b_smallangles(),
    new ftof_edep_p1b_midangles(),
    new ftof_edep_p1b_largeangles(),
    new ftof_edep_p2(),
    new ftof_time_p1a(),
    new ftof_time_p1b(),
    new ftof_time_p2(),
    new ftof_tdcadc_p1a(),
    new ftof_tdcadc_p1b(),
    new ftof_tdcadc_p2(),
    new ftof_ctof_vtdiff(),
    new dc_residuals_sec(),
    new dc_residuals_sec_sl(),
    //new dc_residuals_sec_rescut(),
    new dc_residuals_sec_sl_rescut(),
    new dc_t0_sec_sl(),
    new dc_t0_even_sec_sl(),
    new dc_t0_odd_sec_sl(),
    new dc_tmax_sec_sl()],
  out_RICH: [new rich_dt_m(),
       new rich_trk_m(),
       new rich_etac_dir_m(),
       new rich_etac_plan_m(),
       new rich_etac_sphe_m(),
       new rich_npho_dir_m(),
       new rich_npho_plan_m(),
       new rich_npho_sphe_m(),
       new rich_npim_m(),
       new rich_npip_m(),
       new rich_nkm_m(),
       new rich_nkp_m(),
       new rich_npro_m(),
       new rich_npbar_m()],
  out_HELICITY: [new helicity()],
  out_TRIGGER: [new trigger()],
]


if(args.any{it=="--timelines"}) {
  engines.values().flatten().each{
    println(it.getClass().getSimpleName())
  }
  System.exit(0)
}

def eng = engines.collectMany{key,engs->engs.collect{[key,it]}}
  .find{name,eng->eng.getClass().getSimpleName()==args[0]}

if(eng) {
  def (name,engine) = eng
  def input = new File(args[1])
  println([name,args[0],engine.getClass().getSimpleName(),input])
  def fnames = []
  input.traverse {
    if(it.name.endsWith('.hipo') && it.name.contains(name))
      fnames.add(it.absolutePath)
  }

  fnames.sort().each{arg->
    try{
      println("debug: "+engine.getClass().getSimpleName()+" started $arg")

      TDirectory dir = new TDirectory()
      dir.readFile(arg)

      // get run number from directory name
      def fname = arg.split('/')[-2]
      def m = fname =~ /\d+/
      def run = m[0].toInteger()

      engine.processRun(dir, run)

      println("debug: "+engine.getClass().getSimpleName()+" finished $arg")
    } catch(Exception ex) {
      System.err.println("error: "+engine.getClass().getSimpleName()+" didn't process $arg, due to exception:")
      ex.printStackTrace()
      System.exit(100)
    }
  }
  engine.write()
  println("debug: "+engine.getClass().getSimpleName()+" ended")
} else {
  System.err.println("error: "+args[0]+" not found")
  System.exit(100)
}
