package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.DCFitter

class dc_t0_odd_sec_sl {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def t0fitlist = [[],[],[],[],[],[]]
  def t0list = [[],[],[],[],[],[]]
  def t0chi2list = [[],[],[],[],[],[]]

  def histlist =   (0..<6).collect{sec-> (0..<6).collect{sl ->
      def h1 = dir.getObject(String.format('/dc/DC_Time_odd_%d_%d',(sec+1),(sl+1)))
      h1.setName("sec"+(sec+1)+"sl"+(sl+1))

      def f1 = DCFitter.t0fit(h1, sl+1)
      t0fitlist[sec].add(f1)
      t0list[sec].add(f1.getParameter(2)-(2/f1.getParameter(1)))
      t0chi2list[sec].add(f1.getChiSquare())

      return h1
    }
  }

  data[run] = [run:run, hlist:histlist, t0list:t0fitlist, t0:t0list, t0chi2:t0chi2list]
}



def write() {

  def name = 't0'

  TDirectory out = new TDirectory()
  out.mkdir('/timelines')
  (0..<6).each{ sec->
    (0..<6).each{sl->
      def grtl = new GraphErrors('sec'+(sec+1)+' sl'+(sl+1))
      grtl.setTitle(name+" per sector per superlayer (for odd timestamps)")
      grtl.setTitleY(name+" per sector per superlayer (ns)")
      grtl.setTitleX("run number")

      data.sort{it.key}.each{run,it->
        if (sec==0 && sl==0) out.mkdir('/'+it.run)
        out.cd('/'+it.run)
        out.addDataSet(it.hlist[sec][sl])
        out.addDataSet(it[name+'list'][sec][sl])
        grtl.addPoint(it.run, it[name][sec][sl], 0, 0)
      }
      out.cd('/timelines')
      out.addDataSet(grtl)
    }
  }

  out.writeFile('dc_' + name + '_odd_sec_sl.hipo')
}
}
