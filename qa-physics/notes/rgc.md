# Run Group C QA

This is the first QA to use the time bins and the prescaling.

## Produce prescaled trains

Check the commands carefully before you run; these are just notes...
```bash
cd qa-physics/prescaler
cook-train.rb --listDatasets | grep rgc_su22 | xargs -I{} cook-train.rb --dataset {} --coatjava 10.1.1
start-workflow.sh rgc-a-su22*.json  ## check that this is the correct set of JSON files before running
```

NOTE: one workflow per target; we'll assume step 1's `--flatdir` option can take in multiple run directories,
and output everything in a single `outfiles/$dataset` directory
