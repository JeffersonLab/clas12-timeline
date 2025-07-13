# Procedure for Timeline Production

> [!NOTE]
> Chefs should follow the [chef's guide](/doc/chef_guide.md), since some of the timeline procedure is integrated in the chef cooking workflow.

The main script for running timelines is `qtl`:
```bash
qtl --help
qtl --version
```

Two types of timelines are produced:
1. **Detector timelines**: monitor detector parameters, histograms, and calibration
1. **Physics timelines**: monitor higher-level quantities to perform Quality Assurance (QA) for physics analysis

Both of these timeline types are produced in the following steps (🟢) .

> [!NOTE]
> Physics timeline production and QA are typically only valuable on high-statistics datasets, whereas detector timelines need files produced with `mon` schema, which are typically only produced with low statistics; therefore, for a given dataset, typically one set of timelines is produced but not the other.

## 🟢 Step 1: Histogramming

This step reads input HIPO files (_e.g._, DST or `mon` files) and produces histograms and auxiliary files, which are then consumed by Step 2 to produce the timelines. Run:
```bash
qtl histogram
```
Running it with no arguments will print the usage guide; use the `--help` option for more detailed guidance.

> [!NOTE]
> This step is integrated in the chef's cooking workflow; see the [chef's guide](/doc/chef_guide.md) for more information.

> [!NOTE]
> If you are performing physics QA for QADB, consider using [**prescaled trains**](/qa-physics/prescaler) (and `qtl histogram` will need the `--flatdir` argument)

### Example
```bash
qtl histogram -d rga_sp19_v5 /volatile/clas12/rg-a/production/pass0/sp19/v5/mon
```
- sets the `dataset` name to `"rga_sp19_v5"`, which will be referenced in subsequent steps
- assumes the input data are found in `/volatile/clas12/rg-a/production/pass0/sp19/v5/mon`

Then run one (or both) of the printed `sbatch` commands:
```bash
sbatch ./slurm/job.rga_sp19_v5.detectors.slurm   # for detector timelines (need mon schema)
sbatch ./slurm/job.rga_sp19_v5.physics.slurm     # for physics timelines
```
- monitor progress with Slurm tools (e.g., `squeue -u $LOGNAME`)
- monitor output logs in `/farm_out/$LOGNAME/` or use `qtl error`

> [!NOTE]
> - histogramming for detector timelines is handled by the [`org.jlab.clas.timeline.histograms` package](/src/main/java/org/jlab/clas/timeline/histograms)
> - histogramming for physics timelines is handled by the [`qa-physics/` subdirectory](/qa-physics);
>   see [its documentation](/qa-physics/README.md)

## 🟢 Step 2: Timeline Analysis and QA

After Step 1 is complete, run the following Step 2 scripts to produce the timeline HIPO files and to run the automatic QA procedures. There is one script for each timeline type: run them with no arguments to print the usage guides:

```bash
qtl analysis  # for detector timelines
qtl physics   # for physics timelines (will eventually be combined with 'qtl analysis')
```

> [!TIP]
> If you are processing a large data set on `ifarm`, direct your output files to a location within `/volatile`. Either:
> - make a symbolic link in your working directory named `outfiles` pointing to a location within `/volatile`
> - use the scripts' `-o` option to set the output locations

### Example
**If** you used the chef's cooking workflow for Step 1, the script arguments should be
```bash
qtl analysis -i /path/to/output/files -p some/publish/directory/rga_sp19_v5
```
- the output from the cooking workflow is `/path/to/output/files`; its subdirectories should be run numbers
- the publishing directory given by `-p` is a subdirectory of the web server; see `qtl analysis` usage guide

**Otherwise**, you may omit the `-i /path/to/output/files` option (unless you customized it from Step 1)
- notice the dataset name from Step 1 should be the publishing directory basename
- NOTE: `qtl physics` still needs the dataset name (`-d`) too...

> [!NOTE]
> - detector timeline production is handled by the [`org.jlab.clas.timeline.analysis` package](/src/main/java/org/jlab/clas/timeline/analysis)
> - physics timeline production and QA are handled by the [`qa-physics/` subdirectory](/qa-physics);
>   see [their documentation](/qa-physics/README.md)
