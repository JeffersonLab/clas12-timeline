package org.jlab.clas.timeline.analysis

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

import org.jlab.clas.timeline.histograms.qadb.Charge;

class qadb_charge {

  def data = new ConcurrentHashMap();

  // ----------------------------------------------------------------------------------

  def processRun(dir, runnum, qa_seq) {
    data[runnum] = [run:runnum, histos:[:]];
    qa_seq.each{ qa_bin ->
      def c = new Charge(qa_bin.getBinNum());
      c.readHistograms(dir, qa_bin.getBinNum());
      data[runnum]['histos'][qa_bin.getBinNum()] = c;
    }
  }

  // ----------------------------------------------------------------------------------

  def write() {

    // start ouput `TDirectory`s
    TDirectory tdir_per = new TDirectory(); // charge per run
    TDirectory tdir_acc = new TDirectory(); // accumulated charge as a function of run

    // define timeline graphs: charge vs. run
    def make_tl = { name ->
      def g = new GraphErrors(name);
      g.setTitle("Charge [mC]");
      g.setTitleY("Charge [mC]");
      g.setTitleX("Run Number");
      g
    }
    def tl_gated_dsc2         = make_tl("gated_DSC2");
    def tl_ungated_dsc2       = make_tl("ungated_DSC2");
    def tl_gated_hel_p_struck = make_tl("gated_STRUCK_hel_pos");
    def tl_gated_hel_0_struck = make_tl("gated_STRUCK_hel_0");
    def tl_gated_hel_n_struck = make_tl("gated_STRUCK_hel_neg");

    // define accumulated timeline graphs: accumulated charge vs. run
    def make_acc = { name ->
      def g = new GraphErrors(name);
      g.setTitle("Accumulated Charge [mC]");
      g.setTitleY("Accumulated Charge [mC]");
      g.setTitleX("Run Number");
      g
    }
    def acc_gated_dsc2         = make_acc("gated_DSC2");
    def acc_ungated_dsc2       = make_acc("ungated_DSC2");
    def acc_gated_hel_p_struck = make_acc("gated_STRUCK_hel_pos");
    def acc_gated_hel_0_struck = make_acc("gated_STRUCK_hel_0");
    def acc_gated_hel_n_struck = make_acc("gated_STRUCK_hel_neg");

    // loop over runs, filling graphs
    data.sort{it.key}.each{ runnum, run_data ->
      // define run graphs: charge vs. QA bin, for this run
      def make_rn = { name, title, run ->
        def g = new GraphErrors("${name}__${run}");
        g.setTitle("${title} -- run #{run}");
        g.setTitleY("Charge [mC]");
        g.setTitleX("QA Bin");
        g
      }
      rn_gated_dsc2         = make_rn("gated_DSC2",           "gated DSC2 charge");
      rn_ungated_dsc2       = make_rn("ungated_DSC2",         "ungated DSC2 charge");
      rn_gated_hel_p_struck = make_rn("gated_STRUCK_hel_pos", "gated STRUCK charge for helicity=+1");
      rn_gated_hel_0_struck = make_rn("gated_STRUCK_hel_0",   "gated STRUCK charge for helicity=0");
      rn_gated_hel_n_struck = make_rn("gated_STRUCK_hel_neg", "gated STRUCK charge for helicity=-1");
      // fill run graphs
      run_data['histos'].each{ binnum, histos ->
        rn_gated_dsc2.addPoint(         binnum, Charge.to_mC(histos.getChargeGatedDSC2()),     0, 0 );
        rn_ungated_dsc2.addPoint(       binnum, Charge.to_mC(histos.getChargeUngatedDSC2()),   0, 0 );
        rn_gated_hel_p_struck.addPoint( binnum, Charge.to_mC(histos.getChargeGatedSTRUCK(1)),  0, 0 );
        rn_gated_hel_0_struck.addPoint( binnum, Charge.to_mC(histos.getChargeGatedSTRUCK(0)),  0, 0 );
        rn_gated_hel_n_struck.addPoint( binnum, Charge.to_mC(histos.getChargeGatedSTRUCK(-1)), 0, 0 );
      }
      // set Poisson errors
      def set_errors = { g ->
        g.getDataSize(0).times{ i -> g.setError(i, 0, Math.sqrt(g.getDataY(i))); }
      }
      set_errors(rn_gated_dsc2);
      set_errors(rn_ungated_dsc2);
      set_errors(rn_gated_hel_p_struck);
      set_errors(rn_gated_hel_0_struck);
      set_errors(rn_gated_hel_n_struck);
      // fill timeline graphs
      def add_tl_point = { rn, tl ->
        def val = 0.0;
        rn.getDataSize(0).times{ i -> val += rn.getDataY(i); }
        tl.addPoint(runnum, val);
      }
      add_tl_point(rn_gated_dsc2,         tl_gated_dsc2);
      add_tl_point(rn_ungated_dsc2,       tl_ungated_dsc2);
      add_tl_point(rn_gated_hel_p_struck, tl_gated_hel_p_struck);
      add_tl_point(rn_gated_hel_0_struck, tl_gated_hel_0_struck);
      add_tl_point(rn_gated_hel_n_struck, tl_gated_hel_n_struck);
      // write run graphs
      [ tdir_per, tdir_acc ].each{ tdir ->
        tdir.mkdir("/${runnum}");
        tdir.cd("/${runnum}");
        tdir.addDataSet(rn_gated_dsc2);
        tdir.addDataSet(rn_ungated_dsc2);
        tdir.addDataSet(rn_gated_hel_p_struck);
        tdir.addDataSet(rn_gated_hel_0_struck);
        tdir.addDataSet(rn_gated_hel_n_struck);
      }
    }

    // fill accumulated timeline graphs
    def fill_acc = { acc, tl ->
      tl.getDataSize(0).times{ i ->
        acc.addPoint(
          tl.getDataX(i),
          i==0 ? tl.getDataY(i) : tl.getDataY(i) + acc.getDataY(i-1)
        );
      }
    }
    fill_acc(acc_gated_dsc2,         tl_gated_dsc2);
    fill_acc(acc_ungated_dsc2,       tl_ungated_dsc2);
    fill_acc(acc_gated_hel_p_struck, tl_gated_hel_p_struck);
    fill_acc(acc_gated_hel_0_struck, tl_gated_hel_0_struck);
    fill_acc(acc_gated_hel_n_struck, tl_gated_hel_n_struck);

    // write timelines
    tdir_per.mkdir('/timelines');
    tdir_per.cd('/timelines');
    tdir_per.addDataSet(tl_gated_dsc2);
    tdir_per.addDataSet(tl_ungated_dsc2);
    tdir_per.addDataSet(tl_gated_hel_p_struck);
    tdir_per.addDataSet(tl_gated_hel_0_struck);
    tdir_per.addDataSet(tl_gated_hel_n_struck);
    tdir_acc.mkdir('/timelines');
    tdir_acc.cd('/timelines');
    tdir_acc.addDataSet(acc_gated_dsc2);
    tdir_acc.addDataSet(acc_ungated_dsc2);
    tdir_acc.addDataSet(acc_gated_hel_p_struck);
    tdir_acc.addDataSet(acc_gated_hel_0_struck);
    tdir_acc.addDataSet(acc_gated_hel_n_struck);

    // write HIPO
    tdir_per.writeFile('qadb_charge.hipo');
    tdir_acc.writeFile('qadb_charge_accumulated.hipo');
  }

}
