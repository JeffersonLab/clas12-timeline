#!/bin/bash

# TODO: this will be removed soon!

if [ -z "${BASH_SOURCE[0]}" ]; then
  export CLASQA=$(dirname $(realpath $0))
else
  export CLASQA=$(dirname $(realpath ${BASH_SOURCE[0]}))
fi

export CLASQA_JAVA_OPTS="-Djava.awt.headless=true"

echo """
--- Environment ---
CLASQA           = $CLASQA
CLASQA_JAVA_OPTS = $CLASQA_JAVA_OPTS
-------------------
"""
