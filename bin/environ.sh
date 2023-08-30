#!/bin/bash

# error handling
printError()   { echo -e "\e[1;31m[ERROR]: $* \e[0m"   >&2; }
printWarning() { echo -e "\e[1;35m[WARNING]: $* \e[0m" >&2; }

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

# check coatjava environment
if [ -z "${COATJAVA-}" ]; then
  # if on a CI runner, use CI coatjava build artifacts; otherwise print error
  coatjava_ci=$TIMELINESRC/coatjava/coatjava
  [ -d $coatjava_ci ] &&
    export COATJAVA=$coatjava_ci ||
    printError "cannot find coatjava; please make sure environment variable COATJAVA is set to your coatjava installation"
fi

# ensure coatjava executables are found
[ -n "${COATJAVA-}" ] && export PATH="$COATJAVA/bin${PATH:+:${PATH}}"
