// get run number for a given HIPO file, using RUN::config
import org.jlab.clas.timeline.util.Tools
Tools T = new Tools()

if(args.length<1) {
  System.err.println """
  USAGE: run-groovy ${this.class.getSimpleName()}.groovy [HIPO file]
  Returns run number for a given file
  """
  System.exit(101)
}

def runnum = T.getRunNumber(args[0])
System.out.println(runnum)
if(runnum<=0) System.exit(100)
