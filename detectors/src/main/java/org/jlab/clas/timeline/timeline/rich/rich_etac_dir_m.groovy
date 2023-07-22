package org.jlab.clas.timeline.timeline.rich
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.RICHFitter;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

class rich_etac_dir_m {


def data = new ConcurrentHashMap()

  def processDirectory(dir, run) {
    (1..2).each{module->
      def hs = dir.getObject("/RICH/H_RICH_setup")
      int m = module.toInteger()
      int sec = hs.getBinContent(m-1)

      def ttl = "sector${sec}"

      def h1 = dir.getObject("/RICH/H_RICH_detac_m${module}_top1")
      int nb = h1.getDataSize(0)
      H2F h2 = h1.rebinX(nb);

      ArrayList<H1F> h2_sl = h2.getSlicesX();
      H1F h2_prox = h2_sl.get(0);
      h2_prox.setTitle("RICH Module ${module} sector ${sec}, #theta_C(meas)-#theta_C(calc) for e- (direct photons)")
      h2_prox.setTitleX("#Delta theta (mrad)");
	
      float meand = 0
      float rmsd = 0
      if (h1.getEntries() > 100) {
	meand = h2_prox.getMean()
	rmsd = h2_prox.getRMS()
      }

      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h2_prox, mean:meand, rms:rmsd])

    }
  }


  def close() {
    ['mean', 'rms'].each{ name ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.sort{it.key}.each{ttl, runs->

        def grtl = new GraphErrors(ttl)
        grtl.setTitle("Cherenkov angle shift ${name} for e- (direct photons), ${ttl}")
        grtl.setTitleY("#Delta #theta ${name} (mrad), ${ttl}")
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
       out.writeFile("rich_etac_dir_m_${name}.hipo")
    }

  }

}
