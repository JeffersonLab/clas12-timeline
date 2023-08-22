#!/bin/bash
# undo modifyQaTree.groovy, restoring previous qaTree.json backup
pushd qa
mv -v `ls -t qaTree*.bak | head -n1` qaTree.json
popd
run-groovy parseQaTree.groovy
