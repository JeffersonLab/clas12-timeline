package org.jlab.clas.timeline.histograms.qadb;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jlab.clas.physics.LorentzVector;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.Vector3;
import org.jlab.clas.timeline.util.Tools;
import org.jlab.groot.data.Directory;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.io.base.DataEvent;

/**
 * Histograms for positive- and negative-pion kinematics.
 */
public class PionKinematics {

  /** Calculated pion observables before histogram acceptance cuts. */
  record KinematicValues(
      double p,
      double pT,
      double z,
      double theta,
      double phiH) {}

  /** TDirectory path. */
  public static final String TDIR = "/QADB/pion_kinematics";

  /** Positive-pion PDG identifier. */
  public static final int PIP = 211;

  /** Negative-pion PDG identifier. */
  public static final int PIM = -211;

  private static final double PLANE_ANGLE_EPSILON = 1.0e-5;

  private final double m_beam_energy;
  private final Map<Integer, Map<String, H1F>> m_histograms =
      new LinkedHashMap<>();

  /**
   * @param bin_num QADB bin number
   * @param beam_energy beam energy in GeV
   */
  public PionKinematics(int bin_num, double beam_energy)
  {
    if(!Double.isFinite(beam_energy) || beam_energy <= 0.0) {
      throw new IllegalArgumentException(
          "beam energy must be finite and positive");
    }

    m_beam_energy = beam_energy;
    m_histograms.put(PIP, makeHistograms("pip", bin_num));
    m_histograms.put(PIM, makeHistograms("pim", bin_num));
  }

  private static Map<String, H1F> makeHistograms(
      String particle_name,
      int bin_num)
  {
    String suffix = "_qa" + String.valueOf(bin_num);
    Map<String, H1F> histograms = new LinkedHashMap<>();

    histograms.put(
        "p",
        new H1F(
            particle_name + "_p_hist" + suffix,
            "p [GeV]",
            "counts",
            50, 0.0, 10.0));

    histograms.put(
        "pT",
        new H1F(
            particle_name + "_pT_hist" + suffix,
            "pT [GeV]",
            "counts",
            50, 0.0, 4.0));

    histograms.put(
        "z",
        new H1F(
            particle_name + "_z_hist" + suffix,
            "z",
            "counts",
            50, 0.0, 1.0));

    histograms.put(
        "theta",
        new H1F(
            particle_name + "_theta_hist" + suffix,
            "theta [rad]",
            "counts",
            50, 0.0, Math.PI / 2.0));

    histograms.put(
        "phiH",
        new H1F(
            particle_name + "_phiH_hist" + suffix,
            "phiH [rad]",
            "counts",
            50, -3.15, 3.15));

    return histograms;
  }

  /**
   * Get one pion-kinematics histogram.
   *
   * @param pid pion PDG identifier
   * @param variable kinematic variable
   * @return requested histogram
   */
  public H1F getHistogram(int pid, String variable)
  {
    Map<String, H1F> particle_histograms = m_histograms.get(pid);

    if(particle_histograms == null) {
      throw new IllegalArgumentException("unsupported pion PID: " + pid);
    }

    H1F histogram = particle_histograms.get(variable);

    if(histogram == null) {
      throw new IllegalArgumentException(
          "unsupported pion kinematic variable: " + variable);
    }

    return histogram;
  }

  /**
   * Process all event-builder pion candidates in one event.
   *
   * @param event HIPO event
   * @param electron selected scattered electron
   */
  public void processEvent(DataEvent event, Electron electron)
  {
    if(event == null || !event.hasBank("REC::Particle")) {
      return;
    }

    var particle_bank = event.getBank("REC::Particle");

    for(int row = 0; row < particle_bank.rows(); row++) {
      int pid = particle_bank.getInt("pid", row);

      if(pid == PIP || pid == PIM) {
        processParticle(Tools.getParticle(particle_bank, row), electron);
      }
    }
  }

  /**
   * Calculate and histogram one pion candidate.
   *
   * Package-private visibility provides a direct unit-test seam for the
   * kinematic calculation without constructing a HIPO bank.
   *
   * @param pion pion candidate
   * @param electron selected scattered electron
   */
  void processParticle(Particle pion, Electron electron)
  {
    if(pion == null
        || (pion.pid() != PIP && pion.pid() != PIM)
        || electron == null
        || !electron.found()
        || !(electron.getQ2() > 1.0)
        || !(electron.getW() > 2.0)
        || !(electron.getY() < 0.8))
    {
      return;
    }

    KinematicValues values = calculateKinematics(pion, electron);

    if(!(values.z() > 0.3 && values.z() < 1.0)
        || !Double.isFinite(values.p())
        || !Double.isFinite(values.pT())
        || !Double.isFinite(values.z())
        || !Double.isFinite(values.theta())
        || !Double.isFinite(values.phiH()))
    {
      return;
    }

    getHistogram(pion.pid(), "p").fill(values.p());
    getHistogram(pion.pid(), "pT").fill(values.pT());
    getHistogram(pion.pid(), "z").fill(values.z());
    getHistogram(pion.pid(), "theta").fill(values.theta());
    getHistogram(pion.pid(), "phiH").fill(values.phiH());
  }

  /**
   * Calculate pion observables without applying histogram acceptance cuts.
   *
   * Package-private visibility keeps the numerical physics contract directly
   * testable without exposing it as public API.
   *
   * @param pion pion candidate
   * @param electron selected scattered electron
   * @return calculated pion observables
   */
  KinematicValues calculateKinematics(Particle pion, Electron electron)
  {

    LorentzVector beam = new LorentzVector(
        0.0, 0.0, m_beam_energy, m_beam_energy);
    LorentzVector q = new LorentzVector(beam);
    q.sub(electron.getParticle().vector());

    LorentzVector target = new LorentzVector(
        0.0, 0.0, 0.0, Electron.PROTON_MASS);
    LorentzVector pion_vector = pion.vector();

    double target_dot_q = lorentzDot(target, q);
    double z = lorentzDot(target, pion_vector) / target_dot_q;
    double p = pion_vector.p();
    double pT = Math.hypot(pion_vector.px(), pion_vector.py());
    double theta = pion_vector.theta();
    double phiH = planeAngle(
        q.vect(),
        electron.getParticle().vector().vect(),
        q.vect(),
        pion_vector.vect());

    return new KinematicValues(p, pT, z, theta, phiH);
  }

  private static double lorentzDot(
      LorentzVector first,
      LorentzVector second)
  {
    return first.e() * second.e()
        - first.vect().dot(second.vect());
  }

  private static double planeAngle(
      Vector3 first_a,
      Vector3 first_b,
      Vector3 second_a,
      Vector3 second_b)
  {
    Vector3 first_cross = first_a.cross(first_b);
    Vector3 second_cross = second_a.cross(second_b);

    double sign_value = first_cross.dot(second_b);
    if(Math.abs(sign_value) < PLANE_ANGLE_EPSILON) {
      return Double.NaN;
    }

    double denominator = first_cross.mag() * second_cross.mag();
    if(Math.abs(denominator) < PLANE_ANGLE_EPSILON) {
      return Double.NaN;
    }

    double cosine = first_cross.dot(second_cross) / denominator;
    if(!Double.isFinite(cosine)) {
      return Double.NaN;
    }

    cosine = Math.max(-1.0, Math.min(1.0, cosine));
    return Math.copySign(Math.acos(cosine), sign_value);
  }

  /**
   * Write the pion-kinematics histograms to a HIPO directory.
   *
   * @param tdir output directory
   */
  public void write(TDirectory tdir)
  {
    tdir.mkdir(TDIR);
    tdir.cd(TDIR);

    for(Map<String, H1F> particle_histograms : m_histograms.values()) {
      for(H1F histogram : particle_histograms.values()) {
        tdir.addDataSet(histogram);
      }
    }
  }

  /**
   * Read a complete pion-kinematics histogram schema.
   *
   * @param tdir input directory
   * @param bin_num QADB bin number
   * @return true when all ten required histograms were read
   */
  public boolean readHistograms(TDirectory tdir, int bin_num)
  {
    Directory<?> directory = tdir;

    for(String name : TDIR.split("/")) {
      if(name.isEmpty()) {
        continue;
      }

      directory = (Directory<?>) directory.getDirectoryMap().get(name);
      if(directory == null) {
        return false;
      }
    }

    Map<Integer, Map<String, H1F>> loaded = new LinkedHashMap<>();
    Map<String, ?> objects = directory.getObjectMap();
    String suffix = "_qa" + String.valueOf(bin_num);

    for(int pid : new int[] {PIP, PIM}) {
      String particle_name = pid == PIP ? "pip" : "pim";
      Map<String, H1F> particle_histograms = new LinkedHashMap<>();

      for(String variable : new String[] {
          "p", "pT", "z", "theta", "phiH"})
      {
        String object_name = particle_name + "_" + variable
            + "_hist" + suffix;
        Object object = objects.get(object_name);

        if(!(object instanceof H1F histogram)) {
          return false;
        }

        particle_histograms.put(variable, histogram);
      }

      loaded.put(pid, particle_histograms);
    }

    m_histograms.clear();
    m_histograms.putAll(loaded);
    return true;
  }
}
