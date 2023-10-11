// a more general monitor, for things like <sinPhiH> or helicity
// - this reads DST files or skim files
// - can be run on slurm
// - note: search for 'CUT' to find which cuts are applied

import org.jlab.io.hipo.HipoDataSource
import org.jlab.clas.physics.Particle
import org.jlab.clas.physics.Vector3
import org.jlab.groot.data.H1F
import org.jlab.groot.data.H2F
import org.jlab.groot.data.TDirectory
import org.jlab.clas.physics.LorentzVector
import org.jlab.clas.physics.Vector3
import org.jlab.detector.base.DetectorType
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.lang.Math.*
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

// CONSTANTS
def MIN_NUM_SCALERS = 2000   // at least this many scaler readouts per time bin
def MIN_NUM_EVENTS  = 380000 // at least this many events per time bin
def VERBOSE         = true   // enable extra log messages, for debugging
def printDebug = { msg -> if(VERBOSE) println "[DEBUG]: $msg" }

// ARGUMENTS
def inHipoType = "dst" // options: "dst", "skim"
def runnum = 0
if(args.length<2) {
  System.err.println """
  USAGE: run-groovy ${this.class.getSimpleName()}.groovy [HIPO file directory] [output directory] [type(OPTIONAL)] [runnum(OPTIONAL)]
         REQUIRED parameters:
           - [HIPO file directory] should be a directory of HIPO files, either
             DST file(s) or skim file(s)
           - [output directory] output directory for the produced files
         OPTIONAL parameters:
           - [type] can be 'dst' or 'skim' (default is '$inHipoType')
             NOTE: 'skim' file usage may not work
           - [runnum] the run number; if not specified, it will be obtained from RUN::config

  """
  System.exit(101)
}
def inHipo = args[0]
def outDir = args[1]
if(args.length>=3) inHipoType = args[2]
if(args.length>=4) runnum     = args[3].toInteger()
System.println """
inHipo     = $inHipo
outDir     = $outDir
inHipoType = $inHipoType"""

// get hipo file names
def inHipoList = []
if(inHipoType=="dst") {
  def inHipoDirObj = new File(inHipo)
  def inHipoFilter = ~/.*\.hipo/
  inHipoDirObj.traverse( type: groovy.io.FileType.FILES, nameFilter: inHipoFilter ) {
    if(it.size()>0) inHipoList << inHipo+"/"+it.getName()
  }
  inHipoList.sort()
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


// get runnum; assumes all HIPO files have the same run number
if(runnum<=0)
  runnum = T.getRunNumber(inHipoList.first())
if(runnum<=0)
  System.exit(100)
System.println "runnum     = $runnum"


//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
// RUN GROUP DEPENDENT SETTINGS //////////////////////////

def RG = "unknown"
if(runnum>=4763 && runnum<=5001) RG="RGA" // early period
else if(runnum>=5032 && runnum<=5262) RG="RGA" // inbending1
else if(runnum>=5300 && runnum<=5666) RG="RGA" // inbending1 + outbending
else if(runnum>=5674 && runnum<=6000) RG="RGK" // 6.5+7.5 GeV
else if(runnum>=6120 && runnum<=6604) RG="RGB" // spring 19
else if(runnum>=6616 && runnum<=6783) RG="RGA" // spring 19
else if(runnum>=11093 && runnum<=11300) RG="RGB" // fall 19
else if(runnum>=11323 && runnum<=11571) RG="RGB" // winter 20
else if(runnum>=12210 && runnum<=12951) RG="RGF" // spring+summer 20
else if(runnum>=15019 && runnum<=15884) RG="RGM" 
else if(runnum>=16043 && runnum<=16772) RG="RGC" // summer 22
else System.err.println "WARNING: unknown run group; using default run-group-dependent settings (see monitorRead.groovy)"
println "rungroup = $RG"

// beam energy
// - hard-coded; could instead get from RCDB, but sometimes it is incorrect
def EBEAM = 10.6041 // RGA default
if(RG=="RGA") {
  if(runnum>=6616 && runnum<=6783) EBEAM = 10.1998 // spring 19
  else EBEAM = 10.6041
}
else if(RG=="RGB") {
  if(runnum>=6120 && runnum<=6399) EBEAM = 10.5986 // spring
  else if(runnum>=6409 && runnum<=6604) EBEAM = 10.1998 // spring
  else if(runnum>=11093 && runnum<=11283) EBEAM = 10.4096 // fall
  else if(runnum>=11284 && runnum<=11300) EBEAM = 4.17179 // fall BAND_FT
  else if(runnum>=11323 && runnum<=11571) EBEAM = 10.3894 // winter (RCDB may still be incorrect)
  else System.err.println "ERROR: unknown beam energy"
}
else if(RG=="RGC") {
  if(runnum>=16010 && runnum<=16078) EBEAM = 2.21
  else if(runnum>=16079) EBEAM = 10.55
  else System.err.println "ERROR: unknown beam energy"
}
else if(RG=="RGK") {
  if(runnum>=5674 && runnum<=5870) EBEAM = 7.546
  else if(runnum>=5875 && runnum<=6000) EBEAM = 6.535
  else System.err.println "ERROR: unknown beam energy"
}
else if(RG=="RGF") {
  if     (runnum>=12210 && runnum<=12388) EBEAM = 10.389 // RCDB may still be incorrect
  else if(runnum>=12389 && runnum<=12443) EBEAM =  2.186 // RCDB may still be incorrect
  else if(runnum>=12444 && runnum<=12951) EBEAM = 10.389 // RCDB may still be incorrect
  else System.err.println "ERROR: unknown beam energy"
}
else if(RG=="RGM") {
  if     (runnum>=15013 && runnum<=15490) EBEAM = 5.98636 
  else if(runnum>=15533 && runnum<=15727) EBEAM = 2.07052 
  else if(runnum>=15728 && runnum<=15784) EBEAM = 4.02962 
  else if(runnum>=15787 && runnum<=15884) EBEAM = 5.98636 
  else System.err.println "ERROR: unknown beam energy"
}

/* gated FC charge determination: `FCmode`
 * - 0: DAQ-gated FC charge is incorrect
 *   - recharge option was likely OFF during cook, and should be turned on
 *   - re-calculates DAQ-gated FC charge as: `ungated FC charge * average livetime`
 *   - typically applies to data cooked with version 6.5.3 or below
 *   - typically used as a fallback if there are "spikes" in gated charge when `FCmode==1`
 * - 1: DAQ-gated FC charge can be trusted
 *   - recharge option was either ON or did not need to be ON
 *   - calculate DAQ-gated FC charge directly from `RUN::scaler:fcupgated`
 *   - if you find `fcupgated > fcup(un-gated)`, then most likely the recharge option was OFF
 *     but should have been ON, and `FCmode=0` should be used instead
 * - 2: special case
 *   - calculate DAQ-gated FC charge from `REC::Event:beamCharge`
 *   - useful if `RUN::scaler` is unavailable
 */
def FCmode = 1 // default assumes DAQ-gated FC charge can be trusted
if(RG=="RGA") {
  FCmode=1;
  if(runnum==6724) FCmode=0; // fcupgated charge spike in file 230
}
else if(RG=="RGB") {
  FCmode = 1
  if( runnum in [6263, 6350, 6599, 6601, 11119] ) FCmode=0 // fcupgated charge spikes
}
else if(RG=="RGC") FCmode = 1
else if(RG=="RGK") FCmode = 0
else if(RG=="RGF") FCmode = 0
else if(RG=="RGM") {		  
  FCmode = 1		 
  if(runnum>=15015 && runnum<=15199) { 
    FCmode = 2 //no scalars read out in this range probably dosen't work anyway 
  }
}

//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////

// make outut directories
"mkdir -p $outDir".execute()

// prepare output table for electron count and FC charge
def datfile = new File("$outDir/data_table_${runnum}.dat")
def datfileWriter = datfile.newWriter(false)


// define variables
def event
def timeBins = [:]
def pidList = []
def particleBank
def FTparticleBank
def configBank
def eventBank
def calBank
def scalerBank
def pipList = []
def pimList = []
def eleList = []
def disElectron
def disEleFound
def eleSec
def eventNum
def eventNumList = [] /////////////////// FIXME: refactor
def eventNumMin, eventNumMax /////////////////// FIXME: refactor
def timeBinEventCount = 0 /////////////////// FIXME: refactor
def timeBinScalerCount = 0 /////////////////// FIXME: refactor
def helicity
def helStr
def helDefined
def phi
def runnumTmp = -1
def evCount
def nbins
def sectors = 0..<6
def nElec = sectors.collect{0} /////////////////// FIXME: refactor
def nElecFT = 0 /////////////////// FIXME: refactor
def LTlist = [] /////////////////// FIXME: refactor
def FClist = [] /////////////////// FIXME: refactor
def UFClist = [] /////////////////// FIXME: refactor
def rellumG, rellumU
def detIdEC = DetectorType.ECAL.getDetectorId()
def Q2
def W
def nu
def x,y,z
def p,pT,theta,phiH
def countEvent
def caseCountNtrigGT1 = 0
def caseCountNFTwithTrig = 0
def nElecTotal

// lorentz vectors
def vecBeam = new LorentzVector(0, 0, EBEAM, EBEAM)
def vecTarget = new LorentzVector(0, 0, 0, 0.938)
def vecEle = new LorentzVector()
def vecH = new LorentzVector()
def vecQ = new LorentzVector()
def vecW = new LorentzVector()


// subroutine to increment the number of counted electrons
def disElectronInTrigger
def disElectronInFT
def countTriggerElectrons = { eleRows,eleParts ->

  // reset some vars
  def Emax = 0
  def Etmp
  disElectronInTrigger = false
  disElectronInFT = false
  def nTrigger = 0
  def nFT = 0
  disEleFound = false

  // loop over electrons from REC::Particle
  if(eleRows.size()>0) {
    eleRows.eachWithIndex { row,ind ->

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
            calBank.getByte('detector',it).toInteger() == detIdEC ) ?
            calBank.getByte('sector',it).toInteger() : null
        }.find()

        // CUT for electron: sector must be defined
        if(eleSecTmp!=null) {

          nTrigger++ // count how many trigger electrons we looked at

          // CUT for electron: choose maximum energy electron (for triggers)
          // - choice is from both trigger and FT electron sets (see below)
          Etmp = eleParts[ind].e()
          if(Etmp>Emax) {
            Emax = Etmp
            eleSec = eleSecTmp
            disElectronInTrigger = true
            disElectronInFT = false
            disElectron = eleParts[ind]
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
              eleParts[ind].e() > 0.3
          ) {

            nFT++ // count how many FT electrons we looked at

            // CUT for electron: maximum energy electron (for FT)
            // - choice is from both trigger and FT electron sets (see above)
            Etmp = eleParts[ind].e()
            if(Etmp>Emax) {
              Emax = Etmp
              disElectronInFT = true
              disElectronInTrigger = false
              disElectron = eleParts[ind]
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
    y = nu / EBEAM


    // CUT for electron: Q2 cut
    //if(Q2<2.5) return


    // - increment counters, and set `disEleFound`
    if(disElectronInTrigger && disElectronInFT) { // can never happen (failsafe)
      System.err.println "ERROR: disElectronInTrigger && disElectronInFT == 1; skip event"
      return
    }
    else if(disElectronInTrigger) {
      nElec[eleSec-1]++
      disEleFound = true
    }
    else if(disElectronInFT) {
      nElecFT++
      disEleFound = true
    }

    // increment 'case counters' (for studying overlap of trigger/FT cuts)
    // - case where there are more than one trigger electron in FD
    if(disElectronInTrigger && nTrigger>1)
      caseCountNtrigGT1 += nTrigger-1 // count number of unanalyzed extra electrons
    // - case where disElectron is in FT, but there are trigger electrons in FD
    if(disElectronInFT && nTrigger>0)
      caseCountNFTwithTrig += nTrigger // count number of unanalyzed trigger (FD) electrons

  }

}


// subroutine which returns a list of Particle objects of a certain PID
def findParticles = { pid ->

  // get list of bank rows and Particle objects corresponding to this PID
  def rowList = pidList.findIndexValues{ it == pid }.collect{it as Integer}
  def particleList = rowList.collect { row ->
    new Particle(pid,*['px','py','pz'].collect{particleBank.getFloat(it,row)})
  }
  //println "pid=$pid  found in rows $rowList"

  // if looking for electrons, also count the number of trigger electrons,
  // and find the DIS electron
  if(pid==11) countTriggerElectrons(rowList,particleList)

  // return list of Particle objects
  return particleList
}


// subroutine to get the FC charge and livetime, depending on `FCmode`
def getFCcharge = { scalerBankArg, eventBankArg ->
  if(scalerBankArg.rows()>0) {
    UFClist << scalerBankArg.getFloat("fcup",0)     // ungated charge
    LTlist  << scalerBankArg.getFloat("livetime",0) // livetime
    if(FCmode==1) {
      FClist << scalerBankArg.getFloat("fcupgated",0) // gated charge
    }
  }
  if(FCmode==2 && eventBankArg.rows()>0) {
    FClist << eventBankArg.getFloat("beamCharge",0) // gated charge
  }
}


// subroutine to calculate hadron (pion) kinematics, and fill histograms
// note: needs to have some kinematics defined (vecQ,Q2,W), and helStr
def fillHistos = { list, partN ->
  list.each { part ->

    // calculate z
    vecH.copy(part.vector())
    z = T.lorentzDot(vecTarget,vecH) / T.lorentzDot(vecTarget,vecQ)

    // CUT for pions: particle z
    if(z>0.3 && z<1) {

      // calculate momenta, theta, phiH
      p = vecH.p()
      pT = Math.hypot( vecH.px(), vecH.py() )
      theta = vecH.theta()
      phiH = T.planeAngle( vecQ.vect(), vecEle.vect(), vecQ.vect(), vecH.vect() )

      // CUT for pions: if phiH is defined
      if(phiH>-10000) {

        // fill histograms
        if(helDefined) {
          histTree['helic']['sinPhi'][partN][helStr].fill(Math.sin(phiH))
        }
        histTree['inclusive'][partN]['p'].fill(p)
        histTree['inclusive'][partN]['pT'].fill(pT)
        histTree['inclusive'][partN]['z'].fill(z)
        histTree['inclusive'][partN]['theta'].fill(theta)
        histTree['inclusive'][partN]['phiH'].fill(phiH)

        // tell event counter that this event has at least one particle added to histos
        countEvent = true
      }
    }
  }
}


// subroutine to write out to hipo file
def outHipo = new TDirectory()
outHipo.mkdir("/$runnum")
outHipo.cd("/$runnum")
def histN,histT
def writeHistos = {

  // get event number range
  eventNumMin = eventNumList.min()
  eventNumMax = eventNumList.max()

  // proceed only if there are data to write
  if(eventNumList.size()>0) {

    // loop through histTree, adding histos to the hipo file;
    T.exeLeaves( histTree, {
      outHipo.addDataSet(T.leaf) 
    })
    //println "write histograms:"; T.printTree(histTree,{T.leaf.getName()})


    // get accumulated FC charge
    def ufcStart
    def ufcStop
    if(UFClist.size()>0) {
      ufcStart = UFClist.min()
      ufcStop = UFClist.max()
    } else {
      System.err.println "WARNING: empty UFClist for run=${runnum} timeBinNum=${timeBinNum}"
      ufcStart = 0
      ufcStop = 0
    }

    def fcStart
    def fcStop
    LTlist.removeAll{it<0} // remove undefined livetime values
    def aveLivetime = LTlist.size()>0 ? LTlist.sum() / LTlist.size() : 0
    if(FCmode==0) {
      fcStart = ufcStart * aveLivetime // workaround method
      fcStop = ufcStop * aveLivetime // workaround method
    } else if(FCmode==1 || FCmode==2) {
      if(FClist.size()>0) {
        fcStart = FClist.min()
        fcStop = FClist.max()
      } else {
        System.err.println "WARNING: empty FClist for run=${runnum} timeBinNum=${timeBinNum}"
        fcStart = 0
        fcStop = 0
      }
    }
    if(fcStart>fcStop || ufcStart>ufcStop) {
      System.err.println "WARNING: faraday cup start > stop for run=${runnum} timeBinNum=${timeBinNum}"
    }

    // write number of electrons and FC charge to datfile
    sectors.each{ sec ->
      datfileWriter << [ runnum, timeBinNum ].join(' ') << ' '
      datfileWriter << [ eventNumMin, eventNumMax ].join(' ') << ' '
      datfileWriter << [ sec+1, nElec[sec], nElecFT ].join(' ') << ' '
      datfileWriter << [ fcStart, fcStop, ufcStart, ufcStop, aveLivetime ].join(' ') << '\n'
    }
    printDebug " - event number range: [ $eventNumMin, $eventNumMax ]"
    printDebug " - gated-FC charge:    [ $fcStart, $fcStop ]"
    printDebug " - ungated-FC charge:  [ $ufcStart, $ufcStop ]"

    // print some stats
    /*
    nElecTotal = nElec*.value.sum()
    println "\nnumber of trigger electrons: $nElecTotal" 
    println """number of electrons that satisified FD trigger cuts, but were not analyzed...
    ...because they had subdominant E: $caseCountNtrigGT1
    ...because there was a higher-E electron satisfying FT cuts: $caseCountNFTwithTrig""" 
    caseCountNtrigGT1=0
    caseCountNFTwithTrig=0
    */
  }
  else {
    System.err.println "WARNING: empty time bin (timeBinNum=$timeBinNum)"
    System.err.println " if all time bins in a run are empty, there will be more errors later!"
  }

  // reset number of trigger electrons counter and FC lists
  nElec = sectors.collect{0}
  nElecFT = 0
  UFClist = []
  FClist = []
  LTlist = []
}


// get list of tag1 event numbers
printDebug "Begin tag1 event loop"
def tag1eventNumList = []
inHipoList[0..5].each { inHipoFile ->   /////////////////// FIXME: short circuit

  printDebug "Open HIPO file $inHipoFile"
  def reader = new HipoDataSource()
  reader.getReader().setTags(1)
  reader.open(inHipoFile)
  while(reader.hasEvent()) {
    event = reader.getNextEvent()
    // printDebug "tag1 event bank list: ${event.getBankList()}"
    if(event.hasBank("RUN::scaler") && event.hasBank("RUN::config")) {
      tag1eventNumList << BigInteger.valueOf(event.getBank("RUN::config").getInt('event',0))
    }
  }
  reader.close()
}

// define the time bin boundaries: first, some sorting and transformations
timeBinBounds = tag1eventNumList
  .sort()                         // sort the event numbers
  .collate(MIN_NUM_SCALERS)       // partition
  .collect{ it[0] }               // take the first event number of each subset
  .plus(tag1eventNumList[-1])     // append the final event number
  .collect{ [it, it] }.flatten()  // double each element (since upper bound of bin N = lower bound of bin N+1)
// add the first and last bins by setting the absolute minimum to 0 and absolute maximum to a large number
timeBinBounds = [0] + timeBinBounds + [10**(Math.log10(timeBinBounds[-1]).toInteger()+1)]
// pair the elements to define the bin boundaries
timeBinBounds = timeBinBounds.collate(2)
// logging
println "TIME BIN BOUNDARIES: ["
timeBinBounds.each{ println "  $it," }
println "]"

// define the time bin objects, initializing additional fields
timeBinBounds.eachWithIndex{ bounds, binNum ->
  timeBins[binNum] = [
    eventNumMin: bounds[0],
    eventNumMax: bounds[-1],
    nElec:       sectors.collect{0},
    nElecFT:     0,
    fcStart:     "init",
    fcStop:      "init",
    ufcStart:    "init",
    ufcStop:     "init",
    LTlist:      [],
    histTree:    [:],
  ]
}

// subroutine to find the EARLIEST time bin for a given event number
// - if the event number is on a time-bin boundary, the earlier time bin will be returned
def findTimeBin = { evnum ->
  s = timeBins.find{ evnum >= it.value["eventNumMin"] && evnum <= it.value["eventNumMax"] }
  if(s==null) {
    System.err.println "ERROR: cannot find time bin for event number $evnum"
    return -1
  }
  s.key
}

// subroutine to update a min or max value in a time bin (viz. FC charge start and stop)
def setMinMax = { binNum, minmax, key, val ->
  valOld = timeBins[binNum][key]
  if(valOld==null) {
    System.err.println "ERROR: cannot find time bin $binNum with key $key"
  } else if(valOld=="init") {
    timeBins[binNum][key] = val
  } else if(minmax=="min") {
    timeBins[binNum][key] = [valOld, val].min()
  } else if(minmax=="max") {
    timeBins[binNum][key] = [valOld, val].max()
  } else {
    System.err.println "ERROR: minmax is not 'min' or 'max'"
  }
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

// initialize histograms for each time bin
timeBins.each{ binNum, timeBin ->
  def partList = [ 'pip', 'pim' ]
  def helList  = [ 'hp',  'hm'  ]
  def heluList = [ 'hp',  'hm', 'hu' ]

  T.buildTree(timeBin.histTree, 'helic',     [['sinPhi'],partList,helList],            { new H1F() })
  T.buildTree(timeBin.histTree, 'helic',     [['dist']],                               { new H1F() })
  T.buildTree(timeBin.histTree, 'helic',     [['distGoodOnly']],                       { new H1F() })
  T.buildTree(timeBin.histTree, 'DIS',       [['Q2','W','x','y']],                     { new H1F() })
  T.buildTree(timeBin.histTree, "DIS",       [['Q2VsW']],                              { new H2F() })
  T.buildTree(timeBin.histTree, "inclusive", [partList,['p','pT','z','theta','phiH']], { new H1F() })
  // if(binNum==0) T.printTree(timeBin.histTree,{T.leaf.getClass()});

  nbins = 50

  timeBin.histTree.helic.dist         = buildHist('helic_dist','helicity',[],runnum,3,-1,2)
  timeBin.histTree.helic.distGoodOnly = buildHist('helic_distGoodOnly','helicity (with electron cuts)',[],runnum,3,-1,2)
  timeBin.histTree.DIS.Q2             = buildHist('DIS_Q2','Q^2',[],runnum,2*nbins,0,12)
  timeBin.histTree.DIS.W              = buildHist('DIS_W','W',[],runnum,2*nbins,0,6)
  timeBin.histTree.DIS.x              = buildHist('DIS_x','x',[],runnum,2*nbins,0,1)
  timeBin.histTree.DIS.y              = buildHist('DIS_y','y',[],runnum,2*nbins,0,1)
  timeBin.histTree.DIS.Q2VsW          = buildHist('DIS_Q2VsW','Q^2 vs W',[],runnum,nbins,0,6,nbins,0,12)
  T.exeLeaves( timeBin.histTree.helic.sinPhi, {
    T.leaf = buildHist('helic_sinPhi','sinPhiH',T.leafPath,runnum,nbins,-1,1) 
  })
  T.exeLeaves( timeBin.histTree.inclusive, {
    def lbound=0
    def ubound=0
    if(T.key=='p')          { lbound=0; ubound=10 }
    else if(T.key=='pT')    { lbound=0; ubound=4 }
    else if(T.key=='z')     { lbound=0; ubound=1 }
    else if(T.key=='theta') { lbound=0; ubound=Math.toRadians(90.0) }
    else if(T.key=='phiH')  { lbound=-3.15; ubound=3.15 }
    T.leaf = buildHist('inclusive','',T.leafPath,runnum,nbins,lbound,ubound)
  })

  T.exeLeaves( timeBin.histTree, {
    histN = T.leaf.getName() + "_${binNum}"
    histT = T.leaf.getTitle() + " :: timeBinNum=${binNum}"
    T.leaf.setName(histN)
    T.leaf.setTitle(histT)
  })

  // print the histogram names and titles
  // if(binNum==0) {
    println "---\nhistogram names and titles:"
    T.printTree(timeBin.histTree,{ T.leaf.getName() +" ::: "+ T.leaf.getTitle() })
    println "---"
  // }
}

System.exit(0) // exit prematurely




/////////////////////////////////////////////////////
// EVENT LOOP
/////////////////////////////////////////////////////
evCount = 0
printDebug "Begin event loop"
inHipoList.each { inHipoFile ->

  // open HIPO file
  printDebug "Open HIPO file $inHipoFile"
  def reader = new HipoDataSource()
  reader.open(inHipoFile)

  // EVENT LOOP
  while(reader.hasEvent()) {
    //if(evCount>100000) break // limiter
    event = reader.getNextEvent()

    // get required banks
    particleBank   = event.getBank("REC::Particle")
    eventBank      = event.getBank("REC::Event")
    configBank     = event.getBank("RUN::config")
    FTparticleBank = event.getBank("RECFT::Particle")
    calBank        = event.getBank("REC::Calorimeter")
    scalerBank     = event.getBank("RUN::scaler")

    // get event number
    if(configBank.rows()>0) {
      eventNum = BigInteger.valueOf(configBank.getInt('event',0))
    } else {
      System.err.println "WARNING: cannot get event number for event with no RUN::config bank"
      continue
    }
    if(eventNum==0) {
      System.err.println "WARNING: found event with eventNum=0; banks: ${event.getBankList()}"
      continue
    }

    // find the time bin that contains this event
    timeBin = findTimeBin(eventNum)
    if(timeBin == -1) continue

    // get list of PIDs, with list index corresponding to bank row
    pidList = (0..<particleBank.rows()).collect{ particleBank.getInt('pid',it) }
    //println "pidList = $pidList"

////////////////////////////
////////////////////////////
////////////////////////////
//
// refactoring progress marker, everything below here is broken
// 
////////////////////////////
////////////////////////////
////////////////////////////


    // if we have enough events for a time bin, and if this is an event with a
    // scaler readout, write out filled histos and/or create new histos
    if(timeBinNum==-1 || (timeBinScalerCount>=MIN_NUM_SCALERS && timeBinEventCount>=MIN_NUM_EVENTS && isScalerEvent)) {

      // if this isn't the first time bin, get FC info for this event, and write out filled histograms
      if(timeBinNum>=0) {
        printDebug "End time bin $timeBinNum"
        printDebug " - isScalerEvent      = $isScalerEvent"
        printDebug " - timeBinEventCount  = $timeBinEventCount"
        printDebug " - timeBinScalerCount = $timeBinScalerCount"
        getFCcharge(scalerBank,eventBank)  // updates FClist and UFClist with this last scaler readout
        writeHistos()                      // resets FClist and UFClist
      }

      // update time bin number and reset counters
      timeBinNum++
      timeBinEventCount = 0
      timeBinScalerCount = 0
      printDebug "Start time bin $timeBinNum"
      printDebug " - isScalerEvent      = $isScalerEvent"
    }
    printDebug "    evnum: $eventNum   time bin counts: { events => $timeBinEventCount, scalers => $timeBinScalerCount }"

    // get FC charge
    getFCcharge(scalerBank,eventBank)
    if(isScalerEvent) {
      printDebug "      scaler event:"
      if(FClist.size()>0)  printDebug "       - gated FC charge: ${FClist[-1]}"
      if(UFClist.size()>0) printDebug "       - ungated FC charge: ${UFClist[-1]}"
    }


    // get helicity and fill helicity distribution
    if(event.hasBank("REC::Event")) helicity = eventBank.getByte('helicity',0)
    else helicity = 0 // (undefined)
    switch(helicity) {
      case 1:  helStr='hp'; helDefined=true; break
      case -1: helStr='hm'; helDefined=true; break
      default: helDefined = false; helicity = 0; break
    }
    histTree.helic.dist.fill(helicity)
    
    // get electron list, and increment the number of trigger electrons
    // - also finds the DIS electron, and calculates x,Q2,W,y,nu
    eleList = findParticles(11) // (`eleList` is unused)


    // CUT: if a dis electron was found (see countTriggerElectrons)
    if(disEleFound) {

      if(disElectronInTrigger)
        histTree.helic.distGoodOnly.fill(helicity)

      // CUT for pions: Q2 and W and y and helicity
      if( Q2>1 && W>2 && y<0.8 && helDefined) {

        // get lists of pions
        pipList = findParticles(211)
        pimList = findParticles(-211)

        // calculate pion kinematics and fill histograms
        // countEvent will be set to true if a pion is added to the histos 
        countEvent = false
        fillHistos(pipList,'pip')
        fillHistos(pimList,'pim')

        if(countEvent) {

          // fill event-level histograms
          histTree.DIS.Q2.fill(Q2)
          histTree.DIS.W.fill(W)
          histTree.DIS.x.fill(x)
          histTree.DIS.y.fill(y)
          histTree.DIS.Q2VsW.fill(W,Q2)

          // increment event counter
          evCount++
          if(evCount % 100000 == 0) println "found $evCount events which contain a pion"

        }
      }
    }

    // increment time bin event counters
    timeBinEventCount++
    if(isScalerEvent) timeBinScalerCount++

  } // end event loop
  reader.close()

} // end loop over hipo files

// write final time bin's histograms
printDebug "End time bin $timeBinNum"
printDebug " - isScalerEvent      = $isScalerEvent"
printDebug " - timeBinEventCount  = $timeBinEventCount"
printDebug " - timeBinScalerCount = $timeBinScalerCount"
writeHistos()

// write outHipo file
outHipoN = "$outDir/monitor_${runnum}.hipo"
File outHipoFile = new File(outHipoN)
if(outHipoFile.exists()) outHipoFile.delete()
outHipo.writeFile(outHipoN)

