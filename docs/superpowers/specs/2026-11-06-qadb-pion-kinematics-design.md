# QADB Pion Kinematics Design

## Intent

Add run-by-run QADB timelines for positive and negative pion kinematics. The
result should reproduce the loose selection and variables used by
`qa-physics/monitorRead.groovy`, so a later change can calculate pion beam-spin
asymmetries from the same event population.

## Goals

- Histogram `p`, `pT`, `z`, `theta`, and `phiH` for event-builder PIDs `211`
  (pi+) and `-211` (pi-) in every QADB bin.
- Require the selected scattered electron and the legacy DIS cuts
  `Q2 > 1 GeV^2`, `W > 2 GeV`, and `y < 0.8`.
- Require `0.3 < z < 1` and a defined `phiH` before filling all five pion
  histograms.
- Aggregate QA-bin histograms per run and produce mean and RMS timelines for
  each variable and pion charge.
- Continue processing legacy QADB files that do not contain pion histograms.

## Non-goals

- Kaon or proton timelines.
- Beam-spin-asymmetry calculation or helicity-dependent pion histograms.
- New PID-quality, detector-status, or fiducial cuts beyond the event-builder
  pion PID and the cuts already present in `monitorRead.groovy`.
- Refactoring the existing electron or inclusive-kinematics implementation.

## Anti-goals

- Do not silently tighten the reference event selection.
- Do not emit plausible zero-valued timeline points for missing or empty data.
- Do not make older histogram files prevent charge, yield, helicity, or
  inclusive-electron QADB outputs from being written.

## Constraints

- Java 21, Groovy 5.1.2, coat-libs 14.1.2, and the existing GROOT/HIPO APIs.
- Match the legacy histogram binning: 50 bins, with ranges `p: [0,10] GeV`,
  `pT: [0,4] GeV`, `z: [0,1]`, `theta: [0,pi/2] rad`, and
  `phiH: [-3.15,3.15] rad`.
- Use the beam energy already passed to `QadbBinHistograms` and the selected
  `Electron` already produced once per event.
- Preserve unrelated tracked and untracked workspace changes.

## Approaches

### Selected: focused pion component

Add a `PionKinematics` histogram component for both pion charges and a
`qadb_pion_kinematics` run-level analysis. This follows the boundaries used by
`InclusiveKinematics`, keeps the pion selection in one place, and avoids
premature abstractions for hadrons that are not in scope.

### Considered: generic identified-hadron component

A configurable component could cover pions, kaons, and protons immediately.
This was rejected for this change because the additional particles need their
own selection decisions and are optional for the supervisor's immediate goal.

### Considered: reuse the legacy physics-QA output directly

The existing `monitorRead.groovy` histograms already contain the desired
variables, but the QADB pipeline now creates histograms independently in
`QadbBinHistograms`. Reading the legacy output would couple the two pipelines
and would not supply the new QADB-bin histograms where they are needed.

## Design

`PionKinematics` will own ten histograms under `/QADB/pion_kinematics`: five
variables for each of `pip` and `pim`. For each event it will return immediately
unless the selected electron exists and passes the three DIS cuts. It will then
loop over `REC::Particle`, accept rows whose PID is exactly `211` or `-211`,
construct the pion four-vector, calculate the five legacy quantities, apply the
`z` and defined-`phiH` cuts, and fill all five histograms for the corresponding
charge. Non-finite quantities are not filled.

`QadbBinHistograms` will construct this component with the QA-bin number and
beam energy, process it after `Electron`, and write it with the other QADB
histograms.

`qadb_pion_kinematics` will read all available QA-bin pion histograms, combine
them by run, and write four files:

- `qadb_piPlus_kinematics_mean.hipo`
- `qadb_piPlus_kinematics_rms.hipo`
- `qadb_piMinus_kinematics_mean.hipo`
- `qadb_piMinus_kinematics_rms.hipo`

Each file will contain the five per-run distributions and five timeline graphs.
Runs with missing legacy histograms are skipped with a warning; variables with
empty or non-finite distributions do not receive timeline points.

The `qadb` coordinator will call the new run-level analysis alongside the
existing charge, yield, helicity, and inclusive-electron analyses.

## Verification

Unit tests will cover the reference kinematic values, both pion charges,
boundary cuts, wrong PIDs, undefined/non-finite quantities, HIPO round trips,
QA-bin integration, run aggregation, output files, empty distributions, legacy
files, and coordinator delegation. The complete Maven test suite must pass.
