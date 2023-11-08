// dump the name of all the timeline plots for a given timeline HIPO file
import org.jlab.groot.data.TDirectory

if(args.length<1) {
  System.err.println "USAGE: run-groovy ${this.class.getSimpleName()}.groovy [TIMELINE HIPO FILE]"
  System.exit(101)
}
def inFile = args[0]

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

objList.each { objN ->
  tok = objN.tokenize('/')
  if(tok[0] == "timelines") {
    if( !(tok[1] ==~ /^plotLine.*/) && !(tok[1] ==~ /.*__bad$/) ) {
      timelines << tok[1]
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
  .collect{ it.replaceAll(/.hipo$/,'') }
  .join(' :: ')

System.out.println """
> ${inFileBase}
> timelines = $timelines
> plots     = $plots
"""
