#!/bin/bash

set -e
set -u
source $(dirname $0)/environ.sh

# arguments
dataset=test_v0
if [ $# -eq 0 ]; then
  echo """
  USAGE: $0 [dataset]
  (the default dataset is '$dataset'
  """ >&2
  exit 101
fi
[[ "$1" == "-d" ]] && shift # accept -d option, for consistency with other scripts
dataset=$1
[ -z "$dataset" ] && printError "dataset not specified" && exit 100

pushd $TIMELINESRC/qa-physics

# setup error filtered execution function
logDir=$TIMELINESRC/outfiles/$dataset/log
mkdir -p $logDir
logFile=$logDir/physics.err
logTmp=$logFile.tmp
> $logFile
function sep { printf '%70s\n' | tr ' ' -; }
function exe { 
  sep
  echo "EXECUTE: $*"
  sep
  $* 2> $logTmp
  if [ -s $logTmp ]; then
    echo "stderr from command:  $*" >> $logFile
    cat $logTmp >> $logFile
    sep >> $logFile
  fi
}

# organize the data into datasets
exe ./datasetOrganize.sh $dataset

# produce chargeTree.json
exe run-groovy buildChargeTree.groovy $dataset

# loop over datasets
# trigger electrons monitor
exe run-groovy qaPlot.groovy $dataset
exe run-groovy qaCut.groovy $dataset
# FT electrons
exe run-groovy qaPlot.groovy $dataset FT
exe run-groovy qaCut.groovy $dataset FT
# general monitor
exe run-groovy monitorPlot.groovy $dataset
# move timelines to output area
exe ./stageTimelines.sh $dataset

popd

# print errors
echo """

"""
if [ -s $logFile ]; then
  printError "some scripts had errors or warnings; dumping error output:"
  sep >&2
  cat $logFile >&2
else
  echo "No errors or warnings!"
fi
