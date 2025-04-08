package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class cvt_trks {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/cvt/htrks')
  h1.setTitle("CVT Track Multiplicity");
  h1.setTitleX("CVT Track Multiplicity");

  data[run] = [run:run, h1:h1]
}



def write() {

  TDirectory out = new TDirectory()

  def grtl = new GraphErrors('CVT Track Multiplicity')
  grtl.setTitle("Average CVT Track Multiplicity")
  grtl.setTitleY("Average CVT Track Multiplicity")
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
  out.writeFile('cvt_tracks.hipo')
}
}
