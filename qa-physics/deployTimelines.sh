#!/bin/bash
# copy timeline hipo files to webserver area 
# - must be run on ifarm

if [ $# -ne 2 ]; then
  echo "usage: $0 [dataset] [destinationName]" >&2
  exit 101
fi
if [ -z "$CLASQA" ]; then
  echo "ERROR: please source env.sh first" >&2
  exit 100
fi

dataset=$1
destdir=$2

wwwdir="${TIMELINEDIR}/$(whoami)"

mkdir -p ${wwwdir}/${destdir}
rm -r ${wwwdir}/${destdir}
mkdir -p ${wwwdir}/${destdir}
for file in `ls outmon.${dataset}/*.hipo | grep -v "monitor"`; do
  cp -v $file ${wwwdir}/${destdir}/
done

pushd $wwwdir
supplementaldir=${destdir}_supplemental
mkdir -p $supplementaldir
rm -r $supplementaldir
mkdir -p $supplementaldir
function mvsupplemental { mv -v ${destdir}/$1 ${supplementaldir}/; }
mvsupplemental electron_FT_yield_normalized_values.hipo
mvsupplemental electron_FT_yield_QA_Automatic_Result.hipo
mvsupplemental electron_trigger_yield_QA_Automatic_Result.hipo
mvsupplemental electron_FT_yield_QA_epoch_view.hipo
mvsupplemental electron_FT_yield_stddev.hipo
mvsupplemental electron_FT_yield_values.hipo
mvsupplemental electron_trigger_yield_QA_epoch_view.hipo
mvsupplemental electron_trigger_yield_stddev.hipo
mvsupplemental electron_trigger_yield_values.hipo
mvsupplemental faraday_cup_stddev.hipo
mvsupplemental helicity_sinPhi.hipo
mvsupplemental relative_yield.hipo
popd

if [ -d "outmon.${dataset}.qa" ]; then
  mkdir -p ${wwwdir}/${destdir}_QA
  rm -r ${wwwdir}/${destdir}_QA
  cp -r outmon.${dataset}.qa ${wwwdir}/${destdir}_QA
fi
