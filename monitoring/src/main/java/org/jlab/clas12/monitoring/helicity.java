package org.jlab.clas12.monitoring;

import java.util.ArrayList;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.io.base.DataEvent;

/**
 *
 * @author baltzell
 */
public class helicity {

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

    public helicity() {
        histos = new ArrayList<>();
        histos.add(new H1Fb("helOnlineRaw","HEL::online","helicityRaw"));
        histos.add(new H1Fb("helOfflineRaw","REC::Event","helicityRaw"));
        histos.add(new H1Fb("helOnline","HEL::online","helicity"));
        histos.add(new H1Fb("helOffline","REC::Event","helicity"));
        histos.add(new H1Fb("helBoardRaw","HEL::decoder","helicity"));
    }

    public void processEvent(DataEvent event){
        for (H1Fb h : this.histos) h.fill(event);
    }

    public void write(int runNumber) {
        String path = runNumber>0 ? "plots"+runNumber : "plots";
		TDirectory dir = new TDirectory();
		dir.mkdir("/HELICITY/");
		dir.cd("/HELICITY/");
        for (H1Fb h : this.histos) dir.addDataSet(h);
        dir.writeFile(path+"/out_HELICITY.hipo");
	}

}
