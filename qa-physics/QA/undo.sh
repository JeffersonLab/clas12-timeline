#!/usr/bin/env bash
# undo modifyQaTree.groovy, restoring previous qaTree.json backup
[ -z "$TIMELINE_GROOVY_OPTS" ] && source $(dirname $0)/../../bin/environ.sh
pushd qa
mv -v `ls -t qaTree*.bak | head -n1` qaTree.json
popd
run-groovy-timeline.sh $TIMELINE_GROOVY_OPTS parseQaTree.groovy
