#!/bin/bash
# build root tree

if [ $# -eq 1 ]; then dataset=$1
else
  echo "USAGE: $0 [dataset]" >&2
  exit 101
fi

datfile="outdat.${dataset}/data_table.dat"

> num.tmp
n=$(echo "`cat $datfile|wc -l`/6"|bc)
for i in `seq 1 $n`; do
  for j in {1..6}; do echo $i >> num.tmp; done
done
paste -d' ' num.tmp $datfile > tree.tmp

root -l readTree.C'("'$dataset'")'
rm {num,tree}.tmp
