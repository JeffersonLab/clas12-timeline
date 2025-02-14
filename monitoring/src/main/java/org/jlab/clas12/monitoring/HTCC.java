package org.jlab.clas12.monitoring;

import java.io.*;
import java.util.*;
import org.jlab.clas.pdg.PhysicsConstants;

import org.jlab.groot.math.*;
import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.fitter.DataFitter;

public class HTCC {

    public int runNumber;
    public String outputDir;
    int ring, sector, hs;
    List<H1F> hiNphePMTOneHit = new ArrayList<H1F>();
    List<H1F> hiTimePMTOneHit = new ArrayList<H1F>();
    H1F timeAll;
    H1F npheAll;
    static int nBinsTime = 300;
    static double lowTime = -15; //Apr2023 changed limits from -500, 500 ns to -15 to 15 ns per D. Carman's request
    static double highTime = 15;

    public HTCC(int run, String reqOutputDir) {
        this.runNumber = run;
        this.outputDir = reqOutputDir;
        for (int t = 0; t < 48; t++) {
            ring = (int) (t / 12) + 1;
            hs = (int) (t % 2) + 1;
            sector = (int) (t % 12) / 2 + 1;
            hiNphePMTOneHit.add(new H1F("H_HTCC_nphe_s" + sector + "_r" + ring + "_side" + hs, 80, 0.5, 40.5));
            hiNphePMTOneHit.get(t).setTitle("S" + sector + " HS " + hs + " R " + ring);
            hiNphePMTOneHit.get(t).setTitle("S" + sector + " HS " + hs + " R " + ring);
            hiNphePMTOneHit.get(t).setOptStat(110);
            hiNphePMTOneHit.get(t).setOptStat(110);
            hiTimePMTOneHit.add(new H1F("H_HTCC_vtime_s" + sector + "_r" + ring + "_side" + hs, nBinsTime, lowTime, highTime));
            hiTimePMTOneHit.get(t).setTitle("S" + sector + " HS " + hs + " R " + ring);
            hiTimePMTOneHit.get(t).setTitle("S" + sector + " HS " + hs + " R " + ring);
        }

        timeAll = new H1F("timeAll", 2000, -4, 4);
        timeAll.setOptStat(110);
        timeAll.setTitle("Combined HTCC timing");
        timeAll.setTitleX("Time, ns");

        npheAll = new H1F("npheAll", "npheAll", 50, 0, 50);
        npheAll.setOptStat(110);
    }

    int returnSector(double phi) {
        double phiLoc = phi;
        int sector = -100;
        if (phiLoc < -30) {
            phiLoc = phiLoc + 360;
        }
        if (phiLoc > 30 && phiLoc < 90) {
            sector = 2;
        }
        if (phiLoc > 90 && phiLoc < 150) {
            sector = 3;
        }
        if (phiLoc > 150 && phiLoc < 210) {
            sector = 4;
        }
        if (phiLoc > 210 && phiLoc < 270) {
            sector = 5;
        }
        if (phiLoc > 270 && phiLoc < 330) {
            sector = 6;
        }
        if (phiLoc > 330 || phiLoc < 30) {
            sector = 1;
        }
        return sector;
    }

    int returnHalfSector(double phi) {
        int halfSector = 0;
        halfSector = (int) ((phi + 166.0) / 30);
        if (halfSector > 4) {
            halfSector = halfSector - 5;
        } else {
            halfSector = halfSector + 7;
        }
        return halfSector + 1;
    }

    int returnRing(double theta) {
        int ring = 0;
        if (theta <= 10) {
            ring = 1;
        }
        if (theta > 10 && theta <= 20) {
            ring = 2;
        }
        if (theta > 20 && theta <= 30) {
            ring = 3;
        }
        if (theta > 30) {
            ring = 4;
        }
        return ring;
    }

    int returnPMT(int ring, int halfSector) {
        int pmt = 0;
        pmt = (ring - 1) * 12 + halfSector;
        return pmt;
    }

    int returnNHits(double theta, double phi) {
        int nhits = 0;
        if (((int) Math.round(theta * 100) == 875 || (int) Math.round(theta * 100) == 1625 || (int) Math.round(theta * 100) == 2375 || (int) Math.round(theta * 100) == 3125) && (((int) Math.round(phi) + 165) % 15 == 0)) {
            nhits = 1;
        }
        return nhits;
    }

    public void plot() {
        List<F1D> timeIndPMT = new ArrayList<F1D>();
        for (int t = 0; t < 48; t++) {
            timeIndPMT.add(new F1D("timeIndPMT" + t, "[amp]*gaus(x,[mean],[sigma])", lowTime, highTime));
            timeIndPMT.get(t).setParameter(0, 500);
            timeIndPMT.get(t).setParameter(1, -0.0);
            timeIndPMT.get(t).setParameter(2, 0.7);
            timeIndPMT.get(t).setLineColor(2);
            timeIndPMT.get(t).setLineWidth(2);
            timeIndPMT.get(t).setOptStat(1101);
        }

        EmbeddedCanvas oneHitHTCCOnly = new EmbeddedCanvas();
        oneHitHTCCOnly.setSize(2400, 600);
        oneHitHTCCOnly.divide(12, 4);

        oneHitHTCCOnly.setAxisTitleSize(14);
        oneHitHTCCOnly.setAxisFontSize(14);
        oneHitHTCCOnly.setTitleSize(14);
        for (int t = 0; t < 48; t++) {
            oneHitHTCCOnly.cd(t);
            oneHitHTCCOnly.draw(hiNphePMTOneHit.get(t));
        }
        this.save(oneHitHTCCOnly, "HTCC_nphe");

        for (int t = 0; t < 48; t++) {
            oneHitHTCCOnly.cd(t);
            oneHitHTCCOnly.draw(hiTimePMTOneHit.get(t));
            timeIndPMT.get(t).setParameter(0, hiTimePMTOneHit.get(t).getMax());
            double maxV = hiTimePMTOneHit.get(t).getMaximumBin();
            maxV = lowTime + (maxV + 0.5) * (highTime - lowTime) / nBinsTime;
            timeIndPMT.get(t).setParameter(1, maxV);
            timeIndPMT.get(t).setParameter(2, 0.6);
            timeIndPMT.get(t).setRange(maxV - 1, maxV + 1.3);
            oneHitHTCCOnly.draw(hiTimePMTOneHit.get(t));
            oneHitHTCCOnly.getPad(t).getAxisX().setRange(maxV - 10, maxV + 10);
            DataFitter.fit(timeIndPMT.get(t), hiTimePMTOneHit.get(t), "Q");
            oneHitHTCCOnly.draw(timeIndPMT.get(t), "same");
        }
        this.save(oneHitHTCCOnly, "HTCC_timing");

        EmbeddedCanvas allC = new EmbeddedCanvas();
        allC.setSize(1200, 600);
        allC.divide(2,1);
        allC.cd(0);
        allC.draw(npheAll);
        allC.setSize(600, 600);
        allC.cd(1);
        allC.draw(timeAll);

        F1D timeAllFit = new F1D("timeAllFit", "[amp]*gaus(x,[mean],[sigma])", -1, 1);
        timeAllFit.setRange(-1, 1);
        timeAllFit.setParameter(0, 20000);
        timeAllFit.setParameter(1, 0);
        timeAllFit.setParameter(2, 1);
        timeAllFit.setLineColor(2);
        timeAllFit.setLineWidth(2);
        timeAllFit.setOptStat("1100");
        DataFitter.fit(timeAllFit, timeAll, "Q");
        allC.draw(timeAllFit, "same");

        this.save(allC, "HTCC_e");
    }

    public void save(EmbeddedCanvas canvas, String name) {
        canvas.save(String.format(outputDir+"/"+name+".png"));
        System.out.println(String.format("saved "+outputDir+"/"+name+".png"));
    }
    
    public int isSingle(double theta, double phi) {
        int single = 0;
        int isSigleTheta = 0;
        int isSiglePhi = 0;
        double resPhi = (phi + 166.0) % 30;
        if ((theta > 8 && theta < 9) || (theta > 16 && theta < 17) || (theta > 23 && theta < 24) || (theta > 31 && theta < 32)) {
            isSigleTheta = 1;
        }
        if (resPhi < 2 && resPhi > -2) {
            isSiglePhi = 1;
        }
        return isSigleTheta * isSiglePhi;
    }

    public void processEvent(DataEvent event
    ) {
        double startTime = 0;
        double htccTime = 0;
        int sector = 0;
        int layer = 0;
        int segment = 0;
        double deltaTime = 0;
        int halfSector = 0;
        int ring = 0;
        int pmt = 0;

        if (event.hasBank("REC::Particle") == true && event.hasBank("REC::Cherenkov") == true && event.hasBank("REC::Event") == true && event.hasBank("HTCC::rec")) {
            DataBank recBankPart = event.getBank("REC::Particle");
            DataBank recDeteHTCC = event.getBank("REC::Cherenkov");
            DataBank recEvenEB = event.getBank("REC::Event");
            DataBank recHTCC = event.getBank("HTCC::rec");
            startTime = recEvenEB.getFloat("startTime", 0);
            DataBank configBank = event.getBank("RUN::config");
            runNumber = configBank.getInt("run", 0);
            for (int loopE = 0; loopE < 1; loopE++) {
                double px = recBankPart.getFloat("px", loopE);
                double py = recBankPart.getFloat("py", loopE);
                double pz = recBankPart.getFloat("pz", loopE);
                double p = Math.sqrt(px * px + py * py + pz * pz);
                double vz = recBankPart.getFloat("vz", loopE);
                int status = recBankPart.getInt("status", 0);
                if (recBankPart.getInt("pid", loopE) == 11 && p > 1.5 && status < -1999 && status > -4000 && vz > -10 && vz < 10) {
                    for (int j = 0; j < recDeteHTCC.rows(); j++) {
                        if (recDeteHTCC.getShort("pindex", j) == loopE && recDeteHTCC.getByte("detector", j) == 15) {
                            double nphe = recDeteHTCC.getFloat("nphe", j);
                            double thetaHTCC = Math.toDegrees(recHTCC.getFloat("theta", recDeteHTCC.getInt("index", j)));
                            double phiHTCC = Math.toDegrees(recHTCC.getFloat("phi", recDeteHTCC.getInt("index", j)));
                            double timeCC = recDeteHTCC.getFloat("time", j);
                            double pathCC = recDeteHTCC.getFloat("path", j);
                            //npheAll.fill(nphe); moved this command within the loop below per Dan's request, Apr 2023
                            if (returnNHits(thetaHTCC, phiHTCC) == 1) {
                                double deltaTimeCC = timeCC - pathCC/PhysicsConstants.speedOfLight() - startTime;
                                halfSector = returnHalfSector(phiHTCC);
                                ring = returnRing(thetaHTCC);
                                pmt = returnPMT(ring, halfSector);
                                hiNphePMTOneHit.get(pmt - 1).fill(nphe);
                                hiTimePMTOneHit.get(pmt - 1).fill(deltaTimeCC);
                                timeAll.fill(deltaTimeCC);
				npheAll.fill(nphe);
                            }
                        }
                    }
                }

            }
        }

    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GStyle.setPalette("kRainBow");
        int count = 0;
        int runNum = 0;
        boolean useTB = true;
        String filelist = "list_of_files.txt";
        if (args.length > 0) {
            runNum = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            filelist = args[1];
        }
        int maxevents = 20000000;
        if (args.length > 2) {
            maxevents = Integer.parseInt(args[2]);
        }
        double Eb = 10.2;//10.6f;
        if (args.length > 3) {
            Eb = Double.parseDouble(args[3]);
        }
        if (args.length > 4) {
            if (Integer.parseInt(args[4]) == 0) {
                useTB = false;
            }
        }
        String outputDir = runNum > 0 ? "plots"+runNum : "plots";
        HTCC ana = new HTCC(runNum, outputDir);
        List<String> toProcessFileNames = new ArrayList<String>();
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
        int progresscount = 0;
        int filetot = toProcessFileNames.size();
        for (String runstrg : toProcessFileNames) {
            if (count < maxevents) {
                progresscount++;
                System.out.println(String.format(">>>>>>>>>>>>>>>> %s", runstrg));
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
                        System.out.println(count / 1000 + "k events (this is HTCC analysis on " + runstrg + ") ; progress : " + progresscount + "/" + filetot);
                    }
                }
                reader.close();
            }
        }
        System.out.println("Total : " + count + " events");
        ana.plot();
        ana.write();
    }

    public void write() {
        TDirectory dirout = new TDirectory();
        dirout.mkdir("/HTCC/");
        dirout.cd("/HTCC/");
        for (int s = 0; s < 48; s++) {
            dirout.addDataSet(hiNphePMTOneHit.get(s),hiTimePMTOneHit.get(s));
        }
        dirout.addDataSet(timeAll, npheAll);

        if(runNumber>0) dirout.writeFile(outputDir+"/out_HTCC_"+runNumber+".hipo");
        else            dirout.writeFile(outputDir+"/out_HTCC.hipo");
    }
}
