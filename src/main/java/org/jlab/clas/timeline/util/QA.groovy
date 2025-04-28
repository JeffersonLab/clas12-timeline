package org.jlab.clas.timeline.util
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.TDirectory

/// result of `QA.cutGraphs`
class CutGraphResult {
  /// graphs which include only the "bad" points (points outside QA cuts)
  public ArrayList<GraphErrors> bad_graphs;
  /// cut lines
  public ArrayList<GraphErrors> cut_lines;
}

class QA {

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
        new GraphErrors([
          'plotLine',
          'horizontal',
          val,
          color ?: 'black',
        ].join(':'));
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

}
