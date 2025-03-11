#!/usr/bin/env bash
# run the timeline's version of `groovy`, to ensure the correct dependencies are used
# FIXME: this script should only exist until ALL timeline groovy code is compiled;
#        at the moment, the physics QA code needs this
# NOTE: this assumes $CLASSPATH has already been set, by sourcing `environ.sh`
exec java groovy.ui.GroovyMain -Djava.awt.headless=true "$@"
