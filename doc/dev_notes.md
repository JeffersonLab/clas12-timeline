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
    │   │   ├── qaTreeFD.json         # QADB for Forward Detector only
    │   │   ├── qaTreeFT.json         # QADB for Forward Tagger only
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



