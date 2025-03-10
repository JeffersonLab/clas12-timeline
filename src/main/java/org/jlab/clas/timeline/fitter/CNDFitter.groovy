/**
*
* Fitter package for CND
*
* Writer: Sangbaek Lee, Andrey Kim
*
**/
package org.jlab.clas.timeline.fitter
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D


class CNDFitter{
	static F1D edepfit(H1F h1) {
	    double hAmp  = h1.getBinContent(h1.getMaximumBin());
	    double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
	    double hRMS  = h1.getRMS()
	    def f1=new F1D("fit:"+h1.getName(),"[amp]*landau(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x", 1.0, 4.0);
	    f1.setRange(f1.getRange().getMin(), hMean*2.0);
	    f1.setParameter(0, hAmp);
	    if (hAmp>0) f1.setParLimits(0, 0.5*hAmp, 1.5*hAmp);
	    f1.setParameter(1, hMean);
	    if (hMean>0) f1.setParLimits(1, 0.8*hMean, 1.2*hMean);//Changed from 5-30
	    f1.setParameter(2, 0.15);//Changed from 2
	    f1.setParLimits(2, 0.1, 1);//Changed from 0.5-10
	    f1.setParameter(3, -100);
	    f1.setParameter(4, 500);//Changed from -0.2
	    f1.setParameter(5, -100);//Changed from -0.2

		def makefit = {func->
			hMean = func.getParameter(1)
			hRMS = func.getParameter(2).abs()
		    func.setRange(0.3*hMean,Math.min(1.5*hMean,2.8))
			DataFitter.fit(func,h1,"Q")
			return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
		}

		def fits1 = (0..20).collect{makefit(f1)}
		def bestfit = fits1.sort()[0]
		f1.setParameters(*bestfit[1])
		//makefit(f1)

		return f1
 	}

 	static F1D timefit(H1F h1){
 		def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
		f1.setLineColor(33);
		f1.setLineWidth(10);
		f1.setOptStat("1111");
		double maxt = h1.getBinContent(h1.getMaximumBin());
		double hMean = h1.getAxis().getBinCenter(h1.getMaximumBin());
		double hRMS = h1.getRMS()
		f1.setParameter(1,hMean);
		f1.setParLimits(1,hMean-0.5,hMean+1);
		f1.setRange(hMean-0.5,hMean+0.5);
		f1.setParameter(0,maxt);
		if (maxt>0) f1.setParLimits(0,maxt*0.95,maxt*1.1);
		f1.setParameter(2,0.2);
		DataFitter.fit(f1, h1, "");

		def makefit = {func->
			hMean = func.getParameter(1)
			hRMS = func.getParameter(2).abs()
			func.setRange(hMean-2.5*hRMS,hMean+1.5*hRMS)
			DataFitter.fit(func,h1,"Q")
			return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
		}

		def fits1 = (0..20).collect{makefit(f1)}
		def bestfit = fits1.sort()[0]
		f1.setParameters(*bestfit[1])
		//makefit(f1)

		return f1
	}

	static F1D zdifffit(H1F h1){
		def f1 =new F1D("fit:"+h1.getName(),"[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
		f1.setLineColor(33);
		f1.setLineWidth(10);
		f1.setOptStat("1111");
		double maxz = h1.getBinContent(h1.getMaximumBin());
		f1.setRange(-7,7);
		f1.setParameter(1,0.0);
		f1.setParameter(0,maxz);
		if (maxz>0) f1.setParLimits(0,maxz*0.9,maxz*1.1);
		f1.setParameter(2,3.0);

		double hMean, hRMS

		DataFitter.fit(f1, h1, "");

		def makefit = {func->
			hMean = func.getParameter(1)
			hRMS = func.getParameter(2).abs()
			func.setRange(Math.max(hMean-2.5*hRMS,-5),Math.min(hMean+2.5*hRMS,5))
			DataFitter.fit(func,h1,"Q")
			return [func.getChiSquare(), (0..<func.getNPars()).collect{func.getParameter(it)}]
		}

		def fits1 = (0..20).collect{makefit(f1)}
		def bestfit = fits1.sort()[0]
		f1.setParameters(*bestfit[1])
		//makefit(f1)

		return f1
	}
} 
