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
hipoList=$(find $inputDir -name "monitor_*.hipo")
datList=$(find $inputDir -name "data_table_*.dat")
[ -z "$hipoList" -o -z "$datList" ] && echo "ERROR: no monitoring HIPO and/or DAT files found in $inputDir" >&2 && exit 100
for file in $hipoList; do
  ln -sv $file $(realpath $OUTMON_DIR)/
done
for file in $datList; do
  cat $file >> $OUTDAT_DIR/data_table.dat.tmp
done

# be sure the data table is sorted
sort -n -t ' ' -k1,1 -k2,2 -o $OUTDAT_DIR/data_table.dat{,.tmp}
rm $OUTDAT_DIR/data_table.dat.tmp
