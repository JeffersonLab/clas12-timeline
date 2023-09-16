#!/bin/bash
[ $# -ne 1 ] && exit 100
outfile=outz.$(basename $1)
> $outfile
for f in $(find $1 -name "*.hipo"); do run-groovy test.groovy $f | grep -E '^counts:' | tee -a $outfile; done
