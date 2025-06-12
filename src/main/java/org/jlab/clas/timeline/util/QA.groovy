package org.jlab.clas.timeline.util
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.TDirectory
import org.jlab.io.hipo.HipoDataSource;

/// result of `QA.cutGraphs`
class CutGraphResult {
  /// graphs which include only the "bad" points (points outside QA cuts)
  public ArrayList<GraphErrors> bad_graphs;
  /// cut lines
  public ArrayList<GraphErrors> cut_lines;
}

// ----------------------------------------------------------------------------------

/// general QA class
class QA {

  /// at least this many scaler readouts per QA bin // 2000 is roughly a DST 5-file
  private final def MIN_NUM_SCALERS = 2000;

  /// data structure to hold various data per bin, such as FC charge
  private def bin_data = [:];

  ///////////////////////////////////////////////////////////////////////////////////

  public QA() {
  }

  //////////////////////////////////////////////////////////////////////////////////

  /// @brief define the QA bins; initializes `bin_data`
  ///
  /// The FC charge is handled in various ways, depending on the data set; this is
  /// controlled by the parameter `fc_mode`, where:
  /// - 0: DAQ-gated FC charge is incorrect
  ///   - recharge option was likely OFF during cook, and should be turned on
  ///   - re-calculates DAQ-gated FC charge as: `ungated FC charge * average livetime`
  ///   - typically applies to data cooked with `coatjava` version 6.5.3 or below
  ///   - typically used as a fallback if there are "spikes" in gated charge when `fc_mode==1`
  /// - 1: DAQ-gated FC charge can be trusted
  ///   - recharge option was either ON or did not need to be ON
  ///   - calculate DAQ-gated FC charge directly from `RUN::scaler:fcupgated`
  ///   - if you find `fcupgated > fcup(un-gated)`, then most likely the recharge option was OFF
  ///     but should have been ON, and `fc_mode=0` should be used instead
  /// - 2: calculate DAQ-gated FC charge from `REC::Event:beamCharge`
  ///   - useful if `RUN::scaler` is unavailable
  /// - 3: DAQ-gated FC charge and ungated FC charge are both incorrect
  ///   - Read DAQ-gated charge from a JSON file in `clas12-timeline/data/fccharge/<RG>.json`
  ///   - useful if `RUN::scaler` is unavailable
  ///
  /// @param hipo_files list of HIPO files
  /// @param fc_mode defines how to handle the FC charge (see method description)
  public void defineBins(List<String> hipo_files, int fc_mode=1) {

    // for each tag1 event, get its event number and timestamp
    System.out.println('Defining QA bins ...');
    System.out.println('... reading tag1 event numbers and timestamps ...');
    def tag1_events = [];
    hipo_files.each { hipo_file -> // loop over HIPO files

      def hipo_file_handle = new File(hipo_file);
      if(!hipo_file_handle.exists()) {
        System.err.println("ERROR: FILE DOES NOT EXIST: $hipo_file");
        return;
      }
      def hipo_reader = new HipoDataSource();
      hipo_reader.getReader().setTags(1); // filter on tag 1
      hipo_reader.open(hipo_file);

      while(hipo_reader.hasEvent()) { // loop over events which have `RUN::config` and `RUN::scaler`
        def hipo_event = hipo_reader.getNextEvent();
        if(hipo_event.hasBank("RUN::config") && (fc_mode==3 || hipo_event.hasBank("RUN::scaler"))) {
          def bank_config = hipo_event.getBank("RUN::config");
          if(bank_config.rows()>0 && (fc_mode==3 || hipo_event.getBank("RUN::scaler").rows()>0) {
            tag1_events << [
              BigInteger.valueOf(bank_config.getInt('event',0)),
              BigInteger.valueOf(bank_config.getLong('timestamp',0))
            ];
          }
        }
      }
      hipo_reader.close();
    }

    // sort the events by event number
    System.out.println('... sorting and choosing bin boundaries ...');
    def tag1_evnum_list = tag1_events.sort(false){it[0]}.collect{it[0]};

    // check that we would get the same result, if we instead sorted by timestamp
    if(tag1_evnum_list != tag1_events.sort(false){it[1]}.collect{it[0]}) {
      System.err.println("ERROR: sorting tag1 events by event number is DIFFERENT than sorting by timestamp");
      System.exit(100);
    }

    // define the QA bin boundaries: first, some sorting and transformations
    def qa_bin_bounds = tag1_evnum_list
      .collate(MIN_NUM_SCALERS)   // partition into subsets, each with cardinality MIN_NUM_SCALERS (the last subset may be smaller)
      .collect{ it[0] }           // take the first event number of each subset
      .plus(tag1_evnum_list[-1])  // append the final tag1 event number...
      .unique()                   // ...and make sure it's not just repeating the previous event number
      .collect{ [it, it] }        // double each element (since upper bound of bin N = lower bound of bin N+1)...
      .flatten();                 // ...and flatten it, since we are going to re-collate it below after adding the final bin boundaries
    // set the first bin boundary to 0; we'll fix it later after the main event loop
    qa_bin_bounds = [0] + qa_bin_bounds;
    // set the last bin boundary to a high number, because the true highest event
    // number is not yet known; we'll fix it later after the main event loop
    qa_bin_bounds = qa_bin_bounds + [10**(Math.log10(qa_bin_bounds[-1]).toInteger()+2)]; // two orders of magnitude above largest known event number
    // pair the elements to define the bin boundaries
    qa_bin_bounds = qa_bin_bounds.collate(2);

    // if the scalers were bad, redefine bins to merge all bins except the first and last
    if(fc_mode==3 && qa_bin_bounds.size()>3) {
      new_qa_bin_bounds = [
        qa_bin_bounds[0],
        [ [qa_bin_bounds[0][1], qa_bin_bounds[-1][0]] ],
        qa_bin_bounds[-1],
      ];
      qa_bin_bounds = new_qa_bin_bounds;
    }

    // define the QA bin data, initializing additional fields
    qa_bin_bounds.eachWithIndex{ bounds, bin_num ->
      bin_data[bin_num] = [
        eventNumMin:  bounds[0],           // event number range
        eventNumMax:  bounds[-1],
        timestampMin: "init",              // timestamp range
        timestampMax: "init",
        fcRange:      ["init", "init"],    // gated FC charge at the bin boundaries
        ufcRange:     ["init", "init"],    // ungated  ""             ""
        fcMinMax:     ["init", "init"],    // gated FC charge min,max (to check if they are within the boundaries set in `fcRange`)
        ufcMinMax:    ["init", "init"],    // ungated  ""             ""
        LTlist:       [],                  // livetime list
      ];
    };

    // initialize min and max overall event numbers and timestamps
    def overall_min_evnum     = bin_data[0].eventNumMax;  // it will be smaller than first bin's max
    def overall_max_evnum     = bin_data[bin_data.size()-1].eventNumMin; // it will be larger than last bin's min
    def overall_min_timestamp = "init";
    def overall_max_timestamp = "init";

    // subroutine to update a min and/or max value in a QA bin (viz. FC charge start and stop)
    def setMinMaxInBin = { bin_num, key, val ->
      valOld = bin_data[bin_num][key];
      bin_data[bin_num][key] = [
        valOld[0] == "init" ? val : [valOld[0], val].min(),
        valOld[1] == "init" ? val : [valOld[1], val].max(),
      ];
    };

    //
    //
    //
    // FIXME: we could use `coatjava`'s common-tools/clas-detector/src/main/java/org/jlab/detector/scalers/DaqScalersSequence
    // instead of rolling our own 'scaler interval' thing here; we need to add a parameter `downsample` which changes the
    // interval size from being 1 subsequent scaler readout to 2000 subsequent scaler readouts
    //
    //
    //

    // add information for each bin
    hipo_files.each { hipo_file -> // loop over tag 1 events again, now that we have established bin boundaries
      def hipo_reader = new HipoDataSource();
      hipo_reader.getReader().setTags(1);
      hipo_reader.open(hipo_file);
      while(hipo_reader.hasEvent()) {
        def hipo_event = hipo_reader.getNextEvent();
        if(hipo_event.hasBank("RUN::config") && (fc_mode==3 || hipo_event.hasBank("RUN::scaler"))) {
          def bank_scaler = hipo_event.getBank("RUN::scaler");
          def bank_config = hipo_event.getBank("RUN::config");
          def bank_event  = hipo_event.getBank("REC::Event");
          if(bank_config.rows()>0) {

            def evnum     = BigInteger.valueOf(bank_config.getInt('event',0));
            def timestamp = BigInteger.valueOf(bank_config.getLong('timestamp',0));

            // set overall min and max event numbers and timestamps
            overall_min_evnum = [ overall_min_evnum, evnum].min();
            overall_max_evnum = [ overall_max_evnum, evnum].max();
            if(overall_min_timestamp == "init") overall_min_timestamp = timestamp;
            else overall_min_timestamp = [ overall_min_timestamp, timestamp ].min();
            if(overall_max_timestamp == "init") overall_max_timestamp = timestamp;
            else overall_max_timestamp = [ overall_max_timestamp, timestamp ].max();

            // find the QA bin that contains this event
            def (this_bin_num, this_bin) = findBin(eventNum);
            if(this_bin_num == null) System.exit(100);

            // get the FC charge and livetime, depending on `fc_mode`
            // - also sets the min and max FC charges, for this QA bin
            def lt;
            def fc  = "init";
            def ufc = "init";
            if(bank_scaler.rows()>0) {
              // ungated charge
              ufc = bank_scaler.getFloat("fcup",0);
              setMinMaxInBin(this_bin_num, "ufcMinMax", ufc);
              // livetime
              lt = bank_scaler.getFloat("livetime",0);
              if(lt>=0) { this_bin.LTlist << lt };
              // gated charge (if trustworthy)
              if(fc_mode==1) {
                fc = bank_scaler.getFloat("fcupgated",0);
                setMinMaxInBin(this_bin_num, "fcMinMax", fc);
              }
            }
            if(fc_mode==2 && bank_event.rows()>0) {
              // gated charge only
              fc = bank_event.getFloat("beamCharge",0);
              setMinMaxInBin(this_bin_num, "fcMinMax", fc);
            }
            if(fc_mode==3) {
              //
              //
              // FIXME FIXME FIXME FIXME FIXME 
              //
              //
              // gated charge only from file
              fc = (this_bin_num>0 && this_bin_num<bin_data.size()-1) ? getDataFromJSON(runnum,"fc") : 0.0; //NOTE: Only set FC charge for middle bin(s) since first and last bins should be empty and have same upper and lower limits in `fc_mode==3`.
              setMinMaxInBin(this_bin_num, "fcMinMax", fc);
              // Set ungated charge = gated charge
              ufc = fc;
              setMinMaxInBin(this_bin_num, "ufcMinMax", ufc);
            }

            // if this event is on a bin boundary, and it has `bank_scaler`, update `fcRange` and `ufcRange`
            // FIXME: this will only work for fc_mode==1; need to figure out how to handle the others
            def onBinBoundary = false
            if(eventNum == this_bin.eventNumMax) {
              onBinBoundary = true
              if(bank_scaler.rows()>0 || fc_mode==3) { // must have bank_scaler, so `fc` and `ufc` are set (we'll check if any `(u)fcRange` values are still "init" later) UNLESS `fc_mode==3` in which case thse values are set from a JSON file so the scaler bank is not required.
                // events on the boundary are assigned to earlier bin; this FC charge is that bin's max charge
                this_bin.fcRange[1]   = fc
                this_bin.ufcRange[1]  = ufc
                if (this_bin_num==1 && fc_mode==3) { //NOTE: Set middle bin (of 3) minimums for `fc_mode==3`
                  this_bin.fcRange[0]   = 0.0
                  this_bin.ufcRange[0]  = 0.0
                  this_bin.timestampMin = 0
                  bin_data[0].timestampMax = 0
                }
                this_bin.timestampMax = timestamp
                // this FC charge is also the next bin's min charge
                def nextTimeBin = bin_data[this_bin_num+1]
                if(nextTimeBin==null) { System.err.println "ERROR: found a QA bin that has no subsequent bin, and is not the latest bin" }
                nextTimeBin.fcRange[0]   = fc
                nextTimeBin.ufcRange[0]  = ufc
                nextTimeBin.timestampMin = timestamp
                printDebug "event number ${eventNum} on upper boundary of bin ${this_bin_num}, and assigned to that bin:"
                printDebug "  - gated charge:   ${fc}"
                printDebug "  - ungated charge: ${ufc}"
                printDebug "  - banks: ${hipoEvent.getBankList()}"
              }
            }
            if(eventNum == this_bin.eventNumMin) {
              onBinBoundary = true
              System.err.println "ERROR: event number ${eventNum} on lower boundary of bin ${this_bin_num}, and assigned to that bin; this shouldn't happen in the current binning scheme."
            }

          }
        }
      }
      hipo_reader.close();
    }

    System.out.println('... QA bins defined.');
  }

  //////////////////////////////////////////////////////////////////////////////////

  /// @brief print QA bins
  public void printBins() {
    System.out.println "QA BINS ==============================";
    System.out.println "@ " + [
      'binnum/I',
      'number_of_bins/I',
      'evnum_min/L',
      'evnum_max/L',
      'timestamp_min/L',
      'timestamp_max/L',
      'num_events/L',
    ].join(':');
    bin_data.each{ bin_num, bin_datum ->
      def num_events = bin_datum.eventNumMax - bin_datum.eventNumMin;
      if(bin_num==0) num_events++; // since first bin has no lower bound
      System.out.println "@ " + [
        "${bin_num}",
        "${bin_data.size()}",
        "${bin_datum.eventNumMin}",
        "${bin_datum.eventNumMax}",
        "${bin_datum.timestampMin}",
        "${bin_datum.timestampMax}",
        "${num_events}",
      ].join(' ');
    }
    System.out.println "QA BINS ==========================";
  }

  //////////////////////////////////////////////////////////////////////////////////

  /// @brief find the earliest QA bin for a given event number;
  /// if the event number is on a time-bin boundary, the earlier QA bin will be returned
  /// @param evnum the event number
  /// @returns a key-value pair from `bin_data`, or `null` if not found
  public def findBin(def evnum) {
    def s = bin_data.find{ evnum >= it.value.eventNumMin && evnum <= it.value.eventNumMax };
    if(s==null) {
      System.err.println("ERROR: failed to find QA bin for event number $evnum");
      return null;
    }
    [ s.key, s.value ];
  }

  //////////////////////////////////////////////////////////////////////////////////

  /// @param input_graphs input graphs to process
  /// @param args.lb lower QA bound (default: no bound)
  /// @param args.ub upper QA bound (default: no bound)
  /// @param args.lb_color color of lower bound line
  /// @param args.ub_color color of upper bound line
  /// @param args.out TDirectory for adding graphs and lines, if defined
  /// @returns `CutGraphResult`
  static CutGraphResult cutGraphs(Map args, GraphErrors... input_graphs) {
    CutGraphResult result = new CutGraphResult();
    // make lines
    result.cut_lines = [
      [args.lb, args.lb_color],
      [args.ub, args.ub_color],
    ]
      .collect{ val, color ->
        if(val==null) return null;
        def cut_line = new GraphErrors([
          'plotLine',
          'horizontal',
          val,
          color ?: 'black',
        ].join(':'));
        cut_line.setTitle(input_graphs[0].getTitle());
        cut_line.setTitleX(input_graphs[0].getTitleX());
        cut_line.setTitleY(input_graphs[0].getTitleY());
        cut_line
      }
      .findAll{it != null};
    // define QA criteria
    def qa_crit = []
    if(args.lb != null)
      qa_crit << {val -> val >= args.lb };
    if(args.ub != null)
      qa_crit << {val -> val <= args.ub };
    def qa_cut = { val ->
      def allow = true;
      qa_crit.each{ crit -> allow &= crit(val) };
      allow;
    };
    // apply cuts
    result.bad_graphs = input_graphs.collect{ input_graph ->
      def bad_graph = new GraphErrors();
      bad_graph.setName(input_graph.getName() + "__bad");
      bad_graph.setTitle(input_graph.getTitle());
      bad_graph.setTitleX(input_graph.getTitleX());
      bad_graph.setTitleY(input_graph.getTitleY());
      // loop over points, checking the cuts
      input_graph.getDataSize(0).times{ i ->
        def val = input_graph.getDataY(i);
        if(!qa_cut(val))
          bad_graph.addPoint(input_graph.getDataX(i), val, input_graph.getDataEX(i), input_graph.getDataEY(i));
      }
      bad_graph;
    }
    // write output
    if(args.out != null) {
      result.cut_lines.each{args.out.addDataSet(it)};
      input_graphs.each{args.out.addDataSet(it)};
      result.bad_graphs.each{args.out.addDataSet(it)};
    }
    result;
  }

  //////////////////////////////////////////////////////////////////////////////////

  /// @param mean_or_sigma should be 'mean' or 'sigma'
  /// @param input_graphs input graphs to process
  /// @param args.mean_lb lower bound for mean
  /// @param args.mean_ub upper bound for mean
  /// @param args.sigma_ub upper bound for sigma
  /// @param args.mean_lb_color color of lower bound line for mean
  /// @param args.mean_ub_color color of upper bound line for mean
  /// @param args.sigma_ub_color color of upper bound line for sigma
  /// @param args.out TDirectory for adding graphs and lines, if defined
  /// @returns `CutGraphResult`
  static CutGraphResult cutGraphsMeanSigma(Map args, String mean_or_sigma, GraphErrors... input_graphs) {
    if(mean_or_sigma == 'mean') {
      return cutGraphs(
          input_graphs,
          lb: args.mean_lb,
          ub: args.mean_ub,
          lb_color: args.mean_lb_color,
          ub_color: args.mean_ub_color,
          out: args.out,
          );
    }
    else { // sigma
      return cutGraphs(
          input_graphs,
          ub: args.sigma_ub,
          ub_color: args.sigma_ub_color,
          out: args.out,
          );
    }
  }

}
