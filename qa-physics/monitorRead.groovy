// a more general monitor, for things like <sinPhiH> or helicity
// - this reads DST files or skim files
// - can be run on slurm
// - note: search for 'CUT' to find which cuts are applied

import org.jlab.io.hipo.HipoDataSource
import org.jlab.clas.physics.Particle
import org.jlab.groot.data.H1F
import org.jlab.groot.data.H2F
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.TDirectory
import org.jlab.clas.physics.LorentzVector
import org.jlab.detector.base.DetectorType
import java.lang.Math.*
import org.jlab.clas.timeline.util.Tools
import org.jlab.detector.qadb.QadbBin;
import org.jlab.detector.qadb.QadbBinSequence;
import java.nio.file.Paths
import groovy.json.JsonSlurper
Tools T = new Tools()

// CONSTANTS
int MAX_NUM_SCALERS = 2000   // at most this many scaler readouts per QA bin // 2000 is roughly a DST 5-file
def NBINS           = 50     // number of bins in some histograms
def SECTORS         = 0..<6  // sector range
def ECAL_ID         = DetectorType.ECAL.getDetectorId() // ECAL detector ID
// debugging settings
def VERBOSE = false  // enable extra log messages, for debugging
def LIMITER = 0      // if nonzero, only analyze this many DST files (for quick testing and debugging)

// function to print a debugging message
def printDebug = { msg -> if(VERBOSE) println "[DEBUG]: $msg" }

// ARGUMENTS
if(args.length<5) {
  System.err.println """
  USAGE: groovy ${this.class.getSimpleName()}.groovy [HIPO directory or file] [output directory] [type] [runnum] [beam energy]
         REQUIRED parameters:
           - [HIPO directory or file] should be a directory of HIPO files
             or a single hipo file (depends on [type]: use 'dst' for directory
             or 'skim' for file)
           - [output directory] output directory for the produced files
           - [type] can be 'dst' or 'skim'
           - [runnum] the run number
           - [beam energy] the beam energy in GeV
  """
  System.exit(101)
}
def inHipo     = args[0]
def outDir     = args[1]
def inHipoType = args[2]
def runnum     = args[3].toInteger()
def beamEnergy = args[4].toDouble()
System.println """
inHipo     = $inHipo
outDir     = $outDir
inHipoType = $inHipoType
runnum     = $runnum
beamEnergy = $beamEnergy"""

// get hipo file names
def inHipoList = []
if(inHipoType=="dst") {
  def inHipoDirObj = new File(inHipo)
  def inHipoFilter = ~/.*\.hipo/
  inHipoDirObj.traverse( type: groovy.io.FileType.FILES, nameFilter: inHipoFilter ) {
    if(it.size()>0) inHipoList << inHipo+"/"+it.getName()
  }
  inHipoList.sort(true)
  if(inHipoList.size()==0) {
    System.err.println "ERROR: no hipo files found in this directory"
    System.exit(100)
  }
}
else if(inHipoType=="skim") { inHipoList << inHipo }
else {
  System.err.println "ERROR: unknown inHipoType setting"
  System.exit(100)
}

// limiter: use this to only analyse a few DST files, for quicker testing
if(LIMITER>0) {
  inHipoList = inHipoList[0..(LIMITER-1)]
  System.err.println("WARNING WARNING WARNING: LIMITER ENABLED, we will only be analyzing ${LIMITER} DST files, and not all of them; this is for testing only!")
}

// get runnum; assumes all HIPO files have the same run number
if(runnum<=0)
  runnum = T.getRunNumber(inHipoList.first())
if(runnum<=0)
  System.exit(100)
System.println "runnum     = $runnum"


///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
// RUN GROUP DEPENDENT SETTINGS ///////////////////////////////////////////////////////////////////

def RG = "unknown"
if(runnum>=3031 && runnum<=4325) RG="RGA" // spring 18
else if(runnum>=4763 && runnum<=5001) RG="RGA" // early period
else if(runnum>=5032 && runnum<=5262) RG="RGA" // fall 18 inbending1
else if(runnum>=5300 && runnum<=5666) RG="RGA" // fall 18 inbending1 + outbending
else if(runnum>=5674 && runnum<=6000) RG="RGK" // 6.5+7.5 GeV
else if(runnum>=6120 && runnum<=6604) RG="RGB" // spring 19
else if(runnum>=6616 && runnum<=6783) RG="RGA" // spring 19
else if(runnum>=11093 && runnum<=11300) RG="RGB" // fall 19
else if(runnum>=11323 && runnum<=11571) RG="RGB" // winter 20
else if(runnum>=12210 && runnum<=12951) RG="RGF" // spring+summer 20
else if(runnum>=15019 && runnum<=15884) RG="RGM"
else if(runnum>=16042 && runnum<=16786) RG="RGC" // summer 22
else if(runnum>=16843 && runnum<=17408) RG="RGC" // fall 22
else if(runnum>=17477 && runnum<=17811) RG="RGC" // spring 23
else if(runnum>=18305 && runnum<=19131) RG="RGD" // fall 23
else System.err.println "WARNING: unknown run group; using default run-group-dependent settings (see monitorRead.groovy)"
println "rungroup = $RG"

// gated FC charge determination: see `QadbBin` documentation
enum FCmodeEnum {
  NONE,             // no correction
  CUSTOM,           // read custom numbers from a JSON file
  BY_FLIP,          // see `QadbBin.ChargeCorrectionMethod`
  BY_MEAN_LIVETIME, // see `QadbBin.ChargeCorrectionMethod`
}

FCmodeEnum FCmode = FCmodeEnum.NONE // default, no correction

if(RG=="RGD") {
  FCmode = FCmodeEnum.CUSTOM
}
if(RG=="RGM") {
  FCmode = FCmodeEnum.NONE
  if(runnum>=15015 && runnum<=15199) {
    throw new RuntimeException("STOP, need FC correction mode using 'REC::Event:beamcharge' (see code comments)");
    // FIXME: use `REC::Event:beamCharge`, which we have not yet implemented into `QadbBin`; may not even work....
    // we used this in Pass-1 RG-M, but I don't know when/if they will cook Pass 2
    // this is needed when `RUN::scaler` is unavailable
  }
}

/* PASS 1 FCmode settings, for data which have a better PASS 2
if(RG=="RGA") {
  FCmode = FCmodeEnum.NONE
  if(runnum==6724) FCmode = FCmodeEnum.BY_MEAN_LIVETIME; // fcupgated charge spike in file 230
}
else if(RG=="RGB") {
  FCmode = FCmodeEnum.NONE
  if( runnum in [6263, 6350, 6599, 6601, 11119] ) FCmode = FCmodeEnum.BY_MEAN_LIVETIME // fcupgated charge spikes
}
else if(RG=="RGK") FCmode = FCmodeEnum.BY_MEAN_LIVETIME
else if(RG=="RGF") FCmode = FCmodeEnum.BY_MEAN_LIVETIME
*/

///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////


// Set JSON variables for `FCmode==CUSTOM`
def jsonfilepath   = ""
def runnum_colname = ""
def fcchargeSlurper
def fcchargeTree
if (FCmode==FCmodeEnum.CUSTOM) {

  // Check if JSON file exists
  def TIMELINESRC = System.getenv("TIMELINESRC")
  if(TIMELINESRC == null) {
    System.err.println "ERROR: \$TIMELINESRC is not set"
    System.exit(100)
  }
  jsonfilepath = Paths.get(TIMELINESRC, '/data/fccharge/'+RG+'.json').toAbsolutePath().toString()
  def jsonfile = new File(jsonfilepath)
  if (jsonfile.exists()) {
    System.out.println "INFO: Found JSON file $jsonfilepath"
  } else {
    System.err.println "ERROR: With FCmode="+FCmode+". JSON file `$jsonfilepath` does not exist!"
    System.exit(100)
  }

  // Read JSON file data
  fcchargeSlurper = new JsonSlurper()
  fcchargeTree = fcchargeSlurper.parse(jsonfile) //NOTE: This must be a map of column headers to data columns.

  // Set run number column name assuming it is the first column name containing "run"
  fcchargeTree.keySet().each{colname ->
    if (colname.contains("run") && runnum_colname=="") {
      runnum_colname = colname
    }
  }
}

//Function to read in all data from JSON for a given column and return a map of run numbers to column values
def setDataFromJSON = { _key ->
  def newEntry = [:]
  fcchargeTree[runnum_colname].eachWithIndex{ it, idx ->
    newEntry[it] = fcchargeTree[_key][idx]
  }
  return newEntry
}

// Set data from JSON and define a function to get key values for a given run number
def dataFromJSON = [:]
def conversion_factors = [:]
if (FCmode==FCmodeEnum.CUSTOM) {
  dataFromJSON["fc"] = setDataFromJSON("charge_ave")
  conversion_factors["fc"] = 1e6 //NOTE: IMPORTANT: RGD JSON data is listed in mC but this script expects charge values in nC!
}
def getDataFromJSON = { _runnum, _key ->
    return dataFromJSON[_key][_runnum] * conversion_factors[_key]
}

// make outut directories and define output file
"mkdir -p $outDir".execute()
def outHipo = new TDirectory()
outHipo.mkdir("/$runnum")
outHipo.cd("/$runnum")

// prepare QA-binned output table for electron count and FC charge
def datfileName   = "$outDir/data_table_${runnum}.dat"
def datfile       = new File(datfileName)
def datfileWriter = datfile.newWriter(false)

// define shared variables
def hipoEvent
def pidList = []
def particleBank
def FTparticleBank
def configBank
def eventBank
def calBank
def scalerBank
def disEleFound
def caseCountNtrigGT1 = 0
def caseCountNFTwithTrig = 0
def disElectronInTrigger
def disElectronInFT

// DIS kinematics
def Q2
def W
def nu
def x
def y
def z
def vecBeam = new LorentzVector(0, 0, beamEnergy, beamEnergy)
def vecTarget = new LorentzVector(0, 0, 0, 0.938)
def vecEle = new LorentzVector()
def vecH = new LorentzVector()
def vecQ = new LorentzVector()
def vecW = new LorentzVector()


///////////////////////////////////////////////////////////////////////////////////////////////////
// DEFINE QA BINS
///////////////////////////////////////////////////////////////////////////////////////////////////

// a class to hold all the things we want for each QADB bin
class QaBinPayload {
  public def nElec    = (0..<6).collect{0};
  public def nElecFT  = 0;
  public def histTree = [:];
  public String toString() {
    return """PAYLOAD
    nElec = ${nElec}
    nElecFT = ${nElecFT}"""
  }
}

// set QADB bin width: the number of scalers in each bin
def qaBinWidth = MAX_NUM_SCALERS
if(FCmode == FCmodeEnum.CUSTOM) {
  // for CUSTOM charge-correction, we assume we only have the charge for a FULL
  // run, so, make the main bin as wide as possible, so we get 3 bins: one
  // before the first scaler, one after the last scaler, and one for everything between
  qaBinWidth = Integer.MAX_VALUE
}

// define the QADB bin sequence
QadbBinSequence<QaBinPayload> qaBins = new QadbBinSequence<>(inHipoList, qaBinWidth, (binNum) -> new QaBinPayload());

// correct the FC charge
switch(FCmode) {
  case FCmodeEnum.NONE -> {}
  case FCmodeEnum.BY_FLIP          -> {qaBins.each{it.correctCharge(QadbBin.ChargeCorrectionMethod.BY_FLIP)}}
  case FCmodeEnum.BY_MEAN_LIVETIME -> {qaBins.each{it.correctCharge(QadbBin.ChargeCorrectionMethod.BY_MEAN_LIVETIME)}}
  case FCmodeEnum.CUSTOM -> {
    if(qaBins.size() != 3) {
      // FIXME: first and last bins get 'chargeUnknown', middle bin gets the real charge; this is just a kluge for RG-D...
      // for now we throw an exception for other use attempts
      throw new RuntimeException("we have not yet supported CUSTOM charge correction with number of QA bins != 3")
    }
    def chg = getDataFromJSON(runnum,"fc")
    qaBins.getBin(0).correctCharge(0.0, 0.0);
    qaBins.getBin(1).correctCharge(chg, chg); // set gated = ungated, for now...
    qaBins.getBin(2).correctCharge(0.0, 0.0);
  }
}

// print the QADB bins
// qaBins.each { it.print(true, (data) -> data.toString()) }

// initialize min and max overall event numbers and timestamps
def overallMinEventNumber = "init"
def overallMaxEventNumber = "init"
def overallMinTimestamp   = "init"
def overallMaxTimestamp   = "init"


///////////////////////////////////////////////////////////////////////////////////////////////////
// SUBROUTINES
///////////////////////////////////////////////////////////////////////////////////////////////////

// subroutine which returns a list of Particle objects of a certain PID
// - if `pid==11`, it will count the trigger electrons in FD and/or FT
def findParticles = { pid, binNum ->

  // get list of bank rows and Particle objects corresponding to this PID
  def rowList = pidList.findIndexValues{ it == pid }.collect{it as Integer}
  def particleList = rowList.collect { row ->
    new Particle(pid,*['px','py','pz'].collect{particleBank.getFloat(it,row)})
  }
  //println "pid=$pid  found in rows $rowList"

  // if looking for electrons, also count the number of trigger electrons,
  // and find the DIS electron
  if(pid==11) {

    // reset some vars
    def Emax = 0
    def Etmp
    disElectronInTrigger = false
    disElectronInFT = false
    def nTrigger = 0
    def nFT = 0
    disEleFound = false
    def disElectron
    def eleSec

    // loop over electrons from REC::Particle
    if(rowList.size()>0) {
      rowList.eachWithIndex { row,ind ->

        def status = particleBank.getShort('status',row)
        def chi2pid = particleBank.getFloat('chi2pid',row)

        // TRIGGER ELECTRONS (FD or CD) CUT
        // - must have status<0 and FD or CD bit(s) set
        // - must have |chi2pid|<3
        // - must appear in ECAL, to obtain sector
        if( status<0 &&
            ( Math.abs(status/1000).toInteger() & 0x2 ||
              Math.abs(status/1000).toInteger() & 0x4 ) &&
            Math.abs(chi2pid)<3
        ) {

          // get sector
          def eleSecTmp = (0..calBank.rows()).collect{
            ( calBank.getShort('pindex',it).toInteger() == row &&
              calBank.getByte('detector',it).toInteger() == ECAL_ID ) ?
              calBank.getByte('sector',it).toInteger() : null
          }.find()

          // CUT for electron: sector must be defined
          if(eleSecTmp!=null) {

            nTrigger++ // count how many trigger electrons we looked at

            // CUT for electron: choose maximum energy electron (for triggers)
            // - choice is from both trigger and FT electron sets (see below)
            Etmp = particleList[ind].e()
            if(Etmp>Emax) {
              Emax = Etmp
              eleSec = eleSecTmp
              disElectronInTrigger = true
              disElectronInFT = false
              disElectron = particleList[ind]
            }

          } else {
            System.err.println "WARNING: found electron with unknown sector"
          }
        }

        // FT trigger electrons
        // - REC::Particle:status has FT bit
        // - must also appear in RECFT::Particle with status<0 and FT bit
        // - must have E > 300 MeV
        if( Math.abs(status/1000).toInteger() & 0x1 ) {
          if( FTparticleBank.rows() > row ) {
            def FTpid = FTparticleBank.getInt('pid',row)
            def FTstatus = FTparticleBank.getShort('status',row)
            if( FTpid==11 &&
                FTstatus<0 &&
                Math.abs(FTstatus/1000).toInteger() & 0x1 &&
                particleList[ind].e() > 0.3
            ) {

              nFT++ // count how many FT electrons we looked at

              // CUT for electron: maximum energy electron (for FT)
              // - choice is from both trigger and FT electron sets (see above)
              Etmp = particleList[ind].e()
              if(Etmp>Emax) {
                Emax = Etmp
                disElectronInFT = true
                disElectronInTrigger = false
                disElectron = particleList[ind]
              }

            }
          }
        }

      } // eo loop through REC::Particle
    } // eo if nonempty REC::Particle

    // calculate DIS kinematics and increment counters
    if(disElectronInTrigger || disElectronInFT) {

      // - calculate DIS kinematics
      // calculate Q2
      vecQ.copy(vecBeam)
      vecEle.copy(disElectron.vector())
      vecQ.sub(vecEle)
      Q2 = -1*vecQ.mass2()

      // calculate W
      vecW.copy(vecBeam)
      vecW.add(vecTarget)
      vecW.sub(vecEle)
      W = vecW.mass()

      // calculate x and y
      nu = vecBeam.e() - vecEle.e()
      x = Q2 / ( 2 * 0.938272 * nu )
      y = nu / beamEnergy

      // CUT for electron: Q2 cut
      //if(Q2<2.5) return

      // - increment counters, and set `disEleFound`
      if(disElectronInTrigger && disElectronInFT) { // can never happen (failsafe)
        System.err.println "ERROR: disElectronInTrigger && disElectronInFT == 1; skip event"
        return
      }
      else if(disElectronInTrigger) {
        qaBins.getBin(binNum).data.nElec[eleSec-1]++
        System.out.println("DEBUG: bump sector $eleSec");
        disEleFound = true
      }
      else if(disElectronInFT) {
        qaBins.getBin(binNum).data.nElecFT++
        disEleFound = true
      }

      // increment 'case counters' (for studying overlap of trigger/FT cuts)
      // - case where there are more than one trigger electron in FD
      if(disElectronInTrigger && nTrigger>1)
        caseCountNtrigGT1 += nTrigger-1 // count number of unanalyzed extra electrons
      // - case where disElectron is in FT, but there are trigger electrons in FD
      if(disElectronInFT && nTrigger>0)
        caseCountNFTwithTrig += nTrigger // count number of unanalyzed trigger (FD) electrons

    } // eo if(disElectronInTrigger || disElectronInFT)
  } // eo if(pid==11)

  // return list of Particle objects
  return particleList
}



// subroutine to build a histogram
def buildHist(histName, histTitle, propList, runn, nb, lb, ub, nb2=0, lb2=0, ub2=0) {

  def propT = [
    'pip': 'pi+',
    'pim': 'pi-',
    'hp':  'hel+',
    'hm':  'hel-',
    'hu':  'hel?',
  ]

  def pn = propList.join('_')
  def pt = propList.collect{ propT.containsKey(it) ? propT[it] : it }.join(' ')
  if(propList.size()>0) { pn+='_'; }

  def sn = propList.size()>0 ? '_':''
  def st = propList.size()>0 ? ' ':''
  def hn = "${histName}_${pn}${runn}"
  def ht = "${pt} ${histTitle}"

  if(nb2==0) return new H1F(hn,ht,nb,lb,ub)
  else return new H2F(hn,ht,nb,lb,ub,nb2,lb2,ub2)
}

// initialize histograms for each QA bin
printDebug "Initialize histograms for each QA bin"
qaBins.each{ qaBin ->
  def binNum = qaBin.getBinNum()
  def partList = [ 'pip', 'pim' ]
  T.buildTree(qaBin.data.histTree, 'helic',     [['sinPhi'],partList,['hp','hm']],        { new H1F() })
  T.buildTree(qaBin.data.histTree, 'helic',     [['dist']],                               { new H1F() })
  T.buildTree(qaBin.data.histTree, 'helic',     [['scaler'],['chargeWeighted']],          { new H1F() })
  T.buildTree(qaBin.data.histTree, 'DIS',       [['Q2','W','x','y']],                     { new H1F() })
  T.buildTree(qaBin.data.histTree, "DIS",       [['Q2VsW']],                              { new H2F() })
  T.buildTree(qaBin.data.histTree, "inclusive", [partList,['p','pT','z','theta','phiH']], { new H1F() })
  // if(binNum==0) T.printTree(qaBin.data.histTree,{T.leaf.getClass()});

  qaBin.data.histTree.helic.dist         = buildHist('helic_dist','helicity',[],runnum,3,-1,2)
  qaBin.data.histTree.DIS.Q2             = buildHist('DIS_Q2','Q^2',[],runnum,2*NBINS,0,12)
  qaBin.data.histTree.DIS.W              = buildHist('DIS_W','W',[],runnum,2*NBINS,0,6)
  qaBin.data.histTree.DIS.x              = buildHist('DIS_x','x',[],runnum,2*NBINS,0,1)
  qaBin.data.histTree.DIS.y              = buildHist('DIS_y','y',[],runnum,2*NBINS,0,1)
  qaBin.data.histTree.DIS.Q2VsW          = buildHist('DIS_Q2VsW','Q^2 vs W',[],runnum,NBINS,0,6,NBINS,0,12)
  T.exeLeaves( qaBin.data.histTree.helic.sinPhi, {
    T.leaf = buildHist('helic_sinPhi','sinPhiH',T.leafPath,runnum,NBINS,-1,1)
  })
  qaBin.data.histTree.helic.scaler.chargeWeighted = buildHist('helic_scaler_chargeWeighted','FC-charge-weighted helicity',[],runnum,3,-1,2)
  T.exeLeaves( qaBin.data.histTree.inclusive, {
    def lbound=0
    def ubound=0
    if(T.key=='p')          { lbound=0; ubound=10 }
    else if(T.key=='pT')    { lbound=0; ubound=4 }
    else if(T.key=='z')     { lbound=0; ubound=1 }
    else if(T.key=='theta') { lbound=0; ubound=Math.toRadians(90.0) }
    else if(T.key=='phiH')  { lbound=-3.15; ubound=3.15 }
    T.leaf = buildHist('inclusive','',T.leafPath,runnum,NBINS,lbound,ubound)
  })

  T.exeLeaves( qaBin.data.histTree, {
    def histN = T.leaf.getName() + "_${binNum}"
    def histT = T.leaf.getTitle() + " :: qaBinNum=${binNum}"
    T.leaf.setName(histN)
    T.leaf.setTitle(histT)
  })

  // print the histogram names and titles
  // // if(binNum==0) {
  //   println "---\nhistogram names and titles:"
  //   T.printTree(qaBin.data.histTree,{ T.leaf.getName() +" ::: "+ T.leaf.getTitle() })
  //   println "---"
  // // }
}



///////////////////////////////////////////////////////////////////////////////////////////////////
// MAIN EVENT LOOP
///////////////////////////////////////////////////////////////////////////////////////////////////

def evCount = 0
def evCountFull = 0
def countEvent
printDebug "Begin main event loop"
inHipoList.each { inHipoFile ->

  // open HIPO file
  printDebug "Open HIPO file $inHipoFile"
  def reader = new HipoDataSource()
  reader.open(inHipoFile)

  // EVENT LOOP
  while(reader.hasEvent()) {
    hipoEvent = reader.getNextEvent()

    // get required banks
    particleBank   = hipoEvent.getBank("REC::Particle")
    eventBank      = hipoEvent.getBank("REC::Event")
    configBank     = hipoEvent.getBank("RUN::config")
    FTparticleBank = hipoEvent.getBank("RECFT::Particle")
    calBank        = hipoEvent.getBank("REC::Calorimeter")
    scalerBank     = hipoEvent.getBank("RUN::scaler")
    helScalerBank  = hipoEvent.getBank("HEL::scaler")

    // get event number
    def eventNum
    def timestamp
    if(configBank.rows()>0) {
      eventNum = BigInteger.valueOf(configBank.getInt('event',0))
      timestamp = BigInteger.valueOf(configBank.getLong('timestamp',0))
    }
    else if(hipoEvent.getBankList().length==1 && hipoEvent.getBankList().contains("COAT::config")) {
      printDebug "Skipping event which has only 'COAT::config' bank"
      continue
    }
    else {
      // System.err.println "WARNING: cannot get event number for event with no RUN::config bank; skipping this event; available banks: ${hipoEvent.getBankList()}"
      continue
    }
    if(eventNum==0) {
      // System.err.println "WARNING: found event with eventNum=0; banks: ${hipoEvent.getBankList()}"
      continue
    }

    // set overall min and max event numbers and timestamps
    if(overallMinEventNumber == "init") overallMinEventNumber = eventNum
    else overallMinEventNumber = [ overallMinEventNumber, eventNum ].min()
    if(overallMaxEventNumber == "init") overallMaxEventNumber = eventNum
    else overallMaxEventNumber = [ overallMaxEventNumber, eventNum ].max()
    if(overallMinTimestamp == "init") overallMinTimestamp = timestamp
    else overallMinTimestamp = [ overallMinTimestamp, timestamp ].min()
    if(overallMaxTimestamp == "init") overallMaxTimestamp = timestamp
    else overallMaxTimestamp = [ overallMaxTimestamp, timestamp ].max()

    // find the bin that contains this event
    def thisQaBinOpt = qaBins.findBin(timestamp.longValue())
    if(!thisQaBinOpt.isPresent()) continue; // skip if not found
    def thisQaBin    = thisQaBinOpt.get()
    def thisQaBinNum = thisQaBin.getBinNum()
    System.out.println("DEBUG: $eventNum $thisQaBinNum")

    // get list of PIDs, with list index corresponding to bank row
    pidList = (0..<particleBank.rows()).collect{ particleBank.getInt('pid',it) }
    //println "pidList = $pidList"

    // get the FC charge
    def fc = thisQaBin.getBeamChargeGated()
    def ufc = thisQaBin.getBeamCharge()

    // get helicity and fill helicity distribution
    def helicity = 0 // if undefined, default to 0
    if(hipoEvent.hasBank("REC::Event") && eventBank.rows()>0) {
      helicity = eventBank.getByte('helicity',0)
      thisQaBin.data.histTree.helic.dist.fill(helicity)
    }
    def helStr
    def helDefined
    switch(helicity) {
      case 1:  helStr='hp'; helDefined=true; break
      case -1: helStr='hm'; helDefined=true; break
      default: helDefined = false; helicity = 0; break
    }
    // get scaler helicity from `HEL::scaler`, and fill its charge-weighted distribution
    // NOTE: do not do this if FCmode==CUSTOM (since FC charge is wrong)
    if(hipoEvent.hasBank("HEL::scaler") && FCmode!=FCmodeEnum.CUSTOM) {
      helScalerBank.rows().times{ row -> // HEL::scaler readouts "pile up", so there are multiple bank rows in an event
        def sc_helicity = helScalerBank.getByte("helicity", row)
        def sc_fc       = helScalerBank.getFloat("fcupgated", row) // helicity-latched FC charge
        thisQaBin.data.histTree.helic.scaler.chargeWeighted.fill(sc_helicity, sc_fc)
      }
    }

    // get electron list, and increment the number of trigger electrons
    // - also finds the DIS electron, and calculates x,Q2,W,y,nu
    findParticles(11, thisQaBinNum)

    // CUT: if a DIS electron was found by `findParticles`
    if(disEleFound) {

      // CUT for pions: Q2 and W and y
      if( Q2>1 && W>2 && y<0.8) {

        // get pions, calculate their kinematics and fill histograms
        countEvent = false
        [
          [ findParticles(211, thisQaBinNum),  'pip' ],
          [ findParticles(-211, thisQaBinNum), 'pim' ],
        ].each{ pionList, pionName ->

          pionList.each { part ->

            // calculate z
            vecH.copy(part.vector())
            z = T.lorentzDot(vecTarget,vecH) / T.lorentzDot(vecTarget,vecQ)

            // CUT for pions: particle z
            if(z>0.3 && z<1) {

              // calculate momenta, theta, phiH
              def p     = vecH.p()
              def pT    = Math.hypot( vecH.px(), vecH.py() )
              def theta = vecH.theta()
              def phiH  = T.planeAngle( vecQ.vect(), vecEle.vect(), vecQ.vect(), vecH.vect() )

              // CUT for pions: if phiH is defined
              if(phiH>-10000) {

                // fill histograms
                if(helDefined) {
                  thisQaBin.data.histTree['helic']['sinPhi'][pionName][helStr].fill(Math.sin(phiH))
                }
                thisQaBin.data.histTree['inclusive'][pionName]['p'].fill(p)
                thisQaBin.data.histTree['inclusive'][pionName]['pT'].fill(pT)
                thisQaBin.data.histTree['inclusive'][pionName]['z'].fill(z)
                thisQaBin.data.histTree['inclusive'][pionName]['theta'].fill(theta)
                thisQaBin.data.histTree['inclusive'][pionName]['phiH'].fill(phiH)

                // tell event counter that this event has at least one particle added to histos
                countEvent = true
              }
            }
          }
        }

        if(countEvent) {

          // fill event-level histograms
          thisQaBin.data.histTree.DIS.Q2.fill(Q2)
          thisQaBin.data.histTree.DIS.W.fill(W)
          thisQaBin.data.histTree.DIS.x.fill(x)
          thisQaBin.data.histTree.DIS.y.fill(y)
          thisQaBin.data.histTree.DIS.Q2VsW.fill(W,Q2)

          // increment event counter
          evCount++
          if(evCount % 100 == 0) printDebug "found $evCount events which contain a pion"

        }
      }
    }

    evCountFull++
    if(evCountFull % 100000 == 0) System.out.println "analyzed $evCountFull events"

  } // end event loop
  reader.close()

} // end loop over hipo files


// correct the first and last QA bins' event number ranges
qaBins.correctLowerBound(overallMinEventNumber, overallMinTimestamp.longValue());
qaBins.correctUpperBound(overallMaxEventNumber, overallMaxTimestamp.longValue());

// write final QA bin's histograms
qaBins.each{ itBin ->
  def itBinNum = itBin.getBinNum()

  // loop through histTree, adding histos to the hipo file;
  T.exeLeaves( itBin.data.histTree, {
    outHipo.addDataSet(T.leaf)
  })
  //println "write histograms:"; T.printTree(itBin.data.histTree,{T.leaf.getName()})

  // get FC charge
  // NOTE: prior to the following PRs, we saved both the START and STOP values of the
  //       FC charge; however, we don't really need both of them since everything downstream
  //       only uses the difference STOP-START (except for a systematic uncertainty estimate
  //       for pass-1 RG-A, regarding gaps and overlaps of charge between consecutive bins).
  //       Nowadays we just save the accumulated charge only, setting STOP to be that, and START to 0
  //       - https://github.com/JeffersonLab/coatjava/pull/770
  //       - https://github.com/JeffersonLab/clas12-timeline/pull/367
  def ufcStart = 0
  def ufcStop  = itBin.getBeamCharge()
  def fcStart = 0
  def fcStop  = itBin.getBeamChargeGated()

  // write number of electrons and FC charge to datfile
  SECTORS.each{ sec ->
    datfileWriter << [ runnum, itBinNum ].join(' ') << ' '
    datfileWriter << [ itBin.getEventNumMin(), itBin.getEventNumMax() ].join(' ') << ' '
    datfileWriter << [ itBin.getTimestampMin(), itBin.getTimestampMax() ].join(' ') << ' '
    datfileWriter << [ sec+1, itBin.data.nElec[sec], itBin.data.nElecFT ].join(' ') << ' '
    datfileWriter << [ fcStart, fcStop, ufcStart, ufcStop, itBin.getMeanLivetime() ].join(' ') << '\n'
  }
  printDebug " - charge for QA bin $itBinNum:"
  printDebug "   - event number range: [ ${itBin.getEventNumMin()}, ${itBin.getEventNumMin()} ]"
  printDebug "   - gated-FC charge:    [ $fcStart, $fcStop ]"
  printDebug "   - ungated-FC charge:  [ $ufcStart, $ufcStop ]"

  // print some stats
  /*
  def nElecTotal = itBin.data.nElec*.value.sum()
  println "\nnumber of trigger electrons: $nElecTotal"
  println """number of electrons that satisified FD trigger cuts, but were not analyzed...
  ...because they had subdominant E: $caseCountNtrigGT1
  ...because there was a higher-E electron satisfying FT cuts: $caseCountNFTwithTrig"""
  caseCountNtrigGT1=0
  caseCountNFTwithTrig=0
  */
}


// print the QA bins
println "QA BINS =============================="
println "@ #runnum/I:binnum/I:number_of_bins/I:evnum_min/L:evnum_max/L:timestamp_min/L:timestamp_max/L:num_events/L"
qaBins.each{
  def num_events = it.getEventNumMax() - it.getEventNumMin()
  if(it.getBinNum() == 0) {
    num_events++ // since first bin has no lower bound
  }
  println "@ ${runnum} ${it.getBinNum()} ${qaBins.size()} ${it.getEventNumMin()} ${it.getEventNumMax()} ${it.getTimestampMin()} ${it.getTimestampMax()} ${num_events}"
}
println "END QA BINS =========================="

// cross check: is each QA bin's min and max FC charge within the FC charge values at the bin boundaries?
/*
prior to RGC, we had a 1 second clock such that the FC charge as a function of event number
is a bit non-monotonic, looking like

    charge
     |             /
     |            /
     |         /\/
     |        /
     |       /
     |    /\/
     |   /
     |  /
     +---------------- event num

If the bin boundary is on one of these non-monotonic jumps, the min or max FC charge within a bin
may be smaller or larger than the FC charge values at the bin boundaries
*/
def nonMonotonicityGr = new GraphErrors("nonMonotonicity_${runnum}_0") // one graph for all QA bins, so just set QA bin number to `0`
nonMonotonicityGr.setTitle("FC charge non-monotonicity vs. QA bin")
qaBins.each{ itBin ->
  def itBinNum = itBin.getBinNum()
  if(itBinNum+1 == qaBins.size()) return // can't cross check the last bin
  def fc_lb   = itBin.getChargeExtremum(QadbBin.ExtremumType.FIRST, QadbBin.ChargeType.GATED)
  def fc_ub   = itBin.getChargeExtremum(QadbBin.ExtremumType.LAST,  QadbBin.ChargeType.GATED)
  def fc_min  = itBin.getChargeExtremum(QadbBin.ExtremumType.MIN,   QadbBin.ChargeType.GATED)
  def fc_max  = itBin.getChargeExtremum(QadbBin.ExtremumType.MAX,   QadbBin.ChargeType.GATED)
  def ufc_lb  = itBin.getChargeExtremum(QadbBin.ExtremumType.FIRST, QadbBin.ChargeType.UNGATED)
  def ufc_ub  = itBin.getChargeExtremum(QadbBin.ExtremumType.LAST,  QadbBin.ChargeType.UNGATED)
  def ufc_min = itBin.getChargeExtremum(QadbBin.ExtremumType.MIN,   QadbBin.ChargeType.UNGATED)
  def ufc_max = itBin.getChargeExtremum(QadbBin.ExtremumType.MAX,   QadbBin.ChargeType.UNGATED)
  printDebug "FC charge cross check for bin ${itBinNum}"
  printDebug "  gated:   (lb,ub)   = [${fc_lb}, ${fc_ub}]"
  printDebug "           (min,max) = [${fc_min}, ${fc_max}]"
  printDebug "  ungated: (lb,ub)   = [${ufc_lb}, ${ufc_ub}]"
  printDebug "           (min,max) = [${ufc_min}, ${ufc_max}]"
  def chargeViaBounds = fc_ub  - fc_lb
  def chargeViaMinMax = fc_max - fc_min
  // not clear whether `chargeViaBounds` or `chargeViaMinMax` is the real charge, so we calculate their percent difference w.r.t. their mean:
  def ave = (chargeViaBounds + chargeViaMinMax) / 2.0
  def nonMonotonicity = ave==0 ? 0 : Math.abs(chargeViaBounds - chargeViaMinMax) / ave
  nonMonotonicityGr.addPoint(itBinNum, nonMonotonicity, 0.0, 0.0)
}
outHipo.addDataSet(nonMonotonicityGr)

// close output text files
datfileWriter.flush()
datfileWriter.close()

// write outHipo file
outHipoN = "$outDir/monitor_${runnum}.hipo"
File outHipoFile = new File(outHipoN)
if(outHipoFile.exists()) outHipoFile.delete()
outHipo.writeFile(outHipoN)
println("Wrote the following files:")
println(" - $outHipoN")
println(" - $datfileName")
