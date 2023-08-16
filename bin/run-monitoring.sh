#!/usr/bin/bash


# constants ############################################################
# dependencies
BINDIR="`dirname $0`"
MONDIR="`realpath $BINDIR/..`"
JARPATH="$MONDIR/monitoring/target/clas12-monitoring-v0.0-alpha.jar"
# executable
EXE=org.jlab.clas12.monitoring.ana_2p2
MAX_NUM_EVENTS=100000000
# slurm settings
SLURM_MEMORY=1500
SLURM_TIME=4:00:00
########################################################################

# default options
ver=test
outputDir=plots
declare -A modes
availableModes=(
  findhipo
  rundir
  single
  series
  submit
)
for key in ${availableModes[@]}; do modes[$key]=false; done

# usage
if [ $# -lt 1 ]; then
  echo """
  USAGE: $0  [OPTIONS]...  [RUN_DIRECTORY]...
         runs the monitoring jobs, either on the farm or locally

  REQUIRED ARGUMENTS:

    [RUN_DIRECTORY]...   One or more directories, each directory corresponds to
                         one run and should contain reconstructed hipo files
                         - See \"INPUT FINDING OPTIONS\" below for more control,
                           so that you don't have to specify each run's directory
                         - A regexp or globbing (wildcards) can be used to
                           specify the list of directories as well, if your shell
                           supports it

  OPTIONS:

     -v [VERSION_NAME]      version name, defined by the user, used for
                            slurm jobs identification
                            default = $ver

     -o [OUTPUT_DIRECTORY]  output directory
                            default = $outputDir

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
while getopts "v:o:-:" opt; do
  case $opt in
    v) ver=$OPTARG;;
    o) outputDir=$OPTARG;;
    -)
      for key in "${!modes[@]}"; do
        [ "$key" == "$OPTARG" ] && modes[$OPTARG]=true && break
      done
      [ -z "${modes[$OPTARG]}" ] && echo "ERROR: unknown option --$OPTARG" >&2 && exit 100
      ;;
    *) exit 100;;
  esac
done
shift $((OPTIND - 1))


# parse input directories
rdirs=()
[ $# == 0 ] && echo "ERROR: no run directories specified" >&2 && exit 100
if ${modes['findhipo']}; then
  for topdir in $*; do
    fileList=$(find -L $topdir -type f -name "*.hipo")
    if [ -z "$fileList" ]; then
      echo "WARNING: run directory '$topdir' has no HIPO files" >&2
    else
      rdirs+=($(echo $fileList | xargs dirname | sort | uniq))
    fi
  done
elif ${modes['rundir']}; then
  for topdir in $*; do
    if [ -d $topdir -o -L $topdir ]; then
      for subdir in $(ls $topdir | grep -E "[0-9]+"); do
        rdirs+=($(echo "$topdir/$subdir " | sed 's;//;/;g'))
      done
    else
      echo "ERROR: run directory '$topdir' does not exist" >&2
      exit 100
    fi
  done
else
  rdirs=$@
fi
for rdir in ${rdirs[@]}; do
  [[ "$rdir" =~ ^- ]] && echo "ERROR: option '$rdir' must be specified before run directories" >&2 && exit 100
done

# print arguments
echo """
Settings:
========================
VERSION_NAME     = $ver
OUTPUT_DIRECTORY = $outputDir
OPTIONS = {"""
for key in "${!modes[@]}"; do printf "%20s => %s,\n" $key ${modes[$key]}; done
echo """}
RUN_DIRECTORIES = ["""
for rdir in ${rdirs[@]}; do echo "  $rdir,"; done
echo """]
========================
"""


# test if one jar for clas12-monitoring package exists in the directory
[[ ! -f $JARPATH ]] && echo "---- [ERROR] Problem with jar file for clas12_monitoring package --------" >&2 && echo && exit 100

# test if there is a version name
echo $ver | grep -q "/" && echo "---- [ERROR] version name must not contain / -------" >&2 && echo && exit 100
slurmJobName=clas12-timeline-monitoring-$ver
echo "---- slurm job name will be: $slurmJobName"

# make output directories
mkdir -p log $outputDir slurm

# loop over input directories, building the job list
joblist=slurm/job.${ver}.list
> $joblist
for rdir in ${rdirs[@]}; do

  # check for existence of directory
  echo "---- READ directory $rdir"
  [[ ! -e $rdir ]] && echo "------ [ERROR] the folder $rdir does not exist" >&2 && continue

  # get the run number
  runnum=`basename $rdir | grep -m1 -o -E "[0-9]+"`
  [[ -z "$runnum" ]] && echo "------ [WARNING] unknown run number for directory $rdir" >&2 && continue
  runnum=$((10#$runnum))

  # if output directory exists, skip this run
  plotDir=$outputDir/plots$runnum
  [[ -d $plotDir ]] && echo "------ [WARNING] skipping run $runnum because the directory $plotDir already exists" >&2 && continue

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
  echo "------ beam energy = $beam_energy GeV"

  # prepare slurm script
  echo "------ generating $EXE command"
  cmd="""
echo \"RUN $runnum\" &&
realpath $rdir/* > $outputDir/$runnum.input &&
mkdir -p $plotDir &&
pushd $outputDir &&
java -DCLAS12DIR=\${COATJAVA}/ -Xmx1024m -cp \${COATJAVA}/lib/clas/*:\${COATJAVA}/lib/utils/*:$JARPATH $EXE $runnum $runnum.input $MAX_NUM_EVENTS $beam_energy;
popd"""
  echo $cmd >> $joblist

done


# check if we have any jobs to run
if [ ! -s $joblist ]; then
  echo "[ERROR] there are no jobs to run" >&2
  exit 100
fi


# write slurm script
slurm=slurm/job.${ver}.slurm
cat > $slurm << EOF
#!/bin/sh
#SBATCH --ntasks=1
#SBATCH --job-name=$slurmJobName
#SBATCH --output=log/%x-%A_%a.out
#SBATCH --error=log/%x-%A_%a.err
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

echo """
Generated:
  Slurm script: $slurm
  Job list:     $joblist
"""


# execute the job(s)
singleScript=$slurm.single.sh
if ${modes['single']} || ${modes['series']}; then
  echo "#!/bin/bash" > $singleScript
  echo "set -e" >> $singleScript
  if ${modes['single']}; then
    echo "RUNNING ONE SINGLE JOB LOCALLY:"
    echo "================================================"
    head -n1 $joblist >> $singleScript
  else
    echo "RUNNING ALL JOBS SEQUENTIALLY, LOCALLY:"
    echo "================================================"
    cat $joblist >> $singleScript
  fi
  chmod u+x $singleScript
  $singleScript
  exit $?
elif ${modes['submit']}; then
  sbatch $slurm
  echo "JOBS SUBMITTED!"
else
  echo """
  To submit all jobs to slurm, run:
  =========================
  sbatch $slurm
  =========================
  """
fi
