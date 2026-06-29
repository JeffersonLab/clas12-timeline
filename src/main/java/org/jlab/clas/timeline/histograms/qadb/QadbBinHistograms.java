package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;

/**
 * @author dilks
 */
public class QadbBinHistograms {

  // class instances
  private Electron electron;
  private Yield yield;

  // ----------------------------------------------------------------------------------

  /**
   * constructor
   * @param bin_num QADB bin number
   **/
  public QadbBinHistograms(int bin_num)
  {
    electron = new Electron();
    yield = new Yield(bin_num);
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a single event
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    electron.processEvent(event);
    yield.processEvent(event, electron);
  }

  // ----------------------------------------------------------------------------------

  /**
   * write to a HIPO file
   * @param tdir the output {@code TDirectory}
   */
  public void write(TDirectory tdir)
  {
    yield.write(tdir);
  }

}
