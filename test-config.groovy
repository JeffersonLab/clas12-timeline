import org.jlab.clas.timeline.util.Config
import groovy.json.JsonOutput

Config C = new Config("config/template.yaml")
println JsonOutput.prettyPrint(
  JsonOutput.toJson(C.getConfigTree())
)
