package org.jlab.clas.timeline.analysis.qadb

import org.rcdb.*

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

import org.jlab.clas.timeline.analysis.MYQuery
import org.jlab.clas.timeline.analysis.EpicsTools
import org.jlab.clas.timeline.util.Tools
import org.jlab.clas.timeline.histograms.qadb.Charge

class qadb_beam_charge_asym {

  def runlist  = []
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
    runlist.push(runnum)
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

    // PVs: obtained from `clas12-epics` -> `beam_charge_asym.stc`
    // FIXME: errors may be accessible from changing `q_asym` -> `d_asym`, but not sure if they're actually in MYA history
    def pvNames = [
      EPICS_SLM_qAsym:   'q_asym_3',
      EPICS_FCUP_qAsym:  'q_asym_7',
      // EPICS_2C21A_qAsym: 'q_asym_16', // all zero, at least for RG-C
      // EPICS_2C24A_qAsym: 'q_asym_20', // all zero, at least for RG-C
      // EPICS_2H01_qAsym:  'q_asym_24', // all zero, at least for RG-C
    ]

    // connect to RCDB
    def rcdbURL = System.getenv('RCDB_CONNECTION')
    if(rcdbURL==null)
      throw new Exception("RCDB_CONNECTION not set")
    def rcdbProvider = RCDB.createProvider(rcdbURL)
    try {
      rcdbProvider.connect()
    }
    catch(Exception e) {
      System.err.println "ERROR: unable to connect to RCDB"
      System.out.println ''
      System.exit(100)
    }

    // query MYA for EPICS data
    def myq        = new MYQuery(runlist)
    def epics_data = EpicsTools.queryEpics(myq, pvNames) { name, val -> val / 100.0 } // convert percent to decimal units

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // start ouput `TDirectory`
    TDirectory tdir = new TDirectory()

    // define timeline ('tl') graphs: asym vs. run number
    def make_tl = { name ->
      def g = new GraphErrors(name)
      g.setTitle  'Beam Charge Asymmetry A_FC' // they all get the same title, since they're all plotted on one canvas
      g.setTitleY 'A_FC'
      g.setTitleX 'Run Number'
      g
    }
    def tl_asym_struck_qg = make_tl 'STRUCK_gated'
    def tl_asym_struck_qu = make_tl 'STRUCK_ungated'
    def tl_asym_epics_fc  = make_tl 'EPICS_FCUP'
    def tl_asym_epics_slm = make_tl 'EPICS_SLM'

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // loop over runs, filling graphs for STRUCK scalers
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
      def rn_asym_struck_qg = make_rn 'a1', 'STRUCK_gated',                'Running A_FC from STRUCK q_gated',      'A_FC'
      def rn_asym_struck_qu = make_rn 'a2', 'STRUCK_ungated',              'Running A_FC from STRUCK q_ungated',    'A_FC'
      def rn_struck_qg_helP = make_rn 'b1', 'STRUCK_helPositive_qGated',   'STRUCK helicity=+1 q_gated [nC]',       'q [nC]'
      def rn_struck_qg_helN = make_rn 'c1', 'STRUCK_helNegative_qGated',   'STRUCK helicity=-1 q_gated [nC]',       'q [nC]'
      def rn_struck_qu_helP = make_rn 'b2', 'STRUCK_helPositive_qUngated', 'STRUCK helicity=+1 q_ungated [nC]',     'q [nC]'
      def rn_struck_qu_helN = make_rn 'c2', 'STRUCK_helNegative_qUngated', 'STRUCK helicity=-1 q_ungated [nC]',     'q [nC]'
      def rn_struck_n_helP  = make_rn 'b3', 'STRUCK_helPositive_num',      'STRUCK helicity=+1 num. readouts',      'num readouts'
      def rn_struck_n_helN  = make_rn 'c3', 'STRUCK_helNegative_num',      'STRUCK helicity=-1 num. readouts',      'num readouts'
      def rn_struck_n_rat   = make_rn 'b4', 'STRUCK_num_rat',              'number of STRUCK readouts ratio N+/N-', 'N+/N-'

      // fill run graphs: loop over each QA bin's histograms (`Charge` objects), read the charge etc.
      run_data['histos'].each { binnum, histos ->
        rn_struck_qg_helP.addPoint binnum, histos.getChargeGatedSTRUCK(1),    0, Math.sqrt(histos.getChargeGatedSTRUCK(1))
        rn_struck_qg_helN.addPoint binnum, histos.getChargeGatedSTRUCK(-1),   0, Math.sqrt(histos.getChargeGatedSTRUCK(-1))
        rn_struck_qu_helP.addPoint binnum, histos.getChargeUngatedSTRUCK(1),  0, Math.sqrt(histos.getChargeUngatedSTRUCK(1))
        rn_struck_qu_helN.addPoint binnum, histos.getChargeUngatedSTRUCK(-1), 0, Math.sqrt(histos.getChargeUngatedSTRUCK(-1))
        rn_struck_n_helP.addPoint  binnum, histos.getNumReadoutsSTRUCK(1),    0, Math.sqrt(histos.getNumReadoutsSTRUCK(1))
        rn_struck_n_helN.addPoint  binnum, histos.getNumReadoutsSTRUCK(-1),   0, Math.sqrt(histos.getNumReadoutsSTRUCK(-1))
      }

      // calculate an asymmetry
      // @param q_helP charge for helicity=+1
      // @param q_helN charge for helicity=-1
      // @param n_helP normalization for helicity=+1
      // @param n_helN normalization for helicity=-1
      def calc_asym = { q_helP, q_helN, n_helP, n_helN ->
        def p = Tools.safeRatio q_helP, n_helP
        def n = Tools.safeRatio q_helN, n_helN
        Tools.safeRatio p - n, p + n
      }

      // fill asymmetry graphs
      // @param rn_q_helP run graph for charge for helicity=+1
      // @param rn_q_helN run graph for charge for helicity=-1
      // @param rn_n_helP run graph for num readouts for helicity=+1
      // @param rn_n_helN run graph for num readouts for helicity=-1
      // @param rn_asym run graph for running asymmetry, to be filled
      // @param tl_asym timeline graph for asymmetry, to be filled
      def fill_asym_graphs = { rn_q_helP, rn_q_helN, rn_n_helP, rn_n_helN, rn_asym, tl_asym ->
        def q_sum_helP = 0.0
        def q_sum_helN = 0.0
        def n_sum_helP = 0.0
        def n_sum_helN = 0.0
        def nbins = rn_q_helP.getDataSize 0
        if(nbins != rn_q_helN.getDataSize(0) || nbins != rn_n_helP.getDataSize(0) || nbins != rn_n_helN.getDataSize(0)) {
          System.err.println "ERROR: different num points in graphs for `fill_asym_graphs`"
          System.exit(100)
        }
        nbins.times {
          def binnum =  rn_q_helP.getDataX it
          q_sum_helP += rn_q_helP.getDataY it
          q_sum_helN += rn_q_helN.getDataY it
          n_sum_helP += rn_n_helP.getDataY it
          n_sum_helN += rn_n_helN.getDataY it
          if(binnum > 0) { // don't plot bin 0's point, which is usually way off; this is so the default zoom level is decent
            rn_asym.addPoint binnum, calc_asym(q_sum_helP, q_sum_helN, n_sum_helP, n_sum_helN), 0, 0
          }
        }
        tl_asym.addPoint runnum, calc_asym(q_sum_helP, q_sum_helN, n_sum_helP, n_sum_helN), 0, 0
      }
      fill_asym_graphs rn_struck_qg_helP, rn_struck_qg_helN, rn_struck_n_helP, rn_struck_n_helN, rn_asym_struck_qg, tl_asym_struck_qg
      fill_asym_graphs rn_struck_qu_helP, rn_struck_qu_helN, rn_struck_n_helP, rn_struck_n_helN, rn_asym_struck_qu, tl_asym_struck_qu

      // fill N+/N- graph
      rn_struck_n_helP.getDataSize(0).times {
        def binnum = rn_struck_n_helP.getDataX it
        def n_helP = rn_struck_n_helP.getDataY it
        def n_helN = rn_struck_n_helN.getDataY it
        rn_struck_n_rat.addPoint binnum, Tools.safeRatio(n_helP, n_helN), 0, 0
      }

      // write run graphs for this run
      tdir.mkdir "/${runnum}"
      tdir.cd    "/${runnum}"
      tdir.addDataSet rn_asym_struck_qg
      tdir.addDataSet rn_asym_struck_qu
      tdir.addDataSet rn_struck_n_rat

    } // end loop over runs

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // loop over runs, filling graphs for EPICS data
    epics_data.each{ runnum, vals ->

      // get HWP position
      def hwp_cond = rcdbProvider.getCondition(runnum, 'half_wave_plate') // 0=IN, 1=OUT
      if(hwp_cond == null) {
        System.err.println "ERROR: cannot find run $runnum in RCDB, thus cannot get HWP status"
        System.exit(100)
      }
      def hwp = hwp_cond.toLong()
      // System.out.println("HWP: $runnum $hwp")

      // define HWP correction
      def hwp_corr       = { val -> val * (hwp==0 ? 1 : -1) } // HWP: 0=IN, 1=OUT
      def hwp_corr_title = '(HWP==IN ? +1 : -1)'

      // fill histograms
      def epics_hists = pvNames.collectEntries{ pv_title, pv_name ->
        def entries = vals.collect{hwp_corr(it[pv_title])}.sort()
        [ pv_title, EpicsTools.quantileHist("z_$pv_title$runnum", "$pv_title * $hwp_corr_title;$pv_title", entries) ]
      }
      vals.each{
        epics_hists.each{ name, hist -> hist.fill(hwp_corr(it[name])) }
      }

      // fill timeline graphs
      tl_asym_epics_fc.addPoint  runnum, epics_hists['EPICS_FCUP_qAsym'].getMean(), 0, 0
      tl_asym_epics_slm.addPoint runnum, epics_hists['EPICS_SLM_qAsym'].getMean(),  0, 0

      // write out
      tdir.cd("/$runnum")
      epics_hists.each{ name, hist -> tdir.addDataSet(hist) }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // write timeline graphs
    // charge per run
    tdir.mkdir '/timelines'
    tdir.cd    '/timelines'
    tdir.addDataSet tl_asym_struck_qg
    tdir.addDataSet tl_asym_struck_qu
    tdir.addDataSet tl_asym_epics_fc
    tdir.addDataSet tl_asym_epics_slm

    // write HIPO files
    tdir.writeFile 'qadb_beam_charge_asymmetry.hipo'
  }

}
