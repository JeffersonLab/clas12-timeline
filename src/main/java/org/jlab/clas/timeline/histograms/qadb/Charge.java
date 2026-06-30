package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.detector.qadb.QadbBin;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.data.H1F;

/**
 * @author dilks
 */
public class Charge {

  /** {@code TDirectory} path */
  public static final String TDIR = "/QADB/charge";

  /** charge types from DSC2 scaler */
  public enum DSC2Type {
    /** DAQ-gated DSC2 integrated charge */
    gated_int,
    /** DAQ-ungated DSC2 integrated charge */
    ungated_int,
    /** mean livetime */
    mean_livetime,
  }

  /** charge types from STRUCK scaler */
  public enum STRUCKType {
    /** DAQ-gated STRUCK charge sum in QA bin, latched to helicity = -1 */
    gated_hel_n,
    /** DAQ-gated STRUCK charge sum in QA bin, latched to helicity = 0 */
    gated_hel_0,
    /** DAQ-gated STRUCK charge sum in QA bin, latched to helicity = +1 */
    gated_hel_p,
    /** DAQ-ungated STRUCK charge sum in QA bin, latched to helicity = -1 */
    ungated_hel_n,
    /** DAQ-ungated STRUCK charge sum in QA bin, latched to helicity = 0 */
    ungated_hel_0,
    /** DAQ-ungated STRUCK charge sum in QA bin, latched to helicity = +1 */
    ungated_hel_p,
  }

  // private members
  private H1F dsc2_hist;
  private H1F struck_hist;

  // ----------------------------------------------------------------------------------

  /**
   * constructor
   * @param bin_num QADB bin number
   **/
  public Charge(int bin_num)
  {
    dsc2_hist = new H1F(
        "dsc2_hist_qa" + String.valueOf(bin_num),
        "charge type",
        "charge",
        DSC2Type.values().length,
        0,
        DSC2Type.values().length);
    struck_hist = new H1F(
        "struck_hist_qa" + String.valueOf(bin_num),
        "charge type",
        "charge",
        STRUCKType.values().length,
        0,
        STRUCKType.values().length);
  }

  // ----------------------------------------------------------------------------------
  // accessors

  /** @return DAQ-gated DSC2 integrated charge */
  public double getChargeGatedDSC2()
  {
    return dsc2_hist.getBinContent(DSC2Type.gated_int.ordinal());
  }

  /** @return DAQ-ungated DSC2 integrated charge */
  public double getChargeUngatedDSC2()
  {
    return dsc2_hist.getBinContent(DSC2Type.ungated_int.ordinal());
  }

  /**
   * @param helicity the helicity
   * @return DAQ-gated STRUCK charge sum latched to a given helicity
   */
  public double getChargeGatedSTRUCK(int helicity)
  {
    return switch(helicity) {
      case -1 -> struck_hist.getBinContent(STRUCKType.gated_hel_n.ordinal());
      case  0 -> struck_hist.getBinContent(STRUCKType.gated_hel_0.ordinal());
      case  1 -> struck_hist.getBinContent(STRUCKType.gated_hel_p.ordinal());
      default -> throw new IllegalArgumentException("bad helicity in `getChargeGatedSTRUCK` call");
    };
  }

  /**
   * @param helicity the helicity
   * @return DAQ-ungated STRUCK charge sum latched to a given helicity
   */
  public double getChargeUngatedSTRUCK(int helicity)
  {
    return switch(helicity) {
      case -1 -> struck_hist.getBinContent(STRUCKType.ungated_hel_n.ordinal());
      case  0 -> struck_hist.getBinContent(STRUCKType.ungated_hel_0.ordinal());
      case  1 -> struck_hist.getBinContent(STRUCKType.ungated_hel_p.ordinal());
      default -> throw new IllegalArgumentException("bad helicity in `getChargeUngatedSTRUCK` call");
    };
  }

  /** @return mean livetime */
  public double getMeanLivetime()
  {
    return dsc2_hist.getBinContent(DSC2Type.ungated_int.ordinal());
  }

  // ----------------------------------------------------------------------------------

  /**
   * fill DSC2 histogram, using charge already obtained from {@code QadbBinSequence}
   * @param qa_bin the {@code QadbBin} for this bin
   */
  public void fillDSC2(QadbBin<QadbBinHistograms> qa_bin)
  {
    dsc2_hist.setBinContent(DSC2Type.gated_int.ordinal(),     qa_bin.getBeamChargeGated());
    dsc2_hist.setBinContent(DSC2Type.ungated_int.ordinal(),   qa_bin.getBeamCharge());
    dsc2_hist.setBinContent(DSC2Type.mean_livetime.ordinal(), qa_bin.getMeanLivetime());
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a single event, filling STRUCK histogram
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    // fill STRUCK histogram
    if(event.hasBank("HEL::scaler")) {
      var hel_bank = event.getBank("HEL::scaler");
      for(int row = 0; row < hel_bank.rows(); row++) {
        switch(hel_bank.getByte("helicity", row)) {
          case -1 -> {
            struck_hist.incrementBinContent(STRUCKType.gated_hel_n.ordinal(),   hel_bank.getFloat("fcupgated", row));
            struck_hist.incrementBinContent(STRUCKType.ungated_hel_n.ordinal(), hel_bank.getFloat("fcup",      row));
          }
          case 0 -> {
            struck_hist.incrementBinContent(STRUCKType.gated_hel_0.ordinal(),   hel_bank.getFloat("fcupgated", row));
            struck_hist.incrementBinContent(STRUCKType.ungated_hel_0.ordinal(), hel_bank.getFloat("fcup",      row));
          }
          case 1 -> {
            struck_hist.incrementBinContent(STRUCKType.gated_hel_p.ordinal(),   hel_bank.getFloat("fcupgated", row));
            struck_hist.incrementBinContent(STRUCKType.ungated_hel_p.ordinal(), hel_bank.getFloat("fcup",      row));
          }
        }
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
    tdir.addDataSet(dsc2_hist);
    tdir.addDataSet(struck_hist);
  }

  // ----------------------------------------------------------------------------------

  /** read histograms from a {@code TDirectory}
   * @param tdir the {@code TDirectory}
   * @param bin_num QADB bin number
   */
  void readHistograms(TDirectory tdir, int bin_num)
  {
    dsc2_hist   = (H1F) tdir.getObject(TDIR + "/dsc2_hist_qa"   + String.valueOf(bin_num));
    struck_hist = (H1F) tdir.getObject(TDIR + "/struck_hist_qa" + String.valueOf(bin_num));
  }

  // ----------------------------------------------------------------------------------

  /** replace the DAQ-gated charge with DAQ-ungated charge times mean livetime */
  void correctChargeByLivetime()
  {
    var corrected_charge = getChargeUngatedDSC2() * getMeanLivetime();
    dsc2_hist.setBinContent(DSC2Type.gated_int.ordinal(), corrected_charge);
  }

  // ----------------------------------------------------------------------------------

  /** swap DAQ-gated and DAQ-ungated charge */
  void correctChargeByFlipFlop()
  {
    var gated_charge   = getChargeGatedDSC2();
    var ungated_charge = getChargeUngatedDSC2();
    dsc2_hist.setBinContent(DSC2Type.gated_int.ordinal(),   ungated_charge);
    dsc2_hist.setBinContent(DSC2Type.ungated_int.ordinal(), gated_charge);
  }

  // ----------------------------------------------------------------------------------

  /**
   * directly set the charge and mean livetime
   * @param gated_charge the DAQ-gated charge
   * @param ungated_charge the DAQ-ungated charge
   * @param mean_livetime the mean livetime
   */
  void setCustomCharge(double gated_charge, double ungated_charge, double mean_livetime)
  {
    dsc2_hist.setBinContent(DSC2Type.gated_int.ordinal(),     gated_charge);
    dsc2_hist.setBinContent(DSC2Type.ungated_int.ordinal(),   ungated_charge);
    dsc2_hist.setBinContent(DSC2Type.mean_livetime.ordinal(), mean_livetime);
  }

}
