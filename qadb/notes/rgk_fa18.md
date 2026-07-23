# Run Group K, Fall 2018, Pass 2

We will use SIDIS skims from two datasets: one for a 6.5 GeV beam, and another for a 7.5 GeV beam; we will keep them separate:
- `rgk_fa18_6.5GeV`: from `/cache/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/train/skim1`
- `rgk_fa18_7.5GeV`: from `/cache/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/train/skim1`

Before anything, cross check the train and DST run lists:
```bash
bin/qtl xtrain /mss/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/train/skim1 /mss/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/recon
bin/qtl xtrain /mss/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/train/skim1 /mss/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/recon
```

## Reheat

> [!WARNING]
> The FC charge from the Pass 2 data files is incorrect, since they were cooked with `recharge` set to `false`; therefore, we need to "reheat" the data.
> See `rga_sp18.md` for some more details.

1. make sure all data are on `/cache`; re-cache them if necessary:
```bash
qtl histogram --check-cache -d rgk_fa18_6.5GeV --flatdir --focus-physics /cache/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/train/skim1
qtl histogram --check-cache -d rgk_fa18_7.5GeV --flatdir --focus-physics /cache/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/train/skim1
```
2. run reheat:
```bash
bin/qtl reheat -m rollover -c 13.3.0 rgk_fa18_6.5GeV -o /volatile/clas12/users/$LOGNAME/reheat/rgk_fa18_6.5GeV -i /cache/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/train/skim1
bin/qtl reheat -m rollover -c 13.3.0 rgk_fa18_7.5GeV -o /volatile/clas12/users/$LOGNAME/reheat/rgk_fa18_7.5GeV -i /cache/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/train/skim1
```
3. check the results:
```bash
# before
for f in /cache/clas12/rg-k/production/recon/fall2018/torus+1/6535MeV/pass2/v0/dst/train/skim1/*.hipo; do bin/qtl xcharge -m charge -i $f -o test_charge_rgk_6.5GeV -s original; done
for f in /cache/clas12/rg-k/production/recon/fall2018/torus+1/7546MeV/pass2/v0/dst/train/skim1/*.hipo; do bin/qtl xcharge -m charge -i $f -o test_charge_rgk_7.5GeV -s original; done
# after
for f in /volatile/clas12/users/$LOGNAME/reheat/rgk_fa18_6.5GeV/*.hipo; do bin/qtl xcharge -m charge -i $f -o test_charge_rgk_6.5GeV -s reheated; done
for f in /volatile/clas12/users/$LOGNAME/reheat/rgk_fa18_7.5GeV/*.hipo; do bin/qtl xcharge -m charge -i $f -o test_charge_rgk_7.5GeV -s reheated; done
```


## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

```bash
qtl histogram -d rgk_fa18_6.5GeV --flatdir --focus-physics /volatile/clas12/users/$LOGNAME/reheat/rgk_fa18_6.5GeV
qtl histogram -d rgk_fa18_7.5GeV --flatdir --focus-physics /volatile/clas12/users/$LOGNAME/reheat/rgk_fa18_7.5GeV
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring or reheating, be sure to cross check the output runs with those from `/mss`

## Make timelines

> [!IMPORTANT]
> Pass 2 run 5863 was mistakenly cooked into the 6.5 GeV, but it has beam energy 7.5 GeV. To make sure its QA
> is performed in the correct epoch, move its `outfiles/` files to the correct output dataset directory:
> ```bash
> mv -v outfiles/rgk_fa18_6.5GeV/timeline_physics/5863 outfiles/rgk_fa18_7.5GeV/timeline_physics/
> ```

Make the timelines, separately for each of the 2 datasets:
```bash
bin/qtl physics -d rgk_fa18_6.5GeV -p rgk/pass2/qa/fa18_6.5GeV
bin/qtl physics -d rgk_fa18_7.5GeV -p rgk/pass2/qa/fa18_7.5GeV
```

> [!WARNING]
> You may not have permission to publish to this directory; if not, publish to another place and ask the chef to copy the files
