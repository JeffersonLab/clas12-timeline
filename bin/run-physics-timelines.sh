#!/bin/bash

set -e
set -u
source $(dirname $0)/environ.sh

# default options
inputDir=""
dataset=""
outputDir=""

# input finding command
inputCmd="$TIMELINESRC/bin/set-input-dir.sh -s timeline_physics"
inputCmdOpts=""

# usage
sep="================================================================"
usage() {
  echo """
  $sep
  USAGE: $0 [OPTIONS]...
  $sep
  Creates web-ready physics timelines locally

  REQUIRED OPTIONS: specify at least one of the following:""" >&2
  $inputCmd -h
  echo """
  OPTIONAL OPTIONS:

    -o [OUTPUT_DIR]     output directory
                        default = ./outfiles/[DATASET_NAME]

    -h, --help          print this usage guide
  """ >&2
}
if [ $# -eq 0 ]; then
  usage
  exit 101
fi

# parse options
helpMode=false
while getopts "d:i:Uo:h-:" opt; do
  case $opt in
    d) inputCmdOpts+=" -d $OPTARG" ;;
    i) inputCmdOpts+=" -i $OPTARG" ;;
    U) inputCmdOpts+=" -U" ;;
    o) outputDir=$OPTARG ;;
    h) helpMode=true ;;
    -)
      [ "$OPTARG" != "help" ] && printError "unknown option --$OPTARG"
      helpMode=true
      ;;
  esac
done
if $helpMode; then
  usage
  exit 101
fi

# set input/output directories and dataset name
dataset=$($inputCmd $inputCmdOpts -D)
inputDir=$($inputCmd $inputCmdOpts -I)
[ -z "$outputDir" ] && outputDir=$(pwd -P)/outfiles/$dataset

# set subdirectories
qaDir=$outputDir/timeline_physics_qa
finalDir=$outputDir/timeline_web
logDir=$outputDir/log

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
exe ./datasetOrganize.sh $dataset $inputDir $qaDir

# produce chargeTree.json
exe run-groovy $TIMELINE_GROOVY_OPTS buildChargeTree.groovy $qaDir

# loop over datasets
# trigger electrons monitor
exe run-groovy $TIMELINE_GROOVY_OPTS qaPlot.groovy $qaDir
exe run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $qaDir $dataset
# FT electrons
exe run-groovy $TIMELINE_GROOVY_OPTS qaPlot.groovy $qaDir FT
exe run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $qaDir $dataset FT
# meld FT and FD JSON files
exe run-groovy $TIMELINE_GROOVY_OPTS mergeFTandFD.groovy $qaDir
# general monitor
exe run-groovy $TIMELINE_GROOVY_OPTS monitorPlot.groovy $qaDir
# move timelines to output area
exe ./stageTimelines.sh $qaDir $finalDir

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
