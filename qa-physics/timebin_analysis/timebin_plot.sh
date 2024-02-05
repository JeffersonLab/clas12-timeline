#!/bin/bash
# make plots for time bin sizes, etc.

file_list="/farm_out/$LOGNAME/clas12-timeline--*.out"
[ $# -ge 1 ] && file_list=$1

out_file=time_bins.dat

grep -hE '^@' $file_list | head -n1 | sed 's;^.*#;;' > $out_file
grep -hE '^@' $file_list | sed 's;^@ ;;' | grep -v '^#' >> $out_file

