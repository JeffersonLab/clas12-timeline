# Chefs' Guide for Timeline Production

The timeline code is provided on `ifarm` via
```bash
module load timeline
```
The main script for running timelines is `qtl`:
```bash
qtl --help
qtl --version
```

Please report any issues to the software maintainers, such as warnings or error messages.

## General Timelines

### :green_circle: Step 1: Fill Timeline Histograms

Use the "qtl" model as part of your usual cooking workflow; see [the Chefs' documentation wiki](https://clasweb.jlab.org/wiki/index.php/CLAS12_Chef_Documentation).

Output files will appear in your chosen output directory, within `hist/detectors/`.

### :green_circle: Step 2: Analyze Histograms and Make the Timelines

Print the usage guide:
```bash
qtl analysis
```
In general,
```bash
qtl analysis -i $out_dir/hist/detectors -p $publish_dir
```
where
- `$out_dir` is your output directory from **Step 1**
- `$publish_dir` is the publishing directory to [`clas12mon`](https://clas12mon.jlab.org/)
  - the full publishing path will be `/group/clas/www/clas12mon/html/hipo/${publish_dir}`
  - the URL will be `https://clas12mon.jlab.org/${publish_dir}/tlsummary

Additional options may be _needed_ for your specific dataset, so check the usage guide (run `qtl analysis`).

A URL will be printed upon success, and a link will appear in [`clas12mon`](https://clas12mon.jlab.org/) in your run group's area momentarily.

> [!TIP]
> `qtl analysis` produces temporary files, by default in a subdirectory of `./outfiles/`. Consider making a symbolic link named `outfiles` pointing to somewhere on `/volatile`.

## Physics QA timelines

The physics timelines will eventually be combined with the detector timelines; until then, they are run separately:

- **Step 1:** Either:
  - Use `qtl histogram` instead of the workflow, with the option `--focus-physics`; this will run on SLURM directly (rather than through SWIF)
  - Use the `--physics` option with the workflow "qtl" model
- **Step 2:** Run `qtl physics` instead of `qtl analysis`; its options are similar

---

For more details, such as producing physics QA timelines, see other guides in
[the table of contents](/README.md) or reach out to the software maintainers.
