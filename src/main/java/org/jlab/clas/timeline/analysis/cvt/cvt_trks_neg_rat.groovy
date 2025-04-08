package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class cvt_trks_neg_rat {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/cvt/hnegtrks_rat')
  h1.setTitle("CVT Negative Tracks/ trigger");
  h1.setTitleX("CVT Negative Tracks/ trigger");

  data[run] = [run:run, h1:h1]
}



def write() {

  TDirectory out = new TDirectory()

  def grtl = new GraphErrors('CVT negative tracks per trigger')
  grtl.setTitle("Average CVT negative track multiplicity per trigger")
  grtl.setTitleY("Average CVT negative track multiplicity per trigger")
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
  out.writeFile('cvt_neg_tracks_ratio.hipo')
}
}
