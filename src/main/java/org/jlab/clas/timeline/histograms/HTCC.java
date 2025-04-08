package org.jlab.clas.timeline.histograms;

import java.util.*;
import org.jlab.clas.pdg.PhysicsConstants;

import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;

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

  public void processEvent(DataEvent event) {
    double startTime = 0;
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
