#!/bin/bash

set -e
set -u
source $(dirname $0)/environ.sh

# default options
inputDir=""
dataset=""
rungroup=a
numThreads=8
singleTimeline=""
declare -A modes
for key in list build focus-timelines focus-qa; do
  modes[$key]=false
done

# usage
sep="================================================================"
if [ $# -eq 0 ]; then
  echo """
  $sep
  USAGE: $0 [OPTIONS]...
  $sep
  Creates web-ready detector timelines locally

  REQUIRED OPTIONS: specify at least one of the following:

    -i [INPUT_DIR]      directory containing run subdirectories of timeline histograms

    -d [DATASET_NAME]   unique dataset name, defined by the user, used for organization;
                        output files will be written to ./outfiles/[DATASET_NAME]

      NOTE:
        - use [DATASET_NAME], not [INPUT_DIR], if your input directory is ./outfiles/[DATASET_NAME],
          since if only [DATASET_NAME] is specified, then [INPUT_DIR] will be ./outfiles/[DATASET_NAME]
        - if only [INPUT_DIR] is specified, then [DATASET_NAME] will be based on [INPUT_DIR]

  OPTIONAL OPTIONS:

    -o [OUTPUT_DIR]     output directory
                        default = '$TIMELINESRC/outfiles/[DATASET_NAME]'

    -r [RUN_GROUP]      run group, for run-group specific configurations;
                        default = '$rungroup', which specifies Run Group $(echo $rungroup | tr '[:lower:]' '[:upper:]')
                        (NOTE: THIS OPTION WILL BE REMOVED SOON)

    -n [NUM_THREADS]    number of parallel threads to run
                        default = $numThreads

    -t [TIMELINE]       produce only the single detector timeline [TIMELINE]; useful for debugging
                        use --list to dump the list of timelines
                        default: run all

    --list              dump the list of timelines and exit

    --build             cleanly-rebuild the timeline code, then run

    --focus-timelines   only produce the detector timelines, do not run detector QA code
    --focus-qa          only run the QA code (assumes you have detector timelines already)
  """ >&2
  exit 101
fi

# parse options
while getopts "i:d:r:n:t:-:" opt; do
  case $opt in
    i) 
      if [ -d $OPTARG ]; then
        inputDir=$(realpath $OPTARG)
      else
        printError "input directory $OPTARG does not exist"
        exit 100
      fi
      ;;
    d) 
      echo $OPTARG | grep -q "/" && printError "dataset name must not contain '/' " && exit 100
      [ -z "$OPTARG" ] && printError "dataset name may not be empty" && exit 100
      dataset=$OPTARG
      ;;
    r) 
      rungroup=$(echo $OPTARG | tr '[:upper:]' '[:lower:]')
      ;;
    n)
      numThreads=$OPTARG
      ;;
    t)
      singleTimeline=$OPTARG
      ;;
    -)
      for key in "${!modes[@]}"; do
        [ "$key" == "$OPTARG" ] && modes[$OPTARG]=true && break
      done
      [ -z "${modes[$OPTARG]}" ] && printError "unknown option --$OPTARG" && exit 100
      ;;
    *) exit 100;;
  esac
done

# set class path to include groovy's classpath, for `java` calls
export CLASSPATH="$JYPATH${CLASSPATH:+:${CLASSPATH}}"

# get main executable for detector timelines
# FIXME: remove run group dependence
MAIN="org.jlab.clas.timeline.run"
if [[ "$rungroup" == "b" ]]; then
  MAIN="org.jlab.clas.timeline.run_rgb"
fi
[[ ! "$rungroup" =~ ^[a-zA-Z] ]] && printError "unknown rungroup '$rungroup'" && exit 100
export MAIN

# list detector timelines, if requested
if ${modes['list']}; then
  echo $sep
  echo "LIST OF TIMELINES"
  echo $sep
  java $MAIN --timelines
  exit $?
fi

# set directories and dataset name
# FIXME: copied implementation from `run-physics-timelines.sh`
if [ -z "$inputDir" -a -n "$dataset" ]; then
  inputDir=$TIMELINESRC/outfiles/$dataset/timeline_detectors # default input directory is in ./outfiles/
elif [ -n "$inputDir" -a -z "$dataset" ]; then
  dataset=$(ruby -e "puts '$inputDir'.split('/')[-4..].join('_')") # set dataset using last few subdirectories in inputDir dirname
elif [ -z "$inputDir" -a -z "$dataset" ]; then
  printError "required options, either [INPUT_DIR] or [DATASET_NAME], have not been set"
  exit 100
fi
outputDir=$TIMELINESRC/outfiles/$dataset

# set subdirectories
finalDirPreQA=$outputDir/timeline_web_preQA
finalDir=$outputDir/timeline_web
logDir=$outputDir/log

# check input directory
if [ ! -d $inputDir ]; then
  printError "input directory $inputDir does not exist"
  exit 100
fi

# check focus options
modes['focus-all']=true
for key in focus-timelines focus-qa; do
  if ${modes[$key]}; then modes['focus-all']=false; fi
done

# print settings
echo """
Settings:
$sep
INPUT_DIR       = $inputDir
DATASET_NAME    = $dataset
OUTPUT_DIR      = $outputDir
FINAL_DIR_PREQA = $finalDirPreQA
FINAL_DIR       = $finalDir
LOG_DIR         = $logDir
RUN_GROUP       = $rungroup
NUM_THREADS     = $numThreads
OPTIONS = {"""
for key in "${!modes[@]}"; do printf "%20s => %s,\n" $key ${modes[$key]}; done
echo "}"

# rebuild, if desired
if ${modes['build']}; then
  echo "building detector timeline"
  pushd $TIMELINESRC/detectors
  mvn clean package
  [ $? -ne 0 ] && exit 100
  popd
fi

# output detector subdirectories
detDirs=(
  band
  bmtbst
  central
  cnd
  ctof
  cvt
  dc
  ec
  epics
  forward
  ft
  ftof
  htcc
  ltcc
  m2_ctof_ftof
  rf
  rich
  trigger
)

# cleanup output directories
if ${modes['focus-all']} || ${modes['focus-timelines']}; then
  if [ -d $finalDirPreQA ]; then
    backupDir=$TIMELINESRC/tmp/backup.$dataset.$(date +%s) # use unixtime for uniqueness
    echo ">>> backing up any previous files to $backupDir ..."
    mkdir -p $backupDir/
    mv -v $finalDirPreQA $backupDir/
  fi
fi
if [ -d $logDir ]; then
  for fail in $(find $logDir -name "*.fail"); do
    rm $fail
  done
fi

# make output directories
mkdir -p $logDir $finalDirPreQA $finalDir

######################################
# produce detector timelines
######################################
if ${modes['focus-all']} || ${modes['focus-timelines']}; then

  # change working directory to output directory
  pushd $finalDirPreQA

  # make detector subdirectories
  for detDir in ${detDirs[@]}; do
    mkdir -p $detDir
  done

  # produce timelines, multithreaded
  jobCnt=1
  for timelineObj in $(java $MAIN --timelines); do
    logFile=$logDir/$timelineObj
    [ -n "$singleTimeline" -a "$timelineObj" != "$singleTimeline" ] && continue
    if [ $jobCnt -le $numThreads ]; then
      echo ">>> producing timeline '$timelineObj' ..."
      java $TIMELINE_JAVA_OPTS $MAIN $timelineObj $inputDir > $logFile.out 2> $logFile.err || touch $logFile.fail &
      let jobCnt++
    else
      wait
      jobCnt=1
    fi
  done
  wait

  # organize output timelines
  echo ">>> organizing output timelines..."
  timelineFiles=$(find -name "*.hipo")
  [ -z "$timelineFiles" ] && printError "no timelines were produced; check error logs in $logDir/"
  for timelineFile in $timelineFiles; do
    det=$(basename $timelineFile | sed 's;_.*;;g')
    case $det in
      bmt)    mv $timelineFile bmtbst/  ;;
      bst)    mv $timelineFile bmtbst/  ;;
      cen)    mv $timelineFile central/ ;;
      ftc)    mv $timelineFile ft/      ;;
      fth)    mv $timelineFile ft/      ;;
      rat)    mv $timelineFile trigger/ ;;
      rftime) mv $timelineFile rf/      ;;
      ctof|ftof)
        [[ "$timelineFile" =~ _m2_ ]] && mv $timelineFile m2_ctof_ftof/ || mv $timelineFile $det/
        ;;
      *)
        if [ -d $det ]; then
          mv $timelineFile $det/
        else
          printError "not sure where to put timeline '$timelineFile' for detector '$det'; please update $0 to fix this"
        fi
        ;;
    esac
  done

  # check timelines
  outputFiles=$(find . -name "*.hipo")
  [ -n "$outputFiles" ] && $TIMELINESRC/bin/hipo-check.sh $outputFiles

  popd
fi

######################################
# run QA
######################################

# first, copy the timelines to the final timeline directory; we do this regardless of whether QA is run
# so that (1) only `$finalDir` needs deployment and (2) we can re-run the QA with 'focus-qa' mode
echo ">>> copy timelines to final directory..."
cp -rL $finalDirPreQA/* $finalDir/

if ${modes['focus-all']} || ${modes['focus-qa']}; then
  echo ">>> add QA lines..."
  logFile=$logDir/qa
  run-groovy $TIMELINE_GROOVY_OPTS $TIMELINESRC/qa-detectors/util/applyBounds.groovy $finalDirPreQA $finalDir > $logFile.out 2> $logFile.err || touch $logFile.fail
  outputFiles=$(find $finalDir -name "*_QA.hipo")
  [ -n "$outputFiles" ] && $TIMELINESRC/bin/hipo-check.sh $outputFiles
fi


######################################
# error checking
######################################


# print log file info
echo """
$sep
OUTPUT AND ERROR LOGS:
$logDir/*.out
$logDir/*.err
"""

# exit nonzero if any jobs exitted nonzero
failedJobs=($(find $logDir -name "*.fail" | xargs -I{} basename {} .fail))
if [ ${#failedJobs[@]} -gt 0 ]; then
  for failedJob in ${failedJobs[@]}; do
    echo $sep
    printError "job '$failedJob' returned non-zero exit code; error log dump:"
    cat $logDir/$failedJob.err
  done
  if [ -z "$singleTimeline" -a ${modes['focus-qa']} = false ]; then
    echo $sep
    echo "To re-run only the failed timelines, for debugging, try one of the following commands:"
    for failedJob in ${failedJobs[@]}; do
      if [ "$failedJob" = "qa" ]; then
        echo "  $0 $@ --focus-qa"
      else
        echo "  $0 $@ --focus-timelines -t $failedJob"
      fi
    done
  fi
  exit 100
else
  echo "All jobs exitted normally"
fi

# grep for suspicious things in error logs
errPattern="error:|exception:"
echo """To look for any quieter errors, running \`grep -iE '$errPattern'\` on *.err files:
$sep"""
grep -iE --color "$errPattern" $logDir/*.err || echo "Good news: grep found no errors, but you still may want to take a look yourself..."
echo $sep
