#!/bin/bash

set -e

if [ $# -ne 1 ];then echo "USAGE: $0 [dataset]" >&2; exit 101; fi
dataset=$1

if [ -z "$TIMELINESRC" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi

# cleanup / generate new dataset subdirs
OUTMON_DIR=$TIMELINESRC/qa-physics/outmon.${dataset}
OUTDAT_DIR=$TIMELINESRC/qa-physics/outdat.${dataset}
for dir in $OUTMON_DIR $OUTDAT_DIR; do
  echo "clean $dir"
  mkdir -p $dir
  rm    -r $dir
  mkdir -p $dir
done

# loop over runs, copying and linking to dataset subdirs
INPUT_DIR=$(realpath $TIMELINESRC/outfiles/$dataset/physics)
for file in $INPUT_DIR/monitor_*.hipo; do
  ln -sv $file $OUTMON_DIR/
done
for file in $INPUT_DIR/data_table_*.dat; do
  cat $file >> $OUTDAT_DIR/data_table.dat
done
