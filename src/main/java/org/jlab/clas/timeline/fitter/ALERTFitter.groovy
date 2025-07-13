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
		f1.setRange(peak_location - 50, peak_location + 50);
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


	static F1D totfitter(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin())
		def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(peak_location - 2, peak_location + 2);
		f1.setParameter(0, maxz);

		return f1
	}

	static F1D residual_fitter(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin())
		def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(peak_location - 2, peak_location + 2);
		f1.setParameter(0, maxz);

		return f1
	}
	static F1D time_fitter_rising(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin())
		def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(peak_location - 2, peak_location + 2);
		f1.setParameter(0, maxz);

		return f1
	}
	static F1D time_fitter_falling(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin())
		def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(peak_location - 2, peak_location + 2);
		f1.setParameter(0, maxz);

		return f1
	}
	static F1D time_fitter_width(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[cst]", -5.0, 5.0);
		def peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin())
		def maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(peak_location - 2, peak_location + 2);
		f1.setParameter(0, maxz);

		return f1
	}
} 
