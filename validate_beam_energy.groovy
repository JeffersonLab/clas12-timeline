import groovy.yaml.YamlSlurper
import org.rcdb.*

def slurper = new YamlSlurper()
def set1 = slurper.parse(new File("config/beam-energy.yaml"))

set1['beamEnergy'].each{ runRange ->
  beamEn = runRange.value
  println "Check range ${runRange.runnum} for beamEn=$beamEn"
  (runRange.runnum.first()..runRange.runnum.last()).each{ runnum ->
    println "  $runnum:"
  }
}
