This is a summary of the differences between the hard-coded beam energies in the timeline code and the RCDB.
See other files in this directory, in case there are typos here:
- `*.yaml` contain the hard-coded beam energies, extracted from scripts (see comment within)
- `diff-*.txt` shows all differences between hard-coded beam energies and those from RCDB;
  run `run_validate.sh` to produce these files

Only differences greater than 0.01 GeV are noted.

# RG-A
- RCDB matches timeline hard-coded values

# RG-B
- 11323-11391
  - timeline hard-coded: 10.3894 GeV
  - RCDB: 10.2129 GeV

# RG-C
- 16079
  - timeline hard-coded: 2.22 GeV
  - RCDB: 10.5473 GeV
- 17067-18130
  - timeline hard-coded: 10.54 GeV
  - RCDB: 10.5563 GeV up to run 17716 and 10.5593 GeV thereafter
    - for runs 17411, 17412 (empty target runs), and 18086 (cosmics) RCDB says -9.999 GeV

# RG-F
- 11620-11656
  - timeline hard-coded: 2.182 GeV
  - RCDB: 2.14418 GeV
  - **not in QADB**
- 11657
  - timeline hard-coded: 2.182 GeV
  - RCDB: 10.3894 GeV
  - **not in QADB**
- 12444
  - detector timelines use 2.182 GeV
  - physics timelines use 10.389 GeV
  - RCDB: 10.1966 GeV
- 12447-12951
  - timelines say: 10.389 GeV
  - RCDB: various differing values from about 10.2 to 10.4

# RG-K
- 5985-5990: 
  - timeline hard-coded: 6.535 GeV
  - RCDB: 6.500 GeV
  - this is a small difference, and nothing is noted about this in log book; should we correct RCDB?

# RG-M
- RCDB matches timeline hard-coded values
