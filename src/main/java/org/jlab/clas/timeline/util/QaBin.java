package org.jlab.clas.timeline.util;

import java.util.List;
import org.jlab.detector.scalers.DaqScalers;
import org.jlab.detector.scalers.DaqScalersSequence;

public class QaBin extends DaqScalersSequence {

  private int binNum; /// bin number
  private int evnumMin; /// event number minimum
  private int evnumMax; /// event number maximum
  private long timestampMin; /// timestamp minimum
  private long timestampMax; /// timestamp maximum

  private long[] nElecFD = {0, 0, 0, 0, 0, 0}; /// number of FD electrons in each sector
  private long nElecFT = 0; /// number of electrons /// number of FT electrons

  /// construct a single bin
  /// @param binNum the bin number, in the `QaBinSequence` which contains this bin
  /// @param inputScalers the scaler sequence for this bin
  public QaBin(int binNum, List<DaqScalers> inputScalers) {
    super(inputScalers);
    this.binNum       = binNum;
    this.timestampMin = this.scalers.get(0).getTimestamp();
    this.timestampMax = this.scalers.get(scalers.size()-1).getTimestamp();
    this.evnumMin     = this.scalers.get(0).getEventNum();
    this.evnumMax     = this.scalers.get(scalers.size()-1).getEventNum();
  }

  /// @brief print a QA bin
  /// @param printNames print the variable names too
  public void print(boolean printNames) {
    if(printNames)
      System.out.printf("%15s %15s %15s\n",
          "bin",
          "q_gated",
          "q_corrected"
          );
    System.out.printf("%15d %15.5f %15.5f\n",
        this.binNum,
        this.getInterval().getBeamChargeGated(),
        this.getBeamChargeLivetimeWeighted()
        );
  }

  /// @brief print a QA bin; include header if bin 0
  public void print() {
    this.print(this.binNum==0);
  }

  /// @return minimum timestamp for this bin
  public long getTimestampMin() { return this.timestampMin; }

  /// @return maximum timestamp for this bin
  public long getTimestampMax() { return this.timestampMax; }

  /// @return minimum event number for this bin
  public long getEventNumMin() { return this.evnumMin; }

  /// @return maximum event number for this bin
  public long getEventNumMax() { return this.evnumMax; }

  /// @param timestamp the timestamp
  /// @return true if the bin contains this timestamp
  public boolean containsTimestamp(long timestamp) {
    return timestamp >= this.timestampMin && timestamp <= this.timestampMax;
  }

}
