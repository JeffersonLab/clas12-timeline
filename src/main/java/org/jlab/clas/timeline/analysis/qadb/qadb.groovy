package org.jlab.clas.timeline.analysis.qadb

import org.jlab.detector.qadb.QadbBinSequence
import org.jlab.clas.timeline.util.Tools

class qadb {

  private def qa_map = [:]

  private def ana_qadb_charge = new qadb_charge()
  private def ana_qadb_yield = new qadb_yield()

  public def start(histo_files) {
    Tools T = new Tools()
    histo_files.collect{it.replace(".hipo", ".dat")}.each { histo_file ->
      def run = T.getRunNumberForAnalysis(histo_file)
      qa_map[run] = new QadbBinSequence<Map>(histo_file)
    }
  }

  // ----------------------------------------------------------------------------------

  public def processRun(dir, run) {
    ana_qadb_charge.processRun(dir, run, qa_map)
    // ana_qadb_yield.processRun(dir, run, qa_map)
  }

  // ----------------------------------------------------------------------------------

  public def write() {
    ana_qadb_charge.write(qa_map)
    // ana_qadb_yield.write(qa_map)
  }

}
