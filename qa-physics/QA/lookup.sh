#!/usr/bin/env bash
# pretty print a part of qaTree.json (specify tree path as arguments)
[ -z "$TIMELINE_GROOVY_OPTS" ] && source $(dirname $0)/../../bin/environ.sh
$TIMELINESRC/bin/run-groovy-timeline.sh $TIMELINE_GROOVY_OPTS ../jprint.groovy qa/qaTree.json $*
