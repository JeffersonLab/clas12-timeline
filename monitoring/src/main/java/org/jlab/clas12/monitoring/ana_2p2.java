package org.jlab.clas12.monitoring;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import org.jlab.groot.math.*;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.math.F1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.TDirectory;
import org.jlab.clas.physics.Vector3;
import org.jlab.clas.physics.LorentzVector;
import org.jlab.groot.base.GStyle;

public class ana_2p2 {
	public ana_2p2(){}
////////////////////////////////////////////////
        public static void main(String[] args) {
                System.setProperty("java.awt.headless", "true");
		GStyle.setPalette("kRainBow");
                int count = 0;
		int runNum = 0;
		String filelist = "list_of_files.txt";
		int maxevents = 500000;
		float EB = 10.2f;
		boolean useTB=true;
		boolean useVolatile = false;
		if(args.length>0)runNum=Integer.parseInt(args[0]);
		if(args.length>1)filelist = args[1];
		if(args.length>2)maxevents=Integer.parseInt(args[2]);
		if(args.length>3)EB=Float.parseFloat(args[3]);
		if(args.length>4)if(Integer.parseInt(args[4])==0)useTB=false;
		System.out.println("will process run number "+runNum+" from list "+filelist+" looking for up to "+maxevents+" events, beam energy setting "+EB);

		monitor2p2GeV ana_mon = new monitor2p2GeV(runNum,EB,useTB,useVolatile);
		tof_monitor ana_tof = new tof_monitor(runNum,useTB,useVolatile);
		central ana_cen = new central(runNum,useTB,useVolatile);
		occupancies ana_occ = new occupancies(runNum,useVolatile);
		HTCC ana_htc = new HTCC(runNum,useVolatile);
		LTCC ana_ltc = new LTCC(runNum,EB,useTB,useVolatile);
		RICH ana_rich = new RICH(runNum,EB,useTB,useVolatile);
		cndCheckPlots ana_cnd = new cndCheckPlots(runNum,useTB,useVolatile);		
		FT ana_ft = new FT(runNum,useTB,useVolatile);
		dst_mon ana_dst_mon = new dst_mon(runNum,EB);
		BAND ana_band = new BAND(runNum,EB,useTB,useVolatile);
		//deuterontarget ana_deuteron = new deuterontarget(runNum,EB,useTB,useVolatile);
                List<String> toProcessFileNames = new ArrayList<String>();
                File file = new File(filelist);
                Scanner read;
                try {
                        read = new Scanner(file);
                        do { 
                                String filename = read.next();
                                if(runNum==0 || filename.contains(String.format("%d",runNum) ) ){
					toProcessFileNames.add(filename);
					System.out.println("adding "+filename);
				}

                        }while (read.hasNext());
                        read.close();
                }catch(IOException e){
                        e.printStackTrace();
                }
		int progresscount=0;int filetot = toProcessFileNames.size();
		long startTime = System.currentTimeMillis();
		long previousTime = System.currentTimeMillis();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		for (String runstrg : toProcessFileNames) if( count<maxevents ){
			progresscount++;
			System.out.println(String.format(">>>>>>>>>>>>>>>> %s",runstrg));
			File varTmpDir = new File(runstrg);
			if(!varTmpDir.exists()){System.out.println("FILE DOES NOT EXIST");continue;}
			System.out.println("READING NOW "+runstrg);
			HipoDataSource reader = new HipoDataSource();
			reader.open(runstrg);
			int filecount = 0;
			while(reader.hasEvent()&& count<maxevents ) {
				DataEvent event = reader.getNextEvent();
				ana_mon.processEvent(event);
				ana_cen.processEvent(event);
				ana_tof.processEvent(event);
				ana_occ.processEvent(event);
				ana_htc.processEvent(event);
				ana_ltc.processEvent(event);
				ana_cnd.processEvent(event);
				ana_ft.processEvent(event);
				ana_dst_mon.processEvent(event);
				ana_band.processEvent(event);
				ana_rich.processEvent(event);
				//ana_deuteron.processEvent(event);
				filecount++;count++;
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
		ana_mon.ratio_to_trigger();
		//ana_mon.plot();
		ana_mon.write();
		//ana_cen.plot();
		ana_cen.write();
		ana_tof.analyze();
		//ana_tof.plot();
		ana_tof.write();
		//ana_occ.plot();
		//ana_htc.plot();
		ana_htc.write();
		//ana_ltc.plot();
		ana_ltc.write();
		ana_cnd.fit();
		//ana_cnd.plot();
		ana_cnd.write();
		ana_ft.analyze();
		//ana_ft.plot();
		ana_ft.write();
		//ana_dst_mon.plot();
		ana_dst_mon.write();
		//ana_band.plot();
		ana_band.write();
		//ana_rich.plot();
		ana_rich.postProcess();
		ana_rich.write();
		//ana_deuteron.plot();
        }
}
