package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class ECFitter {
  static F1D ggmfit(H1F h1) {

    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])+[const]", 0.05, 0.2);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS(); 
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, 0.02);
    f1.setParameter(3,0);

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
    //makefit(f1)

    return f1
  }

  static F1D timefit(H1F h1) {
    
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS(); //ns
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);
    f1.setRange(hMean-1,hMean+1)
    DataFitter.fit(f1,h1,"")

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

  static F1D samplfit(H1F h1) {
    
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", 0.15, 0.35);
    double hAmp  = h1.getBinContent(h1.getMaximumBin());
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
    double hRMS  = h1.getRMS();
    f1.setParameter(0, hAmp);
    f1.setParameter(1, hMean);
    f1.setParameter(2, hRMS);

    def makefits = {func->
      hRMS = func.getParameter(2).abs()
      func.setRange(hMean-2*hRMS, hMean+2.5*hRMS)
      DataFitter.fit(func,h1,"Q")
      return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
    }
    def fits1 = (0..10).collect{makefits(f1)}

    def f2 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])+[const]+[slope]*x",-0.2,0.2);

    fits1.sort()[0][1].eachWithIndex{par,ipar->
      f2.setParameter(ipar, par)
    }

    def fits2 = (0..10).collect{makefits(f2)}

    def bestfit = fits2.sort()[0]
    f2.setParameters(*bestfit[1])

    return f2
  }



}
