# QADB Melding

`meld.groovy` is used to combine two `qaTree.json` files, with the _same_ set of runs; for example, use this if
- you've made a major update to the QA, and you want to "meld" the new results with old results
- you change bit definitions and want to update a `qaTree.json` file, with full control of each defect bit's behavior
- you retuned cut definitions of an automatic QA defect bit, _after_ you have done a manual QA

## General Procedure
- be sure to backup any `qaTree.json` files in case something goes wrong
- for each run, the script will "combine" QA info from each of the `qaTree.json` files; the script must know what to do with each case
    - you need to control which defect bits you want to overwrite:
        - some bits you will prefer to use the old `qaTree.json` version
        - other bits you may prefer to use the new ones
    - be careful if your `qaTree.json` files have different/overlapping sets of runs
- this script is "one-time-use", so you need to read it carefully before running it
- the input file names should be `qaTree.json.old` and `qaTree.json.new`
- the output will be `qaTree.json.melded`
