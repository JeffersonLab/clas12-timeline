package org.jlab.clas.timeline.analysis

import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.MoreFitter

class epics_hall_weather {

  def runlist = []

  def processRun(dir, run) {
    runlist.push(run)
  }

  def write() {

    def pvNames = [
      'pressure_hall_B':    'B_SYS_WEATHER_SF_L3_Press',
      'pressure_hall_D':    'RESET:i:GasPanelBarPress1', // Hall D pressure (fallback for periods where HallB pressure sensor is unavailable)
      'temperature_hall_B': 'B_SYS_WEATHER_SF_L1_Temp',
      'humidity_hall_B':    'B_SYS_WEATHER_SF_L1_Humid',
    ]

    def hallDUnitConversion = 4.015 // HallB pressure units = hallDUnitConversion * HallD pressure units

    def MYQ     = new MYQuery()
    def ts      = MYQ.getRunTimeStamps(runlist)
    def epics   = EpicsTools.queryEpics(MYQ, pvNames) { name, val -> name == 'pressure_hall_D' ? val * hallDUnitConversion : val }
    def data    = EpicsTools.mergeAndSort(epics, ts)
    def rundata = EpicsTools.segmentByRun(data, pvNames)

    def out = new TDirectory()

    def timelineGraphs = pvNames.collectEntries{ name, pv -> [name, new GraphErrors(name)] }

    rundata.each{run, vals->
      out.mkdir("/$run")
      out.cd("/$run")

      def hists = pvNames.collectEntries{ name, pv ->
        def entries = vals.collect{it[name]}
        [ name, EpicsTools.quantileHist("h$name$run", "$name from PV $pv for run $run;$name", entries) ]
      }

      vals.each{
        hists.each{ name, hist -> hist.fill(it[name]) }
      }

      timelineGraphs.each{ name, gr ->
        def mean = hists[name].getMean()
        gr.addPoint(run, mean, 0, 0)
      }

      hists.each{ name, hist -> out.addDataSet(hist) }

      println("$run done")
    }

    out.mkdir("/timelines")
    out.cd("/timelines")
    timelineGraphs.each{ name, gr -> out.addDataSet(gr) }
    out.writeFile("epics_hall_weather.hipo")
  }
}
