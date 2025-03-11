#!/usr/bin/env bash
# (called by meld.sh)
#$TIMELINESRC/bin/run-groovy-timeline.sh $TIMELINE_GROOVY_OPTS ../../jprint.groovy qaTree.json.$1 > qaTree.json.${1}.pprint
$TIMELINESRC/bin/run-groovy-timeline.sh $TIMELINE_GROOVY_OPTS ../parseQaTree.groovy qaTree.json.$1
