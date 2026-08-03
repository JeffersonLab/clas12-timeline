# Run Group K, Fall 2023 and Spring 2024, Pass 1

We will use DST files from 3 datasets; we will keep them separate, to be consistent with what we did for Fall 2018 RG-K data:

| Time        | Beam Energy | Dataset Name      | Data Path                                                                |
| ---         | ---         | ---               | ---                                                                      |
| Fall 2023   | 6.4 GeV     | `rgk_fa23_6.4GeV` | `/cache/clas12/rg-k/production/recon/fall2023/pass1/6395MeV/dst/recon`   |
| Spring 2024 | 6.4 GeV     | `rgk_sp24_6.4GeV` | `/cache/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/recon` |
| Spring 2024 | 8.5 GeV     | `rgk_sp24_8.5GeV` | `/cache/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/recon` |

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

check cache:
```bash
bin/qtl histogram -d rgk_fa23_6.4GeV --dstdir --check-cache --focus-physics /cache/clas12/rg-k/production/recon/fall2023/pass1/6395MeV/dst/recon
bin/qtl histogram -d rgk_sp24_6.4GeV --dstdir --check-cache --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/recon
bin/qtl histogram -d rgk_sp24_8.5GeV --dstdir --check-cache --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/recon
```

run:
```bash
bin/qtl histogram -d rgk_fa23_6.4GeV --dstdir --check-charge --fast-ls --submit --focus-physics /cache/clas12/rg-k/production/recon/fall2023/pass1/6395MeV/dst/recon
bin/qtl histogram -d rgk_sp24_6.4GeV --dstdir --check-charge --fast-ls --submit --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/recon
bin/qtl histogram -d rgk_sp24_8.5GeV --dstdir --check-charge --fast-ls --submit --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/recon
```

## Checks
- [ ] Inspect the charge plots, to see if we need to reheat
- [ ] Double check that we have _all_ the runs, and that all DSTs are still there, in case any disappeared from `/cache` _while_ jobs were running

## Make timelines

Make the timelines, separately for each of the 2 datasets:
```bash
bin/qtl physics -d rgk_fa23_6.4GeV -p rgk/pass1/qa/rgk_fa23_6.4GeV
bin/qtl physics -d rgk_sp24_6.4GeV -p rgk/pass1/qa/rgk_sp24_6.4GeV
bin/qtl physics -d rgk_sp24_8.5GeV -p rgk/pass1/qa/rgk_sp24_8.5GeV
```

> [!WARNING]
> You may not have permission to publish to this directory; if not, publish to another place and ask the chef to copy the files
