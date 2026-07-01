package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.groot.data.TDirectory;
import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataEvent;

/**
 * @author dilks
 */
public class Yield {

  /** {@code TDirectory} path */
  public static final String TDIR = "/QADB/yield";

  /** channels to monitor */
  public enum Channel {
    /** trigger electrons in the Forward Detector (FD) sector 1 */
    electronFD_sec1,
    /** trigger electrons in the Forward Detector (FD) sector 2 */
    electronFD_sec2,
    /** trigger electrons in the Forward Detector (FD) sector 3 */
    electronFD_sec3,
    /** trigger electrons in the Forward Detector (FD) sector 4 */
    electronFD_sec4,
    /** trigger electrons in the Forward Detector (FD) sector 5 */
    electronFD_sec5,
    /** trigger electrons in the Forward Detector (FD) sector 6 */
    electronFD_sec6,
    /** trigger electrons in the Forward Tagger (FT) */
    electronFT,
  }

  // private members
  private H1F yield_hist;

  // ----------------------------------------------------------------------------------

  /**
   * constructor
   * @param bin_num QADB bin number
   **/
  public Yield(int bin_num)
  {
    // make a histogram with one bin per `Channel`
    yield_hist = new H1F(
        "yield_hist_qa" + String.valueOf(bin_num),
        "channel",
        "yield",
        Channel.values().length,
        0,
        Channel.values().length);
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a single event
   * @param event the HIPO event object
   * @param ele the scattered electron ({@code Electron})
   */
  public void processEvent(DataEvent event, Electron ele)
  {
    // electron yield
    if(ele.found()) {
      switch(ele.getDetector()) {
        case Electron.Det.none -> {}
        case Electron.Det.FD -> {
          switch(ele.getSector()) {
            case 1 -> yield_hist.incrementBinContent(Channel.electronFD_sec1.ordinal());
            case 2 -> yield_hist.incrementBinContent(Channel.electronFD_sec2.ordinal());
            case 3 -> yield_hist.incrementBinContent(Channel.electronFD_sec3.ordinal());
            case 4 -> yield_hist.incrementBinContent(Channel.electronFD_sec4.ordinal());
            case 5 -> yield_hist.incrementBinContent(Channel.electronFD_sec5.ordinal());
            case 6 -> yield_hist.incrementBinContent(Channel.electronFD_sec6.ordinal());
          }
        }
        case Electron.Det.FT -> yield_hist.incrementBinContent(Channel.electronFT.ordinal());
      }
    }
  }

  // ----------------------------------------------------------------------------------

  /**
   * write to a HIPO file
   * @param tdir the output {@code TDirectory}
   */
  public void write(TDirectory tdir)
  {
    tdir.mkdir(TDIR);
    tdir.cd(TDIR);
    tdir.addDataSet(yield_hist);
  }

}
