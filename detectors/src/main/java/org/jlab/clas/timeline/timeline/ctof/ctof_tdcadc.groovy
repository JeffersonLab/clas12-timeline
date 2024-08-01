package org.jlab.clas.timeline.timeline.ctof
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.CTOFFitter;

class ctof_tdcadc {


def data = new ConcurrentHashMap()

def processDirectory(dir, run) {
  def h1 = dir.getObject('/ctof/CTOF TDC-ADC Time Difference')
  def f1s = CTOFFitter.tdcadcdifffit(h1)

  data[run]  = [
    run:        run,
    h1:         h1,
    meanLeft:   f1s[0].getParameter(1),
    sigmaLeft:  f1s[0].getParameter(2).abs(),
    chi2Left:   f1s[0].getChiSquare(),
    meanRight:  f1s[1].getParameter(1),
    sigmaRight: f1s[1].getParameter(2).abs(),
    chi2Right:  f1s[1].getChiSquare(),
    f1Combined: f1s[2],
  ]
}



def close() {


  ['mean', 'sigma'].each{name->
    def grtlLeft  = new GraphErrors("left_${name}")
    def grtlRight = new GraphErrors("right_${name}")
    [grtlLeft,grtlRight].each{ grtl ->
      grtl.setTitle("TDC time - FADC time averaged over CTOF counters")
      grtl.setTitleY("TDC time - FADC time averaged over CTOF counters [ns]")
      grtl.setTitleX("run number")
    }

    TDirectory out = new TDirectory()

    data.sort{it.key}.each{run,it->
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it.h1)
      out.addDataSet(it.f1Combined)
      grtlLeft.addPoint(it.run, it["${name}Left"], 0, 0)
      grtlRight.addPoint(it.run, it["${name}Right"], 0, 0)
    }

    out.mkdir('/timelines')
    out.cd('/timelines')
    out.addDataSet(grtlLeft)
    out.addDataSet(grtlRight)
    out.writeFile("ctof_tdcadc_time_${name}.hipo")
  }
}
}
