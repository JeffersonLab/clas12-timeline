package org.jlab.clas.timeline.histograms.qadb;

import org.jlab.detector.qadb.QadbBin;

/**
 * minimal subset of info for a {@code QadbBin}
 * @author dilks
 */
public class QadbBinBounds {

  // constants
  public static final String HEADER = "# binNum evnumMin evnumMax timestampMin timestampMax";
  public static final int NCOL      = 5;

  // members
  public int  binNum;
  public int  evnumMin;
  public int  evnumMax;
  public long timestampMin;
  public long timestampMax;
  public QadbBin.BinType binType;

  /** @return a string for this bin */
  @Override
  public String toString()
  {
    return binNum + " " + evnumMin + " " + evnumMax + " " + timestampMin + " " + timestampMax;
  }

}
