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
errlog="errors.log"
> $errlog
function sep { printf '%70s\n' | tr ' ' -; }
function exe { 
  sep
  echo "EXECUTE: $*"
  sep
  sep >> $errlog
  echo "$* errors:" >> $errlog
  $* 2>>$errlog
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


# print errors (filtering out hipo logo contamination)
sep
echo "ERROR LOGS:"
grep -vE '█|═|Physics Division|^     $' $errlog
sep
rm $errlog

# print final message
popd
echo """


TIMELINE GENERATION COMPLETE
==============================================================================
If this script ran well, open the TIMELINE URL (printed above) in your browser
and take a look at the timelines produced for this dataset ($dataset)
==============================================================================

"""
