# Setup Guide for `clas12-timeline`

The software is available from `ifarm` interactive nodes; use the `module` command to load it:
```bash
module avail timeline      # check which versions are available
module load timeline/dev   # load the 'dev' version (likely the most recent version)
module load timeline/1.0.0 # alternatively, load a specific version, such as 1.0.0
```

If you want to install locally, clone the repository, then run:
```bash
./install.sh
```

The directory `target/` will contain the build files, which are mostly JAR files.

You may now use the scripts in `bin/`.

> [!TIP]
> For convenience, you may prepend the `bin/` directory to your `$PATH`

> [!TIP]
> Use `mvn clean` if you want to clean the build
