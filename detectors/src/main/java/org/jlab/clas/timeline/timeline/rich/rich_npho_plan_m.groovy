package org.jlab.clas.timeline.timeline.rich
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.RICHFitter;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

class rich_npho_plan_m {


def data = new ConcurrentHashMap()

  def processDirectory(dir, run) {
    (1..2).each{module->
      def hs = dir.getObject("/RICH/H_RICH_setup")
      int m = module.toInteger()
      int sec = hs.getBinContent(m-1)

      def ttl = "sector${sec}"

      def h1 = dir.getObject("/RICH/H_RICH_npho_m${module}_top2")
      int nb = h1.getDataSize(0)
      H2F h2 = h1.rebinX(nb);

      ArrayList<H1F> h2_sl = h2.getSlicesX();
      H1F h2_prox = h2_sl.get(0);
      h2_prox.setTitle("RICH Module ${module} sector ${sec}, Number of planar photons per e-")
	
      float meand = 0
      if (h1.getEntries() > 100) {
	meand = h2_prox.getMean()
      }

      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h2_prox, mean:meand])

    }
  }


  def close() {
    ['mean'].each{ name ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.sort{it.key}.each{ttl, runs->

        def grtl = new GraphErrors(ttl)
        grtl.setTitle("Number of planar photons per e-, ${ttl}")
        grtl.setTitleY("N_#gamma, ${ttl}")
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
       out.writeFile("rich_npho_plan_m_${name}.hipo")
    }

  }

}
