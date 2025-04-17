// dump the contents of all the timeline plots for a given timeline HIPO file or timeline URL
import org.jlab.groot.data.TDirectory

def outFilePrefix = "out"
if(args.length<1) {
  System.err.println """
  USAGE: groovy ${this.class.getSimpleName()}.groovy [TIMELINE] [OUTPUT FILE PREFIX]
  - [TIMELINE] may either be a timeline URL or a timeline HIPO file
  - [OUTPUT FILE PREFIX] for the dumped timelines (default = $outFilePrefix)
  """
  System.exit(101)
}
if(args.length>1) outFilePrefix = args[1]
def inSpec = args[0]
def inFiles = []
if(inSpec ==~ /^https.*clas12mon.jlab.org.*timeline.*/) {
  def inDir = inSpec.replaceAll(/^.*jlab.org/, "/u/group/clas/www/clas12mon/html/hipo")
  inDir = "/"+inDir.tokenize('/')[0..-2].join("/")
  def inDirObj = new File(inDir)
  inDirObj.traverse(type: groovy.io.FileType.FILES, nameFilter: ~/.*\.hipo/) {
    if(it.size()>0) inFiles << inDir+"/"+it.getName()
  }
} else {
  inFiles << inSpec
}
System.out.println "inFiles:"
inFiles.each{System.out.println " - $it"}
System.out.println "outFilePrefix: $outFilePrefix"

def outFileNames = []

inFiles.each{ inFile ->

  def inTdir = new TDirectory()
  try {
    inTdir.readFile(inFile)
  } catch(Exception ex) {
    System.err.println("ERROR: cannot read file $inFile; it may be corrupt")
    System.exit(100)
  }

  def objList   = inTdir.getCompositeObjectList(inTdir)
  def timelines = []
  def plots     = []
  def tlGraphs  = []

  objList.each { objN ->
    tok = objN.tokenize('/')
    if(tok[0] == "timelines") {
      if( !(tok[1] ==~ /^plotLine.*/) && !(tok[1] ==~ /.*__bad$/) ) {
        timelines << tok[1]
        tlGraphs << inTdir.getObject(objN)
      }
    }
    else {
      if( ! tok[1].contains(":") ) {
        plots << tok[1]
      }
    }
  }
  plots.unique()

  inFileBase = inFile
    .tokenize('/')[-2..-1]
    .collect{ it.replaceAll(/.hipo$/,'') }[-1]

  // System.out.println """
  // > ${inFileBase}
  // > timelines = $timelines
  // > plots     = $plots
  // """

  def numRunsCheck = tlGraphs.collect{ it.getDataSize(0) }.unique()
  if(numRunsCheck.size()>1) {
    System.err.println("ERROR: timelines have differing number of runs")
    System.exit(100)
  }
  def numRuns = numRunsCheck[0]
  System.out.println "numRuns: $numRuns"

  def outFileName = "${outFilePrefix}_${inFileBase}.dat"
  def outFile = new File(outFileName)
  def outFileWriter = outFile.newWriter(false)

  numRuns.times{
    def runnum = tlGraphs[0].getDataX(it)
    if(it==0) {
      outFileWriter << "#runnum " << tlGraphs.collect{gr->"'${gr.getName()}'"}.join(' ') << '\n'
    }
    outFileWriter << runnum.toInteger() << ' '
    tlGraphs.each{ gr ->
      if(runnum != gr.getDataX(it)) {
        System.err.println("ERROR: timelines have differing run numbers")
        System.exit(100)
      }
      outFileWriter << gr.getDataY(it) << ' '
    }
    outFileWriter << '\n'
  }
  outFileWriter.close()
  outFileNames << outFileName

}

System.out.println "DONE. Timelines dumped to:"
outFileNames.each{System.out.println " - $it"}
