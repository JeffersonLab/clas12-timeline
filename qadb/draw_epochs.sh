#!/usr/bin/env bash
# build root tree

set -e
set -u
source $(dirname $0)/../libexec/environ.sh

if [ $# -eq 2 ]; then
  inDir=$1
  dataset=$2
else
  echo """
  USAGE: $0 [INPUT_DIR] [DATASET]
  - [INPUT_DIR] is a dataset's output dir from ../bin/qtl physics
  - [DATASET] is needed by draw_epochs.C to draw the epoch lines
  """ >&2
  exit 101
fi

datfile="$inDir/timeline_physics_qa/outdat/data_table.dat"
cat "$TIMELINESRC/qadb/epochs/epochs.$dataset.txt" | sed 's;#.*;;g' > epochs.tmp # strip comments

root -l $TIMELINESRC/qadb/draw_epochs.C'("'$dataset'","'$datfile'","epochs.tmp")'
rm epochs.tmp
