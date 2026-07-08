package org.jlab.clas.timeline.analysis.qadb

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

import org.jlab.clas.timeline.histograms.qadb.Charge

class qadb_charge {

  def data = new ConcurrentHashMap()

  // ----------------------------------------------------------------------------------

  def processRun(dir, runnum, qa_map) {
    data[runnum] = [run:runnum, histos:[:]]
    qa_map[runnum].each { qa_bin ->
      def histos = new Charge(qa_bin.getBinNum())
      histos.readHistograms(dir, qa_bin.getBinNum())
      data[runnum]['histos'][qa_bin.getBinNum()] = histos
    }
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map) {

    // start ouput `TDirectory`s
    TDirectory tdir_per = new TDirectory() // charge per run
    TDirectory tdir_acc = new TDirectory() // accumulated charge as a function of run

    // define timeline graphs: charge vs. run number
    def make_tl = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Charge q [mC]'
      g.setTitleY 'q [mC]'
      g.setTitleX 'Run Number'
      g
    }
    def tl_gated_dsc2         = make_tl 'gated_DSC2'
    def tl_ungated_dsc2       = make_tl 'ungated_DSC2'
    def tl_gated_hel_p_struck = make_tl 'gated_STRUCK_hel_pos'
    def tl_gated_hel_0_struck = make_tl 'gated_STRUCK_hel_0'
    def tl_gated_hel_n_struck = make_tl 'gated_STRUCK_hel_neg'

    // define accumulated timeline graphs: accumulated charge vs. run
    def make_acc = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Accumulated Charge q [mC]'
      g.setTitleY 'Accumulated q [mC]'
      g.setTitleX 'Run Number'
      g
    }
    def acc_gated_dsc2         = make_acc 'gated_DSC2'
    def acc_ungated_dsc2       = make_acc 'ungated_DSC2'
    def acc_gated_hel_p_struck = make_acc 'gated_STRUCK_hel_pos'
    def acc_gated_hel_0_struck = make_acc 'gated_STRUCK_hel_0'
    def acc_gated_hel_n_struck = make_acc 'gated_STRUCK_hel_neg'

    // loop over runs, filling graphs
    data.sort{it.key}.each { runnum, run_data ->
      // define run graphs: charge vs. QA bin, for this run
      def make_rn = { name, title ->
        def g = new GraphErrors("${name}__${runnum}")
        g.setTitle  "${title} -- run ${runnum}"
        g.setTitleY 'q [mC]'
        g.setTitleX 'QA Bin'
        g
      }
      def rn_gated_dsc2         = make_rn 'gated_DSC2',           'gated DSC2 charge q'
      def rn_ungated_dsc2       = make_rn 'ungated_DSC2',         'ungated DSC2 charge q'
      def rn_gated_hel_p_struck = make_rn 'gated_STRUCK_hel_pos', 'gated STRUCK charge q for helicity=+1'
      def rn_gated_hel_0_struck = make_rn 'gated_STRUCK_hel_0',   'gated STRUCK charge q for helicity=0'
      def rn_gated_hel_n_struck = make_rn 'gated_STRUCK_hel_neg', 'gated STRUCK charge q for helicity=-1'
      // fill run graphs: loop over each QA bin's histograms (`Charge` objects), read the charge, and plot it
      run_data['histos'].each { binnum, histos ->
        rn_gated_dsc2.addPoint          binnum, Charge.to_mC(histos.getChargeGatedDSC2()),     0, 0 // NOTE: errors are calculated later
        rn_ungated_dsc2.addPoint        binnum, Charge.to_mC(histos.getChargeUngatedDSC2()),   0, 0
        rn_gated_hel_p_struck.addPoint  binnum, Charge.to_mC(histos.getChargeGatedSTRUCK(1)),  0, 0
        rn_gated_hel_0_struck.addPoint  binnum, Charge.to_mC(histos.getChargeGatedSTRUCK(0)),  0, 0
        rn_gated_hel_n_struck.addPoint  binnum, Charge.to_mC(histos.getChargeGatedSTRUCK(-1)), 0, 0
      }
      // set Poisson errors for each run graph
      def set_errors = { g ->
        g.getDataSize(0).times{ g.setError it, 0, Math.sqrt(g.getDataY(it)) }
      }
      set_errors rn_gated_dsc2
      set_errors rn_ungated_dsc2
      set_errors rn_gated_hel_p_struck
      set_errors rn_gated_hel_0_struck
      set_errors rn_gated_hel_n_struck
      // fill timeline graphs: sum over each QA bin's charge values, and plot that sum on the timeline graph
      def add_tl_point = { rn, tl ->
        def sum = 0.0
        rn.getDataSize(0).times{ sum += rn.getDataY(it) }
        tl.addPoint runnum, sum, 0, 0
      }
      add_tl_point rn_gated_dsc2,         tl_gated_dsc2
      add_tl_point rn_ungated_dsc2,       tl_ungated_dsc2
      add_tl_point rn_gated_hel_p_struck, tl_gated_hel_p_struck
      add_tl_point rn_gated_hel_0_struck, tl_gated_hel_0_struck
      add_tl_point rn_gated_hel_n_struck, tl_gated_hel_n_struck
      // write run graphs for this run
      [ tdir_per, tdir_acc ].each { tdir ->
        tdir.mkdir "/${runnum}"
        tdir.cd    "/${runnum}"
        tdir.addDataSet rn_gated_dsc2
        tdir.addDataSet rn_ungated_dsc2
        tdir.addDataSet rn_gated_hel_p_struck
        tdir.addDataSet rn_gated_hel_0_struck
        tdir.addDataSet rn_gated_hel_n_struck
      }
    } // end loop over runs

    // fill accumulated timeline graphs: loop over timeline graph, and accumulate the sum
    def fill_acc = { acc, tl ->
      tl.getDataSize(0).times {
        def val = it==0 ? tl.getDataY(it) : tl.getDataY(it) + acc.getDataY(it-1)
        acc.addPoint tl.getDataX(it), val, 0, 0
      }
    }
    fill_acc acc_gated_dsc2,         tl_gated_dsc2
    fill_acc acc_ungated_dsc2,       tl_ungated_dsc2
    fill_acc acc_gated_hel_p_struck, tl_gated_hel_p_struck
    fill_acc acc_gated_hel_0_struck, tl_gated_hel_0_struck
    fill_acc acc_gated_hel_n_struck, tl_gated_hel_n_struck

    // write timelines
    tdir_per.mkdir '/timelines'
    tdir_per.cd    '/timelines'
    tdir_per.addDataSet tl_gated_dsc2
    tdir_per.addDataSet tl_ungated_dsc2
    tdir_per.addDataSet tl_gated_hel_p_struck
    tdir_per.addDataSet tl_gated_hel_0_struck
    tdir_per.addDataSet tl_gated_hel_n_struck
    tdir_acc.mkdir '/timelines'
    tdir_acc.cd    '/timelines'
    tdir_acc.addDataSet acc_gated_dsc2
    tdir_acc.addDataSet acc_ungated_dsc2
    tdir_acc.addDataSet acc_gated_hel_p_struck
    tdir_acc.addDataSet acc_gated_hel_0_struck
    tdir_acc.addDataSet acc_gated_hel_n_struck

    // write HIPO
    tdir_per.writeFile 'qadb_charge.hipo'
    tdir_acc.writeFile 'qadb_charge_accumulated.hipo'
  }

}
