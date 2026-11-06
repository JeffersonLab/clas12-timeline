package org.jlab.clas.timeline.histograms.qadb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.clas.physics.LorentzVector;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.Vector3;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

public class PionKinematicsTest {

  private record HistogramRange(double lower, double upper) {}

  private static class TestElectron extends Electron {

    private final boolean m_found;
    private final double m_q2;
    private final double m_w;
    private final double m_y;
    private final Particle m_particle;

    TestElectron(
        boolean found,
        double q2,
        double w,
        double y,
        Particle particle)
    {
      super(6.4);
      m_found = found;
      m_q2 = q2;
      m_w = w;
      m_y = y;
      m_particle = particle;
    }

    @Override
    public boolean found() { return m_found; }

    @Override
    public double getQ2() { return m_q2; }

    @Override
    public double getW() { return m_w; }

    @Override
    public double getY() { return m_y; }

    @Override
    public Particle getParticle() { return m_particle; }
  }

  private static Particle particleWithEnergy(
      int pid,
      double px,
      double py,
      double pz,
      double energy)
  {
    Particle particle = new Particle(pid, px, py, pz, 0.0, 0.0, 0.0);
    particle.setVector(
        new LorentzVector(px, py, pz, energy),
        new Vector3());
    return particle;
  }

  private static TestElectron acceptedElectron()
  {
    return new TestElectron(
        true,
        2.5,
        2.4,
        0.5,
        new Particle(11, 1.0, 0.0, 3.0, 0.0, 0.0, 0.0));
  }

  private static void assertAllEntries(
      PionKinematics kinematics,
      int pid,
      long expected)
  {
    for(String variable : new String[] {"p", "pT", "z", "theta", "phiH"}) {
      assertEquals(expected, kinematics.getHistogram(pid, variable).getEntries());
    }
  }

  private static DataEvent eventWithPionCandidates()
  {
    int[] pids = {211, -211, 321};
    float[][] momenta = {
      {0.3f, 0.4f, 1.2f},
      {0.3f, -0.4f, 1.2f},
      {0.3f, 0.4f, 1.2f}
    };

    DataBank bank = (DataBank) Proxy.newProxyInstance(
        DataBank.class.getClassLoader(),
        new Class<?>[] {DataBank.class},
        (proxy, method, arguments) -> switch(method.getName()) {
          case "rows" -> pids.length;
          case "getInt" -> pids[(int) arguments[1]];
          case "getFloat" -> {
            String column = (String) arguments[0];
            int row = (int) arguments[1];
            yield switch(column) {
              case "px" -> momenta[row][0];
              case "py" -> momenta[row][1];
              case "pz" -> momenta[row][2];
              case "vx", "vy", "vz" -> 0.0f;
              default -> throw new UnsupportedOperationException(column);
            };
          }
          default -> throw new UnsupportedOperationException(method.getName());
        });

    return (DataEvent) Proxy.newProxyInstance(
        DataEvent.class.getClassLoader(),
        new Class<?>[] {DataEvent.class},
        (proxy, method, arguments) -> switch(method.getName()) {
          case "hasBank" -> "REC::Particle".equals(arguments[0]);
          case "getBank" -> bank;
          default -> throw new UnsupportedOperationException(method.getName());
        });
  }

  @Test
  public void constructsLegacyHistogramSchema()
  {
    PionKinematics kinematics = new PionKinematics(3, 6.4);

    Map<String, HistogramRange> expected = Map.of(
        "p",     new HistogramRange(0.0, 10.0),
        "pT",    new HistogramRange(0.0, 4.0),
        "z",     new HistogramRange(0.0, 1.0),
        "theta", new HistogramRange(0.0, Math.PI / 2.0),
        "phiH",  new HistogramRange(-3.15, 3.15));

    for(int pid : new int[] {211, -211}) {
      for(var entry : expected.entrySet()) {
        H1F histogram = kinematics.getHistogram(pid, entry.getKey());

        assertNotNull(histogram);
        assertEquals(50, histogram.getAxis().getNBins());
        assertEquals(entry.getValue().lower(), histogram.getAxis().min(), 0.0);
        assertEquals(entry.getValue().upper(), histogram.getAxis().max(), 0.0);
      }
    }
  }

  @Test
  public void rejectsInvalidBeamEnergy()
  {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PionKinematics(0, 0.0));

    assertThrows(
        IllegalArgumentException.class,
        () -> new PionKinematics(0, -1.0));

    assertThrows(
        IllegalArgumentException.class,
        () -> new PionKinematics(0, Double.NaN));
  }

  @Test
  public void rejectsUnknownPidOrVariable()
  {
    PionKinematics kinematics = new PionKinematics(0, 6.4);

    assertThrows(
        IllegalArgumentException.class,
        () -> kinematics.getHistogram(321, "p"));

    assertThrows(
        IllegalArgumentException.class,
        () -> kinematics.getHistogram(211, "energy"));
  }

  @Test
  public void fillsKnownKinematicsForBothPionCharges()
  {
    PionKinematics kinematics = new PionKinematics(0, 6.4);

    kinematics.processParticle(
        new Particle(211, 0.3, 0.4, 1.2, 0.0, 0.0, 0.0),
        acceptedElectron());

    kinematics.processParticle(
        new Particle(-211, 0.3, -0.4, 1.2, 0.0, 0.0, 0.0),
        acceptedElectron());

    assertAllEntries(kinematics, 211, 1);
    assertAllEntries(kinematics, -211, 1);

    Map<String, Double> expectedPlus = Map.of(
        "p", 1.3,
        "pT", 0.5,
        "z", 0.4038242566,
        "theta", 0.3947911197,
        "phiH", 0.5682910885);

    Map<String, Double> expectedMinus = Map.of(
        "p", 1.3,
        "pT", 0.5,
        "z", 0.4038242566,
        "theta", 0.3947911197,
        "phiH", -0.5682910885);

    expectedPlus.forEach((variable, value) -> {
      H1F histogram = kinematics.getHistogram(211, variable);
      assertEquals(
          1.0,
          histogram.getBinContent(histogram.getAxis().getBin(value)),
          0.0);
    });

    expectedMinus.forEach((variable, value) -> {
      H1F histogram = kinematics.getHistogram(-211, variable);
      assertEquals(
          1.0,
          histogram.getBinContent(histogram.getAxis().getBin(value)),
          0.0);
    });
  }

  @Test
  public void countsMultipleAcceptedCandidatesIndependently()
  {
    PionKinematics kinematics = new PionKinematics(0, 6.4);
    Particle pion = new Particle(
        211, 0.3, 0.4, 1.2, 0.0, 0.0, 0.0);

    kinematics.processParticle(pion, acceptedElectron());
    kinematics.processParticle(pion, acceptedElectron());

    assertAllEntries(kinematics, 211, 2);
    assertAllEntries(kinematics, -211, 0);
  }

  @Test
  public void rejectsWrongPidAndMissingElectron()
  {
    PionKinematics kinematics = new PionKinematics(0, 6.4);

    kinematics.processParticle(
        new Particle(321, 0.3, 0.4, 1.2, 0.0, 0.0, 0.0),
        acceptedElectron());

    kinematics.processParticle(
        new Particle(211, 0.3, 0.4, 1.2, 0.0, 0.0, 0.0),
        new TestElectron(
            false,
            2.5,
            2.4,
            0.5,
            new Particle(11, 1.0, 0.0, 3.0, 0.0, 0.0, 0.0)));

    assertAllEntries(kinematics, 211, 0);
    assertAllEntries(kinematics, -211, 0);
  }

  @Test
  public void rejectsDisBoundaryAndFailedValues()
  {
    double[][] rejected = {
      {1.0, 2.4, 0.5},
      {0.9, 2.4, 0.5},
      {2.5, 2.0, 0.5},
      {2.5, 1.9, 0.5},
      {2.5, 2.4, 0.8},
      {2.5, 2.4, 0.9}
    };

    for(double[] dis : rejected) {
      PionKinematics kinematics = new PionKinematics(0, 6.4);
      TestElectron electron = new TestElectron(
          true,
          dis[0],
          dis[1],
          dis[2],
          new Particle(11, 1.0, 0.0, 3.0, 0.0, 0.0, 0.0));

      kinematics.processParticle(
          new Particle(211, 0.3, 0.4, 1.2, 0.0, 0.0, 0.0),
          electron);

      assertAllEntries(kinematics, 211, 0);
    }
  }

  @Test
  public void rejectsZBoundariesAndOutOfRangeValues()
  {
    Particle exactEnergyElectron = particleWithEnergy(
        11, 1.0, 0.0, 3.039736787763058, 3.2);
    TestElectron electron = new TestElectron(
        true, 2.5, 2.4, 0.5, exactEnergyElectron);

    Particle[] rejected = {
      particleWithEnergy(
          211,
          0.21918462343107617,
          0.29224616457476826,
          0.8767384937243047,
          0.96),
      particleWithEnergy(
          211,
          0.21690353360366913,
          0.28920471147155886,
          0.8676141344146765,
          0.95),
      particleWithEnergy(
          211,
          0.7377588040781191,
          0.9836784054374922,
          2.9510352163124765,
          3.2),
      particleWithEnergy(
          211,
          0.7400675611918355,
          0.9867567482557808,
          2.960270244767342,
          3.21)
    };

    for(Particle pion : rejected) {
      PionKinematics kinematics = new PionKinematics(0, 6.4);
      kinematics.processParticle(pion, electron);
      assertAllEntries(kinematics, 211, 0);
    }
  }

  @Test
  public void rejectsUndefinedOrNonFiniteKinematics()
  {
    PionKinematics undefinedAngle = new PionKinematics(0, 6.4);
    undefinedAngle.processParticle(
        new Particle(211, 0.3, 0.0, 1.2, 0.0, 0.0, 0.0),
        acceptedElectron());
    assertAllEntries(undefinedAngle, 211, 0);

    PionKinematics nonFinite = new PionKinematics(0, 6.4);
    nonFinite.processParticle(
        particleWithEnergy(211, 0.3, 0.4, 1.2, Double.NaN),
        acceptedElectron());
    assertAllEntries(nonFinite, 211, 0);
  }

  @Test
  public void eventWithoutParticleBankLeavesHistogramsEmpty()
  {
    DataEvent event = (DataEvent) Proxy.newProxyInstance(
        DataEvent.class.getClassLoader(),
        new Class<?>[] {DataEvent.class},
        (proxy, method, arguments) -> {
          if(method.getName().equals("hasBank")) {
            return false;
          }
          throw new UnsupportedOperationException(method.getName());
        });

    PionKinematics kinematics = new PionKinematics(0, 6.4);
    kinematics.processEvent(event, acceptedElectron());

    assertAllEntries(kinematics, 211, 0);
    assertAllEntries(kinematics, -211, 0);
  }

  @Test
  public void processEventReadsBothPionChargesFromParticleBank()
  {
    PionKinematics kinematics = new PionKinematics(0, 6.4);

    kinematics.processEvent(eventWithPionCandidates(), acceptedElectron());

    assertAllEntries(kinematics, 211, 1);
    assertAllEntries(kinematics, -211, 1);
  }

  @Test
  public void writeStoresAllPionHistograms()
  {
    PionKinematics kinematics = new PionKinematics(3, 6.4);
    TDirectory directory = new TDirectory();

    kinematics.write(directory);

    for(String particle : new String[] {"pip", "pim"}) {
      for(String variable : new String[] {"p", "pT", "z", "theta", "phiH"}) {
        assertNotNull(directory.getObject(
            "/QADB/pion_kinematics/"
                + particle + "_" + variable + "_hist_qa3"));
      }
    }
  }

  @Test
  public void readHistogramsRestoresAllPionHistograms()
  {
    PionKinematics written = new PionKinematics(4, 6.4);
    written.processParticle(
        new Particle(211, 0.3, 0.4, 1.2, 0.0, 0.0, 0.0),
        acceptedElectron());
    written.processParticle(
        new Particle(-211, 0.3, -0.4, 1.2, 0.0, 0.0, 0.0),
        acceptedElectron());

    TDirectory directory = new TDirectory();
    written.write(directory);

    PionKinematics read = new PionKinematics(4, 6.4);
    assertTrue(read.readHistograms(directory, 4));
    assertAllEntries(read, 211, 1);
    assertAllEntries(read, -211, 1);
  }

  @Test
  public void readHistogramsReturnsFalseForMissingDirectory()
  {
    PionKinematics kinematics = new PionKinematics(4, 6.4);

    assertFalse(kinematics.readHistograms(new TDirectory(), 4));
  }

  @Test
  public void readHistogramsReturnsFalseForPartialSchema()
  {
    TDirectory directory = new TDirectory();
    directory.mkdir(PionKinematics.TDIR);
    directory.cd(PionKinematics.TDIR);
    directory.addDataSet(new H1F(
        "pip_p_hist_qa4", "p [GeV]", "counts", 50, 0.0, 10.0));

    PionKinematics kinematics = new PionKinematics(4, 6.4);

    assertFalse(kinematics.readHistograms(directory, 4));
  }
}
