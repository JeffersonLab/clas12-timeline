# Setup Guide for `clas12-timeline`

The software is available from `ifarm` interactive nodes; use the `module` command to load it:
```bash
module avail timeline      # check which versions are available
module load timeline/dev   # load the 'dev' version (likely the most recent version)
module load timeline/1.0.0 # alternatively, load a specific version, such as 1.0.0
```

If you want to install locally, download the repository:
```bash
git clone https://github.com/JeffersonLab/clas12-timeline.git
```
Then build:
```bash
./install.sh
```

> [!TIP]
> - Use `./install.sh clean` if you want to clean build (or `mvn clean`)
