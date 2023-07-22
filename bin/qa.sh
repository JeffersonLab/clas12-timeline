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
  USAGE: $0 [URL (or path) of timelines directory] [optional: custom output dir]
   - if specifying a path, it must be relative to TIMELINEDIR = $TIMELINEDIR
   - optional output directory must also be relative to TIMELINEDIR = $TIMELINEDIR
     (otherwise the default is based on the input timeline directory name)
  """
  exit 2
fi

# define dataset, and input/output timelines directories
indir=$(echo $1 | sed 's/\/$//g')
indir=$(echo $indir | sed 's/^http.*clas12mon\.jlab\.org\///g')
indir=$(echo $indir | sed 's/\/tlsummary$//g')
[ $# -ge 2 ] && outdir=$2 || outdir=${indir}_qa
dataset=$(echo $indir | sed 's/\//_/g')
echo "dataset = $dataset"
echo "indir   = $indir"
echo "outdir  = $outdir"
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
