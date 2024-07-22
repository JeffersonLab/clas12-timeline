#!/bin/bash
# pretty print a part of qaTree.json (specify tree path as arguments)
[ -z "$TIMELINE_GROOVY_OPTS" ] && source $(dirname $0)/../../bin/environ.sh
run-groovy $TIMELINE_GROOVY_OPTS ../jprint.groovy qa/qaTree.json $*
