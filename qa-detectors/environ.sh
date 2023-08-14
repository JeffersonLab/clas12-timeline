#!/bin/bash

if [ -z "${BASH_SOURCE[0]}" ]; then
  export CALIBQA=$(dirname $(realpath $0))
else
  export CALIBQA=$(dirname $(realpath ${BASH_SOURCE[0]}))
fi

export TIMELINEDIR="/u/group/clas/www/clas12mon/html/hipo"
JYPATH="${JYPATH}:${CALIBQA}/src:${CALIBQA}/../qa-physics"
export JYPATH=$(echo $JYPATH | sed 's/^://')
export CALIBQA_JAVA_OPTS="-Djava.awt.headless=true"

echo """
--- Environment ---
CALIBQA           = $CALIBQA
TIMELINEDIR       = $TIMELINEDIR
JYPATH            = $JYPATH
CALIBQA_JAVA_OPTS = $CALIBQA_JAVA_OPTS
-------------------
"""
