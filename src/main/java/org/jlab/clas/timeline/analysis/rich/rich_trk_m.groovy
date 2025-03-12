package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.RICHFitter;

class rich_trk_m {


def data = new ConcurrentHashMap()

  def processRun(dir, run) {
    (1..2).each{module->
      def hs = dir.getObject("/RICH/H_RICH_setup")
      int m = module.toInteger()
      int sec = hs.getBinContent(m-1)

      def ttl = "sector${sec}"

      def h1 = dir.getObject("/RICH/H_RICH_trk_match_m${module}")

      float meand = 0
      float rmsd = 0
      if (h1.getEntries() > 100) {
	meand = h1.getMean()
	rmsd = h1.getRMS()
      }
      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h1, mean:meand, rms:rmsd])

    }
  }


  def write() {
    ['mean', 'rms'].each{ name ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.sort{it.key}.each{ttl, runs->

        def grtl = new GraphErrors(ttl)
        grtl.setTitle("Track matching ${name} for e-, ${ttl}")
        grtl.setTitleY("#Delta R ${name} (cm), ${ttl}")
        grtl.setTitleX("run number")

        runs.sort{it.run}.each{
          out.mkdir("/${it.run}")
          out.cd("/${it.run}")

          out.addDataSet(it.h1)

          grtl.addPoint(it.run, it[name], 0, 0)
        }

        out.cd('/timelines')
        out.addDataSet(grtl)
       }
       out.writeFile("rich_trk_m_${name}.hipo")
    }

  }

}
