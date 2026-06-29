package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.detector.qadb.QadbBin;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.data.H1F;

/**
 * @author dilks
 */
public class Charge {

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

  /**
   * fill DSC2 histogram
   * @param qa_bin the {@code QadbBin} for this bin
   */
  public void fillDSC2(QadbBin<QadbBinHistograms> qa_bin)
  {
    dsc2_hist.setBinContent(DSC2Type.gated_int.ordinal(),     qa_bin.getBeamChargeGated());
    dsc2_hist.setBinContent(DSC2Type.ungated_int.ordinal(),   qa_bin.getBeamCharge());
    dsc2_hist.setBinContent(DSC2Type.mean_livetime.ordinal(), qa_bin.getMeanLivetime());
    // FIXME:
    // switch(FCmode) {
    //   case FCmodeEnum.NONE -> {}
    //   case FCmodeEnum.BY_FLIP          -> {qaBins.each{it.correctCharge(QadbBin.ChargeCorrectionMethod.BY_FLIP)}}
    //   case FCmodeEnum.BY_MEAN_LIVETIME -> {qaBins.each{it.correctCharge(QadbBin.ChargeCorrectionMethod.BY_MEAN_LIVETIME)}}
    //   case FCmodeEnum.CUSTOM -> {
    //     if(qaBins.size() != 3) {
    //       // FIXME: first and last bins get 'chargeUnknown', middle bin gets the real charge; this is just a kluge for RG-D...
    //       // for now we throw an exception for other use attempts
    //       throw new RuntimeException("we have not yet supported CUSTOM charge correction with number of QA bins != 3")
    //     }
    //     def chg = getDataFromJSON(runnum,"fc")
    //     qaBins.getBin(0).correctCharge(0.0, 0.0);
    //     qaBins.getBin(1).correctCharge(chg, chg); // set gated = ungated, for now...
    //     qaBins.getBin(2).correctCharge(0.0, 0.0);
    //   }
    // }
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a single event
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    // fill STRUCK histogram
    if(event.hasBank("HEL::scaler")) {
      var hel_bank = event.getBank("HEL::scaler");
      for(int row = 0; row < hel_bank.rows(); row++) {
        switch(hel_bank.getByte("helicity", row) {
          case -1 -> struck_hist.incrementBinContent(STRUCKType.gated_hel_n.ordinal(), hel_bank.getFloat("fcupgated", row));
          case  0 -> struck_hist.incrementBinContent(STRUCKType.gated_hel_0.ordinal(), hel_bank.getFloat("fcupgated", row));
          case  1 -> struck_hist.incrementBinContent(STRUCKType.gated_hel_p.ordinal(), hel_bank.getFloat("fcupgated", row));
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
    tdir.mkdir("/QADB/charge/");
    tdir.cd("/QADB/charge/");
    tdir.addDataSet(dsc2_hist);
    tdir.addDataSet(struck_hist);
  }

}
