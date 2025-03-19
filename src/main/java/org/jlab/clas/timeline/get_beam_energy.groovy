package org.jlab.clas.timeline

// get beam energy from RCDB
import org.rcdb.*

// arguments
if(args.length<1) {
  System.err.println "USAGE: groovy ${this.class.getSimpleName()}.groovy [RUN_NUMBER]"
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

// connect to RCDB
def rcdbURL = System.getenv('RCDB_CONNECTION')
if(rcdbURL==null)
  throw new Exception("RCDB_CONNECTION not set")
def rcdbProvider = RCDB.createProvider(rcdbURL)
try {
  rcdbProvider.connect()
}
catch(Exception e) {
  System.err.println "ERROR: unable to connect to RCDB"
  System.out.println ''
  System.exit(100)
}

// get the run number
result = rcdbProvider.getCondition(runnum, 'beam_energy')
if(result==null) {
  System.err.println "ERROR: cannot find run $runnum in RCDB, thus cannot get beam energy"
  System.out.println ''
  System.exit(100)
}
beamEn = result.toDouble() / 1e3 // [MeV] -> [GeV]
System.out.println beamEn
