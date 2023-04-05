package org.jlab.clas.timeline.timeline.htcc
import org.jlab.groot.data.H1F
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.HTCCFitter

class htcc_vtimediff_sector {

  def data = new ConcurrentHashMap()

  def processDirectory(dir, run) {

    (0..<6).each{sec->
      def hlist = [(0..<4),(0..<2)].combinations().collect{ring,side->dir.getObject("/HTCC/H_HTCC_vtime_s${sec+1}_r${ring+1}_side${side+1}")}
      def h0 = hlist.head()
      hlist.tail().each{h0.add(it)}

      def maxbin = h0.getMaximumBin()

      def h1 = new H1F(h0.getName(), h0.getTitle(), 200, h0.getDataX(maxbin)-100.5*h0.getDataEX(0), h0.getDataX(maxbin)+99.5*h0.getDataEX(0))
      (0..<200).each{h1.setBinContent(it, h0.getBinContent(maxbin-100+it))}

      def f1 = HTCCFitter.timeIndPMT(h1)

      def ttl = "sector ${sec+1}"
      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h1, f1:f1, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs()])
    }

    println("debug: "+run)
  }



  def close() {
    ['mean', 'sigma'].each{name ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.sort{it.key}.each{ttl, runs->

        def grtl = new GraphErrors(ttl)
        grtl.setTitle("HTCC vtime - STT, electrons")
        grtl.setTitleY("HTCC vtime - STT, electrons, per PMTs (ns)")
        grtl.setTitleX("run number")

        runs.sort{it.run}.each{
          out.mkdir('/'+it.run)
          out.cd('/'+it.run)
          out.addDataSet(it.h1)
          out.addDataSet(it.f1)

          grtl.addPoint(it.run, it[name], 0, 0)
        }

        out.cd('/timelines')
        out.addDataSet(grtl)
      }
      out.writeFile("htcc_vtimediff_sector_${name}.hipo")
    }
  }

}
