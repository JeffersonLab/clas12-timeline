// get beam energy from RCDB
import org.jlab.detector.calib.utils.RCDBProvider

// arguments
if(args.length<1) {
  System.err.println "USAGE: run-groovy ${this.class.getSimpleName()}.groovy [RUN_NUMBER]"
  System.out.println ''
  System.exit(101)
}
def runnum
try {
  runnum = args[0].toInteger()
}
catch(Exception e) {
  System.err.println "ERROR: run number argument is not an integer"
  System.out.println ''
  System.exit(100)
}

def db = new RCDBProvider()
def beam_en = db.getConstants(runnum).getDouble("beam_energy")
if(beam_en == null) {
  System.err.println "ERROR: cannot find run $runnum in RCDB, thus cannot get beam energy"
  System.out.println ''
  System.exit(100)
}
beam_en /= 1e3 // [MeV] -> [GeV]
System.out.println beam_en
