#!/usr/bin/env bash

set -e
set -u
source $(dirname $0)/../libexec/environ.sh

# default options
inputDir=""
dataset=""
outputDir=""

# usage
sep="================================================================"
usage() {
  echo """
  $sep
  USAGE: $0 [OPTIONS]...
  $sep
  Creates web-ready physics timelines locally

  REQUIRED OPTIONS: specify at least one of the following:

    -d [DATASET_NAME]   unique dataset name, defined by the user

    -i [INPUT_DIR]      input directory
                        default = ./outfiles/[DATASET_NAME]

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
while getopts "d:i:o:h-:" opt; do
  case $opt in
    d)
      echo $OPTARG | grep -q "/" && printError "dataset name must not contain '/' " && exit 100
      dataset=$OPTARG
      ;;
    i) inputDir=$OPTARG ;;
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

# set input and output directories
[ -z "$inputDir" ] && inputDir=$(pwd -P)/outfiles/$dataset/timeline_physics
[ ! -d $inputDir ] && printError "input directory $inputDir does not exist" && exit 100
[ -z "$outputDir" ] && outputDir=$(realpath $(pwd -P)/outfiles/$dataset) || outputDir=$(realpath $outputDir)

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
  mv $logTmp{,.bak}
  cat $logTmp.bak |\
    { grep -v '^Picked up _JAVA_OPTIONS:' || test $? = 1; } |\
    { grep -v 'VariableMetricBuilder: no improvement' || test $? = 1; } \
    > $logTmp
  rm $logTmp.bak
  if [ -s $logTmp ]; then
    echo "stderr from command:  $*" >> $logFile
    cat $logTmp >> $logFile
    echo $sep >> $logFile
  fi
}

# organize the data into datasets
exe ./datasetOrganize.sh $dataset $inputDir $qaDir

# produce chargeTree.json
exe $TIMELINESRC/libexec/run-groovy-timeline.sh buildChargeTree.groovy $qaDir

# loop over datasets
# trigger electrons monitor
exe $TIMELINESRC/libexec/run-groovy-timeline.sh qaPlot.groovy $qaDir
exe $TIMELINESRC/libexec/run-groovy-timeline.sh qaCut.groovy $qaDir $dataset
# FT electrons
exe $TIMELINESRC/libexec/run-groovy-timeline.sh qaPlot.groovy $qaDir FT
exe $TIMELINESRC/libexec/run-groovy-timeline.sh qaCut.groovy $qaDir $dataset FT
# meld FT and FD JSON files
exe $TIMELINESRC/libexec/run-groovy-timeline.sh mergeFTandFD.groovy $qaDir
# general monitor
exe $TIMELINESRC/libexec/run-groovy-timeline.sh monitorPlot.groovy $qaDir
# move timelines to output area
exe ./stageTimelines.sh $qaDir $finalDir
# trash empty files
exe $TIMELINESRC/libexec/run-groovy-timeline.sh $TIMELINESRC/qa-physics/removeEmptyFiles.groovy $outputDir/trash $finalDir

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
