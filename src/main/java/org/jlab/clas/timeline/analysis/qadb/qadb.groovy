package org.jlab.clas.timeline.analysis.qadb

import org.jlab.detector.qadb.QadbBinSequence
import org.jlab.clas.timeline.util.Tools

class qadb {

  private def qa_map = [:]
  private boolean qa_allowed = false

  private def ana_qadb_charge = new qadb_charge()
  private def ana_qadb_yield = new qadb_yield()

  // ----------------------------------------------------------------------------------

  /**
   * read the QADB binning scheme for all files
   * @param histo_files the list of HIPO files to be analyzed
   */
  public def start(histo_files) {
    if(histo_files.isEmpty()) {
      // FIXME: for now, let this warning go to `stdout`, since we expect to have an upstream
      // warning already in `stderr` from `run_histograms.java`, and I don't want chefs to
      // be bothered by an additional warning here, until this new QADB code is stable
      System.out.println "WARNING: `qadb.start` called with no input HIPO histogram files; no QADB timelines will be produced"
      return
    }
    histo_files.collect{it.replace(".hipo", ".dat")}.each { histo_file ->
      def run = Tools.getRunNumberForAnalysis(histo_file)
      qa_map[run] = new QadbBinSequence<Map>(histo_file)
    }
    qa_allowed = true
  }

  // ----------------------------------------------------------------------------------

  public def processRun(dir, run) {
    if(qa_allowed) {
      ana_qadb_charge.processRun(dir, run, qa_map)
      ana_qadb_yield.processRun(dir, run, qa_map)
    }
  }

  // ----------------------------------------------------------------------------------

  public def write() {
    if(qa_allowed) {
      ana_qadb_charge.write(qa_map)
      ana_qadb_yield.write(qa_map)
    }
  }

}
