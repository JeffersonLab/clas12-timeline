package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTFitter
import org.jlab.clas.timeline.util.QA

class fth_MIPS_energy {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []

  def h1
  def h2

  def histlist = [h1, h2].withIndex().collect{hist, it ->
    hist = dir.getObject('/ft/hi_hodo_ematch_l'+(it+1))
    funclist.add(FTFitter.fthedepfit(hist, it))
    meanlist.add(funclist[it].getParameter(1))
    sigmalist.add(funclist[it].getParameter(2).abs())
    chi2list.add(funclist[it].getChiSquare())
    hist
  }

  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def write() {


  TDirectory out = new TDirectory()
  out.mkdir('/timelines')
  ['layer1','layer2'].eachWithIndex{layer, lindex ->
    def grtl = new GraphErrors(layer)
    grtl.setTitle("FTH MIPS energy per layer (mean value)")
    grtl.setTitleY("FTH MIPS energy per layer (mean value) (MeV)")
    grtl.setTitleX("run number")

    data.sort{it.key}.each{run,it->
      out.mkdir('/'+it.run)
      out.cd('/'+it.run)

      out.addDataSet(it.hlist[lindex])
      out.addDataSet(it.flist[lindex])
      grtl.addPoint(it.run, it.mean[lindex], 0, 0)
    }
    out.cd('/timelines')
    if(layer == 'layer1')
      QA.cutGraphs(grtl, lb: 0.9, ub: 1.9, lb_color: 'red', ub_color: 'red', out: out)
    else if(layer == 'layer2')
      QA.cutGraphs(grtl, lb: 2.3, ub: 3.3, lb_color: 'blue', ub_color: 'blue', out: out)
  }
  out.writeFile('fth_MIPS_energy_QA.hipo')
}
}
