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

class CrystalBallFunc extends F1D {

    CrystalBallFunc(String name, double min, double max) {
        super(name)  // check constructor order!
        setRange(min, max);
        addParameter("amp");
        addParameter("mean");
        addParameter("sigma");
        addParameter("alpha");
        addParameter("n");
        addParameter("cst");
    }

    @Override
    double evaluate(double x) {
        double amp   = getParameter(0)
        double mean  = getParameter(1)
        double sigma = getParameter(2)
        double alpha = getParameter(3)
        double n     = getParameter(4)
        double cst   = getParameter(5)

        double t = (x - mean) / sigma
        double absAlpha = Math.abs(alpha)
        double result

        if (t > -absAlpha) {
            result = Math.exp(-0.5 * t * t)
        } else {
            double A = Math.pow(n / absAlpha, n) * Math.exp(-0.5 * absAlpha * absAlpha)
            double B = n / absAlpha - absAlpha
            double arg = B - t
            if (arg <= 0) arg = 1e-10
            result = A * Math.pow(arg, -n)
        }

        return amp * result + cst
    }
}

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


	static F1D atof_time_fitter(H1F h1, int component, double fit_min, double fit_max){
           
            String expression = "[amp]*(  (1/(1+exp(-50*(((x-[mean])/[sigma])+abs([alpha]))))) * exp(-0.5*((x-[mean])/[sigma])^2)  +  (1-(1/(1+exp(-50*(((x-[mean])/[sigma])+abs([alpha])))))) *  (([n]/abs([alpha]))^[n] * exp(-0.5*abs([alpha])^2)) *  (abs([n]/abs([alpha]) - abs([alpha]) - (x-[mean])/[sigma]))^(-[n])) + [cst]"
		if(component>9){//bars
			double maxz = h1.getBinContent(h1.getMaximumBin());
			double peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin());
            double halfMax = maxz / 2.0
            int peakBin = h1.getMaximumBin()
            int nBins = h1.getAxis().getNBins()
            int bLow = peakBin
            int bHigh = peakBin
            while (bLow > 0 && h1.getBinContent(bLow) > halfMax) bLow--
            while (bHigh < nBins && h1.getBinContent(bHigh) > halfMax) bHigh++
            double fwhm = h1.getAxis().getBinCenter(bHigh) - h1.getAxis().getBinCenter(bLow)
            double sigmaEst = (fwhm > 0) ? fwhm / 2.35 : 0.5
            if (sigmaEst < 0.2) sigmaEst = 0.5
            if (sigmaEst > 1.5) sigmaEst = 1.0
            double fitLow  = peak_location - 2.0
            double fitHigh = peak_location + 1.5
            
            def f1 = new CrystalBallFunc("fitCrystal:" + h1.getName(), fitLow, fitHigh)
            f1.setLineColor(33);
            f1.setLineWidth(10);
            f1.setOptStat("1111");
            f1.setParameter(0, maxz)                    // amp
            f1.setParameter(1, peak_location)            // mean
            f1.setParameter(2, sigmaEst)                 // sigma
            f1.setParameter(3, -1.0)                      // alpha (positive = left tail)
            f1.setParameter(4, 2.0)                      // n
            f1.setParameter(5, h1.getBinContent(0))      // constant background
			if (maxz>0) f1.setParLimits(0, maxz*0.7,maxz*1.3);
            f1.setParLimits(2, 0.1, 2.0)
            f1.setParLimits(3, -10.0, 0.0)               // alpha
            f1.setParLimits(4, 1.0, 50.0)               // n
            f1.setParLimits(5, 0.0, 0.1 * maxz)         // constant

			//double hMean, hRMS
			//def originalOut = System.out
			//System.setOut(new PrintStream(OutputStream.nullOutputStream()))  // Java 11+
            println "=== DEBUG BARS: before fit ==="
            println "  f1 class: ${f1.getClass().getName()}"
            println "  f1 is Func1D: ${f1 instanceof Func1D}"
            println "  f1 is F1D: ${f1 instanceof F1D}"
            println "  amp before fit: ${f1.getParameter(0)}"
            println "  mean before fit: ${f1.getParameter(1)}"
            try {
                //System.setOut(new PrintStream(OutputStream.nullOutputStream()))
                DataFitter.fit(f1, h1, "")
                //System.setOut(originalOut)  // Restore the original output
                println "  FIT SUCCEEDED"
            } catch (Exception e) {
                println "  FIT FAILED: ${e.getClass().getName()}: ${e.getMessage()}"
            }

            println "  amp after fit: ${f1.getParameter(0)}"
            println "  mean after fit: ${f1.getParameter(1)}"
            println "  sigma after fit: ${f1.getParameter(2)}"
            println("Printing out f1...")
            println(f1)
			// Code that prints to System.out
//			DataFitter.fit(f1, h1, "");

           def fout = new F1D("fit:" +  h1.getName(), expression, fitLow, fitHigh)//Use sigmoid to mock up the step function.
           fout.setParameter(0, f1.getParameter(0))
           fout.setParameter(1, f1.getParameter(1))
           fout.setParameter(2, f1.getParameter(2))
           fout.setParameter(3, f1.getParameter(3))
           fout.setParameter(4, f1.getParameter(4))
           fout.setParameter(5, f1.getParameter(5))
           println("Printing out fout...")
           println(fout)
           println "=== END DEBUG BARS==="

            return fout
		}
		else{//wedges
            PrintStream original = System.out

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
            
            // --- Step 2: Crystal Ball fit at the found peak ---

            int pkBin = h1.getAxis().getBin(mean_fit)
            double pkHeight = h1.getBinContent(pkBin)
            if (pkHeight <= 0) pkHeight = height_fit
            double halfMax = pkHeight / 2.0
            int bL = pkBin, bH = pkBin
            int nBins = h1.getAxis().getNBins()
            while (bL > 0 && h1.getBinContent(bL) > halfMax) bL--
            while (bH < nBins && h1.getBinContent(bH) > halfMax) bH++
            double fwhm = h1.getAxis().getBinCenter(bH) - h1.getAxis().getBinCenter(bL)
            double sigmaEst = (fwhm > 0) ? fwhm / 2.35 : sigma_fit
            if (sigmaEst < 0.2) sigmaEst = 0.5
            if (sigmaEst > 1.5) sigmaEst = 1.0
            double localMax = 0
            int bLo2 = h1.getAxis().getBin(mean_fit - 0.5)
            int bHi2 = h1.getAxis().getBin(mean_fit + 0.5)
            for (int b = bLo2; b <= bHi2; b++) {
                if (h1.getBinContent(b) > localMax) localMax = h1.getBinContent(b)
            }
            if (localMax <= 0) localMax = height_fit

            double fitLow  = mean_fit - 1.0
            double fitHigh = mean_fit + 1.0

            def f1 = new CrystalBallFunc("fitCrystal:" + h1.getName(), fitLow, fitHigh)
            f1.setLineColor(33)
            f1.setLineWidth(10)

            f1.setParameter(0, localMax)       // amp   → index 0
            f1.setParameter(1, mean_fit)       // mean  → index 1
            f1.setParameter(2, sigmaEst)       // sigma → index 2
            f1.setParameter(3, -0.5)           // alpha → index 3
            f1.setParameter(4, 2.0)            // n     → index 4
            f1.setParameter(5, 0)              // cst   → index 5
            f1.setParLimits(0, localMax * 0.9, localMax * 1.1)
            f1.setParLimits(1, mean_fit - 0.5, mean_fit)
            f1.setParLimits(2, 0.1, 1.5)
            f1.setParLimits(3, -10.0, -0.0)
            f1.setParLimits(4, 1.0, 5.0)
            f1.setParLimits(5, 0, maxY * 0.5)
            println "=== DEBUG WEDGES ==="
            println "  f1 class: ${f1.getClass().getName()}"
            println "  f1 is Func1D: ${f1 instanceof Func1D}"
            println "  f1 is F1D: ${f1 instanceof F1D}"
            println "  amp before fit: ${f1.getParameter(0)}"
            println "  mean before fit: ${f1.getParameter(1)}"

            try {
                //System.setOut(new PrintStream(OutputStream.nullOutputStream()))
                DataFitter.fit(f1, h1, "RQ")
                //System.setOut(original)
                println "  FIT SUCCEEDED"
            } catch (Exception e) {
                println "  FIT FAILED: ${e.getClass().getName()}: ${e.getMessage()}"
            }
            println "  amp after fit: ${f1.getParameter(0)}"
            println "  mean after fit: ${f1.getParameter(1)}"
            println "  sigma after fit: ${f1.getParameter(2)}"
            println("Printing out f1...")
            println(f1)
            def fout = new F1D("fit:" +  h1.getName(), expression, fitLow, fitHigh)
            fout.setParameter(0, f1.getParameter(0))
            fout.setParameter(1, f1.getParameter(1))
            fout.setParameter(2, f1.getParameter(2))
            fout.setParameter(3, f1.getParameter(3))
            fout.setParameter(4, f1.getParameter(4))
            fout.setParameter(5, f1.getParameter(5))
            println("Printing out fout...")
            println(fout)

            println "=== END DEBUG WEDGES ==="

            return fout
//            System.setOut(new PrintStream(OutputStream.nullOutputStream()))
//            DataFitter.fit(fout, h1, "RQ")
//        System.setOut(original)

		}
	}

	static F1D atof_z_fitter(H1F h1){
		double maxz = h1.getBinContent(h1.getMaximumBin())
		double peak = h1.getAxis().getBinCenter(h1.getMaximumBin())
		int bin_low  = h1.getAxis().getBin(peak - 100.0)
		int bin_high = h1.getAxis().getBin(peak + 100.0)
		double sigma = ALERTFitter.getRestrictedRMS(h1, bin_low, bin_high)
		if (sigma <= 0 || Double.isNaN(sigma)) sigma = 100.0

		def f1 = new F1D("fit:" + h1.getName(), "[amp]*gaus(x,[mean],[sigma])", peak - 2*sigma, peak + 2*sigma)
		f1.setLineColor(33)
		f1.setLineWidth(10)
		f1.setOptStat("1111")
		f1.setParameter(0, maxz)
		f1.setParameter(1, peak)
		f1.setParameter(2, sigma)
		if (maxz > 0) f1.setParLimits(0, maxz * 0.5, maxz * 1.5)
		f1.setParLimits(1, peak - 50.0, peak + 50.0)
		f1.setParLimits(2, 0.01, 100.0)

		PrintStream original = System.out
		System.setOut(new PrintStream(OutputStream.nullOutputStream()))
		DataFitter.fit(f1, h1, "RQ")
		System.setOut(original)

		return f1
	}

	static F1D residual_fitter(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
		f1.setLineColor(33);
		f1.setLineWidth(10);
		f1.setOptStat("1111");
		double maxz = h1.getBinContent(h1.getMaximumBin());
		double peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin());
		f1.setRange(peak_location - 1.0, peak_location + 1.0);
		f1.setParameter(0,maxz-h1.getBinContent(0));
		f1.setParameter(1, peak_location);
		f1.setParameter(2, 0.2);
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
