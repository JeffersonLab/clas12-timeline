#!/usr/bin/env bash
# (called by meld.sh)
#$TIMELINESRC/libexec/run-groovy-timeline.sh ../../jprint.groovy qaTree.json.$1 > qaTree.json.${1}.pprint
$TIMELINESRC/libexec/run-groovy-timeline.sh ../parseQaTree.groovy qaTree.json.$1
