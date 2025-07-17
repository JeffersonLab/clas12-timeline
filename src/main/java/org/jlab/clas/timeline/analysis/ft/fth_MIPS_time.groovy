package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTFitter
import org.jlab.clas.timeline.util.QA

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
      if(layer == 'layer1') // mean_ub and mean_lb are the same for the 2 layers, so color is black, whereas sigma differs, so color is red/blue
        QA.cutGraphsMeanSigma(name, grtl, mean_lb: -0.5, mean_ub: 0.5, sigma_ub: 1.4, mean_lb_color: 'black', mean_ub_color: 'black', sigma_ub_color: 'red', out: out)
      else if(layer == 'layer2')
        QA.cutGraphsMeanSigma(name, grtl, mean_lb: -0.5, mean_ub: 0.5, sigma_ub: 1.2, mean_lb_color: 'black', mean_ub_color: 'black', sigma_ub_color: 'blue', out: out)
    }
    out.writeFile("fth_MIPS_time_${name}_QA.hipo")
  }
}
}
