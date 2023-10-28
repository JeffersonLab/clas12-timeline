package org.jlab.clas.timeline.timeline.epics

import org.apache.groovy.dateutil.extensions.DateUtilExtensions
import java.text.SimpleDateFormat
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.MoreFitter


class epics_hall_weather {
  def runlist = []

  def processDirectory(dir, run) {
    runlist.push(run)
  }

  def close() {
    def runmin = runlist.min()
    def runmax = runlist.max()

    def ts = REST.get("https://clas12mon.jlab.org/rcdb/runs/time?runmin=$runmin&runmax=$runmax").findAll{it[0] in runlist}
    def (t0,t1) = [ts[0][1], ts[-1][2]].collect{new Date(((long)it)*1000)}
    t1 = DateUtilExtensions.plus(t1, 1)
    print([t0,t1])
    def (t0str, t1str) = [t0, t1].collect{DateUtilExtensions.format(it, "yyyy-MM-dd")}

    def epics = [:].withDefault{[:]}
    def press = REST.get("https://epicsweb.jlab.org/myquery/interval?c=B_SYS_WEATHER_SF_L3_PRESS&b=$t0str&e=$t1str&l=&t=eventsimple&m=history&f=3&v=&d=on&p=on").data
      .each{epics[new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS').parse(it.d).getTime()].press = it.v}
    def temp = REST.get("https://epicsweb.jlab.org/myquery/interval?c=B_SYS_WEATHER_SF_L1_TEMP&b=$t0str&e=$t1str&l=&t=eventsimple&m=history&f=3&v=&d=on&p=on").data
      .each{epics[new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS').parse(it.d).getTime()].temp = it.v}
    def humid = REST.get("https://epicsweb.jlab.org/myquery/interval?c=B_SYS_WEATHER_SF_L1_HUMID&b=$t0str&e=$t1str&l=&t=eventsimple&m=history&f=3&v=&d=on&p=on").data
      .each{epics[new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS').parse(it.d).getTime()].humid = it.v}
    // def xs = REST.get("https://epicsweb.jlab.org/myquery/interval?c=IPM2H01.XPOS&b=$t0str&e=$t1str&l=&t=eventsimple&m=history&f=3&v=&d=on&p=on").data
    //   .each{epics[new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS').parse(it.d).getTime()].x = it.v}
    // def ys = REST.get("https://epicsweb.jlab.org/myquery/interval?c=IPM2H01.YPOS&b=$t0str&e=$t1str&l=&t=eventsimple&m=history&f=3&v=&d=on&p=on").data
    //   .each{epics[new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS').parse(it.d).getTime()].y = it.v}
    // def is = REST.get("https://epicsweb.jlab.org/myquery/interval?c=IPM2H01&b=$t0str&e=$t1str&l=&t=eventsimple&m=history&f=3&v=&d=on&p=on").data
    //   .each{epics[new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS').parse(it.d).getTime()].i = it.v}

    println('dl finished')

    def data = epics.collect{kk,vv->[ts:kk]+vv} + ts.collectMany{[[run:it[0], ts: (((long)it[1])*1000)], [run:it[0], ts: (((long)it[2])*1000)]]}
    data.sort{it.ts}

    println('data sorted')

    def ts0, press0, temp0, humid0, r0=null
    def rundata = [:].withDefault{[]}
    data.each{
      if(it.run!=null) {
        r0 = r0 ? null : it.run
      } else if(r0) {
        rundata[r0].push([it.ts-ts0, press0, temp0, humid0])
      }
      ts0 = it.ts
      if(it.press!=null) press0 = it.press
      if(it.temp!=null)  temp0  = it.temp
      if(it.humid!=null) humid0 = it.humid
    }

    def out = new TDirectory()

    def (grPress, grTemp, grHumid) = [new GraphErrors('pressure'), new GraphErrors('temperature'), new GraphErrors('humidity')]

    rundata.each{run,vals->
      out.mkdir("/$run")
      out.cd("/$run")

      def (hPress, hTemp, hHumid) = (1..3).collect{ind->
        // 
        // 
        // stopped here
        // 
        // 
        def entries = vals.collectMany{[it[ind]]*(it[0]*it[3]/1000 as int)}.sort()
        def nlen = entries.size()
        def (nq1,nq2,nq3) = [nlen/4 as int, nlen/2 as int, nlen*3/4 as int]
        def (q1,q2,q3) = [entries[nq1], entries[nq2], entries[nq3]]
        def (xm,dx) = [q2, q3-q1]
        def xy = 'xy'[ind-1]
        return new H1F("h$xy$run","$xy for run $run;$xy", 200, xm-3*dx, xm+3*dx)
      }

      vals.each{
        hx.fill(it[1], it[0]*it[3]/1000)
        hy.fill(it[2], it[0]*it[3]/1000)
      }

      def fx = MoreFitter.gausFit(hx, "Q")
      def fy = MoreFitter.gausFit(hy, "Q")
      grPress.addPoint(run, fx.getParameter(1), 0,0)
      grTemp.addPoint(run, fy.getParameter(1), 0,0)
      grHumid.addPoint(run, fy.getParameter(1), 0,0)

      [hx,hy,fx,fy].each{out.addDataSet(it)}
      println("$run done")
    }

    out.mkdir("/timelines")
    out.cd("/timelines")
    out.addDataSet(grPress)
    out.addDataSet(grTemp)
    out.addDataSet(grHumid)
    out.writeFile("epics_hall_weather.hipo")
  }
}



