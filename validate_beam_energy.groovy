import groovy.json.JsonSlurper

def slurper = new JsonSlurper()
def set1 = slurper.parse(new File("config/beam-energy.json"))

set1.each{ set ->

  // loop over runs and compare to RCDB
  println "check $set:"
  (set['runnumMin']..set['runnumMax']).each{ runnum ->
    println "  $runnum:"
  }

}
