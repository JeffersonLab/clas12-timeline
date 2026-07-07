package org.jlab.clas.timeline.analysis.qadb;

import org.jlab.detector.qadb.QadbBinSequence;
import org.jlab.clas.timeline.util.Tools;

class QADB {

  private def qa_map = [:]

  public def start(histo_files) {
    Tools T = new Tools()
    histo_files.collect{it.replace(".hipo", ".dat")}.each { histo_file ->
      def run = T.getRunNumberForAnalysis(hist_file);
      qa_map[run] = new QadbBinSequence<Map>(histo_file);
    }
  }

  // ----------------------------------------------------------------------------------

  public def processRun(dir, run) {
    qadb_charge.processRun(dir, run, qa_map);
    qadb_yield.processRun(dir, run, qa_map);
  }

  // ----------------------------------------------------------------------------------

  public def write() {
    qadb_charge.write(qa_map);
    qadb_yield.write(qa_map);
  }

}
