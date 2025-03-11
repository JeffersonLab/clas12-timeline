# Procedure for Timeline Production

Two types of timelines are produced:
1. **Detector timelines**: monitor detector parameters, histograms, and calibration
1. **Physics timelines**: monitor higher-level quantities to perform Quality Assurance (QA) for physics analysis

Both of these timeline types are produced in the following steps (🟢) .

> [!NOTE]
> Physics timeline production and QA are typically only valuable on high-statistics datasets, whereas detector timelines need files produced with `mon` schema, which are typically only produced with low statistics; therefore, for a given dataset, typically one set of timelines is produced but not the other.

## 🟢 Step 1: Data Monitoring

This step reads input HIPO files (_e.g._, DST or `mon` files) and produces histograms and auxiliary files, which are then consumed by Step 2 to produce the timelines. Since many input files are read, it is recommended to use a computing cluster.

This step can either be run during the usual data cooking procedure, using [`clas12-workflow`](https://github.com/baltzell/clas12-workflow) (see its usage guide), or it may be run separately on already-cooked data using:
```bash
bin/run-monitoring.sh
```
Running it with no arguments will print the usage guide; use the `--help` option for more detailed guidance.

> [!NOTE]
> If you are performing physics QA for QADB, consider using [**prescaled trains**](/qa-physics/prescaler) (and `run-monitoring.sh` will need the `--flatdir` argument)

### Example
If using `clas12-workflow`, see it's documentation; otherwise if using `run-monitoring.sh`:
```bash
bin/run-monitoring.sh -d rga_sp19_v5 /volatile/clas12/rg-a/production/pass0/sp19/v5/mon
```
- sets the `dataset` name to `"rga_sp19_v5"`, which will be referenced in subsequent steps
- assumes the input data are found in `/volatile/clas12/rg-a/production/pass0/sp19/v5/mon`

Then run one (or both) of the printed `sbatch` commands:
```bash
sbatch ./slurm/job.rga_sp19_v5.detectors.slurm   # for detector timelines (need mon schema)
sbatch ./slurm/job.rga_sp19_v5.physics.slurm     # for physics timelines
```
- monitor progress with Slurm tools (e.g., `squeue -u $LOGNAME`)
- monitor output logs in `/farm_out/$LOGNAME/` or use `bin/error-print.sh`

> [!NOTE]
> - data monitoring for detector timelines is handled by the [`org.jlab.clas.timeline.histograms` package](/src/main/java/org/jlab/clas/timeline/histograms)
> - data monitoring for physics timelines is handled by the [`qa-physics/` subdirectory](/qa-physics);
>   see [its documentation](/qa-physics/README.md)

## 🟢 Step 2: Timeline Production and QA

After Step 1 is complete, run the following Step 2 scripts to produce the timeline HIPO files and to run the automatic QA procedures. There is one script for each timeline type: run them with no arguments to print the usage guides:

```bash
bin/run-detectors-timelines.sh
bin/run-physics-timelines.sh
```

> [!IMPORTANT]
> If you are processing a large data set on `ifarm`, direct your output files to a location within `/volatile`. Either:
> - make a symbolic link in your working directory named `outfiles` pointing to a location within `/volatile`
> - use the scripts' `-o` option to set the output locations

### Example
**If** you used `clas12-workflow` for Step 1:
```bash
bin/run-detectors-timelines.sh -d rga_sp19_v5 -i /path/to/output/files   # for detector timelines
bin/run-physics-timelines.sh   -d rga_sp19_v5 -i /path/to/output/files   # for physics timelines
```
- the dataset is _given_ the name `"rga_sp19_v5"` (and does not have to be related to any name given from Step 1)
- the output from `clas12-workflow` is `/path/to/output/files`; its subdirectories should be run numbers

**Otherwise**, you may omit the `-i /path/to/output/files` option (unless you customized it from Step 1):
```bash
bin/run-detectors-timelines.sh -d rga_sp19_v5   # for detector timelines
bin/run-physics-timelines.sh   -d rga_sp19_v5   # for physics timelines
```
- the dataset name must match that of Step 1, otherwise you need to specify the path to the input files with `-i`


> [!NOTE]
> - detector timeline production is handled by the [`org.jlab.clas.timeline.analysis` package](/src/main/java/org/jlab/clas/timeline/analysis)
> - QA of detector timelines is handled by the [`qa-detectors/` subdirectory](/qa-detectors);
>   see [its documentation](/qa-detectors/README.md)
> - physics timeline production and QA are handled by the [`qa-physics/` subdirectory](/qa-physics);
>   see [their documentation](/qa-physics/README.md)

## 🟢 Step 3: Deployment

To view the timelines on the web, you must deploy them by copying the timeline HIPO files to a directory with a running web server. Note that you must have write-permission for that directory. To deploy, run (with no arguments, for the usage guide):

```bash
bin/deploy-timelines.sh
```

If all went well, a URL for the new timelines will be printed; open it in a browser to view them.

### Example
```bash
bin/deploy-timelines.sh -d rga_sp19_v5 -t rga/sp19/pass0/v5 -D   # deploy to a run-group web directory (for chefs)
### or ###
bin/deploy-timelines.sh -d rga_sp19_v5 -t $LOGNAME/my_test -D    # deploy to a personal web directory (for testing)
```
- this will _only_ print what will be done: deploy the timelines from dataset `"rga_sp19_v5"` (defined in previous step(s)) to the printed path
  - you must have write access to that path; contact the maintainers if you need help with this
  - if you are a chef, consider using the appropriate run group subdirectory, _e.g._, `rga/sp19/pass0/v5`
- if it looks correct, remove the `-D` option to deploy for real and follow the printed URL
