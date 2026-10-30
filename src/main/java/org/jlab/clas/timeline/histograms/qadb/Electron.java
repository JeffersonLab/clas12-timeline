package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.clas.physics.LorentzVector;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.timeline.util.Tools;
import org.jlab.detector.base.DetectorType;
import org.jlab.io.base.DataEvent;

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
  public static final double PROTON_MASS = 0.9382720813; // GeV

  // private members
  private boolean  m_found;
  private int      m_pindex;
  private int      m_sec;
  private Det      m_det;
  private Particle m_par = new Particle(PDG, 0.0, 0.0, 0.0);
  private final double m_beam_energy;

  private double m_q2 = Double.NaN;
  private double m_x  = Double.NaN;
  private double m_w  = Double.NaN;
  private double m_y  = Double.NaN;

  // ----------------------------------------------------------------------------------

  /**
   * Construct an electron selector using the historical default beam energy.
   *
   * @deprecated pass the run-dependent beam energy explicitly
   */
  @Deprecated
  public Electron()
  {
    this(DEFAULT_BEAM_ENERGY);
  }

  /**
   * @param beam_energy beam energy in GeV
   */
  public Electron(double beam_energy)
  {
    if(!Double.isFinite(beam_energy) || beam_energy <= 0.0) {
      throw new IllegalArgumentException("beam energy must be finite and positive");
    }
    m_beam_energy = beam_energy;
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

  /** @return Q^2 in GeV^2 */
  public double getQ2() { return m_q2; }

  /** @return Bjorken x */
  public double getX() { return m_x; }

  /** @return invariant mass W in GeV */
  public double getW() { return m_w; }

  /** @return inelasticity y */
  public double getY() { return m_y; }

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
    m_q2 = Double.NaN;
    m_x  = Double.NaN;
    m_w  = Double.NaN;
    m_y  = Double.NaN;
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

    // The loop is complete and m_par contains the selected electron.
    if(m_found) {
      calculateKinematics(m_par);
    }
  }

  /**
   * Calculate inclusive kinematics from the scattered electron.
   *
   * Package-private visibility allows direct unit testing without
   * exposing this method as part of the public API.
   *
   * @param electron scattered electron
   */
  void calculateKinematics(Particle electron)
  {
    LorentzVector beam = new LorentzVector(
        0.0, 0.0, m_beam_energy, m_beam_energy);

    LorentzVector q = new LorentzVector(0.0, 0.0, 0.0, 0.0);
    q.add(beam);
    q.sub(electron.vector());
    double nu = q.e();

    m_q2 = -q.mass2();
    m_y  = nu / m_beam_energy;
    m_x  = nu > 0.0
        ? m_q2 / (2.0 * PROTON_MASS * nu)
        : Double.NaN;

    LorentzVector hadronic_system =
        new LorentzVector(0.0, 0.0, 0.0, PROTON_MASS);
    hadronic_system.add(q);

    double w2 = hadronic_system.mass2();
    m_w = w2 >= 0.0 ? Math.sqrt(w2) : Double.NaN;
  }

}
