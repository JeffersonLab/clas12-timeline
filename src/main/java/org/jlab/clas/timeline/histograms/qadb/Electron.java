package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import org.jlab.clas.physics.Particle;
import org.jlab.detector.base.DetectorType;
import org.jlab.clas.timeline.util.Tools;

/**
 * @author dilks
 */
public class Electron {

  /** particle banks */
  public enum Det {
    /** electron not found */
    none,
    /** electron in {@code REC::Particle} */
    FD,
    /** electron in {@code RECFT::Particle} */
    FT,
  }

  // constants
  public static final int PDG = 11;
  public static final int ECAL_ID = DetectorType.ECAL.getDetectorId();

  // private members
  private boolean  m_found;
  private int      m_pindex;
  private int      m_sec;
  private Det      m_det;
  private Particle m_par = new Particle(PDG, 0.0, 0.0, 0.0);

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

  /** @return the electron {@code Det} */
  public Det getDetector() { return m_det; }

  /** @returns the electron {@code Particle} */
  public Particle getParticle() { return m_par; }

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
   * process a single event
   * @param event the HIPO event object
   */
  public void processEvent(DataEvent event)
  {
    // reset class members
    m_found  = false;
    m_pindex = -1;
    m_sec    = -1;
    m_det    = Det.none;
    m_par.setVector(PDG, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    // find the electron
    if(event.hasBank("REC::Particle") && event.hasBank("REC::Calorimeter")) {
      var par_bank = event.getBank("REC::Particle");
      var cal_bank = event.getBank("REC::Calorimeter");

      // loop over electrons from `REC::Particle`
      for(int par_row = 0; par_row < par_bank.rows(); par_row++) {
        var pid = par_bank.getInt("pid", par_row);
        if(pid == PDG) {
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
              var par = Tools.getParticle(par_bank, par_row);
              if(par.e() > m_par.e()) {
                m_found  = true;
                m_pindex = par_row;
                m_sec    = sec;
                m_det    = Det.FD;
                m_par.copy(par);
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
              if( par_ft_bank.getInt("pid", par_row) == PDG
                  && ft_status < 0
                  && (Math.abs(ft_status/1000) & 0x1) != 0)
              {
                var par = Tools.getParticle(par_bank, par_row);
                // CUT: must have E > 300 MeV, and be the max energy
                if(par.e() > 0.3 && par.e() > m_par.e()) {
                  m_found  = true;
                  m_pindex = par_row;
                  m_sec    = 0;
                  m_det    = Det.FT;
                  m_par.copy(par);
                }
              }
            }
          }

        }
      } // end loop over `REC::Particle`
    }
  }

}
