package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class cvt_efficiency {

  def data = new ConcurrentHashMap();

  def sl_loop(ftn) {
    for(int s = 1; s <= 3; s++) {
      for(int l = 1; l <= 6; l++) {
        ftn("S${s}L${l}");
      }
    }
  }

  def processRun(dir, run) {
    data[run] = [run:run, counts:[:]]
    sl_loop({ sl_string ->
      data[run]['counts'][sl_string] = dir.getObject("/CVT/H_CVT_counts_${sl_string}");
    });
  }

  def write() {
    TDirectory out = new TDirectory();
    out.mkdir('/timelines');
    data.sort{it.key}.each{ run, run_data -> out.mkdir("/$run"); };

    sl_loop({ sl_string ->
      def gr = new GraphErrors(sl_string);
      gr.setTitle("CVT Efficiency");
      gr.setTitleY("CVT Efficiency");
      gr.setTitleX("run number");
      data.sort{it.key}.each{ run, run_data ->
        def hist           = run_data['counts'][sl_string];
        def counts_all     = hist.getBinContent(0);
        def counts_matched = hist.getBinContent(1);
        if(counts_all > 0) {
          gr.addPoint(run, (double) counts_matched / counts_all);
        }
        out.cd("/$run");
        out.addDataSet(hist);
      }
      out.cd('/timelines');
      out.addDataSet(gr);
    });

    out.writeFile('cvt_efficiency.hipo');
  }

}
