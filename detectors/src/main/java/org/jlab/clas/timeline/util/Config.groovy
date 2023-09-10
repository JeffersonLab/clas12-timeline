package org.jlab.clas.timeline.util
import groovy.yaml.YamlSlurper

class Config {

  // instance variables -------------------------------------

  // config file vars
  private String      configFileName
  private File        configFile
  private Object      configTree
  private YamlSlurper slurper

  // run group and period
  private int            runNum
  private Range<Integer> runNumRange
  private String         runGroup
  private String         runPeriod

  // ------------------------------------------------------

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

    // initialize run period variables
    resetRunPeriod()

  }


  /**
   * Reset the run period variables
   */
  private void resetRunPeriod() {
    runNum      = 0
    runNumRange = 0..0
    runGroup    = "unknown"
    runPeriod   = "unknown"
  }


  /**
   * Determine what run-dependent settings to use for specified run
   * @param runNum the run number
   */
  public void setRun(int runNum) {
    this.runNum = runNum
    try {
      // if runNum is in the same run period as the previous call, do nothing
      if(runNumRange.contains(runNum)) return
      // reset run period variables
      resetRunPeriod()
      // search for the run period which contains this runNum
      def found = false
      configTree["run_groups"].find { runGroupIt, runGroupConfigTree ->
        runGroupConfigTree["runs"].find { runPeriodIt, runNumRangeArr ->
          Range<Integer> runNumRangeIt = runNumRangeArr[0]..runNumRangeArr[1]
          if(runNumRangeIt.contains(runNum)) {
            runNumRange = runNumRangeIt
            runGroup    = runGroupIt
            runPeriod   = runPeriodIt
            found       = true
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
  Run Group:        $runGroup
  Run Period:       $runPeriod
  Run Number Range: ${runNumRange.getFrom()} to ${runNumRange.getTo()}
"""
  }

}
