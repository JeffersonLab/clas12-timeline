#!/usr/bin/env bash
# test using a working directory similar to what a SWIF runner will use, for testing --swifjob

set -e
set -u
source $(dirname $0)/../libexec/environ.sh

# arguments
if [ $# -lt 2 ]; then
  echo """
  USAGE: $0 [INPUT_RUN_DIRECTORY] [RUNNER_DIRECTORY] [OPTIONS]...
         where [OPTIONS] are forwared to run-monitoring.sh
  """ >&2
  exit 101
fi
inputDir=$(realpath $1)
runnerDir=$2
shift
shift
cmd="$TIMELINESRC/bin/run-monitoring.sh --swifjob $@"

# symlink inputs to runner directory
[ ! -d $inputDir ] && printError "input directory $inputDir does not exist" && exit 100
mkdir -pv $runnerDir
find $inputDir -name "*.hipo" | xargs -I{} ln -svf {} $runnerDir/

# recursive ls
ls_rec() {
  echo ""
  echo "LS: ------------------------------------------------"
  ls -Rl --color
  echo "----------------------------------------------------"
  echo ""
}

# execute
pushd $runnerDir
ls_rec
echo """
EXECUTING:
$cmd
"""
$cmd
ls_rec
popd
