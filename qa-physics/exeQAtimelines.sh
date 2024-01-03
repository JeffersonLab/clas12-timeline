#!/bin/bash
# after finishing analysis in the `QA` subdirectory, this script will call
# qaCut.groovy on the results

if [ -z "$TIMELINESRC" ]; then source `dirname $0`/../bin/environ.sh; fi

if [ $# -ne 2 ]; then
  echo "USAGE: $0 [INPUT_DIR] [DATASET]" >&2
  exit 101
fi
inDir=$1
dataset=$2

qaDir=$inDir/outmon.qa

mkdir -p $qaDir
rm -r $qaDir
mkdir -p $qaDir

echo "FD part"
for bit in {0..5} 100; do
  run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $inDir $dataset false $bit
  qa=$(ls -t $inDir/outmon/electron_FD_*QA*.hipo | grep -v epoch | head -n1)
  mv $qa ${qaDir}/$(echo $qa | sed 's/^.*_QA_//g')
done

echo "FT part"
for bit in {6..9}; do
  run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $inDir $dataset useFT $bit
  qa=$(ls -t $inDir/outmon/electron_FT_*QA*.hipo | grep -v epoch | head -n1)
  mv $qa ${qaDir}/$(echo $qa | sed 's/^.*_QA_//g')
done

cp QA/qa.${dataset}/qaTree.json $qaDir
echo ""
cat $inDir/outdat/passFractions.dat
