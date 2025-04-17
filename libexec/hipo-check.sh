#!/usr/bin/env bash

set -e
set -u
source $(dirname $0)/environ.sh

# default options
rm_bad=false
hipoFiles=()

# arguments
if [ $# -lt 1 ]; then
  echo """
  USAGE: $0 [OPTIONS]... [HIPO_FILE(S)]...

  Checks each [HIPO_FILE] for corruption, etc.

  OPTIONS
    --rm-bad           delete (rm) bad HIPO files
  """ >&2
  exit 101
fi
for arg in "$@"; do
  if [[ $arg =~ ^- ]]; then
    case $arg in
      --rm-bad) rm_bad=true ;;
      *) printError "unknown option $arg" && exit 100 ;;
    esac
  else
    hipoFiles+=($arg)
  fi
done
[ ${#hipoFiles[@]} -lt 1 ] && printError "no HIPO files specified for $(basename $0)" && exit 100

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
  java $TIMELINE_JAVA_OPTS org.jlab.jnp.hipo4.utils.HipoUtilities -test $hipoFile
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
  # remove bad HIPO files
  if $rm_bad; then
    printError "These HIPO files will now be REMOVED!"
    for badFile in ${badFiles[@]}; do
      rm -v $badFile >&2
    done
  fi
  exit 100
fi
