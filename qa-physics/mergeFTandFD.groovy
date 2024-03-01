// combine qaTreeFD.json and qaTreeFT.json
// - see README.md

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

def debug = false

// parse arguments
if(args.length<1) {
  System.err.println "USAGE: run-groovy ${this.class.getSimpleName()}.groovy [INPUT_DIR]"
  System.exit(101)
}
inDir = args[0] + "/outdat"

// vars
def fdFileN = "$inDir/qaTreeFD.json"
def ftFileN = "$inDir/qaTreeFT.json"
def FDFile = new File(fdFileN)
def FTFile = new File(ftFileN)
def slurper = new JsonSlurper()
def qaTreeFD = slurper.parse(FDFile)
def qaTreeFT = slurper.parse(FTFile)
def qaTreeMelded = [:]
def defectListFT
def meldList
def meldListOR
def defectMask
def comment
def runQAFT
def binQAFT

// loop through FD qaTree runs
// run number loop
qaTreeFD.each{ runnum, binTree ->
  if(debug) println "RUN=$runnum ---------------"
  qaTreeMelded[runnum] = [:]

  // time bin loop
  binTree.each{ binnum, binQAFD ->
    if(debug) println "\nrun=$runnum bin=$binnum"
    qaTreeMelded[runnum][binnum] = [:]

    // get QA info from FT qaTree
    runQAFT = qaTreeFT[runnum]
    if(runQAFT != null) {
      binQAFT = qaTreeFT[runnum][binnum]
    } else {
      binQAFT = null
    }

    // get the comment from the FT qaTree bin, if it exists; if not
    // grab the comment from the FD qaTree bin
    if(binQAFT != null) {
      comment = T.getLeaf(binQAFT,['comment'])
    } else {
      comment = T.getLeaf(binQAFD,['comment'])
    }
    if(comment==null) {
      comment = ""
    }
    qaTreeMelded[runnum][binnum]['comment'] = comment

    // copy event number range from FD qaTree bin
    ['evnumMin', 'evnumMax'].each{ qaTreeMelded[runnum][binnum][it] = binQAFD[it] }

    // loop through sectors and meld their defect bits
    meldListOR = []
    qaTreeMelded[runnum][binnum]['sectorDefects'] = [:]
    binQAFD['sectorDefects'].each{ sector, defectListFD ->

      meldList = []

      // meld FD defect bits
      meldList += defectListFD

      // meld FT defect bits
      if(binQAFT!=null) {
        defectListFT = T.getLeaf(binQAFT,['sectorDefects',sector])
      }
      meldList+=defectListFT

      // add this sector's meldList to the OR of each sector's meldList,
      // and to the melded tree
      meldList.unique()
      qaTreeMelded[runnum][binnum]['sectorDefects'][sector] = meldList
      meldListOR += meldList

      // debugging printout
      if(debug) {
        println "s${sector}"
        println "     FD: $defectListFD"
        println "     FT: $defectListFT"
        println "  melded: $meldList"
      }

    } // end sector loop

    // compute defect bitmask
    defectMask = 0
    meldListOR.unique().each { defectMask += (0x1<<it) }
    if(debug) {
      println "--> DEFECT: $defectMask = " + T.printBinary(defectMask)
      println "--> comment: $comment"
    }
    qaTreeMelded[runnum][binnum]['defect'] = defectMask

  } // end binnum loop
} // end runnum loop

// output melded qaTree.json
new File("$inDir/qaTree.json").write(JsonOutput.toJson(qaTreeMelded))
