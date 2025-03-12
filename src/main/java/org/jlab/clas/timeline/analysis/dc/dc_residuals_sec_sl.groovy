package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.DCFitter

class dc_residuals_sec_sl {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = [[],[],[],[],[],[]]
  def meanlist = [[],[],[],[],[],[]]
  def sigmalist = [[],[],[],[],[],[]]
  def chi2list = [[],[],[],[],[],[]]
  def histlist =   (0..<6).collect{sec-> (0..<6).collect{sl ->
      def h1 = dir.getObject(String.format('/dc/DC_residuals_trkDoca_%d_%d',(sec+1),(sl+1))).projectionY()
      h1.setName("sec"+(sec+1)+"sl"+(sl+1))
      h1.setTitle("DC residuals per sector per superlayer (with basic DC4gui cuts)")
      h1.setTitleX("DC residuals per sector per superlayer (with basic DC4gui cuts) (cm)")
      def f1 = DCFitter.fit(h1)
      funclist[sec].add(f1)
      meanlist[sec].add(f1.getParameter(1))
      sigmalist[sec].add(f1.getParameter(2).abs())
      chi2list[sec].add(f1.getChiSquare())
      return h1
    }
  }

  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def write() {

  ['mean', 'sigma'].each{ name ->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    (0..<6).each{ sec->
      (0..<6).each{sl->
        def grtl = new GraphErrors('sec'+(sec+1)+' sl'+(sl+1))
        grtl.setTitle("DC residuals (" + name + ") per sector per superlayer (with basic DC4gui cuts)")
        grtl.setTitleY("DC residuals (" + name + ") per sector per superlayer (with basic DC4gui cuts) (cm)")
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

    out.writeFile('dc_residuals_sec_sl_'+name+'.hipo')
  }
}
}
