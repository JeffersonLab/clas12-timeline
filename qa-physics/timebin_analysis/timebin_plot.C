// make plots for time bin sizes, etc.

void timebin_plot(
    TString dat_file="data_table.dat" // concatenated table from monitorRead.groovy -> datasetOrganize.sh
    )
{

  gStyle->SetOptStat(0);

  // read the file
  auto tr = new TTree("tr", "tr");
  std::vector<TString> branch_list = {
    "runnum/I",
    ":binnum/I",
    ":eventNumMin/L",
    ":eventNumMax/L",
    ":timestampMin/L",
    ":timestampMax/L",
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
  Long64_t timestampMin;
  Long64_t timestampMax;
  Int_t    sector;
  Long64_t nElecFD;
  Long64_t nElecFT;
  Double_t fcStart;
  Double_t fcStop;
  Double_t ufcStart;
  Double_t ufcStop;
  Double_t aveLiveTime;
  tr->SetBranchAddress("runnum",       &runnum);
  tr->SetBranchAddress("binnum",       &binnum);
  tr->SetBranchAddress("eventNumMin",  &eventNumMin);
  tr->SetBranchAddress("eventNumMax",  &eventNumMax);
  tr->SetBranchAddress("timestampMin", &timestampMin);
  tr->SetBranchAddress("timestampMax", &timestampMax);
  tr->SetBranchAddress("sector",       &sector);
  tr->SetBranchAddress("nElecFD",      &nElecFD);
  tr->SetBranchAddress("nElecFT",      &nElecFT);
  tr->SetBranchAddress("fcStart",      &fcStart);
  tr->SetBranchAddress("fcStop",       &fcStop);
  tr->SetBranchAddress("ufcStart",     &ufcStart);
  tr->SetBranchAddress("ufcStop",      &ufcStop);
  tr->SetBranchAddress("aveLiveTime",  &aveLiveTime);

  // convert a `timestamp` to number of seconds
  auto timestamp_to_sec = [] (decltype(timestampMin) t) -> Long64_t {
    return t * 4e-9;
  };

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
  auto is_primary_bin = [&number_of_bins_map] (Int_t runnum_, Int_t binnum_) {
    return binnum_>0 && binnum_+1<number_of_bins_map[runnum_];
  };

  // get maxima
  Long64_t max_num_events = 0;
  Long64_t max_duration   = 0;
  Double_t max_fc         = 0;
  for(Long64_t e=0; e<tr->GetEntries(); e++) {
    tr->GetEntry(e);
    if(is_primary_bin(runnum, binnum)) {
      max_num_events = std::max(max_num_events, eventNumMax-eventNumMin);
      max_duration   = std::max(max_duration,   timestamp_to_sec(timestampMax-timestampMin));
      max_fc = std::max(max_fc, fcStop-fcStart);
    }
  }
  // since sometimes the FC charge spikes, guess a more reasonable maximum here:
  max_fc = 3000;

  // various checks
  auto warn_check = [&is_primary_bin] (
      decltype(runnum) runnum_,
      decltype(binnum) binnum_,
      decltype(eventNumMin) eventNumMin_,
      decltype(eventNumMax) eventNumMax_,
      TString message_
      )
  {
    std::cerr
      << "WARNING: runnum=" << runnum_
      << " binnum=" << binnum_
      << " eventNumMax-Min=" << eventNumMax_-eventNumMin_
      << ": " << message_;
    if(!is_primary_bin(runnum_, binnum_))
      std::cerr << " -- TERMINAL BIN";
    std::cerr << std::endl;
  };
  decltype(runnum) runnum_prev = 0;
  decltype(fcStop) fcStop_prev = 0;
  for(Long64_t e=0; e<tr->GetEntries(); e++) {
    tr->GetEntry(e);
    if(sector!=1) continue;
    if(runnum != runnum_prev) runnum_prev = runnum;
    if(binnum!=1 && is_primary_bin(runnum,binnum)) { // don't do this check on the first 2 bins or the last bin, since the first and last bins' charge range is [0, 0]
      if(fcStart != fcStop_prev)
        warn_check(runnum, binnum, eventNumMin, eventNumMax, "fcStart is not fcStop of previous bin");
    }
    /* // ignore these, since we'll use a defect bit instead
    if(fcStart > fcStop)
      warn_check(runnum, binnum, eventNumMin, eventNumMax, Form("gated FC charge is negative: %f", fcStop - fcStart));
    if(ufcStart > ufcStop)
      warn_check(runnum, binnum, eventNumMin, eventNumMax, Form("ungated FC charge is negative: %f", ufcStop - ufcStart));
    */
    fcStop_prev = fcStop;
  }

  // define histograms
  auto num_events_primary = new TH1D(
      "num_events_primary",
      "Number of Events per Primary Time Bin;Num Events",
      1000,
      0,
      max_num_events + 1
      );
  auto num_events_terminal = new TH1D(
      "num_events_terminal",
      "Number of Events per Terminal Time Bin;Num Events",
      1000,
      num_events_primary->GetXaxis()->GetXmin(),
      1000
      );

  auto duration_primary = new TH1D(
      "duration_primary",
      "Time Bin Duration per Primary Time Bin;Duration [s]",
      max_duration + 1,
      0,
      max_duration + 1
      );
  auto duration_terminal = new TH1D(
      "duration_terminal",
      "Time Bin Duration per Terminal Time Bin;Duration [s]",
      100,
      0,
      100
      );

  auto num_events_vs_runnum = new TH2D(
      "num_events_vs_runnum",
      "Number of Primary Events vs. Run Number;Run Number;Num Events",
      runnum_nbins,
      runnum_min,
      runnum_max + 1,
      num_events_primary->GetNbinsX(),
      num_events_primary->GetXaxis()->GetXmin(),
      num_events_primary->GetXaxis()->GetXmax()
      );
  auto num_events_vs_charge = new TH2D(
      "num_events_vs_charge",
      "Number of Primary Events vs. Gated FC Charge;Charge [nC];Num Events",
      1000,
      0,
      max_fc,
      num_events_primary->GetNbinsX(),
      num_events_primary->GetXaxis()->GetXmin(),
      num_events_primary->GetXaxis()->GetXmax()
      );

  auto duration_vs_num_events = new TH2D(
      "duration_vs_num_events",
      "Duration of Primary Bins vs. Number of Events;Num Events;Duration [s]",
      num_events_primary->GetNbinsX(),
      num_events_primary->GetXaxis()->GetXmin(),
      num_events_primary->GetXaxis()->GetXmax(),
      duration_primary->GetNbinsX(),
      duration_primary->GetXaxis()->GetXmin(),
      duration_primary->GetXaxis()->GetXmax()
      );

  auto duration_vs_runnum = new TH2D(
      "duration_vs_runnum",
      "Duration of Primary Bins vs. Run Number;Run Number;Duration [s]",
      runnum_nbins,
      runnum_min,
      runnum_max + 1,
      duration_primary->GetNbinsX(),
      duration_primary->GetXaxis()->GetXmin(),
      duration_primary->GetXaxis()->GetXmax()
      );
  auto duration_vs_charge = new TH2D(
      "duration_vs_charge",
      "Duration of Primary Bins vs. Gated FC Charge;Charge [nC];Duration [s]",
      1000,
      0,
      max_fc,
      duration_primary->GetNbinsX(),
      duration_primary->GetXaxis()->GetXmin(),
      duration_primary->GetXaxis()->GetXmax()
      );

  auto run_duration = new TH1D(
      "run_duration",
      "Duration of each run;Run Number;Duration [min]",
      runnum_nbins,
      runnum_min,
      runnum_max + 1
      );

  // fill histograms
  for(Long64_t e=0; e<tr->GetEntries(); e++) {
    tr->GetEntry(e);
    if(sector!=1) continue;

    // get the number of events
    auto num_events = eventNumMax - eventNumMin;
    if(binnum==0) num_events++; // since first bin has no lower bound

    // get the duration
    auto duration = timestamp_to_sec(timestampMax - timestampMin);

    // fill histograms
    if(is_primary_bin(runnum, binnum)) {
      num_events_primary->Fill(num_events);
      duration_primary->Fill(duration);
      duration_vs_num_events->Fill(num_events, duration);
      num_events_vs_runnum->Fill(runnum, num_events);
      num_events_vs_charge->Fill(fcStop-fcStart, num_events);
      duration_vs_runnum->Fill(runnum, duration);
      duration_vs_charge->Fill(fcStop-fcStart, duration);
    }
    else {
      num_events_terminal->Fill(num_events);
      duration_terminal->Fill(duration);
    }
    run_duration->Fill(runnum, duration / 60.0);
  }

  // check for underflow and overflow
  for(auto& hist : {num_events_primary, num_events_terminal, duration_primary, duration_terminal}) {
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
  for(auto& hist : {duration_primary, duration_terminal}) {
    hist->SetFillColor(kOrange+1);
    hist->SetLineColor(kOrange+1);
  }
  run_duration->SetFillColor(kBlue);
  run_duration->SetLineColor(kBlue);

  // draw
  const int canvW = 800;
  const int canvH = 600;
  auto canv0 = new TCanvas("canv0", "canv0", 3*canvW, canvH);
  canv0->Divide(3,2);
  for(int i=1; i<=6; i++) {
    canv0->GetPad(i)->SetGrid(1,1);
    canv0->GetPad(i)->SetLeftMargin(0.15);
    canv0->GetPad(i)->SetBottomMargin(0.15);
    canv0->GetPad(i)->SetRightMargin(0.15);
  }
  canv0->cd(1);
  canv0->GetPad(1)->SetLogy();
  num_events_primary->Draw();
  canv0->cd(2);
  canv0->GetPad(2)->SetLogy();
  num_events_terminal->Draw();
  canv0->cd(3);
  canv0->GetPad(3)->SetLogz();
  duration_vs_num_events->Draw("colz");
  canv0->cd(4);
  canv0->GetPad(4)->SetLogy();
  duration_primary->Draw();
  canv0->cd(5);
  canv0->GetPad(5)->SetLogy();
  duration_terminal->Draw();

  auto canv1 = new TCanvas("canv1", "canv1", 2*canvW, canvH);
  canv1->Divide(2,2);
  for(int i=1; i<=4; i++) {
    canv1->GetPad(i)->SetGrid(1,1);
    canv1->GetPad(i)->SetLogz();
    canv1->GetPad(i)->SetLeftMargin(0.15);
    canv1->GetPad(i)->SetBottomMargin(0.15);
    canv1->GetPad(i)->SetRightMargin(0.15);
  }
  canv1->cd(1);
  num_events_vs_runnum->Draw("colz");
  canv1->cd(2);
  num_events_vs_charge->Draw("colz");
  canv1->cd(3);
  duration_vs_runnum->Draw("colz");
  canv1->cd(4);
  duration_vs_charge->Draw("colz");

  auto canv2 = new TCanvas("canv2", "canv2", canvW, canvH);
  canv2->SetGrid(1,1);
  canv2->SetLeftMargin(0.15);
  canv2->SetBottomMargin(0.15);
  canv2->SetRightMargin(0.15);
  run_duration->Draw("hist");
}
