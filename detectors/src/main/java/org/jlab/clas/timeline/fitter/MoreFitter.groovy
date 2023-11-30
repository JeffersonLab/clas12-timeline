package org.jlab.clas.timeline.fitter

import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import org.jlab.groot.fitter.DataFitter
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture

class MoreFitter {
  static void fit(F1D f1, H1F h1, String opts) {
    def out = System.out
    def err = System.err
    def fut = CompletableFuture.runAsync{DataFitter.fit(f1,h1,opts)}
    try {
      fut.get(10, TimeUnit.SECONDS)
    } catch(def ex) {
      System.setOut(out)
      System.setErr(err)
      println("FIT timeout")
      f1.setParameters(0,h1.getDataX(0),0)
    }
  }

  static F1D gausFit(H1F h1, String opts) {
    def f1 = new F1D('fit:'+h1.getName(), '[amp]*gaus(x,[mean],[sigma])', 0,1)

    def scale = Math.max(h1.getBinContent(h1.getMaximumBin())/500, 1)
    def data = (1..<h1.getDataSize(0)).collectMany{[h1.getDataX(it)]*(h1.getBinContent(it)/scale as int)}
    int nlen = data.size()
    if(nlen==0) {
      f1.setParameters(0,h1.getDataX(0),0)
      return f1
    }

    def (q1,q2,q3) = [data[nlen/4 as int], data[nlen/2 as int], data[nlen*3/4 as int]]
    def (x1,x2) = [1, h1.getDataSize(0)-1].collect{h1.getDataX(it)}
    def norms = [h1.getBinContent(h1.getMaximumBin())]
    def mus = [h1.getAxis().getBinCenter(h1.getMaximumBin()), h1.getMean(), q2, h1.getDataX(h1.getDataSize(0)/2 as int)]
    def sigs = [h1.getRMS()/2, (q3-q1)/1.35]

    def makefit = {amp,mu,sig->
      f1.setParameters(amp,mu,sig)
      def rng = [mu-2.5*sig, mu+2.5*sig]
      f1.setRange(*rng)
      DataFitter.fit(f1,h1,opts)
      (amp,mu,sig) = (0..<f1.getNPars()).collect{f1.getParameter(it)}
      if(mu<x1 || mu>x2 || sig>(x2-x1)) return null
      return [f1.getChiSquare(), [amp,mu,sig], rng]
    }

    def fits = [norms,mus,sigs].combinations().findResults{makefit(it)}
    fits += fits.findResults{makefit(it[1])}

    def best = fits.min{it[0]} ?: [0, [0,x1,x2-x1], [x1,x2]]
    f1.setParameters(*best[1])
    f1.setRange(*best[2])
    return f1
  }

  static F1D fitgaus(H1F h1) {
    def f1 = new F1D('fit:'+h1.getName(), '[amp]*gaus(x,[mean],[sigma])', 0,1)
    f1.setRange(h1.getDataX(0), h1.getDataX(h1.getDataSize(0)-1))
    def maxv = h1.getMax()
    def amps = [maxv*1.5, maxv, maxv/1.5]

    def vals = (0..<h1.getDataSize(0)).collect{[h1.getDataX(it)]*h1.getBinContent(it)}.flatten()
    def mns = [vals[vals.size().intdiv(2)], h1.getMean(), h1.getDataX(h1.getMaximumBin())]

    def rms = h1.getRMS()
    def sigs = [rms*1.5, rms, rms/1.5]

    def pars = [amps, mns, sigs].combinations().collect{amp,mn,sig->
      f1.setParameters(amp,mn,sig)
      fit(f1,h1,'Q')
      return [f1.getChiSquare(), *(0..2).collect{f1.getParameter(it)}]
    }.min{it[0]}
    f1.setParameters(*pars[1..-1])

    return f1
  }
}
