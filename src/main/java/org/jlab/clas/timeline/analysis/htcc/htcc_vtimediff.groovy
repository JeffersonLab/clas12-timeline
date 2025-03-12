package org.jlab.clas.timeline.analysis
import org.jlab.groot.data.H1F
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.HTCCFitter

class htcc_vtimediff {

  def data = new ConcurrentHashMap()

  def processRun(dir, run) {

    [(0..<6), (0..<4), (0..<2)].combinations().each{s,r,side->
      def h1 = dir.getObject(String.format("/HTCC/H_HTCC_vtime_s%d_r%d_side%d",s+1,r+1,side+1))
      def f1 = HTCCFitter.timeIndPMT(h1)

      def ttl = "sector ${s+1} ring ${r+1} side ${side+1}"
      data.computeIfAbsent(ttl, {[]}).add([run:run, h1:h1, f1:f1, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs()])
    }

    println("debug: "+run)
  }



  def write() {
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
      out.writeFile("htcc_vtimediff_${name}.hipo")
    }
  }

}
