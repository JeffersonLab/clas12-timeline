import groovy.yaml.YamlSlurper
import org.rcdb.*

// parse beam energy config file
def slurper = new YamlSlurper()
def beamEnConfig = slurper.parse(new File("config/beam-energy.yaml"))

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
    print "$runnum: "
    res = rcdbProvider.getCondition(runnum, 'beam_energy')
    if(res==null) {
      println " not in RCDB, skip"
      return
    }
    beamEnRCDB = res.toDouble() / 1e3 // [MeV] -> [GeV]
    print "    local=$beamEnLocal  RCDB=$beamEnRCDB"
    if(Math.abs(beamEnLocal - beamEnRCDB)>0.01)
      println " --> DIFFERENT"
    else
      println ""
  }
  println "STOP PREMATURELY"
  System.exit(0)
}
