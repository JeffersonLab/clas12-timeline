package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class band_meantimetdc {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def h1 = dir.getObject('/BAND/H_BAND_MeanTimeTDC_SectorCombination1')
  def h2 = dir.getObject('/BAND/H_BAND_MeanTimeTDC_SectorCombination2')

  data[run] = [run:run, Comb1:h1, Comb2:h2]
}



def write() {


  TDirectory out = new TDirectory()
  out.mkdir('/timelines')

  ["Comb1", "Comb2"].each{ name ->
    def gr = new GraphErrors(name)
    gr.setTitle("BAND mean time tdc sector combination")
    gr.setTitleY("maximum location of meantimeTDC - STT – sqrt(x^2+y^2+z^2)/c (ns)")
    gr.setTitleX("run number")

    data.sort{it.key}.each{run,it->
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it[name])
      gr.addPoint(it.run, it[name].getAxis().getBinCenter(it[name].getMaximumBin()), 0, 0)
    }
    out.cd('/timelines')
    out.addDataSet(gr)
  }

  out.writeFile('band_meantimetdc.hipo')
}
}
