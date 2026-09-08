package org.jlab.clas.timeline.analysis

import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.MoreFitter

import org.rcdb.*

class epics_q_asym {

  def runlist = []

  def processRun(dir, run) {
    runlist.push(run)
  }

  def write() {

    // PVs obtained from `clas12-epics` -> `beam_charge_asym.stc`
    // FIXME: errors may be accessible from changing `q_asym` -> `d_asym`, but not sure if they're actually in MYA history
    def pvNames = [
      q_asym_SLM:   'q_asym_3',
      q_asym_FCUP:  'q_asym_7',
      // q_asym_2C21A: 'q_asym_16', // all zero, at least for RG-C
      // q_asym_2C24A: 'q_asym_20', // all zero, at least for RG-C
      // q_asym_2H01:  'q_asym_24', // all zero, at least for RG-C
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

    // query MYA
    def MYQ     = new MYQuery()
    def ts      = MYQ.getRunTimeStamps(runlist)
    def epics   = EpicsTools.queryEpics(MYQ, pvNames)
    def data    = EpicsTools.mergeAndSort(epics, ts)
    def rundata = EpicsTools.segmentByRun(data, pvNames)

    def out = new TDirectory()

    def timelineGraphs = pvNames.collectEntries{ name, pv -> [name, new GraphErrors(name)] }
    timelineGraphs.each{ name, gr -> gr.setTitle 'Beam Charge Asymmetry * (HWP==IN ? -1 : +1) [units=%]' }

    rundata.each{run, vals->
      out.mkdir("/$run")
      out.cd("/$run")

      // fill histograms (they are NOT multiplied by HWP sign)
      def hists = pvNames.collectEntries{ name, pv ->
        def entries = vals.collect{it[name]}
        [ name, EpicsTools.quantileHist("h$name$run", "$name from PV $pv for run $run;$name", entries) ]
      }
      vals.each{
        hists.each{ name, hist -> hist.fill(it[name]) }
      }

      // get HWP position
      def hwp_cond = rcdbProvider.getCondition(run, 'half_wave_plate') // 0=IN, 1=OUT
      if(hwp_cond==null) {
        System.err.println "ERROR: cannot find run $run in RCDB, thus cannot get HWP status"
        System.exit(100)
      }
      def hwp = hwp_cond.toLong()
      // System.out.println("HWP: $run $hwp")

      // fill timeline graphs, correcting for HWP sign
      timelineGraphs.each{ name, gr ->
        def mean = hists[name].getMean() * (hwp==0 ? -1 : 1) // HWP: 0=IN, 1=OUT
        gr.addPoint(run, mean, 0, 0)
      }

      // write out
      hists.each{ name, hist -> out.addDataSet(hist) }
      println("$run done")
    }

    out.mkdir("/timelines")
    out.cd("/timelines")
    timelineGraphs.each{ name, gr -> out.addDataSet(gr) }
    out.writeFile("epics_q_asym.hipo")
  }
}
