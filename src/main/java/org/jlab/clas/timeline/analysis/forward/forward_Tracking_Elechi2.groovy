package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class forward_Tracking_Elechi2 {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def histlist =   (0..<6).collect{
    def h1 = dir.getObject('/dc/H_dce_chi2_S'+(it+1))
    return h1
  }
  data[run] = [run:run, hlist:histlist]
}



def write() {

  TDirectory out = new TDirectory()
  out.mkdir('/timelines')
  (0..<6).each{ sec->
    def grtl = new GraphErrors('sec'+(sec+1))
    grtl.setTitle("Average Forward Reconstruction chi2 for electrons")
    grtl.setTitleY("Average Forward Reconstruction chi2 for electrons")
    grtl.setTitleX("run number")

    data.sort{it.key}.each{run,it->
      if (sec==0){
        out.mkdir('/'+it.run)
      }
      out.cd('/'+it.run)
      out.addDataSet(it.hlist[sec])
      grtl.addPoint(it.run, it.hlist[sec].getMean(), 0, 0)
    }
    out.cd('/timelines')
    out.addDataSet(grtl)
  }

  out.writeFile('forward_electron_chi2.hipo')
}
}
