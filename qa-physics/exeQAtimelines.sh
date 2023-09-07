#!/bin/bash
# after finishing analysis in the `QA` subdirectory, this script will call
# qaCut.groovy on the results

if [ -z "$TIMELINESRC" ]; then source `dirname $0`/../bin/environ.sh; fi

if [ $# -ne 1 ]; then
  echo "USAGE: $0 [dataset]" >&2
  exit 101
fi
dataset=$1

qaDir=outmon.${dataset}.qa

mkdir -p $qaDir
rm -r $qaDir
mkdir -p $qaDir

for bit in {0..5} 100; do
  run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $dataset false $bit
  qa=$(ls -t outmon.${dataset}/electron_trigger_*QA*.hipo | grep -v epoch | head -n1)
  mv $qa ${qaDir}/$(echo $qa | sed 's/^.*_QA_//g')
done

cp QA/qa.${dataset}/qaTree.json $qaDir
echo ""
cat outdat.${dataset}/passFractions.dat
