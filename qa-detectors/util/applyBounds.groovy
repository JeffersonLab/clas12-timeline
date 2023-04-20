import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import java.lang.Math.*
import qa.Tools
Tools T = new Tools()


// ARGUMENTS ****************************
def dataset = 'rga_spring19'
if(args.length>=1) dataset = args[0]
// **************************************


// get www dir, by searching datasetList.txt for specified `dataset`, and set
// `indir` and `outdir`
def calibqadir = System.getenv('CALIBQA')
def wwwdir = System.getenv('TIMELINEDIR')
if(calibqadir==null) throw new Exception("env vars not set; source env.sh")
File datasetList = new File(calibqadir+"/datasetList.txt")
def datasetFound = false
def indir,outdir
if(!(datasetList.exists())) throw new Exception("datasetList.txt not found")
datasetList.eachLine { line ->
  def tok = line.tokenize(' ')
  if(dataset==tok[0]) { // find the specified dataset
    indir = wwwdir+"/"+tok[1]
    outdir = wwwdir+"/"+tok[2]
    datasetFound = true
  }
}
if(!datasetFound) throw new Exception("unknown dataset \"$dataset\"")



// define tree "B" of closures for each calibration QA constraint
// -first branch: detectors (=directory names)
// -second branch: hipo file name
// -leaf name: graph name
// -leaf: closure for QA cuts
// -- note: relevant units are in comment
// also define tree "L", which lists bound lines to draw
def B = [:]
def L = [:]

// loop through cuts list
File cutsFile = new File(calibqadir+"/cuts/cuts.txt")
def tok
if(!(cutsFile.exists())) throw new Exception("cuts file $cutsFile not found")
def lastWord = { str -> str.tokenize('_')[-1] }
cutsFile.eachLine { line ->
  line = line.replaceAll(/#.*/,'')
  tok = line.tokenize(' ')
  if(tok.size()==0) return
  cutPath = tok[0..-4]
  def det = cutPath[0]
  def timeline = cutPath[1]
  def spec = cutPath.size()>2 ? cutPath[2] : ''
  def lbound = tok[-3].toDouble()
  def ubound = tok[-2].toDouble()
  def units = tok[-1]

  // add cuts to graph
  def addCut = { graphN ->
    T.addLeaf(B,[det,timeline,graphN],{[lbound,ubound]})
    T.addLeaf(L,[det,timeline],{[]})
    T.getLeaf(L,[det,timeline]).push(lbound)
    T.getLeaf(L,[det,timeline]).push(ubound)
  }

  // graph name convention varies among detector timelines, such as including
  // sector dependence, or layer dependence, or a timeline with a single graph;
  // - here we parse the detector and timeline names and determine which graph(s)
  //   the cut bounds apply to
  if(det=='ft') addCut(timeline.contains('ftc') ? lastWord(timeline) : spec)
  else if(det=='rich') addCut('fwhm_max')
  else if(det=='ctof') addCut(timeline.contains('edep') ? 'Edep' : lastWord(timeline))
  else if(det=='cnd') {
    (1..3).each{ layernum -> addCut('layer'+layernum+' '+lastWord(timeline)) }
  }
  else { // sector dependent detectors
    (1..6).each{ sec ->
      if(det=='rf') addCut('sec'+sec)
      else if(det=='ftof') addCut('sec'+sec)
      else if(det=='ltcc' && (sec==3 || sec==5)) addCut('sec'+sec)
      else if(det=='htcc') {
        if(timeline.contains("vtimediff")) {
          def rings = (1..4).collect()
          def sides = (1..2).collect()
          if(timeline.contains("sector_ring"))
            rings.each{ ring -> addCut(['sector', sec, 'ring', ring].join(' ')) }
          else if(timeline.contains("sector"))
            addCut(['sector', sec].join(' '))
          else
            [rings,sides].combinations().each{ ring, side -> addCut(['sector', sec, 'ring', ring, 'side', side].join(' ')) }
        }
        else addCut('sec'+sec)
      }
      else if(det=='ec') {
        if(timeline=='ec_Sampling') addCut('sec'+sec)
        else if(sec==1) addCut(lastWord(timeline)) // sector independent
      }
      else if(det=='dc') {
        (1..6).each{ sl ->
          def plotname = 'sec'+sec+' '+'sl'+sl
          if(spec=='R1' && (sl==1 || sl==2)) addCut(plotname)
          else if(spec=='R2' && (sl==3 || sl==4)) addCut(plotname)
          else if(spec=='R3' && (sl==5 || sl==6)) addCut(plotname)
        }
      }
    }
  }

}
T.exeLeaves(L,{ T.leaf = T.leaf.unique() })


// ==============================================================================

println "=== TIMELINES ========================="
T.exeLeaves(B,{println T.leafPath})
println T.pPrint(L)
println "======================================="


// closure for creating lines for the front end graphs
// - lineTitle* must be set before calling this
def lineTitle, lineTitleX, lineTitleY
def buildLine = { v,color ->
  def graphLine = new GraphErrors(['plotLine','horizontal',v,color].join(':'))
  graphLine.setTitle(lineTitle)
  graphLine.setTitleX(lineTitleX)
  graphLine.setTitleY(lineTitleY)
  return graphLine
}


// apply boundaries, for each closure defined above
// - loops through B tree, and read in corresponding input timeline
// - create bad timeline, for runs where a calibration QA constraint fails
// - loop through the input timeline, test constraints, add result to bad
//   timeline as necessary
// - store bad timeline in a tree "TL" with same structure as "T"
File inTdirFile
def gr
def TL = [:]
T.exeLeaves(B,{

  // setup
  def graphPath = T.leafPath
  def fileN = indir+'/'+graphPath[0,-2].join('/') + ".hipo"
  def bounds = T.leaf

  // read input timeline; do nothing if input timeline file
  // does not exist
  def graphN = graphPath[-1]
  T.printStatus("open file=\"$fileN\" graph=\"$graphN\"")
  def inTdir = new TDirectory()
  inTdirFile = new File(fileN)
  if(inTdirFile.exists()) {
    inTdir.readFile(fileN)
    gr = inTdir.getObject("/timelines/${graphN}")

    // define output bad timeline
    T.addLeaf(TL,graphPath,{
      def g = new GraphErrors()
      g.setName(gr.getName()+"__bad")
      g.setTitle(gr.getTitle())
      g.setTitleX(gr.getTitleX())
      g.setTitleY(gr.getTitleY())
      return g
    })


    // loop over runs
    gr.getDataSize(0).times { i ->

      // check QA bounds
      def run = gr.getDataX(i)
      def val = gr.getDataY(i)
      def inbound = val>=bounds[0] && val<=bounds[1]
      if(!inbound) {
        //T.printStatus("OB "+graphPath+" $run $val")
        T.getLeaf(TL,graphPath).addPoint(run,val,0,0)
      }

    }
  }
})


// write output timelines
TL.each{ det, detTr -> // loop through detector directories
  detTr.each{ hipoFile, graphTr -> // loop through timeline hipo files

    // create output TDirectory
    def outTdir = new TDirectory()

    // write graphs
    outTdir.mkdir("/timelines")
    outTdir.cd("/timelines")
    graphTr.each{ graphName, graph ->
      outTdir.addDataSet(graph)
      lineTitle = graph.getTitle()    // (note: all graphs have same title,
      lineTitleX = graph.getTitleX()  //  but we need it for `buildLine`)
      lineTitleY = graph.getTitleY()
    }

    // copy TDirectories for each run from input hipo file
    def inTdir = new TDirectory()
    def inHipoN = "${indir}/${det}/${hipoFile}.hipo"
    inTdir.readFile(inHipoN)
    def inList = inTdir.getCompositeObjectList(inTdir)
    inList.each{
      if(!it.contains("timelines")) {
        def rundir = it.tokenize('/')[0]
        outTdir.mkdir("/$rundir")
        outTdir.cd("/$rundir")
        outTdir.addDataSet(inTdir.getObject(it))
      } else {
        outTdir.cd("/timelines")
        def graphName = it.replaceAll(/^.*\//,'')
        def graph = inTdir.getObject(it)
        outTdir.addDataSet(graph)
      }
    }

    // add cut lines
    outTdir.cd("/timelines")
    T.getLeaf(L,[det,hipoFile]).eachWithIndex{ num,idx ->
      println "LINE: $det $hipoFile $num"
      def lineColor = 'black'
      if(hipoFile=="fth_MIPS_energy") {
        def lineColors = ['red','red','blue','blue']
        lineColor = lineColors[idx]
      }
      else if(hipoFile=="fth_MIPS_time_sigma") {
        def lineColors = ['black','red','blue']
        lineColor = lineColors[idx]
      }
      outTdir.addDataSet(buildLine(num,lineColor))
    }



    // create output hipo file
    def outHipoDir = "${outdir}/${det}"
    def outHipoN = "${outHipoDir}/${hipoFile}_QA.hipo"
    File outHipoFile = new File(outHipoN)
    if(outHipoFile.exists()) outHipoFile.delete()
    outTdir.writeFile(outHipoN)
  }
}
