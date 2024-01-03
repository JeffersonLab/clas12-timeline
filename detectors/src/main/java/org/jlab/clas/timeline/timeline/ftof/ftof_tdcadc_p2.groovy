package org.jlab.clas.timeline.timeline.ftof
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.FTOFFitter
import org.jlab.clas.timeline.util.HistoUtil

class ftof_tdcadc_p2 {

def data = new ConcurrentHashMap()

def processDirectory(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []
  def histlist =   (0..<6).collect{
    def h1 = HistoUtil.zoomHisto(dir.getObject('/tof/p2_tdcadc_dt_S'+(it+1)))
    def f1 = FTOFFitter.tdcadcdifffit_p2(h1)

    funclist.add(f1)
    meanlist.add(f1.getParameter(1))
    sigmalist.add(f1.getParameter(2).abs())
    chi2list.add(f1.getChiSquare())
    return h1
  }
  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def close() {

  ['mean', 'sigma'].each{ name ->
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    (0..<6).each{ sec->
      def grtl = new GraphErrors('sec'+(sec+1))
    grtl.setTitle("p2 t_tdc-t_fadc (" + name +")")
    grtl.setTitleY("p2 t_tdc-t_fadc (" + name +") (ns)")
    grtl.setTitleX("run number")

      data.sort{it.key}.each{run,it->
        if (sec==0){
          out.mkdir('/'+it.run)
        }
        out.cd('/'+it.run)
        out.addDataSet(it.hlist[sec])
        out.addDataSet(it.flist[sec])
        grtl.addPoint(it.run, it[name][sec], 0, 0)
      }
      out.cd('/timelines')
      out.addDataSet(grtl)
    }

    out.writeFile('ftof_tdcadc_time_p2_' + name + '.hipo')
  }
}
}
