package org.jlab.clas.timeline

import org.jlab.groot.data.TDirectory
import org.jlab.clas.timeline.timeline.*

def engines = [
  out_BAND: [new band.band_adccor(),
    new band.band_lasertime(),
    new band.band_meantimeadc(),
    new band.band_meantimetdc()],
  out_monitor: [new bmtbst.bmt_Occupancy(),
    new bmtbst.bmt_OnTrkLayers(),
    new bmtbst.bst_Occupancy(),
    new bmtbst.bst_OnTrkLayers(),
    new central.central_Km_num(),
    new central.central_pim_num(),
    new central.central_prot_num(),
    new central.central_Kp_num(),
    new central.central_pip_num(),
    new cvt.cvt_Vz_negative(),
    new cvt.cvt_Vz_positive(),
    new cvt.cvt_chi2_elec(),
    new cvt.cvt_chi2_neg(),
    new cvt.cvt_chi2_pos(),
    new cvt.cvt_chi2norm(),
    new cvt.cvt_ndf(),
    new cvt.cvt_p(),
    new cvt.cvt_pathlen(),
    new cvt.cvt_pt(),
    new cvt.cvt_trks(),
    new cvt.cvt_trks_neg(),
    new cvt.cvt_trks_neg_rat(),
    new cvt.cvt_trks_pos(),
    new cvt.cvt_trks_pos_rat(),
    new trigger.rat_Km_num(),
    new trigger.rat_neg_num(),
    new trigger.rat_pos_num(),
    new trigger.rat_Kp_num(),
    new trigger.rat_neu_num(),
    new trigger.rat_prot_num(),
    new trigger.rat_elec_num(),
    new trigger.rat_pim_num(),
    new trigger.rat_muon_num(),
    new trigger.rat_pip_num(),
    new forward.forward_Tracking_Elechi2(),
    new forward.forward_Tracking_EleVz(),
    new forward.forward_Tracking_Poschi2(),
    new forward.forward_Tracking_PosVz(),
    new forward.forward_Tracking_Negchi2(),
    new forward.forward_Tracking_NegVz(),
    new ec.ec_Sampl(),
    new ec.ec_gg_m(),
    new ec.ec_pcal_time(),
    new ec.ec_ecin_time(),
    new ec.ec_ecou_time(),
    new ltcc.ltcc_nphe_sector(),
    new rf.rftime_diff(),
    new rf.rftime_pim_FD(),
    new rf.rftime_pim_CD(),
    new rf.rftime_pip_FD(),
    new rf.rftime_pip_CD(),
    new rf.rftime_elec_FD(),
    new rf.rftime_diff_corrected(),
    new rf.rftime_prot_FD(),
    new rf.rftime_prot_CD(),
    new epics.epics_xy()],
  out_CND: [new cnd.cnd_MIPS_dE_dz(),
    new cnd.cnd_time_neg_vtP(),
    new cnd.cnd_zdiff()],
  out_CTOF: [new ctof.ctof_edep(),
    new ctof.ctof_time(),
    new ctof.ctof_tdcadc_left(),
    new ctof.ctof_tdcadc_right(),
    new particle_mass_ctof_and_ftof.ctof_m2_pim(),
    new particle_mass_ctof_and_ftof.ctof_m2_pip()],
  out_FT: [new ft.ftc_pi0_mass(),
    new ft.ftc_time_charged(),
    new ft.ftc_time_neutral(),
    new ft.fth_MIPS_energy(),
    new ft.fth_MIPS_time(),
    new ft.fth_MIPS_energy_board(),
    new ft.fth_MIPS_time_board()],
  out_HTCC: [new htcc.htcc_nphe_ring_sector(),
    new htcc.htcc_nphe_sector(),
    new htcc.htcc_vtimediff(),
    new htcc.htcc_vtimediff_sector(),
    new htcc.htcc_vtimediff_sector_ring(),
    new htcc.htcc_npheAll()],
  out_LTCC: [new ltcc.ltcc_had_nphe_sector()],
  out_TOF: [new ftof.ftof_edep_p1a_smallangles(),
    new ftof.ftof_edep_p1a_midangles(),
    new ftof.ftof_edep_p1a_largeangles(),
    new ftof.ftof_edep_p1b_smallangles(),
    new ftof.ftof_edep_p1b_midangles(),
    new ftof.ftof_edep_p1b_largeangles(),
    new ftof.ftof_edep_p2(),
    new ftof.ftof_time_p1a(),
    new ftof.ftof_time_p1b(),
    new ftof.ftof_time_p2(),
    new ftof.ftof_time_noTriggers_p1a(),
    new ftof.ftof_time_noTriggers_p1b(),
    new ftof.ftof_tdcadc_p1a(),
    new ftof.ftof_tdcadc_p1b(),
    new ftof.ftof_tdcadc_p2(),
    new ftof.ftof_tdcadc_p1a_zoomed(),
    new ftof.ftof_tdcadc_p1b_zoomed(),
    new ftof.ftof_tdcadc_p2_zoomed(),
    new ftof.ftof_ctof_vtdiff(),
    new dc.dc_residuals_sec(),
    new dc.dc_residuals_sec_sl(),
    //new dc.dc_residuals_sec_rescut(),
    new dc.dc_residuals_sec_sl_rescut(),
    new dc.dc_t0_sec_sl(),
    new dc.dc_t0_even_sec_sl(),
    new dc.dc_t0_odd_sec_sl(),        
    new dc.dc_tmax_sec_sl()],
  out_RICH: [new rich.rich_dt_m(), 
       new rich.rich_trk_m(), 
       new rich.rich_etac_dir_m(), 
       new rich.rich_etac_plan_m(), 
       new rich.rich_etac_sphe_m(), 
       new rich.rich_npho_dir_m(), 
       new rich.rich_npho_plan_m(), 
       new rich.rich_npho_sphe_m(), 
       new rich.rich_npim_m(), 
       new rich.rich_npip_m(), 
       new rich.rich_nkm_m(), 
       new rich.rich_nkp_m(), 
       new rich.rich_npro_m(), 
       new rich.rich_npbar_m()],
  dst_mon: [new particle_mass_ctof_and_ftof.ftof_m2_p1a_pim(),
    new particle_mass_ctof_and_ftof.ftof_m2_p1a_pip(),
    new particle_mass_ctof_and_ftof.ftof_m2_p1a_prot(),
    new particle_mass_ctof_and_ftof.ftof_m2_p1b_pim(),
    new particle_mass_ctof_and_ftof.ftof_m2_p1b_pip(),
    new particle_mass_ctof_and_ftof.ftof_m2_p1b_prot()
  ]
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

      engine.processDirectory(dir, run)

      println("debug: "+engine.getClass().getSimpleName()+" finished $arg")
    } catch(Exception ex) {
      System.err.println("error: "+engine.getClass().getSimpleName()+" didn't process $arg, due to exception:")
      ex.printStackTrace()
      System.exit(100)
    }
  }
  engine.close()
  println("debug: "+engine.getClass().getSimpleName()+" ended")
} else {
  System.err.println("error: "+args[0]+" not found")
  System.exit(100)
}
