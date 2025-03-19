package org.jlab.clas.timeline.histograms;

import java.util.ArrayList;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import org.jlab.detector.helicity.DecoderBoardUtil;

/**
 *
 * @author baltzell
 */
public class helicity {

  public static final byte DELAY_WINDOWS = 8;
  private static int nExceptions = 0; 

  private class H1Fb extends H1F {
    String bankName;
    String varName;
    public H1Fb(String name, String bankName, String varName) {
      super(name,3,-1.5,1.5);
      this.bankName = bankName;
      this.varName = varName;
    }
    public void fill(DataEvent event) {
      if (event.hasBank(this.bankName) && event.getBank(this.bankName).rows()>0) {
        this.fill(event.getBank(this.bankName).getByte(this.varName, 0));
      }
    }
  }

  ArrayList<H1Fb> histos;
  H1F hboard;

  public helicity() {
    histos = new ArrayList<>();
    histos.add(new H1Fb("onlineRaw","HEL::online","helicityRaw"));
    histos.add(new H1Fb("offlineRaw","REC::Event","helicityRaw"));
    histos.add(new H1Fb("online","HEL::online","helicity"));
    histos.add(new H1Fb("offline","REC::Event","helicity"));
    hboard = histos.get(0).histClone("boardRaw");
  }

  public void processEvent(DataEvent event){
    for (H1Fb h : this.histos) {
      h.fill(event);
    }
    if (nExceptions < 10) {
      try {
        if (event.hasBank("HEL::decoder") && event.getBank("HEL::decoder").rows()>0) {
          DataBank bank = event.getBank("HEL::decoder");
          int h = DecoderBoardUtil.QUARTET.check(bank) ?
            -1+2*DecoderBoardUtil.QUARTET.getWindowHelicity(bank, DELAY_WINDOWS) :
            0;
          this.hboard.fill(h);
        }
      }
      catch (NoSuchFieldError e) {
        nExceptions++;
        System.out.println(e);
      }
    }
  }

  public void write(String outputDir, int runNumber) {
    TDirectory dir = new TDirectory();
    dir.mkdir("/HELICITY/");
    dir.cd("/HELICITY/");
    for (H1Fb h : this.histos) dir.addDataSet(h);
    dir.addDataSet(hboard);
    dir.writeFile(outputDir + String.format("/out_HELICITY_%d.hipo",runNumber));
  }

}
