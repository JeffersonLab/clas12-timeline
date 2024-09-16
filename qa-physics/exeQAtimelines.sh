#!/usr/bin/env bash
# after finishing analysis in the `QA` subdirectory, this script will call
# qaCut.groovy on the results

 set -e

if [ -z "$TIMELINESRC" ]; then source `dirname $0`/../bin/environ.sh; fi

if [ $# -ne 2 ]; then
  echo """USAGE: $0 [INPUT_DIR] [DATASET]

  - run this script from \`$TIMELINESRC/qa-physics\`
  - [INPUT_DIR] is the location of the output timeline files from Step 2
    (likely \`outfiles/[DATASET]/timeline_physics_qa\`)
  - the \`qaTree.json\` file used will be \`QA/qa.[DATASET]/qaTree.json\`
  """ >&2
  exit 101
fi
inDir=$1
dataset=$2

qaDir=$inDir/outmon.qa

mkdir -p $qaDir
rm -r $qaDir
mkdir -p $qaDir

# defect bit lists
# FIXME: violates DRY (maybe use `jq` to parse from a bit definition's JSON file)
defect_bits_FD=(
  0
  1
  2
  3
  4
  5
  10
  11
  # 12 # not yet used
  # 13 # not yet used
  # 14 # not yet used
  # 15 # not yet used
  16
  17
  18
  19
  100 # "any defect"
)
defect_bits_FT=(
  6
  7
  8
  9
)

for bit in ${defect_bits_FD[@]}; do
  echo "================ FD DEFECT BIT $bit ================"
  run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $inDir $dataset false $bit
  qa=$(ls -t $inDir/outmon/electron_FD_*QA*.hipo | grep -v epoch | head -n1)
  mv -v $qa ${qaDir}/$(echo $qa | sed 's/^.*_QA_//g')
done

for bit in ${defect_bits_FT[@]}; do
  echo "================ FT DEFECT BIT $bit ================"
  run-groovy $TIMELINE_GROOVY_OPTS qaCut.groovy $inDir $dataset FT $bit
  qa=$(ls -t $inDir/outmon/electron_FT_*QA*.hipo | grep -v epoch | head -n1)
  mv -v $qa ${qaDir}/$(echo $qa | sed 's/^.*_QA_//g')
done

cp QA/qa.${dataset}/qaTree.json $qaDir
echo ""
cat $inDir/outdat/passFractions.dat
