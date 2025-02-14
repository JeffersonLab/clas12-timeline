# Run Group B, Fall 2019, Pass 2

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

We will use the `sidisdvcs` train. There are inbending and outbending data, which we'll
combine to one "dataset" in `run-monitoring.sh`.

First make sure all skim files are cached:
```bash
bin/run-monitoring.sh -d rgb_fa19_sidisdvcs --check-cache --flatdir --focus-physics \
  /cache/clas12/rg-b/production/recon/fall2019/torus+1/pass2/v1/dst/train/sidisdvcs/ \
  /cache/clas12/rg-b/production/recon/fall2019/torus-1/pass2/v1/dst/train/sidisdvcs/
```
then run monitoring
```bash
bin/run-monitoring.sh -d rgb_fa19_sidisdvcs --submit --flatdir --focus-physics \
  /cache/clas12/rg-b/production/recon/fall2019/torus+1/pass2/v1/dst/train/sidisdvcs/ \
  /cache/clas12/rg-b/production/recon/fall2019/torus-1/pass2/v1/dst/train/sidisdvcs/
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring, be sure to cross check the output
> runs with those from `/mss`

## Make timelines

Make the timelines:
```bash
bin/run-physics-timelines.sh -d rgb_fa19_sidisdvcs
```

Deploy either to your area or the common area (remove the `-D` option once you confirm this is the correct directory):
```bash
# your area, for testing
bin/deploy-timelines.sh -d rgb_fa19_sidisdvcs  -t $LOGNAME -D

# common area
bin/deploy-timelines.sh -d rgb_fa19_sidisdvcs  -t rgb/pass2/qa/fa19 -D
```
