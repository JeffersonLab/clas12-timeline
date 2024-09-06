package org.jlab.clas12.monitoring;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;

/**
 *
 * @author baltzell
 */
public class trigger {

    H1F bits;

    public trigger() {
        bits = new H1F("bits",65,0,65);
        bits.getDataX(0);
        bits.getEntries();
        bits.getMaximumBin();
        bits.getAxis().getNBins();
    }

    public void processEvent(DataEvent event){
        DataBank bank = event.getBank("RUN::config");
        if (bank.rows()>0) {
            bits.fill(64);
            long t = bank.getLong("trigger",0);
            for (int i=0; i<64; ++i)
                if ( 1 == ((t>>i)&1) )
                    bits.fill(i);
        }
    }

    public void write(String outputDir, int runNumber) {
		TDirectory dir = new TDirectory();
		dir.mkdir("/TRIGGER/");
		dir.cd("/TRIGGER/");
        dir.addDataSet(bits);
        dir.writeFile(outputDir + String.format("/out_TRIGGER_%d.hipo",runNumber));
	}

}
