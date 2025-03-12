package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class FTOFFitter {

  static F1D timefit_p1(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = Math.min(h1.getRMS(),0.05); //ns
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
    //makefit(f1)
    return f1
  }


  static F1D timefit_p2(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -0.5, 0.5);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = 0.3;//h1.getRMS(); //ns
    double rangeMin = (hMean - 2.5*hRMS);
    double rangeMax = (hMean + 2.5*hRMS);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-1.5*hRMS,hMean+1.5*hRMS)
      MoreFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }


  static F1D tdcadcdifffit_p1a(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = 1.5//h1.getRMS(); //ns
    double rangeMin = (hMean - 1.5*hRMS);
    double rangeMax = (hMean + 1.5*hRMS);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-1.5*hRMS,hMean+1.5*hRMS)
      MoreFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }


  static F1D tdcadcdifffit_p1b(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = 1.2//h1.getRMS(); //ns
    double rangeMin = (hMean - 1.5*hRMS);
    double rangeMax = (hMean + 1.5*hRMS);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-1.5*hRMS,hMean+1.5*hRMS)
      MoreFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }


  static F1D tdcadcdifffit_p2(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = 1.9//h1.getRMS(); //ns
    double rangeMin = (hMean - 1.5*hRMS);
    double rangeMax = (hMean + 1.5*hRMS);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-1.5*hRMS,hMean+1.5*hRMS)
      MoreFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }


  static edepfit(H1F h1){
    def f1 = new F1D("fit:"+h1.getName(),"[amp]*landau(x,[mean],[sigma])+[p0]*exp(-[p1]*x)", 0, 50.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS(); //ns
    f1.setRange(7, hMean*2.0);
    f1.setParameter(0, hAmp);
    if (hAmp>0) f1.setParLimits(0, 0.5*hAmp, 1.5*hAmp);
    f1.setParameter(1, hMean);
    if (hMean>0) f1.setParLimits(1, 0.8*hMean, 1.2*hMean);//Changed from 5-30
    f1.setParameter(2, 0.3);//Changed from 2
    f1.setParLimits(2, 0.1, 1);//Changed from 0.5-10
    if (hAmp>0) f1.setParLimits(3,0, hAmp);
    f1.setParLimits(4,0,100);

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(7, hMean*2.0);
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
