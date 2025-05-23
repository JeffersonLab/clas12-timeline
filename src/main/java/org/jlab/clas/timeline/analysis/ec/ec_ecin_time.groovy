package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ECFitter
import org.jlab.clas.timeline.util.QA

class ec_ecin_time {

def data = new ConcurrentHashMap()

  def processRun(dir, run) {
    (1..6).each{sec->
      def ttl = "sec${sec}"

      def h1 = dir.getObject("/elec/H_trig_ECIN_vt_S${sec}")
      h1.setTitle("ECIN Time Residual")
      h1.setTitleX("ECIN Time Residual (ns)")

      def f1 = ECFitter.timefit(h1)
      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h1, f1:f1, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs()])
    }
  }



  def write() {
    ['mean', 'sigma'].each{name->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      def grtlList = data.sort{it.key}.collect{ttl, runs->

        def grtl = new GraphErrors(ttl)
        grtl.setTitle("e- time - start time, ${name}")
        grtl.setTitleY("e- time - start time, ${name} (ns)")
        grtl.setTitleX("run number")

        runs.sort{it.run}.each{
          out.mkdir("/${it.run}")
          out.cd("/${it.run}")

          out.addDataSet(it.h1)
          out.addDataSet(it.f1)

          grtl.addPoint(it.run, it[name], 0, 0)
        }

        grtl
      }

      out.cd('/timelines')
      QA.cutGraphsMeanSigma(name, *grtlList, mean_lb: -0.15, mean_ub: 0.15, sigma_ub: 0.6, out: out)
      out.writeFile("ec_elec_ecin_time_${name}_QA.hipo")

    }
  }
}
