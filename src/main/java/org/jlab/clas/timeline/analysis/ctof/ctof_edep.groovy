package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.CTOFFitter;

class ctof_edep {


def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/ctof/PathLCorrected Edep_p5')
  (6..10).each{
       def h2  = dir.getObject(String.format("/ctof/PathLCorrected Edep_p%d",it))
       h1.add(h2)
  }
  h1.setTitle(h1.getTitle()+"_10")
  def f1 = CTOFFitter.edepfit(h1)

  data[run] = [run:run, h1:h1, f1:f1, mean:f1.getParameter(1), chi2:f1.getChiSquare()]
}



def write() {


  def grtl = new GraphErrors('Edep')
  grtl.setTitle("Path-length corrected edep for negative tracks")
  grtl.setTitleY("Path-length corrected edep for negative tracks (MeV)")
  grtl.setTitleX("run number")

  TDirectory out = new TDirectory()

  data.sort{it.key}.each{run,it->
    out.mkdir('/'+it.run)
    out.cd('/'+it.run)
    out.addDataSet(it.h1)
    out.addDataSet(it.f1)
    grtl.addPoint(it.run, it.mean, 0, 0)
  }

  out.mkdir('/timelines')
  out.cd('/timelines')
  out.addDataSet(grtl)
  out.writeFile('ctof_edep.hipo')
}
}
