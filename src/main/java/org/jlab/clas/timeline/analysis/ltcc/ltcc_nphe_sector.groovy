package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class ltcc_nphe_sector {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def hlist = [3,5].collect{
    def hist = dir.getObject('/elec/H_trig_LTCCn_theta_S'+it).projectionY()
    hist.setName("sec"+(it))
    hist.setTitle("LTCC Number of Photoelectrons for electrons")
    hist.setTitleX("LTCC Number of Photoelectrons for electrons")
    hist
  }
  data[run] = [run:run, h3:hlist[0], h5:hlist[1]]
}



def write() {


  TDirectory out = new TDirectory()
  out.mkdir('/timelines')

  [3,5].each{ sec->
    def grtl = new GraphErrors('sec'+sec)
    grtl.setTitle("LTCC Number of Photoelectrons for electrons per sector")
    grtl.setTitleY("LTCC Number of Photoelectrons for electrons per sector")
    grtl.setTitleX("run number")

    data.sort{it.key}.each{run,it->
      if (sec==3){
        out.mkdir('/'+it.run)
      }
      out.cd('/'+it.run)

      out.addDataSet(it["h"+sec])
      grtl.addPoint(it.run, it["h"+sec].getMean(), 0, 0)
    }
    out.cd('/timelines')
    out.addDataSet(grtl)
  }

  out.writeFile('ltcc_elec_nphe_sec.hipo')
}
}
