#!/usr/bin/env bash
# build root tree

set -e
set -u
source $(dirname $0)/../libexec/environ.sh

if [ $# -ge 2 ]; then
  inDir=$1
  dataset=$2
  [ $# -ge 3 ] && maxNQ=$3 || maxNQ=0
else
  echo """
  USAGE: $0 [INPUT_DIR] [DATASET] [MAX_NQ_OVERRIDE]
  - INPUT_DIR        is a dataset's output dir from ../bin/qtl physics
  - DATASET          is the dataset's name
  - MAX_NQ_OVERRIDE  if set, use this value as the maximum N/q plotted
                      default: choose automatically
  """ >&2
  exit 101
fi

datfile="$inDir/timeline_physics_qa/outdat/data_table.dat"
cat "$TIMELINESRC/qadb/epochs/epochs.$dataset.txt" | sed 's;#.*;;g' > epochs.tmp # strip comments

root -l $TIMELINESRC/qadb/src/draw_epochs.C'("'$dataset'","'$datfile'","epochs.tmp",'$maxNQ')'
rm epochs.tmp
