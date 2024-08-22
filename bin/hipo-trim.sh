#!/usr/bin/env bash

set -e

# arguments
if [ $# -lt 3 ]; then
  echo """
  Search for HIPO files, trim each to specified number of events,
  and output the trimmed files to a specified directory

  USAGE: $0 [NUM EVENTS] [OUTPUT DIRECTORY] [INPUT DIRECTORY]...

  - finds all HIPO files in each [INPUT DIRECTORY] and trims them
    to have [NUM EVENTS] events; multiple directorys are allowed

  - the output will be in [OUTPUT DIRECTORY], with the same file tree
    as the input [INPUT DIRECTORY] directories
  """ >&2
  exit 101
fi
nEvents=$1
outputDir=$(realpath $2)
shift
shift
inputDirs=$*

# checks and preparation
[ $nEvents -lt 1 ] && echo "ERROR: [NUM EVENTS] should be greater than zero" >&2 && exit 100
mkdir -pv $outputDir

# loop over input directories
for inputDir in $inputDirs; do

  # check input directory
  inputDir=$(realpath $inputDir)
  if [ ! -d $inputDir -a ! -L $inputDir ]; then
    if [ -f $inputDir ]; then
      echo "WARNING: [INPUT DIRECTORY]=$inputDir is actually a file... ignoring" >&2
    else
      echo "ERROR: [INPUT DIRECTORY]=$inputDir does not exist" >&2
    fi
    continue
  fi

  # find input HIPO files
  for inputFile in $(find $inputDir -name "*.hipo"); do

    # create output file name
    outputSubDir=$outputDir/$(dirname $(realpath $inputFile --relative-to $inputDir/..))
    outputFile=$outputSubDir/$(basename $inputFile)
    echo """[+] TRIM:
      input:        $inputFile
      output:       $outputFile
      outputSubDir: $outputSubDir
    """

    # if the output file exists, `hipo-utils -filter` will fail
    if [ -f $outputFile ]; then
      echo """
      ERROR: this output file already exists...
      SUGGESTION: remove [OUTPUT DIRECTORY] using the following command;
                  BE SURE IT IS CORRECT BEFORE YOU RUN IT!

        rm -r $outputDir

      """ >&2
      exit 100
    fi
    mkdir -pv $outputSubDir

    # trim the input file
    hipo-utils -filter \
      -b "*::*"      \
      -n $nEvents    \
      -o $outputFile \
      $inputFile

  done

done
