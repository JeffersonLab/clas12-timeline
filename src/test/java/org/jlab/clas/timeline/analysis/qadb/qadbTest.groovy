package org.jlab.clas.timeline.analysis.qadb

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

class qadbTest {

  private static class RecordingAnalysis {
    int processRunCalls = 0
    int writeCalls = 0

    def processRun(dir, runnum, qa_map)
    {
      processRunCalls++
    }

    def write(qa_map)
    {
      writeCalls++
    }
  }

  private static void setPrivateField(
      Object target,
      String fieldName,
      Object value)
  {
    def field = target.class.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.set(target, value)
  }

  @Test
  void delegatesProcessingAndWritingToInclusiveKinematicsAnalysis()
  {
    def coordinator = new qadb()

    def charge = new RecordingAnalysis()
    def yieldAnalysis = new RecordingAnalysis()
    def helicity = new RecordingAnalysis()
    def inclusiveKinematics = new RecordingAnalysis()

    setPrivateField(coordinator, 'ana_qadb_charge', charge)
    setPrivateField(coordinator, 'ana_qadb_yield', yieldAnalysis)
    setPrivateField(coordinator, 'ana_qadb_helicity', helicity)
    setPrivateField(
        coordinator,
        'ana_qadb_inclusive_kinematics',
        inclusiveKinematics)

    setPrivateField(coordinator, 'qa_allowed', true)
    setPrivateField(coordinator, 'qa_map', [42: []])

    coordinator.processRun(new Object(), 42)
    coordinator.write()

    assertEquals(1, inclusiveKinematics.processRunCalls)
    assertEquals(1, inclusiveKinematics.writeCalls)
  }
}
