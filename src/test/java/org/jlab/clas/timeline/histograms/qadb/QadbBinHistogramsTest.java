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
}
