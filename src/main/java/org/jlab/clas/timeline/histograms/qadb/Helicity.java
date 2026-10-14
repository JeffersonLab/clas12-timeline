package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.groot.data.TDirectory;
import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;

/**
 * @author dilks
 */
public class Helicity {

  // public members
  public H1F helicity_hist;

  /**
   * constructor
   * @param bin_num QADB bin number
   **/
  public Helicity(int bin_num)
  {
    // make a histogram with bin centers at -1, 0, and 1
    helicity_hist = new H1F("helicity_hist_qa" + String.valueOf(bin_num), "helicity", "count", 3, -1.5, 1.5);
  }

  /**
   * process a single event
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    if(event.hasBank("REC::Event")) {
      DataBank rec_event_bank = event.getBank("REC::Event");
      if(rec_event_bank.rows() > 0) {
        helicity_hist.fill(rec_event_bank.getByte("helicity", 0));
      }
    }
  }

  /**
   * write to a HIPO file
   * @param tdir the output {@code TDirectory}
   */
  public void write(TDirectory tdir)
  {
    tdir.mkdir("/QADB/helicity/");
    tdir.cd("/QADB/helicity/");
    tdir.addDataSet(helicity_hist);
  }
  // ----------------------------------------------------------------------------------

  /**
   * Read the helicity histogram from a TDirectory.
   * @param tdir the TDirectory
   * @param bin_num QADB bin number
   */
  public void readHistograms(TDirectory tdir, int bin_num)
  {
    helicity_hist = (H1F) tdir.getObject(
        "/QADB/helicity/helicity_hist_qa" + String.valueOf(bin_num));
  }
}
