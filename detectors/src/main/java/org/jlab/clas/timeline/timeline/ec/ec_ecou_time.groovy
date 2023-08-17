package org.jlab.clas.timeline.timeline.ec
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ECFitter

class ec_ecou_time {

def data = new ConcurrentHashMap()

  def processDirectory(dir, run) {
    (1..6).each{sec->
      def ttl = "sec${sec}"

      def h1 = dir.getObject("/elec/H_trig_ECOU_vt_S${sec}")
      h1.setTitle("ECOU Time Residual")
      h1.setTitleX("ECOU Time Residual (ns)")

      def f1 = ECFitter.timefit(h1)
      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h1, f1:f1, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs()])
    }
  }



  def close() {
    ['mean', 'sigma'].each{name->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.sort{it.key}.each{ttl, runs->

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

        out.cd('/timelines')
        out.addDataSet(grtl)
       }
       out.writeFile("ec_elec_ecou_time_${name}.hipo")
    }
  }
}
