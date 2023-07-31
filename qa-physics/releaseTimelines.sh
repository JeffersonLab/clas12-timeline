#!/bin/bash
# copy a locally deployed timeline to the release directory

if [ $# -ne 1 ]; then 
  echo "USAGE: $0 [dataset]" >&2
  exit 101
fi
if [ -z "$CLASQA" ]; then
  echo "ERROR: please source env.sh first" >&2
  exit 100
fi

dataset=$1
rungroup=$(echo $dataset|sed 's/_.*$//g')

wwwReleaseDir="${CLASQAWWW}/${rungroup}/pass1/qa"
wwwLocalDir="${CLASQAWWW}/$(whoami)"

echo "dataset=$dataset"
echo "rungroup=$rungroup"
echo "wwwReleaseDir=$wwwReleaseDir"
echo "wwwLocalDir=$wwwLocalDir"

rm -r ${wwwReleaseDir}/${dataset}*
cp -rv ${wwwLocalDir}/${dataset}* ${wwwReleaseDir}/
cp outdat.${dataset}/qaTree.json ${wwwReleaseDir}/${dataset}_QA/

run-groovy $CLASQA_JAVA_OPTS indexPage.groovy $wwwReleaseDir
