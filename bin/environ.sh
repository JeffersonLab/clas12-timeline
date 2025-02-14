#!/usr/bin/env bash

# error handling
printError()   { echo -e "\e[1;31m[ERROR]: $* \e[0m"   >&2; }
printWarning() { echo -e "\e[1;35m[WARNING]: $* \e[0m" >&2; }

# enable/disable verbose logger
log_config=logging # set to 'logging' for quiet, or to 'debug' for verbose

# get the working directory
[ -z "${BASH_SOURCE[0]}" ] && thisEnv=$0 || thisEnv=${BASH_SOURCE[0]}
export TIMELINESRC=$(realpath $(dirname $thisEnv)/..)

# RCDB
[ -z "${RCDB_CONNECTION-}" ] && RCDB_CONNECTION=mysql://rcdb@clasdb-farm.jlab.org/rcdb
export RCDB_CONNECTION

# check coatjava environment
if [ -z "${COATJAVA-}" ]; then
  # if on a CI runner, use CI coatjava build artifacts; otherwise print error
  coatjava_ci=$TIMELINESRC/coatjava
  [ -d $coatjava_ci ] &&
    export COATJAVA=$coatjava_ci ||
    printError "cannot find coatjava; please make sure environment variable COATJAVA is set to your coatjava installation"
fi

# ensure coatjava executables are found
[ -n "${COATJAVA-}" ] && export PATH="$COATJAVA/bin${PATH:+:${PATH}}"

# class paths
java_classpath=(
  "$COATJAVA/lib/clas/*"
  "$COATJAVA/lib/utils/*"
  "$TIMELINESRC/monitoring/target/*"
)
groovy_classpath=(
  "$TIMELINESRC/detectors/target/*"
  "$(dirname $(dirname $(which groovy)))/lib/*"
)

# java and groovy options
timeline_java_opts=(
  -DCLAS12DIR=$COATJAVA/
  -Djava.util.logging.config.file=$COATJAVA/etc/logging/$log_config.properties
  -Xmx1024m
  -XX:+UseSerialGC
)
timeline_groovy_opts=(
  -Djava.awt.headless=true
)

# run java with more resources, to mitigate large memory residence for long run periods
timeline_java_opts_highmem=$(echo ${timeline_java_opts[*]} | sed 's;Xmx1024m;Xmx2048m;')

# exports
export CLASSPATH="$(echo "${java_classpath[*]}" | sed 's; ;:;g')${CLASSPATH:+:${CLASSPATH}}"
export JYPATH="$(echo "${groovy_classpath[*]}" | sed 's; ;:;g')${JYPATH:+:${JYPATH}}"
export TIMELINE_JAVA_OPTS="${timeline_java_opts[*]}"
export TIMELINE_GROOVY_OPTS="${timeline_groovy_opts[*]}"
export TIMELINE_JAVA_OPTS_HIGHMEM=$timeline_java_opts_highmem

# cleanup vars
unset thisEnv
unset log_config
unset java_classpath
unset groovy_classpath
unset timeline_java_opts
unset timeline_groovy_opts
unset timeline_java_opts_highmem
