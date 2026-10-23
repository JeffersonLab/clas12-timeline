package org.jlab.clas.timeline.histograms.qadb;

import java.util.Map;

import org.jlab.groot.data.Directory;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;

/**
 * Histograms for inclusive electron kinematics.
 */
public class Kinematics {

  /** TDirectory path */
  public static final String TDIR = "/QADB/kinematics";

  // histograms
  public H1F q2_hist;
  public H1F x_hist;
  public H1F w_hist;
  public H1F y_hist;

  /**
   * @param bin_num QADB bin number
   */
  public Kinematics(int bin_num)
  {
    String suffix = "_qa" + String.valueOf(bin_num);

    q2_hist = new H1F(
        "q2_hist" + suffix,
        "Q^2 [GeV^2]",
        "counts",
        100, 0.0, 12.0);

    x_hist = new H1F(
        "x_hist" + suffix,
        "Bjorken x",
        "counts",
        100, 0.0, 1.0);

    w_hist = new H1F(
        "w_hist" + suffix,
        "W [GeV]",
        "counts",
        100, 0.0, 6.0);

    y_hist = new H1F(
        "y_hist" + suffix,
        "y",
        "counts",
        100, 0.0, 1.0);
  }

  /**
   * Fill the kinematic histograms for one event.
   *
   * @param electron the selected scattered electron
   */
  public void processEvent(Electron electron)
  {
    if(!electron.found()) {
      return;
    }

    double q2 = electron.getQ2();
    double x = electron.getX();
    double w = electron.getW();
    double y = electron.getY();

    if(Double.isFinite(q2)) {
      q2_hist.fill(q2);
    }

    if(Double.isFinite(x)) {
      x_hist.fill(x);
    }

    if(Double.isFinite(w)) {
      w_hist.fill(w);
    }

    if(Double.isFinite(y)) {
      y_hist.fill(y);
    }
  }

  /**
   * Write the kinematic histograms to a HIPO directory.
   *
   * @param tdir output TDirectory
   */
  public void write(TDirectory tdir)
  {
    tdir.mkdir(TDIR);
    tdir.cd(TDIR);
    tdir.addDataSet(q2_hist, x_hist, w_hist, y_hist);
  }

  /**
   * Check whether all kinematic histograms exist.
   *
   * @param tdir input TDirectory
   * @param bin_num QADB bin number
   * @return true if all required histograms exist
   */
  public boolean hasHistograms(TDirectory tdir, int bin_num)
  {
    Directory<?> qadb_directory = (Directory<?>)
        tdir.getDirectoryMap().get("QADB");

    if(qadb_directory == null) {
      return false;
    }

    Directory<?> kinematics_directory = (Directory<?>)
        qadb_directory.getDirectoryMap().get("kinematics");

    if(kinematics_directory == null) {
      return false;
    }

    String suffix = "_qa" + String.valueOf(bin_num);
    Map<String, ?> objects = kinematics_directory.getObjectMap();

    return objects.containsKey("q2_hist" + suffix)
        && objects.containsKey("x_hist" + suffix)
        && objects.containsKey("w_hist" + suffix)
        && objects.containsKey("y_hist" + suffix);
  }

  /**
   * Read the kinematic histograms from a TDirectory.
   *
   * @param tdir input TDirectory
   * @param bin_num QADB bin number
   */
  public void readHistograms(TDirectory tdir, int bin_num)
  {
    String suffix = "_qa" + String.valueOf(bin_num);

    q2_hist = (H1F) tdir.getObject(
        TDIR + "/q2_hist" + suffix);

    x_hist = (H1F) tdir.getObject(
        TDIR + "/x_hist" + suffix);

    w_hist = (H1F) tdir.getObject(
        TDIR + "/w_hist" + suffix);

    y_hist = (H1F) tdir.getObject(
        TDIR + "/y_hist" + suffix);
  }
}
