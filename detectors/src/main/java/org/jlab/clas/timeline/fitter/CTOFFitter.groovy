package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class CTOFFitter {
  static F1D timefit(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0)
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin())
    double hRMS  = Math.min(h1.getRMS(), 0.2)
    double rangeMin = (hMean - 2.5*hRMS);
    double rangeMax = (hMean + 2.5*hRMS);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);

    def dmean=0.001
    def dsigma=0.005

    int nfits = 0
    def makefit = {func->
      def dm = hMean - func.getParameter(1)
      def dr = hRMS - func.getParameter(2)
      if ( nfits==0 || dm.abs()>dmean || dr.abs()>dsigma ) {
        hMean = func.getParameter(1)
        hRMS = func.getParameter(2).abs()
        func.setRange(hMean-2*hRMS,hMean+2*hRMS)
        DataFitter.fit(func,h1,"Q")
        nfits++
        return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
      }
    }

    def fits1 = (0..20).findResults{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    return f1
  }

  static F1D tdcadcdifffit(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0)
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin())
    double hRMS  = h1.getRMS();
    double factor1 = 3.0
    double factor2 = 1.57
    double rangeMin = (hMean - factor1*0.5);
    double rangeMax = (hMean + factor2*0.5);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, 1);

    def dmean=0.001
    def dsigma=0.005
    int nfits = 0
    def makefit = {func->
      def dm = hMean - func.getParameter(1)
      def dr = hRMS - func.getParameter(2)
      if ( nfits==0 || dm.abs()>dmean || dr.abs()>dsigma ) {
        hMean = func.getParameter(1)
        hRMS = func.getParameter(2).abs()
        func.setRange(hMean-2*hRMS,hMean+2*hRMS)
        DataFitter.fit(func,h1,"Q")
        nfits++
        return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
      }
    }

    def fits1 = (0..20).findResults{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    return f1
  }


  static F1D tdcadcdifffit_right(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0)
    def peakbin = (1000..1500).max{h1.getBinContent(it)}
    double hAmp  = h1.getBinContent(peakbin);
    double hMean = h1.getAxis().getBinCenter(peakbin)
    double hRMS  = h1.getRMS();
    double factor1 = 3.0
    double factor2 = 1.57
    double rangeMin = (hMean - factor1*0.5);
    double rangeMax = (hMean + factor2*0.5);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, 1);

    def dmean=0.001
    def dsigma=0.005
    int nfits = 0
    def makefit = {func->
      def dm = hMean - func.getParameter(1)
      def dr = hRMS - func.getParameter(2)
      if ( nfits==0 || ( (dm.abs()>dmean || dr.abs()>dsigma) && (hMean>h1.getAxis().min() && hMean<h1.getAxis().max() ) ) ) {
        hMean = func.getParameter(1)
        hRMS = func.getParameter(2).abs()
        func.setRange(hMean-2*hRMS,hMean+2*hRMS)
        DataFitter.fit(func,h1,"Q")
        nfits++
        System.out.println(nfits+" "+func.getParameter(1)+" "+func.getParameter(2))
        return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
      }
    }

    def fits1 = (0..20).findResults{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    return f1
  }

  static F1D tdcadcdifffit_left(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0)
    def peakbin = (400..900).max{h1.getBinContent(it)}
    double hAmp  = h1.getBinContent(peakbin);
    double hMean = h1.getAxis().getBinCenter(peakbin)
    double hRMS  = h1.getRMS();
    double factor1 = 3.0
    double factor2 = 1.57
    double rangeMin = (hMean - factor1*0.5);
    double rangeMax = (hMean + factor2*0.5);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, 1);

    def dmean=0.001
    def dsigma=0.005
    int nfits = 0
    def makefit = {func->
      def dm = hMean - func.getParameter(1)
      def dr = hRMS - func.getParameter(2)
      if ( nfits==0 || ( (dm.abs()>dmean || dr.abs()>dsigma) && (hMean>h1.getAxis().min() && hMean<h1.getAxis().max() ) ) ) {
        hMean = func.getParameter(1)
        hRMS = func.getParameter(2).abs()
        func.setRange(hMean-2*hRMS,hMean+2*hRMS)
        DataFitter.fit(func,h1,"Q")
        nfits++
        System.out.println(nfits+" "+func.getParameter(1)+" "+func.getParameter(2))
        return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
      }
    }

    def fits1 = (0..20).findResults{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    return f1
  }


  static edepfit(H1F h1){
    def f1 = new F1D("fit:"+h1.getName(),"[amp]*landau(x,[mean],[sigma])+[p0]*exp(-[p1]*x)", 0, 30.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS();
    f1.setRange(hMean*0.65, hMean*2);
    f1.setParameter(0, hAmp);
    if (hAmp>0)f1.setParLimits(0, 0.5*hAmp, 1.5*hAmp);
    f1.setParameter(1, hMean);
    if (hMean>0) f1.setParLimits(1, 0.8*hMean, 1.2*hMean);
    f1.setParameter(2, 0.3);
    f1.setParLimits(2, 0.1, 1);
    if (hAmp>0) f1.setParLimits(3,0, hAmp);
    f1.setParLimits(4,0,100);

    def dmean=0.01
    def dsigma=0.01
    int nfits = 0
    def makefit = {func->
      def dm = hMean - func.getParameter(1)
      def dr = hRMS - func.getParameter(2)
      if ( nfits==0 || dm.abs()>dmean || dr.abs()>dsigma ) {
        hMean = func.getParameter(1)
        hRMS = func.getParameter(2).abs()
        func.setRange(hMean*0.65, hMean*2)
        DataFitter.fit(func,h1,"Q")
        nfits++
        return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
      }
    }

    def fits1 = (0..20).findResults{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    return f1
  }
}
