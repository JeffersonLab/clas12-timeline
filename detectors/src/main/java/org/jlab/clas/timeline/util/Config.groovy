package org.jlab.clas.timeline.util
import groovy.yaml.YamlSlurper
import org.rcdb.*

class Config {

  private File        configFile
  private Object      configTree
  private YamlSlurper slurper

  private int    runNum
  private String runGroup

  /////////////////////////////////////////

  public Config(String configFileName="config/timelines.yaml") {
    configFile = new File(configFileName)
    if(!configFile.exists())
      throw new Exception("ERROR: config file $configFileName does not exist")
    slurper = new YamlSlurper()
    configTree = slurper.parse(configFile)
  }

  /////////////////////////////////////////

  public void setRun(int runNum_) {
    runNum = runNum_
  }

  /////////////////////////////////////////

  public Object getConfigTree() {
    return configTree
  }

}
