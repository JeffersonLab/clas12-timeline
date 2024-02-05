// make plots for time bin sizes, etc.

void time_bin_plot(TString in_file="time_bins.dat") {
  auto tr = new TTree("tr", "tr");
  tr->ReadFile(in_file);
}
