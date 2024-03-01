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
//----------------------------------------------------------------------------------

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
  }
  T.addLeaf(chargeTree,[runnum,binnum,'nElec',sector],{nElec})
}

chargeTree.each { chargeRun, chargeRunTree -> chargeRunTree.sort{it.key.toInteger()} }
chargeTree.sort()
new File("${inDir}/chargeTree.json").write(JsonOutput.toJson(chargeTree))
