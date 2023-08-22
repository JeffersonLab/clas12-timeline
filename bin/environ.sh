#!/bin/bash

# get the working directory
[ -z "${BASH_SOURCE[0]}" ] && thisEnv=$0 || thisEnv=${BASH_SOURCE[0]}
export TIMELINESRC=$(realpath $(dirname $thisEnv)/..)

# classpath for `run-groovy`
for p in \
  $TIMELINESRC/qa-detectors/src \
  $TIMELINESRC/qa-physics       \
  ; do
  export JYPATH="$p${JYPATH:+:${JYPATH}}"
done

# timeline webserver directory
export TIMELINEDIR=/u/group/clas/www/clas12mon/html/hipo

# error handling
printError()   { echo "[ERROR]: $1"   >&2; }
printWarning() { echo "[WARNING]: $1" >&2; }
