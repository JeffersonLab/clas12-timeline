package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.detector.qadb.QadbBin;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataEvent;
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
  private H1F dsc2_charge_hist;
  private H1F struck_charge_hist;
  private H1F struck_clock_hist;
  private H1F struck_numreadouts_hist;

  // ----------------------------------------------------------------------------------

  /**
   * constructor
   * @param bin_num QADB bin number
   **/
  public Charge(int bin_num)
  {
    // these are histograms, with one bin for each `enum` value; see the `enum`'s definitions for details
    dsc2_charge_hist = new H1F(
        "dsc2_charge_hist" + "_qa" + String.valueOf(bin_num),
        "enum DSC2Type",
        "charge [nC]",
        DSC2Type.values().length,
        0,
        DSC2Type.values().length);
    struck_charge_hist = new H1F(
        "struck_charge_hist" + "_qa" + String.valueOf(bin_num),
        "enum STRUCKType",
        "charge [nC]",
        STRUCKType.values().length,
        0,
        STRUCKType.values().length);
    struck_clock_hist = new H1F(
        "struck_clock_hist" + "_qa" + String.valueOf(bin_num),
        "enum STRUCKType",
        "clock",
        STRUCKType.values().length,
        0,
        STRUCKType.values().length);
    struck_numreadouts_hist = new H1F(
        "struck_numreadouts_hist" + "_qa" + String.valueOf(bin_num),
        "helicity",
        "num readouts",
        3,
        -1,
        1);
  }

  // ----------------------------------------------------------------------------------
  // accessors

  /** @return DAQ-gated DSC2 integrated charge */
  public double getChargeGatedDSC2()
  {
    return dsc2_charge_hist.getBinContent(DSC2Type.gated_int.ordinal());
  }

  /** @return DAQ-ungated DSC2 integrated charge */
  public double getChargeUngatedDSC2()
  {
    return dsc2_charge_hist.getBinContent(DSC2Type.ungated_int.ordinal());
  }

  /**
   * @param helicity the helicity
   * @return DAQ-gated STRUCK charge sum, latched to a given helicity
   */
  public double getChargeGatedSTRUCK(int helicity)
  {
    return switch(helicity) {
      case -1 -> struck_charge_hist.getBinContent(STRUCKType.gated_hel_n.ordinal());
      case  0 -> struck_charge_hist.getBinContent(STRUCKType.gated_hel_0.ordinal());
      case  1 -> struck_charge_hist.getBinContent(STRUCKType.gated_hel_p.ordinal());
      default -> throw new IllegalArgumentException("bad helicity in `getChargeGatedSTRUCK` call");
    };
  }

  /**
   * @param helicity the helicity
   * @return DAQ-ungated STRUCK charge sum, latched to a given helicity
   */
  public double getChargeUngatedSTRUCK(int helicity)
  {
    return switch(helicity) {
      case -1 -> struck_charge_hist.getBinContent(STRUCKType.ungated_hel_n.ordinal());
      case  0 -> struck_charge_hist.getBinContent(STRUCKType.ungated_hel_0.ordinal());
      case  1 -> struck_charge_hist.getBinContent(STRUCKType.ungated_hel_p.ordinal());
      default -> throw new IllegalArgumentException("bad helicity in `getChargeUngatedSTRUCK` call");
    };
  }

  /**
   * @param helicity the helicity
   * @return DAQ-gated STRUCK clock sum, latched to a given helicity
   */
  public double getClockGatedSTRUCK(int helicity)
  {
    return switch(helicity) {
      case -1 -> struck_clock_hist.getBinContent(STRUCKType.gated_hel_n.ordinal());
      case  0 -> struck_clock_hist.getBinContent(STRUCKType.gated_hel_0.ordinal());
      case  1 -> struck_clock_hist.getBinContent(STRUCKType.gated_hel_p.ordinal());
      default -> throw new IllegalArgumentException("bad helicity in `getClockGatedSTRUCK` call");
    };
  }

  /**
   * @param helicity the helicity
   * @return DAQ-ungated STRUCK clock sum, latched to a given helicity
   */
  public double getClockUngatedSTRUCK(int helicity)
  {
    return switch(helicity) {
      case -1 -> struck_clock_hist.getBinContent(STRUCKType.ungated_hel_n.ordinal());
      case  0 -> struck_clock_hist.getBinContent(STRUCKType.ungated_hel_0.ordinal());
      case  1 -> struck_clock_hist.getBinContent(STRUCKType.ungated_hel_p.ordinal());
      default -> throw new IllegalArgumentException("bad helicity in `getClockUngatedSTRUCK` call");
    };
  }

  /**
   * @param helicity the helicity
   * @return number of STRUCK scaler readouts for a given helicity
   */
  public double getNumReadoutsSTRUCK(int helicity)
  {
    var binnum = struck_numreadouts_hist.getXaxis().getBin(helicity);
    return struck_numreadouts_hist.getBinContent(binnum);
  }

  /** @return mean livetime */
  public double getMeanLivetime()
  {
    return dsc2_charge_hist.getBinContent(DSC2Type.mean_livetime.ordinal());
  }

  // ----------------------------------------------------------------------------------

  /**
   * fill DSC2 histogram, using charge already obtained from {@code QadbBinSequence}
   * @param qa_bin the {@code QadbBin} for this bin
   */
  public void fillDSC2(QadbBin<QadbBinHistograms> qa_bin)
  {
    dsc2_charge_hist.setBinContent(DSC2Type.gated_int.ordinal(),     qa_bin.getBeamChargeGated());
    dsc2_charge_hist.setBinContent(DSC2Type.ungated_int.ordinal(),   qa_bin.getBeamCharge());
    dsc2_charge_hist.setBinContent(DSC2Type.mean_livetime.ordinal(), qa_bin.getMeanLivetime());
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a single event, filling STRUCK histograms
   * NOTE: in the source code, comments with the string 'CUT' indicate the cuts used for reading STRUCK scaler data
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    // get the tag
    int tag = ((HipoDataEvent) event).getHipoEvent().getEventTag();
    // CUT: read tag-1 events only, since other tag events with scaler banks are duplicates of tag-1 events
    if(tag != 1) return;
    // CUT: must have `HEL::scaler` bank
    if(event.hasBank("HEL::scaler")) {
      var hel_bank = event.getBank("HEL::scaler");
      // loop over all rows of the bank (pileup?)
      for(int row = 0; row < hel_bank.rows(); row++) {
        var fcup_gated    = hel_bank.getFloat("fcupgated",  row);
        var fcup_ungated  = hel_bank.getFloat("fcup",       row);
        var clock_gated   = hel_bank.getFloat("clockgated", row);
        var clock_ungated = hel_bank.getFloat("clock",      row);
        // CUT: avoid t-settle region
        if(clock_ungated < 1000) continue;
        // CUT: avoid rows with zero charge, which may be bogus; these are typically for `row>0`
        if(Math.abs(fcup_gated)<1e-6 || Math.abs(fcup_ungated)<1e-6) continue;
        // fill the STRUCK histograms
        var helicity = hel_bank.getByte("helicity", row);
        switch(helicity) {
          case -1 -> {
            struck_charge_hist.incrementBinContent( STRUCKType.gated_hel_n.ordinal(),   fcup_gated    );
            struck_charge_hist.incrementBinContent( STRUCKType.ungated_hel_n.ordinal(), fcup_ungated  );
            struck_clock_hist.incrementBinContent(  STRUCKType.gated_hel_n.ordinal(),   clock_gated   );
            struck_clock_hist.incrementBinContent(  STRUCKType.ungated_hel_n.ordinal(), clock_ungated );
          }
          case 0 -> {
            struck_charge_hist.incrementBinContent( STRUCKType.gated_hel_0.ordinal(),   fcup_gated    );
            struck_charge_hist.incrementBinContent( STRUCKType.ungated_hel_0.ordinal(), fcup_ungated  );
            struck_clock_hist.incrementBinContent(  STRUCKType.gated_hel_0.ordinal(),   clock_gated   );
            struck_clock_hist.incrementBinContent(  STRUCKType.ungated_hel_0.ordinal(), clock_ungated );
          }
          case 1 -> {
            struck_charge_hist.incrementBinContent( STRUCKType.gated_hel_p.ordinal(),   fcup_gated    );
            struck_charge_hist.incrementBinContent( STRUCKType.ungated_hel_p.ordinal(), fcup_ungated  );
            struck_clock_hist.incrementBinContent(  STRUCKType.gated_hel_p.ordinal(),   clock_gated   );
            struck_clock_hist.incrementBinContent(  STRUCKType.ungated_hel_p.ordinal(), clock_ungated );
          }
        }
        struck_numreadouts_hist.fill(helicity);
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
    tdir.addDataSet(dsc2_charge_hist);
    tdir.addDataSet(struck_charge_hist);
    tdir.addDataSet(struck_clock_hist);
    tdir.addDataSet(struck_numreadouts_hist);
  }

  // ----------------------------------------------------------------------------------

  /** read histograms from a {@code TDirectory}
   * @param tdir the {@code TDirectory}
   * @param bin_num QADB bin number
   */
  void readHistograms(TDirectory tdir, int bin_num)
  {
    dsc2_charge_hist        = (H1F) tdir.getObject(TDIR + "/dsc2_charge_hist"        + "_qa" + String.valueOf(bin_num));
    struck_charge_hist      = (H1F) tdir.getObject(TDIR + "/struck_charge_hist"      + "_qa" + String.valueOf(bin_num));
    struck_clock_hist       = (H1F) tdir.getObject(TDIR + "/struck_clock_hist"       + "_qa" + String.valueOf(bin_num));
    struck_numreadouts_hist = (H1F) tdir.getObject(TDIR + "/struck_numreadouts_hist" + "_qa" + String.valueOf(bin_num));
  }

  // ----------------------------------------------------------------------------------

  /** replace the DAQ-gated charge with DAQ-ungated charge times mean livetime */
  void correctChargeByLivetime()
  {
    var corrected_charge = getChargeUngatedDSC2() * getMeanLivetime();
    dsc2_charge_hist.setBinContent(DSC2Type.gated_int.ordinal(), corrected_charge);
  }

  // ----------------------------------------------------------------------------------

  /** swap DAQ-gated and DAQ-ungated charge */
  void correctChargeByFlipFlop()
  {
    var gated_charge   = getChargeGatedDSC2();
    var ungated_charge = getChargeUngatedDSC2();
    dsc2_charge_hist.setBinContent(DSC2Type.gated_int.ordinal(),   ungated_charge);
    dsc2_charge_hist.setBinContent(DSC2Type.ungated_int.ordinal(), gated_charge);
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
    dsc2_charge_hist.setBinContent(DSC2Type.gated_int.ordinal(),     gated_charge);
    dsc2_charge_hist.setBinContent(DSC2Type.ungated_int.ordinal(),   ungated_charge);
    dsc2_charge_hist.setBinContent(DSC2Type.mean_livetime.ordinal(), mean_livetime);
  }

  // ----------------------------------------------------------------------------------

  /**
   * convert default charge units (nC) to mC
   * @param q the input charge in (nC)
   * @return the charge in mC
   */
  static double to_mC(double q)
  {
    return q / 1e6;
  }

}
