#!/bin/bash
# parse `datasetList.sh`, setting:
# - `RUNL`: lower run bound
# - `RUNH`: upper run bound
# usage: `source $0 $dataset`; ONLY use this in a script, not on the command line

if [ $# -ne 1 ]; then
  echo "ERROR: dataset not specified" >&2
  exit 100
fi
dataset_query=$1

# find the last instance of a given data set
line=$(grep -wE "^$dataset_query" datasetList.txt | tail -n1)

if [ -z "$line" ]; then
  echo "ERROR: cannot find dataset '$dataset_query' in datasetList.txt" >&2
  exit 100
fi

RUNL=$(echo $line | awk '{print $2}')
RUNH=$(echo $line | awk '{print $3}')
echo """found dataset '$dataset_query':
  RUNL    = $RUNL
  RUNH    = $RUNH"""
