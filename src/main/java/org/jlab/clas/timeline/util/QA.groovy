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
  /// @param hipo_files list of HIPO files
  /// @param bad_scaler if true, handle the case where FC charge from scalers was bad (RG-D)
  public void defineBins(List<String> hipo_files) {

    // for each tag1 event, get its event number and timestamp
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

      while(hipo_reader.hasEvent()) { // loop over events
        hipo_event = hipo_reader.getNextEvent();
        if(
            (hipo_event.hasBank("RUN::scaler") && hipo_event.getBank("RUN::scaler").rows()>0) &&
            (hipo_event.hasBank("RUN::config") && hipo_event.getBank("RUN::config").rows()>0)
          )
        {
          tag1_events << [
            BigInteger.valueOf(hipo_event.getBank("RUN::config").getInt('event',0)),
            BigInteger.valueOf(hipo_event.getBank("RUN::config").getLong('timestamp',0))
          ];
        }
      }
      hipo_reader.close();
    }

    // sort the events by event number
    tag1_evnum_list = tag1_events.sort(false){it[0]}.collect{it[0]};

    // check that we would get the same result, if we instead sorted by timestamp
    if(!bad_scaler && tag1_evnum_list != tag1_events.sort(false){it[1]}.collect{it[0]}) {
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
    if(bad_scaler && qa_bin_bounds.size()>3) {
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
        LTlist:       [],
      ];
    };
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
