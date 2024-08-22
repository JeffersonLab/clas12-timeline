import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F
import org.jlab.groot.math.F1D
import java.lang.Math.*
import groovy.io.FileType
import groovy.json.JsonOutput
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

// Cuts files list //////////////////////////////////
/* list of pairs `[regex, cutsFile]`
 * - timeline file path is matched to `regex` to determine which cuts file(s) to use
 * - typically case: use the default `cuts.txt` file, with an overriding file specific
 *   to the run period
 */
def cutsFileList = [
  [ /./, "cuts.txt"], // default file
  [ /rga.*fa18/, "cuts_rga_fa18.txt"], // RGA Fall 2018
  [ /rgc/, "cuts_rgc_su22.txt"], // RGC
]
/////////////////////////////////////////////////////

// parse arguments
if(args.length!=2) { 
  System.err.println "USAGE: run-groovy ${this.class.getSimpleName()}.groovy [INPUT TIMELINES] [OUTPUT TIMELINES]"
  System.exit(100)
}
def indir  = args[0]
def outdir = args[1]

// get source dir
def calibqadir = System.getenv('TIMELINESRC')
if(calibqadir==null) throw new Exception("env vars not set; source environ.sh")
calibqadir += "/qa-detectors"

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
def tok
def lastWord = { str -> str.tokenize('_')[-1] }
cutsFileList.each { re, cutsFile ->

  // check if this cuts file should be read, by comparing `indir` to regex `re`
  print "Use $cutsFile?"
  if(indir =~ re) {
    println " YES."
    def cutsFileName = "$calibqadir/cuts/$cutsFile"
    File cutsFileHandle = new File(cutsFileName)
    if(!(cutsFileHandle.exists())) throw new Exception("cuts file $cutsFileName not found")

    // data structures to track which leaves have been reset, which applies to the case
    // of overriding cuts files; this ensures leaves are only cleared once per cuts file
    clearedLeavesB = []
    clearedLeavesL = []

    // add cut definitions
    cutsFileHandle.eachLine { line ->

      // tokenize
      line = line.replaceAll(/#.*/,'')
      tok = line.tokenize(' ')
      if(tok.size()==0)
        return
      if(tok =~ /\t/) {
        System.err.println "ERROR: $cutsFileName contains a TAB, please replace them with SPACEs"
        System.exit(100)
      }
      def det      = tok[0]
      def timeline = tok[1]
      def lbound   = tok[2]
      def ubound   = tok[3]
      def units    = tok[4]
      cutPath = [det, timeline]
      spec = tok.size()>5 ? tok[5] : ''
      if(spec!='')
        cutPath.add(spec)

      // convert bounds to 'double' type, unless they are a string
      def lboundCasted
      def uboundCasted
      try { lboundCasted = lbound.toDouble(); } catch(Exception ex) { lboundCasted = lbound.toString(); }
      try { uboundCasted = ubound.toDouble(); } catch(Exception ex) { uboundCasted = ubound.toString(); }

      // add cuts to graph
      def addCut = { graphN ->
        [
          [B, clearedLeavesB, [det,timeline,graphN] ],
          [L, clearedLeavesL, [det,timeline]        ],
        ].each { tr, clearedLeaves, nodePath ->
          T.addLeaf(tr, nodePath, {[]})
          if(!(nodePath in clearedLeaves)) {
            T.getLeaf(tr, nodePath).clear()
            clearedLeaves.add(nodePath)
          }
          T.getLeaf(tr, nodePath).add(lboundCasted)
          T.getLeaf(tr, nodePath).add(uboundCasted)
        }
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
      else if(det=='ltcc') addCut(spec)
      else { // sector dependent detectors
        (1..6).each{ sec ->
          if(det=='rf') addCut('sec'+sec)
          else if(det=='ftof') addCut('sec'+sec)
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
            if(timeline.contains("ec_gg_m")) {
              if(sec==1) addCut(lastWord(timeline))
            }
            else addCut('sec'+sec)
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
  }
  else println " NO."
}


// ==============================================================================

println "=== TIMELINES ========================="
println T.pPrint(B)
println "======================================="
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
  def qaBounds = T.leaf

  // figure out the bound types
  // FIXME: this just checks if `qaBounds` entries are Strings, and not whether they are set to
  // "NB" or not; nonetheless, the documentation says to use "NB"
  def qaBoundsClasses = qaBounds.collect{it.getClass().getSimpleName()}
  def (kNone, kMin, kMax, kRange) = (0..3).collect{it}
  def qaBoundsType = -1
  if(qaBoundsClasses[0] == "String" && qaBoundsClasses[1] == "String") {
    qaBoundsType = kNone
  } else if(qaBoundsClasses[0] == "String") {
    qaBoundsType = kMax
  } else if(qaBoundsClasses[1] == "String") {
    qaBoundsType = kMin
  } else {
    qaBoundsType = kRange
  }

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
      def inbound = false
      switch(qaBoundsType) {
        case kNone:  inbound = true;             break;
        case kMin:   inbound = val>=qaBounds[0]; break;
        case kMax:   inbound = val<=qaBounds[1]; break;
        case kRange: inbound = val>=qaBounds[0] && val<=qaBounds[1]; break
      }
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
      if(num.getClass().getSimpleName() == "String") { // skip "NB" (No Bound) values
        return
      }
      println "LINE: $det $hipoFile $num"
      def lineColor = 'black'
      if(hipoFile=="ltcc_elec_nphe_sec") {
        def lineColors = ['red','red','blue','blue']
        lineColor = lineColors[idx]
      }
      else if(hipoFile=="fth_MIPS_energy") {
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


//// cleanup output directory
println "\nCLEANUP $outdir\n"
def outdirHandle = new File(outdir)
outdirHandle.eachFileRecurse(FileType.FILES) { hipoFile ->
  if(hipoFile.name =~ /_QA.hipo$/) {
    delFile = hipoFile.getPath().replaceAll(/_QA\.hipo$/, '.hipo')
    println "\nremove:        $delFile"
    println "since we have: ${hipoFile.getPath()}"
    new File(delFile).delete()
  }
}
