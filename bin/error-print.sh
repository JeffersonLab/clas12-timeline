#!/usr/bin/env bash
# dump errors from Slurm logs
# - pass an argument to dump only UNIQUE errors
# - filters out log messages that should not be in stderr

cmd() {
  grep -HE '.*' /farm_out/$LOGNAME/clas12-timeline--*.err |\
    grep -vE '\[DataSourceDump\] --> opened file with events #' |\
    grep -vE 'Picked up _JAVA_OPTIONS:' |\
    grep --color -E '^.*\.err:'
}

if [ $# -gt 0 ]; then
  echo """
  PRINTING ONLY UNIQUE ERRORS:
  ============================
  """
  cmd | sed 's;^.*\.err:;;' | sort -u
else
  echo """
  PRINTING ALL ERRORS
  ===================
  """
  cmd
fi
