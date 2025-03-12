package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.H1F
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.DCFitter

class dc_residuals_sec_sl_rescut {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = [[],[],[],[],[],[]]
  def meanlist = [[],[],[],[],[],[]]
  def sigmalist = [[],[],[],[],[],[]]
  def maxlist = [[],[],[],[],[],[]]
  def chi2list = [[],[],[],[],[],[]]
  def histlist =   (0..<6).collect{sec-> (0..<6).collect{sl ->
      def h1 = dir.getObject(String.format('/dc/DC_residuals_trkDoca_rescut_%d_%d',(sec+1),(sl+1))).projectionY()
      h1.setName("sec"+(sec+1)+"sl"+(sl+1))
      h1.setTitle("DC residuals per sector per superlayer with fitresidual cut")
      h1.setTitleX("DC residuals per sector per superlayer with fitresidual cut (cm)")
      def f1 = DCFitter.doublegausfit(h1)
      funclist[sec].add(f1)
      meanlist[sec].add(f1.getParameter(1))
      //smaller sigma is put into timelines
      if (f1.getParameter(2).abs() <= f1.getParameter(4).abs()) {
    	sigmalist[sec].add(f1.getParameter(2).abs()) 
      }
      else {
    	sigmalist[sec].add(f1.getParameter(4).abs()) 
      }  
   	  maxlist[sec].add(h1.getDataX(h1.getMaximumBin()))
      chi2list[sec].add(f1.getChiSquare())
      return h1
    }
  }

  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, max:maxlist, clist:chi2list]
}



def write() {

  ['mean', 'sigma', 'max'].each{ name ->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    (0..<6).each{ sec->
      (0..<6).each{sl->
        def grtl = new GraphErrors('sec'+(sec+1)+' sl'+(sl+1))
        grtl.setTitle("DC residuals (" + name + ") per sector per superlayer with fitresidual cut")
        grtl.setTitleY("DC residuals (" + name + ") per sector per superlayer with fitresidual cut (cm)")
        grtl.setTitleX("run number")

        data.sort{it.key}.each{run,it->
          if (sec==0 && sl==0) out.mkdir('/'+it.run)
          out.cd('/'+it.run)
          out.addDataSet(it.hlist[sec][sl])
          out.addDataSet(it.flist[sec][sl])
          grtl.addPoint(it.run, it[name][sec][sl], 0, 0)
        }
        out.cd('/timelines')
        out.addDataSet(grtl)
      }
    }

    out.writeFile('dc_residuals_sec_sl_rescut_'+name+'.hipo')
  }
}
}
