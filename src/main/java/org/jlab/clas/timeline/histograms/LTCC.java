package org.jlab.clas.timeline.histograms;

import org.jlab.groot.data.H1F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;


// Issue 510: Removed unused histograms to save only called by timeline step.
// For the full version, please refer to the previous version, such as e1b4bf2d0f70ade26bf70f67bab26554a62e6511.

public class LTCC{
  boolean userTimeBased;
  int runNum;
  public String outputDir;

  public H1F[] H_Particle_PiPlus_nphe_LTCC_S;   // related timeline: ['ltcc_had_nphe_sector']
  public H1F[] H_Particle_PiMinus_nphe_LTCC_S;  // related timeline: ['ltcc_had_nphe_sector']

  public LTCC(int reqR, String reqOutputDir, float reqEb, boolean reqTimeBased){
    runNum = reqR;userTimeBased=reqTimeBased;
    outputDir = reqOutputDir;
    H_Particle_PiPlus_nphe_LTCC_S = new H1F[6];
    H_Particle_PiMinus_nphe_LTCC_S = new H1F[6];

    for(int s=0;s<6;s++){
      H_Particle_PiPlus_nphe_LTCC_S[s] = new H1F(String.format("H_piplus_S%d_nphe",s+1), "LTCC nphe #pi^+ Particles",100,0,100);
      H_Particle_PiPlus_nphe_LTCC_S[s].setTitle(String.format("LTCC nphe #pi^+ Particles_S%d",s+1));
      H_Particle_PiPlus_nphe_LTCC_S[s].setTitleX("nphe");
      H_Particle_PiPlus_nphe_LTCC_S[s].setTitleY("counts");
      H_Particle_PiMinus_nphe_LTCC_S[s] = new H1F(String.format("H_piminus_S%d_nphe",s+1), "LTCC nphe #pi^- Particles",100,0,100);
      H_Particle_PiMinus_nphe_LTCC_S[s].setTitle(String.format("LTCC nphe #pi^- Particles_S%d",s+1));
      H_Particle_PiMinus_nphe_LTCC_S[s].setTitleX("nphe");
      H_Particle_PiMinus_nphe_LTCC_S[s].setTitleY("counts");
    }
  }

  public int isLTCCmatch(DataBank LTCCbank, int index){
    int indexltcc=-1;
    if(userTimeBased){
      for(int l = 0; l < LTCCbank.rows(); l++) {
        if(LTCCbank.getShort("pindex",l)==index && LTCCbank.getByte("detector",l)==16){
          indexltcc=l;
        }
      }
    }
    return indexltcc;
  }

  public int isDCmatch(DataEvent event, int index){
    int sectordc=-1;
    if(userTimeBased && event.hasBank("REC::Track")){
      DataBank DCbank = event.getBank("REC::Track");
      for(int l = 0; l < DCbank.rows(); l++) {
        if(DCbank.getShort("pindex",l)==index && DCbank.getInt("detector",l)==6){
          sectordc=DCbank.getByte("sector",l);
        }
      }
    }
    return sectordc;
  }

  public void processEvent(DataEvent event){
    if(event.hasBank("RUN::config")){
      DataBank partBank = null, cherenkovBank = null;
      if(userTimeBased){
        if(event.hasBank("REC::Particle"))partBank = event.getBank("REC::Particle");
        if(event.hasBank("REC::Cherenkov"))cherenkovBank = event.getBank("REC::Cherenkov");
      }
      if(!userTimeBased){
        if(event.hasBank("RECHB::Particle"))partBank = event.getBank("RECHB::Particle");
        if(event.hasBank("RECHB::Cherenkov"))cherenkovBank = event.getBank("RECHB::Cherenkov");
      }
      if(partBank!=null && cherenkovBank!=null) fillPions_LTCC(event,partBank,cherenkovBank);
    }
  }


  public void fillPions_LTCC(DataEvent event, DataBank part, DataBank ltcc){
    for(int i=0;i<part.rows();i++) {
      float nphe = 0;
      int pid = -100;
      int charge = -100;
      float mom;
      int status = part.getShort("status", i);
      if (status<0) status = -status;
      pid = part.getInt("pid", i);
      charge = part.getByte("charge", i);
      float px = part.getFloat("px", i);
      float py = part.getFloat("py", i);
      float pz = part.getFloat("pz", i);
      int sector = isDCmatch(event,i);
      if ((status>=2000 && status<4000) && isLTCCmatch(ltcc,i) != -1 && (pid == 211 || pid == -211)) {
        nphe = ltcc.getFloat("nphe",isLTCCmatch(ltcc,i));
        mom = (float)Math.sqrt(px*px+py*py+pz*pz);
        if (nphe >0. && charge == 1 && pid == 211 && mom > 4.) {
          H_Particle_PiPlus_nphe_LTCC_S[sector-1].fill(nphe);
        }
        if (nphe >0. && charge == -1 && pid == -211 && mom > 4.) {
          H_Particle_PiMinus_nphe_LTCC_S[sector-1].fill(nphe);
        }
      }
    }
  }

  public void write() {
    TDirectory dirout = new TDirectory();
    dirout.mkdir("/LTCC/");
    dirout.cd("/LTCC/");
    for(int s=0;s<6;s++){
      dirout.addDataSet(H_Particle_PiPlus_nphe_LTCC_S[s], H_Particle_PiMinus_nphe_LTCC_S[s]);
    }

    if(runNum>0) dirout.writeFile(outputDir+"/out_LTCC_"+runNum+".hipo");
    else         dirout.writeFile(outputDir+"/out_LTCC.hipo");
  }
}
