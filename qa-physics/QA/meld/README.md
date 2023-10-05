`meld.groovy` is used if you've made a major update to the QA, and you
want to "meld" the new results with old results
* you need to control which defect bits you want to overwrite:
  * some bits you will prefer to use the old `qaTree.json` version
  * other bits you may prefer to use the new ones
* this script is "one-time-use", so you need to read it carefully before
  running it
* it assumes the file names are `qaTree.json.old` and `qaTree.json.new`
* the output will be `qaTree.json.melded`
* be sure to backup any `qaTree.json` files in case something goes wrong
