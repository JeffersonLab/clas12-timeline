#!/bin/bash

if [ -z "${BASH_SOURCE[0]}" ]; then
  export CLASQA=$(dirname $(realpath $0))
else
  export CLASQA=$(dirname $(realpath ${BASH_SOURCE[0]}))
fi

export TIMELINEDIR="/u/group/clas/www/clas12mon/html/hipo"
JYPATH="${JYPATH}:${CLASQA}"
export JYPATH=$(echo $JYPATH | sed 's/^://')
export CLASQA_JAVA_OPTS="-Djava.awt.headless=true"

echo """
--- Environment ---
CLASQA           = $CLASQA
TIMELINEDIR      = $TIMELINEDIR
JYPATH           = $JYPATH
CLASQA_JAVA_OPTS = $CLASQA_JAVA_OPTS
-------------------
"""
