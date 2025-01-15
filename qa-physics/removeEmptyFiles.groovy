//-------------------------------------------------------------------------//
// Author: Matthew McEneaney
// Date: 1/15/25
// Description: Simple script to check for non-empty HIPO IDataSet objects
// (H1F, H2F, GraphErrors) in a set of files.
//-------------------------------------------------------------------------//

import groovy.io.FileType
import org.jlab.groot.data.IDataSet
import org.jlab.groot.data.H1F
import org.jlab.groot.data.H2F
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.Directory

//--------------------------------------------------------------------------
// ARGUMENTS:
if(args.length<1) {
  System.err.println "USAGE: run-groovy ${this.class.getSimpleName()}.groovy [INPUT_DIR]"
  System.exit(101)
}
def verbose = true
def inPaths = []
args.each{ path ->
    def d = new File(path)
    d.eachFileRecurse (FileType.FILES) { file ->
        if (file.name.endsWith(".hipo")) inPaths << file.path //NOTE: Only consider HIPO files.
    }
}
//--------------------------------------------------------------------------

// Initialize path lists
def emptyPaths = []
def outPaths = []

// Function to check if IDataSet object is non-empty
def checkObj (IDataSet obj, boolean verbose=false) { //NOTE: Use a statically typed argument from the base class of all your data types here.

    def flag = false
    if (obj instanceof H1F) {
        flag = obj.entries > 0
    } else if (obj instanceof H2F) {
        flag = obj.entries > 0
    } else if (obj instanceof GraphErrors) {
        flag = obj.vectorY.size > 0
    } else {
        if (verbose) System.err.println("WARNING: Unallowed HIPO object type "+obj.getClass()+"")
    }

    return flag

} // def checkObj (IDataSet obj) {

// Recursive function to check file for non-empty objects
// - NOTE: This will return true if there are non-empty objects within the directory or any subdirectories.
def checkDir (tdir, dirName, flag, osPathDelimiter="/", verbose=false) {  //NOTE: This needs to have a traditional function definition, not a closure, for recursion to work.
    
    // Open directory
    if (dirName!=osPathDelimiter) { tdir.cd(dirName) }
    else { tdir.cd() }

    // Check if any objects in object list are non-empty
    flag = (flag || tdir.getDir().objectList.collect{obj -> checkObj(obj, verbose)}.any{it})

    // Recursively check subdirectories for non-empty objects
    def checkDirResults = tdir.getDir().directoryList.collect{subDirName -> checkDir(tdir,[(dirName!=osPathDelimiter) ? dirName : "",subDirName].join(osPathDelimiter),flag,osPathDelimiter,verbose)}
    flag = (flag || checkDirResults.any{it})

    return flag

} // def checkDir (tdir, dirName, flag, osPathDelimiter = "/") {

// Function to check entire file directory structure for non-empty objects
def checkFile = { path ->

    // Open TDirectory
    def tdir = new TDirectory()
    tdir.readFile(path)
    tdir.cd() //NOTE: This is important because you want to start out in the top level directory.

    // Check TDirectory recursively for non-empty objects
    def osPathDelimiter = "/"
    def flag = checkDir(tdir,osPathDelimiter,false,osPathDelimiter,verbose)

    return flag

} // def checkFile = { path ->

// Loop paths and check file contents
inPaths.each{ path ->

    def nonEmpty = checkFile(path)
    if (nonEmpty) {
        outPaths.add(path)
    } else {
        emptyPaths.add(path)
    }

} // inPaths.each{ path ->

// Show files lists
if (verbose) {
    println "DEBUGGING: inPaths    = $inPaths"
    println "DEBUGGING: outPaths   = $outPaths"
    println "DEBUGGING: emptyPaths = $emptyPaths"
}

// Delete empty files
emptyPaths.each { path ->
    def file = new File(path)
    if (verbose) println "file.delete() -> $path"
    file.delete()
}
