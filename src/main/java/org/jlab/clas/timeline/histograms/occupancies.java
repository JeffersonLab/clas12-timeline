package org.jlab.clas.timeline.histograms;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

public class occupancies {
  public int runNum;
  public String outputDir;
  int found_elec;
  float elec_mom, elec_th, elec_ph, elec_vz;
  H2F H_occ_e_th_ph, H_occ_e_th_p, H_occ_e_ph_p;
  H2F H_BST_sect_rec_occ;
  H2F H_BST_R1B, H_BST_R2B, H_BST_R3B;
  H2F H_BST_R1T, H_BST_R2T, H_BST_R3T;
  H2F H_BST_R1_phi_z, H_BST_R2_phi_z, H_BST_R3_phi_z;
  H2F H_BMT_occ;
  H1F[][] H_BMT_occ_LS;
  H2F H_BMT_R1_phi_z, H_BMT_R2_phi_z, H_BMT_R3_phi_z;
  H2F[] H_BMT_occ_StripLayer;
  H1F H_BST_occ_reg1_l1, H_BST_occ_reg1_l2, H_BST_occ_reg2_l1, H_BST_occ_reg2_l2, H_BST_occ_reg3_l1, H_BST_occ_reg3_l2;
  H1F H_BST_multi;

  H2F H_BST_R1_theta, H_BST_R2_theta, H_BST_R3_theta;
  H2F H_BMT_R1_theta, H_BMT_R2_theta, H_BMT_R3_theta;
  H2F H_BST_R1_phi, H_BST_R2_phi, H_BST_R3_phi;
  H2F H_BMT_R1_phi, H_BMT_R2_phi, H_BMT_R3_phi;

  H1F H_BMT_multi;

  public occupancies(int reqrunNum, String reqOutputDir) {
    runNum = reqrunNum;
    outputDir = reqOutputDir;
    H_BST_multi = new H1F("bst_multi", "bst_multi", 501, -0.5, 500.5);
    H_BST_multi.setTitleX("hit multiplicity");
    H_BST_multi.setTitleY("counts");
    H_BST_multi.setTitle("Multiplicity of BST channels");
    H_BST_occ_reg1_l1 = new H1F("bst_occ_reg1_l1", "bst_occ_reg1_l1", 2560, 0.5, 2560.5);
    H_BST_occ_reg1_l1.setTitleX("strip");
    H_BST_occ_reg1_l1.setTitleY("hits");
    H_BST_occ_reg1_l1.setTitle("region 1 - bottom layer");
    H_BST_occ_reg1_l2 = new H1F("bst_occ_reg1_l2", "bst_occ_reg1_l2", 2560, 0.5, 2560.5);
    H_BST_occ_reg1_l2.setTitleX("strip");
    H_BST_occ_reg1_l2.setTitleY("hits");
    H_BST_occ_reg1_l2.setTitle("region 1 - top layer");
    H_BST_occ_reg2_l1 = new H1F("bst_occ_reg2_l1", "bst_occ_reg2_l1", 3584, 0.5, 3584.5);
    H_BST_occ_reg2_l1.setTitleX("strip");
    H_BST_occ_reg2_l1.setTitleY("hits");
    H_BST_occ_reg2_l1.setTitle("region 2 - bottom layer");
    H_BST_occ_reg2_l2 = new H1F("bst_occ_reg2_l2", "bst_occ_reg2_l2", 3584, 0.5, 3584.5);
    H_BST_occ_reg2_l2.setTitleX("strip");
    H_BST_occ_reg2_l2.setTitleY("hits");
    H_BST_occ_reg2_l2.setTitle("region 2 - top layer");
    H_BST_occ_reg3_l1 = new H1F("bst_occ_reg3", "bst_occ_reg3_l1", 4608, 0.5, 4608.5);
    H_BST_occ_reg3_l1.setTitleX("strip");
    H_BST_occ_reg3_l1.setTitleY("hits");
    H_BST_occ_reg3_l1.setTitle("region 3 - bottom layer");
    H_BST_occ_reg3_l2 = new H1F("bst_occ_reg3_l2", "bst_occ_reg3_l2", 4608, 0.5, 4608.5);
    H_BST_occ_reg3_l2.setTitleX("strip");
    H_BST_occ_reg3_l2.setTitleY("hits");
    H_BST_occ_reg3_l2.setTitle("region 3 - top layer");
    H_occ_e_th_ph = new H2F("H_occ_e_th_ph","H_occ_e_th_ph",100,-180,180,100,5,35);
    H_occ_e_th_ph.setTitle("e^- #theta vs #phi");
    H_occ_e_th_ph.setTitleX("#phi (^o)");
    H_occ_e_th_ph.setTitleY("#theta (^o)");
    H_occ_e_th_p  = new H2F("H_occ_e_th_p","H_occ_e_th_p",100,0.75,10.5,100,5,35);
    H_occ_e_th_p.setTitle("e^- #theta vs p");
    H_occ_e_th_p.setTitleX("p (GeV)");
    H_occ_e_th_p.setTitleY("#theta (^o)");
    H_occ_e_ph_p  = new H2F("H_occ_e_ph_p","H_occ_e_ph_p",100,0.75,10.5,100,-180,180);
    H_occ_e_ph_p.setTitle("e^- #phi vs p");
    H_occ_e_ph_p.setTitleX("p (GeV)");
    H_occ_e_ph_p.setTitleY("#phi (^o)");
    H_BST_R1B = new H2F("H_BST_R1B","H_BST_R1B",250,0,250,11,0.5,11.5);
    H_BST_R1B.setTitle("region 1 bottom");
    H_BST_R1B.setTitleX("strip");
    H_BST_R1B.setTitleY("sector");
    H_BST_R2B = new H2F("H_BST_R2B","H_BST_R2B",250,0,250,15,0.5,15.5);
    H_BST_R2B.setTitle("region 2 bottom");
    H_BST_R2B.setTitleX("strip");
    H_BST_R2B.setTitleY("sector");
    H_BST_R3B = new H2F("H_BST_R3B","H_BST_R3B",250,0,250,18,0.5,18.5);
    H_BST_R3B.setTitle("region 3 bottom");
    H_BST_R3B.setTitleX("strip");
    H_BST_R3B.setTitleY("sector");
    H_BST_R1T = new H2F("H_BST_R1B","H_BST_R1B",250,0,250,11,0.5,11.5);
    H_BST_R1T.setTitle("region 1 top");
    H_BST_R1T.setTitleX("strip");
    H_BST_R1T.setTitleY("sector");
    H_BST_R2T = new H2F("H_BST_R2B","H_BST_R2B",250,0,250,15,0.5,15.5);
    H_BST_R2T.setTitle("region 2 top");
    H_BST_R2T.setTitleX("strip");
    H_BST_R2T.setTitleY("sector");
    H_BST_R3T = new H2F("H_BST_R3B","H_BST_R3B",250,0,250,18,0.5,18.5);
    H_BST_R3T.setTitle("region 3 top");
    H_BST_R3T.setTitleX("strip");
    H_BST_R3T.setTitleY("sector");
    H_BST_sect_rec_occ = new H2F("H_BST_sect_rec_occ","H_BST_sect_rec_occ",20,0.5,20.5,3,0.5,3.5);
    H_BST_sect_rec_occ.setTitle("BST region vs sect occ");
    H_BST_sect_rec_occ.setTitleX("sector");
    H_BST_sect_rec_occ.setTitleY("region");
    H_BST_R1_phi_z = new H2F("H_BST_R1_phi_z","H_BST_R1_phi_z",100,-25,25,100,-180,180);
    H_BST_R1_phi_z.setTitle("BST R1 #phi vs z");
    H_BST_R1_phi_z.setTitleX("z (mm)");
    H_BST_R1_phi_z.setTitleY("#phi (^o)");
    H_BST_R2_phi_z = new H2F("H_BST_R2_phi_z","H_BST_R2_phi_z",100,-25,25,100,-180,180);
    H_BST_R2_phi_z.setTitle("BST R2 #phi vs z");
    H_BST_R2_phi_z.setTitleX("z (mm)");
    H_BST_R2_phi_z.setTitleY("#phi (^o)");
    H_BST_R3_phi_z = new H2F("H_BST_R3_phi_z","H_BST_R3_phi_z",100,-25,25,100,-180,180);
    H_BST_R3_phi_z.setTitle("BST R3 #phi vs z");
    H_BST_R3_phi_z.setTitleX("z (mm)");
    H_BST_R3_phi_z.setTitleY("#phi (^o)");

    H_BMT_multi = new H1F("bmt_multi", "bmt_multi", 501, -0.5, 500.5);
    H_BMT_multi.setTitleX("hit multiplicity");
    H_BMT_multi.setTitleY("counts");
    H_BMT_multi.setTitle("Multiplicity of BMT channels");
    H_BMT_occ = new H2F("H_BMT_occ","H_BMT_occ",1400,0,1400,18,0.5,18.5);
    H_BMT_occ.setTitle("BMT occ");
    H_BMT_occ.setTitleX("strip");
    H_BMT_occ.setTitleY("detector");
    H_BMT_occ_LS = new H1F[6][3];
    H_BMT_occ_StripLayer = new H2F[3];
    int[] maxStripDet = {900,700,700,1100,800,1200};
    for(int l=0;l<6;l++){
      for(int s=0;s<3;s++){
        H_BMT_occ_LS[l][s] = new H1F(String.format("H_BMT_occ_L%dS%d",l+1,s+1),String.format("H_BMT_occ_L%dS%d",l+1,s+1),maxStripDet[l],0,maxStripDet[l]);
        H_BMT_occ_LS[l][s].setTitle(String.format("BMT occ Layer %d Sector %d",l+1,s+1));
        H_BMT_occ_LS[l][s].setTitleX(String.format("strip L%d S%d",l+1,s+1));
      }
    }
    for(int s=0;s<3;s++){
      H_BMT_occ_StripLayer[s] = new H2F(String.format("H_BMT_occ_S%d",s+1),String.format("H_BMT_occ_S%d",s+1),1200,0.5,1200.5,6,0.5,6.5);
      H_BMT_occ_StripLayer[s].setTitle(String.format("BMT occ Sector %d",s+1));
      H_BMT_occ_StripLayer[s].setTitleX("strip");
      H_BMT_occ_StripLayer[s].setTitleY("layer");
    }
    H_BMT_R1_phi_z = new H2F("H_BMT_R1_phi_z","H_BMT_R1_phi_z",100,-25,25,100,-180,180);
    H_BMT_R1_phi_z.setTitle("BMT R1 #phi vs z");
    H_BMT_R1_phi_z.setTitleX("z (mm)");
    H_BMT_R1_phi_z.setTitleY("#phi (^o)");
    H_BMT_R2_phi_z = new H2F("H_BMT_R2_phi_z","H_BMT_R2_phi_z",100,-25,25,100,-180,180);
    H_BMT_R2_phi_z.setTitle("BMT R2 #phi vs z");
    H_BMT_R2_phi_z.setTitleX("z (mm)");
    H_BMT_R2_phi_z.setTitleY("#phi (^o)");
    H_BMT_R3_phi_z = new H2F("H_BMT_R3_phi_z","H_BMT_R3_phi_z",100,-25,25,100,-180,180);
    H_BMT_R3_phi_z.setTitle("BMT R3 #phi vs z");
    H_BMT_R3_phi_z.setTitleX("z (mm)");
    H_BMT_R3_phi_z.setTitleY("#phi (^o)");

    H_BST_R1_theta = new H2F("H_BST_R1_theta","H_BST_R1_theta",100,0,90,100,5,25);
    H_BST_R1_theta.setTitle("BST R1 vs e^- #theta");
    H_BST_R1_theta.setTitleY("e^- #theta (^o)");
    H_BST_R1_theta.setTitleX("R1 #theta (^o)");
    H_BST_R2_theta = new H2F("H_BST_R2_theta","H_BST_R2_theta",100,0,90,100,5,25);
    H_BST_R2_theta.setTitle("BST R2 vs e^- #theta");
    H_BST_R2_theta.setTitleY("e^- #theta (^o)");
    H_BST_R2_theta.setTitleX("R2 #theta (^o)");
    H_BST_R3_theta = new H2F("H_BST_R3_theta","H_BST_R3_theta",100,0,90,100,5,25);
    H_BST_R3_theta.setTitle("BST R3 vs e^- #theta");
    H_BST_R3_theta.setTitleY("e^- #theta (^o)");
    H_BST_R3_theta.setTitleX("R3 #theta (^o)");
    H_BMT_R1_theta = new H2F("H_BMT_R1_theta","H_BMT_R1_theta",100,0,90,100,5,25);
    H_BMT_R1_theta.setTitle("BMT R1 vs e^- #theta");
    H_BMT_R1_theta.setTitleY("e^- #theta (^o)");
    H_BMT_R1_theta.setTitleX("R1 #theta (^o)");
    H_BMT_R2_theta = new H2F("H_BMT_R2_theta","H_BMT_R2_theta",100,0,90,100,5,25);
    H_BMT_R2_theta.setTitle("BMT R2 vs e^- #theta");
    H_BMT_R2_theta.setTitleY("e^- #theta (^o)");
    H_BMT_R2_theta.setTitleX("R2 #theta (^o)");
    H_BMT_R3_theta = new H2F("H_BMT_R3_theta","H_BMT_R3_theta",100,0,90,100,5,25);
    H_BMT_R3_theta.setTitle("BMT R3 vs e^- #theta");
    H_BMT_R3_theta.setTitleY("e^- #theta (^o)");
    H_BMT_R3_theta.setTitleX("R3 #theta (^o)");
    H_BST_R1_phi = new H2F("H_BST_R1_phi","H_BST_R1_phi",100,-180,180,100,-180,180);
    H_BST_R1_phi.setTitle("BST R1 vs e^- #phi");
    H_BST_R1_phi.setTitleX("e^- #phi (^o)");
    H_BST_R1_phi.setTitleY("R1 #phi (^o)");
    H_BST_R2_phi = new H2F("H_BST_R2_phi","H_BST_R2_phi",100,-180,180,100,-180,180);
    H_BST_R2_phi.setTitle("BST R2 vs e^- #phi");
    H_BST_R2_phi.setTitleX("e^- #phi (^o)");
    H_BST_R2_phi.setTitleY("R2 #phi (^o)");
    H_BST_R3_phi = new H2F("H_BST_R3_phi","H_BST_R3_phi",100,-180,180,100,-180,180);
    H_BST_R3_phi.setTitle("BST R3 vs e^- #phi");
    H_BST_R3_phi.setTitleX("e^- #phi (^o)");
    H_BST_R3_phi.setTitleY("R3 #phi (^o)");
    H_BMT_R1_phi = new H2F("H_BMT_R1_phi","H_BMT_R1_phi",100,-180,180,100,-180,180);
    H_BMT_R1_phi.setTitle("BMT R1 vs e^- #phi");
    H_BMT_R1_phi.setTitleX("e^- #phi (^o)");
    H_BMT_R1_phi.setTitleY("R1 #phi (^o)");
    H_BMT_R2_phi = new H2F("H_BMT_R2_phi","H_BMT_R2_phi",100,-180,180,100,-180,180);
    H_BMT_R2_phi.setTitle("BMT R2 vs e^- #phi");
    H_BMT_R2_phi.setTitleX("e^- #phi (^o)");
    H_BMT_R2_phi.setTitleY("R2 #phi (^o)");
    H_BMT_R3_phi = new H2F("H_BMT_R3_phi","H_BMT_R3_phi",100,-180,180,100,-180,180);
    H_BMT_R3_phi.setTitle("BMT R3 vs e^- #phi");
    H_BMT_R3_phi.setTitleX("e^- #phi (^o)");
    H_BMT_R3_phi.setTitleY("R3 #phi (^o)");
  }

  public void MakeBST_hits(DataBank bank, DataBank hits){
    H_BST_multi.fill(hits.rows());
    for(int k=0;k<bank.rows();k++){
      int S = bank.getByte("sector",k);
      int l = bank.getByte("layer",k);
      int comp = bank.getShort("component",k);
      int ADC = bank.getInt("ADC",k);
      //if (ADC != -1) {
      if (ADC >= 0) {
        if (l == 1) H_BST_occ_reg1_l1.fill((S-1)*256+comp);
        if (l == 3) H_BST_occ_reg2_l1.fill((S-1)*256+comp);
        if (l == 5) H_BST_occ_reg3_l1.fill((S-1)*256+comp);
        if (l == 2) H_BST_occ_reg1_l2.fill((S-1)*256+comp);
        if (l == 4) H_BST_occ_reg2_l2.fill((S-1)*256+comp);
        if (l == 6) H_BST_occ_reg3_l2.fill((S-1)*256+comp);

      }
      //System.out.println(S + " , " + l + " , " + s);
      }
    }

    public void MakeBMT_hits(DataBank bank){
      H_BMT_multi.fill(bank.rows());
      for(int k=0;k<bank.rows();k++){
        int S = bank.getByte("sector",k);
        int l = bank.getByte("layer",k);
        int s = bank.getShort("component",k);
        //System.out.println(S + " , " + l + " , " + s);
        H_BMT_occ.fill(s,S+(l-1)*3);
        if( S>0 && S<4 && l>0 && l<7 )H_BMT_occ_LS[l-1][S-1].fill(s);
        if( S>0 && S<4 && l>0 && l<7 ) H_BMT_occ_StripLayer[S-1].fill(s,l);

        else{System.out.println("BMT numbering error S="+S+" l="+l);}
        /*
           if(l==1)H_BST_R1B.fill(s,S);
           if(l==3)H_BST_R2B.fill(s,S);
           if(l==5)H_BST_R3B.fill(s,S);
           if(l==2)H_BST_R1T.fill(s,S);
           if(l==4)H_BST_R2T.fill(s,S);
           if(l==6)H_BST_R3T.fill(s,S);
           */
      }
    }

    public void MakeBST_crosses(DataBank bank){
      for(int k=0;k<bank.rows();k++){
        int    s = bank.getInt("sector",k);
        int    r = bank.getInt("region",k);
        float  x = bank.getFloat("x",k);
        float  y = bank.getFloat("y",k);
        float  z = bank.getFloat("z",k);
        // float ux = bank.getFloat("ux",k);
        // float uy = bank.getFloat("uy",k);
        // float uz = bank.getFloat("uz",k);
        //System.out.println("s="+s+" , r="+r+" , x="+x+" , y="+y+" , z="+z+" , ux="+ux+" , uy="+uy+" , uz="+uz);
        float DelPhi = (float)Math.toDegrees(Math.atan2(y,x)) - elec_ph;
        DelPhi += 180f;
        while(DelPhi>180f)DelPhi-=360f;
        while(DelPhi<-180f)DelPhi+=360f;
        H_BST_sect_rec_occ.fill(s,r);
        if(true /*Math.abs(DelPhi)<180*/){
          if(r==1)H_BST_R1_phi_z.fill(z,Math.toDegrees(Math.atan2(y,x)));
          if(r==2)H_BST_R2_phi_z.fill(z,Math.toDegrees(Math.atan2(y,x)));
          if(r==3)H_BST_R3_phi_z.fill(z,Math.toDegrees(Math.atan2(y,x)));
          if(found_elec==1){
            if(r==1)H_BST_R1_theta.fill(Math.toDegrees(Math.acos(z/Math.sqrt(x*x+y*y+z*z))),elec_th);
            if(r==2)H_BST_R2_theta.fill(Math.toDegrees(Math.acos(z/Math.sqrt(x*x+y*y+z*z))),elec_th);
            if(r==3)H_BST_R3_theta.fill(Math.toDegrees(Math.acos(z/Math.sqrt(x*x+y*y+z*z))),elec_th);
            if(r==1)H_BST_R1_phi.fill(elec_ph,Math.toDegrees(Math.atan2(y,x)));
            if(r==2)H_BST_R2_phi.fill(elec_ph,Math.toDegrees(Math.atan2(y,x)));
            if(r==3)H_BST_R3_phi.fill(elec_ph,Math.toDegrees(Math.atan2(y,x)));
          }
        }
      }
    }

    public void MakeBMT_crosses(DataBank bank){
      //System.out.println("MAKING BMT CROSSES");
      for(int k=0;k<bank.rows();k++){
        int    s = bank.getInt("sector",k);
        int    r = bank.getInt("region",k);
        float  x = bank.getFloat("x",k);
        float  y = bank.getFloat("y",k);
        float  z = bank.getFloat("z",k);
        // float ux = bank.getFloat("ux",k);
        // float uy = bank.getFloat("uy",k);
        // float uz = bank.getFloat("uz",k);
        float DelPhi = (float)Math.toDegrees(Math.atan2(y,x)) - elec_ph;
        DelPhi += 180f;
        while(DelPhi>180f)DelPhi-=360f;
        while(DelPhi<-180f)DelPhi+=360f;
        if( (!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z) && Math.abs(DelPhi)<180) ){
          //System.out.println("s="+s+" , r="+r+" , x="+x+" , y="+y+" , z="+z+" , ux="+ux+" , uy="+uy+" , uz="+uz);
          H_BST_sect_rec_occ.fill(s,r);
          if(r==1)H_BMT_R1_phi_z.fill(z,Math.toDegrees(Math.atan2(y,x)));
          if(r==2)H_BMT_R2_phi_z.fill(z,Math.toDegrees(Math.atan2(y,x)));
          if(r==3)H_BMT_R3_phi_z.fill(z,Math.toDegrees(Math.atan2(y,x)));
          if(found_elec==1){
            if(r==1)H_BMT_R1_theta.fill(Math.toDegrees(Math.acos(z/Math.sqrt(x*x+y*y+z*z))),elec_th);
            if(r==2)H_BMT_R2_theta.fill(Math.toDegrees(Math.acos(z/Math.sqrt(x*x+y*y+z*z))),elec_th);
            if(r==3)H_BMT_R3_theta.fill(Math.toDegrees(Math.acos(z/Math.sqrt(x*x+y*y+z*z))),elec_th);
            if(r==1)H_BMT_R1_phi.fill(elec_ph,Math.toDegrees(Math.atan2(y,x)));
            if(r==2)H_BMT_R2_phi.fill(elec_ph,Math.toDegrees(Math.atan2(y,x)));
            if(r==3)H_BMT_R3_phi.fill(elec_ph,Math.toDegrees(Math.atan2(y,x)));
          }
        }
        //else if(bank.getInt("trkID",k)!=-1){
        //	System.out.println("BMTRec::Crosses track hit is NaN");
        //	bank.show();
        //}
        //else if(Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z) ){
        //	System.out.println("s="+s+" , r="+r+" , x="+x+" , y="+y+" , z="+z+" , ux="+ux+" , uy="+uy+" , uz="+uz);
        //}
      }
    }

    public void MakeElectron(DataBank bank){
      for(int k = 0; k < bank.rows() && found_elec==0; k++){
        int pid = bank.getInt("pid", k);
        if( pid == 11 ){
          float px = bank.getFloat("px", k);
          float py = bank.getFloat("py", k);
          float pz = bank.getFloat("pz", k);
          elec_mom = (float)Math.sqrt(px*px+py*py+pz*pz);
          elec_th = (float)Math.toDegrees(Math.acos(pz/elec_mom));
          elec_ph = (float)Math.toDegrees(Math.atan2(py,px));
          elec_vz = bank.getFloat("vz", k);
          if(elec_mom>1.5 && elec_th>5.5 && Math.abs(elec_vz)<15){
            found_elec = 1;
            H_occ_e_th_ph.fill(elec_ph,elec_th);
            H_occ_e_th_p.fill(elec_mom,elec_th);
            H_occ_e_ph_p.fill(elec_mom,elec_ph);
          }
        }
      }
    }

    public void processEvent(DataEvent event) {
      found_elec = 0;
      if(event.hasBank("REC::Particle"))MakeElectron(event.getBank("REC::Particle"));
      //if(event.hasBank("BST::adc"))MakeBST_hits(event.getBank("BST::adc"));
      if(event.hasBank("BSTRec::Hits") && event.hasBank("BST::adc"))MakeBST_hits(event.getBank("BST::adc"),event.getBank("BSTRec::Hits"));
      if(event.hasBank("BMT::adc"))MakeBMT_hits(event.getBank("BMT::adc"));
      if(event.hasBank("BSTRec::Crosses"))MakeBST_crosses(event.getBank("BSTRec::Crosses"));
      if(event.hasBank("BMTRec::Crosses"))MakeBMT_crosses(event.getBank("BMTRec::Crosses"));
    }
  }
