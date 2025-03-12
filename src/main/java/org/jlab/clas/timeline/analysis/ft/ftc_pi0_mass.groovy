package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTFitter

class ftc_pi0_mass {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/ft/hpi0sum')
  def f1 = FTFitter.pi0fit(h1)

  data[run] = [run:run, h1:h1, f1:f1, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs(), chi2:f1.getChiSquare()]
}



def write() {


  ['mean', 'sigma'].each{name->
    def grtl = new GraphErrors(name)
    grtl.setTitle("FTC pi0 mass ("+name+")")
    grtl.setTitleY("FTC pi0 mass ("+name+") (MeV)")
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
    out.writeFile('ftc_pi0_mass_'+name+'.hipo')
  }
}
}
