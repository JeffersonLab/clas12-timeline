package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture


class RICHFitter {
  static F1D timefit(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])+[p0]", -1.0, 1.0)
    def hAmp  = h1.getBinContent(h1.getMaximumBin());
    def hMean = h1.getAxis().getBinCenter(h1.getMaximumBin())
    def hRMS = Math.min(h1.getRMS(),0.44)
    def h1range = h1.getDataX(h1.getDataSize(0)-1) - h1.getDataX(0)

    f1.setParameter(0, hAmp)
    f1.setParameter(1, hMean)
    f1.setParameter(2, hRMS)
    f1.setParLimits(2, 0, h1range) // don't let sigma be < 0
    f1.setParameter(3, 0)

    def fitTimedOut = false

    def makefits = {func->
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-3.0*hRMS, hMean+3.0*hRMS)

      // try the fit, but don't try for too long...
      if(!fitTimedOut) {
        def out = System.out
        def err = System.err
        def fut = CompletableFuture.runAsync{DataFitter.fit(func,h1,"Q")}
        try {
          fut.get(10, TimeUnit.SECONDS) // 10 second timeout
        } catch(def ex) {
          System.setOut(out)
          System.setErr(err)
          err.println("FIT timeout")
          fitTimedOut = true
        }
      }

      // give up, if timed out
      if(fitTimedOut) {
        func.getNPars().times{func.setParameter(it, 0.0)}
      }

      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }
    def fits1 = (0..10).collect{makefits(f1)}

    def f2 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x",-0.2,0.2);
    f2.setParLimits(2, 0, h1range) // don't let sigma be < 0

    fits1.sort()[0][1].eachWithIndex{par,ipar->
      f2.setParameter(ipar, par)
    }

    def fits2 = (0..10).collect{makefits(f2)}

    def bestfit = fits2.sort()[0]
    f2.setParameters(*bestfit[1])

    // if the fit timed out, set the mean and sigma to an obviously bad value
    if(fitTimedOut) {
      f2.setParameter(1, 10 * h1range)
      f2.setParameter(2, 10 * h1range)
    }

    return f2
  }
}
