#!/usr/bin/env bash

# error handling
printError()   { echo -e "\e[1;31m[ERROR]: $* \e[0m"   >&2; }
printWarning() { echo -e "\e[1;35m[WARNING]: $* \e[0m" >&2; }

# enable/disable verbose logger
log_config=logging # set to 'logging' for quiet, or to 'debug' for verbose

# get the source code directory
[ -z "${BASH_SOURCE[0]}" ] && thisEnv=$0 || thisEnv=${BASH_SOURCE[0]}
export TIMELINESRC=$(realpath $(dirname $thisEnv)/..)

# RCDB
[ -z "${RCDB_CONNECTION-}" ] && export RCDB_CONNECTION=mysql://rcdb@clasdb.jlab.org/rcdb

# java options
timeline_java_opts=(
  -cp "$TIMELINESRC/target/*:$TIMELINESRC/target/dependency/*"
  -Djava.util.logging.config.file=$TIMELINESRC/data/logging/$log_config.properties
  -Xmx1536m
  -XX:+UseSerialGC
)

# groovy options
timeline_groovy_opts=(
  -cp "$TIMELINESRC/target/*:$TIMELINESRC/target/dependency/*"
  -Djava.awt.headless=true
)

# exports
export TIMELINE_JAVA_OPTS="${timeline_java_opts[*]}"
export TIMELINE_GROOVY_OPTS="${timeline_groovy_opts[*]}"

# cleanup vars
unset thisEnv
unset log_config
unset timeline_java_opts
unset timeline_groovy_opts
