# QADB Pion Kinematics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce QADB mean and RMS timelines for `p`, `pT`, `z`, `theta`, and `phiH` for event-builder pi+ and pi- candidates selected with the legacy loose DIS cuts.

**Architecture:** A new Java `PionKinematics` component owns per-QA-bin histograms and processes pion candidates using the already-selected `Electron`. A new Groovy `qadb_pion_kinematics` analysis combines those histograms by run and writes charge-separated mean/RMS timeline files; the existing QADB container and coordinator provide lifecycle integration.

**Tech Stack:** Java 21, Groovy 5.1.2, Maven/JUnit 5, coat-libs 14.1.2, GROOT `H1F`/`GraphErrors`/`TDirectory`, CLAS12 `Particle`/`LorentzVector`/`DataEvent`.

**Spec:** `docs/superpowers/specs/2026-11-06-qadb-pion-kinematics-design.md`

## Global Constraints

- Accept only event-builder PIDs `211` and `-211`; add no status, chi2-PID, detector, or fiducial cuts.
- Require a selected electron with `Q2 > 1 GeV^2`, `W > 2 GeV`, and `y < 0.8`.
- Require `0.3 < z < 1` and a defined `phiH`; all inequalities are strict.
- Histogram exactly 50 bins with `p: [0,10] GeV`, `pT: [0,4] GeV`, `z: [0,1]`, `theta: [0,pi/2] rad`, and `phiH: [-3.15,3.15] rad`.
- Skip non-finite values and never create zero-valued timeline points for missing or empty distributions.
- Missing pion histograms in legacy files must not prevent other QADB analyses from completing.
- Do not add kaon/proton support or beam-spin-asymmetry calculations.
- Preserve unrelated tracked and untracked workspace changes.

## Review Focus

- Electron values exactly on a DIS boundary must reject the event; pin this in Task 1 boundary tests.
- Pion `z` exactly `0.3` or `1.0` must be rejected; pin this in Task 1 boundary tests.
- Collinear vectors that make `phiH` undefined must not fill any pion variable; pin this in Task 1 geometry tests.
- A missing `REC::Particle` bank or an unselected electron must leave all histograms empty; pin this in Task 1 event-selection tests.
- A run with only missing or empty pion distributions must produce graphs with zero points while preserving valid data for the other charge/variables; pin this in Task 3 output tests.

---

### Task 1: Per-QA-bin pion selection and histograms

**Files:**
- Create: `src/main/java/org/jlab/clas/timeline/histograms/qadb/PionKinematics.java`
- Create: `src/test/java/org/jlab/clas/timeline/histograms/qadb/PionKinematicsTest.java`

**Interfaces:**
- Consumes: `Electron.found()`, `getParticle()`, `getQ2()`, `getW()`, and `getY()`; `Tools.getParticle(DataBank, int)`; `DataEvent` bank access.
- Produces: `PionKinematics(int bin_num, double beam_energy)`, `void processEvent(DataEvent event, Electron electron)`, `H1F getHistogram(int pid, String variable)`, `void write(TDirectory tdir)`, and `boolean readHistograms(TDirectory tdir, int bin_num)`.

- [ ] **Step 1: Write failing construction and histogram-schema tests**

  Add tests named `constructsLegacyHistogramSchema`, `rejectsInvalidBeamEnergy`, and `rejectsUnknownPidOrVariable`. Assert the ten histograms exist for PIDs `211`/`-211`, variables `p`/`pT`/`z`/`theta`/`phiH`, the exact bin counts/ranges from Global Constraints, and explicit exceptions for invalid constructor/API inputs.

- [ ] **Step 2: Run the focused test and verify it fails**

  Run: `mvn -Dtest=PionKinematicsTest test`

  Expected: FAIL because `PionKinematics` does not exist.

- [ ] **Step 3: Implement the histogram schema and lookup API**

  Create `PionKinematics` with `TDIR = "/QADB/pion_kinematics"`, constants for `PIP = 211` and `PIM = -211`, private charge/variable histogram maps, exact legacy binning, constructor validation, and `getHistogram(int pid, String variable)`.

- [ ] **Step 4: Run the focused test and verify the schema passes**

  Run: `mvn -Dtest=PionKinematicsTest test`

  Expected: PASS for schema tests.

- [ ] **Step 5: Write failing kinematics, selection, and failure-mode tests**

  Add package-visible `void processParticle(Particle pion, Electron electron)` as the direct calculation seam. Tests must assert known numerical `p`, `pT`, `z`, `theta`, and signed `phiH` values for both charges; multiple accepted candidates increment entries independently; wrong PID, absent electron, each exact/failed DIS boundary, exact/failed `z` boundaries, undefined `phiH`, and non-finite results leave all five histograms unchanged. Add an event-level test proving an event without `REC::Particle` leaves histograms empty.

- [ ] **Step 6: Run the focused test and verify the new cases fail**

  Run: `mvn -Dtest=PionKinematicsTest test`

  Expected: FAIL in the new processing tests because candidate/event processing is not implemented.

- [ ] **Step 7: Implement candidate and event processing**

  Implement `processParticle(Particle pion, Electron electron)` and `processEvent(DataEvent event, Electron electron)`. Construct beam `(0,0,E,E)`, target `(0,0,0,Electron.PROTON_MASS)`, and `q = beam - electron`; reproduce the `monitorRead.groovy` Lorentz dot product and signed plane-angle algorithm, including its undefined-angle threshold `1.0e-5`; clamp only floating-point cosine roundoff to `[-1,1]`. Apply all cuts before filling any of a candidate's five histograms.

- [ ] **Step 8: Run the focused test and verify processing passes**

  Run: `mvn -Dtest=PionKinematicsTest test`

  Expected: PASS.

- [ ] **Step 9: Write failing HIPO persistence tests**

  Add `writeStoresAllPionHistograms`, `readHistogramsRestoresAllPionHistograms`, `readHistogramsReturnsFalseForMissingDirectory`, and `readHistogramsReturnsFalseForPartialSchema`. Assert object names follow `{pip|pim}_{variable}_hist_qa{bin}` under `TDIR`.

- [ ] **Step 10: Implement HIPO write/read**

  Implement `write(TDirectory)` and all-or-nothing `readHistograms(TDirectory, int)` without unchecked null dereferences when directories or objects are absent.

- [ ] **Step 11: Run the focused test and commit Task 1**

  Run: `mvn -Dtest=PionKinematicsTest test`

  Expected: PASS.

  Commit:

  ```bash
  git add src/main/java/org/jlab/clas/timeline/histograms/qadb/PionKinematics.java src/test/java/org/jlab/clas/timeline/histograms/qadb/PionKinematicsTest.java
  git commit -m "feat(QADB): add pion kinematics histograms"
  ```

### Task 2: QADB-bin lifecycle integration

**Files:**
- Modify: `src/main/java/org/jlab/clas/timeline/histograms/qadb/QadbBinHistograms.java`
- Modify: `src/test/java/org/jlab/clas/timeline/histograms/qadb/QadbBinHistogramsTest.java`

**Interfaces:**
- Consumes: Task 1 `PionKinematics(int, double)`, `processEvent(DataEvent, Electron)`, and `write(TDirectory)`.
- Produces: QADB files written by `QadbBinHistograms.write` include all ten pion histograms, and event processing invokes pion selection after the shared `Electron` is processed.

- [ ] **Step 1: Write failing container persistence test**

  Extend `QadbBinHistogramsTest` with `writeIncludesPionKinematicsHistograms`; assert representative plus/minus objects and then every charge/variable combination exists at the Task 1 names.

- [ ] **Step 2: Run the focused test and verify it fails**

  Run: `mvn -Dtest=QadbBinHistogramsTest test`

  Expected: FAIL because the container does not own/write `PionKinematics`.

- [ ] **Step 3: Integrate `PionKinematics` into the container**

  Add a private `PionKinematics pionKinematics`, construct it with `(bin_num, beam_energy)`, call `pionKinematics.processEvent(event, electron)` immediately after `electron.processEvent(event)`, and write it alongside `InclusiveKinematics`.

- [ ] **Step 4: Run histogram tests and commit Task 2**

  Run: `mvn -Dtest=PionKinematicsTest,QadbBinHistogramsTest test`

  Expected: PASS.

  Commit:

  ```bash
  git add src/main/java/org/jlab/clas/timeline/histograms/qadb/QadbBinHistograms.java src/test/java/org/jlab/clas/timeline/histograms/qadb/QadbBinHistogramsTest.java
  git commit -m "feat(QADB): fill pion kinematics per QA bin"
  ```

### Task 3: Per-run aggregation and timeline outputs

**Files:**
- Create: `src/main/java/org/jlab/clas/timeline/analysis/qadb/qadb_pion_kinematics.groovy`
- Create: `src/test/java/org/jlab/clas/timeline/analysis/qadb/qadb_pion_kinematicsTest.groovy`

**Interfaces:**
- Consumes: Task 1 `PionKinematics.readHistograms(TDirectory, int)` and `getHistogram(int, String)`.
- Produces: `processRun(TDirectory dir, int runnum, Map qa_map)`, `calculateRunStatistics(int runnum)`, `write(Map qa_map)`, package-visible `writeToDirectory(Map qa_map, File output_dir)`, and four charge/statistic HIPO files named in the spec.

- [ ] **Step 1: Write failing QA-bin reading and run-aggregation tests**

  Add tests `processRunReadsEveryQaBin`, `calculateRunStatisticsCombinesQaBinsForBothCharges`, and `processRunSkipsOnlyMissingBins`. Use deterministic bin centers and assert nested statistics keys `pip`/`pim` then `p`/`pT`/`z`/`theta`/`phiH`, with exact mean/RMS values and cloned run histograms.

- [ ] **Step 2: Run the focused analysis test and verify it fails**

  Run: `mvn -Dtest=qadb_pion_kinematicsTest test`

  Expected: FAIL because `qadb_pion_kinematics` does not exist.

- [ ] **Step 3: Implement QA-bin reading and run aggregation**

  Follow `qadb_inclusive_kinematics.groovy`: use a `ConcurrentHashMap`, store only bins whose full pion schema can be read, warn once per run with all missing bin numbers, clone the first histogram for each charge/variable, and add later-bin histograms.

- [ ] **Step 4: Run the focused analysis test and verify aggregation passes**

  Run: `mvn -Dtest=qadb_pion_kinematicsTest test`

  Expected: PASS for reading/aggregation tests.

- [ ] **Step 5: Write failing output, empty-data, and legacy tests**

  Add `writeCreatesFourChargeSeparatedTimelineFiles`, `writeSkipsEmptyOrNonFiniteDistributionsIndependently`, and `legacyFilesProduceEmptyGraphsWithoutThrowing`. Assert each file contains `/timelines/{variable}_{mean|rms}`, run number on X, expected statistic on Y, zero X/Y errors, and the combined per-run distribution under `/{runnum}`. Prove valid pi+ data is retained when pi- is empty and all graphs remain zero-length for a legacy run.

- [ ] **Step 6: Implement timeline writing**

  Implement the four files from the spec. Use titles and axes with `GeV` for `p`/`pT`, unitless `z`, and `rad` for `theta`/`phiH`. Add timeline points only when the combined histogram integral is positive and both statistic and histogram values are finite; store each run's combined histogram in both corresponding mean and RMS files.

- [ ] **Step 7: Run the focused analysis test and commit Task 3**

  Run: `mvn -Dtest=qadb_pion_kinematicsTest test`

  Expected: PASS.

  Commit:

  ```bash
  git add src/main/java/org/jlab/clas/timeline/analysis/qadb/qadb_pion_kinematics.groovy src/test/java/org/jlab/clas/timeline/analysis/qadb/qadb_pion_kinematicsTest.groovy
  git commit -m "feat(QADB): add pion kinematics timelines"
  ```

### Task 4: Coordinator integration and complete verification

**Files:**
- Modify: `src/main/java/org/jlab/clas/timeline/analysis/qadb/qadb.groovy`
- Modify: `src/test/java/org/jlab/clas/timeline/analysis/qadb/qadbTest.groovy`
- Modify: `docs/superpowers/specs/2026-11-06-qadb-pion-kinematics-design.md` only if implementation discoveries require a documented correction.

**Interfaces:**
- Consumes: Task 3 `qadb_pion_kinematics.processRun` and `write`.
- Produces: the top-level QADB analysis delegates every eligible run and final write to pion kinematics without changing existing analyses.

- [ ] **Step 1: Write failing coordinator delegation test**

  Extend `qadbTest` to inject a `RecordingAnalysis` into private field `ana_qadb_pion_kinematics`; assert exactly one `processRun` and one `write` call while preserving the inclusive-kinematics delegation assertions.

- [ ] **Step 2: Run the focused test and verify it fails**

  Run: `mvn -Dtest=qadbTest test`

  Expected: FAIL because the coordinator field/delegation does not exist.

- [ ] **Step 3: Integrate the run-level pion analysis**

  Construct `qadb_pion_kinematics` in `qadb.groovy` and invoke it from both `processRun` and `write` after inclusive kinematics.

- [ ] **Step 4: Run all QADB-focused tests**

  Run: `mvn -Dtest=PionKinematicsTest,QadbBinHistogramsTest,InclusiveKinematicsTest,ElectronTest,qadb_pion_kinematicsTest,qadb_inclusive_kinematicsTest,qadbTest test`

  Expected: PASS with zero failures/errors.

- [ ] **Step 5: Run the complete test suite**

  Run: `mvn test`

  Expected: `BUILD SUCCESS` with zero failures/errors.

- [ ] **Step 6: Inspect the final diff and workspace preservation**

  Run: `git status --short && git diff --check && git diff --stat origin/qadb-electron-kinematics...HEAD`

  Expected: no whitespace errors; only intended pion changes and documentation are added; the pre-existing `.vscode/`, numbered duplicate source files, and `src/test/java 2/` remain untouched and untracked.

- [ ] **Step 7: Commit coordinator and documentation**

  ```bash
  git add src/main/java/org/jlab/clas/timeline/analysis/qadb/qadb.groovy src/test/java/org/jlab/clas/timeline/analysis/qadb/qadbTest.groovy docs/superpowers/specs/2026-11-06-qadb-pion-kinematics-design.md docs/superpowers/plans/2026-11-06-qadb-pion-kinematics.md
  git commit -m "feat(QADB): integrate pion kinematics analysis"
  ```

- [ ] **Step 8: Record final evidence**

  Record the Maven summary, focused-test summary, `git diff --check` result, and final changed-file list for the implementation handoff.
