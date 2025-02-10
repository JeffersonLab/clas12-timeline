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
	static F1D fit(H1F h1) {
	    def f1 = new F1D("fit:"+h1.getName(), "[amp1]*gaus(x,[mean1],[sigma1])+[amp2]*gaus(x,[mean2],[sigma2])+[p0]+[p1]*x", -20.0, 10.0);
        double hAmp  = h1.getBinContent(h1.getMaximumBin());
        double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
        double hRMS  = h1.getRMS(); //ns
        double rangeMin = (hMean - (3*hRMS));
        double rangeMax = (hMean + (3*hRMS));
        f1.setRange(rangeMin, rangeMax);
        f1.setParameter(0, hAmp);
        f1.setParameter(1, -8);
        f1.setParameter(2, 0.1);
		f1.setParameter(3, hAmp);
        f1.setParameter(4, -3);
        f1.setParameter(5, 0.1);
		MoreFitter.fit(f1,h1,"LQ");

		def makefit = {func->
			double hMean1 = func.getParameter(1)
			double hRMS1 = func.getParameter(2).abs()
			double hMean2 = func.getParameter(3)
			double hRMS2 = func.getParameter(4).abs()
			func.setRange(-20, 20)
			MoreFitter.fit(func,h1,"Q")
			return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
		}

		def fits1 = (0..20).collect{makefit(f1)}
		def bestfit = fits1.sort()[0]
		f1.setParameters(*bestfit[1])
		//makefit(f1)

		return f1
 	}
} 
