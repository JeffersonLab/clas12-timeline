// make plots from an auxfile
// - requires `AUXFILE = true` in `monitorRead.groovy`

void auxfile_plot(TString auxfile="aux_table_6724.dat") {
  auto tr = new TTree("tr", "tr");
  tr->ReadFile(auxfile);
  TString cut = "";
  cut += "has_run_scaler_bank==1";
  cut += " && evnum%100==0"; // reduce the statistics so this doesn't take too long to run
  tr->Draw("fc:evnum", cut, "*");
}
