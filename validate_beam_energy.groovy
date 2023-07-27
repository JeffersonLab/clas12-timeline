import groovy.yaml.YamlSlurper
import org.rcdb.*

// arguments
if(args.length<1) {
  System.err << "ERROR: specify yaml file\n"
  System.exit(100)
}
beamEnConfigYaml = args[0]

// parse beam energy config file
def slurper = new YamlSlurper()
def beamEnConfig = slurper.parse(new File(beamEnConfigYaml))

// connect to RCDB
def rcdbURL = System.getenv('RCDB_CONNECTION')
if(rcdbURL==null)
  throw new Exception("RCDB_CONNECTION not set")
println "Connecting to $rcdbURL"
def rcdbProvider = RCDB.createProvider(rcdbURL)
try{ rcdbProvider.connect() }
catch(Exception e) {
  System.err << "ERROR: unable to connect to RCDB\n"
  System.exit(100)
}

// loop over run ranges
beamEnConfig['beamEnergy'].each{ runRange ->

  beamEnLocal = runRange.value
  println "Check range ${runRange.runnum}"

  // loop over runs in this range
  (runRange.runnum.first()..runRange.runnum.last()).each{ runnum ->
    res = rcdbProvider.getCondition(runnum, 'beam_energy')
    if(res==null) {
      // println "  $runnum: not in RCDB, skip"
      return
    }
    beamEnRCDB = res.toDouble() / 1e3 // [MeV] -> [GeV]
    if(Math.abs(beamEnLocal - beamEnRCDB)>0.01)
      println "  $runnum:  yaml=$beamEnLocal  RCDB=$beamEnRCDB --> DIFFERENT"
  }
}
