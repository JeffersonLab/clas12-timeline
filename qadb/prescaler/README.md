# DST Prescaler

Recipes for producing "prescaled" trains from DST files: randomly chooses a fraction of the events to keep,
so that the QA code has a much smaller dataset to analyze.

First, generate a workflow `json` file:
```bash
./cook-train.rb --help   # use --help for usage
```

Then submit it using
```bash
start-workflow.sh [JSON_FILES]...
```
or manually; _e.g._, if the `json` file is `my_workflow.json`:
```bash
swif2 import -file my_workflow.json         # import the workflow
swif2 run -workflow my_workflow             # start it
swif2 cancel -delete -workflow my_workflow  # delete it (if you need to try again)
swif2 list                                  # list your workflows
swif2 status my_workflow                    # check the status
swif-gui                                    # monitor it with a GUI
```
