package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import org.jlab.clas.physics.Particle;
import org.jlab.detector.base.DetectorType;

/**
 * @author dilks
 */
public class Electron {

  /** particle banks */
  public enum ElectronDet {
    /** electron not found */
    none,
    /** electron in {@code REC::Particle} */
    FD,
    /** electron in {@code RECFT::Particle} */
    FT,
  }

  // private members
  private boolean     m_found;
  private int         m_pindex;
  private int         m_sec;
  private ElectronDet m_det;

  // constants
  private static final int ELE_PDG = 11;
  private static final int ECAL_ID = DetectorType.ECAL.getDetectorId();

  // ----------------------------------------------------------------------------------

  /** constructor **/
  public Electron()
  {
  }

  // ----------------------------------------------------------------------------------

  /** @return whether an electron was found */
  public boolean found() { return m_found; }

  /** @return the electron {@code pindex} */
  public int getPIndex() { return m_pindex; }

  /** @return the electron sector */
  public int getSector() { return m_sec; }

  /** @return the electron {@code ElectronDet} */
  public ElectronDet getDet() { return m_det; }

  /** @return the electron particle-bank name */
  public String getParticleBankName()
  {
    return switch(m_det) {
      case none -> "NONE";
      case FD   -> "REC::Particle";
      case FT   -> "RECFT::Particle";
    };
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
    // reset class members
    m_found  = false;
    m_pindex = -1;
    m_sec    = -1;
    m_det    = ElectronDet.none;

    // electron Particle
    Particle ele = new Particle(ELE_PDG, 0.0, 0.0, 0.0);

    // find the electron
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
              if(par.e() > ele.e()) {
                m_found  = true;
                m_pindex = par_row;
                m_sec    = sec;
                m_det    = ElectronDet.FD;
                ele      = par;
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
                if(par.e() > 0.3 && par.e() > ele.e()) {
                  m_found  = true;
                  m_pindex = par_row;
                  m_sec    = 0;
                  m_det    = ElectronDet.FT;
                  ele      = par;
                }
              }
            }
          }

        }
      } // end loop over `REC::Particle`
    }
  }

}
