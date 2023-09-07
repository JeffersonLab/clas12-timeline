#!/bin/bash
# pretty print a part of qaTree.json (specify tree path as arguments)
run-groovy $TIMELINE_GROOVY_OPTS ../jprint.groovy qa/qaTree.json $*
