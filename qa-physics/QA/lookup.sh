#!/usr/bin/env bash
# pretty print a part of qaTree.json (specify tree path as arguments)
[ -z "$TIMELINESRC" ] && source $(dirname $0)/../../libexec/environ.sh
$TIMELINESRC/libexec/run-groovy-timeline.sh ../jprint.groovy qa/qaTree.json $*
