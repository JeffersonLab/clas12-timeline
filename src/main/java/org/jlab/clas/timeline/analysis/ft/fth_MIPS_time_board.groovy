package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTFitter

class fth_MIPS_time_board {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = [[],[]]
  def meanlist = [[],[]]
  def sigmalist = [[],[]]
  def chi2list = [[],[]]

  def histlist = [[], []].withIndex().collect{list, layer ->
    (0..<15).each{board ->
      def hist = dir.getObject('/ft/hi_hodo_tmatch_l'+(layer+1)+'_b'+(board+1))
      funclist[layer].add(FTFitter.fthtimefit(hist))
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

  ['mean', 'sigma'].each{name->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    ['layer1','layer2'].eachWithIndex{layer, lindex ->
      (1..15).each{board->
        def grtl = new GraphErrors(layer+'board'+board)
        grtl.setTitle("FTH MIPS time per layer (" + name + ")")
        grtl.setTitleY("FTH MIPS time per layer (" + name + ") (ns)")
        grtl.setTitleX("run number")

        data.sort{it.key}.each{run,it->
          out.mkdir('/'+it.run)
          out.cd('/'+it.run)
          grtl.addPoint(it.run, it[name][lindex][board-1], 0, 0)
          out.addDataSet(it.hlist[lindex][board-1])
          out.addDataSet(it.flist[lindex][board-1])
        }
        out.cd('/timelines')
        out.addDataSet(grtl)
      }
    }

    out.writeFile('fth_MIPS_time_board_' + name + '.hipo')
  }
}
}
