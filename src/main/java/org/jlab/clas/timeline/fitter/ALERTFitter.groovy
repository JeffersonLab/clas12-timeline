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

	static F1D tdc_minus_start_time_fitter(H1F h1){
/* //works for bars
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
*/
                double height_fit_set = 0;
                double mean_fit_set = 0;
                double sigma_fit_set = 0;
                double step = 2;
                if (h1.getEntries() < 10) return null;
                // Clone & clean histogram
                H1F hpy = h1.histClone("hpy_zoom");
                int maxBin = hpy.getMaximumBin();
                double maxY = hpy.getBinContent(maxBin);
                double peak = hpy.getXaxis().getBinCenter(maxBin);
                double sigma0 = Math.min(step, getRestrictedRMS(hpy, peak - step, peak + step));

                // ----- Primary Gaussian Fit -----
                F1D fgaus = new F1D("fgaus", "[amp]*gaus(x,[mean],[sigma])",
                                peak - step, peak + step);

                fgaus.setParameter(0, maxY);
                fgaus.setParameter(1, peak);
                fgaus.setParameter(2, sigma0 > 0 ? sigma0 : 0.8);
                fgaus.setParLimits(0, 0, 1.2 * maxY);
                fgaus.setParLimits(1, peak - step, peak + step);
                fgaus.setParLimits(2, 0, step);

                // Silence DFit output
                PrintStream original = System.out;
                System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                DataFitter.fit(fgaus, hpy, "RQ");
                System.setOut(original);

                double height = fgaus.getParameter(0);
                double mean = fgaus.getParameter(1);
                double sigma = fgaus.getParameter(2);
                double entriesTotal = hpy.integral();

                // ----- Build left-side histogram -----
                H1F hpy_side = hpy.histClone("hpy_side");
                int bin1 = hpy_side.getXaxis().getBin(mean - step);
                for (int b = bin1; b <= hpy_side.getXaxis().getNBins(); b++) {
                        hpy_side.setBinContent(b, 0);
                        hpy_side.setBinError(b, 0);
                }
                // ----- Find valley over 10 bins -----
                int valleyBin = bin1 - 10;
                double valleyValue = hpy_side.getBinContent(valleyBin);

                for (int b = bin1 - 10; b < bin1; b++) {
                        double val = hpy_side.getBinContent(b);
                        if (val < valleyValue) {
                                valleyValue = val;
                                valleyBin = b;
                        }
                }
                if (hpy_side.getMaximumBin() > bin1 - 5) {
                        for (int b = valleyBin; b < bin1; b++) {
                                hpy_side.setBinContent(b, 0);
                                hpy_side.setBinError(b, 0);
                        }
                } else {
                        valleyBin = bin1;
                }

                // ----- Fit potential secondary peak -----
                boolean flag_replace = false;
                double height2 = 0, mean2 = 0, sigma2 = 0;
                double entries2 = hpy_side.integral();

                if (entries2 > 0 && hpy_side.getMaximumBin() < bin1 - 2) {

                        int maxBin2 = hpy_side.getMaximumBin();
                        double peak2 = hpy_side.getXaxis().getBinCenter(maxBin2);

                        F1D fgaus2 = new F1D("fgaus_side", "[amp]*gaus(x,[mean],[sigma])",
                                        peak2 - step, peak2 + step);

                        fgaus2.setParameter(0, hpy_side.getBinContent(maxBin2));
                        fgaus2.setParameter(1, peak2);
                        fgaus2.setParameter(2, 0.8);

                        fgaus2.setParLimits(0, 0, 1.2 * hpy_side.getBinContent(maxBin2));
                        fgaus2.setParLimits(1, peak2 - step, peak2 + step);
                        fgaus2.setParLimits(2, 0, step);

                        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                        DataFitter.fit(fgaus2, hpy_side, "RQ");
                        System.setOut(original);

                        height2 = fgaus2.getParameter(0);
                        mean2 = fgaus2.getParameter(1);
                        sigma2 = fgaus2.getParameter(2);
                        if (entries2 > 0.1 * entriesTotal &&
                                        height2 > maxY * 0.3 &&
                                        sigma2 < step && sigma2 > 0.1 &&
                                        mean2 < mean - sigma) {
                                flag_replace = true;
                        }
                }

                // ----- Final selection -----
                height_fit_set = height;
                mean_fit_set = mean;
                sigma_fit_set = sigma;

                if (flag_replace) {
                        height_fit_set = height2;
                        mean_fit_set = mean2;
                        sigma_fit_set = sigma2;
                }

                // ----- Return final fit function -----
                F1D fout = new F1D("fit:" + h1.getName(),
                                "[amp]*gaus(x,[mean],[sigma])",
                                mean_fit_set - step * 2, mean_fit_set + step * 2);

                fout.setParameter(0, height_fit_set);
                fout.setParameter(1, mean_fit_set);
                fout.setParameter(2, sigma_fit_set);

                fout.setLineColor(33);
                fout.setLineWidth(10);

                return fout;

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
