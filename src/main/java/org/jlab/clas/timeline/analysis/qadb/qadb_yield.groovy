package org.jlab.clas.timeline.analysis.qadb

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

import org.jlab.clas.timeline.util.Tools
import org.jlab.clas.timeline.histograms.qadb.Yield
import org.jlab.clas.timeline.histograms.qadb.Charge

class qadb_yield {

  def data_map = new ConcurrentHashMap()

  // ----------------------------------------------------------------------------------

  // the `data_map` data structure will be filled like so:
  /*
     data_map
     |_ runnum 1
     |  |
     |  |__ charge
     |  |   |_ 1 -> histograms from Charge.java for bin 1
     |  |   :
     |  |   |_ N -> histograms from Charge.java for bin N
     |  |
     |  |__ yield
     |      |_ 1 -> histograms from Yield.java for bin 1
     |      :
     |      |_ N -> histograms from Yield.java for bin N
     |
     |_ runnum 2
     |  |_ charge
     |  |  |_ ...
     |  |_ yield
     |     |_ ...
     :
  */
  def processRun(dir, runnum, qa_map) {
    data_map[runnum] = [run:runnum, yield:[:], charge:[:]]
    // loop over QA bins for this run
    qa_map[runnum].each { qa_bin ->
      def yield  = new Yield(qa_bin.getBinNum())
      def charge = new Charge(qa_bin.getBinNum())
      yield.readHistograms(dir, qa_bin.getBinNum())
      charge.readHistograms(dir, qa_bin.getBinNum())
      data_map[runnum]['yield'][qa_bin.getBinNum()]  = yield
      data_map[runnum]['charge'][qa_bin.getBinNum()] = charge
    }
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map) {

    // start ouput `TDirectory`s
    TDirectory tdir_fd_ele = new TDirectory() // forward detector (FD) electrons
    TDirectory tdir_ft_ele = new TDirectory() // forward tagger (FT) electrons

    // define timeline ('tl') graphs: N/q vs. run number
    def make_tl = { name, title ->
      def g = new GraphErrors(name)
      g.setTitle title
      g.setTitleY 'N/q [nC^-1]'
      g.setTitleX 'Run Number'
      g
    }
    def tl_fd_ele_nq = Tools.collectSectors{ s -> // `collectSectors` will build a map of sector number (`s`) -> graph returned by `make_tl`
      make_tl "FD_ele_sec${s}", 'FD Electron Yield N / Charge q [nC]' // they all get the same title, since they'll be plotted on the same canvas; the graph name goes in the legend
    }
    def tl_ft_ele_nq = make_tl "FT_ele", 'FT Electron Yield N / Charge q [nC]'

    // loop over runs, filling graphs
    data_map.sort{it.key}.each { runnum, run_data ->

      // define run graphs ('rn'): e.g., N/q vs. QA bin, for this run
      // NOTE: the front-end will order them alphabetically, so prefix their names with unique letters (`sort_prefix`)
      def make_rn = { sort_prefix, name, title, ytitle ->
        def g = new GraphErrors("${sort_prefix}__${name}__${runnum}")
        g.setTitle  title
        g.setTitleY ytitle
        g.setTitleX 'QA Bin'
        g
      }
      def rn_fd_ele_nq = Tools.collectSectors{ s -> make_rn "a${s}a", "FD_ele_sec${s}_nq", "FD sector ${s} Electron N/q [nC^-1]", 'N/q [nC^-1])' }
      def rn_fd_ele_n  = Tools.collectSectors{ s -> make_rn "a${s}b", "FD_ele_sec${s}_n",  "FD sector ${s} Electron N",           'N' }
      def rn_ft_ele_nq = make_rn 'aa', "FT_ele_nq", 'FT Electron N/q [nC^-1]', 'N/q [nC^-1]'
      def rn_ft_ele_n  = make_rn 'ab', "FT_ele_n",  'FT Electron N',           'N'

      // fill run graphs: loop over each QA bin's histograms (`Yield` objects)
      run_data['yield'].each { binnum, histos ->
        // get the yields
        def n_fd_ele = [
          1: histos.yield_hist.getBinContent(Yield.Channel.electronFD_sec1.ordinal()),
          2: histos.yield_hist.getBinContent(Yield.Channel.electronFD_sec2.ordinal()),
          3: histos.yield_hist.getBinContent(Yield.Channel.electronFD_sec3.ordinal()),
          4: histos.yield_hist.getBinContent(Yield.Channel.electronFD_sec4.ordinal()),
          5: histos.yield_hist.getBinContent(Yield.Channel.electronFD_sec5.ordinal()),
          6: histos.yield_hist.getBinContent(Yield.Channel.electronFD_sec6.ordinal()),
        ]
        def n_ft_ele = histos.yield_hist.getBinContent(Yield.Channel.electronFT.ordinal())
        // get the charge
        def q = run_data['charge'][binnum].getChargeGatedDSC2()
        // plot the points, with Poisson uncertainty
        Tools.eachSector { s ->
          rn_fd_ele_nq[s].addPoint binnum, Tools.safeRatio(n_fd_ele[s], q), 0, Tools.ratioPoissonUncertainty(n_fd_ele[s], q)
          rn_fd_ele_n[s].addPoint  binnum, n_fd_ele[s],                     0, Math.sqrt(n_fd_ele[s])
        }
        rn_ft_ele_nq.addPoint binnum, Tools.safeRatio(n_ft_ele, q), 0, Tools.ratioPoissonUncertainty(n_ft_ele, q)
        rn_ft_ele_n.addPoint  binnum, n_ft_ele,                     0, Math.sqrt(n_ft_ele)
      }

      // calculate total charge for this run by summing over its QA bins' charges
      def q_run = 0.0
      run_data['charge'].each{ binnum, histos -> q_run += histos.getChargeGatedDSC2() }

      // fill timeline graphs
      def add_tl_point = { rn_n, tl_nq ->
        // get the total N for this run
        def n_run = 0.0
        rn_n.getDataSize(0).times{ n_run += rn_n.getDataY(it) }
        // put total N / total q for this run onto the graph
        tl_nq.addPoint runnum, Tools.safeRatio(n_run,q_run), 0, 0
      }
      Tools.eachSector{ s -> add_tl_point rn_fd_ele_n[s], tl_fd_ele_nq[s] }
      add_tl_point rn_ft_ele_n, tl_ft_ele_nq

      // write run graphs for this run
      tdir_fd_ele.mkdir "/${runnum}"
      tdir_fd_ele.cd    "/${runnum}"
      Tools.eachSector{ s ->
        tdir_fd_ele.addDataSet rn_fd_ele_nq[s]
        tdir_fd_ele.addDataSet rn_fd_ele_n[s]
      }
      tdir_ft_ele.mkdir "/${runnum}"
      tdir_ft_ele.cd    "/${runnum}"
      tdir_ft_ele.addDataSet rn_ft_ele_nq
      tdir_ft_ele.addDataSet rn_ft_ele_n

    } // end loop over runs

    // write timelines
    tdir_fd_ele.mkdir '/timelines'
    tdir_fd_ele.cd    '/timelines'
    Tools.eachSector{ s -> tdir_fd_ele.addDataSet tl_fd_ele_nq[s] }
    tdir_ft_ele.mkdir '/timelines'
    tdir_ft_ele.cd    '/timelines'
    tdir_ft_ele.addDataSet tl_ft_ele_nq

    // write HIPO files
    tdir_fd_ele.writeFile 'qadb_yield_FD_electrons.hipo'
    tdir_ft_ele.writeFile 'qadb_yield_FT_electrons.hipo'
  }

}
