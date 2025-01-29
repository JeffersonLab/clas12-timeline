#!/usr/bin/env bash

set -e
set -u
source $(dirname $0)/environ.sh

# set class path to include groovy's classpath, for `java` calls
export CLASSPATH="$JYPATH${CLASSPATH:+:${CLASSPATH}}"

java org.jlab.clas.timeline.get_beam_energy $*
