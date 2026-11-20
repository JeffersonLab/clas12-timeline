package org.jlab.clas.timeline.histograms.qadb;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jlab.groot.data.TDirectory;
import org.junit.jupiter.api.Test;

public class QadbBinHistogramsTest {

  @Test
  public void writeIncludesInclusiveKinematicsHistograms()
  {
    QadbBinHistograms histograms =
        new QadbBinHistograms(2, 6.4);

    TDirectory directory = new TDirectory();
    histograms.write(directory);

    assertNotNull(directory.getObject(
        "/QADB/inclusive_kinematics/q2_hist_qa2"));
    assertNotNull(directory.getObject(
        "/QADB/inclusive_kinematics/x_hist_qa2"));
    assertNotNull(directory.getObject(
        "/QADB/inclusive_kinematics/w_hist_qa2"));
    assertNotNull(directory.getObject(
        "/QADB/inclusive_kinematics/y_hist_qa2"));
  }

  @Test
  public void writeIncludesPionKinematicsHistograms()
  {
    QadbBinHistograms histograms =
        new QadbBinHistograms(2, 6.4);

    TDirectory directory = new TDirectory();
    histograms.write(directory);

    for(String particle : new String[] {"pip", "pim"}) {
      for(String variable : new String[] {"p", "pT", "z", "theta", "phiH"}) {
        assertNotNull(directory.getObject(
            "/QADB/pion_kinematics/"
                + particle + "_" + variable + "_hist_qa2"));
      }
    }
  }
}
