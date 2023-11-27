package org.jlab.clas.timeline.timeline.epics
import org.apache.groovy.dateutil.extensions.DateUtilExtensions

class MYQuery {

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
  ]

  // timestamps
  private def t0str, t1str


  // constructor
  public MYQuery() {
  }

  // check if an object is null or empty
  public def checkObj(java.lang.Object obj, String msg) {
    if(obj==null || obj.size()==0)
      throw new Exception(msg)
  }

  // get run start and stop times
  public def getRunTimeStamps(java.util.ArrayList runlist) {

    // query RCDB
    def result = REST.get("https://clas12mon.jlab.org/rcdb/runs/time?runmin=${runlist.min()}&runmax=${runlist.max()}")
    checkObj(result, "ERROR: MYQuery failed to get time stamps from RCDB")
    def resultSelected = result.findAll{it[0] in runlist}
    checkObj(resultSelected, "ERROR: MYQuery failed to get time stamps from RCDB for specified runs")

    // re-format
    def (t0,t1) = [resultSelected[0][1], resultSelected[-1][2]].collect{new Date(((long)it)*1000)}
    t1 = DateUtilExtensions.plus(t1, 1)
    (t0str, t1str) = [t0, t1].collect{DateUtilExtensions.format(it, "yyyy-MM-dd")}

    return resultSelected
  }

  // query MYA database
  public def query(String pvName) {
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
