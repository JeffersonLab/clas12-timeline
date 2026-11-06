package org.jlab.clas.timeline.histograms.qadb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jlab.groot.data.TDirectory;
import org.junit.jupiter.api.Test;

public class InclusiveKinematicsTest {

  private static class TestElectron extends Electron {

    TestElectron()
    {
      super(6.4);
    }

    @Override
    public boolean found() { return true; }

    @Override
    public double getQ2() { return 3.05; }

    @Override
    public double getX() { return 0.35; }

    @Override
    public double getW() { return 2.50; }

    @Override
    public double getY() { return 0.45; }
  }

  @Test
  public void processEventFillsAllKinematicHistograms()
  {
    InclusiveKinematics kinematics = new InclusiveKinematics(0);
    kinematics.processEvent(new TestElectron());

    assertEquals(1, kinematics.q2_hist.getEntries());
    assertEquals(1, kinematics.x_hist.getEntries());
    assertEquals(1, kinematics.w_hist.getEntries());
    assertEquals(1, kinematics.y_hist.getEntries());

    assertEquals(
        1.0,
        kinematics.q2_hist.getBinContent(
            kinematics.q2_hist.getAxis().getBin(3.05)),
        0.0);

    assertEquals(
        1.0,
        kinematics.x_hist.getBinContent(
            kinematics.x_hist.getAxis().getBin(0.35)),
        0.0);

    assertEquals(
        1.0,
        kinematics.w_hist.getBinContent(
            kinematics.w_hist.getAxis().getBin(2.50)),
        0.0);

    assertEquals(
        1.0,
        kinematics.y_hist.getBinContent(
            kinematics.y_hist.getAxis().getBin(0.45)),
        0.0);
  }

  @Test
  public void writeStoresAllHistograms()
  {
    InclusiveKinematics kinematics = new InclusiveKinematics(3);
    TDirectory directory = new TDirectory();

    kinematics.write(directory);

    assertNotNull(directory.getObject(
        "/QADB/inclusive_kinematics/q2_hist_qa3"));
    assertNotNull(directory.getObject(
        "/QADB/inclusive_kinematics/x_hist_qa3"));
    assertNotNull(directory.getObject(
        "/QADB/inclusive_kinematics/w_hist_qa3"));
    assertNotNull(directory.getObject(
        "/QADB/inclusive_kinematics/y_hist_qa3"));
  }

  @Test
  public void readHistogramsRestoresStoredHistograms()
  {
    InclusiveKinematics written = new InclusiveKinematics(4);
    written.processEvent(new TestElectron());

    TDirectory directory = new TDirectory();
    written.write(directory);

    InclusiveKinematics read = new InclusiveKinematics(4);
    assertTrue(read.readHistograms(directory, 4));

    assertEquals(1, read.q2_hist.getEntries());
    assertEquals(1, read.x_hist.getEntries());
    assertEquals(1, read.w_hist.getEntries());
    assertEquals(1, read.y_hist.getEntries());
  }

  @Test
  public void readHistogramsReturnsFalseWhenDirectoryIsMissing()
  {
    InclusiveKinematics kinematics = new InclusiveKinematics(4);

    assertFalse(kinematics.readHistograms(new TDirectory(), 4));
  }

  @Test
  public void processEventSkipsNonFiniteValues()
  {
    Electron electron = new Electron(6.4) {
      @Override
      public boolean found() { return true; }

      @Override
      public double getQ2() { return 2.0; }

      @Override
      public double getX() { return Double.NaN; }

      @Override
      public double getW() { return Double.POSITIVE_INFINITY; }

      @Override
      public double getY() { return Double.NEGATIVE_INFINITY; }
    };

    InclusiveKinematics kinematics = new InclusiveKinematics(0);
    kinematics.processEvent(electron);

    /*
     * The valid quantity is filled independently, while invalid
     * quantities must not affect entries or underflow/overflow.
     */
    assertEquals(1, kinematics.q2_hist.getEntries());
    assertEquals(0, kinematics.x_hist.getEntries());
    assertEquals(0, kinematics.w_hist.getEntries());
    assertEquals(0, kinematics.y_hist.getEntries());
  }
}
