package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class central_pip_num {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/trig/H_trig_central_piplus_rat')

  data[run] = [run:run, h1:h1]
}



def write() {

  TDirectory out = new TDirectory()

  def grtl = new GraphErrors("pi plus per trigger")
  grtl.setTitle("#pi^+ per trigger")
  grtl.setTitleY("#pi^+ per trigger")
  grtl.setTitleX("run number")

  data.sort{it.key}.each{run,it->
    out.mkdir('/'+it.run)
    out.cd('/'+it.run)
    out.addDataSet(it.h1)
    grtl.addPoint(it.run, it.h1.getBinContent(0), 0, 0)
  }

  out.mkdir('/timelines')
  out.cd('/timelines')
  grtl.each{ out.addDataSet(it) }
  out.writeFile('cen_piplus.hipo')
}
}
