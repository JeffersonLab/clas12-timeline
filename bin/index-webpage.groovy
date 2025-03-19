// generate ListOfTimelines.json file, for hipo files in the online directory's subdirectories

import static groovy.io.FileType.FILES
import groovy.json.JsonOutput

if(args.length!=1) {
  System.err.println """
  USAGE: groovy ${this.class.getSimpleName()}.groovy [WEBSERVER_DIRECTORY]
  """
  System.exit(101)
}

def wwwDirName = args[0]
def wwwDir = new File(wwwDirName)
def subDirs = []
wwwDir.traverse(type: groovy.io.FileType.DIRECTORIES, maxDepth: 0 ) {
  subDirs << it.getName()
}
println "GENERATING INDEX PAGE FOR $wwwDirName"
subDirs.sort()
println "SUBDIRECTORIES:"
println subDirs

def fileTreeList = []
subDirs.each { subDirName ->
  def timelineList = []
  def fileTree     = [:]
  fileTree.put('subsystem', subDirName)

  def subDir = new File("$wwwDirName/$subDirName")
  subDir.traverse( type: groovy.io.FileType.FILES, nameFilter: ~/.*\.hipo/ ) {
    timelineList << it.getName().replaceAll(/\.hipo$/,"")
  }

  fileTree.put('variables', timelineList)
  fileTreeList << fileTree
}

//println JsonOutput.prettyPrint(JsonOutput.toJson(fileTreeList))
new File("$wwwDirName/ListOfTimelines.json").write(JsonOutput.toJson(fileTreeList))
