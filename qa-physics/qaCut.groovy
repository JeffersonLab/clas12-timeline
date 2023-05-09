import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import Tools
Tools T = new Tools()

//--------------------------------------------------------------------------
// ARGUMENTS:
dataset = 'inbending1'
useFT = false // if true, use FT electrons instead
qaBit = -1 // if positive, produce QA timeline based on QA/qa.${dataset}/qaTree.json
if(args.length>=1) dataset = args[0]
if(args.length>=2) useFT = (args[1]=="FT") ? true : false
if(args.length>=3) qaBit = args[2].toInteger()
//--------------------------------------------------------------------------

// vars and subroutines
def sectors = 0..<6
def sec = { int i -> i+1 }
def runnum, filenum, sector, epoch, evnumMin, evnumMax
def gr
def jPrint = { name,object -> new File(name).write(JsonOutput.toJson(object)) }


// read epochs list file
def epochFile = new File("epochs/epochs.${dataset}.txt")
if(!(epochFile.exists())) {
  System.err << "WARNING: using epochs/epochs.default.txt\n"
  epochFile = new File("epochs/epochs.default.txt")
}

def getEpoch = { r,s ->
  //return 1 // (for testing single-epoch mode)
  def lb,ub
  def e = -1
  epochFile.eachLine { line,i ->
    (lb,ub) = line.tokenize(' ').collect{it.toInteger()}
    if(r>=lb && r<=ub) e=i
  }
  if(e<0) throw new Exception("run $r sector $s has unknown epoch")
  return e
}
def lowerBound, upperBound
def getEpochBounds = { e ->
  epochFile.eachLine { line,i ->
    if(i==e) (lowerBound,upperBound) = line.tokenize(' ').collect{it.toInteger()}
  }
}


// build map of (runnum,filenum) -> (evnumMin,evnumMax)
def dataFile = new File("outdat.${dataset}/data_table.dat")
def tok
def evnumTree = [:]
if(!(dataFile.exists())) throw new Exception("data_table.dat not found")
dataFile.eachLine { line ->
  tok = line.tokenize(' ')
  runnum = tok[0].toInteger()
  filenum = tok[1].toInteger()
  evnumMin = tok[2].toInteger()
  evnumMax = tok[3].toInteger()
  if(!evnumTree.containsKey(runnum))
    evnumTree[runnum] = [:]
  if(!evnumTree[runnum].containsKey(filenum)) {
    evnumTree[runnum][filenum] = [
      "evnumMin":evnumMin,
      "evnumMax":evnumMax
    ]
  }
}


// open hipo file
def inTdir = new TDirectory()
inTdir.readFile("outmon.${dataset}/monitorElec"+(useFT?"FT":"")+".hipo")
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


// loop over 'grA' graphs (of N/F vs. filenum), filling ratioTree leaves
def (minA,maxA) = [100000,0]
inList.each { obj ->
  if(obj.contains("/grA_")) {

    // get runnum and sector
    (runnum,sector) = obj.tokenize('_').subList(1,3).collect{ it.toInteger() }
    if(sector<1||sector>6) throw new Exception("bad sector number $sector")

    // get epoch num, then initialize epoch branch if needed
    epoch = getEpoch(runnum,sector)
    if(ratioTree[sector][epoch]==null) {
      ratioTree[sector].put(epoch,[])
      cutTree[sector].put(epoch,[:])
      epochPlotTree[sector].put(epoch,[:])
    }

    // append N/F values to the list associated to this (sector,epoch)
    // also determine minimum and maximum values of N/F
    gr = inTdir.getObject(obj)
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
def median = { d ->
  if(d.size()==0) {
    System.err << "WARNING: attempt to calculate median of an empty list\n"
    return -10000
  }
  d.sort()
  def m = d.size().intdiv(2)
  d.size() % 2 ? d[m] : (d[m-1]+d[m]) / 2
}


// establish cut lines using 'cutFactor' x IQR method, and fill cutTree
// - note: for the FT electrons, it seems that N/F has a long tail toward
//   lower values, so cutLo is forced to be lower 
def cutFactor = 4.0
def mq,lq,uq,iqr,cutLo,cutHi
sectors.each { s ->
  sectorIt = sec(s)
  ratioTree[sectorIt].each { epochIt,ratioList ->

    mq = median(ratioList) // middle quartile
    lq = median(ratioList.findAll{it<mq}) // lower quartile
    uq = median(ratioList.findAll{it>mq}) // upper quartile
    iqr = uq - lq // interquartile range
    cutLo = lq - cutFactor * iqr // lower QA cut boundary
    cutHi = uq + cutFactor * iqr // upper QA cut boundary

    if(useFT) cutLo = lq - 2 * cutFactor * iqr // override FT low cut to be lower
    
    cutTree[sectorIt][epochIt]['mq'] = mq
    cutTree[sectorIt][epochIt]['lq'] = lq
    cutTree[sectorIt][epochIt]['uq'] = uq
    cutTree[sectorIt][epochIt]['iqr'] = iqr
    cutTree[sectorIt][epochIt]['cutLo'] = cutLo
    cutTree[sectorIt][epochIt]['cutHi'] = cutHi
  }
}
//jPrint("cuts.${dataset}.json",cutTree) // output cutTree to JSON
//println T.pPrint(cutTree)


// vars and subroutines for splitting graphs into "good" and "bad", 
// i.e., "pass QA cuts" and "outside QA cuts", respectively
def grA,grA_good,grA_bad
def grN,grN_good,grN_bad
def grF,grF_good,grF_bad
def grT,grT_good,grT_bad
def histA_good, histA_bad
def nGood,nBad
def nGoodTotal = 0
def nBadTotal = 0
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
  if(useFT) g.setTitle(ytitle+" vs. file index -- epoch $e")
  else      g.setTitle(ytitle+" vs. file index -- Sector $s, epoch $e")
  g.setTitleY(ytitle)
  g.setTitleX("file index")
  return splitGraph(g) // returns list of plots ['good','bad']
}
def insertEpochPlot = { map,name,plots ->
  map.put(name+"_good",plots[0])
  map.put(name+"_bad",plots[1])
}
def electronT = useFT ? "Forward Tagger Electron" : "Trigger Electron"
sectors.each { s ->
  sectorIt = sec(s)
  ratioTree[sectorIt].each { epochIt,ratioList ->
    insertEpochPlot(epochPlotTree[sectorIt][epochIt],
      "grA",defineEpochPlot("grA_epoch","${electronT} N/F",sectorIt,epochIt))
    insertEpochPlot(epochPlotTree[sectorIt][epochIt],
      "grN",defineEpochPlot("grN_epoch","Number ${electronT}s N",sectorIt,epochIt))
    insertEpochPlot(epochPlotTree[sectorIt][epochIt],
      "grF",defineEpochPlot("grF_epoch","Faraday cup charge F [nC]",sectorIt,epochIt))
    insertEpochPlot(epochPlotTree[sectorIt][epochIt],
      "grT",defineEpochPlot("grT_epoch","Live Time",sectorIt,epochIt))
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
def outHipoT = new TDirectory()
def outHipoSigmaN = new TDirectory()
def outHipoSigmaF = new TDirectory()
def outHipoRhoNF = new TDirectory()

// define qaTree
def qaTree // [runnum][filenum] -> defects enumeration
def slurper
def jsonFile
if(qaBit>=0) {
  slurper = new JsonSlurper()
  jsonFile = new File("QA/qa.${dataset}/qaTree.json")
  qaTree = slurper.parse(jsonFile)
}
else qaTree = [:]


// define QA timeline title and name
def qaTitle, qaName
if(qaBit>=0) {
  if(qaBit==100) {
    qaTitle = ":: Fraction of files with any defect"
    qaName = "Any_Defect"
  }
  else {
    qaTitle = ":: Fraction of files with " + T.bitDescripts[qaBit]
    qaName = T.bitNames[qaBit]
  }
} else {
  qaTitle = ":: AUTOMATIC QA RESULT: Fraction of files with any defect"
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
      g.setTitleX("run number")
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
def TLT = defineTimeline("Live Time","Live Time","LT")
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
  TLqaEpochs.setTitleX("sector")
  sectors.each{ TLqaEpochs.addPoint(sec(it),1.0,0,0) }
}


// other subroutines
def lineMedian, lineCutLo, lineCutHi
def elineMedian, elineCutLo, elineCutHi
def buildLine = { graph,lb,ub,name,val ->
  new F1D(graph.getName()+":"+name,Double.toString(val),lb-3,ub+3)
}
def addEpochPlotPoint = { plotOut,plotIn,i,r ->
  def f = plotIn.getDataX(i) // filenum
  def n = r + f/5000.0 // "file index"
  plotOut.addPoint(n,plotIn.getDataY(i),0,0)
}
def writeHipo = { hipo,outList -> outList.each{ hipo.addDataSet(it) } }
def addGraphsToHipo = { hipoFile ->
  hipoFile.mkdir("/${runnum}")
  hipoFile.cd("/${runnum}")
  writeHipo(
    hipoFile,
    [
      grA_good,grA_bad,
      grN_good,grN_bad,
      grF_good,grF_bad,
      grT_good,grT_bad,
      histA_good,histA_bad,
      lineMedian, lineCutLo, lineCutHi
    ]
  )
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
  def histT = graph.getTitle().replaceAll(/vs\. file number/,"distribution")

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
def listA, listN, listF, listT, listOne, listWgt
def graph2list = { graph ->
  def lst = []
  graph.getDataSize(0).times { i -> lst.add(graph.getDataY(i)) }
  return lst
}
  
// loop over runs, apply the QA cuts, and fill 'good' and 'bad' graphs
def muN, muF
def varN, varF 
def totN, totF, totA, totU, totT
def totFacc = sectors.collect{0}
def reluncN, reluncF
def NF,NFerrH,NFerrL,LT
def valN,valF,valA
def defectList = []
def badfile
inList.each { obj ->
  if(obj.contains("/grA_")) {

    // get runnum, sector, epoch
    (runnum,sector) = obj.tokenize('_').subList(1,3).collect{ it.toInteger() }
    epoch = getEpoch(runnum,sector)
    if(qaBit<0 && !qaTree.containsKey(runnum)) qaTree[runnum] = [:]

    // if using the FT, only loop over sector 1 (no sectors-dependence for FT)
    if( !useFT || (useFT && sector==1)) {

      // get all the graphs and convert to value lists
      grA = inTdir.getObject(obj)
      grN = inTdir.getObject(obj.replaceAll("grA","grN"))
      grF = inTdir.getObject(obj.replaceAll("grA","grF"))
      grT = inTdir.getObject(obj.replaceAll("grA","grT"))
      listA = graph2list(grA)
      listN = graph2list(grN)
      listF = graph2list(grF)
      listT = graph2list(grT)
      listOne = []
      listA.size().times{listOne<<1}

      // decide whether to enable livetime weighting
      listWgt = listOne // disable
      //listWgt = listT // enable

      // get totals
      totN = listN.sum()
      totF = listF.sum()
      totA = totN/totF
      totU = 0
      listF.size().times{ totU += listF[it] / listT[it] }
      totT = totF / totU

      // accumulated charge (units converted nC -> mC)
      // - should be same for all sectors
      totFacc[sector-1] += totF/1e6 // (same for all sectors)

      // get mean, and variance of N and F
      muN = listMean(listN,listWgt)
      muF = listMean(listF,listWgt)
      varN = listVar(listN,listWgt,muN)
      varF = listVar(listF,listWgt,muF)

      // calculate Pearson correlation coefficient
      covarNF = listCovar(listN,listF,listWgt,muN,muF)
      corrNF = covarNF / (varN*varF)

      // calculate uncertainties of N and F relative to the mean
      reluncN = Math.sqrt(varN) / muN
      reluncF = Math.sqrt(varF) / muF

      // assign Poisson statistics error bars to graphs of N, F, and N/F
      // - note that N/F error uses Pearson correlation determined from the full run's 
      //   covariance(N,F)
      grA.getDataSize(0).times { i ->
        valN = grN.getDataY(i)
        valF = grF.getDataY(i)
        grN.setError(i,0,Math.sqrt(valN))
        grF.setError(i,0,Math.sqrt(valF))
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
      (grT_good,grT_bad) = splitGraph(grT)

      // loop through points in grA and fill good and bad graphs
      grA.getDataSize(0).times { i -> 

        filenum = grA.getDataX(i).toInteger()

        // DETERMINE DEFECT BITS, or load them from modified qaTree.json
        badfile = false
        if(qaBit<0) {

          if(!qaTree[runnum].containsKey(filenum)) {
            qaTree[runnum][filenum] = [:]
            qaTree[runnum][filenum]['evnumMin'] = evnumTree[runnum][filenum]['evnumMin']
            qaTree[runnum][filenum]['evnumMax'] = evnumTree[runnum][filenum]['evnumMax']
            qaTree[runnum][filenum]['comment'] = ""
            qaTree[runnum][filenum]['defect'] = 0
            qaTree[runnum][filenum]['sectorDefects'] = sectors.collectEntries{s->[sec(s),[]]}
          }

          // get variables needed for checking for defects
          NF = grA.getDataY(i)
          NFerrH = NF + grA.getDataEY(i)
          NFerrL = NF - grA.getDataEY(i)
          cutLo = cutTree[sector][epoch]['cutLo']
          cutHi = cutTree[sector][epoch]['cutHi']
          LT = grT.getDataY(i)

          defectList = []
          // set outlier bit
          if( NF<cutLo || NF>cutHi ) {
            if( NFerrH>cutLo && NFerrL<cutHi ) {
              defectList.add(T.bit("MarginalOutlier"))
            } else if( i==0 || i+1==grA.getDataSize(0) ) {
              defectList.add(T.bit("TerminalOutlier"))
            } else {
              defectList.add(T.bit("TotalOutlier"))
            }
          }
          // set FC bit
          if( LT<0.9 ) defectList.add(T.bit("LowLiveTime"))

          // insert in qaTree
          qaTree[runnum][filenum]['sectorDefects'][sector] = defectList.collect()
          badfile = defectList.size() > 0
        }
        else {
          // lookup defectList for this sector
          if(qaBit==100) { // bad if not perfect
            badfile = qaTree["$runnum"]["$filenum"]['sectorDefects']["$sector"].size() > 0
          } else { // bad only if defectList includes qaBit
            if(qaTree["$runnum"]["$filenum"]['sectorDefects']["$sector"].size()>0) {
              badfile = qaBit in qaTree["$runnum"]["$filenum"]['sectorDefects']["$sector"]
            }
          }
        }

        // send points to "good" or "bad" graphs
        if(badfile) {
          copyPoint(grA,grA_bad,i)
          copyPoint(grN,grN_bad,i)
          copyPoint(grF,grF_bad,i)
          copyPoint(grT,grT_bad,i)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grA_bad'],grA,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grN_bad'],grN,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grF_bad'],grF,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grT_bad'],grT,i,runnum)
        } else {
          copyPoint(grA,grA_good,i)
          copyPoint(grN,grN_good,i)
          copyPoint(grF,grF_good,i)
          copyPoint(grT,grT_good,i)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grA_good'],grA,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grN_good'],grN,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grF_good'],grF,i,runnum)
          addEpochPlotPoint(epochPlotTree[sector][epoch]['grT_good'],grT,i,runnum)
        }
      }

      // fill histograms
      histA_good = buildHisto(grA_good,250,minA,maxA)
      histA_bad = buildHisto(grA_bad,250,minA,maxA)

      // define lines
      lowerBound = grA.getDataX(0)
      upperBound = grA.getDataX(grA.getDataSize(0)-1)
      lineMedian = buildLine(
        grA,lowerBound,upperBound,"median",cutTree[sector][epoch]['mq'])
      lineCutLo = buildLine(
        grA,lowerBound,upperBound,"cutLo",cutTree[sector][epoch]['cutLo'])
      lineCutHi = buildLine(
        grA,lowerBound,upperBound,"cutHi",cutTree[sector][epoch]['cutHi'])

      // write graphs to hipo file
      addGraphsToHipo(outHipoQA)
      addGraphsToHipo(outHipoA)
      addGraphsToHipo(outHipoN)
      addGraphsToHipo(outHipoU)
      addGraphsToHipo(outHipoF)
      addGraphsToHipo(outHipoFA)
      addGraphsToHipo(outHipoT)
      addGraphsToHipo(outHipoSigmaN)
      addGraphsToHipo(outHipoSigmaF)
      addGraphsToHipo(outHipoRhoNF)

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
      TLT[sector-1].addPoint(runnum,totT,0,0)
      TLsigmaN[sector-1].addPoint(runnum,reluncN,0,0)
      TLsigmaF[sector-1].addPoint(runnum,reluncF,0,0)
      TLrhoNF[sector-1].addPoint(runnum,corrNF,0,0)
    }
  }
}


// assign defect masks
qaTree.each { qaRun, qaRunTree -> 
  qaRunTree.each { qaFile, qaFileTree ->
    def defList = []
    def defMask = 0
    qaFileTree["sectorDefects"].each { qaSec, qaDefList ->
      defList += qaDefList.collect{it.toInteger()}
    }
    defList.unique().each { defMask += (0x1<<it) }
    qaTree[qaRun][qaFile]["defect"] = defMask
  }
}


// write epoch plots to hipo file
sectors.each { s ->
  sectorIt = sec(s)

  if( !useFT || (useFT && sectorIt==1)) {
    outHipoEpochs.mkdir("/${sectorIt}")
    outHipoEpochs.cd("/${sectorIt}")
    epochPlotTree[sectorIt].each { epochIt,map ->

      getEpochBounds(epochIt) // sets lower(upper)Bound
      elineMedian = buildLine(
        map['grA_good'],lowerBound,upperBound,"median",cutTree[sectorIt][epochIt]['mq'])
      elineCutLo = buildLine(
        map['grA_good'],lowerBound,upperBound,"cutLo",cutTree[sectorIt][epochIt]['cutLo'])
      elineCutHi = buildLine(
        map['grA_good'],lowerBound,upperBound,"cutHi",cutTree[sectorIt][epochIt]['cutHi'])

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
  def outHipoName = "outmon.${dataset}/${title}.hipo"
  File outHipoFile = new File(outHipoName)
  if(outHipoFile.exists()) outHipoFile.delete()
  tdir.writeFile(outHipoName)
}

electronN = "electron_" + (useFT ? "FT" : "trigger")
writeTimeline(outHipoQA,TLqa,"${electronN}_yield_QA_${qaName}")
writeTimeline(outHipoA,TLA,"${electronN}_yield_normalized_values")
writeTimeline(outHipoN,TLN,"${electronN}_yield_values")
writeTimeline(outHipoSigmaN,TLsigmaN,"${electronN}_yield_stddev")
if(!useFT) {
  writeTimeline(outHipoU,TLU,"ungated_faraday_cup_charge",true)
  writeTimeline(outHipoF,TLF,"faraday_cup_charge",true)
  writeTimeline(outHipoFA,TLFA,"accumulated_faraday_cup_charge",true)
  writeTimeline(outHipoT,TLT,"live_time",true)
  writeTimeline(outHipoSigmaF,TLsigmaF,"faraday_cup_stddev",true)
}
//writeTimeline(outHipoRhoNF,TLrhoNF,"faraday_cup_vs_${electronN}_yield_correlation",true)

outHipoEpochs.mkdir("/timelines")
outHipoEpochs.cd("/timelines")
outHipoEpochs.addDataSet(TLqaEpochs)
outHipoName = "outmon.${dataset}/${electronN}_yield_QA_epoch_view.hipo"
File outHipoEpochsFile = new File(outHipoName)
if(outHipoEpochsFile.exists()) outHipoEpochsFile.delete()
outHipoEpochs.writeFile(outHipoName)


// sort qaTree and output to json file
//println T.pPrint(qaTree)
qaTree.each { qaRun, qaRunTree -> qaRunTree.sort{it.key.toInteger()} }
qaTree.sort()
new File("outdat.${dataset}/qaTree"+(useFT?"FT":"")+".json").write(JsonOutput.toJson(qaTree))


// print total QA passing fractions
def PF = nGoodTotal / (nGoodTotal+nBadTotal)
def FF = 1-PF
if(qaBit<0) println "\nQA cut overall passing fraction: $PF"
else {
  def PFfile = new File("outdat.${dataset}/passFractions.dat")
  def PFfileWriter = PFfile.newWriter(qaBit>0?true:false)
  def PFstr = qaBit==100 ? "Fraction of golden files (no defects): $PF" :
                           "Fraction of files with "+T.bitDescripts[qaBit]+": $FF"
  PFfileWriter << PFstr << "\n"
  PFfileWriter.close()
}

