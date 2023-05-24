import groovy.json.JsonOutput

//--------------------------------------------------------------------------
// SQLite3 IMPORTS AND DEPENDENCIES
@Grapes([
 @Grab(group='org.xerial',module='sqlite-jdbc',version='3.7.2'),
 @GrabConfig(systemClassLoader=true)
])
 
import java.sql.*
import org.sqlite.SQLite
import groovy.sql.Sql
//--------------------------------------------------------------------------

//--------------------------------------------------------------------------
// ARGUMENTS:
dataset = 'inbending1'
if(args.length<1) {
  println("SQLiteToJSON.groovy [dataset] [db_path] [useFT]")
}
if(args.length>=1) dataset = args[0]
def out_dir = "outdat."+dataset+"/"
def db_path = out_dir+dataset+".db"
if(args.length>=2) { db_path = args[1]; out_dir = './' }
def useFT = false
if(args.length>=3) useFT = true
def qaTree_path = out_dir+"qaTree"+(useFT?"FT":"")+"_FROM_SQL.json"
//--------------------------------------------------------------------------

//--------------------------------------------------------------------------
// OPEN SQL DATABASE CONNECTION AND TABLE AND FILL QA TREE
def sectors = 0..6
def sec = { int i -> i+1 }
def sql
try { sql = Sql.newInstance("jdbc:sqlite:"+db_path, "org.sqlite.JDBC")
} catch (SQLException e) {
  println "*** ERROR *** Database ${dataset} does not exist."
  e.printStackTrace()
  System.exit(0)
}
def db
def qaTree = [:]
def det = useFT ? 'eFT' : 'eCFD'
def getBitsFromMask = { bitMask -> return Integer.toString(bitMask,2).collect{s->s as Integer} }
try {  sql.eachRow("select * from "+dataset+" where detector=='${det}'") { //TODO: Check for relevant column entries?   Also since you can have loss bit and 1/3 outlier bits need to convert back to lists...

    // Check entries
    if (!qaTree.keySet().contains(it.run)) { qaTree[it.run] = [:] }
    if (!qaTree[it.run].keySet().contains(it.filenum)) {
        qaTree[it.run][it.filenum] = ['evnumMin':it.evmin,'evnumMax':it.evmax,'comment':it.comment,'defect':0,'sectorDefects':sectors.collectEntries{s->[sec(s),[]]}]
    }

    // Add entries
    qaTree[it.run][it.filenum]['defect'] = it.defect //NOTE: This should be the same for all sector entries
    def sectorDefects = getBitsFromMask(it.sectordefect)
    qaTree[it.run][it.filenum]['sectorDefects'][it.sector].addAll(sectorDefects)

} } catch (SQLException e) {
  println "*** ERROR *** Could not open table ${dataset}."
  e.printStackTrace()
  System.exit(0)
}
//--------------------------------------------------------------------------

//--------------------------------------------------------------------------
// SORT AND WRITE OUT QATREE
qaTree.each { qaRun, qaRunTree -> qaRunTree.sort{it.key.toInteger()} }
qaTree.sort()

new File(qaTree_path).write(JsonOutput.toJson(qaTree))
println "Converted SQLite database to JSON tree in:\n\t${qaTree_path}"
//--------------------------------------------------------------------------
