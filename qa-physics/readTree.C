// called by mkTree.sh
//
void readTree(TString dataset="fall18") {

  /////////////////////////////
  // SETTINGS
  Int_t NBINS = 2000; // number of N/F bins (default=200)
  /////////////////////////////

  // open root file
  gStyle->SetOptStat(0);
  TFile * f = new TFile("tree.root","RECREATE");
  TTree * tr = new TTree("tr","tr");
  TString cols = "i/I:runnum/I:binnum/I:evnumMin/L:evnumMax/L:timestampMin/L:timestampMax/L";
  cols += ":sector/I:nElec/F:nElecFT/F";
  cols += ":fcstart/F:fcstop/F:ufcstart/F:ufcstop/F";
  cols += ":livetime/F";
  tr->ReadFile("tree.tmp",cols);
  Double_t maxLineY = 16000;

  // draw epoch lines
  // - green line is start of epoch
  // - red line is end of epoch 
  // - lines are shifted so they are drawn in bin centers
  const Int_t maxN = 50;
  TLine * eLine[2][maxN];
  int n=0;
  TTree * etr = new TTree("etr","etr");
  etr->ReadFile(TString("epochs/epochs."+dataset+".txt"),"lb/I:ub/I");
  Int_t e[2];
  int color[2] = {kGreen+1,kRed};
  etr->SetBranchAddress("lb",&e[0]);
  etr->SetBranchAddress("ub",&e[1]);
  for(int x=0; x<etr->GetEntries(); x++) {
    etr->GetEntry(x);
    for(int j=0; j<2; j++) {
      eLine[j][n] = new TLine(e[j]+0.5,0,e[j]+0.5,maxLineY);
      eLine[j][n]->SetLineColor(color[j]);
      eLine[j][n]->SetLineWidth(3);
    };
    n++;
  };


  // get bounds for histograms
  Int_t minRun = 1000000;
  Int_t maxRun = 0;
  Float_t minNF = 100000;
  Float_t maxNF = 0;
  Float_t minF = 100000;
  Float_t maxF = 0;
  Int_t runnum;
  Float_t nElec,fcstart,fcstop,ufcstart,ufcstop,F,NF,UF;
  Double_t Ftot,UFtot;
  Int_t sector;
  Ftot=UFtot=0;
  tr->SetBranchAddress("runnum",&runnum);
  tr->SetBranchAddress("nElec",&nElec);
  tr->SetBranchAddress("fcstart",&fcstart);
  tr->SetBranchAddress("fcstop",&fcstop);
  tr->SetBranchAddress("ufcstart",&ufcstart);
  tr->SetBranchAddress("ufcstop",&ufcstop);
  tr->SetBranchAddress("sector",&sector);
  for(int x=0; x<tr->GetEntries(); x++) {
    tr->GetEntry(x);
    F = fcstop - fcstart;
    UF = ufcstop - ufcstart;
    if(F>0) {
      NF = nElec / F;
      minRun = runnum < minRun ? runnum : minRun;
      maxRun = runnum > maxRun ? runnum : maxRun;
      minNF = NF < minNF ? NF : minNF;
      maxNF = NF > maxNF ? NF : maxNF;
      minF = F < minF ? F : minF;
      maxF = F > maxF ? F : maxF;
    };
    if(sector==1) {
      if(F>0) Ftot += F;
      if(UF>0) UFtot += UF;
    };
  };
  //maxNF = 15; // override maxNF
  printf("--------------------------------------------\n");
  printf("total gated FC charge = %.1f mC\n",Ftot*1e-6);
  printf("total ungated FC charge = %.1f mC\n",UFtot*1e-6);
  printf("--------------------------------------------\n");


  // draw everything
  gStyle->SetPalette(kBrownCyan);
  TH2D * h[6];
  TProfile * hp[6];
  TCanvas * c[6];
  TString hN,cN,cut,rundrawNF,rundrawF;
  for(int s=0; s<6; s++) {

    hN = Form("sector%d",s+1);
    h[s] = new TH2D(hN,hN+" N/F vs. runnum;runnum;N/F",
      maxRun-minRun, minRun, maxRun, NBINS, minNF, maxNF );
    cut = Form("sector==%d && fcstop-fcstart>0",s+1);
    rundrawNF = "nElec/(fcstop-fcstart):runnum";
    rundrawF = "fcstop-fcstart:runnum";

    cN = hN+"canv";
    c[s] = new TCanvas(cN,cN,800,800);

    ///*
    c[s]->SetGrid(1,1);
    tr->Project(hN,rundrawNF,cut);
    h[s]->Draw("colz");
    hp[s] = h[s]->ProfileX();
    hp[s]->SetLineColor(kRed);
    hp[s]->SetLineWidth(5);
    hp[s]->Draw("same");
    for(int k=0; k<n; k++) for(int j=0; j<2; j++) eLine[j][k]->Draw("same");
    //c[s]->SetLogz();
    //*/

    /*
    c[s]->Divide(2,2);
    for(int p=1; p<=4; p++) c[s]->GetPad(p)->SetGrid(0,1);
    c[s]->cd(1);
      tr->Draw("nElec/(fcstop-fcstart):i",cut,"*");
    c[s]->cd(2);
      tr->Draw(rundrawNF,cut,"colz");
      c[s]->GetPad(2)->SetLogz();
      for(int k=0; k<n; k++) for(int j=0; j<2; j++) eLine[j][k]->Draw("same");
    c[s]->cd(3);
      tr->Draw("fcstop-fcstart:i",cut,"*");
    c[s]->cd(4);
      tr->Draw(rundrawF,cut,"colz");
      c[s]->GetPad(4)->SetLogz();
      for(int k=0; k<n; k++) for(int j=0; j<2; j++) eLine[j][k]->Draw("same");
      */

    c[s]->Write();
  };
  tr->Write();
};
