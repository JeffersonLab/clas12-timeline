package org.jlab.clas.timeline.timeline.rich
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.RICHFitter;

class rich_dt_m {


def data = new ConcurrentHashMap()

  def processDirectory(dir, run) {
    (1..2).each{module->
      def hs = dir.getObject("/RICH/H_RICH_setup")
      int m = module.toInteger()
      int sec = hs.getBinContent(m-1)

      def ttl = "sector${sec}"

      def h1 = dir.getObject("/RICH/H_RICH_dt_m${module}")

      def f1 = RICHFitter.timefit(h1)
      def meandt = 0
      def sigmadt = 0
      if (h1.getEntries() > 100) {
	meandt = f1.getParameter(1)
	sigmadt = f1.getParameter(2)
      }
      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h1, f1:f1, mean:meandt, sigma:sigmadt.abs()])
    }
  }


  def close() {
    ['mean', 'sigma'].each{ name ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.sort{it.key}.each{ttl, runs->

        def grtl = new GraphErrors(ttl)
        grtl.setTitle("#Delta T ${name} for e-, ${ttl}")
        grtl.setTitleY("#Delta T ${name} (ns)")
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
       out.writeFile("rich_dt_m_${name}.hipo")
    }
  }
}
