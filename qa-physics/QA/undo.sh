#!/usr/bin/env bash
# undo modifyQaTree.groovy, restoring previous qaTree.json backup
[ -z "$TIMELINESRC" ] && source $(dirname $0)/../../bin/environ.sh
pushd qa
mv -v `ls -t qaTree*.bak | head -n1` qaTree.json
popd
$TIMELINESRC/bin/run-groovy-timeline.sh parseQaTree.groovy
