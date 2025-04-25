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
import org.jlab.groot.base.GStyle;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.hipo.HipoDataSource;

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
    public H1F[] hi_hodo_eall, hi_hodo_ematch, hi_hodo_tmatch;
    public F1D[] f_charge_landau;
    public H2F[] hi_hodo_ematch_2D, hi_hodo_tmatch_2D;

    //Hodoscope by mezzanine board
    public H1F[] hi_hodo_ematch_board, hi_hodo_tmatch_board;
    public F1D[] f_charge_landau_board;

    //Calorimeter
    public H1F hi_cal_nclusters, hi_cal_clsize, hi_cal_clsize_ch, hi_cal_e_all, hi_cal_e_ch, hi_cal_e_neu, hi_cal_theta_ch, hi_cal_phi_ch, hi_cal_time_ch, hi_cal_time_cut_ch, hi_cal_time_neu, hi_cal_time_cut_neu;
    public H2F hi_cal_clsize_en, hi_cal_time_e_ch, hi_cal_time_theta_ch, hi_cal_time_e_neu, hi_cal_time_theta_neu;
    public F1D ftime_ch, ftime_neu;

    //pi0
    public H1F hpi0sum;
    public F1D fpi0;
    public H2F hmassangle;

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
        hi_hodo_eall = new H1F[2];
        hi_hodo_ematch = new H1F[2];
        hi_hodo_tmatch = new H1F[2];
        hi_hodo_ematch_2D = new H2F[2];
        hi_hodo_tmatch_2D = new H2F[2];
        f_charge_landau = new F1D[2];
        hi_hodo_ematch_board = new H1F[30];
        hi_hodo_tmatch_board = new H1F[30];
        f_charge_landau_board = new F1D[30];
        int counter = 0;
        for (int layer = 0; layer < 2; layer++) {
            hi_hodo_eall[layer] = new H1F(String.format("hi_hodo_eall_l%d", layer + 1), String.format("hi_hodo_eall_l%d", layer + 1), 200, 0, 10);
            hi_hodo_eall[layer].setTitleX("E (MeV)");
            hi_hodo_eall[layer].setTitleY("Counts");
            hi_hodo_eall[layer].setFillColor(4);
            hi_hodo_ematch[layer] = new H1F(String.format("hi_hodo_ematch_l%d", layer + 1), String.format("hi_hodo_ematch_l%d", layer + 1), 200, 0, 10);
            hi_hodo_ematch[layer].setTitleX(String.format("E (MeV)"));
            hi_hodo_ematch[layer].setTitleY(String.format("Counts"));
            hi_hodo_ematch[layer].setFillColor(3);
            f_charge_landau[layer] = new F1D(String.format("Landau_%d", layer + 1), "[amp]*landau(x,[mean],[sigma])+[p0]+[p1]*x", 0.5 * (layer + 1), 10.0);
            f_charge_landau[layer].setParameter(0, 0.0);
            f_charge_landau[layer].setParameter(1, 0.0);
            f_charge_landau[layer].setParameter(2, 1.0);
            f_charge_landau[layer].setParameter(3, 0.0);
            f_charge_landau[layer].setParameter(4, 0.0);
            f_charge_landau[layer].setOptStat(1111111);
            f_charge_landau[layer].setLineWidth(2);
            hi_hodo_ematch_2D[layer] = new H2F(String.format("hi_hodo_ematch_2D_l%d", layer + 1), String.format("hi_hodo_ematch_2D_l%d", layer + 1), 100, 0, 10, 118, 0, 118);
            hi_hodo_ematch_2D[layer].setTitleX("E (MeV)");
            hi_hodo_ematch_2D[layer].setTitleY("Tile");
            hi_hodo_tmatch[layer] = new H1F(String.format("hi_hodo_tmatch_l%d", layer + 1), String.format("hi_hodo_tmatch_l%d", layer + 1), 400, -20, 20);
            hi_hodo_tmatch[layer].setTitleX(String.format("T-T_start (ns)"));
            hi_hodo_tmatch[layer].setTitleY(String.format("Counts"));
            hi_hodo_tmatch[layer].setFillColor(3);
            hi_hodo_tmatch_2D[layer] = new H2F(String.format("hi_hodo_tmatch_2D_l%d", layer + 1), String.format("hi_hodo_tmatch_2D_l%d", layer + 1), 200, -20, 20, 118, 0, 118);
            hi_hodo_tmatch_2D[layer].setTitleX("T-T_start (ns)");
            hi_hodo_tmatch_2D[layer].setTitleY("Tile");

            for (int board = 0; board < 15; board++) {
                counter = 15 * layer + board;
                hi_hodo_ematch_board[counter] = new H1F(String.format("hi_hodo_ematch_l%d_b%d", layer + 1, board + 1), String.format("hi_hodo_eall_l%d_b%d", layer + 1, board + 1), 200, 0, 10);
                hi_hodo_ematch_board[counter].setTitleX(String.format("E (MeV)"));
                hi_hodo_ematch_board[counter].setTitleY(String.format("Counts"));
                hi_hodo_ematch_board[counter].setFillColor(3);
                f_charge_landau_board[counter] = new F1D(String.format("Landau_%d_%d", layer + 1, board + 1), "[amp]*landau(x,[mean],[sigma])+[p0]+[p1]*x", 0.5 * (layer + 1), 10.0);
                f_charge_landau_board[counter].setParameter(0, 0.0);
                f_charge_landau_board[counter].setParameter(1, 0.0);
                f_charge_landau_board[counter].setParameter(2, 1.0);
                f_charge_landau_board[counter].setParameter(3, 0.0);
                f_charge_landau_board[counter].setParameter(4, 0.0);
                f_charge_landau_board[counter].setOptStat(1111111);
                f_charge_landau_board[counter].setLineWidth(2);
                hi_hodo_tmatch_board[counter] = new H1F(String.format("hi_hodo_tmatch_l%d_b%d", layer + 1, board + 1), String.format("hi_hodo_tmatch_l%d_b%d", layer + 1, board + 1), 200, -50, 50);
                hi_hodo_tmatch_board[counter].setTitleX(String.format("T-T_start (ns)"));
                hi_hodo_tmatch_board[counter].setTitleY(String.format("Counts"));
                hi_hodo_tmatch_board[counter].setFillColor(3);
            }
        }

        //Calorimeter Histograms
        hi_cal_nclusters = new H1F("hi_cal_nclusters", "N. Clusters", "Counts", 5, 0, 5);
        hi_cal_nclusters.setFillColor(44);
        hi_cal_clsize = new H1F("hi_cal_clsize", "Cluster Size", "Counts", 25, 0, 25);
        hi_cal_clsize.setFillColor(44);
        hi_cal_clsize_ch = new H1F("hi_cal_clsize_ch", "Cluster Size", "Counts", 25, 0, 25);
        hi_cal_clsize_ch.setFillColor(44);
        hi_cal_clsize_en = new H2F("hi_cal_clsize_en", " ", 25, 0, 25, 100, 0, 12);
        hi_cal_clsize_en.setTitleX("Cluster size");
        hi_cal_clsize_en.setTitleY("E (GeV)");
        hi_cal_e_all = new H1F("hi_cal_e_all", "E (GeV)", "Counts", 200, 0, 12);
        hi_cal_e_all.setFillColor(4);
        hi_cal_e_ch = new H1F("hi_cal_e_ch", "E (GeV)", "Counts", 200, 0, 12);
        hi_cal_e_ch.setFillColor(2);
        hi_cal_e_neu = new H1F("hi_cal_e_neu", "E (GeV)", "Counts", 200, 0, 12);
        hi_cal_e_neu.setFillColor(3);
        hi_cal_theta_ch = new H1F("hi_cal_theta_ch", "#theta (deg)", "Counts", 100, 2, 6);
        hi_cal_theta_ch.setFillColor(2);
        hi_cal_phi_ch = new H1F("hi_cal_phi_ch", "#phi (deg)", "Counts", 100, -180, 180);
        hi_cal_phi_ch.setFillColor(2);
        hi_cal_time_ch = new H1F("hi_cal_time_ch", "T-T_RF(ns)", "Counts", 200, -rfPeriod / 2, rfPeriod / 2);
        hi_cal_time_ch.setFillColor(33);
        hi_cal_time_cut_ch = new H1F("hi_cal_time_cut_ch", "T-T_RF(ns)", "Counts", 200, -rfPeriod / 2, rfPeriod / 2);
        hi_cal_time_cut_ch.setFillColor(3);
        ftime_ch = new F1D("ftime_ch", "[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
        ftime_ch.setParameter(0, 0.0);
        ftime_ch.setParameter(1, 0.0);
        ftime_ch.setParameter(2, 2.0);
        ftime_ch.setLineWidth(2);
        ftime_ch.setOptStat("1111");
        hi_cal_time_e_ch = new H2F("hi_cal_time_e_ch", "hi_cal_time_e_ch", 100, 0.0, 12.0, 100, -rfPeriod / 2, rfPeriod / 2);
        hi_cal_time_e_ch.setTitleX("E (GeV)");
        hi_cal_time_e_ch.setTitleY("T-T_RF (ns)");
        hi_cal_time_theta_ch = new H2F("hi_cal_time_theta_ch", "hi_cal_time_theta_ch", 100, 2.0, 6.0, 100, -rfPeriod / 2, rfPeriod / 2);
        hi_cal_time_theta_ch.setTitleX("#theta (deg)");
        hi_cal_time_theta_ch.setTitleY("T-T_RF (ns)");
        hi_cal_time_neu = new H1F("hi_cal_time_neu", "T-T_start(ns)", "Counts", 100, -2, 2);
        hi_cal_time_neu.setFillColor(44);
        hi_cal_time_cut_neu = new H1F("hi_cal_time_cut_neu", "T-T_start(ns)", "Counts", 100, -2, 2);
        hi_cal_time_cut_neu.setFillColor(4);
        ftime_neu = new F1D("ftime_neu", "[amp]*gaus(x,[mean],[sigma])", -0.3, 0.3);
        ftime_neu.setParameter(0, 0.0);
        ftime_neu.setParameter(1, 0.0);
        ftime_neu.setParameter(2, 2.0);
        ftime_neu.setLineWidth(2);
        ftime_neu.setOptStat("1111");
        hi_cal_time_e_neu = new H2F("hi_cal_time_e_neu", "hi_cal_time_e_neu", 100, 0.0, 12.0, 100, -2, 2);
        hi_cal_time_e_neu.setTitleX("E (GeV)");
        hi_cal_time_e_neu.setTitleY("T-T_start (ns)");
        hi_cal_time_theta_neu = new H2F("hi_cal_time_theta_neu", "hi_cal_time_theta_neu", 100, 2.0, 6.0, 100, -2, 2);
        hi_cal_time_theta_neu.setTitleX("#theta (deg)");
        hi_cal_time_theta_neu.setTitleY("T-T_start (ns)");

        //Pi0 Histograms
        hpi0sum = new H1F("hpi0sum", 200, 50.0, 250.0);
        hpi0sum.setTitleX("M (MeV)");
        hpi0sum.setTitleY("Counts");
        hpi0sum.setTitle("2#gamma invariant mass");
        hpi0sum.setFillColor(3);
        fpi0 = new F1D("fpi0", "[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x", 80.0, 200.0);
        fpi0.setParameter(0, 0.0);
        fpi0.setParameter(1, 140.0);
        fpi0.setParameter(2, 2.0);
        fpi0.setParameter(3, 0.0);
        fpi0.setParameter(4, 0.0);
        fpi0.setLineWidth(2);
        fpi0.setOptStat("1111111");
        hmassangle = new H2F("hmassangle", 100, 0.0, 300.0, 100, 0.0, 6.0);
        hmassangle.setTitleX("M (MeV)");
        hmassangle.setTitleY("Angle (deg)");
        hmassangle.setTitle("Angle vs. Mass");

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
            hi_hodo_eall[hodoL - 1].fill(hodoHitE);

            for (int j = 0; j < HodoClusters.rows(); j++) {
                if (clusterId == HodoClusters.getShort("id", j) && HodoClusters.getShort("size", j) > 1) {
                    hi_hodo_ematch[hodoL - 1].fill(hodoHitE);
                    hi_hodo_ematch_2D[hodoL - 1].fill(hodoHitE, tile);
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
                        hi_hodo_tmatch_2D[hodoL - 1].fill(hodoHitT - path / PhysicsConstants.speedOfLight() - startTime, tile);
                    }
                }
            }
        }
    }

    public void fillFTCalo(DataBank ftPart, DataBank CalClusters) {
        List<Particle> gammas = new ArrayList<>();
        hi_cal_nclusters.fill(ftPart.rows());
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

            double energyR = 0;
            int size = 0;
            double path = 0;
            vz = triggerVZ;
            for (int i = 0; i < CalClusters.rows(); i++) {
                if (calID == CalClusters.getShort("id", i)) {
                    energyR = CalClusters.getFloat("recEnergy", i);
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
            hi_cal_clsize.fill(size);
            hi_cal_e_all.fill(energy);
            hi_cal_clsize_en.fill(size, energy);

            if (charge != 0) {
                hi_cal_clsize_ch.fill(size);
                hi_cal_e_ch.fill(energy);
                hi_cal_theta_ch.fill(theta);
                hi_cal_phi_ch.fill(Math.toDegrees(Math.atan2(cy, cx)));
                if (rfTime != -1000 && good) {
                    hi_cal_time_ch.fill((time - rfTime + (rf_large_integer + 0.5) * rfPeriod) % rfPeriod - 0.5 * rfPeriod);
                    if (energy > 2) {
                        hi_cal_time_cut_ch.fill((time - rfTime + (rf_large_integer + 0.5) * rfPeriod) % rfPeriod - 0.5 * rfPeriod);
                    }
                    hi_cal_time_e_ch.fill(energy, (time - rfTime + (rf_large_integer + 0.5) * rfPeriod) % rfPeriod - 0.5 * rfPeriod);
                    hi_cal_time_theta_ch.fill(theta, (time - rfTime + (rf_large_integer + 0.5) * rfPeriod) % rfPeriod - 0.5 * rfPeriod);
                }
            } else {
                Particle recParticle = new Particle(22, energy * cx, energy * cy, energy * cz, vx, vy, vz);
                if (energy > 0.5 && size > 3) {
                    gammas.add(recParticle);
                }
                hi_cal_e_neu.fill(energy);
                if (startTime != -1000 && triggerPID == 11 && good) {
                    hi_cal_time_neu.fill(time - startTime);
                    if (energy > 2) {
                        hi_cal_time_cut_neu.fill(time - startTime);
                    }
                    hi_cal_time_e_neu.fill(energy, time - startTime);
                    hi_cal_time_theta_neu.fill(Math.toDegrees(Math.acos(cz)), time - startTime);
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
                    hmassangle.fill(invmass * 1000, angle);
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

    public void analyze() {
        //Fit hodoscope charge
        for (int layer = 0; layer < 2; layer++) {
            initLandauFitPar(hi_hodo_ematch[layer], f_charge_landau[layer]);
            DataFitter.fit(f_charge_landau[layer], hi_hodo_ematch[layer], "LRQ");
            hi_hodo_ematch[layer].setFunction(null);
        }
        //Fit calorimeter time
        initTimeGaussFitPar(ftime_ch, hi_cal_time_cut_ch);
        DataFitter.fit(ftime_ch, hi_cal_time_cut_ch, "LQ");
        hi_cal_time_cut_ch.setFunction(null);
        initTimeGaussFitPar(ftime_neu, hi_cal_time_neu);
        double Mean = hi_cal_time_neu.getAxis().getBinCenter(hi_cal_time_neu.getMaximumBin());
        double Min = (Mean - 0.35);
        double Max = (Mean + 0.35);
        ftime_neu.setRange(Min, Max);
        DataFitter.fit(ftime_neu, hi_cal_time_neu, "LQ");
        hi_cal_time_neu.setFunction(null);
        //Fit pi0 mass
        double hAmp = hpi0sum.getBinContent(hpi0sum.getMaximumBin());
        double hMean = hpi0sum.getAxis().getBinCenter(hpi0sum.getMaximumBin());
        double hRMS = 10; //ns
        fpi0.setParameter(0, hAmp);
        if (hAmp != 0) {
            fpi0.setParLimits(0, hAmp * 0.8, hAmp * 1.2);
        }
        fpi0.setParameter(1, hMean);
        if (hRMS != 0) {
            fpi0.setParLimits(1, hMean - hRMS, hMean + hRMS);
        }
        DataFitter.fit(fpi0, hpi0sum, "LQ");
        hpi0sum.setFunction(null);
    }

    private void initLandauFitPar(H1F hcharge, F1D fcharge) {
        double hAmp = hcharge.getBinContent(hcharge.getMaximumBin());
        double hMean = hcharge.getAxis().getBinCenter(hcharge.getMaximumBin());
        fcharge.setRange(fcharge.getRange().getMin(), hMean * 2.0);
        fcharge.setParameter(0, hAmp);
        if (hAmp != 0) {
            fcharge.setParLimits(0, 0.5 * hAmp, 1.5 * hAmp);
        }
        fcharge.setParameter(1, hMean);
        if (hMean != 0) {
            fcharge.setParLimits(1, 0.8 * hMean, 1.2 * hMean);//Changed from 5-30        
        }
        fcharge.setParameter(2, 0.3);//Changed from 2
        fcharge.setParLimits(2, 0.1, 1);//Changed from 0.5-10
        fcharge.setParameter(3, 0.2 * hAmp);
        fcharge.setParameter(4, -0.3);//Changed from -0.2
    }

    private void initTimeGaussFitPar(F1D ftime, H1F htime) {
        double hAmp = htime.getBinContent(htime.getMaximumBin());
        double hMean = htime.getAxis().getBinCenter(htime.getMaximumBin());
        double hRMS = htime.getRMS(); //ns
        double rangeMin = (hMean - (3 * hRMS));
        double rangeMax = (hMean + (3 * hRMS));
        double pm = hRMS * 3;
        ftime.setRange(rangeMin, rangeMax);
        ftime.setParameter(0, hAmp);
        if (hAmp != 0) {
            ftime.setParLimits(0, hAmp * 0.8, hAmp * 1.2);
        }
        ftime.setParameter(1, hMean);
        if (pm != 0) {
            ftime.setParLimits(1, hMean - pm, hMean + (pm));
        }
        ftime.setParameter(2, 0.2);
        if (hRMS != 0) {
            ftime.setParLimits(2, 0.1 * hRMS, 0.8 * hRMS);
        }
    }

    public void write() {
        analyze();
        TDirectory dirout = new TDirectory();
        dirout.mkdir("/ft/");
        dirout.cd("/ft/");
        int counter;
        for (int s = 0; s < 2; s++) {
            dirout.addDataSet(hi_hodo_eall[s], hi_hodo_ematch[s], hi_hodo_ematch_2D[s], hi_hodo_tmatch[s], hi_hodo_tmatch_2D[s]);
            for (int board = 0; board < 15; board++) {
                counter = 15 * s + board;
                dirout.addDataSet(hi_hodo_ematch_board[counter], hi_hodo_tmatch_board[counter]);
            }
        }
        dirout.addDataSet(hi_cal_nclusters, hi_cal_clsize, hi_cal_clsize_ch, hi_cal_clsize_en, hi_cal_e_ch, hi_cal_e_all, hi_cal_theta_ch, hi_cal_phi_ch, hi_cal_time_ch, hi_cal_time_cut_ch, hi_cal_time_e_ch);
        dirout.addDataSet(hi_cal_time_theta_ch, hi_cal_time_neu, hi_cal_time_cut_neu, hi_cal_time_e_neu, hi_cal_time_theta_neu, hpi0sum, hmassangle);

        if (runNum > 0) {
            dirout.writeFile(outputDir + "/out_FT_" + runNum + ".hipo");
        } else {
            dirout.writeFile(outputDir + "/out_FT.hipo");
        }
    }
}
