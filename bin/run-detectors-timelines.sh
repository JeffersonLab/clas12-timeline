#!/bin/bash

set -e
set -u
source $(dirname $0)/environ.sh

# default options
dataset=test_v0
rungroup=a
inputDir=""
numThreads=4
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
  Creates detector timelines locally

  OPTIONS:

    -d [DATASET_NAME]   unique dataset name, defined by the user, used for organization
                        default = '$dataset'

    -r [RUN_GROUP]      run group, for run-group specific configurations;
                        default = '$rungroup', which specifies Run Group $(echo $rungroup | tr '[:lower:]' '[:upper:]')

    -i [INPUT_DIR]      directory of input files; by default this is based on [DATASET_NAME]:
                        default = '$TIMELINESRC/outfiles/[DATASET_NAME]/detectors'

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
while getopts "d:r:i:n:t:-:" opt; do
  case $opt in
    d) 
      echo $OPTARG | grep -q "/" && printError "dataset name must not contain '/' " && exit 100
      [ -z "$OPTARG" ] && printError "dataset name may not be empty" && exit 100
      dataset=$OPTARG
      ;;
    r) 
      rungroup=$(echo $OPTARG | tr '[:upper:]' '[:lower:]')
      ;;
    i) 
      if [ -d $OPTARG ]; then
        inputDir=$(realpath $OPTARG)
      else
        printError "input directory $OPTARG does not exist"
        exit 100
      fi
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

# set default input directory
[ -z "$inputDir" ] && inputDir=$TIMELINESRC/outfiles/$dataset/detectors

# check focus options
modes['focus-all']=true
for key in focus-timelines focus-qa; do
  if ${modes[$key]}; then modes['focus-all']=false; fi
done

# print arguments
echo """
Settings:
$sep
DATASET_NAME = $dataset
RUN_GROUP    = $rungroup
INPUT_DIR    = $inputDir
NUM_THREADS  = $numThreads
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

# set class path
GROOVYPATH=`which groovy`
GROOVYBIN=`dirname $GROOVYPATH`
export GROOVYLIB="`dirname $GROOVYBIN`/lib"
export JARPATH="$TIMELINESRC/detectors/target"
export CLASSPATH="${COATJAVA}/lib/clas/*:${COATJAVA}/lib/utils/*:$JARPATH/*:$GROOVYLIB/*"

# output directory names
outputDir=$TIMELINESRC/outfiles/$dataset/detectors/timelines
finalDir=$TIMELINESRC/outfiles/$dataset/timelines
logDir=$TIMELINESRC/outfiles/$dataset/log

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
backupDir=$TIMELINESRC/tmp/backup.$dataset.$(date +%s) # use unixtime for uniqueness
echo ">>> backing up any previous files to $backupDir ..."
mkdir -p $backupDir
if ${modes['focus-all']} || ${modes['focus-timelines']}; then
  mkdir -p $backupDir/detectors
  [ -d $outputDir ] && mv -v $outputDir $backupDir/detectors/
  [ -d $finalDir  ] && mv -v $finalDir  $backupDir/
fi
if ${modes['focus-all']} || ${modes['focus-qa']}; then
  [ -d $finalDir ] && mv -v $finalDir $backupDir/
fi
for fail in $(find $logDir -name "*.fail"); do
  rm $fail
done

# make output directories
mkdir -p $outputDir $logDir $finalDir

######################################
# produce detector timelines
######################################
if ${modes['focus-all']} || ${modes['focus-timelines']} || ${modes['list']}; then

  # change working directory to output directory
  pushd $outputDir

  # make detector subdirectories
  for detDir in ${detDirs[@]}; do
    mkdir -p $detDir
  done

  # get main executable
  # FIXME: remove run group dependence
  MAIN="org.jlab.clas.timeline.run"
  if [[ "$rungroup" == "b" ]]; then
    MAIN="org.jlab.clas.timeline.run_rgb"
  fi
  [[ ! "$rungroup" =~ ^[a-zA-Z] ]] && printError "unknown rungroup '$rungroup'" && exit 100
  export MAIN

  if ${modes['list']}; then
    echo $sep
    echo "LIST OF TIMELINES"
    echo $sep
    java $MAIN --timelines
    exit $?
  fi

  # produce timelines, multithreaded
  jobCnt=1
  for timelineObj in $(java $MAIN --timelines); do
    logFile=$logDir/$timelineObj
    [ -n "$singleTimeline" -a "$timelineObj" != "$singleTimeline" ] && continue
    if [ $jobCnt -le $numThreads ]; then
      echo ">>> producing timeline '$timelineObj' ..."
      java -DCLAS12DIR=$COATJAVA/ $MAIN $timelineObj $inputDir > $logFile.out 2> $logFile.err || touch $logFile.fail &
      let jobCnt++
    else
      wait
      jobCnt=1
    fi
  done
  wait

  # organize outputs
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

  popd
fi

######################################
# run QA
######################################

# first, copy the timelines to the final timeline directory; we do this regardless of whether QA is run
# so that (1) only `$finalDir` needs deployment and (2) we can re-run the QA with 'focus-qa' mode
echo ">>> copy timelines to final directory..."
cp -rL $outputDir/* $finalDir/

if ${modes['focus-all']} || ${modes['focus-qa']}; then
  echo ">>> add QA lines..."
  logFile=$logDir/qa
  run-groovy $TIMELINESRC/qa-detectors/util/applyBounds.groovy $outputDir $finalDir > $logFile.out 2> $logFile.err || touch $logFile.fail
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
