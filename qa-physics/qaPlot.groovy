/* create hipo file with plots of N, F, N/F, etc. vs. time bin, for each run
 * - this starts to build the structure of the 'timeline' hipo file
 */

import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

//----------------------------------------------------------------------------------
// ARGUMENTS:
if(args.length<1) {
  System.err.println "USAGE: run-groovy ${this.class.getSimpleName()}.groovy [INPUT_DIR] [USE_FT(optional,default=false)]"
  System.exit(101)
}
def useFT = false // if true, use FT electrons instead
inDir = args[0]
if(args.length>=2) useFT = true
//----------------------------------------------------------------------------------

// define vars and subroutines
def sectors = 0..<6
def sec = { int i -> i+1 }
def tok
int r=0
def runnum, binnum, sector
def eventNumMin, eventNumMax
def timestampMin, timestampMax
def nElec, nElecFT
def fcStart, fcStop
def ufcStart, ufcStop
def aveLivetime
def fcCharge
def ufcCharge
def trigRat
def errPrint = { str -> System.err << "ERROR in run ${runnum}_${binnum}: "+str+"\n" }

// define graphs
def defineGraph = { name,ytitle ->
  sectors.collect {
    def g = new GraphErrors(name+"_${runnum}_"+sec(it))
    def gT = ytitle+" vs. time bin"+(useFT?"":" -- Sector "+sec(it))
    g.setTitle(gT)
    g.setTitleY(ytitle)
    g.setTitleX("time bin")
    return g
  }
}
def grA, grN, grF, grU, grT

// define output hipo file
def outHipo = new TDirectory()
"mkdir -p ${inDir}/outmon".execute()
def outHipoN = "${inDir}/outmon/monitorElec"+(useFT?"FT":"")+".hipo"
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
def dataFile = new File("${inDir}/outdat/data_table.dat")
def runnumTmp = 0
def electronT = useFT ? "Forward Tagger Electron" : "Trigger Electron"
if(!(dataFile.exists())) throw new Exception("data_table.dat not found")
dataFile.eachLine { line ->

  // read columns of data_table.dat (in order left-to-right)
  tok = line.tokenize(' ')
  r=0
  runnum = tok[r++].toInteger()
  binnum = tok[r++].toInteger()
  eventNumMin = tok[r++].toBigInteger()
  eventNumMax = tok[r++].toBigInteger()
  timestampMin = tok[r++].toBigInteger()
  timestampMax = tok[r++].toBigInteger()
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
  //livetime = aveLivetime // average `livetime`, directly from scaler bank
  livetime = livetimeFromFCratio // from gated/ungated FC charge
  //println "LIVETIME: aveLivetime, livetimeFromFCratio, diff = ${aveLivetime}, ${livetimeFromFCratio}, ${aveLivetime-livetimeFromFCratio}"

  // add points to graphs
  s = sector-1
  if(s<0||s>5) { errPrint("bad sector number $sector") }
  else {
    grA[s].addPoint(binnum,trigRat,0,0)
    grN[s].addPoint(binnum,nElec,0,0)
    grF[s].addPoint(binnum,fcCharge,0,0)
    grU[s].addPoint(binnum,ufcCharge,0,0)
    grT[s].addPoint(binnum,livetime,0,0)
  }

} // eo loop through data_table.dat
writePlots(runnum) // write last run's graphs


// write hipo file
File outHipoFile = new File(outHipoN)
if(outHipoFile.exists()) outHipoFile.delete()
outHipo.writeFile(outHipoN)
