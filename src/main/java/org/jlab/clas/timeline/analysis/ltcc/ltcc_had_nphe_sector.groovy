package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class ltcc_had_nphe_sector {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/LTCC/H_piplus_S3_nphe')
  def h2 = dir.getObject('/LTCC/H_piplus_S5_nphe')
  def h3 = dir.getObject('/LTCC/H_piminus_S3_nphe')
  def h4 = dir.getObject('/LTCC/H_piminus_S5_nphe')
  data[run] = [run:run, pip3:h1, pip5:h2, pim3:h3, pim5:h4]
}



def write() {



  ['pip', 'pim'].each{ name ->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    [3,5].each{ sec->
      def grtl = new GraphErrors('sec'+sec)
      grtl.setTitle("LTCC Number of Photoelectrons for " + name + " per sector")
      grtl.setTitleY("LTCC Number of Photoelectrons for " + name + " per sector")
      grtl.setTitleX("run number")

      data.sort{it.key}.each{run,it->
        if (sec==3){
          out.mkdir('/'+it.run)
        }
        out.cd('/'+it.run)
        out.addDataSet(it[name+sec])
        grtl.addPoint(it.run, it[name+sec].getMean(), 0, 0)
      }
      out.cd('/timelines')
      out.addDataSet(grtl)
    }

    out.writeFile('ltcc_'+name+'_nphe_sec.hipo')
  }
}
}
