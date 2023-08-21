#!/bin/bash

set -e

if [ $# -ne 1 ];then echo "USAGE: $0 [dataset]" >&2; exit 101; fi
dataset=$1

if [ -z "$CLASQA" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi

# cleanup / generate new dataset subdirs
for outdir in outmon outdat; do
  dir=$CLASQA/${outdir}.${dataset}
  echo "clean $dir"
  mkdir -p $dir
  rm -r $dir
  mkdir -p $dir
done

# loop over runs, copying and linking to dataset subdirs
source datasetListParser.sh $dataset
INPUT_DIR=$(realpath $CLASQA/../outfiles/physics)
for file in $INPUT_DIR/monitor_*.hipo; do
  run=$(echo $file | sed 's/^.*monitor_//'|sed 's/\.hipo$//')

  if [ $run -ge $RUNL -a $run -le $RUNH ]; then
    echo "file run $run to dataset $dataset"
    cat $INPUT_DIR/data_table_${run}.dat >> $CLASQA/outdat.${dataset}/data_table.dat
    ln -sv $INPUT_DIR/monitor_${run}.hipo $CLASQA/outmon.${dataset}/monitor_${run}.hipo
  fi

done
