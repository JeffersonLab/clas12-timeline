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


class HTCCFitter{
  static F1D timeIndPMT(H1F h1) {
    double maxV = h1.getDataX(h1.getMaximumBin());

    // compute the IQR of `h1`, which is a better estimate of the initial sigma parameter than
    // the RMS, since it is not (typically) biased by outliers
    def l1 = []
    h1.getAxis().getNBins().times { bin ->
      def counts = h1.getBinContent(bin).toInteger() // FIXME: assumes the histogram is unweighted
      def value  = h1.getAxis().getBinCenter(bin)
      counts.times { l1 += value }
    }
    def listMedian = { d ->
      if(d.size()==0) {
        System.err.println("WARNING: attempt to calculate median of an empty list")
        return 0
      }
      d.sort()
      def m = d.size().intdiv(2)
      d.size() % 2 ? d[m] : (d[m-1]+d[m]) / 2
    }
    def mq = listMedian(l1)
    def lq = listMedian(l1.findAll{it<mq})
    def uq = listMedian(l1.findAll{it>mq})
    def iqr = uq - lq


    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", maxV-2, maxV+3);

    f1.setLineColor(2);
    f1.setLineWidth(2);
    f1.setOptStat(1101);

    f1.setParameter(0, h1.getMax());
    f1.setParameter(1, maxV);
    f1.setParameter(2, Math.max(iqr / 2.0, 0.1));
    f1.setParLimits(2, Math.max(iqr / 5.0, 0.05), Math.min(iqr * 5.0, h1.getAxis().getBinCenter(h1.getAxis().getNBins()-1)));

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
