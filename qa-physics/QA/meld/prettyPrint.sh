#!/bin/bash
# (called by meld.sh)
#run-groovy $TIMELINE_GROOVY_OPTS ../../jprint.groovy qaTree.json.$1 > qaTree.json.${1}.pprint
run-groovy $TIMELINE_GROOVY_OPTS ../parseQaTree.groovy qaTree.json.$1
