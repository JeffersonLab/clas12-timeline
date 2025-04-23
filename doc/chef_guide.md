# Chefs' Guide for Timeline Production

The timeline code is provided on `ifarm` via
```bash
module load timeline
```

Please report any issues to the software maintainers, such as warnings or error messages.

## :green_circle: Step 1: The workflow

Use the `qtl` model as part of your usual cooking workflow;
see [the Chefs' documentation wiki](https://clasweb.jlab.org/wiki/index.php/CLAS12_Chef_Documentation).

Output files will appear in your chosen output directory, within `hist/detectors/`.

## :green_circle: Step 2: Make the timelines

```bash
qtl analysis -d $dataset -i $out_dir/hist/detectors -p $publish_dir
```
- `$out_dir` is your output directory from **Step 1** and `$dataset` is a unique name for this cook, _e.g._, `rga_v1.23`.
- `$publish_dir` is the publishing directory
  - example: `rgb/pass0`

A URL will be printed upon success, and a link will appear in [`clas12mon`](https://clas12mon.jlab.org/) in your run group's area momentarily.

> [!TIP]
> Step 2 generates analyzed timeline files and logging information to a
> separate directory.
> - You can control that directory with the `-o` option, or just use the default.
> - If using the default, consider making a symbolic link named `outfiles` pointing to somewhere on `/volatile`

---

For more details, such as producing physics QA timelines, see other guides in
[the table of contents](/README.md) or reach out to the software maintainers.
