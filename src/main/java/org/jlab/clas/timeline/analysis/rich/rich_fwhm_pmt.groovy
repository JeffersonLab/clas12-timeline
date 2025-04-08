package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.RICHFitter;

class rich_fwhm_pmt {


def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/RICH/H_RICH_FWHM')
  data[run] = [run:run, h1:h1, fwhm_max:h1.getBinContent(h1.getMaximumBin())]
}



def write() {


  ['fwhm_max'].each{name->
    def grtl = new GraphErrors(name)
    grtl.setTitle("RICH maximum FWHM of T_meas-T_calc per pmt photons")
    grtl.setTitleY("RICH maximum FWHM of T_meas-T_calc per pmt photons (ns)")
    grtl.setTitleX("run number")

    TDirectory out = new TDirectory()

    data.sort{it.key}.each{run,it->
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      it.h1.setName(it.h1.getName() + ", max at "+it.h1.getAxis().getBinCenter(it.h1.getMaximumBin()))
      out.addDataSet(it.h1)
      grtl.addPoint(it.run, it[name], 0, 0)
    }

    out.mkdir('/timelines')
    out.cd('/timelines')
    out.addDataSet(grtl)
    out.writeFile('rich_time_'+name+'.hipo')
  }

}
}
