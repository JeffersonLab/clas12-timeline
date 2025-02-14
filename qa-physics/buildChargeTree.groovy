import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

//----------------------------------------------------------------------------------
// ARGUMENTS:
if(args.length<1) {
  System.err.println "USAGE: run-groovy ${this.class.getSimpleName()}.groovy [INPUT_DIR]"
  System.exit(101)
}
inDir = args[0] + "/outdat"
monDir = args[0] + "/outmon"
//----------------------------------------------------------------------------------

def monFile
def inMdir = new TDirectory()
def runPrevious = -1 // initialize the previous run number
def binPrevious = -1 // initialize the previous bin number
def helicity = []    // list to hold the helicity values per run per bin
def helState = [-1,0,1]  // labels for the json file
def tok
int r=0
def runnum, binnum, sector
def eventNumMin, eventNumMax
def timestampMin, timestampMax
def nElec, nElecFT
def fcStart, fcStop
def ufcStart, ufcStop
def livetime
def fcCharge
def ufcCharge
def chargeTree = [:] // [runnum][binnum] -> charge

// open data_table.dat
def dataFile = new File("${inDir}/data_table.dat")
if(!(dataFile.exists())) throw new Exception("data_table.dat not found")
dataFile.eachLine { line ->

  // tokenize
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
  livetime = tok.size()>11 ? tok[r++].toBigDecimal() : -1

  // open monitor_<runnum>.hipo
  // data_table.dat has lines that repeat the run number and bin number.
  // Use runPrevious and binPrevious to avoid repeating the extraction of the histogram bin contents
  if(runnum!=runPrevious){
    runPrevious = runnum
    binPrevious = -1
    monFile = new File("${monDir}/monitor_${runnum}.hipo")
    println "Opening $monFile"
    if(!(monFile.exists())) throw new Exception("monitor<run>.hipo not found")
    try {
      inMdir.readFile(monDir + "/" + monFile.getName())
    } catch(Exception ex) {
      System.err.println("ERROR: cannot read file $monFile; it may be corrupt")
      return
    }
  }

  if(binnum!=binPrevious){
    binPrevious = binnum
    def histName = "helic_scaler_chargeWeighted_" + runnum + "_" + binnum
    H1F hist = (H1F)inMdir.getObject(runnum + "/",histName);
    helicity.add(hist.getBinContent(0))
    helicity.add(hist.getBinContent(1))
    helicity.add(hist.getBinContent(2))
  }

  // fill tree
  if(sector==1) {
    println "add $runnum $binnum"
    T.addLeaf(chargeTree,[runnum,binnum],
      {[
        'fcChargeMin':fcStart,
        'fcChargeMax':fcStop,
        'ufcChargeMin':ufcStart,
        'ufcChargeMax':ufcStop,
        'livetime':livetime
      ]}
    )
    helState.eachWithIndex { it, i -> T.addLeaf(chargeTree,[runnum,binnum,'fcChargeHelicity',it],{helicity[i]})}
    helicity.clear()  // reset the helicity list
  }
  T.addLeaf(chargeTree,[runnum,binnum,'nElec',sector],{nElec})
}

chargeTree.each { chargeRun, chargeRunTree -> chargeRunTree.sort{it.key.toInteger()} }
chargeTree.sort()
new File("${inDir}/chargeTree.json").write(JsonOutput.toJson(chargeTree))
