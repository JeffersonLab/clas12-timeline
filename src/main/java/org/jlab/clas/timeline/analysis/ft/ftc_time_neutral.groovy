package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTFitter

class ftc_time_neutral {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/ft/hi_cal_time_neu')
  def f1 = FTFitter.ftctimefit(h1)

  data[run] = [run:run, h1:h1, f1:f1, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs(), chi2:f1.getChiSquare()]
}



def write() {


  ['mean', 'sigma'].each{name->
    def grtl = new GraphErrors(name)
    grtl.setTitle("FTC time - start time for neutrals (" + name + ")")
    grtl.setTitleY("FTC time - start time for neutrals (" + name + ") (ns)")
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
    out.writeFile('ftc_time_neu_'+name+'.hipo')
  }
}
}
