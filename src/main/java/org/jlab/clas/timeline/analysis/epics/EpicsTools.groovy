package org.jlab.clas.timeline.analysis

import java.text.SimpleDateFormat
import org.jlab.groot.data.H1F

/** Static helper functions for EPICS timelines */
class EpicsTools {

  static final String DATE_FORMAT = 'yyyy-MM-dd HH:mm:ss.SSS'

  /**
   * Query MYA for each EPICS PV in {@code pv_names}
   * @param runlist the list of run numbers
   * @param myq {@code MYQuery} instance, created externally so the caller can configure it before using it here
   * @param pv_names map of timeline name (a custom PV name, local to here) to actual PV name
   * @param value_transform if defined, apply this transformation to the PV values; signature: {@code pvName, pvVal -> pvValTransformed}
   * @return the MYA data
   */
  static def queryEpics(java.util.ArrayList runlist, MYQuery myq, Map pv_names, Closure value_transform = null) {
    // query MYA DB
    System.out.println('MYA query started')
    def mya_data_unsorted = [:].withDefault{[:]}
    def fmt               = new SimpleDateFormat(DATE_FORMAT)
    pv_names.each{ timeline_name, pv_name ->
      myq.query(pv_name).each{
        def val = it.v
        if(value_transform) {
          val = value_transform timeline_name, val
        }
        mya_data_unsorted[fmt.parse(it.d).getTime()][timeline_name] = val
      }
    }
    System.out.println('MYA query finished')
    // merge and sort readings
    def run_time_stamps = myq.getRunTimeStamps runlist
    def mya_data_sorted =
      mya_data_unsorted.collect{ timestamp, pvDict -> [ts:timestamp] + pvDict } +
      run_time_stamps.collectMany{[
        [run:it[0], ts:(((long)it[1])*1000)],
        [run:it[0], ts:(((long)it[2])*1000)]
      ]}
    mya_data_sorted.sort{it.ts}
    System.out.println('MYA data sorted')
    // segment the time-sorted data stream into per-run lists of readings, carrying forward the
    // last-known value of each PV in pv_names and tagging each entry with its elapsed 'time'
    // since the previous reading.
    def ts0, r0         = null
    def vals0           = pv_names.collectEntries{ timeline_name, pv_name -> [timeline_name, null] }
    def mya_data_result = [:].withDefault{[]}
    mya_data_sorted.each{
      if(it.run != null) {
        r0 = r0 ? null : it.run
      } else if(r0) {
        mya_data_result[r0].push(['time':it.ts-ts0] + vals0)
      }
      ts0 = it.ts
      pv_names.each{ timeline_name, pv_name ->
        if(it[timeline_name]!=null) {
          vals0[timeline_name] = it[timeline_name]
        }
      }
    }
    System.out.println('MYA data segmented')
    mya_data_result
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
