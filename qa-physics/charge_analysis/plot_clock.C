void plot_clock(TString out_dir, TString suffix, Int_t do_fit) {

  TString basename = out_dir + "/clock_" + suffix;
  TFile* out_file = new TFile(basename+".root", "RECREATE");

  TTree* tr = new TTree("tr", "tr");
  tr->ReadFile(basename+".dat");
  Long64_t clock_gated, clock_ungated, timestamp;
  tr->SetBranchAddress("clock_gated", &clock_gated);
  tr->SetBranchAddress("clock_ungated", &clock_ungated);
  tr->SetBranchAddress("timestamp", &timestamp);

  Long64_t timestamp_max = 0;
  Long64_t timestamp_min = 1000e9;

  TGraph* gr_g = new TGraph();
  TGraph* gr_u = new TGraph();
  gr_g->SetName("clock_gated");
  gr_u->SetName("clock_ungated");
  gr_g->SetTitle("gated clock vs. timestamp");
  gr_u->SetTitle("ungated clock vs. timestamp");
  gr_g->SetMarkerStyle(kFullCircle);
  gr_u->SetMarkerStyle(kFullCircle);
  gr_g->SetMarkerColor(kRed);
  gr_u->SetMarkerColor(kMagenta);

  for(Long64_t e = 0; e < tr->GetEntries(); e++) {
    tr->GetEntry(e);
    gr_g->AddPoint(timestamp, clock_gated);
    gr_u->AddPoint(timestamp, clock_ungated);
    timestamp_max = std::max(timestamp, timestamp_max);
    timestamp_min = std::min(timestamp, timestamp_min);
  }

  if(do_fit == 1) {
    gr_g->Fit("pol1", "", "", timestamp_min, timestamp_max);
    gr_u->Fit("pol1", "", "", timestamp_min, timestamp_max);

    auto fun_g = gr_g->GetFunction("pol1");
    auto fun_u = gr_u->GetFunction("pol1");
    auto slp_g = fun_g->GetParameter(1);
    auto slp_u = fun_u->GetParameter(1);

    fun_g->SetLineColor(kBlack);
    fun_u->SetLineColor(kBlack);

    TCanvas* canv = new TCanvas("canv", "canv", 800, 2*600);
    canv->Divide(1,2);
    canv->GetPad(1)->SetGrid(1,1);
    canv->GetPad(2)->SetGrid(1,1);
    canv->cd(1);
    gr_g->Draw("APE");
    fun_g->Draw("SAME");
    canv->cd(2);
    gr_u->Draw("APE");
    fun_u->Draw("SAME");
    canv->Write("canv");

    auto slp2freq = [] (auto slp) {
      auto freq_hz = slp / 4e-9; // convert denominator of `slp` from `timestamp` to `duration [s]`
      return freq_hz / 1e6; // convert `Hz` -> `MHz`
    };
    auto freq_g = slp2freq(slp_g);
    auto freq_u = slp2freq(slp_u);

    std::cout << "[clock_freq_result]: " << suffix << " " << freq_g << " " << freq_u << "\n";

    std::cout << "\n";
    std::cout << "===========================\n";
    std::cout << "estimated clock frequencies\n";
    std::cout << "===========================\n";
    std::cout << "   gated: " << freq_g << " MHz\n";
    std::cout << " ungated: " << freq_u << " MHz\n";
    std::cout << "===========================\n";
    std::cout << "\n";
  }

  gr_g->Write("gr_g");
  gr_u->Write("gr_u");
  tr->Write("tr");

  out_file->Close();
  std::cout << "WROTE " << basename << ".root\n";
}
