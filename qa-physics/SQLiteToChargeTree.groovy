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
  println("SQLiteToChargeTree.groovy [dataset] [db_path]")
  System.exit(0)
}
if(args.length>=1) dataset = args[0]
def out_dir = "outdat."+dataset+"/"
def db_path = out_dir+dataset+".db"
if(args.length>=2) { db_path = args[1]; out_dir = './' }
def useFT = false
if(args.length>=3) useFT = true
def chargeTree_path = out_dir+"chargeTree_FROM_SQL.json"
def table = 'chargeTree'
//--------------------------------------------------------------------------

//--------------------------------------------------------------------------
// OPEN SQL DATABASE CONNECTION AND TABLE AND FILL CHARGE TREE
def sectors = 0..<6
def sec = { int i -> i+1 }
def sql
try { sql = Sql.newInstance("jdbc:sqlite:"+db_path, "org.sqlite.JDBC")
} catch (SQLException e) {
  println "*** ERROR *** Could not open ${db_path}."
  e.printStackTrace()
  System.exit(0)
}
def db
def chargeTree = [:]
try {  sql.eachRow("select * from "+table) { //TODO: Check for relevant column entries?   Also since you can have loss bit and 1/3 outlier bits need to convert back to lists...

    // Check and add entries
    if (!chargeTree.keySet().contains(it.run)) { chargeTree[it.run] = [:] }
    if (!chargeTree[it.run].keySet().contains(it.filenum)) {
        chargeTree[it.run][it.filenum] = [
        'fcChargeMin':it.fcChargeMin,'fcChargeMax':it.fcChargeMax,
        'ufcChargeMin':it.ufcChargeMin,'ufcChargeMax':it.ufcChargeMax,
        'livetime':it.livetime,'nElec':[1:it.nElec_sec1,2:it.nElec_sec2,3:it.nElec_sec3,4:it.nElec_sec4,5:it.nElec_sec5,6:it.nElec_sec6]] //NOTE: Hard-coded for now but maybe there is some more elegant way to do this...
    }
} } catch (SQLException e) {
  println "*** ERROR *** Could not open table ${table}."
  e.printStackTrace()
  System.exit(0)
}
//--------------------------------------------------------------------------

//--------------------------------------------------------------------------
// SORT AND WRITE OUT chargeTree
chargeTree.each { chargeRun, chargeRunTree -> chargeRunTree.sort{it.key.toInteger()} }
chargeTree.sort()

new File(chargeTree_path).write(JsonOutput.toJson(chargeTree))
println "Converted SQLite database to JSON tree in:\n\t${chargeTree_path}"
//--------------------------------------------------------------------------
