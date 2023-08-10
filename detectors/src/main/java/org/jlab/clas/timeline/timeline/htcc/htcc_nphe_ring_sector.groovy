package org.jlab.clas.timeline.timeline.htcc
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class htcc_nphe_ring_sector {

def data = new ConcurrentHashMap()

def processDirectory(dir, run) {
  // Initialize a variable to store the total NPHE across all channels
  def totalNphe = 0 
  def numChannels = 48
  // First we need to calculate average NPHE
  (1..6).each{sec->
    (1..4).each{ring->
      def h1 = dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side1") //left
      def h2 = dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side2") //right
      h1.add(h2)

      def meanNphe = h1.getMean() // Calculate the mean NPHE for this channel
      totalNphe += meanNphe // Add the mean NPHE to the total
    }
  }

  // Calculate the average NPHE across all channels
  def averageNphe = totalNphe / numChannels
  // Now we can add run, histogram, and correction factor to data
  (1..6).each{sec->
    (1..4).each{ring->
      def h1 = dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side1") //left
      def h2 = dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side2") //right
      h1.add(h2)

      def name = "sec $sec ring $ring"
      // Calculate the correction factor
      def correctionFactor = averageNphe > 0 ? h1.getMean() / averageNphe : 0
      // Store the histogram and correction factor
      data.computeIfAbsent(name, {[]}).add([run: run, h1: h1, correctionFactor: correctionFactor]) 
    }
  }
}

def close() {

  TDirectory out = new TDirectory()
  out.mkdir('/timelines')

  data.each{name,runs->
    def grtl = new GraphErrors(name)
    grtl.setTitle("Average HTCC Number of Photoelectrons per sector per ring")
    grtl.setTitleY("Average HTCC Number of Photoelectrons per sector per ring")
    grtl.setTitleX("run number")

    def grcorrection = new GraphErrors(name + "normalization factor")
    grcorrection.setTitle("Normalization factor (mean nphe per channel / average nphe across all channels) per sector per ring")
    grcorrection.setTitleY("Normalization factor per sector per ring")
    grcorrection.setTitleX("run number")

    runs.sort{it.run}.each{
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)
      out.addDataSet(it.h1)
      grtl.addPoint(it.run, it.h1.getMean(), 0, 0)
      // Add the correction factor to the plot
      grCorrection.addPoint(it.run, it.correctionFactor, 0, 0) 
    }
    out.cd('/timelines')
    out.addDataSet(grtl)
    out.addDataSet(grcorrection)
  }

  out.writeFile('htcc_nphe_sec_ring.hipo')
  out.writeFile('htcc_factor_sec_ring.hipo')
}
}
