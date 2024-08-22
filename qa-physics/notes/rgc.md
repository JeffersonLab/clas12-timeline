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
