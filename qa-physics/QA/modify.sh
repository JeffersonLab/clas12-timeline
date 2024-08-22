#!/usr/bin/env bash
# modify the qaTree using modifyQaTree.groovy
[ -z "$TIMELINE_GROOVY_OPTS" ] && source $(dirname $0)/../../bin/environ.sh
run-groovy $TIMELINE_GROOVY_OPTS modifyQaTree.groovy $*
