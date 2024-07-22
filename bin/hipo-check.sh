#!/usr/bin/env bash

set -e
set -u
source $(dirname $0)/environ.sh

# arguments
if [ $# -lt 1 ]; then
  printError "no HIPO files specified for $(basename $0)"
  echo """
  USAGE: $0 [HIPO_FILE(S)]...

  Checks each [HIPO_FILE] for corruption, etc.
  """ >&2
  exit 101
fi
hipoFiles=$@

# minimum file size for a valid HIPO file
# - seems to be 192 bytes, but setting the threshold slightly higher may be safer
# - a HIPO file with a single empty `H1F` in a TDirectory named `/0` is 320 bytes
SIZE_THRESHOLD=200

# handle failure
declare -a badFiles=()
markBad() {
  file=$1
  shift
  printError "HIPO file '$file' $*"
  badFiles+=($file)
}

# loop over HIPO files
for hipoFile in ${hipoFiles[@]}; do
  echo "[+] CHECK: $hipoFile"

  # check existence
  [ ! -f $hipoFile ] && markBad $hipoFile "does not exist" && continue

  # check file size
  fileSize=$(wc -c < $hipoFile)
  [ $fileSize -lt $SIZE_THRESHOLD ] && markBad $hipoFile "file size ($fileSize) is less than threshold ($SIZE_THRESHOLD)" && continue

  # run `hipo-utils -test`
  set +e
  hipo-utils -test $hipoFile
  testCode=$?
  set -e
  [ $testCode -ne 0 ] && markBad $hipoFile "\`hipo-utils -test\` failed" && continue

done

# exit nonzero if any HIPO files were bad
if [ ${#badFiles[@]} -gt 0 ]; then
  printError "The following HIPO files are bad:"
  for badFile in ${badFiles[@]}; do
    echo "         - $badFile" >&2
  done
  exit 100
fi
