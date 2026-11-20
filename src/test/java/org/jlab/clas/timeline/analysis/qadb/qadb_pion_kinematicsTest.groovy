package org.jlab.clas.timeline.analysis.qadb

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

import java.nio.file.Path

import org.jlab.groot.data.TDirectory
import org.jlab.io.hipo.HipoDataEvent
import org.jlab.jnp.hipo4.data.Event
import org.jlab.jnp.hipo4.data.Schema
import org.jlab.jnp.hipo4.data.SchemaFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import org.jlab.clas.timeline.histograms.qadb.Electron
import org.jlab.clas.timeline.histograms.qadb.PionKinematics
import org.jlab.clas.timeline.histograms.qadb.QadbBinHistograms

class qadb_pion_kinematicsTest {

  private static class TestQaBin {
    int binNum

    int getBinNum()
    {
      binNum
    }
  }

  private static void fillVariables(
      PionKinematics histograms,
      int pid,
      Map<String, Double> values)
  {
    values.each { variable, value ->
      histograms.getHistogram(pid, variable).fill(value)
    }
  }

  private static HipoDataEvent acceptedContainerEvent()
  {
    def particleSchema = new Schema.SchemaBuilder(
        'REC::Particle', 1200, 1)
        .addEntry('pid', 'I', '')
        .addEntry('status', 'S', '')
        .addEntry('chi2pid', 'F', '')
        .addEntry('px', 'F', '')
        .addEntry('py', 'F', '')
        .addEntry('pz', 'F', '')
        .addEntry('vx', 'F', '')
        .addEntry('vy', 'F', '')
        .addEntry('vz', 'F', '')
        .build()
    def calorimeterSchema = new Schema.SchemaBuilder(
        'REC::Calorimeter', 1201, 1)
        .addEntry('pindex', 'S', '')
        .addEntry('detector', 'B', '')
        .addEntry('sector', 'B', '')
        .build()

    def factory = new SchemaFactory()
    factory.addSchema(particleSchema)
    factory.addSchema(calorimeterSchema)

    def event = new HipoDataEvent(new Event(), factory)
    def particles = event.createBank('REC::Particle', 3)
    def particleData = [
      [pid: Electron.PDG, px: 1.02606043f, py: 0.0f, pz: 2.81907786f],
      [pid: PionKinematics.PIP, px: 0.3f, py: 0.4f, pz: 1.2f],
      [pid: PionKinematics.PIM, px: 0.3f, py: -0.4f, pz: 1.2f]
    ]

    particleData.eachWithIndex { particle, row ->
      particles.setInt('pid', row, particle.pid)
      particles.setShort('status', row, (short) (row == 0 ? -2000 : 2000))
      particles.setFloat('chi2pid', row, 0.0f)
      particles.setFloat('px', row, particle.px)
      particles.setFloat('py', row, particle.py)
      particles.setFloat('pz', row, particle.pz)
      particles.setFloat('vx', row, 0.0f)
      particles.setFloat('vy', row, 0.0f)
      particles.setFloat('vz', row, 0.0f)
    }

    def calorimeter = event.createBank('REC::Calorimeter', 1)
    calorimeter.setShort('pindex', 0, (short) 0)
    calorimeter.setByte('detector', 0, (byte) Electron.ECAL_ID)
    calorimeter.setByte('sector', 0, (byte) 1)
    event.appendBanks(particles, calorimeter)
    event
  }

  @Test
  void processRunReadsEveryQaBin()
  {
    def directory = new TDirectory()

    def first = new PionKinematics(1, 6.4)
    fillVariables(first, PionKinematics.PIP, [
      p: 1.01, pT: 0.21, z: 0.205, theta: 0.15, phiH: -1.0
    ])
    first.write(directory)

    def second = new PionKinematics(2, 6.4)
    fillVariables(second, PionKinematics.PIM, [
      p: 4.01, pT: 1.31, z: 0.705, theta: 0.9, phiH: 2.0
    ])
    second.write(directory)

    def qa_map = [
      42: [
        new TestQaBin(binNum: 1),
        new TestQaBin(binNum: 2)
      ]
    ]

    def analysis = new qadb_pion_kinematics()
    analysis.processRun(directory, 42, qa_map)

    assertEquals([1, 2] as Set, analysis.data_map[42]['histos'].keySet())
    assertEquals(
        1,
        analysis.data_map[42]['histos'][1]
            .getHistogram(PionKinematics.PIP, 'p')
            .getEntries())
    assertEquals(
        1,
        analysis.data_map[42]['histos'][2]
            .getHistogram(PionKinematics.PIM, 'p')
            .getEntries())
  }

  @Test
  void calculateRunStatisticsCombinesQaBinsForBothCharges()
  {
    def directory = new TDirectory()

    def first = new PionKinematics(1, 6.4)
    fillVariables(first, PionKinematics.PIP, [
      p: 1.01, pT: 0.21, z: 0.205, theta: 0.15, phiH: -1.0
    ])
    fillVariables(first, PionKinematics.PIM, [
      p: 2.01, pT: 0.91, z: 0.305, theta: 0.3, phiH: -2.0
    ])
    first.write(directory)

    def second = new PionKinematics(2, 6.4)
    fillVariables(second, PionKinematics.PIP, [
      p: 3.01, pT: 0.61, z: 0.605, theta: 0.75, phiH: 1.0
    ])
    fillVariables(second, PionKinematics.PIM, [
      p: 4.01, pT: 1.31, z: 0.705, theta: 0.9, phiH: 2.0
    ])
    second.write(directory)

    def qa_map = [
      42: [
        new TestQaBin(binNum: 1),
        new TestQaBin(binNum: 2)
      ]
    ]

    def analysis = new qadb_pion_kinematics()
    analysis.processRun(directory, 42, qa_map)

    def statistics = analysis.calculateRunStatistics(42)
    def expected = [
      pip: [
        p:     [mean: 2.1,                rms: 1.0],
        pT:    [mean: 0.4,                rms: 0.2],
        z:     [mean: 0.41,               rms: 0.2],
        theta: [mean: 0.43982297150257105, rms: 0.2984513020910304],
        phiH:  [mean: 0.0,                rms: 0.945]
      ],
      pim: [
        p:     [mean: 3.1,                rms: 1.0],
        pT:    [mean: 1.12,               rms: 0.2],
        z:     [mean: 0.51,               rms: 0.2],
        theta: [mean: 0.5969026041820608,  rms: 0.2984513020910304],
        phiH:  [mean: 0.0,                rms: 1.953]
      ]
    ]

    expected.each { particle, variables ->
      variables.each { variable, values ->
        assertNotNull(statistics[particle][variable].hist)
        assertEquals(2.0, statistics[particle][variable].hist.integral(), 0.0)
        assertEquals(
            values.mean,
            statistics[particle][variable].mean,
            1.0e-9)
        assertEquals(
            values.rms,
            statistics[particle][variable].rms,
            1.0e-9)
      }
    }
  }

  @Test
  void containerEventSurvivesDiskRoundTripAndTimelineAggregation(
      @TempDir Path tempDir)
  {
    def container = new QadbBinHistograms(7, 6.4)
    container.processEvent(acceptedContainerEvent())

    def written = new TDirectory()
    container.write(written)
    def persistedFile = tempDir.resolve('out_QADB_42.hipo')
    written.writeFile(persistedFile.toString())

    def restored = new TDirectory()
    restored.readFile(persistedFile.toString())

    def qaMap = [42: [new TestQaBin(binNum: 7)]]
    def analysis = new qadb_pion_kinematics()
    analysis.processRun(restored, 42, qaMap)

    def statistics = analysis.calculateRunStatistics(42)
    assertEquals(1.0, statistics.pip.p.hist.integral(), 0.0)
    assertEquals(1.0, statistics.pim.p.hist.integral(), 0.0)

    analysis.writeToDirectory(qaMap, tempDir.toFile())
    def timeline = new TDirectory()
    timeline.readFile(
        tempDir.resolve('qadb_piPlus_kinematics_mean.hipo').toString())

    assertEquals(1, timeline.getObject('/timelines/p_mean').getDataSize(0))
    assertEquals(42.0, timeline.getObject('/timelines/p_mean').getDataX(0), 0.0)
  }

  @Test
  void processRunSkipsOnlyMissingBins()
  {
    def directory = new TDirectory()
    new PionKinematics(1, 6.4).write(directory)

    def qa_map = [
      42: [
        new TestQaBin(binNum: 1),
        new TestQaBin(binNum: 2)
      ]
    ]

    def analysis = new qadb_pion_kinematics()
    analysis.processRun(directory, 42, qa_map)

    assertEquals([1] as Set, analysis.data_map[42]['histos'].keySet())
    assertTrue(analysis.data_map[42]['histos'].containsKey(1))
  }

  @Test
  void writeCreatesFourChargeSeparatedTimelineFiles(@TempDir Path tempDir)
  {
    def directory = new TDirectory()
    def written = new PionKinematics(1, 6.4)

    [
      [pid: PionKinematics.PIP,
       first: [p: 1.01, pT: 0.21, z: 0.205, theta: 0.15, phiH: -1.0],
       second: [p: 3.01, pT: 0.61, z: 0.605, theta: 0.75, phiH: 1.0]],
      [pid: PionKinematics.PIM,
       first: [p: 2.01, pT: 0.91, z: 0.305, theta: 0.3, phiH: -2.0],
       second: [p: 4.01, pT: 1.31, z: 0.705, theta: 0.9, phiH: 2.0]]
    ].each { data ->
      fillVariables(written, data.pid, data.first)
      fillVariables(written, data.pid, data.second)
    }

    written.write(directory)

    def analysis = new qadb_pion_kinematics()
    analysis.processRun(
        directory,
        42,
        [42: [new TestQaBin(binNum: 1)]])
    analysis.writeToDirectory([42: []], tempDir.toFile())

    def expected = [
      piPlus: [
        histogramPrefix: 'pip',
        mean: [
          p: 2.1, pT: 0.4, z: 0.41,
          theta: 0.43982297150257105, phiH: 0.0
        ],
        rms: [
          p: 1.0, pT: 0.2, z: 0.2,
          theta: 0.2984513020910304, phiH: 0.945
        ]
      ],
      piMinus: [
        histogramPrefix: 'pim',
        mean: [
          p: 3.1, pT: 1.12, z: 0.51,
          theta: 0.5969026041820608, phiH: 0.0
        ],
        rms: [
          p: 1.0, pT: 0.2, z: 0.2,
          theta: 0.2984513020910304, phiH: 1.953
        ]
      ]
    ]

    expected.each { particle, particleData ->
      ['mean', 'rms'].each { statistic ->
        def output = new TDirectory()
        output.readFile(
            tempDir.resolve(
                "qadb_${particle}_kinematics_${statistic}.hipo")
                .toString())

        particleData[statistic].each { variable, value ->
          def graph = output.getObject(
              "/timelines/${variable}_${statistic}")

          assertNotNull(graph)
          assertEquals(1, graph.getDataSize(0))
          assertEquals(42.0, graph.getDataX(0), 0.0)
          assertEquals(value, graph.getDataY(0), 1.0e-9)
          assertEquals(0.0, graph.getDataEX(0), 0.0)
          assertEquals(0.0, graph.getDataEY(0), 0.0)

          assertNotNull(output.getObject(
              "/42/${particleData.histogramPrefix}_${variable}_run42"))
        }
      }
    }
  }

  @Test
  void writeSkipsEmptyOrNonFiniteDistributionsIndependently(
      @TempDir Path tempDir)
  {
    def directory = new TDirectory()
    def written = new PionKinematics(1, 6.4)

    written.getHistogram(PionKinematics.PIP, 'p').fill(1.01)
    written.getHistogram(PionKinematics.PIP, 'pT')
        .setBinContent(0, Double.NaN)
    written.write(directory)

    def analysis = new qadb_pion_kinematics()
    analysis.processRun(
        directory,
        42,
        [42: [new TestQaBin(binNum: 1)]])
    analysis.writeToDirectory([42: []], tempDir.toFile())

    def piPlusMean = new TDirectory()
    piPlusMean.readFile(
        tempDir.resolve('qadb_piPlus_kinematics_mean.hipo').toString())

    assertEquals(
        1,
        piPlusMean.getObject('/timelines/p_mean').getDataSize(0))
    assertEquals(
        0,
        piPlusMean.getObject('/timelines/pT_mean').getDataSize(0))

    ['z', 'theta', 'phiH'].each { variable ->
      assertEquals(
          0,
          piPlusMean
              .getObject("/timelines/${variable}_mean")
              .getDataSize(0))
    }

    ['mean', 'rms'].each { statistic ->
      def piMinus = new TDirectory()
      piMinus.readFile(
          tempDir.resolve(
              "qadb_piMinus_kinematics_${statistic}.hipo")
              .toString())

      ['p', 'pT', 'z', 'theta', 'phiH'].each { variable ->
        assertEquals(
            0,
            piMinus
                .getObject("/timelines/${variable}_${statistic}")
                .getDataSize(0))
      }
    }
  }

  @Test
  void legacyFilesProduceEmptyGraphsWithoutThrowing(@TempDir Path tempDir)
  {
    def analysis = new qadb_pion_kinematics()
    def qa_map = [42: [new TestQaBin(binNum: 1)]]

    analysis.processRun(new TDirectory(), 42, qa_map)
    analysis.writeToDirectory(qa_map, tempDir.toFile())

    [piPlus: 'pip', piMinus: 'pim'].each { particle, _ ->
      ['mean', 'rms'].each { statistic ->
        def output = new TDirectory()
        output.readFile(
            tempDir.resolve(
                "qadb_${particle}_kinematics_${statistic}.hipo")
                .toString())

        ['p', 'pT', 'z', 'theta', 'phiH'].each { variable ->
          assertEquals(
              0,
              output
                  .getObject("/timelines/${variable}_${statistic}")
                  .getDataSize(0))
        }
      }
    }
  }
}
