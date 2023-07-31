#!/bin/bash
# copy a locally deployed timeline to the release directory

if [ $# -ne 2 ]; then
  echo """
  USAGE: $0 [dataset] [target_directory]

    Final release of [dataset] to directory [target_directory]

    Example: $0 rga_inbending rga/pass1/qa

    WARNING: be careful not to overwrite anything you shouldn't...
  """ >&2
  exit 101
fi
if [ -z "$CLASQA" ]; then
  echo "ERROR: please source env.sh first" >&2
  exit 100
fi

dataset=$1
target=$2

wwwReleaseDir="${TIMELINEDIR}/${target}"
wwwLocalDir="${TIMELINEDIR}/$(whoami)"

echo """
dataset       = $dataset
target        = $target
wwwReleaseDir = $wwwReleaseDir
wwwLocalDir   = $wwwLocalDir
"""

rm -r ${wwwReleaseDir}/${dataset}*
cp -rv ${wwwLocalDir}/${dataset}* ${wwwReleaseDir}/
cp outdat.${dataset}/qaTree.json ${wwwReleaseDir}/${dataset}_QA/

run-groovy $CLASQA_JAVA_OPTS indexPage.groovy $wwwReleaseDir
