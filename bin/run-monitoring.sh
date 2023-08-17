#!/bin/bash


# constants ############################################################
# dependencies
BINDIR="`dirname $0`"
MAINDIR=$BINDIR/..
JARPATH="$(realpath $BINDIR/..)/monitoring/target/clas12-monitoring-v0.0-alpha.jar"
# executable
EXE=org.jlab.clas12.monitoring.ana_2p2
MAX_NUM_EVENTS=100000000
# slurm settings
SLURM_MEMORY=1500
SLURM_TIME=4:00:00
SLURM_LOG=/farm_out/%u/%x-%A_%a
########################################################################

source $BINDIR/environ.sh

# default options
ver=test
declare -A modes
for key in findhipo rundir single series submit check-cache focus-detectors focus-physics; do
  modes[$key]=false
done

# usage
sep="================================================================"
if [ $# -lt 1 ]; then
  echo """
  $sep
  USAGE: $0  [OPTIONS]...  [RUN_DIRECTORY]...
  $sep
  Runs the monitoring jobs, either on the farm or locally

  REQUIRED ARGUMENTS:

    [RUN_DIRECTORY]...   One or more directories, each directory corresponds to
                         one run and should contain reconstructed hipo files
                         - See \"INPUT FINDING OPTIONS\" below for more control,
                           so that you don't have to specify each run's directory
                         - A regexp or globbing (wildcards) can be used to
                           specify the list of directories as well, if your shell
                           supports it

  $sep

  OPTIONS:

     -v [VERSION_NAME]      version name, defined by the user, used for
                            slurm jobs identification
                            default = $ver

     *** INPUT FINDING OPTIONS: choose only one, or the default will assume each specified
         [RUN_DIRECTORY] is a single run's directory full of HIPO files

       --findhipo  use \`find\` to find all HIPO files in each
                   [RUN_DIRECTORY]; this is useful if you have a
                   directory tree, e.g., runs grouped by target

       --rundir    assume each specified [RUN_DIRECTORY] contains
                   subdirectories named as just run numbers; it is not
                   recommended to use wildcards for this option

     *** EXECUTION CONTROL OPTIONS: choose only one, or the default will generate a
         Slurm job description and print out the suggested \`sbatch\` command

       --single    run only the first job, locally; useful for
                   testing before submitting jobs to slurm

       --series    run all jobs locally, one at a time; useful
                   for testing on systems without slurm

       --submit    submit the slurm jobs, rather than just
                   printing the \`sbatch\` command

       --check-cache   cross check /cache directories with tape stub
                       directories (/mss); does not create/run any jobs

     *** FOCUS OPTIONS: these options allow for running single types of jobs,
         rather than the default of running everything; you may specify more
         than one

       --focus-detectors   run monitoring for detector (and QA) timelines

       --focus-physics     run monitoring for physics QA timelines

  $sep

  EXAMPLES:

  $  $0 -v v1.0.0 --submit --rundir /volatile/mon
       -> submit slurm jobs for all numerical subdirectories of /volatile/mon/,
          where each subdirectory should be a run number; this is the most common usage

  $  $0 -v v1.0.0 /volatile/mon/*
       -> generate the slurm script to run on all subdirectories of
          /volatile/mon/ no matter their name

  $  $0 -v v1.0.0 --single /volatile/mon/run*
       -> run on the first directory named run[RUNNUM], where [RUNNUM] is a run number

  """ >&2
  exit 101
fi

# parse options
while getopts "v:-:" opt; do
  case $opt in
    v) ver=$OPTARG;;
    -)
      for key in "${!modes[@]}"; do
        [ "$key" == "$OPTARG" ] && modes[$OPTARG]=true && break
      done
      [ -z "${modes[$OPTARG]}" ] && printError "unknown option --$OPTARG" && exit 100
      ;;
    *) exit 100;;
  esac
done
shift $((OPTIND - 1))


# parse input directories
rdirs=()
[ $# == 0 ] && printError "no run directories specified" && exit 100
if ${modes['findhipo']}; then
  for topdir in $*; do
    fileList=$(find -L $topdir -type f -name "*.hipo")
    if [ -z "$fileList" ]; then
      printWarning "run directory '$topdir' has no HIPO files"
    else
      rdirs+=($(echo $fileList | xargs dirname | sort -u))
    fi
  done
elif ${modes['rundir']}; then
  for topdir in $*; do
    if [ -d $topdir -o -L $topdir ]; then
      for subdir in $(ls $topdir | grep -E "[0-9]+"); do
        rdirs+=($(echo "$topdir/$subdir " | sed 's;//;/;g'))
      done
    else
      printError "run directory '$topdir' does not exist"
      exit 100
    fi
  done
else
  rdirs=$@
fi
for rdir in ${rdirs[@]}; do
  [[ "$rdir" =~ ^- ]] && printError "option '$rdir' must be specified before run directories" && exit 100
done


# check focus options
modes['focus-all']=true
for key in focus-detectors focus-physics; do
  if ${modes[$key]}; then modes['focus-all']=false; fi
done

# print arguments
echo """
Settings:
$sep
VERSION_NAME = $ver
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
  $BINDIR/check-cache.sh ${rdirs[@]}
  exit $?
fi

# initial checks and preparations
[[ ! -f $JARPATH ]] && printError "Problem with jar file for clas12_monitoring package" && echo && exit 100
echo $ver | grep -q "/" && printError "version name must not contain '/' " && echo && exit 100
slurmJobName=clas12-timeline--$ver

# start job lists
echo """
Generating job scripts..."""
mkdir -p $MAINDIR/slurm/scripts
jobkeys=()
for key in detectors physics; do
  if ${modes['focus-all']} || ${modes['focus-'$key]}; then
    jobkeys+=($key)
  fi
done
declare -A joblists
for key in ${jobkeys[@]}; do
  joblists[$key]=$MAINDIR/slurm/job.$ver.$key.list
  > ${joblists[$key]}
done

# make output directories and backup directories
unixtime=$(date +%s)
for key in ${jobkeys[@]}; do
  case $key in
    detectors)
      mkdir -p $MAINDIR/detectors/outplots
      ;;
    physics)
      mkdir -p $MAINDIR/qa-physics/outdat
      mkdir -p $MAINDIR/qa-physics/outmon
      ;;
  esac
  mkdir -p $MAINDIR/tmp/backup.$unixtime/$key
done

# loop over input directories, building the job lists
runnumMin=0
runnumMax=0
for rdir in ${rdirs[@]}; do

  # get the run number
  [[ ! -e $rdir ]] && printError "the folder $rdir does not exist" && continue
  runnum=`basename $rdir | grep -m1 -o -E "[0-9]+"`
  [[ -z "$runnum" ]] && printError "unknown run number for directory $rdir" && continue
  runnum=$((10#$runnum))
  [ $runnum -lt $runnumMin -o $runnumMin -eq 0 ] && runnumMin=$runnum
  [ $runnum -gt $runnumMax -o $runnumMax -eq 0 ] && runnumMax=$runnum

  # get the beam energy
  # FIXME: use a config file or RCDB; this violates DRY with qa-physics/monitorRead.groovy
  beam_energy=`python -c """
beamlist = [
(3861,5673,10.6), (5674, 5870, 7.546), (5871, 6000, 6.535), (6608, 6783, 10.199),
(11620, 11657, 2.182), (11658, 12283, 10.389), (12389, 12444, 2.182), (12445, 12951, 10.389),
(15013,15490, 5.98636), (15533,15727, 2.07052), (15728,15784, 4.02962), (15787,15884, 5.98636),
(16010, 16079, 2.22), (16084, 1e9, 10.54) ]

ret=10.6
for r0,r1,eb in beamlist:
  if $runnum>=r0 and $runnum<=r1:
    ret=eb
    print(ret)
"""`
  if [ -z "$beam_energy" ]; then
    printError "Unknown beam energy for run $runnum"
    printWarning "Since this script is still undergoing testing, let's assume the beam energy is 10.6 GeV" # FIXME
    beam_energy=10.6
  fi

  # generate job scripts
  for key in ${jobkeys[@]}; do
    jobscript=$MAINDIR/slurm/scripts/$key.$runnum.sh

    case $key in

      detectors)
        # preparation
        plotDir=$MAINDIR/detectors/outplots/plots$runnum
        [[ -d $plotDir ]] && mv -v $plotDir $MAINDIR/tmp/backup.$unixtime/detectors/
        realpath $rdir/* > $MAINDIR/detectors/outplots/$runnum.input
        mkdir -p $plotDir
        # wrapper script
        cat > $jobscript << EOF
#!/bin/bash
echo "RUN $runnum"
pushd $MAINDIR/detectors/outplots
java -DCLAS12DIR=${COATJAVA}/ -Xmx1024m -cp ${COATJAVA}/lib/clas/*:${COATJAVA}/lib/utils/*:$JARPATH $EXE $runnum $runnum.input $MAX_NUM_EVENTS $beam_energy
popd
EOF
        ;;

      physics)
        # preparation: backup old files
        for backupFile in $MAINDIR/qa-physics/outdat/data_table_${runnum}.dat $MAINDIR/qa-physics/outmon/monitor_${runnum}.hipo; do
          [ -f $backupFile ] && mv -v $backupFile $MAINDIR/tmp/backup.$unixtime/physics/
        done
        # wrapper script
        cat > $jobscript << EOF
#!/bin/bash
echo "RUN $runnum"
pushd $MAINDIR/qa-physics
run-groovy -Djava.awt.headless=true monitorRead.groovy $(realpath $rdir) dst
popd
EOF
        ;;

    esac
    chmod u+x $jobscript
    echo $jobscript >> ${joblists[$key]}
  done

done


# prepare qa-physics/datasetList.txt
for key in ${jobkeys[@]}; do
  if [ "$key" == "physics" ]; then
    echo "$ver $runnumMin $runnumMax" >> $MAINDIR/qa-physics/datasetList.txt
  fi
done


# now generate slurm descriptions and/or local scripts
echo """
Generating batch scripts..."""
exelist=()
for key in ${jobkeys[@]}; do

  # check if we have any jobs to run
  joblist=${joblists[$key]}
  [ ! -s $joblist ] && printWarning "there are no $key timeline jobs to run" && continue
  slurm=$(echo $joblist | sed 's;.list$;.slurm;')

  # either generate single/sequential run scripts
  if ${modes['single']} || ${modes['series']}; then
    singleScript=$(echo $joblist | sed 's;.list$;.local.sh;')
    echo "#!/bin/bash" > $singleScript
    echo "set -e" >> $singleScript
    if ${modes['single']}; then
      head -n1 $joblist >> $singleScript
    else
      cat $joblist >> $singleScript
    fi
    chmod u+x $singleScript
    exelist+=($singleScript)

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

source /group/clas12/packages/setup.sh
module load clas12/pro
module switch clas12/pro

srun \$(head -n\$SLURM_ARRAY_TASK_ID $joblist | tail -n1)
EOF
    exelist+=($slurm)
  fi
done


# execution
echo """
$sep
"""
if ${modes['single']} || ${modes['series']}; then
  if ${modes['single']}; then
    echo "RUNNING ONE SINGLE JOB LOCALLY:"
  else
    echo "RUNNING ALL JOBS SEQUENTIALLY, LOCALLY:"
  fi
  echo $sep
  for exe in ${exelist[@]}; do $exe; done
elif ${modes['submit']}; then
  echo "SUBMITTING JOBS TO SLURM"
  echo sep
  for exe in ${exelist[@]}; do sbatch $exe; done
  echo sep
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
