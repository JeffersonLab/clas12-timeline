#!/bin/bash
# test this PR
run-groovy $TIMELINE_GROOVY_OPTS qa-detectors/util/applyBounds.groovy ~/v/test-rgb-timeline/v29.43_org outfiles/test_rgb_qacuts |& tee outz
bin/deploy-timelines.sh -i outfiles/test_rgb_qacuts -d rgb-ltcc-test -t dilks
