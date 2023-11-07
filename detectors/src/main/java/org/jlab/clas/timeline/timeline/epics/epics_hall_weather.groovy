package org.jlab.clas.timeline.timeline.epics

import java.text.SimpleDateFormat
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.MoreFitter


class epics_hall_weather {
  def runlist = []

  def processDirectory(dir, run) {
    runlist.push(run)
  }

  def close() {

    def pvNames = [
      'pressure':    'B_SYS_WEATHER_SF_L3_Press',
      'temperature': 'B_SYS_WEATHER_SF_L1_Temp',
      'humidity':    'B_SYS_WEATHER_SF_L1_Humid',
    ]

    def MYQ = new MYQuery()
    def ts = MYQ.getRunTimeStamps(runlist)

    def epics = [:].withDefault{[:]}
    def dateFormatStr = 'yyyy-MM-dd HH:mm:ss.SSS'
    def press = MYQ.query(pvNames.pressure).each{    epics[new SimpleDateFormat(dateFormatStr).parse(it.d).getTime()].press = it.v }
    def temp  = MYQ.query(pvNames.temperature).each{ epics[new SimpleDateFormat(dateFormatStr).parse(it.d).getTime()].temp  = it.v }
    def humid = MYQ.query(pvNames.humidity).each{    epics[new SimpleDateFormat(dateFormatStr).parse(it.d).getTime()].humid = it.v }

    println('dl finished')

    def data = epics.collect{kk,vv->[ts:kk]+vv} + ts.collectMany{[[run:it[0], ts: (((long)it[1])*1000)], [run:it[0], ts: (((long)it[2])*1000)]]}
    data.sort{it.ts}

    println('data sorted')

    def ts0, press0, temp0, humid0, r0=null
    def rundata = [:].withDefault{[]}
    data.each{
      if(it.run!=null) {
        r0 = r0 ? null : it.run
      } else if(r0) {
        rundata[r0].push(['time':it.ts-ts0, 'pressure':press0, 'temperature':temp0, 'humidity':humid0])
      }
      ts0 = it.ts
      if(it.press!=null) press0 = it.press
      if(it.temp!=null)  temp0  = it.temp
      if(it.humid!=null) humid0 = it.humid
    }

    def out = new TDirectory()

    def timelineGraphs = pvNames.collectEntries{ name,pv -> [name, new GraphErrors(pv)] }

    rundata.each{run,vals->
      out.mkdir("/$run")
      out.cd("/$run")

      def hists = pvNames.collectEntries{ name, pv ->
        def entries = vals.collectMany{[it[name]]}.sort()
        def nlen = entries.size()
        def (nq1,nq2,nq3) = [nlen/4 as int, nlen/2 as int, nlen*3/4 as int]
        def (q1,q2,q3) = [entries[nq1], entries[nq2], entries[nq3]]
        def (xm,dx) = [q2, q3-q1]
        [ name, new H1F("h$name$run","$name for run $run;$name", 200, xm-3*dx, xm+3*dx) ]
      }

      vals.each{
        hists.each{ name, hist -> hist.fill(it[name]) }
      }

      def fits = hists.collectEntries{ name, hist -> MoreFitter.gausFit(hist, "Q") }
      timelineGraphs.each{ name, gr -> gr.addPoint(run, fits[name].getParameter(1), 0, 0) }

      hists.each{ name, hist -> out.addDataSet(hist) }
      fits.each{  name, f    -> out.addDataSet(f)    }

      println("$run done")
    }

    out.mkdir("/timelines")
    out.cd("/timelines")
    timelineGraphs.each{ name, gr -> out.addDataSet(gr) }
    out.writeFile("epics_hall_weather.hipo")
  }
}
