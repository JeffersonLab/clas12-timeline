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

> [!NOTE]
> If you want physics timelines, for now you'll need to use `qtl histogram --focus-physics`; we will eventually include this in the workflow!

## :green_circle: Step 2: Make the timelines

```bash
qtl analysis -d $dataset -i $out_dir/hist/detectors -p $publish_dir
```
- `$dataset` is a unique name for this cook, _e.g._, `rga_v1.23`
- `$out_dir` is your output directory from **Step 1**
- `$publish_dir` is the publishing directory
  - example: `rgb/pass0`
  - note: the full publishing path will be `/group/clas/www/clas12mon/html/hipo/${publish_dir}/${dataset}`

A URL will be printed upon success, and a link will appear in [`clas12mon`](https://clas12mon.jlab.org/) in your run group's area momentarily.

> [!IMPORTANT]
> Run `qtl analysis` with no further arguments for additional usage; some options may be _needed_ for your run group!

> [!TIP]
> Step 2 generates analyzed timeline files and logging information to a separate directory.
> - You can control that directory with the `-o` option, or just use the default.
> - If using the default, consider making a symbolic link named `outfiles` pointing to somewhere on `/volatile`, since once timelines are published successfully, you don't really need these output files

---

For more details, such as producing physics QA timelines, see other guides in
[the table of contents](/README.md) or reach out to the software maintainers.
