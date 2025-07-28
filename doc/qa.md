# Automatic QA

<details>
<summary>start a new notes file for this dataset</summary>

- notes files are stored in the [`qadb/notes/` directory](/qadb/notes)
- must contain _explicit_ commands used, for reproducibility purposes
- choose a unique dataset name; we will use `$dataset` in the example commands below, for this unique name
- include any other information specific to these data
- keep it up-to-date as the automatic _and_ manual QA proceeds
- suggestion: copy one of the others, and start from there
</details>

<details>
<summary>choose input data files</summary>

- decide whether to analyze full DSTs or specific train(s)
- do you need to combine data from various targets?
    - if so, you can combine them into your `$dataset` using "step 1" (`qtl histogram`); see RG-C notes files for examples
</details>


<details>
<summary>make sure all data are cached</summary>

- all data files _must_ be on `/cache`
- use `qtl histogram` with the `--check-cache` option (see [timeline-production procedure](procedure.md) for details of `qtl`)
    - this will cross check the list of files on `/cache` with the list of stub files on `/mss`
    - if not all data are on `/cache`, this command will generate a `jcache` script
      - run it and wait
      - use `jcache pendingRequest -u $LOGNAME` to monitor progress
      - run `qtl histogram --check-cache` again, when done, in case additional files were auto-removed from `/cache` during your `jcache` run
</details>

<details>
<summary>produce histogram files (step 1)</summary>

- this is "step 1" of the [timeline-production procedure](procedure.md)
    - see also other [notes files](/qadb/notes) for examples
- use the same `qtl histogram` command, but without the `--check-cache` argument
- the jobs will run on Slurm
    - be sure to monitor the output log and error files, in case something goes wrong
    - any warnings or errors should _not_ be ignored
    - all of the data must be analyzed _successfully_
</details>

<details>
<summary>double check one more time that all the runs were analyzed</summary>

- cross check the output directory (likely `outfiles/$dataset`) with data on `/mss`
- the QADB should print warnings if an analyzer tries to use the QADB on data that are not available in the QADB, but we do not want that to happen
</details>

<details>
<summary>produce initial timelines (step 2)</summary>

- this is "step 2" of the [timeline-production procedure](procedure.md)
    - see also other [notes files](/qadb/notes) for examples
- you may need to publish to your "personal" timeline directory, if you do not have write permissions to the run group's timeline directory
    - for example, use `-p $LOGNAME/$dataset`
- any warnings or errors should _not_ be ignored
</details>

<details>
<summary>QA cut tuning 1: choose the epochs</summary>

- the average normalized electron yields (N/F) will jump occasionally in a dataset; for example, we often have jumps when:
    - trigger configuration changes
    - target changes
- to establish QA cut lines, we need to first establish epochs
- start the file `epochs.$dataset.txt` in the [`qadb/epochs/`](/qadb/epochs) directory, which is a list epoch boundary lines
    - each line should contain two numbers: the first and last runs of the epoch
    - a comment is allowed, using `#` (as in Python); this can be used to _describe_ why an epoch was needed
- to help determine epochs, execute [`qadb/draw_epochs.sh`](/qadb/draw_epochs.sh); note that it requires ROOT
    - this script will build a `ROOT` tree and draw N/F vs. run number, along with the current epoch boundary lines (if defined)
    - look at N/F and identify where the average value "jumps": this typically occurs at the same time for all 6 sectors, but you should check all 6 regardless
- after defining epochs, re-produce timelines (re-run step 2)
    - now check the QA timeline "epoch view" in the extra (expert) timelines
        - this is a timeline used to evaluate how the QA cuts look overall, for each epoch
        - the timeline itself is just a list of the 6 sectors; clicking on one of them will show plots of N/F, N, F, and livetime, for each epoch
        - the horizontal axis of these plots is an index, defined as the run number plus a small offset (<1) proportional to the QA bin
        - the N/F plots include the cut lines: here you can zoom in and see how well-defined the cut lines are for each epoch
            - if there are any significant 'jumps' in the N/F value, the cut lines may be appear to be too wide: this indicates an epoch boundary line needs to be drawn at the step in N/F, or the cut definitions need some adjustments (the next step)
</details>

<details>
<summary>QA cut tuning 2: tune cut definitions and overrides</summary>

- add the file `${dataset}.yaml` in the [`qadb/cutdefs`](/qadb/cutdefs) directory
    - you may copy one of the existing ones, most likely the default one
- tune the settings in this file as needed
    - see [`qa-physics/qaCut.groovy`](/qa-physics/qaCut.groovy) to see how the numbers are used
- re-produce timelines again (re-run step 2) and check the results
</details>

# Manual QA


# Deployment

This checklist is for the QADB repository.

- [ ] update the tables in `README.md`
- [ ] update any internal spreadsheets, tracking QADB progress (they are not in this repository)
- [ ] make a new dataset directory in `qadb/pass[N]/`, where `[N]` is the pass number
- [ ] make or update the symbolic in `qadb/latest/`, to point to the new dataset directory
