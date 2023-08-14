#!/bin/bash
# copy a locally deployed timeline to the release directory

if [ -z "$CLASQA" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi

if [ $# -ne 2 ]; then
  echo """
  USAGE: $0 [dataset] [target_directory]

    Final release of [dataset] to directory [target_directory]
    - [target_directory] should be relative to TIMELINEDIR
    - Example:
        $0 rga_inbending rga/pass1/qa
        => relases dataset 'rga_inbending' QA timelines to the
           directory $TIMELINEDIR/rga/pass1/qa

    ------------------------------------------------------------
    WARNING: be careful not to overwrite anything you shouldn't!
    ------------------------------------------------------------
  """ >&2
  exit 101
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

subdirs=(
  ${dataset}
  ${dataset}_QA
  ${dataset}_supplemental
)

echo "CHECK LOCAL DIRECTORIES --------------------------------"
for subdir in ${subdirs[@]}; do
  echo "Checking $subdir ..."
  if [ ! -d ${wwwLocalDir}/${subdir} ]; then
    echo "ERROR: local directory ${wwwLocalDir}/${subdir} does not exist" >&2
    exit 100
  fi
done

echo "CLEAN TARGET SUBDIRECTORIES ----------------------------"
for subdir in ${subdirs[@]}; do
  mkdir -pv ${wwwReleaseDir}/${subdir}
  rm    -rv ${wwwReleaseDir}/${subdir}
done

echo "COPY TO TARGET -----------------------------------------"
for subdir in ${subdirs[@]}; do
  cp -rv ${wwwLocalDir}/${subdir} ${wwwReleaseDir}/
done
cp outdat.${dataset}/qaTree.json ${wwwReleaseDir}/${dataset}_QA/

echo "BUILD INDEX PAGE ---------------------------------------"
run-groovy $CLASQA_JAVA_OPTS indexPage.groovy $wwwReleaseDir
