package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import org.jlab.clas.timeline.util.HistoUtil


class CTOFFitter {
  static F1D timefit(H1F h1) {
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0)
    double hAmp   = h1.getBinContent(h1.getMaximumBin());
    double hMu    = h1.getAxis().getBinCenter(h1.getMaximumBin())
    double hSigma = Math.min(HistoUtil.getHistoIQR(h1) / 2.0, 0.2)
    def rangeFactor = 1.5
    double rangeMin = (hMu - rangeFactor*hSigma);
    double rangeMax = (hMu + rangeFactor*hSigma);
    f1.setRange(rangeMin, rangeMax);
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMu);
    f1.setParameter(2, hSigma);

    def makefit = {func->
      def fMu = func.getParameter(1)
      def fSigma = func.getParameter(2).abs()
      func.setRange(fMu-rangeFactor*fSigma, fMu+rangeFactor*fSigma)
      DataFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }

    def fits1 = (0..20).collect{makefit(f1)}
    def bestfit = fits1.sort()[0]
    f1.setParameters(*bestfit[1])
    //makefit(f1)
    return f1
  }


  static def tdcadcdifffit(H1F h1) {

    def fit_peak = { peakbin, prefix ->
      def f1 = new F1D("${prefix}:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0)
      double hAmp  = h1.getBinContent(peakbin);
      double hMean = h1.getAxis().getBinCenter(peakbin)
      double hRMS  = h1.getRMS();
      double factor1 = 3.0
      double factor2 = 1.57
      double rangeMin = (hMean - factor1*0.5);
      double rangeMax = (hMean + factor2*0.5);
      f1.setRange(rangeMin, rangeMax);
      f1.setParameter(0, hAmp);
      // f1.setParLimits(0, hAmp*0.8, hAmp*1.2);
      f1.setParameter(1, hMean);
      // f1.setParLimits(1, 0, 0.5);
      f1.setParameter(2, 1);
      // f1.setParLimits(2, 0.5*hRMS, 1.5*hRMS);

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

    // find the highest peak, and fit it
    def bins = (1..h1.getAxis().getNBins())
    def peakbin1 = bins.max{h1.getBinContent(it)}
    def func1 = fit_peak(peakbin1,'fit1')

    // find the 2nd highest peak by excluding the region around the 1st highest
    // peak, and searching for the new highest max, then fit it
    def peak1Start = func1.getParameter(1) - 2*func1.getParameter(2)
    def peak1End   = func1.getParameter(1) + 2*func1.getParameter(2)
    def peakbin2 = bins
      .collect{ [ it, h1.getBinContent(it) ] }
      .findAll{ h1.getAxis().getBinCenter(it[0]) < peak1Start || h1.getAxis().getBinCenter(it[0]) > peak1End }
      .max{ it[1] }[0]
    def func2 = fit_peak(peakbin2,'fit2')

    // decide which fit result is upstream and downstream
    def funcs = func1.getParameter(1) < func2.getParameter(1) ? [func1,func2] : [func2,func1]

    // create a summed combination (since the front-end seems to prefer this)
    def combinedFunc = new F1D(
      "fit:"+h1.getName(),
      "[ampUpstream]*gaus(x,[meanUpstream],[sigmaUpstream])+[ampDownstream]*gaus(x,[meanDownstream],[sigmaDownstream])",
      funcs[0].getParameter(1) - 3*funcs[0].getParameter(2),
      funcs[1].getParameter(1) + 3*funcs[1].getParameter(2))
    [0,1,2].each{
      combinedFunc.setParameter(it, funcs[0].getParameter(it))
      combinedFunc.setParameter(it+3, funcs[1].getParameter(it))
    }
    funcs.add(combinedFunc)

    return funcs // [ upstream fit, downstream fit, combined fit ]
  }


  static edepfit(H1F h1){
    def f1 = new F1D("fit:"+h1.getName(),"[amp]*landau(x,[mean],[sigma])+[p0]*exp(-[p1]*x)", 0, 30.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS(); //ns
    f1.setRange(hMean*0.65, hMean*2);
    f1.setParameter(0, hAmp);
    if (hAmp>0)f1.setParLimits(0, 0.5*hAmp, 1.5*hAmp);
    f1.setParameter(1, hMean);
    if (hMean>0) f1.setParLimits(1, 0.8*hMean, 1.2*hMean);//Changed from 5-30
    f1.setParameter(2, 0.3);//Changed from 2
    f1.setParLimits(2, 0.1, 1);//Changed from 0.5-10
    if (hAmp>0) f1.setParLimits(3,0, hAmp);
    f1.setParLimits(4,0,100);

    def makefit = {func->
      hMean = func.getParameter(1)
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean*0.65, hMean*2)
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
