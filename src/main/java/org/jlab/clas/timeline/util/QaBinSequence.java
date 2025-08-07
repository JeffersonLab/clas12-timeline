package org.jlab.clas.timeline.util;

import java.util.List;
import java.util.ArrayList;

import org.jlab.clas.timeline.util.QaBin;
import org.jlab.detector.scalers.DaqScalersSequence;

public class QaBinSequence extends DaqScalersSequence {

  /// sequence of QA bins
  private final List<QaBin> qaBins = new ArrayList<>();

  /// @param filenames list of HIPO files to read
  /// @param binWidth the number of scaler readouts in each bin
  public QaBinSequence(List<String> filenames, int binWidth) {
    if(binWidth <= 0)
      throw new RuntimeException("binWidth must be greater than 0");
    this.readFiles(filenames);
    System.out.println("DEBUG: original size = " + this.scalers.size()); // FIXME: remove
    if(this.scalers.isEmpty())
      throw new RuntimeException("scalers is empty");
    List<Integer> keep = new ArrayList<>();
    keep.add(0);
    for(int i=0; i<this.scalers.size(); i+=binWidth) {
      int end = Math.min(i+binWidth, this.scalers.size()-1); // the last sample may be smaller
      qaBins.add(new QaBin(this.scalers.subList(i, end)));
      keep.add(end);
    }
    System.out.println("DEBUG: keep indices = " + keep); // FIXME: remove
    for (int i=this.scalers.size()-1; i>=0; i--) {
      if (!keep.contains(i))
        this.scalers.remove(i);
    }
    System.out.println("DEBUG: sampled size = " + this.scalers.size()); // FIXME: remove
    System.out.println("DEBUG: num qaBins = " + this.qaBins.size()); // FIXME: remove
  }


  /// @brief print the QA bins
  public void print() {
    System.out.println("QA BINS:");
    int binnum = 0;
    System.out.printf("%20s %20s %20s\n", "bin", "q_gated", "q_corrected");
    for(var qaBin : qaBins) {
      System.out.printf("%20d %20.5f %20.5f\n",
          binnum++,
          qaBin.getInterval().getBeamChargeGated(),
          qaBin.getBeamChargeLivetimeWeighted()
          );
    }
  }

}
