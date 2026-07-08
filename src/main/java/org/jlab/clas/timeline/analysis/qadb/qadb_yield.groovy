package org.jlab.clas.timeline.analysis.qadb

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

import org.jlab.clas.timeline.histograms.qadb.Yield
import org.jlab.clas.timeline.histograms.qadb.Charge

class qadb_yield {

  def data_map = new ConcurrentHashMap()

  // ----------------------------------------------------------------------------------

  def processRun(dir, runnum, qa_map) {
    data_map[runnum] = [run:runnum, histos:[:]]
    qa_map[runnum].each { qa_bin ->
      def histos = new Yield(qa_bin.getBinNum())
      def charge = new Charge(qa_bin.getBinNum())
      histos.readHistograms(dir, qa_bin.getBinNum())
      charge.readHistograms(dir, qa_bin.getBinNum())
      data_map[runnum]['histos'][qa_bin.getBinNum()] = histos
      data_map[runnum]['charge'][qa_bin.getBinNum()] = charge
    }
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map) {

    // start ouput `TDirectory`s
    TDirectory tdir_FD_ele = new TDirectory() // forward detector (FD) electrons
    TDirectory tdir_FT_ele = new TDirectory() // forward tagger (FT) electrons

  }

}
