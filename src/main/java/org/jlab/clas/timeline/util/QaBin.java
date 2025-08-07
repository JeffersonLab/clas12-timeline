package org.jlab.clas.timeline.util;

import java.util.List;
import org.jlab.detector.scalers.DaqScalers;
import org.jlab.detector.scalers.DaqScalersSequence;

public class QaBin extends DaqScalersSequence {

  private int evnumMin; /// event number minimum
  private int evnumMax; /// event number maximum
  private long timestampMin; /// timestamp minimum
  private long timestampMax; /// timestamp maximum

  private long[] nElecFD = {0, 0, 0, 0, 0, 0}; /// number of FD electrons in each sector
  private long nElecFT = 0; /// number of electrons /// number of FT electrons

  /// constructor
  public QaBin(List<DaqScalers> inputScalers) {
    super(inputScalers);
    this.timestampMin = this.scalers.get(0).getTimestamp();
    this.timestampMax = this.scalers.get(scalers.size()-1).getTimestamp();
    this.evnumMin     = this.scalers.get(0).getEventNum();
    this.evnumMax     = this.scalers.get(scalers.size()-1).getEventNum();
  }

}
