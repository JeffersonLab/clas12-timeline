# `clas12_monitoring`
An independent set of classes for clas12 monitoring of detectors

Follow the [main documentation](../README.md) for guidance, otherwise continue reading this file for details on detector monitoring code and execution.

## How to run the detector monitoring code

For example, the run 11127 of Run Group B can be monitored in following steps.

The first step is to create the list of files to be monitored:
```bash
ls /cache/clas12/rg-b/production/decoded/6.5.6/011127/*.hipo > list11127.txt
mkdir plots11127
```

Next run the compiled code, after setting some environment variables from [`environ.sh`](../bin/environ.sh):
```bash
source ../bin/environ.sh
java $TIMELINE_JAVA_OPTS org.jlab.clas12.monitoring.ana_2p2 11127 plots11127 list11127.txt 100000000 10.4
```
 Change 11127 to the wanted run number. The last two arguments are the maximum number of events to monitor, and the beam energy (10.4 GeV for example).

The class `ana_2p2` runs [all detectors' monitoring](src/main/java/org/jlab/clas12/monitoring). If it is enough to create updates in a specific detector monitoring, one can run the relevant code only. For example, for DC and TOF monitoring, please use `org.jlab.clas12.monitoring.tof_monitor`.

Currently, each detector is related to the following class names.

| Detector(s)                 | Class           |
| ---                         | ---             |
| All detectors               | `ana_2p2`       |
| BAND                        | `BAND`          |
| CVT, CTOF                   | `central`       |
| CND                         | `cndCheckPlots` |
| general DST monitoring      | `monitor2p2GeV` |
| rgb LD2 target              | `deutrontarget` |
| rgb specific DST monitoring | `dst_mon`       |
| FT                          | `FT`            |
| HTCC                        | `HTCC`          |
| LTCC                        | `LTCC`          |
| CVT occupancies             | `occupancies`   |
| RICH                        | `RICH`          |
| DC, FTOF                    | `tof_monitor`   |
