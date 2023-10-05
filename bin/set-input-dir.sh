#!/bin/bash
# helper script for setting the default dataset name and input directory; this is not
# meant to be used as a standalone script, rather it is called by other scripts

set -e
set -u
source $(dirname $0)/environ.sh

# defaults
inputDir=""
subDir=""
dataset=""
datasetDefault="test_v0"
useDefaults=false
printDatasetOnly=false
printInputDirOnly=false

# usage guide
usage() {
  echo """
    -d [DATASET_NAME]   unique dataset name, defined by the user
                        default = $datasetDefault

    -i [INPUT_DIR]      directory containing run subdirectories of timeline histograms
                        default = ./outfiles/[DATASET_NAME]/$subDir

    -U                  use the above default [DATASET_NAME] and [INPUT_DIR]""" >&2
}
if [ $# -eq 0 ]; then
  echo """USAGE: $0 [OPTIONS]...

  NOTE: this script is primarily used by other scripts, and is not useful on the command line""" >&2
  usage
  echo """
    -s [SUBDIR]         input subdirectory (optional)
                        default = ''

    -h                  print terse help guide (useful for callers)

    -D                  return just the dataset

    -I                  return just the input directory""" >&2
  exit 101
fi

# parse options
while getopts "d:i:Us:hDI" opt; do
  case $opt in
    d) 
      echo $OPTARG | grep -q "/" && printError "dataset name must not contain '/' " && exit 100
      dataset=$OPTARG
      ;;
    i) 
      inputDir=$OPTARG
      if [ -n "$inputDir" ]; then
        if [ -d $OPTARG ]; then
          inputDir=$(realpath $inputDir)
        else
          printError "input directory $inputDir does not exist"
          exit 100
        fi
      fi
      ;;
    U)
      useDefaults=true
      ;;
    s)
      subDir=$OPTARG
      ;;
    h)
      usage
      exit 0
      ;;
    D)
      printDatasetOnly=true
      ;;
    I)
      printInputDirOnly=true
      ;;
  esac
done

# set default input directory and dataset
if $useDefaults; then
  dataset=""
  inputDir=""
fi
[ -z "$dataset" ] && dataset=$datasetDefault
[ -z "$inputDir" ] && inputDir=$(pwd -P)/outfiles/$dataset/$subDir

# chomp trailing /
inputDir=$(echo $inputDir | sed 's;/$;;')

# return results
if $printDatasetOnly; then
  echo $dataset
elif $printInputDirOnly; then
  echo $inputDir
else
  echo "$dataset $inputDir"
fi
