package org.jlab.clas.timeline.analysis.qadb

import org.jlab.detector.qadb.QadbBinSequence
import org.jlab.clas.timeline.util.Tools

class qadb {

  private def qa_map = [:]
  private boolean qa_allowed = false

  private def ana_qadb_charge = new qadb_charge()
  private def ana_qadb_yield = new qadb_yield()
  // private def ana_qadb_helicity = new qadb_helicity() // FIXME FIXME FIXME re-enable this
  private def ana_qadb_beam_charge_asym = new qadb_beam_charge_asym()

  // ----------------------------------------------------------------------------------

  /**
   * read the QADB binning scheme for all files
   * @param histo_files the list of HIPO files to be analyzed
   */
  public void start(histo_files) {
    if(histo_files.isEmpty()) {
      System.err.println "WARNING: `qadb.start` called with no input HIPO histogram files; no QADB timelines will be produced"
      return
    }
    histo_files.collect{it.replace(".hipo", ".dat")}.each { histo_file ->
      def run = Tools.getRunNumberForAnalysis(histo_file)
      qa_map[run] = new QadbBinSequence<Map>(histo_file)
    }
    qa_allowed = true
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a run's histograms
   * @param dir the {@code TDirectory} to read
   * @param run the run number
   */
  public void processRun(dir, run) {
    if(qa_allowed) {
      ana_qadb_charge.processRun(dir, run, qa_map)
      ana_qadb_yield.processRun(dir, run, qa_map)
      // ana_qadb_helicity.processRun(dir, run, qa_map) // FIXME FIXME FIXME re-enable this
      ana_qadb_beam_charge_asym.processRun(dir, run, qa_map)
    }
  }

  // ----------------------------------------------------------------------------------

  /** analyse the histograms and write the output timeline file */
  public void write() {
    if(qa_allowed) {
      ana_qadb_charge.write(qa_map)
      ana_qadb_yield.write(qa_map)
      // ana_qadb_helicity.write(qa_map) // FIXME FIXME FIXME re-enable this
      ana_qadb_beam_charge_asym.write(qa_map)
    }
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a run's trigger-bit histograms
   * @param dir the {@code TDirectory} to read
   * @param run the run number
   */
  public void processTrig(dir, run) {
    if(qa_allowed) {
      ana_qadb_beam_charge_asym.processTrig(dir, run)
    }
  }

}
