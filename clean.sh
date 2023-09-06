#!/bin/bash
#
#
#
# DO NOT MERGE, this script cleans up local directories for TESTING
#
#
#

### testing run-monitoring
rm -rv slurm
rm -rv outfiles
rm -v /farm_out/$LOGNAME/clas12-timeline*

### testing detectors timelines
# rm -rv outfiles/test_v0/log
# rm -rv outfiles/test_v0/detectors/timelines
# rm -rv outfiles/test_v0/timelines
# rm -v  qa-physics/datasetList.txt
# rm -rv qa-physics/outdat.test_v0
# rm -rv qa-physics/outmon.test_v0
