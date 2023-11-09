#!/bin/bash

set -e
set -u
source $(dirname $0)/environ.sh

# default options
inputDir=""
dataset=""
outputDir=""
rungroup=a
numThreads=8
singleTimeline=""
declare -A modes
for key in list build skip-mya focus-timelines focus-qa debug help; do
  modes[$key]=false
done

# input finding command
inputCmd="$TIMELINESRC/bin/set-input-dir.sh -s timeline_detectors"
inputCmdOpts=""

# usage
sep="================================================================"
usage() {
  echo """
  $sep
  USAGE: $0 [OPTIONS]...
  $sep
  Creates web-ready detector timelines locally

  REQUIRED OPTIONS: specify at least one of the following:""" >&2
  $inputCmd -h
  echo """
  OPTIONAL OPTIONS:

    -o [OUTPUT_DIR]     output directory
                        default = ./outfiles/[DATASET_NAME]

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

    --skip-mya          skip timelines which require MYA (needed if running offsite or on CI)

    --focus-timelines   only produce the detector timelines, do not run detector QA code
    --focus-qa          only run the QA code (assumes you have detector timelines already)

    --debug             enable debug mode: run a single timeline with stderr and stdout printed to screen;
                        it is best to use this with the '-t' option to debug specific timeline issues

    -h, --help          print this usage guide
  """ >&2
}
if [ $# -eq 0 ]; then
  usage
  exit 101
fi

# parse options
while getopts "d:i:Uo:r:n:t:h-:" opt; do
  case $opt in
    d) inputCmdOpts+=" -d $OPTARG" ;;
    i) inputCmdOpts+=" -i $OPTARG" ;;
    U) inputCmdOpts+=" -U" ;;
    o) outputDir=$OPTARG ;;
    r) rungroup=$(echo $OPTARG | tr '[:upper:]' '[:lower:]') ;;
    n) numThreads=$OPTARG ;;
    t) singleTimeline=$OPTARG ;;
    h) modes['help']=true ;;
    -)
      for key in "${!modes[@]}"; do
        [ "$key" == "$OPTARG" ] && modes[$OPTARG]=true && break
      done
      [ -z "${modes[$OPTARG]-}" ] && printError "unknown option --$OPTARG" && exit 100
      ;;
    *) exit 100;;
  esac
done
if ${modes['help']}; then
  usage
  exit 101
fi

# set class path to include groovy's classpath, for `java` calls
export CLASSPATH="$JYPATH${CLASSPATH:+:${CLASSPATH}}"

# get main executable for detector timelines
# FIXME: remove run group dependence
MAIN="org.jlab.clas.timeline.run"
if [ "$rungroup" = "b" -o "$rungroup" = "d" ]; then
  MAIN="org.jlab.clas.timeline.run_rgb"
fi
[[ ! "$rungroup" =~ ^[a-zA-Z] ]] && printError "unknown rungroup '$rungroup'" && exit 100
export MAIN

# build list of timelines
if ${modes['skip-mya']}; then
  timelineList=$(java $MAIN --timelines | grep -vE '^epics_')
else
  timelineList=$(java $MAIN --timelines)
fi

# list detector timelines, if requested
if ${modes['list']}; then
  echo $sep
  echo "LIST OF TIMELINES"
  echo $sep
  echo $timelineList | sed 's; ;\n;g'
  exit $?
fi

# set input/output directories and dataset name
dataset=$($inputCmd $inputCmdOpts -D)
inputDir=$($inputCmd $inputCmdOpts -I)
[ -z "$outputDir" ] && outputDir=$(pwd -P)/outfiles/$dataset

# set subdirectories
finalDirPreQA=$outputDir/timeline_web_preQA
finalDir=$outputDir/timeline_web
logDir=$outputDir/log

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
    rm -rv $finalDirPreQA
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
  for timelineObj in $timelineList; do
    logFile=$logDir/$timelineObj
    [ -n "$singleTimeline" -a "$timelineObj" != "$singleTimeline" ] && continue
    echo ">>> producing timeline '$timelineObj' ..."
    if ${modes['debug']}; then
      java $TIMELINE_JAVA_OPTS $MAIN $timelineObj $inputDir
      echo "PREMATURE EXIT, since --debug option was used"
      exit
    else
      java $TIMELINE_JAVA_OPTS $MAIN $timelineObj $inputDir > $logFile.out 2> $logFile.err || touch $logFile.fail &
    fi
    if [ $jobCnt -lt $numThreads ]; then
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

  # remove any empty directories
  echo ">>> removing any empty directories..."
  for detDir in ${detDirs[@]}; do
    [ -z "$(find $detDir -name '*.hipo')" ] && rm -rv $detDir
  done

  echo ">>> done producing timelines..."
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
