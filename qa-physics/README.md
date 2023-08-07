# Physics QA Timeline Production
Data monitoring tools for CLAS12 physics-level QA and [QADB](https://github.com/JeffersonLab/clas12-qadb) production

* Tracks the electron trigger count, normalized by the Faraday cup charge
* Also implements helicity monitoring, by tracking inclusive beam spin asymmetries
* Accepts DST or skim files
* See [flowchart documentation](docs/docDiagram.md) for a visual 
  representation of the scripts, input, and output
* The variable `${dataset}` will be used throughout as a name specifying the data set to
  be analyzed; this name is for organization purposes, for those who want to
  monitor several different sets of data

# Setup
0. recommended to use `zsh` or `bash` as your shell; development has been
   done in `zsh`
1. set `COATJAVA` environment
  * on `ifarm`:
    * `source /group/clas12/packages/setup.sh`
    * `module load clas12/pro`
  * local environment:
    * `$COATJAVA` must point to your local install
    * `run-groovy` (likely in `$COATJAVA/bin`) must be in your `$PATH`
2. set local environment variables with `source env.sh`
  * some primary run scripts do this automatically, in case the user forgets
  * note: `JYPATH` is added to the classpath for groovy called via
    `run-groovy`, from `coatjava`
  * the variable `TIMELINEDIR` is the webserver directory to which the output
    hipo files will be copied; this is a directory which the front-end will
    read in order to produce the web page version of the timelines. If you are
    not using this feature, change `TIMELINEDIR` to any local directory; if you
    don't want to edit `env.sh`, then simply create the directory `../www`,
    which is the default value of `TIMELINEDIR`

## PASS1 Procedure for Automatic QA
* prepare run-group dependent settings in `monitorRead.groovy`
  * guess `helFlip`; you can only know if your guess is correct after timelines
    have been produced
  * obtain the beam energies from the `RCDB`; CAUTION: sometimes the `RCDB` is 
    wrong, and it is good to ask for the correct beam energy from the run group
  * set `FCmode`, to specify how to calculate the FC charge
    * this depends on whether the data needed to be cooked with the recharge
      option ON or OFF (see `README.json`, typically included with the cooked
      data)
      * note that the `FCmode` is NOT determined from the recharge setting, but
        instead from which charge values in the data we can use
      * see `monitorRead.groovy` for more details
    * if you find that the DAQ-gated FC charge is larger than the ungated
      charge, you may have assumed here that the recharge option was ON, when
      actually it was OFF and needs to be ON
* `exeSlurm.sh $dataset`: runs `monitorRead.groovy` on DSTs using slurm
  * `$dataset` is specified in `datasetList.txt`, along with a range of runs
    * the syntax of this file is `$dataset $firstRun $lastRun`
    * several scripts use this file; some loop over all datasets, whereas
      others require you to specify which dataset
      * for scripts which loop over all datasets, you can restrict them by
        commenting out lines in `datasetList.txt` (using `#`); currently
        there are no such scripts in use
  * wait for slurm jobs to finish
  * execute `errorPrint.sh` to inspect error logs
  * resubmit failed jobs, e.g., those that exceeded time limit on a slow node:
    * get list of run numbers to be resubmitted: 
      `errorPrint.sh | cut -d':' -f1 | uniq | sed 's/err$/out/g' | xargs grep -i runnum`
    * copy relevant lines of `joblist.${dataset}.slurm` to file `joblist.resubmit.slurm`
    * copy `job.${dataset}.slurm` to `job.resubmit.slurm`, then update the array size
      and `joblist` file name
    * delete logs from previous slurm submission
    * submit `job.${dataset}.slurm` to slurm
      * note: `monitorRead.groovy` will overwrite any partial files left behind by jobs
        which were terminated prematurely, there is no need to delete them prior to
        resubmission
* `exeTimelines.sh $dataset`, which does the following:
  * runs `qaPlot.groovy` (on electron trigger and FT)
  * runs `qaCut.groovy` (on electron trigger and FT)
  * runs `datasetOrganize.sh`
  * runs `monitorPlot.groovy`
  * copies timelines to webserver using `deployTimelines.sh`
  * if any of these scripts throw errors, they will be redirected and printed at the end
    of `exeTimelines.sh`
    * if you see any errors for a script, it's best to rerun that script independently
      to diagnose the problem
* integrity check: check if all available data were analyzed (must be done AFTER
  `exeTimelines.sh`)
  * `getListOfDSTs.sh [dataset]` (takes some time to run)
  * `integrityCheck.sh [dataset]`
* perform the manual QA (see QA procedure below)
* if this is the **FINAL** version of the timeline:
  * release timeline to main run group's directory: use `releaseTimelines.sh`
  * the variable `$TIMELINEDIR` should point to the webserver directory
  * **WARNING:** this is where we store "final" versions of QA timelines; only run
    `releaseTimelines.sh` if you are certain they are ready for release


## Automatic QA Procedure and Script Details
* The automatic QA is executed by `exeSlurm.sh` followed by `exeTimelines.sh`;
  these scripts execute several groovy scripts, which are described in this
  section

### Input files
* DST or skim files
  * DST files are assumed to be organized into directories, with one directory
    corresponding to one run
  * Skim files are assumed to be contained in a single directory, with one skim file
    corresponding to one run
* The file `Tools.groovy` contains several subroutines used by the general monitor
  scripts; depending on your environment, you may need to ensure that this working
  directory is included in the environment variable `$CLASSPATH`
  * One way to do this in `bash` is `export CLASSPATH="${CLASSPATH}:.`, which adds the
    present working directory


### DST / Skim reading
First step is to read DST or Skim files, producing hipo files and data tables

* `groovy monitorRead.groovy __hipo_file_(directory)__ skim(dst)`
  * It is better to run this using `slurm`, but this can be run on a single skim file or
    directory of one run's DST files
    * see `exeSlurm.sh` for example job submission scripts
  * the 2nd argument, `inHipoType` needs to be specified so that determination of run
    number and segment(file) number is done correctly
    * use `dst` for DST files
    * use `skim` for skim files
  * you may want to check the "RUN GROUP DEPENDENT SETTINGS" in the code, to make sure
    certain settings (e.g., beam energy) are correct for your run group
  * Outputs:
    * `outdat/data_table_${run}.dat`, which is a data table with the following columns:
      * run number
      * 5-file number
      * sector
      * number of electron triggers (`N`)
      * number of electrons in the forward tagger
      * DAQ-gated FC charge at beginning of 5-file (`F_i`)
      * DAQ-gated FC charge at end of 5-file (`F_f`)
      * DAQ-ungated FC charge at beginning of 5-file
      * DAQ-ungated FC charge at end of 5-file
    * `outmon/monitor_${runnum}.hipo` contains several plots 
      * in the script, they are organized into a tree data structure, which allows plot
        any variable, for any set of properties
        * for example, the helicity plots are for pi+,pi-, and positive helicity and
          negative helicity; this is a total of four plots
          * each particle branches into two helicity branches, each of which contain the
            plot object as a leaf
      * there is one plot per 'segment' where a segment is a single DST file
        (5-file) or a set of 10000 events for skim files
        * for skim files, the variable `segmentSize` can be used to change this
          number of events
        * for skim files, the segment number is set to be the average event
          number; the standard deviation is also included, which indicates the
          temporal localization of these events; typically the standard deviation
          is much smaller than the distance between two consecutive segments'
          averege event numbers
        * for DST files, the segment number is set to be the 5-file number, which
          is the minimum file number of the 5 consecutive, concatenated files (and
          is hence divisible by 5)


### Data Organization
* use the script `datasetOrganize.sh [dataset]`
  * this will concatenate files from `outdat` into a single file
    `outdat.${dataset}/data_table.dat`, for the specified dataset
  * it will also generate symlinks from `outmon.${dataset}/monitor*.hipo` to the
    relevant `outmon/monitor*.hipo` files


### Plotting Scripts
These scripts primary purpose is to make plots and store them in HIPO files; these HIPO
files can then be fed to a QA script

* `groovy monitorPlot.groovy $dataset`
  * this will read `outmon.${dataset}/monitor*.hipo` files and produce several timelines
    * the list of timelines is at the bottom of the script, and is handled by
      the `hipoWrite` closure, which takes two arguments:
      * the name of the timeline, which will be the name of the corresponding
        hipo file, also send to the `outmon/` directory
      * a list of filters, used to access the plots that are added to that
        timeline; all plots which pass those filters will be plotted together in
        a single timeline hipo file
        * these filters are just names of branches, e.g., `['helic','sinPhi']`
          will select on the sin(phiH) helicity monitor
      * some of the timelines have some custom modifications, such as the
        helicity monitors, so that positive and negative helicities are plotted
        together
    * Let X represent a quantity plotted in a timeline; the timeline plots the
      average value of X, versus run number
      * Several variables may be plotted together, on the same timeline plot,
        sharing the same vertical axis numbers, despite possibly having
        different units
        * Units of energy or momentum are GeV
        * Units of angle are radians
      * Clicking on a point will draw several plots below the timeline,
        corresponding to that run:
        * distribution of the average value of X, with one entry per segment
        * graph of the average value of X versus segment number
    * see the supplementary scripts section for some helpful scripts to upload
      timelines to the webserver

* `groovy qaPlot.groovy $dataset [$useFT]` 
  * reads `outdat.${dataset}/data_table.dat` and generates `outmon/monitorElec.hipo`
    * within this hipo file, there is one directory for each run, containing several
      plots:
      * `grA*`: N/F vs. file number (the `A` notation is so it appears first in the
        online timeline front-end)
      * `grF*`: F vs. file number
      * `grN*`: N vs. file number
      * `grT*`: livetime vs. file number
    * if `$useFT` is set, it will use FT electrons instead


### Automated QA of Normalized Electron Yield
This section will run the automated QA of the electron yield; it will ultimately
generate QA timelines, and a `json` file which is used for the manual followup QA

* Determine epoch lines
  * At the moment, this step is manual, but could be automated in a future release
  * You need to generate `epochs/epochs.${dataset}.txt`, which is a list epoch boundary lines
    * Each line should contain two numbers: the first run of the epoch, and the last run
      of the epoch
    * If you do not want to use epoch lines, execute 
      `echo "0 1000000" > epochs/epochs.${dataset}.txt`; this is a trick to ensure every run is
      in a single epoch
  * To help determine where to draw the epoch boundaries, execute `mkTree.sh $dataset`
    * this script, in conjunction with `readTree.C`, will build a `ROOT` tree and draw
      N/F vs. file index, along with the current epoch boundary lines (if defined)
      * other plots will be drawn, including N/F vs. run number (as a 2d histogram),
        along with plots for the Faraday Cup
      * the N/F plots are helpful in determining where to put the epoch boundary lines
      * look at N/F and identify where the average value "jumps": this typically
        occurs at the same time for all 6 sectors, but you should check all 6 regardless
      * the decision of where to put the epoch boundary lines is currently done
        manually, but could be automated in a future release

* `groovy qaCut.groovy $dataset [$useFT]`
  * reads `outmon/monitorElec.hipo`, along with `epochs/epochs.${dataset}.txt`, to build
    timelines for the online monitor
  * if `$useFT` is set, it will use FT electrons instead
  * the runs are organized into epochs, wherein each:
    * calculate N/F quartiles
      * `mq`: middle quartile, the overall median N/F
      * `lq`: lower quartile, the median N/F below `mq`
      * `uq`: upper quartile, the median N/F above `mq`
    * QA cut lines are set using an interquartile range (IQR) rule: `cutFactor` * IQR,
      where `cutFactor` adjusts the overall width of the cuts (currently set to `3.0`)
      * this is done for each sector individually
      * at this point, the automatic QA is initiated; results are stored in
        `outdat.${dataset}/qaTree.json`
  * timelines are generated (which can be uploaded to the webserver):
    * QA timeline
      * timeline is the 'pass fraction': the fraction of files in a run which pass QA
        cuts
      * 6 timelines are plotted simultaneously: one for each sector
      * click any point to show the corresponding graphs and histograms of N/F, N, F,
        and livetime
    * QA timeline "epoch view"
      * this is a timeline used to evaluate how the QA cuts look overall, for each epoch
      * the timeline is just a list of the 6 sectors; clicking on one of them will show
        plots of N/F, N, F, and livetime, for each epoch
        * the horizontal axis of these plots is a file index, defined as the run
          number plus a small offset (<1) proportional to the file number
      * the N/F plots include the cut lines: here you can zoom in and see how
        well-defined the cut lines are for each epoch
        * if there are any significant 'jumps' in the N/F value, the cut lines may be
          appear to be too wide: this indicates an epoch boundary line needs to be drawn
          at the step in N/F
    * Several other timelines are generated as well, such as standard deviation of 
      the number of electrons


## Manual QA procedure
This procedure is to be performed after the automatic QA; it is called "manual
QA" because it requires substantially more user interaction, carefully checking
the timelines and recording features not identified by the automatic QA in
`qaTree.json`
* first, make sure you have an automatically generated `qaTree.json`
  * verify your epoch lines are where you want them
    * use `mkTree.sh`
    * look at "supplemental" `epoch view` timelines
  * if you make changes to the epoch lines, re-run `exeTimelines.sh` to
    generate the updated `qaTree.json`
* verify all the data have been analyzed by the automatic QA
  * execute `getListOfDSTs.sh [dataset]` to obtain a list of run numbers and file
    numbers from the DST file directory; this script takes some time to run
  * execute `integrityCheck.sh [dataset]` to compare the list of DST files
    to those which appear in `outdat.$dataset/data_table.dat`; if any DST files
    do not appear in `data_table.dat`, the check will fail, the missing files
    will be printed, and it is recommended to re-run the automatic QA, either for
    those specific runs, or in its entirety
* `cd QA`; this subdirectory contains code for the "manual QA"
* `import.sh [dataset]` to import the automatically generated `qaTree.json`; this
  will also generate a human-readable file, `qa/qaTable.dat`  
  * append the option `-cnds=user_comment` to copy the user comment from RCDB
    to `qa/qaTable.dat`, which is helpful for the manual QA procedure
  * by default, the `json` file is in `../outdat.${dataset}/qaTree.json`;
    you can also specify a path to a specific `qaTree.json` file; this is 
    useful if you have a more up-to-date version somewhere else, and you
    want to use the tools in this QA directory to make revisions
* open `qa/qaTable.dat` in another window; this is the human-readable version of
  the imported `qaTree.json`
* now scan through `qaTable.dat`, inspecting each run:
  * tip: if using `vim`, type `:LogiPat !"GOLDEN"` to search for files that are
    not `GOLDEN`, i.e., for defects; then use `n` and `N` to skip to the next
    and previous non-golden files, respectively
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
        to define a sector loss period: use `modify.sh sectorLoss ...` (see below)
      * mark all files in a run with `Misc` bit for special cases which are best
        summarized in a comment; use `modify.sh addBit ...` to set the `Misc` bit
        (it will prompt you for a comment)
    * `modify.sh` usage:
      * this script allows for easy revision of the imported `qaTree.json`; it will also
        update `qaTable.dat`, so it is useful to have `qaTable.dat` open in a window
        which will auto-refresh whenever you call `modify.sh`
      * type `modify.sh` without any arguments for the most up-to-date usage documentation
      * the first argument is the modifification you want to make, whether it
        is to add a defect bit, delete a defect bit, add a comment, etc.
        * call `modify.sh` with one of these arguments for additional documentation specific
          to this argument, e.g., `modify.sh addComment`
        * the subsequent arguments are typically the run number, the range of
          files, and sectors, but depends on the revision you are making
      * if you make a mistake, call `undo.sh` to revert `qaTree.json` and
        `qaTable.dat` to the previous version; in fact, everytime you call
        `modify.sh`, a backup copy of `qaTree.json`, before the revision, is
        stored
  * you should also look through each timeline for any issues that may have slipped under
    the radar; revise `qaTree.json` using `modify.sh` as needed
    * in particular, check for any spin asymmetry that has the wrong sign,
      especially the piPlus BSA; a significant asymmetry with the wrong sign
      indicates the helicity has the wrong sign
    * check stddev timelines; usually a high standard deviation indicates a
      step or change in the data, or merely a short, low statistics run
    * check fraction of events with defined helicity; if it's low it could
      indicate a problem; so far in all cases we have checked and there are no
      issues with the reported beam spin asymmetry, but it is useful to
      document these cases with the Misc defect bit
* after scanning through `qaTable.dat` and revising `qaTree.json`, return to the parent
directory and call `exeQAtimelines.sh` to produce the updated QA timelines
  * these QA timelines are stored in `outmon.${dataset}.qa`
  * it copies the revised`qaTree.json` (`QA/qa.${dataset}/qaTree.json`) to
    the new QA timeline directory, which can then be deployed to the webservers
  * this final `qaTree.json` is stored in the 
    [`clas12-qadb` repository](https://github.com/JeffersonLab/clas12-qadb)
    and should be copied there, along with `chargeTree.json`
    * remember to run `util/syncCheck.groovy` in `clas12-qadb`
  * the scripts which copy timelines to the webserver (`deployTimelines.sh` and
    `releaseTimelines.sh`) will copy the new `outmon.${dataset}.qa` directory's
    timelines, but you must call these scripts manually

### melding
* This more advanced procedure is used if you need to combine two `qaTree.json` files
  * you must read the script carefully and edit it for your use case
  * for each run, the script will "combine" QA info from each of the
    `qaTree.json` files; the script must know what to do with each case
    * be careful if your `qaTree.json` files have different/overlapping sets of runs
  * this procedure is useful if, e.g, you change bit definitions and want to
    update a `qaTree.json` file, with full control of each defect bit's
    behavior
  * see `QA/meld/README.md`


## Supplementary Scripts
* `deployTimelines.sh __subdirectory__`
  * reads `outmon` directory for timeline `hipo` files and copies them to the online
    timeline webserver (you may need to edit the path), into the specified subdirectory

* `indexPage.groovy`
  * generate `ListOfTimelines.json` file, for hipo files in the online directory's
    subirectories

* `upload.sh __hipoFile(s)__`
  * upload a file to the timeline webserver, via `scp`
  * you may need to alter the webserver location

