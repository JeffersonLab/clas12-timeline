#!/bin/bash
# copy timeline hipo files to webserver area 
# - must be run on ifarm

if [ $# -ne 2 ]; then
  echo "usage: $0 [dataset] [destinationName]"
  exit
fi

dataset=$1
destdir=$2

wwwdir="../www/${USER}" # symlink www

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
mvsupplemental electron_FT_yield_QA_epoch_view.hipo
mvsupplemental electron_FT_yield_stddev.hipo
mvsupplemental electron_FT_yield_values.hipo
mvsupplemental electron_trigger_yield_QA_epoch_view.hipo
mvsupplemental electron_trigger_yield_stddev.hipo
mvsupplemental electron_trigger_yield_values.hipo
mvsupplemental faraday_cup_stddev.hipo
mvsupplemental helicity_sinPhi.hipo
popd

if [ -d "outmon.${dataset}.qa" ]; then
  cp -r outmon.${dataset}.qa ${wwwdir}/${destdir}_QA
fi