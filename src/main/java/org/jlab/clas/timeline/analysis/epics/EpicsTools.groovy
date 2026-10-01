package org.jlab.clas.timeline.analysis

import java.text.SimpleDateFormat
import org.jlab.groot.data.H1F
import org.jlab.clas.timeline.util.Tools

/** Static helper functions for EPICS timelines */
class EpicsTools {

  static final String DATE_FORMAT = 'yyyy-MM-dd HH:mm:ss.SSS'


  /**
   * Query MYA for each EPICS PV in {@code pv_names}
   * @param myq {@code MYQuery} instance, created externally so the caller can configure it before using it here
   * @param pv_names map of PV alias (a custom PV name, local to here) to actual PV name
   * @param carry_forward if true, use last-known PV value for each timestamp output, otherwise just use the current values and set the rest to be null
   * @param value_transform if defined, apply this transformation to the PV values; signature: {@code pvName, pvVal -> pvValTransformed}
   * @return the MYA data
   */
  static def queryEpics(MYQuery myq, Map pv_names, boolean carry_forward, Closure value_transform = null) {

    // query MYA DB
    /* `mya_data_unsorted` will look like:
       |_ timestamp 1
       |  |__ PV 1 -> value
       |  :
       |  |__ PV N -> value
       |_ timestamp 2
       :
    */
    System.out.println('MYA query started')
    def mya_data_unsorted = [:].withDefault{[:]}
    def fmt               = new SimpleDateFormat(DATE_FORMAT)
    pv_names.each{ pv_alias, pv_name ->
      myq.query(pv_name).each{
        def val = it.v
        if(value_transform) {
          val = value_transform pv_alias, val
        }
        mya_data_unsorted[fmt.parse(it.d).getTime()][pv_alias] = val
      }
    }
    System.out.println('MYA query finished')
    // System.out.println Tools.prettyPrint('mya_data_unsorted', mya_data_unsorted)

    // merge readings with run timestamps, and sort by timestamp
    /* `mya_data_unsorted` will be a list with 2 types of elements, let's call them 'timeread's:
       [
         // timeread type 1: PV readouts
         { 'ts' -> timestamp 1, PV 1 -> value, PV 2 -> value, ... },
         { 'ts' -> timestamp 1, PV 1 -> value, PV 3 -> value, ... }, // NOTE: not all PVs are read out for each timestamp
         ...
         // timeread type 2: runs and timestamp boundaries
         { 'run' -> run number 1, 'ts' -> run-start timestamp },
         { 'run' -> run number 1, 'ts' -> run-stop timestamp  },
         ...
       ]
    */
    def mya_data_sorted =
      mya_data_unsorted.collect{ timestamp, pvDict -> [ts:timestamp] + pvDict } +
      myq.getRunTimeStamps().collectMany{[
        [run:it[0], ts:(((long)it[1])*1000)],
        [run:it[0], ts:(((long)it[2])*1000)]
      ]}
    mya_data_sorted.sort{it.ts}
    System.out.println('MYA data sorted')
    // System.out.println Tools.prettyPrint('mya_data_sorted', mya_data_sorted)

    // segment the time-sorted timereads into per-run lists of readings for each timestamp; either:
    // - if carry_forward == true, carry forward the last-known value of each PV and write that
    // - if carry_forward == false, just take the current reading's values and let PVs that were not read out be written as 'null'
    // also tags each entry with elapsed 'time' since the previous reading and 'timestamp' since the beginning of the run
    /*
       the return value, `mya_data_result`, will look like:
       |
       |_ runnum 1
       |  |__ 'time'      -> time since previous reading
       |  |__ 'timestamp' -> time since run start
       |  |__ PV alias 0  -> PV value
       |  :
       |  |__ PV alias N  -> PV value
       |
       |_ runnum 2 ...

    */
    def in_run = false
    def runnum, ts_runstart, ts_prev = null
    def vals0 = pv_names.collectEntries{ pv_alias, pv_name -> [pv_alias, null] }
    def mya_data_result = [:].withDefault{[]}
    mya_data_sorted.each{ timeread ->
      if(timeread.run != null) { // if timeread type 2 (run and timestamp)
        if(!in_run) { // is run-start -> set `runnum` to be the run number and `ts_runstart` to be the timestamp
          in_run      = true
          runnum      = timeread.run
          ts_runstart = timeread.ts
        } else { // is run-stop -> clear `runnum` and `ts_runstart`
          in_run      = false
          runnum      = null
          ts_runstart = null
        }
      } else if(in_run) { // if timeread type 1 (PV info) AND timestamp is between run-start and run-stop
        // either carry forward the last known reading for each PV, or just take the current reading's values
        def vals = carry_forward ? vals0 : pv_names.collectEntries{ pv_alias, pv_name -> [pv_alias, timeread[pv_alias]] }
        // populate `mya_data_result`
        mya_data_result[runnum].push(
          [
            'time':      timeread.ts - ts_prev,
            'timestamp': timeread.ts - ts_runstart,
          ]
          + vals
        )
      }
      // time since previous reading
      ts_prev = timeread.ts
      // populate `vals0` with the current PV vals, so that when we go to populate `mya_data_result`, the
      // PV values will be carried forward as the last-known values
      pv_names.each{ pv_alias, pv_name ->
        if(timeread[pv_alias]!=null) {
          vals0[pv_alias] = timeread[pv_alias]
        }
      }
    }
    System.out.println('MYA data segmented')
    // System.out.println Tools.prettyPrint('mya_data_result', mya_data_result)
    mya_data_result
  }


  /**
   * Build a 1D histogram with a quantile-based range from a raw list of values.
   * @param name the histogram name
   * @param title the histogram title
   * @param entries the list of values, which MUST be sorted
   * @return the new histogram
   */
  static H1F quantileHist(String name, String title, List entries) {
    // compute the median and IQR
    def nlen            = entries.size()
    def (nq1, nq2, nq3) = [nlen/4 as int, nlen/2 as int, nlen*3/4 as int]
    def (q1, q2, q3)    = [entries[nq1], entries[nq2], entries[nq3]]
    def (med, iqr)      = [q2, q3-q1]
    // create the histogram
    return new H1F(name, title, 200, med-3*iqr, med+3*iqr)
  }

}
