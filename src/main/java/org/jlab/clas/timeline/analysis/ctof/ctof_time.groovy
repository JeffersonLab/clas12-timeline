package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.CTOFFitter;

class ctof_time {


def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/ctof/H_CVT_t_neg')
  def f1 = CTOFFitter.timefit(h1)

  data[run] = [run:run, h1:h1, f1:f1, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs(), chi2:f1.getChiSquare()]
}



def write() {


  ['mean', 'sigma'].each{name->
    def grtl = new GraphErrors(name)
    grtl.setTitle("Corrected CTOF_vtime-STT for negative tracks, all pads (" + name +")")
    grtl.setTitleY("Corrected CTOF_vtime-STT for negative tracks, all pads (" + name + ") (ns)")
    grtl.setTitleX("run number")

    TDirectory out = new TDirectory()

    data.sort{it.key}.each{run,it->
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it.h1)
      out.addDataSet(it.f1)
      grtl.addPoint(it.run, it[name], 0, 0)
    }

    out.mkdir('/timelines')
    out.cd('/timelines')
    out.addDataSet(grtl)
    out.writeFile('ctof_time_'+name+'.hipo')
  }
}
}
