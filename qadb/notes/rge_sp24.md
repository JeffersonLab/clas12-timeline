# Run Group E, Spring 2024, Pass 1

There is no SIDIS-like train, so let's use DST files for the QA.

Similarly to RG-C, there are several targets that we'll need to combine; let's also combine torus polarities.

The DSTs are found in:
```
/cache/clas12/rg-e/production/spring2024/pass1/*/*/dst/recon
```

## Reheat

**TODO**:
- [ ] check if needed; see Fall 2018 RG-K notes
- [ ] if reheating is needed, fix `qtl histogram` paths below

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

check cache:
```bash
bin/qtl histogram -d rge_sp24 --check-cache --flatdir --focus-physics $(ls -d /cache/clas12/rg-e/production/spring2024/pass1/*/*/dst/recon)
```

run:
```bash
bin/qtl histogram -d rge_sp24 --flatdir --focus-physics $(ls -d /cache/clas12/rg-e/production/spring2024/pass1/*/*/dst/recon)
```

## Double check that we have all the runs

> [!IMPORTANT]
> In case any runs disappeared from `/cache` while running monitoring or reheating, be sure to cross check the output runs with those from `/mss`

## Make timelines

**TODO**:
- [ ] populate `/data/metadata` for RG-E's target types

Make the timelines:
```bash
bin/qtl physics -d rge_sp24 -p rgk/qa_pass1_sp24
```

> [!WARNING]
> You may not have permission to publish to this directory; if not, publish to another place and ask the chef to copy the files
