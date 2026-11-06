package org.jlab.clas.timeline.analysis.qadb

import groovy.transform.PackageScope

import java.util.concurrent.ConcurrentHashMap

import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.TDirectory

import org.jlab.clas.timeline.histograms.qadb.PionKinematics

class qadb_pion_kinematics {

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

      /* Beam energy is not used when restoring persisted histograms. */
      def histos = new PionKinematics(binnum, 1.0)

      if(histos.readHistograms(dir, binnum)) {
        data_map[runnum]['histos'][binnum] = histos
      }
      else {
        missing_bins.add(binnum)
      }
    }

    if(!missing_bins.isEmpty()) {
      System.err.println(
          "WARNING: Skipping missing pion kinematic histograms " +
          "for run ${runnum}, QA bins ${missing_bins}")
    }
  }

  // ----------------------------------------------------------------------------------

  def calculateRunStatistics(runnum)
  {
    def histograms = data_map[runnum]['histos'].values()

    if(histograms.isEmpty()) {
      throw new IllegalArgumentException(
          "No pion kinematic histograms found for run ${runnum}")
    }

    def particles = [
      pip: PionKinematics.PIP,
      pim: PionKinematics.PIM
    ]
    def variables = ['p', 'pT', 'z', 'theta', 'phiH']
    def combined = [:]

    particles.each { particle, pid ->
      combined[particle] = [:]

      variables.each { variable ->
        combined[particle][variable] = null
      }
    }

    histograms.each { histos ->
      particles.each { particle, pid ->
        variables.each { variable ->
          def source = histos.getHistogram(pid, variable)

          if(combined[particle][variable] == null) {
            combined[particle][variable] = source.histClone(
                "${particle}_${variable}_run${runnum}")
          }
          else {
            combined[particle][variable].add(source)
          }
        }
      }
    }

    def statistics = [:]

    particles.each { particle, _ ->
      statistics[particle] = [:]

      variables.each { variable ->
        def histogram = combined[particle][variable]
        statistics[particle][variable] = [
          mean: histogram.getMean(),
          rms: histogram.getRMS(),
          hist: histogram
        ]
      }
    }

    statistics
  }

  // ----------------------------------------------------------------------------------

  def write(qa_map)
  {
    writeToDirectory(qa_map, new File('.'))
  }

  @PackageScope
  def writeToDirectory(qa_map, File output_dir)
  {
    def particle_info = [
      pip: [file: 'piPlus', title: 'pi+'],
      pim: [file: 'piMinus', title: 'pi-']
    ]
    def variable_info = [
      p: [title: 'p', unit: ' [GeV]'],
      pT: [title: 'pT', unit: ' [GeV]'],
      z: [title: 'z', unit: ''],
      theta: [title: 'theta', unit: ' [rad]'],
      phiH: [title: 'phiH', unit: ' [rad]']
    ]
    def statistic_info = [
      mean: 'Mean',
      rms: 'Standard Deviation'
    ]

    def directories = [:]
    def graphs = [:]

    particle_info.each { particle, particle_data ->
      directories[particle] = [:]
      graphs[particle] = [:]

      statistic_info.each { statistic, statistic_title ->
        directories[particle][statistic] = new TDirectory()
        graphs[particle][statistic] = [:]

        variable_info.each { variable, variable_data ->
          def graph = new GraphErrors("${variable}_${statistic}")
          graph.setTitle(
              "${particle_data.title} ${variable_data.title} " +
              statistic_title)
          graph.setTitleX('Run Number')
          graph.setTitleY(
              "${statistic_title} ${variable_data.title}" +
              variable_data.unit)
          graphs[particle][statistic][variable] = graph
        }
      }
    }

    data_map
      .findAll { _, run_data -> !run_data['histos'].isEmpty() }
      .sort { it.key }
      .each { runnum, run_data ->
        def statistics = calculateRunStatistics(runnum)

        particle_info.keySet().each { particle ->
          variable_info.keySet().each { variable ->
            def variable_statistics = statistics[particle][variable]
            def histogram = variable_statistics.hist

            statistic_info.keySet().each { statistic ->
              def value = variable_statistics[statistic]

              if(histogram.integral() > 0
                  && Double.isFinite(value))
              {
                graphs[particle][statistic][variable].addPoint(
                    runnum,
                    value,
                    0,
                    0)
              }

              def directory = directories[particle][statistic]
              directory.mkdir("/${runnum}")
              directory.cd("/${runnum}")
              directory.addDataSet(histogram)
            }
          }
        }
      }

    particle_info.each { particle, particle_data ->
      statistic_info.keySet().each { statistic ->
        def directory = directories[particle][statistic]
        directory.mkdir('/timelines')
        directory.cd('/timelines')

        graphs[particle][statistic].values().each { graph ->
          directory.addDataSet(graph)
        }

        directory.writeFile(
            new File(
                output_dir,
                "qadb_${particle_data.file}_kinematics_${statistic}.hipo")
                .getPath())
      }
    }
  }
}
