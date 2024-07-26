# Run Group C QA

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

We will now combine the targets' data into a single dataset named `qa_rgc_su22`.
Assuming your output data are in
```
/volatile/clas12/users/$LOGNAME/qa_rgc_su22_*
```
and that this wildcard pattern does _not_ include any files you _don't_ want, you may run
```bash
bin/run-monitoring.sh -d qa_rgc_su22 --flatdir --focus-physics $(ls -d /volatile/clas12/users/$LOGNAME/qa_rgc_su22_*/train/QA)
```

## Make timelines

Make the timelines:
```bash
bin/run-physics-timelines.sh -d qa_rgc_su22
```

Deploy either to your area or the common area (remove the `-D` option once you confirm this is the correct directory):
```bash
# your area, for testing
bin/deploy-timelines.sh -d qa_rgc_su22 -t $LOGNAME -D

# common area
bin/deploy-timelines.sh -d qa_rgc_su22 -t rgc/Summer2022/qa-physics -s pass1-prescaled -D
```

# List of Runs
Together with targets and beam energies
```
016042 2.2gev ET
016043 2.2gev ET
016044 2.2gev ET
016047 2.2gev C
016048 2.2gev C
016049 2.2gev C
016050 2.2gev C
016051 2.2gev C
016052 2.2gev C
016054 2.2gev C
016066 2.2gev NH3
016067 2.2gev NH3
016069 2.2gev NH3
016074 2.2gev NH3
016075 2.2gev NH3
016076 2.2gev NH3
016077 2.2gev NH3
016078 2.2gev NH3
016089 10.5gev ET
016096 10.5gev C
016098 10.5gev C
016100 10.5gev C
016101 10.5gev C
016102 10.5gev C
016103 10.5gev C
016105 10.5gev C
016106 10.5gev C
016107 10.5gev C
016108 10.5gev C
016109 10.5gev C
016110 10.5gev C
016111 10.5gev C
016112 10.5gev C
016113 10.5gev C
016114 10.5gev C
016115 10.5gev C
016116 10.5gev C
016117 10.5gev C
016119 10.5gev C
016122 10.5gev C
016128 10.5gev C
016134 10.5gev C
016137 10.5gev NH3
016138 10.5gev NH3
016144 10.5gev NH3
016145 10.5gev NH3
016146 10.5gev NH3
016148 10.5gev NH3
016156 10.5gev NH3
016157 10.5gev NH3
016158 10.5gev NH3
016164 10.5gev NH3
016166 10.5gev NH3
016167 10.5gev NH3
016168 10.5gev NH3
016169 10.5gev NH3
016170 10.5gev NH3
016178 10.5gev NH3
016184 10.5gev ET
016185 10.5gev ET
016186 10.5gev ET
016188 10.5gev Align
016190 10.5gev Align
016191 10.5gev Align
016194 10.5gev ET
016211 10.5gev NH3
016213 10.5gev NH3
016214 10.5gev NH3
016221 10.5gev NH3
016222 10.5gev NH3
016223 10.5gev NH3
016224 10.5gev NH3
016225 10.5gev NH3
016226 10.5gev NH3
016228 10.5gev NH3
016231 10.5gev NH3
016232 10.5gev NH3
016233 10.5gev NH3
016234 10.5gev NH3
016235 10.5gev NH3
016236 10.5gev NH3
016238 10.5gev NH3
016243 10.5gev NH3
016244 10.5gev NH3
016245 10.5gev NH3
016246 10.5gev NH3
016248 10.5gev NH3
016249 10.5gev NH3
016250 10.5gev NH3
016251 10.5gev NH3
016252 10.5gev NH3
016253 10.5gev NH3
016256 10.5gev NH3
016257 10.5gev NH3
016259 10.5gev NH3
016260 10.5gev NH3
016262 10.5gev ND3
016263 10.5gev ND3
016270 10.5gev ND3
016271 10.5gev ND3
016273 10.5gev ND3
016276 10.5gev ND3
016277 10.5gev ND3
016279 10.5gev ND3
016280 10.5gev ND3
016281 10.5gev ND3
016283 10.5gev ND3
016284 10.5gev ND3
016285 10.5gev ND3
016286 10.5gev ND3
016287 10.5gev ND3
016288 10.5gev ND3
016289 10.5gev ND3
016290 10.5gev C
016291 10.5gev C
016292 10.5gev C
016293 10.5gev C
016296 10.5gev C
016297 10.5gev C
016298 10.5gev CH2
016299 10.5gev CH2
016300 10.5gev CH2
016301 10.5gev CH2
016302 10.5gev CH2
016303 10.5gev CH2
016306 10.5gev ET
016307 10.5gev ET
016308 10.5gev ET
016309 10.5gev ET
016317 10.5gev NH3
016318 10.5gev NH3
016320 10.5gev NH3
016321 10.5gev NH3
016322 10.5gev NH3
016323 10.5gev NH3
016325 10.5gev NH3
016326 10.5gev NH3
016327 10.5gev NH3
016328 10.5gev NH3
016329 10.5gev NH3
016330 10.5gev NH3
016331 10.5gev NH3
016332 10.5gev NH3
016333 10.5gev NH3
016335 10.5gev NH3
016336 10.5gev NH3
016337 10.5gev NH3
016338 10.5gev NH3
016339 10.5gev NH3
016341 10.5gev NH3
016343 10.5gev NH3
016345 10.5gev NH3
016346 10.5gev NH3
016348 10.5gev NH3
016350 10.5gev NH3
016352 10.5gev NH3
016353 10.5gev NH3
016354 10.5gev NH3
016355 10.5gev NH3
016356 10.5gev NH3
016357 10.5gev NH3
016358 10.5gev ND3
016359 10.5gev ND3
016360 10.5gev ND3
016361 10.5gev ND3
016362 10.5gev ND3
016396 10.5gev ND3
016397 10.5gev ND3
016398 10.5gev ND3
016400 10.5gev ND3
016401 10.5gev ND3
016403 10.5gev ND3
016404 10.5gev ND3
016405 10.5gev ND3
016406 10.5gev ND3
016407 10.5gev ND3
016408 10.5gev ND3
016409 10.5gev ND3
016410 10.5gev ND3
016411 10.5gev ND3
016412 10.5gev ND3
016414 10.5gev ND3
016415 10.5gev ND3
016416 10.5gev ND3
016419 10.5gev ND3
016420 10.5gev ND3
016421 10.5gev ND3
016422 10.5gev ND3
016423 10.5gev ND3
016424 10.5gev ND3
016425 10.5gev ND3
016426 10.5gev ND3
016432 10.5gev ND3
016433 10.5gev ND3
016434 10.5gev ND3
016435 10.5gev ND3
016436 10.5gev ND3
016438 10.5gev ND3
016440 10.5gev ND3
016441 10.5gev ND3
016442 10.5gev ND3
016443 10.5gev ND3
016444 10.5gev ND3
016445 10.5gev ND3
016447 10.5gev ND3
016448 10.5gev ND3
016449 10.5gev ND3
016454 10.5gev ND3
016455 10.5gev ND3
016456 10.5gev ND3
016457 10.5gev ND3
016458 10.5gev ND3
016460 10.5gev ND3
016461 10.5gev ND3
016463 10.5gev ND3
016465 10.5gev ND3
016466 10.5gev ND3
016467 10.5gev ND3
016468 10.5gev ND3
016469 10.5gev ND3
016470 10.5gev ND3
016471 10.5gev ND3
016472 10.5gev ND3
016473 10.5gev ND3
016474 10.5gev ND3
016475 10.5gev ND3
016476 10.5gev ND3
016477 10.5gev ND3
016478 10.5gev ND3
016480 10.5gev ND3
016482 10.5gev ND3
016483 10.5gev ND3
016484 10.5gev ND3
016489 10.5gev ND3
016490 10.5gev ND3
016491 10.5gev ND3
016493 10.5gev ND3
016494 10.5gev ND3
016495 10.5gev ND3
016498 10.5gev ND3
016500 10.5gev ND3
016501 10.5gev ND3
016502 10.5gev ND3
016503 10.5gev ND3
016504 10.5gev ND3
016505 10.5gev ND3
016506 10.5gev ND3
016507 10.5gev ND3
016508 10.5gev ND3
016509 10.5gev ND3
016510 10.5gev ND3
016511 10.5gev ND3
016512 10.5gev ND3
016513 10.5gev ND3
016514 10.5gev ND3
016515 10.5gev ND3
016517 10.5gev ND3
016518 10.5gev ND3
016519 10.5gev ND3
016520 10.5gev ND3
016580 10.5gev ND3
016581 10.5gev ND3
016583 10.5gev ND3
016586 10.5gev ND3
016587 10.5gev ND3
016588 10.5gev ND3
016594 10.5gev ND3
016597 10.5gev ND3
016598 10.5gev ND3
016599 10.5gev ND3
016600 10.5gev ND3
016601 10.5gev ND3
016602 10.5gev ND3
016604 10.5gev ND3
016609 10.5gev ND3
016610 10.5gev ND3
016611 10.5gev ND3
016615 10.5gev ND3
016616 10.5gev ND3
016617 10.5gev ND3
016618 10.5gev ND3
016619 10.5gev ND3
016620 10.5gev ND3
016625 10.5gev ND3
016626 10.5gev ND3
016627 10.5gev ND3
016628 10.5gev ND3
016629 10.5gev ND3
016630 10.5gev ND3
016631 10.5gev ND3
016632 10.5gev ND3
016633 10.5gev ND3
016634 10.5gev ND3
016636 10.5gev ND3
016658 10.5gev NH3
016659 10.5gev NH3
016660 10.5gev NH3
016664 10.5gev NH3
016665 10.5gev NH3
016666 10.5gev NH3
016671 10.5gev NH3
016672 10.5gev NH3
016673 10.5gev NH3
016674 10.5gev NH3
016675 10.5gev NH3
016676 10.5gev NH3
016678 10.5gev NH3
016679 10.5gev NH3
016681 10.5gev NH3
016682 10.5gev NH3
016683 10.5gev NH3
016685 10.5gev NH3
016686 10.5gev NH3
016687 10.5gev NH3
016688 10.5gev NH3
016689 10.5gev NH3
016690 10.5gev NH3
016692 10.5gev NH3
016693 10.5gev NH3
016695 10.5gev NH3
016697 10.5gev C
016698 10.5gev C
016699 10.5gev C
016700 10.5gev C
016701 10.5gev C
016702 10.5gev C
016704 10.5gev C
016709 10.5gev NH3
016710 10.5gev NH3
016711 10.5gev NH3
016712 10.5gev NH3
016713 10.5gev NH3
016715 10.5gev NH3
016716 10.5gev NH3
016717 10.5gev NH3
016718 10.5gev NH3
016719 10.5gev NH3
016720 10.5gev NH3
016721 10.5gev NH3
016722 10.5gev NH3
016723 10.5gev NH3
016726 10.5gev NH3
016727 10.5gev NH3
016728 10.5gev NH3
016729 10.5gev NH3
016730 10.5gev NH3
016731 10.5gev NH3
016732 10.5gev NH3
016733 10.5gev NH3
016734 10.5gev NH3
016736 10.5gev NH3
016738 10.5gev NH3
016742 10.5gev NH3
016743 10.5gev NH3
016744 10.5gev NH3
016746 10.5gev NH3
016747 10.5gev NH3
016748 10.5gev NH3
016749 10.5gev NH3
016750 10.5gev NH3
016751 10.5gev NH3
016752 10.5gev NH3
016753 10.5gev NH3
016754 10.5gev NH3
016755 10.5gev NH3
016756 10.5gev NH3
016757 10.5gev NH3
016758 10.5gev NH3
016759 10.5gev NH3
016761 10.5gev NH3
016762 10.5gev NH3
016763 10.5gev NH3
016765 10.5gev NH3
016766 10.5gev NH3
016767 10.5gev NH3
016768 10.5gev NH3
016769 10.5gev NH3
016770 10.5gev NH3
016771 10.5gev NH3
016772 10.5gev NH3
016783 10.5gev Align
016784 10.5gev Align
016785 10.5gev Align
016786 10.5gev Align
```
