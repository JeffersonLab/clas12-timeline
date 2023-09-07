#!/bin/bash

# error handling
printError()   { echo -e "\e[1;31m[ERROR]: $* \e[0m"   >&2; }
printWarning() { echo -e "\e[1;35m[WARNING]: $* \e[0m" >&2; }

# get the working directory
[ -z "${BASH_SOURCE[0]}" ] && thisEnv=$0 || thisEnv=${BASH_SOURCE[0]}
export TIMELINESRC=$(realpath $(dirname $thisEnv)/..)

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

# classpath to coatjava
for p in \
  "$COATJAVA/lib/clas/*"  \
  "$COATJAVA/lib/utils/*" \
  ; do
  export CLASSPATH="$p${CLASSPATH:+:${CLASSPATH}}"
done

# classpath to local dependencies, for `run-groovy`
for p in \
  $TIMELINESRC/qa-detectors/src \
  $TIMELINESRC/qa-physics       \
  ; do
  export JYPATH="$p${JYPATH:+:${JYPATH}}"
done

# java and groovy options
timeline_java_opts=(
  -DCLAS12DIR=$COATJAVA/
  -Djava.util.logging.config.file=$COATJAVA/etc/logging/debug.properties
  -Xmx1024m
)
timeline_groovy_opts=(
  -Djava.awt.headless=true
)
export TIMELINE_JAVA_OPTS="${timeline_java_opts[*]}"
export TIMELINE_GROOVY_OPTS="${timeline_groovy_opts[*]}"
