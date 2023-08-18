# clas12-timeline

Timeline production for CLAS12. Timelines are deployed to [clas12mon](https://clas12mon.jlab.org).

To download,
```bash
git clone https://github.com/JeffersonLab/clas12-timeline.git
```

To build,
```bash
./bin/build-all.sh        # standard build
./bin/build-all.sh clean  # alternatively, a clean build
```

## Detector Timelines
The detector timeline production procedure is outlined in the following steps:

### 1. `clas12_monitoring`
To submit `clas12_monitoring` for each run from specified directory one should run these commands, e.g.:
```bash
./bin/run-monitoring.sh   # print usage guide
```

To run it interactively, see the [monitoring subdirectory](monitoring)

### 2. Detector Timelines
To run, execute following command,
```bash
./bin/run-detectors.sh "run group" "cooking version" "/path/to/monitoring/files/""
```
with the adequate arguments, e.g.,
```bash
./bin/run-detectors.sh rgb pass0v25.18 /volatile/clas12/rg-b/offline_monitoring/pass0/v25.18/
```

### 3. Detector QA
To run,
```bash
./bin/run-qa.sh TIMELINE
```
where `TIMELINE` is either the URL, for example,
```
https://clas12mon.jlab.org/rga/pass1/version3/tlsummary
```
or the relative path to the timeline, which for this example would be `rga/pass1/version3`. The output
URL containing QA timelines will be printed at the end of the script output; for this example, it will be
```
https://clas12mon.jlab.org/rga/pass1/version3_qa/tlsummary
```

See [further details](qa-detectors/README.md) for more information.

# Physics QA Timelines
The physics QA is typically performed only on a fully cooked dataset, whereas the above detector timeline production
is produced on much smaller data subsets. The physics QA timeline production is thus separate from the
detector timeline production, and produces the QADB.

See [its documentation here](qa-physics) for more details.


# Flowchart
Here is a flowchart illustrating the data and steps for timeline production:

```mermaid
flowchart TB

    classDef proc     fill:#8f8,color:black
    classDef data     fill:#ff8,color:black
    classDef misc     fill:#f8f,color:black
    classDef timeline fill:#8ff,color:black

    dst[(DST Files)]:::data

    subgraph Data Monitoring
        subgraph "<strong>bin/run-monitoring.sh</strong>"
            monitorDetectors["Make detector histograms<br/>monitoring/: org.jlab.clas12.monitoring.ana_2p2"]:::proc
            monitorPhysics["Make physics QA histograms<br/>qa-physics/monitorRead.groovy"]:::proc
        end
    end
    dst --> monitorDetectors
    dst --> monitorPhysics

    monitorDetectors --> outplots[(detectors/outplots)]:::data
    monitorPhysics   --> outdat[(qa-physics/outdat/*.dat)]:::data
    monitorPhysics   --> outmon[(qa-physics/outmon/*.hipo)]:::data

    subgraph Timeline Production
        subgraph "<strong>bin/run-detector-timelines.sh</strong>"
            timelineDetectorsPreQA["Make detector timelines<br/>detectors/: org.jlab.clas.timeline.run"]:::proc
            timelineDetectors["Draw QA lines<br/>qa-detectors/: applyBounds.groovy"]:::proc
        end
        subgraph "<strong>bin/run-physics-timelines.sh</strong>"
            timelinePhysics["Run physics QA:<br/>qa-physics/_____"]:::proc
        end
    end

    outplots --> timelineDetectorsPreQA --> timelineDetectors
    outdat   --> timelinePhysics
    outmon   --> timelinePhysics

    subgraph QADB Production
        qadb([QADB]):::misc
        manualQA[Perform manual<br/>physics QA]:::proc
    end
    
    timelines{{timelines}}:::timeline

    timelineDetectors --> timelines
    timelinePhysics   --> timelines
    timelinePhysics   --> qadb
    manualQA <-.-> timelines
    manualQA <-.-> qadb
```
