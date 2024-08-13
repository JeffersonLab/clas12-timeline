A minimal timeline generation example looks like this:
```boo
module load timeline
run-detectors-timelines.sh -d rgx_sp24_v2 -i $outDir/hist/detectors
deploy-timelines.sh -d rgx_sp24_v2 -t rgx/sp24/v2 -D 
```
where `$outDir` was used to configure a (now completed) workflow with qtl model.  For physics timelines, use `run-physics-timelines.sh` instead.
