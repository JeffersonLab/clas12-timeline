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
def fileQAFT

// loop through FD qaTree runs
// run number loop
qaTreeFD.each{ runnum, fileTree ->
  if(debug) println "RUN=$runnum ---------------"
  qaTreeMelded[runnum] = [:]

  // file number loop
  fileTree.each{ filenum, fileQAFD ->
    if(debug) println "\nrun=$runnum file=$filenum"
    qaTreeMelded[runnum][filenum] = [:]
    
    // get QA info from FT qaTree
    runQAFT = qaTreeFT[runnum]
    if(runQAFT != null) {
      fileQAFT = qaTreeFT[runnum][filenum]
    } else {
      fileQAFT = null
    }

    // get the comment from the FT qaTree file, if it exists; if not
    // grab the comment from the FD qaTree file
    if(fileQAFT != null) {
      comment = T.getLeaf(fileQAFT,['comment'])
    } else {
      comment = T.getLeaf(fileQAFD,['comment'])
    }
    if(comment==null) {
      comment = ""
    }
    qaTreeMelded[runnum][filenum]['comment'] = comment

    // copy event number range from FD qaTree file
    ['evnumMin', 'evnumMax'].each{ qaTreeMelded[runnum][filenum][it] = fileQAFD[it] }

    // loop through sectors and meld their defect bits
    meldListOR = []
    qaTreeMelded[runnum][filenum]['sectorDefects'] = [:]
    fileQAFD['sectorDefects'].each{ sector, defectListFD ->

      meldList = []

      // meld FD defect bits
      meldList += defectListFD

      // meld FT defect bits
      if(fileQAFT!=null) {
        defectListFT = T.getLeaf(fileQAFT,['sectorDefects',sector])
      }
      meldList+=defectListFT

      // add this sector's meldList to the OR of each sector's meldList,
      // and to the melded tree
      meldList.unique()
      qaTreeMelded[runnum][filenum]['sectorDefects'][sector] = meldList
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
    qaTreeMelded[runnum][filenum]['defect'] = defectMask

  } // end filenum loop
} // end runnum loop

// output melded qaTree.json
new File("$inDir/qaTree.json").write(JsonOutput.toJson(qaTreeMelded))
