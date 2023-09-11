// combine a new qaTree.json file with an old one
// - see README.md

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

def debug = false

def newFileN = "qaTree.json.new"
def oldFileN = "qaTree.json.old"
def newFile = new File(newFileN)
def oldFile = new File(oldFileN)
def slurper = new JsonSlurper()
def qaTreeNew = slurper.parse(newFile)
def qaTreeOld = slurper.parse(oldFile)
def qaTreeMelded = [:]
def defectListOld
def meldList
def meldListOR
def defectMask
def comment
def runQAold
def fileQAold
def deleteComment

// loop through new qaTree runs
// run number loop
qaTreeNew.each{ runnum, fileTree ->
  if(debug) println "RUN=$runnum ---------------"
  qaTreeMelded[runnum] = [:]

  // file number loop
  fileTree.each{ filenum, fileQAnew ->
    if(debug) println "\nrun=$runnum file=$filenum"
    qaTreeMelded[runnum][filenum] = [:]
    
    // get QA info from old qaTree
    runQAold = qaTreeOld[runnum]
    if(runQAold!=null) fileQAold = qaTreeOld[runnum][filenum]
    else fileQAold = null

    // get the comment from the old qaTree file, if it exists; if not
    // grab the comment from the new qaTree file
    if(fileQAold!=null) comment = T.getLeaf(fileQAold,['comment'])
    else comment = T.getLeaf(fileQAnew,['comment'])
    if(comment==null) comment=""
    qaTreeMelded[runnum][filenum]['comment'] = comment
    deleteComment = false

    // copy event number range from new qaTree file
    qaTreeMelded[runnum][filenum]['evnumMin'] = fileQAnew['evnumMin']
    qaTreeMelded[runnum][filenum]['evnumMax'] = fileQAnew['evnumMax']

    // loop through sectors and meld their defect bits
    meldListOR = []
    qaTreeMelded[runnum][filenum]['sectorDefects'] = [:]
    fileQAnew['sectorDefects'].each{ sector, defectListNew ->

      meldList = []

      // meld new defect bits
      defectListNew.each{ defect ->
        if(defect==T.bit("TotalOutlier")) meldList << defect
        if(defect==T.bit("TerminalOutlier")) meldList << defect
        if(defect==T.bit("MarginalOutlier")) meldList << defect
        if(defect==T.bit("LowLiveTime")) meldList << defect
      }

      // meld old defect bits
      if(fileQAold!=null) {
        defectListOld = T.getLeaf(fileQAold,['sectorDefects',sector])
        defectListOld.each{ defect ->
          
          if(defect==T.bit("SectorLoss")) {
            meldList << defect
            // remove outlier bits
            meldList.removeAll(T.bit("TotalOutlier"))
            meldList.removeAll(T.bit("TerminalOutlier"))
            meldList.removeAll(T.bit("MarginalOutlier"))
          }
          if(defect==T.bit("Misc")) {
            if(comment.contains("please delete this comment")) deleteComment=true
            else {
              meldList << defect
              // remove all other bits
              meldList.removeAll(T.bit("TotalOutlier"))
              meldList.removeAll(T.bit("TerminalOutlier"))
              meldList.removeAll(T.bit("MarginalOutlier"))
              meldList.removeAll(T.bit("SectorLoss"))
              meldList.removeAll(T.bit("LowLiveTime"))
            }
          }
        }
      }


      // add this sector's meldList to the OR of each sector's meldList,
      // and to the melded tree
      qaTreeMelded[runnum][filenum]['sectorDefects'][sector] = meldList
      meldListOR += meldList


      if(debug) {
        println "s${sector}"
        println "     new: $defectListNew"
        println "     old: $defectListOld"
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
    if(deleteComment) qaTreeMelded[runnum][filenum]['comment'] = ""

  } // end filenum loop
} // end runnum loop


// output melded qaTree.json
new File("qaTree.json.melded").write(JsonOutput.toJson(qaTreeMelded))
['new','old','melded'].each{"./prettyPrint.sh $it".execute()}
