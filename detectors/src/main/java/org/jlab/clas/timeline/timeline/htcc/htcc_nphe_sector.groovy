package org.jlab.clas.timeline.timeline.htcc
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class htcc_nphe_sector {

def data = new ConcurrentHashMap()
def totalNphe = 0

def processDirectory(dir, run) {
  (1..6).each{sec->

    def hlist = [(1..4), [1,2]].combinations().collect{ring,side -> dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side$side")}
    def h1 = hlist.head()
    hlist.tail().each{h1.add(it)}

    def meanNphe = h1.getMean() 
    totalNphe += meanNphe 

    h1.setName("sec ${sec}")
    h1.setTitle("HTCC Number of Photoelectrons")
    h1.setTitleX("HTCC Number of Photoelectrons")

    data.computeIfAbsent(sec, {[]}).add([run:run, h1:h1])
  }
}

def averageNphe = totalNphe / 48 

def close() {

  TDirectory out = new TDirectory()
  out.mkdir('/timelines')

  data.each{sec,runs->
    def grtl = new GraphErrors("sec$sec")
    grtl.setTitle("Average HTCC Number of Photoelectrons per sector")
    grtl.setTitleY("Average HTCC Number of Photoelectrons per sector")
    grtl.setTitleX("run number")

    def grtl_2 = new GraphErrors("sec$sec")
    grtl_2.setTitle("Normalization factor (mean nphe per channel / average nphe across all channels) per sector per ring")
    grtl_2.setTitleY("Normalization factor per sector per ring")
    grtl_2.setTitleX("run number")

    runs.sort{it.run}.each{
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it.h1)
      grtl.addPoint(it.run, it.h1.getMean(), 0, 0)

      def correctionFactor = it.h1.getMean() / averageNphe
      grtl_2.addPoint(it.run, correctionFactor, 0, 0)
    }
    out.cd('/timelines')
    out.addDataSet(grtl)
    out.addDataSet(grtl_2)
  }

  out.writeFile('htcc_nphe_sec.hipo')
  out.writeFile('htcc_factor_sec_ring.hipo')
}
}
