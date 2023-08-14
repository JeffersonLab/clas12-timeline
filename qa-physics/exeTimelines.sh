#!/bin/bash

if [ -z "$CLASQA" ]; then source environ.sh; fi

if [ $# -ne 1 ];then echo "USAGE: $0 [dataset]" >&2; exit 101; fi
dataset=$1

# setup error filtered execution function
errlog="errors.log"
> $errlog
function sep { printf '%70s\n' | tr ' ' -; }
function exe { 
  sep
  echo "EXECUTE: $*"
  sep
  sep >> $errlog
  echo "$* errors:" >> $errlog
  $* 2>>$errlog
}

# organize the data into datasets
exe ./datasetOrganize.sh $dataset

# produce chargeTree.json
exe run-groovy $CLASQA_JAVA_OPTS buildChargeTree.groovy $dataset

# loop over datasets
# trigger electrons monitor
exe run-groovy $CLASQA_JAVA_OPTS qaPlot.groovy $dataset
exe run-groovy $CLASQA_JAVA_OPTS qaCut.groovy $dataset
# FT electrons
exe run-groovy $CLASQA_JAVA_OPTS qaPlot.groovy $dataset FT
exe run-groovy $CLASQA_JAVA_OPTS qaCut.groovy $dataset FT
# general monitor
exe run-groovy $CLASQA_JAVA_OPTS monitorPlot.groovy $dataset
# deploy timelines to dev www
exe ./deployTimelines.sh $dataset $dataset


# print errors (filtering out hipo logo contamination)
sep
echo "ERROR LOGS:"
grep -vE '█|═|Physics Division|^     $' $errlog
sep
rm $errlog

# print final message
echo """


TIMELINE GENERATION COMPLETE
==============================================================================
If this script ran well, open the TIMELINE URL (printed above) in your browser
and take a look at the timelines produced for this dataset ($dataset)
==============================================================================

"""
