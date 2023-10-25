#!/bin/bash

set -e

if [ $# -ne 3 ];then echo "USAGE: $0 [dataset] [input_dir] [output_dir]" >&2; exit 101; fi
dataset=$1
inputDir=$2
outputDir=$3

if [ -z "$TIMELINESRC" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi

# cleanup / generate new dataset subdirs
OUTMON_DIR=$outputDir/outmon
OUTDAT_DIR=$outputDir/outdat
for dir in $OUTMON_DIR $OUTDAT_DIR; do
  echo "clean $dir"
  mkdir -p $dir
  rm    -r $dir
  mkdir -p $dir
done

# loop over runs, copying and linking to dataset subdirs
for file in $(find $inputDir -name "monitor_*.hipo"); do
  ln -sv $file $OUTMON_DIR/
done
for file in $(find $inputDir -name "data_table_*.dat"); do
  cat $file >> $OUTDAT_DIR/data_table.dat.tmp
done

# be sure the data table is sorted
sort -n -o $OUTDAT_DIR/data_table.dat{,.tmp}
rm $OUTDAT_DIR/data_table.dat.tmp
