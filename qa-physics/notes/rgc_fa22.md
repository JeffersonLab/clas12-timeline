# Run Group C, Fall 2022, Pass 1

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

We will use the `sidisdvcs` train.

We will combine the targets' data into a single dataset named `rgc_fa22_prescaled`.
```bash
qtl histogram --check-cache -d rgc_fa22_sidisdvcs --flatdir --focus-physics $(ls -d /cache/clas12/rg-c/production/fall22/pass1/*/*/dst/train/sidisdvcs)
qtl histogram -d rgc_fa22_sidisdvcs --flatdir --focus-physics $(ls -d /cache/clas12/rg-c/production/fall22/pass1/*/*/dst/train/sidisdvcs)
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring, be sure to cross check the output
> runs with those from `/mss`

## Make timelines

Make the timelines:
```bash
bin/qtl physics -d rgc_fa22_sidisdvcs -p rgc/Fall2022/qa-physics/pass1-sidisdvcs
```
