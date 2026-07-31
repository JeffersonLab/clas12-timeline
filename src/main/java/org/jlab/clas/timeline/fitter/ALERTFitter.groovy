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

    // =========================================================================
    // Crystal Ball function implemented as a pure F1D expression.
    //
    // Standard Crystal Ball (right tail, alpha > 0):
    //   f(x) = exp(-t^2 / 2)                       for t <= |alpha|
    //          A * (B + t)^(-n)                     for t >  |alpha|
    //
    //   where t = (x-mu)/sigma
    //         A = (n/|alpha|)^n * exp(-|alpha|^2 / 2)
    //         B = n/|alpha| - |alpha|
    //
    // Since F1D expressions cannot express a hard if/else branch, we replace
    // the switch with a sigmoid smooth step: s(t) = 1/(1+exp(+k*(t-|alpha|)))
    // The steepness k=50 makes this a near-perfect step at t = +|alpha|:
    //
    //   f(x) = s(t) * exp(-t^2/2)  +  (1-s(t)) * A * (B+t)^(-n)
    //
    // The abs() guard on (B+t) in the power-law term prevents NaN during
    // fitting if parameters wander into regions where B+t < 0.  In the
    // physically meaningful tail region (t > |alpha|), B+t is always >0,
    // and elsewhere the (1-s(t)) factor suppresses the term anyway.
    //
    // Parameters: [amp] [mean] [sigma] [alpha] [n] [cst]
    // =========================================================================

    private static final String CRYSTAL_BALL =
        "[amp]*(" +
        "  (1/(1+exp(50*(((x-[mean])/[sigma])-abs([alpha])))))" +
        "  * exp(-0.5*((x-[mean])/[sigma])^2)" +
        "  +" +
        "  (1-(1/(1+exp(50*(((x-[mean])/[sigma])-abs([alpha]))))))" +
        "  * (([n]/abs([alpha]))^[n] * exp(-0.5*abs([alpha])^2))" +
        "  * (abs(([n]/abs([alpha]) - abs([alpha])) + (x-[mean])/[sigma]))^(-[n])" +
        ") + [cst]"


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

        PrintStream original = System.out

        if(component > 9) { // bars — single peak: FWHM-based initial estimates

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
            double fitHigh = peak_location + 1.0

            def f1 = new F1D("fit:" + h1.getName(), CRYSTAL_BALL, fitLow, fitHigh)
            f1.setLineColor(33);
            f1.setLineWidth(10);
            f1.setOptStat("1111");
            f1.setParameter(0, maxz)                    // amp
            f1.setParameter(1, peak_location)            // mean
            f1.setParameter(2, sigmaEst)                 // sigma
            f1.setParameter(3, 1.0)                      // alpha (positive = right tail)
            f1.setParameter(4, 2.0)                      // n
            f1.setParameter(5, h1.getBinContent(0))      // cst (background)
            if (maxz > 0) f1.setParLimits(0, maxz*0.7, maxz*1.3);
            f1.setParLimits(2, 0.1, 2.0)
            f1.setParLimits(3, 0.0, 10.0)                // alpha must be positive
            f1.setParLimits(4, 1.0, 50.0)                // n
            f1.setParLimits(5, 0.0, 0.1 * maxz)          // background

            System.setOut(new PrintStream(OutputStream.nullOutputStream()))
            DataFitter.fit(f1, h1, "RQ")
            System.setOut(original)

            return f1

        } else { // wedges — multi-peak: Gaussian cascade to find the correct peak

            int maxBin = h1.getMaximumBin()
            double maxY = h1.getBinContent(maxBin)
            double peak = h1.getAxis().getBinCenter(maxBin)
            double step = 1.0f

            int binLow  = h1.getAxis().getBin(peak - step)
            int binHigh = h1.getAxis().getBin(peak + step)
            double sigma0 = ALERTFitter.getRestrictedRMS(h1, binLow, binHigh)
            if (sigma0 <= 0 || Double.isNaN(sigma0)) sigma0 = 1.0

            // --- Step 1: Gaussian pre-fit at the main peak ---

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

            // --- Helper: recursively fit a Gaussian to a left-side peak ---

            def fitLeftPeak = { H1F h, double prevMean, double prevSigma ->

                H1F hcut = h.histClone("hcut")
                int cutBin = hcut.getXaxis().getBin(prevMean - prevSigma * 2)

                // zero bins to the right of the cut
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

            double fitLow  = mean_fit - 2.0
            double fitHigh = mean_fit + 1.0

            def f1 = new F1D("fit:" + h1.getName(), CRYSTAL_BALL, fitLow, fitHigh)
            f1.setLineColor(33)
            f1.setLineWidth(10)

            f1.setParameter(0, localMax)        // amp
            f1.setParameter(1, mean_fit)        // mean
            f1.setParameter(2, sigmaEst)        // sigma
            f1.setParameter(3, 0.5)             // alpha (positive = right tail)
            f1.setParameter(4, 2.0)             // n
            f1.setParameter(5, 0)               // cst (background)
            f1.setParLimits(0, localMax * 0.9, localMax * 1.1)
            f1.setParLimits(1, mean_fit - 0.5, mean_fit)
            f1.setParLimits(2, 0.1, 1.5)
            f1.setParLimits(3, 0.0, 10.0)       // alpha must be positive
            f1.setParLimits(4, 1.0, 5.0)
            f1.setParLimits(5, 0, maxY * 0.5)

            System.setOut(new PrintStream(OutputStream.nullOutputStream()))
            DataFitter.fit(f1, h1, "RQ")
            System.setOut(original)

            return f1
        }
    }


    static F1D atof_z_fitter(H1F h1){
        // Rebin a clone for fitting if low statistics; original h1 is not modified
        H1F hfit = h1
        int entries = (int) h1.getEntries()
        if (entries < 400) {
            int ngroup = (entries < 200) ? 4 : 2
            int nbins = h1.getAxis().getNBins()
            int newbins = nbins / ngroup
            hfit = new H1F("hfit_rebin", newbins, h1.getAxis().min(), h1.getAxis().max())
            for (int i = 0; i < newbins; i++) {
                float sum = 0
                for (int j = 0; j < ngroup; j++) sum += h1.getBinContent(i * ngroup + j)
                hfit.setBinContent(i, sum)
            }
        }

        double maxz = hfit.getBinContent(hfit.getMaximumBin())
        double peak = hfit.getAxis().getBinCenter(hfit.getMaximumBin())

        def f1 = new F1D("fit:" + h1.getName(), "[amp]*gaus(x,[mean],[sigma])", peak - 75, peak + 75)
        f1.setLineColor(33)
        f1.setLineWidth(10)
        f1.setOptStat("1111")
        f1.setParameter(0, maxz)
        f1.setParameter(1, peak)
        f1.setParameter(2, 15.0)
        if (maxz > 0) f1.setParLimits(0, maxz * 0.5, maxz * 1.5)
        f1.setParLimits(1, peak - 50.0, peak + 50.0)
        f1.setParLimits(2, 0.01, 100.0)

        PrintStream original = System.out
        System.setOut(new PrintStream(OutputStream.nullOutputStream()))
        DataFitter.fit(f1, hfit, "RQ")
        System.setOut(original)

        if (entries < 400) hfit = null

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
