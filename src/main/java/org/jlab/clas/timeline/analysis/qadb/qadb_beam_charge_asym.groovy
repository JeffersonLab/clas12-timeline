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
import org.jlab.clas.timeline.histograms.Trigger

class qadb_beam_charge_asym {

  def runlist          = []
  def data_map         = new ConcurrentHashMap()
  def enable_trig_asym = false

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

  // process trigger files (optional), appending to `data_map` like so:
  /*
     data_map
     |_ runnum 1
     |  |__ trig -> trigger-bit histograms for run 1
     |_ runnum 2
     |  |__ trig -> trigger-bit histograms for run 2
     :
  */
  def processTrig(dir, runnum) {
    def histos = new Trigger()
    histos.readHistograms(dir)
    data_map[runnum]['trig'] = histos
    enable_trig_asym = true
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map) {

    // PVs: obtained from `clas12-epics` -> `beam_charge_asym.stc`
    // these are maps of PV custom alias -> PV name from MYA/EPICS
    // FIXME: errors may be accessible from changing `q_asym` -> `d_asym`, but not sure if they're actually in MYA history
    def pv_names_asym = [
      EPICS_SLM_qAsym:   'q_asym_3',
      EPICS_FCUP_qAsym:  'q_asym_7',
      // EPICS_2C21A_qAsym: 'q_asym_16', // all zero, at least for RG-C
      // EPICS_2C24A_qAsym: 'q_asym_20', // all zero, at least for RG-C
      // EPICS_2H01_qAsym:  'q_asym_24', // all zero, at least for RG-C
    ]
    def pv_names_curr = [beam_curr: 'IPM2H01']

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

    // query MYA for EPICS data; done in a closure to minimize duplicate heap allocations
    def get_epics_data = {
      // get BCA data
      def myq_asym = new MYQuery(runlist)
      def data_asym = EpicsTools.queryEpics(myq_asym, pv_names_asym, false) { pv, val -> val / 100.0 } // convert percent to decimal units
      // get BCM data separately
      def myq_curr = new MYQuery(runlist)
      def data_curr = EpicsTools.queryEpics myq_curr, pv_names_curr, false
      // interleave them
      EpicsTools.interleave data_asym, data_curr
    }
    def epics_data = get_epics_data()
    // System.out.println Tools.prettyPrint('epics_data', epics_data)

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
    def tl_asym_struck_qg   = make_tl 'STRUCK_gated'
    def tl_asym_struck_qu   = make_tl 'STRUCK_ungated'
    def tl_asym_epics_fc    = make_tl 'EPICS_FCUP'
    def tl_asym_epics_slm   = make_tl 'EPICS_SLM'
    def tl_asym_trig_pulser = enable_trig_asym ? make_tl('TRIG_pulser') : null
    def tl_asym_trig_fc     = enable_trig_asym ? make_tl('TRIG_FC')     : null

    // helper function to calculate an asymmetry
    // @param q_helP charge (or yield) for helicity=+1
    // @param q_helN charge (or yield) for helicity=-1
    // @param n_helP normalization for helicity=+1
    // @param n_helN normalization for helicity=-1
    def calc_asym = { q_helP, q_helN, n_helP=1.0, n_helN=1.0 ->
      def p = Tools.safeRatio q_helP, n_helP
      def n = Tools.safeRatio q_helN, n_helN
      Tools.safeRatio p - n, p + n
    }

    // helper function for an error on raw asymmetries (e.g., trigger asymmetry)
    // @param p yield for helicity=+1
    // @param n yield for helicity=-1
    def calc_raw_asym_err = { p, n ->
      def t = p + n;
      2.0 * Math.sqrt( (p*n) / (t*t*t) )
    }

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
      def rn_struck_qu_helP = make_rn 'b2', 'STRUCK_helPositive_qUngated', 'STRUCK helicity=+1 q_ungated [nC]',     'q [nC]'
      def rn_struck_n_helP  = make_rn 'y1', 'STRUCK_helPositive_num',      'STRUCK helicity=+1 num. readouts',      'num readouts'
      def rn_struck_qg_helN = make_rn 'c1', 'STRUCK_helNegative_qGated',   'STRUCK helicity=-1 q_gated [nC]',       'q [nC]'
      def rn_struck_qu_helN = make_rn 'c2', 'STRUCK_helNegative_qUngated', 'STRUCK helicity=-1 q_ungated [nC]',     'q [nC]'
      def rn_struck_n_helN  = make_rn 'y2', 'STRUCK_helNegative_num',      'STRUCK helicity=-1 num. readouts',      'num readouts'
      def rn_struck_n_rat   = make_rn 'y3', 'STRUCK_num_rat',              'number of STRUCK readouts ratio N+/N-', 'N+/N-'

      // fill run graphs: loop over each QA bin's histograms (`Charge` objects), read the charge etc.
      run_data['histos'].each { binnum, histos ->
        rn_struck_qg_helP.addPoint binnum, histos.getChargeGatedSTRUCK(1),    0, Math.sqrt(histos.getChargeGatedSTRUCK(1))
        rn_struck_qu_helP.addPoint binnum, histos.getChargeUngatedSTRUCK(1),  0, Math.sqrt(histos.getChargeUngatedSTRUCK(1))
        rn_struck_n_helP.addPoint  binnum, histos.getNumReadoutsSTRUCK(1),    0, Math.sqrt(histos.getNumReadoutsSTRUCK(1))
        rn_struck_qg_helN.addPoint binnum, histos.getChargeGatedSTRUCK(-1),   0, Math.sqrt(histos.getChargeGatedSTRUCK(-1))
        rn_struck_qu_helN.addPoint binnum, histos.getChargeUngatedSTRUCK(-1), 0, Math.sqrt(histos.getChargeUngatedSTRUCK(-1))
        rn_struck_n_helN.addPoint  binnum, histos.getNumReadoutsSTRUCK(-1),   0, Math.sqrt(histos.getNumReadoutsSTRUCK(-1))
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
      tdir.addDataSet rn_struck_n_helP
      tdir.addDataSet rn_struck_n_helN

    } // end loop over runs

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // loop over runs, filling graphs for EPICS data
    epics_data.each{ runnum, epics_vals ->

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

      // create graphs and histograms, one for each PV
      def rn_epics_hists = pv_names_asym.collectEntries{ pv, _ ->
        [
          pv,
          EpicsTools.quantileHist(
            "d__${pv}__${runnum}",
            "$pv * $hwp_corr_title;$pv",
            epics_vals.findAll{it[pv]!=null}.collect{hwp_corr(it[pv])}.sort()
          )
        ]
      }
      def rn_epics_graphs = pv_names_asym.collectEntries{ pv, _ ->
        def gr = new GraphErrors("e__${pv}__${runnum}")
        gr.setTitle  "$pv * $hwp_corr_title"
        gr.setTitleY pv
        gr.setTitleX 'timestamp since run start'
        [pv, gr]
      }
      def rn_curr_graph = new GraphErrors("z__beam_curr__${runnum}")
      rn_curr_graph.setTitle  'beam current'
      rn_curr_graph.setTitleY 'beam current [nA]'
      rn_curr_graph.setTitleX 'timestamp since run start'

      // fill histograms, weighting by charge obtained from <current> * delta_time
      // - for a given PV and histogram entry:
      //   - the <current> is the average of the beam-current readings, weighted by the time between each of those readings
      //   - delta_time is the time between readings of this PV
      rn_epics_hists.each{ pv, hist ->
        def curr_sum   = 0.0
        def curr_count = 0
        def curr_ts0   = null
        def asym_ts0   = null
        epics_vals.each{ vals ->
          if(vals.beam_curr != null) { // if it's a beam current reading
            if(curr_ts0 != null) { // if we know the previous timestamp (i.e., skips first reading)
              def delta_time =  vals.timestamp - curr_ts0      // time since last current reading
              curr_sum       += vals.beam_curr * delta_time // sum, weighted by delta_time
              curr_count     += delta_time
            }
            curr_ts0 = vals.timestamp
          }
          else if(vals[pv] != null) { // otherwise if it's a BCA reading that includes the current `pv`
            if(asym_ts0 != null) { // if we know the previous timestamp (i.e., skips first reading)
              def beam_curr_ave = Tools.safeRatio curr_sum, curr_count // average current, weighted by time between each current reading
              def delta_time       = vals.timestamp - asym_ts0            // time since last asym `pv` reading
              def pv_val           = hwp_corr vals[pv]                    // HWP correction
              hist.fill pv_val, beam_curr_ave * delta_time             // fill histogram, weighting by charge
              curr_sum   = 0.0 // reset the current sums
              curr_count = 0
            }
            asym_ts0 = vals.timestamp
          }
        }
      }

      // fill graphs
      epics_vals.each{ vals ->
        rn_epics_graphs.each{ pv, gr ->
          if(vals[pv] != null) {
            def pv_val = hwp_corr vals[pv]
            gr.addPoint vals.timestamp, pv_val, 0, 0
          }
        }
        if(vals.beam_curr != null) {
          rn_curr_graph.addPoint vals.timestamp, vals.beam_curr, 0, 0
        }
      }

      // fill timeline graphs
      tl_asym_epics_fc.addPoint  runnum, rn_epics_hists['EPICS_FCUP_qAsym'].getMean(), 0, 0
      tl_asym_epics_slm.addPoint runnum, rn_epics_hists['EPICS_SLM_qAsym'].getMean(),  0, 0

      // write out
      tdir.cd("/$runnum")
      rn_epics_graphs.each{ pv, gr -> tdir.addDataSet(gr) }
      tdir.addDataSet rn_curr_graph
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // if there are trigger histograms, produce trigger-bit asymmetry timelines too
    if(enable_trig_asym) {
      data_map.sort{it.key}.each { runnum, run_data ->
        if(run_data['trig'] != null) {
          // trigger bit numbers we want
          def TRIG_BIT = [
            'pulser': 31,
            'fc':     39,
          ]
          // count number of triggers for each one we want
          def ntrig = ['helP':[:], 'helN':[:]]
          [
            'helP': run_data.trig.bits_helP,
            'helN': run_data.trig.bits_helN,
          ].each { heli, histo ->
            TRIG_BIT.each{ name, bit -> ntrig[heli][name] = histo.getBinContent(bit) }
          }
          // System.out.println "$runnum" + Tools.prettyPrint(ntrig)
          // fill the timeline
          tl_asym_trig_pulser.addPoint runnum, calc_asym(ntrig.helP.pulser, ntrig.helN.pulser), 0, calc_raw_asym_err(ntrig.helP.pulser, ntrig.helN.pulser)
          tl_asym_trig_fc.addPoint     runnum, calc_asym(ntrig.helP.fc,     ntrig.helN.fc),     0, calc_raw_asym_err(ntrig.helP.fc,     ntrig.helN.fc)
          // write the trigger-bit histos
          tdir.cd("/$runnum")
          run_data.trig.bits_helP.setName 't1' + run_data.trig.bits_helP.getName()
          run_data.trig.bits_helN.setName 't2' + run_data.trig.bits_helN.getName()
          tdir.addDataSet run_data.trig.bits_helP
          tdir.addDataSet run_data.trig.bits_helN
        }
      }
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
    if(enable_trig_asym) {
      tdir.addDataSet tl_asym_trig_pulser
      tdir.addDataSet tl_asym_trig_fc
    }

    // write HIPO files
    tdir.writeFile 'qadb_beam_charge_asymmetry.hipo'
  }

}
