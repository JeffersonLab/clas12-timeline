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

> num.tmp
n=$(echo "`cat $datfile|wc -l`/6"|bc)
for i in `seq 1 $n`; do
  for j in {1..6}; do echo $i >> num.tmp; done
done
paste -d' ' num.tmp $datfile > tree.tmp

root -l readTree.C'("'$dataset'")'
rm {num,tree}.tmp
