import groovy.json.JsonSlurper
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
  println("ChargeTreeToSQLite.groovy [dataset] [chargeTree_path]")
  System.exit(0)
}
if(args.length>=1) dataset = args[0]
def out_dir = "QA/qa."+dataset+"/" //NOTE: KEEP THIS THE SAME AS THE DEFAULT PATH IN JSONTOSQLITE.GROOVY.  //TODO: CHECK THESE PATHS AND MAYBE CLEAN ALL THIS UP A BIT.
def chargeTree_path = "outdat.${dataset}/chargeTree.json"
if(args.length>=2) qaTree_path = args[1]; out_dir = './'
def db_path = out_dir+dataset+"_FROM_JSON.db"
def table = 'chargeTree'
//--------------------------------------------------------------------------

//--------------------------------------------------------------------------
// CREATE SQL DATABASE AND TABLE
// def db_path = "outdat."+dataset+"/"+dataset+".db" //TODO: Add option to manually specify path to db //NOTE: Not needed here.
def sql = Sql.newInstance("jdbc:sqlite:"+db_path, "org.sqlite.JDBC")
def force = false // force overwrite of database table
if (force) sql.execute("drop table if exists "+table)
try { sql.execute("create table "+table+
      " (id integer, run integer, filenum integer,"+
      " fcChargeMin double, fcChargeMax double,"+
      " ufcChargeMin double, ufcChargeMax double,"+
      " livetime double,"+
      " nElec_sec1 integer, nElec_sec2 integer, nElec_sec3 integer,"+
      " nElec_sec4 integer, nElec_sec5 integer, nElec_sec6 integer,"+
      " comment text)")
} catch (SQLException e) {
  println "*** WARNING ***  Database table ${table} already exists."
  e.printStackTrace()
  // System.exit(0) //NOTE: Don't exit here since for FT option you should just update existing database.
}
def db
try { db = sql.dataSet(table)
} catch (SQLException e) {
  println "*** ERROR *** Could not open table ${table}."
  e.printStackTrace()
  System.exit(0)
}
def db_id = sql.rows("select count(*) as nrows from "+table)[0]["nrows"] // global counter for entries added to database table
//--------------------------------------------------------------------------

// define qaTree
def slurper = new JsonSlurper()
def jsonFile = new File(chargeTree_path)
def chargeTree = slurper.parse(jsonFile) // [runnum][filenum] -> defects enumeration

// Add chargeTree data to database
chargeTree.each { chargeRun, chargeRunTree ->
  chargeRunTree.each { chargeFile, chargeFileTree ->
    def nElec = chargeFileTree["nElec"]
    def sector = 1
    db.add(id:db_id,
        run:chargeRun,
        filenum:chargeFile,
        fcChargeMin:chargeFileTree['fcChargeMin'],
        fcChargeMax:chargeFileTree['fcChargeMax'],
        ufcChargeMin:chargeFileTree['ufcChargeMin'],
        ufcChargeMax:chargeFileTree['ufcChargeMax'],
        livetime:chargeFileTree['livetime'],
        nElec_sec1:nElec["${sector++}"], //NOTE: THESE KEYS ARE STRINGS OF INTS NOT INTS.
        nElec_sec2:nElec["${sector++}"],
        nElec_sec3:nElec["${sector++}"],
        nElec_sec4:nElec["${sector++}"],
        nElec_sec5:nElec["${sector++}"],
        nElec_sec6:nElec["${sector++}"],
        comment:""
        )
    db_id += 1
  }
}

//--------------------------------------------------------------------------
println "Converted JSON file to SQLite database in:\n\t${db_path}"
