#!/bin/bash
# compares list of DSTs from `getListOfDSTs.sh` to the
# list of files in `data_table.dat`, to see if anything
# is missing; this helps protect against cases where slurm
# jobs fail silently
# - must run getListOfDSTs.sh first

if [ -z "$CLASQA" ]; then
  echo "ERROR: please source env.sh first" >&2
  exit 100
fi

if [ $# -ne 1 ];then echo "USAGE: $0 [dataset]" >&2; exit 101; fi
dataset=$1

if [ ! -f dstlist.${dataset}.dat ]; then
  echo "ERROR: dstlist.${dataset}.dat does not exist" >&2
  echo "execute getListOfDSTs.sh first" >&2
  exit 100
fi

if [ ! -f outdat.${dataset}/data_table.dat ]; then
  echo "ERROR: outdat.${dataset}/data_table.dat does not exist" >&2
  echo "execute exeTimelines.sh first" >&2
  exit 100
fi

cat outdat.${dataset}/data_table.dat | awk '{print $1" "$2}' > qalist.tmp
cat qalist.tmp | sort -n | uniq > qalist.dat
rm qalist.tmp

diff dstlist.${dataset}.dat qalist.dat | tee diff.dat
nl=$(cat diff.dat|wc -l)
echo "differences printed to diff.dat, which has $nl lines"
if [ $nl -gt 0 ]; then
  echo "integrity check FAILED"
  echo "execute vimdiff dstlist.${dataset}.dat qalist.dat"
else
  echo "integrity check PASSED"
fi
