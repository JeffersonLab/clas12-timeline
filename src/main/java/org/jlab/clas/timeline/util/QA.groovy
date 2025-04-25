package org.jlab.clas.timeline.util
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.TDirectory

class QA {

  /// @returns a single horizontal cut line
  /// @param value the y-axis value of this line
  /// @param the color of this line
  static GraphErrors makeCutLine(double value, String color=null) {
    if(value==null) return null;
    line_name = [
      'plotLine',
      'horizontal',
      value,
      color ?: 'black',
    ].join(':');
    new GraphErrors(line_name);
  }

  /// result of `cutGraphs`
  class CutGraphResult {
    /// graphs which include only the "bad" points (points outside QA cuts)
    public GraphErrors[] bad_graphs;
    /// cut lines
    public GraphErrors[] cut_lines;
  }

  /// @param input_graphs a list of graphs to cut
  /// @param args.lb lower QA bound (default: no bound)
  /// @param args.ub upper QA bound (default: no bound)
  /// @param args.lb_color color of lower bound line
  /// @param args.ub_color color of upper bound line
  /// @param args.out TDirectory for adding graphs and lines, if defined
  /// @returns `CutGraphResult`
  static CutGraphResult cutGraphs(Map args, GraphErrors[] input_graphs) {
    def result = new CutGraphResult();
    // make lines
    result.cut_lines = [
      [args.lb, args.lb_color],
      [args.ub, args.ub_color],
    ]
      .collect{makeCutLine(*it)}
      .findAll{it != null};
    // define QA cut
    def reqs = []
    if(args.lb != null)
      reqs << {val -> val >= args.lb };
    if(args.ub != null)
      reqs << {val -> val <= args.ub };
    def checkPoint = { val ->
      allow = true;
      reqs.each{ req -> allow &= req(val) };
      pass;
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
        def val_x = input_graph.getDataX(i);
        def val_y = input_graph.getDataY(i);
        if(!checkPoint(val_y))
          bad_graph.addPoint(val_x, val_y);
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

}
