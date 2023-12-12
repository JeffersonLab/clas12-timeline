package org.jlab.clas.timeline.timeline.htcc
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class htcc_nphe_sector {
  def data = new ConcurrentHashMap()
  
  def processDirectory(dir, run) {
    // histogram with the nphe for all channels in a single run
    def h_npheAll = dir.getObject('/HTCC/npheAll')
    def averageNphe = h_npheAll.getMean() 

    (1..6).each{sec->
      def hlist = [(1..4), [1,2]].combinations().collect{ring,side -> dir.getObject("/HTCC/H_HTCC_nphe_s${sec}_r${ring}_side$side")}
      def h1 = hlist.head()
      hlist.tail().each{h1.add(it)}

      h1.setName("sec ${sec}")
      h1.setTitle("HTCC Number of Photoelectrons")
      h1.setTitleX("HTCC Number of Photoelectrons")

      def correctionFactor = averageNphe > 0 ? h1.getMean() / averageNphe : 0
      // data.computeIfAbsent(sec, {[]}).add([run:run, h1:h1, correctionFactor:correctionFactor])
      data.computeIfAbsent(sec, {[]}).add([run:run, h1:h1, mean:h1.getMean(), correctionFactor:correctionFactor])
    }
  }

  def close() {
    ['npheMean', 'normFactor'].each { plotType ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')

      data.each { sec, runs ->
        GraphErrors graph

        if (plotType == 'npheMean') {
          graph = new GraphErrors("sec$sec")
          graph.setTitle("Average HTCC Number of Photoelectrons per sector")
          graph.setTitleY("Average HTCC Number of Photoelectrons per sector")
          graph.setTitleX("run number")
        } else if (plotType == 'normFactor') {
          graph = new GraphErrors("sec$sec")
          graph.setTitle("Normalization factor (mean nphe per channel / average nphe across all channels) per sector")
          graph.setTitleY("Normalization factor per sector")
          graph.setTitleX("run number")
        }

        runs.sort { it.run }.each {
          out.mkdir('/' + it.run)
          out.cd('/' + it.run)
          out.addDataSet(it.h1)

          if (plotType == 'npheMean') {
            graph.addPoint(it.run, it.mean, 0, 0)
          } else if (plotType == 'normFactor') {
            graph.addPoint(it.run, it.correctionFactor, 0, 0)
          }
        }

        out.cd('/timelines')
        out.addDataSet(graph)
      }
      out.writeFile("htcc_nphe_sec_${plotType}.hipo")
    }
  }

}
