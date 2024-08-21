# Chefs' Guide for Timeline Production

Please use the timeline code that is provided for you on `ifarm`; you may load it with
```bash
module load timeline
```

## :green_circle: Step 1: The workflow

To produce timeline 'histogram' files, use the `qtl` model as part of your usual cooking workflow;
see [the Chefs' documentation wiki](https://clasweb.jlab.org/wiki/index.php/CLAS12_Chef_Documentation).

You should have some _unique_ name for your cook, likely as part of your output file path; for example `rga_v1.23`.
You'll need such a name in the next steps.

## :green_circle: Step 2: Make the timelines

```bash
run-detectors-timelines.sh -d $dataset -i $out_dir/hist/detectors
```
where:
- `$out_dir` is your output directory from **Step 1**
- `$dataset` is the aforementioned unique dataset name

Output should appear in `./outfiles/$dataset/`.

Please report any errors to the software maintainers.

## :green_circle: Step 3: Deploy the timelines

```bash
deploy-timelines.sh -d $dataset -t $target_dir -D
```
where `$target_dir` is a subdirectory of `/group/clas/www/clas12mon/html/hipo`, for example,
```bash
rgb/pass0/$dataset   # deploys to /group/clas/www/clas12mon/html/hipo/rgb/pass0/$dataset/
```
- the `-D` argument only _prints_ what the command will do; remove `-D` to _actually_ deploy the timelines if everything looks okay
- try to keep this directory organized
- a URL will be printed upon success, and a link will appear in [`clas12mon`](https://clas12mon.jlab.org/) in your run group's area within an hour
  or two

For more details, such as producing physics QA timelines, see other guides in
[the table of contents](/README.md) or reach out to the software maintainers.
