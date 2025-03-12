package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class rat_muon_num {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/trig/H_trig_sector_muon_rat')
  data[run] = [run:run, h1:h1]
}



def write() {


  TDirectory out = new TDirectory()
  out.mkdir('/timelines')

  (0..<3).each{ sec->
    def grtl = new GraphErrors('sector-pair '+(sec+1))
    grtl.setTitle("Muons per trigger per sector-pair number")
    grtl.setTitleY("Muons per trigger per sector-pair number")
    grtl.setTitleX("run number")

    data.sort{it.key}.each{run,it->
      if (sec==0){
        out.mkdir('/'+it.run)
        out.cd('/'+it.run)
        out.addDataSet(it.h1)
      }
      grtl.addPoint(it.run, it.h1.getBinContent(sec),0,0)
    }
    out.cd('/timelines')
    out.addDataSet(grtl)
  }

  out.writeFile('rat_muon.hipo')
}
}
