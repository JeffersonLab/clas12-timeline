package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class FTFitter {
  static F1D pi0fit(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])+[p0]", -1.0, 1.0)
    def hAmp  = h1.getBinContent(h1.getMaximumBin());
    def hMean=h1.getAxis().getBinCenter(h1.getMaximumBin())
    double hRMS  = 4.5

    f1.setParameter(0, hAmp)
    f1.setParameter(1, hMean)
    // f1.setParLimits(1, hMean-2.5*hRMS, hMean+2.5*hRMS)
    f1.setParameter(2, hRMS)
    // f1.setParLimits(2, 0, 20)
    f1.setParameter(3, 0)

    def makefit = {func->
      hRMS = Math.min(func.getParameter(2).abs(),6)
      func.setRange(hMean-3*hRMS, hMean+3*hRMS)
      DataFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }

  static F1D ftctimefit(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS(); //ns
    double rangeMin = (hMean - (3*hRMS));
    double rangeMax = (hMean + (3*hRMS));
    double pm = hRMS*3;
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    if (hAmp>0) f1.setParLimits(0, hAmp*0.8, hAmp*1.2);
    f1.setParameter(1, hMean);
    if (pm>0) f1.setParLimits(1, hMean-pm, hMean+(pm));
    f1.setParameter(2, 0.2);
    // f1.setParLimits(2, 0.1*hRMS, 0.8*hRMS);

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-2*hRMS,hMean+2*hRMS)
      DataFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }

  static F1D fthtimefit(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS(); //ns
    double rangeMin = (hMean - (3*hRMS));
    double rangeMax = (hMean + (3*hRMS));
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    if (hAmp>0) f1.setParLimits(0, hAmp*0.9, hAmp*1.1);
    f1.setParameter(1, hMean);
    if (hRMS>0) f1.setParLimits(1, hMean-2*hRMS, hMean+2*hRMS);
    f1.setParameter(2, 1.2);


    def makefit = {func->
      // hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-2.5*hRMS,hMean+2.5*hRMS)
      DataFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }

  static F1D fthedepfit(H1F h1, int layer) {
    
    def f1 = new F1D("fit:"+h1.getName(),"[amp]*landau(x,[mean],[sigma])+[p0]+[p1]*x", 0.5*(layer+1), 10.0);

    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS(); //ns
    f1.setRange(f1.getRange().getMin(), hMean*2.0);
    f1.setParameter(0, hAmp);
    if (hAmp>0) f1.setParLimits(0, 0.5*hAmp, 1.5*hAmp);
    f1.setParameter(1, hMean);
    if (hMean>0) f1.setParLimits(1, 0.8*hMean, 1.2*hMean);//Changed from 5-30
    f1.setParameter(2, 0.3);//Changed from 2
    f1.setParLimits(2, 0.1, 1);//Changed from 0.5-10
    f1.setParameter(3, 0.2*hAmp);
    f1.setParameter(4, -0.3);//Changed from -0.2

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-2.5*hRMS,hMean+2.5*hRMS)
      DataFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }

}
