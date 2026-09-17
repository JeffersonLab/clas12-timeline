// parses qa/qaTree.json into human readable format

import org.rcdb.*
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

def infile  = "qa/qaTree.json"
def outfile = "qa/qaTree.json.table"
def cnds    = ['user_comment', 'beam_current', 'beam_current_request', 'target']
if(args.size()>=1) {
  infile = args[0]
  outfile = "${infile}.table"
  args = args.minus([args[0]]) // remove json file from args since no longer needed
}

def outfileF = new File(outfile)
def outfileW = outfileF.newWriter(false)

// Print out help message
if(args.contains("-h") || args.contains("--help")) {
  System.out.println("Additional Options:")
  System.out.println(" -cnds=cnd1,cnd2,...     : Set RCDB fields to include in output table")
  System.out.println("                           default: '${cnds.join(',')}'")
  System.out.println(" -addCnds=cnd1,cnd2,...  : Add RCDB fields to output table")
  System.exit(0)
}

/* CCDB/RCDB Addresses for CLAS12 see https://indico.jlab.org/event/222/contributions/2343/attachments/1959/2468/clas12-dbases.pdf
MySql : mysql://clas12reader@clasdb/ccdb
SQLite : sqlite:///$CCDB_HOME/sql/ccdb.sqlite
MySql : mysql://rcdb@clasdb/rcdb
SQLite : sqlite:////group/clas12/rcdb/example.db
*/

// Open SQL database connection
def address = System.getenv('RCDB_CONNECTION')
if(address==null)
  throw new Exception("RCDB_CONNECTION not set")
def db      = RCDB.createProvider(address)
def success = false
try { db.connect(); success = true; System.out.println("Connected to "+address) }
catch(Exception e) {
  System.err.println("Unable to connect to rcdb provider "+address)
  System.err.println("\nProceeding without db...")
  success = false
}

// Print available conditions and exit if requested
if((args.contains("--list") || args.contains("-l")) && success) {
  Vector<ConditionType> cndTypes = db.getConditionTypes()
  HashMap<String, ConditionType> cndTypeByNames = db.getConditionTypeByNames()
  System.out.println("Available fields in RCDB at "+address+":")
  for(ConditionType cndType in cndTypes){
    String row = String.format("   %-30s %s", cndType.getName(), cndType.getValueType().toString())
    System.out.println(row);
  }
  System.exit(0)
}

// List of rcdb condition entries to add
if(!success) { cnds = []; args = []} // set cnds and args to empty if no db connection

// Add conditions from command line
for (arg in args) { 
  if(arg.startsWith("-cnds=")) {
    try { cnds = arg.split('=')[1].split(',')}
    catch(Exception e) {
      e.printStackTrace()
      System.exit(100)
    }    	
  }
  if(arg.startsWith("-addCnds=")) {
    try { cnds.addAll(arg.split('=')[1].split(',')) }
    catch(Exception e) {
      e.printStackTrace()
      System.exit(100)
    }
  }
} 

// Check if conditions given are in db
for (cnd in cnds) {
  if(!db.getConditionTypeByNames().keySet().contains(cnd)) {
    System.err.println("WARNING: Condition: "+cnd+" not found in db at "+address+"\nOmitting...")
    cnds = cnds.minus([cnd])
  }
}

def slurper = new JsonSlurper()
def jsonFile = new File(infile)
def qaTree = slurper.parse(jsonFile)
def defStr = []
qaTree.sort{a,b -> a.key.toInteger() <=> b.key.toInteger() }.each{
  run, runTree ->

  // Loop condition list and append db entries
  def head = ["RUN: $run"]
  for (cnd in cnds) {
    def condition = db.getCondition(Long.valueOf(run),cnd)
    entry = ""
    val = "String"
    try {val = condition.valueType.toString() }
    catch (Exception e) { System.err.println(" *** WARNING *** Value type for entry: "+cnd+" undefined") }
    switch(val) {
      case "Double":  entry = condition.toDouble();  break
      case "String":  entry = condition.toString();  break
      case "Long":    entry = condition.toLong();    break
      case "Boolean": entry = condition.toBoolean(); break
      default:        entry = condition.toString();  break
    }
    head += ["  ${cnd}: ${entry}"]
  }
  outfileW << """
==================================================================================
${head.join("\n")}
----------------------------------------------------------------------------------
"""

  runTree.sort{a,b -> a.key.toInteger() <=> b.key.toInteger() }.each{
    itBin, binTree ->
    def defect = binTree.defect
    defStr = [run, itBin.padRight(5)]
    if(binTree.comment!=null) {
      if(binTree.comment.length()>0) defStr += ":: ${binTree.comment} ::"
    }
    def getSecList = { bitNum ->
      def secList = []
      binTree.sectorDefects.each{
        if(bitNum in it.value) secList+=it.key
      }
      if(secList==["1", "2", "3", "4", "5", "6"]) {
        secList = ["all"]
      }
      return secList
    }

    if(defect>0) {
      T.bitNames.eachWithIndex { str,i ->
        if(defect >> i & 0x1) defStr += "${i}-${str}[${getSecList(i).join()}]".padLeft(28)
      }
    } else defStr += "|"
    outfileW << defStr.join(' ') << "\n"
    //outfileW << binTree.sectorDefects << "\n"
  }
}

outfileW.close()
System.out.println("\nparsed $infile to $outfile")
