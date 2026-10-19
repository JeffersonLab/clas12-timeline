package org.jlab.clas.timeline.histograms;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;

/**
 *
 * @author baltzell
 */
public class Trigger {

  public H1F bits;
  public H1F bits_helP;
  public H1F bits_helN;

  public static final String TDIR = "/TRIGGER";
  public static final int NBITS   = 64;

  public Trigger() {
    bits      = new H1F("bits",      "trigger bits",                NBITS+1, 0, NBITS+1);
    bits_helP = new H1F("bits_helP", "trigger bits, helicity = +1", NBITS+1, 0, NBITS+1);
    bits_helN = new H1F("bits_helN", "trigger bits, helicity = -1", NBITS+1, 0, NBITS+1);
  }

  public void processEvent(DataEvent event){
    DataBank ev   = event.getBank("REC::event");
    int helicity  = ev.rows() > 0 ? ev.getByte("helicity",0) : 0;
    DataBank bank = event.getBank("RUN::config");
    if (bank.rows()>0) {
      bits.fill(NBITS);
      switch(helicity) {
        case  1 -> bits_helP.fill(NBITS);
        case -1 -> bits_helN.fill(NBITS);
      }
      long t = bank.getLong("trigger",0);
      for (int i=0; i<NBITS; ++i) {
        if ( 1 == ((t>>i)&1) ) {
          bits.fill(i);
          switch(helicity) {
            case  1 -> bits_helP.fill(i);
            case -1 -> bits_helN.fill(i);
          }
        }
      }
    }
  }

  public void write(String outputDir, int runNumber) {
    TDirectory dir = new TDirectory();
    dir.mkdir(TDIR);
    dir.cd(TDIR);
    dir.addDataSet(bits);
    dir.writeFile(outputDir + String.format("/out_TRIGGER_%d.hipo",runNumber));
  }

  void readHistograms(TDirectory tdir)
  {
    bits      = (H1F) tdir.getObject(TDIR + "/bits");
    bits_helP = (H1F) tdir.getObject(TDIR + "/bits_helP");
    bits_helN = (H1F) tdir.getObject(TDIR + "/bits_helN");
  }

}
