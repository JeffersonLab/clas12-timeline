# Physics QA Timeline Production

* See [flowchart documentation](docs/docDiagram.md) for a visual 
  representation of the scripts, input, and output

* See [QADB documentation](/doc/qa.md) for how to produce a QADB

# Automatic QA Details

* `../bin/qtl physics $dataset`, which does the following:
  * runs `datasetOrganize.sh`
  * runs `qaPlot.groovy` (on FD and FT)
  * runs `qaCut.groovy` (on FD and FT)
  * runs `mergeFTandFD.groovy` to combine FD and FT results
  * runs `monitorPlot.groovy`
  * copies timelines to output timeline directory
    using `stageTimelines.sh`
  * if any of these scripts throw errors, they will be redirected and printed at the end
    * if you see any errors for a particular script, you may re-run it individually
      to diagnose the problem (the full command for each script is printed in the output)

* take a look at the "time bin analysis" plots by running `timebin_analysis/timebin_plot.C`

### Input files
* DST or skim files
  * DST files are assumed to be organized into directories, with one directory
    corresponding to one run
  * Skim files are assumed to be contained in a single directory, with one skim file
    corresponding to one run

### DST / Skim reading
First step is to read DST or Skim files, producing HIPO files and data tables

* `monitorRead.groovy`
  * run with no arguments for usage guide
  * It is better to run this using `slurm`, but this can be run on a single skim file or
    directory of one run's DST files
  * Outputs:
    * `[output_directory]/data_table_${run}.dat`, which is a data table with the following columns:
      * run number
      * time bin number
      * minimum event number
      * maximum event number
      * minimum timestamp
      * maximum timestamp
      * sector
      * number of electron triggers in this sector
      * number of electrons in the forward tagger
      * DAQ-gated FC charge at beginning of time bin
      * DAQ-gated FC charge at end of time bin
      * DAQ-ungated FC charge at beginning of time bin
      * DAQ-ungated FC charge at end of time bin
      * average livetime
    * monitoring HIPO file, `[output_directory]/monitor_${runnum}.hipo`, contains several plots 
      for each time bin

### Data Organization
* use the script `datasetOrganize.sh`
  * this will concatenate `dat` files from the input directory into a single file
  * it will also generate symlinks to the relevant monitoring HIPO files

### Plotting Scripts
* `monitorPlot.groovy`
  * this will read monitoring HIPO files and produce several timelines
    * Let X represent a quantity plotted in a timeline; the timeline plots the
      average value of X, versus run number
      * Several variables may be plotted together, on the same timeline plot,
        sharing the same vertical axis numbers, despite possibly having
        different units, for example,
        * Units of energy or momentum are GeV
        * Units of angle are radians
      * Clicking on a point will draw several plots below the timeline,
        corresponding to that run:
        * distribution of the average value of X, with one entry per time bin
        * graph of the average value of X versus time bin number

* `qaPlot.groovy` 
  * reads data table and generates `monitorElec.hipo`
    * within this HIPO file, there is one directory for each run, containing several
      plots:
      * `grA*`: N/F vs. time bin
      * `grF*`: F vs. time bin
      * `grN*`: N vs. time bin
      * `grT*`: livetime vs. time bin

* `qaCut.groovy`
  * reads `monitorElec.hipo`, along with `../qadb/epochs/epochs.${dataset}.txt`, to build
    timelines for the online monitor
  * if `$useFT` is set, it will use FT electrons instead
  * the runs are organized into epochs, wherein each:
    * calculate N/F quartiles
      * `mq`: middle quartile, the overall median N/F
      * `lq`: lower quartile, the median N/F below `mq`
      * `uq`: upper quartile, the median N/F above `mq`
    * QA cut lines are set using an interquartile range (IQR) rule: `cutFactor` * IQR,
      where `cutFactor` adjusts the overall width of the cuts
      * this is done for each sector individually
      * results are stored in `qaTree.json`
      * timelines HIPO files are also generated (which can be uploaded to the web server):

## Manual QA procedure
This procedure is to be performed after the automatic QA, on a _fully_ cooked dataset; it is called "manual
QA" because it requires substantially more user interaction, carefully checking
the timelines and recording features not identified by the automatic QA in
`qaTree.json`
* first, make sure you have an automatically generated `qaTree.json`
  * verify your epoch lines are where you want them
  * if you make changes to the epoch lines, re-run `../bin/qtl physics` to
    generate the updated `qaTree.json`
* verify all the data have been analyzed by the automatic QA
  * execute `getListOfDSTs.sh [dataset]` to obtain a list of run numbers and file
    numbers from the DST file directory; this script takes some time to run
  * execute `integrityCheck.sh [dataset]` to compare the list of DST files
    to those which appear in `data_table.dat`; if any DST files
    do not appear in `data_table.dat`, the check will fail, the missing files
    will be printed, and it is recommended to re-run the automatic QA, either for
    those specific runs, or in its entirety
* `cd QA`; this subdirectory contains code for the "manual QA"
* `import.sh` to import the automatically generated `qaTree.json`; this
  will also generate a human-readable file, `qa/qaTable.dat`  
  * append the option `-cnds=user_comment` to copy the user comment from RCDB
    to `qa/qaTable.dat`, which is helpful for the manual QA procedure
* open `qa/qaTable.dat` in another window; this is the human-readable version of
  the imported `qaTree.json`
* now scan through `qaTable.dat`, inspecting each run:
  * Use `modify.sh` if you need to make revisions to `qaTree.json` and `qaTable.dat`
    * this script allows for easy revision of the imported `qaTree.json`; it will also
      update `qaTable.dat`, so it is useful to have `qaTable.dat` open in a window
      which will auto-refresh whenever you call `modify.sh`
    * type `modify.sh` without any arguments for the most up-to-date usage documentation
    * the first argument is the modification you want to make, whether it
      is to add a defect bit, delete a defect bit, add a comment, etc.
      * call `modify.sh` with one of these arguments for additional documentation specific
        to this argument, e.g., `modify.sh addComment`
      * the subsequent arguments are typically the run number, the range of
        files, and sectors, but depends on the revision you are making
    * if you make a mistake, call `undo.sh` to revert `qaTree.json` and
      `qaTable.dat` to the previous version; in fact, every time you call
      `modify.sh`, a backup copy of `qaTree.json`, before the revision, is
      stored
  * recommended procedures and checks:
    * use [`clas12mon`](https://clas12mon.jlab.org/rga/runs/table/) table to
      look for useful comments; it is helpful to copy/paraphrase any
      data-quality-relevant comments to the comment field using `modify.sh
      addComment ...`; for more information check the electronic logbook by
      clicking on a run
    * scan through `qaTable.dat` looking for anything interesting; pay close attention
      if the `clas12mon` table has any comments
    * there are some cases where the automatic QA result is not sufficient:
      * if you find a string of consecutive outliers, maybe it is a sector loss;
        to define a sector loss period: use `modify.sh sectorLoss ...`
      * mark all files in a run with `Misc` bit for special cases which are best
        summarized in a comment; use `modify.sh addBit ...` to set the `Misc` bit
        (it will prompt you for a comment)
      * some automatic QA defect bit assignments may be incorrect for some edge cases;
        you may need to correct these using `modify.sh` bit commands (`addbit`, `delbit`, _etc._)
  * you should also look through each timeline for any issues that may have slipped under
    the radar; revise `qaTree.json` using `modify.sh` as needed
    * check `stddev` timelines; usually a high standard deviation indicates a
      step or change in the data, or merely a short, low statistics run
    * check fraction of events with defined helicity; if it's low it could
      indicate a problem; so far in all cases we have checked and there are no
      issues with the reported beam spin asymmetry, but it is useful to
      document these cases with the Misc defect bit
  * if you are producing QADB for a _re-cooked_ dataset, _i.e._ Pass 2 or greater, cross
    check the QA results with the previous Pass(es), paying special attention to the
    `Misc` defect bit
  * if the run group provides a table of runs with comments, _e.g._, a table of "good" or
    "golden" runs, cross check that table with the QADB
* after scanning through `qaTable.dat` and revising `qaTree.json`, return to the parent
directory and call `exeQAtimelines.sh` to produce the updated QA timelines
  * it copies the revised`qaTree.json` (`qa.${dataset}/qaTree.json`) to
    the new QA timeline directory, which can then be deployed to the web servers
  * this final `qaTree.json` is stored in the 
    [`clas12-qadb` repository](https://github.com/JeffersonLab/clas12-qadb)
    and should be copied there, along with `chargeTree.json`

