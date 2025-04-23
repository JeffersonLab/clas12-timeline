# Run Group A, Fall 2019, Pass 2

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

We will use the `nSidis` train.

First make sure all skim files are cached:
```bash
qtl histogram -d rga_fa18_inbending_nSidis  --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/fall2018/torus-1/pass2/main/train/nSidis
qtl histogram -d rga_fa18_outbending_nSidis --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/fall2018/torus+1/pass2/train/nSidis
```
then run monitoring
```bash
qtl histogram -d rga_fa18_inbending_nSidis  --submit --flatdir --focus-physics /cache/clas12/rg-a/production/recon/fall2018/torus-1/pass2/main/train/nSidis
qtl histogram -d rga_fa18_outbending_nSidis --submit --flatdir --focus-physics /cache/clas12/rg-a/production/recon/fall2018/torus+1/pass2/train/nSidis
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring, be sure to cross check the output
> runs with those from `/mss`

## Make timelines

Make the timelines:
```bash
bin/qtl physics -d rga_fa18_inbending_nSidis -p rga/pass2/fa18/qa
bin/qtl physics -d rga_fa18_outbending_nSidis -p rga/pass2/fa18/qa
```
