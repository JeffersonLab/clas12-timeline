package org.jlab.clas.timeline.daqconfig
import org.jlab.clas.timeline.util.RunDependentCut

import org.jlab.groot.data.TDirectory

// define timeline engines
def engines = [
  out_ALERT: [new alert_daqconfig(),
  ]
]


// parse arguments
if(args.any{it=="--timelines"}) {
  engines.values().flatten().each{
    println(it.getClass().getSimpleName())
  }
  System.exit(0)
}
if(args.length != 2) {
  System.err.println "ARGUMENTS: [timeline] [input_dir]"
  System.err.println "use --timelines for a list of available timelines"
  System.exit(101)
}
def (timelineArg, inputDirArg) = args

// check the timeline argument
def eng = engines.collectMany{key,engs->engs.collect{[key,it]}}
  .find{name,eng->eng.getClass().getSimpleName()==timelineArg}
if(eng == null) {
  System.err.println("error: timeline '$timelineArg' is not defined")
  System.exit(100)
}

// get list of input HIPO histogram files
def (name,engine) = eng
def inputDir = new File(inputDirArg)
println([name,timelineArg,engine.getClass().getSimpleName(),inputDir])
def fnames = []
inputDir.traverse {
  if(it.name.endsWith('.daq.config') && it.name.contains(name))
    fnames.add(it.absolutePath)
}

// loop over input HIPO histogram files
def allow_timeline = false
fnames.sort().each{ fname ->
  try{
    println("debug: "+engine.getClass().getSimpleName()+" started $fname")

    // get run number from directory name
    def dname = fname.split('/')[-2]
    def m = dname =~ /\d+/
    def run = m[0].toInteger()

    // exclude certain run ranges from certain timelines
    def allow_run = true
    def dataset = RunDependentCut.findDataset(run)
    if(dataset == 'rgl') {
      if( timelineArg ==~ /^bmt.*/ ||
          timelineArg ==~ /^bst.*/ ||
          timelineArg ==~ /^cen.*/ ||
          timelineArg ==~ /^cvt.*/ ) { allow_run = false }
    }
    else { // not RG-L
      if(timelineArg ==~ /^alert.*/) { allow_run = false }
    }

    // run the daqconfig for this run
    if(allow_run) {
      allow_timeline = true // allow the timeline if at least one run is allowed
      engine.processRun(fname, run)
      println("debug: "+engine.getClass().getSimpleName()+" finished $fname")
    }
    else {
      println("debug: "+engine.getClass().getSimpleName()+" excludes run $run")
    }

  } catch(Exception ex) {
    System.err.println("error: "+engine.getClass().getSimpleName()+" didn't process $fname, due to exception:")
    ex.printStackTrace()
    System.exit(100)
  }
}

// write the timeline HIPO file
if(allow_timeline) {
  engine.write()
  println("debug: "+engine.getClass().getSimpleName()+" ended")
}
else {
  println("debug: "+engine.getClass().getSimpleName()+" was not produced, since all runs were excluded")
}
