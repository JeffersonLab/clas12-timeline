#!/usr/bin/env bash
# run the timeline's version of `groovy`, to ensure the correct dependencies are used
# FIXME: this script should only exist until ALL timeline groovy code is compiled;
#        at the moment, the physics QA code needs this

if [ -z "$TIMELINE_GROOVY_OPTS" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi

exec java $TIMELINE_GROOVY_OPTS groovy.ui.GroovyMain "$@"
