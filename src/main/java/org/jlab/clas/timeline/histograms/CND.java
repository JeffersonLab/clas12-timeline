package org.jlab.clas.timeline.histograms;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;

// Issue 510: Removed unused histograms to save only called by timeline step.
// For the full version, please refer to the previous version, such as e1b4bf2d0f70ade26bf70f67bab26554a62e6511.

public class CND {
  boolean userTimeBased;
  public int runNum;
  public String outputDir;

  public float STT;
  public float RF;	
  public float TimeJitter;

  public H2F[] H_CND_time_z_charged;  // related timeline: ['cnd_time_neg_vtP']
  public H2F[] DiffZCND;              // related timeline: ['cnd_zdiff']
  public H1F[] H_CND_alignE;          // related timeline: ['cnd_MIPS_dE_dz']
  

  public CND(int reqrunNum, String reqOutputDir, boolean reqTimeBased) {
    userTimeBased=reqTimeBased;
    runNum = reqrunNum;
    outputDir = reqOutputDir;

    H_CND_time_z_charged = new H2F[3];
    for(int iL=0;iL<3;iL++){
      H_CND_time_z_charged[iL] = new H2F(String.format("H_CND_time_z_charged_L%d",iL+1),"H_CND_time_z_charged",50,0,40,100,-3,3);
      H_CND_time_z_charged[iL].setTitle("CND vt vs z (negative tracks) (layer "+(iL+1)+")");
      H_CND_time_z_charged[iL].setTitleX("CND z");
      H_CND_time_z_charged[iL].setTitleY("CND vt");

      DiffZCND[iL] = new H2F(String.format("Diff Z CND_L%d",iL+1),50,0,45,150,-10,10);	
      DiffZCND[iL].setTitle("DiffZ vs zCND (negative tracks) (layer "+(iL+1)+")");
    }

    H_CND_alignE=new H1F[144];
    for(int layer=0;layer<3;layer++){
      for(int sector=0;sector<24;sector++){
        for(int comp=0;comp<2;comp++){
          H_CND_alignE[(comp*3)+layer+(sector*6)] = new H1F(String.format("CND_alignE_L%d_S%d_C%d",layer+1,sector+1,comp+1),"CND_alignE",40,0,6);
          H_CND_alignE[(comp*3)+layer+(sector*6)].setTitle("layer "+(layer+1)+" sector "+(sector+1)+" comp "+(comp+1));
          H_CND_alignE[(comp*3)+layer+(sector*6)].setTitleX("dE/dz");
        }
      }
    }
  }

  public void FillCND(DataBank CNDbank, DataBank CVTbank, DataBank PARTbank){


    int[] Tracks = new int[CVTbank.rows()];
    for(int i=0; i<Tracks.length; i++){
      Tracks[i]=0;
    }
    float vertexTrigger=PARTbank.getFloat("vz",0);


    for(int iCND=0;iCND<CNDbank.rows();iCND++){
      int layer = CNDbank.getInt("layer",iCND);
      int trkID = CNDbank.getInt("trkID",iCND);
      float e  = CNDbank.getFloat("energy",iCND);
      float z  = CNDbank.getFloat("z",iCND);
      float time = CNDbank.getFloat("time",iCND);
      int sector = CNDbank.getInt("sector",iCND);
      int comp = CNDbank.getInt("component",iCND);

      if(layer>0 && layer<4 && trkID>-1 && STT>-999){
        float tz = CNDbank.getFloat("tz",iCND);
        float path = CNDbank.getFloat("pathlength",iCND);
        float mom = CVTbank.getFloat("p",trkID);
        float vertex = CVTbank.getFloat("z0",trkID);
        int charge = CVTbank.getInt("q",trkID);
        float betaP = mom/(float)Math.sqrt(mom*mom+0.139f*0.139f);


        float vertexCorrCentral=vertex/29.92f;
        float vertexCorrForward=vertexTrigger/29.92f;

        float vtP = time - (STT-vertexCorrForward+vertexCorrCentral) - (path/29.92f/betaP);//-(vertex/29.92f);//- vertex/29.92f;
        float pathTH = CNDbank.getFloat("tlength",iCND);

        float timeC = (float) (time -STT);
        float betaCND = path/timeC/29.92f;	

        float mass2=mom*mom*(float)((1.f/(betaCND*betaCND))-1.f);

        if (charge==-1 && /*Math.sqrt(Math.abs(mass2))>0.4 &&*/ /*mass2>-0.2*0.2 &&*/ z<(15.+5*(layer-1)) && Math.abs(vtP)<1.5)
        {
          H_CND_time_z_charged[layer-1].fill(z,vtP);
          DiffZCND[layer-1].fill(z,z-tz);
        }

        //pi-
        if (charge==-1 && Math.sqrt(Math.abs(mass2))<0.38 && mass2>-0.35*0.35 && z<(15.+5*(layer-1)) && Math.abs(vtP)<1.5)
        {
          float dE = e/pathTH;
          H_CND_alignE[((comp-1)*3)+(layer-1)+((sector-1)*6)].fill(dE);
        }
      }

    }

  }

  public void processEvent(DataEvent event) {
    if(event.hasBank("REC::Event"))STT = event.getBank("REC::Event").getFloat("startTime",0);
    //else return;
    if(event.hasBank("REC::Event"))RF = event.getBank("REC::Event").getFloat("RFTime",0);
    if(event.hasBank("RUN::config"))TimeJitter = event.getBank("RUN::config").getLong("timestamp",0);
    //else return;
    if(event.hasBank("CND::hits") && event.hasBank("CVTRec::Tracks") && event.hasBank("REC::Particle"))FillCND(event.getBank("CND::hits"),event.getBank("CVTRec::Tracks"),event.getBank("REC::Particle"));
  }

  public void write(){
    TDirectory dirout = new TDirectory();
    dirout.mkdir("/cnd/");
    dirout.cd("/cnd/");

    for(int layer=0;layer<3;layer++){
      for(int sector=0;sector<24;sector++){
        for(int comp=0;comp<2;comp++){
          dirout.addDataSet(H_CND_alignE[(comp*3)+layer+(sector*6)]);
        }
      }
    }
    for(int iL=0;iL<3;iL++) dirout.addDataSet(H_CND_time_z_charged[iL],DiffZCND[iL]);


    if(runNum>0) dirout.writeFile(outputDir+"/out_CND_"+runNum+".hipo");
    else         dirout.writeFile(outputDir+"/out_CND.hipo");
  }

}
