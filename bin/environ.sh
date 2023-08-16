#!/bin/bash

# get the correct working directory
[ -z "${BASH_SOURCE[0]}" ] && thisEnv=$0 || thisEnv=${BASH_SOURCE[0]}
mainDir=$(realpath $(dirname $thisEnv)/..)

# classpath for `run-groovy`
for p in \
  $mainDir/qa-detectors/src \
  $mainDir/qa-physics       \
  ; do
  export JYPATH="$p${JYPATH:+:${JYPATH}}"
done

# timeline webserver directory
export TIMELINEDIR=/u/group/clas/www/clas12mon/html/hipo
