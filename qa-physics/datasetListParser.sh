#!/bin/bash
# parse `datasetList.sh`, setting:
# - `runL`: lower run bound
# - `runH`: upper run bound
# - `datadir`: data directory
# usage: `source $0 $dataset`

if [ $# -ne 1 ]; then
  echo "ERROR: dataset not specified"
  exit 100
fi

line=$(grep -wE "^$dataset" datasetList.txt | tail -n1)

if [ -z "$line" ]; then
  echo "ERROR: cannot find dataset '$dataset' in datasetList.txt"
  exit 100
fi

runL=$(echo $line | awk '{print $2}')
runH=$(echo $line | awk '{print $3}')
datadir=$(echo $line | awk '{print $4}')
echo """found dataset '$dataset':
  runL    = $runL
  runH    = $runH
  datadir = $datadir"""
