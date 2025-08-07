package org.jlab.clas.timeline.util;

import org.jlab.detector.scalers.DaqScalersSequence;

public class QaBin {

  private final DaqScalersSequence seq; /// scaler readout sequence
  private int evnumMin; /// event number minimum
  private int evnumMax; /// event number maximum
  private long timestampMin; /// timestamp minimum
  private long timestampMax; /// timestamp maximum

  private long[] nElecFD = {0, 0, 0, 0, 0, 0}; /// number of FD electrons in each sector
  private long nElecFT = 0; /// number of electrons /// number of FT electrons

  private double fc = 0; /// DAQ-gated FC charge
  private double ufc = 0; /// full FC charge (ungated)

  /// constructor
  public QaBin(DaqScalersSequence seq) {
    this.seq = seq;
  }



}
