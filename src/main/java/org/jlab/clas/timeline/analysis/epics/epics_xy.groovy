package org.jlab.clas.timeline.analysis

import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.MoreFitter

class epics_xy {

  def runlist = []

  def processRun(dir, run) {
    runlist.push(run)
  }


  static F1D gausFromStats(H1F h1) { // no fit, just use mean and RMS from the histogram
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -10, 10)
    double hAmp  = h1.getBinContent(h1.getMaximumBin())
    double hMean = h1.getMean()
    double hRMS  = h1.getRMS()
    f1.setRange(hMean-2.0*hRMS, hMean+2.0*hRMS)
    f1.setParameter(0, hAmp)
    f1.setParameter(1, hMean)
    f1.setParameter(2, hRMS)
    return f1
  }


  def write() {

    def pvNames = [
      x: 'IPM2H01.XPOS',
      y: 'IPM2H01.YPOS',
      i: 'IPM2H01',
    ]

    def myq = new MYQuery(runlist)
    myq.querySettings['l'] = "${1000*runlist.size()}" // downsample the payload, since it's too big for a full run period
    def epics_data = EpicsTools.queryEpics myq, pvNames

    def out = new TDirectory()

    def (grx, gry) = [new GraphErrors('2H01.xpos'), new GraphErrors('2H01.ypos')]

    epics_data.each{run,vals->
      out.mkdir("/$run")
      out.cd("/$run")

      // weight each reading by elapsed time * beam intensity
      def (hx, hy) = ['x','y'].collect{ax->
        def entries = vals.collectMany{[it[ax]]*(it.time*it.i/1000 as int)}.sort()
        EpicsTools.quantileHist("h$ax$run", "$ax for run $run;$ax", entries)
      }

      vals.each{
        hx.fill(it.x, it.time*it.i/1000)
        hy.fill(it.y, it.time*it.i/1000)
      }

      def fx = gausFromStats(hx)
      def fy = gausFromStats(hy)
      grx.addPoint(run, fx.getParameter(1), 0,0)
      gry.addPoint(run, fy.getParameter(1), 0,0)

      [hx,hy].each{out.addDataSet(it)}
      // [fx,fy].each{out.addDataSet(it)}
      println("$run done")
    }

    out.mkdir("/timelines")
    out.cd("/timelines")
    [x: grx, y: gry].each{ name, gr -> out.addDataSet(gr) }
    out.writeFile("epics_xy.hipo")
  }
}
