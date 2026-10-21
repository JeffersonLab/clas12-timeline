package org.jlab.clas.timeline.histograms;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import org.jlab.io.hipo.HipoDataEvent;

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
    bits      = new H1F("bits",      "trigger bits",                NBITS+1, -0.5, NBITS+0.5);
    bits_helP = new H1F("bits_helP", "trigger bits, helicity = +1", NBITS+1, -0.5, NBITS+0.5);
    bits_helN = new H1F("bits_helN", "trigger bits, helicity = -1", NBITS+1, -0.5, NBITS+0.5);
  }

  private void fillHisto(H1F h, long t) {
    // fill total number of triggers in the `NBITS`th bin
    h.fill(NBITS);
    // fill number of triggers for each trigger bit
    for(int i=0; i<NBITS; ++i) {
      if( 1 == ((t>>i)&1) )
        h.fill(i);
    }
  }

  public void processEvent(DataEvent event){
    if(!event.hasBank("RUN::config")) return;
    DataBank b_conf = event.getBank("RUN::config");
    // avoid duplicate tag-1 events
    if (((HipoDataEvent) event).getHipoEvent().getEventTag() != 0) return;
    // get delay-corrected helicity
    int helicity = 0;
    if(event.hasBank("HEL::online")) {
      DataBank b_hel = event.getBank("HEL::online");
      helicity = b_hel.rows() > 0 ? b_hel.getByte("helicity",0) : 0;
    }
    // fill histograms
    if(b_conf.rows()>0) {
      long t = b_conf.getLong("trigger",0);
      fillHisto(bits, t);
      switch(helicity) {
        case  1 -> fillHisto(bits_helP, t);
        case -1 -> fillHisto(bits_helN, t);
      }
    }
  }

  public void write(String outputDir, int runNumber) {
    TDirectory dir = new TDirectory();
    dir.mkdir(TDIR);
    dir.cd(TDIR);
    dir.addDataSet(bits);
    dir.addDataSet(bits_helP);
    dir.addDataSet(bits_helN);
    dir.writeFile(outputDir + String.format("/out_TRIGGER_%d.hipo",runNumber));
  }

  void readHistograms(TDirectory tdir)
  {
    bits      = (H1F) tdir.getObject(TDIR + "/bits");
    bits_helP = (H1F) tdir.getObject(TDIR + "/bits_helP");
    bits_helN = (H1F) tdir.getObject(TDIR + "/bits_helN");
  }

}
