package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;

/**
 * @author dilks
 */
public class QadbBinData {

  // histogram class instances
  private Yield yield;

  // ----------------------------------------------------------------------------------

  /**
   * constructor
   * @param bin_num QADB bin number
   **/
  public QadbBinData(int bin_num)
  {
    yield = new Yield(bin_num);
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a single event
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    yield.processEvent(event);
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
