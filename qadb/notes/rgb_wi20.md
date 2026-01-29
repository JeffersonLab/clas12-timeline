# Run Group B, Winter 2020, Pass 2

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

We will use the `sidisdvcs` train.

Cross check the train and DST run lists:
```bash
bin/qtl xtrain /mss/clas12/rg-b/production/recon/spring2020/torus-1/pass2/v1/dst/train/sidisdvcs /mss/clas12/rg-b/production/recon/spring2020/torus-1/pass2/v1/dst/recon
```

Make sure all skim files are cached:
```bash
bin/qtl histogram -d rgb_wi20_sidisdvcs --check-cache --flatdir --focus-physics /cache/clas12/rg-b/production/recon/spring2020/torus-1/pass2/v1/dst/train/sidisdvcs
```
then run monitoring
```bash
bin/qtl histogram -d rgb_wi20_sidisdvcs --submit --flatdir --focus-physics /cache/clas12/rg-b/production/recon/spring2020/torus-1/pass2/v1/dst/train/sidisdvcs
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring, be sure to cross check the output
> runs with those from `/mss`

## Make timelines

Make the timelines:
```bash
bin/qtl physics -d rgb_wi20_sidisdvcs -p rgb/pass2/qa/wi20
```
