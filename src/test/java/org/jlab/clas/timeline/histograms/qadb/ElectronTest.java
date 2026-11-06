package org.jlab.clas.timeline.histograms.qadb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.jlab.clas.physics.Particle;

public class ElectronTest {

  @Test
  public void calculatesInclusiveKinematics()
  {
    Electron electron = new Electron(6.4);

    /*
     * A 4 GeV electron scattered by 20 degrees.
     */
    Particle scatteredElectron = new Particle(
        Electron.PDG,
        1.3680805621391403,
        0.0,
        3.758770452472075,
        0.0,
        0.0,
        0.0);

    electron.calculateKinematics(scatteredElectron);

    assertEquals(3.0877379472, electron.getQ2(), 1.0e-6);
    assertEquals(0.6855993606, electron.getX(), 1.0e-6);
    assertEquals(1.5153621816, electron.getW(), 1.0e-6);
    assertEquals(0.3750000000, electron.getY(), 1.0e-6);
  }

}
