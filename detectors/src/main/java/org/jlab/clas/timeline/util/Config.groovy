package org.jlab.clas.timeline.util
import groovy.yaml.YamlSlurper

class Config {

  private String      configFileName
  private File        configFile
  private Object      configTree
  private YamlSlurper slurper

  private int    runNum    = 0
  private String runGroup  = "unknown"
  private String runPeriod = "unknown"


  /**
   * @param configFileName the configuration file name
   */
  public Config(String configFileName="config/timelines.yaml") {

    // parse config file
    try {
      this.configFileName = configFileName
      configFile          = new File(configFileName)
      slurper             = new YamlSlurper()
      configTree          = slurper.parse(configFile)
    } catch(Exception ex) {
      ex.printStackTrace()
      System.exit(100)
    }

    // check the config file
    def checkNode = { name, tree, key ->
      if(!tree.containsKey(key))
        throw new Exception("$configFileName has no node '$key' in $name")
    }
    ["default", "run_groups"].each {
      checkNode("top-level", configTree, it)
    }
    configTree["run_groups"].each { runGroupIt, runGroupConfigTree ->
      checkNode("run group '$runGroupIt'", runGroupConfigTree, "runs")
    }

  }


  /**
   * Determine what run-dependent settings to use for specified run
   * @param runNum the run number
   */
  public void setRun(int runNum) {

    // find the run group and run period
    this.runNum = runNum
    try {
      def found = false
      configTree["run_groups"].find { runGroupIt, runGroupConfigTree ->
        runGroupConfigTree["runs"].find { runPeriodIt, runRange ->
          if(runNum >= runRange[0] && runNum <= runRange[1]) {
            runGroup  = runGroupIt
            runPeriod = runPeriodIt
            found     = true
          }
          return found
        }
        return found
      }
      if(!found)
        System.err.println("ERROR: cannot find a run period which contains run $runNum")
    } catch(Exception ex) {
      ex.printStackTrace()
      System.exit(100)
    }

  }


  /**
   * Return the full configuration tree
   */
  public Object getConfigTree() {
    return configTree
  }


  /**
   * Print configuration for current run
   */
  public void printConfig() {
    System.out.println """
Configuration for run $runNum:
  Run Group:  $runGroup
  Run Period: $runPeriod"""
  }

}
