package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.RICHFitter;

class rich_npim_m {


def data = new ConcurrentHashMap()

  def processRun(dir, run) {
    (1..2).each{module->
      def hs = dir.getObject("/RICH/H_RICH_setup")
      int m = module.toInteger()
      int sec = hs.getBinContent(m-1)

      def ttl = "sector${sec}"

      def he = dir.getObject("/RICH/H_RICH_ntrigele")
      //float nele = he.getBinContent(sec-1)
      float nele = he.getEntries()

      def h1 = dir.getObject("/RICH/H_RICH_npim_tile_m${module}")
      float np = h1.getEntries()
      float rp = 0;
      if (nele > 0) {
	rp = np / nele
      }

      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h1, rat:rp])

    }
  }


  def write() {
    ['rat'].each{ name ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.sort{it.key}.each{ttl, runs->

        def grtl = new GraphErrors(ttl)
        grtl.setTitle("RICH #pi- per trigger e-, ${ttl}")
        grtl.setTitleY("N(pi-)/N(e), ${ttl}")
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
       out.writeFile("rich_npim_m_${name}.hipo")
    }

  }

}
