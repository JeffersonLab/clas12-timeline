# Run Group C, Summer 2022, Pass 1

This is the first QA to use the time bins and the prescaling.

## Produce prescaled trains

Check the commands carefully before you run; these are just notes...
```bash
cd qa-physics/prescaler
cook-train.rb --listDatasets | grep rgc_su22 | xargs -I{} cook-train.rb --dataset {} --coatjava 10.1.1
start-workflow.sh rgc-a-su22*.json  ## check that this is the correct set of JSON files before running
```

> [!IMPORTANT]
> 10.5 GeV ET workflow failed with `SITE_PREP_FAIL`, where the disk usage allocation was a bit too small; for those,
> for example, add 2 GB:
> ```bash
> swif2 modify-jobs rgc-a-su2210.5ET-16089x9 -disk add 2gb -problems SITE_PREP_FAIL
> ```
> - this will automatically retry the problematic jobs
> - you might need more than the added 2 GB for some jobs
> - **alternatively**: edit the JSON file by hand, increasing the value of `disk_bytes` nodes by 4 GB
<!--`-->

> [!NOTE]
> This runs one workflow per target; step 1's `--flatdir` option can take in multiple run directories,
> and output everything in a single `outfiles/$dataset` directory.

## Check prescaled trains

> [!IMPORTANT]
> To be sure the workflows succeeded and we have all the data, run `check-train.rb`.

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

We will now combine the targets' data into a single dataset named `rgc_su22_prescaled`.
Assuming your output data are in
```
/volatile/clas12/users/$LOGNAME/qa_rgc_su22_*
```
and that this wildcard pattern does _not_ include any files you _don't_ want, you may run
```bash
bin/run-monitoring.sh -d rgc_su22_prescaled --flatdir --focus-physics $(ls -d /volatile/clas12/users/$LOGNAME/qa_rgc_su22_*/train/QA)
```
Alternatively, for `sidisdvcs` trains (which have better statistics for asymmetries):
```bash
bin/run-monitoring.sh --check-cache -d rgc_su22_sidisdvcs --flatdir --focus-physics $(ls -d /cache/clas12/rg-c/production/summer22/pass1/*/*/dst/train/sidisdvcs)
bin/run-monitoring.sh -d rgc_su22_sidisdvcs --flatdir --focus-physics $(ls -d /cache/clas12/rg-c/production/summer22/pass1/*/*/dst/train/sidisdvcs)
```

## Make timelines

Make the timelines:
```bash
bin/run-physics-timelines.sh -d rgc_su22_prescaled
bin/run-physics-timelines.sh -d rgc_su22_sidisdvcs
```

Deploy either to your area or the common area (remove the `-D` option once you confirm this is the correct directory):
```bash
# your area, for testing
bin/deploy-timelines.sh -d rgc_su22_prescaled -t $LOGNAME -m rgc_su22 -D
bin/deploy-timelines.sh -d rgc_su22_sidisdvcs -t $LOGNAME -m rgc_su22 -D

# common area
bin/deploy-timelines.sh -d rgc_su22_prescaled -t rgc/Summer2022/qa-physics -s pass1-prescaled -m rgc_su22 -D
bin/deploy-timelines.sh -d rgc_su22_sidisdvcs -t rgc/Summer2022/qa-physics -s pass1-sidisdvcs -m rgc_su22 -D
```

# List of Runs
Together with targets and beam energies
```
16042 2.2gev ET
16043 2.2gev ET
16044 2.2gev ET
16047 2.2gev C
16048 2.2gev C
16049 2.2gev C
16050 2.2gev C
16051 2.2gev C
16052 2.2gev C
16054 2.2gev C
16066 2.2gev NH3
16067 2.2gev NH3
16069 2.2gev NH3
16074 2.2gev NH3
16075 2.2gev NH3
16076 2.2gev NH3
16077 2.2gev NH3
16078 2.2gev NH3
16089 10.5gev ET
16096 10.5gev C
16098 10.5gev C
16100 10.5gev C
16101 10.5gev C
16102 10.5gev C
16103 10.5gev C
16105 10.5gev C
16106 10.5gev C
16107 10.5gev C
16108 10.5gev C
16109 10.5gev C
16110 10.5gev C
16111 10.5gev C
16112 10.5gev C
16113 10.5gev C
16114 10.5gev C
16115 10.5gev C
16116 10.5gev C
16117 10.5gev C
16119 10.5gev C
16122 10.5gev C
16128 10.5gev C
16134 10.5gev C
16137 10.5gev NH3
16138 10.5gev NH3
16144 10.5gev NH3
16145 10.5gev NH3
16146 10.5gev NH3
16148 10.5gev NH3
16156 10.5gev NH3
16157 10.5gev NH3
16158 10.5gev NH3
16164 10.5gev NH3
16166 10.5gev NH3
16167 10.5gev NH3
16168 10.5gev NH3
16169 10.5gev NH3
16170 10.5gev NH3
16178 10.5gev NH3
16184 10.5gev ET
16185 10.5gev ET
16186 10.5gev ET
16188 10.5gev Align
16190 10.5gev Align
16191 10.5gev Align
16194 10.5gev ET
16211 10.5gev NH3
16213 10.5gev NH3
16214 10.5gev NH3
16221 10.5gev NH3
16222 10.5gev NH3
16223 10.5gev NH3
16224 10.5gev NH3
16225 10.5gev NH3
16226 10.5gev NH3
16228 10.5gev NH3
16231 10.5gev NH3
16232 10.5gev NH3
16233 10.5gev NH3
16234 10.5gev NH3
16235 10.5gev NH3
16236 10.5gev NH3
16238 10.5gev NH3
16243 10.5gev NH3
16244 10.5gev NH3
16245 10.5gev NH3
16246 10.5gev NH3
16248 10.5gev NH3
16249 10.5gev NH3
16250 10.5gev NH3
16251 10.5gev NH3
16252 10.5gev NH3
16253 10.5gev NH3
16256 10.5gev NH3
16257 10.5gev NH3
16259 10.5gev NH3
16260 10.5gev NH3
16262 10.5gev ND3
16263 10.5gev ND3
16270 10.5gev ND3
16271 10.5gev ND3
16273 10.5gev ND3
16276 10.5gev ND3
16277 10.5gev ND3
16279 10.5gev ND3
16280 10.5gev ND3
16281 10.5gev ND3
16283 10.5gev ND3
16284 10.5gev ND3
16285 10.5gev ND3
16286 10.5gev ND3
16287 10.5gev ND3
16288 10.5gev ND3
16289 10.5gev ND3
16290 10.5gev C
16291 10.5gev C
16292 10.5gev C
16293 10.5gev C
16296 10.5gev C
16297 10.5gev C
16298 10.5gev CH2
16299 10.5gev CH2
16300 10.5gev CH2
16301 10.5gev CH2
16302 10.5gev CH2
16303 10.5gev CH2
16306 10.5gev ET
16307 10.5gev ET
16308 10.5gev ET
16309 10.5gev ET
16317 10.5gev NH3
16318 10.5gev NH3
16320 10.5gev NH3
16321 10.5gev NH3
16322 10.5gev NH3
16323 10.5gev NH3
16325 10.5gev NH3
16326 10.5gev NH3
16327 10.5gev NH3
16328 10.5gev NH3
16329 10.5gev NH3
16330 10.5gev NH3
16331 10.5gev NH3
16332 10.5gev NH3
16333 10.5gev NH3
16335 10.5gev NH3
16336 10.5gev NH3
16337 10.5gev NH3
16338 10.5gev NH3
16339 10.5gev NH3
16341 10.5gev NH3
16343 10.5gev NH3
16345 10.5gev NH3
16346 10.5gev NH3
16348 10.5gev NH3
16350 10.5gev NH3
16352 10.5gev NH3
16353 10.5gev NH3
16354 10.5gev NH3
16355 10.5gev NH3
16356 10.5gev NH3
16357 10.5gev NH3
16358 10.5gev ND3
16359 10.5gev ND3
16360 10.5gev ND3
16361 10.5gev ND3
16362 10.5gev ND3
16396 10.5gev ND3
16397 10.5gev ND3
16398 10.5gev ND3
16400 10.5gev ND3
16401 10.5gev ND3
16403 10.5gev ND3
16404 10.5gev ND3
16405 10.5gev ND3
16406 10.5gev ND3
16407 10.5gev ND3
16408 10.5gev ND3
16409 10.5gev ND3
16410 10.5gev ND3
16411 10.5gev ND3
16412 10.5gev ND3
16414 10.5gev ND3
16415 10.5gev ND3
16416 10.5gev ND3
16419 10.5gev ND3
16420 10.5gev ND3
16421 10.5gev ND3
16422 10.5gev ND3
16423 10.5gev ND3
16424 10.5gev ND3
16425 10.5gev ND3
16426 10.5gev ND3
16432 10.5gev ND3
16433 10.5gev ND3
16434 10.5gev ND3
16435 10.5gev ND3
16436 10.5gev ND3
16438 10.5gev ND3
16440 10.5gev ND3
16441 10.5gev ND3
16442 10.5gev ND3
16443 10.5gev ND3
16444 10.5gev ND3
16445 10.5gev ND3
16447 10.5gev ND3
16448 10.5gev ND3
16449 10.5gev ND3
16454 10.5gev ND3
16455 10.5gev ND3
16456 10.5gev ND3
16457 10.5gev ND3
16458 10.5gev ND3
16460 10.5gev ND3
16461 10.5gev ND3
16463 10.5gev ND3
16465 10.5gev ND3
16466 10.5gev ND3
16467 10.5gev ND3
16468 10.5gev ND3
16469 10.5gev ND3
16470 10.5gev ND3
16471 10.5gev ND3
16472 10.5gev ND3
16473 10.5gev ND3
16474 10.5gev ND3
16475 10.5gev ND3
16476 10.5gev ND3
16477 10.5gev ND3
16478 10.5gev ND3
16480 10.5gev ND3
16482 10.5gev ND3
16483 10.5gev ND3
16484 10.5gev ND3
16489 10.5gev ND3
16490 10.5gev ND3
16491 10.5gev ND3
16493 10.5gev ND3
16494 10.5gev ND3
16495 10.5gev ND3
16498 10.5gev ND3
16500 10.5gev ND3
16501 10.5gev ND3
16502 10.5gev ND3
16503 10.5gev ND3
16504 10.5gev ND3
16505 10.5gev ND3
16506 10.5gev ND3
16507 10.5gev ND3
16508 10.5gev ND3
16509 10.5gev ND3
16510 10.5gev ND3
16511 10.5gev ND3
16512 10.5gev ND3
16513 10.5gev ND3
16514 10.5gev ND3
16515 10.5gev ND3
16517 10.5gev ND3
16518 10.5gev ND3
16519 10.5gev ND3
16520 10.5gev ND3
16580 10.5gev ND3
16581 10.5gev ND3
16583 10.5gev ND3
16586 10.5gev ND3
16587 10.5gev ND3
16588 10.5gev ND3
16594 10.5gev ND3
16597 10.5gev ND3
16598 10.5gev ND3
16599 10.5gev ND3
16600 10.5gev ND3
16601 10.5gev ND3
16602 10.5gev ND3
16604 10.5gev ND3
16609 10.5gev ND3
16610 10.5gev ND3
16611 10.5gev ND3
16615 10.5gev ND3
16616 10.5gev ND3
16617 10.5gev ND3
16618 10.5gev ND3
16619 10.5gev ND3
16620 10.5gev ND3
16625 10.5gev ND3
16626 10.5gev ND3
16627 10.5gev ND3
16628 10.5gev ND3
16629 10.5gev ND3
16630 10.5gev ND3
16631 10.5gev ND3
16632 10.5gev ND3
16633 10.5gev ND3
16634 10.5gev ND3
16636 10.5gev ND3
16658 10.5gev NH3
16659 10.5gev NH3
16660 10.5gev NH3
16664 10.5gev NH3
16665 10.5gev NH3
16666 10.5gev NH3
16671 10.5gev NH3
16672 10.5gev NH3
16673 10.5gev NH3
16674 10.5gev NH3
16675 10.5gev NH3
16676 10.5gev NH3
16678 10.5gev NH3
16679 10.5gev NH3
16681 10.5gev NH3
16682 10.5gev NH3
16683 10.5gev NH3
16685 10.5gev NH3
16686 10.5gev NH3
16687 10.5gev NH3
16688 10.5gev NH3
16689 10.5gev NH3
16690 10.5gev NH3
16692 10.5gev NH3
16693 10.5gev NH3
16695 10.5gev NH3
16697 10.5gev C
16698 10.5gev C
16699 10.5gev C
16700 10.5gev C
16701 10.5gev C
16702 10.5gev C
16704 10.5gev C
16709 10.5gev NH3
16710 10.5gev NH3
16711 10.5gev NH3
16712 10.5gev NH3
16713 10.5gev NH3
16715 10.5gev NH3
16716 10.5gev NH3
16717 10.5gev NH3
16718 10.5gev NH3
16719 10.5gev NH3
16720 10.5gev NH3
16721 10.5gev NH3
16722 10.5gev NH3
16723 10.5gev NH3
16726 10.5gev NH3
16727 10.5gev NH3
16728 10.5gev NH3
16729 10.5gev NH3
16730 10.5gev NH3
16731 10.5gev NH3
16732 10.5gev NH3
16733 10.5gev NH3
16734 10.5gev NH3
16736 10.5gev NH3
16738 10.5gev NH3
16742 10.5gev NH3
16743 10.5gev NH3
16744 10.5gev NH3
16746 10.5gev NH3
16747 10.5gev NH3
16748 10.5gev NH3
16749 10.5gev NH3
16750 10.5gev NH3
16751 10.5gev NH3
16752 10.5gev NH3
16753 10.5gev NH3
16754 10.5gev NH3
16755 10.5gev NH3
16756 10.5gev NH3
16757 10.5gev NH3
16758 10.5gev NH3
16759 10.5gev NH3
16761 10.5gev NH3
16762 10.5gev NH3
16763 10.5gev NH3
16765 10.5gev NH3
16766 10.5gev NH3
16767 10.5gev NH3
16768 10.5gev NH3
16769 10.5gev NH3
16770 10.5gev NH3
16771 10.5gev NH3
16772 10.5gev NH3
16783 10.5gev Align
16784 10.5gev Align
16785 10.5gev Align
16786 10.5gev Align
```
