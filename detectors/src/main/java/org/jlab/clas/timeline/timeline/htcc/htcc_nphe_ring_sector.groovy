package org.jlab.clas.timeline.timeline.htcc
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class htcc_nphe_ring_sector {

def data = new ConcurrentHashMap()
def totalNphe = 0 // Initialize a variable to store the total NPHE across all channels

def processDirectory(dir, run) {
  (1..6).each{sec->
    (1..4).each{ring->
      def h1 = dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side1") //left
      def h2 = dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side2") //right
      h1.add(h2)

      def meanNphe = h1.getMean() // Calculate the mean NPHE for this channel
      totalNphe += meanNphe // Add the mean NPHE to the total

      def name = "sec $sec ring $ring"
      h1.setName(name)
      h1.setTitle("HTCC Number of Photoelectrons")
      h1.setTitleX("HTCC Number of Photoelectrons")

      data.computeIfAbsent(name, {[]}).add([run: run, h1: h1])
    }
  }
}

// Calculate the average NPHE across all channels
def averageNphe = totalNphe / 48 

def close() {

  TDirectory out = new TDirectory()
  out.mkdir('/timelines')

  data.each{name,runs->
    def grtl = new GraphErrors(name)
    grtl.setTitle("Average HTCC Number of Photoelectrons per sector per ring")
    grtl.setTitleY("Average HTCC Number of Photoelectrons per sector per ring")
    grtl.setTitleX("run number")

    def grtl_2 = new GraphErrors(name)
    grtl_2.setTitle("Normalization factor (mean nphe per channel / average nphe across all channels) per sector per ring")
    grtl_2.setTitleY("Normalization factor per sector per ring")
    grtl_2.setTitleX("run number")

    runs.sort{it.run}.each{
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it.h1)
      grtl.addPoint(it.run, it.h1.getMean(), 0, 0)

      // Calculate the correction factor
      def correctionFactor = it.h1.getMean() / averageNphe
      grtl_2.addPoint(it.run, correctionFactor, 0, 0)
    }
    out.cd('/timelines')
    out.addDataSet(grtl)
    out.addDataSet(grtl_2)
  }

  out.writeFile('htcc_nphe_sec_ring.hipo')
  out.writeFile('htcc_factor_sec_ring.hipo')
}
}
