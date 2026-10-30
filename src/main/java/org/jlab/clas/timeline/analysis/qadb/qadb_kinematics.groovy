package org.jlab.clas.timeline.analysis.qadb

import java.util.concurrent.ConcurrentHashMap

import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.TDirectory

import org.jlab.clas.timeline.histograms.qadb.Kinematics

class qadb_kinematics {

  def data_map = new ConcurrentHashMap()

  // ----------------------------------------------------------------------------------

  def processRun(dir, runnum, qa_map)
  {
    data_map[runnum] = [
      run: runnum,
      histos: [:]
    ]

    def missing_bins = []

    qa_map[runnum].each { qa_bin ->
      def binnum = qa_bin.getBinNum()
      def histos = new Kinematics(binnum)

      if(histos.hasHistograms(dir, binnum)) {
        histos.readHistograms(dir, binnum)
        data_map[runnum]['histos'][binnum] = histos
      }
      else {
        missing_bins.add(binnum)
      }
    }

    if(!missing_bins.isEmpty()) {
      System.err.println(
          "WARNING: Skipping missing kinematic histograms " +
          "for run ${runnum}, QA bins ${missing_bins}")
    }
  }

  // ----------------------------------------------------------------------------------

  def calculateRunStatistics(runnum)
  {
    def histograms = data_map[runnum]['histos'].values()

    if(histograms.isEmpty()) {
      throw new IllegalArgumentException(
          "No kinematic histograms found for run ${runnum}")
    }

    def iterator = histograms.iterator()
    def first = iterator.next()

    def q2 = first.q2_hist.histClone("q2_run${runnum}")
    def x = first.x_hist.histClone("x_run${runnum}")
    def w = first.w_hist.histClone("w_run${runnum}")
    def y = first.y_hist.histClone("y_run${runnum}")

    while(iterator.hasNext()) {
      def next = iterator.next()

      q2.add(next.q2_hist)
      x.add(next.x_hist)
      w.add(next.w_hist)
      y.add(next.y_hist)
    }

    return [
      q2: [mean: q2.getMean(), rms: q2.getRMS(), hist: q2],
      x:  [mean: x.getMean(),  rms: x.getRMS(),  hist: x],
      w:  [mean: w.getMean(),  rms: w.getRMS(),  hist: w],
      y:  [mean: y.getMean(),  rms: y.getRMS(),  hist: y]
    ]
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map)
  {
    TDirectory mean_directory = new TDirectory()
    TDirectory rms_directory = new TDirectory()

    def variable_info = [
      q2: [
        title: 'Q^2',
        mean_y: 'Mean Q^2 [GeV^2]',
        rms_y: 'Q^2 Standard Deviation [GeV^2]'
      ],
      x: [
        title: 'Bjorken x',
        mean_y: 'Mean Bjorken x',
        rms_y: 'Bjorken x Standard Deviation'
      ],
      w: [
        title: 'W',
        mean_y: 'Mean W [GeV]',
        rms_y: 'W Standard Deviation [GeV]'
      ],
      y: [
        title: 'y',
        mean_y: 'Mean y',
        rms_y: 'y Standard Deviation'
      ]
    ]

    def make_timeline = { name, title, y_title ->
      def graph = new GraphErrors(name)

      graph.setTitle(title)
      graph.setTitleX('Run Number')
      graph.setTitleY(y_title)

      graph
    }

    def mean_graphs = [:]
    def rms_graphs = [:]

    variable_info.each { variable, info ->
      mean_graphs[variable] = make_timeline(
          "${variable}_mean",
          "${info.title} Mean",
          info.mean_y)

      rms_graphs[variable] = make_timeline(
          "${variable}_rms",
          "${info.title} Standard Deviation",
          info.rms_y)
    }

    data_map
      .findAll { _, run_data -> !run_data['histos'].isEmpty() }
      .sort { it.key }
      .each { runnum, run_data ->
        def statistics = calculateRunStatistics(runnum)

        variable_info.each { variable, _ ->
          def variable_statistics = statistics[variable]

          /*
           * Do not create plausible-looking zero-valued timeline
           * points when the distribution contains no in-range data.
           */
          if(variable_statistics.hist.integral() > 0
              && Double.isFinite(variable_statistics.mean)
              && Double.isFinite(variable_statistics.rms))
          {
            mean_graphs[variable].addPoint(
                runnum,
                variable_statistics.mean,
                0,
                0)

            rms_graphs[variable].addPoint(
                runnum,
                variable_statistics.rms,
                0,
                0)
          }
        }

        /*
         * Save the combined histograms for this run together with
         * both timeline outputs.
         */
        [mean_directory, rms_directory].each { directory ->
          directory.mkdir("/${runnum}")
          directory.cd("/${runnum}")

          variable_info.each { variable, _ ->
            directory.addDataSet(statistics[variable].hist)
          }
        }
      }

    mean_directory.mkdir('/timelines')
    mean_directory.cd('/timelines')

    mean_graphs.each { _, graph ->
      mean_directory.addDataSet(graph)
    }

    rms_directory.mkdir('/timelines')
    rms_directory.cd('/timelines')

    rms_graphs.each { _, graph ->
      rms_directory.addDataSet(graph)
    }

    mean_directory.writeFile(
        new File(output_dir, 'qadb_inclusive_kinematics_mean.hipo').getPath())

    rms_directory.writeFile(
        new File(output_dir, 'qadb_inclusive_kinematics_rms.hipo').getPath())
  }
}
