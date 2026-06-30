package org.jlab.clas.timeline.analysis

class qadb {

  def processRun(dir, run, qa_seq) {
    qadb_charge.processRun(dir, run, qa_seq);
  }

  def write() {
    qadb_charge.write();
  }

}
