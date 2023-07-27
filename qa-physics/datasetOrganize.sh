#!/bin/bash

set -e

if [ $# -ne 1 ];then echo "USAGE: $0 [dataset]"; exit; fi
dataset=$1

# cleanup / generate new dataset subdirs
for outdir in outmon outdat; do
  dir=${outdir}.${dataset}
  echo "clean $dir"
  mkdir -p $dir
  rm -r $dir
  mkdir -p $dir
done

# loop over runs, copying and linking to dataset subdirs
source datasetListParser.sh $dataset
for file in outmon/monitor_*.hipo; do
  run=$(echo $file | sed 's/^.*monitor_//'|sed 's/\.hipo$//')

  if [ $run -ge $runL -a $run -le $runH ]; then
    echo "file run $run to dataset $dataset"
    cat outdat/data_table_${run}.dat >> outdat.${dataset}/data_table.dat
    ln -sv `pwd`/outmon/monitor_${run}.hipo ./outmon.${dataset}/monitor_${run}.hipo
  fi

done
