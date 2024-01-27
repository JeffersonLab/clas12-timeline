// study differences between charge calculated from RUN::scaler bank, via:
//   1. fcupgated
//   2. <livetime>*fcup
// input: tree.root, produced by mkTree.sh
// see monitorRead.groovy, `FCmode` setting, to know which charge is used

void studyCharge(TString infileN="tree.root") {

  // open tree.root
  TFile * infile = new TFile(infileN,"READ");
  TTree * tr = (TTree*) infile->Get("tr");
  Float_t fcstart,fcstop,ufcstart,ufcstop,livetime;
  Int_t runnum,sector;
  tr->SetBranchAddress("fcstart",&fcstart);
  tr->SetBranchAddress("fcstop",&fcstop);
  tr->SetBranchAddress("ufcstart",&ufcstart);
  tr->SetBranchAddress("ufcstop",&ufcstop);
  tr->SetBranchAddress("livetime",&livetime);
  tr->SetBranchAddress("runnum",&runnum);
  tr->SetBranchAddress("sector",&sector);

  // per file ------------------------
  TH1D * h1 = new TH1D("h1","fcupgated - <livetime>*fcup, per file, zoomed",200,-100,100);
  TH1D * h2 = new TH1D("h2","fcupgated - <livetime>*fcup, per file",1000,-3000,3000);
  TH2D * h3 = new TH2D("h3","fcupgated vs. <livetime>*fcup, per file;<livetime>*fcup;fcupgated",
    100,0,3000,
    100,0,3000);
  // per run ------------------------
  TH1D * r1 = new TH1D("r1","fcupgated - <livetime>*fcup, per run, zoomed",100,-10e3,10e3);
  TH1D * r2 = new TH1D("r2","fcupgated - <livetime>*fcup, per run",1000,-200e3,200e3);
  TH2D * r3 = new TH2D("r3","fcupgated vs. <livetime>*fcup, per run;<livetime>*fcup;fcupgated",
    100,0,1000e3,
    100,0,1000e3);


  // LOOP
  Int_t runnumTmp=-1;
  Double_t chargeFC;
  Double_t chargeUFCLT;
  Double_t chargeFCrun=0;
  Double_t chargeUFCLTrun=0;
  Double_t chargeFCtot=0;
  Double_t chargeUFCLTtot=0;
  for(int i=0; i<tr->GetEntries(); i++) {
    tr->GetEntry(i);

    if(sector!=1) continue;

    chargeFC = fcstop-fcstart;
    chargeUFCLT = livetime*(ufcstop-ufcstart);

    chargeFCrun += chargeFC;
    chargeUFCLTrun += chargeUFCLT;

    chargeFCtot += chargeFC;
    chargeUFCLTtot += chargeUFCLT;

    h1->Fill(chargeFC-chargeUFCLT);
    h2->Fill(chargeFC-chargeUFCLT);
    h3->Fill(chargeUFCLT,chargeFC);

    if(runnumTmp<0) runnumTmp=runnum;
    if(runnum!=runnumTmp) {
      r1->Fill(chargeFCrun-chargeUFCLTrun);
      r2->Fill(chargeFCrun-chargeUFCLTrun);
      r3->Fill(chargeUFCLTrun,chargeFCrun);
      chargeFCrun=0;
      chargeUFCLTrun=0;
      runnumTmp=runnum;
    };
  };

  // draw
  TCanvas * canv1 = new TCanvas();
  canv1->Divide(2,2);
  for(int i=1; i<=4; i++) canv1->GetPad(i)->SetGrid(1,1);
  canv1->cd(1); canv1->GetPad(1)->SetLogy(); h1->Draw();
  canv1->cd(2); canv1->GetPad(2)->SetLogy(); h2->Draw();
  canv1->cd(3); canv1->GetPad(3)->SetLogz(); h3->Draw("colz");
  TCanvas * canv2 = new TCanvas();
  canv2->Divide(2,2);
  for(int i=1; i<=4; i++) canv2->GetPad(i)->SetGrid(1,1);
  canv2->cd(1); canv2->GetPad(1)->SetLogy(); r1->Draw();
  canv2->cd(2); canv2->GetPad(2)->SetLogy(); r2->Draw();
  canv2->cd(3); canv2->GetPad(3)->SetLogz(); r3->Draw("colz");


  // compute total charge, and compare
  printf("fcupgated total charge: %f mC\n",chargeFCtot/1e6);
  printf("<livetime>*fcup total charge: %f mC\n",chargeUFCLTtot/1e6);
  printf("difference: %f mC\n",(chargeFCtot-chargeUFCLTtot)/1e6);


  // compare total charge of runs, rather than charge per file, since longer duration charge should be more accurate
  // -- can do this readingn QADB, but need to add livetime and ungated charge readers to QADB
  // -- or just query the QADB by time bin...
}
