package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class cvt_chi2_neg {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/cvt/H_CVT_chi2_neg')

  data[run] = [run:run, h1:h1]
}



def write() {

  TDirectory out = new TDirectory()

  def grtl = new GraphErrors('cvt_chi2_neg')
  grtl.setTitle("Average CVT chi2 for negatives")
  grtl.setTitleY("Average CVT chi2 for negatives")
  grtl.setTitleX("run number")

  data.sort{it.key}.each{run,it->
    out.mkdir('/'+it.run)
    out.cd('/'+it.run)
    out.addDataSet(it.h1)
    grtl.addPoint(it.run, it.h1.getMean(), 0, 0)
  }

  out.mkdir('/timelines')
  out.cd('/timelines')
  grtl.each{ out.addDataSet(it) }
  out.writeFile('cvt_chi2_neg.hipo')
}
}
