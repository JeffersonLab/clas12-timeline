#!/bin/bash
# parse `datasetList.sh`, setting:
# - `RUNL`: lower run bound
# - `RUNH`: upper run bound
# - `DATADIR`: data directory
# usage: `source $0 $dataset`

if [ $# -ne 1 ]; then
  echo "ERROR: dataset not specified" >&2
  exit 100
fi

line=$(grep -wE "^$dataset" datasetList.txt | tail -n1)

if [ -z "$line" ]; then
  echo "ERROR: cannot find dataset '$dataset' in datasetList.txt" >&2
  exit 100
fi

RUNL=$(echo $line | awk '{print $2}')
RUNH=$(echo $line | awk '{print $3}')
DATADIR=$(echo $line | awk '{print $4}')
echo """found dataset '$dataset':
  RUNL    = $RUNL
  RUNH    = $RUNH
  DATADIR = $DATADIR"""
