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

	static F1D fit(H1F h1) {
	    def f1 = new F1D("fit:"+h1.getName(), "[amp]*gaus(x,[mean],[sigma])", -20.0, 10.0);
        double hAmp  = h1.getBinContent(h1.getMaximumBin());
        double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
        double hRMS  = h1.getRMS(); //ns
        double rangeMin = (hMean - (3*hRMS));
        double rangeMax = (hMean + (3*hRMS));
        f1.setRange(rangeMin, rangeMax);
        f1.setParameter(0, hAmp);
        f1.setParameter(1, hMean);
        f1.setParameter(2, hRMS);
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
		//makefit(f1)

		return f1
 	}
} 
