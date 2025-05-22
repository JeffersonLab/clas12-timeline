package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_start_time {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)

  def processRun(dir, run) {

    data[run] = [run:run]

    def start_time = dir.getObject('/ALERT/start time')
    if (start_time!=null) {
      data[run].put('start time', start_time)
      has_data.set(true)
    }
  }

  def write() {

    if(!has_data.get()) {
      System.err.println "WARNING: no data for this timeline, not producing"
      return
    }
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')

    // start time
    def gr = new GraphErrors("start time")
    gr.setTitle(  String.format("Start Time"))
    gr.setTitleY( String.format("Start Time of Electron Trigger Events(ns)"))
    gr.setTitleX("run number")
    data.sort{it.key}.each{run,it->
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      if (it.containsKey("start time")){
        out.addDataSet(it["start time"])
        gr.addPoint(it.run, it["start time"].getBinContent(it["start time"].getMaximumBin()), 0, 0)
      }
    }
    out.cd('/timelines')
    out.addDataSet(gr)
    out.writeFile('alert_atof_tdc_minus_start_time.hipo')
  }
}
