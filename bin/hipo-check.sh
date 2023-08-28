#!/bin/bash

set -e

# arguments
if [ $# -ne 3 ]; then
  echo """
  Search a directory for HIPO files, checking each one for corruption
  or other issues

  USAGE: $0 [TOP-LEVEL DIRECTORY]
  """ >&2
  exit 101
fi
inputTopDir=$(realpath $1)
sizeThreshold=128000

## TODO:
# - option to "trash" or `rm` a bad file
# - option to stop on single failure (set -e)
# - option to set a minimum size threshold; what is the exact minimum?

# failure handler
fail() {
  echo "[+] FAILURE: $1" >&2
}

# checks and preparation
[ ! -d $inputTopDir ] && echo "ERROR: [TOP-LEVEL DIRECTORY]=$inputTopDir does not exist" >&2 && exit 100

# find input HIPO files
for inputFile in $(find $inputTopDir -name "*.hipo"); do
  echo "[+] CHECK: $inputFile"

  # check file size
  fileSize=$(wc -c < $inputFile)
  if [ $fileSize -lt $threshold ]; then
    fail "file size ($fileSize) is less than threshold ($threshold)"
  fi

  # HIPO smoke test
  # run-groovy -e """
  # TODO
  # TODO
  # TODO
  # """
  
  # run HIPO test
  hipo-utils -test $inputFile
  status=$?
  if [ $? -ne 1 ]; then
    fail "HIPO test failed"
  fi


done
