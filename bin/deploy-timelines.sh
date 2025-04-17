#!/usr/bin/env bash
# copy timelines to a target webserver directory

set -e
set -u
source $(dirname $0)/../libexec/environ.sh

# timeline webserver directory
TIMELINEDIR=/u/group/clas/www/clas12mon/html/hipo

# input finding command
inputCmd="$TIMELINESRC/libexec/set-input-dir.sh -s timeline_web"
inputCmdOpts=""

usage() {
  echo """
  USAGE: $0 [OPTIONS]...

  ========================================================================
  | CAUTION: READ ALL OF THIS GUIDE BEFORE RUNNING!
  |
  |   If you want a quick start, try the most typical command:
  |     $0 -d [DATASET] -t [TARGET_DIRECTORY] -D
  |   This will only print what will be done; remove -D to actually run
  |
  | WARNING: be careful not to overwrite anything you shouldn't
  ========================================================================

  REQUIRED OPTIONS: copy timelines for dataset [DATASET] to [TARGET_DIRECTORY]

  *** Specify at least one of the following:""" >&2
  $inputCmd -h
  echo """
  *** Also, specify the target:

    -t [TARGET_DIRECTORY]   the top-level destination where the directory of
                            final timelines will be copied to

  NOTES:
    - [TARGET_DIRECTORY] is RELATIVE to \$TIMELINEDIR, where
        TIMELINEDIR = $TIMELINEDIR
    - use the -c option for a [TARGET_DIRECTORY] not relative to \$TIMELINEDIR
    - you must have write permission to the destination directory
    - recommendations for [TARGET_DIRECTORY]:

      \$LOGNAME                  your personal directory for testing

      [RUN_GROUP]/pass[PASS]    the Run Group's directory; note the
                                file organization may vary between Run Groups

  OPTIONAL OPTIONS:
  -----------------

  -D                     dry-run: prints what your command will do, but does not do it

  -c                     interpret [TARGET_DIRECTORY] as a custom directory,
                         not relative to \$TIMELINEDIR

  -s [SUB_DIRECTORY]     customize the subdirectory of [TARGET_DIRECTORY] to
                         where timeline files will be copied
                           default = [DATASET]

  -m [METADATA]          include run-filter controls (for specific datasets)
                         available choices:
$(find $TIMELINESRC/data/metadata -name "*.json" -exec basename {} .json \; | sed 's;^;                           ;')

  -n [NOTE]              add a note that will be shown on the clas12mon webpage
                         (i.e., the README file); surround your note in quotes

  -h                     print this usage guide
  """ >&2
}

# parse arguments
[ $# -lt 2 ] && usage && exit 101
### optional arguments
dataset=""
targetDirArg=""
dryRun=false
customTarget=false
subDir=""
metadataFile=""
readmeNote=""
inputDir=""
helpMode=false
while getopts "d:i:Ut:Dcs:m:n:h-:" opt; do
  case $opt in
    d) inputCmdOpts+=" -d $OPTARG" ;;
    i) inputCmdOpts+=" -i $OPTARG" ;;
    U) inputCmdOpts+=" -U" ;;
    t) targetDirArg=$OPTARG ;;
    D) dryRun=true ;;
    c) customTarget=true ;;
    s) subDir=$OPTARG ;;
    m) metadataFile=$OPTARG ;;
    n) readmeNote=$OPTARG ;;
    h) helpMode=true ;;
    -)
      [ "$OPTARG" != "help" ] && printError "unknown option --$OPTARG"
      helpMode=true
      ;;
    *) exit 100 ;;
  esac
done
if $helpMode; then
  usage
  exit 101
fi

# set input directory and dataset name
dataset=$($inputCmd $inputCmdOpts -D)
inputDir=$($inputCmd $inputCmdOpts -I)

# check required options
[ -z "$dataset" -o -z "$inputDir" -o -z "$targetDirArg" ] && printError "missing one or more required options" && usage && exit 100

# check if the metadata file exists
if [ -n "$metadataFile" ]; then
  metadataFile=$TIMELINESRC/data/metadata/$metadataFile.json
  if [ -f "$metadataFile" ]; then
    echo "using metadata file '$metadataFile'"
  else
    printError "metadata file '$metadataFile' does not exist (bad argument for option '-m')"
    exit 100
  fi
fi

# print the README note, if set
[ -n "$readmeNote" ] && echo "using the note: '$readmeNote'"

# specify target directory
[ -z "$subDir" ] && subDir=$dataset
if ${customTarget}; then
  targetDir=$targetDirArg/$subDir
else
  targetDir=$TIMELINEDIR/$targetDirArg/$subDir
fi

# print what we will do; exit prematurely if dry run
echo """
-----------------------------------------------------------
DEPLOYMENT PLAN: copy [SOURCE]/* [DESTINATION]/*

  [SOURCE]:      $inputDir

  [DESTINATION]: $targetDir

  [NOTE]:        '$readmeNote'

  WARNING: [DESTINATION] will be REMOVED before deploying!
-----------------------------------------------------------
"""
if ${dryRun}; then
  echo """
  This was a dry run, and nothing was done. Inspect the output above. Make sure
  that you have write access to [DESTINATION]. If everything looks okay, re-run
  your command without the -D option.
  """
  exit 0
fi

# deploy
echo """-------------------------
DEPLOYING
-------------------------"""
mkdir -pv $targetDir
rm    -rv $targetDir
mkdir -pv $targetDir
cp -rv $inputDir/* $targetDir/
[ -n "$metadataFile" ] && cp -v $metadataFile $targetDir/metadata.json
[ -n "$readmeNote" ] && echo "$readmeNote" > $targetDir/README
$TIMELINESRC/libexec/run-groovy-timeline.sh $TIMELINESRC/libexec/index-webpage.groovy $targetDir
echo "DONE."

# print URL
echo "-----------------------------------------------------------"
if ${customTarget}; then
  domain="https://[YOUR_CUSTOM_DOMAIN]"
  msg="""
  Since you specified a custom [TARGET_DIRECTORY] with -c, you will need to
  figure out the correct URL yourself; use the following URL as a template,
  noting that you may need to remove some parent directories from the URL
  """
else
  domain="https://clas12mon.jlab.org"
  msg=""
fi
echo """
TIMELINE URL:
  $msg
  $domain/$targetDirArg/$subDir/tlsummary
"""
echo "-----------------------------------------------------------"
