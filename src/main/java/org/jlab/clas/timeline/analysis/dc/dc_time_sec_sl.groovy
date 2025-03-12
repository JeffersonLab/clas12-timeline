package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.DCFitter

class dc_time_sec_sl {

def data = new ConcurrentHashMap()

def processRun(dir, run) {
  def funclist = [t0:[[],[],[],[],[],[]], tmax:[[],[],[],[],[],[]]] // actually it's dict! not a list!
  def fomlist = [t0:[[],[],[],[],[],[]], tmax:[[],[],[],[],[],[]]]
  def chi2list = [t0:[[],[],[],[],[],[]], tmax:[[],[],[],[],[],[]]]

  def histlist =   (0..<6).collect{sec-> (0..<6).collect{sl ->
      def h1 = dir.getObject(String.format('/dc/DC_Time_%d_%d',(sec+1),(sl+1)))
      h1.setName("sec"+(sec+1)+"sl"+(sl+1))

      def f1 = DCFitter.t0fit(h1, sl+1)
      funclist.t0[sec].add(f1)
      fomlist.t0[sec].add(f1.getParameter(2)-(2/f1.getParameter(1)))
      chi2list.t0[sec].add(f1.getChiSquare())

      def f2 = DCFitter.tmaxfit(h1, sl+1)
      funclist.tmax[sec].add(f2)
      fomlist.tmax[sec].add(f2.getParameter(2)-(2/f2.getParameter(1)))
      chi2list.tmax[sec].add(f2.getChiSquare())

      return h1
    }
  }

  data[run] = [run:run, hlist:[t0:histlist, tmax:histlist], flist:funclist, fomlist:fomlist, chi2list:chi2list]
}



def write() {

  ['t0', 'tmax'].each{ name ->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    (0..<6).each{ sec->
      (0..<6).each{sl->
        def grtl = new GraphErrors('sec'+(sec+1)+' sl'+(sl+1))
        grtl.setTitle(name+" per sector per superlayer")
        grtl.setTitleY(name+" per sector per superlayer (ns)")
        grtl.setTitleX("run number")

        data.sort{it.key}.each{run,it->
          if (sec==0 && sl==0) out.mkdir('/'+it.run)
          out.cd('/'+it.run)
          def h1 = it.hlist[name][sec][sl]
          def f1 = it.flist[name][sec][sl]
          h1.setFunction(f1)
          out.addDataSet(h1)
          out.addDataSet(f1)
          grtl.addPoint(it.run, it.fomlist[name][sec][sl], 0, 0)
        }
        out.cd('/timelines')
        out.addDataSet(grtl)
      }
    }

    out.writeFile('dc_' + name + '_sec_sl.hipo')
  }
}
}
