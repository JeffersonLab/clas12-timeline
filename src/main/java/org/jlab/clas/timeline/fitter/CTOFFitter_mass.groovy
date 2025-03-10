package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class CTOFFitter_mass {
  static F1D fit(H1F h1) {
    def hAmp = h1.getBinContent(h1.getMaximumBin())
    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin())
    double xmin = hMean-0.2
    double xmax = hMean+0.2
    double hRMS  = 0.08
    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])+[const]",xmin,xmax);

    f1.setParameter(1, hMean)
    f1.setParameter(2, hRMS)
    f1.setParLimits(2, 0, 0.4)
    f1.setParameter(3, 0)
    if (hAmp>0) f1.setParLimits(3, 0, hAmp*0.2)

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
