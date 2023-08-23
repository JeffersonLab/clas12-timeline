#!/bin/bash

source $(dirname $0)/environ.sh

# default options
dataset=test_v0
rungroup=a
inputDir=""
declare -A modes
for key in build focus-timelines focus-qa; do
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

    --build             cleanly-rebuild the timeline code, then run

    --focus-timelines   only produce the detector timelines, do not run detector QA code
    --focus-qa          only run the QA code (assumes you have detector timelines already)
  """ >&2
  exit 101
fi

# parse options
while getopts "d:r:i:-:" opt; do
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
outputDir=$TIMELINEDIR/outfiles/$dataset/detectors/timelines
finalDir=$TIMELINEDIR/outfiles/$dataset/timelines
logDir=$TIMELINEDIR/outfiles/$dataset/log

# cleanup output directories
backupDir=$TIMELINESRC/tmp/backup.$dataset.$(date +%s) # use unixtime for uniqueness
mkdir -p $backupDir
[ -d $outputDir ] && mv $outputDir $backupDir/detectors
if [ -d $finalDir ]; then
  mkdir -p $backupDir/timelines
  for d in $(ls -d $finalDir/*/ | grep -v '/physics_'); do
    mv $d $backupDir/timelines/
  done
fi

# make output directories
mkdir -p $outputDir $logDir $finalDir

# detector subdirectories
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
  particle_mass_ctof_and_ftof
  rf
  rich
  trigger
)

######################################
# produce detector timelines
######################################
if ${modes['focus-all']} || ${modes['focus-timelines']}; then

  # change working directory to output directory
  pushd $outputDir

  # make detector subdirectories
  for dir in ${detDirs[@]}; do
    mkdir -p $dir
  done

  # get main executable
  # FIXME: remove run group dependence
  MAIN="org.jlab.clas.timeline.run"
  if [[ "$rungroup" == "b" ]]; then
    MAIN="org.jlab.clas.timeline.run_rgb"
  fi
  export MAIN

  # run function
  run() {
    echo "processing $1"
    java -DCLAS12DIR="$COATJAVA/" $MAIN "$@" > $logDir/$1.out 2> $logDir/$1.err
  }
  export -f run
  #JAVA_OPTS="-Dsun.java2d.pmoffscreen=false -Xms1024m -Xmx12288m"; export JAVA_OPTS

  # execution
  java $MAIN --timelines |
    xargs -I{} -n1 --max-procs 4 bash -c 'run "$@"' -- {} $inputDir

  # organize outputs
  for dir in ${detDirs[@]}; do
    case $dir in
      bmtbst)
        mv bmt_*.hipo $dir/
        mv bst_*.hipo $dir/
        ;;
      central)
        mv cen_*.hipo $dir/
        ;;
      ft)
        mv ftc_*.hipo $dir/
        mv fth_*.hipo $dir/
        ;;
      rf)
        mv rftime_*.hipo $dir/
        ;;
      trigger)
        mv rat_*.hipo $dir/
        ;;
      particle_mass_ctof_and_ftof)
        mv ctof/*m2*.hipo $dir/
        mv ftof/*m2*.hipo $dir/
        ;;
      *)
        mv ${dir}_*.hipo $dir/
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
cp -rL $outputDir/* $finalDir/

if ${modes['focus-all']} || ${modes['focus-qa']}; then
  run-groovy $TIMELINESRC/qa-detectors/util/applyBounds.groovy $outputDir $finalDir > $logDir/qa.out 2> $logDir/qa.err
fi


######################################
# finalize
######################################
echo "ERROR LOGS:"
echo "---------------------------------------------------------------------------------------------------------------------"
grep "error:" $logDir/*.err
echo "---------------------------------------------------------------------------------------------------------------------"

echo "The possible technical errors can be inspected through the log files in $logDir."
