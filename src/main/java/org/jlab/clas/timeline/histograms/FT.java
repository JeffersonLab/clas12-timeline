package org.jlab.clas.timeline.histograms;

import java.util.*;
import org.jlab.clas.pdg.PhysicsConstants;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.math.F1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.clas.physics.Particle;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.ConstantsManager;

// Issue 510: Removed unused histograms to save only called by timeline step.
// For the full version, please refer to the previous version, such as e1b4bf2d0f70ade26bf70f67bab26554a62e6511.

public class FT {

    boolean userTimeBased;
    public int runNum;
    public String outputDir;
    public int crate;

    public boolean hasRF;
    public int triggerPID;
    public double triggerVZ;
    public double startTime, rfTime;

    public double rfPeriod;
    public int rf_large_integer;

    //Hodoscope
    public H1F[] hi_hodo_ematch;        // related timeline: ['fth_MIPS_energy', 'fth_MIPS_energy_board']
    public H1F[] hi_hodo_tmatch;        // related timeline: ['fth_MIPS_time', 'fth_MIPS_time_board']

    //Hodoscope by mezzanine board
    public H1F[] hi_hodo_ematch_board;  // related timeline: ['fth_MIPS_energy', 'fth_MIPS_energy_board']
    public H1F[] hi_hodo_tmatch_board;  // related timeline: ['fth_MIPS_time', 'fth_MIPS_time_board']

    //Calorimeter
    public H1F hi_cal_time_ch;          // related timeline: ['ftc_time_charged']
    public H1F hi_cal_time_neu;         // related timeline: ['ftc_time_neutral']

    //pi0
    public H1F hpi0sum;                 // related timeline: ['ftc_pi0_mass']

    public IndexedTable InverseTranslationTable;
    public IndexedTable calibrationTranslationTable;
    public IndexedTable rfTable;

    public ConstantsManager ccdb;

    public FT(int reqrunNum, String reqOutputDir, boolean reqTimeBased) {
        runNum = reqrunNum;
        outputDir = reqOutputDir;
        userTimeBased = reqTimeBased;

        rfPeriod = 4.008;
        ccdb = new ConstantsManager();
        ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo", "/calibration/eb/rf/config"}));
        rfTable = ccdb.getConstants(runNum, "/calibration/eb/rf/config");
        if (rfTable.hasEntry(1, 1, 1)) {
            System.out.println(String.format("RF period from ccdb for run %d: %f", runNum, rfTable.getDoubleValue("clock", 1, 1, 1)));
            rfPeriod = rfTable.getDoubleValue("clock", 1, 1, 1);
        }
        rf_large_integer = 1000;

        //Hodoscope Histograms
        hi_hodo_ematch = new H1F[2];
        hi_hodo_tmatch = new H1F[2];
        hi_hodo_ematch_board = new H1F[30];
        hi_hodo_tmatch_board = new H1F[30];
        int counter = 0;
        for (int layer = 0; layer < 2; layer++) {
            hi_hodo_ematch[layer] = new H1F(String.format("hi_hodo_ematch_l%d", layer + 1), String.format("hi_hodo_ematch_l%d", layer + 1), 200, 0, 10);
            hi_hodo_ematch[layer].setTitleX(String.format("E (MeV)"));
            hi_hodo_ematch[layer].setTitleY(String.format("Counts"));
            hi_hodo_ematch[layer].setFillColor(3);
            hi_hodo_tmatch[layer] = new H1F(String.format("hi_hodo_tmatch_l%d", layer + 1), String.format("hi_hodo_tmatch_l%d", layer + 1), 400, -20, 20);
            hi_hodo_tmatch[layer].setTitleX(String.format("T-T_start (ns)"));
            hi_hodo_tmatch[layer].setTitleY(String.format("Counts"));
            hi_hodo_tmatch[layer].setFillColor(3);

            for (int board = 0; board < 15; board++) {
                counter = 15 * layer + board;
                hi_hodo_ematch_board[counter] = new H1F(String.format("hi_hodo_ematch_l%d_b%d", layer + 1, board + 1), String.format("hi_hodo_eall_l%d_b%d", layer + 1, board + 1), 200, 0, 10);
                hi_hodo_ematch_board[counter].setTitleX(String.format("E (MeV)"));
                hi_hodo_ematch_board[counter].setTitleY(String.format("Counts"));
                hi_hodo_ematch_board[counter].setFillColor(3);
                hi_hodo_tmatch_board[counter] = new H1F(String.format("hi_hodo_tmatch_l%d_b%d", layer + 1, board + 1), String.format("hi_hodo_tmatch_l%d_b%d", layer + 1, board + 1), 200, -50, 50);
                hi_hodo_tmatch_board[counter].setTitleX(String.format("T-T_start (ns)"));
                hi_hodo_tmatch_board[counter].setTitleY(String.format("Counts"));
                hi_hodo_tmatch_board[counter].setFillColor(3);
            }
        }

        //Calorimeter Histograms
        hi_cal_time_ch = new H1F("hi_cal_time_ch", "T-T_RF(ns)", "Counts", 200, -rfPeriod / 2, rfPeriod / 2);
        hi_cal_time_ch.setFillColor(33);
        hi_cal_time_neu = new H1F("hi_cal_time_neu", "T-T_start(ns)", "Counts", 100, -2, 2);
        hi_cal_time_neu.setFillColor(44);

        //Pi0 Histograms
        hpi0sum = new H1F("hpi0sum", 200, 50.0, 250.0);
        hpi0sum.setTitleX("M (MeV)");
        hpi0sum.setTitleY("Counts");
        hpi0sum.setTitle("2#gamma invariant mass");
        hpi0sum.setFillColor(3);

        crate = 72;
        InverseTranslationTable = new CalibrationConstants(3,
                "crate/I:"
                +//3
                "slot/I:"
                +//4
                "chan/I");
        calibrationTranslationTable = ccdb.getConstants(runNum, "/daq/tt/fthodo");

        for (int slotn = 3; slotn < 20; slotn++) {
            for (int chann = 0; chann < 16; chann++) {
                if (!calibrationTranslationTable.hasEntry(crate, slotn, chann)) {
                    continue;
                }
                int secn = calibrationTranslationTable.getIntValue("sector", crate, slotn, chann);
                int layn = calibrationTranslationTable.getIntValue("layer", crate, slotn, chann);
                int compn = calibrationTranslationTable.getIntValue("component", crate, slotn, chann);
                //System.out.println("about to add Entry "+secn+" "+layn+" "+compn);
                InverseTranslationTable.addEntry(secn, layn, compn);
                InverseTranslationTable.setIntValue(crate, "crate", secn, layn, compn);
                InverseTranslationTable.setIntValue(slotn, "slot", secn, layn, compn);
                InverseTranslationTable.setIntValue(chann, "chan", secn, layn, compn);
                //System.out.println("Added Entry "+secn+" "+layn+" "+compn);
            }
        }
    }

    public void fillFTHodo(DataBank HodoHits, DataBank HodoClusters, DataBank ftParticles) {
        for (int i = 0; i < HodoHits.rows(); i++) {
            int hodoS = HodoHits.getByte("sector", i);
            int hodoL = HodoHits.getByte("layer", i);
            int component = HodoHits.getShort("component", i);
            int tile = -1;
            int slot = InverseTranslationTable.getIntValue("slot", hodoS, hodoL, component);
            int board = slot - 3; //mezzanine board number = slot-3
            if (slot > 12) {
                board = board - 2; //slot skips 10->13.
            }            // System.out.println(String.format("%d\t%d\t%d\t%d\t%d",board,slot,hodoS, hodoL, component )); // debuggin line
            int counter = 15 * hodoL - 15 + board; //board runs from 0 to 14.
            switch (hodoS) {
                case 1:
                    tile = component + 0;
                    break;
                case 2:
                    tile = component + 9;
                    break;
                case 3:
                    tile = component + 29;
                    break;
                case 4:
                    tile = component + 38;
                    break;
                case 5:
                    tile = component + 58;
                    break;
                case 6:
                    tile = component + 67;
                    break;
                case 7:
                    tile = component + 87;
                    break;
                case 8:
                    tile = component + 96;
                    break;
                default:
                    tile = -1;
                    break;
            }
            double hodoHitE = HodoHits.getFloat("energy", i);
            double hodoHitT = HodoHits.getFloat("time", i);
            double hodoHitX = HodoHits.getFloat("x", i);
            double hodoHitY = HodoHits.getFloat("y", i);
            double hodoHitZ = HodoHits.getFloat("z", i);
            int clusterId = HodoHits.getShort("clusterID", i);

            for (int j = 0; j < HodoClusters.rows(); j++) {
                if (clusterId == HodoClusters.getShort("id", j) && HodoClusters.getShort("size", j) > 1) {
                    hi_hodo_ematch[hodoL - 1].fill(hodoHitE);
                    hi_hodo_ematch_board[counter].fill(hodoHitE);
                    int charge = 0;
                    double vz = 0;
                    for (int k = 0; k < ftParticles.rows(); k++) {
                        if (ftParticles.getShort("hodoID", k) == clusterId) {
                            charge = 1;
                            vz = ftParticles.getFloat("vz", k);
                            vz = triggerVZ;
                            break;
                        }
                    }
                    double path = Math.sqrt(hodoHitX * hodoHitX + hodoHitY * hodoHitY + (hodoHitZ - vz) * (hodoHitZ - vz));
                    if (startTime > -100 && charge == 1 && triggerPID == 11) {
                        hi_hodo_tmatch[hodoL - 1].fill(hodoHitT - path / PhysicsConstants.speedOfLight() - startTime);
                        hi_hodo_tmatch_board[counter].fill(hodoHitT - path / PhysicsConstants.speedOfLight() - startTime);
                    }
                }
            }
        }
    }

    public void fillFTCalo(DataBank ftPart, DataBank CalClusters) {
        List<Particle> gammas = new ArrayList<>();
        for (int loop = 0; loop < ftPart.rows(); loop++) {
            int charge = ftPart.getByte("charge", loop);
            double energy = ftPart.getFloat("energy", loop);
            double time = ftPart.getFloat("time", loop);
            double cx = ftPart.getFloat("cx", loop);
            double cy = ftPart.getFloat("cy", loop);
            double cz = ftPart.getFloat("cz", loop);
            double vx = ftPart.getFloat("vx", loop);
            double vy = ftPart.getFloat("vy", loop);
            double vz = ftPart.getFloat("vz", loop);
            int calID = ftPart.getShort("calID", loop);
            double theta = Math.toDegrees(Math.acos(cz));

            int size = 0;
            double path = 0;
            vz = triggerVZ;
            for (int i = 0; i < CalClusters.rows(); i++) {
                if (calID == CalClusters.getShort("id", i)) {
                    size = CalClusters.getInt("size", i);
                    double x = CalClusters.getFloat("x", i) - vx;
                    double y = CalClusters.getFloat("y", i) - vy;
                    double z = CalClusters.getFloat("z", i) - vz;
                    path = Math.sqrt(x * x + y * y + z * z);
                    cx = x/path;
                    cy = y/path;
                    cz = z/path;
                    time = CalClusters.getFloat("time", i) - path / PhysicsConstants.speedOfLight();
                    break;
                }
            }
            boolean good = energy > 0.5 && size > 3 && theta > 2.5 && theta < 4.5;

            if (charge != 0) {
                if (rfTime != -1000 && good) {
                    hi_cal_time_ch.fill((time - rfTime + (rf_large_integer + 0.5) * rfPeriod) % rfPeriod - 0.5 * rfPeriod);
                }
            } else {
                Particle recParticle = new Particle(22, energy * cx, energy * cy, energy * cz, vx, vy, vz);
                if (energy > 0.5 && size > 3) {
                    gammas.add(recParticle);
                }
                if (startTime != -1000 && triggerPID == 11 && good) {
                    hi_cal_time_neu.fill(time - startTime);
                }
            }
        }

        if (gammas.size() >= 2) {
            for (int i1 = 0; i1 < gammas.size(); i1++) {
                for (int i2 = i1 + 1; i2 < gammas.size(); i2++) {
                    Particle partGamma1 = gammas.get(i1);
                    Particle partGamma2 = gammas.get(i2);
                    Particle partPi0 = new Particle();
                    partPi0.copy(partGamma1);
                    partPi0.combine(partGamma2, +1);
                    double invmass = Math.sqrt(partPi0.mass2());
                    // double x = (partGamma1.p() - partGamma2.p()) / (partGamma1.p() + partGamma2.p());
                    double angle = Math.toDegrees(Math.acos(partGamma1.cosTheta(partGamma2)));
                    if (angle > 2.5) {
                        hpi0sum.fill(invmass * 1000);
                    }
                }
            }
        }

    }

    public void processEvent(DataEvent event) {

        startTime = -1000;
        rfTime = -1000;
        triggerPID = 0;
        DataBank recBankEB = null;
        DataBank recEvenEB = null;
        DataBank ftParticles = null;
        DataBank ftCalClusters = null;
        DataBank ftHodoClusters = null;
        DataBank ftHodoHits = null;
        if (event.hasBank("REC::Particle")) {
            recBankEB = event.getBank("REC::Particle");
        }
        if (event.hasBank("REC::Event")) {
            recEvenEB = event.getBank("REC::Event");
        }
        if (event.hasBank("FT::particles")) {
            ftParticles = event.getBank("FT::particles");
        }
        if (event.hasBank("FTCAL::clusters")) {
            ftCalClusters = event.getBank("FTCAL::clusters");
        }
        if (event.hasBank("FTHODO::clusters")) {
            ftHodoClusters = event.getBank("FTHODO::clusters");
        }
        if (event.hasBank("FTHODO::hits")) {
            ftHodoHits = event.getBank("FTHODO::hits");
        }

        //Get event start time
        if (recEvenEB != null) {
            startTime = recEvenEB.getFloat("startTime", 0);
            rfTime = recEvenEB.getFloat("RFTime", 0);
        }

        //Get trigger particle PID and vz
        if (recBankEB != null && recBankEB.getShort("status",0)<-2000) {
            triggerPID = recBankEB.getInt("pid", 0);
            triggerVZ  = recBankEB.getFloat("vz", 0);
        }

        //Main Processing
        if (ftParticles != null && triggerPID!=0) {
            if (ftHodoHits != null && ftHodoClusters != null) {
                fillFTHodo(ftHodoHits, ftHodoClusters, ftParticles);
            }
            if (ftCalClusters != null) {
                fillFTCalo(ftParticles, ftCalClusters);
            }
        } //End if ftParticle is not null

    }


    public void write() {
        TDirectory dirout = new TDirectory();
        dirout.mkdir("/ft/");
        dirout.cd("/ft/");
        int counter;
        for (int s = 0; s < 2; s++) {
            dirout.addDataSet(hi_hodo_ematch[s], hi_hodo_tmatch[s]);
            for (int board = 0; board < 15; board++) {
                counter = 15 * s + board;
                dirout.addDataSet(hi_hodo_ematch_board[counter], hi_hodo_tmatch_board[counter]);
            }
        }
        dirout.addDataSet(hi_cal_time_ch, hi_cal_time_neu, hpi0sum);

        if (runNum > 0) {
            dirout.writeFile(outputDir + "/out_FT_" + runNum + ".hipo");
        } else {
            dirout.writeFile(outputDir + "/out_FT.hipo");
        }
    }
}
