/**
*
* Fitter package for Forward
*
* Writer: Sangbaek Lee, Andrey Kim
*
**/
package org.jlab.clas.timeline.fitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class ForwardFitter{

  // bimodal fit for RG-D
  static F1D fitBimodal(H1F h1, float mean1, float mean2, float sigma1, float sigma2, float range_min, float range_max) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp1]*gaus(x,[mean1],[sigma1])+[amp2]*gaus(x,[mean2],[sigma2])+[p0]+[p1]*x", -9.0, 0.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS();
    f1.setRange(range_min, range_max);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, mean1);
    f1.setParameter(2, sigma1);
    f1.setParameter(3, hAmp);
    f1.setParameter(4, mean2);
    f1.setParameter(5, sigma2);
    f1.setParameter(6, 1);
    f1.setParameter(7, 1);
    MoreFitter.fit(f1,h1,"LQ");
    double hMean1 = f1.getParameter(1)
    double hRMS1 = f1.getParameter(2).abs()
    double hMean2 = f1.getParameter(3)
    double hRMS2 = f1.getParameter(4).abs()

    def makefit = {func->
      hMean1 = func.getParameter(1)
      hRMS1 = func.getParameter(2).abs()
      hMean2 = func.getParameter(4)
      hRMS2 = func.getParameter(5).abs()
      func.setRange(range_min, range_max)
      MoreFitter.fit(func,h1,"Q")

      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)

    return f1
  }

  /// single peak fit
  /// @param h1 the histogram
  /// @param args.amp override dynamic initial value of `amp` parameter
  /// @param args.mean override dynamic initial value of `mean` parameter
  /// @param args.rms override dynamic initial value of `rms` parameter
  static F1D fit(Map args=[:], H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", h1.getDataX(0), h1.getDataX(h1.getDataSize(0)-1));
    double hAmp  = args.amp  ?: h1.getBinContent(h1.getMaximumBin());
    double hMean = args.mean ?: h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = args.rms  ?: h1.getRMS(); //ns

    def findRange = { start, frac, sgn ->
      def peak = h1.getBinContent(start)
      def bn = start
      while(bn >= 0 && bn <= h1.getDataSize(0)-1) {
        bn += sgn
        if(h1.getBinContent(bn) < frac * peak) {
          return h1.getAxis().getBinCenter(bn)
        }
      }
      System.out.println("failed to find range; setting to default")
      return hMean + sgn * 3 * hRMS
    }
    double rangeMin = findRange(h1.getMaximumBin(), 0.7, -1)
    double rangeMax = findRange(h1.getMaximumBin(), 0.7, 1)
    // double rangeMin = (hMean - (3*hRMS));
    // double rangeMax = (hMean + (3*hRMS));

    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);
    f1.setParLimits(2, 0.5, rangeMax-rangeMin);
    System.out.println("DEBUG: initial params: amp=$hAmp  mean=$hMean  sigma=$hRMS  range: $rangeMin to $rangeMax");
    MoreFitter.fit(f1,h1,"LQ");

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-2.5*hRMS,hMean+2.5*hRMS)
      MoreFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    System.out.println("DEBUG: result params: amp=${f1.getParameter(0)}  mean=${f1.getParameter(1)}  sigma=${f1.getParameter(2)}");
    //makefit(f1)

    return f1
  }
}
