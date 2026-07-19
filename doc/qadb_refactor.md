# QADB Timeline Code Refactor

The original QADB timeline code is contained in the [`qa-physics/`](/qa-physics) directory, and follows a code design that is
fully independent of the main timeline-production code found in [`src/.../timeline`](/src/main/java/org/jlab/clas/timeline/). Because of this separation,
we have historically called these QADB timelines "physics timelines", while the timelines produced by the code in `src/.../timeline`
are called "detector timelines". Chefs currently need to produce the physics (QADB) timelines _separately_ from
the detector timelines; one goal of this refactor is to produce the QADB timelines as _part_ of the detector
timelines, removing the need for such a separation altogether.

> [!NOTE]
> - physics (QADB) timelines are most useful for DST files
> - detector timelines are most useful for calibration purposes

Unfortunately, over the years the `qa-physics/` code suffered from scope creep and is built on a non-ideal
foundation, and has now become spaghetti code and is therefore very difficult to maintain. This QADB timeline
code refactor involves a _full rewrite_ of the essential parts of `qa-physics` such that its timelines are
produced in the _same way_ that the detector timelines are produced.

## Design

For each timeline, we have 2 types of classes within `src/.../timeline`:

- Histogramming Java classes
    - found in [`src/.../timeline/histograms`](/src/main/java/org/jlab/clas/timeline/histograms);
      QADB-specific classes are in this path's `qadb/` subdirectory
    - run by [`qtl histogram`](/bin/qtl-histogram) via [`run_histograms.java`](/src/main/java/org/jlab/clas/timeline/run_histograms.java)
    - class methods:
        - constructor, to instantiate histograms and other data structures
        - `processEvent`, to read banks from a single event and fill histograms, _etc._
        - `write`, which writes histograms _etc._ to a HIPO file
        - `readHistograms`, which reads those histograms from a HIPO file
- Analysis Groovy classes
    - found in [`src/.../timeline/analysis`](/src/main/java/org/jlab/clas/timeline/analysis);
      QADB-specific classes are in this path's `qadb/` subdirectory
    - run by [`qtl analysis`](/bin/qtl-analysis) via [`run_analysis.groovy`](/src/main/java/org/jlab/clas/timeline/run_analysis.groovy)
    - class methods:
        - `processRun`, which reads a run's histograms, using the histogramming class's `readHistograms`
        - `write`, which makes the timeline graphs and writes them to timeline HIPO file(s)

### Example New QADB Classes
At the time of writing these notes, we have the following such classes for QADB production:

- Histogramming Java classes
    - [`QadbBinHistograms.java`](/src/main/java/org/jlab/clas/timeline/histograms/qadb/QadbBinHistograms.java)
        - this class runs all the other QADB histogramming classes, listed below
        - the caller, [`run_histograms.java`](/src/main/java/org/jlab/clas/timeline/run_histograms.java),
          creates one `QadbBinHistograms` instance per QADB bin per run; in other words, the histograms
          are filled with data from _one specific_ QADB bin within _one specific_ run, and these histogramming
          classes do not need to know anything about the binning scheme
        - the various histogramming classes need to be called in a certain order, as seen in
          `QadbBinHistograms.processEvent`
    - [`Charge.java`](/src/main/java/org/jlab/clas/timeline/histograms/qadb/Charge.java)
        - histograms for the Faraday cup charge, read from the scalers
        - the DSC2 scalers are integrating, and the charge information is obtained from [upstream code in `coatjava`](https://github.com/JeffersonLab/coatjava),
          from the `QadbBinSequence` class; the corresponding histograms are filled with a special method called by
          `run_histograms.java`
        - on the other hand, the STRUCK scalers, which are latched to helicity state, have histograms that must be filled
          per tag-1 scaler event; this is done in the `processEvent` method
    - [`Yield.java`](/src/main/java/org/jlab/clas/timeline/histograms/qadb/Yield.java)
        - histograms for particle yields, currently just electrons
        - the electron cuts are applied in a separate class, [`Electron.java`](/src/main/java/org/jlab/clas/timeline/histograms/qadb/Electron.java),
          since information about the electron is useful for other timelines; therefore, `QadbBinHistograms`
          runs `Electron.processEvent` and passes its output to `Yield.processEvent`
- Analysis Groovy classes
    - [`qadb.groovy`](/src/main/java/org/jlab/clas/timeline/analysis/qadb/qadb.groovy)
        - similar to the `QadbBinHistograms.java`, this class runs all the other QADB analysis Groovy classes,
          in the correct order
        - it includes an additional method, `start`, responsible for reading the QADB binning scheme from files
          output by the histogramming classes
    - [`qadb_charge.groovy`](/src/main/java/org/jlab/clas/timeline/analysis/qadb/qadb_charge.groovy)
        - reads histograms produced by `Charge.java`
        - writes various charge ($q$) timelines
    - [`qadb_yield.groovy`](/src/main/java/org/jlab/clas/timeline/analysis/qadb/qadb_yield.groovy)
        - reads histograms produced by `Yield.java`
        - writes various charge-normalized yield ($N/q$) timelines

## Plans

We plan to add code to these analysis classes which produce the same QADB `json` object that is produced
by the code in `qa-physics`.

These new QADB timelines will start appearing during the usual chef's workflow
(via the `qtl` model in [`wok`](https://code.jlab.org/hallb/clas12/wok)), assuming the read files have
enough scaler information to define a QADB binning scheme for each run. The "physics" timelines from
`qa-physics/` will still need to be produced separately, and for now will remain as the official QADB production code.
Once the refactor is complete, the `qa-physics` code will be removed, and QADB will then be produced
by this refactored code.

## Testing and Development

Here are some example commands to test the new code, meant to be executed
from the top-level source-code directory; see other documentation for details.

### Compilation
This must be done for any code change.
```bash
# just compile
mvn install
# alternatively, compile from a clean slate
mvn clean install
```

### Run Histogramming
Run the histogramming classes with:
```bash
# for 1 run
bin/qtl histogram -d refactor --focus-qadb --flatdir --single /work/clas12b/users/dilks/dm/data-rga-sp19-subset
# for all the runs, one at a time
bin/qtl histogram -d refactor --focus-qadb --flatdir --series /work/clas12b/users/dilks/dm/data-rga-sp19-subset
```
> [!NOTE]
> - the dataset name is set to `refactor`, with the `-d` option; you may pick any name
> - you may use a different data directory, if you prefer, since these commands are just examples;
>   for testing purposes, it's recommended to create a directory with symbolic links to a few runs' skim files
> - `--focus-qadb` is used to restrict the histogramming to the new QADB histogram classes only
> - `--flatdir` assumes the data directory contains HIPO files, one for each run
> - `--single` reads 1 single HIPO file, whereas `--series` reads all of them, one at a time

### Run Analysis
To analyze the histograms with the analysis classes, which produce the timelines, run:
```bash
bin/qtl analysis -d refactor -p dilks/share/filippin/refactor --overwrite -t qadb
```
> [!NOTE]
> - the dataset name (`refactor`) must match that in the Histogramming step
> - the publish directory (set by `-p`) is an example and points to Marco's test path:
>     - `dilks/share/filippin` (which is within `/group/clas/www/clas12mon/html/hipo/`)
>     - it's convention to use the same dataset name as a final subdirectory, that is, `refactor` at the
>       end of `-p dilks/share/filippin/refactor`
>     - use a different path, if you are not Marco
> - `--overwrite` means the publish directory will be _removed_ and replaced
> - `-t qadb` restricts the analysis to produce QADB timelines only

Upon success, a web URL will be printed, where you can view the resulting timelines in a web browser.
