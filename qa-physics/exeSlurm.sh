#!/bin/bash

if [ -z "$CLASQA" ]; then
  echo "ERROR: please source environ.sh first" >&2
  exit 100
fi

if [ $# -lt 1 ]; then
  echo "USAGE: $0 [dataset]" >&2
  exit 101
fi
dataset=$1
echo "dataset=$dataset"


# functions
# -----------------------------------------------------------------------

# convert cache directory <=> tape stub directory
cache2tape() {
  realpath $1 | sed 's;^.*/cache/;/mss/;'
}
tape2cache() {
  realpath $1 | sed 's;^.*/mss/;/cache/;'
}

# note an issue, to be printed altogether at the end to stderr
issueList=tmp/issueList.txt
printIssue() {
  echo """$1
""" >> $issueList
}

# compare cache directory with tape stub directory; if there are differences, note them in $issueList
jcacheList=tmp/jcacheSuggested.sh
checkIfCached() {
  cacheDir=$1
  tapeDir=$2
  if [ ! -d $cacheDir ]; then
    printIssue "WARNING: cache directory does not exist: $cacheDir"
  elif [ ! -d $tapeDir ]; then
    printIssue "WARNING: tape stub directory does not exist: $tapeDir"
  else
    cacheDirLS=$(ls $cacheDir | grep -vi readme)
    tapeDirLS=$(ls $tapeDir  | grep -vi readme)
    if [ -n "$(diff <(echo $cacheDirLS) <(echo $tapeDirLS))" ]; then
      printIssue """ERROR: some files are on tape but not on cache
        cache dir: $cacheDir
        tape dir:  $tapeDir
      consider running jcacheRun.sh on this cache directory
      or run $jcacheList to do so on all such directories"""
      echo "./jcacheRun.sh $cacheDir" >> $jcacheList
    fi
  fi
}

# get a list of run subdirectories
getRunDirs() {
  echo $(ls -d $1/*/ | sed 's;/$;;')
}


# preparation
# -----------------------------------------------------------------------

# get DATADIR and check its existence
source datasetListParser.sh $dataset
if [ ! -d "$DATADIR" ]; then
  echo "ERROR: DATADIR=$DATADIR does not exist" >&2
  exit 100
fi
if [ $(ls -d $DATADIR/*/ | wc -l) -eq 0 ]; then
  echo "ERROR: DATADIR=$DATADIR has no subdirectories, and likely no data" >&2
  exit 100
fi

# make directories and start $issueList
mkdir -p outdat/trash
mkdir -p outmon/trash
mkdir -p slurm
mkdir -p tmp
> $issueList
echo "#!/bin/bash" > $jcacheList

# check if DATADIR is a /cache subdirectory; if so, we will loop through the
# corresponding tape stub directory, cross checking with the /cache directory
isCacheDir=0
if [[ "$DATADIR" =~ "/cache/" ]]; then
  isCacheDir=1
  dataDirCache=$DATADIR
  dataDirTape=$(cache2tape $DATADIR)

  # check for existence of directories
  skipCheck=0
  [ ! -d $dataDirCache ] && printIssue "WARNING: cache directory does not exist: $dataDirCache"    && skipCheck=1
  [ ! -d $dataDirTape ]  && printIssue "WARNING: tape stub directory does not exist: $dataDirTape" && skipCheck=1
  if [ $skipCheck -eq 1 ]; then
    printIssue """WARNING: since cache or tape directory does not exist, it is not possible to make sure
         that all files on tape also exist on cache"""
    echo "Skipping cache check, since problems were detected (see errors and warnings below)..."
  else

    # diff the cache and tape stub top-level directories, to see if one has runs that the other does not
    ls $dataDirCache | grep -vi readme > tmp/cacheList
    ls $dataDirTape | grep -vi readme > tmp/tapeList
    diff -y --suppress-common-lines tmp/{cache,tape}List > tmp/diffList
    if [ -s tmp/diffList ]; then
      printIssue """WARNING: there are differences between the list of runs in the cache and tape directories.
        cache dir: $dataDirCache
        tape dir:  $dataDirTape
      This means that one may have run subdirectories that the other does not.
      Here is a summary of their differences:

     cache_dir                                                    tape_dir
===============                                              ==============
$(cat tmp/diffList)

      """
    fi

    # then diff each run subdirectory
    for runDir in `getRunDirs $dataDirTape`; do
      echo "Checking if cached: $runDir"
      checkIfCached $(tape2cache $runDir) $runDir
    done
  fi

fi


# build job commands, only for the runs in specified range
# -----------------------------------------------------------------------
joblist=slurm/joblist.${dataset}.slurm
> $joblist
for runDir in `getRunDirs $DATADIR`; do

  # get the run number, and check if it's in range
  runnum=$((10#$(echo $runDir | sed 's;.*/;;g')))
  printf "run $runnum: "
  if [ $runnum -ge $RUNL -a $runnum -le $RUNH ]; then
    echo "--- found"

    # append the command to the joblist
    echo "run-groovy $CLASQA_JAVA_OPTS monitorRead.groovy $runDir dst" >> $joblist

    # move old output files to a trash directory
    for runfile in outdat/data_table_${runnum}.dat outmon/monitor_${runnum}.hipo; do
      [ -f $runfile ] && mv -v $runfile $(echo $runfile | sed 's;^out...;&/trash;;')
    done

  else
    echo "--- WARNING: run $runnum is not within run range ($RUNL to $RUNH)" >&2
  fi
done


# write job descriptor
# -----------------------------------------------------------------------
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

app "#SBATCH --output=/farm_out/%u/%x-%A_%a.out"
app "#SBATCH --error=/farm_out/%u/%x-%A_%a.err"

app "srun \$(head -n\$SLURM_ARRAY_TASK_ID $joblist | tail -n1)"


# printout status and how to launch jobs
# -----------------------------------------------------------------------
sep() { printf '%70s\n' | tr ' ' -; }
sep
echo "JOB LIST: $joblist"
cat $joblist
sep
echo "JOB DESCRIPTOR: $slurm"
cat $slurm
sep
if [ $isCacheDir -eq 1 ]; then
  echo """
=====================
LIST OF CACHE ISSUES:
=====================

$(cat $issueList)
"""
  sep
fi
echo """
JOB SUBMISSION COMMAND (run this to submit the jobs):

  ===================================
  sbatch $slurm
  ===================================
"""
if [ $isCacheDir -eq 1 ]; then
  if [ -s $issueList ]; then
    echo """
    WARNING: some of the comparisons between cache and tape failed;
             check the LIST OF CACHE ISSUES above to decide whether or not
             you are ready to submit jobs
    """ >&2
    chmod u+x $jcacheList
  else
    echo """
    SUCCESS: all files that are on tape are also on cache;
             you are ready to run!
    """
  fi
fi
