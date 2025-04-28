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
qtl histogram -d rga_sp19_prescaled --submit --focus-physics PATH_TO_PRESCALED_TRAIN
```

For the SIDIS train, `nSidis`, first make sure all skim files are cached:
```bash
qtl histogram -d rga_sp19_nSidis --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2019/torus-1/pass2/dst/train/nSidis
```
If they are not:
```bash
ls /mss/clas12/rg-a/production/recon/spring2019/torus-1/pass2/dst/train/nSidis/* | tee jlist.txt
jcache get $(cat jlist.txt)
# then wait for them to be cached
```
then run monitoring
```bash
qtl histogram -d rga_sp19_nSidis --submit --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2019/torus-1/pass2/dst/train/nSidis
```

## Make timelines

Make the timelines:
```bash
bin/qtl physics -d rga_sp19_prescaled -p rga/pass2/sp19/qa
bin/qtl physics -d rga_sp19_nSidis -p rga/pass2/sp19/qa
```
