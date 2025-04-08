package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class cvt_d0_max_neg {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/cvt/H_CVT_absd0_neg')

  data[run] = [run:run, hlist:h1]
}



def write() {

  TDirectory out = new TDirectory()

  def grtl = new GraphErrors('cvt_d0_neg')
  grtl.setTitle("Std dev. for negatives")
  grtl.setTitleY("Std dev. for negatives (cm)")
  grtl.setTitleX("run number")


  data.sort{it.key}.each{run,it->
    out.mkdir('/'+it.run)
    out.cd('/'+it.run)
    out.addDataSet(it.hlist)
    def h1 = it.hlist
    grtl.addPoint(it.run, h1.getXaxis().getBinCenter(h1.getMaximumBin()), 0, 0)
  }

  out.mkdir('/timelines')
  out.cd('/timelines')
  grtl.each{ out.addDataSet(it) }
  out.writeFile('cvt_d0_max_neg.hipo')
}
}
