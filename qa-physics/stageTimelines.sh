#!/bin/bash
# copy timeline hipo files to output timelines area, staging them for deployment

if [ $# -ne 1 ]; then
  echo "USAGE: $0 [dataset]" >&2
  exit 101
fi
if [ -z "$TIMELINESRC" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi
dataset=$1

# directory names
inputDir=$TIMELINESRC/qa-physics/outmon.$dataset
outputDir=$TIMELINESRC/outfiles/$dataset/timelines
if [ ! -d $inputDir ]; then
  printError "ERROR: dataset '$dataset' files not found in $inputDir"
  exit 100
fi

# clean output directory
mkdir -p $outputDir
rm    -r $outputDir
mkdir -p $outputDir

# copy timelines to output directory
for file in $(ls $inputDir/*.hipo | grep -vE '^monitor'); do
  cp -v $file $outputDir/
done

# organize them
pushd $outputDir
mkdir -v phys_qa{,_extra}
extraList=(
  electron_FT_yield_normalized_values
  electron_FT_yield_QA_Automatic_Result
  electron_trigger_yield_QA_Automatic_Result
  electron_FT_yield_QA_epoch_view
  electron_FT_yield_stddev
  electron_FT_yield_values
  electron_trigger_yield_QA_epoch_view
  electron_trigger_yield_stddev
  electron_trigger_yield_values
  faraday_cup_stddev
  helicity_sinPhi
  relative_yield
)
for extraFile in ${extraList[@]}; do
  mv -v $extraFile.hipo phys_qa_extra/
done
mv -v *.hipo phys_qa/
popd

# if QADB timelines were produced, copy them too
if [ -d $inputDir.qa ]; then
  mkdir -p $outputDir/qadb
  cp -v $inputDir.qa/* $outputDir/qadb/
fi
