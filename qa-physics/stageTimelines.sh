#!/bin/bash
# copy timeline hipo files to output timelines area, staging them for deployment

set -e

if [ $# -ne 2 ]; then
  echo "USAGE: $0 [input_dir] [output_dir]" >&2
  exit 101
fi
if [ -z "$TIMELINESRC" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi
inputDir=$1/outmon
outputDir=$2

# check HIPO files
timelineFiles=$(find $inputDir -name "*.hipo" -type f | grep -v 'monitorElec')
$TIMELINESRC/bin/hipo-check.sh $timelineFiles

# copy timelines to output directory
mkdir -p $outputDir
for file in $timelineFiles; do
  cp -v $file $outputDir/
done

# organize them
mkdir_clean() {
  mkdir -p $*
  rm -r $*
  mkdir -p $*
}
pushd $outputDir
mkdir_clean phys_qa phys_qa_extra
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

# if QADB timelines were produced, copy them too
if [ -d $inputDir.qa ]; then
  mkdir_clean qadb
  cp -v $inputDir.qa/* qadb/
fi

popd
