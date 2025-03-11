#!/usr/bin/env bash

if [ -z "$TIMELINESRC" ]; then source `dirname $0`/../../bin/environ.sh; fi

# copy qaTree.json, so we can start the QA
if [ $# -lt 2 ]; then
  echo """
  USAGE: $0 [dataset] [path to qaTree.json] [optional: options for parseQaTree.groovy]

  - to see parseQaTree options: $0 [dataset] -h
                           and: $0 [dataset] -l

  - for manual QA: $0 [dataset] -cnds=user_comment
  """ >&2
  exit 101
fi
dataset=$1
shift

# make new dataset working directory
mkdir -p qa.${dataset}
rm -r qa.${dataset}
mkdir -p qa.${dataset}

# parse arguments
qatree=""
opts=""
for opt in "$@"; do
  if [[ $opt =~ \.json$ ]]; then qatree=$opt
  else opts="$opts $opt"
  fi
done
[ -z "$qatree" ] && echo "ERROR: no qaTree.json file specified" && exit 100

# import the JSON file, and symlink qa
cp -v $qatree qa.${dataset}/qaTree.json
touch qa
rm qa
ln -sv qa.${dataset} qa
echo "imported $qatree to local area: qa/qaTree.json"

# parse the JSON file into human-readable format
$TIMELINESRC/bin/run-groovy-timeline.sh parseQaTree.groovy qa/qaTree.json $opts
