#!/bin/bash
#
#
#
# DO NOT MERGE, this script cleans up local directories for TESTING
#
#
#

### testing run-monitoring
# rm -rv slurm
# rm -rv outfiles
# rm qa-physics/datasetList.txt
# rm /farm_out/$LOGNAME/clas12-timeline*

### testing detectors timelines
rm -rv outfiles/test_v0/log
rm -rv outfiles/test_v0/detectors/timelines
rm -rv outfiles/test_v0/timelines
