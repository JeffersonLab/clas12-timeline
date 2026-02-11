# Run Group A, Spring 2018, Pass 1

Before anything, cross check the train and DST run lists:
```bash
# 10.6 GeV data
bin/qtl xtrain /mss/clas12/rg-a/production/recon/spring2018/10.59gev/torus+1/pass1/dst/train/nSidis /mss/clas12/rg-a/production/recon/spring2018/10.59gev/torus+1/pass1/dst/recon
bin/qtl xtrain /mss/clas12/rg-a/production/recon/spring2018/10.59gev/torus-1/pass1/dst/train/nSidis /mss/clas12/rg-a/production/recon/spring2018/10.59gev/torus-1/pass1/dst/recon
# 6.4 GeV data
bin/qtl xtrain /mss/clas12/rg-a/production/recon/spring2018/6.42gev/torus+1/pass1/dst/train/nSidis    /mss/clas12/rg-a/production/recon/spring2018/6.42gev/torus+1/pass1/dst/recon
bin/qtl xtrain /mss/clas12/rg-a/production/recon/spring2018/6.42gev/torus-1/pass1/dst/train/nSidis    /mss/clas12/rg-a/production/recon/spring2018/6.42gev/torus-1/pass1/dst/recon
bin/qtl xtrain /mss/clas12/rg-a/production/recon/spring2018/6.42gev/torus+0.75/pass1/dst/train/nSidis /mss/clas12/rg-a/production/recon/spring2018/6.42gev/torus+0.75/pass1/dst/recon
bin/qtl xtrain /mss/clas12/rg-a/production/recon/spring2018/6.42gev/torus-0.75/pass1/dst/train/nSidis /mss/clas12/rg-a/production/recon/spring2018/6.42gev/torus-0.75/pass1/dst/recon
```

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

We decided to reheat only the `nSidis` train, and store the result on `/volatile`; here are the commands.

Note that the 10.6 GeV data and 6.4 GeV data have some differences in how they were cooked.
In particular, the `recharge` option was `false` for 10.6 GeV data, and `true` for 6.4 GeV data. Here is a comparison of the `README.json` files

| Key           | 10.6 GeV torus=-1 | 10.6 GeV torus=+1 | 6.4 GeV torus=-1 | 6.4 GeV torus=+1 | 6.4 GeV torus=-0.75 | 6.4 GeV torus=+0.75 |
| ---           | ---               | ---               | ---              | ---              | ---                 | ---                 |
| recharge      | false             | false             | true             | true             | true                | true                |
| model         | ana               | recana            | decrecana        | decrecana        | decrecana           | decrecana           |
| denoise       | true              | true              | 4.0.1            | 4.0.1            | 4.0.1               | 4.0.1               |
| has reconYaml | false             | true              | true             | true             | true                | true                |
| coatjava      | 11.1.1            | 11.1.1            | 11.1.1           | 11.1.1           | 11.1.1              | 11.1.1              |

The files are from:
```
/cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus-1/pass1/dst/recon/README.json
/cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus+1/pass1/dst/recon/README.json
/cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus+1/pass1/dst/recon/README.json
/cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus-0.75/pass1/dst/recon/README.json
/cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus-1/pass1/dst/recon/README.json
/cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus+0.75/pass1/dst/recon/README.json
```

### Reheat procedure

1. make sure all data are on `/cache`; re-cache them if necessary:
```bash
# 10.6 GeV data
bin/qtl histogram -d rga_sp18_outbending_nSidis --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus+1/pass1/dst/train/nSidis
bin/qtl histogram -d rga_sp18_inbending_nSidis  --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus-1/pass1/dst/train/nSidis
# 6.4 GeV data
bin/qtl histogram -d rga_sp18_6.4GeV_outbending_nSidis    --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus+1/pass1/dst/train/nSidis
bin/qtl histogram -d rga_sp18_6.4GeV_inbending_nSidis     --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus-1/pass1/dst/train/nSidis
bin/qtl histogram -d rga_sp18_6.4GeV_outbending_lo_nSidis --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus+0.75/pass1/dst/train/nSidis
bin/qtl histogram -d rga_sp18_6.4GeV_inbending_lo_nSidis  --check-cache --flatdir --focus-physics /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus-0.75/pass1/dst/train/nSidis
```

2. run reheat:
```bash
# 10.6 GeV data
bin/qtl reheat -c rollover -d rga_sp18_outbending_nSidis -o /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_outbending_nSidis -i /cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus+1/pass1/dst/train/nSidis
bin/qtl reheat -c rollover -d rga_sp18_inbending_nSidis  -o /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_inbending_nSidis  -i /cache/clas12/rg-a/production/recon/spring2018/10.59gev/torus-1/pass1/dst/train/nSidis
# 6.4 GeV data
bin/qtl reheat -c rollover -d rga_sp18_6.4GeV_outbending_nSidis    -o /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_6.4GeV_outbending_nSidis    -i /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus+1/pass1/dst/train/nSidis
bin/qtl reheat -c rollover -d rga_sp18_6.4GeV_inbending_nSidis     -o /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_6.4GeV_inbending_nSidis     -i /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus-1/pass1/dst/train/nSidis
bin/qtl reheat -c rollover -d rga_sp18_6.4GeV_outbending_lo_nSidis -o /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_6.4GeV_outbending_lo_nSidis -i /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus+0.75/pass1/dst/train/nSidis
bin/qtl reheat -c rollover -d rga_sp18_6.4GeV_inbending_lo_nSidis  -o /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_6.4GeV_inbending_lo_nSidis  -i /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus-0.75/pass1/dst/train/nSidis
```

3. check the results on some runs; see [`qa-physics/charge_analysis/README.md`](/qa-physics/charge_analysis/README.md); for example:
```bash
cd qa-physics/charge_analysis

# before reheat
for f in /cache/clas12/rg-a/production/recon/spring2018/6.42gev/torus+1/pass1/dst/train/nSidis/*.hipo; do ./analyze.py $f before; done

# after reheat
for f in ~/v/reheat/rga_sp18_6.4GeV_outbending_nSidis/*.hipo; do ./analyze.py $f after; done
```

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`.

> [!NOTE]
> - for 10.6 GeV data, we kept inbending and outbending data separate, since we started producing the QADB for one while the other was still cooking
> - for 6.4 GeV data, all data were cooked prior to starting QADB, so we combine all of it into one dataset with this step here

```bash
# 10.6 GeV data
bin/qtl histogram -d rga_sp18_outbending_nSidis --submit --flatdir --focus-physics /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_outbending_nSidis
bin/qtl histogram -d rga_sp18_inbending_nSidis  --submit --flatdir --focus-physics /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_inbending_nSidis
# 6.4 GeV data
bin/qtl histogram -d rga_sp18_6.4GeV_nSidis --submit --flatdir --focus-physics    \
  /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_6.4GeV_outbending_nSidis    \
  /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_6.4GeV_inbending_nSidis     \
  /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_6.4GeV_outbending_lo_nSidis \
  /volatile/clas12/users/$LOGNAME/reheat/rga_sp18_6.4GeV_inbending_lo_nSidis
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring, be sure to cross check the output
> runs with those from `/mss`

## Make timelines

Make the timelines:
```bash
# 10.6 GeV data
bin/qtl physics -d rga_sp18_outbending_nSidis -p rga/pass2/sp18/qa
bin/qtl physics -d rga_sp18_inbending_nSidis -p rga/pass2/sp18/qa
# 6.4 GeV data
bin/qtl physics -d rga_sp18_6.4GeV_nSidis -p rga/pass2/sp18/qa
```
