// package org.jlab.clas12.monitoring;

import java.io.*;
import java.util.*;
import org.jlab.clas.pdg.PhysicsConstants;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.math.F1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.base.GStyle;
import org.jlab.clas.physics.Particle;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.ConstantsManager;

public class ALERT {

    boolean userTimeBased;
    public int runNum, trigger;
    public String outputDir;
    public int crate;

    public boolean hasRF;
    public double startTime, rfTime;

    public double rfPeriod;
    public int rf_large_integer;

    //AHDC
    public H1F[] h_AHDC_wire;

    public IndexedTable InverseTranslationTable;
    public IndexedTable calibrationTranslationTable;
    public IndexedTable rfTable;

    public ConstantsManager ccdb;

    public ALERT(int reqrunNum, String reqOutputDir, float reqEb,boolean reqTimeBased) {
        runNum = reqrunNum;
        outputDir = reqOutputDir;
        userTimeBased = reqTimeBased;

        startTime = -1000;
        rfTime = -1000;
        trigger = 0;

        rfPeriod = 4.008;
        ccdb = new ConstantsManager();
        ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo", "/calibration/eb/rf/config"}));
        rfTable = ccdb.getConstants(runNum, "/calibration/eb/rf/config");
        if (rfTable.hasEntry(1, 1, 1)) {
            System.out.println(String.format("RF period from ccdb for run %d: %f", runNum, rfTable.getDoubleValue("clock", 1, 1, 1)));
            rfPeriod = rfTable.getDoubleValue("clock", 1, 1, 1);
        }
        rf_large_integer = 1000;

        //AHDC Histograms
        h_AHDC_wire = new H1F[8];
        for (int layer = 0; layer < 8; layer++) {
            h_AHDC_wire[layer] = new H1F(String.format("h_AHDC_wire_l%d", layer + 1), String.format("h_AHDC_wire_l%d", layer + 1), 200, 0, 200);
            h_AHDC_wire[layer].setTitleX("Wire No.");
            h_AHDC_wire[layer].setTitleY("Counts");
            h_AHDC_wire[layer].setFillColor(4);
        }
    }

    public void fillAHDC(DataBank AHDCHits, DataBank AHDCClusters, DataBank recBankEB) {
        // create histograms based on https://github.com/JeffersonLab/mon12/blob/main/src/main/java/org/clas/detectors/AHDCmonitor.java
        for (int i = 0; i < AHDCHits.rows(); i++) {
            int AHDC_layer = AHDCHits.getByte("layer", i);
            int AHDC_superlayer = AHDCHits.getByte("superlayer", i);
            int AHDC_wire       = AHDCHits.getInt("wire", i);
            int layer_number = 0;
            switch (10* AHDC_superlayer + AHDC_layer) {
                case 11 :
                    layer_number = 1;
                    break;
                case 21 :
                    layer_number = 2;
                    break;
                case 22 :
                    layer_number = 3;
                    break;
                case 31 :
                    layer_number = 4;
                    break;
                case 32 :
                    layer_number = 5;
                    break;
                case 41 :
                    layer_number = 6;
                    break;
                case 42 :
                    layer_number = 7;
                    break;
                case 51 :
                    layer_number = 8;
                    break;
            }
            h_AHDC_wire[layer_number-1].fill(AHDC_wire);
        }
    }

    public void fillATOF(DataBank ATOFHits, DataBank ATOFClusters, DataBank alertPart) {
        // create histograms based on https://github.com/JeffersonLab/mon12/blob/main/src/main/java/org/clas/detectors/ATOFmonitor.java
    }

    public void processEvent(DataEvent event) {

        DataBank recRun = null;
        DataBank recBankEB = null;
        DataBank recEvenEB = null;
        DataBank alertProjections = null;
        DataBank ATOFHITS = null;
        DataBank AHDCHits = null;
        DataBank ATOFClusters = null;
        DataBank AHDCClusters = null;
        if (event.hasBank("RUN::config")) {
            recRun = event.getBank("RUN::config");
        }
        if (event.hasBank("REC::Particle")) {
            recBankEB = event.getBank("REC::Particle");
        }
        if (event.hasBank("REC::Event")) {
            recEvenEB = event.getBank("REC::Event");
        }
        if (event.hasBank("ALERT::Projections")) {
            alertProjections = event.getBank("ALERT::Projections");
        }
        if (event.hasBank("ATOF::hits")) {
            ATOFHITS = event.getBank("ATOF::hits");
        }
        if (event.hasBank("AHDC::Hits")) {
            AHDCHits = event.getBank("AHDC::Hits");
        }
        if (event.hasBank("ATOF::clusters")) {
            ATOFClusters = event.getBank("ATOF::clusters");
        }
        if (event.hasBank("AHDC::Clusters")) {
            AHDCClusters = event.getBank("AHDC::Clusters");
        }
        //Get event start time
        if (recEvenEB != null) {
            startTime = recEvenEB.getFloat("startTime", 0);
            rfTime = recEvenEB.getFloat("RFTime", 0);
        }

        //Get trigger particle
        if (recBankEB != null) {
            trigger = recBankEB.getInt("pid", 0);
        }

        //Main Processing
        if (AHDCHits != null && AHDCClusters != null) {
            fillAHDC(AHDCHits, AHDCClusters, recBankEB);
        }
        if (ATOFHITS != null && ATOFClusters != null) {
            fillATOF(ATOFHITS, ATOFClusters, recBankEB);
        } 
    }

    public void analyze() {
        //we don't have to analyze anything for now.
    }


    public void plot() {
        //we don't have to save png plots for now.
    }

    public void write() {
        TDirectory dirout = new TDirectory();
        dirout.mkdir("/alert/");
        dirout.cd("/alert/");
        for (int layer = 0; layer < 7; layer++) {
            dirout.addDataSet(h_AHDC_wire[layer]);
        }
        if(runNum>0) dirout.writeFile(outputDir+"/out_ALERT_"+runNum+".hipo");
        else         dirout.writeFile(outputDir+"/out_ALERT.hipo");
    }
////////////////////////////////////////////////

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GStyle.setPalette("kRainBow");
        int count = 0;
        int runNum = 0;
        boolean useTB = true;
        String filelist = "list_of_files.txt";
        float EB = 10.6f; // GeV
        if (args.length > 0) {
            runNum = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            filelist = args[1];
        }
        if (args.length > 2) {
            if (Integer.parseInt(args[2]) == 0) {
                useTB = false;
            }
        }
        if(args.length>3)EB=Float.parseFloat(args[3]);
        String outputDir = runNum > 0 ? "plots"+runNum : "plots";
        ALERT ana = new ALERT(runNum, outputDir, EB, useTB);
        List<String> toProcessFileNames = new ArrayList<>();
        File file = new File(filelist);
        Scanner read;
        try {
            read = new Scanner(file);
            do {
                String filename = read.next();
                toProcessFileNames.add(filename);

            } while (read.hasNext());
            read.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(100);
        }

        int maxevents = 50000000;
        if (args.length > 2) {
            maxevents = Integer.parseInt(args[2]);
        }
        int progresscount = 0;
        int filetot = toProcessFileNames.size();
        for (String runstrg : toProcessFileNames) {
            if (count < maxevents) {
                progresscount++;
                System.out.println(String.format(">>>>>>>>>>>>>>>> ALERT %s", runstrg));
                File varTmpDir = new File(runstrg);
                if (!varTmpDir.exists()) {
                    System.out.println("FILE DOES NOT EXIST");
                    continue;
                }
                System.out.println("READING NOW " + runstrg);
                HipoDataSource reader = new HipoDataSource();
                reader.open(runstrg);
                int filecount = 0;
                while (reader.hasEvent() && count < maxevents) {
                    DataEvent event = reader.getNextEvent();
                    ana.processEvent(event);
                    filecount++;
                    count++;
                    if (count % 10000 == 0) {
                        System.out.println(count / 1000 + "k events (this is ALERT on " + runstrg + ") progress : " + progresscount + "/" + filetot);
                    }
                }
                reader.close();
            }
        }
        System.out.println("Total : " + count + " events");
        // ana.analyze();
        // ana.plot();
        ana.write();
    }
}
