#!/usr/bin/bash

#test if one jar for clas12-monitoring package exists in the directory
BINDIR="`dirname $0`"
MONDIR="`realpath $BINDIR/..`"
JARPATH="$MONDIR/monitoring/target/clas12-monitoring-v0.0-alpha.jar"

[[ ! -f $JARPATH ]] && echo "---- [ERROR] Problem with jar file for clas12_monitoring package --------" >&2 && echo && exit 100

# test if there is a version name
echo $1 | grep -q "/" && echo "---- [ERROR] version name must not contain / -------" >&2 && echo && exit 100

ver=$1
shift
echo "---- slurm job name will be: $ver"

mkdir -p log plots

for rdir
do

[[ ! -e $rdir ]] && echo "------ [ERROR] the folder $rdir does not exist" && continue

run=`basename $rdir | grep -m1 -o "[0-9][0-9][0-9][0-9][0-9][0-9]"`
[[ -z $run ]] && continue
irun=$((10#$run))

[[ -e plots/plots${irun} ]] && echo "------ [WARNING] skipping run ${irun} because the directory plots/plots${irun} already exists" && continue

echo "Submitting the job for run $run"

mkdir plots/plots${irun}

max_num_events=100000000

beam_energy=`python -c "beamlist = [
(3861,5673,10.6), (5674, 5870, 7.546), (5871, 6000, 6.535), (6608, 6783, 10.199),
(11620, 11657, 2.182), (11658, 12283, 10.389), (12389, 12444, 2.182), (12445, 12951, 10.389),
(15013,15490, 5.98636), (15533,15727, 2.07052), (15728,15784, 4.02962), (15787,15884, 5.98636),
(16010, 16079, 2.22), (16084, 1e9, 10.54) ]

ret=10.6
for r0,r1,eb in beamlist:
  if $irun>=r0 and $irun<=r1:
    ret=eb
print(ret)
"`

sbatch << END_OF_SBATCH
#!/bin/sh
#SBATCH --ntasks=1
#SBATCH --job-name=$ver
#SBATCH --output=log/%x-run-$run-%j-%N.out
#SBATCH --error=log/%x-run-$run-%j-%N.err
#SBATCH --partition=production
#SBATCH --account=clas12
#SBATCH --mem-per-cpu=1500

source /group/clas12/packages/setup.sh
module load clas12/pro
module switch clas12/pro

realpath $rdir/* > plots/"$irun".input

cd plots

echo "[shell] >> MAKING DIRECTORY FOR PLOTS FOR SINGLE RUN: $irun with beam energy: $beam_energy"
mkdir plots${irun}/

java -DCLAS12DIR="\${COATJAVA}/" -Xmx1024m -cp "\${COATJAVA}/lib/clas/*:\${COATJAVA}/lib/utils/*:$JARPATH" org.jlab.clas12.monitoring.ana_2p2 $irun "$irun".input $max_num_events $beam_energy

END_OF_SBATCH

done
