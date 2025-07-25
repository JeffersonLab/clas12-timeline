# Automatic QA

- [ ] start a new note file in [`qadb/notes`] for this dataset
    - must contain _explicit_ commands used, for reproducibility purposes
    - include any other information specific to these data
    - keep it up-to-date as the automatic _and_ manual QA proceeds
    - suggestion: copy one of the others, and start from there
- [ ] decide on input data:
    - full DSTs or trains?
    - need to combine data from various targets (see, _e.g._, RG-C notes)?
- [ ] make sure all data are cached, _i.e._, on `/cache`
    - use `qtl histogram --check-cache`
    - if not all data are on `/cache`, this will fail, and a `jcache` script will be generated
      - run it and wait
      - use `jcache pendingRequest -u $LOGNAME` to monitor progress
      - run `qtl histogram --check-cache` again, when done, in case additional files were auto-removed from `/cache` during your `jcache` run
- [ ] produce `histograms` files
    - use `qtl histogram` without the `--check-cache` argument
- [ ] double check one more time that all the runs were analyzed
    - cross check the output directory (likely `outfiles/$dataset`) with data on `/mss`

# Manual QA


# Deployment

This checklist is for the QADB repository.

- [ ] update the tables in `README.md`
- [ ] update any internal spreadsheets, tracking QADB progress (they are not in this repository)
- [ ] make a new dataset directory in `qadb/pass[N]/`, where `[N]` is the pass number
- [ ] make or update the symbolic in `qadb/latest/`, to point to the new dataset directory
