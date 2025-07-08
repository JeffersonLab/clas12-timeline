package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.util.QA
import org.jlab.clas.timeline.util.RunDependentCut

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

  if(data.size() == 0) {
    System.err.println "ERROR: no data for this timeline"
    System.exit(100)
  }

  def dataset = RunDependentCut.findDataset(data.keySet().toList())
  def qa_cuts = [:]
  if(dataset == 'rga_fa18_inbending' || dataset == 'rga_fa18_outbending') {
    qa_cuts[3] = [7, 9]
    qa_cuts[5] = [4, 6]
  }
  else if(dataset == 'rgc_su22') {
    qa_cuts[3] = [12, 18]
    qa_cuts[5] = [12, 18]
  }
  else {
    qa_cuts[3] = [11, 14]
    qa_cuts[5] = [11, 14]
  }
  def qa_colors = [3: 'red', 5: 'blue']

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
    QA.cutGraphs(grtl, lb: qa_cuts[sec][0], ub: qa_cuts[sec][1], lb_color: qa_colors[sec], ub_color: qa_colors[sec], out: out)
  }

  out.writeFile('ltcc_elec_nphe_sec_QA.hipo')
}
}
