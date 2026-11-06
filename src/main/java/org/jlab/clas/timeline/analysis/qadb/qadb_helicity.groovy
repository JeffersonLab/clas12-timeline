package org.jlab.clas.timeline.analysis.qadb

import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

import org.jlab.clas.timeline.util.Tools
import org.jlab.clas.timeline.histograms.qadb.Helicity

class qadb_helicity {

  def data_map = new ConcurrentHashMap()

  // ----------------------------------------------------------------------------------

  /*
     data_map
     |_ runnum 1
     |  |_ helicity
     |      |_ 0 -> histogram from Helicity.java for QA bin 0
     |      |_ 1 -> histogram from Helicity.java for QA bin 1
     |      :
     |      |_ N -> histogram from Helicity.java for QA bin N
     |
     |_ runnum 2
        |_ helicity
           |_ ...
  */
  def processRun(dir, runnum, qa_map) {

    data_map[runnum] = [
      run: runnum,
      helicity: [:]
    ]

    // Loop over the QA bins belonging to this run.
    qa_map[runnum].each { qa_bin ->

      def binnum = qa_bin.getBinNum()
      def helicity = new Helicity(binnum)

      helicity.readHistograms(dir, binnum)

      data_map[runnum]['helicity'][binnum] = helicity
    }
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map) {

    TDirectory tdir = new TDirectory()

    // Timeline graphs: helicity fraction versus run number.
    def make_tl = { name ->
      def graph = new GraphErrors(name)
      graph.setTitle('Helicity Fractions')
      graph.setTitleY('Event Fraction')
      graph.setTitleX('Run Number')
      graph
    }

    def tl_hel_positive  = make_tl('helicityPositive')
    def tl_hel_undefined = make_tl('helicityUndefined')
    def tl_hel_negative  = make_tl('helicityNegative')
    def tl_hel_defined   = make_tl('helicityDefined')

    // Loop over runs.
    data_map.sort { it.key }.each { runnum, run_data ->

      // Run graphs: quantities versus QA bin.
      def make_rn = { sort_prefix, name, title, ytitle ->
        def graph = new GraphErrors(
          "${sort_prefix}__${name}__${runnum}"
        )

        graph.setTitle(title)
        graph.setTitleY(ytitle)
        graph.setTitleX('QA Bin')

        graph
      }

      // Event counts versus QA bin.
      def rn_hel_positive_n = make_rn(
        'a1',
        'helicityPositiveCounts',
        'Positive Helicity Event Counts',
        'N_{+}'
      )

      def rn_hel_undefined_n = make_rn(
        'a2',
        'helicityUndefinedCounts',
        'Undefined Helicity Event Counts',
        'N_{0}'
      )

      def rn_hel_negative_n = make_rn(
        'a3',
        'helicityNegativeCounts',
        'Negative Helicity Event Counts',
        'N_{-}'
      )

      // Fractions versus QA bin.
      def rn_hel_positive_fraction = make_rn(
        'b1',
        'helicityPositiveFraction',
        'Positive Helicity Fraction',
        'N_{+} / N'
      )

      def rn_hel_undefined_fraction = make_rn(
        'b2',
        'helicityUndefinedFraction',
        'Undefined Helicity Fraction',
        'N_{0} / N'
      )

      def rn_hel_negative_fraction = make_rn(
        'b3',
        'helicityNegativeFraction',
        'Negative Helicity Fraction',
        'N_{-} / N'
      )

      // Loop over the helicity histogram of every QA bin.
      run_data['helicity'].sort { it.key }.each { binnum, histos ->

        /*
         * Helicity.java creates three bins:
         *
         * histogram bin 0 -> helicity -1
         * histogram bin 1 -> helicity  0
         * histogram bin 2 -> helicity +1
         */
        def n_negative  = histos.helicity_hist.getBinContent(0)
        def n_undefined = histos.helicity_hist.getBinContent(1)
        def n_positive  = histos.helicity_hist.getBinContent(2)

        def n_total = n_negative + n_undefined + n_positive

        def f_negative  = Tools.safeRatio(n_negative,  n_total)
        def f_undefined = Tools.safeRatio(n_undefined, n_total)
        def f_positive  = Tools.safeRatio(n_positive,  n_total)

        // Poisson uncertainties on counts.
        rn_hel_positive_n.addPoint(
          binnum, n_positive, 0, Math.sqrt(n_positive)
        )

        rn_hel_undefined_n.addPoint(
          binnum, n_undefined, 0, Math.sqrt(n_undefined)
        )

        rn_hel_negative_n.addPoint(
          binnum, n_negative, 0, Math.sqrt(n_negative)
        )

        // Binomial uncertainties on fractions.
        def fraction_uncertainty = { fraction ->
          n_total > 0
            ? Math.sqrt(fraction * (1.0 - fraction) / n_total)
            : 0.0
        }

        rn_hel_positive_fraction.addPoint(
          binnum,
          f_positive,
          0,
          fraction_uncertainty(f_positive)
        )

        rn_hel_undefined_fraction.addPoint(
          binnum,
          f_undefined,
          0,
          fraction_uncertainty(f_undefined)
        )

        rn_hel_negative_fraction.addPoint(
          binnum,
          f_negative,
          0,
          fraction_uncertainty(f_negative)
        )
      }

      // Calculate total event counts for this run.
      def sum_graph = { graph ->
        def sum = 0.0

        graph.getDataSize(0).times {
          sum += graph.getDataY(it)
        }

        sum
      }

      def n_positive_run  = sum_graph(rn_hel_positive_n)
      def n_undefined_run = sum_graph(rn_hel_undefined_n)
      def n_negative_run  = sum_graph(rn_hel_negative_n)

      def n_total_run =
        n_positive_run +
        n_undefined_run +
        n_negative_run

      // Fill timeline graphs.
      tl_hel_positive.addPoint(
        runnum,
        Tools.safeRatio(n_positive_run, n_total_run),
        0,
        0
      )

      tl_hel_undefined.addPoint(
        runnum,
        Tools.safeRatio(n_undefined_run, n_total_run),
        0,
        0
      )

      tl_hel_negative.addPoint(
        runnum,
        Tools.safeRatio(n_negative_run, n_total_run),
        0,
        0
      )

      tl_hel_defined.addPoint(
        runnum,
        Tools.safeRatio(n_positive_run + n_negative_run, n_total_run),
        0,
        0
      )

      // Write graphs for this run.
      tdir.mkdir("/${runnum}")
      tdir.cd("/${runnum}")

      tdir.addDataSet(rn_hel_positive_n)
      tdir.addDataSet(rn_hel_undefined_n)
      tdir.addDataSet(rn_hel_negative_n)

      tdir.addDataSet(rn_hel_positive_fraction)
      tdir.addDataSet(rn_hel_undefined_fraction)
      tdir.addDataSet(rn_hel_negative_fraction)
    }

    // Write timeline graphs.
    tdir.mkdir('/timelines')
    tdir.cd('/timelines')

    tdir.addDataSet(tl_hel_positive)
    tdir.addDataSet(tl_hel_undefined)
    tdir.addDataSet(tl_hel_negative)
    tdir.addDataSet(tl_hel_defined)

    // Write the output HIPO file.
    tdir.writeFile('qadb_helicity.hipo')
  }
}
