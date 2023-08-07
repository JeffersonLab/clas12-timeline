#!/bin/bash
# get the list of DST files for specified dataset
# - this script may take a while to run
# - use the output for integrityCheck.sh

set -e

if [ -z "$CLASQA" ]; then
  echo "ERROR: please source env.sh first" >&2
  exit 100
fi

if [ $# -ne 1 ];then echo "USAGE: $0 [dataset]" >&2; exit 101; fi
dataset=$1

source datasetListParser.sh $dataset
DATADIR=$(echo $DATADIR | sed 's/^\/cache/\/mss/g')
echo "DATADIR=$DATADIR"
ls $DATADIR


find $DATADIR -name "*.hipo" -print | sed 's/^.*\/rec_clas_//g' > dstfilelist.tmp

> dstlist.tmp
while read dst; do
  run=$(echo $dst | sed 's/\..*$//g' | sed 's/^0*//')
  file=$(echo $dst | awk -F. '{print $3}' | sed 's/-.*$//g' | sed 's/^0*//')
  if [ -z "$file" ]; then file=0; fi
  #echo "dst=$dst run=$run file=$file"
  echo "$run $file" | tee -a dstlist.tmp
done < dstfilelist.tmp
rm dstfilelist.tmp

cat dstlist.tmp | sort -n > dstlist.${dataset}.dat
rm dstlist.tmp
