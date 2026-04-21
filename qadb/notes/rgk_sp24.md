# Run Group K, Spring 2024, Pass 1

We will use SIDIS skims from two datasets: one for a 6.4 GeV beam, and another for a 8.5 GeV beam; we will keep them separate, to be consistent with what we did for Fall 2018 RG-K data:
- `rgk_sp24_6.4GeV`: from `/cache/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/train/skim1`
- `rgk_sp24_8.5GeV`: from `/cache/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/train/skim1`

Before anything, cross check the train and DST run lists:
```bash
bin/qtl xtrain /mss/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/train/skim1 /mss/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/recon
bin/qtl xtrain /mss/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/train/skim1 /mss/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/recon
```

## Reheat

**TODO**:
- [ ] check if needed; see Fall 2018 RG-K notes
- [ ] if reheating is needed, fix `qtl histogram` paths below

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

```bash
qtl histogram -d rgk_sp24_6.4GeV --flatdir --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/train/skim1
qtl histogram -d rgk_sp24_8.5GeV --flatdir --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/train/skim1
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring or reheating, be sure to cross check the output runs with those from `/mss`

## Make timelines

Make the timelines, separately for each of the 2 datasets:
```bash
bin/qtl physics -d rgk_sp24_6.4GeV -p rgk/pass2/qa/sp24_6.4GeV
bin/qtl physics -d rgk_sp24_8.5GeV -p rgk/pass2/qa/sp24_8.5GeV
```

> [!WARNING]
> You may not have permission to publish to this directory; if not, publish to another place and ask the chef to copy the files
