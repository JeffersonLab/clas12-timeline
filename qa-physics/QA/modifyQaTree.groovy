import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.util.Date
import org.jlab.clas.timeline.util.Tools
import org.rcdb.*
Tools T = new Tools()


// list of commands and description
def usage = [:]
usage["setbit"]     = "overwrites stored defectBit(s) with specified bit"
usage["addbit"]     = "add specified bit to defectBit(s)"
usage["delbit"]     = "delete specified bit from defectBit(s)"
usage["sectorloss"] = "specify a sector loss"
usage["lossft"]     = "specify a FT loss"
usage["nobeam"]     = "add 'PossiblyNoBeam' bit"
usage["misc"]       = "add the Misc bit, with default comment from shift expert"
usage["setcomment"] = "change or delete the comment"
usage["addcomment"] = "append a comment"
usage["custom"]     = "do a custom action (see code)"
println("\n\n")


// check arguments and print usage
def exe = "modify.sh"
def cmd
if(args.length>=1) cmd = args[0].toLowerCase()
else {
  System.err.println(
  """
  SYNTAX: ${exe} [command] [arguments]\n
List of Commands:
  """)
  usage.each{ key, value -> printf("%20s     %s\n", key, value) }
  printf("\nType any command without arguments for usage guidance for that command\n\n")
  System.exit(101)
}


// backup qaTree.json
def D = new Date()
("cp qa/qaTree.json qa/qaTree.json."+D.getTime()+".bak").execute()


// open qaTree.json
infile="qa/qaTree.json"
def slurper = new JsonSlurper()
def jsonFile = new File(infile)
def qaTree = slurper.parse(jsonFile)


// subroutine to recompute defect bitmask
def recomputeDefMask = { runnum,binnum ->
  defList = []
  defMask = 0
  (1..6).each{ s ->
    qaTree["$runnum"]["$binnum"]["sectorDefects"]["$s"].unique()
    defList +=
      qaTree["$runnum"]["$binnum"]["sectorDefects"]["$s"].collect{it.toInteger()}
  }
  defList.unique().each { defMask += (0x1<<it) }
  qaTree["$runnum"]["$binnum"]["defect"] = defMask
}



///////////////////////
// COMMAND EXECUTION //
///////////////////////


if( cmd=="setbit" || cmd=="addbit" || cmd=="delbit") {
  def rnum,bnumL,bnumR
  def bit
  def secList = []
  if(args.length>2) {
    bit = args[1].toInteger()
    rnum = args[2].toInteger()
    bnumL = args.length < 4 ?  0 : args[3].toInteger()
    bnumR = args.length < 5 ? -1 : args[4].toInteger()
    if(args.length<6 || args[5]=="all") secList = (1..6).collect{it}
    else (5..<args.length).each{ secList<<args[it].toInteger() }

    println("run $rnum bins ${bnumL}-"+(bnumR==-1 ? "END" : bnumR) +
      " sectors ${secList}: $cmd ${bit}="+T.bitNames[bit])

    println("Enter a comment, if you want, otherwise press return")
    print("> ")
    def cmt = System.in.newReader().readLine()


    qaTree["$rnum"].each { k,v ->
      def qaFnum = k.toInteger()
      if( qaFnum>=bnumL && ( bnumR==-1 || qaFnum<=bnumR ) ) {

        if(cmd=="setbit") {
          secList.each{
            qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] = [bit]
          }
        }
        else if(cmd=="addbit") {
          secList.each{
            qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] += bit
          }
        }
        else if(cmd=="delbit") {
          secList.each{
            qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] -= bit
          }
        }

        recomputeDefMask(rnum,qaFnum)

        if(cmt.length()>0) qaTree["$rnum"]["$qaFnum"]["comment"] = cmt
      }
    }

  }
  else {
    def helpStr = usage["$cmd"]
    System.err.println(
    """
    SYNTAX: ${exe} ${cmd} [defectBit] [run] [firstBin (default=0)] [lastBin (default=-1)] [list_of_sectors (default=all)]
      -$helpStr
      - set [lastBin] to -1 to denote last time bin of run
      - use \"all\" in place of [list_of_sectors] to apply to all sectors
      - you will be prompted to enter a comment
    """)
    System.err.println("Bit List:\n")
    T.bitDefinitions.size().times {
      System.err.println(sprintf("%5d %20s:   %s", it, T.bitNames[it], T.bitDescripts[it]))
    }
    System.err.println("\n")
    System.exit(101)
  }
}

else if(cmd=="sectorloss") {
  def rnum,bnumL,bnumR
  def secList = []
  if(args.length>3) {
    rnum = args[1].toInteger()
    bnumL = args[2].toInteger()
    bnumR = args[3].toInteger()
    if(args.length<5 || args[4]=="all") secList = (1..6).collect{it}
    else (4..<args.length).each{ secList<<args[it].toInteger() }

    println("run $rnum bins ${bnumL}-"+(bnumR==-1 ? "END" : bnumR) +
      " sectors ${secList}: define sector loss")

    println("Enter a comment, if you want, otherwise press return")
    print("> ")
    def cmt = System.in.newReader().readLine()

    qaTree["$rnum"].each { k,v ->
      def qaFnum = k.toInteger()
      if( qaFnum>=bnumL && ( bnumR==-1 || qaFnum<=bnumR ) ) {

        secList.each{
          // qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] -= T.bit("TotalOutlier")
          // qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] -= T.bit("TerminalOutlier")
          // qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] -= T.bit("MarginalOutlier")
          qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] += T.bit("SectorLoss")
        }

        recomputeDefMask(rnum,qaFnum)

        if(cmt.length()>0) qaTree["$rnum"]["$qaFnum"]["comment"] = cmt
      }
    }

  }
  else {
    def helpStr = usage["$cmd"]
    System.err.println(
    """
    SYNTAX: ${exe} ${cmd} [run] [firstBin] [lastBin] [list_of_sectors (default=all)]
      -$helpStr
      - set [lastBin] to -1 to denote last time bin of run
      - use \"all\" in place of [list_of_sectors] to apply to all sectors
      - this will add the SectorLoss bit for specified time bins and sectors
      - you will be prompted to enter a comment
    """)
    System.exit(101)
  }
}

else if(cmd=="nobeam") {
  def rnum,bnumL,bnumR
  def secList = (1..6).collect{it}
  if(args.length>3) {
    rnum = args[1].toInteger()
    bnumL = args[2].toInteger()
    bnumR = args[3].toInteger()

    println("run $rnum bins ${bnumL}-"+(bnumR==-1 ? "END" : bnumR) +
      " sectors ${secList}: add PossiblyNoBeam bit")

    println("Enter an additional comment, if you want, otherwise press return")
    print("> ")
    def cmt = System.in.newReader().readLine()
    cmt = ['manually added PossiblyNoBeam defect bit', cmt].findAll{it.length()>0}.join('; ')

    qaTree["$rnum"].each { k,v ->
      def qaFnum = k.toInteger()
      if( qaFnum>=bnumL && ( bnumR==-1 || qaFnum<=bnumR ) ) {
        secList.each{
          qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] += T.bit("PossiblyNoBeam")
        }
        recomputeDefMask(rnum,qaFnum)
        if(cmt.length()>0) {
          if(!qaTree["$rnum"]["$qaFnum"].containsKey("comment")) {
            qaTree["$rnum"]["$qaFnum"]["comment"] = cmt
          }
          else {
            if(qaTree["$rnum"]["$qaFnum"]["comment"].length()>0)
              qaTree["$rnum"]["$qaFnum"]["comment"] += "; "
            qaTree["$rnum"]["$qaFnum"]["comment"] += cmt
          }
        }
      }
    }

  }
  else {
    def helpStr = usage["$cmd"]
    System.err.println(
    """
    SYNTAX: ${exe} ${cmd} [run] [firstBin] [lastBin]
      -$helpStr
      - set [lastBin] to -1 to denote last time bin of run
      - this will set the PossiblyNoBeam bit for specified time bins, and add
        a comment saying this was done
      - you will be prompted to enter an additional comment
    """)
    System.exit(101)
  }
}

else if(cmd=="misc") {
  def rnum,bnumL,bnumR
  def secList = []
  if(args.length>1) {
    rnum = args[1].toInteger()
    bnumL = args.length < 3 ?  0 : args[2].toInteger()
    bnumR = args.length < 4 ? -1 : args[3].toInteger()
    if(args.length<5 || args[4]=="all") secList = (1..6).collect{it}
    else (4..<args.length).each{ secList<<args[it].toInteger() }

    def rcdbURL = System.getenv('RCDB_CONNECTION')
    if(rcdbURL==null)
      throw new Exception("RCDB_CONNECTION not set")
    def db = RCDB.createProvider(rcdbURL)
    def shift_expert_comment = null
    try {
      db.connect()
      shift_expert_comment = db.getCondition(Long.valueOf(rnum), 'user_comment').toString()
    }
    catch(Exception e) {
      System.err.println("Unable to connect to RCDB provider")
      System.exit(100)
    }
    if(shift_expert_comment == null) {
      System.err.println("Failed to get shift expert's comment from RCDB")
      System.exit(100)
    }

    println("run $rnum bins ${bnumL}-"+(bnumR==-1 ? "END" : bnumR) +
      " sectors ${secList}: add Misc bit with shift expert's comment")
    println("\n---------------- Shift Expert's Comment ----------------\n${shift_expert_comment}\n--------------------------------------------------------\n")
    println("Enter a different comment, if you want, otherwise press return to use this one")
    print("> ")
    def cmt = System.in.newReader().readLine()
    if(cmt == "") {
      cmt = shift_expert_comment
    }

    qaTree["$rnum"].each { k,v ->
      def qaFnum = k.toInteger()
      if( qaFnum>=bnumL && ( bnumR==-1 || qaFnum<=bnumR ) ) {
        secList.each{
          qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["$it"] += T.bit("Misc")
        }
        recomputeDefMask(rnum,qaFnum)
        if(cmt.length()>0) {
          qaTree["$rnum"]["$qaFnum"]["comment"] = cmt
        }
      }
    }

  }
  else {
    def helpStr = usage["$cmd"]
    System.err.println(
    """
    SYNTAX: ${exe} ${cmd} [run] [firstBin (default=0)] [lastBin (default=-1)] [list_of_sectors (default=all)]
      -$helpStr
      - set [lastBin] to -1 to denote last time bin of run
      - use \"all\" in place of [list_of_sectors] to apply to all sectors
      - you will be prompted to enter a different comment, if you do not
        want to use the shift expert comment
    """)
    System.exit(101)
  }
}

else if(cmd=="lossft") {
  def rnum,bnumL,bnumR
  if(args.length>3) {
    rnum = args[1].toInteger()
    bnumL = args[2].toInteger()
    bnumR = args[3].toInteger()

    println("run $rnum bins ${bnumL}-"+(bnumR==-1 ? "END" : bnumR) + ": define sector loss")

    println("Enter a comment, if you want, otherwise press return")
    print("> ")
    def cmt = System.in.newReader().readLine()

    qaTree["$rnum"].each { k,v ->
      def qaFnum = k.toInteger()
      if( qaFnum>=bnumL && ( bnumR==-1 || qaFnum<=bnumR ) ) {

        // qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["1"] -= T.bit("TotalOutlierFT")
        // qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["1"] -= T.bit("TerminalOutlierFT")
        // qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["1"] -= T.bit("MarginalOutlierFT")
        qaTree["$rnum"]["$qaFnum"]["sectorDefects"]["1"] += T.bit("LossFT")

        recomputeDefMask(rnum,qaFnum)

        if(cmt.length()>0) qaTree["$rnum"]["$qaFnum"]["comment"] = cmt
      }
    }

  }
  else {
    def helpStr = usage["$cmd"]
    println(
    """
    SYNTAX: ${exe} ${cmd} [run] [firstBin] [lastBin]
      -$helpStr
      - set [lastBin] to -1 to denote last time bin of run
      - this will add the LossFT bit for specified time bins
      - you will be prompted to enter a comment
    """)
    System.exit(101)
  }
}

else if( cmd=="addcomment" || cmd=="setcomment") {
  def rnum,bnumL,bnumR
  def secList = []
  if(args.length==4) {
    rnum = args[1].toInteger()
    bnumL = args[2].toInteger()
    bnumR = args[3].toInteger()

    println("run $rnum bins ${bnumL}-"+(bnumR==-1 ? "END" : bnumR) + ": $cmd")
    if(cmd=="addcomment")
      println("Enter the new comment to be appended")
    else if(cmd=="setcomment")
      println("Enter the new comment, or leave it blank to delete any stored comment")
    print("> ")
    def cmt = System.in.newReader().readLine()
    qaTree["$rnum"].each { k,v ->
      def qaFnum = k.toInteger()
      if( qaFnum>=bnumL && ( bnumR==-1 || qaFnum<=bnumR ) ) {
        if (cmd=="addcomment") {
          if(!qaTree["$rnum"]["$qaFnum"].containsKey("comment")) {
            qaTree["$rnum"]["$qaFnum"]["comment"] = cmt
          }
          else {
            if(qaTree["$rnum"]["$qaFnum"]["comment"].length()>0)
              qaTree["$rnum"]["$qaFnum"]["comment"] += "; "
            qaTree["$rnum"]["$qaFnum"]["comment"] += cmt
          }
        }
        else if (cmd=="setcomment")
          qaTree["$rnum"]["$qaFnum"]["comment"] = cmt
      }
    }
  }
  else {
    System.err.println(
    """
    SYNTAX: ${exe} ${cmd} [run] [firstBin] [lastBin]
      - set [lastBin] to -1 to denote last time bin of run
      - you will be prompted to enter the comment
    """)
    System.exit(101)
  }
}

else if( cmd=="custom") {
  // this cmd is useful if you want to do a specific action, while
  // calling this groovy script from another program;
  // - it is likely you need to modify this block
  def rnum,bnum

  // arguments
  /* // [runnum] [binnum]; operate on single time bin
  if(args.length==3) {
    rnum = args[1].toInteger()
    bnum = args[2].toInteger()
  } else System.exit(101)
  */
  ///* // [runnum]; operate on full run
  if(args.length==2) {
    rnum = args[1].toInteger()
  } else System.exit(101)
  //*/

  qaTree["$rnum"].each { k,v -> bnum = k.toInteger() // loop over bins

    /* // remove outlier bits and add misc bit, to all sectors
    def secList = (1..6).collect{it}
    secList.each{
      qaTree["$rnum"]["$bnum"]["sectorDefects"]["$it"] += T.bit("Misc") // add bit
      //qaTree["$rnum"]["$bnum"]["sectorDefects"]["$it"] = [T.bit("Misc")] // set bit
      qaTree["$rnum"]["$bnum"]["sectorDefects"]["$it"] -= T.bit("TotalOutlier") // delete bit
      qaTree["$rnum"]["$bnum"]["sectorDefects"]["$it"] -= T.bit("TerminalOutlier") // delete bit
      qaTree["$rnum"]["$bnum"]["sectorDefects"]["$it"] -= T.bit("MarginalOutlier") // delete bit
    }
    def cmt = "setup period; possible beam modulation issues"
    */

    /* // add misc bit to sector 6 only
    qaTree["$rnum"]["$bnum"]["sectorDefects"]["6"] += T.bit("Misc")
    def cmt = "FADC failure in ECAL sector 6; see https://logbooks.jlab.org/entry/3678262"
    */

    ///* // low helicity fraction
    def secList = (1..6).collect{it}
    secList.each{
      qaTree["$rnum"]["$bnum"]["sectorDefects"]["$it"] += T.bit("Misc")
    }
    def cmt = "fraction of events with defined helicity is low"
    //*/

    if(!qaTree["$rnum"]["$bnum"].containsKey("comment")) {
      qaTree["$rnum"]["$bnum"]["comment"] = cmt
    }
    else {
      if(qaTree["$rnum"]["$bnum"]["comment"].length()>0)
        qaTree["$rnum"]["$bnum"]["comment"] += "; "
      qaTree["$rnum"]["$bnum"]["comment"] += cmt
    }
    // println("modify $rnum $bnum")

    recomputeDefMask(rnum,bnum)

  } // end loop over bins
}


else { System.err.println("ERROR: unknown command!"); System.exit(100) }


// update qaTree.json
new File("qa/qaTree.json").write(JsonOutput.toJson(qaTree))
["${System.getenv('TIMELINESRC')}/bin/run-groovy-timeline.sh", "parseQaTree.groovy"].execute().waitFor()
