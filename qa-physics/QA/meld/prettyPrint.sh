#!/bin/bash
# (called by meld.sh)
#run-groovy ../../jprint.groovy qaTree.json.$1 > qaTree.json.${1}.pprint
run-groovy ../parseQaTree.groovy qaTree.json.$1
