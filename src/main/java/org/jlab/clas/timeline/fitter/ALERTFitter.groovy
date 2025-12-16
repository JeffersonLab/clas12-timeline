/**
*
* Fitter package for ALERT
*
* Writer: Sangbaek Lee, Zhiwan Xu
*
**/
package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class ALERTFitter{

	static double getRestrictedRMS(H1F h1, int bin_low, int bin_high){
		double mean = h1.getMean();
		double rms = 0.0;
		double summ  = 0.0;
		int    count = 0; 
		for(int i = bin_low; i <= bin_high; i++){
				int bincontent = (int) h1.getBinContent(i);
				if(bincontent!=0){
						double variance = h1.getAxis().getBinCenter(i) - mean;
						summ  += variance*variance*h1.getBinContent(i);
						count += (int) h1.getBinContent(i);
				}
		}
		if(count!=0) {
				rms = summ/count;
				return Math.sqrt(rms);
		}
		return rms;
  }


	static F1D atof_time_fitter(H1F h1, int component){
		if(component>9){//bars
			def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
			f1.setLineColor(33);
			f1.setLineWidth(10);
			f1.setOptStat("1111");
			double maxz = h1.getBinContent(h1.getMaximumBin());
			double peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin());
			int bin_low  = h1.getAxis().getBin(peak_location - 2.0);
			int bin_high = h1.getAxis().getBin(peak_location + 2.0);
			double sigma = ALERTFitter.getRestrictedRMS(h1, bin_low, bin_high);
			if(sigma>1 || sigma<0 || Double.isNaN(sigma)) sigma=1;
			f1.setRange(peak_location - sigma, peak_location + sigma);
			f1.setParameter(0,maxz-h1.getBinContent(0));
			f1.setParameter(1, peak_location);
			f1.setParameter(2, 1.0);
			f1.setParameter(3, h1.getBinContent(0));
			if (maxz>0) f1.setParLimits(0, maxz*0.7,maxz*1.3);
			f1.setParLimits(3, 0.0, 0.1*maxz);
			f1.setParLimits(2,0,1.0);
			f1.setParLimits(1,peak_location - sigma, peak_location + sigma);

			double hMean, hRMS
			def originalOut = System.out
			System.setOut(new PrintStream(OutputStream.nullOutputStream()))  // Java 11+

			// Code that prints to System.out
			DataFitter.fit(f1, h1, "");

			System.setOut(originalOut)  // Restore the original output

				return f1
		}
		else{//wedges
 			int maxBin = h1.getMaximumBin()
			double maxY = h1.getBinContent(maxBin)
 			double peak = h1.getAxis().getBinCenter(maxBin)
 			double step = 1.0f
 			int binLow  = h1.getAxis().getBin(peak - step)
 			int binHigh = h1.getAxis().getBin(peak + step)
 			double sigma0 = ALERTFitter.getRestrictedRMS(h1, binLow, binHigh)
 			if (sigma0 <= 0 || Double.isNaN(sigma0)) sigma0 = 1.0
			
			F1D fgaus = new F1D("fgaus_main",
			        "[amp]*gaus(x,[mean],[sigma])",
			        peak - step, peak + step)

			fgaus.setParameter(0, maxY)
			fgaus.setParameter(1, peak)
			fgaus.setParameter(2, sigma0)

			fgaus.setParLimits(0, 0, 1.2 * maxY)
			fgaus.setParLimits(1, peak - step, peak + step)
			fgaus.setParLimits(2, 0, step)

			PrintStream original = System.out
			System.setOut(new PrintStream(OutputStream.nullOutputStream()))
			DataFitter.fit(fgaus, h1, "RQ")
			System.setOut(original)

			double height_main = fgaus.getParameter(0)
			double mean_main   = fgaus.getParameter(1)
			double sigma_main  = fgaus.getParameter(2)

			double entriesTotal = h1.integral()

			def fitLeftPeak = { H1F h, double prevMean, double prevSigma ->

			    H1F hcut = h.histClone("hcut")
			    int cutBin = hcut.getXaxis().getBin(prevMean - prevSigma * 2)

			    // zero bins to the right
			    for (int b = cutBin; b <= hcut.getXaxis().getNBins(); b++) {
			        hcut.setBinContent(b, 0)
			        hcut.setBinError(b, 0)
			    }

			    if (hcut.integral() < 3) return null

			    int mb = hcut.getMaximumBin()
			    if (mb >= cutBin - 2) return null

			    double pk   = hcut.getXaxis().getBinCenter(mb)
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

			    // same validation as your code
			    if (A > maxY * 0.4 &&
			        S < step && S > 0.1 &&
			        M < prevMean - prevSigma &&
			        hcut.integral() > 0.05 * entriesTotal)
			    {
			        return [A, M, S]
			    }
			    return null
			}
			def p2 = fitLeftPeak(h1, mean_main, sigma_main)
			def p3 = p2 ? fitLeftPeak(h1, p2[1], p2[2]) : null
			def p4 = p3 ? fitLeftPeak(h1, p3[1], p3[2]) : null

			double height_fit, mean_fit, sigma_fit

			if (p4) {
			    (height_fit, mean_fit, sigma_fit) = p4
			} else if (p3) {
			    (height_fit, mean_fit, sigma_fit) = p3
			} else if (p2) {
			    (height_fit, mean_fit, sigma_fit) = p2
			} else {
			    height_fit = height_main
			    mean_fit   = mean_main
			    sigma_fit  = sigma_main
			}

			F1D fout = new F1D("fit:" + h1.getName(),
			        "[amp]*gaus(x,[mean],[sigma])",
			        mean_fit - 2 * step, mean_fit + 2 * step)

			fout.setParameter(0, height_fit)
			fout.setParameter(1, mean_fit)
			fout.setParameter(2, sigma_fit)

			fout.setLineColor(33)
			fout.setLineWidth(10)

			return fout
		}
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
