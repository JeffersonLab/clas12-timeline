package org.jlab.clas.timeline.timeline.ftof
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.MoreFitter

class ftof_ctof_vtdiff {

def data = new ConcurrentHashMap()

def processDirectory(dir, run) {
  (1..6).collect{sec->
    def h1 = dir.getObject("/tof/ftof-ctof_vtdiff_S${sec}")
    if(h1.integral() > 0.0) {
      def f1 = MoreFitter.fitgaus(h1)
      data.computeIfAbsent(sec, {[]}).add([run:run, h1:h1, f1:f1, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs()])
    }
  }
}



def close() {

  ['mean', 'sigma'].each{ name ->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')

    data.each{sec,runs->
      def grtl = new GraphErrors("sec${sec}")
      grtl.setTitle("TOF-CTOF vtdiff sector ${sec} (${name})")
      grtl.setTitleY("TOF-CTOF vtdiff sector ${sec} (${name}) [ns]")
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

    out.writeFile("ftof_ctof_vtdiff_${name}.hipo")
  }
}
}
