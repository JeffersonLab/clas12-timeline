/**
*
* Fitter package for HTCC
*
* Writer: Sangbaek Lee, Raffaella De Vita
*
**/
package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import org.jlab.clas.timeline.util.HistoUtil


class HTCCFitter{
  static F1D timeIndPMT(H1F h1) {
    double maxV = h1.getDataX(h1.getMaximumBin());

    // compute the IQR of `h1`, which is a better estimate of the initial sigma parameter than
    // the RMS, since it is not (typically) biased by outliers
    def iqr = HistoUtil.getHistoIQR(h1, -1.0)

    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", maxV-2, maxV+3);

    f1.setLineColor(2);
    f1.setLineWidth(2);
    f1.setOptStat(1101);

    f1.setParameter(0, h1.getMax());
    f1.setParameter(1, maxV);
    if(iqr > 0) {
      f1.setParameter(2, Math.max(iqr / 2.0, 0.1));
      f1.setParLimits(2, Math.max(iqr / 5.0, 0.05), Math.min(iqr * 5.0, h1.getAxis().getBinCenter(h1.getAxis().getNBins()-1)));
    } else {
      f1.setParameter(2, 0.1);
      f1.setParLimits(2, 0.05, h1.getAxis().getBinCenter(h1.getAxis().getNBins()-1));
    }

    DataFitter.fit(f1, h1, "");

    return f1
  }

 	static F1D timeAllFit(H1F h1){
        F1D f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -1, 1);
        f1.setRange(-1, 1);
        f1.setParameter(0, 20000);
        f1.setParameter(1, 0);
        f1.setParameter(2, 1);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1100");
        DataFitter.fit(f1, h1, "");
		return f1
	}
}
