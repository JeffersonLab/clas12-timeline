#!/bin/bash
# get the list of DST files for specified DST directory
# - this script may take a while to run
# - use the output for integrityCheck.sh

set -e

if [ -z "$TIMELINESRC" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi

if [ $# -ne 1 ];then echo "USAGE: $0 [DST directory]" >&2; exit 101; fi

tapeDir=$(echo $1 | sed 's/^\/cache/\/mss/g')
echo "tapeDir=$tapeDir"
ls $tapeDir


find $tapeDir -name "*.hipo" -print | sed 's/^.*\/rec_clas_//g' > dstfilelist.tmp

> dstlist.tmp
while read dst; do
  run=$(echo $dst | sed 's/\..*$//g' | sed 's/^0*//')
  file=$(echo $dst | awk -F. '{print $3}' | sed 's/-.*$//g' | sed 's/^0*//')
  if [ -z "$file" ]; then file=0; fi
  #echo "dst=$dst run=$run file=$file"
  echo "$run $file" | tee -a dstlist.tmp
done < dstfilelist.tmp
rm dstfilelist.tmp

cat dstlist.tmp | sort -n > dstlist.dat
rm dstlist.tmp
