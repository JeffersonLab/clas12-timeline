/* create hipo file with plots of N, F, N/F, etc. vs. file number, for each run
 * - this starts to build the structure of the 'timeline' hipo file
 */

import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

//----------------------------------------------------------------------------------
// ARGUMENTS:
def dataset = 'inbending1'
def useFT = false // if true, use FT electrons instead
if(args.length>=1) dataset = args[0]
if(args.length>=2) useFT = true
//----------------------------------------------------------------------------------

// define vars and subroutines
def sectors = 0..<6
def sec = { int i -> i+1 }
def tok
int r=0
def runnum, filenum, eventNumMin, eventNumMax, sector
def nElec, nElecFT
def fcStart, fcStop
def ufcStart, ufcStop
def aveLivetime
def fcCharge
def ufcCharge
def trigRat
def errPrint = { str -> System.err << "ERROR in run ${runnum}_${filenum}: "+str+"\n" }

// define graphs
def defineGraph = { name,ytitle ->
  sectors.collect {
    def g = new GraphErrors(name+"_${runnum}_"+sec(it))
    def gT = ytitle+" vs. file number"+(useFT?"":" -- Sector "+sec(it)) 
    g.setTitle(gT)
    g.setTitleY(ytitle)
    g.setTitleX("file number")
    return g
  }
}
def grA, grN, grF, grU, grT

// define output hipo file
def outHipo = new TDirectory()
"mkdir -p outmon.${dataset}".execute()
def outHipoN = "outmon.${dataset}/monitorElec"+(useFT?"FT":"")+".hipo"
def writeHipo = { o -> o.each{ outHipo.addDataSet(it) } }
def writePlots = { run ->
  println "write run $run"
  outHipo.mkdir("/${run}")
  outHipo.cd("/${run}")
  writeHipo(grA)
  writeHipo(grN)
  writeHipo(grF)
  writeHipo(grU)
  writeHipo(grT)
}

// open data_table.dat
def dataFile = new File("outdat.${dataset}/data_table.dat")
def runnumTmp = 0
def electronT = useFT ? "Forward Tagger Electron" : "Trigger Electron"
if(!(dataFile.exists())) throw new Exception("data_table.dat not found")
dataFile.eachLine { line ->

  // read columns of data_table.dat (in order left-to-right)
  tok = line.tokenize(' ')
  r=0
  runnum = tok[r++].toInteger()
  filenum = tok[r++].toInteger()
  eventNumMin = tok[r++].toInteger()
  eventNumMax = tok[r++].toInteger()
  sector = tok[r++].toInteger()
  nElec = tok[r++].toBigDecimal()
  nElecFT = tok[r++].toBigDecimal()
  fcStart = tok[r++].toBigDecimal()
  fcStop = tok[r++].toBigDecimal()
  ufcStart = tok[r++].toBigDecimal()
  ufcStop = tok[r++].toBigDecimal()
  aveLivetime = tok[r++].toBigDecimal()


  // if we are using the FT electrons, simply set nElec to nElecFT, since
  // all code below uses nElec; note that we do not have sector info for
  // nElecFT, so all 6 sectors' plots will be the same (for downstream code
  // compatibility)
  if(useFT) nElec = nElecFT


  // if the run number changed, write filled graphs, then start new graphs
  if(runnum!=runnumTmp) {
    if(runnumTmp>0) writePlots(runnumTmp)
    grA = defineGraph("grA","${electronT} Normalized Yield N/F")
    grN = defineGraph("grN","${electronT} Yield N")
    grF = defineGraph("grF","Gated Faraday Cup charge F [nC]")
    grU = defineGraph("grU","Ungated Faraday Cup charge F [nC]")
    grT = defineGraph("grT","Live Time")
    runnumTmp = runnum
  }


  // calculations
  fcCharge = fcStop - fcStart
  ufcCharge = ufcStop - ufcStart
  //if(fcCharge<=0) errPrint("fcCharge = ${fcCharge} <= 0")
  //if(ufcCharge<=0) errPrint("ufcCharge = ${ufcCharge} <= 0")
  trigRat = fcCharge!=0 ? nElec/fcCharge : 0
  livetimeFromFCratio = ufcCharge!=0 ? fcCharge/ufcCharge : 0

  // choose which livetime to plot and use for QA cut "LowLiveTime"
  livetime = aveLivetime // average `livetime`, directly from scaler bank
  //livetime = livetimeFromFCratio // from gated/ungated FC charge
  //println "LIVETIME: aveLivetime, livetimeFromFCratio, diff = ${aveLivetime}, ${livetimeFromFCratio}, ${aveLivetime-livetimeFromFCratio}"

  // add points to graphs
  s = sector-1
  if(s<0||s>5) { errPrint("bad sector number $sector") }
  else {
    grA[s].addPoint(filenum,trigRat,0,0)
    grN[s].addPoint(filenum,nElec,0,0)
    grF[s].addPoint(filenum,fcCharge,0,0)
    grU[s].addPoint(filenum,ufcCharge,0,0)
    grT[s].addPoint(filenum,livetime,0,0)
  }

} // eo loop through data_table.dat
writePlots(runnum) // write last run's graphs


// write hipo file
File outHipoFile = new File(outHipoN)
if(outHipoFile.exists()) outHipoFile.delete()
outHipo.writeFile(outHipoN)
