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

def helState = [-1,0,1]  // labels for the json file
def chargeTree = [:] // [runnum][binnum] -> charge
def runTree = [:] // [runnum] -> list of bin numbers

// open data_table.dat
def dataFile = new File("${inDir}/data_table.dat")
if(!(dataFile.exists())) throw new Exception("data_table.dat not found")
dataFile.eachLine { line ->

  // tokenize
  def tok = line.tokenize(' ')
  int r=0
  def runnum = tok[r++].toInteger()
  def binnum = tok[r++].toInteger()
  def eventNumMin = tok[r++].toBigInteger()
  def eventNumMax = tok[r++].toBigInteger()
  def timestampMin = tok[r++].toBigInteger()
  def timestampMax = tok[r++].toBigInteger()
  def sector = tok[r++].toInteger()
  def nElec = tok[r++].toBigDecimal()
  def nElecFT = tok[r++].toBigDecimal()
  def fcStart = tok[r++].toBigDecimal()
  def fcStop = tok[r++].toBigDecimal()
  def ufcStart = tok[r++].toBigDecimal()
  def ufcStop = tok[r++].toBigDecimal()
  def livetime = tok.size()>11 ? tok[r++].toBigDecimal() : -1



  // fill tree
  if(sector==1) {
    T.addLeaf(runTree, [runnum], {[]})
    runTree[runnum].add(binnum)
    T.addLeaf(chargeTree,[runnum,binnum],
      {[
        'fcChargeMin':fcStart,
        'fcChargeMax':fcStop,
        'ufcChargeMin':ufcStart,
        'ufcChargeMax':ufcStop,
        'livetime':livetime
      ]}
    )
  }
  T.addLeaf(chargeTree,[runnum,binnum,'nElec',sector],{nElec})
}


// open monitor_*.hipo files
runTree.each { runnum, binnums ->

  // open monitor_<runnum>.hipo
  def monFile = new File("${monDir}/monitor_${runnum}.hipo")
  def inMdir = new TDirectory()
  if(!(monFile.exists())) throw new Exception("monitor<run>.hipo not found")
  try {
    inMdir.readFile(monDir + "/" + monFile.getName())
  } catch(Exception ex) {
    System.err.println("ERROR: cannot read file $monFile; it may be corrupt")
    System.exit(100)
  }

  // add helicity-latched charge for each bin
  binnums.each { binnum ->
    def histName = "helic_scaler_chargeWeighted_" + runnum + "_" + binnum
    def hist = (H1F)inMdir.getObject(runnum + "/",histName);
    def helicityCharge = []
    helicityCharge.add(hist.getBinContent(0))
    helicityCharge.add(hist.getBinContent(1))
    helicityCharge.add(hist.getBinContent(2))
    helState.eachWithIndex { it, i -> T.addLeaf(chargeTree,[runnum,binnum,'fcChargeHelicity',it],{helicityCharge[i]})}
  }
}

chargeTree.each { chargeRun, chargeRunTree -> chargeRunTree.sort{it.key.toInteger()} }
chargeTree.sort()
new File("${inDir}/chargeTree.json").write(JsonOutput.toJson(chargeTree))
