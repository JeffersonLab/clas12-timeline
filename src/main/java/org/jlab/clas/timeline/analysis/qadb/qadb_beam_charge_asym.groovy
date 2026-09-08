package org.jlab.clas.timeline.analysis.qadb

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

import org.jlab.clas.timeline.util.Tools
import org.jlab.clas.timeline.histograms.qadb.Charge

class qadb_beam_charge_asym {

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

    // start ouput `TDirectory`
    TDirectory tdir = new TDirectory()

    // define timeline ('tl') graphs: asym vs. run number
    def make_tl = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Beam Charge Asymmetry A_FC' // they all get the same title, since they're all plotted on one canvas
      g.setTitleY 'A_FC [%]'
      g.setTitleX 'Run Number'
      g
    }
    def tl_asym_struck_qg = make_tl 'STRUCK_qGated'
    def tl_asym_struck_qu = make_tl 'STRUCK_qUnated'
    def tl_asym_epics_fc  = make_tl 'EPICS_FCUP'
    def tl_asym_epics_slm = make_tl 'EPICS_SLM'

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
      def rn_asym_struck_qg = make_rn 'a1', 'STRUCK_qGated',               'A_FC from STRUCK q_gated',          'A_FC [%]'
      def rn_asym_struck_qu = make_rn 'a2', 'STRUCK_qUngated',             'A_FC from STRUCK q_ungated',        'A_FC [%]'
      def rn_struck_helP_qg = make_rn 'b1', 'STRUCK_helPositive_qGated',   'STRUCK helicity=+1 q_gated [nC]',   'q [nC]'
      def rn_struck_helN_qg = make_rn 'c1', 'STRUCK_helNegative_qGated',   'STRUCK helicity=-1 q_gated [nC]',   'q [nC]'
      def rn_struck_helP_qu = make_rn 'b2', 'STRUCK_helPositive_qUngated', 'STRUCK helicity=+1 q_ungated [nC]', 'q [nC]'
      def rn_struck_helN_qu = make_rn 'c2', 'STRUCK_helNegative_qUngated', 'STRUCK helicity=-1 q_ungated [nC]', 'q [nC]'

      // calculate an asymmetry
      def calc_asym = { p, n -> Tools.safeRatio p-n, p+n }

      // fill run graphs: loop over each QA bin's histograms (`Charge` objects), read the charge etc., calculate asymmetries
      run_data['histos'].each { binnum, histos ->
        // calculate asymmetries from scalers
        def asym_struck_qg = calc_asym histos.getChargeGatedSTRUCK(1),   histos.getChargeGatedSTRUCK(-1)
        def asym_struck_qu = calc_asym histos.getChargeUngatedSTRUCK(1), histos.getChargeUngatedSTRUCK(-1)
        rn_asym_struck_qg.addPoint binnum, asym_struck_qg, 0, 0
        rn_asym_struck_qu.addPoint binnum, asym_struck_qu, 0, 0
        // fill charge graphs too, since we need them to get a run's total charge asymmetry
        rn_struck_helP_qg.addPoint binnum, histos.getChargeGatedSTRUCK(1),    0, Math.sqrt(histos.getChargeGatedSTRUCK(1))
        rn_struck_helN_qg.addPoint binnum, histos.getChargeGatedSTRUCK(-1),   0, Math.sqrt(histos.getChargeGatedSTRUCK(-1))
        rn_struck_helP_qu.addPoint binnum, histos.getChargeUngatedSTRUCK(1),  0, Math.sqrt(histos.getChargeUngatedSTRUCK(1))
        rn_struck_helN_qu.addPoint binnum, histos.getChargeUngatedSTRUCK(-1), 0, Math.sqrt(histos.getChargeUngatedSTRUCK(-1))
      }

      // fill timeline graphs: sum over the charge run graphs to get the total charge for the run, then calculate the asymmetry from that
      def add_tl_point = { rnP, rnN, tl ->
        def sumP = 0.0
        def sumN = 0.0
        rnP.getDataSize(0).times{ sumP += rnP.getDataY(it) }
        rnN.getDataSize(0).times{ sumN += rnN.getDataY(it) }
        tl.addPoint runnum, calc_asym(sumP, sumN), 0, 0
      }
      add_tl_point rn_struck_helP_qg, rn_struck_helN_qg, tl_asym_struck_qg
      add_tl_point rn_struck_helP_qu, rn_struck_helN_qu, tl_asym_struck_qu

      // write run graphs for this run
      tdir.mkdir "/${runnum}"
      tdir.cd    "/${runnum}"
      tdir.addDataSet rn_asym_struck_qg
      tdir.addDataSet rn_asym_struck_qu

    } // end loop over runs

    // write timeline graphs
    // charge per run
    tdir.mkdir '/timelines'
    tdir.cd    '/timelines'
    tdir.addDataSet tl_asym_struck_qg
    tdir.addDataSet tl_asym_struck_qu

    // write HIPO files
    tdir.writeFile 'qadb_beam_charge_asymmetry.hipo'
  }

}
