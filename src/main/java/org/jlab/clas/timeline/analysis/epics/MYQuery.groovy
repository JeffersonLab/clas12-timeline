package org.jlab.clas.timeline.analysis
import org.apache.groovy.dateutil.extensions.DateUtilExtensions

class MYQuery {

  // controls whether or not to actually query MYA; useful for the GitHub CI case where MYA is inaccessible
  static boolean skipMYA = System.getenv('TIMELINE_SKIP_MYA') == 'true'

  // MYA URL
  private def dbURL = 'https://epicsweb.jlab.org/myquery/interval'

  // constant settings
  public def querySettings = [
    't': 'eventsimple', // sampling type
    'f': '3',           // fractional time digits
    'v': '',            // significant figures
    'd': 'on',          // data update events only
    'p': 'on',          // include prior point
    'm': 'history',     // MYA deployment
    'l': '',            // limit by binning (downsample)
  ]

  // timestamps
  private def t0str, t1str
  private def runTimeStamps

  // constructor
  public MYQuery(java.util.ArrayList runlist) {
    setRunTimeStamps(runlist)
  }

  // check if an object is null or empty
  public def checkObj(java.lang.Object obj, String msg) {
    if(obj==null || obj.size()==0)
      throw new Exception(msg)
  }

  // set run start and stop times
  private void setRunTimeStamps(java.util.ArrayList runlist) {
    if(skipMYA) {
      runTimeStamps = runlist.collect{[it, 0, 0]}
      return
    }
    // query RCDB
    def result = REST.get("https://clas12mon.jlab.org/rcdb/runs/time?runmin=${runlist.min()}&runmax=${runlist.max()}")
    checkObj(result, "ERROR: MYQuery failed to get time stamps from RCDB")
    runTimeStamps = result.findAll{it[0] in runlist}
    checkObj(runTimeStamps, "ERROR: MYQuery failed to get time stamps from RCDB for specified runs")
    // re-format
    def (t0,t1) = [runTimeStamps[0][1], runTimeStamps[-1][2]].collect{new Date(((long)it)*1000)}
    t1 = DateUtilExtensions.plus(t1, 1)
    // set `t0str` and `t1str`
    (t0str, t1str) = [t0, t1].collect{DateUtilExtensions.format(it, "yyyy-MM-dd")}
  }

  // get run start and stop times
  public def getRunTimeStamps() {
    return runTimeStamps
  }

  // query MYA database
  public def query(String pvName) {
    if(skipMYA) {
      System.out.println("used `--skip-mya` -> returning empty payload for MYA query of PV='$pvName'")
      return []
    }
    // try 'ops' deployment; if that fails, retry with 'history'
    def exceptionList = []
    try {
      def payload = queryDeployment(pvName, 'ops')
      return payload
    } catch(Exception ex) {
      System.out.println("Cannot find PV='$pvName' in range '$t0str' to '$t1str' in 'ops' deployment; trying 'history' deployment...")
      exceptionList << ex
    }
    try {
      def payload = queryDeployment(pvName, 'history')
      return payload
    } catch(Exception ex) {
      System.out.println("... deployment 'history' failed too; throwing exception!")
      exceptionList << ex
    }
    System.err.println("MYQUERY FAILURE: printing relevant exceptions:")
    exceptionList.each{it.printStackTrace()}
    throw new Exception("Cannot find PV='$pvName' in range '$t0str' to '$t1str' in MYA DB")
  }

  private def queryDeployment(String pvName, String deployment) {

    // build query URL
    querySettings['c'] = pvName     // channel (PV name)
    querySettings['b'] = t0str      // begin date
    querySettings['e'] = t1str      // end date
    querySettings['m'] = deployment // MYA deployment
    def queryStr = querySettings.collect{k,v->"$k=$v"}.join('&')
    def queryURL = "${dbURL}?${queryStr}"

    // query
    println("MYQUERY: $queryURL")
    def payload = REST.get(queryURL)
    checkObj(payload, "Failed to receive payload from URL '$queryURL'")
    // println("PAYLOAD: $payload")
    if(payload['returnCount']==null) {
      throw new Exception("myquery payload has no key 'returnCount' from URL '$queryURL'")
    }
    if(payload.returnCount <= 2) {
      throw new Exception("myquery payload received, but seems to be empty, from URL '$queryURL'")
    }
    return payload.data
  }

}
