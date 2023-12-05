import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import org.jlab.clas.timeline.util

def inHipo = new TDirectory()
inHipo.readFile("test.hipo")
def h1 = inHipo.getObject("/tof/p2_tdcadc_dt_S1")
def h2 = HistoUtil.zoomHisto(h1)

def outHipo = new TDirectory()
outHipo.mkdir("/test")
outHipo.cd("/test")
outHipo.addDataSet(h1)
outHipo.addDataSet(h2)
outHipo.write("out.hipo")
