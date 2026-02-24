void plot_clock(TString out_dir, TString suffix) {
  TString basename = out_dir + "/clock_" + suffix;
  TFile* o = new TFile(basename+".root", "RECREATE");
  TTree* tr = new TTree("tr", "tr");
  tr->ReadFile(basename+".dat");
  tr->Write();
  o->Close();
  std::cout << "WROTE " << basename << ".root\n";
  std::cout << "Recommended plot commands:" << std::endl;
  std::cout << "  tr->Draw(\"clock_ungated:timestamp\")" << std::endl;
  std::cout << "  tr->Draw(\"clock_gated:timestamp\")" << std::endl;
}
