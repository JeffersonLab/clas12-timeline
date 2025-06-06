package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ForwardFitter
import org.jlab.clas.timeline.util.RunDependentCut;

class forward_Tracking_PosVz {

def data = new ConcurrentHashMap()
def data_2nd_peak = new ConcurrentHashMap()

def is_RGD_LD2 = { run ->
  return (RunDependentCut.runIsInRange(run, 18305, 18313, true) ||
          RunDependentCut.runIsInRange(run, 18317, 18336, true) ||
          RunDependentCut.runIsInRange(run, 18419, 18439, true) ||
          RunDependentCut.runIsInRange(run, 18528, 18559, true) ||
          RunDependentCut.runIsInRange(run, 18644, 18656, true) ||
          RunDependentCut.runIsInRange(run, 18763, 18790, true) ||
          RunDependentCut.runIsInRange(run, 18851, 18873, true) ||
          RunDependentCut.runIsInRange(run, 18977, 19059, true))
}

def processRun(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []

  def funclist_2nd_peak = []
  def meanlist_2nd_peak = []
  def sigmalist_2nd_peak = []
  def chi2list_2nd_peak = []

  def histlist =   (0..<6).collect{
    def h1 = dir.getObject('/dc/H_dcp_vz_s'+(it+1))
    h1.setTitle("VZ, negatives")
    h1.setTitleX("VZ, negatives (cm)")

    def usefitBimodal = false
    def f1
    def dataset = RunDependentCut.findDataset(run)
    if (dataset == 'rgd') {
      if (is_RGD_LD2(run)) {
        f1 = ForwardFitter.fit(h1)
      }
      else {
        if (RunDependentCut.runIsOneOf(run, 18399)) {
          f1 = ForwardFitter.fitRGDbimodal(h1, -17.5, -13, 1, 1, -19, -10)
        }
        else {
          f1 = ForwardFitter.fitRGDbimodal(h1, -8, -3, 0.8, 0.8, -10, 0)
        }
        usefitBimodal = true
      }
    }
    else if (dataset == 'rgl') {
      f1 = ForwardFitter.fitRGL(h1)
    }
    else {
      f1 = ForwardFitter.fit(h1)
    }

    funclist.add(f1)
    meanlist.add(f1.getParameter(1))
    sigmalist.add(f1.getParameter(2).abs())
    chi2list.add(f1.getChiSquare())

    if (usefitBimodal) {
      funclist_2nd_peak.add(f1)
      meanlist_2nd_peak.add(f1.getParameter(4))
      sigmalist_2nd_peak.add(f1.getParameter(5).abs())
      chi2list_2nd_peak.add(f1.getChiSquare())
    }

    return h1
  }
  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
  if (funclist_2nd_peak.size() > 0) {
    data_2nd_peak[run] = [run:run, hlist:histlist, flist:funclist_2nd_peak, mean:meanlist_2nd_peak, sigma:sigmalist_2nd_peak, clist:chi2list_2nd_peak]
  }
}



def write() {

  TDirectory out = new TDirectory()
  out.mkdir('/timelines')
  (0..<6).each{ sec->
    def grtl = new GraphErrors('sec'+(sec+1))
    grtl.setTitle("VZ (peak value) for positives per sector")
    grtl.setTitleY("VZ (peak value) for positives per sector (cm)")
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

    def grtl_2nd_peak = new GraphErrors('sec'+(sec+1)+'_2')
    grtl_2nd_peak.setTitle("VZ (peak value) for electrons per sector")
    grtl_2nd_peak.setTitleY("VZ (peak value) for electrons per sector (cm)")
    grtl_2nd_peak.setTitleX("run number")
    data_2nd_peak.sort{it.key}.each{run,it->
      grtl_2nd_peak.addPoint(it.run, it.mean[sec], 0, 0)
    }

    out.cd('/timelines')
    out.addDataSet(grtl)

    if (data_2nd_peak.size() != 0) {
      out.addDataSet(grtl_2nd_peak)
    }
  }

  out.writeFile('forward_positive_VZ.hipo')
}
}
