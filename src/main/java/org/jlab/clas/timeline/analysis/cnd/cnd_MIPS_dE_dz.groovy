package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.CNDFitter

class cnd_MIPS_dE_dz {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []

  def h1
  def h2
  def h3

  def histlist = [h1, h2, h3].withIndex().collect{hist, lindex ->
    for(int sector=0;sector<24;sector++){
      for(int comp=0;comp<2;comp++){
        if (!hist) hist = dir.getObject(String.format("/cnd/CND_alignE_L%d_S%d_C%d",lindex+1,sector+1,comp+1))
        else hist.add(dir.getObject(String.format("/cnd/CND_alignE_L%d_S%d_C%d",lindex+1,sector+1,comp+1)))
      }
    }
    hist.setName("layer"+(lindex+1))
    hist.setTitle("dE/dz (MeV/cm)")
    funclist.add(CNDFitter.edepfit(hist))
    meanlist.add(funclist[lindex].getParameter(1))
    sigmalist.add(funclist[lindex].getParameter(2).abs())
    chi2list.add(funclist[lindex].getChiSquare())
    hist
  }

  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def write() {


  ['mean','sigma'].each{name ->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    ['layer1','layer2','layer3'].eachWithIndex{layer, lindex ->
      def grtl = new GraphErrors(layer+' '+name)
      grtl.setTitle("MIPS dE/dz, "+ name)
      grtl.setTitleY("MIPS dE/dz, " + name + " (MeV/cm)")
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
    out.writeFile('cnd_dEdz_'+name+'.hipo')
  }
}
}
