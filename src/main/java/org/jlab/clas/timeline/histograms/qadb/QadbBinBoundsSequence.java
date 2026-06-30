package org.jlab.clas.timeline.histograms.qadb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import org.jlab.detector.qadb.QadbBin;
import org.jlab.detector.qadb.QadbBinSequence;

/**
 * sequence of {@code QadbBinBounds}
 * @author dilks
 */
public class QadbBinBoundsSequence implements Iterable<QadbBinBounds> {

  // private members
  private final TreeMap<Integer, QadbBinBounds> m_seq = new TreeMap<>();

  // ----------------------------------------------------------------------------------

  /**
   * construct a sequence from a {@code QadbBinSequence}
   * @param qa_seq the {@code QadbBinBoundsSequence}
   */
  public static <T> QadbBinBoundsSequence read(QadbBinSequence<T> qa_seq)
  {
    QadbBinBoundsSequence seq = new QadbBinBoundsSequence();
    for(var qa_bin : qa_seq) {
      QadbBinBounds bin = new QadbBinBounds();
      bin.binNum        = qa_bin.getBinNum();
      bin.evnumMin      = (int) qa_bin.getEventNumMin();
      bin.evnumMax      = (int) qa_bin.getEventNumMax();
      bin.timestampMin  = qa_bin.getTimestampMin();
      bin.timestampMax  = qa_bin.getTimestampMax();
      seq.add(bin);
    }
    seq.setBinTypes();
    return seq;
  }

  // ----------------------------------------------------------------------------------

  /**
   * construct a sequence from a table file
   * @param file_name the path to the file
   */
  public static QadbBinBoundsSequence read(String file_name) throws IOException
  {
    Path path                 = Path.of(file_name);
    QadbBinBoundsSequence seq = new QadbBinBoundsSequence();
    List<String> lines        = Files.readAllLines(path);
    int lineNum               = 0;
    for(String rawLine : lines) {
      lineNum++;
      String line = rawLine.trim();
      if(line.isEmpty() || line.startsWith("#"))
        continue;
      String[] tokens = line.split("\\s+");
      if(tokens.length != QadbBinBounds.NCOL)
        throw new IOException("Malformed line " + lineNum + " in " + path + ": " + rawLine);
      try {
        QadbBinBounds bin = new QadbBinBounds();
        bin.binNum        = Integer.parseInt(tokens[0]);
        bin.evnumMin      = Integer.parseInt(tokens[1]);
        bin.evnumMax      = Integer.parseInt(tokens[2]);
        bin.timestampMin  = Long.parseLong(tokens[3]);
        bin.timestampMax  = Long.parseLong(tokens[4]);
        seq.add(bin);
      } catch(NumberFormatException e) {
        throw new IOException("Malformed line " + lineNum + " in " + path + ": " + rawLine, e);
      }
    }
    seq.setBinTypes();
    return seq;
  }

  // ----------------------------------------------------------------------------------

  /** set the {@code QadbBin.BinType} for each bin */
  private void setBinTypes()
  {
    for(var bin : m_seq.values()) {
      if(bin.binNum == 0)
        bin.binType = QadbBin.BinType.FIRST;
      else if(bin.binNum == m_seq.size() - 1)
        bin.binType = QadbBin.BinType.LAST;
      else
        bin.binType = QadbBin.BinType.INTERMEDIATE;
    }
  }

  // ----------------------------------------------------------------------------------

  /**
   * add a bin
   * @param bin the bin
   */
  public void add(QadbBinBounds bin)
  {
    m_seq.put(bin.binNum, bin);
  }

  // ----------------------------------------------------------------------------------

  /**
   * @return the bin for a certain bin number
   * @param binNum the bin number
   */
  public QadbBinBounds getBin(int binNum)
  {
    QadbBinBounds bin = m_seq.get(binNum);
    if(bin == null)
      throw new NoSuchElementException("No bin with number " + binNum);
    return bin;
  }

  // ----------------------------------------------------------------------------------

  /** @return the number of bins */
  public int size()
  {
    return m_seq.size();
  }

  // ----------------------------------------------------------------------------------

  /**
   * Allows: {@code for(QadbBinBounds bin : binningScheme) { ... }}
   */
  @Override
  public Iterator<QadbBinBounds> iterator()
  {
    return m_seq.values().iterator();
  }

  // ----------------------------------------------------------------------------------

  /**
   * write a table file
   * @param file_name the path to the file
   */
  public void write(String file_name) throws IOException {
    Path path = Path.of(file_name);
    try(BufferedWriter writer = Files.newBufferedWriter(path)) {
      writer.write(QadbBinBounds.HEADER);
      writer.newLine();
      for(var bin : m_seq.values()) {
        writer.write(bin.toString());
        writer.newLine();
      }
    }
  }

}
