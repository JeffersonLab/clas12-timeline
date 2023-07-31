// generate ListOfTimelines.json file, for hipo files in the online directory's
// subirectories
import static groovy.io.FileType.FILES
import groovy.json.JsonOutput

def wwwDirName = System.getenv("TIMELINEDIR") + "/" + System.getenv("LOGNAME")
if(args.length>=1) wwwDirName = args[0]

def wwwDir = new File(wwwDirName)
def datasets = []
wwwDir.traverse(
  type: groovy.io.FileType.DIRECTORIES, maxDepth: 0 ) 
{ if(!it.getName().contains("calib")) datasets << it.getName() }
datasets.sort()
println datasets

def fileTreeList = []
def fileTree = [:]
def datasetDir
def timelineList = []
datasets.each { datasetDirName ->
  timelineList = []
  fileTree = [:]
  fileTree.put('subsystem',datasetDirName)

  datasetDir = new File("${wwwDirName}/${datasetDirName}")
  datasetDir.traverse(
    type: groovy.io.FileType.FILES,
    nameFilter: ~/.*\.hipo/ )
  { 
    timelineList << it.getName().replaceAll(/\.hipo$/,"")
  }

  fileTree.put('variables',timelineList)
  fileTreeList << fileTree
}

//println JsonOutput.prettyPrint(JsonOutput.toJson(fileTreeList))
new File("${wwwDirName}/ListOfTimelines.json").write(JsonOutput.toJson(fileTreeList))
