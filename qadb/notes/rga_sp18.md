# Run Group A, Spring 2018, Pass 1

## Reheat

> [!WARNING]
> The FC charge from the Pass 1 data files is incorrect; therefore, we need to "reheat" the data.

We need to use the Faraday Cup for the livetime, along with a DCS2 rollover fix. See the following pull requests:
- https://github.com/JeffersonLab/coatjava/pull/49
- https://github.com/JeffersonLab/coatjava/pull/814
- https://github.com/JeffersonLab/coatjava/pull/822

To do so, we'll need to run the following commands on every file
```bash
rebuild-scalers -c X -o $tmpFile $inputFile
postprocess -q 1 -o $outputFile $tmpFile
```

> [!IMPORTANT]
> You _must_ use Coatjava v13.3.0 or newer

We decided to reheat only the `nSidis` train, and store the result on `/volatile`; here are the commands:

1. make sure all data are on `/cache`; re-cache them if necessary:
```bash
qtl histogram -d rga_sp18_outbending_nSidis --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus+1/pass1/dst/train/nSidis
qtl histogram -d rga_sp18_inbending_nSidis  --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus-1/pass1/dst/train/nSidis
```

2. run reheat:
```bash
qtl reheat -c rollover -d rga_sp18_outbending_nSidis -o /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_outbending_nSidis -i /cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus+1/pass1/dst/train/nSidis
qtl reheat -c rollover -d rga_sp18_inbending_nSidis  -o /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_inbending_nSidis  -i /cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus-1/pass1/dst/train/nSidis
```

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`.

```bash
qtl histogram -d rga_sp18_outbending_nSidis --submit --flatdir --focus-physics /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_outbending_nSidis
qtl histogram -d rga_sp18_inbending_nSidis  --submit --flatdir --focus-physics /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_inbending_nSidis
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring, be sure to cross check the output
> runs with those from `/mss`

## Make timelines

Make the timelines:
```bash
bin/qtl physics -d rga_sp18_outbending_nSidis -p rga/pass2/sp18/qa
bin/qtl physics -d rga_sp18_inbending_nSidis -p rga/pass2/sp18/qa
```
