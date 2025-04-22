# Run Group B, Spring 2019, Pass 2

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

We will use the `sidisdvcs` train.

First make sure all skim files are cached:
```bash
qtl histogram -d rgb_sp19_sidisdvcs --check-cache --flatdir --focus-physics /cache/clas12/rg-b/production/recon/spring2019/torus-1/pass2/v0/dst/train/sidisdvcs
```
then run monitoring
```bash
qtl histogram -d rgb_sp19_sidisdvcs --submit --flatdir --focus-physics /cache/clas12/rg-b/production/recon/spring2019/torus-1/pass2/v0/dst/train/sidisdvcs
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring, be sure to cross check the output
> runs with those from `/mss`

## Make timelines

Make the timelines:
```bash
bin/qtl physics -d rgb_sp19_sidisdvcs
```

Deploy either to your area or the common area (remove the `-D` option once you confirm this is the correct directory):
```bash
# your area, for testing
qtl deploy -d rgb_sp19_sidisdvcs  -t $LOGNAME -D

# common area
qtl deploy -d rgb_sp19_sidisdvcs  -t rgb/pass2/qa/sp19 -D
```
