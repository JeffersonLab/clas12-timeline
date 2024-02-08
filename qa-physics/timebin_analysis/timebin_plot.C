// make plots for time bin sizes, etc.

void timebin_plot(
    TString dat_file="data_table.dat" // concatenated table from monitorRead.groovy -> datasetOrganize.sh
    )
{

  gStyle->SetOptStat(0);

  const Int_t MIN_NUM_SCALERS = 2000;   // at least this many scaler readouts per time bin; must match that in ../monitorRead.groovy
  const Int_t NUM_EVENTS_WITH_NO_BEAM = MIN_NUM_SCALERS * 40; // bins with no beam should have this many events

  // read the file
  auto tr = new TTree("tr", "tr");
  std::vector<TString> branch_list = {
    "runnum/I",
    ":binnum/I",
    ":eventNumMin/L",
    ":eventNumMax/L",
    ":sector/I",
    ":nElecFD/L",
    ":nElecFT/L",
    ":fcStart/D",
    ":fcStop/D",
    ":ufcStart/D",
    ":ufcStop/D",
    ":aveLiveTime/D"
  };
  TString branch_list_joined = "";
  for(auto branch : branch_list)
    branch_list_joined += branch;
  tr->ReadFile(dat_file, branch_list_joined.Data());
  Int_t    runnum;
  Int_t    binnum;
  Long64_t eventNumMin;
  Long64_t eventNumMax;
  Int_t    sector;
  Long64_t nElecFD;
  Long64_t nElecFT;
  Double_t fcStart;
  Double_t fcStop;
  Double_t ufcStart;
  Double_t ufcStop;
  Double_t aveLiveTime;
  tr->SetBranchAddress("runnum",      &runnum);
  tr->SetBranchAddress("binnum",      &binnum);
  tr->SetBranchAddress("eventNumMin", &eventNumMin);
  tr->SetBranchAddress("eventNumMax", &eventNumMax);
  tr->SetBranchAddress("sector",      &sector);
  tr->SetBranchAddress("nElecFD",     &nElecFD);
  tr->SetBranchAddress("nElecFT",     &nElecFT);
  tr->SetBranchAddress("fcStart",     &fcStart);
  tr->SetBranchAddress("fcStop",      &fcStop);
  tr->SetBranchAddress("ufcStart",    &ufcStart);
  tr->SetBranchAddress("ufcStop",     &ufcStop);
  tr->SetBranchAddress("aveLiveTime", &aveLiveTime);

  // get run number range
  Int_t runnum_min = tr->GetMinimum("runnum");
  Int_t runnum_max = tr->GetMaximum("runnum");
  auto runnum_nbins = runnum_max - runnum_min + 1;

  // get the number of bins for each run number
  std::unordered_map<Int_t,Int_t> number_of_bins_map;
  for(Long64_t e=0; e<tr->GetEntries(); e++) {
    tr->GetEntry(e);
    auto it = number_of_bins_map.find(runnum);
    if(it == number_of_bins_map.end())
      number_of_bins_map.insert({runnum, binnum});
    else if(binnum > it->second)
      number_of_bins_map[runnum] = binnum;
  }
  for(const auto& [r,b] : number_of_bins_map)
    number_of_bins_map[r]++; // if bin number starts at zero, make sure it is counted

  // check if a certain bin is a primary bin (i.e., not a terminal bin)
  auto is_primary_bin = [] (Int_t binnum_, Int_t number_of_bins_) {
    return binnum_>0 && binnum_+1<number_of_bins_;
  };

  // get maxima
  Long64_t max_num_events = 0;
  Double_t max_fc         = 0;
  for(Long64_t e=0; e<tr->GetEntries(); e++) {
    tr->GetEntry(e);
    if(is_primary_bin(binnum, number_of_bins_map[runnum])) {
      max_num_events = std::max(max_num_events, eventNumMax-eventNumMin);
      max_fc = std::max(max_fc, fcStop-fcStart);
    }
  }

  // define histograms
  auto num_events_primary = new TH1D(
      "num_events_primary",
      "Number of Events per Non-Terminal Time Bin",
      1000,
      0,
      max_num_events + 1
      );
  auto num_events_terminal = new TH1D(
      "num_events_terminal",
      "Number of Events per Terminal Time Bin",
      1000,
      num_events_primary->GetXaxis()->GetXmin(),
      1000
      );
  auto num_events_vs_runnum = new TH2D(
      "num_events_vs_runnum",
      "Number of Non-Terminal-Time-Bin Events vs. Run Number;Run Number;Num Events",
      runnum_nbins,
      runnum_min,
      runnum_max + 1,
      num_events_primary->GetNbinsX(),
      num_events_primary->GetXaxis()->GetXmin(),
      num_events_primary->GetXaxis()->GetXmax()
      );
  auto num_events_vs_charge = new TH2D(
      "num_events_vs_charge",
      "Number of Non-Terminal-Time-Bin Events vs. Gated FC Charge;Charge;Num Events",
      1000,
      0,
      max_fc,
      num_events_primary->GetNbinsX(),
      num_events_primary->GetXaxis()->GetXmin(),
      num_events_primary->GetXaxis()->GetXmax()
      );

  // MAIN LOOP
  for(Long64_t e=0; e<tr->GetEntries(); e++) {
    tr->GetEntry(e);

    // get the number of events
    auto num_events = eventNumMax - eventNumMin;
    if(binnum==0) num_events++; // since first bin has no lower bound

    // fill histograms
    if(is_primary_bin(binnum, number_of_bins_map[runnum])) {
      num_events_primary->Fill(num_events);
      num_events_vs_runnum->Fill(runnum, num_events);
      num_events_vs_charge->Fill(fcStop-fcStart, num_events);
    }
    else {
      num_events_terminal->Fill(num_events);
    }
  }

  // check for underflow and overflow
  for(auto& hist : {num_events_primary, num_events_terminal}) {
    auto underflow = hist->GetBinContent(0);
    auto overflow  = hist->GetBinContent(hist->GetNbinsX()+1);
    if(underflow>0) std::cerr << "WARNING: histogram '" << hist->GetName() << "' has underflow of " << underflow << std::endl;
    if(overflow>0)  std::cerr << "WARNING: histogram '" << hist->GetName() << "' has overflow  of " << overflow  << std::endl;
  }

  // format histograms
  for(auto& hist : {num_events_primary, num_events_terminal}) {
    hist->SetFillColor(kCyan+2);
    hist->SetLineColor(kCyan+2);
  }

  // draw
  auto canv0 = new TCanvas("canv0", "canv0", 1600, 600);
  canv0->Divide(2,1);
  for(int i=1; i<=2; i++) canv0->GetPad(i)->SetGrid(1,1);
  canv0->cd(1);
  num_events_primary->Draw();
  canv0->cd(2);
  num_events_terminal->Draw();

  auto canv1 = new TCanvas("canv1", "canv1", 800, 600);
  canv1->SetGrid(1,1);
  canv1->SetLogz();
  num_events_vs_runnum->Draw("colz");

  auto canv2 = new TCanvas("canv2", "canv2", 800, 600);
  canv1->SetGrid(1,1);
  num_events_vs_charge->Draw("colz");
}
