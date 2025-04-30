package org.jlab.clas.timeline.util

/// @brief functions to handle run dependent things
///
/// Many of these functions are very simple, but we prefer to use them anyway so that
/// it is easier to track what parts of the code involve run-number dependence.
class RunDependentCut {

  /// @param check_run the run number to check
  /// @param cut_run the preferred run number
  /// @returns true if `check_run` matches any of `cut_runs`
  static boolean runIsOneOf(int check_run, int... cut_runs) {
    return cut_runs.find{ it == check_run } != null;
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

}
