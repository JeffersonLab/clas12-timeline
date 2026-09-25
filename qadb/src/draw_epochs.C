// called by draw_epochs.sh
//
void draw_epochs(TString dataset, TString datfile, TString epochsfile, Float_t maxNQoverride=0) {

  /////////////////////////////
  // SETTINGS
  Int_t const NBINS = 400; // number of N/q bins (default=400)
  /////////////////////////////

  // open root file
  gStyle->SetOptStat(0);
  TFile * f = new TFile("tree.root","RECREATE");
  TTree * tr = new TTree("tr","tr");
  TString cols = "runnum/I:binnum/I:evnumMin/L:evnumMax/L:timestampMin/L:timestampMax/L";
  cols += ":sector/I:nElec/F:nElecFT/F";
  cols += ":fcstart/F:fcstop/F:ufcstart/F:ufcstop/F";
  cols += ":livetime/F";
  tr->ReadFile(datfile,cols);
  Double_t maxLineY = 16000;

  // get bounds for histograms
  Int_t minRun = 1000000;
  Int_t maxRun = 0;
  Float_t minNQ = 100000;
  Float_t maxNQ = 0;
  Float_t minQ = 100000;
  Float_t maxQ = 0;
  Int_t runnum;
  Float_t nElec,fcstart,fcstop,ufcstart,ufcstop,Q,NQ,UQ;
  Double_t Qtot,UQtot;
  Int_t sector;
  Qtot=UQtot=0;
  tr->SetBranchAddress("runnum",&runnum);
  tr->SetBranchAddress("nElec",&nElec);
  tr->SetBranchAddress("fcstart",&fcstart);
  tr->SetBranchAddress("fcstop",&fcstop);
  tr->SetBranchAddress("ufcstart",&ufcstart);
  tr->SetBranchAddress("ufcstop",&ufcstop);
  tr->SetBranchAddress("sector",&sector);
  for(int x=0; x<tr->GetEntries(); x++) {
    tr->GetEntry(x);
    Q = fcstop - fcstart;
    UQ = ufcstop - ufcstart;
    if(Q>0) {
      NQ = nElec / Q;
      minRun = runnum < minRun ? runnum : minRun;
      maxRun = runnum > maxRun ? runnum : maxRun;
      minNQ = NQ < minNQ ? NQ : minNQ;
      maxNQ = NQ > maxNQ ? NQ : maxNQ;
      minQ = Q < minQ ? Q : minQ;
      maxQ = Q > maxQ ? Q : maxQ;
    };
    if(sector==1) {
      if(Q>0) Qtot += Q;
      if(UQ>0) UQtot += UQ;
    };
  };
  if(maxNQoverride > 0)
    maxNQ = maxNQoverride;
  printf("--------------------------------------------\n");
  printf("total gated FC charge = %.1f mC\n",Qtot*1e-6);
  printf("total ungated FC charge = %.1f mC\n",UQtot*1e-6);
  printf("--------------------------------------------\n");

  // epoch lines
  // - green line is start of epoch
  // - red line is end of epoch 
  // - lines are shifted so they are drawn in bin centers
  const Int_t maxN = 50;
  TLine * eLine[2][maxN];
  int n=0;
  TTree * etr = new TTree("etr","etr");
  etr->ReadFile(epochsfile,"lb/I:ub/I");
  Int_t e[2];
  int color[2] = {kGreen+1,kRed};
  etr->SetBranchAddress("lb",&e[0]);
  etr->SetBranchAddress("ub",&e[1]);
  for(int x=0; x<etr->GetEntries(); x++) {
    etr->GetEntry(x);
    for(int j=0; j<2; j++) {
      auto ypos = j==0 ? e[j] - 0.4 : e[j] + 0.4; // slightly shift overlapping lb and ub lines so they're visible
      eLine[j][n] = new TLine(ypos,0,ypos,maxNQ);
      eLine[j][n]->SetLineColor(color[j]);
      eLine[j][n]->SetLineWidth(3);
    };
    n++;
  };

  // draw everything
  gStyle->SetPalette(kBrownCyan);
  TH2D * h[6];
  TProfile * hp[6];
  TCanvas * c[6];
  TString hN,cN,cut,rundrawNQ,rundrawQ;
  for(int s=0; s<6; s++) {

    hN = Form("sector%d",s+1);
    h[s] = new TH2D(hN,hN+" N/q vs. runnum;runnum;N/q",
      maxRun-minRun+1, minRun-0.5, maxRun+0.5, NBINS, minNQ, maxNQ );
    cut = Form("sector==%d && fcstop-fcstart>0",s+1);
    rundrawNQ = "nElec/(fcstop-fcstart):runnum";
    rundrawQ = "fcstop-fcstart:runnum";

    cN = hN+"canv";
    c[s] = new TCanvas(cN,cN,800,800);

    c[s]->SetGrid(1,1);
    tr->Project(hN,rundrawNQ,cut);
    h[s]->Draw("colz");
    // hp[s] = h[s]->ProfileX();
    // hp[s]->SetLineColor(kRed);
    // hp[s]->SetLineWidth(5);
    // hp[s]->Draw("same");
    for(int k=0; k<n; k++) for(int j=0; j<2; j++) eLine[j][k]->Draw("same");
    c[s]->SetLogz();

    /*
    c[s]->Divide(2,2);
    for(int p=1; p<=4; p++) c[s]->GetPad(p)->SetGrid(0,1);
    c[s]->cd(1);
      tr->Draw("nElec/(fcstop-fcstart):binnum",cut,"*");
    c[s]->cd(2);
      tr->Draw(rundrawNQ,cut,"colz");
      c[s]->GetPad(2)->SetLogz();
      for(int k=0; k<n; k++) for(int j=0; j<2; j++) eLine[j][k]->Draw("same");
    c[s]->cd(3);
      tr->Draw("fcstop-fcstart:binnum",cut,"*");
    c[s]->cd(4);
      tr->Draw(rundrawQ,cut,"colz");
      c[s]->GetPad(4)->SetLogz();
      for(int k=0; k<n; k++) for(int j=0; j<2; j++) eLine[j][k]->Draw("same");
      */

    c[s]->Write();
  };
  tr->Write();
};
