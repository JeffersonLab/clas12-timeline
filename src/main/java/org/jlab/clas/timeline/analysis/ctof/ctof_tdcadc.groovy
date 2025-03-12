package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.CTOFFitter;

class ctof_tdcadc {


def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/ctof/CTOF TDC-ADC Time Difference')
  def f1s = CTOFFitter.tdcadcdifffit(h1)

  data[run]  = [
    run:        run,
    h1:         h1,
    meanUpstream:   f1s[0].getParameter(1),
    sigmaUpstream:  f1s[0].getParameter(2).abs(),
    chi2Upstream:   f1s[0].getChiSquare(),
    meanDownstream:  f1s[1].getParameter(1),
    sigmaDownstream: f1s[1].getParameter(2).abs(),
    chi2Downstream:  f1s[1].getChiSquare(),
    f1Combined: f1s[2],
  ]
}



def write() {


  ['mean', 'sigma'].each{name->
    def grtlUpstream  = new GraphErrors("upstream_${name}")
    def grtlDownstream = new GraphErrors("downstream_${name}")
    [grtlUpstream,grtlDownstream].each{ grtl ->
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
      grtlUpstream.addPoint(it.run, it["${name}Upstream"], 0, 0)
      grtlDownstream.addPoint(it.run, it["${name}Downstream"], 0, 0)
    }

    out.mkdir('/timelines')
    out.cd('/timelines')
    out.addDataSet(grtlUpstream)
    out.addDataSet(grtlDownstream)
    out.writeFile("ctof_tdcadc_time_${name}.hipo")
  }
}
}
