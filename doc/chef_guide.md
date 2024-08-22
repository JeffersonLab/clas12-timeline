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
run-detectors-timelines.sh -d $dataset -i $out_dir/hist/detectors
```
where `$out_dir` is your output directory from **Step 1** and `$dataset` is a unique name for this cook, _e.g._, `rga_v1.23`.

Output will appear in `./outfiles/$dataset/`.

## :green_circle: Step 3: Deploy the timelines

```bash
deploy-timelines.sh -d $dataset -t $target_dir -D
```
where `$target_dir` is a subdirectory of `/group/clas/www/clas12mon/html/hipo`, for example,
```bash
-t rgb/pass0/$dataset   # deploys to /group/clas/www/clas12mon/html/hipo/rgb/pass0/$dataset/
```
- remove the `-D` argument if everything looks okay (`-D` only prints what the script will do, _i.e._, a "dry run")
- a URL will be printed upon success, and a link will appear in [`clas12mon`](https://clas12mon.jlab.org/) in your run group's area momentarily

---

For more details, such as producing physics QA timelines, see other guides in
[the table of contents](/README.md) or reach out to the software maintainers.
