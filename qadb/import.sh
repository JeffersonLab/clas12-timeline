#!/usr/bin/env bash

if [ -z "$TIMELINESRC" ]; then source `dirname $0`/../libexec/environ.sh; fi

# copy qaTree.json, so we can start the QA
if [ $# -lt 2 ]; then
  echo """
  Import a QADB file (qaTree.json) for use with these QADB tools.

  USAGE: $0 [dataset] [path to qaTree.json] [optional: parseQaTree options]

    [dataset]               may be any unique name, such as 'rga_fa18'

    [path to qaTree.json]   the path to a qaTree.json file

            note: if you have done an automatic QA, it will be in (by default)
                   'outfiles/\$dataset/timeline_physics_qa/outdat/qaTree.json'

    [parseQaTree options]   run the following to see these options:
                              $0 [dataset] -h
                              $0 [dataset] -l
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
$TIMELINESRC/libexec/run-groovy-timeline.sh $TIMELINESRC/qadb/src/parseQaTree.groovy qa/qaTree.json $opts
