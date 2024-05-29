# clas12-recipes

Recipes for CLAS12 chefs.

First, generate a workflow `json` file:
```bash
./cook-train.rb --help   # use --help for usage
```

Then submit it; _e.g._, if the `json` file is `my_workflow.json`:
```bash
swif2 import -file my_workflow.json    # import the workflow
swif2 run -workflow my_workflow        # start it
swif2 list                             # list your workflows
swif2 status my_workflow               # check the status
swif-gui                               # monitor it with a GUI
```
