package org.jlab.clas.timeline.histograms;
import java.util.*;


import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.TDirectory;
import org.jlab.clas.physics.Vector3;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.ui.PaveText;
import org.jlab.groot.base.PadAttributes;
import java.util.ArrayList;

public class RICH{
  boolean userTimeBased;
  int runNum;
  String outputDir;
  boolean[] trigger_bits;
  public float EBeam;

  public static int LundElectron = 11;
  public static int LundPiplus = 211;
  public static int LundPiminus = -211;
  public static int LundKplus = 321;
  public static int LundKminus = -321;
  public static int LundProton = 2212;
  public static int LundPbar = -2212;

  public static float PionMass = 0.13957f;
  public static float KaonMass = 0.493677f;
  public static float ProtonMass = 0.493677f;

  public static final int nPMTS = 391, nANODES = 64, NTBITS = 32;
  public static int nSECTORS = 6;
  public static int nMODULES = 2;
  public static int nLAYERS = 3;
  public static int nMAXTILES = 50;
  public static int nTOP = 3;
  public static int RICHUSEDFLAG = 111;
  //public static int RICHUSEDFLAG = 11;

  public static float pixelsize = 0.6f;

  public int BINWINDOW;
  public PaveText statBox = null;
  public PadAttributes attr1;

  public H2F[] H_dt_channel = new H2F[nMODULES];
  public H1F[] H_dt = new H1F[nMODULES];
  public H1F[] H_dt_RMS = new H1F[nMODULES];
  public H1F[] H_dt_MEAN = new H1F[nMODULES];
  public H1F[][] H_dt_PMT = new H1F[nPMTS][nMODULES];

  public H2F[][] H_detac_tile = new H2F[nMODULES][nTOP];
  public H2F[][] H_npho_tile = new H2F[nMODULES][nTOP];
  public H1F[] H_trk_match = new H1F[nMODULES];

  public int Ntrig = 0;

  public H1F H_npip_tile[] = new H1F[nMODULES];
  public H1F H_npim_tile[] = new H1F[nMODULES];
  public H1F H_nkp_tile[] = new H1F[nMODULES];
  public H1F H_nkm_tile[] = new H1F[nMODULES];
  public H1F H_npro_tile[] = new H1F[nMODULES];
  public H1F H_npbar_tile[] = new H1F[nMODULES];

  public H1F H_ntrigele;

  public H1F H_setup;

  public int eventN;

  public int ModuleInSector[] = new int[6];
  public float AerogelRefIndex[][][] = new float[nMODULES][nLAYERS][nMAXTILES];
  public int LastTile[][] = new int[nMODULES][nLAYERS];

  public ConstantsManager ccdb;
  public IndexedTable setupTable;
  public IndexedTable aerogelTable;

  public static boolean verbose = false;


  public RICH(int reqR, String reqOutputDir, float reqEb, boolean reqTimeBased){
    runNum = reqR;userTimeBased=reqTimeBased;
    outputDir = reqOutputDir;
    EBeam = reqEb;
    BINWINDOW = 14;

    ccdb = new ConstantsManager();

    String rich_setup = "/geometry/rich/setup";
    String rich_aerogel[] = new String[nMODULES];
    for (int m=1; m<=nMODULES; m++) rich_aerogel[m-1] = String.format("/geometry/rich/module%d/aerogel", m);

    ccdb.init(Arrays.asList(new String[]{rich_setup, rich_aerogel[0], rich_aerogel[1]}));
    setupTable = ccdb.getConstants(runNum, rich_setup);
    for (int s=1; s<=6; s++) {
      if (setupTable.hasEntry(s, 0, 0)) {
        ModuleInSector[s-1] = setupTable.getIntValue("module", s, 0, 0);
      }
    }
    //System.out.println(String.format("  mod[1]=" + ModuleInSector[0] + "  mod[4]=" + ModuleInSector[3]));


    for (int m=1; m<=nMODULES; m++) {
      aerogelTable = ccdb.getConstants(runNum, rich_aerogel[m-1]);
      int sector = GetSector(m);

      for (int l=0; l<nLAYERS; l++) {
        int layer = l + 201;
        for (int t=1; t<=nMAXTILES-1; t++) {
          if (aerogelTable.hasEntry(sector, layer, t)) {
            AerogelRefIndex[m-1][l][t] = (float)aerogelTable.getDoubleValue("n400", sector, layer, t);
            LastTile[m-1][l] = t;
            //System.out.println(String.format(" " + m + " " + sector + " " + layer + " " + t + " " + AerogelRefIndex[m-1][l][t]));

          }
          else {
            //System.out.println(String.format("ERROR missing entry for m=" + m + " sector=" + sector + "  layer=" + layer + " tile=" + t));
            AerogelRefIndex[m-1][l][t] = 1;
          }
        }
      }
    }


    trigger_bits = new boolean[NTBITS];

    String histitle, histname;

    for (int m=1; m<=nMODULES; m++) {

      histitle = String.format("RICH Module %d, DeltaT Photons", m);
      histname = String.format("H_RICH_dt_m%d", m);
      H_dt[m-1] = new H1F(histname, histitle, 500, -150, 50);
      H_dt[m-1].setTitle(histitle);
      H_dt[m-1].setTitleX("T_meas - T_calc (ns)");
      H_dt[m-1].setTitleY("counts");
      H_dt[m-1].setOptStat("1111111");

      histitle = String.format("RICH Module %d, DeltaT vs channel", m);
      histname = String.format("H_RICH_dt_channel_m%d", m);
      H_dt_channel[m-1] = new H2F(histname, histitle, nPMTS*nANODES, 0.5, 0.5+nPMTS*nANODES, 500, -150, 50);
      H_dt_channel[m-1].setTitle(histitle);
      H_dt_channel[m-1].setTitleX("channel");
      H_dt_channel[m-1].setTitleY("T_meas - T_calc (ns)");

      histitle = String.format("RICH Module %d, MEAN of DeltaT within BINWINDOW bins around the Max", m);
      histname = String.format("H_RICH_dt_MEAN_m%d", m);
      H_dt_MEAN[m-1] = new H1F(histname, histitle, nPMTS, 0.5, 0.5+nPMTS);
      H_dt_MEAN[m-1].setTitle(histitle);
      H_dt_MEAN[m-1].setTitleX("PMT number");
      H_dt_MEAN[m-1].setTitleY("MEAN of (T_meas - T_calc) (ns)");
      H_dt_MEAN[m-1].setOptStat("1111111");

      histitle = String.format("RICH Module %d, RMS of DeltaT within BINWINDOW bins around the Max", m);
      histname = String.format("H_RICH_dt_RMS_m%d", m);
      H_dt_RMS[m-1] = new H1F(histname, histitle, nPMTS, 0.5, 0.5+nPMTS);
      H_dt_RMS[m-1].setTitle(histitle);
      H_dt_RMS[m-1].setTitleX("PMT number");
      H_dt_RMS[m-1].setTitleY("RMS of (T_meas - T_calc) (ns)");
      H_dt_RMS[m-1].setOptStat("1111111");


      for (int top=1; top<=nTOP; top++) {
        if (top == 1) histitle = String.format("RICH Module %d, EtaC_meas - EtaC_calc vs tile (direct)", m);
        else if (top == 2) histitle = String.format("RICH Module %d, EtaC_meas - EtaC_calc vs tile (planar)", m);
        else if (top == 3) histitle = String.format("RICH Module %d, EtaC_meas - EtaC_calc vs tile (spherical)", m);
        histname = String.format("H_RICH_detac_m%d_top%d", m, top);
        H_detac_tile[m-1][top-1] = new H2F(histname, histitle, nLAYERS*nMAXTILES, -0.5, nLAYERS*nMAXTILES-0.5, 200, -50, 50);
        H_detac_tile[m-1][top-1].setTitle(histitle);
        H_detac_tile[m-1][top-1].setTitleX("Aerogel tile number");
        H_detac_tile[m-1][top-1].setTitleY("(EtaC_meas - EtaC_calc) (mrad)");

        if (top == 1) histitle = String.format("RICH Module %d, Number of photons per track vs tile (direct)", m);
        else if (top == 2) histitle = String.format("RICH Module %d, Number of photons per track vs tile (planar)", m);
        else if (top == 3) histitle = String.format("RICH Module %d, Number of photons per track vs tile (spherical)", m);
        histname = String.format("H_RICH_npho_m%d_top%d", m, top);
        H_npho_tile[m-1][top-1] = new H2F(histname, histitle, nLAYERS*nMAXTILES, -0.5, nLAYERS*nMAXTILES-0.5, 50, -0.5, 49.5);
        H_npho_tile[m-1][top-1].setTitle(histitle);
        H_npho_tile[m-1][top-1].setTitleX("Aerogel tile number");
        H_npho_tile[m-1][top-1].setTitleY("Number of photons");
      }

      histitle = String.format("RICH Module %d, Track matching with clusters", m);
      histname = String.format("H_RICH_trk_match_m%d", m);
      H_trk_match[m-1] = new H1F(histname, histitle, 100, 0, 10);
      H_trk_match[m-1].setTitle(histitle);
      H_trk_match[m-1].setTitleX("Distance (cm)");
      H_trk_match[m-1].setOptStat("1111111");


      histitle = String.format("RICH Module %d, Number of pi+ vs tile", m);
      histname = String.format("H_RICH_npip_tile_m%d", m);
      H_npip_tile[m-1] = new H1F(histname, histitle, nLAYERS*nMAXTILES, -0.5, nLAYERS*nMAXTILES-0.5);
      H_npip_tile[m-1].setTitle(histitle);
      H_npip_tile[m-1].setTitleX("Aerogel tile number");
      H_npip_tile[m-1].setTitleY("N(pi+)");
      H_npip_tile[m-1].setOptStat("1111111");


      histitle = String.format("RICH Module %d, Number of pi- vs tile", m);
      histname = String.format("H_RICH_npim_tile_m%d", m);
      H_npim_tile[m-1] = new H1F(histname, histitle, nLAYERS*nMAXTILES, -0.5, nLAYERS*nMAXTILES-0.5);
      H_npim_tile[m-1].setTitle(histitle);
      H_npim_tile[m-1].setTitleX("Aerogel tile number");
      H_npim_tile[m-1].setTitleY("N(pi+)");
      H_npim_tile[m-1].setOptStat("1111111");


      histitle = String.format("RICH Module %d, Number of K+ vs tile", m);
      histname = String.format("H_RICH_nkp_tile_m%d", m);
      H_nkp_tile[m-1] = new H1F(histname, histitle, nLAYERS*nMAXTILES, -0.5, nLAYERS*nMAXTILES-0.5);
      H_nkp_tile[m-1].setTitle(histitle);
      H_nkp_tile[m-1].setTitleX("Aerogel tile number");
      H_nkp_tile[m-1].setTitleY("N(K+)");
      H_nkp_tile[m-1].setOptStat("1111111");


      histitle = String.format("RICH Module %d, Number of K- per trigger vs tile", m);
      histname = String.format("H_RICH_nkm_tile_m%d", m);
      H_nkm_tile[m-1] = new H1F(histname, histitle, nLAYERS*nMAXTILES, -0.5, nLAYERS*nMAXTILES-0.5);
      H_nkm_tile[m-1].setTitle(histitle);
      H_nkm_tile[m-1].setTitleX("Aerogel tile number");
      H_nkm_tile[m-1].setTitleY("N(K+)");
      H_nkm_tile[m-1].setOptStat("1111111");


      histitle = String.format("RICH Module %d, Number of protons vs tile", m);
      histname = String.format("H_RICH_npro_tile_m%d", m);
      H_npro_tile[m-1] = new H1F(histname, histitle, nLAYERS*nMAXTILES, -0.5, nLAYERS*nMAXTILES-0.5);
      H_npro_tile[m-1].setTitle(histitle);
      H_npro_tile[m-1].setTitleX("Aerogel tile number");
      H_npro_tile[m-1].setTitleY("N(p)");
      H_npro_tile[m-1].setOptStat("1111111");


      histitle = String.format("RICH Module %d, Number of pbar vs tile", m);
      histname = String.format("H_RICH_npbar_tile_m%d", m);
      H_npbar_tile[m-1] = new H1F(histname, histitle, nLAYERS*nMAXTILES, -0.5, nLAYERS*nMAXTILES-0.5);
      H_npbar_tile[m-1].setTitle(histitle);
      H_npbar_tile[m-1].setTitleX("Aerogel tile number");
      H_npbar_tile[m-1].setTitleY("N(pbar)");
      H_npbar_tile[m-1].setOptStat("1111111");

    }


    histitle = String.format("Number of trigger electrons per sector");
    histname = String.format("H_RICH_ntrigele");
    H_ntrigele = new H1F(histname, histitle, nSECTORS, 0.5, nSECTORS+0.5);
    H_ntrigele.setTitle(histitle);
    H_ntrigele.setTitleX("Sector");
    H_ntrigele.setTitleY("Nunmber of trigger electrons");
    H_ntrigele.setOptStat("1111111");



    histitle = String.format("RICH setup");
    histname = String.format("H_RICH_setup");
    H_setup = new H1F(histname, histitle, nMODULES, 0.5, nMODULES+0.5);
    H_setup.setTitle(histitle);
    H_setup.setTitleX("Module");
    H_setup.setTitleY("Sector");
    H_setup.setOptStat("1111111");
    for (int m=1; m<=nMODULES; m++) H_setup.fill(m, GetSector(m));

  }


  public void getParticles(DataBank part, DataBank hadr, DataBank phot){

    for (int k=0; k<hadr.rows(); k++) {
      int pindex = hadr.getInt("pindex", k);

      int pid = part.getInt("pid", pindex);
      float chi2pid = part.getFloat("chi2pid", pindex);

      int rich_pid = hadr.getInt("best_PID", k);
      float rich_rq = hadr.getFloat("RQ", k);
      float rich_chi2 = hadr.getFloat("best_c2", k);
      float rich_ntot = hadr.getFloat("best_ntot", k);
      float rich_mchi2 = pixelsize * hadr.getFloat("mchi2", k);
      int rich_layer = hadr.getInt("emilay", k);
      int rich_tile = 1 + hadr.getInt("emico", k);

      int sector = 0;
      int module = 0;
      for (int j = 0; j< phot.rows(); j++) {
        int phot_pindex = phot.getInt("pindex", j);
        if (phot_pindex == pindex) {
          sector = phot.getInt("sector", j);
          module = ModuleInSector[sector-1];
          break;
        }
      }

      if (module == 0) continue;

      /* Filling the track matching for electrons */
      float chi2pid_max = 3;
      if ( (Math.abs(pid) == LundElectron) && (Math.abs(chi2pid) < chi2pid_max) )  {
        if (rich_mchi2 > 0) H_trk_match[module-1].fill(rich_mchi2);
      }


      /* Filling hadron counters */
      if (Math.abs(pid) != LundElectron)  {
        if (AcceptRichID(rich_pid, rich_rq, rich_chi2, rich_ntot) == 1) {
          int bin = rich_layer*nMAXTILES + rich_tile;

          if (rich_pid == LundPiplus) H_npip_tile[module-1].fill(bin);
          else if (rich_pid == LundPiminus) H_npim_tile[module-1].fill(bin);
          else if (rich_pid == LundKplus) H_nkp_tile[module-1].fill(bin);
          else if (rich_pid == LundKminus) H_nkm_tile[module-1].fill(bin);
          else if (rich_pid == LundProton) H_npro_tile[module-1].fill(bin);
          else if (rich_pid == LundPbar) H_npbar_tile[module-1].fill(bin);

        }


      }

    }

  }


  public void getPhotons(DataBank part, DataBank hadr, DataBank phot){
    float p_min = 2.0f;
    int pmt, anode, absChannel, sector, module;
    int layers, compos, use;
    int nrefl, refl1, topology;
    double DTimeCorr, etac, beta, mass;

    int nmax_rich = 10;
    int npho_top[][] = new int[nTOP][nmax_rich];
    int module_trk[] = new int[nmax_rich];
    for (int k=0; k<hadr.rows(); k++) {
      for (int top=1; top<=nTOP; top++) {
        npho_top[top-1][k] = 0;
      }
      module_trk[k] = 0;
    }

    for (int j = 0; j< phot.rows(); j++) {
      int part_hypo = phot.getInt("hypo", j);
      int pindex = phot.getInt("pindex", j);
      int pid = part.getInt("pid", pindex);
      Vector3 P3 = new Vector3(part.getFloat("px", pindex),part.getFloat("py", pindex),part.getFloat("pz", pindex));

      int rich_pid = 0;
      int layer = -1;
      int tile = -1;
      int itrk = -1;
      for (int k=0; k<hadr.rows(); k++) {
        int hadr_pindex = hadr.getInt("pindex", k);
        if (hadr_pindex == pindex) {
          rich_pid = hadr.getInt("best_PID", k);
          layer = hadr.getInt("emilay", k);
          tile = 1 + hadr.getInt("emico", k);
          itrk = k;
          break;
        }
      }

      if (rich_pid == 0) continue;

      //mass = GetMass(rich_pid);
      mass = GetMass(pid);
      beta = P3.mag() / Math.sqrt(P3.mag()*P3.mag() + mass*mass);

      //System.out.println(String.format("j="+j+"  hypo="+part_hypo+"   pid="+pid));

      /* channel info */
      sector = phot.getInt("sector", j);
      pmt = phot.getInt("pmt", j);
      anode = phot.getInt("anode", j);
      absChannel = anode + (pmt-1)*nANODES;
      DTimeCorr = phot.getFloat("dtime", j);
      etac = phot.getFloat("etaC", j);
      layers = phot.getInt("layers", j);
      compos = phot.getInt("compos", j);
      use = phot.getInt("use", j);

      module = ModuleInSector[sector-1];
      nrefl = GetNReflections(layers);
      refl1 = GetFirstReflection(layers, compos);
      topology = GetTopology(nrefl, refl1);


      //if ( ( (pid == LundElectron) || (Math.abs(pid) == LundPiplus) ) && (part_hypo == pid)  && (P3.mag() >= p_min) ) {
      if ( (pid == LundElectron) && (part_hypo == pid)  && (P3.mag() >= p_min) ) {

        //if (topology==0)System.out.println(String.format("layers=" + layers + "  compos=" + compos + "  nr=" + nrefl + "  r1=" + refl1 + "   top=" + topology));

        if ( (0 < module) && (module <= nMODULES) ) {
          H_dt[module-1].fill(DTimeCorr);
          H_dt_channel[module-1].fill(absChannel, DTimeCorr);	

          if ( (layer >= 0) & (tile >= 1) && (use == RICHUSEDFLAG) && ( (1 <= topology) && (topology <= nTOP) ) ) {
            double DEtaC = 1000 * (etac - Math.acos(1. / (beta*AerogelRefIndex[module-1][layer][tile]) ) );
            int bin = layer*nMAXTILES + tile;

            H_detac_tile[module-1][topology-1].fill(bin, DEtaC);


            npho_top[topology-1][itrk]++;
            module_trk[itrk] = module;
          }

        }

      }

      }

    for (int k=0; k<hadr.rows(); k++) {
      int tile = 1 + hadr.getInt("emico", k);
      int layer = hadr.getInt("emilay", k);
      int bin = layer*nMAXTILES + tile;

      int m = module_trk[k];
      for (int top=1; top<=nTOP; top++) {
        if (npho_top[top-1][k] > 0) {
          H_npho_tile[m-1][top-1].fill(bin, npho_top[top-1][k]);
        }
      }
    }


    }    

    public void FillTimeHistogram() {
      int npeakMin = 20;

      for (int m=1; m<=nMODULES; m++) {

        H_dt_RMS[m-1].reset();
        H_dt_MEAN[m-1].reset();
        H2F H_dt_channel_rb = H_dt_channel[m-1].rebinX(nANODES);
        ArrayList<H1F> H_dt_PMT_AL = H_dt_channel_rb.getSlicesX();

        for (int p=0; p<nPMTS; p++) {
          H_dt_PMT[p][m-1] = H_dt_PMT_AL.get(p);
          H_dt_PMT[p][m-1].setTitle(String.format("dT, Module %d, PMT=%d",m, p+1));
          H_dt_PMT[p][m-1].setTitleX("dT (ns)");
          H_dt_PMT[p][m-1].setTitleY("counts");
          H_dt_PMT[p][m-1].setOptStat("1111111");
          int binM = (int) H_dt_PMT[p][m-1].getMaximumBin();
          int binL = binM - BINWINDOW/2;
          int binH = binM + BINWINDOW/2;
          float rms = 0;
          float mean = 0;

          int nentries = getHistoEntries(H_dt_PMT[p][m-1]);

          if (nentries >= npeakMin) {
            mean =  getMEAN(H_dt_PMT[p][m-1], binL, binH);
            rms =  getRMS(H_dt_PMT[p][m-1], binL, binH);
          }

          H_dt_RMS[m-1].fill(p+1,rms);
          H_dt_MEAN[m-1].fill(p+1,mean);

        }

      }


    }

    public void processEvent(DataEvent event){
      if(event.hasBank("RUN::config")){
        DataBank confbank = event.getBank("RUN::config");
        long TriggerWord = confbank.getLong("trigger",0);
        for (int i = NTBITS-1; i >= 0; i--) {trigger_bits[i] = (TriggerWord & (1 << i)) != 0;} 
        DataBank eventBank=null, partBank = null, hadrBank = null, photBank = null, trackBank = null;
        if(userTimeBased){
          if(event.hasBank("REC::Event")) eventBank = event.getBank("REC::Event");
          if(event.hasBank("REC::Particle"))partBank = event.getBank("REC::Particle");
          if(event.hasBank("REC::Track"))trackBank = event.getBank("REC::Track");
        }
        if(!userTimeBased){
          if(event.hasBank("REC::Event"))eventBank = event.getBank("REC::Event");
          if(event.hasBank("RECHB::Particle"))partBank = event.getBank("RECHB::Particle");
          if(event.hasBank("RECHB::Track"))trackBank = event.getBank("RECHB::Track");
        }


        eventN = confbank.getInt("event", 0);


        if(event.hasBank("RICH::Ring")) photBank = event.getBank("RICH::Ring");
        if(event.hasBank("RICH::Particle")) hadrBank = event.getBank("RICH::Particle");

        //if( (trigger_bits[1] || trigger_bits[2] || trigger_bits[3] || trigger_bits[4] || trigger_bits[5] || trigger_bits[6]) && partBank!=null)e_part_ind = makeElectron(partBank);
        if (eventBank!=null) {
          if ((eventBank.rows() > 0) && (confbank.rows() > 0)) {

            if(partBank!=null && hadrBank!=null && photBank!=null) {

              //Looking for one good trigger electron 
              int pid = partBank.getInt("pid", 0);
              int status = partBank.getInt("status", 0);
              float chi2pid = partBank.getFloat("chi2pid", 0);
              Vector3 P3 = new Vector3(partBank.getFloat("px", 0),partBank.getFloat("py", 0),partBank.getFloat("pz", 0));
              int sector = 0;


              if ( IsGoodElectron(pid, P3, status, chi2pid) == 1) {

                for(int l=0;l<trackBank.rows() && sector==0 ;l++) {
                  if(trackBank.getInt("pindex",l) == 0) {
                    sector = trackBank.getInt("sector", l);
                  }
                }

                if (sector != 0) {
                  Ntrig++;

                  H_ntrigele.fill(sector);


                  getPhotons(partBank, hadrBank, photBank);
                  getParticles(partBank, hadrBank, photBank);
                }

              }
            }
          }
        }
      }
    }

    public void postProcess(){
      this.FillTimeHistogram();
      this.CalcCounters();
    }

    public void write() {
      postProcess();
      TDirectory dirout = new TDirectory();
      dirout.mkdir("/RICH/");
      dirout.cd("/RICH/");

      dirout.addDataSet(H_dt);
      //dirout.addDataSet(H_dt_channel);
      dirout.addDataSet(H_dt_RMS);
      dirout.addDataSet(H_dt_MEAN);

      for (int m=1; m<=nMODULES; m++) {
        for (int top=1; top<=nTOP; top++) {
          dirout.addDataSet(H_detac_tile[m-1][top-1]);
        }

        dirout.addDataSet(H_npho_tile[m-1]);

        dirout.addDataSet(H_npip_tile[m-1], H_npim_tile[m-1], H_nkp_tile[m-1], H_nkm_tile[m-1], H_npro_tile[m-1], H_npbar_tile[m-1]);
      }

      dirout.addDataSet(H_trk_match);
      dirout.addDataSet(H_ntrigele);
      dirout.addDataSet(H_setup);


      if(runNum>0) dirout.writeFile(outputDir+"/out_RICH_"+runNum+".hipo");
      else         dirout.writeFile(outputDir+"/out_RICH.hipo");
    }


    public float getRMS(H1F h, int binL, int binH){
      float rms=0.f;
      float avg=0.f;


      /* First get the average within BINWINDOW */
      int Ntot = 0;
      for (int c=binL;c<=binH;c++) {
        avg += h.getBinContent(c)*(float)h.getDataX(c);
        rms += h.getBinContent(c)*(float)h.getDataX(c)*(float)h.getDataX(c);
        Ntot += (int)h.getBinContent(c);
      }

      if (Ntot!=0) avg = avg/Ntot;

      /* Calculate the RMS */
      rms = (float)Math.sqrt(rms/(Ntot-1) - avg*avg*Ntot/(Ntot-1));

      return rms;

    }

    public float getMEAN(H1F h, int binL, int binH){
      float avg=0.f;


      /* Get the average within BINWINDOW */
      int Ntot = 0;
      for (int c=binL;c<=binH;c++) {
        avg += h.getBinContent(c)*(float)h.getDataX(c);
        Ntot += (int)h.getBinContent(c);
      }

      if (Ntot!=0) avg = avg/Ntot;

      return avg;

    }



    public int getHistoEntries(H1F h){
      int entries = 0;
      for(int loop = 0; loop < h.getAxis().getNBins(); loop++){
        entries += (int) h.getBinContent(loop);
      }
      return entries;
    }


    public void CalcCounters(){

      Ntrig = (int)H_ntrigele.getEntries();

      for (int m=1; m<=nMODULES; m++) {

        float npip = 0f;
        float npim = 0f;
        float nkp = 0f;
        float nkm = 0f;
        float npro = 0f;
        float npbar = 0f;

        if (Ntrig > 0) {
          npip = (float)H_npip_tile[m-1].getEntries() / Ntrig;
          npim = (float)H_npim_tile[m-1].getEntries() / Ntrig;
          nkp = (float)H_nkp_tile[m-1].getEntries() / Ntrig;
          nkm = (float)H_nkm_tile[m-1].getEntries() / Ntrig;
          npro = (float)H_npro_tile[m-1].getEntries() / Ntrig;
          npbar = (float)H_npbar_tile[m-1].getEntries() / Ntrig;
        }
        if (verbose) {
          System.out.println(String.format("RUN Counters module " + m));
          System.out.println(String.format("RUN  "+runNum+"  "+npip+" "+npim+" "+nkp+" "+nkm+" "+npro+" "+npbar));
        }

      }

      return;
    }



    public int GetSector(int module){
      for (int s=1; s<=nSECTORS; s++) {
        if (ModuleInSector[s-1] == module) return s;
      }

      return 0;
    }


    public int GetNReflections(int layers){
      int nr = 0;
      int l = layers;
      while (l > 0) {
        nr++;
        l = l / 10;
      }


      return nr;
    }

    public int GetFirstReflection(int layers, int compos){
      int r1 = 0;

      if (layers > 0) {
        int currentLayer = layers%10;
        r1 = 1 + compos%10 + 10*currentLayer;
      }

      return r1;
    }

    public float GetMass(int pid){

      if (Math.abs(pid) == LundPiplus) return PionMass;
      else if (Math.abs(pid) == LundKplus) return KaonMass;
      else if (Math.abs(pid) == LundProton) return ProtonMass;

      return 0;
    }


    public int GetTopology(int nr, int r1) {
      int top = 0;

      if (nr == 0) top = 1;
      else {
        if ( (nr == 1) && ( (10 < r1) && (r1 < 20) ) ) top = 2;
        else if ( (nr == 2) && ( (20 < r1) && (r1 <= 30) ) ) top = 3;
      }


      return top;
    }


    public int IsGoodElectron(int pid, Vector3 P3, int status, float chi2pid) {

      if (pid == LundElectron) {

        if ( (P3.mag() > 1.5) && (Math.abs(chi2pid) < 3.) && (status < 0) ) {
          return 1;
        }
      }

      return 0;
    }


    public int AcceptRichID(int rich_pid, float rich_rq, float rich_chi2, float rich_ntot) {

      return 1;
    }


  }
