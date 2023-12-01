package org.jlab.clas.timeline.timeline.epics

import java.text.SimpleDateFormat
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.MoreFitter


class epics_xy {
  def runlist = []

  def processDirectory(dir, run) {
    runlist.push(run)
  }

  static F1D gausFit(H1F h1) { // FIXME: not stable...

    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -10, 10);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = Math.min(h1.getRMS(),1.0);
    f1.setRange(hMean-2.0*hRMS, hMean+2.0*hRMS);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-2.0*hRMS,hMean+2.0*hRMS)
      MoreFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    return f1

  }

  static F1D justUseStats(H1F h1) { // no fit, just use mean and RMS from the histogram
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -10, 10);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getMean()
    double hRMS  = h1.getRMS()
    f1.setRange(hMean-2.0*hRMS, hMean+2.0*hRMS);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);
    return f1
  }


  def close() {

    def MYQ = new MYQuery()
    MYQ.querySettings['l'] = "${1000*runlist.size()}" // downsample the payload, since it's too big for a full run period
    def ts = MYQ.getRunTimeStamps(runlist)

    def epics = [:].withDefault{[:]}
    def dateFormatStr = 'yyyy-MM-dd HH:mm:ss.SSS'
    MYQ.query('IPM2H01.XPOS').each{ epics[new SimpleDateFormat(dateFormatStr).parse(it.d).getTime()].x = it.v }
    MYQ.query('IPM2H01.YPOS').each{ epics[new SimpleDateFormat(dateFormatStr).parse(it.d).getTime()].y = it.v }
    MYQ.query('IPM2H01').each{      epics[new SimpleDateFormat(dateFormatStr).parse(it.d).getTime()].i = it.v }

    println('dl finished')

    def data = epics.collect{kk,vv->[ts:kk]+vv} + ts.collectMany{[[run:it[0], ts: (((long)it[1])*1000)], [run:it[0], ts: (((long)it[2])*1000)]]}
    data.sort{it.ts}

    println('data sorted')

    def ts0, x0, y0, i0, r0=null
    def rundata = [:].withDefault{[]}
    data.each{
      if(it.run!=null) {
        r0 = r0 ? null : it.run
      } else if(r0) {
        rundata[r0].push([it.ts-ts0, x0, y0, i0])
      }
      ts0 = it.ts
      if(it.x!=null) x0 = it.x
      if(it.y!=null) y0 = it.y
      if(it.i!=null) i0 = it.i
    }

    def out = new TDirectory()

    def (grx, gry) = [new GraphErrors('2H01.xpos'), new GraphErrors('2H01.ypos')]

    rundata.each{run,vals->
      out.mkdir("/$run")
      out.cd("/$run")

      def (hx, hy) = (1..2).collect{ind->
        def entries = vals.collectMany{[it[ind]]*(it[0]*it[3]/1000 as int)}.sort()
        def nlen = entries.size()
        def (nq1,nq2,nq3) = [nlen/4 as int, nlen/2 as int, nlen*3/4 as int]
        def (q1,q2,q3) = [entries[nq1], entries[nq2], entries[nq3]]
        def (xm,dx) = [q2, q3-q1]
        def xy = 'xy'[ind-1]
        return new H1F("h$xy$run","$xy for run $run;$xy", 200, xm-3*dx, xm+3*dx)
      }

      vals.each{
        hx.fill(it[1], it[0]*it[3]/1000)
        hy.fill(it[2], it[0]*it[3]/1000)
      }

      def fx = justUseStats(hx)
      def fy = justUseStats(hy)
      grx.addPoint(run, fx.getParameter(1), 0,0)
      gry.addPoint(run, fy.getParameter(1), 0,0)

      [hx,hy].each{out.addDataSet(it)}
      // [fx,fy].each{out.addDataSet(it)}
      println("$run done")
    }

    out.mkdir("/timelines")
    out.cd("/timelines")
    out.addDataSet(grx)
    out.addDataSet(gry)
    out.writeFile("epics_xy.hipo")
  }
}



