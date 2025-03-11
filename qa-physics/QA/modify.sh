#!/usr/bin/env bash
# modify the qaTree using modifyQaTree.groovy
[ -z "$TIMELINE_GROOVY_OPTS" ] && source $(dirname $0)/../../bin/environ.sh
run-groovy-timeline.sh $TIMELINE_GROOVY_OPTS modifyQaTree.groovy $*
