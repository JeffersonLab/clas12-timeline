package org.jlab.clas12.monitoring;
import java.io.*;
import java.util.*;

import org.jlab.groot.math.*;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.math.F1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.TDirectory;
import org.jlab.clas.physics.Vector3;
import org.jlab.clas.physics.LorentzVector;
import org.jlab.groot.base.GStyle;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.ConstantsManager;

public class cndCheckPlots {
		boolean userTimeBased;
		public int runNum;
                public String outputDir;

		public float STT;
		public float RF;	
		public float TimeJitter;

		public H1F H_CND_occ, H_CND_time, massP,massN;
		public H2F H_CND_beta_energy, H_CND_beta_p, H_CND_beta_e_neutral,H_CND_beta_pn,H_CND_vt_P;
		public H2F H_CND_phi_pad, H_CND_layer_pad;
		public H2F[] H_CND_edep_z, H_CND_edep_phi, H_CND_vt_pad,H_CND_z_pad;
		public H2F[] H_CVT_CND_z,H_CVT_CND_z1, H_CVT_CND_phi;
		public H2F[] pathlength,momentum,stt,pathlengthpl,momentumpl,sttpl,pathlengthm,momentumm,sttm;
		public F1D BetaPProt, BetaPPion,IntRes,IntRes1,IntRes2,IntRes3,funcZ,funcT,funcE;
		public H2F[] H_CND_time_z_charged, H_CND_time_z_neutral;
		public H2F H_CND_t_t;
		public H1F RFTIME;
		public H1F[] H_CND_res;
		public H2F[] DiffZCVT, DiffZCND;
		public F1D[] fitz, fitt,fitE;
		//Check of the z alignment
		public H1F[] H_CND_align, H_CND_alignt,H_CND_alignE;
		public GraphErrors resot, resoz, alignz ,alignt ,resop,resopg,resop1,resopg1,resop2,resopg2,alignE;

		public float massp = 0.938f;
		public float masspion = 0.1395f;
		public float light = 29.92f;
		public double rfPeriod;
		public int rf_large_integer;

		public double resolutiont=0.0;
		public double resolutionz=0.0;

		public double moyE=0.0;
		public double moyZ=0.0;
		public double moyT=0.0;

		public IndexedTable InverseTranslationTable;
        	public IndexedTable calibrationTranslationTable;
        	public IndexedTable rfTable;
        	public ConstantsManager ccdb;

		public cndCheckPlots(int reqrunNum, String reqOutputDir, boolean reqTimeBased) {
				userTimeBased=reqTimeBased;
				runNum = reqrunNum;
                                outputDir = reqOutputDir;
				rfPeriod = 4.008;
                		ccdb = new ConstantsManager();
                		ccdb.init(Arrays.asList(new String[]{"/daq/tt/fthodo","/calibration/eb/rf/config"}));
                		rfTable = ccdb.getConstants(runNum,"/calibration/eb/rf/config");
                		if (rfTable.hasEntry(1, 1, 1)){
                        		System.out.println(String.format("RF period from ccdb for run %d: %f",runNum,rfTable.getDoubleValue("clock",1,1,1)));
                        		rfPeriod = rfTable.getDoubleValue("clock",1,1,1);
                		}
                		rf_large_integer = 1000;
				H_CND_time = new H1F("H_CND_time","H_CND_time",100,-2,2);
				H_CND_time.setTitle("CND vertex time - STT");
				H_CND_time.setTitleX("vt (ns)");
				H_CND_occ = new H1F("H_CND_occ","H_CND_occ",48,0.5,48.5);
				H_CND_occ.setTitle("CND occupancy");
				H_CND_occ.setTitleX("CND counter #");
				H_CND_beta_energy = new H2F("H_CND_beta_energy","H_CND_beta_energy",100,0,40,60,0.,1.4);
				H_CND_beta_energy.setTitle("CND beta vs energy (charged particles)");
				H_CND_beta_energy.setTitleY("CND beta");
				H_CND_beta_energy.setTitleX("CND energy");
				H_CND_beta_p = new H2F("H_CND_beta_p","H_CND_beta_p",100,0,1.5,60,0.1,1.2);
				H_CND_beta_p.setTitle("CND beta vs p for p part.");
				H_CND_beta_p.setTitleY("CND beta");
				H_CND_beta_p.setTitleX("CVT p");
				H_CND_beta_pn = new H2F("H_CND_beta_pn","H_CND_beta_pn",100,0,1.5,60,0.1,1.2);
				H_CND_beta_pn.setTitle("CND beta vs p for n part.");
				H_CND_beta_pn.setTitleY("CND beta");
				H_CND_beta_pn.setTitleX("CVT p");
				H_CND_beta_e_neutral = new H2F("H_CND_beta_e_neutral","H_CND_beta_e_neutral",100,0,40,60,0.,1.4);
				H_CND_beta_e_neutral.setTitle("CND beta vs energy for neutral hits");
				H_CND_beta_e_neutral.setTitleY("CND beta");
				H_CND_beta_e_neutral.setTitleX("CND energy");
				H_CND_phi_pad = new H2F("H_CND_phi_pad","H_CND_phi_pad",48,0.5,48.5,48,-180,180);
				H_CND_phi_pad.setTitle("CND #phi vs pad");
				H_CND_phi_pad.setTitleX("CND counter #");
				H_CND_phi_pad.setTitleY("CND #phi (^o)");
				H_CND_layer_pad = new H2F("H_CND_layer_pad","H_CND_layer_pad",3,0.5,3.5,48,0.5,48.5);
				H_CND_layer_pad.setTitle("CND layer vs pad");
				H_CND_layer_pad.setTitleY("CND counter #");
				H_CND_layer_pad.setTitleX("CND layer");
				H_CND_vt_P = new H2F("vtp","vtp",100,0.1,1.5,100,-2.5,2.5);
				H_CND_vt_P.setTitle("vt VS momentum (negative tracks)");
				H_CND_edep_z = new H2F[3];
				H_CND_edep_phi = new H2F[3];
				H_CVT_CND_z = new H2F[3];
				H_CVT_CND_phi = new H2F[3];
				H_CND_vt_pad  = new H2F[3];
				H_CND_z_pad  = new H2F[3];
				H_CND_time_z_charged = new H2F[3];
				H_CND_time_z_neutral = new H2F[3];
				H_CVT_CND_z1 = new H2F[3];
				DiffZCVT = new H2F[3];
				DiffZCND = new H2F[3];
				H_CND_res = new H1F[3];
				pathlength= new H2F[3];
				momentum= new H2F[3];
				stt= new H2F[3];
				pathlengthpl= new H2F[3];
				momentumpl= new H2F[3];
				sttpl= new H2F[3];
				pathlengthm= new H2F[3];
				momentumm= new H2F[3];
				sttm= new H2F[3];
				RFTIME = new H1F("beta diff","beta diff",50,-2,2);
				H_CND_t_t = new H2F("H_CND_t_t","H_CND_t_t",50,0,5,50,0,5);
				H_CND_t_t.setTitle("tCND vs tCVT");
				H_CND_t_t.setTitleX("tCND");
				H_CND_t_t.setTitleY("tCVT");
				for(int iL=0;iL<3;iL++){
						H_CND_edep_z[iL] = new H2F("H_CND_edep_z","H_CND_edep_z",100,-20,40,100,0,40);
						H_CND_edep_z[iL].setTitle("CND Edep vs z (all hits)(layer "+(iL+1)+")");
						H_CND_edep_z[iL].setTitleX("z (cm)");
						H_CND_edep_z[iL].setTitleY("E (MeV)");
						H_CND_edep_phi[iL] = new H2F("H_CND_edep_phi","H_CND_edep_phi",48,0,48,100,0,30);
						H_CND_edep_phi[iL].setTitle("CND Edep vs paddle (negative tracks)(layer "+(iL+1)+")");
						H_CND_edep_phi[iL].setTitleX("paddle");
						H_CND_edep_phi[iL].setTitleY("E (MeV)");
						H_CVT_CND_z[iL] = new H2F(String.format("H_CVT_CND_z_L%d",iL+1),"H_CVT_CND_z",80,0,40,80,0,40);
						H_CVT_CND_z[iL].setTitle("CND z vs CVT z (positive tracks)(layer "+(iL+1)+")");
						H_CVT_CND_z[iL].setTitleX("CND z (cm)");
						H_CVT_CND_z[iL].setTitleY("CVT z (cm)");
						H_CVT_CND_phi[iL] = new H2F("H_CVT_CND_phi","H_CVT_CND_phi",48,-180,180,100,-180,180);
						H_CVT_CND_phi[iL].setTitle("CND #phi vs CVT #phi (layer "+(iL+1)+")");
						H_CVT_CND_phi[iL].setTitleX("CND #phi (^o)");
						H_CVT_CND_phi[iL].setTitleY("CVT #phi (^o)");
						H_CND_vt_pad[iL] = new H2F("H_CND_phi_pad","H_CND_phi_pad",48,0,48,50,-1,1);
						H_CND_vt_pad[iL].setTitle("CND vertex time vs pad (layer "+(iL+1)+")");
						H_CND_vt_pad[iL].setTitleX("CND counter #");
						H_CND_vt_pad[iL].setTitleY("CND vt (ns)");
						H_CND_time_z_neutral[iL] = new H2F("H_CND_time_z_neutral","H_CND_time_z_neutral",50,0,40,100,-5,5);
						H_CND_time_z_neutral[iL].setTitle("CND time vs z for neutral (layer "+(iL+1)+")");
						H_CND_time_z_neutral[iL].setTitleX("CND z");
						H_CND_time_z_neutral[iL].setTitleY("CND time");
						H_CND_time_z_charged[iL] = new H2F(String.format("H_CND_time_z_charged_L%d",iL+1),"H_CND_time_z_charged",50,0,40,100,-3,3);

						H_CND_time_z_charged[iL].setTitle("CND vt vs z (negative tracks) (layer "+(iL+1)+")");
						H_CND_time_z_charged[iL].setTitleX("CND z");
						H_CND_time_z_charged[iL].setTitleY("CND vt");

						H_CND_z_pad[iL] = new H2F("H_CND_z_pad","H_CND_z_pad",47,0,48,80,0,40);
						H_CND_z_pad[iL].setTitle("CND z vs pad (negative tracks) (layer "+(iL+1)+")");
						H_CND_z_pad[iL].setTitleX("CND counter #");
						H_CND_z_pad[iL].setTitleY("CND z (cm)");

						H_CVT_CND_z1[iL] = new H2F(String.format("H_CVT_CND_z1_L%d",iL+1),"H_CVT_CND_z1",80,0,40,80,0,40);
						H_CVT_CND_z1[iL].setTitle("CND z vs CVT z (negative tracks) (layer "+(iL+1)+")");
						H_CVT_CND_z1[iL].setTitleX("CND z (cm)");
						H_CVT_CND_z1[iL].setTitleY("CVT z (cm)");

						H_CND_res[iL] = new H1F("H_CND_res","H_CND_res",100,-5,5);
						H_CND_res[iL].setTitle("CND resolution");
						H_CND_res[iL].setTitleX("vertex time");

						DiffZCVT[iL] = new H2F(String.format("Diff Z CVT_L%d",iL+1),"Diff Z CVT",50,0,45,150,-10,10);
						DiffZCVT[iL].setTitle("DiffZ vs zCVT (negative tracks) (layer "+(iL+1)+")");

						DiffZCND[iL] = new H2F(String.format("Diff Z CND_L%d",iL+1),50,0,45,150,-10,10);	
						DiffZCND[iL].setTitle("DiffZ vs zCND (negative tracks) (layer "+(iL+1)+")");

						pathlength[iL] = new H2F("pathlength","pathlength",48,0,48,100,30,40);
						pathlength[iL].setTitle("pathlength proton");
						pathlength[iL].setTitleX("paddle");
						pathlength[iL].setTitleY("pathlength (cm)");

						momentum[iL] = new H2F("momentum","momentum",48,0,48,100,0.2,1);
						momentum[iL].setTitle("momentum proton");
						momentum[iL].setTitleX("paddle");
						momentum[iL].setTitleY("momentum (GeV)");

						stt[iL] = new H2F("stt","stt",48,0,48,100,150,200);
						stt[iL].setTitle("stt proton");
						stt[iL].setTitleX("paddle");
						stt[iL].setTitleY("stt");

						pathlengthpl[iL] = new H2F("pathlengthp","pathlengthp",48,0,48,100,30,40);
						pathlengthpl[iL].setTitle("pathlength pion+");
						pathlengthpl[iL].setTitleX("paddle");
						pathlengthpl[iL].setTitleY("pathlength (cm)");

						momentumpl[iL] = new H2F("momentump","momentump",48,0,48,100,0.2,1);
						momentumpl[iL].setTitle("momentum pion+");
						momentumpl[iL].setTitleX("paddle");
						momentumpl[iL].setTitleY("momentum (GeV)");

						sttpl[iL] = new H2F("sttp","sttp",48,0,48,100,150,200);
						sttpl[iL].setTitle("stt pion+");
						sttpl[iL].setTitleX("paddle");
						sttpl[iL].setTitleY("stt");

						pathlengthm[iL] = new H2F("pathlengthm","pathlengthm",48,0,48,100,30,10);
						pathlengthm[iL].setTitle("pathlength pion-");
						pathlengthm[iL].setTitleX("paddle");
						pathlengthm[iL].setTitleY("pathlength (cm)");

						momentumm[iL] = new H2F("momentumm","momentumm",48,0,48,100,0.2,1);
						momentumm[iL].setTitle("momentum pion-");
						momentumm[iL].setTitleX("paddle");
						momentumm[iL].setTitleY("momentum (GeV)");

						sttm[iL] = new H2F("sttm","sttm",48,0,48,100,150,200);
						sttm[iL].setTitle("stt pion-");
						sttm[iL].setTitleX("paddle");
						sttm[iL].setTitleY("stt");

				}

				H_CND_align=new H1F[144];
				for(int layer=0;layer<3;layer++){
						for(int sector=0;sector<24;sector++){
								for(int comp=0;comp<2;comp++){
										H_CND_align[(comp*3)+layer+(sector*6)] = new H1F("CND_align","CND_align",50,-10,10);
										H_CND_align[(comp*3)+layer+(sector*6)].setTitle("layer "+(layer+1)+" sector "+(sector+1)+" comp "+(comp+1));
										H_CND_align[(comp*3)+layer+(sector*6)].setTitleX("CND z-CVT z (cm)");
								}
						}
				}		


				H_CND_alignt=new H1F[144];
				for(int layer=0;layer<3;layer++){
						for(int sector=0;sector<24;sector++){
								for(int comp=0;comp<2;comp++){
										H_CND_alignt[(comp*3)+layer+(sector*6)] = new H1F("CND_alignt","CND_alignt",100,-1,1);
										H_CND_alignt[(comp*3)+layer+(sector*6)].setTitle("layer "+(layer+1)+" sector "+(sector+1)+" comp "+(comp+1));
										H_CND_alignt[(comp*3)+layer+(sector*6)].setTitleX("vt (ns)");
								}
						}
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

				massP = new H1F("massP","massP",100,-1,3);
				massP.setTitle("mass^2 positive particle");
				massP.setTitleX("mass^2 (GeV)");

				massN = new H1F("massN","massN",100,-1,3);
				massN.setTitle("mass^2 negative particle");
				massN.setTitleX("mass^2 (GeV)");

				funcE= new F1D("funcE","[a]",0,144);
				funcE.setLineWidth(10);
				funcE.setLineColor(33);
				funcT= new F1D("funcT","[a]",0,144);
				funcT.setLineWidth(10);
				funcT.setLineColor(33);
				funcZ= new F1D("funcZ","[a]",0,144);
				funcZ.setLineWidth(10);
				funcZ.setLineColor(33);

				BetaPProt = new F1D("betaPProt","x/sqrt([a]*[a]+x*x)",0,1.5);
				BetaPPion = new F1D("betaPPion","x/sqrt([a]*[a]+x*x)",0,1.5);


				BetaPProt.setParameter(0,massp);
				BetaPPion.setParameter(0,masspion);
				BetaPProt.setLineWidth(10);
				BetaPProt.setLineColor(33);
				BetaPPion.setLineWidth(10);
				BetaPPion.setLineColor(44);
				IntRes = new F1D("integrated resolution","[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
				IntRes.setLineColor(33);
				IntRes.setLineWidth(10);

				IntRes1 = new F1D("integrated resolution1","[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
				IntRes1.setLineColor(33);
				IntRes1.setLineWidth(10);

				IntRes2 = new F1D("integrated resolution2","[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
				IntRes2.setLineColor(33);
				IntRes2.setLineWidth(10);

				IntRes3 = new F1D("integrated resolution3","[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
				IntRes3.setLineColor(33);
				IntRes3.setLineWidth(10);


				fitz=new F1D[144];
				fitt=new F1D[144];
				fitE=new F1D[144];
				for(int layer=0;layer<3;layer++){
						for(int sector=0;sector<24;sector++){
								for(int comp=0;comp<2;comp++){

										fitz[(comp*3)+layer+(sector*6)]=new F1D("z resolution","[amp]*gaus(x,[mean],[sigma])+[cst]", -5.0, 5.0);
										fitz[(comp*3)+layer+(sector*6)].setLineColor(33);
										fitz[(comp*3)+layer+(sector*6)].setLineWidth(10);					
										fitt[(comp*3)+layer+(sector*6)]=new F1D("t resolution","[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
										fitt[(comp*3)+layer+(sector*6)].setLineColor(33);
										fitt[(comp*3)+layer+(sector*6)].setLineWidth(10);
										fitE[(comp*3)+layer+(sector*6)]=new F1D("E resolution","[amp]*gaus(x,[mean],[sigma])+[cst]+[a]*x", 0.0, 6.0);
										fitE[(comp*3)+layer+(sector*6)].setLineColor(33);
										fitE[(comp*3)+layer+(sector*6)].setLineWidth(10);
								}
						}
				}

				resot=new GraphErrors();
				resot.setTitle("Vertex time resolution");
				resot.setTitleX("paddle");
				resot.setTitleY("vt resolution");
				resoz=new GraphErrors();
				resoz.setTitle("Position resolution");
				resoz.setTitleX("paddle");
				resoz.setTitleY("z resolution");
				alignz=new GraphErrors();
				alignz.setTitle("Position alignement");
				alignz.setTitleX("paddle");
				alignz.setTitleY("z offset");
				alignt=new GraphErrors();
				alignt.setTitle("Vertex time alignement");
				alignt.setTitleX("paddle");
				alignt.setTitleY("z alignement offset");
				resop=new GraphErrors();
				resop.setTitle("Beta vs P (45^o)");
				resop.setTitleX("Momentum (GeV)");
				resop.setTitleY("Beta");
				resopg=new GraphErrors();
				resop1=new GraphErrors();
				resop1.setTitle("Beta vs P (60^o)");
                resop1.setTitleX("Momentum (GeV)");
                resop1.setTitleY("Beta");
				resopg1=new GraphErrors();
				resop2=new GraphErrors();
				resop2.setTitle("Beta vs P (90^o)");
                resop2.setTitleX("Momentum (GeV)");
                resop2.setTitleY("Beta");
				resopg2=new GraphErrors();
				alignE=new GraphErrors();
				alignE.setTitle("dE/dz alignement");
				alignE.setTitleX("paddle");
				alignE.setTitleY("dE/dz");
		}
		public void FillCND(DataBank CNDbank, DataBank CVTbank, DataBank PARTbank){

				//System.out.println("New event");
				//System.out.println();
				//Clusterizing CND hits by hand (for charge particles and neutral hits separetly	
				int[] InClusters = new int[CNDbank.rows()];
				for(int i=0; i<InClusters.length; i++){
						InClusters[i]=0;
				}

				int[] Tracks = new int[CVTbank.rows()];
				for(int i=0; i<Tracks.length; i++){
						Tracks[i]=0;
				}
				float vertexTrigger=PARTbank.getFloat("vz",0);
				int pidTrigger=PARTbank.getInt("pid",0);
				
			
				for(int iCND=0;iCND<CNDbank.rows();iCND++){
						int layer = CNDbank.getInt("layer",iCND);
						int trkID = CNDbank.getInt("trkID",iCND);
						float e  = CNDbank.getFloat("energy",iCND);
						float x  = CNDbank.getFloat("x",iCND);
						float y  = CNDbank.getFloat("y",iCND);
						float z  = CNDbank.getFloat("z",iCND);
						float time = CNDbank.getFloat("time",iCND);
						int sector = CNDbank.getInt("sector",iCND);
						int comp = CNDbank.getInt("component",iCND);
						//        System.out.println(trkID+" "+e+" "+x+" "+y+" "+z+" "+time);

						//	float adcL=TDCbank.getFloat("tdc",iCND);		

						float cndPhi = (float)Math.toDegrees(Math.atan2(y,x));

						H_CND_edep_z[layer-1].fill(z,e);
						//H_CND_edep_phi[layer-1].fill((sector-1)*2+(comp-0.5),e);
						//H_CND_z_pad[layer-1].fill(comp+2*(sector-1),z);
						H_CND_occ.fill(sector + 25*(comp-1));
						H_CND_phi_pad.fill(sector + 25*(comp-1),cndPhi);
						H_CND_layer_pad.fill(layer , sector + 25*(comp-1));



						if(layer>0 && layer<4 && trkID==-1 ){//&& STT>-999){
								float z0 = CVTbank.getFloat("z0", trkID) / 10.0f;
								boolean cluster=false;
								boolean sidehit=false;	

								int newHit = InClusters[iCND];	
								if(InClusters[iCND]==0){
										InClusters[iCND]=1;
								} else {
										break;
								}

								for(int jCND=0;jCND<CNDbank.rows();jCND++){
										if(jCND!=iCND &&
														sector==CNDbank.getInt("sector",jCND)){
												InClusters[jCND]=1;
										}
										if(jCND!=iCND &&
														((sector-1)==CNDbank.getInt("sector",jCND) || (sector+1)==CNDbank.getInt("sector",jCND))){
												sidehit=true;
										}

								}				
								if(/*newHit==0 && !sidehit && sector!=3 && sector!=11 &&sector!=19*/true){
										float betaN = (float)Math.sqrt(x*x+y*y+(z-z0)*(z-z0))/(time-STT)/29.92f;
										//System.out.println("time "+time+"STT "+STT+" betaN "+betaN+"layer "+layer);
									if (betaN>0.2) {
										H_CND_beta_e_neutral.fill(e,betaN);
										H_CND_time_z_neutral[layer-1].fill(z,time-STT);
									}
								}
						}

						if(layer>0 && layer<4 && trkID>-1 && STT>-999){
								float tx = CNDbank.getFloat("tx",iCND);
								float ty = CNDbank.getFloat("ty",iCND);
								float tz = CNDbank.getFloat("tz",iCND);
								float path = CNDbank.getFloat("pathlength",iCND);
								float mom = CVTbank.getFloat("p",trkID);
								float vertex = CVTbank.getFloat("z0",trkID);
								int charge = CVTbank.getInt("q",trkID);
								float beta = mom/(float)Math.sqrt(mom*mom+0.93827f*0.93827f);
								float betaP = mom/(float)Math.sqrt(mom*mom+0.139f*0.139f);
								float phase = 4.f*((TimeJitter+1.f)%6.f);
							
								
								float vertexCorrCentral=vertex/29.92f;
								float vertexCorrForward=vertexTrigger/29.92f;
							
								float vt = time - STT - (path/29.92f/beta);//-(vertex/29.92f);
								float vtP = time - (STT-vertexCorrForward+vertexCorrCentral) - (path/29.92f/betaP);//-(vertex/29.92f);//- vertex/29.92f;
								float rfp = (float)rfPeriod;
								float vtPRF = ((time - RF - path/29.92f/betaP)+1000*rfp+(0.5f*rfp))%rfp - 0.5f*rfp;
								float pathTH = CNDbank.getFloat("tlength",iCND);


								//System.out.println(tx+" "+ty+" "+tz+" "+path+" "+mom+" "+charge);

								float timeC = (float) (time -STT);
								float betaCND = path/timeC/29.92f;	
								float timeCVT =(float)(path/29.92f/mom)*(float)Math.sqrt(0.938*0.938f+mom*mom);
								//System.out.println("time "+time+"STT "+STT+" time-STT "+timeC+" betaN "+betaN+"layer "+layer);
								float cvtPhi = (float)Math.toDegrees(Math.atan2(ty,tx));
								float rf = ((RF - (vt + STT))+2000f+1f)%2f -1f;


								float mass2=mom*mom*(float)((1.f/(betaCND*betaCND))-1.f);

								if(charge==-1 && Math.abs(vtP)<1.5 && z<(15.+5*(layer-1)))massN.fill(mass2);
								if(charge==1 && Math.abs(vtP)<1.5 && z<(15.+5*(layer-1)))massP.fill(mass2);

								//if(charge==-1)H_CND_time.fill(vtP);
								//if(charge==-1 && Math.abs(mass2)<0.4*0.4 && z<(25.+2.5*(layer-1)))H_CND_vt_pad[layer-1].fill(sector + 25*(comp-1),vtP);
								if(charge==-1){H_CVT_CND_z1[layer-1].fill(z,tz);
										if(charge==-1)H_CND_res[0].fill(vtP);
								}

								if(charge==1){H_CVT_CND_z[layer-1].fill(z,tz);
								}

								if(layer==1 && sector==1 && comp==2){H_CND_res[1].fill(vtP);}
								if(layer==1 && sector==4 && comp==1){H_CND_res[2].fill(vtP);}
								boolean in=false;
								//for(int jCND=(iCND+1);jCND<CNDbank.rows();jCND++){
								//	in=(sector==CNDbank.getInt("sector",jCND) && layer==(CNDbank.getInt("layer",jCND)-1));
								//	if(in)System.out.println("problem");
								//}
								/*if (timeC>2.0)*/if( charge==-1 && Math.sqrt(Math.abs(mass2))<0.4 && mass2>-0.2*0.2 && z<(15.+5*(layer-1)) && Math.abs(vtP)<1.5/*&& betaCND<0.7*//*&& timeC>1.5*//*&& Math.abs(z-tz)<5.*/){//H_CND_alignt[((comp-1)*3)+(layer-1)+((sector-1)*6)].fill(vtP);
										//H_CND_vt_P.fill(mom,vtP);
								}
								if (charge==-1 && Math.abs(vtP)<1.5) RFTIME.fill(betaCND-betaP);
								//if(timeC>2.0){
								//if (charge==-1)H_CND_alignt[((comp-1)*3)+(layer-1)+((sector-1)*6)].fill(vtPRF);

								if (charge==-1 &&  Math.abs(vtP)<1.5)
								{
										H_CND_z_pad[layer-1].fill((comp-0.5)+2*(sector-1),z);
								}

								if (charge==-1 && /*Math.sqrt(Math.abs(mass2))>0.4 &&*/ /*mass2>-0.2*0.2 &&*/ z<(15.+5*(layer-1)) && Math.abs(vtP)<1.5)
								{
										H_CND_time_z_charged[layer-1].fill(z,vtP);
										H_CND_edep_phi[layer-1].fill((sector-1)*2+(comp-0.5),e);
										//H_CVT_CND_z[layer-1].fill(z,tz);
										H_CND_align[((comp-1)*3)+(layer-1)+((sector-1)*6)].fill(z-tz);
										DiffZCVT[layer-1].fill(tz,z-tz);
										DiffZCND[layer-1].fill(z,z-tz);
										//H_CND_z_pad[layer-1].fill((comp-0.5)+2*(sector-1),z);
								}
								if (charge==-1 && Math.abs(z-tz)<5.){	
										//H_CND_edep_phi[layer-1].fill((sector-1)*2+(comp-0.5),e);
								}
								H_CVT_CND_phi[layer-1].fill(cndPhi,cvtPhi);
								//if(charge==-1)H_CND_time_z_charged[layer-1].fill(z,vtP);
								if(charge==-1 && z<(15.+5*(layer-1)))H_CND_t_t.fill(timeC,timeCVT);
								//only include one hit from cluster
								//if(Tracks[trkID]==0){
								if (betaCND > 0.2){
									H_CND_beta_energy.fill(e,betaCND);
								}
								if(/*sector==18 &&*/   /*&& mass2>-0.2*0.2*/  charge==1  && z<(15.+5*(layer-1)) && Math.abs(vtP)<1.5 /*&& Math.abs(z-tz)<5.*/)H_CND_beta_p.fill(mom,betaCND);
								if(charge==-1 /*&& Math.sqrt(Math.abs(mass2))<0.3  && mass2>-0.3*0.3*/ && z<(15.+5*(layer-1)) && Math.abs(vtP)<1.5/* && Math.abs(z-tz)<5.*/)H_CND_beta_pn.fill(mom,betaCND);				
								//	Tracks[trkID]=1;
								//}


								//proton
								if (charge==1 && Math.sqrt(Math.abs(mass2))>0.4 && /*mass2>-0.2*0.2 &&*/ z<(15.+5*(layer-1)) /*&& Math.abs(vtP)<3.0*/) 
								{
										pathlength[layer-1].fill((sector-1)*2+(comp-0.5),path);
										momentum[layer-1].fill((sector-1)*2+(comp-0.5),mom);
										stt[layer-1].fill((sector-1)*2+(comp-0.5),STT);

								}
								//pi-
								if (charge==-1 && Math.sqrt(Math.abs(mass2))<0.38 && mass2>-0.35*0.35 && z<(15.+5*(layer-1)) && Math.abs(vtP)<1.5)
								{
										H_CND_vt_pad[layer-1].fill((sector-1)*2+(comp-0.5),vtP);
										H_CND_time.fill(vtP);
										H_CND_alignt[((comp-1)*3)+(layer-1)+((sector-1)*6)].fill(vtP);
										H_CND_vt_P.fill(mom,vtP);
										pathlengthm[layer-1].fill((sector-1)*2+(comp-0.5),path);
										momentumm[layer-1].fill((sector-1)*2+(comp-0.5),mom);
										sttm[layer-1].fill((sector-1)*2+(comp-0.5),STT);

										float dE = e/pathTH;

										H_CND_alignE[((comp-1)*3)+(layer-1)+((sector-1)*6)].fill(dE);


								}
								//pi+
								if (charge==1 && Math.sqrt(Math.abs(mass2))<0.4 && mass2>-0.2*0.2 && z<(15.+5*(layer-1)) && Math.abs(vtP)<1.5)
								{
										pathlengthpl[layer-1].fill((sector-1)*2+(comp-0.5),path);
										momentumpl[layer-1].fill((sector-1)*2+(comp-0.5),mom);
										sttpl[layer-1].fill((sector-1)*2+(comp-0.5),STT);

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

						public void fit() {

								IntRes.setRange(-1.,1.);
								IntRes.setParameter(1,0.0);
								//IntRes.setParLimits(1,-0.2,0.2);
								var H_CND_time_max_bin_content = H_CND_time.getBinContent(H_CND_time.getMaximumBin());
								IntRes.setParameter(0,H_CND_time_max_bin_content);
								if(H_CND_time_max_bin_content!=0) IntRes.setParLimits(0,H_CND_time_max_bin_content*0.98,H_CND_time_max_bin_content*1.1);
								System.out.println("height "+H_CND_time.getBinContent(H_CND_time.getMaximumBin()));
								IntRes.setParameter(2,0.2);
								try {
										DataFitter.fit(IntRes, H_CND_time, "Q");
										H_CND_time.setTitle("Integrated vertex time. Width=" + IntRes.getParameter(2));
								} catch (Exception ex) {
										ex.printStackTrace();
								}

								IntRes1.setRange(-0.5,0.5);
								IntRes1.setParameter(1,0.0);
								IntRes1.setParLimits(1,-0.2,0.2);
								var H_CND_res_max_bin_content = H_CND_res[0].getBinContent(H_CND_res[0].getMaximumBin());
								IntRes1.setParameter(0,H_CND_res_max_bin_content);
								if(H_CND_res_max_bin_content!=0) IntRes1.setParLimits(0,H_CND_res_max_bin_content*0.9,H_CND_res_max_bin_content*1.1);
								System.out.println("height "+H_CND_res[0].getBinContent(H_CND_res[0].getMaximumBin()));
								IntRes1.setParameter(2,2.0);
								try {
										// DataFitter.fit(IntRes1, H_CND_res[0], "Q");
										//H_CND_res[0].setTitle("Integrated vertex time. Width=" + IntRes1.getParameter(2));
								} catch (Exception ex) {
										ex.printStackTrace();
								}

								//double resolutiont=0.0;
								//double resolutionz=0.0;

								for(int layer=0;layer<3;layer++){
										for(int sector=0;sector<24;sector++){
												for(int comp=0;comp<2;comp++){


														double maxz = H_CND_align[(comp*3)+layer+(sector*6)].getBinContent(H_CND_align[(comp*3)+layer+(sector*6)].getMaximumBin());
														double maxzp = H_CND_align[(comp*3)+layer+(sector*6)].getMaximumBin();

														double maxE = H_CND_alignE[(comp*3)+layer+(sector*6)].getBinContent(H_CND_alignE[(comp*3)+layer+(sector*6)].getMaximumBin());

														double maxt = H_CND_alignt[(comp*3)+layer+(sector*6)].getBinContent(H_CND_alignt[(comp*3)+layer+(sector*6)].getMaximumBin());
														double maxtp = H_CND_alignt[(comp*3)+layer+(sector*6)].getMaximumBin();
														double tped = H_CND_alignt[(comp*3)+layer+(sector*6)].getBinContent(1);
														//System.out.println(maxtp);

														fitz[(comp*3)+layer+(sector*6)].setRange(-5,5);
														fitz[(comp*3)+layer+(sector*6)].setParameter(1,0.0);
														fitz[(comp*3)+layer+(sector*6)].setParameter(0,maxz);
														if(maxz!=0) fitz[(comp*3)+layer+(sector*6)].setParLimits(0,maxz*0.9,maxz*1.1);
														fitz[(comp*3)+layer+(sector*6)].setParameter(2,3.0);
														fitz[(comp*3)+layer+(sector*6)].setParameter(3,10.0);

														fitt[(comp*3)+layer+(sector*6)].setRange(-0.7,0.7);
														fitt[(comp*3)+layer+(sector*6)].setParameter(1,0.0);
														fitt[(comp*3)+layer+(sector*6)].setParLimits(1,-1,1);
														fitt[(comp*3)+layer+(sector*6)].setParameter(0,maxt);
														if(maxt!=0) fitt[(comp*3)+layer+(sector*6)].setParLimits(0,maxt*0.95,maxt*1.1);
														fitt[(comp*3)+layer+(sector*6)].setParameter(2,0.2);
														//fitt[(comp*3)+layer+(sector*6)].setParameter(3,0.0);

														fitE[(comp*3)+layer+(sector*6)].setRange(1.5,5);
														fitE[(comp*3)+layer+(sector*6)].setParameter(1,2.0);
														fitE[(comp*3)+layer+(sector*6)].setParameter(0,maxE);
														if(maxE!=0) fitE[(comp*3)+layer+(sector*6)].setParLimits(0,maxE*0.9,maxE*1.1);
														fitE[(comp*3)+layer+(sector*6)].setParameter(2,1.0);
														fitE[(comp*3)+layer+(sector*6)].setParameter(3,0.0);
														fitE[(comp*3)+layer+(sector*6)].setParameter(4,0.0);

														try {
																DataFitter.fit(fitz[(comp*3)+layer+(sector*6)], H_CND_align[(comp*3)+layer+(sector*6)], "Q");
																double resz =Math.abs(fitz[(comp*3)+layer+(sector*6)].getParameter(2));
																double alig=fitz[(comp*3)+layer+(sector*6)].getParameter(1);
																//double aligt=fitt[(comp*3)+layer+(sector*6)].getParameter(1);
																if(Math.abs(resz)<10) resoz.addPoint((comp*3)+layer+(sector*6),resz,0.,0.);
																if(Math.abs(resz)<5 && resz>1.5 ) resolutionz+=resz;
																if(Math.abs(alig)<5) alignz.addPoint((comp*3)+layer+(sector*6),alig,0.,resz/2.);
																if(Math.abs(alig)<5) moyZ+=alig;						

														} catch (Exception ex) {
																ex.printStackTrace();
														}

														try {
																DataFitter.fit(fitt[(comp*3)+layer+(sector*6)], H_CND_alignt[(comp*3)+layer+(sector*6)], "Q");
																double rest=Math.abs(fitt[(comp*3)+layer+(sector*6)].getParameter(2));
																double aligt=fitt[(comp*3)+layer+(sector*6)].getParameter(1);
																if(rest<0.5)resot.addPoint((comp*3)+layer+(sector*6),rest,0.,0.);
																if(Math.abs(aligt)<0.8) alignt.addPoint((comp*3)+layer+(sector*6),aligt,0.,rest/2.);
																resolutiont+=rest;		
																moyT+=aligt;
														} catch (Exception ex) {
																ex.printStackTrace();
														}

														try {
																DataFitter.fit(fitE[(comp*3)+layer+(sector*6)], H_CND_alignE[(comp*3)+layer+(sector*6)], "Q");
																double resE=Math.abs(fitE[(comp*3)+layer+(sector*6)].getParameter(2));
																double aligE=fitE[(comp*3)+layer+(sector*6)].getParameter(1);
																//if(rest<0.5)resot.addPoint((comp*3)+layer+(sector*6),rest,0.,0.);
																if(Math.abs(resE)<5)alignE.addPoint((comp*3)+layer+(sector*6),aligE,0.,resE/2.);
																moyE+=aligE;
																// resolutiont+=rest;
														} catch (Exception ex) {
																ex.printStackTrace();
														}

												}
										}
								}
								moyZ=moyZ/144;
								moyT=moyT/144;
								moyE=moyE/144;
								funcE.setParameter(0,moyE);
								funcT.setParameter(0,moyT);
								funcZ.setParameter(0,moyZ);
								resolutionz=resolutionz/144.;
								resolutiont=resolutiont/144.;
								//cout<<"Resolution z "<< resolutionz <<endl;
								System.out.println("Resolution z "+resolutionz);
								System.out.println("Resolution t "+resolutiont);

						}
						public void plot() {

								//fill the reso p plot here
								for(int i=1;i<10;i++){

										double p=0.1*i;
										double energy=Math.sqrt(massp*massp+p*p);
										double beta=p/energy;
										double R=30.5;

										double z=30./Math.sqrt(3);//60deg
										double d=z*2;

										double dz=resolutionz/2.;
										//double dz=2.75/2.;
										double c=29.97;
										//double dt=0.2;
										double dt=resolutiont;

										double dd=2*(Math.sqrt(R*R+z*z+2*z*dz+dz*dz)-Math.sqrt(R*R+z*z));
										//double db=(Math.sqrt(dd*dd+beta*beta*c*c*dt*dt)*beta)/d;
										//double dbg=Math.sqrt(dd*dd+c*c*dt*dt)/d;
										double db=(beta*dd+beta*beta*c*dt)/(d);
										double dbg=(dd+c*dt)/d;
										resop.addPoint(p,beta,0,db);
										resopg.addPoint(p,1,0,dbg);

										double z1=30.;//45deg
										double d1=z1*Math.sqrt(2);
										double dd1=2*(Math.sqrt(R*R+z1*z1+2*z1*dz+dz*dz)-Math.sqrt(R*R+z1*z1));
										//double db1=(Math.sqrt(dd1*dd1+beta*beta*c*c*dt*dt)*beta)/d1;
										//double dbg1=Math.sqrt(dd1*dd1+c*c*dt*dt)/d1;				
										double db1=(beta*dd1+beta*beta*c*dt)/d1;
										double dbg1=(dd1+c*dt)/d1;
										resop1.addPoint(p,beta,0,db1);
										resopg1.addPoint(p,1,0,dbg1);

										double z2=0.0;//90deg
										double d2=R;
										double dd2=2*(Math.sqrt(R*R+z2*z2+2*z2*dz+dz*dz)-Math.sqrt(R*R+z2*z2));
										//double db2=(Math.sqrt(dd2*dd2+beta*beta*c*c*dt*dt)*beta)/d2;
										//double dbg2=Math.sqrt(dd2*dd2+c*c*dt*dt)/d2;				
										double db2=(beta*dd2+beta*beta*c*dt)/d2;
										double dbg2=(dd2+c*dt)/d2;
										resop2.addPoint(p,beta,0,db2);
										resopg2.addPoint(p,1,0,dbg2);
								}



								EmbeddedCanvas can_cnd  = new EmbeddedCanvas();
								can_cnd.setSize(2500,10000);
								can_cnd.divide(3,17);
								can_cnd.setAxisTitleSize(18);
								can_cnd.setAxisFontSize(18);
								can_cnd.setTitleSize(18);
								can_cnd.cd(0);can_cnd.draw(H_CND_time);can_cnd.draw(IntRes,"same");
								can_cnd.cd(1);can_cnd.draw(H_CND_layer_pad);
								for(int iL=0;iL<3;iL++){
										can_cnd.cd(3+iL);can_cnd.draw(H_CND_vt_pad[iL]);
										can_cnd.cd(6+iL);can_cnd.draw(H_CND_edep_z[iL]);
										can_cnd.cd(9+iL);can_cnd.draw(H_CVT_CND_z1[iL]);
										can_cnd.cd(12+iL);can_cnd.draw(H_CVT_CND_z[iL]);
										can_cnd.cd(15+iL);can_cnd.draw(H_CVT_CND_phi[iL]);
								}
								//can_cnd.getPad(18).getAxisZ().setLog(true);
								can_cnd.cd(18);can_cnd.draw(H_CND_beta_energy);
								can_cnd.cd(34);can_cnd.draw(H_CND_beta_p);can_cnd.draw(BetaPProt,"same");can_cnd.draw(BetaPPion,"same");can_cnd.getPad().getAxisZ().setLog(true);
								//can_cnd.getPad(19).getAxisZ().setLog(true);
								can_cnd.cd(19);can_cnd.draw(H_CND_beta_e_neutral);
								for(int iL=0;iL<3;iL++){
										can_cnd.cd(21+iL);can_cnd.draw(H_CND_time_z_charged[iL]);
										can_cnd.cd(24+iL);can_cnd.draw(H_CND_time_z_neutral[iL]);
								}
								//can_cnd.cd(26);can_cnd.draw(RFTIME);
								//can_cnd.cd(27);can_cnd.getPad().getAxisZ().setLog(true);can_cnd.draw(H_CND_t_t);
								for(int iL=0;iL<3;iL++){
										can_cnd.cd(27+iL);can_cnd.draw(H_CND_z_pad[iL]);
								}
								for(int iL=0;iL<3;iL++){
										can_cnd.cd(30+iL);can_cnd.draw(H_CND_edep_phi[iL]);
								}
								can_cnd.cd(33);can_cnd.draw(H_CND_beta_pn);can_cnd.draw(BetaPProt,"same");can_cnd.draw(BetaPPion,"same");
								/*for(int iL=0;iL<3;iL++){
								  can_cnd.cd(36+iL);can_cnd.draw(H_CND_res[iL]);
								  }*/
								for(int iL=0;iL<3;iL++){
										can_cnd.cd(36+iL);can_cnd.draw(DiffZCVT[iL]);
								}
								for(int iL=0;iL<3;iL++){
										can_cnd.cd(39+iL);can_cnd.draw(DiffZCND[iL]);
								}
								resot.setTitle("Resolution t");
								resoz.setTitle("Resolution z");			
								alignz.setTitle("align zCVT/zCND");
								alignt.setTitle("vertex time alignment");	
								can_cnd.cd(42);can_cnd.draw(resot);
								can_cnd.cd(43);can_cnd.draw(resoz);
								can_cnd.cd(44);can_cnd.draw(alignz);can_cnd.draw(funcZ,"same");
								can_cnd.cd(45);can_cnd.draw(alignt);can_cnd.draw(funcT,"same");
                                can_cnd.cd(47);can_cnd.draw(H_CND_vt_P);
                                can_cnd.cd(46);can_cnd.draw(alignE);can_cnd.draw(funcE,"same");
								//resop.setMarkerColor(2);resop.setLineColor(2);
								//resopg.setMarkerColor(4);resopg.setLineColor(4);
								//can_cnd.cd(49);can_cnd.draw(resop);can_cnd.draw(resopg,"same");
								resop1.setMarkerColor(2);resop1.setLineColor(2);
								resopg1.setMarkerColor(4);resopg1.setLineColor(4);
								can_cnd.cd(48);can_cnd.draw(resop1);can_cnd.draw(resopg1,"same");
								resop.setMarkerColor(2);resop.setLineColor(2);
								resopg.setMarkerColor(4);resopg.setLineColor(4);
								can_cnd.cd(49);can_cnd.draw(resop);can_cnd.draw(resopg,"same");			
								resop2.setMarkerColor(2);resop2.setLineColor(2);
								resopg2.setMarkerColor(4);resopg2.setLineColor(4);
								can_cnd.cd(50);can_cnd.draw(resop2);can_cnd.draw(resopg2,"same");	

                                                                can_cnd.save(String.format(outputDir+"/cnd.png"));
                                                                System.out.println(String.format("saved "+outputDir+"/cnd.png"));


								//	can_cnd.save("cnd1.png");

								/*

								   EmbeddedCanvas can_cnd2  = new EmbeddedCanvas();
								   can_cnd2.setSize(1500,24000);
								   can_cnd2.divide(3,48);
								   can_cnd2.setAxisTitleSize(18);
								   can_cnd2.setAxisFontSize(18);
								   can_cnd2.setTitleSize(18);

								   for(int layer=0;layer<3;layer++){
								   for(int sector=0;sector<24;sector++){
								   for(int comp=0;comp<2;comp++){
								   can_cnd2.cd((comp*3)+layer+(sector*6));can_cnd2.draw(H_CND_align[(comp*3)+layer+(sector*6)]);
								   can_cnd2.draw(fitz[(comp*3)+layer+(sector*6)],"same");
								   }
								   }
								   }
								   can_cnd2.save("cnd2.png");

								   EmbeddedCanvas can_cnd3  = new EmbeddedCanvas();
								   can_cnd3.setSize(1500,24000);
								   can_cnd3.divide(3,48);
								   can_cnd3.setAxisTitleSize(18);
								   can_cnd3.setAxisFontSize(18);
								   can_cnd3.setTitleSize(18);

								   for(int layer=0;layer<3;layer++){
								   for(int sector=0;sector<24;sector++){
								   for(int comp=0;comp<2;comp++){
								   can_cnd3.cd((comp*3)+layer+(sector*6));can_cnd3.draw(H_CND_alignt[(comp*3)+layer+(sector*6)]);
								   can_cnd3.draw(fitt[(comp*3)+layer+(sector*6)],"same");					
								   }
								   }
								   }
								   can_cnd3.save("cnd3.png");


								   EmbeddedCanvas can_cndE  = new EmbeddedCanvas();
								   can_cndE.setSize(1500,24000);
								   can_cndE.divide(3,48);
								   can_cndE.setAxisTitleSize(18);
								   can_cndE.setAxisFontSize(18);
								   can_cndE.setTitleSize(18);

								   for(int layer=0;layer<3;layer++){
								   for(int sector=0;sector<24;sector++){
								   for(int comp=0;comp<2;comp++){
								   can_cndE.cd((comp*3)+layer+(sector*6));can_cndE.draw(H_CND_alignE[(comp*3)+layer+(sector*6)]);
								   can_cndE.draw(fitE[(comp*3)+layer+(sector*6)],"same");
								   }
								   }
								   }
								   can_cndE.save("cndE.png");




								   EmbeddedCanvas can_cnd4  = new EmbeddedCanvas();
								   can_cnd4.setSize(1500,1000);
								   can_cnd4.divide(2,2);
								   can_cnd4.setAxisTitleSize(18);
								   can_cnd4.setAxisFontSize(18);
								   can_cnd4.setTitleSize(18);
								   can_cnd4.cd(0);can_cnd4.draw(H_CND_beta_p);can_cnd4.draw(BetaPProt,"same");can_cnd4.draw(BetaPPion,"same");can_cnd4.getPad().getAxisZ().setLog(true);
								   can_cnd4.cd(1);can_cnd4.draw(H_CND_beta_pn);can_cnd4.draw(BetaPProt,"same");can_cnd4.draw(BetaPPion,"same");
								   can_cnd4.cd(2);can_cnd4.getPad().getAxisY().setLog(true);can_cnd4.draw(massP);
								   can_cnd4.cd(3);can_cnd4.getPad().getAxisY().setLog(true);can_cnd4.draw(massN);
								   can_cnd4.save("cnd4.png");

								   EmbeddedCanvas can_cnd5  = new EmbeddedCanvas();
								   can_cnd5.setSize(2500,9000);
								can_cnd5.divide(3,9);
								can_cnd5.setAxisTitleSize(18);
								can_cnd5.setAxisFontSize(18);
								can_cnd5.setTitleSize(18);

								for(int iL=0;iL<3;iL++){
										can_cnd5.cd(iL);can_cnd5.draw(pathlength[iL]);
										can_cnd5.cd(iL+3);can_cnd5.draw(momentum[iL]);
										can_cnd5.cd(iL+6);can_cnd5.draw(stt[iL]);
										can_cnd5.cd(iL+9);can_cnd5.draw(pathlengthpl[iL]);
										can_cnd5.cd(iL+12);can_cnd5.draw(momentumpl[iL]);
										can_cnd5.cd(iL+15);can_cnd5.draw(sttpl[iL]);
										can_cnd5.cd(iL+18);can_cnd5.draw(pathlengthm[iL]);
										can_cnd5.cd(iL+21);can_cnd5.draw(momentumm[iL]);
										can_cnd5.cd(iL+24);can_cnd5.draw(sttm[iL]);

								}

								can_cnd5.save("cnd5.png");
								*/	

						}
						public void write(){
								TDirectory dirout = new TDirectory();
								dirout.mkdir("/cnd/");
								dirout.cd("/cnd/");
								dirout.addDataSet(H_CND_beta_energy, H_CND_beta_e_neutral);

								for(int layer=0;layer<3;layer++){
										for(int sector=0;sector<24;sector++){
												for(int comp=0;comp<2;comp++){
														dirout.addDataSet(H_CND_alignE[(comp*3)+layer+(sector*6)]);
												}
										}
								}
								for(int iL=0;iL<3;iL++) dirout.addDataSet(H_CND_time_z_charged[iL],H_CVT_CND_z[iL],H_CVT_CND_z1[iL],DiffZCVT[iL],DiffZCND[iL]);


                                                                if(runNum>0) dirout.writeFile(outputDir+"/out_CND_"+runNum+".hipo");
                                                                else         dirout.writeFile(outputDir+"/out_CND.hipo");
						}


						////////////////////////////////////////////////
						public static void main(String[] args) {
								System.setProperty("java.awt.headless", "true");
								int count = 0;
								int runNum = 0;
								boolean useTB=true;
								String filelist = "list_of_files.txt";
								if(args.length>0)runNum=Integer.parseInt(args[0]);
								if(args.length>1)filelist = args[1];
								if(args.length>2)if(Integer.parseInt(args[2])==0)useTB=false;
                                                                String outputDir = runNum > 0 ? "plots"+runNum : "plots";
								cndCheckPlots ana = new cndCheckPlots(runNum,outputDir,useTB);
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
								}catch(IOException e){
										e.printStackTrace();
                                                                                System.exit(100);
								}
								int maxevents = 50000000;
								if(args.length>2)maxevents=Integer.parseInt(args[2]);
								int progresscount=0;int filetot = toProcessFileNames.size();
								for (String runstrg : toProcessFileNames) if(count<maxevents){
										progresscount++;
										System.out.println(String.format(">>>>>>>>>>>>>>>> FT %s",runstrg));
										File varTmpDir = new File(runstrg);
										if(!varTmpDir.exists()){System.out.println("FILE DOES NOT EXIST");continue;}
										System.out.println("READING NOW "+runstrg);
										HipoDataSource reader = new HipoDataSource();
										reader.open(runstrg);
										int filecount = 0;
										while(reader.hasEvent() && count<maxevents) {
												DataEvent event = reader.getNextEvent();
												ana.processEvent(event);
												filecount++;count++;
												if(count%10000 == 0) System.out.println(count/1000 + "k events (this is CND on "+runstrg+") progress : " + progresscount+"/"+filetot);
										}
										reader.close();
								}

								System.out.println("Total : " + count + " events");
								ana.fit();
								ana.plot();
								ana.write();
						}
				}

