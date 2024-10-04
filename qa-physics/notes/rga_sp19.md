# Run Group A, Spring 2019, Pass 2

## Produce prescaled trains

> [!NOTE]
> Bad tape 803771 (April, 2024) included DSTs that were not recooked;
> this QA therefore did not check the 'prescale' train (but commands
> are still written below)

Check the commands carefully before you run; these are just notes...
```bash
cd qa-physics/prescaler
cook-train.rb --dataset rga_sp19 --coatjava 10.1.1
start-workflow.sh rga-a-sp19*.json  ## check that this is the correct JSON file before running
```

## Check prescaled trains

> [!IMPORTANT]
> To be sure the workflows succeeded and we have all the data, run `check-train.rb`.

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

For the prescaled train:
```bash
bin/run-monitoring.sh -d rga_sp19_prescaled --submit --focus-physics PATH_TO_PRESCALED_TRAIN
```

For the SIDIS train, `nSidis`, first make sure all skim files are cached:
```bash
bin/run-monitoring.sh -d rga_sp19_nSidis --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2019/torus-1/pass2/dst/train/nSidis
```
If they are not:
```bash
ls /mss/clas12/rg-a/production/recon/spring2019/torus-1/pass2/dst/train/nSidis/* | tee jlist.txt
jcache get $(cat jlist.txt)
# then wait for them to be cached
```
then run monitoring
```bash
bin/run-monitoring.sh -d rga_sp19_nSidis --submit --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2019/torus-1/pass2/dst/train/nSidis
```

## Make timelines

Make the timelines:
```bash
bin/run-physics-timelines.sh -d rga_sp19_prescaled
bin/run-physics-timelines.sh -d rga_sp19_nSidis
```

Deploy either to your area or the common area (remove the `-D` option once you confirm this is the correct directory):
```bash
# your area, for testing
bin/deploy-timelines.sh -d rga_sp19_prescaled -t $LOGNAME -D
bin/deploy-timelines.sh -d rga_sp19_nSidis -t $LOGNAME -D

# common area
bin/deploy-timelines.sh -d rga_sp19_prescaled -t rga/pass2/sp19/qa -D
bin/deploy-timelines.sh -d rga_sp19_nSidis -t rga/pass2/sp19/qa -D
```
