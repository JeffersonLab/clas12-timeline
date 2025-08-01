import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

//--------------------------------------------------------------------------
// ARGUMENTS:
if(args.length<2) {
  System.err.println "USAGE: groovy ${this.class.getSimpleName()}.groovy [INPUT_DIR] [DATASET] [USE_FT(optional,default=false)]"
  System.exit(101)
}
useFT = false // if true, use FT electrons instead
qaBit = -1 // if positive, produce QA timeline based on manual QA results
inDir = args[0]
dataset = args[1]
if(args.length>=3) useFT = (args[2]=="FT") ? true : false
if(args.length>=4) qaBit = args[3].toInteger()
//--------------------------------------------------------------------------

// sector handlers
def sectors = 0..<6
def sec = { int i -> i+1 }

// subroutine to write JSON
def jPrint = { name,object -> new File(name).write(JsonOutput.toJson(object)) }

// read env vars
def TIMELINESRC = System.getenv("TIMELINESRC")
if(TIMELINESRC == null) {
  System.err.println "ERROR: \$TIMELINESRC is not set"
  System.exit(100)
}

// read cutDefs yaml file into a tree
def cutDefsFile = new File("${TIMELINESRC}/qadb/cutdefs/${dataset}.yaml")
if(!(cutDefsFile.exists())) {
  cutDefsFile = new File("${TIMELINESRC}/qadb/cutdefs/default.yaml")
}
def cutDefsParser = new Yaml()
def cutDefsTree = cutDefsParser.load(cutDefsFile.text)
// return the cutDef for a given tree path
def cutDef = { path, required=true ->
  def val
  try { val = T.getLeaf(cutDefsTree, path) }
  catch(Exception e) {
    System.err.println("ERROR: missing cut definition in cutdefs file: [${path.join(',')}]")
    System.exit(100)
  }
  if(val == null && required) {
    System.err.println("ERROR: missing cut definition in cutdefs file: [${path.join(',')}]")
    System.exit(100)
  }
  val
}

// read epochs list file
def epochFile = new File("${TIMELINESRC}/qadb/epochs/epochs.${dataset}.txt")
if(!(epochFile.exists())) {
  epochFile = new File("${TIMELINESRC}/qadb/epochs/epochs.default.txt")
}

// get the epoch number for a given run `r` and sector `s`
// - the epoch number ranges from 1 to the number of epochs
//   (so this number is the line number of the epochs/*.txt file)
def getEpoch = { r,s ->
  //return 1 // (for testing single-epoch mode)
  def lb,ub
  def e = -1
  epochFile.eachLine { line,i ->
    (lb,ub) = line.tokenize(' ')[0,1].collect{it.toInteger()}
    if(r>=lb && r<=ub) e=i
  }
  if(e<0) throw new Exception("run $r sector $s has unknown epoch")
  return e
}


// build map of (runnum,binnum) -> (evnumMin,evnumMax)
def dataFile = new File("${inDir}/outdat/data_table.dat")
def tok
def evnumTree = [:]
if(!(dataFile.exists())) throw new Exception("data_table.dat not found")
dataFile.eachLine { line ->
  tok = line.tokenize(' ')
  int r = 0
  def runnum       = tok[r++].toInteger()
  def binnum       = tok[r++].toInteger()
  def evnumMin     = tok[r++].toBigInteger()
  def evnumMax     = tok[r++].toBigInteger()
  def timestampMin = tok[r++].toBigInteger()
  def timestampMax = tok[r++].toBigInteger()
  if(!evnumTree.containsKey(runnum))
    evnumTree[runnum] = [:]
  if(!evnumTree[runnum].containsKey(binnum)) {
    evnumTree[runnum][binnum] = [
      evnumMin:     evnumMin,
      evnumMax:     evnumMax,
      timestampMin: timestampMin,
      timestampMax: timestampMax,
    ]
  }
}


// open hipo file
def inTdir = new TDirectory()
inTdir.readFile("${inDir}/outmon/monitorElec"+(useFT?"FT":"")+".hipo")
def inList = inTdir.getCompositeObjectList(inTdir)

// define 'ratioTree', a tree with the following structure
/*
  {
    sector1: {
      epoch1: [ list of N/F values ]
      epoch2: [ list of N/F values ]
    }
    sector2: {
      epoch1: [ list of N/F values ]
      epoch2: [ list of N/F values ]
      epoch3: [ list of N/F values ]
    }
    ...
  }
// - also define 'cutTree', which follows the same structure as 'ratioTree', but
//   with the leaves [ list of N/F values ] replaced with [ cut boundaries (map) ]
// - also define 'epochPlotTree', with leaves as maps of plots
*/
def ratioTree = [:]
def cutTree = [:]
def epochPlotTree = [:]


// initialize sector branches
sectors.each{
  ratioTree.put(sec(it),[:])
  cutTree.put(sec(it),[:])
  epochPlotTree.put(sec(it),[:])
}


// loop over 'grA' graphs (of N/F vs. binnum), filling ratioTree leaves
def (minA,maxA) = [100000,0]
inList.each { obj ->
  if(obj.contains("/grA_")) {

    // get runnum and sector
    def (runnum,sector) = obj.tokenize('_').subList(1,3).collect{ it.toInteger() }
    if(sector<1||sector>6) throw new Exception("bad sector number $sector")

    // get epoch num, then initialize epoch branch if needed
    def epoch = getEpoch(runnum,sector)
    if(ratioTree[sector][epoch]==null) {
      ratioTree[sector].put(epoch,[])
      cutTree[sector].put(epoch,[:])
      epochPlotTree[sector].put(epoch,[:])
    }

    // append N/F values to the list associated to this (sector,epoch)
    // also determine minimum and maximum values of N/F
    def gr = inTdir.getObject(obj)
    gr.getDataSize(0).times { i ->
      def val = gr.getDataY(i)
      minA = val < minA ? val : minA
      maxA = val > maxA ? val : maxA
      ratioTree[sector][epoch].add(val)
    }
    //ratioTree[sector][epoch].add(runnum) // useful for testing
  }
}
//println T.pPrint(ratioTree)


// subroutine for calculating median of a list
def listMedian = { d, name ->
  if(d.size()==0) {
    System.err.println "WARNING: attempt to calculate median of empty list '${name}'"
    return -10000
  }
  d.sort()
  def m = d.size().intdiv(2)
  d.size() % 2 ? d[m] : (d[m-1]+d[m]) / 2
}


// establish cut lines using 'cutFactor' x IQR method, and fill cutTree
// - note: for the FT electrons, it seems that N/F has a long tail toward
//   lower values, so cutLo is forced to be lower
def cutFactor = cutDef([ useFT ? "OutlierFT" : "OutlierFD", "IQR_cut_factor" ])
System.out.println "N/F outliers will be determined with ${cutFactor} x IQR method"
sectors.each { s ->
  sectorIt = sec(s)
  if( !useFT || (useFT && sectorIt==1)) {
    ratioTree[sectorIt].each { epochIt, ratioList ->

      // filter out zero/negative N/F
      def ratioListForIQR = ratioList.findAll{it>0}

      // check if FT was off (all N/F are zero, so `ratioListForIQR` is empty)
      def ft_was_off = useFT && ratioListForIQR.size()==0

      // if the cutDef file says to "recalculate" the IQR, do so; this is used when there are too many outliers
      // for the IQR method to be robust
      if(cutDef(["RecalculateIQR"], false) != null) {
        cutDef(["RecalculateIQR"]).each { recalcIt ->
          def whichDet = useFT ? "FT" : "FD"
          if(recalcIt['detector'] == whichDet && recalcIt['epoch'] == epochIt && recalcIt['sectors'].contains(sectorIt)) {
            def recalcRange = recalcIt['within_range']
            System.err.println "WARNING: recalculating IQR for epoch ${epochIt} sector ${sectorIt}, as specified in cutdef file"
            ratioListForIQR = ratioList.findAll{ it >= recalcRange[0] && it <= recalcRange[1] }
          }
        }
      }

      // calculate the IQR
      def mq
      def lq
      def uq
      if(ft_was_off) { // if we are QA-ing FT data, but FT was off, just assign "quartiles" such that all N/F values will pass QA
        mq = 0.0
        lq = -1.0
        uq = 1.0
      }
      else { // otherwise compute quartiles
        mq = listMedian(ratioListForIQR, "epoch ${epochIt} ratioListForIQR") // middle quartile
        lq = listMedian(ratioListForIQR.findAll{it<mq}, "epoch ${epochIt} ratioListForIQR < median") // lower quartile
        uq = listMedian(ratioListForIQR.findAll{it>mq}, "epoch ${epochIt} ratioListForIQR > median") // upper quartile
      }
      def iqr = uq - lq // interquartile range
      def cutLo = lq - cutFactor * iqr // lower QA cut boundary
      def cutHi = uq + cutFactor * iqr // upper QA cut boundary

      System.out.println "QA CUTS: epoch ${epochIt} sector ${sectorIt}   $cutLo  $cutHi"

      cutTree[sectorIt][epochIt]['mq'] = mq
      cutTree[sectorIt][epochIt]['lq'] = lq
      cutTree[sectorIt][epochIt]['uq'] = uq
      cutTree[sectorIt][epochIt]['iqr'] = iqr
      cutTree[sectorIt][epochIt]['cutLo'] = cutLo
      cutTree[sectorIt][epochIt]['cutHi'] = cutHi
    }
  }
}
//jPrint("cuts.${dataset}.json",cutTree) // output cutTree to JSON
//println T.pPrint(cutTree)


// vars and subroutines for splitting graphs into "good" and "bad",
// i.e., "pass QA cuts" and "outside QA cuts", respectively
def grA, grA_good, grA_bad
def grN, grN_good, grN_bad
def grF, grF_good, grF_bad
def grU, grU_good, grU_bad
def grT, grT_good, grT_bad
def histA_good, histA_bad
def grDuration,  grDuration_good,  grDuration_bad
def grNumEvents, grNumEvents_good, grNumEvents_bad
def nGood, nBad
def nGoodTotal = 0
def nBadTotal  = 0
def copyTitles = { g1,g2 ->
  g2.setTitle(g1.getTitle())
  g2.setTitleX(g1.getTitleX())
  g2.setTitleY(g1.getTitleY())
}
def copyPoint = { g1,g2,i ->
  g2.addPoint(g1.getDataX(i),g1.getDataY(i),g1.getDataEX(i),g1.getDataEY(i))
}
def splitGraph = { g ->
  def gG,gB
  gG = new GraphErrors(g.getName())
  gB = new GraphErrors(g.getName()+":red")
  copyTitles(g,gG)
  copyTitles(g,gB)
  gB.setMarkerColor(2)
  return [gG,gB]
}



// define 'epoch plots', which are time-ordered concatenations of all the plots,
// and put them in the epochPlotTree
def defineEpochPlot = { name,ytitle,s,e ->
  def g = new GraphErrors("${name}_s${s}_e${e}")
  if(useFT) g.setTitle(ytitle+" vs. bin index -- epoch $e")
  else      g.setTitle(ytitle+" vs. bin index -- Sector $s, epoch $e")
  g.setTitleY(ytitle)
  g.setTitleX("Bin Index")
  return splitGraph(g) // returns list of plots ['good','bad']
}
def insertEpochPlot = { map,name,plots ->
  map.put(name+"_good",plots[0])
  map.put(name+"_bad",plots[1])
}
def electronT = useFT ? "Forward Tagger Electron" : "Trigger Electron"
sectors.each { s ->
  sectorIt = sec(s)
  if( !useFT || (useFT && sectorIt==1)) {
   ratioTree[sectorIt].each { epochIt,ratioList ->
      insertEpochPlot(epochPlotTree[sectorIt][epochIt],
        "grA",defineEpochPlot("grA_epoch","${electronT} N/F",sectorIt,epochIt))
      insertEpochPlot(epochPlotTree[sectorIt][epochIt],
        "grN",defineEpochPlot("grN_epoch","Number ${electronT}s N",sectorIt,epochIt))
      insertEpochPlot(epochPlotTree[sectorIt][epochIt],
        "grF",defineEpochPlot("grF_epoch","Gated Faraday Cup charge F [nC]",sectorIt,epochIt))
      insertEpochPlot(epochPlotTree[sectorIt][epochIt],
        "grU",defineEpochPlot("grU_epoch","Ungated Faraday Cup charge F [nC]",sectorIt,epochIt))
      insertEpochPlot(epochPlotTree[sectorIt][epochIt],
        "grT",defineEpochPlot("grT_epoch","Live Time",sectorIt,epochIt))
    }
  }
}

// define output hipo files and outliers list
def outHipoQA = new TDirectory()
def outHipoEpochs = new TDirectory()
def outHipoA = new TDirectory()
def outHipoN = new TDirectory()
def outHipoU = new TDirectory()
def outHipoF = new TDirectory()
def outHipoFA = new TDirectory()
def outHipoLTT = new TDirectory()
def outHipoLTA = new TDirectory()
def outHipoSigmaN = new TDirectory()
def outHipoSigmaF = new TDirectory()
def outHipoRhoNF = new TDirectory()

// define qaTree
def qaTree // [runnum][binnum] -> defects enumeration
def qaTreeSlurper
def jsonFile
if(qaBit>=0) {
  qaTreeSlurper = new JsonSlurper()
  jsonFile = new File("${TIMELINESRC}/qadb/qa.${dataset}/qaTree.json")
  qaTree = qaTreeSlurper.parse(jsonFile)
}
else qaTree = [:]


// define QA timeline title and name
def qaTitle, qaName
if(qaBit>=0) {
  if(qaBit==100) {
    qaTitle = ":: Fraction of bins with any defect"
    qaName = "Any_Defect"
  }
  else {
    qaTitle = ":: Fraction of bins with " + T.bitDescripts[qaBit]
    qaName = T.bitNames[qaBit]
  }
} else {
  qaTitle = ":: AUTOMATIC QA RESULT: Fraction of bins with any defect"
  qaName = "Automatic_Result"
}


// define timeline graphs
def defineTimeline = { title,ytitle,name ->
  sectors.collect { s ->
    if( !useFT || (useFT && s==0) ) {
      def gN = useFT ? "${name}_FT" : "${name}_sector_"+sec(s)
      def g = new GraphErrors(gN)
      g.setTitle(title)
      g.setTitleY(ytitle)
      g.setTitleX("Run Number")
      return g
    }
  }.findAll()
}
def TLqa = defineTimeline("${electronT} ${qaTitle}","","QA")
def TLA = defineTimeline("Number of ${electronT}s N / Faraday Cup Charge F","N/F","A")
def TLN = defineTimeline("Number of ${electronT}s N","N","N")
def TLU = defineTimeline("Ungated Faraday Cup Charge [mC]","Charge","F")
def TLF = defineTimeline("Faraday Cup Charge [mC]","Charge","F")
def TLFA = defineTimeline("Accumulated Faraday Cup Charge [mC]","Charge","F")
def TLTT = defineTimeline("Live Time, from Total Gated FC Charge / Total Ungated FC Charge","Live Time","LT")
def TLTA = defineTimeline("Live Time, from Average RUN::scaler::livetime","Live Time","LT")
def TLsigmaN = defineTimeline("${electronT} Yield sigmaN / aveN","sigmaN/aveN","sigmaN")
def TLsigmaF = defineTimeline("Faraday Cup Charge sigmaF / aveF","sigmaF/aveF","sigmaF")
def TLrhoNF = defineTimeline("Correlation Coefficient rho_{NF}","rho_{NF}","rhoNF")

def TLqaEpochs
if(useFT) {
  TLqaEpochs = new GraphErrors("epoch_FT")
  TLqaEpochs.setTitle("click the point to load graphs")
  TLqaEpochs.addPoint(1.0,1.0,0,0)
}
else {
  TLqaEpochs = new GraphErrors("epoch_sectors")
  TLqaEpochs.setTitle("choose a sector")
  TLqaEpochs.setTitleX("Sector")
  sectors.each{ TLqaEpochs.addPoint(sec(it),1.0,0,0) }
}


// other subroutines
def lineMedian, lineCutLo, lineCutHi
def elineMedian, elineCutLo, elineCutHi
def buildLine = { graph,lb,ub,name,val ->
  new F1D(graph.getName()+":"+name,Double.toString(val),lb-3,ub+3)
}
def addEpochPlotPoint = { plotOut,plotIn,i,r ->
  def f = plotIn.getDataX(i) // binnum
  def n = r + f/5000.0 // "bin index"
  plotOut.addPoint(n,plotIn.getDataY(i),0,0)
}
def writeHipo = { hipo,outList ->
  outList.each{ hipo.addDataSet(it) }
}
def prioGraph = { g, prio ->
  prioStr = prio < 10 ? "0$prio" : "$prio"
  g.setName("p${prioStr}_${g.getName()}")
}
def addGraphsToHipo = { runnum, hipoFile, sectorNum ->
  hipoFile.mkdir("/${runnum}")
  hipoFile.cd("/${runnum}")
  // organize graphs for the front-end (they will render in alphabetical order, so prefix them with p#)
  if(sectorNum==1) {
    [grNumEvents_good, grNumEvents_bad].each{prioGraph(it, 1)}
    [grDuration_good, grDuration_bad].each{prioGraph(it, 2)}
  }
  [grA_good, grA_bad, lineMedian, lineCutLo, lineCutHi].each{prioGraph(it, 2*sectorNum+1)}
  [grN_good, grN_bad].each{prioGraph(it, 2*sectorNum+2)}
  [grF_good, grF_bad].each{prioGraph(it, 15)}
  [grU_good, grU_bad].each{prioGraph(it, 16)}
  [grT_good, grT_bad].each{prioGraph(it, 17)}
  // list of graphs to include on the front-end
  writeList = [
    grA_good, grA_bad,
    grN_good, grN_bad,
    lineMedian, lineCutLo, lineCutHi
  ]
  if(sectorNum == 1) {
    writeList += [
      grNumEvents_good, grNumEvents_bad,
      grDuration_good,  grDuration_bad,
      grF_good, grF_bad,
      grU_good, grU_bad,
      grT_good, grT_bad,
    ]
  }
  writeHipo(hipoFile, writeList)
}


// subroutine for projecting a graph onto the y-axis as a histogram
def buildHisto = { graph,nbins,binmin,binmax ->

  // expand histogram range a bit so the projected histogram is padded
  def range = binmax - binmin
  binmin -= 0.05*range
  binmax += 0.05*range

  // set the histogram names and titles
  // assumes the graph name is 'gr._.*' (regex syntax) and names the histogram 'gr.h_.*'
  def histN = graph.getName().replaceAll(/^gr./) { graph.getName().replaceAll(/_.*$/,"h") }
  def histT = graph.getTitle().replaceAll(/vs\. time bin/,"distribution")

  // define histogram and set formatting
  def hist = new H1F(histN,histT,nbins,binmin,binmax)
  hist.setTitleX(graph.getTitleY())
  hist.setLineColor(graph.getMarkerColor())

  // project the graph and return the histogram
  graph.getDataSize(0).times { i -> hist.fill(graph.getDataY(i)) }
  return hist
}


// subroutines for calculating means and variances of lists
def listMean = { valList, wgtList ->
  def numer = 0.0
  def denom = 0.0
  valList.eachWithIndex { val,i ->
    numer += wgtList[i] * val
    denom += wgtList[i]
  }
  return denom>0 ? numer/denom : 0
}
def listCovar = { Alist, Blist, wgtList, muA, muB ->
  def numer = 0.0
  def denom = 0.0
  Alist.size().times { i ->
    numer += wgtList[i] * (Alist[i]-muA) * (Blist[i]-muB)
    denom += wgtList[i]
  }
  return denom>0 ? numer/denom : 0
}
def listVar = { valList, wgtList, mu ->
  return listCovar(valList,valList,wgtList,mu,mu)
}


// subroutine to convert a graph into a list of values
def graph2list = { graph ->
  def lst = []
  graph.getDataSize(0).times { i -> lst.add(graph.getDataY(i)) }
  return lst
}

// loop over runs, apply the QA cuts, and fill 'good' and 'bad' graphs
def totFacc = sectors.collect{0}
inList.each { obj ->
  if(obj.contains("/grA_")) {

    // get runnum, sector, epoch
    def (runnum,sector) = obj.tokenize('_').subList(1,3).collect{ it.toInteger() }
    def epoch = getEpoch(runnum,sector)
    if(qaBit<0 && !qaTree.containsKey(runnum)) qaTree[runnum] = [:]

    // if using the FT, only loop over sector 1 (no sectors-dependence for FT)
    if( !useFT || (useFT && sector==1)) {

      // define timebin characterizing graphs
      if(sector==1) {
        grDuration = new GraphErrors("grDuration_${runnum}")
        grDuration.setTitle("Duration vs. Time Bin")
        grDuration.setTitleX("Time Bin")
        grDuration.setTitleY("Duration [s]")
        grNumEvents = new GraphErrors("grNumEvents_${runnum}")
        grNumEvents.setTitle("Number of Events vs. Time Bin")
        grNumEvents.setTitleX("Time Bin")
        grNumEvents.setTitleY("Number of Events")
      }

      // get all the graphs from `inTdir` and convert to value lists
      grA = inTdir.getObject(obj)
      grN = inTdir.getObject(obj.replaceAll("grA","grN"))
      grF = inTdir.getObject(obj.replaceAll("grA","grF"))
      grU = inTdir.getObject(obj.replaceAll("grA","grU"))
      grT = inTdir.getObject(obj.replaceAll("grA","grT"))
      def listA = graph2list(grA)
      def listN = graph2list(grN)
      def listF = graph2list(grF)
      def listU = graph2list(grU)
      def listT = graph2list(grT)
      def listOne = []
      listA.size().times{listOne<<1}

      // decide whether to enable livetime weighting
      def listWgt = listOne // disable
      // def listWgt = listT // enable

      // get totals
      def totN = listN.sum()
      def totF = listF.sum()
      def totU = listU.sum()
      def totA = totF > 0 ? totN / totF : 0

      // compute livetime
      def totLT = totU > 0 ? totF / totU : 0 // from total FC charge
      def aveLT = listT.size()>0 ? listT.sum() / listT.size() : 0 // average livetime for the run

      // accumulated charge (units converted nC -> mC)
      // - should be same for all sectors
      totFacc[sector-1] += totF/1e6 // (same for all sectors)

      // get mean, quartiles, and variance of N and F
      def muN  = listMean(listN,listWgt)
      def muF  = listMean(listF,listWgt)
      def varN = listVar(listN,listWgt,muN)
      def varF = listVar(listF,listWgt,muF)
      def mqF  = listMedian(listF, 'listF') // median (middle quartile)
      def lqF  = listMedian(listF.findAll{it<mqF}, 'listF < median') // lower quartile
      def uqF  = listMedian(listF.findAll{it>mqF}, 'listF > median') // upper quartile
      //// sometimes listN is full of a bunch of zeroes, so the IQR method won't work; in this case,
      //// set the upper and lower quartiles to zero, so the IQR is also zero, and set bad_IQR_N=true
      def mqN  = listMedian(listN, 'listN') // median (middle quartile)
      def listNlq = listN.findAll{it<mqN}
      def listNuq = listN.findAll{it>mqN}
      def lqN = 0.0
      def uqN = 0.0
      def bad_IQR_N = false
      if(listNlq.size() > 0 && listNuq.size() > 0) {
        lqN = listMedian(listNlq, 'listN < median') // lower quartile
        uqN = listMedian(listNuq, 'listN > median') // upper quartile
      }
      else {
        bad_IQR_N = true
      }

      // use IQR rule to define ranges where N and F are consistent (cf. cutLo and cutHi, which apply to N/F)
      def iqrN       = uqN - lqN
      def iqrF       = uqF - lqF
      def inRangeN   = { scale_factor -> [ lqN - scale_factor * iqrN, uqN + scale_factor * iqrN ] }
      def inRangeF   = { scale_factor -> [ lqF - scale_factor * iqrF, uqF + scale_factor * iqrF ] }

      // calculate Pearson correlation coefficient
      def covarNF = listCovar(listN,listF,listWgt,muN,muF)
      def corrNF = covarNF / (varN*varF)

      // calculate uncertainties of N and F relative to the mean
      def reluncN = Math.sqrt(varN) / muN
      def reluncF = Math.sqrt(varF) / muF

      // assign Poisson statistics error bars to graphs of N, F, and N/F
      // - note that N/F error uses Pearson correlation determined from the full run's
      //   covariance(N,F)
      grA.getDataSize(0).times { i ->
        def valN = grN.getDataY(i)
        def valF = grF.getDataY(i)
        def valU = grU.getDataY(i)
        grN.setError(i,0,Math.sqrt(valN))
        grF.setError(i,0,Math.sqrt(valF))
        grU.setError(i,0,Math.sqrt(valU))
        grA.setError(i,0,
          (valN/valF) * Math.sqrt(
            1/valN + 1/valF - 2 * corrNF * Math.sqrt(valN*valF) / (valN*valF)
          )
        )
      }


      // split graphs into good and bad
      (grA_good,grA_bad) = splitGraph(grA)
      (grN_good,grN_bad) = splitGraph(grN)
      (grF_good,grF_bad) = splitGraph(grF)
      (grU_good,grU_bad) = splitGraph(grU)
      (grT_good,grT_bad) = splitGraph(grT)
      (grDuration_good,grDuration_bad)   = splitGraph(grDuration)
      (grNumEvents_good,grNumEvents_bad) = splitGraph(grNumEvents)

      // get the first and last bins' binnums
      def firstBinnum = grA.getDataX(0).toInteger()
      def lastBinnum  = grA.getDataX(grA.getDataSize(0)-1).toInteger()

      // loop through points in grA and fill good and bad graphs
      grA.getDataSize(0).times { i ->

        def binnum       = grA.getDataX(i).toInteger()
        def evnumMin     = evnumTree[runnum][binnum]['evnumMin']
        def evnumMax     = evnumTree[runnum][binnum]['evnumMax']
        def timestampMin = evnumTree[runnum][binnum]['timestampMin']
        def timestampMax = evnumTree[runnum][binnum]['timestampMax']

        // DETERMINE DEFECT BITS, or load them from modified qaTree.json
        def badbin = false

        // calculate the number of events in this timebin
        def numEvents = evnumMax - evnumMin
        if(binnum == 0) { numEvents++ } // first bin has no lower bound, so need one more

        // fill `grDuration` and `grNumEvents`
        if(sector == 1) {
          def duration = (timestampMax - timestampMin) * 4e-9 // convert timestamp [4ns] -> [s]
          grDuration.addPoint(binnum,  duration,  0, 0)
          grNumEvents.addPoint(binnum, numEvents, 0, 0)
        }

        // fill `qaTree`
        if(qaBit<0) {
          if(!qaTree[runnum].containsKey(binnum)) {
            qaTree[runnum][binnum]                  = [:]
            qaTree[runnum][binnum]['evnumMin']      = evnumMin
            qaTree[runnum][binnum]['evnumMax']      = evnumMax
            qaTree[runnum][binnum]['timestampMin']  = timestampMin
            qaTree[runnum][binnum]['timestampMax']  = timestampMax
            qaTree[runnum][binnum]['comment']       = ""
            qaTree[runnum][binnum]['defect']        = 0
            qaTree[runnum][binnum]['sectorDefects'] = sectors.collectEntries{s->[sec(s),[]]}
          }

          // get variables needed for checking for defects
          def Nval   = grN.getDataY(i)
          def Fval   = grF.getDataY(i)
          def NFval  = grA.getDataY(i)
          def NFerrH = NFval + grA.getDataEY(i)
          def NFerrL = NFval - grA.getDataEY(i)
          def cutLo  = cutTree[sector][epoch]['cutLo']
          def cutHi  = cutTree[sector][epoch]['cutHi']
          def LTval  = grT.getDataY(i)

          def defectList = []

          // set FD and FT outlier bits
          if( NFval<cutLo || NFval>cutHi ) {
            if( NFerrH>cutLo && NFerrL<cutHi ) {
              defectList.add(T.bit("MarginalOutlier${useFT?"FT":""}"))
            } else if( i==0 || i+1==grA.getDataSize(0) ) {
              defectList.add(T.bit("TerminalOutlier${useFT?"FT":""}"))
            } else {
              defectList.add(T.bit("TotalOutlier${useFT?"FT":""}"))
            }
          }

          // set bits which need only the FD (don't do it again for the FT):
          if(!useFT) {

            // set livetime bit
            if( LTval < cutDef(["LowLiveTime", "min_live_time"]) ) {
              defectList.add(T.bit("LowLiveTime"))
            }

            // set FC bits
            if( binnum == firstBinnum || binnum == lastBinnum ) { // FC charge cannot be known for the first or last bin
              defectList.add(T.bit("ChargeUnknown"))
            }
            else if(Fval > inRangeF(cutDef(["ChargeHigh","IQR_cut_factor"]))[1]) {
              defectList.add(T.bit("ChargeHigh"))
            }
            else if(Fval < 0) {
              defectList.add(T.bit("ChargeNegative"))
            }

            // set no-beam bit:
            // - don't bother doing this for first or last bins since their charge is unknown
            // - numEvents is low
            // - both N and F are near zero
            if(binnum != firstBinnum && binnum != lastBinnum &&
              numEvents < cutDef(["PossiblyNoBeam","max_num_events"]) &&
              Nval < cutDef(["PossiblyNoBeam","max_num_electrons"]) &&
              Fval < cutDef(["PossiblyNoBeam","max_FC_charge"]) /*nC*/
            ) {
              defectList.add(T.bit("PossiblyNoBeam"))
            }
          }

          // insert in qaTree
          qaTree[runnum][binnum]['sectorDefects'][useFT ? 1 : sector] = defectList.collect()
          badbin = defectList.size() > 0
        }
        else {
          // lookup defectList for this sector
          if(qaBit==100) { // bad if not perfect
            badbin = qaTree["$runnum"]["$binnum"]['sectorDefects']["$sector"].size() > 0
          } else { // bad only if defectList includes qaBit
            if(qaTree["$runnum"]["$binnum"]['sectorDefects']["$sector"].size()>0) {
              badbin = qaBit in qaTree["$runnum"]["$binnum"]['sectorDefects']["$sector"]
            }
          }
        }

        // send points to "good" or "bad" graphs
        if(badbin) {
          copyPoint(grA,grA_bad,i)
          copyPoint(grN,grN_bad,i)
          copyPoint(grF,grF_bad,i)
          copyPoint(grU,grU_bad,i)
          copyPoint(grT,grT_bad,i)
          copyPoint(grDuration,grDuration_bad,i)
          copyPoint(grNumEvents,grNumEvents_bad,i)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grA_bad'],grA,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grN_bad'],grN,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grF_bad'],grF,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grU_bad'],grU,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grT_bad'],grT,i,runnum)
        } else {
          copyPoint(grA,grA_good,i)
          copyPoint(grN,grN_good,i)
          copyPoint(grF,grF_good,i)
          copyPoint(grU,grU_good,i)
          copyPoint(grT,grT_good,i)
          copyPoint(grDuration,grDuration_good,i)
          copyPoint(grNumEvents,grNumEvents_good,i)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grA_good'],grA,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grN_good'],grN,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grF_good'],grF,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grU_good'],grU,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grT_good'],grT,i,runnum)
        }
      }

      // fill histograms
      histA_good = buildHisto(grA_good,250,minA,maxA)
      histA_bad = buildHisto(grA_bad,250,minA,maxA)

      // define lines
      def lowerBound = grA.getDataX(0)
      def upperBound = grA.getDataX(grA.getDataSize(0)-1)
      lineMedian = buildLine(
        grA,lowerBound,upperBound,"median",cutTree[sector][epoch]['mq'])
      lineCutLo = buildLine(
        grA,lowerBound,upperBound,"cutLo",cutTree[sector][epoch]['cutLo'])
      lineCutHi = buildLine(
        grA,lowerBound,upperBound,"cutHi",cutTree[sector][epoch]['cutHi'])

      // write graphs to hipo file
      addGraphsToHipo(runnum, outHipoQA, sector)
      addGraphsToHipo(runnum, outHipoA, sector)
      addGraphsToHipo(runnum, outHipoN, sector)
      addGraphsToHipo(runnum, outHipoU, sector)
      addGraphsToHipo(runnum, outHipoF, sector)
      addGraphsToHipo(runnum, outHipoFA, sector)
      addGraphsToHipo(runnum, outHipoLTT, sector)
      addGraphsToHipo(runnum, outHipoLTA, sector)
      addGraphsToHipo(runnum, outHipoSigmaN, sector)
      addGraphsToHipo(runnum, outHipoSigmaF, sector)
      addGraphsToHipo(runnum, outHipoRhoNF, sector)

      // fill timeline points
      nGood = grA_good.getDataSize(0)
      nBad = grA_bad.getDataSize(0)
      nGoodTotal += nGood
      nBadTotal += nBad
      TLqa[sector-1].addPoint(
        runnum,
        nGood+nBad>0 ? nBad/(nGood+nBad) : 0,
        0,0
      )
      TLA[sector-1].addPoint(runnum,totA,0,0)
      TLN[sector-1].addPoint(runnum,totN,0,0)
      TLU[sector-1].addPoint(runnum,totU/1e6,0,0) // (converted nC->mC)
      TLF[sector-1].addPoint(runnum,totF/1e6,0,0) // (converted nC->mC)
      TLFA[sector-1].addPoint(runnum,totFacc[sector-1],0,0)
      TLTT[sector-1].addPoint(runnum,totLT,0,0)
      TLTA[sector-1].addPoint(runnum,aveLT,0,0)
      TLsigmaN[sector-1].addPoint(runnum,reluncN,0,0)
      TLsigmaF[sector-1].addPoint(runnum,reluncF,0,0)
      TLrhoNF[sector-1].addPoint(runnum,corrNF,0,0)
    }
  }
}


// assign defect masks
qaTree.each { qaRun, qaRunTree ->
  qaRunTree.each { qaBin, qaBinTree ->
    def defList = []
    def defMask = 0
    qaBinTree["sectorDefects"].each { qaSec, qaDefList ->
      defList += qaDefList.collect{it.toInteger()}
    }
    defList.unique().each { defMask += (0x1<<it) }
    qaTree[qaRun][qaBin]["defect"] = defMask
  }
}


// write epoch plots to hipo file
sectors.each { s ->
  sectorIt = sec(s)

  if( !useFT || (useFT && sectorIt==1)) {
    outHipoEpochs.mkdir("/${sectorIt}")
    outHipoEpochs.cd("/${sectorIt}")
    epochPlotTree[sectorIt].each { epochIt,map ->

      def elowerBound, eupperBound
      epochFile.eachLine { line,i ->
        if(i==epochIt) (elowerBound,eupperBound) = line.tokenize(' ')[0,1].collect{it.toInteger()}
      }
      elineMedian = buildLine(
        map['grA_good'],elowerBound,eupperBound,"median",cutTree[sectorIt][epochIt]['mq'])
      elineCutLo = buildLine(
        map['grA_good'],elowerBound,eupperBound,"cutLo",cutTree[sectorIt][epochIt]['cutLo'])
      elineCutHi = buildLine(
        map['grA_good'],elowerBound,eupperBound,"cutHi",cutTree[sectorIt][epochIt]['cutHi'])

      histA_good = buildHisto(map['grA_good'],500,minA,maxA)
      histA_bad = buildHisto(map['grA_bad'],500,minA,maxA)

      writeHipo(outHipoEpochs,map.values())
      writeHipo(outHipoEpochs,[histA_good,histA_bad])
      writeHipo(outHipoEpochs,[elineMedian,elineCutLo,elineCutHi])
    }
  }
}



// write timelines to output hipo files
def electronN
def writeTimeline (tdir,timeline,title,once=false) {
  tdir.mkdir("/timelines")
  tdir.cd("/timelines")
  if(once) {
    def name = timeline[0].getName().replaceAll(/_sector.*$/,"")
    timeline[0].setName(name)
    tdir.addDataSet(timeline[0])
  }
  else {
    timeline.each { tdir.addDataSet(it) }
  }
  def outHipoName = "${inDir}/outmon/${title}.hipo"
  File outHipoFile = new File(outHipoName)
  if(outHipoFile.exists()) outHipoFile.delete()
  tdir.writeFile(outHipoName)
}

electronN = "electron_" + (useFT ? "FT" : "FD")
writeTimeline(outHipoQA,TLqa,"${electronN}_yield_QA_${qaName}",useFT)
writeTimeline(outHipoA,TLA,"${electronN}_normalized_yield",useFT)
writeTimeline(outHipoN,TLN,"${electronN}_yield_values",useFT)
writeTimeline(outHipoSigmaN,TLsigmaN,"${electronN}_yield_stddev",useFT)
if(!useFT) {
  writeTimeline(outHipoU,TLU,"faraday_cup_charge_ungated",true)
  writeTimeline(outHipoF,TLF,"faraday_cup_charge_gated",true)
  writeTimeline(outHipoFA,TLFA,"faraday_cup_charge_gated_accumulated",true)
  writeTimeline(outHipoLTT,TLTT,"live_time_from_fc_charge_totals",true)
  writeTimeline(outHipoLTA,TLTA,"live_time_average",true)
  writeTimeline(outHipoSigmaF,TLsigmaF,"faraday_cup_stddev",true)
}
//writeTimeline(outHipoRhoNF,TLrhoNF,"faraday_cup_vs_${electronN}_yield_correlation",true)

outHipoEpochs.mkdir("/timelines")
outHipoEpochs.cd("/timelines")
outHipoEpochs.addDataSet(TLqaEpochs)
outHipoName = "${inDir}/outmon/${electronN}_yield_QA_epoch_view.hipo"
File outHipoEpochsFile = new File(outHipoName)
if(outHipoEpochsFile.exists()) outHipoEpochsFile.delete()
outHipoEpochs.writeFile(outHipoName)


// sort qaTree and output to json file
//println T.pPrint(qaTree)
qaTree.each { qaRun, qaRunTree -> qaRunTree.sort{it.key.toInteger()} }
qaTree.sort()
new File("${inDir}/outdat/qaTree"+(useFT?"FT":"FD")+".json").write(JsonOutput.toJson(qaTree))


// print total QA passing fractions
def PF = nGoodTotal / (nGoodTotal+nBadTotal)
def FF = 1-PF
if(qaBit<0) println "\nQA cut overall passing fraction: $PF"
else {
  def PFfile = new File("${inDir}/outdat/passFractions.dat")
  def PFfileWriter = PFfile.newWriter(qaBit>0?true:false)
  def PFstr = qaBit==100 ? "Fraction of golden bins (no defects): $PF" :
                           "Fraction of bins with "+T.bitDescripts[qaBit]+": $FF"
  PFfileWriter << PFstr << "\n"
  PFfileWriter.close()
}

