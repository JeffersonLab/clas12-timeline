#!/usr/bin/env bash
# (called by meld.sh)
#$TIMELINESRC/libexec/run-groovy-timeline.sh ../src/jprint.groovy qaTree.json.$1 > qaTree.json.${1}.pprint
$TIMELINESRC/libexec/run-groovy-timeline.sh ../src/parseQaTree.groovy qaTree.json.$1
