package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;

/**
 * @author dilks
 */
public class QadbBinHistograms {

  // histogram class instances
  public Charge charge;
  private Electron electron;
  private Kinematics kinematics;
  private Yield yield;
  private Helicity helicity;

  // ----------------------------------------------------------------------------------

  /**
   * Construct QADB histograms using the historical default beam energy.
   *
   * @param bin_num QADB bin number
   * @deprecated pass the run-dependent beam energy explicitly
   */
  @Deprecated
  public QadbBinHistograms(int bin_num)
  {
    this(bin_num, Electron.DEFAULT_BEAM_ENERGY);
  }

  /**
   * Constructor.
   *
   * @param bin_num QADB bin number
   * @param beam_energy beam energy in GeV
   */
  public QadbBinHistograms(int bin_num, double beam_energy)
  {
    charge = new Charge(bin_num);
    electron = new Electron(beam_energy);
    kinematics = new Kinematics(bin_num);
    yield = new Yield(bin_num);
    helicity = new Helicity(bin_num);
  }

  // ----------------------------------------------------------------------------------

  /**
   * Process a single event.
   *
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    charge.processEvent(event);
    electron.processEvent(event);
    kinematics.processEvent(electron);
    yield.processEvent(event, electron);
    helicity.processEvent(event);
  }

  // ----------------------------------------------------------------------------------

  /**
   * Write to a HIPO file.
   *
   * @param tdir the output {@code TDirectory}
   */
  public void write(TDirectory tdir)
  {
    charge.write(tdir);
    yield.write(tdir);
    kinematics.write(tdir);
    helicity.write(tdir);
  }
}
