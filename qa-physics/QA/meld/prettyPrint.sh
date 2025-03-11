#!/usr/bin/env bash
# (called by meld.sh)
#groovy $TIMELINE_GROOVY_OPTS ../../jprint.groovy qaTree.json.$1 > qaTree.json.${1}.pprint
groovy $TIMELINE_GROOVY_OPTS ../parseQaTree.groovy qaTree.json.$1
