#!/bin/bash

set -e

if [ -z "$CLASQA" ]; then
  echo "ERROR: please source env.sh first"
  exit
fi

if [ $# -lt 1 ]; then
  echo "USAGE: $0 [dataset]"
  echo "optional: if you specify a second argument, it will use files from tape"
  echo "          (warning: this feature has never been tested, and needs development!)"
  exit
fi
dataset=$1
usetape=0
if [ $# -eq 2 ]; then usetape=1; fi
echo "dataset=$dataset"
echo "usetape=$usetape"

source datasetListParser.sh $dataset

if [ $usetape -eq 1 ]; then
  datadir=$(echo $datadir | sed 's/^\/cache/\/mss/g')
fi

# make directories
mkdir -p outdat/trash
mkdir -p outmon/trash
mkdir -p slurm

# build list of files, and cleanup outdat and outmon directories, moving
# old files to the `trash` subdirectory
joblist=slurm/joblist.${dataset}.slurm
> $joblist
for rundir in `ls -d ${datadir}/*/ | sed 's/\/$//'`; do
  run=$(echo $rundir | sed 's/^.*\/0*//g')
  if [ $run -ge $runL -a $run -le $runH ]; then
    echo "--- found dir=$rundir  run=$run"
    if [ $usetape -eq 1 ]; then
      scratchdir="/scratch/slurm/$(whoami)/${run}"
      cmd="mkdir -P $scratchdir && jget ${rundir}/* ${scratchdir}/"
      cmd="$cmd && run-groovy $CLASQA_JAVA_OPTS monitorRead.groovy $scratchdir dst"
      cmd="$cmd && rm -r $scratchdir"
    else
      cmd="run-groovy $CLASQA_JAVA_OPTS monitorRead.groovy $rundir dst"
    fi
    echo "$cmd" >> $joblist
    for runfile in outdat/data_table_${run}.dat outmon/monitor_${run}.hipo; do
      [ -f $runfile ] && mv -v $runfile $(echo $runfile | sed 's;^out...;&/trash;;')
    done
  fi
done


# write job descriptor
slurm=slurm/job.${dataset}.slurm
> $slurm

function app { echo "$1" >> $slurm; }

app "#!/bin/bash"

app "#SBATCH --job-name=clasqa"
app "#SBATCH --account=clas12"
app "#SBATCH --partition=production"

app "#SBATCH --mem-per-cpu=2000"
app "#SBATCH --time=18:00:00"

app "#SBATCH --array=1-$(cat $joblist | wc -l)"
app "#SBATCH --ntasks=1"

app "#SBATCH --output=/farm_out/%u/%x-%j-%N.out"
app "#SBATCH --error=/farm_out/%u/%x-%j-%N.err"

app "srun \$(head -n\$SLURM_ARRAY_TASK_ID $joblist | tail -n1)"


# launch jobs
printf '%70s\n' | tr ' ' -
echo "JOB LIST: $joblist"
cat $joblist
printf '%70s\n' | tr ' ' -
echo "JOB DESCRIPTOR: $slurm"
cat $slurm
printf '%70s\n' | tr ' ' -
echo "submitting to slurm..."
sbatch $slurm
squeue -u `whoami`
