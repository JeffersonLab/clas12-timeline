#!/bin/bash
# runs `qa-detectors` on specified timeline directory
# - output directory path will be the same as timeline directory path, with "_qa"
#   appended to the directory name, unless a custom output directory is specified
# - dataset name is determined automatically and added to `datasetList.txt`

set -e

# arguments
if [ -z "$CALIBQA" ]; then
  echo "Setting local environment vars:"
  source $(dirname $(realpath ${BASH_SOURCE[0]}))/../qa-detectors/env.sh
fi
if [ $# -lt 1 ]; then
  echo """
  USAGE: $0 [URL or path of timelines] [OPTIONS]

  - if specifying a path, it must be relative to TIMELINEDIR, which by default is
    - TIMELINEDIR = $TIMELINEDIR
    - it may be overriden with the -t option

  OPTIONS:

    -o DIRECTORY
         custom output directory, relative to TIMELINEDIR
         by default, it is the input directory with the string '_qa' appended

    -t DIRECTORY
         set TIMELINEDIR to specified directory
  """
  exit 2
fi

# define dataset, and input/output timelines directories
indir=$(echo $1 | sed 's/\/$//g')
indir=$(echo $indir | sed 's/^http.*clas12mon\.jlab\.org\///g')
indir=$(echo $indir | sed 's/\/tlsummary$//g')
shift
outdir=${indir}_qa
while getopts "o:t:" opt; do
  case $opt in
    o) outdir=$OPTARG ;;
    t) export TIMELINEDIR=$OPTARG ;;
  esac
done

dataset=$(echo $indir | sed 's/\//_/g')
echo """
dataset     = $dataset
indir       = $indir
outdir      = $outdir
TIMELINEDIR = $TIMELINEDIR
"""
echo "$dataset $indir $outdir" >> ${CALIBQA}/datasetList.txt

# prepare output directory
echo "[+] CLEANUP OUTPUT DIRECTORY $outdir:"
mkdir -pv $TIMELINEDIR/$outdir
rm    -rv $TIMELINEDIR/$outdir
mkdir -pv $TIMELINEDIR/$outdir
echo "[+] COPY TIMELINE FILES TO OUTPUT DIRECTORY"
cp -rvL $TIMELINEDIR/$indir/* $TIMELINEDIR/$outdir/

# run the qa
echo "[+] APPLY QA BOUNDS"
run-groovy $CALIBQA_JAVA_OPTS ${CALIBQA}/util/applyBounds.groovy $dataset
