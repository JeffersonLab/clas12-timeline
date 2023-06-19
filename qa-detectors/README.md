# qa-detectors

This procedure reads `clas12mon` detector timelines, and checks if certain parameters are within
QA specifications.

## Defining the Specifications
The specifications are specified by text files in [the `cuts` directory](cuts). The default file is `cuts/cuts.txt`
- cuts file syntax:
  - each line should have the following columns, delimited by spaces:
    - detector name
    - hipo file name
    - lower bound
    - upper bound
    - units
    - (optional) additional specifier(s)
  - comments can be added using the symbol `#`
    - this is useful for commenting out timelines, especially when debugging a particular timeline
- other files in `cuts/` may override the default file
  - overrides are applied by comparing the `clas12mon` input timeline path to a regular expression
  - see `util/applyBounds.groovy` for the mapping of regular expressions to overriding cuts file
  - for example, paths which match the regular expression `/rga.*fa18/` could use the file `cuts/cuts_rga_fa18.txt`

## Procedure
- set environment variables:
  ```bash
  source env.sh
  ```
- Run one of:
  ```bash
  ../bin/qa.sh                     # print usage guide
  ../bin/qa.sh timeline_URL        # URL for a timeline
  ../bin/qa.sh path/to/timelines   # or specify a timeline directory, relative to $TIMELINEDIR
  ../bin/qa.sh path/to/timelines output_dir_name   # custom output directory name, relative to $TIMELINEDIR
  ```
- The URLs for the timelines will be printed upon success
- The new timeline files will appear in `/path/to/timelines_qa` (or in the
  custom directory, if you specified one), where any timeline hipo file to
  which QA cuts were applied has replaced the original hipo file, with the name
  "QA" appended to the file name
