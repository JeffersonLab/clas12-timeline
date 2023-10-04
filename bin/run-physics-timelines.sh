#!/bin/bash

set -e
set -u
source $(dirname $0)/environ.sh

# default options
inputDir=""
dataset=""
outputDir=""

# usage
sep="================================================================"
if [ $# -eq 0 ]; then
  echo """
  $sep
  USAGE: $0 [OPTIONS]...
  $sep
  Creates web-ready physics timelines locally

  REQUIRED OPTIONS: specify one or both of the following:

    -d [DATASET_NAME]   unique dataset name, defined by the user
                        default = based on [INPUT_DIR]

    -i [INPUT_DIR]      directory containing run subdirectories of timeline histograms
                        default = ./outfiles/[DATASET_NAME]/timeline_physics

  OPTIONAL OPTIONS:

    -o [OUTPUT_DIR]     output directory
                        default = ./outfiles/[DATASET_NAME]

  """ >&2
  exit 101
fi

# parse options
while getopts "i:d:o:" opt; do
  case $opt in
    i) 
      if [ -d $OPTARG ]; then
        inputDir=$(realpath $OPTARG)
      else
        printError "input directory $OPTARG does not exist"
        exit 100
      fi
      ;;
    d) 
      echo $OPTARG | grep -q "/" && printError "dataset name must not contain '/' " && exit 100
      [ -z "$OPTARG" ] && printError "dataset name may not be empty" && exit 100
      dataset=$OPTARG
      ;;
    o)
      outputDir=$OPTARG
      ;;
  esac
done

# set directories and dataset name
# FIXME: copied implementation from `run-detector-timelines.sh`
if [ -z "$inputDir" -a -n "$dataset" ]; then
  inputDir=$(pwd -P)/outfiles/$dataset/timeline_physics # default input directory is in ./outfiles/
elif [ -n "$inputDir" -a -z "$dataset" ]; then
  dataset=$(ruby -e "puts '$inputDir'.split('/')[-4..].join('_')") # set dataset using last few subdirectories in inputDir dirname
elif [ -z "$inputDir" -a -z "$dataset" ]; then
  printError "required options, either [INPUT_DIR] or [DATASET_NAME], have not been set"
  exit 100
fi
[ -z "$outputDir" ] && outputDir=$(pwd -P)/outfiles/$dataset

# set subdirectories
finalDir=$outputDir/timeline_web
logDir=$outputDir/log

# check input directory
if [ ! -d $inputDir ]; then
  printError "input directory $inputDir does not exist"
  exit 100
fi

# print settings
echo """
Settings:
$sep
INPUT_DIR       = $inputDir
DATASET_NAME    = $dataset
OUTPUT_DIR      = $outputDir
FINAL_DIR       = $finalDir
LOG_DIR         = $logDir
"""

pushd $TIMELINESRC/qa-physics

# setup error-filtered execution function
mkdir -p $logDir
logFile=$logDir/physics.err
logTmp=$logFile.tmp
> $logFile
function exe { 
  echo $sep
  echo "EXECUTE: $*"
  echo $sep
  $* 2> >(tee $logTmp >&2)
  if [ -s $logTmp ]; then
    echo "stderr from command:  $*" >> $logFile
    cat $logTmp >> $logFile
    echo $sep >> $logFile
  fi
}

# organize the data into datasets
exe ./datasetOrganize.sh $dataset $inputDir

# produce chargeTree.json
exe run-groovy $TIMELINE_GROOVY_OPTS buildChargeTree.groovy $dataset

# loop over datasets
# trigger electrons monitor
exe run-groovy $TIMELINE_GROOVY_OPTS qaPlot.groovy $dataset
exe run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $dataset
# FT electrons
exe run-groovy $TIMELINE_GROOVY_OPTS qaPlot.groovy $dataset FT
exe run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $dataset FT
# general monitor
exe run-groovy $TIMELINE_GROOVY_OPTS monitorPlot.groovy $dataset
# move timelines to output area
exe ./stageTimelines.sh $dataset $finalDir

popd

# print errors
echo """

"""
if [ -s $logFile ]; then
  printError "some scripts had errors or warnings; dumping error output:"
  echo $sep >&2
  cat $logFile >&2
else
  echo "No errors or warnings!"
fi
