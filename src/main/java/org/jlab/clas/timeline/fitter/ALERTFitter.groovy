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

	static F1D tdcfitter(H1F h1, float tdc_offset){
		def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
		f1.setLineColor(33);
		f1.setLineWidth(10);
		f1.setOptStat("1111");
		double maxz = h1.getBinContent(h1.getMaximumBin());
		double peak_location = 0.5*(h1.getMaximumBin() + 0.5) + tdc_offset;
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

	static F1D totfitter(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
		f1.setLineColor(33);
		f1.setLineWidth(10);
		f1.setOptStat("1111");
		double maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(h1.getMaximumBin() - 3, h1.getMaximumBin() + 3);
		f1.setParameter(0,maxz-h1.getBinContent(0));
		f1.setParameter(1, h1.getMaximumBin() + 0.5);
		f1.setParameter(2, 3.0);
		f1.setParameter(3, h1.getBinContent(0));
		if (maxz>0) f1.setParLimits(0, maxz*0.9,maxz*1.1);
		f1.setParLimits(3, 0.0, 0.1*maxz);

		double hMean, hRMS
		def originalOut = System.out
		System.setOut(new PrintStream(OutputStream.nullOutputStream()))  // Java 11+
                
                // Code that prints to System.out
		DataFitter.fit(f1, h1, "");

		System.setOut(originalOut)  // Restore the original output


//		def makefit = {func->
//			hMean = func.getParameter(1)
//			hRMS  = func.getParameter(2).abs()
//			func.setRange(Math.max(hMean-2.5*hRMS, h1.getMaximumBin() - 50),Math.min(hMean+2.5*hRMS, h1.getMaximumBin() + 50))
//      func.setParLimits(0, func.getParameter(0)*0.9,func.getParameter(0)*1.1);
//      f1.setParLimits(3, 0.0, 0.1*func.getParameter(0));
//			DataFitter.fit(func,h1,"Q")
//			return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
//		}
//
		//def fits1 = (0..0).collect{makefit(f1)}
		//def bestfit = fits1.sort()[0]
		//f1.setParameters(*bestfit[1])
		//makefit(f1)

		return f1
	}
} 
