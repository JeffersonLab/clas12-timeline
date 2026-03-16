#!/usr/bin/env bash
set -euo pipefail
offset=0
dry=false
NUM_THREADS=4 # number of parallel jobs
usage() {
  echo """
  Run jobs from a slurm submission script's job list on
  the current interactive node; be nice and keep NUM_JOBS
  low, since this is only meant for rapid testing

  USAGE: $0 [JOB_LIST] [LOG_DIR] [NUM_JOBS] [OFFSET] [DRY]
    JOB_LIST  file with job scripts, one per line
    LOG_DIR   directory for log files (clobbered)
    NUM_JOBS  the number of parallel jobs to run
    OFFSET    offset of the first job; NUM_JOBS
              consecutive lines will be used
              default: $offset
    DRY       set to '1' for a dry-run (no submission)
              default: run immediately
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
[ $# -ge 5 ] && dry=true

if [ ! -f "$job_list" ]; then
  echo "ERROR: File '$job_list' not found"
  exit 1
fi

mkdir -p $log_dir

job_ids=()

function cleanup_jobs() {
  echo ""
  echo ">>> Caught signal, killing all jobs..."
  for job_id in "${job_ids[@]}"; do
    if ps -p "$job_id" >& /dev/null; then
      # Kill the entire process group (negative PID)
      kill -- -"$job_id" 2>/dev/null || true # SIGTERM
    fi
  done
  sleep 1
  # SIGKILL, if still alive
  for job_id in "${job_ids[@]}"; do
    if ps -p "$job_id" >& /dev/null; then
      kill -9 -- -"$job_id" 2>/dev/null || true
    fi
  done
  echo """>>> All jobs killed.
  To check if any remain:
    ps -ef | grep $(whoami)
  Kill zombies with, e.g.,:
    pkill -u $(whoami) java
  """
  exit 1
}
trap cleanup_jobs SIGINT SIGTERM

function wait_for_jobs() {
  stat=10
  while [ "${#job_ids[@]}" -gt $1 ]; do
    for i in "${!job_ids[@]}"; do
      if [ "$1" -eq 0 ]; then
        if [ "${#job_ids[@]}" -lt $stat ]; then
          echo ">>> $(date) >>> waiting on ${#job_ids[@]} jobs"
          stat=${#job_ids[@]}
        fi
      fi
      set +e
      ps ${job_ids[$i]} >& /dev/null
      if [ "$?" -ne 0 ]; then
        echo ">>> jobid ${job_ids[$i]} finished."
        unset job_ids[$i]
      fi
      set -e
    done
    sleep 1
  done
}

j=0
echo "===== JOBS: ====="
while IFS= read -r cmd; do
  j=$((j+1))
  echo "JOB $j: $cmd"
  if ! $dry; then
    setsid $cmd > $log_dir/job.$j.out 2> $log_dir/job.$j.err &
    job_ids+=($!)
    wait_for_jobs $((NUM_THREADS-1))
  fi
done < <(tail -n +$((offset + 1)) $job_list | head -n $num_jobs)
wait_for_jobs 0

echo "================="
if $dry; then
  echo "THIS WAS A DRY-RUN; no jobs submitted"
else
  echo "DONE!"
fi
