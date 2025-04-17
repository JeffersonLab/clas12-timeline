#!/usr/bin/env bash

set -e
set -u
source $(dirname $0)/environ.sh

java $TIMELINE_JAVA_OPTS org.jlab.clas.timeline.get_beam_energy $*
