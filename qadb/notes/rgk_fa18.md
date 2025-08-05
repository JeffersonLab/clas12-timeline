# Run Group K, Fall 2018, Pass 2

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

We will use SIDIS skims from two datasets: one for a 6.5 GeV beam, and another for a 7.5 GeV beam; we will keep them separate:
- `rgk_fa18_6.5GeV`: from `/cache/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/train/skim1`
- `rgk_fa18_7.5GeV`: from `/cache/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/train/skim1`

- **6.5 GeV:**
```bash
qtl histogram --check-cache -d rgk_fa18_6.5GeV --rundir --focus-physics /cache/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/train/skim1
qtl histogram -d rgk_fa18_6.5GeV --rundir --focus-physics /cache/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/train/skim1
```

- **7.5 GeV:**
```bash
qtl histogram --check-cache -d rgk_fa18_7.5GeV --rundir --focus-physics /cache/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/train/skim1
qtl histogram -d rgk_fa18_7.5GeV --rundir --focus-physics /cache/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/train/skim1
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring, be sure to cross check the output
> runs with those from `/mss`

## Make timelines

Make the timelines, separately for each of the 2 datasets:
```bash
bin/qtl physics -d rgk_fa18_6.5GeV -p rgk/pass2/qa/fa18_6.5GeV
bin/qtl physics -d rgk_fa18_7.5GeV -p rgk/pass2/qa/fa18_7.5GeV
```

> [!WARNING]
> You may not have permission to publish to this directory; if not, publish to another place and ask the chef to copy the files
