#!/usr/bin/env bash

if [ -z "$TIMELINESRC" ]; then source `dirname $0`/../libexec/environ.sh; fi

# usage guide
usage() {
  echo """
  Import a QADB file (qaTree.json) for use with these QADB tools.

  USAGE: $0 [dataset] [path to qaTree.json]

    [dataset]               may be any unique name, such as 'rga_fa18'

    [path to qaTree.json]   the path to a qaTree.json file

            note: if you have done an automatic QA, it will be in (by default)
                   'outfiles/[dataset]/timeline_physics_qa/outdat/qaTree.json'
  """
}
if [ $# -eq 0 ]; then
  usage
  echo """
  NOTE: run with \`--help\` as the only argument for additional options
  """
  exit 101
fi

# if using `--help` or `--list` as the first arg, pass to `parseQaTree.groovy` and exit
if [ "$1" = '--help' ]; then
  usage
  $TIMELINESRC/libexec/run-groovy-timeline.sh $TIMELINESRC/qadb/src/parseQaTree.groovy _ --help
  echo """
  Asking RCDB about its available fields..."
  $TIMELINESRC/libexec/run-groovy-timeline.sh $TIMELINESRC/qadb/src/parseQaTree.groovy _ --list
  exit
fi

# otherwise, actually import
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
