# QADB Production

The QADB (Quality Assurance Database) for a dataset is produced as an additional product of the timelines.
This document contains the **full procedure** for QADB production; all checklists must be followed.

> [!NOTE]
> - Final versions of the QADB files are stored in the [QADB Repository](https://github.com/JeffersonLab/clas12-qadb)
> - The [`qadb` directory in this timeline repository](/qadb) contains tools for producing and refining QADB files

# Procedures and Checklists

The following **checklists** must be followed for any QADB production.
- you may copy and paste the checklist elsewhere, so you may keep track of your progress
- click each item to expand the details

## :zap: Automatic QA :zap:

The [timeline code](..) produces an initial QADB as a byproduct. This initial version automatically assigns several defect bits,
therefore this procedure is called the "automatic QA".

### :ballot_box_with_check: Checklist

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
- alternatively, create a "prescaled" train
    - this is **deprecated**, but here if you need it
    - use the scripts in the [`prescaler/` directory](/qadb/prescaler)
</details>

<details>
<summary>make sure all data are cached</summary>

- all data files _must_ be on `/cache`
- use `qtl histogram` with the `--check-cache` option (see [timeline-production procedure](procedure.md) for details of `qtl`)
    - use the `--flatdir` option if you are analyzing trains (most likely)
    - this will cross check the list of files on `/cache` with the list of stub files on `/mss`
    - if not all data are on `/cache`, this command will generate a `jcache` script
      - run it and wait
      - use `jcache pendingRequest -u $LOGNAME` to monitor progress
      - run `qtl histogram --check-cache` again, when done, in case additional files were auto-removed from `/cache` during your `jcache` run
</details>

<details>
<summary>verify run-dependent settings are correct for these data</summary>

- the script [`monitorRead.groovy`](/qa-physics/monitorRead.groovy) contains some run-dependent settings
- make sure they are correct for these data
- you may need to produce timelines first, and come back to this step after making changes, for example, if the Faraday Cup (FC) charge is incorrect
- in particular:
    - set `FCmode`, to specify how to calculate the FC charge
        - for example, this depends on whether the data needed to be cooked with the recharge option ON or OFF (see `README.json`, typically included with the cooked data)
        - note that the `FCmode` is NOT determined from the recharge setting, but instead from which charge values in the data we can use
        - if you find that the DAQ-gated FC charge is larger than the ungated charge, you may have assumed here that the recharge option was ON, when actually it was OFF and needs to be ON
        - additional `FCmode` settings are used for certain special cases; see the `monitorRead.groovy` script comments for more information
</details>

<details>
<summary>produce histogram files (step 1)</summary>

- this is "step 1" of the [timeline-production procedure](procedure.md)
    - see also other [notes files](/qadb/notes) for examples
- use the same `qtl histogram` command, but without the `--check-cache` argument
    - use the `--flatdir` option if you are analyzing trains (most likely)
- the jobs will run on Slurm
    - be sure to monitor the output log and error files, in case something goes wrong; you may use `qtl error` to help with this
    - any warnings or errors should _not_ be ignored
    - all of the data must be analyzed _successfully_
</details>

<details>
<summary>make sure the beam energy from step 1 was correct</summary>

- we have had cases in the past where the beam energy from RCDB was incorrect
- either have RCDB corrected (preferred), or correct the beam energy yourself (not preferred)
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

<details>
<summary>start a pull request in the QADB repository</summary>

- create a new `git` branch in [the QADB repository](https://github.com/JeffersonLab/clas12-qadb)
- make a new QADB dataset directory in `qadb/pass[N]/`, where `[N]` is the pass number
- make or update the symbolic in `qadb/latest/`, to point to the new dataset directory
- copy the relevant QADB files to this directory
    - the automatic QA procedure above produced these files to an "output" directory; by default
      it is `outfiles/$dataset`
    - QADB files are within this output directory, in `timeline_physics_qa/outdat/qaTree.json`
    - copy the files `qaTree.json` and `chargeTree.json` to the new QADB repository directory
- commit and push the changes, and start a draft pull request
    - this _preserves_ the initial version of the repository, before we start making changes
    - you may use this pull request to push additional changes to the QADB, as the manual QA procedure
      proceeds, but _this version_ is the one that we want to use to initiate the independent cross check
      of the manual QA
</details>

<details>
<summary>double check one more time that all the runs were analyzed</summary>

- cross check QADB's runs with data on `/mss`
- the QADB should print warnings if an analyzer tries to use the QADB on data that are not available in the QADB, but we do not want that to happen
</details>

<details>
<summary>send the QADB to the cross checker</summary>

- we require a cross check of the manual QA results, for all fully cooked datasets; the procedure
  is in the next section
- send the above _initial_ version of the QADB file, `qaTree.json`, to the cross checker
</details>

## :clipboard: Manual QA :clipboard:

This procedure follows up on the "automatic QA" above, further refining the QADB; substantial
user interaction is required, along with careful checks of the timelines and logbook, therefore
we call this procedure the "manual QA".

Two people must perform this procedure, independently, and the results must be cross checked.

All steps this procedure should be performed with in the [`qadb/`](/qadb) subdirectory;
there is [additional documentation](/qadb/README.md) within `qadb/`, for more detailed guidance.

### :ballot_box_with_check: Checklist

<details>
<summary>import the initial QADB</summary>

- run `./import.sh` with:
    - first with no arguments, for usage guidance, then:
    - the dataset name should be `$dataset` (the same as from the automatic QA procedure)
    - the `qaTree.json` file from the automatic QA procedure
        - if you are a cross checker, you likely have been given this file directly
</details>

<details>
<summary>open the table file in another window or text editor</summary>

- the file `qa/qaTree.json.table` is a human-readable version of the QADB
- open it in a separate window or text editor
    - tip: use a text editor that automatically updates the file view, since the next steps
      will _modify_ the file
    - do _not_ edit this file, since it will be _overwritten_ as the QA proceeds
</details>

<details>
<summary>open the QA timelines in your browser</summary>

- the QA timelines (produced by the above automatic QA procedure), should also be open on your computer
- clicking on a run's point(s) will:
    - draw several plots below
        - some plots will have cut lines shown
        - some points on those plots are colored red, since they have defects identified by the automatic QA
    - add the run to a small table (under the main timeline)
        - the columns come from the [`clas12mon` run table](https://clas12mon.jlab.org/rga/runs/table/)
        - clicking on a row of that table will take you to the electronic log book, with
            - a plot of the beam current versus time
            - shift summary log entries, with this run highlighted (sometimes you may have to go to the
              actual logbook and dig around, to find more information about a run)
</details>

<details>
<summary>scan the table file for non-standard running conditions</summary>

- warning: this step takes a _significant_ amount of time and is rather _tedious_
    - you need attention to detail
    - take frequent breaks, if you have to
- the following scripts are used here:
    - `./modify.sh` to modify the QADB, usually to assign defect bits
    - `./undo.sh` to undo a `./modify.sh` call, in case you make a mistake
- scroll through this file, looking at each run and its QA bins; here are some things to look for:
    - check the `user_comment`, which is the Shift Expert's comment (entered at the beginning
      and/or end of each DAQ run)
        - most normal runs say something like "production"
        - non-production runs, or runs with issues, are often identified by this comment,
          but _not always_
        - check the log book as well
        - sometimes this comment is _wrong_, or refers to the previous run
    - if you find a region of several outliers, and the `PossiblyNoBeam` defect is also not set
      for this region:
        - take a look at the timelines and log book, to find out what's wrong
        - sporadic outliers here and there are normal
        - many consecutive outliers, which happen when there is beam (_i.e._, `PossiblyNoBeam` is not set), is
          not normal and typically indicates either an issue or a non-standard run (_e.g._, low luminosity or
          empty target)
        - in some cases, a single sector will have several consecutive outliers for the remainder of a run; this
          is called a "sector loss" and we typically manually assign the `SectorLoss` defect bit by using
          `./modify.sh sectorloss`
    - if you find a short run (_i.e._, not many QA bins), take a look at the log book to find out why
        - sometimes short runs had issues
        - other times, the accelerator had a problem and the run was ended since significant downtime was expected,
          that is, the data are fine
    - sometimes you may find the automatic assignment of certain defect bits is "wrong"
        - in this case, you are permitted to _correct_ the assignment manually
        - for example, if the beam was _not available_ for most of a run, you may find `ChargeHigh` assigned to the
          "good" part of the run, since indeed the "good" charge is an "outlier" compared to the majority of the
          run where the charge was (nearly) zero
        - if you find _frequent_ mistakes from the automatic QA assignment, stop doing the manual QA and fix
          the problem upstream (if you are the cross checker, ask the QADB maintainer(s) to do this)
    - we recommend you take a look at every run in the log book, just to be safe
- for _anything_ that you observe, whether it is an issue or a non-standard (non-production) run, please assign
  the `Misc` defect bit
    - use `./modify.sh misc` to do this
    - be sure to only assign it to the relevant bins
        - typically we assign `Misc` to entire runs, but not always
        - in some cases, we also restrict `Misc` to specific FD sectors
    - be sure to include a comment about _why_ you assigned the `Misc` bit
        - the default comment just copies the `user_comment` (Shift Expert's comment), for convenience
        - you may need to _correct_ the `user_comment`, or provide more details from what you find in the logbook
    - if you make any mistake, use `./undo.sh` to revert your previous `./modify.sh` run
- once you are done this long procedure, please make a backup of your `qaTree.json` file
</details>

<details>
<summary>scan the timelines for anything else you may have missed</summary>

- this step is much faster than scanning through the table file, but still requires careful attention to detail
- this step shifts the focus to the _timeline_ plots, rather than the _table_ file, to see if anything slipped under the radar
- as before, use `./modify.sh` to make changes
- in particular:
    - check standard-deviation-type timelines
        - usually a high standard deviation indicates a step or change in the data, or merely a short, low statistics run
        - sometimes it indicates a problem (that you likely already caught while scanning through the table file)
    - check the beam spin asymmetry
        - the automatic QA typically handles this timeline pretty well, but it is wise to take a look at this timeline anyway
        - the $\pi^+$ beam spin asymmetry "amplitude" is expected to be around +2%
            - if the sign is wrong, the helicity sign is wrong, and the automatic QA should have assigned the `BSAWrong` defect bit
            - the $\pi^-$ asymmetry is too small, so we focus on the $\pi^+$
        - the asymmetry "offset" is included in the fit, for cases when the target was polarized
            - jumps in the offset often happen when the target type or polarization changes
            - we typically do _not_ use this for QA purposes, but the parameter is needed in the asymmetry fit to correctly get the "amplitude"
    - check fraction of events with defined helicity
        - if it's relatively low, it could indicate a problem; please assign the `Misc` bit
        - typically this fraction is around 99%
        - check the beam spin asymmetry for such cases
        - so far in all cases we have checked and there are no issues with the reported beam spin asymmetry,
          but it is useful to document these cases with the `Misc` defect bit anyway
    - kinematics distributions
        - average kinematics should be relatively constant, but they may change sometimes
        - for example, the pion average $\phi_h$ may change if the solenoid polarity changes
        - if you see something suspicious, either assign the `Misc` defect bit, check the logbook, or ask the Run Group for more information
</details>

<details>
<summary>if these data are a "Pass 2" or higher, cross check with the previous Pass's QADB</summary>

- to remain unbiased, you should have _not_ looked at the previous Pass's QADB yet; in any case, cross check
  your new QADB with the old QADB, in case you missed anything
    - pay close attention to the `Misc` defect bit assignments and comments
- use `./modify.sh` to make corrections as needed
- do not modify the old Pass's QADB
    - we do not want to "suddenly" change analyzer's results
    - if you _must_ change the old Pass's QADB, be sure this change will be announced to the Run Group
</details>

<details>
<summary>if the Run Group provides a table of runs and notes about them, cross check it with the QADB</summary>

- some Run Groups produce a table (spreadsheet) of runs and notes about each of them
- cross check the QADB, and make changes as necessary
- consider also updating the Run Group's table, though that is the responsibility of the Run Group, not of the QADB maintainers
</details>

<details>
<summary>backup your final version of the QADB</summary>

- make sure your final `qa/qaTree.json` file is duplicated somewhere on another device
- if you are the author of the QADB pull request (see last steps of the automatic QA checklist), this pull request is the
  ideal place for such a backup
</details>

### :warning: Cross Check

After two people have independently finished all steps in the manual QA checklist, you are ready for the cross check.

<details>
<summary>click here for the procedure</summary>

- use `import.sh` to import both versions of the `qaTree.json` file
- open the two `qaTree.json.table` files in a text editor which shows their differences (_e.g._, `vimdiff`)
- the two people should meet and go through the differences, resolving any conflicts with `modify.sh`
- afterward, make sure the final QADB file `qaTree.json` is backed up
</details>

## :sailboat: Deployment :sailboat:

The final step is to _deploy_ the new QADB to the [QADB repository](https://github.com/JeffersonLab/clas12-qadb).
Most of the steps in the following checklist are performed within that repository, on the branch associated
with the pull request you opened earlier.

### :ballot_box_with_check: Checklist

<details>
<summary>copy the final QA timelines to the Run Group's directory</summary>

- you may need to ask the chef to do this
</details>

<details>
<summary>update the tables in 'README.md'</summary>

- link to the timelines
- fill out all the other fields
- if you are deploying a Pass 2 or higher, make sure the previous Passes' status symbols are updated appropriately
</details>

<details>
<summary>make sure the symbolic link in 'qadb/latest' points to the new QADB directory</summary>

- you already did this, but check to make sure
</details>

<details>
<summary>review the pull request, merge, tag a new version, and deploy</summary>

- review the pull request
- merge it
- update the version number in `bin/qadb-info`
- tag a new version and create a new release
    - be sure to install it, _e.g._, on `ifarm` (with a new module file)
    - be sure to announce the new release, especially to the Run Group
</details>
