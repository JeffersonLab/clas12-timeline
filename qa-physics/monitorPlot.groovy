// reads outmon/monitor* files and generates timeline hipo files
// - to be executed after monitorRead.groovy

import org.jlab.groot.data.IDataSet
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.math.F1D
import java.lang.Math.*
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

// ARGUMENTS:
if(args.length<1) {
  System.err.println "USAGE: run-groovy ${this.class.getSimpleName()}.groovy [INPUT_DIR]"
  System.exit(101)
}
def inDir = args[0]

// get list of input hipo files
def inDirObj = new File(inDir+"/outmon")
def inList = []
def inFilter = ~/monitor_.*\.hipo/
inDirObj.traverse( type: groovy.io.FileType.FILES, nameFilter: inFilter ) {
  if(it.size()>0) inList << inDir+"/outmon/"+it.getName()
}
inList.sort()
inList.each { println it }

// get qaTree
def qaTreeFileN = "${inDir}/outdat/qaTreeFTandFD.json"
def slurper     = new JsonSlurper()
def qaTreeFile  = new File(qaTreeFileN)
def qaTree      = slurper.parse(qaTreeFile)

// subroutine to recompute defect bitmask
def recomputeDefMask = { rnum, bnum ->
  def defList = []
  def defMask = 0
  (1..6).each{ s ->
    qaTree["$rnum"]["$bnum"]["sectorDefects"]["$s"].unique()
    defList += qaTree["$rnum"]["$bnum"]["sectorDefects"]["$s"].collect{it.toInteger()}
  }
  defList.unique().each { defMask += (0x1<<it) }
  qaTree["$rnum"]["$bnum"]["defect"] = defMask
}

// subroutine to add a defect bit
def addDefectBit = { bitnum, rnum, bnumRange, sectorList ->
  qaTree["$rnum"].each{ k, v ->
    def bnum = k.toInteger()
    if(bnum>=bnumRange[0] && (bnumRange[1]==-1 || bnum<=bnumRange[1])) {
      sectorList.each{
        qaTree["$rnum"]["$bnum"]["sectorDefects"]["$it"] += bitnum
      }
      recomputeDefMask(rnum, bnum)
    }
  }
}

// common arguments to the above qaTree-mutating subroutines
def allSectors = [1, 2, 3, 4, 5, 6]
def allBins    = [0, -1]

// input hipo files contain a set of distributions for each time bin
// this program accumulates these time bins' distributions into 'monitor' distributions:
// - let 'X' denote a kinematic variable, plotted as one of these distributions
// - monitors include:
//   - average X vs. time bin number
//   - distribution of average X
//   - 2D distribution of X vs. time bin number

// subroutine to transform an object name to a monitor name
def objToMonName = { name ->
  // strip time bin number
  def tokN = name.tokenize('_')
  name = tokN[0..-2].join('_')
  return name
}

// subroutine to transform an object title into a monitor title
def objToMonTitle = { title ->
  title = title.replaceAll(/::.*$/,'')
  return title
}


// build map of (runnum,binnum) -> (FC charges)
// - this is only used for the relative luminosity attempt
// - not enough statistics; disabled
/*
def dataFile = new File("${inDir}/outdat/data_table.dat")
def fcTree = [:]
def fcrun,fcfile,fcp,fcm,ufcp,ufcm
if(!(dataFile.exists())) throw new Exception("data_table.dat not found")
dataFile.eachLine { line ->
  tok = line.tokenize(' ')
  fcrun = tok[0].toInteger()
  fcbin = tok[1].toInteger()
  fcp = tok[11].toBigDecimal()
  fcm = tok[12].toBigDecimal()
  ufcp = tok[13].toBigDecimal()
  ufcm = tok[14].toBigDecimal()
  if(!fcTree.containsKey(fcrun)) fcTree[fcrun] = [:]
  if(!fcTree[fcrun].containsKey(fcbin)) {
    fcTree[fcrun][fcbin] = [
      'fcP':fcp,
      'fcM':fcm,
      'ufcP':ufcp,
      'ufcM':ufcm
    ]
  }
}
*/

//---------------------------------
// monitor builders
//---------------------------------

// append legend for helicity (as needed)
def appendLegend = { t ->
  if(t.contains('hel+') || t.contains('hel-')) {
    t = t.replaceAll(/ hel. /,' ')
    t += '   (  black = hel+   red = hel-  )'
  }
  return t
}

// build monitor 'average X vs. time bin number'
def buildMonAveGr = { tObj ->
  def grN = objToMonName(tObj.getName())
  def grT = objToMonTitle(tObj.getTitle())
  grN = grN + "_aveGr"
  grT = "average " + grT
  grT = grT.replaceAll(/$/,' vs. time bin number')
  grT = appendLegend(grT)
  def gr = new GraphErrors(grN)
  gr.setTitle(grT)
  if(grN.contains("_hm_")) { gr.setMarkerColor(2); gr.setLineColor(2); }
  return gr
}

// build monitor 'average X' distribution
def buildMonAveDist = { tObj,nb,lb,ub ->
  def histN = objToMonName(tObj.getName())
  def histT = objToMonTitle(tObj.getTitle())
  histN = histN + "_aveDist"
  histT = "average " + histT
  histT = histT.replaceAll(/$/,' distribution')
  histT = appendLegend(histT)
  def hist = new H1F(histN,histT,nb,lb,ub)
  if(histN.contains("_hm_")) { hist.setLineColor(2); }
  return hist
}

// build asymGrid: a coarse distribution of sinPhi, used for calculating the asymmetry
def buildAsymGrid = { tObj,nb ->
  def histN = objToMonName(tObj.getName())
  def histT = objToMonTitle(tObj.getTitle())
  histN = histN + "_asymGrid"
  histT = histT.replaceAll(/$/,' distribution')
  histT = appendLegend(histT)
  def hist = new H1F(histN,histT,nb,-1,1)
  if(histN.contains("_hm_")) { hist.setLineColor(2); }
  return hist
}

// build asymGraph: a graph of the asymmetry, which will be used for the fit
// - it is the difference between asymGrid for hel+ and asymGrid for hel-,
//   divided by the sum
def buildAsymGraph = { tObj ->
  def grN = objToMonName(tObj.getName())
  if(grN.contains('_hp_'))      grN = grN.replaceAll('_hp_','_')
  else if(grN.contains('_hm_')) grN = grN.replaceAll('_hm_','_')
  grN = grN.replaceAll('sinPhi','asym')
  grN = grN + "_asymGraph"
  def grT = objToMonTitle(tObj.getTitle())
  grT = grT.replaceAll(/hel.*$/,'asymmetry vs. sin(phiH)')
  def gr = new GraphErrors(grN)
  gr.setTitle(grT)
  return gr
}

// calculate beam charge asymmetry
def calculateBeamChargeAsym = { qP, qM ->
  if(qP+qM > 0) {
    return [
      (qP - qM) / (qP + qM),   // asymmetry
      1 / Math.sqrt( qP + qM ) // error, assuming |beamChargeAsym| << 1
    ]
  }
  return ["unknown","unknown"]
}

//-----------------------------------------
// fill the monitors
//-----------------------------------------

def monTree = [:]

def tok
def objList
def part
def hel
def varStr
def varNB
def varLB,varUB
def runnum
def timeBinNum
def obj
def aveX
def aveXerr
def stddevX
def ent
def helDef,helUndef,helFrac,helFracErr,rellum,rellumErr

// loop over input hipo files
inList.each { inFile ->
  def inTdir = new TDirectory()
  try {
    inTdir.readFile(inFile)
  } catch(Exception ex) {
    System.err.println("ERROR: cannot read file $inFile; it may be corrupt")
    return
  }
  objList = inTdir.getCompositeObjectList(inTdir)
  objList.each { objN ->
    obj = inTdir.getObject(objN)

    // tokenize object name to get runnum, time bin number
    tok = objN.tokenize('/')[-1].tokenize('_')
    runnum = tok[-2].toInteger()
    timeBinNum = new BigInteger(tok[-1])

    // <sinPhi> monitors
    //------------------------------------
    if(objN.contains("/helic_sinPhi_")) {
      part = tok[2]
      hel = tok[3]

      // instantiate <sinPhi> graph and sinPhi dist (if not yet instantiated)
      T.addLeaf(monTree,[runnum,'helic','sinPhi',part,hel,'aveGr'],{
        buildMonAveGr(obj)
      })
      T.addLeaf(monTree,[runnum,'helic','sinPhi',part,hel,'aveDist'],{
        buildMonAveDist(obj,100,-0.25,0.25)
      })

      // instantiate sinPhi dists binned for an asymmetry, denoted "asymGrid" (if not
      // yet instantiated)
      // also instantiate graph used for asymmetry fit, denoted "asymGraph"
      T.addLeaf(monTree,[runnum,'helic','sinPhi',part,hel,'asymGrid'],{
        buildAsymGrid(obj,8)
      })
      T.addLeaf(monTree,[runnum,'helic','asym',part,'asymGraph'],{
        buildAsymGraph(obj)
      })

      // add <sinPhi> point to the monitors, and rebinned sinPhi distribution to
      // asymGrids
      ent = obj.integral()
      if(ent>0) {
        aveX = obj.getMean()
        aveXerr = obj.getRMS() / Math.sqrt(ent)
        monTree[runnum]['helic']['sinPhi'][part][hel]['aveGr'].addPoint(
          timeBinNum, aveX, 0, aveXerr )
        monTree[runnum]['helic']['sinPhi'][part][hel]['aveDist'].fill(aveX)
        // add rebinned <sinPhi> distribution to asymGrid
        obj.getAxis().getNBins().times { bin ->
          def counts = obj.getBinContent(bin)
          def value = obj.getAxis().getBinCenter(bin)
          monTree[runnum]['helic']['sinPhi'][part][hel]['asymGrid'].fill(value,counts)
        }
      }
    }

    // helicity distribution monitor
    //------------------------------------
    if(objN.contains("/helic_dist_")) {

      // fraction of events with a defined helicity
      T.addLeaf(monTree,[runnum,'helic','dist','heldef','heldefNumer'],{0})
      T.addLeaf(monTree,[runnum,'helic','dist','heldef','heldefDenom'],{0})
      T.addLeaf(monTree,[runnum,'helic','dist','heldef','heldefGr'],{
        def g = buildMonAveGr(obj)
        def gN = g.getName().replaceAll(/_aveGr$/,'_heldefGr')
        g.setName(gN)
        g.setTitle('average defined helicity fraction vs. time bin number')
        return g
      })
      T.addLeaf(monTree,[runnum,'helic','dist','heldef','heldefDist'],{
        def h = buildMonAveDist(obj,50,0,1)
        def hN = h.getName().replaceAll(/_aveDist$/,'_heldefDist')
        h.setName(hN)
        h.setTitle('average defined helicity fraction distribution')
        return h
      })
      ent = obj.integral()
      if(ent>0) {
        helDef = obj.getBinContent(0) + obj.getBinContent(2)
        helUndef = obj.getBinContent(1)
        def numer = helDef
        def denom = helDef + helUndef
        helFrac = numer / denom
        helFracErr = helFrac * Math.sqrt( 1.0/numer + 1.0/denom )
        monTree[runnum]['helic']['dist']['heldef']['heldefGr'].addPoint(
          timeBinNum, helFrac, 0, helFracErr )
        monTree[runnum]['helic']['dist']['heldef']['heldefDist'].fill(helFrac)
        monTree[runnum]['helic']['dist']['heldef']['heldefNumer'] += numer
        monTree[runnum]['helic']['dist']['heldef']['heldefDenom'] += denom
      }

      // relative luminosity
      T.addLeaf(monTree,[runnum,'helic','dist','rellum','rellumNumer'],{0})
      T.addLeaf(monTree,[runnum,'helic','dist','rellum','rellumDenom'],{0})
      T.addLeaf(monTree,[runnum,'helic','dist','rellum','rellumGr'],{
        def g = buildMonAveGr(obj)
        def gN = g.getName().replaceAll(/_aveGr$/,'_rellumGr')
        g.setName(gN)
        g.setTitle('average n+/n- vs. time bin number')
        return g
      })
      T.addLeaf(monTree,[runnum,'helic','dist','rellum','rellumDist'],{
        def h = buildMonAveDist(obj,50,0.9,1.1)
        def hN = h.getName().replaceAll(/_aveDist$/,'_rellumDist')
        h.setName(hN)
        h.setTitle('average n+/n- distribution')
        return h
      })
      if(obj.integral()>0) {
        // use values from helic_dist
        def helM = obj.getBinContent(0) // helicity = -1
        def helP = obj.getBinContent(2) // helicity = +1
        // use charge from FC (disabled)
        //helP = fcTree[runnum][timeBinNum.toInteger()]['fcP']
        //helM = fcTree[runnum][timeBinNum.toInteger()]['fcM']
        rellum = helM>0 ? helP / helM : 0
        rellumErr = rellum>0 ? rellum * Math.sqrt( 1.0/helP + 1.0/helM ) : 0
        monTree[runnum]['helic']['dist']['rellum']['rellumGr'].addPoint(
          timeBinNum, rellum, 0, rellumErr )
        monTree[runnum]['helic']['dist']['rellum']['rellumDist'].fill(rellum)
        monTree[runnum]['helic']['dist']['rellum']['rellumNumer'] += helP
        monTree[runnum]['helic']['dist']['rellum']['rellumDenom'] += helM
      }
    }

    // beam charge asymmetry
    //----------------------------
    if(objN.contains("/helic_scaler_chargeWeighted_")) {
      ['numHelP','numHelM'].each{
        T.addLeaf(monTree,[runnum,'helic','beamChargeAsym',it],{0.0})
      }
      T.addLeaf(monTree,[runnum,'helic','beamChargeAsym','asymGraph'],{
        def g = buildMonAveGr(obj)
        def gN = g.getName().replaceAll(/_aveGr$/,'_chargeAsymGr')
        g.setName(gN)
        g.setTitle('beam charge asymmetry vs. time bin number')
        return g
      })
      if(obj.integral()>0) {
        def numHelP = obj.getBinContent(2) // helicity = +1
        def numHelM = obj.getBinContent(0) // helicity = -1
        monTree[runnum]['helic']['beamChargeAsym']['numHelP'] += numHelP
        monTree[runnum]['helic']['beamChargeAsym']['numHelM'] += numHelM
        def asym = calculateBeamChargeAsym(numHelP, numHelM)
        if(!asym.contains("unknown")) {
          monTree[runnum]['helic']['beamChargeAsym']['asymGraph'].addPoint(timeBinNum, asym[0], 0, asym[1])
        }
      }
    }

    // DIS kinematics monitor
    //---------------------------------
    if(objN.contains("/DIS_")) {
      if(!objN.contains("Q2VsW")) {
        varStr = tok[1]

        T.addLeaf(monTree,[runnum,'DIS',varStr,'aveGr'],{buildMonAveGr(obj)})
        T.addLeaf(monTree,[runnum,'DIS',varStr,'aveDist'],{
          varNB = 100
          if(varStr=="Q2") { varLB=0; varUB=12; }
          else if(varStr=="W") { varLB=0; varUB=6; }
          else { varLB=0; varUB=1; }
          buildMonAveDist(obj,varNB,varLB,varUB)
        })

        // add <varStr> point to the monitors
        ent = obj.integral()
        if(ent>0) {
          aveX = obj.getMean()
          aveXerr = obj.getRMS() / Math.sqrt(ent)
          monTree[runnum]['DIS'][varStr]['aveGr'].addPoint(
            timeBinNum, aveX, 0, aveXerr )
          monTree[runnum]['DIS'][varStr]['aveDist'].fill(aveX)
        }
      }
    }

    // pion kinematics monitor
    //----------------------------
    if(objN.contains("/inclusive_")) {
      part = tok[1]
      varStr = tok[2]
      T.addLeaf(monTree,[runnum,'inclusive',part,varStr,'aveGr'],{buildMonAveGr(obj)})
      T.addLeaf(monTree,[runnum,'inclusive',part,varStr,'aveDist'],{
        varNB = 100
        varLB = 0
        varUB = 0
        if(varStr=='p') { varLB=0; varUB=10 }
        else if(varStr=='pT') { varLB=0; varUB=4 }
        else if(varStr=='z') { varLB=0; varUB=1 }
        else if(varStr=='theta') { varLB=0; varUB=Math.toRadians(90.0) }
        else if(varStr=='phiH') { varLB=-3.15; varUB=3.15 }
        buildMonAveDist(obj,varNB,varLB,varUB)
      })
      ent = obj.integral()
      if(ent>0) {
        aveX = obj.getMean()
        aveXerr = obj.getRMS() / Math.sqrt(ent)
        monTree[runnum]['inclusive'][part][varStr]['aveGr'].addPoint(
          timeBinNum, aveX, 0, aveXerr )
        monTree[runnum]['inclusive'][part][varStr]['aveDist'].fill(aveX)
      }
    }

    // charge non-monotonicicity
    //--------------------------
    if(objN.contains("/nonMonotonicity_")) {
      T.addLeaf(monTree,[runnum,'charge','nonMonotonicity','valGraph'],{obj})
    }

  } // eo loop over objects in the file (run)


  // fit beam spin asymmetry
  T.exeLeaves(monTree[runnum]['helic']['asym'],{
    def particle = T.leafPath[0]
    def grP = T.getLeaf(monTree,[runnum,'helic','sinPhi',particle,'hp','asymGrid'])
    def grM = T.getLeaf(monTree,[runnum,'helic','sinPhi',particle,'hm','asymGrid'])
    grP.getAxis().getNBins().times { bin ->
      def yp = grP.getBinContent(bin)
      def ym = grM.getBinContent(bin)
      def xval = grP.getAxis().getBinCenter(bin)
      if(yp+ym>0)
        T.leaf.addPoint(xval,(yp-ym)/(yp+ym),0.0,1.0/Math.sqrt(yp+ym))
      else
        T.leaf.addPoint(xval,0.0,0.0,1.0)
    }
    def fitFuncN = T.leaf.getName() + ":fit"
    fitFuncN.replaceAll('hp_asymGrid','fitFunc')
    def amp = 0
    def fitFunc = new F1D(fitFuncN,"[amp]*x",-1,1)
    fitFunc.setParameter(0,amp)
    DataFitter.fit(fitFunc,T.leaf,"")
    def fitValue = fitFunc.parameter(0).value()
    def fitError = fitFunc.parameter(0).error()
    T.addLeaf(monTree,[runnum,'helic','asym',particle,'asymValue'],{fitValue})
    T.addLeaf(monTree,[runnum,'helic','asym',particle,'asymError'],{fitError})
    T.addLeaf(monTree,[runnum,'helic','asym',particle,'asymFit'],{fitFunc})
  })

} // eo loop over each file (run)


//---------------------
// build timelines
//---------------------

// loop through 'aveDist' monitors: for each one, add its mean to the timeline
def timelineTree = [:]
T.exeLeaves(monTree,{
  if(T.key.contains('Dist') || T.key.contains('Graph')) {

    // get leaf paths
    def tlRun = T.leafPath[0]
    def tlPath = T.leafPath[1..-2]

    // initialise new timeline graph, if not yet initialised
    T.addLeaf(timelineTree,tlPath+'timeline',{
      def tlN = (tlPath+'timeline').join('_')
      def tlT
      if(tlPath.contains('helic')) {
        if(tlPath.contains('sinPhi')) tlT = "sinPhiH"
        else if(T.key=='heldefDist') tlT = "defined helicity fraction"
        else if(T.key=='rellumDist') tlT = "n+/n-"
        else if(tlPath.contains('beamChargeAsym')) tlT = "beam charge asymmetry"
        else if(T.key=='asymGraph') tlT = "beam spin asymmetry: pion sin(phiH) amplitude"
        else tlT = "unknown"
      }
      if(tlPath.contains('DIS')) tlT = "DIS kinematics"
      if(tlPath.contains('inclusive')) {
        if(tlPath.contains('pip')) tlT = "inclusive pi+ kinematics"
        if(tlPath.contains('pim')) tlT = "inclusive pi- kinematics"
      }
      if(tlPath.contains('nonMonotonicity')) {
        tlT = "FC charge non-monotonicity"
        tlN = "mean_non-monotonicity"
      }
      if(T.key.contains('Dist')) tlT = "average ${tlT}"
      tlT = "${tlT} vs. run number"
      def tl = new GraphErrors(tlN)
      tl.setTitle(tlT)
      return tl
    })

    // we also want a few timelines to monitor standard deviations
    T.addLeaf(timelineTree,tlPath+'timelineDev',{
      if(tlPath.contains('DIS') || tlPath.contains('inclusive') || T.key=='valGraph') {
        def tlN = (tlPath+'timelineDev').join('_')
        def tlT
        if(tlPath.contains('DIS')) tlT = "DIS kinematics"
        if(tlPath.contains('inclusive')) {
          if(tlPath.contains('pip')) tlT = "inclusive pi+ kinematics"
          if(tlPath.contains('pim')) tlT = "inclusive pi- kinematics"
        }
        if(tlPath.contains('nonMonotonicity')) {
          tlT = "FC charge non-monotonicity"
          tlN = "stddev_non-monotonicicity"
        }
        if(T.key.contains('Dist')) tlT = "standard deviation of ${tlT}"
        tlT = "${tlT} vs. run number"
        def tl = new GraphErrors(tlN)
        tl.setTitle(tlT)
        return tl
      } else return
    })

    // add this run's <X> to the timeline (and stddev to the stddev timelines)
    if(T.key=='aveDist') {
      aveX = T.leaf.getMean()
      stddevX = T.leaf.getRMS()
      aveXerr = stddevX / Math.sqrt(T.leaf.integral())
      T.getLeaf(timelineTree,tlPath+'timeline').addPoint(tlRun,aveX,0.0,aveXerr)
      if(tlPath.contains('DIS') || tlPath.contains('inclusive')) {
        T.getLeaf(timelineTree,tlPath+'timelineDev').addPoint(tlRun,stddevX,0.0,0.0)
      }
    }
    // or if it's a `valGraph`, calculate the avarage and stddev y-axis value and add them to the timelines
    else if(T.key=='valGraph') {
      def vals = []
      T.leaf.getDataSize(0).times{ vals += T.leaf.getDataY(it) }
      def tot    = vals.size()
      def ave    = tot>0 ? vals.sum() / tot : 0
      def devs   = vals.collect{ (it-ave)**2 }
      def stddev = tot>0 ? Math.sqrt( devs.sum() / tot ) : tot
      T.getLeaf(timelineTree,tlPath+'timeline').addPoint(tlRun,ave,0.0,0.0)
      T.getLeaf(timelineTree,tlPath+'timelineDev').addPoint(tlRun,stddev,0.0,0.0)
    }
    // or if it's a helicity distribution monitor, add the run's overall fractions
    if(T.key=='heldefDist' ||  T.key=='rellumDist') {
      def ndKey = T.key.replaceAll('Dist','')
      def numer = monTree[tlRun]['helic']['dist'][ndKey]["${ndKey}Numer"]
      def denom = monTree[tlRun]['helic']['dist'][ndKey]["${ndKey}Denom"]
      def frac = denom>0 ? numer/denom : 0
      T.getLeaf(timelineTree,tlPath+'timeline').addPoint(tlRun,frac,0.0,0.0)
    }
    // or if it's an asymmetry graph, add its results to the timeline
    if(T.key=='asymGraph') {
      // beam charge asymmetry --------
      if(T.leafPath.contains("beamChargeAsym")) {
        def numHel = ['numHelP','numHelM'].collect{T.getLeaf(monTree, T.leafPath[0..-2] + it)}
        def asym = calculateBeamChargeAsym(*numHel)
        if(!asym.contains("unknown")) {
          T.getLeaf(timelineTree,tlPath+'timeline').addPoint(tlRun, asym[0], 0.0, asym[1])
        }
        else {
          System.err.println "WARNING: unknown beam charge asymmetry for run $tlRun"
        }
      }
      // beam spin asymmetry --------
      else {
        def valPath = T.leafPath[0..-2] + 'asymValue'
        def errPath = T.leafPath[0..-2] + 'asymError'
        def asymVal = T.getLeaf(monTree,valPath)
        def asymErr = T.getLeaf(monTree,errPath)
        T.getLeaf(timelineTree,tlPath+'timeline').addPoint(tlRun, asymVal, 0.0, asymErr)
        // and assign a defect bit for pi+ BSA
        if(tlPath.contains('pip')) {
          def asymMargin = asymVal.abs() - asymErr
          if(asymMargin <= 0) {
            addDefectBit(T.bit("BSAUnknown"), tlRun, allBins, allSectors)
          } else if(asymVal < 0) {
            addDefectBit(T.bit("BSAWrong"), tlRun, allBins, allSectors)
          }
        }
      }
    }
  }
})


// subroutines to output timelines and associated plots to a hipo file
def checkFilter( list, filter, keyName="" ) {
  return list.intersect(filter).size() == filter.size() &&
         !keyName.contains("Numer") && !keyName.contains("Denom")
}

def hipoWrite = { hipoName, filterList, TLkeys ->
  def outHipo = new TDirectory()
  monTree.each { run,tree ->
    outHipo.mkdir("/${run}")
    outHipo.cd("/${run}")
    // add plots to /$run directory; plots defined for + and - helicities
    // will be renamed such that the front end plots them together
    T.exeLeaves(tree,{
      if(checkFilter(T.leafPath,filterList,T.key)) {
        if(T.leaf instanceof IDataSet) {
          def name = T.leaf.getName()
          if(name.contains('_hp_')) name = name.replaceAll('_hp_','_')
          else if(name.contains('_hm_')) {
            name = name.replaceAll('_hm_','_')
            name += ":hm"
          }
          T.leaf.setName(name)
          outHipo.addDataSet(T.leaf)
        }
      }
    })
  }
  outHipo.mkdir("/timelines")
  outHipo.cd("/timelines")
  T.exeLeaves(timelineTree,{
    if(checkFilter(T.leafPath,filterList) && TLkeys.contains(T.key)) {
      outHipo.addDataSet(T.leaf)
    }
  })

  def outHipoN = "${inDir}/outmon/${hipoName}.hipo"
  File outHipoFile = new File(outHipoN)
  if(outHipoFile.exists()) outHipoFile.delete()
  outHipo.writeFile(outHipoN)
}

// write objects to hipo files
hipoWrite("helicity_sinPhi",['helic','sinPhi'],["timeline"])
hipoWrite("beam_spin_asymmetry",['helic','asym'],["timeline"])
hipoWrite("defined_helicity_fraction",['helic','dist','heldef'],["timeline"])
hipoWrite("beam_charge_asymmetry",['helic','beamChargeAsym'],["timeline"])
hipoWrite("relative_yield",['helic','dist','rellum'],["timeline"])
hipoWrite("q2_W_x_y_means",['DIS'],["timeline"])
hipoWrite("pip_kinematics_means",['inclusive','pip'],["timeline"])
hipoWrite("pim_kinematics_means",['inclusive','pim'],["timeline"])
hipoWrite("q2_W_x_y_stddevs",['DIS'],["timelineDev"])
hipoWrite("pip_kinematics_stddevs",['inclusive','pip'],["timelineDev"])
hipoWrite("pim_kinematics_stddevs",['inclusive','pim'],["timelineDev"])
hipoWrite("faraday_cup_charge_non-monotonicity",['charge','nonMonotonicity'],["timeline","timelineDev"])

// sort qaTree and output to json file
qaTree.each { qaRun, qaRunTree -> qaRunTree.sort{it.key.toInteger()} }
qaTree.sort()
new File("${inDir}/outdat/qaTree.json").write(JsonOutput.toJson(qaTree))
