#!/usr/bin/env bash
set -euo pipefail
offset=0
usage() {
  echo """
  Run jobs from a slurm submission script's job list on
  the current interactive node; be nice and keep NUM_JOBS
  low, since this is only meant for rapid testing

  USAGE: $0 [JOB_LIST] [LOG_DIR] [NUM_JOBS] [OFFSET]
    JOB_LIST  file with job scripts, one per line
    LOG_DIR   directory for log files (clobbered)
    NUM_JOBS  the number of parallel jobs to run
    OFFSET    offset of the first job; NUM_JOBS
              consecutive lines will be used
              default: $offset
    """
}

if [ $# -lt 3 ]; then
  usage
  exit 2
fi
job_list=$1
log_dir=$2
num_jobs=$3
[ $# -ge 4 ] && offset=$4

if [ ! -f "$job_list" ]; then
  echo "ERROR: File '$job_list' not found"
  exit 1
fi

if [ $num_jobs -gt 16 ]; then
  echo "ERROR: too many jobs!"
  exit 1
fi

mkdir -p $log_dir

i=0
echo "SUBMITTING:"
tail -n +$((offset + 1)) $job_list | head -n $num_jobs | while IFS= read -r cmd; do
  echo "JOB $i: $cmd"
  # $cmd > $log_dir/job.$i.out 2> $log_dir/job.$i.err &
  i=$((i+1))
done
echo """
JOBS SUBMITTED.
- They are running in the backround
- Monitor with \`htop -u $(whoami)\`
- Logs written to \`$log_dir\`
"""
