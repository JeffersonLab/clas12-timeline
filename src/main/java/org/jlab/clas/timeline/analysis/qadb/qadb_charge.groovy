package org.jlab.clas.timeline.analysis.qadb

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

import org.jlab.clas.timeline.util.Tools
import org.jlab.clas.timeline.histograms.qadb.Charge

class qadb_charge {

  def data_map = new ConcurrentHashMap()

  // ----------------------------------------------------------------------------------

  def processRun(dir, runnum, qa_map) {
    data_map[runnum] = [run:runnum, histos:[:]]
    qa_map[runnum].each { qa_bin ->
      def histos = new Charge(qa_bin.getBinNum())
      histos.readHistograms(dir, qa_bin.getBinNum())
      data_map[runnum]['histos'][qa_bin.getBinNum()] = histos
    }
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map) {

    // start ouput `TDirectory`s
    TDirectory tdir_tl  = new TDirectory() // charge per run
    TDirectory tdir_ctl = new TDirectory() // cumulative charge as a function of run

    // define timeline ('tl') graphs: charge vs. run number
    def make_tl = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Charge q [nC]'
      g.setTitleY 'q [nC]'
      g.setTitleX 'Run Number'
      g
    }
    def tl_dsc2_qg        = make_tl 'DSC2_qGated'
    def tl_dsc2_qu        = make_tl 'DSC2_qUngated'
    def tl_struck_helP_qg = make_tl 'STRUCK_helPositive_qGated'
    def tl_struck_hel0_qg = make_tl 'STRUCK_helUndefined_qGated'
    def tl_struck_helN_qg = make_tl 'STRUCK_helNegative_qGated'
    def tl_struck_totl_qg = make_tl 'STRUCK_total_qGated'

    // define cumulative timeline ('ctl') graphs: cumulative charge vs. run
    def make_ctl = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Cumulative Charge q [mC]'
      g.setTitleY 'Cumulative q [mC]'
      g.setTitleX 'Run Number'
      g
    }
    def ctl_dsc2_qg        = make_ctl 'DSC2_qGated'
    def ctl_dsc2_qu        = make_ctl 'DSC2_qUngated'
    def ctl_struck_helP_qg = make_ctl 'STRUCK_helPositive_qGated'
    def ctl_struck_hel0_qg = make_ctl 'STRUCK_helUndefined_qGated'
    def ctl_struck_helN_qg = make_ctl 'STRUCK_helNegative_qGated'
    def ctl_struck_totl_qg = make_ctl 'STRUCK_total_qGated'

    // loop over runs, filling graphs
    data_map.sort{it.key}.each { runnum, run_data ->
      // define run graphs ('rn'): charge vs. QA bin, for this run
      // NOTE: the front-end will order them alphabetically, so prefix their names with unique letters (`sort_prefix`)
      def make_rn = { sort_prefix, name, title, ytitle ->
        def g = new GraphErrors("${sort_prefix}__${name}__${runnum}")
        g.setTitle  title
        g.setTitleY ytitle
        g.setTitleX 'QA Bin'
        g
      }
      def rn_dsc2_qg        = make_rn 'a', 'DSC2_qGated',                'DSC2 q_gated [nC]',                  'q [nC]'
      def rn_dsc2_qu        = make_rn 'b', 'DSC2_qUngated',              'DSC2 q_ungated [nC]',                'q [nC]'
      def rn_struck_totl_qg = make_rn 'c', 'STRUCK_total_qGated',        'STRUCK total q_gated [nC]',          'q [nC]'
      def rn_struck_to_dsc2 = make_rn 'd', 'STRUCK_to_DSC2',             'STRUCK q_gated / DSC2 q_gated',      'q_STRUCK / q_DSC2'
      def rn_struck_helP_qg = make_rn 'e', 'STRUCK_helPositive_qGated',  'STRUCK helicity=+1 q_gated [nC]',    'q [nC]'
      def rn_struck_helN_qg = make_rn 'f', 'STRUCK_helNegative_qGated',  'STRUCK helicity=-1 q_gated [nC]',    'q [nC]'
      def rn_struck_hel0_qg = make_rn 'g', 'STRUCK_helUndefined_qGated', 'STRUCK helicity=undef q_gated [nC]', 'q [nC]'
      // fill run graphs: loop over each QA bin's histograms (`Charge` objects), read the charge, and plot it
      run_data['histos'].each { binnum, histos ->
        def q_struck_totl    = [1,0,-1].collect{histos.getChargeGatedSTRUCK(it)}.sum()
        def q_struck_to_dsc2 = Tools.safeRatio q_struck_totl, histos.getChargeGatedDSC2()
        rn_dsc2_qg.addPoint        binnum, histos.getChargeGatedDSC2(),     0, 0 // NOTE: errors are calculated later
        rn_dsc2_qu.addPoint        binnum, histos.getChargeUngatedDSC2(),   0, 0
        rn_struck_helP_qg.addPoint binnum, histos.getChargeGatedSTRUCK(1),  0, 0
        rn_struck_hel0_qg.addPoint binnum, histos.getChargeGatedSTRUCK(0),  0, 0
        rn_struck_helN_qg.addPoint binnum, histos.getChargeGatedSTRUCK(-1), 0, 0
        rn_struck_totl_qg.addPoint binnum, q_struck_totl,                   0, 0
        rn_struck_to_dsc2.addPoint binnum, q_struck_to_dsc2,                0, 0
      }
      // set Poisson errors for each run graph
      def set_errors = { g ->
        g.getDataSize(0).times{ g.setError it, 0, Math.sqrt(g.getDataY(it)) }
      }
      set_errors rn_dsc2_qg
      set_errors rn_dsc2_qu
      set_errors rn_struck_helP_qg
      set_errors rn_struck_hel0_qg
      set_errors rn_struck_helN_qg
      set_errors rn_struck_totl_qg
      rn_struck_to_dsc2.getDataSize(0).times {
        def s = rn_struck_totl_qg.getDataY(it)
        def d = rn_dsc2_qg.getDataY(it)
        rn_struck_to_dsc2.setError it, 0, Tools.ratioPoissonUncertainty(s,d)
      }
      // fill timeline graphs: sum over each QA bin's charge values, and plot that sum on the timeline graph
      def add_tl_point = { rn, tl ->
        def sum = 0.0
        rn.getDataSize(0).times{ sum += rn.getDataY(it) }
        tl.addPoint runnum, sum, 0, 0
      }
      add_tl_point rn_dsc2_qg,        tl_dsc2_qg
      add_tl_point rn_dsc2_qu,        tl_dsc2_qu
      add_tl_point rn_struck_helP_qg, tl_struck_helP_qg
      add_tl_point rn_struck_hel0_qg, tl_struck_hel0_qg
      add_tl_point rn_struck_helN_qg, tl_struck_helN_qg
      add_tl_point rn_struck_totl_qg, tl_struck_totl_qg
      // write run graphs for this run
      [ tdir_tl, tdir_ctl ].each { tdir ->
        tdir.mkdir "/${runnum}"
        tdir.cd    "/${runnum}"
        tdir.addDataSet rn_dsc2_qg
        tdir.addDataSet rn_dsc2_qu
        tdir.addDataSet rn_struck_helP_qg
        tdir.addDataSet rn_struck_hel0_qg
        tdir.addDataSet rn_struck_helN_qg
        tdir.addDataSet rn_struck_totl_qg
        tdir.addDataSet rn_struck_to_dsc2
      }
    } // end loop over runs

    // fill cumulative timeline graphs: loop over timeline graph, and accumulate the sum
    // NOTE: since the cumulative charge gets large, we plot it in mC rather than nC
    def fill_ctl = { ctl, tl ->
      tl.getDataSize(0).times {
        def q_this_run   = Charge.to_mC tl.getDataY(it) // NOTE: this is the one and only place we do this conversion, i.e., for the cumulative timeline graph only!
        def q_cumulative = it==0 ? 0.0 : ctl.getDataY(it-1)
        ctl.addPoint tl.getDataX(it), q_cumulative + q_this_run, 0, 0
      }
    }
    fill_ctl ctl_dsc2_qg,        tl_dsc2_qg
    fill_ctl ctl_dsc2_qu,        tl_dsc2_qu
    fill_ctl ctl_struck_helP_qg, tl_struck_helP_qg
    fill_ctl ctl_struck_hel0_qg, tl_struck_hel0_qg
    fill_ctl ctl_struck_helN_qg, tl_struck_helN_qg
    fill_ctl ctl_struck_totl_qg, tl_struck_totl_qg

    // write timelines
    tdir_tl.mkdir '/timelines'
    tdir_tl.cd    '/timelines'
    tdir_tl.addDataSet tl_dsc2_qg
    tdir_tl.addDataSet tl_dsc2_qu
    tdir_tl.addDataSet tl_struck_helP_qg
    tdir_tl.addDataSet tl_struck_hel0_qg
    tdir_tl.addDataSet tl_struck_helN_qg
    tdir_tl.addDataSet tl_struck_totl_qg
    tdir_ctl.mkdir '/timelines'
    tdir_ctl.cd    '/timelines'
    tdir_ctl.addDataSet ctl_dsc2_qg
    tdir_ctl.addDataSet ctl_dsc2_qu
    tdir_ctl.addDataSet ctl_struck_helP_qg
    tdir_ctl.addDataSet ctl_struck_hel0_qg
    tdir_ctl.addDataSet ctl_struck_helN_qg
    tdir_ctl.addDataSet ctl_struck_totl_qg

    // write HIPO
    tdir_tl.writeFile  'qadb_charge_per_run.hipo'
    tdir_ctl.writeFile 'qadb_charge_cumulative.hipo'
  }

}
