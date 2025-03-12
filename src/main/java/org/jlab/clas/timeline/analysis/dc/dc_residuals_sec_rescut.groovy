package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.DCFitter

class dc_residuals_sec_rescut {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []
  def histlist =   (0..<6).collect{sec->
    def h1 = dir.getObject(String.format('/dc/DC_residuals_trkDoca_rescut_%d_%d',(sec+1),1)).projectionY()
    h1.setName("sec"+(sec+1))
    h1.setTitle("DC residuals per sector with fitresidual cut")
    h1.setTitleX("DC residuals per sector with fitresidual cut (cm)")
    (1..<6).each{sl ->
      def h2 = dir.getObject(String.format('/dc/DC_residuals_trkDoca_rescut_%d_%d',(sec+1),(sl+1))).projectionY()
      h1.add(h2)
    }
    def f1 = DCFitter.doublegausfit(h1)
    funclist.add(f1)
    meanlist.add(f1.getParameter(1))
    
    //ensure smaller sigma is used for plotting of timelines
    if (f1.getParameter(2).abs() <= f1.getParameter(4).abs()) {
    	sigmalist.add(f1.getParameter(2).abs()) 
    }
    else {
    	sigmalist.add(f1.getParameter(4).abs()) 
    }  
    chi2list.add(f1.getChiSquare())
    return h1
  }
  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def write() {

  ['mean', 'sigma'].each{ name ->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    (0..<6).each{ sec->
        def grtl = new GraphErrors('sec'+(sec+1))
        grtl.setTitle("DC residuals (" + name + ") per sector with fitresidual cut")
        grtl.setTitleY("DC residuals (" + name + ") per sector with fitresidual cut (cm)")
        grtl.setTitleX("run number")

        data.sort{it.key}.each{run,it->
          if (sec==0) out.mkdir('/'+it.run)
          out.cd('/'+it.run)
          out.addDataSet(it.hlist[sec])
          out.addDataSet(it.flist[sec])
          grtl.addPoint(it.run, it[name][sec], 0, 0)
        }
        out.cd('/timelines')
        out.addDataSet(grtl)
    }

    out.writeFile('dc_residuals_sec_rescut_'+name+'.hipo')
  }
}
}
