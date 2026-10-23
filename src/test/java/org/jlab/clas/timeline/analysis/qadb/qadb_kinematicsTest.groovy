package org.jlab.clas.timeline.analysis.qadb

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

import java.nio.file.Path

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.jlab.groot.data.TDirectory

import org.jlab.clas.timeline.histograms.qadb.Kinematics

class qadb_kinematicsTest {

  private static class TestQaBin {
    int binNum

    int getBinNum()
    {
      binNum
    }
  }

  @Test
  void processRunReadsKinematicsHistogramsForEveryQaBin()
  {
    def directory = new TDirectory()

    def written = new Kinematics(2)
    written.q2_hist.fill(3.05)
    written.x_hist.fill(0.35)
    written.w_hist.fill(2.50)
    written.y_hist.fill(0.45)
    written.write(directory)

    def qa_map = [
      42: [new TestQaBin(binNum: 2)]
    ]

    def analysis = new qadb_kinematics()
    analysis.processRun(directory, 42, qa_map)

    def read = analysis.data_map[42]['histos'][2]

    assertNotNull(read)
    assertEquals(1, read.q2_hist.getEntries())
    assertEquals(1, read.x_hist.getEntries())
    assertEquals(1, read.w_hist.getEntries())
    assertEquals(1, read.y_hist.getEntries())
  }

  @Test
  void calculateRunStatisticsCombinesAllQaBins()
  {
    def directory = new TDirectory()

    def first = new Kinematics(1)
    first.q2_hist.fill(1.02)
    first.x_hist.fill(0.205)
    first.w_hist.fill(0.99)
    first.y_hist.fill(0.205)
    first.write(directory)

    def second = new Kinematics(2)
    second.q2_hist.fill(3.06)
    second.x_hist.fill(0.605)
    second.w_hist.fill(3.03)
    second.y_hist.fill(0.605)
    second.write(directory)

    def qa_map = [
      42: [
        new TestQaBin(binNum: 1),
        new TestQaBin(binNum: 2)
      ]
    ]

    def analysis = new qadb_kinematics()
    analysis.processRun(directory, 42, qa_map)

    def statistics = analysis.calculateRunStatistics(42)

    assertEquals(2.04, statistics.q2.mean, 1.0e-9)
    assertEquals(1.02, statistics.q2.rms, 1.0e-9)

    assertEquals(0.405, statistics.x.mean, 1.0e-9)
    assertEquals(0.200, statistics.x.rms, 1.0e-9)

    assertEquals(2.01, statistics.w.mean, 1.0e-9)
    assertEquals(1.02, statistics.w.rms, 1.0e-9)

    assertEquals(0.405, statistics.y.mean, 1.0e-9)
    assertEquals(0.200, statistics.y.rms, 1.0e-9)
  }

  @Test
  void writeCreatesMeanAndRmsTimelines(@TempDir Path tempDir)
  {
    def directory = new TDirectory()

    def written = new Kinematics(1)

    written.q2_hist.fill(1.02)
    written.q2_hist.fill(3.06)

    written.x_hist.fill(0.205)
    written.x_hist.fill(0.605)

    written.w_hist.fill(0.99)
    written.w_hist.fill(3.03)

    written.y_hist.fill(0.205)
    written.y_hist.fill(0.605)

    written.write(directory)

    def qa_map = [
      42: [new TestQaBin(binNum: 1)]
    ]

    def analysis = new qadb_kinematics()
    analysis.processRun(directory, 42, qa_map)
    analysis.write(qa_map, tempDir.toString())

    def meanDirectory = new TDirectory()
    meanDirectory.readFile(
        tempDir.resolve('qadb_kinematics_mean.hipo').toString())

    def rmsDirectory = new TDirectory()
    rmsDirectory.readFile(
        tempDir.resolve('qadb_kinematics_rms.hipo').toString())

    def expected = [
      q2: [mean: 2.04,  rms: 1.02],
      x:  [mean: 0.405, rms: 0.200],
      w:  [mean: 2.01,  rms: 1.02],
      y:  [mean: 0.405, rms: 0.200]
    ]

    expected.each { variable, values ->
      def meanGraph = meanDirectory.getObject(
          "/timelines/${variable}_mean")

      def rmsGraph = rmsDirectory.getObject(
          "/timelines/${variable}_rms")

      assertNotNull(meanGraph)
      assertNotNull(rmsGraph)

      assertEquals(1, meanGraph.getDataSize(0))
      assertEquals(1, rmsGraph.getDataSize(0))

      assertEquals(42.0, meanGraph.getDataX(0), 1.0e-9)
      assertEquals(42.0, rmsGraph.getDataX(0), 1.0e-9)

      assertEquals(values.mean, meanGraph.getDataY(0), 1.0e-9)
      assertEquals(values.rms, rmsGraph.getDataY(0), 1.0e-9)
    }
  }

  @Test
  void skipsLegacyRunsWithoutKinematicsHistograms(
      @TempDir Path tempDir)
  {
    /*
     * This directory represents a QADB file produced before
     * kinematic histograms were introduced.
     */
    def legacyDirectory = new TDirectory()

    def qa_map = [
      42: [new TestQaBin(binNum: 1)]
    ]

    def analysis = new qadb_kinematics()
    analysis.processRun(legacyDirectory, 42, qa_map)

    assertTrue(analysis.data_map[42]['histos'].isEmpty())

    /*
     * Missing kinematics must not prevent the other QADB
     * analyses from completing.
     */
    analysis.write(qa_map, tempDir.toString())

    def meanDirectory = new TDirectory()
    meanDirectory.readFile(
        tempDir.resolve('qadb_kinematics_mean.hipo').toString())

    def rmsDirectory = new TDirectory()
    rmsDirectory.readFile(
        tempDir.resolve('qadb_kinematics_rms.hipo').toString())

    assertEquals(
        0,
        meanDirectory
            .getObject('/timelines/q2_mean')
            .getDataSize(0))

    assertEquals(
        0,
        rmsDirectory
            .getObject('/timelines/q2_rms')
            .getDataSize(0))
  }

  @Test
  void writeSkipsEmptyKinematicDistributions(
      @TempDir Path tempDir)
  {
    def directory = new TDirectory()

    /*
     * Histograms exist, but no selected electron has filled them.
     */
    def emptyHistograms = new Kinematics(1)
    emptyHistograms.write(directory)

    def qa_map = [
      42: [new TestQaBin(binNum: 1)]
    ]

    def analysis = new qadb_kinematics()
    analysis.processRun(directory, 42, qa_map)
    analysis.write(qa_map, tempDir.toString())

    def meanDirectory = new TDirectory()
    meanDirectory.readFile(
        tempDir.resolve('qadb_kinematics_mean.hipo').toString())

    def rmsDirectory = new TDirectory()
    rmsDirectory.readFile(
        tempDir.resolve('qadb_kinematics_rms.hipo').toString())

    ['q2', 'x', 'w', 'y'].each { variable ->
      def meanGraph = meanDirectory.getObject(
          "/timelines/${variable}_mean")

      def rmsGraph = rmsDirectory.getObject(
          "/timelines/${variable}_rms")

      assertEquals(0, meanGraph.getDataSize(0))
      assertEquals(0, rmsGraph.getDataSize(0))
    }
  }
}
