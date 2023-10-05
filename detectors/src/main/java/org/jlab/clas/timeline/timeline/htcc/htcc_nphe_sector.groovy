package org.jlab.clas.timeline.timeline.htcc
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class htcc_nphe_sector {

def data = new ConcurrentHashMap()

def processDirectory(dir, run) {
  def totalNphe = 0
  def numChannels = 48

  (1..6).each{sec->

    def hlist = [(1..4), [1,2]].combinations().collect{ring,side -> dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side$side")}
    def h1 = hlist.head()
    hlist.tail().each{h1.add(it)}

    def meanNphe = h1.getMean() 
    totalNphe += meanNphe 
  }

  def averageNphe = totalNphe / numChannels 

  (1..6).each{sec->
    def hlist = [(1..4), [1,2]].combinations().collect{ring,side -> dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side$side")}
    def h1 = hlist.head()
    hlist.tail().each{h1.add(it)}

    h1.setName("sec ${sec}")
    h1.setTitle("HTCC Number of Photoelectrons")
    h1.setTitleX("HTCC Number of Photoelectrons")

    def correctionFactor = averageNphe > 0 ? h1.getMean() / averageNphe : 0
    data.computeIfAbsent(sec, {[]}).add([run:run, h1:h1, correctionFactor:correctionFactor])
  }
}

def close() {

  TDirectory out = new TDirectory()
  out.mkdir('/timelines')

  data.each{sec,runs->
    def grtl = new GraphErrors("sec$sec")
    grtl.setTitle("Average HTCC Number of Photoelectrons per sector")
    grtl.setTitleY("Average HTCC Number of Photoelectrons per sector")
    grtl.setTitleX("run number")

    def grcorrection = new GraphErrors("sec$sec normalization factor")
    grcorrection.setTitle("Normalization factor (mean nphe per channel / average nphe across all channels) per sector")
    grcorrection.setTitleY("Normalization factor per sector")
    grcorrection.setTitleX("run number")

    runs.sort{it.run}.each{
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it.h1)
      grtl.addPoint(it.run, it.h1.getMean(), 0, 0)
      grcorrection.addPoint(it.run, it.correctionFactor, 0, 0)
    }
    out.cd('/timelines')
    out.addDataSet(grtl)
    out.addDataSet(grcorrection)
  }

  out.writeFile('htcc_nphe_sec.hipo')
}
}
