package org.jlab.clas.timeline.util

/// @brief functions to handle run dependent things
///
/// Many of these functions are very simple, but we prefer to use them anyway so that
/// it is easier to track what parts of the code involve run-number dependence.
class RunDependentCut {

  /// @param check_run the run number to check
  /// @param cut_runs the preferred run numbers
  /// @returns true if `check_run` matches any of `cut_runs`
  static boolean runIsOneOf(int check_run, List<Integer> cut_runs) {
    return cut_runs.find{ it == check_run } != null;
  }

  /// @param check_run the run number to check
  /// @param cut_run the preferred run number
  /// @returns true if `check_run` matches `cut_run`
  static boolean runIsOneOf(int check_run, int cut_run) {
    return runIsOneOf(check_run, [cut_run]);
  }

  /// @param check_run the run number to check
  /// @param cut_run_lb the run number lower bound
  /// @param cut_run_ub the run number upper bound
  /// @param include_bound if true, include `cut_run_lb` and `cut_run_ub` in the range
  /// @returns true if `check_run` is within range `cut_run_lb` and `cut_run_ub`
  static boolean runIsInRange(int check_run, int cut_run_lb, int cut_run_ub, boolean include_bound) {
    if(include_bound)
      return check_run >= cut_run_lb && check_run <= cut_run_ub;
    else
      return check_run > cut_run_lb && check_run < cut_run_ub;
  }

  /// @param check_run the run number to check
  /// @param cut_run the run number threshold
  /// @param include_bound if true, include `cut_run` in the range
  /// @returns true if `check_run` is before `cut_run`
  static boolean runIsBefore(int check_run, int cut_run, boolean include_bound) {
    if(include_bound)
      return check_run <= cut_run;
    else
      return check_run < cut_run;
  }

  /// @param check_run the run number to check
  /// @param cut_run the run number threshold
  /// @param include_bound if true, include `cut_run` in the range
  /// @returns true if `check_run` is after `cut_run`
  static boolean runIsAfter(int check_run, int cut_run, boolean include_bound) {
    if(include_bound)
      return check_run >= cut_run;
    else
      return check_run > cut_run;
  }

  /// @param check_runs the run numbers to check
  /// @returns the dataset name that contains this run
  static String findDataset(List<Integer> check_runs) {
    def datasets = check_runs.collect{ check_run ->
      if(runIsInRange(check_run, 5032, 5419, true))
        return 'rga_fa18_inbending';
      if(runIsInRange(check_run, 5423, 5666, true))
        return 'rga_fa18_outbending';
      if(runIsInRange(check_run, 16042, 16772, true))
        return 'rgc_su22';
      if(runIsInRange(check_run, 18301, 19131, true))
        return 'rgd';
      if(runIsAfter(check_run, 21317, true)) // RG-L FIXME: needs upper bound when RG-L completes <https://github.com/JeffersonLab/clas12-timeline/issues/325>
        return 'rgl';
      return 'unknown';
    }.toUnique();
    if(datasets.size() > 1) {
      System.err.println "WARNING: RunDependentCut.findDataset run list spans more than one dataset: $datasets; returning the first one;";
      System.err.println "WARNING:   runs: $check_runs";
    }
    return datasets[0];
  }

  /// @param check_run the run number to check
  /// @returns the dataset name that contains this run
  static String findDataset(int check_run) {
    return findDataset([check_run]);
  }

}
