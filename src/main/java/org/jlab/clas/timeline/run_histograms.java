package org.jlab.clas.timeline.histograms;

import org.jlab.clas.timeline.util.RunDependentCut;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.groot.base.GStyle;

public class run_histograms {
  public run_histograms(){}
  ////////////////////////////////////////////////
  public static void main(String[] args) {
    System.setProperty("java.awt.headless", "true");
    GStyle.setPalette("kRainBow");
    int count = 0;
    int runNum = 0;
    String outputDir = "plots";
    String filelist = "list_of_files.txt";
    float EB = 10.2f;
    boolean useTB=true;
    if(args.length>0)runNum=Integer.parseInt(args[0]);
    if(args.length>1)outputDir=args[1];
    if(args.length>2)filelist=args[2];
    if(args.length>3)EB=Float.parseFloat(args[3]);
    if(args.length>4)if(Integer.parseInt(args[4])==0)useTB=false;
    System.out.println("will process run number "+runNum+" from list "+filelist+", beam energy setting "+EB);

    // get the dataset which contains this run number
    var dataset = RunDependentCut.findDataset(runNum);

    //// instantiate histogramming classes
    // GeneralMon ana_mon      = new GeneralMon(runNum,outputDir,EB,useTB);
    // DCandFTOF  ana_dc_ftof  = new DCandFTOF(runNum,outputDir,useTB);
    // CTOF       ana_ctof     = new CTOF(runNum,outputDir,useTB);
    // HTCC       ana_htcc     = new HTCC(runNum,outputDir);
    // LTCC       ana_ltcc     = new LTCC(runNum,outputDir,EB,useTB);
    // RICH       ana_rich     = new RICH(runNum,outputDir,EB,useTB);
    // CND        ana_cnd      = new CND(runNum,outputDir,useTB);
    CVT        ana_cvt      = dataset != "rgl" ? new CVT() : null;
    // FT         ana_ft       = new FT(runNum,outputDir,useTB);
    // BAND       ana_band     = new BAND(runNum,outputDir,EB,useTB);
    // ALERT      ana_alert    = dataset == "rgl" ? new ALERT(runNum,outputDir,EB,useTB) : null;
    // helicity   ana_helicity = new helicity();
    // trigger    ana_trigger  = new trigger();

    List<String> toProcessFileNames = new ArrayList<String>();
    File file = new File(filelist);
    Scanner read;
    try {
      read = new Scanner(file);
      do { 
        String filename = read.next()
          .replaceAll("^file:", "")
          .replaceAll("^mss:", "");
        if(runNum==0 || filename.contains(String.format("%d",runNum) ) ){
          toProcessFileNames.add(filename);
          System.out.println("adding "+filename);
        }

      }while (read.hasNext());
      read.close();
    }catch(IOException e){
      e.printStackTrace();
      System.exit(100);
    }
    int progresscount = 0;
    int filetot = toProcessFileNames.size();
    long startTime = System.currentTimeMillis();
    long previousTime = System.currentTimeMillis();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    for (String runstrg : toProcessFileNames) {
      progresscount++;
      System.out.println(String.format(">>>>>>>>>>>>>>>> %s",runstrg));
      File varTmpDir = new File(runstrg);
      if(!varTmpDir.exists()) {
        System.err.println(String.format("ERROR: FILE DOES NOT EXIST: '%s'",runstrg));
        continue;
      }
      System.out.println("READING NOW "+runstrg);
      HipoDataSource reader = new HipoDataSource();
      reader.open(runstrg);
      while(reader.hasEvent()) {
        DataEvent event = reader.getNextEvent();

        //// call each histogramming class instance's `processEvent`
        // if(ana_mon!=null) ana_mon.processEvent(event);
        // if(ana_ctof!=null) ana_ctof.processEvent(event);
        // if(ana_dc_ftof!=null) ana_dc_ftof.processEvent(event);
        // if(ana_htcc!=null) ana_htcc.processEvent(event);
        // if(ana_ltcc!=null) ana_ltcc.processEvent(event);
        // if(ana_cnd!=null) ana_cnd.processEvent(event);
        if(ana_cvt!=null) ana_cvt.processEvent(event);
        // if(ana_ft!=null) ana_ft.processEvent(event);
        // if(ana_band!=null) ana_band.processEvent(event);
        // if(ana_alert!=null) ana_alert.processEvent(event);
        // if(ana_rich!=null) ana_rich.processEvent(event);
        // if(ana_helicity!=null) ana_helicity.processEvent(event);
        // if(ana_trigger!=null) ana_trigger.processEvent(event);

        count++;
        if(count%10000 == 0){
          long nowTime = System.currentTimeMillis();
          long elapsedTime = nowTime - previousTime;
          long totalTime = nowTime - startTime;
          elapsedTime = elapsedTime/1000;
          totalTime = totalTime/1000;
          Date date = new Date();
          System.out.println(count/1000 + "k events (this is all analysis on "+runstrg+") ; time : " + dateFormat.format(date) + " , last elapsed : " + elapsedTime + "s ; total elapsed : " + totalTime + "s ; progress : "+progresscount+"/"+filetot);
          previousTime = nowTime;
        }
      }
      reader.close();
    }
    System.out.println("Total : " + count + " events");

    //// call each histogramming class instance's `write`
    // if(ana_mon!=null) ana_mon.write();
    // if(ana_ctof!=null) ana_ctof.write();
    // if(ana_dc_ftof!=null) ana_dc_ftof.write();
    // if(ana_htcc!=null) ana_htcc.write();
    // if(ana_ltcc!=null) ana_ltcc.write();
    // if(ana_cnd!=null) ana_cnd.write();
    if(ana_cvt!=null) ana_cvt.write(outputDir, runNum);
    // if(ana_ft!=null) ana_ft.write();
    // if(ana_band!=null) ana_band.write();
    // if(ana_alert!=null) ana_alert.write();
    // if(ana_rich!=null) ana_rich.write();
    // if(ana_helicity!=null) ana_helicity.write(outputDir, runNum);
    // if(ana_trigger!=null) ana_trigger.write(outputDir, runNum);

  }
}
