#!/bin/bash
# copy timelines to a target webserver directory

source $(dirname $0)/environ.sh

# timeline webserver directory
TIMELINEDIR=/u/group/clas/www/clas12mon/html/hipo

usage() {
  echo """
  USAGE: $0 [OPTIONS]... [DATASET] [TARGET_DIRECTORY]

  =================================================================
  CAUTION: READ ALL OF THIS BEFORE RUNNING !

     If you really don't want to read, do a dry run using -d:
       $0 -d [ARGS]...

  WARNING: be careful not to overwrite anything you shouldn't
  =================================================================

  REQUIRED ARGUMENTS: copy timelines for dataset [DATASET] to [TARGET_DIRECTORY]
  -------------------

  - [DATASET]  the unique dataset name used by other scripts;
               final timeline files are assumed to be in the default location;
               if they are elsewhere, use the -i option

  - [TARGET_DIRECTORY]   the top-level destination where the directory of
                         final timelines will be copied to

    - [TARGET_DIRECTORY] is RELATIVE to \$TIMELINEDIR, where
        TIMELINEDIR = $TIMELINEDIR
    - use the -c option for a [TARGET_DIRECTORY] not relative to \$TIMELINEDIR
    - you must have write permission to the destination directory
    - when in doubt, explore \$TIMELINEDIR
    - recommendations for [TARGET_DIRECTORY]:
      
      \$LOGNAME                  your personal directory for testing

      [RUN_GROUP]/pass[PASS]    the Run Group's directory; note the
                                file organization may vary between Run Groups

  OPTIONAL ARGUMENTS: must precede REQUIRED ARGUMENTS
  -------------------

  -d                     dry-run: prints what your command will do, but does not do it

  -c                     interpret [TARGET_DIRECTORY] as a custom directory,
                         not relative to \$TIMELINEDIR

  -s [SUB_DIRECTORY]     customize the subdirectory of [TARGET_DIRECTORY] to
                         where timeline files will be copied
                           default = [DATASET]

  -i [INPUT_DIRECTORY]   specify an alternate input directory of timelines
                           default = $TIMELINESRC/outfiles/[DATASET]/timelines
  """ >&2
}

# parse arguments
[ $# -lt 2 ] && usage && exit 101
### optional arguments
dryRun=false
customTarget=false
subDir=""
inputDir=""
while getopts "dcs:i:" opt; do
  case $opt in
    d) dryRun=true       ;;
    c) customTarget=true ;;
    s) subDir=$OPTARG    ;;
    i) inputDir=$OPTARG  ;;
    *) exit 100          ;;
  esac
done
shift $((OPTIND - 1))
### required arguments
[ $# -ne 2 ] && usage && exit 101
dataset=$1
targetDirArg=$2

# specify input directory
[ -z "$inputDir" ] && inputDir=$TIMELINESRC/outfiles/$dataset/timelines
[ ! -d $inputDir ] && printError "input timelines directory $inputDir does not exist" && exit 100

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

  WARNING: [DESTINATION] will be REMOVED before deploying!
-----------------------------------------------------------
"""
if ${dryRun}; then
  echo """
  This was a dry run, and nothing was done. Inspect the output above. Make sure
  that you have write access to [DESTINATION]. If everything looks okay, re-run
  your command without the -d option.
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
run-groovy $TIMELINE_GROOVY_OPTS $TIMELINESRC/bin/index-webpage.groovy $targetDir
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
