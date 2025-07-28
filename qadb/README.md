# QADB Tools

This directory contains tools for modifying and refining the QADB. We recommend you work _within_ this
current directory, that is
- have your _own_ copy of this repository (`git clone`)
- make sure the code is built and installed locally (see [setup guide](/doc/setup.md))

To run any tool, first **import** a QADB file (typically named `qaTree.json`); run the following for usage:
```bash
./import.sh    # run with no arguments for usage guide
```
The directory `qa.$dataset` will be produced, where `$dataset` is the dataset name you chose. A symbolic link
named `qa` will point to this directory, and we will refer to this directory as the "`qa` directory".

> [!NOTE]
> The `qa` symbolic link points to the _current working QADB_, that is, the QADB that many other tools will operate on; therefore, if you need to change which QADB you are working on, you can just change the directory to which `qa` points to, rather than re-importing.

Within the `qa` directory, the additional `qaTree.json.table` file will be produced, which is a human-readable
"table file" version of `qaTree.json`. See [QADB documentation](https://github.com/JeffersonLab/clas12-qadb) for guidance on how
to read this file. We recommend opening this file and watching its changes while you run other scripts in this directory; this file
will be overwritten, so do not edit it yourself.

To modify the QADB, use
```bash
./modify.sh    # run with no arguments for usage guide
```
and to undo that modification, use
```bash
./undo.sh
```
