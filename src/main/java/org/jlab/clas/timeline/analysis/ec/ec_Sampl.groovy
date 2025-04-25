package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ECFitter
import org.jlab.clas.timeline.util.QA

class ec_Sampl {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []
  def histlist =   (0..<6).collect{
    def h1 = dir.getObject('/elec/H_trig_ECALsampl_S'+(it+1)).projectionY()
    h1.setName("sec"+(it+1))
    h1.setTitle("ECAL Sampling Fraction")
    h1.setTitleX("ECAL Sampling Fraction")
    def f1 = ECFitter.samplfit(h1)
    funclist.add(f1)
    meanlist.add(f1.getParameter(1))
    sigmalist.add(f1.getParameter(2).abs())
    chi2list.add(f1.getChiSquare())
    return h1
  }
  data[run] = [run:run, hlist:histlist, flist:funclist, Sampling:meanlist, sigma:sigmalist, clist:chi2list]
}



def write() {


  TDirectory out = new TDirectory()
  out.mkdir('/timelines')
  def grtlList = (0..<6).collect{ sec->
    def grtl = new GraphErrors('sec'+(sec+1))
    grtl.setTitle("ECAL Sampling Fraction per sector")
    grtl.setTitleY("ECAL Sampling Fraction per sector")
    grtl.setTitleX("run number")

    data.sort{it.key}.each{run,it->
      if (sec==0){
        out.mkdir('/'+it.run)
      }
      out.cd('/'+it.run)
      out.addDataSet(it.hlist[sec])
      out.addDataSet(it.flist[sec])
      grtl.addPoint(it.run, it.Sampling[sec], 0, 0)
    }
    grtl
  }
  out.cd('/timelines')
  QA.cutGraphs(grtlList, lb: 0.23, ub: 0.26, out: out)
  out.writeFile('ec_Sampling_QA.hipo')
}
}
