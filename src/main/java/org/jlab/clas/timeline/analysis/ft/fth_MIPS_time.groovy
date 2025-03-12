package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTFitter

class fth_MIPS_time {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []

  def h1
  def h2

  def histlist = [h1, h2].withIndex().collect{hist, it ->
    hist = dir.getObject('/ft/hi_hodo_tmatch_l'+(it+1))
    funclist.add(FTFitter.fthtimefit(hist))
    meanlist.add(funclist[it].getParameter(1))
    sigmalist.add(funclist[it].getParameter(2).abs())
    chi2list.add(funclist[it].getChiSquare())
    hist
  }

  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def write() {

  ['mean', 'sigma'].each{name->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    ['layer1','layer2'].eachWithIndex{layer, lindex ->
      def grtl = new GraphErrors(layer)
      grtl.setTitle("FTH MIPS time per layer (" + name + ")")
      grtl.setTitleY("FTH MIPS time per layer (" + name + ") (ns)")
      grtl.setTitleX("run number")

      data.sort{it.key}.each{run,it->
        out.mkdir('/'+it.run)
        out.cd('/'+it.run)

        out.addDataSet(it.hlist[lindex])
        out.addDataSet(it.flist[lindex])
        grtl.addPoint(it.run, it[name][lindex], 0, 0)
      }
      out.cd('/timelines')
      out.addDataSet(grtl)
    }
    out.writeFile('fth_MIPS_time_' + name + '.hipo')
  }
}
}
