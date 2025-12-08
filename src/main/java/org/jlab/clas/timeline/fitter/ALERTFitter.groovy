/**
 *
 * Fitter package for CND
 *
 * Writer: Sangbaek Lee
 *
 **/
package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class ALERTFitter{

	static F1D tdcfitter(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
		f1.setLineColor(33);
		f1.setLineWidth(10);
		f1.setOptStat("1111");
		double maxz = h1.getBinContent(h1.getMaximumBin());
		double peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin());
		f1.setRange(peak_location - 10, peak_location + 10);
		f1.setParameter(0,maxz-h1.getBinContent(0));
		f1.setParameter(1, peak_location);
		f1.setParameter(2, 5.0);
		f1.setParameter(3, h1.getBinContent(0));
		if (maxz>0) f1.setParLimits(0, maxz*0.9,maxz*1.1);
		f1.setParLimits(3, 0.0, 0.1*maxz);

		double hMean, hRMS
			def originalOut = System.out
			System.setOut(new PrintStream(OutputStream.nullOutputStream()))  // Java 11+

			// Code that prints to System.out
			DataFitter.fit(f1, h1, "");

		System.setOut(originalOut)  // Restore the original output

			return f1
	}

	static F1D tdc_minus_start_time_fitter(H1F h1, int component){
		//works for bars
		if(component>9){
			def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
			f1.setLineColor(33);
			f1.setLineWidth(10);
			f1.setOptStat("1111");
			double maxz = h1.getBinContent(h1.getMaximumBin());
			double peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin());
			f1.setRange(peak_location - 5, peak_location + 5);
			f1.setParameter(0,maxz-h1.getBinContent(0));
			f1.setParameter(1, peak_location);
			f1.setParameter(2, 1.0);
			f1.setParameter(3, h1.getBinContent(0));
			if (maxz>0) f1.setParLimits(0, maxz*0.9,maxz*1.1);
			f1.setParLimits(3, 0.0, 0.1*maxz);

			double hMean, hRMS
			def originalOut = System.out
			System.setOut(new PrintStream(OutputStream.nullOutputStream()))  // Java 11+

			// Code that prints to System.out
			DataFitter.fit(f1, h1, "");

			System.setOut(originalOut)  // Restore the original output

				return f1
		}
		else {
		    double height_fit_set = 0
		    double mean_fit_set = 0
		    double sigma_fit_set = 0
		    double step = 2

		    if (h1.getEntries() < 10) return null

		    // ----- Clone & analyze primary peak -----
		    H1F hpy = h1.histClone("hpy_zoom")
		    int maxBin = hpy.getMaximumBin()
		    double maxY = hpy.getBinContent(maxBin)
		    double peak = hpy.getXaxis().getBinCenter(maxBin)
		    double sigma0 = Math.min(step, getRestrictedRMS(hpy, peak - step, peak + step))

		    // ----- Primary Gaussian Fit -----
		    F1D fgaus = new F1D("fgaus", "[amp]*gaus(x,[mean],[sigma])",
		            peak - step, peak + step)

		    fgaus.setParameter(0, maxY)
		    fgaus.setParameter(1, peak)
		    fgaus.setParameter(2, sigma0 > 0 ? sigma0 : 0.8)
		    fgaus.setParLimits(0, 0, 1.2 * maxY)
		    fgaus.setParLimits(1, peak - step, peak + step)
		    fgaus.setParLimits(2, 0, step)

		    PrintStream original = System.out
		    System.setOut(new PrintStream(OutputStream.nullOutputStream()))
		    DataFitter.fit(fgaus, hpy, "RQ")
		    System.setOut(original)

		    double height = fgaus.getParameter(0)
		    double mean = fgaus.getParameter(1)
		    double sigma = fgaus.getParameter(2)
		    double entriesTotal = hpy.integral()

    // -------------------------------------------------------------------------
    // Utility closure to fit a left-side peak after cutting the histogram
    // -------------------------------------------------------------------------
		    def fitLeftPeak = { H1F h, double prevMean, double prevSigma ->
		        // Cut histogram to the left of the previous peak
		        H1F hcut = h.histClone("hcut")
		        int cutBin = hcut.getXaxis().getBin(prevMean - prevSigma * 2)
		        for (int b = cutBin; b <= hcut.getXaxis().getNBins(); b++) {
		            hcut.setBinContent(b, 0)
		            hcut.setBinError(b, 0)
		        }

		        if (hcut.integral() < 3) return null

		        int mb = hcut.getMaximumBin()
		        if (mb >= cutBin - 2) return null   // ensure left-side peak

		        double pk = hcut.getXaxis().getBinCenter(mb)
		        double amp0 = hcut.getBinContent(mb)

		        F1D ftmp = new F1D("fgaus_left",
		                "[amp]*gaus(x,[mean],[sigma])",
		                pk - step, pk + step)

		        ftmp.setParameter(0, amp0)
		        ftmp.setParameter(1, pk)
		        ftmp.setParameter(2, 0.8)

		        ftmp.setParLimits(0, 0, 1.2 * amp0)
		        ftmp.setParLimits(1, pk - step, pk + step)
		        ftmp.setParLimits(2, 0, step)
		
 		       System.setOut(new PrintStream(OutputStream.nullOutputStream()))
		        DataFitter.fit(ftmp, hcut, "RQ")
		        System.setOut(original)

 		       double A = ftmp.getParameter(0)
 		       double M = ftmp.getParameter(1)
 		       double S = ftmp.getParameter(2)

 		       // validation conditions
 		       if (A > maxY * 0.3 &&
 		           S < step && S > 0.1 &&
 		           M < prevMean - prevSigma &&
 		           hcut.integral() > 0.05 * entriesTotal)
 		       {
 		           return [A, M, S]
 		       }
 		       return null
		    }

    // -------------------------------------------------------------------------
    // Peak 1 (primary) already known: height, mean, sigma
    // Try Peak 2 (first left peak)
    // -------------------------------------------------------------------------
		    def p2 = fitLeftPeak(hpy, mean, sigma)

    // If Peak 2 exists, try Peak 3 (second left peak)
		    def p3 = p2 ? fitLeftPeak(hpy, p2[1], p2[2]) : null

    // If Peak 3 exists, try Peak 4 (third left peak)
		    def p4 = p3 ? fitLeftPeak(hpy, p3[1], p3[2]) : null

    // -------------------------------------------------------------------------
    // Choose deepest detected peak on the left: p4 > p3 > p2 > primary
    // -------------------------------------------------------------------------
		    if (p4) {
		        height_fit_set = p4[0]
		        mean_fit_set   = p4[1]
		        sigma_fit_set  = p4[2]
		    } else if (p3) {
		        height_fit_set = p3[0]
		        mean_fit_set   = p3[1]
		        sigma_fit_set  = p3[2]
		    } else if (p2) {
		        height_fit_set = p2[0]
		        mean_fit_set   = p2[1]
		        sigma_fit_set  = p2[2]
		    } else {
		        height_fit_set = height
		        mean_fit_set   = mean
		        sigma_fit_set  = sigma
		    }

 		   // Return final selected Gaussian
		    F1D fout = new F1D("fit:" + h1.getName(),
		            "[amp]*gaus(x,[mean],[sigma])",
 		           mean_fit_set - step * 2, mean_fit_set + step * 2)

		    fout.setParameter(0, height_fit_set)
		    fout.setParameter(1, mean_fit_set)
		    fout.setParameter(2, sigma_fit_set)

		    fout.setLineColor(33)
		    fout.setLineWidth(10)

		    return fout
		}

	}


	static F1D totfitter(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin())
			def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(peak_location - 1, peak_location + 1);
		f1.setParameter(0, maxz);

		return f1
	}

	static F1D residual_fitter(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
		f1.setLineColor(33);
		f1.setLineWidth(10);
		f1.setOptStat("1111");
		double maxz = h1.getBinContent(h1.getMaximumBin());
		double peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin());
		f1.setRange(peak_location - 2.5, peak_location + 2.5);
		f1.setParameter(0,maxz-h1.getBinContent(0));
		f1.setParameter(1, peak_location);
		f1.setParameter(2, 0.5);
		f1.setParameter(3, h1.getBinContent(0));
		if (maxz>0) f1.setParLimits(0, maxz*0.9,maxz*1.1);
		f1.setParLimits(3, 0.0, 0.1*maxz);

		double hMean, hRMS
			def originalOut = System.out
			System.setOut(new PrintStream(OutputStream.nullOutputStream()))  // Java 11+

			// Code that prints to System.out
			DataFitter.fit(f1, h1, "");

		System.setOut(originalOut)  // Restore the original output

			return f1
	}
	static F1D time_fitter_rising(H1F h1, float t0){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(t0 - 2, t0 + 2);
		f1.setParameter(0, 0.25*maxz);

		return f1
	}
	static F1D time_fitter_falling(H1F h1, float tmax){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(tmax - 2, tmax + 2);
		f1.setParameter(0, 0.25*maxz);

		return f1
	}
	static F1D time_fitter_width(H1F h1, float t0, float tmax){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(t0, tmax);
		f1.setParameter(0, 0.25*maxz);

		return f1
	}
} 
