package org.jlab.clas.timeline.analysis

import java.text.SimpleDateFormat
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
    def MYQ = new MYQuery()
    def ts = MYQ.getRunTimeStamps(runlist)
    def epics = [:].withDefault{[:]}
    def dateFormatStr = 'yyyy-MM-dd HH:mm:ss.SSS'
    pvNames.each{ name, pv ->
      MYQ.query(pv).each{
        def val = it.v
        epics[new SimpleDateFormat(dateFormatStr).parse(it.d).getTime()][name] = val
      }
    }
    println('dl finished')

    def data = epics.collect{kk,vv->[ts:kk]+vv} + ts.collectMany{[[run:it[0], ts: (((long)it[1])*1000)], [run:it[0], ts: (((long)it[2])*1000)]]}
    data.sort{it.ts}

    println('data sorted')

    def ts0, r0=null
    def vals0 = pvNames.collectEntries{ name, pv -> [name, null] }
    def rundata = [:].withDefault{[]}
    data.each{
      if(it.run!=null) {
        r0 = r0 ? null : it.run
      } else if(r0) {
        rundata[r0].push(['time':it.ts-ts0] + vals0)
      }
      ts0 = it.ts
      pvNames.each{ name, pv -> if(it[name]!=null) vals0[name] = it[name] }
    }

    def out = new TDirectory()

    def timelineGraphs = pvNames.collectEntries{ name, pv -> [name, new GraphErrors(name)] }
    timelineGraphs.each{ name, gr -> gr.setTitle 'Beam Charge Asymmetry * (HWP==IN ? -1 : +1) [units=%]' }

    rundata.each{run, vals->
      out.mkdir("/$run")
      out.cd("/$run")

      // fill histograms (they are NOT multiplied by HWP sign)
      def hists = pvNames.collectEntries{ name, pv ->
        def entries = vals.collectMany{[it[name]]}.sort()
        def nlen = entries.size()
        def (nq1,nq2,nq3) = [nlen/4 as int, nlen/2 as int, nlen*3/4 as int]
        def (q1,q2,q3) = [entries[nq1], entries[nq2], entries[nq3]]
        def (xm,dx) = [q2, q3-q1]
        [ name, new H1F("h$name$run","$name from PV $pv for run $run;$name", 200, xm-3*dx, xm+3*dx) ]
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
