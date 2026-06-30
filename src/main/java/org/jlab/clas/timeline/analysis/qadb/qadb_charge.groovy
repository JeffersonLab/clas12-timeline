package org.jlab.clas.timeline.analysis

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

import org.jlab.clas.timeline.histograms.qadb.Charge;

class qadb_charge {

  def data = new ConcurrentHashMap();

  def processRun(dir, run, qa_seq) {
    data[run] = [run:run, hists:[:]];
    qa_seq.each{ qa_bin ->
      def c = new Charge(qa_bin.binNum);
      c.readHistograms(dir, qa_bin.binNum);
      data[run]['hists'][qa_bin.binNum] = c;
    }
  }

  def write() {

    // set titles
    def set_titles = { gr ->
      gr.setTitle("Charge [mC]");
      gr.setTitleY("Charge [mC]");
      gr.setTitleX("Run Number");
    }

    // convert nC -> mC
    def nC_to_mC = { q -> q / 1e6 }

    // charge per bin
    TDirectory out_per = new TDirectory();
    out_per.mkdir('/timelines');
    def per_gated_dsc2         = set_titles(new GraphErrors("gated_DSC2"));
    def per_ungated_dsc2       = set_titles(new GraphErrors("ungated_DSC2"));
    def per_gated_hel_p_struck = set_titles(new GraphErrors("gated_STRUCK_hel_pos"));
    def per_gated_hel_0_struck = set_titles(new GraphErrors("gated_STRUCK_hel_0"));
    def per_gated_hel_n_struck = set_titles(new GraphErrors("gated_STRUCK_hel_neg"));
    data.sort{it.key}.each{ run, run_data ->
      run_data['hists'].each{ bin_num, it ->
        

    /*
    for (int i=0; i<64; ++i) {
      def gr = new GraphErrors("bit$i");
      gr.setTitle("Trigger Bit $i");
      gr.setTitleY("Event Fraction");
      gr.setTitleX("Run Number");
      data.sort{it.key}.each{run,it->
        out_per.mkdir('/'+it.run);
        out_per.cd('/'+it.run);
        out_per.addDataSet(it["Bits"]);
        gr.addPoint(it.run, it["Bits"].getDataX(i) / it["Bits"].getDataX(64), 0, 0);
      }
      out_per.cd('/timelines');
      out_per.addDataSet(gr);
    }
    */
    out_per.writeFile('qadb_charge.hipo');
  }

}
