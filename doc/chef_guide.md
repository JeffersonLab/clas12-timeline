# Chefs' Guide for Timeline Production

The timeline code is provided on `ifarm` via `module load timeline`.
Please report _any_ issues to the software maintainers, such as warnings or error messages.

## :green_circle: Step 1: Fill Timeline Histograms

This step is integrated in the cooking workflow using the "qtl" model; see [the Chefs' documentation wiki for details](https://clasweb.jlab.org/wiki/index.php/CLAS12_Chef_Documentation). Output files will appear in your chosen output directory, within `hist/detectors/`.

<details>
<summary>If you are not using the cooking workflow...</summary>

> See [the detailed procedure's Step 1](/doc/procedure.md) instead. The cooking workflow's "qtl" model just runs `qtl histogram` with the appropriate arguments.

</details>

<details>
<summary>For physics QA timelines...</summary>

> Note: we are working on combining the procedure for physics timelines with
> that for detector timelines; until then, to get the _full_ set of physics
> timelines, the procedure is separate. For Step 1, either:
> - Use the `--physics` option with the workflow "qtl" model
> - Use `qtl histogram` instead of the workflow, with the option `--focus-physics`; this will run on SLURM directly (rather than through SWIF)
</details>

## :green_circle: Step 2: Analyze Histograms and Make the Timelines

In general, run:
```bash
qtl analysis -i $out_dir/hist/detectors -p $publish_dir
```
where `$out_dir` is the output directory from **Step 1** and `$publish_dir` is the publishing directory to [`clas12mon`](https://clas12mon.jlab.org/).
Additional options may be _needed_ for your specific dataset, so check the usage guide by running with no arguments:
```bash
qtl analysis
```
A URL will be printed upon success, and a link will appear in [`clas12mon`](https://clas12mon.jlab.org/) in your run group's area momentarily.

<details>
<summary>For physics QA timelines...</summary>

> Run `qtl physics` instead of `qtl analysis`; its options are similar.
</details>

> [!TIP]
> Step 2 runs on an interactive node and can take some time if there are a lot of data. We recommend using tools such as [`tmux` terminal multiplexer](https://github.com/tmux/tmux/wiki) or [`GNU Screen`](https://www.gnu.org/software/screen/) to keep the job running if you lose your SSH connection.

> [!TIP]
> Step 2 produces temporary files, by default in a subdirectory of `./outfiles/`. Consider making a symbolic link named `outfiles` pointing to somewhere on `/volatile`.

---

For more details, see other guides in
[the table of contents](/README.md) or reach out to the software maintainers.
