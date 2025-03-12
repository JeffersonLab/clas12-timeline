package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.HTCCFitter

class htcc_npheAll {

  def data = new ConcurrentHashMap()

  def processRun(dir, run) {
    def h1 = dir.getObject('/HTCC/npheAll')
    def f1 = HTCCFitter.timeAllFit(h1)
    def mean = f1.getParameter(1)
    def sigma = f1.getParameter(2)
    data[run] = [run:run, h1:h1 ]
  }



  def write() {

    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    // (0..<6).each{ sec->
    def grtl = new GraphErrors('htcc_npheAll_mean')
    grtl.setTitle("Mean NPHE Combined Across All Channels per Run")
    grtl.setTitleY("Mean NPHE")
    grtl.setTitleX("Run Number")

    data.sort{it.key}.each{run,it->
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it.h1)
      grtl.addPoint(it.run, it.h1.getMean(), 0, 0)
    }
    out.cd('/timelines')
    out.addDataSet(grtl)

    out.writeFile('htcc_npheAll.hipo')
  }
}
