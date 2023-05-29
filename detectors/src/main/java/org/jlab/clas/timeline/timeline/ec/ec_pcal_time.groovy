package org.jlab.clas.timeline.timeline.ec
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ECFitter

class ec_pcal_time {

def data = new ConcurrentHashMap()

def processDirectory(dir, run) {
  def funclist = []
  def meanlist = []
  def sigmalist = []
  def chi2list = []
  def histlist =   (0..<6).collect{
	  def h1 = dir.getObject('/elec/H_trig_PCAL_vt_S'+(it+1))
	  h1.setName("sec"+(it+1))
	  h1.setTitle("PCAL Time Residual")
	  h1.setTitleX("PCAL Time Residual (ns)")
	  def f1 = ECFitter.timefit(h1)
	  funclist.add(f1)
	  meanlist.add(f1.getParameter(1))
	  sigmalist.add(f1.getParameter(2).abs())
	  chi2list.add(f1.getChiSquare())
	  return h1
  }
  data[run] = [run:run, hlist:histlist, flist:funclist, mean:meanlist, sigma:sigmalist, clist:chi2list]
}



def close() {


  ['mean', 'sigma'].each{name->
	  TDirectory out = new TDirectory()
	  out.mkdir('/timelines')
	  (0..<6).each{ sec->
		  def grtl = new GraphErrors(name)
		  grtl.setTitle("e- time - start time, " + name)
		  grtl.setTitleY("e- time - start time, "+ name +" (ns)")
		  grtl.setTitleX("run number")

		  data.sort{it.key}.each{run,it->
			  if (sec==0){
				  out.mkdir('/'+it.run)
				}
				out.cd('/'+it.run)
				out.addDataSet(it.hlist[sec])
			 	out.addDataSet(it.flist[sec])
				grtl.addPoint(it.run, it.name[sec], 0, 0)
		  }
		  out.cd('/timelines')
		  out.addDataSet(grtl)
	  }
	  out.writeFile('ec_elec_pcal_time_'+name+'.hipo')	  
  }
  
}
}
