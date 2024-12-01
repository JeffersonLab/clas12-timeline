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
  println("JSONToSQLite.groovy [dataset] [qaTree_path] [useFT]")
  System.exit(0)
}
if(args.length>=1) dataset = args[0]
def out_dir = "QA/qa."+dataset+"/" //TODO: CHECK THESE PATHS AND MAYBE CLEAN ALL THIS UP A BIT.
def useFT = false
if(args.length>=3) useFT = true
def qaTree_path = "QA/qa.${dataset}/qaTree"+(useFT?"FT":"")+".json"
if(args.length>=2) qaTree_path = args[1]; out_dir = './'
def db_path = out_dir+dataset+"_FROM_JSON.db"
def table = 'qaTree'
//--------------------------------------------------------------------------

//--------------------------------------------------------------------------
// CREATE SQL DATABASE AND TABLE
// def db_path = "outdat."+dataset+"/"+dataset+".db" //TODO: Add option to manually specify path to db //NOTE: Not needed here.
def sql = Sql.newInstance("jdbc:sqlite:"+db_path, "org.sqlite.JDBC")
def force = false // force overwrite of database table
if (force) sql.execute("drop table if exists "+table)
try { sql.execute("create table "+table+
      " (id integer, run integer, filenum integer,"+
      " evmin integer, evmax integer,"+
      " detector string,"+
      " defect integer, sector integer, sectordefect integer, comment string)")
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
def db_id = sql.rows("select count(*) as nrows from "+table)[0]["nrows"] // global counter for entries added to database
//--------------------------------------------------------------------------

// define qaTree
def slurper = new JsonSlurper()
def jsonFile = new File(qaTree_path)
def qaTree = slurper.parse(jsonFile) // [runnum][filenum] -> defects enumeration

// assign defect masks
// def db_id = db.rows() //NOTE: Defined above just after opening sql database. Global database id is needed for adding entries.
def det = useFT ? 'eFT' : 'eCFD' //NOTE: The naming here is for forwards compatibility with changes to database structure.
qaTree.each { qaRun, qaRunTree -> 
  qaRunTree.each { qaFile, qaFileTree ->
    def defList = []
    def defMask = 0
    qaFileTree["sectorDefects"].each { qaSec, qaDefList ->
      defList += qaDefList.collect{it.toInteger()}
    }
    defList.unique().each { defMask += (0x1<<it) }
    if (qaTree[qaRun][qaFile]["defect"] != defMask) println("*** WARNING *** Defect and sectorDefects do not agree @ run, file = ${qaRun}, ${qaFile}")
    // qaTree[qaRun][qaFile]["defect"] = defMask //NOTE: This block should be identical to that in qaCut.groovy, but don't update the qaTree here since you're reading it from file already.

    // Add entries to SQL Database for each sector
    qaFileTree["sectorDefects"].each { qaSec, qaDefList ->
      def sectorDefMask = 0
      qaDefList.unique().each { sectorDefMask += (0x1<<it) }
      db.add(id:db_id,
            run:qaRun,
            filenum:qaFile,
            evmin:qaFileTree['evnumMin'],
            evmax:qaFileTree['evnumMax'],
            detector:det,
            defect:defMask,
            sector:qaSec,
            sectordefect:sectorDefMask,
            comment:""
            )
      db_id += 1
    }
  }
}

//--------------------------------------------------------------------------
println "Converted JSON file to SQLite database in:\n\t${db_path}"
