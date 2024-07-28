#!/usr/bin/env bash
# build root tree

if [ $# -eq 2 ]; then
  inDir=$1
  dataset=$2
else
  echo """
  USAGE: $0 [INPUT_DIR] [DATASET]
  - [INPUT_DIR] is a dataset's output dir from ../bin/run-physics-timelines.sh
  - [DATASET] is needed by readTree.C to draw the epoch lines
  """ >&2
  exit 101
fi

datfile="$inDir/timeline_physics_qa/outdat/data_table.dat"
cat "epochs/epochs.$dataset.txt" | sed 's;#.*;;g' > epochs.tmp # strip comments

root -l readTree.C'("'$dataset'","'$datfile'","epochs.tmp")'
rm epochs.tmp
