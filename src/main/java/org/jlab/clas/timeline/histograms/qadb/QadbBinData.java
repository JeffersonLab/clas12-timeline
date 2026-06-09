package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.io.base.DataEvent;

/**
 * @author dilks
 */
public class QadbBinData {

  // histogram class instances
  private Yield yield;

  // ----------------------------------------------------------------------------------

  /** constructor */
  public QadbBinData()
  {
    yield = new Yield();
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
   * @param output_dir the output directory
   * @param run_num the run number
   */
  public void write(String output_dir, int run_num)
  {
    yield.write(output_dir, run_num);
  }

}
