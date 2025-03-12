package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.RFFitter;

class rftime_pim_CD {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/RF/H_pim_RFtime1')
  def f1 = RFFitter.fit(h1)

  data[run] = [run:run, mean:f1.getParameter(1), sigma:f1.getParameter(2).abs(), h1:h1, f1:f1]
}



def write() {


  ['mean', 'sigma'].each{name ->
    TDirectory out = new TDirectory()

    def grtl = new GraphErrors('RFtime_pim_CD'+name)
    grtl.setTitle("Average #pi<sup>-</sup> rftime1, CD ("+name+")")
    grtl.setTitleY("Average #pi<sup>-</sup> rftime1, CD ("+name+") (ns)")
    grtl.setTitleX("run number")

    data.sort{it.key}.each{run,it->
      grtl.addPoint(it.run, it[name], 0, 0)
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it.h1)
      out.addDataSet(it.f1)
    }

    out.mkdir('/timelines')
    out.cd('/timelines')
    out.addDataSet(grtl)
    out.writeFile('rftime_pim_CD_'+name+'.hipo')
  }

}
}
