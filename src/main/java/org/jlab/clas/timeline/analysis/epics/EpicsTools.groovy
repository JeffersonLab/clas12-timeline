package org.jlab.clas.timeline.analysis

import java.text.SimpleDateFormat
import org.jlab.groot.data.H1F

/** Static helper functions for EPICS timelines */
class EpicsTools {

  static final String DATE_FORMAT = 'yyyy-MM-dd HH:mm:ss.SSS'

  /**
   * Query MYA for each PV in {@code pvNames}
   * @param MYQ {@code MYQuery} instance
   * @param pvNames map of timeline name (a custom PV name, local to here) to actual PV name
   * @return nested dictionary mapping (timestamp, timeline name) to PV name
   */
  static def queryEpics(MYQ, Map pvNames, Closure valueTransform = null) {
    System.out.println('MYA query started')
    def result = [:].withDefault{[:]}
    def fmt    = new SimpleDateFormat(DATE_FORMAT)
    pvNames.each{ timeline_name, pv_name ->
      MYQ.query(pv_name).each{
        def val = it.v
        if(valueTransform) {
          val = valueTransform(timeline_name, val)
        }
        result[fmt.parse(it.d).getTime()][timeline_name] = val
      }
    }
    System.out.println('MYA query finished')
    result
  }

  /**
   * Merge and sort EPICS readings
   * @param epicsData the output from {@link queryEpics}
   * @param runTimeStamps the timestamps from {@link MYQuery.getRunTimeStamps}
   * @return merged and sorted EPICS data
   */
  static def mergeAndSort(epicsData, runTimeStamps) {
    def result =
      epicsData.collect{ timestamp, pvDict -> [ts:timestamp] + pvDict } +
      runTimeStamps.collectMany{[
        [run:it[0], ts:(((long)it[1])*1000)],
        [run:it[0], ts:(((long)it[2])*1000)]
      ]}
    result.sort{it.ts}
    System.out.println('MYA data sorted')
    result
  }

  /**
   * Segment the time-sorted {@code data} stream (from {@link mergeAndSort}) into per-run lists
   * of readings, carrying forward the last-known value of each PV in pvNames and
   * tagging each entry with its elapsed 'time' since the previous reading.
   * @param epicsMergedData the output from {@link mergeAndSort}
   * @param pvNames map of timeline name (a custom PV name, local to here) to actual PV name
   * @return the segmented data
   */
  static def segmentByRun(epicsMergedData, Map pvNames) {
    def ts0, r0 = null
    def vals0   = pvNames.collectEntries{ timeline_name, pv_name -> [timeline_name, null] }
    def result  = [:].withDefault{[]}
    epicsMergedData.each{
      if(it.run != null) {
        r0 = r0 ? null : it.run
      } else if(r0) {
        result[r0].push(['time':it.ts-ts0] + vals0)
      }
      ts0 = it.ts
      pvNames.each{ timeline_name, pv_name ->
        if(it[timeline_name]!=null) {
          vals0[timeline_name] = it[timeline_name]
        }
      }
    }
    result
  }

  /**
   * Build a 1D histogram with a quantile-based range from a raw list of values.
   * @param name the histogram name
   * @param title the histogram title
   * @param entries the list of values
   * @return the new histogram
   */
  static H1F quantileHist(String name, String title, List entries) {
    // compute the median and IQR
    entries             = entries.sort()
    def nlen            = entries.size()
    def (nq1, nq2, nq3) = [nlen/4 as int, nlen/2 as int, nlen*3/4 as int]
    def (q1, q2, q3)    = [entries[nq1], entries[nq2], entries[nq3]]
    def (med, iqr)      = [q2, q3-q1]
    // create the histogram
    new H1F(name, title, 200, med-3*iqr, med+3*iqr)
  }

}
