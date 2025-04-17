# `qa-detectors`

This procedure reads detector timelines and checks if certain parameters are within
QA specifications.

## Defining the Specifications
The specifications are specified by text files in [the `cuts` directory](cuts). The default file is `cuts/cuts.txt`
- cuts file syntax:
  - each line should have the following columns, delimited by spaces:
    - detector name
    - HIPO file name (_i.e._, the timeline name, with spaces changed to underscores)
    - lower bound of QA cut; if there is no lower bound, write `NB`, which means "No Bound"
    - upper bound of QA cut; if there is no upper bound, write `NB`, which means "No Bound"
    - units
    - (optional) additional specifier(s), _e.g._, specific sectors
  - comments can be added using the symbol `#`, which is useful for commenting out timelines, especially when debugging a particular timeline
- other files in `cuts/` may override the default file
  - overrides are applied by comparing the input timeline path to a regular expression
  - see [`util/applyBounds.groovy`](util/applyBounds.groovy) for the mapping of regular expressions to overriding cuts file
  - for example, paths which match the regular expression `/rga.*fa18/` could use the file `cuts/cuts_rga_fa18.txt`

## Procedure
- Run one of:
  ```bash
  ../bin/run-detectors-timelines.sh               # print usage guide
  ../bin/run-detectors-timelines.sh  --focus-qa   # run detector QA only (for debugging this QA code; you may need to set other options)
  ```
- see [main documentation](../README.md) for more details
- note to developers: if you want to run local scripts, call `source ../libexec/environ.sh` (this is
  automatically done when running the wrapper `../bin/run-detectors-timelines.sh`)
