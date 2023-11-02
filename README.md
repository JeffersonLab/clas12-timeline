# clas12-timeline

Timeline production for CLAS12. Timelines are deployed to [`clas12mon`](https://clas12mon.jlab.org).

## Setup

To download,
```bash
git clone https://github.com/JeffersonLab/clas12-timeline.git
```

To build,
```bash
mvn package
```
#### Additional Build Notes:
- Use `mvn clean` if you want to clean build targets.
- Use the `-f` option of `mvn` to build individual submodules:
  1. [`monitoring`](monitoring): generates histograms for detectors
  1. [`detectors`](detectors): uses detector histograms to generate timelines

## Procedure

Two types of timelines are produced:
1. **Detector timelines**: monitor detector parameters, histograms, and calibration
1. **Physics timelines**: monitor higher-level quantities to perform Quality Assurance (QA) for physics analysis

NOTE: physics timeline production and QA are typically only valuable on high-statistics datasets, whereas detector timelines need files produced with `mon` schema, which are typically only produced with low statistics; therefore, for a given dataset, typically one set of timelines is produced but not the other.

Both of these timeline types are produced in the following steps:

### Step 1: Data Monitoring

This step reads input HIPO files (_e.g._, DST or `mon` files) and produces histograms and auxiliary files, which are then consumed by Step 2 to produce the timelines. Since many input files are read, it is recommended to use a computing cluster.

To run this step, execute:
```bash
bin/run-monitoring.sh
```
which will print the usage guide (since it was called with no arguments); use the `--help` option for more detailed guidance.

#### Details
- data monitoring for detector timelines is handled by the [`monitoring/` subdirectory](monitoring);
  see [its documentation](monitoring/README.md)
- data monitoring for physics timelines is handled by the [`qa-physics/` subdirectory](qa-physics);
  see [its documentation](qa-physics/README.md)

### Step 2: Timeline Production and QA

After Step 1 is complete, run the following Step 2 scripts to produce the timeline HIPO files and to run the automatic QA procedures. There is one script for each timeline type: run them with no arguments to print the usage guides:

```bash
bin/run-detectors-timelines.sh
bin/run-physics-timelines.sh
```

#### Details
- detector timeline production is handled by the [`detectors/` subdirectory](detectors);
  see [its documentation](detectors/README.md)
- QA of detector timelines is handled by the [`qa-detectors/` subdirectory](qa-detectors);
  see [its documentation](qa-detectors/README.md)
- physics timeline production and QA are handled by the [`qa-physics/` subdirectory](qa-physics);
  see [their documentation](qa-physics/README.md)

### Step 3: Deployment

To view the timelines on the web, you must deploy them by copying the timeline HIPO files to a directory with a running web server. Note that you must have write-permission for that directory. To deploy, run (with no arguments, for the usage guide):

```bash
bin/deploy-timelines.sh
```

If all went well, a URL for the new timelines will be printed; open it in a browser to view them.


# Flowchart
Here is a flowchart illustrating the data and steps for timeline production. See the next section for details on output file organization.

```mermaid
flowchart TB

    classDef proc     fill:#8f8,color:black
    classDef data     fill:#ff8,color:black
    classDef misc     fill:#f8f,color:black
    classDef timeline fill:#8ff,color:black

    dst[(Input HIPO Files)]:::data

    subgraph Data Monitoring
        subgraph "<strong>bin/run-monitoring.sh</strong>"
            monitorDetectors["<strong>Make detector histograms</strong><br/>monitoring/: org.jlab.clas12.monitoring.ana_2p2"]:::proc
            monitorPhysics["<strong>Make physics QA histograms</strong><br/>qa-physics/: monitorRead.groovy"]:::proc
        end
        outplots[(___/detectors/$run_number/*.hipo)]:::data
        outdat[(___/physics/$run_number/*.dat)]:::data
        outmon[(___/physics/$run_number/*.hipo)]:::data
    end
    dst --> monitorDetectors
    dst --> monitorPhysics
    monitorDetectors --> outplots
    monitorPhysics   --> outdat
    monitorPhysics   --> outmon

    subgraph Timeline Production
        subgraph "<strong>bin/run-detectors-timelines.sh</strong>"
            timelineDetectorsPreQA["<strong>Make detector timelines</strong><br/>detectors/: org.jlab.clas.timeline.run"]:::proc
            outTimelineDetectorsPreQA{{outfiles/$dataset/timeline_web_preQA/$detector/*.hipo}}:::timeline
            timelineDetectors["<strong>Draw QA lines</strong><br/>qa-detectors/: applyBounds.groovy"]:::proc
        end
        subgraph "<strong>bin/run-physics-timelines.sh</strong>"
            timelinePhysics["<strong>Make physics QA timelines:</strong><br/>qa-physics/: (see documentation)"]:::proc
        end
    end
    subgraph Final Timelines
        outTimelinePhysics{{outfiles/$dataset/timeline_web/phys_*/*}}:::timeline
        outTimelineDetectors{{outfiles/$dataset/timeline_web/$detector/*.hipo}}:::timeline
        deploy["<strong>Deployment</strong><br/>bin/deploy-timelines.sh"]:::proc
        timelineDir{{timelines on web server}}:::timeline
    end
    outplots --> timelineDetectorsPreQA --> outTimelineDetectorsPreQA --> timelineDetectors --> outTimelineDetectors
    outdat   --> timelinePhysics
    outmon   --> timelinePhysics
    timelinePhysics --> outTimelinePhysics
    outTimelineDetectors --> deploy
    outTimelinePhysics   --> deploy
    deploy --> timelineDir

    subgraph QADB Production
        qadb([QADB]):::misc
        manualQA[<strong>Perform manual<br/>physics QA</strong>]:::proc
    end
    outTimelinePhysics   --> qadb
    manualQA <-.-> outTimelinePhysics
    manualQA <-.-> qadb
```

# Output Files Tree

The following shows the tree of output files produced by code in this repository. A unique dataset name `$dataset` is used by most scripts, and almost all output files are contained in `outfiles/$dataset`.

Temporary files are additionally stored in `tmp/`, including backups (for the case when you re-run timelines for the same `$dataset`)

```
outfiles
└── $dataset
    │
    ├── timeline_detectors            # histograms, etc. for detector timelines, from `bin/run-monitoring.sh`
    │   │
    │   ├── 5000                      # for run number 5000
    │   │   ├── out_HTCC_5000.hipo
    │   │   ├── out_LTCC_5000.hipo
    │   │   └── ...
    │   │
    │   ├── 5001                      # for run number 5001
    │   └── ...
    │
    ├── timeline_physics              # histograms, etc. for physics timelines, from `bin/run-monitoring.sh`
    │   │
    │   ├── 5000                      # for run number 5000
    │   │   ├── data_table_5000.dat
    │   │   └── monitor_5000.hipo
    │   │
    │   ├── 5001                      # for run number 5001
    │   └── ...
    │
    ├── timeline_physics_qa           # transient files for the physics QA
    │   ├── outdat
    │   │   ├── qaTree.json           # QADB
    │   │   ├── qaTreeFT.json         # QADB for FT only
    │   │   ├── chargeTree.json       # FC charge info
    │   │   └── data_table.dat        # combined data_table*.dat from each run
    │   ├── outmon                    # timeline (and other) HIPO files
    │   └── outmon.qa                 # QADB timelines
    │
    ├── timeline_web_preQA            # detector timelines, before QA lines are drawn
    │   ├── htcc
    │   ├── ltcc
    │   └── ...
    │
    ├── timeline_web                  # final output timeline files, for deployment to web server
    │   │
    │   ├── htcc                      # detector timelines, with QA, from `bin/run-detectors-timelines.sh`
    │   ├── ltcc
    │   ├── ...
    │   │
    │   ├── phys_qa                   # physics timelines, with QA, from `bin/run-physics-timelines.sh`
    │   ├── phys_qa_extra             # extra physics QA timelines, for experts
    │   └── qadb                      # QADB results timeline
    │
    └── log                           # log files from `bin/run-*-timelines.sh` (not slurm logs (/farm_out/$LOGNAME/))
        ├── $timeline.out
        └── $timeline.err
```

# Notes on SWIF Workflows

For [CLAS12 `swif` workflow](https://github.com/baltzell/clas12-workflow) integration, the `bin/run-monitoring.sh` script (which normally generates `slurm` jobs) has a specific mode `--swifjob`:
```bash
bin/run-monitoring.sh --swifjob --focus-detectors   # generate files for detector timelines
bin/run-monitoring.sh --swifjob --focus-physics     # generate files for physics QA timelines
```
Either or both of these commands is _all_ that needs to be executed on a runner node, within [`clas12-workflow`](https://github.com/baltzell/clas12-workflow); calling one of these will automatically run the wrapped code, with the following assumptions and conventions:
- input HIPO files are at `./` and only a single run will be processed
- run number is obtained by `RUN::config` from one of the HIPO files; all HIPO files are assumed to belong to the same run
- all output files will be in `./outfiles` (no `$dataset` subdirectory as above)

The output files `./outfiles/` are moved to the `swif` output target, following the usual file tree convention with run-number subdirectories:
```
top_level_target_directory
  │
  ├── detectors
  │   ├── 005000  # run 5000; corresponding output files from `--focus-detectors` in `outfiles/` are moved here
  │   ├── 005001  # run 5001
  │   └── ...
  │
  ├── physics
  │   ├── 005000  # run 5000; corresponding output files from `--focus-physics` in `outfiles/` are moved here
  │   └── ...
  │
  ├── recon
  │
  ├── train
  │
  └── ...
```
For compatibility with the file tree expected by downstream `bin/run-*-timelines.sh` scripts (see above), symbolic links may be made to these `timeline_{detectors,physics}` directories, but this is not required since these scripts also allow for the specification of an input directory.

Separate `--focus-detectors` and `--focus-physics` options are preferred, since:
- offers better organization of the contract data between `swif` jobs and downstream scripts
- often we will run one and not the other: `--focus-detectors` needs `mon` schema, whereas `--focus-physics` prefers high statistics


## QA Database (QADB) Production

The [QADB](https://github.com/JeffersonLab/clas12-qadb) is produced by the physics timeline QA, typically only for a fully cooked dataset. It is automatically produced from the physics QA, but it is highly recommended to perform a "manual QA" afterward, by looking at the automatic QA results, cross checking with the experimental log book, and modifying the QADB accordingly.

See [its documentation here](qa-physics) for more details.
