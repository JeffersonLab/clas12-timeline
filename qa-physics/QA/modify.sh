#!/usr/bin/env bash
# modify the qaTree using modifyQaTree.groovy
[ -z "$TIMELINESRC" ] && source $(dirname $0)/../../bin/environ.sh
$TIMELINESRC/libexec/run-groovy-timeline.sh modifyQaTree.groovy $*
