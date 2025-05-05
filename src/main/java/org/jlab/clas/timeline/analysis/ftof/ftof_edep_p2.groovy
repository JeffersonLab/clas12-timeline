package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTOFFitter
import org.jlab.clas.timeline.util.QA

class ftof_edep_p2 {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []
  def histlist =   (0..<6).collect{
    def h1 = dir.getObject('/tof/p2_edep_S'+(it+1))
    def f1 = FTOFFitter.edepfit(h1)

    funclist.add(f1)
    meanlist.add(f1.getParameter(1))
    sigmalist.add(f1.getParameter(2).abs())
    chi2list.add(f1.getChiSquare())
    return h1
  }
  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def write() {

  TDirectory out = new TDirectory()
  out.mkdir('/timelines')
  def grtlList = (0..<6).collect{ sec->
    def grtl = new GraphErrors('sec'+(sec+1))
    grtl.setTitle("p2 Path-length Corrected Edep for p, pi-, and pi+")
    grtl.setTitleY("p2 Path-length Corrected Edep for p, pi-, and pi+ (MeV)")
    grtl.setTitleX("run number")

    data.sort{it.key}.each{run,it->
      if (sec==0){
        out.mkdir('/'+it.run)
      }
      out.cd('/'+it.run)
      out.addDataSet(it.hlist[sec])
      out.addDataSet(it.flist[sec])
      grtl.addPoint(it.run, it.mean[sec], 0, 0)
    }
    grtl
  }
  out.cd('/timelines')
  QA.cutGraphs(*grtlList, lb: 8.75, ub: 10.75, out: out)
  out.writeFile('ftof_edep_p2_QA.hipo')
}
}
