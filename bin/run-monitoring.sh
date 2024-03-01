#!/bin/bash

set -e
set -u
source $(dirname $0)/environ.sh

# constants ############################################################
# max number of events for detector monitoring timelines
MAX_NUM_EVENTS=100000000
# slurm settings
SLURM_MEMORY=1500
SLURM_TIME=10:00:00
SLURM_LOG=/farm_out/%u/%x-%A_%a
########################################################################


# default options
dataset=test_v0
declare -A modes
for key in findhipo rundir eachdir single series submit check-cache swifjob focus-detectors focus-physics help; do
  modes[$key]=false
done
outputDir=""
modes['rundir']=true

# usage
sep="================================================================"
usageTerse() {
  echo """
  Run the timeline monitoring jobs
  $sep
  USAGE: $0 -d [DATASET_NAME] [RUN_DIRECTORY]
  $sep

     -d [DATASET_NAME]   unique dataset name, defined by the user, used for organization

     [RUN_DIRECTORY]     input data directory, with subdirectories organized by run,
                         **must be specified last**

  After running this script, run either or both of the suggested 'sbatch' command(s);
  one is for detector QA timelines and the other is for physics QA timelines.
  Output files will appear in ./outfiles/[DATASET_NAME]

  For more options, run:
    $0 --help
  """ >&2
}
usageVerbose() {
  echo """
  $sep
  USAGE: $0  [OPTIONS]...  [RUN_DIRECTORY]...
  $sep

  REQUIRED ARGUMENTS:

    [RUN_DIRECTORY]...   One or more directories, each directory corresponds to
                         one run and should contain reconstructed HIPO files
                         - See \"INPUT FINDING OPTIONS\" below for more control,
                           so that you don't have to specify each run's directory
                         - A regexp or globbing (wildcards) can be used to
                           specify the list of directories as well, if your shell
                           supports it

  $sep

  OPTIONS:

     -d [DATASET_NAME]      unique dataset name, defined by the user, used for organization
                            default = '$dataset'

     -o [OUTPUT_DIR]        custom output directory
                            default = ./outfiles/[DATASET_NAME]

     *** INPUT FINDING OPTIONS: control how the input HIPO files are found;
         choose only one:

       --findhipo     use \`find\` to find all HIPO files in each
                      [RUN_DIRECTORY]; this is useful if you have a
                      directory tree, e.g., runs grouped by target

       --rundir       assume each specified [RUN_DIRECTORY] contains
                      subdirectories named as just run numbers; it is not
                      recommended to use wildcards for this option
                      **this is the default option**

       --eachdir      assume each specified [RUN_DIRECTORY] is a single
                      run's directory full of HIPO files

       --check-cache  cross check /cache directories with tape stub directories
                      (/mss) and exit without creating or running any jobs; this is
                      useful if you are running QA on older DSTs which may no longer be
                      fully pinned on /cache

     *** EXECUTION CONTROL OPTIONS: choose only one, or the default will generate a
         Slurm job description and print out the suggested \`sbatch\` command

       --single    run only the first job, locally; useful for
                   testing before submitting jobs to slurm

       --series    run all jobs locally, one at a time; useful
                   for testing on systems without slurm

       --submit    submit the slurm jobs, rather than just
                   printing the \`sbatch\` command

       --swifjob   run this on a workflow runner, where the input
                   HIPO files are found in ./ and specifying [RUN_DIRECTORIES] is
                   not required; overrides some other settings; this is NOT meant
                   to be used interactively, but rather as a part of a workflow

     *** FOCUS OPTIONS: these options allow for running single types of jobs,
         rather than the default of running everything; you may specify more
         than one

       --focus-detectors   run monitoring for detector QA timelines

       --focus-physics     run monitoring for physics QA timelines

  $sep

  EXAMPLES:

  $  $0 -v v1.0.0 --submit --rundir /volatile/mon
       -> submit slurm jobs for all numerical subdirectories of /volatile/mon/,
          where each subdirectory should be a run number; this is the most common usage

  $  $0 -v v1.0.0 --eachdir /volatile/mon/*
       -> generate the slurm script to run on all subdirectories of
          /volatile/mon/ no matter their name

  $  $0 -v v1.0.0 --single /volatile/mon/run*
       -> run on the first directory named run[RUNNUM], where [RUNNUM] is a run number

  """ # stream to stdout to permit grepping
}
if [ $# -lt 1 ]; then
  usageTerse
  exit 101
fi

# parse options
while getopts "d:o:h-:" opt; do
  case $opt in
    d) dataset=$OPTARG;;
    o) outputDir=$OPTARG;;
    h) modes['help']=true;;
    -)
      for key in "${!modes[@]}"; do
        [ "$key" == "$OPTARG" ] && modes[$OPTARG]=true && break
      done
      [ -z "${modes[$OPTARG]-}" ] && printError "unknown option --$OPTARG" && exit 100
      ;;
    *) exit 100;;
  esac
done
shift $((OPTIND - 1))

# print full usage guide
if ${modes['help']}; then
  usageVerbose
  exit 101
fi

# parse input directories
rdirs=()
if ${modes['swifjob']}; then
  rdirs=(.) # all input files reside in ./ on a workflow runner
else
  [ $# == 0 ] && printError "no run directories specified" && exit 100
  rdirsArgs="$@"
  for topdir in ${rdirsArgs[@]}; do
    [[ "$topdir" =~ ^- ]] && printError "option '$topdir' must be specified before run directories" && exit 100
  done
  if ${modes['rundir']}; then
    for topdir in ${rdirsArgs[@]}; do
      if [ -d $topdir ]; then
        for subdir in $(ls $topdir | grep -E "[0-9]+"); do
          rdirs+=($(echo "$topdir/$subdir " | sed 's;//;/;g'))
        done
      else
        printError "run directory '$topdir' does not exist"
        exit 100
      fi
    done
  elif ${modes['eachdir']}; then
    rdirs=$@
  elif ${modes['findhipo']}; then # N.B.: the default option must be last
    for topdir in ${rdirsArgs[@]}; do
      echo "finding .hipo files in $topdir ....."
      fileList=$(find -L $topdir -type f -name "*.hipo")
      if [ -z "$fileList" ]; then
        printWarning "run directory '$topdir' has no HIPO files"
      else
        rdirs+=($(echo $fileList | xargs dirname | sort -u))
      fi
    done
  else
    printError "unknown input option"
    exit 100
  fi
fi
[ ${#rdirs[@]} -eq 0 ] && printError "no run directories found" && exit 100
echo "done finding input run directories"

# set and make output directory
if ${modes['swifjob']}; then
  outputDir=$(pwd -P)/outfiles
else
  [ -z "$outputDir" ] && outputDir=$(pwd -P)/outfiles/$dataset
fi
mkdir -p $outputDir

# check focus options
modes['focus-all']=true
for key in focus-detectors focus-physics; do
  if ${modes[$key]}; then modes['focus-all']=false; fi
done
if ${modes['swifjob']} && ${modes['focus-all']}; then
  printError "option --swifjob must be used with either --focus-detectors or --focus-physics"
  exit 100
fi

# print arguments
echo """
Settings:
$sep
DATASET_NAME = $dataset
OUTPUT_DIR   = $outputDir
OPTIONS = {"""
for key in "${!modes[@]}"; do printf "%20s => %s,\n" $key ${modes[$key]}; done
echo """}
RUN_DIRECTORIES = ["""
for rdir in ${rdirs[@]}; do echo "  $rdir,"; done
echo """]
$sep
"""

# check cache (and exit), if requested
if ${modes['check-cache']}; then
  echo "Cross-checking /cache and /mss..."
  $TIMELINESRC/bin/check-cache.sh ${rdirs[@]}
  exit $?
fi

# initial checks and preparations
echo $dataset | grep -q "/" && printError "dataset name must not contain '/' " && echo && exit 100
[ -z "$dataset" ] && printError "dataset name must not be empty" && echo && exit 100
slurmJobName=clas12-timeline--$dataset

# start job lists
echo """
Generating job scripts..."""
slurmDir=./slurm
mkdir -p $slurmDir/scripts
jobkeys=()
for key in detectors physics; do
  if ${modes['focus-all']} || ${modes['focus-'$key]}; then
    jobkeys+=($key)
  fi
done
declare -A joblists
for key in ${jobkeys[@]}; do
  joblists[$key]=$slurmDir/job.$dataset.$key.list
  > ${joblists[$key]}
done

# define backup directory (used only if the output files already exist; not used `if ${modes['swifjob']}`)
backupDir=$(pwd -P)/tmp/backup.$dataset.$(date +%s) # use unixtime for uniqueness

# loop over input directories, building the job lists
for rdir in ${rdirs[@]}; do

  # get the run number, either from `rdir` basename (fast), or from `RUN::config` (slow)
  [[ ! -e $rdir ]] && printError "the run directory '$rdir' does not exist" && continue
  runnum=$(basename $rdir | grep -m1 -o -E "[0-9]+" || echo '') # first, try from run directory basename
  if [ -z "$runnum" ] || ${modes['swifjob']}; then # otherwise, use RUN::config from a HIPO file (NOTE: assumes all HIPO files have the same run number)
    firstHipo=$(find $rdir -name "*.hipo" | head -n1)
    [ -z "$firstHipo" ] && printError "no HIPO files in run directory '$rdir'; cannot get run number or create job" && continue
    echo "using HIPO file $firstHipo to get run number for run directory '$rdir'"
    $TIMELINESRC/bin/hipo-check.sh $firstHipo
    runnum=$(run-groovy $TIMELINE_GROOVY_OPTS $TIMELINESRC/bin/get-run-number.groovy $firstHipo | tail -n1 | grep -m1 -o -E "[0-9]+" || echo '')
  fi
  [ -z "$runnum" -o $runnum -eq 0 ] && printError "unknown run number for run directory '$rdir'; ignoring this directory" && continue
  runnum=$((10#$runnum))
  echo "run directory '$rdir' has run number $runnum"

  # get list of input files, and append prefix for SWIF
  echo "..... getting its input files ....."
  inputListFile=$slurmDir/files.$dataset.$runnum.inputs.list
  [[ "$(realpath $rdir)" =~ /mss/ ]] && swifPrefix="mss:" || swifPrefix="file:"
  realpath $rdir/*.hipo | sed "s;^;$swifPrefix;" > $inputListFile

  # generate job scripts
  echo "..... generating its job scripts ....."
  for key in ${jobkeys[@]}; do

    # preparation: make output subdirectory and backup old one, if it exists
    outputSubDir=$outputDir/timeline_$key/$runnum
    if ${modes['swifjob']}; then
      outputSubDir=$outputDir # no need for run subdirectory or backup on swif runner
    else
      if [ -d $outputSubDir ]; then
        mkdir -p $backupDir/timeline_$key
        mv -v $outputSubDir $backupDir/timeline_$key/
      fi
    fi
    mkdir -p $outputSubDir

    # make job scripts for each $key
    jobscript=$slurmDir/scripts/$key.$runnum.sh
    case $key in

      detectors)

        # hard-coded beam energy values, which may be "more correct" than those in RCDB; if a run
        # is not found here, RCDB will be used instead
        # FIXME: use a config file; this violates DRY with qa-physics/monitorRead.groovy
        beam_energy=`python -c """
beamlist = [
  (3861,  5673,  10.6),
  (5674,  5870,  7.546),
  (5871,  6000,  6.535),
  (6608,  6783,  10.199),
  (11093, 11283, 10.4096),
  (11284, 11300, 4.17179),
  (11323, 11571, 10.3894),
  (11620, 11657, 2.182),
  (11658, 12283, 10.389),
  (12389, 12444, 2.182),
  (12445, 12951, 10.389),
  (15013, 15490, 5.98636),
  (15533, 15727, 2.07052),
  (15728, 15784, 4.02962),
  (15787, 15884, 5.98636),
  (16010, 16078, 2.21),
  (16079, 19130, 10.55),
]
for r0,r1,eb in beamlist:
  if $runnum>=r0 and $runnum<=r1:
    print(eb)
      """`
        if [ -z "$beam_energy" ]; then
          echo "Retrieving beam energy from RCDB..."
          beam_energy=$(run-groovy $TIMELINE_GROOVY_OPTS $TIMELINESRC/bin/get-beam-energy.groovy $runnum | tail -n1)
        fi
        if [ -z "$beam_energy" ]; then
          printError "Unknown beam energy for run $runnum"
          exit 100
        fi
        echo "Beam energy = $beam_energy"

        cat > $jobscript << EOF
#!/bin/bash
set -e
set -u
set -o pipefail
echo "RUN $runnum"

# set classpath
export CLASSPATH=$CLASSPATH

# produce histograms
java \\
  $TIMELINE_JAVA_OPTS \\
  org.jlab.clas12.monitoring.ana_2p2 \\
    $runnum \\
    $outputSubDir \\
    $inputListFile \\
    $MAX_NUM_EVENTS \\
    $beam_energy

# check output HIPO files
$TIMELINESRC/bin/hipo-check.sh \$(find $outputSubDir -name "*.hipo")
EOF
        ;;

      physics)
        cat > $jobscript << EOF
#!/bin/bash
set -e
set -u
set -o pipefail
echo "RUN $runnum"

# set classpath
export JYPATH=$JYPATH

# produce histograms
run-groovy \\
  $TIMELINE_GROOVY_OPTS \\
  $TIMELINESRC/qa-physics/monitorRead.groovy \\
    $(realpath $rdir) \\
    $outputSubDir \\
    dst \\
    $runnum

# check output HIPO files
$TIMELINESRC/bin/hipo-check.sh \$(find $outputSubDir -name "*.hipo")
EOF
        ;;

    esac

    # grant permission and add it `joblists`
    chmod u+x $jobscript
    echo $jobscript >> ${joblists[$key]}

  done # loop over `jobkeys`

done # loop over `rdirs`


# now generate slurm descriptions and/or local scripts
echo """
Generating batch scripts..."""
exelist=()
for key in ${jobkeys[@]}; do

  # check if we have any jobs to run
  joblist=${joblists[$key]}
  [ ! -s $joblist ] && printError "there are no $key timeline jobs to run" && continue
  slurm=$(echo $joblist | sed 's;.list$;.slurm;')

  # either generate single/sequential run scripts
  if ${modes['single']} || ${modes['series']} || ${modes['swifjob']}; then
    localScript=$(echo $joblist | sed 's;.list$;.local.sh;')
    echo "#!/bin/bash" > $localScript
    echo "set -e" >> $localScript
    if ${modes['single']}; then
      head -n1 $joblist >> $localScript
    else # ${modes['series']} || ${modes['swifjob']}
      cat $joblist >> $localScript
    fi
    chmod u+x $localScript
    exelist+=($localScript)

  # otherwise generate slurm description
  else
    cat > $slurm << EOF
#!/bin/sh
#SBATCH --ntasks=1
#SBATCH --job-name=$slurmJobName--$key
#SBATCH --output=$SLURM_LOG.out
#SBATCH --error=$SLURM_LOG.err
#SBATCH --partition=production
#SBATCH --account=clas12

#SBATCH --mem-per-cpu=$SLURM_MEMORY
#SBATCH --time=$SLURM_TIME

#SBATCH --array=1-$(cat $joblist | wc -l)
#SBATCH --ntasks=1

srun \$(head -n\$SLURM_ARRAY_TASK_ID $joblist | tail -n1)
EOF
    exelist+=($slurm)
  fi
done


# execution
[ ${#exelist[@]} -eq 0 ] && printError "no jobs were created at all; check errors and warnings above" && exit 100
echo """
$sep
"""
if ${modes['single']} || ${modes['series']} || ${modes['swifjob']}; then
  if ${modes['single']}; then
    echo "RUNNING ONE SINGLE JOB LOCALLY:"
  elif ${modes['series']}; then
    echo "RUNNING ALL JOBS SEQUENTIALLY, LOCALLY:"
  fi
  for exe in ${exelist[@]}; do
    echo """
    $sep
    EXECUTING: $exe
    $sep"""
    $exe
  done
elif ${modes['submit']}; then
  echo "SUBMITTING JOBS TO SLURM"
  echo $sep
  for exe in ${exelist[@]}; do sbatch $exe; done
  echo $sep
  echo "JOBS SUBMITTED!"
else
  echo """  SLURM JOB DESCRIPTIONS GENERATED
  - Slurm job name prefix will be: $slurmJobName
  - To submit all jobs to slurm, run:
    ------------------------------------------"""
  for exe in ${exelist[@]}; do echo "    sbatch $exe"; done
  echo """    ------------------------------------------
  """
fi
