package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class htcc_nphe_ring_sector {

  def data = new ConcurrentHashMap()

  def processRun(dir, run) {
    // histogram with the nphe for all channels in a single run
    def h_npheAll = dir.getObject('/HTCC/npheAll')
    def averageNphe = h_npheAll.getMean() 
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

  def write() {
    ['npheMean', 'normFactor'].each { plotType ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.each { name, runs ->
        GraphErrors graph

        if (plotType == 'npheMean') {
          graph = new GraphErrors(name)
          graph.setTitle("Average HTCC Number of Photoelectrons per sector per ring")
          graph.setTitleY("Average HTCC Number of Photoelectrons per sector per ring")
          graph.setTitleX("run number")
        } else if (plotType == 'normFactor') {
          graph = new GraphErrors(name + " normalization factor")
          graph.setTitle("Normalization factor (mean nphe per channel / average nphe across all channels) per sector per ring")
          graph.setTitleY("Normalization factor per sector per ring")
          graph.setTitleX("run number")
        }

        runs.sort { it.run }.each {
          out.mkdir('/' + it.run)
          out.cd('/' + it.run)
          out.addDataSet(it.h1)

          if (plotType == 'npheMean') {
            graph.addPoint(it.run, it.h1.getMean(), 0, 0)
          } else if (plotType == 'normFactor') {
            graph.addPoint(it.run, it.correctionFactor, 0, 0)
          }
        }

        out.cd('/timelines')
        out.addDataSet(graph)
      }
      out.writeFile("htcc_nphe_sec_ring_${plotType}.hipo")
    }
  }

}
