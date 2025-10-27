#!/usr/bin/env bash

# error handling
printError()   { echo -e "\e[1;31m[ERROR]: $* \e[0m"   >&2; }
printWarning() { echo -e "\e[1;35m[WARNING]: $* \e[0m" >&2; }

# enable/disable verbose logger
log_config=logging # set to 'logging' for quiet, or to 'debug' for verbose

# get the source code directory
[ -z "${BASH_SOURCE[0]}" ] && this_env=$0 || this_env=${BASH_SOURCE[0]}
export TIMELINESRC=$(realpath $(dirname $this_env)/..)

# database URIs
if [ -z "${RCDB_CONNECTION-}" ]; then
  export RCDB_CONNECTION=mysql://rcdb@clasdb-farm.jlab.org/rcdb
  printWarning "RCDB_CONNECTION not set, defaulting to $RCDB_CONNECTION"
fi
if [ -z "${CCDB_CONNECTION-}" ]; then
  export CCDB_CONNECTION=mysql://clas12reader@clasdb-farm.jlab.org/clas12
  printWarning "CCDB_CONNECTION not set, defaulting to $CCDB_CONNECTION"
fi

# java options
timeline_java_opts=(
  -cp "$TIMELINESRC/target/*:$TIMELINESRC/target/dependency/*"
  -Djava.util.logging.config.file=$TIMELINESRC/data/logging/$log_config.properties
  -Xmx2048m
  -XX:+UseSerialGC
  -Djava.awt.headless=true
)
export TIMELINE_JAVA_OPTS="${timeline_java_opts[*]}"

# cleanup vars
unset this_env
unset log_config
unset timeline_java_opts
