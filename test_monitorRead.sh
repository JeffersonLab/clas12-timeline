#!/bin/bash
#
#
# GENERATED FROM `run-monitoring.sh`
#
#
set -e
set -u
set -o pipefail
echo "RUN 6666"

# set classpath
export JYPATH=/w/hallb-scshelf2102/clas12/users/dilks/dm/clas12-timeline/detectors/target/*:/group/clas12/packages/groovy/4.0.3/lib/*

# produce histograms
run-groovy \
  -Djava.awt.headless=true \
  /w/hallb-scshelf2102/clas12/users/dilks/dm/clas12-timeline/qa-physics/monitorRead.groovy \
    /lustre19/expphy/cache/clas12/rg-a/production/recon/spring2019/torus-1/pass2/dst/recon/006666 \
    /w/hallb-scshelf2102/clas12/users/dilks/dm/clas12-timeline/outfiles/pass2/timeline_physics/6666 \
    dst \
    6666

# check output HIPO files
/w/hallb-scshelf2102/clas12/users/dilks/dm/clas12-timeline/bin/hipo-check.sh $(find /w/hallb-scshelf2102/clas12/users/dilks/dm/clas12-timeline/outfiles/pass2/timeline_physics/6666 -name "*.hipo")
