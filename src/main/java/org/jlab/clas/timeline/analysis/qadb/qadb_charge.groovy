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

  // the `data_map` data structure will be filled like so:
  /*
     data_map
     |_ runnum 1
     |  |__ histos
     |      |_ 1 -> histograms from Charge.java for bin 1
     |      :
     |      |_ N -> histograms from Charge.java for bin N
     |
     |_ runnum 2
     |  |_ histos
     |     |_ ...
     :
  */
  def processRun(dir, runnum, qa_map) {
    data_map[runnum] = [run:runnum, histos:[:]]
    // loop over QA bins for this run
    qa_map[runnum].each { qa_bin ->
      def histos = new Charge(qa_bin.getBinNum())
      histos.readHistograms(dir, qa_bin.getBinNum())
      data_map[runnum]['histos'][qa_bin.getBinNum()] = histos
    }
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map) {

    // start ouput `TDirectory`s
    TDirectory tdir_tl   = new TDirectory() // charge per run
    TDirectory tdir_cutl = new TDirectory() // cumulative charge as a function of run
    TDirectory tdir_cltl = new TDirectory() // clock as a function of run

    // define timeline ('tl') graphs: charge vs. run number
    def make_tl = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Charge q [nC]' // they all get the same title, since they're all plotted on one canvas
      g.setTitleY 'q [nC]'
      g.setTitleX 'Run Number'
      g
    }
    def tl_dsc2_qg        = make_tl 'DSC2_qGated' // these names appear in timeline legend, make them user-friendly
    def tl_dsc2_qu        = make_tl 'DSC2_qUngated'
    def tl_struck_helP_qg = make_tl 'STRUCK_helPositive_qGated'
    def tl_struck_hel0_qg = make_tl 'STRUCK_helUndefined_qGated'
    def tl_struck_helN_qg = make_tl 'STRUCK_helNegative_qGated'
    def tl_struck_totl_qg = make_tl 'STRUCK_total_qGated'
    def tl_struck_totl_qu = make_tl 'STRUCK_total_qUngated'

    // define cumulative timeline ('cutl') graphs: cumulative charge vs. run
    def make_cutl = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Cumulative Charge q [mC]' // use mC instead of nC, since cumulative charge will get large
      g.setTitleY 'Cumulative q [mC]'
      g.setTitleX 'Run Number'
      g
    }
    def cutl_dsc2_qg        = make_cutl 'DSC2_qGated'
    def cutl_dsc2_qu        = make_cutl 'DSC2_qUngated'
    def cutl_struck_helP_qg = make_cutl 'STRUCK_helPositive_qGated'
    def cutl_struck_hel0_qg = make_cutl 'STRUCK_helUndefined_qGated'
    def cutl_struck_helN_qg = make_cutl 'STRUCK_helNegative_qGated'
    def cutl_struck_totl_qg = make_cutl 'STRUCK_total_qGated'
    def cutl_struck_totl_qu = make_cutl 'STRUCK_total_qUngated'

    // define clock timeline ('cltl') graphs: clock counts vs. run number
    def make_cltl = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Clock Counts C'
      g.setTitleY 'C'
      g.setTitleX 'Run Number'
      g
    }
    def cltl_struck_helP_cg = make_cltl 'STRUCK_helPositive_clkGated'
    def cltl_struck_hel0_cg = make_cltl 'STRUCK_helUndefined_clkGated'
    def cltl_struck_helN_cg = make_cltl 'STRUCK_helNegative_clkGated'
    def cltl_struck_totl_cg = make_cltl 'STRUCK_total_clkGated'
    def cltl_struck_totl_cu = make_cltl 'STRUCK_total_clkUngated'

    // loop over runs, filling graphs
    data_map.sort{it.key}.each { runnum, run_data ->

      // define run graphs ('rn'): various values vs. QA bin, for this run
      // NOTE: the front-end will order them alphabetically, so prefix their names with unique letters (`sort_prefix`)
      def make_rn = { sort_prefix, name, title, ytitle ->
        def g = new GraphErrors("${sort_prefix}__${name}__${runnum}")
        g.setTitle  title
        g.setTitleY ytitle
        g.setTitleX 'QA Bin'
        g
      }
      // charge run graphs
      def rn_dsc2_qg           = make_rn 'a1', 'DSC2_qGated',                'DSC2 q_gated [nC]',                  'q [nC]'
      def rn_dsc2_qu           = make_rn 'a2', 'DSC2_qUngated',              'DSC2 q_ungated [nC]',                'q [nC]'
      def rn_struck_totl_qg    = make_rn 'b1', 'STRUCK_total_qGated',        'STRUCK total q_gated [nC]',          'q [nC]'
      def rn_struck_totl_qu    = make_rn 'b2', 'STRUCK_total_qUngated',      'STRUCK total q_ungated [nC]',        'q [nC]'
      def rn_struck_to_dsc2_qg = make_rn 'c1', 'STRUCK_to_DSC2_qGated',      'STRUCK q_gated / DSC2 q_gated',      'q_STRUCK / q_DSC2'
      def rn_struck_to_dsc2_qu = make_rn 'c2', 'STRUCK_to_DSC2_qUngated',    'STRUCK q_ungated / DSC2 q_ungated',  'q_STRUCK / q_DSC2'
      def rn_struck_helP_qg    = make_rn 'd1', 'STRUCK_helPositive_qGated',  'STRUCK helicity=+1 q_gated [nC]',    'q [nC]'
      def rn_struck_helN_qg    = make_rn 'd2', 'STRUCK_helNegative_qGated',  'STRUCK helicity=-1 q_gated [nC]',    'q [nC]'
      def rn_struck_hel0_qg    = make_rn 'd3', 'STRUCK_helUndefined_qGated', 'STRUCK helicity=undef q_gated [nC]', 'q [nC]'
      // clock run graphs
      def rn_struck_totl_cg = make_rn 'a1', 'STRUCK_total_clkGated',        'STRUCK total C_gated',          'C'
      def rn_struck_totl_cu = make_rn 'a2', 'STRUCK_total_clkUngated',      'STRUCK total C_ungated',        'C'
      def rn_struck_helP_cg = make_rn 'b1', 'STRUCK_helPositive_clkGated',  'STRUCK helicity=+1 C_gated',    'C'
      def rn_struck_helN_cg = make_rn 'b2', 'STRUCK_helNegative_clkGated',  'STRUCK helicity=-1 C_gated',    'C'
      def rn_struck_hel0_cg = make_rn 'b3', 'STRUCK_helUndefined_clkGated', 'STRUCK helicity=undef C_gated', 'C'

      // fill run graphs: loop over each QA bin's histograms (`Charge` objects), read the charge etc., and plot it
      run_data['histos'].each { binnum, histos ->
        // charge and clock summed over helicity states
        def qg_struck_totl = [1,0,-1].collect{ histos.getChargeGatedSTRUCK(it)   }.sum()
        def qu_struck_totl = [1,0,-1].collect{ histos.getChargeUngatedSTRUCK(it) }.sum()
        def cg_struck_totl = [1,0,-1].collect{ histos.getClockGatedSTRUCK(it)    }.sum()
        def cu_struck_totl = [1,0,-1].collect{ histos.getClockUngatedSTRUCK(it)  }.sum()
        // STRUCK / DSC2 values and uncertainty
        def qg_struck_to_dsc2     = Tools.safeRatio               qg_struck_totl, histos.getChargeGatedDSC2()
        def qg_struck_to_dsc2_unc = Tools.ratioPoissonUncertainty qg_struck_totl, histos.getChargeGatedDSC2()
        def qu_struck_to_dsc2     = Tools.safeRatio               qu_struck_totl, histos.getChargeUngatedDSC2()
        def qu_struck_to_dsc2_unc = Tools.ratioPoissonUncertainty qu_struck_totl, histos.getChargeUngatedDSC2()
        // add charge points to 'rn' graphs
        rn_dsc2_qg.addPoint           binnum, histos.getChargeGatedDSC2(),     0, Math.sqrt(histos.getChargeGatedDSC2())
        rn_dsc2_qu.addPoint           binnum, histos.getChargeUngatedDSC2(),   0, Math.sqrt(histos.getChargeUngatedDSC2())
        rn_struck_helP_qg.addPoint    binnum, histos.getChargeGatedSTRUCK(1),  0, Math.sqrt(histos.getChargeGatedSTRUCK(1))
        rn_struck_hel0_qg.addPoint    binnum, histos.getChargeGatedSTRUCK(0),  0, Math.sqrt(histos.getChargeGatedSTRUCK(0))
        rn_struck_helN_qg.addPoint    binnum, histos.getChargeGatedSTRUCK(-1), 0, Math.sqrt(histos.getChargeGatedSTRUCK(-1))
        rn_struck_totl_qg.addPoint    binnum, qg_struck_totl,                  0, Math.sqrt(qg_struck_totl)
        rn_struck_totl_qu.addPoint    binnum, qu_struck_totl,                  0, Math.sqrt(qu_struck_totl)
        rn_struck_to_dsc2_qg.addPoint binnum, qg_struck_to_dsc2,               0, qg_struck_to_dsc2_unc
        rn_struck_to_dsc2_qu.addPoint binnum, qu_struck_to_dsc2,               0, qu_struck_to_dsc2_unc
        // add clock points to 'rn' graphs
        rn_struck_helP_cg.addPoint binnum, histos.getClockGatedSTRUCK(1),  0, Math.sqrt(histos.getClockGatedSTRUCK(1))
        rn_struck_hel0_cg.addPoint binnum, histos.getClockGatedSTRUCK(0),  0, Math.sqrt(histos.getClockGatedSTRUCK(0))
        rn_struck_helN_cg.addPoint binnum, histos.getClockGatedSTRUCK(-1), 0, Math.sqrt(histos.getClockGatedSTRUCK(-1))
        rn_struck_totl_cg.addPoint binnum, cg_struck_totl,                 0, Math.sqrt(cg_struck_totl)
        rn_struck_totl_cu.addPoint binnum, cu_struck_totl,                 0, Math.sqrt(cu_struck_totl)
      }

      // fill timeline graphs: sum over each QA bin's charge values, and plot that sum on the timeline graph
      def add_tl_point = { rn, tl ->
        def sum = 0.0
        rn.getDataSize(0).times{ sum += rn.getDataY(it) }
        tl.addPoint runnum, sum, 0, 0
      }
      // charge
      add_tl_point rn_dsc2_qg,        tl_dsc2_qg
      add_tl_point rn_dsc2_qu,        tl_dsc2_qu
      add_tl_point rn_struck_helP_qg, tl_struck_helP_qg
      add_tl_point rn_struck_hel0_qg, tl_struck_hel0_qg
      add_tl_point rn_struck_helN_qg, tl_struck_helN_qg
      add_tl_point rn_struck_totl_qg, tl_struck_totl_qg
      add_tl_point rn_struck_totl_qu, tl_struck_totl_qu
      // clock
      add_tl_point rn_struck_helP_cg, cltl_struck_helP_cg
      add_tl_point rn_struck_hel0_cg, cltl_struck_hel0_cg
      add_tl_point rn_struck_helN_cg, cltl_struck_helN_cg
      add_tl_point rn_struck_totl_cg, cltl_struck_totl_cg
      add_tl_point rn_struck_totl_cu, cltl_struck_totl_cu

      // write charge run graphs for this run
      [ tdir_tl, tdir_cutl ].each { tdir -> // both 'tl' and 'cutl' timelines will have these 'rn' graphs
        tdir.mkdir "/${runnum}"
        tdir.cd    "/${runnum}"
        tdir.addDataSet rn_dsc2_qg
        tdir.addDataSet rn_dsc2_qu
        tdir.addDataSet rn_struck_helP_qg
        tdir.addDataSet rn_struck_hel0_qg
        tdir.addDataSet rn_struck_helN_qg
        tdir.addDataSet rn_struck_totl_qg
        tdir.addDataSet rn_struck_totl_qu
        tdir.addDataSet rn_struck_to_dsc2_qg
        tdir.addDataSet rn_struck_to_dsc2_qu
      }
      // write clock run graphs for this run
      tdir_cltl.mkdir "/${runnum}"
      tdir_cltl.cd    "/${runnum}"
      tdir_cltl.addDataSet rn_struck_helP_cg
      tdir_cltl.addDataSet rn_struck_hel0_cg
      tdir_cltl.addDataSet rn_struck_helN_cg
      tdir_cltl.addDataSet rn_struck_totl_cg
      tdir_cltl.addDataSet rn_struck_totl_cu

    } // end loop over runs

    // fill cumulative timeline graphs: loop over timeline graph, and accumulate the sum
    // NOTE: since the cumulative charge gets large, we plot it in mC rather than nC
    def fill_cutl = { cutl, tl ->
      tl.getDataSize(0).times {
        def q_this_run   = Charge.to_mC tl.getDataY(it) // NOTE: this is the one and only place we do this conversion, i.e., for the cumulative timeline graph only!
        def q_cumulative = it==0 ? 0.0 : cutl.getDataY(it-1)
        cutl.addPoint tl.getDataX(it), q_cumulative + q_this_run, 0, 0
      }
    }
    fill_cutl cutl_dsc2_qg,        tl_dsc2_qg
    fill_cutl cutl_dsc2_qu,        tl_dsc2_qu
    fill_cutl cutl_struck_helP_qg, tl_struck_helP_qg
    fill_cutl cutl_struck_hel0_qg, tl_struck_hel0_qg
    fill_cutl cutl_struck_helN_qg, tl_struck_helN_qg
    fill_cutl cutl_struck_totl_qg, tl_struck_totl_qg
    fill_cutl cutl_struck_totl_qu, tl_struck_totl_qu

    // write timeline graphs
    // charge per run
    tdir_tl.mkdir '/timelines'
    tdir_tl.cd    '/timelines'
    tdir_tl.addDataSet tl_dsc2_qg
    tdir_tl.addDataSet tl_dsc2_qu
    tdir_tl.addDataSet tl_struck_helP_qg
    tdir_tl.addDataSet tl_struck_hel0_qg
    tdir_tl.addDataSet tl_struck_helN_qg
    tdir_tl.addDataSet tl_struck_totl_qg
    tdir_tl.addDataSet tl_struck_totl_qu
    // cumulative charge
    tdir_cutl.mkdir '/timelines'
    tdir_cutl.cd    '/timelines'
    tdir_cutl.addDataSet cutl_dsc2_qg
    tdir_cutl.addDataSet cutl_dsc2_qu
    tdir_cutl.addDataSet cutl_struck_helP_qg
    tdir_cutl.addDataSet cutl_struck_hel0_qg
    tdir_cutl.addDataSet cutl_struck_helN_qg
    tdir_cutl.addDataSet cutl_struck_totl_qg
    tdir_cutl.addDataSet cutl_struck_totl_qu
    // clock
    tdir_cltl.mkdir '/timelines'
    tdir_cltl.cd    '/timelines'
    tdir_cltl.addDataSet cltl_struck_helP_cg
    tdir_cltl.addDataSet cltl_struck_hel0_cg
    tdir_cltl.addDataSet cltl_struck_helN_cg
    tdir_cltl.addDataSet cltl_struck_totl_cg
    tdir_cltl.addDataSet cltl_struck_totl_cu

    // write HIPO files
    tdir_tl.writeFile   'qadb_charge_per_run.hipo'
    tdir_cutl.writeFile 'qadb_charge_cumulative.hipo'
    tdir_cltl.writeFile 'qadb_scaler_clock.hipo'
  }

}
