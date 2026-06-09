package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.groot.data.TDirectory;
import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import org.jlab.clas.physics.Particle;
import org.jlab.detector.base.DetectorType;

import java.util.OptionalInt;

/**
 * @author dilks
 */
public class Yield {

  /** channels to monitor */
  public enum Channel {
    /** trigger electrons in the Forward Detector (FD) */
    electronFD,
    /** trigger electrons in the Foward Tagger (FT) */
    electronFT,
  }

  // private members
  private H1F         yield_hist;
  private OptionalInt ele_pindex = OptionalInt.empty();

  // constants
  private static final int ELE_PDG = 11;
  private static final int ECAL_ID = DetectorType.ECAL.getDetectorId();

  // ----------------------------------------------------------------------------------

  /** constructor */
  public Yield()
  {
    // make a histogram with one bin per `Channel`
    yield_hist = new H1F("yield_hist", "channel", "yield", Channel.values().length, 0, Channel.values().length);
  }

  // ----------------------------------------------------------------------------------

  /**
   * @return the electron {@code pindex}, if found
   */
  public OptionalInt getElectronPIndex()
  {
    return ele_pindex;
  }

  // ----------------------------------------------------------------------------------

  /**
   * create a {@code Particle} object given a bank row
   * @param bank the HIPO bank
   * @param row the HIPO bank row
   * @param pid the PID value (could be gotten from the bank row, but you probably already have it)
   */
  private Particle getParticle(DataBank bank, int row, int pid)
  {
    return new Particle(
        pid,
        bank.getFloat("px", row),
        bank.getFloat("py", row),
        bank.getFloat("pz", row));
  }

  // ----------------------------------------------------------------------------------

  /**
   * process a single event
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    enum det { none, FD, FT };

    // reset class members
    ele_pindex = OptionalInt.empty();

    // handle trigger electron
    Particle ele_par = new Particle(ELE_PDG, 0.0, 0.0, 0.0);
    int      ele_sec = 0;
    det      ele_det = det.none;
    if(event.hasBank("REC::Particle") && event.hasBank("REC::Calorimeter")) {
      var par_bank = event.getBank("REC::Particle");
      var cal_bank = event.getBank("REC::Calorimeter");
      // loop over electrons from `REC::Particle`
      for(int par_row = 0; par_row < par_bank.rows(); par_row++) {
        var pid = par_bank.getInt("pid", par_row);
        if(pid == ELE_PDG) {
          var status  = (int) par_bank.getShort("status", par_row);
          var chi2pid = par_bank.getFloat("chi2pid", par_row);

          // CUT: trigger electrons in FD or CD
          // - must have status<0 and FD or CD bit(s) set
          // - must have |chi2pid|<3
          // - must appear in ECAL, to obtain sector
          if(status < 0
            && ( (Math.abs(status / 1000) & 0x2) != 0 ||
                 (Math.abs(status / 1000) & 0x4) != 0 )
            && Math.abs(chi2pid) < 3)
          {
            // get sector from ECAL
            Integer sec = null;
            for(int cal_row = 0; cal_row < cal_bank.rows(); cal_row++) {
              if(cal_bank.getShort("pindex", cal_row) == par_row && cal_bank.getByte("detector", cal_row) == ECAL_ID) {
                sec = (int) cal_bank.getByte("sector", cal_row);
                break;
              }
            }
            // CUT: sector must be defined
            if(sec != null) {
              // CUT: choose max-E electron; choice is from both FD and FT electron sets
              var par = getParticle(par_bank, par_row, pid);
              var e = par.e();
              if(par.e() > ele_par.e()) {
                ele_pindex = OptionalInt.of(par_row);
                ele_par    = par;
                ele_sec    = sec;
                ele_det    = det.FD;
              }
            }
          }

          // CUT: trigger electron in FT
          // - event has `RECFT::Particle` bank
          // - `REC::Particle:status` has FT bit
          // - must also appear in RECFT::Particle with status<0 and FT bit
          // - must have E > 300 MeV
          if(event.hasBank("RECFT::Particle") && (Math.abs(status/1000) & 0x1) != 0) {
            var par_ft_bank = event.getBank("RECFT::Particle");
            if(par_ft_bank.rows() > par_row) {
              var ft_status = par_ft_bank.getShort("status", par_row);
              if( par_ft_bank.getInt("pid", par_row) == ELE_PDG
                  && ft_status < 0
                  && (Math.abs(ft_status/1000) & 0x1) != 0)
              {
                var par = getParticle(par_bank, par_row, pid);
                // CUT: must have E > 300 MeV, and be the max energy
                if(par.e() > 0.3 && par.e() > ele_par.e()) {
                  ele_pindex = OptionalInt.of(par_row);
                  ele_par    = par;
                  ele_det    = det.FT;
                }
              }
            }
          }

        }
      }
    }

    // fill the yield histogram
    switch(ele_det) {
      case FD -> yield_hist.incrementBinContent(Channel.electronFD.ordinal());
      case FT -> yield_hist.incrementBinContent(Channel.electronFT.ordinal());
    }

  }

  // ----------------------------------------------------------------------------------

  /**
   * write to a HIPO file
   * @param output_dir the output directory
   * @param run_num the run number
   */
  public void write(String output_dir, int run_num)
  {
    TDirectory dir = new TDirectory();
    dir.mkdir("/QADB_YIELD/");
    dir.cd("/QADB_YIELD/");
    dir.addDataSet(yield_hist);
    dir.writeFile(output_dir + String.format("/out_QADB_YIELD_%d.hipo", run_num));
  }

}
