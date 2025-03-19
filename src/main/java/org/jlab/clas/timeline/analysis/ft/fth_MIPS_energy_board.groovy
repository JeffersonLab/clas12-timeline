package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTFitter

class fth_MIPS_energy_board {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = [[],[]]
  def meanlist = [[],[]]
  def sigmalist = [[],[]]
  def chi2list = [[],[]]

  def histlist = [[], []].withIndex().collect{list, layer ->
    (0..<15).each{board ->
      def hist = dir.getObject('/ft/hi_hodo_ematch_l'+(layer+1)+'_b'+(board+1))
      funclist[layer].add(FTFitter.fthedepfit(hist, layer))
      meanlist[layer].add(funclist[layer][board].getParameter(1))
      sigmalist[layer].add(funclist[layer][board].getParameter(2).abs())
      chi2list[layer].add(funclist[layer][board].getChiSquare())
      list.add(hist)
    }
    return list
  }

  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def write() {


  TDirectory out = new TDirectory()
  out.mkdir('/timelines')
  ['layer1','layer2'].eachWithIndex{layer, lindex ->
    (1..15).each{board->
      def grtl = new GraphErrors(layer+'board'+board)
      grtl.setTitle("FTH MIPS energy per layer per board (mean value)")
      grtl.setTitleY("FTH MIPS energy per layer per board (mean value) (MeV)")
      grtl.setTitleX("run number")

      data.sort{it.key}.each{run,it->
        out.mkdir('/'+it.run)
        out.cd('/'+it.run)
        grtl.addPoint(it.run, it.mean[lindex][board-1], 0, 0)
        out.addDataSet(it.hlist[lindex][board-1])
        out.addDataSet(it.flist[lindex][board-1])
      }
      out.cd('/timelines')
      out.addDataSet(grtl)
    }
  }

  out.writeFile('fth_MIPS_energy_board.hipo')
}
}
