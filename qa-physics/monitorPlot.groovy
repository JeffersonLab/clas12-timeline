// reads outmon.${dataset}/monitor* files and generates timeline hipo files
// - to be executed after monitorRead.groovy

import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.math.F1D
import java.lang.Math.*
import Tools
Tools T = new Tools()

// ARGUMENTS:
def dataset = 'inbending1'
if(args.length>=1) dataset = args[0]


// get list of input hipo files
def inDir = "outmon.${dataset}"
def inDirObj = new File(inDir)
def inList = []
def inFilter = ~/monitor_.*\.hipo/
inDirObj.traverse( type: groovy.io.FileType.FILES, nameFilter: inFilter ) {
  if(it.size()>0) inList << inDir+"/"+it.getName()
}
inList.sort()
inList.each { println it }

// input hipo files contain a set of distributions for each segment
// this program accumulates these segments' distributions into 'monitor' distributions:
// - let 'X' denote a kinematic variable, plotted as one of these distributions
// - moninotors include: 
//   - average X vs. segment number
//   - distribution of average X
//   - 2D distribution of X vs. segment number

// subroutine to transform an object name to a monitor name
def objToMonName = { name ->
  // strip segment number (and standard dev)
  def tokN = name.tokenize('_')
  name = tokN[0..-3].join('_')
  return name
}

// subroutine to transform an object title into a monitor title
def objToMonTitle = { title ->
  title = title.replaceAll(/::.*$/,'')
  return title
}


// build map of (runnum,filenum) -> (FC charges)
// - this is only used for the relative luminosity attempt
// - not enough statistics; disabled
/*
def dataFile = new File("outdat.${dataset}/data_table.dat")
def fcTree = [:]
def fcrun,fcfile,fcp,fcm,ufcp,ufcm
if(!(dataFile.exists())) throw new Exception("data_table.dat not found")
dataFile.eachLine { line ->
  tok = line.tokenize(' ')
  fcrun = tok[0].toInteger()
  fcfile = tok[1].toInteger()
  fcp = tok[11].toBigDecimal()
  fcm = tok[12].toBigDecimal()
  ufcp = tok[13].toBigDecimal()
  ufcm = tok[14].toBigDecimal()
  if(!fcTree.containsKey(fcrun)) fcTree[fcrun] = [:]
  if(!fcTree[fcrun].containsKey(fcfile)) {
    fcTree[fcrun][fcfile] = [
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

// build monitor 'average X vs. segment number'
def buildMonAveGr = { tObj ->
  def grN = objToMonName(tObj.getName())
  def grT = objToMonTitle(tObj.getTitle())
  grN = grN + "_aveGr"
  grT = "average " + grT
  grT = grT.replaceAll(/$/,' vs. segment number')
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
def runnum,segnum
def obj
def aveX
def aveXerr
def stddevX
def ent
def helP,helM,helDef,helUndef,helFrac,helFracErr,rellum,rellumErr

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

    // tokenize object name to get runnum, segment number & deviation
    tok = objN.tokenize('/')[-1].tokenize('_')
    runnum = tok[-3].toInteger()
    segnum = new BigInteger(tok[-2])
    segnumDev = new BigInteger(tok[-1])

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
          segnum, aveX, segnumDev, aveXerr )
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
        g.setTitle('average defined helicity fraction vs. segment number')
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
          segnum, helFrac, segnumDev, helFracErr )
        monTree[runnum]['helic']['dist']['heldef']['heldefDist'].fill(helFrac)
        monTree[runnum]['helic']['dist']['heldef']['heldefNumer'] += numer
        monTree[runnum]['helic']['dist']['heldef']['heldefDenom'] += denom
      }

    // cut for rellum from events with FD trigger electrons only
    //}
    //if(objN.contains("/helic_distGoodOnly_")) {

      // relative luminosity
      T.addLeaf(monTree,[runnum,'helic','dist','rellum','rellumNumer'],{0})
      T.addLeaf(monTree,[runnum,'helic','dist','rellum','rellumDenom'],{0})
      T.addLeaf(monTree,[runnum,'helic','dist','rellum','rellumGr'],{
        def g = buildMonAveGr(obj)
        def gN = g.getName().replaceAll(/_aveGr$/,'_rellumGr')
        g.setName(gN)
        g.setTitle('average n+/n- vs. segment number')
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
        helM = obj.getBinContent(0) // helicity = -1
        helP = obj.getBinContent(2) // helicity = +1
        // use charge from FC (disabled)
        //helP = fcTree[runnum][segnum.toInteger()]['fcP']
        //helM = fcTree[runnum][segnum.toInteger()]['fcM']
        rellum = helM>0 ? helP / helM : 0
        rellumErr = rellum>0 ? rellum * Math.sqrt( 1.0/helP + 1.0/helM ) : 0
        monTree[runnum]['helic']['dist']['rellum']['rellumGr'].addPoint(
          segnum, rellum, segnumDev, rellumErr )
        monTree[runnum]['helic']['dist']['rellum']['rellumDist'].fill(rellum)
        monTree[runnum]['helic']['dist']['rellum']['rellumNumer'] += helP
        monTree[runnum]['helic']['dist']['rellum']['rellumDenom'] += helM
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
            segnum, aveX, segnumDev, aveXerr )
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
          segnum, aveX, segnumDev, aveXerr )
        monTree[runnum]['inclusive'][part][varStr]['aveDist'].fill(aveX)
      }
    }


  } // eo loop over objects in the file (run)

  
  // fit asymmetry
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
  if(T.key.contains('Dist') || T.key.contains('asymGraph')) {

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
        else if(T.key=='asymGraph') tlT = "beam spin asymmetry: pion sin(phiH) amplitude"
        else tlT = "unknown"
      }
      if(tlPath.contains('DIS')) tlT = "DIS kinematics"
      if(tlPath.contains('inclusive')) {
        if(tlPath.contains('pip')) tlT = "inclusive pi+ kinematics"
        if(tlPath.contains('pim')) tlT = "inclusive pi- kinematics"
      }
      if(T.key.contains('Dist')) tlT = "average ${tlT}"
      tlT = "${tlT} vs. run number"
      def tl = new GraphErrors(tlN)
      tl.setTitle(tlT)
      return tl
    })

    // we also want a few timelines to monitor standard deviations
    T.addLeaf(timelineTree,tlPath+'timelineDev',{
      if(tlPath.contains('DIS') || tlPath.contains('inclusive')) {
        def tlN = (tlPath+'timelineDev').join('_')
        def tlT
        if(tlPath.contains('DIS')) tlT = "DIS kinematics"
        if(tlPath.contains('inclusive')) {
          if(tlPath.contains('pip')) tlT = "inclusive pi+ kinematics"
          if(tlPath.contains('pim')) tlT = "inclusive pi- kinematics"
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
    // or if it's a helicity distribution monitor, add the run's overall fractions
    if(T.key=='heldefDist' ||  T.key=='rellumDist') {
      def ndKey = T.key.replaceAll('Dist','')
      def numer = monTree[tlRun]['helic']['dist'][ndKey]["${ndKey}Numer"]
      def denom = monTree[tlRun]['helic']['dist'][ndKey]["${ndKey}Denom"]
      def frac = denom>0 ? numer/denom : 0
      T.getLeaf(timelineTree,tlPath+'timeline').addPoint(tlRun,frac,0.0,0.0)
    }
    // or if it's an asymmetry graph, add fit results to the timeline
    if(T.key=='asymGraph') {
      def valPath = T.leafPath[0..-2] + 'asymValue'
      def errPath = T.leafPath[0..-2] + 'asymError'
      T.getLeaf(timelineTree,tlPath+'timeline').addPoint(
        tlRun, T.getLeaf(monTree,valPath),
        0.0, T.getLeaf(monTree,errPath))
    }
  }
})
    

// subroutines to output timelines and associated plots to a hipo file
def checkFilter( list, filter, keyName="" ) {
  return list.intersect(filter).size() == filter.size() &&
         !keyName.contains("Numer") && !keyName.contains("Denom")
}

def hipoWrite = { hipoName, filterList, TLkey ->
  def outHipo = new TDirectory()
  monTree.each { run,tree ->
    outHipo.mkdir("/${run}")
    outHipo.cd("/${run}")
    // add plots to /$run directory; plots defined for + and - helicities
    // will be renamed such that the front end plots them together
    T.exeLeaves(tree,{
      if(checkFilter(T.leafPath,filterList,T.key)) {
        if(T.key=='asymValue' || T.key=='asymError' || T.key=='asymGrid') return
        def name = T.leaf.getName()
        if(name.contains('_hp_')) name = name.replaceAll('_hp_','_')
        else if(name.contains('_hm_')) {
          name = name.replaceAll('_hm_','_')
          name += ":hm"
        }
        T.leaf.setName(name)
        outHipo.addDataSet(T.leaf)
      }
    })
  }
  outHipo.mkdir("/timelines")
  outHipo.cd("/timelines")
  T.exeLeaves(timelineTree,{
    if(checkFilter(T.leafPath,filterList) && T.key==TLkey) {
      outHipo.addDataSet(T.leaf)
    }
  })

  def outHipoN = "outmon.${dataset}/${hipoName}.hipo"
  File outHipoFile = new File(outHipoN)
  if(outHipoFile.exists()) outHipoFile.delete()
  outHipo.writeFile(outHipoN)
}

// write objects to hipo files
hipoWrite("helicity_sinPhi",['helic','sinPhi'],"timeline")
hipoWrite("beam_spin_asymmetry",['helic','asym'],"timeline")
hipoWrite("defined_helicity_fraction",['helic','dist','heldef'],"timeline")
hipoWrite("relative_yield",['helic','dist','rellum'],"timeline")
hipoWrite("q2_W_x_y_means",['DIS'],"timeline")
hipoWrite("pip_kinematics_means",['inclusive','pip'],"timeline")
hipoWrite("pim_kinematics_means",['inclusive','pim'],"timeline")
hipoWrite("q2_W_x_y_stddevs",['DIS'],"timelineDev")
hipoWrite("pip_kinematics_stddevs",['inclusive','pip'],"timelineDev")
hipoWrite("pim_kinematics_stddevs",['inclusive','pim'],"timelineDev")
